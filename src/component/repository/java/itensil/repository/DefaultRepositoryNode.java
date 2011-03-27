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

import itensil.security.Group;
import itensil.security.User;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultRepositoryNode implements RepositoryNode, Serializable {


    private String nodeId;
    protected String parentNodeId;
    protected String uri;
    private User owner;
    private Group contextGroup;
    private boolean isCollection;

    /**
     *
     * @param nodeId
     * @param parentNodeId
     * @param uri
     * @param isCollection
     * @param owner
     */
    public DefaultRepositoryNode(
            String nodeId,
            String parentNodeId,
            String uri,
            boolean isCollection,
            User owner,
            Group contextGroup) {

        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        this.uri = uri;
        this.isCollection = isCollection;
        this.owner = owner;
        this.contextGroup = contextGroup;
    }

    /**
     * Make a Clone
     * @param node
     */
    public DefaultRepositoryNode(RepositoryNode node) {

        this.nodeId = node.getNodeId();
        this.parentNodeId = node.getParentNodeId();
        this.uri = node.getUri();
        this.isCollection = node.isCollection();
        this.owner = node.getOwner();
        this.contextGroup = node.getContextGroup();
    }

   /**
     * @see itensil.repository.RepositoryNode#isCollection()
     */
    public boolean isCollection() {
        return isCollection;
    }

    /**
     * @see itensil.repository.RepositoryNode#getOwner()
     */
    public User getOwner() {
        return owner;
    }
    
    public Group getContextGroup() {
    	return contextGroup;
    }

    /**
     * @see itensil.repository.RepositoryNode#getNodeId()
     */
    public String getNodeId() {
        return nodeId;
    }


    /**
     * @see itensil.repository.RepositoryNode#getUri()
     */
    public String getUri() {
        return uri;
    }

    /**
     * @see itensil.repository.RepositoryNode#getParentNodeId()
     */
    public String getParentNodeId()  {
        return parentNodeId;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof RepositoryNode) {
            return getNodeId().equals(((RepositoryNode)obj).getNodeId());
        }
        return false;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getNodeId().hashCode();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "[" +  getNodeId() + "] " + getUri();
    }

}
