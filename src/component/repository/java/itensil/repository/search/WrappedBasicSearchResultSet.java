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

import itensil.repository.*;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class WrappedBasicSearchResultSet
    implements BasicSearchResultSet, Serializable {

    static final long serialVersionUID = 1079554220539L;

    private BasicSearchResultSet resultSet;
    private Repository repository;
    private String mount;

    public WrappedBasicSearchResultSet(
        BasicSearchResultSet resultSet,
        Repository repository,
        String mount) {
            
        this.resultSet = resultSet;
        this.repository = repository;
        this.mount = mount;
    }

    /*
     * @see BasicSearchResultSet#next()
     */
    public boolean next() {
        return resultSet.next();
    }

    /*
     * @see BasicSearchResultSet#getNode()
     */
    public RepositoryNode getNode() {
        
        RepositoryNode node = resultSet.getNode();
        if (node instanceof MutableRepositoryNode) {
            return node;
        } else {
            return new DefaultMutableRepositoryNode(node, repository, mount);
        }
    }

    /*
     * @see BasicSearchResultSet#getProperties()
     */
    public NodeProperties getProperties() {
        return resultSet.getProperties();
    }

    /*
     * @see BasicSearchResultSet#addResults(BasicSearchResultSet)
     */
    public void addResults(BasicSearchResultSet resultSet) {
        this.resultSet.addResults(resultSet);
    }

    /**
     * start back at the beginning
     */
    public void reset() {
        resultSet.reset();
    }

}
