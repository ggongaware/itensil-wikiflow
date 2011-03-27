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
package itensil.workflow.activities.timer;

import java.util.Date;

import itensil.workflow.activities.state.Activity;

public class ActivityTimer {

	String id;
	String timerId;
	Activity activity;	
	Date atTime;
	boolean conditional;
	Date checkTime;
	
	public ActivityTimer() {
		conditional = false;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Activity getActivity() {
		return activity;
	}
	
	public void setActivity(Activity activity) {
		this.activity = activity;
	}
	
	public Date getAtTime() {
		return atTime;
	}
	
	public void setAtTime(Date atTime) {
		this.atTime = atTime;
	}
	
	public Date getCheckTime() {
		return checkTime;
	}
	
	public void setCheckTime(Date checkTime) {
		this.checkTime = checkTime;
	}
	
	public boolean isConditional() {
		return conditional;
	}
	
	public void setConditional(boolean conditional) {
		this.conditional = conditional;
	}
	
	public String getTimerId() {
		return timerId;
	}
	
	public void setTimerId(String timerId) {
		this.timerId = timerId;
	}
	
}
