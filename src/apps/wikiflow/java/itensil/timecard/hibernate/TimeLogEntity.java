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
package itensil.timecard.hibernate;

import java.util.Date;

public class TimeLogEntity {
	
	/* unique db id */
	long id;
	
	/* For Role/Project */
	RoleLogEntity roleLog;
	
	/* Day resolution */
	Date logDate;
	
	/* track hours */
	float hours;
	
	/* optional activityId */
	String activityId;
	
	/* Approved by */
	String appUserId;
	
	/* Approval date */
	Date appDate;
	
	/**
	 * Ctor
	 *
	 */
	public TimeLogEntity() {
		
	}

	public String getActivityId() {
		return activityId;
	}

	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}

	public Date getAppDate() {
		return appDate;
	}

	public void setAppDate(Date appDate) {
		this.appDate = appDate;
	}

	public String getAppUserId() {
		return appUserId;
	}

	public void setAppUserId(String appUserId) {
		this.appUserId = appUserId;
	}

	public float getHours() {
		return hours;
	}

	public void setHours(float hours) {
		this.hours = hours;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getLogDate() {
		return logDate;
	}

	public void setLogDate(Date logDate) {
		this.logDate = logDate;
	}

	public RoleLogEntity getRoleLog() {
		return roleLog;
	}

	public void setRoleLog(RoleLogEntity roleLog) {
		this.roleLog = roleLog;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TimeLogEntity other = (TimeLogEntity) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
}
