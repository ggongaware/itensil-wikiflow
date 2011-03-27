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

import java.util.*;

public class ContractEntity {
	
	/* unique db id */
	long id;
	
	/* community partition id */
	String userSpaceId;
	
	/* Client Name                                                                               */
	String clientName;
	/* Contract Name (many for each Client) - e.g. Creative Challenge or ILM QuickStart Workshop */
	String name;
	/* Team (one for each contract) - e.g. East, West, Strategy, Tech, etc.                      */
	String team;
	/* Status one per contract - e.g. backlog, scheduled, in process, ongoing, complete          */
	String status;
	/* Billing/Rev-Rec Terms (one per contract)                                                  */
	String billTerms;
	/* Can we bill overages (Y/N/Notes) (one per contract)                                       */
	boolean billOverages;
	/* Total value  Project$$                                                                    */
	float moneyBudget;
	/* Total hours budgeted                                                                      */
	float hoursBudget;
	/* Client Manager (one for each contract)                                                    */
	String manager;
	
	Date openDate;
	
	Date closeDate;
	
	/* project list clob */
	String projects;
	
	/* Roles (many for each contract) - e.g. Project Management, Campaign Execution,  etc.       */
	Set<ContractRoleEntity> roles = new HashSet<ContractRoleEntity>();

	/**
	 * Ctor
	 *
	 */
	public ContractEntity() {
		
	}
	
	public boolean isBillOverages() {
		return billOverages;
	}
	
	public void setBillOverages(boolean billOverages) {
		this.billOverages = billOverages;
	}
	
	public String getBillTerms() {
		return billTerms;
	}
	
	public void setBillTerms(String billTerms) {
		this.billTerms = billTerms;
	}
	
	public String getClientName() {
		return clientName;
	}
	
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	
	public float getHoursBudget() {
		return hoursBudget;
	}
	
	public void setHoursBudget(float hoursBudget) {
		this.hoursBudget = hoursBudget;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getManager() {
		return manager;
	}
	
	public void setManager(String manager) {
		this.manager = manager;
	}
	
	public float getMoneyBudget() {
		return moneyBudget;
	}
	
	public void setMoneyBudget(float moneyBudget) {
		this.moneyBudget = moneyBudget;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Set<ContractRoleEntity> getRoles() {
		return roles;
	}
	
	public void setRoles(Set<ContractRoleEntity> roles) {
		this.roles = roles;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getTeam() {
		return team;
	}
	
	public void setTeam(String team) {
		this.team = team;
	}
	
	public String getUserSpaceId() {
		return userSpaceId;
	}
	
	public void setUserSpaceId(String userSpaceId) {
		this.userSpaceId = userSpaceId;
	}

	public Date getCloseDate() {
		return closeDate;
	}

	public void setCloseDate(Date closeDate) {
		this.closeDate = closeDate;
	}

	public Date getOpenDate() {
		return openDate;
	}

	public void setOpenDate(Date openDate) {
		this.openDate = openDate;
	}

	public String getProjects() {
		return projects;
	}

	public void setProjects(String projects) {
		this.projects = projects;
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
		final ContractEntity other = (ContractEntity) obj;
		if (id != other.id)
			return false;
		return true;
	}

	
	public static ContractEntity createBlankContract() {
		ContractEntity cont = new ContractEntity();
		cont.setBillOverages(true);
		cont.setBillTerms("");
		cont.setClientName("");
		cont.setManager("");
		cont.setName("");
		cont.setProjects("<projects>\n<project>Main</project>\n</projects>\n");
		cont.setStatus("");
		cont.setTeam("");
		
		HashSet<ContractRoleEntity> roles = new HashSet<ContractRoleEntity>();
		ContractRoleEntity genRole = new ContractRoleEntity();
		genRole.setRole("General");
		
		cont.setRoles(roles);
		
		return cont;
	}
	
}
