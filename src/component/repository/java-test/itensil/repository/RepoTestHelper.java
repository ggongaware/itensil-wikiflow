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

import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.util.UriHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ggongaware@itensil.com
 *
 */
public class RepoTestHelper {

    public static Repository initRepository(String mount, User user)
            throws AccessDeniedException, DuplicateException, NotFoundException {

        RepositoryManager repoMan = RepositoryManagerFactory.getManager(user);
        Repository repo;
        try {
            repo = repoMan.getRepository(mount);
        } catch (NotFoundException e) {
            repo = repoMan.createRepository(mount, user, null, null, null, null);
            repoMan.addRepositoryMount(mount, true);
        }
        SecurityAssociation.setUser(user);
        return repo;
    }

    /**
     * Load or replace a file into the repository
     *
     * @param file
     * @param foldUri
     * @param contentType
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     * @throws LockException
     * @throws DuplicateException
     * @throws IOException
     */
    public static RepositoryNode loadFile(File file, String foldUri, String contentType)
            throws NotFoundException, AccessDeniedException, LockException, DuplicateException, IOException {
        return RepoTestHelper.loadFile(new FileInputStream(file),file.getName(), (int)file.length(), foldUri, contentType);
    }
    
    
    public static RepositoryNode loadFile(InputStream in, String filename, int size, String foldUri, String contentType)
    	throws NotFoundException, AccessDeniedException, LockException, DuplicateException, IOException {
    	
    	Repository repo = RepositoryHelper.getRepository(foldUri);
        String dstUri = UriHelper.absoluteUri(foldUri, filename);
        try {
            repo.getNodeByUri(foldUri, false);
            try {
                RepositoryNode node = repo.getNodeByUri(dstUri, true);
                repo.removeNode(node.getNodeId());
            } catch (NotFoundException nfe) {
                // eat it
            }
        } catch (NotFoundException nfe) {
            repo.createNode(foldUri, true, SecurityAssociation.getUser());
        }
        MutableRepositoryNode dstNode = repo.createNode(dstUri, false, SecurityAssociation.getUser());
        RepositoryHelper.createContent(dstNode, in, size, contentType);
        return dstNode;
    }

}
