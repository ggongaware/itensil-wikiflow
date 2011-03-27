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
package itensil.workflow.activities.state;

import itensil.workflow.state.StepLog;
import itensil.workflow.state.StepState;
import itensil.workflow.state.SubState;
import itensil.workflow.activities.state.FlowState;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class FlowStepLog extends StepLog<Activity> implements Serializable {

    protected long id;
    private FlowState flow;
    private String userId;

    public FlowStepLog() { }

    public FlowStepLog(FlowState flow) {
        this.flow = flow;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    
	public void fromState(StepState<Activity> state) {
		if (state instanceof ActivityStepState) {
			setUserId(((ActivityStepState)state).getAssignId());
		}
		super.fromState(state);
	}

	public int hashCode() {
        return (int)id;
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return getId() == ((FlowStepLog)o).getId();
    }

    public int getSubStateInt() {
        return subState.ordinal();
    }

    public void setSubStateInt(int subState) {
        this.subState = SubState.values()[subState];
    }

    public FlowState getFlow() {
        return flow;
    }

    public void setFlow(FlowState flow) {
        this.flow = flow;
    }

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
    
    
}
