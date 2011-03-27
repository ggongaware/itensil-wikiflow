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
 * Created on Dec 8, 2003
 *
 */
package itensil.repository;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultNodeProperties implements NodeProperties, Serializable {


    private Map<QName, String> properties;
    private NodeVersion version;

    /**
     *
     * @param version
     * @param properties
     */

    public DefaultNodeProperties(NodeVersion version, Map<QName, String> properties) {
        this.properties = properties;
        this.version = version;
    }

    /**
     *
     * @param version
     */
    public DefaultNodeProperties(NodeVersion version) {
        this(version, new HashMap<QName, String>());
    }

    /*
     * @see itensil.repository.NodeProperties#getNames()
     */
    public QName[] getNames() {
        Set<QName> keySet = properties.keySet();
        return keySet.toArray(new QName[keySet.size()]);
    }

    /*
     * @see itensil.repository.NodeProperties#getValue(org.apache.xml.utils.QName)
     */
    public String getValue(QName name) {
        return properties.get(name);
    }

    /*
     * @see itensil.repository.NodeProperties#setValue(org.apache.xml.utils.QName, java.lang.String)
     */
    public void setValue(QName name, String value) {
        properties.put(name, value);
    }

    /*
     * @see itensil.repository.NodeProperties#remove(org.apache.xml.utils.QName)
     */
    public void remove(QName name) {
        properties.remove(name);
    }

    /*
     * @see itensil.repository.NodeProperties#getVersion()
     */
    public NodeVersion getVersion() {
        return version;
    }

    /*
     * @see itensil.repository.NodeProperties#getValue(java.lang.String)
     */
    public String getValue(String localName) {
        return getValue(PropertyHelper.defaultQName(localName));
    }

    /*
     * @see itensil.repository.NodeProperties#setValue(java.lang.String, java.lang.String)
     */
    public void setValue(String localName, String value) {
        setValue(PropertyHelper.defaultQName(localName), value);
    }

    /*
     * @see itensil.repository.NodeProperties#remove(java.lang.String)
     */
    public void remove(String localName) {
        remove(PropertyHelper.defaultQName(localName));
    }

    /*
     * @see itensil.repository.NodeProperties#getPropertyMap()
     */
    public Map<QName, String> getPropertyMap() {
        return properties;
    }



}
