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
/*
 * Created on Nov 20, 2003
 *
 */
package itensil.repository;

import java.io.Serializable;
import java.util.Date;

import itensil.security.Group;
import itensil.security.User;
import itensil.util.UriHelper;

import javax.xml.namespace.QName;


/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultMutableRepositoryNode
    extends DefaultRepositoryNode
    implements MutableRepositoryNode, Serializable {


    private Repository home;
    
    /**
    *
    * @param nodeId
    * @param parentNodeId
    * @param uri
    * @param isCollection
    * @param owner
    * @param home
    */
   public DefaultMutableRepositoryNode(
           String nodeId,
           String parentNodeId,
           String uri,
           boolean isCollection,
           User owner,
           Group contextGroup,
           Repository home) {
	   
	   super(nodeId, parentNodeId, uri, isCollection, owner, contextGroup);
	   this.home = home;
   }

    /**
     *
     * @param nodeId
     * @param parentNodeId
     * @param uri
     * @param isCollection
     * @param owner
     * @param home
     */
    public DefaultMutableRepositoryNode(
            String nodeId,
            String parentNodeId,
            String uri,
            boolean isCollection,
            User owner,
            Repository home) {

    	this(nodeId, parentNodeId, uri, isCollection, owner, null, home);
    }

    /**
     *
     * @param node
     * @param home
     * @param mount
     */
    public DefaultMutableRepositoryNode(
            RepositoryNode node, Repository home, String mount) {

        this(   node.getNodeId(),
                node.getParentNodeId(),
                UriHelper.absoluteUri(mount, node.getUri()),
                node.isCollection(),
                node.getOwner(),
                home);
    }

    /**
     * @see MutableRepositoryNode#getVersions()
     */
    public NodeVersion[] getVersions() throws AccessDeniedException {

        try {
            return home.getVersions(getNodeId());
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#getProperties(NodeVersion)
     */
    public NodeProperties getProperties(NodeVersion version)
            throws AccessDeniedException {

        try {
            return home.getProperties(getNodeId(), version);
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#setProperties(NodeProperties)
     */
    public void setProperties(NodeProperties properties)
            throws AccessDeniedException, LockException {

        try {
            home.setProperties(getNodeId(), properties);
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#getLocks()
     */
    public NodeLock[] getLocks() throws AccessDeniedException {

        try {
            return home.getLocks(getNodeId());
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#remove()
     */
    public void remove() throws AccessDeniedException, LockException {

        try {
            home.removeNode(getNodeId());
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }

    }

    /**
     * @see MutableRepositoryNode#getPermissions()
     */
    public NodePermission[] getPermissions() throws AccessDeniedException {

        try {
            return home.getPermissions(getNodeId());
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#getChildren()
     */
    public RepositoryNode[] getChildren() throws AccessDeniedException {

        try {
            return home.getChildren(getNodeId());
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#getParent()
     */
    public RepositoryNode getParent() throws AccessDeniedException {
        try {
            return home.getNode(getParentNodeId(), false);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }


    /**
     * @see MutableRepositoryNode#getContent(NodeVersion)
     */
    public NodeContent getContent(NodeVersion version)
            throws AccessDeniedException, LockException {

        try {
            return home.getContent(getNodeId(), version);
        } catch (NotFoundException e) {
             throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#setContent(NodeContent)
     */
    public void setContent(NodeContent content)
            throws AccessDeniedException, LockException {

        try {
            home.setContent(getNodeId(), content);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }


    /**
     * @see MutableRepositoryNode#grantPermission(NodePermission)
     */
    public void grantPermission(NodePermission permission)
            throws AccessDeniedException {

        try {
            home.grantPermission(getNodeId(), permission);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#revokePermission(NodePermission)
     */
    public void revokePermission(NodePermission permission)
            throws AccessDeniedException {

        try {
            home.revokePermission(getNodeId(), permission);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

   /**
    * @see MutableRepositoryNode#copy(String, boolean)
    */
    public RepositoryNode copy(String dstUri, boolean deep)
        throws
           AccessDeniedException,
           NotFoundException,
           DuplicateException,
           LockException {

         return RepositoryHelper.copy(home, getNodeId(), dstUri, deep);
    }

    /**
     * @see MutableRepositoryNode#move(String)
     */
    public void move(String dstUri)
        throws
           AccessDeniedException,
           NotFoundException,
           DuplicateException,
           LockException {

        RepositoryNode updated =
            RepositoryHelper.move(home, getNodeId(), dstUri);
        this.home = RepositoryHelper.getRepository(dstUri);
        this.uri = updated.getUri();
        this.parentNodeId = updated.getParentNodeId();
    }

    /**
     * @see MutableRepositoryNode#getPropertyValue(QName)
     */
    public String getPropertyValue(QName name) throws AccessDeniedException {

        try {
            return home.getPropertyValue(getNodeId(), name);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#setDefaultVersion(NodeVersion)
     */
    public void setDefaultVersion(NodeVersion version)
            throws AccessDeniedException, NotFoundException, LockException {

        home.setDefaultVersion(getNodeId(), version);
    }

    /**
     * @see MutableRepositoryNode#putLock(itensil.security.User, Date, boolean, boolean, String)
     */
    public NodeLock putLock(
        User owner,
        Date expireTime,
        boolean exclusive,
        boolean inheritable,
        String ownerInfo)
        throws AccessDeniedException, LockException {

        try {
            return home.putLock(
                getNodeId(),
                owner,
                expireTime,
                exclusive,
                inheritable,
                ownerInfo);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#renewLock(NodeLock)
     */
    public void renewLock(NodeLock lock) throws AccessDeniedException {

        try {
            home.renewLock(getNodeId(), lock);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#killLock(NodeLock)
     */
    public void killLock(NodeLock lock) throws AccessDeniedException {

        try {
            home.killLock(getNodeId(), lock);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#isExclusiveLocked(User)
     */
    public boolean isExclusiveLocked(User toUser) throws AccessDeniedException {

        try {
            return home.isExclusiveLocked(getNodeId(), toUser);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see MutableRepositoryNode#hasPermission(NodePermission)
     */
    public boolean hasPermission(NodePermission perm)
        throws AccessDeniedException {

        try {
            return home.hasPermission(getNodeId(), perm);
        } catch (NotFoundException e) {
            throw new RepositoryException(e);
        }
    }

	public Repository getRepository() throws AccessDeniedException {
		return home;
	}
	
	/**
     * @see MutableRepositoryNode#pruneVersions(int)
     */
	public void pruneVersions(int keepRecentCount) throws AccessDeniedException, LockException {
		throw new AccessDeniedException(getNodeId(), "Pruning not supported");
	}

	/**
	 * 
	 */
	public void setContextGroup(Group group) throws AccessDeniedException {
		throw new AccessDeniedException(getNodeId(), "setContextGroup not supported");
	}
}
