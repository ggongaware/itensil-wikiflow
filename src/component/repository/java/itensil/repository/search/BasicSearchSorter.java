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
 * Created on Jan 29, 2004
 *
 */
package itensil.repository.search;

import itensil.util.UriHelper;
import itensil.repository.search.DefaultBasicSearchResultSet.Entry;

import javax.xml.namespace.QName;
import java.util.Comparator;


/**
 * @author ggongaware@itensil.com
 *
 */
public class BasicSearchSorter implements Comparator<Entry> {

    private SearchEntryCompare compares[];

    /**
     *
     */
    public BasicSearchSorter(BasicSearchOrderBy orderBys[]) {

        compares = new SearchEntryCompare[orderBys.length];
        for (int i=0; i < orderBys.length; i++) {
            BasicSearchOrderBy order = orderBys[i];
            if ( BasicSearchCompiler.
                QNAME_DISPLAYNAME.equals(order.getProperty())) {

                if (order.isDescending()) {
                    compares[i] = new DisplayNameDescending();
                } else {
                    compares[i] = new DisplayNameAscending();
                }
            } else {
                if (order.isDescending()) {
                    compares[i] = new PropertyDescending(order.getProperty());
                } else {
                    compares[i] = new PropertyAscending(order.getProperty());
                }
            }
        }
    }

    public boolean hasSort() {
        return compares.length > 0;
    }

    /*
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare(Entry e1, Entry e2) {
        for (SearchEntryCompare compare : compares) {
            int val = compare.compare(e1, e2);
            if (val != 0) {
                return val;
            }
        }
        return 0;
    }

    /**
     *
     */
    protected interface SearchEntryCompare {

        public int compare(Entry e1, Entry e2);
    }

    /**
     *
     */
    protected static class PropertyAscending implements SearchEntryCompare {

        private QName propName;

        protected PropertyAscending(QName propName) {
            this.propName = propName;
        }

        public int compare(Entry e1, Entry e2) {

            String v1 = e1.properties.getValue(propName);
            String v2 = e2.properties.getValue(propName);
            if (v1 == null) {
                return -1;
            }
            return v1.compareTo(v2);
        }

    }

    /**
     *
     */
    protected static class PropertyDescending implements SearchEntryCompare {

        private QName propName;

        protected PropertyDescending(QName propName) {
            this.propName = propName;
        }

        public int compare(Entry e1, Entry e2) {

            String v1 = e1.properties.getValue(propName);
            String v2 = e2.properties.getValue(propName);
            if (v2 == null) {
                return -1;
            }
            return v2.compareTo(v1);
        }

    }

    /**
     *
     */
    protected static class DisplayNameAscending implements SearchEntryCompare {

        protected DisplayNameAscending() {
        }

        public int compare(Entry e1, Entry e2) {

            String v1 = UriHelper.name(e1.node.getUri());
            String v2 = UriHelper.name(e2.node.getUri());
            if (v1 == null) {
                return -1;
            }
            return v1.compareTo(v2);
        }

    }

    /**
     *
     */
    protected static class DisplayNameDescending implements SearchEntryCompare {

        protected DisplayNameDescending() {
        }

        public int compare(Entry e1, Entry e2) {

            String v1 = UriHelper.name(e1.node.getUri());
            String v2 = UriHelper.name(e2.node.getUri());
            if (v2 == null) {
                return -1;
            }
            return v2.compareTo(v1);
        }

    }
}
