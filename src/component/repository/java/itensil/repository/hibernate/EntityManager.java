/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.repository.hibernate;

import itensil.repository.*;
import itensil.repository.event.ContentEvent;
import itensil.security.*;
import itensil.io.HibernateUtil;
import itensil.util.UriHelper;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author ggongaware@itensil.com
 *
 */
public class EntityManager implements RepositoryManager, Serializable {

    private String userSpaceId;
    private transient RepositoryManagerFactory factory;

    public EntityManager(String userSpaceId, RepositoryManagerFactory factory) {
        this.userSpaceId = userSpaceId;
        this.factory = factory;
    }

    public Repository createRepository(String mount, User owner, Group editors, Group creators, Group readers, Group guests)
            throws AccessDeniedException, DuplicateException {

        User caller = SecurityAssociation.getUser();
        if (SysAdmin.isSysAdmin(caller)) {
            Session session = HibernateUtil.getSession();

            // 1. Check for duplicate
            Query qry = session.getNamedQuery("RepoMan.getRepoByMount");
            qry.setString("mount", mount);
            if (qry.iterate().hasNext()) {
                throw new DuplicateException(mount);
            }

            // 2. Create Repository
            RepositoryEntity repository = new RepositoryEntity();
            repository.setMount(mount);
            repository.setManager(this);
            session.persist(repository);

            // 3. Create root node
            NodeEntity root = new NodeEntity();
            root.initNew();
            root.setRepoEntity(repository);
            root.setLocalUri("");
            root.setCollection(true);
            root.setOwner(owner);
            root.setCreateTime(new Date());
            session.persist(root);
            
            // 4. Assign creator permissions
            if (editors != null) {
                PermissionEntity perm = new PermissionEntity(editors, DefaultNodePermission.WRITE, true);
                perm.setNodeEntity(root);
                session.persist(perm);
            }

            // 5. Assign creator permissions
            if (creators != null) {
                PermissionEntity perm = new PermissionEntity(creators, DefaultNodePermission.CREATE, true);
                perm.setNodeEntity(root);
                session.persist(perm);
            }

            // 6. Assign reader permissions
            if (readers != null) {
                PermissionEntity perm = new PermissionEntity(readers, DefaultNodePermission.READ, true);
                perm.setNodeEntity(root);
                session.persist(perm);
            }
            
            // 7. Assign reader permissions
            if (guests != null) {
                PermissionEntity perm = new PermissionEntity(guests, DefaultNodePermission.READ, true);
                perm.setNodeEntity(root);
                session.persist(perm);
            }

            return repository;
        }
        throw new AccessDeniedException("createRepository", "sysadmin");
    }

    public void addRepositoryMount(String mount, boolean isPrimary)
            throws NotFoundException, AccessDeniedException {

        User caller = SecurityAssociation.getUser();
        if (SysAdmin.isSysAdmin(caller)) {
            Session session = HibernateUtil.getSession();
            Query qry = session.getNamedQuery("RepoMan.getRepoByMount");
            qry.setString("mount", mount);
            Iterator itr = qry.iterate();
            if (!itr.hasNext()) {
                throw new NotFoundException(mount);
            }
            RepositoryEntity repository = (RepositoryEntity)itr.next();
            if (isPrimary) {
                qry = session.getNamedQuery("RepoMan.resetPrimaryMount");
                qry.setString("userSpaceId", userSpaceId);
                qry.executeUpdate();
            }
            Mount mntEnt = new Mount();
            mntEnt.setUserSpaceId(userSpaceId);
            mntEnt.setRepoEntity(repository);
            mntEnt.setPrimary(isPrimary);
            session.saveOrUpdate(mntEnt);
            return;
        }
        throw new AccessDeniedException("addRepositoryMount", "sysadmin");
    }

    public Repository getPrimaryRepository() {
        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("RepoMan.getPrimaryRepository");
        qry.setString("userSpaceId", userSpaceId);
        RepositoryEntity repo = (RepositoryEntity)qry.uniqueResult();
        if (repo != null) repo.setManager(this);
        return repo;
    }

    /**
     * Primary will be at index = 0
     * @return object array
     */
    @SuppressWarnings("unchecked")
	public Repository [] getRepositories() {
        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("RepoMan.getRepositories");
        qry.setString("userSpaceId", userSpaceId);
        List<RepositoryEntity> reps = qry.list();
        for (RepositoryEntity repo : reps) {
            repo.setManager(this);
        }
        return reps.toArray(new Repository[reps.size()]);
    }

    public Repository getRepository(String mount) throws NotFoundException {
        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("RepoMan.getUserRepository");
        qry.setString("userSpaceId", userSpaceId);
        qry.setString("mount", mount);
        RepositoryEntity repo = (RepositoryEntity)qry.uniqueResult();
        if (repo == null) {
            throw new NotFoundException(mount);
        }
        repo.setManager(this);
        return repo;
    }

    /**
     * Fast for 100% entity based stores
     *  (this is the only support mode currently)
     * @param nodeId
     * @return
     * @throws NotFoundException
     */
    public Repository getRepositoryByNode(String nodeId) throws NotFoundException {
    	Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("RepoMan.getUserRepoByNode");
        qry.setString("nodeId", nodeId);
        List nodes = qry.list();
        if (nodes.isEmpty()) {
        	 throw new NotFoundException(nodeId);
        }
        NodeEntity nod = (NodeEntity)nodes.get(0);
        RepositoryEntity repo = nod.getRepoEntity();
        repo.setManager(this);
        return repo;
    }

    /**
     *
     * @param srcRepository
     * @param srcNodeId
     * @param dstRepository
     * @param dstUri
     * @param deep
     * @return new node
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode copy(
            Repository srcRepository, String srcNodeId, Repository dstRepository, String dstUri, boolean deep)
            throws AccessDeniedException, NotFoundException, DuplicateException, LockException {

        User caller = SecurityAssociation.getUser();
        authorizedCheck(caller);

        MutableRepositoryNode srcNode = (MutableRepositoryNode)srcRepository.getNode(srcNodeId, false);
        MutableRepositoryNode dstNode =
                (MutableRepositoryNode)dstRepository.createNode(dstUri, srcNode.isCollection(), caller);
        
        // bug not dest sometimes not ready for kids
        HibernateUtil.getSession().flush();
        
        deepCopy(caller, srcNode, dstNode, dstRepository, deep);
        return dstNode;
    }

    protected void deepCopy(
            User caller,
            MutableRepositoryNode srcNode,
            MutableRepositoryNode dstNode,
            Repository dstRepository,
            boolean deep)
            throws AccessDeniedException, LockException, NotFoundException, DuplicateException {

        DefaultNodeVersion defaultVersion = new DefaultNodeVersion();

        // copy props
        NodeProperties nprops = srcNode.getProperties(defaultVersion);
        if (nprops != null) {
            dstNode.setProperties(nprops);
        }

        // copy content
        NodeContent ncont = srcNode.getContent(defaultVersion);
        if (ncont != null) {
            dstNode.setContent(ncont);
        }

        // copy read-able kids
        if (srcNode.isCollection() && deep) {
            for (RepositoryNode srcKid : srcNode.getChildren()) {
                MutableRepositoryNode srcChildNode = (MutableRepositoryNode)srcKid;

                // Maybe this should collect the un-readable kids and report back to the caller ?
                if (srcChildNode.hasPermission(DefaultNodePermission.readPermission(caller))) {
                    String dstChildUri = UriHelper.absoluteUri(
                            dstNode.getUri(), UriHelper.localizeUri(srcNode.getUri(), srcChildNode.getUri()));
                    MutableRepositoryNode dstChildNode =
                        (MutableRepositoryNode)dstRepository.createNode(
                                dstChildUri, srcChildNode.isCollection(), caller);
                    deepCopy(caller, srcChildNode, dstChildNode, dstRepository, true);
                }
            }
        }
    }

    public RepositoryNode move(
            Repository srcRepository, String srcNodeId, Repository dstRepository, String dstUri)
            throws AccessDeniedException, NotFoundException, DuplicateException, LockException {

        User caller = SecurityAssociation.getUser();
        authorizedCheck(caller);

        if (!(dstRepository instanceof RepositoryEntity) || !(srcRepository instanceof RepositoryEntity)) {

            // this is slow and looses version history
            RepositoryNode resNode = copy(srcRepository, srcNodeId, dstRepository, dstUri, true);
            srcRepository.removeNode(srcNodeId);
            return resNode;
        }

        NodeEntity srcNode = (NodeEntity)srcRepository.getNode(srcNodeId, true);

        String dstParentUri = UriHelper.getParent(dstUri);
        String srcParentUri = UriHelper.getParent(srcNode.getUri());

         // check if inplace move
        boolean inplace = srcParentUri.equals(dstParentUri);
        NodeEntity dstParentNode = null;
        if (!inplace) {

            dstParentNode = (NodeEntity)dstRepository.getNodeByUri(dstParentUri, false);
            if (!dstParentNode.isCollection()) {
                throw new AccessDeniedException(dstParentUri, "collection");
            }

            // recursive check
            NodeEntity dstAncestor = dstParentNode;
            while (dstAncestor != null) {
                if (srcNodeId.equals(dstAncestor.getNodeId()))
                    throw new DuplicateException("recursive");
                dstAncestor = dstAncestor.getParentNode();
            }

            // need manage for relocating
            srcNode.checkPermission(DefaultNodePermission.MANAGE);

            // and no locks
            srcNode.checkLocked();

            // and can create
            dstParentNode.checkPermission(DefaultNodePermission.CREATE);
        }

        // duplicate check
        try {

            dstRepository.getNodeByUri(dstUri, false);
            throw new DuplicateException(dstUri);

        } catch (NotFoundException nfe) {
            // this is good
        }

        String localDstUri = UriHelper.localizeUri(dstRepository.getMount(), dstUri);

        Session session = HibernateUtil.getSession();
        if (srcNode.isCollection()) {
            Query qry = session.getNamedQuery("Repo.moveCollection");
            qry.setEntity("repo", srcRepository);
            qry.setString("uriPat", srcNode.getLocalUri() + "/%");
            qry.setEntity("dstRepo", dstRepository);
            qry.setString("dstUri", localDstUri);
            qry.setInteger("subUriLen", srcNode.getLocalUri().length());
            qry.executeUpdate();
        }

        srcNode.setLocalUri(localDstUri);
        if (!inplace) {
            Set<NodeEntity> parKids = srcNode.getParentNode().getChildEntities();
            if (Hibernate.isInitialized(parKids)) {
                parKids.remove(srcNode);
            }
            srcNode.setParentNode(dstParentNode);
            srcNode.setRepoEntity((RepositoryEntity)dstRepository);

            parKids = dstParentNode.getChildEntities();
            if (Hibernate.isInitialized(parKids)) {
                parKids.add(srcNode);
            }
        }

        session.update(srcNode);
        return srcNode;
    }
    
    public void fireContentChangeEvent(RepositoryNode node, NodeContent content, ContentEvent.Type type) {
    	factory.fireContentChangeEvent(new ContentEvent(node, content, type));
    }

    protected void authorizedCheck(User caller) throws AccessDeniedException {
        if (!(caller instanceof AuthenticatedUser))
            throw new AccessDeniedException("authorizedCheck", caller.toString());
    }
}
