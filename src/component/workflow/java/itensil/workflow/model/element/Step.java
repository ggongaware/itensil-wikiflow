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
import itensil.workflow.model.BasicElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.ValidationLogger;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author ggongaware@itensil.com
 *
 */
public abstract class Step extends ContainerElement {

	public static final String VALIDATE_NO_PARALLEL = "nopara";
	
    protected ArrayList<Path> paths;

    public Step(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
        paths = new ArrayList<Path>();
    }

    public Collection<Path> getPaths() {
         return paths;
    }

    public void addChild(BasicElement child) {
        if (child instanceof Path) {
            paths.add((Path)child);
        }
        super.addChild(child);
    }

    public int pathCount() {
        return paths.size();
    }

    public Path getPath(String id) {
        for (Path pth : getPaths()) {
            if (id.equals(pth.getId())) {
                return pth;
            }
        }
        return null;
    }

	public void validate(ValidationLogger vlogger) {
		
		// check id
		if (Check.isEmpty(getId())) {
			vlogger.error(this, "model", "Missing ID");
		} else {
			
			if (getOwner().getStep(getId()) != this) {
				vlogger.error(this, "model", "Duplicate ID");
			}
			
			// check orphaned
			if (!(this instanceof Start) && getOwner().getFromPaths(getId()).isEmpty()) {
				vlogger.warn(this, "flow", "Orphaned Step");
			}
			
			// parallel check
			if (!(this instanceof Switch) 
					&& vlogger.check(Step.VALIDATE_NO_PARALLEL)
					&& pathCount() > 1) {
				vlogger.error(this, "flow", "Parallel Paths");
			}
			
		}
		super.validate(vlogger);
	}
}