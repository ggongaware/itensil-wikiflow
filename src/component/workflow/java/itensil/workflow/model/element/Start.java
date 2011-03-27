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
import itensil.workflow.model.ValidationLogger;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Start extends Step {

    public final static String NAME = "start";
    public final static String [] ATTRIBUTES = {"style", "apptype", "rev", "glyph"};

    public Start(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Start.NAME;
    }

    public String [] getAttributeNames() {
        return Start.ATTRIBUTES;
    }
    
	public void validate(ValidationLogger vlogger) {
		
		// validate start is start
		if (!getOwner().getFromPaths(getId()).isEmpty()) {
			vlogger.warn(this, "flow", "Paths into a start");
		}
		super.validate(vlogger);
	}

}
