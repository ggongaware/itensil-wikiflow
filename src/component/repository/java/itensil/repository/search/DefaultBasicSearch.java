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

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultBasicSearch
    implements BasicSearch, Serializable, Cloneable {

    static final long serialVersionUID = 1079554220539L;

    private String uri;
    private int depth;
    private int limit;
    private boolean includeVersions;
    private List<QName> selects;
    BasicSearchClause clause;
    private List<BasicSearchOrderBy> orderBys;

    /**
     * @param uri
     * @param depth
     * @param includeVersions
     */
    public DefaultBasicSearch(String uri, int depth, boolean includeVersions) {

        this.uri = uri;
        this.depth = depth;
        this.includeVersions = includeVersions;
        selects = new ArrayList<QName>();
        orderBys = new ArrayList<BasicSearchOrderBy>();
        limit = -1;
    }


    /*
     * @see BasicSearch#getScopeUri()
     */
    public String getScopeUri() {
        return uri;
    }

    /*
     * @see BasicSearch#setScopeUri(String)
     */
    public void setScopeUri(String uri) {
        this.uri = uri;
    }

    /*
     * @see BasicSearch#getScopeDepth()
     */
    public int getScopeDepth() {
        return depth;
    }

    /*
     * @see BasicSearch#getScopeIncludeVersions()
     */
    public boolean getScopeIncludeVersions() {
        return includeVersions;
    }

    /*
     * @see itensil.repository.search.BasicSearch#getSelectProperties()
     */
    public QName[] getSelectProperties() {
        if (selects == null) {
            return null;
        }
        return selects.toArray(new QName[selects.size()]);
    }

    /**
     * @param property
     */
    public void addSelectProperty(QName property) {
        if (selects != null) {
            selects.add(property);
        }
    }

    /**
     * ResultSet will have all declare properties
     */
    public void setSelectAllProperties() {
        selects = null;
    }

    /*
     * @see itensil.repository.search.BasicSearch#getWhereClauses()
     */
    public BasicSearchClause getWhereClause() {
        return clause;
    }

    /**
     *
     * @param clause
     */
    public void setWhereClause(BasicSearchClause clause) {
        this.clause = clause;
    }

    /*
     * @see itensil.repository.search.BasicSearch#getOrderBys()
     */
    public BasicSearchOrderBy[] getOrderBys() {
        return orderBys.toArray(new BasicSearchOrderBy[orderBys.size()]);
    }

    /**
     * @param orderBy
     */
    public void addOrderBy(BasicSearchOrderBy orderBy) {
        orderBys.add(orderBy);
    }

    /**
     * -1 for no limit
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /*
     * @see itensil.repository.search.BasicSearch#getLimit()
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param i
     */
    public void setScopeDepth(int i) {
        this.depth = i;
    }

    /*
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        DefaultBasicSearch search =
            new DefaultBasicSearch(uri, depth, includeVersions);
        search.clause = clause;
        search.limit = limit;
        search.orderBys =  new ArrayList<BasicSearchOrderBy>(orderBys);
        if (selects != null) {
            search.selects = new ArrayList<QName>(selects);
        } else {
            search.selects = null;
        }
        return search;
    }

}
