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
public class End extends Step {

    public final static String NAME = "end";
    public final static String [] ATTRIBUTES = {"style", "apptype", "rev"};

    public End(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return End.NAME;
    }

    public String [] getAttributeNames() {
        return End.ATTRIBUTES;
    }

	public void validate(ValidationLogger vlogger) {
		
		// validate end is end
		if (!getPaths().isEmpty()) {
			vlogger.warn(this, "flow", "Paths out of an end");
		}
		
		super.validate(vlogger);
	}
    
}
