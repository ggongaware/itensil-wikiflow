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
 * Exit a sub-screen or group
 * 
 * @author grantg
 *
 */
public class Exit extends Junction {

    public final static String NAME = "exit";
    public final static String [] ATTRIBUTES = {"style", "apptype", "rev"};

    public Exit(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Exit.NAME;
    }

    public String [] getAttributeNames() {
        return Exit.ATTRIBUTES;
    }

}
