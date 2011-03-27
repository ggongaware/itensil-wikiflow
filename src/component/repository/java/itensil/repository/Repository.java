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
 * Created on Aug 26, 2003
 *
 */
package itensil.repository;

import itensil.repository.search.BasicSearch;
import itensil.repository.search.BasicSearchResultSet;
import itensil.repository.search.SearchException;
import itensil.security.User;

import javax.xml.namespace.QName;
import java.util.Date;
import java.util.List;


/**
 * @author ggongaware@itensil.com
 */
public interface Repository {

    /**
     * @return mount string
     */
    public String getMount();

    /**
     * @return manager object
     */
    public RepositoryManager getManager();

    /**
     *
     * @param nodeId
     * @param forUpdate - will this node's content/properties be changed? 
     * 		(not required but helps locking)
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public MutableRepositoryNode getNode(String nodeId, boolean forUpdate)
        throws AccessDeniedException, NotFoundException;

    /**
     *
     * @param uri
     * @param forUpdate - will this node's content/properties be changed? 
     * 		(not required but helps locking)
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public MutableRepositoryNode getNodeByUri(String uri, boolean forUpdate)
        throws AccessDeniedException, NotFoundException;

    /**
     *
     * @param uri
     * @param isCollection
     * @param owner
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public MutableRepositoryNode createNode(
        String uri,
        boolean isCollection,
        User owner)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     *
     * @param nodeId
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void removeNode(String nodeId)
        throws AccessDeniedException, NotFoundException, LockException;

    /**
     * @param nodeId
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public NodeVersion[] getVersions(String nodeId)
        throws AccessDeniedException, NotFoundException;


    /**
     * Change which version is the default
     *
     * @param nodeId
     * @param version
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void setDefaultVersion(String nodeId, NodeVersion version)
        throws
            AccessDeniedException,
            NotFoundException,
            LockException;

    /**
     * @param nodeId
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public RepositoryNode[] getChildren(String nodeId)
        throws AccessDeniedException, NotFoundException;

    /**
     * Overwrites previous permissions for a principal
     *
     * @param nodeId
     * @param perm
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public void grantPermission(String nodeId, NodePermission perm)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @return local subset of permissions (not recurisve)
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public NodePermission[] getPermissions(String nodeId)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @param perm
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public void revokePermission(String nodeId, NodePermission perm)
        throws AccessDeniedException, NotFoundException;


    /**
     * Checked recursively
     * Ignores inheritable option
     * @param nodeId
     * @param perm
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public boolean hasPermission(String nodeId, NodePermission perm)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @param owner
     * @param expireTime
     * @param exclusive
     * @param inheritable
     * @return NodeLock object or null if not lockable
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public NodeLock putLock(
        String nodeId,
        User owner,
        Date expireTime,
        boolean exclusive,
        boolean inheritable,
        String ownerInfo)
        throws AccessDeniedException, NotFoundException, LockException;

    /**
     * @param nodeId
     * @param lock
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public void renewLock(String nodeId, NodeLock lock)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public NodeLock[] getLocks(String nodeId)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @param lock
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public void killLock(String nodeId, NodeLock lock)
        throws AccessDeniedException, NotFoundException;

    /**
     * Checked recursively
     * @param nodeId
     * @param toUser
     * @return true if locked by some other than toUser
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public boolean isExclusiveLocked(String nodeId, User toUser)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @param version
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public NodeContent getContent(String nodeId, NodeVersion version)
        throws AccessDeniedException, NotFoundException, LockException;

    /**
     * @param nodeId
     * @param content
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void setContent(String nodeId, NodeContent content)
        throws AccessDeniedException, NotFoundException, LockException;

    /**
     * @param nodeId
     * @param version
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public NodeProperties getProperties(String nodeId, NodeVersion version)
        throws AccessDeniedException, NotFoundException;


    /**
     * Assumes default version
     *
     * @param nodeId
     * @param name
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     */
    public String getPropertyValue(String nodeId, QName name)
        throws AccessDeniedException, NotFoundException;

    /**
     * @param nodeId
     * @param properties
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     */
    public void setProperties(String nodeId, NodeProperties properties)
        throws AccessDeniedException, NotFoundException, LockException;

    /**
     * @param srcNodeId
     * @param dstUri
     * @param deep
     * @return the root copy
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode copy(String srcNodeId, String dstUri, boolean deep)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     * @param srcNodeId
     * @param dstUri
     * @return the root move
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode move(String srcNodeId, String dstUri)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     * @param query
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException
     * @throws SearchException
     */
    public BasicSearchResultSet search(BasicSearch query)
        throws
            AccessDeniedException,
            NotFoundException,
            LockException,
            SearchException;
    
    /**
     * @param uriPat - SQL - LIKE style pattern
     * @param limit - how many to return
     * @param minPerms - the minimum permission level to include
     * @return
     */
    public List<MutableRepositoryNode> getRecentlyModified(String uriPat, int limit, NodePermission minPerms);
}
