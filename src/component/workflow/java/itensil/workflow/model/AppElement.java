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
package itensil.workflow.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * External NameSpace Element
 *
 * @author ggongaware@itensil.com
 *
 *
 * Warning: doesn't support mixed content elements, nor external attribute namespaces (xml:xxx ok)
 */
public class AppElement extends BasicElement {

    protected String sName;
    protected String namespaceURI;
    protected ArrayList<AppElement> children;

    public AppElement() {
        attributes = new HashMap<String, String>();
        children = new ArrayList<AppElement>();
    }
    
    public void init(String sName, String namespaceURI, FlowModel owner, BasicElement parent) {
    	this.owner = owner;
        this.parent = parent;
    	this.sName = sName;
        this.namespaceURI = namespaceURI;
    }

    public String getElementName() {
        return sName;
    }

    public String [] getAttributeNames() {
        return new String[0];
    }

    public String getNamespaceURI() {
         return namespaceURI;
    }

     public void addChild(AppElement child) {
        children.add(child);
    }

    public Collection<AppElement> getChildren() {
        return children;
    }

    public Collection<AppElement> matchChildElements(String elementName) {
        ArrayList<AppElement> matches = new ArrayList<AppElement>();
        for (AppElement kid : getChildren()) {
            if (elementName.equals(kid.getElementName())) {
                matches.add(kid);
            }
        }
        return matches;
    }
    
    public <T extends AppElement> Collection<T> selectChildren(Class<T> klass) {
    	ArrayList<T> select = new ArrayList<T>();
    	for (AppElement kid : getChildren()) {
    		if (klass.isInstance(kid)) {
    			select.add(klass.cast(kid));
    		}
    	}
    	return select;
    }
    
    public <T extends AppElement> T selectOneChild(Class<T> klass) {
    	for (AppElement kid : getChildren()) {
    		if (klass.isInstance(kid)) return klass.cast(kid);
    	}
    	return null;
    }
    
    public void validate(ValidationLogger vlogger) {
    	// default validate kids
    	for (AppElement kid : getChildren()) {
    		kid.validate(vlogger);
    	}
    } 
}
