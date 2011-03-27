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

import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Group extends ContainerElement {

    public final static String NAME = "group";
    public final static String [] ATTRIBUTES = {"style", "rev", "glyph"};

    public Group(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Group.NAME;
    }

    public String [] getAttributeNames() {
        return Group.ATTRIBUTES;
    }

}
