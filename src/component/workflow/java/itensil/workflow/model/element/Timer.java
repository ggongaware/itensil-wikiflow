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
public class Timer extends Step {

    public final static String NAME = "timer";
    public final static String [] ATTRIBUTES = {"mode", "style", "apptype", "rev", "glyph"};

    public Timer(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Timer.NAME;
    }

    public String [] getAttributeNames() {
        return Timer.ATTRIBUTES;
    }
    
    public void validate(ValidationLogger vlogger) {
    	
    	// validate modes
    	String mode = getAttribute("mode");
    	if ("wait".equals(mode)) {
    		if (selectOneChild(Wait.class) == null) {
    			vlogger.error(this, "model", "Wait timer missing definition");
    		}
    	} else if ("until".equals(mode)) {
    		if (selectOneChild(Until.class) == null) {
    			vlogger.error(this, "model", "Until timer missing definition");
    		}
    	} else {
    		vlogger.error(this, "model", "Invalid timer mode '" + mode + "'");
    	}
    	super.validate(vlogger);
    }
}
