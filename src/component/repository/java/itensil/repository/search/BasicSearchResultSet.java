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
 * Created on Jan 20, 2004
 *
 */
package itensil.repository.search;

import itensil.repository.NodeProperties;
import itensil.repository.RepositoryNode;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface BasicSearchResultSet {

    /**
     * Get next matching node
     * @return true if there is a next, false if end reached
     */
    public boolean next();
    
    /*** 
     * @return Current matching node
     */
    public RepositoryNode getNode();
    
    /**
     * Properties will only include selected values
     * @return Current matching node properties
     */
    public NodeProperties getProperties();

    /**
     * 
     * @param resultSet
     */
    public void addResults(BasicSearchResultSet resultSet);

    /**
     * start back at the beginning
     */ 
    public void reset();

}
