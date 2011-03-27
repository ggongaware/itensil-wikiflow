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
package itensil.workflow.activities.signals;

import junit.framework.TestCase;
import itensil.io.HibernateUtil;
import itensil.workflow.state.SimulatorStateStore;
import itensil.workflow.state.StepState;
import itensil.workflow.state.SubState;
import itensil.workflow.state.Token;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.element.Condition;
import itensil.workflow.Runner;
import itensil.workflow.FlowEvent;
import itensil.workflow.rules.ConditionEval;
import itensil.workflow.activities.rules.XPathConditionEval;
import itensil.workflow.activities.rules.WFActivityStepInvoker;
import itensil.workflow.activities.state.*;
import itensil.workflow.activities.timer.TimerDaemon;
import itensil.security.DefaultUser;
import itensil.security.LoginTestHelper;
import itensil.security.User;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.repository.DuplicateException;
import itensil.repository.RepoTestHelper;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryNode;
import itensil.uidgen.IUIDGenerator;

import org.hibernate.Query;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.io.File;

/**
 * @author ejones@itensil.com
 *
 */
public class WFSignalManagerJunit extends TestCase {

	static Set<String> role = new HashSet<String>();
    static {
        role.add("inviter");

}

	
	IUIDGenerator idGen = new IUIDGenerator();
    String userName;

	public WFSignalManagerJunit() {
	}

	protected void setUp() throws Exception {
        userName = "junit" + System.currentTimeMillis();
	}
	protected void tearDown() throws Exception {

	}

	@SuppressWarnings("unchecked")
	public void testPersistSignal1_NoExistingTransaction() throws Exception {

		Session session = HibernateUtil.getSession();
		String usr1Id = "junitUsr1";
		String usr2Id = "junitUsr2";
		String flowId = "flow1";

		HibernateUtil.beginTransaction();
		Object del = session.get(FlowState.class, flowId);
		if(del != null) session.delete((FlowState)del);
		HibernateUtil.commitTransaction();
		


		HibernateUtil.beginTransaction();
		FlowState fState = new FlowState();
		fState.setId(flowId);
		fState.setActive(true);

		session.saveOrUpdate(fState);

		Activity item1 = new Activity(idGen.createID().toString());
		item1.setFlow(fState);
		//item1.setId(normSess.getIdentifier());
		item1.setName("Activity One");

		item1.setSubmitId(usr2Id);
		ActivityStepState state = new ActivityStepState();
		state.setAssignId(usr1Id);
		state.setActivity(item1);
		state.setStepId("My Step");
		item1.getStates().put(state.getStepId(), state);

		Activity subItem1 = new Activity(idGen.createID().toString());
		subItem1.setFlow(fState);
		subItem1.setName("Sub Activity One");

		subItem1.setSubmitId(usr2Id);
		subItem1.setParent(item1);
		ActivityStepState state2 = new ActivityStepState();
		state2.setAssignId(usr1Id);
		state2.setActivity(subItem1);
		state2.setStepId("My Other Step");
		subItem1.getStates().put(state2.getStepId(), state2);

		session.persist(item1);
		session.persist(subItem1);
		session.persist(state);
		session.persist(state2);

		HibernateUtil.commitTransaction();

		SignalImpl sig = new AlertSignalImpl();
		sig.setActivity(item1);
		sig.setStepId("My Other Step");
		sig.setAssignId(state.getAssignId());
		sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
		sig.setTimeStamp(new Date());
		sig.setRole("role1");

		assertNull("signal id is not null", sig.getId());

		SignalManager.saveOrUpdateSignal(sig);

		assertNotNull("signal id is null", sig.getId());

		SignalManager.deleteSignal(sig);
	}

	@SuppressWarnings("unchecked")
	public void testPersistSignal1_ExistingTransaction() throws Exception {

		Session session = HibernateUtil.getSession();
		String usr1Id = "junitUsr1";
		String usr2Id = "junitUsr2";
		String flowId = "flow1";

		HibernateUtil.beginTransaction();
		Object del = session.get(FlowState.class, flowId);
		if(del != null) session.delete((FlowState)del);
		HibernateUtil.commitTransaction();
		
		HibernateUtil.beginTransaction();
		FlowState fState = new FlowState();
		fState.setId(flowId);
		fState.setActive(true);

		session.saveOrUpdate(fState);

		Activity item1 = new Activity(idGen.createID().toString());
		item1.setFlow(fState);
		item1.setName("Activity One");

		item1.setSubmitId(usr2Id);
		ActivityStepState state = new ActivityStepState();
		state.setAssignId(usr1Id);
		state.setActivity(item1);
		state.setStepId("My Step");
		item1.getStates().put(state.getStepId(), state);

		Activity subItem1 = new Activity(idGen.createID().toString());
		subItem1.setFlow(fState);
		subItem1.setName("Sub Activity One");

		subItem1.setSubmitId(usr2Id);
		subItem1.setParent(item1);
		ActivityStepState state2 = new ActivityStepState();
		state2.setAssignId(usr1Id);
		state2.setActivity(subItem1);
		state2.setStepId("My Other Step");
		subItem1.getStates().put(state2.getStepId(), state2);

		//item1.getChildren().add(subItem1);
		session.persist(item1);
		session.persist(subItem1);
		session.persist(state);
		session.persist(state2);


		//normSess.persist(state);

		HibernateUtil.commitTransaction();

		HibernateUtil.beginTransaction();

		SignalImpl sig = new AlertSignalImpl();
		sig.setActivity(item1);
		sig.setStepId("My Other Step");
		sig.setAssignId(state.getAssignId());
		sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
		sig.setTimeStamp(new Date());
		sig.setRole("role1");

		assertNull("signal id is not null", sig.getId());

		SignalManager.saveOrUpdateSignal(sig);

		HibernateUtil.commitTransaction();

		assertNotNull("signal id is null", sig.getId());
		
		SignalManager.deleteSignal(sig);
		
	}

	@SuppressWarnings("unchecked")
	public void testPendingAlertCountGetAlerts() throws Exception {

 		Session session = HibernateUtil.getSession();

 		String flowId = "flow1";

		HibernateUtil.beginTransaction();
		Object del = session.get(FlowState.class, flowId);
		if(del != null) session.delete((FlowState)del);
		HibernateUtil.commitTransaction();
		

		
	    	 SecurityAssociation.setUser(SysAdmin.getUser());

	    	 // create user  id for test
	         HibernateUtil.beginTransaction();
	         long idMod = System.currentTimeMillis();
	         String name = "junit" + idMod;
	         String name2 = "junit2" + idMod;
	         UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
	         UserEntity user = 
	        	 (UserEntity)
	        	 	uspace.createUser(userName, "junit", "pass", role, Locale.getDefault(), TimeZone.getDefault());
	         HibernateUtil.commitTransaction();
	         
	         // build alert for user id
			String assignedUserId = user.getUserId();
			//String usr1Id = "junitUsr1";
			String submitId = "junitUsr2";

			HibernateUtil.beginTransaction();
			FlowState fState = new FlowState();
			fState.setId(flowId);
			fState.setActive(true);

			session.saveOrUpdate(fState);

			Activity item1 = new Activity(idGen.createID().toString());
			item1.setFlow(fState);
			//item1.setId(normSess.getIdentifier());
			item1.setName("Activity One");

			item1.setSubmitId(submitId);
			ActivityStepState state = new ActivityStepState();
			state.setAssignId(assignedUserId);
			state.setActivity(item1);
			state.setStepId("My Step");
			item1.getStates().put(state.getStepId(), state);

			Activity subItem1 = new Activity(idGen.createID().toString());
			subItem1.setFlow(fState);
			subItem1.setName("Sub Activity One");

			subItem1.setSubmitId(submitId);
			subItem1.setParent(item1);
			ActivityStepState state2 = new ActivityStepState();
			state2.setAssignId(assignedUserId);
			state2.setActivity(subItem1);
			state2.setStepId("My Other Step");
			subItem1.getStates().put(state2.getStepId(), state2);

			//item1.getChildren().add(subItem1);
			session.persist(item1);
			session.persist(subItem1);
			session.persist(state);
			session.persist(state2);

			//normSess.persist(state);

			HibernateUtil.commitTransaction();

			SignalImpl sig = new AlertSignalImpl();
			sig.setActivity(item1);
			sig.setStepId("My Other Step");
			sig.setAssignId(state.getAssignId());
			sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
			sig.setTimeStamp(new Date());
			sig.setRole("role1");

			SignalManager.saveOrUpdateSignal(sig);
			String sigId = sig.getId();

			int count = SignalManager.pendingAlertCount(user);

			assertEquals(1, count);
			
			sig=null;
			
			List<SignalImpl> sigList = SignalManager.getAlerts(user);
			
			assertEquals("number alert for user error", 1, sigList.size());

			assertEquals("signal id retrieved does not match", sigId, sigList.get(0).getId());
			

			sig = new AlertSignalImpl();
			sig.setActivity(item1);
			sig.setStepId("My Other Step");
			sig.setAssignId(state.getAssignId());
			sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
			sig.setTimeStamp(new Date());
			sig.setRole("role1");

			SignalManager.saveOrUpdateSignal(sig);

			sigList = SignalManager.getAlerts(user);

			assertEquals(2, sigList.size());

			assertEquals("signal id retrieved does not match", sigId, sigList.get(0).getId());
			assertEquals("signal id retrieved does not match", sig.getId(), sigList.get(1).getId());
			
			
			
	}	
	
	
	@SuppressWarnings("unchecked")
	public void testGetSignalStatus() throws Exception {

 		Session session = HibernateUtil.getSession();

 		String flowId = "flow1";

		HibernateUtil.beginTransaction();
		Object del = session.get(FlowState.class, flowId);
		if(del != null) session.delete((FlowState)del);
		HibernateUtil.commitTransaction();
		

		
	    	 SecurityAssociation.setUser(SysAdmin.getUser());

	    	 // create user  id for test
	         HibernateUtil.beginTransaction();
	         long idMod = System.currentTimeMillis();
	         String name = "junit" + idMod;
	         String name2 = "junit2" + idMod;
	         UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
	         UserEntity user = 
	        	 (UserEntity)
	        	 	uspace.createUser(userName, "junit", "pass", role, Locale.getDefault(), TimeZone.getDefault());
	         HibernateUtil.commitTransaction();
	         
	         // build alert for user id
			String assignedUserId = user.getUserId();
			//String usr1Id = "junitUsr1";
			String submitId = "junitUsr2";

			HibernateUtil.beginTransaction();
			FlowState fState = new FlowState();
			fState.setId(flowId);
			fState.setActive(true);

			session.saveOrUpdate(fState);

			Activity item1 = new Activity(idGen.createID().toString());
			item1.setFlow(fState);
			//item1.setId(normSess.getIdentifier());
			item1.setName("Activity One");

			item1.setSubmitId(submitId);
			ActivityStepState state = new ActivityStepState();
			state.setAssignId(assignedUserId);
			state.setActivity(item1);
			state.setStepId("My Step");
			item1.getStates().put(state.getStepId(), state);

			Activity subItem1 = new Activity(idGen.createID().toString());
			subItem1.setFlow(fState);
			subItem1.setName("Sub Activity One");

			subItem1.setSubmitId(submitId);
			subItem1.setParent(item1);
			ActivityStepState state2 = new ActivityStepState();
			state2.setAssignId(assignedUserId);
			state2.setActivity(subItem1);
			state2.setStepId("My Other Step");
			subItem1.getStates().put(state2.getStepId(), state2);

			//item1.getChildren().add(subItem1);
			session.persist(item1);
			session.persist(subItem1);
			session.persist(state);
			session.persist(state2);

			//normSess.persist(state);

			HibernateUtil.commitTransaction();

			SignalImpl sig = new AlertSignalImpl();
			sig.setActivity(item1);
			sig.setStepId("My Other Step");
			sig.setAssignId(state.getAssignId());
			sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
			sig.setTimeStamp(new Date());
			sig.setRole("role1");

			SignalManager.saveOrUpdateSignal(sig);

			long sigMask = SignalManager.getSignalStatus(user);

			assertEquals("Activity Alert signal mask not properly set", SignalUtil.SIGNAL_ACTIVE_ALERT_MASK, sigMask & SignalUtil.SIGNAL_ACTIVE_ALERT_MASK);
			
	}	

	@SuppressWarnings("unchecked")
	public void testMarkAlertAsRead() throws Exception {


 		Session session = HibernateUtil.getSession();

 		String flowId = "flow1";

		HibernateUtil.beginTransaction();
		Object del = session.get(FlowState.class, flowId);
		if(del != null) session.delete((FlowState)del);
		HibernateUtil.commitTransaction();
		

		
	    	 SecurityAssociation.setUser(SysAdmin.getUser());

	    	 // create user  id for test
	         HibernateUtil.beginTransaction();
	         long idMod = System.currentTimeMillis();
	         String name = "junit" + idMod;
	         String name2 = "junit2" + idMod;
	         UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
	         UserEntity user = 
	        	 (UserEntity)
	        	 	uspace.createUser(userName, "junit", "pass", role, Locale.getDefault(), TimeZone.getDefault());
	         HibernateUtil.commitTransaction();
	         
	         // build alert for user id
			String assignedUserId = user.getUserId();
			//String usr1Id = "junitUsr1";
			String submitId = "junitUsr2";

			HibernateUtil.beginTransaction();
			FlowState fState = new FlowState();
			fState.setId(flowId);
			fState.setActive(true);

			session.saveOrUpdate(fState);

			Activity item1 = new Activity(idGen.createID().toString());
			item1.setFlow(fState);
			//item1.setId(normSess.getIdentifier());
			item1.setName("Activity One");

			item1.setSubmitId(submitId);
			ActivityStepState state = new ActivityStepState();
			state.setAssignId(assignedUserId);
			state.setActivity(item1);
			state.setStepId("My Step");
			item1.getStates().put(state.getStepId(), state);

			Activity subItem1 = new Activity(idGen.createID().toString());
			subItem1.setFlow(fState);
			subItem1.setName("Sub Activity One");

			subItem1.setSubmitId(submitId);
			subItem1.setParent(item1);
			ActivityStepState state2 = new ActivityStepState();
			state2.setAssignId(assignedUserId);
			state2.setActivity(subItem1);
			state2.setStepId("My Other Step");
			subItem1.getStates().put(state2.getStepId(), state2);

			//item1.getChildren().add(subItem1);
			session.persist(item1);
			session.persist(subItem1);
			session.persist(state);
			session.persist(state2);

			//normSess.persist(state);

			HibernateUtil.commitTransaction();

			SignalImpl sig = new AlertSignalImpl();
			sig.setActivity(item1);
			sig.setStepId("My Other Step");
			sig.setAssignId(state.getAssignId());
			sig.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
			sig.setTimeStamp(new Date());
			sig.setRole("role1");

			SignalManager.saveOrUpdateSignal(sig);

			sig=null;
			
			List<SignalImpl> sigList = SignalManager.getAlerts(user);

			assertEquals("number alert for user error", 1, sigList.size());
			assertEquals("alert read status error", false, sigList.get(0).read);

			SignalManager.markAlertAsRead(sigList.get(0).getId());
			
			sig=null;
			
			// get alert after update
			sigList = SignalManager.getAlerts(user);

			assertEquals("number alert for user error", 1, sigList.size());
			assertEquals("alert read status error", true, sigList.get(0).read);
			
	}	
	

}
