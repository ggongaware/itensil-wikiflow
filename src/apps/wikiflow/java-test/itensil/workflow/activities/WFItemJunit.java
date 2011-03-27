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
package itensil.workflow.activities;

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
import java.util.List;
import java.io.File;

/**
 * @author ggongaware@itensil.com
 *
 * Run this from a path that can access ./testdata/
 */
public class WFItemJunit extends TestCase {

    IUIDGenerator idGen = new IUIDGenerator();
    
    public WFItemJunit() {
    	//  local mode
        TimerDaemon td = TimerDaemon.initInstance();
        td.setSleepTime(200);
    }
    
    protected void setUp() throws Exception {
        
    }

	protected void tearDown() throws Exception {
		
	}


	@SuppressWarnings("unchecked")
	public void testUserItem() throws Exception {
        Session normSess = HibernateUtil.getSession();
        String usr1Id = "junitUsr1";
        String usr2Id = "junitUsr2";
        String flowId = "flow1";


        HibernateUtil.beginTransaction();
        FlowState fState = new FlowState();
        fState.setId(flowId);
        fState.setActive(true);

        normSess.saveOrUpdate(fState);

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
        
        //item1.getChildren().add(subItem1);
        normSess.persist(item1);
        normSess.persist(subItem1);
        normSess.persist(state);
        normSess.persist(state2);
        

        //normSess.persist(state);

        HibernateUtil.commitTransaction();


        HibernateUtil.beginTransaction();
        normSess = HibernateUtil.getSession();
        normSess.refresh(item1);
        assertEquals(1, item1.getChildren().size());
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        normSess = HibernateUtil.getSession();

        Activity act1 = (Activity)normSess.get(Activity.class, item1.getId());
        System.out.println(ActivityXML.display(act1));

        act1 = (Activity)normSess.get(Activity.class, subItem1.getId());
        System.out.println(ActivityXML.display(act1));


        System.out.println("--getAssignItems--");

        UserActivities usrItms = new UserActivities(new DefaultUser(usr1Id), normSess);
        List<Activity> asgnElems = usrItms.getAssignActivities(0, false);
        for (Activity aEl : asgnElems) {
            System.out.println(ActivityXML.display(aEl));
        }

        System.out.println("--getSubmitItems--");
        usrItms = new UserActivities(new DefaultUser(usr2Id), normSess);
        List<Activity> subtElems = usrItms.getSubmitActivities(0, false);
        for (Activity sEl : subtElems) {
            System.out.println(ActivityXML.display(sEl));
        }

        HibernateUtil.closeSession();
    }

    public void testFlow1() throws Exception {
        FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow1.xml"));

        HibernateUtil.beginTransaction();
        FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, "flow1");
        if (fState == null) {
            fState = new FlowState();
            fState.setId("flow1");
            fState.setActive(true);
            HibernateUtil.getSession().saveOrUpdate(fState);
        }

        FlowRole role = fState.getRoles().get("role1");
        if (role == null) {
            role = new FlowRole();
        }
        role.setRole("role1");
        role.setAssignId("bob");
        role.setFlow(fState);
        fState.getRoles().put(role.getRole(), role);
        HibernateUtil.getSession().saveOrUpdate(role);

        role = fState.getRoles().get("role2");
        if (role == null) {
            role = new FlowRole();
        }
        role.setRole("role2");
        role.setAssignId("jim");
        role.setFlow(fState);
        fState.getRoles().put(role.getRole(), role);
        HibernateUtil.getSession().saveOrUpdate(role);

        HibernateUtil.commitTransaction();


        ActivityStateStore store = new  ActivityStateStore(fState);
        Runner<Activity,String> run = new Runner<Activity,String>(
                fmod, store, store, new MatchEvtDataEval<Activity>(), new WFActivityStepInvoker<String>());
        Activity tok = new Activity(idGen.createID().toString());
        tok.setSubmitId("fred");
        //tok.setAssignId("fred");

        HibernateUtil.beginTransaction();
        store.clearAllTokens();

        fState = (FlowState)HibernateUtil.getSession().load(FlowState.class, "flow1");
        assertFalse(fState.getRoles().isEmpty());
        assertEquals("bob", fState.getRoles().get("role1").getAssignId());
        HibernateUtil.getSession().saveOrUpdate(tok);
        HibernateUtil.commitTransaction();


        FlowEvent<Activity,String> evt;

        // at start
        HibernateUtil.beginTransaction();
        assertEquals("s1", run.startToken(null, tok));
        
        assertEquals(1, store.countTokens());
        assertEquals(tok, store.getTokens().iterator().next());
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        // kick off start
        run.handleEvent(new FlowEvent<Activity,String>(tok, "s1"));
        HibernateUtil.commitTransaction();

        // at activity a1
        HibernateUtil.beginTransaction();
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());
        assertEquals("bob", ((ActivityStepState)store.getActiveSteps(tok).iterator().next()).getAssignId());
        UserActivities usrItm = new UserActivities(new DefaultUser("bob"), HibernateUtil.getSession());
        for (Object ob : usrItm.getAssignActivities(0, false)) {
            System.out.println(ActivityXML.display((Activity) ob));
        }
        assertEquals(1, store.countStepTokens("a1"));
        HibernateUtil.commitTransaction();

        // exit a1 with loop data
        HibernateUtil.beginTransaction();
        evt = new FlowEvent<Activity,String>(tok, "a1");
        evt.setEventData("loop");
        run.handleEvent(evt);

        // looped activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());
        HibernateUtil.commitTransaction();

        //Date tstamp = new Date();
        HibernateUtil.beginTransaction();
        evt = new FlowEvent<Activity,String>(tok, "a1");
        evt.setEventData("noloop");
        run.handleEvent(evt);


        // activity 2
        assertEquals("a2", store.getActiveSteps(tok).iterator().next().getStepId());
        assertEquals("jim", ((ActivityStepState)store.getActiveSteps(tok).iterator().next()).getAssignId());

        //domSess = HibernateUtil.getSession().getSession(EntityMode.DOM4J);
        usrItm = new UserActivities(new DefaultUser("jim"), HibernateUtil.getSession());
        for (Object ob : usrItm.getAssignActivities(0, false)) {
            System.out.println(ActivityXML.display((Activity) ob));
        }
        HibernateUtil.commitTransaction();

        // restart
        HibernateUtil.beginTransaction();
        run.handleEvent(new FlowEvent<Activity,String>(tok, "s1"));


        // back at activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());
        HibernateUtil.commitTransaction(); 
        
        HibernateUtil.closeSession();
    }

    public void testScriptFlow1() throws Exception {
        FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("scriptflow1.xml"));

        HibernateUtil.beginTransaction();
        FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, "scriptflow1");
        if (fState == null) {
            fState = new FlowState();
            fState.setId("scriptflow1");
            fState.setActive(true);
            HibernateUtil.getSession().saveOrUpdate(fState);
        }

        FlowRole role = fState.getRoles().get("role1");
        if (role == null) {
            role = new FlowRole();
        }
        role.setRole("role1");
        role.setAssignId("bob");
        role.setFlow(fState);
        fState.getRoles().put(role.getRole(), role);
        HibernateUtil.getSession().saveOrUpdate(role);

        role = fState.getRoles().get("role2");
        if (role == null) {
            role = new FlowRole();
        }
        role.setRole("role2");
        role.setAssignId("jim");
        role.setFlow(fState);
        fState.getRoles().put(role.getRole(), role);
        HibernateUtil.getSession().saveOrUpdate(role);

        HibernateUtil.commitTransaction();


        ActivityStateStore store = new  ActivityStateStore(fState);
        Runner<Activity,String> run = new Runner<Activity,String>(
                fmod, store, store, new XPathConditionEval<String>(fmod), new WFActivityStepInvoker<String>());
        Activity tok = new Activity(idGen.createID().toString());
        tok.setSubmitId("fred");
        //tok.setAssignId("fred");

        HibernateUtil.beginTransaction();
        store.clearAllTokens();

        fState = (FlowState)HibernateUtil.getSession().load(FlowState.class, "scriptflow1");
        assertFalse(fState.getRoles().isEmpty());
        assertEquals("bob", fState.getRoles().get("role1").getAssignId());
        HibernateUtil.getSession().saveOrUpdate(tok);
        HibernateUtil.commitTransaction();


        // at start
        HibernateUtil.beginTransaction();
        assertEquals("s1", run.startToken(null, tok));
        
        assertEquals(1, store.countTokens());
        assertEquals(tok, store.getTokens().iterator().next());
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        // kick off start
        run.handleEvent(new FlowEvent<Activity,String>(tok, "s1"));
        HibernateUtil.commitTransaction();

        // at activity a1
        HibernateUtil.beginTransaction();
        assertEquals("a2", store.getActiveSteps(tok).iterator().next().getStepId());
        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();
    }
    
    
    public void testRepoFlow1() throws Exception {

        HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(SysAdmin.getUser());
        User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        RepoTestHelper.initRepository("/j1test", user);
        
        try {
        	RepositoryHelper.createCollection("/j1test/process");
        } catch (DuplicateException de) {
        	// eat it
        }
        
        SecurityAssociation.setUser(user);
        RepoTestHelper.loadFile(getClass().getResourceAsStream("testflow1.flow.xml"), "chart.flow", -1,
                "/j1test/process/testflow1", "application/itensil-flow+xml");

        try {
        	RepositoryHelper.createCollection("/j1test/process/testflow1/activities");
        } catch (DuplicateException de) {
        	// eat it
        }
        String itemUri = RepositoryHelper.getAvailableUri("/j1test/process/testflow1/activities/testrun");
        RepositoryNode itemNode = RepositoryHelper.getRepository(itemUri).createNode(itemUri, true, user);

        RepoTestHelper.loadFile(getClass().getResourceAsStream("testdata1.xml"), "testdata1.xml", -1,
                itemNode.getUri(), "text/xml");

        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        FlowModel fmod = new FlowModel();
        String flowuri = "/j1test/process/testflow1/chart.flow";
        fmod.loadXML(RepositoryHelper.loadContent(flowuri));
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        FlowState fState;
        String flowId = RepositoryHelper.getNode(flowuri, false).getParentNodeId();
        fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flowId);
        if (fState == null) {
        	fState = new FlowState();
        	fState.setId(flowId);
        }

        HibernateUtil.getSession().saveOrUpdate(fState);
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        XPathConditionEval<String> eval = new XPathConditionEval<String>(fmod);
        ActivityStateStore store = new  ActivityStateStore(fState);
        // SimulatorStateStore<Activity,String> timStore = 
        //	new SimulatorStateStore<Activity,String>(fState.getId(), false, false);
        Runner<Activity,String> run = new Runner<Activity,String>(
                fmod, store, store, eval,
                new WFActivityStepInvoker<String>());

        Activity tok = new Activity(itemNode.getNodeId());
        tok.setSubmitId(user.getUserId());
        tok.setUserSpaceId(user.getUserSpaceId());
        HibernateUtil.getSession().persist(tok);
        run.handleEvent(new FlowEvent<Activity,String>(tok, run.startToken(null, tok)));
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(tok);

        // at activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());

        // leave a1
        run.handleEvent(new FlowEvent<Activity,String>(tok, "a1"));

        // loop back in a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());
        
        // change data
        eval.setDataValueExpr(tok, "val1", "val1 + 1");
        
        // leave a1 again
        run.handleEvent(new FlowEvent<Activity,String>(tok, "a1"));

        // at timer t1
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());
        
        
        //timStore.fireTimers(new Date(), run, null);
        
        // still at timer t1
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(tok);
        
        // change data
        eval.setDataValueExpr(tok, "val1", "val1 + 1");
        
        HibernateUtil.commitTransaction();
        
        Thread.sleep(1000);
        
        synchronized (ActivityStateStore.timerSync) {
        	ActivityStateStore.timerSync.wait(20000);
        }
        
        
        //timStore.fireTimers(new Date(), run, null);
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(tok);
        
        // at end e1
        assertEquals("e1", store.getActiveSteps(tok).iterator().next().getStepId());
        
        System.out.println(ActivityXML.display(tok).asXML());

        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
    }



    public void testParallel1() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("parallel1.xml"));
        
        HibernateUtil.beginTransaction();
        FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, "parallel1");
        if (fState == null) {
            fState = new FlowState();
            fState.setId("parallel1");
            fState.setActive(true);
            HibernateUtil.getSession().saveOrUpdate(fState);
        }
        ActivityStateStore store = new ActivityStateStore(fState);
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        Runner<Activity,String> run = new Runner<Activity,String>(
                fmod, store, store, new MatchEvtDataEval<Activity>(), null);
        
        Activity tok = new Activity(idGen.createID().toString());
        tok.setSubmitId("fred");
        //tok.setAssignId("fred");
        
        FlowEvent<Activity,String> evt;

        // start and continue
        run.handleEvent(new FlowEvent<Activity,String>(tok, run.startToken(null, tok)));
        
        // the pre
        assertEquals("pre", store.getActiveSteps(tok).iterator().next().getStepId());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "pre"));
        //StepState actStps[] = new StepState<Token>[1];
        
        StepState actStps[] = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("trackA", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "trackA"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "trackB"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "trackB2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "join1")); 
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("pre2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "pre2"));
                
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track2A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        // loop it
        evt = new FlowEvent<Activity,String>(tok, "track2A");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("pre2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
       
        run.handleEvent(new FlowEvent<Activity,String>(tok, "pre2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track2A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "track2B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        // loop it again
        evt = new FlowEvent<Activity,String>(tok, "track2A");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("pre2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "pre2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(3, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        assertEquals("track2B", actStps[2].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[2].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "track2B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        // no loop
        run.handleEvent(new FlowEvent<Activity,String>(tok, "track2A"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "join2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("pre3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "pre3"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track3A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track3B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "track3B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track3A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();

        run.handleEvent(new FlowEvent<Activity,String>(tok, "track3A"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        // loop it again
        evt = new FlowEvent<Activity,String>(tok, "join3");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("track3B", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        run.handleEvent(new FlowEvent<Activity,String>(tok, "track3B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        
        // no loop
        run.handleEvent(new FlowEvent<Activity,String>(tok, "join3"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("End", actStps[0].getStepId());
        assertEquals(SubState.ENTER_END, actStps[0].getSubState());
        
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
    }
    
    
    
    public void testParallel2() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("parallel2.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("parallel2", true, true);
        Runner<Token,String> run = new Runner<Token,String>(
                fmod, store, store, new MatchEvtDataEval<Token>(), null);
        
        Token tok = new Token("junit1");
        FlowEvent<Token,String> evt;

        // start and continue
        run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));
        
        // the pre
        assertEquals("pre", store.getActiveSteps(tok).iterator().next().getStepId());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "pre"));
        //StepState actStps[] = new StepState<Token>[1];
        
        StepState actStps[] = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("trackA", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "trackA"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        // loop
        evt = new FlowEvent<Token,String>(tok, "trackB");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        // no loop
        run.handleEvent(new FlowEvent<Token,String>(tok, "trackB"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "join1")); 
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("pre2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "pre2"));
                
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track2A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        // loop it
        evt = new FlowEvent<Token,String>(tok, "track2A");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("pre2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
       
        run.handleEvent(new FlowEvent<Token,String>(tok, "pre2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track2A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "track2B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        // loop it again
        evt = new FlowEvent<Token,String>(tok, "track2A");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("pre2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "pre2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(3, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        assertEquals("track2B", actStps[2].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[2].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "track2B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        
        // no loop
        run.handleEvent(new FlowEvent<Token,String>(tok, "track2A"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track2A2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "track2A2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join2", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "join2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("pre3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "pre3"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("track3A", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track3B", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "track3B"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("track3A", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        

        run.handleEvent(new FlowEvent<Token,String>(tok, "track3A"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        // loop it
        evt = new FlowEvent<Token,String>(tok, "join3");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("join3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        // no loop
        run.handleEvent(new FlowEvent<Token,String>(tok, "join3"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("End", actStps[0].getStepId());
        assertEquals(SubState.ENTER_END, actStps[0].getSubState());
   }
    
   public void testRecentActiveFlows() throws Exception {
	   HibernateUtil.beginTransaction();
       SecurityAssociation.setUser(SysAdmin.getUser());
       User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
       HibernateUtil.commitTransaction();
      
	   HibernateUtil.beginTransaction();
	   Query qry = HibernateUtil.getSession().getNamedQuery("FlowState.recentlyActiveFlows");
	   qry.setString("userSpaceId", user.getUserSpaceId());
	   
	   List nodes = qry.list();
	   
	   assertFalse(nodes.isEmpty());
	   
	   HibernateUtil.commitTransaction();
   }
   
   public void testChangeId() throws Exception {
	   HibernateUtil.beginTransaction();
	   Activity tok = new Activity(idGen.createID().toString());
	   HibernateUtil.getSession().save(tok);
	   HibernateUtil.commitTransaction();
	   
	   HibernateUtil.beginTransaction();
	   tok.changeActivityId(idGen.createID().toString());
	   HibernateUtil.commitTransaction();
   }
   
   static class SSCompareStepId implements Comparator<StepState> {
	   public int compare(StepState ss1, StepState ss2) {
		   return ss1.getStepId().compareTo(ss2.getStepId());
	   }   
   }

    static class MatchEvtDataEval<Tk extends Token> implements ConditionEval<Tk,String> {

        public boolean isAsync() { return false; }

        public String evalExclusive(String flowId, Condition[] conditions, FlowEvent<Tk,String> evt) {
        	int last = conditions.length - 1;
            String dat = evt.getEventData();
            if (dat == null) dat = "";
            for (int ii = 0; ii < last; ii++) {
                Condition cond = conditions[ii];
                if (dat.equals(cond.getInnerText())) {
                    return cond.getReturnId();
                }
            }
            // else
            Condition ec = conditions[last];
            return ec == null ? null : ec.getReturnId();
        }

        public String[] evalInclusive(String flowId, Condition[] conditions, FlowEvent<Tk,String> evt) {
            ArrayList<String> rets = new ArrayList<String>(conditions.length);
            for (Condition cond : conditions) {
                if (cond != null && Boolean.parseBoolean(cond.getInnerText())) {
                    rets.add(cond.getReturnId());
                }
            }
            return rets.toArray(new String[rets.size()]);
        }
    }

}
