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

/**
 * @author ggongaware@itensil.com
 *
 */
public interface BasicSearch {

    public static final int INFINITE_DEPTH = -1;

    /**
     * What collections to search under
     * @return uri of search root
     */
    public String getScopeUri();


    /**
     * What collections to search under
     * @param uri
     */
    public void setScopeUri(String uri);

    /**
     * -1 = infinity
     * 1+ = includes some children
     * 0 = only the ScopeURI
     * @return depth count
     */
    public int getScopeDepth();

    /**
     * If true the search scope will include all versions,
     * other wise just the default version
     *
     * @return true to search inactive versions
     */
    public boolean getScopeIncludeVersions();

    /**
     * @return array length = 0 for empty and null for "all"
     */
    public QName[] getSelectProperties();


    /**
     * @return array length = 0 for empty
     */
    public BasicSearchClause getWhereClause();


    /**
     * @return array length = 0 for empty
     */
    public BasicSearchOrderBy[] getOrderBys();


    /**
     * -1 = infinity
     * @return the result set limit
     */
    public int getLimit();

}
