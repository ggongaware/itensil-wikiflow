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
public interface BasicSearchOrderBy {


    /**
     *
     * @return The property to sort on
     */
    public QName getProperty();

    /**
     * Is the sort order descending?
     * @return false if ascending
     */
    public boolean isDescending();

}
