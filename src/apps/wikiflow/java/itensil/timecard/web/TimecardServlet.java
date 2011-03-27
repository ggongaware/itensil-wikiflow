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
package itensil.timecard.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;

import itensil.io.HibernateUtil;
import itensil.repository.NotFoundException;
import itensil.security.AuthenticatedUser;
import itensil.timecard.hibernate.ContractEntity;
import itensil.util.Check;
import itensil.web.ContentType;
import itensil.web.MethodServlet;

public class TimecardServlet extends MethodServlet {

	/**
     *  /activeContracts
     *
     *  List active contracts:
     *  
     *  <contracts>
     *		<contract id="2323" client="WalMart" name="News wire" />
     *		...
     *  </contracts>
     */
    @ContentType("text/xml")
    public void webActiveContracts(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    }
    
    
	/**
     *  /contractRoles
     *
     *  Distinct list of roles across contracts:
     *  
     *  <roles>
     *		<role></role>
     *  </roles>
     */
    @ContentType("text/xml")
    public void webContractRoles(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	 AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
		 HibernateUtil.beginTransaction();
		 HibernateUtil.readOnlySession();
		 Query qry = HibernateUtil.getSession().getNamedQuery("Timecard.allRoles");
		 qry.setString("usid", self.getUserSpaceId());
		
		 Document doc = DocumentHelper.createDocument();
		 Element root = doc.addElement("roles");
		  
		 for (Object roleStr : qry.list()) {
			  root.addElement("role").setText(roleStr.toString());
		 }
		  
		 HibernateUtil.commitTransaction();
		  
		 doc.write(response.getWriter());
    }
    
    
	/**
     *  /contractOptions
     *  
     *  Params:
     *  
     *  	contract_id - required
     *  
     *  Get contract options:
     *  
     *   	<options>
	 *			<project>DDJSS001</project>
	 *			<project>WLL921</project>
	 *			<role>General</role>
	 *			<role>Data Entry</role>
	 *			<role>Web Designer</role>
	 *		</options>
     *	
     */
    @ContentType("text/xml")
    public void webContractOptions(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    }
    
    
    /**
     *  /getContract
     *  
     * Params:
     * 
     * 		contract_id  - optional, empty = new
     *
     * Get a contract:
     *
     *		<contract id="123">
     *  		<clientName/>
     *          <name/>
     *          <team/>
     *          <estMonths/>
     *          <status/>
     *          <billTerms/>
     *          <billOverages/>
     *          <moneyBudget/>
     *          <hoursBudget/>
     *          <openDate/>
     *          <closeDate/>
     *  		<manager/>
     *  	
     *  		<projects>
     *  			<project>Main</project>
     *  			...
     *  		</projects>
     *  	
     *  		<role id="122" rate="25">General</role>
     *  		...
     *  	</contract>
     */
    @ContentType("text/xml")
    public void webGetContract(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    	AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
		HibernateUtil.beginTransaction();
		HibernateUtil.readOnlySession();
		 
    	String contractId = request.getParameter("contract_id");
    	
    	ContractEntity cont;
    	if (Check.isEmpty(contractId)) {
    		cont = ContractEntity.createBlankContract();
    	} else {
    		cont = (ContractEntity)HibernateUtil.getSession().load(ContractEntity.class, contractId);
    		if (cont == null || !cont.getUserSpaceId().equals(self.getUserSpaceId())) {
    			throw new NotFoundException("Contract id:" + contractId);
    		}
    	}
    	
    	Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("contract");
		root.addAttribute("id", String.valueOf(cont.getId()));
		root.addElement("clientName").setText(cont.getClientName());
		root.addElement("name").setText(cont.getName());
		root.addElement("team").setText(cont.getTeam());
		
    	HibernateUtil.commitTransaction();
		  
		doc.write(response.getWriter());
    }
    
    /**
     *  /saveContract
     *
     * Save a contract
     * 
     */
    @ContentType("text/xml")
    public void webSaveContract(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    }

    
    /**
     *  /getWeek
     *  
     * Params:
     * 
     * 	start -	optional starting date, empty = this week
     *
     * Get a week:
     * 
     *	 	<timecard>
	 *			<day date="2007-01-07" name="Sun" work="0"/>
	 *			...
	 *			<role-log id="" contractId="2323" billable="1" project="DDJSS001" 
	 *					role="Data Entry" userId="AAAAAAAAAAAAAAAAAAAAA">
	 *				<time id="" hours="0" activityId="" />
	 *			</role-log>
	 *			...
	 *		</timcard>
	 *
     */
    @ContentType("text/xml")
    public void webGetWeek(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    }    
    
    
    /**
     *  /saveWeek
     *
     * Save a contract
     *
     */
    @ContentType("text/xml")
    public void webSaveWeek(HttpServletRequest request, HttpServletResponse response) throws Exception {
   
    }

    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        HibernateUtil.rollbackTransaction();
    }

    /**
     * Clean-up
     */
    public void afterMethod() {
        HibernateUtil.closeSession();
    }
}
