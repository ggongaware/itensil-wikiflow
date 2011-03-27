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
 * Created on Aug 27, 2003
 *
 */
package itensil.repository;

import java.util.Map;
import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 */
public interface NodeProperties {

    /**
     * @return currently named properties
     */
    public QName[] getNames();

    /**
     * @param name
     * @return property value
     */
    public String getValue(QName name);

    /**
     * Get using the default qname space
     * @param localName
     * @return property value
     */
    public String getValue(String localName);

    /**
     * Set a value overwrites existing, or adds new property
     * @param name
     * @param value
     */
    public void setValue(QName name, String value);

    /**
     * Set using the default qname space
     * @param localName
     * @param value
     */
    public void setValue(String localName, String value);

    /**
     * remove a property and its value
     * @param name
     */
    public void remove(QName name);


    /**
     * remove a property and its value
     * using the default qname space
     * @param localName
     */
    public void remove(String localName);

    /**
     * @return version related to these properties
     */
    public NodeVersion getVersion();


    /**
     * @return a map of all properties
     */
    public Map<QName, String> getPropertyMap();


}
