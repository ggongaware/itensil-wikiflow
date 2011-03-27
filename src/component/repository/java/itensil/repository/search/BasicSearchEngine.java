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
 * Created on Jan 21, 2004
 *
 */
package itensil.repository.search;

import itensil.repository.NodeProperties;
import itensil.repository.RepositoryNode;
import itensil.repository.search.DefaultBasicSearchResultSet.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ggongaware@itensil.com
 *
 */
public class BasicSearchEngine {

    BasicSearchCompiler compiled;
    BasicSearchSorter sorter;

    /**
     * @param search
     * @throws SearchException
     */
    public BasicSearchEngine(BasicSearch search)
        throws SearchException {
        
        compiled = new BasicSearchCompiler(search.getWhereClause());
        sorter = new BasicSearchSorter(search.getOrderBys());
    }

    /**
     * @param initialSet
     * @return
     * @throws SearchException
     */
    public DefaultBasicSearchResultSet execute(BasicSearchResultSet initialSet)
        throws SearchException {

        ArrayList<Entry> entries = new ArrayList<Entry>();
        while (initialSet.next()) {
            RepositoryNode node = initialSet.getNode();
            NodeProperties props = initialSet.getProperties();
            if (compiled.test(node, props)) {
                entries.add(new Entry(node, props));
            }
        }
        return new DefaultBasicSearchResultSet(entries);
    }

    /**
     * @param entries List of DefaultBasicSearchResultSet.Entry
     */
    public void sort(List<Entry> entries) {

        if (sorter.hasSort()) {
            Collections.sort(entries, sorter);
        }
    }

}
