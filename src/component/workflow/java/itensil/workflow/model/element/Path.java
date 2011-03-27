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

import itensil.util.Check;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.ValidationLogger;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Path extends ContainerElement {

    public final static String NAME = "path";
    public final static String [] ATTRIBUTES = {"to", "startDir", "endDir", "points", "rev"};

    protected transient Step toStep;

    public Path(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return NAME;
    }

    public String [] getAttributeNames() {
        return ATTRIBUTES;
    }
    
    public String getTo() {
    	return getAttribute("to");
    }

    public Step getToStep() {
        if (toStep == null) {
            toStep = owner.getStep(getTo());
        }
        return toStep;
    }

    public Step getFromStep() {
        return (Step)getParent();
    }
    
	public void validate(ValidationLogger vlogger) {
		
		// check id
		if (Check.isEmpty(getId())) {
			vlogger.error(this, "model", "Missing ID");
		}
		
		if (getParent() instanceof Switch) { // check switch condition
			if (selectOneChild(Condition.class) == null) {
				vlogger.error(this, "model", "Switch path missing condition");
			}
		} else if (!(getParent() instanceof Step)) { // check orphaned
			vlogger.warn(this, "flow", "Orphaned Path");
		}
		
		// check to
		if (Check.isEmpty(getTo())) {
			vlogger.error(this, "model", "Empty to step");
		} else if (getToStep() == null) {
			vlogger.error(this, "model", "Missing to step");
		}
		
		super.validate(vlogger);
	}
}
