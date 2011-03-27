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

import java.io.Serializable;
import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityCurrentPlan implements Serializable {

	private ActivityStepState state;
    private int priority;
    private Date startDate;
    private Date dueDate;
    private int duration;

    public ActivityCurrentPlan() {
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date val) {
        this.dueDate = val;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public ActivityStepState getState() {
		return state;
	}

	public void setState(ActivityStepState state) {
		this.state = state;
	}

	public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
