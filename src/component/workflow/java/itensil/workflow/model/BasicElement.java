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

import java.util.HashMap;

/**
 * @author ggongaware@itensil.com
 *
 */
public abstract class BasicElement {

    protected FlowModel owner = null;
    protected BasicElement parent = null;
    protected String innerText;
    protected HashMap<String, String> attributes;

    public BasicElement(FlowModel owner, ContainerElement parent) {
        this.owner = owner;
        this.parent = parent;
        attributes = new HashMap<String, String>(getAttributeNames().length);
    }
    
    protected BasicElement() { }

    /**
     * Element name, primarily form XML mapping
     * @return the name of the Element type
     */
    public abstract String getElementName();


    /**
     * Returns the owner.
     * @return FlowModel
     */
    public FlowModel getOwner() {
        return owner;
    }

    /**
     * Returns the parent.
     * @return BasicElement
     */
    public BasicElement getParent() {
        return parent;
    }

    /**
     * Sets the parent element.
     * @param parent The parent to set
     */
    public void setParent(ContainerElement parent) {
        this.parent = parent;
    }


    public String getInnerText() {
        return innerText;
    }

    public void setInnerText(String innerText) {
        this.innerText = innerText;
    }


    public abstract String [] getAttributeNames();

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public String toString() {
        return getElementName();
    }
    
    public void validate(ValidationLogger vlogger) {
    	// default valid
    } 
    
}
