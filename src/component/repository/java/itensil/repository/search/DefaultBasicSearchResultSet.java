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

import java.io.Serializable;
import java.util.List;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultBasicSearchResultSet
    implements BasicSearchResultSet, Serializable {

    static final long serialVersionUID = 1079554220539L;

    private int index;
    private List<Entry> entryList;

    public DefaultBasicSearchResultSet(List<Entry> entryList) {
        this.entryList = entryList;
        index = -1;
    }


    /*
     * @see itensil.repository.search.BasicSearchResultSet#next()
     */
    public boolean next() {
        ++index;
        return entryList.size() > index;
    }

    /*
     * @see itensil.repository.search.BasicSearchResultSet#getNode()
     */
    public RepositoryNode getNode() {
        return ((Entry)entryList.get(index)).node;
    }

    /*
     * @see itensil.repository.search.BasicSearchResultSet#getProperties()
     */
    public NodeProperties getProperties() {
        return entryList.get(index).properties;
    }

    /**
     *
     * @param resultSet
     */
    public void addResults(BasicSearchResultSet resultSet) {

        // Add all
        while (resultSet.next()) {
            entryList.add(
                new Entry(resultSet.getNode(), resultSet.getProperties()));
        }
    }

    /**
     * start back at the beginning
     */
    public void reset() {
        index = -1;
    }

    /**
     * For prune and sort access
     * @return list
     */
    public List<Entry> getEntryList() {
        return entryList;
    }

    public void setEntryList(List<Entry> entryList) {
        this.entryList = entryList;
    }

    public static class Entry implements Serializable {

        public RepositoryNode node;
        public NodeProperties properties;

        public Entry(RepositoryNode node, NodeProperties properties) {
            this.node = node;
            this.properties = properties;
        }

    }

}
