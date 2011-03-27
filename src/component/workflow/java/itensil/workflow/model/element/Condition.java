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
package itensil.workflow.model.element;

import itensil.workflow.model.BasicElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Condition extends BasicElement {

    public final static String NAME = "condition";
    public final static String [] ATTRIBUTES = {};

    public Condition(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return NAME;
    }

    public String [] getAttributeNames() {
        return ATTRIBUTES;
    }

    public String getReturnId() {
    	if (parent instanceof Path) {
    		return ((Path)parent).getId();
    	}
        return "condition";
    }
}
