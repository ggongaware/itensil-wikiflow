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

import java.util.Date;
import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityPlan implements Serializable {

    private String id;
    private String stepId;
    private int priority;
    private Date startDate;
    private Date dueDate;
    private String assignId;
    private Activity activity;
    private boolean skip;
    private int duration;

    public ActivityPlan() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String val) {
        this.stepId = val;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date val) {
        this.dueDate = val;
    }

    public String getAssignId() {
        return assignId;
    }

    public void setAssignId(String val) {
        this.assignId = val;
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ActivityPlan activityPlan = (ActivityPlan) o;

        return id.equals(activityPlan.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
