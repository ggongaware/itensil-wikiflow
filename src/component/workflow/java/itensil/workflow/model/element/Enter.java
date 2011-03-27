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
 * Enter a sub-screen or group
 * 
 * @author grantg
 *
 */
public class Enter extends Junction {

    public final static String NAME = "enter";
    public final static String [] ATTRIBUTES = {"style", "apptype", "rev"};

    public Enter(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Enter.NAME;
    }

    public String [] getAttributeNames() {
        return Enter.ATTRIBUTES;
    }

}
