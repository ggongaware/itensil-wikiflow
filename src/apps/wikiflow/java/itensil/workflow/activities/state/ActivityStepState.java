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

import itensil.workflow.state.StepState;
import itensil.workflow.state.SubState;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityStepState extends StepState<Activity> implements Serializable {
	
	private String id;
    private String assignId;
    private ActivityCurrentPlan currentPlan;
    private boolean flowDirty;
    private int userStatus;
    private int progress;
    private String subActivityId;
    
    public ActivityStepState() {
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSubStateInt() {
        return subState.ordinal();
    }

    public void setSubStateInt(int subState) {
        this.subState = SubState.values()[subState];
    }
    
    
    public Activity getActivity() {
        return getToken();
    }

    public void setActivity(Activity activity) {
    	setToken(activity);
    }
    
    public String getAssignId() {
        return assignId;
    }

    public void setAssignId(String val) {
        this.assignId = val;
    }
    
    public ActivityCurrentPlan getCurrentPlan() {
        return currentPlan;
    }

    public void setCurrentPlan(ActivityCurrentPlan currentPlan) {
        this.currentPlan = currentPlan;
    }

	public boolean isFlowDirty() {
		return flowDirty;
	}

	public void setFlowDirty(boolean flowDirty) {
		this.flowDirty = flowDirty;
	}
	

	public int getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(int userStatus) {
		this.userStatus = userStatus;
	}
	
	
	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getSubActivityId() {
		return subActivityId;
	}

	public void setSubActivityId(String subActivityId) {
		this.subActivityId = subActivityId;
	}


	public int hashCode() {
		final int PRIME = 31;
		int result = PRIME * ((id == null) ? 0 : id.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final ActivityStepState other = (ActivityStepState) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
