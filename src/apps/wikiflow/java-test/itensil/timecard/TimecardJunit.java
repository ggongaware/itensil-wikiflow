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
package itensil.timecard;

import itensil.io.HibernateUtil;
import itensil.timecard.hibernate.ContractEntity;
import itensil.timecard.hibernate.ContractRoleEntity;
import junit.framework.TestCase;

public class TimecardJunit extends TestCase {
	
	public void testContract() throws Exception {
		
		ContractEntity contract = new ContractEntity();
		contract.setUserSpaceId("AAAAAAAAAAAAAAAAAAAA");
		contract.setClientName("Acme Corp.");
		
		HibernateUtil.beginTransaction();
		
		HibernateUtil.getSession().persist(contract);
		
		ContractRoleEntity role = new ContractRoleEntity();
		role.setContract(contract);
		role.setRole("Data Services");
		role.setRate(120.00f);
		contract.getRoles().add(role);
		
		HibernateUtil.getSession().persist(role);
		
		HibernateUtil.commitTransaction();
		HibernateUtil.closeSession();
		
	}
}
