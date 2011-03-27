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
public class Note extends ContainerElement {

    public final static String NAME = "note";
    public final static String [] ATTRIBUTES = {"style"};

    public Note(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Note.NAME;
    }

    public String [] getAttributeNames() {
        return Note.ATTRIBUTES;
    }

}
