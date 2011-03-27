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
 * Created on Aug 27, 2003
 *
 */
package itensil.repository;

import java.util.Date;
import javax.xml.namespace.QName;

import itensil.security.Group;
import itensil.security.User;


/**
 * @author ggongaware@itensil.com
 */
public interface MutableRepositoryNode extends RepositoryNode {

    /**
     * @return
     * @throws AccessDeniedException
     */
    public NodeVersion[] getVersions() throws AccessDeniedException;

    /**
     * @param version
     * @return
     * @throws AccessDeniedException
     */
    public NodeProperties getProperties(NodeVersion version)
            throws AccessDeniedException;

    /**
     * @param properties
     * @throws AccessDeniedException
     */
    public void setProperties(NodeProperties properties)
            throws AccessDeniedException, LockException;

    /**
     * @return
     * @throws AccessDeniedException
     */
    public NodeLock[] getLocks() throws AccessDeniedException;

    /**
     * @param owner
     * @param expireTime
     * @param exclusive
     * @param inheritable
     * @return
     * @throws AccessDeniedException
     * @throws LockException
     */
    public NodeLock putLock(
        User owner,
        Date expireTime,
        boolean exclusive,
        boolean inheritable,
        String ownerInfo)
        throws AccessDeniedException, LockException;

    /**
     * @param lock
     * @throws AccessDeniedException
     */
    public void killLock(NodeLock lock) throws AccessDeniedException;

    /**
     * Checked recursively
     * @param toUser
     * @return true if locked by some other than toUser
     * @throws AccessDeniedException
     */
    public boolean isExclusiveLocked(User toUser)
        throws AccessDeniedException;

    /**
     * @param lock
     * @throws AccessDeniedException
     */
    public void renewLock(NodeLock lock) throws AccessDeniedException;

    /**
     * @throws AccessDeniedException
     * @throws LockException
     */
    public void remove() throws AccessDeniedException, LockException;

    /**
     * @return local subset of permissions (not recurisve)
     * @throws AccessDeniedException
     */
    public NodePermission[] getPermissions() throws AccessDeniedException;

    /**
     * Checked recursively
     * @param perm
     * @return
     * @throws AccessDeniedException
     */
    public boolean hasPermission(NodePermission perm)
        throws AccessDeniedException;

    /**
     * Overwrites previous permissions for a principal
     *
     * @param permission
     * @throws AccessDeniedException
     */
    public void grantPermission(NodePermission permission)
        throws AccessDeniedException;

    /**
     * @param permission
     * @throws AccessDeniedException
     */
    public void revokePermission(NodePermission permission)
        throws AccessDeniedException;

    /**
     * @return
     * @throws AccessDeniedException
     */
    public RepositoryNode[] getChildren() throws AccessDeniedException;

    /**
     * @return
     * @throws AccessDeniedException
     */
    public RepositoryNode getParent() throws AccessDeniedException;
    
    /**
     * @return
     * @throws AccessDeniedException
     */
    public Repository getRepository() throws AccessDeniedException;

    /**
     * @param version
     * @return
     * @throws AccessDeniedException
     * @throws LockException
     */
    public NodeContent getContent(NodeVersion version)
        throws AccessDeniedException, LockException;

    /**
     * @param content
     * @throws AccessDeniedException
     * @throws LockException
     */
    public void setContent(NodeContent content)
        throws AccessDeniedException, LockException;

    /**
     * 
     * @param group
     * @throws AccessDeniedException
     */
    public void setContextGroup(Group group)
    	throws AccessDeniedException;
    
    /**
     * @param dstUri
     * @param deep
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode copy(String dstUri, boolean deep)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     * @param dstUri
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public void move(String dstUri)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     *
     * @param name
     * @return
     * @throws AccessDeniedException
     */
    public String getPropertyValue(QName name)
            throws AccessDeniedException;

    
    /**
     * Change which version is the default
     *
     * @param version
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void setDefaultVersion(NodeVersion version)
        throws
            AccessDeniedException,
            NotFoundException,
            LockException;

    /**
     * Prune all but the most recent versions
     * 
     * @param keepRecentCount
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
	public void pruneVersions(int keepRecentCount)
		throws
		    AccessDeniedException,
		    LockException;
}
