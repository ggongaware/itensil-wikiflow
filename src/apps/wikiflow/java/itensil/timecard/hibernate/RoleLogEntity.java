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

import java.util.HashSet;
import java.util.Set;

public class RoleLogEntity {
	
	/* unique db id */
	long id;
	
	/* For contract */
	ContractEntity contract;
	
	boolean billable;
	
	/* Project / Description (many for each contract) - e.g. November Newsletter or PHI1107-NL-001 */
	String project;

	/* Which contract role */
	String role;
	
	/* Who */
	String userId;
	
	/* Hours-by-date */
	Set<TimeLogEntity> timeLogs = new HashSet<TimeLogEntity>();
	
	/**
	 * Ctor
	 *
	 */
	public RoleLogEntity() {
		
	}

	public boolean isBillable() {
		return billable;
	}

	public void setBillable(boolean billable) {
		this.billable = billable;
	}

	public ContractEntity getContract() {
		return contract;
	}

	public void setContract(ContractEntity contract) {
		this.contract = contract;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Set<TimeLogEntity> getTimeLogs() {
		return timeLogs;
	}

	public void setTimeLogs(Set<TimeLogEntity> timeLogs) {
		this.timeLogs = timeLogs;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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
		final RoleLogEntity other = (RoleLogEntity) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
}
