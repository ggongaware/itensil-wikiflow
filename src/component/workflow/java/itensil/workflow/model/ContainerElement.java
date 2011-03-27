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

/**
 * @author ggongaware@itensil.com
 *
 */
public abstract class ContainerElement extends BasicElement {

    protected ArrayList<BasicElement> children;
    protected String id;

    public ContainerElement(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
        children = new ArrayList<BasicElement>();
    }

    public void addChild(BasicElement child) {
        children.add(child);
    }

    public Collection<BasicElement> getChildren() {
        return children;
    }
    
    public <T extends BasicElement> Collection<T> selectChildren(Class<T> klass) {
    	ArrayList<T> select = new ArrayList<T>();
    	for (BasicElement kid : getChildren()) {
    		if (klass.isInstance(kid)) {
    			select.add(klass.cast(kid));
    		}
    	}
    	return select;
    }
    
    public <T extends BasicElement> T selectOneChild(Class<T> klass) {
    	for (BasicElement kid : getChildren()) {
    		if (klass.isInstance(kid)) return klass.cast(kid);
    	}
    	return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void validate(ValidationLogger vlogger) {
    	// default validate kids
    	for (BasicElement kid : getChildren()) {
    		kid.validate(vlogger);
    	}
    } 
}
