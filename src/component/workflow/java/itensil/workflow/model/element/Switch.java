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
import itensil.workflow.model.ValidationLogger;

import java.util.ArrayList;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Switch extends Step {

    public final static String NAME = "switch";
    public final static String [] ATTRIBUTES = {"style", "mode", "apptype", "rev", "glyph"};

    protected transient Condition [] conditions;
    protected transient double [] allocations;

    public Switch(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return NAME;
    }

    public String [] getAttributeNames() {
        return ATTRIBUTES;
    }

    public Condition[] getConditions() {
        if (conditions == null) {
            ArrayList<Condition> conds = new ArrayList<Condition>(pathCount());
            for (Path pth : getPaths()) {
                for (BasicElement kid : pth.getChildren()) {
                    if (kid instanceof Condition) {
                        conds.add((Condition)kid);
                        break;
                    }
                }
            }
            conditions = conds.toArray(new Condition[conds.size()]);
        }
        return conditions;
    }

    public double [] getAllocs() {
        if (allocations == null) {
            Condition conds[] = getConditions();
            // Weights are between 0.0-1.0, totalling 1.0
            allocations = new double[conds.length];
            for (int ii = 0; ii < conds.length; ii++) {
                allocations[ii] = Double.parseDouble(conds[ii].getInnerText());
            }
        }
        return allocations;
    }

    public String getMode() {
        return getAttribute("mode");
    }
    
    public void validate(ValidationLogger vlogger) {
    	
    	// check mode
    	if (!"XOR".equalsIgnoreCase(getMode()) && !"ALLOC".equalsIgnoreCase(getMode())) {
    		vlogger.error(this, "model", "Invalid switch mode: " + getMode());
    	}
    	
    	// check out count
    	if (getPaths().size() < 2) {
    		vlogger.warn(this, "flow", "Switch has no effect");
    	}
    	
    	// check alloc weights
    	if ("ALLOC".equalsIgnoreCase(getMode())) {
    		double anums[];
    		try {
    			anums = getAllocs();
    			double dt = 0.0;
    			for (double dd : anums) {
    				dt += dd;
    			}
    			if (dt != 1.0) {
    				vlogger.warn(this, "flow", "Allocation weights do not total 100%");
    			}
    		} catch (NumberFormatException nfe) {
    			vlogger.error(this, "model", "Invalid allocation weight");
    		}
    	}
    	
    	super.validate(vlogger);
    }
}
