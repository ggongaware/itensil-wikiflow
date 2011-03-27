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
package itensil.workflow.rules;

import itensil.workflow.FlowEvent;
import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Path;
import itensil.workflow.state.StateException;
import itensil.workflow.state.StateStore;
import itensil.workflow.state.Token;

/**
 * Traffic director for single state tokens
 * 
 * @author grantg
 *
 */
public class DefaultTrafficDirector<Tk extends Token, Dt> implements TrafficDirector<Tk, Dt> {

	public Path[] travelPaths(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		// return first path
		for (Path pth : step.getPaths()) {
			return new Path[]{pth};
		}
		return new Path[0];
	}

	public boolean waitRequired(Step step, FlowEvent<Tk, Dt> evt, StateStore<Tk> states) throws StateException {
		return false; // never wait in single state mode
	}

}
