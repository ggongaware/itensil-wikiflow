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

import itensil.security.Group;
import itensil.security.User;

/**
 * @author ggongaware@itensil.com
 */
public interface RepositoryNode {

    /**
     * @return unqiue id of node
     */
    public String getNodeId();

	/**
     * @return the uri path of node
     */
    public String getUri();

	/**
     * @return true if folder/collection
     */
    public boolean isCollection();

    /**
     * @return the user who owns this (probably creator)
     */
    public User getOwner();
    
    
    /**
     * 
     * @return
     */
    public Group getContextGroup();

    /**
     * @return the unique id of parentnode
     */
    public String getParentNodeId();

}
