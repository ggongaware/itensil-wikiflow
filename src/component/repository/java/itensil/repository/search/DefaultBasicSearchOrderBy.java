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


/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultBasicSearchOrderBy
    implements BasicSearchOrderBy, Serializable {

    static final long serialVersionUID = 1079554220539L;

    private QName property;
    private boolean descending;

    /**
     * Defaults to ascending
     * @param property
     */
    public DefaultBasicSearchOrderBy(QName property) {
        this(property, false);
    }

    /**
     * @param property
     * @param descending
     */
    public DefaultBasicSearchOrderBy(QName property, boolean descending) {
        this.property = property;
        this.descending = descending;
    }

    /*
     * @see itensil.repository.search.BasicSearchOrderBy#getProperty()
     */
    public QName getProperty() {
        return property;
    }

    /*
     * @see itensil.repository.search.BasicSearchOrderBy#isDescending()
     */
    public boolean isDescending() {
        return descending;
    }

    /**
     * @param b
     */
    public void setDescending(boolean b) {
        descending = b;
    }

}
