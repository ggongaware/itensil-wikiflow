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
package itensil.repository;

import itensil.repository.event.ContentEvent;
import itensil.security.User;
import itensil.security.Group;

/**
 * @author ggongaware@itensil.com
 */
public interface RepositoryManager {

    /**
     * Create a new repository with root node
     * root node will have initial owner, creator and reader permissions
     *
     * @param mount
     * @param owner
     * @param creators
     * @param readers
     * @throws AccessDeniedException
     */
    public Repository createRepository(String mount, User owner, Group editors, Group creators, Group readers, Group guests)
            throws AccessDeniedException, DuplicateException;

    /**
     * @param mount
     * @param isPrimary
     */
    public void addRepositoryMount(String mount, boolean isPrimary) throws AccessDeniedException, NotFoundException;


    /**
     * @return the primary repository object
     */
    public Repository getPrimaryRepository();

    /**
     * Primary will be at index = 0
     * @return object array
     */
    public Repository [] getRepositories();


    /**
     * @return repository object
     */
    public Repository getRepository(String mount) throws NotFoundException;

    /**
     * Depending on implementation, this could have to scan multiple repositories
     * @return repository object
     */
    public Repository getRepositoryByNode(String nodeId) throws NotFoundException;

    /**
     * Cross repository Copy
     *
     * @param srcRepository
     * @param srcNodeId
     * @param dstRepository
     * @param dstUri
     * @param deep
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode copy(
        Repository srcRepository,
        String srcNodeId,
        Repository dstRepository,
        String dstUri,
        boolean deep)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;

    /**
     * Cross repository Move
     *
     * @param srcRepository
     * @param srcNodeId
     * @param dstRepository
     * @param dstUri
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
    public RepositoryNode move(
        Repository srcRepository,
        String srcNodeId,
        Repository dstRepository,
        String dstUri)
        throws
            AccessDeniedException,
            NotFoundException,
            DuplicateException,
            LockException;
    
    /**
     * 
     * @param node
     * @param content
     * @param type
     */
    public void fireContentChangeEvent(RepositoryNode node, NodeContent content, ContentEvent.Type type);
}
