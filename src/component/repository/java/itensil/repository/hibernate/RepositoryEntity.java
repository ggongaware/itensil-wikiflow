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
import itensil.repository.search.*;
import itensil.security.User;
import itensil.security.SecurityAssociation;
import itensil.security.AuthenticatedUser;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.io.HibernateUtil;

import javax.xml.namespace.QName;
import java.util.*;
import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.Hibernate;

/**
 * @author ggongaware@itensil.com
 *
 */
public class RepositoryEntity implements Repository, Serializable {

    private int id;
    private String mount;
    private EntityManager manager;
    private Set<Mount> mounts;
    private Set<NodeEntity> nodeEntities;

    public RepositoryEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMount() {
        return mount;
    }

    public RepositoryManager getManager() {
        return manager;
    }

    /**
     *
     * @param uri
     * @return node object
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public MutableRepositoryNode getNodeByUri(String uri, boolean forUpdate) throws AccessDeniedException, NotFoundException {

        User caller = SecurityAssociation.getUser();
        authorizedCheck(caller);

        String localUri = localizeUri(uri);

        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("Repo.getNodeByUri");
        qry.setEntity("repo", this);
        qry.setString("uri", localUri);
        if (forUpdate) qry.setLockMode("node", LockMode.UPGRADE);
        NodeEntity node = (NodeEntity)qry.uniqueResult();
        if (node == null) {
            if (!existingAncestorBlock(caller, localUri)) {
                throw new NotFoundException(uri);
            } else {
                throw new AccessDeniedException(uri, "read");
            }
        }
        if (!hasPermission(caller, node, DefaultNodePermission.READ)) {
            throw new AccessDeniedException(uri, "read");
        }
        node.setRepoEntity(this);
        return node;
    }

    /**
     *
     * @param nodeId
     * @return node object
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public MutableRepositoryNode getNode(String nodeId, boolean forUpdate) throws AccessDeniedException, NotFoundException {

        User caller = SecurityAssociation.getUser();
        authorizedCheck(caller);
        Session session = HibernateUtil.getSession();

        Query qry = session.getNamedQuery("Repo.getNode");
        qry.setString("id", nodeId);
        qry.setEntity("repo", this);
        if (forUpdate) qry.setLockMode("node", LockMode.UPGRADE);

        NodeEntity node = (NodeEntity)qry.uniqueResult();

        if (node == null) {
            throw new NotFoundException(nodeId);
        }
        if (!hasPermission(caller, node, DefaultNodePermission.READ)) {
            throw new AccessDeniedException(nodeId, "read");
        }
        node.setRepoEntity(this);
        return node;
    }

    /**
     *
     * @param uri
     * @param isCollection
     * @param owner
     * @return node object
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public MutableRepositoryNode createNode(String uri, boolean isCollection, User owner)
            throws AccessDeniedException, NotFoundException, DuplicateException, LockException {

        User caller = SecurityAssociation.getUser();
        authorizedCheck(caller);
        Session session = HibernateUtil.getSession();

        uri = localizeUri(uri);

        // check parent elegability
        String parentUri = UriHelper.getParent(uri);
        if (parentUri.equals(uri)) {
            throw new DuplicateException(parentUri);
        }
        Query qry = session.getNamedQuery("Repo.getNodeByUri");
        qry.setEntity("repo", this);
        qry.setString("uri", parentUri);
        NodeEntity parentNode = (NodeEntity)qry.uniqueResult();
        if (parentNode == null) {
            if (!existingAncestorBlock(caller, parentUri)) {
                throw new NotFoundException(absoluteUri(parentUri));
            } else {
                throw new AccessDeniedException(absoluteUri(parentUri), "read");
            }
        }
        if (!parentNode.isCollection()) {
            throw new AccessDeniedException(absoluteUri(parentUri), "collection");
        }
        if (!hasPermission(caller, parentNode, DefaultNodePermission.CREATE)) {
            throw new AccessDeniedException(absoluteUri(parentUri), "create");
        }

        // check for duplicates
        qry.setString("uri", uri);
        Iterator itr = qry.iterate();
        if (itr.hasNext()) {
            throw new DuplicateException(uri);
        }

        // parent locked?
        checkLocked(caller, parentNode);

        NodeEntity node = new NodeEntity();
        node.initNew();
        node.setOwner(owner);
        node.setParentNode(parentNode);
        node.setCollection(isCollection);
        node.setLocalUri(uri);
        node.setRepoEntity(this);
        node.setCreateTime(new Date());
        session.persist(node);

        Set<NodeEntity> parKids = parentNode.getChildEntities();
        if (Hibernate.isInitialized(parKids)) {
            parKids.add(node);
        }

        return node;
    }


    /**
     *
     * @param nodeId
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void removeNode(String nodeId) throws AccessDeniedException, NotFoundException, LockException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        removeNode(node);
    }
    

    /**
     * 
     * @param node
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
	public void removeNode(NodeEntity node)  throws AccessDeniedException, NotFoundException, LockException {
		
		User caller = SecurityAssociation.getUser();
		
		if (!hasPermission(caller, node, DefaultNodePermission.WRITE)) {
            throw new AccessDeniedException(node.getUri(), "write");
        }
        /*
            perhaps a lock check here? but this guy has managed to
            worked without (under light use) for 2 years
        */
        deepRemoveNode(node,  node.getNodeId());
        Set parKids = node.getParentNode().getChildEntities();
        if (Hibernate.isInitialized(parKids)) {
            parKids.remove(node);
        }
        
        getManager().fireContentChangeEvent(node, null, ContentEvent.Type.REMOVE);
        
        Session session = HibernateUtil.getSession();
        session.flush();
        session.lock(node, LockMode.NONE);
        session.evict(node);
	}

    protected void deepRemoveNode(NodeEntity node, String deleteStamp) {
        Session session = HibernateUtil.getSession();
        Query qry;

        long deleteTime = System.currentTimeMillis();

        // delete the kids
        if (node.isCollection()) {
            qry = session.getNamedQuery("Repo.removeCollection");
            qry.setEntity("repo", this);
            qry.setString("uriPat", node.getLocalUri() + "/%");
            qry.setString("deleteStamp", deleteStamp);
            qry.setLong("deleted", deleteTime);
            qry.executeUpdate();
        }

        node.setLocalUri(node.getLocalUri() + deleteStamp);
        node.setDeleted(deleteTime);
        session.update(node);
    }

    public NodeVersion[] getVersions(String nodeId) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getVersions();
    }

    public void setDefaultVersion(String nodeId, NodeVersion version)
            throws AccessDeniedException, NotFoundException, LockException {

        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.setDefaultVersion(version);
    }

    public RepositoryNode[] getChildren(String nodeId) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getChildren();
    }

    public void grantPermission(String nodeId, NodePermission perm) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.grantPermission(perm);
    }

    public NodePermission[] getPermissions(String nodeId) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getPermissions();
    }

    public void revokePermission(String nodeId, NodePermission perm) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.revokePermission(perm);
    }

    public boolean hasPermission(String nodeId, NodePermission perm) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.hasPermission(perm);
    }

    public NodeLock putLock(
            String nodeId, User owner, Date expireTime, boolean exclusive, boolean inheritable, String ownerInfo)
            throws AccessDeniedException, NotFoundException, LockException {

        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        return node.putLock(owner, expireTime, exclusive, inheritable, ownerInfo);
    }

    public void renewLock(String nodeId, NodeLock lock) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.renewLock(lock);
    }

    public NodeLock[] getLocks(String nodeId) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getLocks();
    }

    public void killLock(String nodeId, NodeLock lock) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.killLock(lock);
    }

    public boolean isExclusiveLocked(String nodeId, User toUser) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.isExclusiveLocked(toUser);
    }

    public NodeContent getContent(String nodeId, NodeVersion version)
            throws AccessDeniedException, NotFoundException, LockException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getContent(version);
    }

    public void setContent(String nodeId, NodeContent content)
            throws AccessDeniedException, NotFoundException, LockException {
        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.setContent(content);
    }

    public NodeProperties getProperties(String nodeId, NodeVersion version)
            throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getProperties(version);
    }

    public String getPropertyValue(String nodeId, QName name) throws AccessDeniedException, NotFoundException {
        NodeEntity node = (NodeEntity)getNode(nodeId, false);
        return node.getPropertyValue(name);
    }

    public void setProperties(String nodeId, NodeProperties properties)
            throws AccessDeniedException, NotFoundException, LockException {

        NodeEntity node = (NodeEntity)getNode(nodeId, true);
        node.setProperties(properties);
    }

    public RepositoryNode copy(String srcNodeId, String dstUri, boolean deep)
            throws AccessDeniedException, NotFoundException, DuplicateException, LockException {

        return getManager().copy(
                this,
                srcNodeId,
                RepositoryHelper.getRepository(dstUri),
                dstUri,
                deep);
    }

    public RepositoryNode move(String srcNodeId, String dstUri)
            throws AccessDeniedException, NotFoundException, DuplicateException, LockException {

        return getManager().move(
                this,
                srcNodeId,
                RepositoryHelper.getRepository(dstUri),
                dstUri);
    }

    public BasicSearchResultSet search(BasicSearch query)
            throws AccessDeniedException, NotFoundException, LockException, SearchException {

        User caller = SecurityAssociation.getUser();

        String sUri = query.getScopeUri();
        if (sUri.endsWith("%")) {
            sUri = UriHelper.getParent(sUri);
        }
        RepositoryNode scopeNode = getNodeByUri(sUri, false);
        DefaultBasicSearchResultSet results = BasicSearcher.doSearch(this, query);
        List<DefaultBasicSearchResultSet.Entry> entries = results.getEntryList();

        // treat any depth > 1 as infinite
        if (query.getScopeDepth() == 1) {
            ArrayList<DefaultBasicSearchResultSet.Entry> scopeEntries =
                    new ArrayList<DefaultBasicSearchResultSet.Entry>(entries.size());
            String scopeID = scopeNode.getNodeId();
            for (DefaultBasicSearchResultSet.Entry entry : entries) {
                if (scopeID.equals(entry.node.getParentNodeId())) {
                    scopeEntries.add(entry);
                }
            }
            results.setEntryList(scopeEntries);
        }

        // Do some real result searching
        BasicSearchEngine engine = new BasicSearchEngine(query);
        results = engine.execute(results);
        entries = results.getEntryList();

        // filter results with no read permissions
        ArrayList<DefaultBasicSearchResultSet.Entry> accessEntries =
                new ArrayList<DefaultBasicSearchResultSet.Entry>(entries.size());
        for (DefaultBasicSearchResultSet.Entry entry : entries) {
            if (((NodeEntity)entry.node).hasPermission(caller, DefaultNodePermission.READ)) {
                accessEntries.add(entry);
            }
        }
        results.setEntryList(accessEntries);
        entries = accessEntries;

        // do 'order by'
        engine.sort(entries);

        // do limits
        int limit = query.getLimit();
        if (limit > 0 && limit < entries.size()) {
            int resize = entries.size() - limit;
            for (int i = 0; i < resize; i++) {
                entries.remove(entries.size() - 1);
            }
        }

        // prune non-selected properties
        QName selProps[] = query.getSelectProperties();
        if (selProps != null) {
            for (DefaultBasicSearchResultSet.Entry entry : entries) {
                NodeProperties props = entry.properties;
                QName propNames[] = props.getNames();
                Map<QName, String> keepProps = entry.properties.getPropertyMap();
                for (QName propName : propNames) {
                    boolean selected = false;
                    for (QName selProp : selProps) {
                        if (selProp.equals(propName)) {
                            selected = true;
                            break;
                        }
                    } // sel loop

                    if (!selected) keepProps.remove(propName);

                } // prop loop
                entry.properties = new DefaultNodeProperties(entry.properties.getVersion(), keepProps);
            } // entry loop
        }
        return results;
    }
    
    
    /**
     * @param uriPat - SQL - LIKE style pattern
     * @param limit - how many to return
     * @param minPerms - the minimum permission level to include
     * @return
     */
    public List<MutableRepositoryNode> getRecentlyModified(String uriPat, int limit, NodePermission minPerms) {
    	
    	Query qry = HibernateUtil.getSession().getNamedQuery("Repo.recentlyModifiedNodes");
	   	qry.setEntity("repo", this);
	   	qry.setString("uriPat", localizeUri(uriPat));
	   	qry.setMaxResults(limit * 2);
	   	ArrayList<MutableRepositoryNode> nodes = new ArrayList<MutableRepositoryNode>(limit);
	   	int qualified = 0;
	   	for (Object obj : qry.list()) {
		   MutableRepositoryNode nod = (MutableRepositoryNode)obj;
		   try {
			   if (nod.hasPermission(minPerms)) {
				   nodes.add(nod);
				   qualified++;
				   if (qualified >= limit) break;
			   }
			} catch (AccessDeniedException e) {
				// eat it
			}
	   	}	   	
	   	return nodes;
    }

    public void setMount(String mount) {
        this.mount = mount;
    }

    public void setManager(EntityManager manager) {
        this.manager = manager;
    }

    public String localizeUri(String uri) {
    	if (mount.equals(uri)) return "";
        return UriHelper.localizeUri(mount, uri);
    }

    protected boolean hasPermission(User user, NodeEntity node, int permission) throws AccessDeniedException {
        return node.hasPermission(user, permission);
    }

    protected boolean existingAncestorBlock(User user, String uri) throws AccessDeniedException {
        Session session = HibernateUtil.getSession();
        String parentUri = UriHelper.getParent(uri);
        Query qry = session.getNamedQuery("Repo.getNodeByUri");
        qry.setEntity("repo", this);

        String lastUri = uri;
        while (!parentUri.equals(lastUri)) {
            qry.setString("uri", parentUri);
            NodeEntity parentNode = (NodeEntity)qry.uniqueResult();
            if (parentNode != null) {
                return !hasPermission(user, parentNode, DefaultNodePermission.READ);
            }
            lastUri = parentUri;
            parentUri = UriHelper.getParent(parentUri);
        }
        return false;
    }

    protected void checkLocked(User user, NodeEntity node) throws LockException, AccessDeniedException {
        if (node.isExclusiveLocked(user)) {
            throw new LockException(node.getUri(), node.getExclusiveLock().getOwner().toString());
        }
    }

    protected void authorizedCheck(User caller) throws AccessDeniedException {
        if (!(caller instanceof AuthenticatedUser))
            throw new AccessDeniedException("authorizedCheck", caller.toString());
    }

    public String absoluteUri(String localUri) {
        return Check.isEmpty(localUri) ? mount : UriHelper.absoluteUri(mount, localUri);
    }

    public Set<Mount> getMounts() {
        return mounts;
    }

    public void setMounts(Set<Mount> mounts) {
        this.mounts = mounts;
    }

    public Set<NodeEntity> getNodeEntities() {
        return nodeEntities;
    }

    public void setNodeEntities(Set<NodeEntity> nodeEntities) {
        this.nodeEntities = nodeEntities;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RepositoryEntity that = (RepositoryEntity) o;

        return id == that.id;
    }

    public int hashCode() {
        return id;
    }
}
