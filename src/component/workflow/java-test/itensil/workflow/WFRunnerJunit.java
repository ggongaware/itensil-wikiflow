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
package itensil.workflow;

import itensil.workflow.model.AppElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.FlowSAXHandler;
import itensil.workflow.model.ValidationLogger;
import itensil.workflow.model.element.ActivityStep;
import itensil.workflow.model.element.Condition;
import itensil.workflow.model.element.End;
import itensil.workflow.model.element.Step;
import itensil.workflow.rules.ActivityStepException;
import itensil.workflow.rules.ActivityStepInvoker;
import itensil.workflow.rules.ActivityStepResult;
import itensil.workflow.rules.ConditionEval;
import itensil.workflow.state.SimulatorStateStore;
import itensil.workflow.state.StepState;
import itensil.workflow.state.SubState;
import itensil.workflow.state.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;


/**
 * @author ggongaware@itensil.com
 *
 */
public class WFRunnerJunit extends TestCase {

    protected void setUp() throws Exception {
    }
    
    public void testValidationFlow1() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow1.xml"));
        ValidationLogger vlog = new ValidationLogger();
        vlog.addCheck(Step.VALIDATE_NO_PARALLEL);
        fmod.validate(vlog);
        assertTrue(vlog.isValid());
    }
    
    public void testValidationFlow2() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow2.xml"));
        ValidationLogger vlog = fmod.validate();
        assertTrue(vlog.isValid());
    }
    
    public void testValidationFlow3() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow3.xml"));
        ValidationLogger vlog = fmod.validate();
        assertFalse(vlog.isValid());
        assertEquals(3, vlog.getErrors());
        assertEquals(2, vlog.getWarns());
        assertEquals(0, vlog.getInfos());
    }
    
    public void testNamespaceFlow4() throws Exception {
    	String junitNS = "junit";
    	
    	FlowModel fmod = new FlowModel();
    	FlowSAXHandler saxHand = new FlowSAXHandler(fmod);
    	saxHand.addAppElementType(JuRoot.class, junitNS, "root");
    	saxHand.addAppElementType(JuStart.class, junitNS, "start");
    	saxHand.addAppElementType(JuAct.class, junitNS, "act");
    	saxHand.addAppElementType(JuSubAct.class, junitNS, "sub-act");
    	
        fmod.loadXML(saxHand, getClass().getResourceAsStream("flow4.xml"));
        
        Collection<JuRoot> roots = fmod.selectAppChildren(JuRoot.class);
        assertEquals(2, roots.size());
        int ii = 1;
        for (JuRoot jr : roots) {
        	assertEquals("root" + ii, jr.getInnerText());
        	assertEquals("root" + ii + "-att1", jr.getAttr1());
        	assertTrue(jr.getChildren().isEmpty());
        	ii++;
        }
        
        Step s1Step = fmod.getStep("s1");
        JuStart js = s1Step.selectOneChild(JuStart.class);
        assertNotNull(js);
        assertEquals("nots1", js.getRef());
        assertTrue(js.getChildren().isEmpty());
        
        Step a1Step = fmod.getStep("a1");
        JuAct ja = a1Step.selectOneChild(JuAct.class);
        assertNotNull(ja);
        assertEquals("a1", ja.getRef());
        Collection<JuSubAct> subActs = ja.selectChildren(JuSubAct.class);
        assertEquals(3, subActs.size());
        
        ii = 1;
        for (JuSubAct jsa : subActs) {
        	assertEquals("sub" + ii, jsa.getInnerText());
        	ii++;
        }
        
        ValidationLogger vlog = fmod.validate();
        assertFalse(vlog.isValid());
        assertEquals(1, vlog.getErrors());
        assertEquals(js, vlog.getRecords().iterator().next().getSource());
    }
    
    public void testFlow1() throws Exception {
    	_testFlow1(false);
    	_testFlow1(true);
    }

    private void _testFlow1(boolean isMulti) throws Exception {
        FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow1.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("flow1", true, isMulti);
        Runner<Token,String> run = new Runner<Token,String>(fmod, store, store, new MatchEvtDataEval<Token>(), null);
        Token tok = new Token("junit1");
        FlowEvent<Token,String> evt;

        // at start
        assertEquals("s1", run.startToken(null, tok));

        // kick off start
        run.handleEvent(new FlowEvent<Token,String>(tok, "s1"));

        // at activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());

        // exit a1 with loop data
        evt = new FlowEvent<Token,String>(tok, "a1");
        evt.setEventData("loop");
        run.handleEvent(evt);

        // looped activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());

        Date tstamp = new Date();

        evt = new FlowEvent<Token,String>(tok, "a1");
        evt.setEventData("noloop");
        run.handleEvent(evt);

        // in timer
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());

        store.fireTimers(tstamp, run, null);

        // no time passed, still in timer
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());

        // progress 4 minutes
        tstamp.setTime(tstamp.getTime() + 4*60000);
        store.fireTimers(tstamp, run, null);
        
        // no time passed, still in timer
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());
        
        // progress 62 minutes
        tstamp.setTime(tstamp.getTime() + 62*60000);
        store.fireTimers(tstamp, run, null);

        // at end
        assertEquals("e1", store.getActiveSteps(tok).iterator().next().getStepId());

        // restart
        run.handleEvent(new FlowEvent<Token,String>(tok, "s1"));

        // back at activity a1
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());

        // leave a1
        run.handleEvent(new FlowEvent<Token,String>(tok, "a1"));

        // in timer
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());

        // move to a1
        run.moveToken(new FlowEvent<Token,String>(tok, "a1"));
        assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());
        
        // exit a1 with loop data
        evt = new FlowEvent<Token,String>(tok, "a1");
        evt.setEventData("path2");
        run.handleEvent(evt);
        
        // in timer
        assertEquals("t2", store.getActiveSteps(tok).iterator().next().getStepId());
        
        store.fireTimers(tstamp, run, null);
        
        // no condition change
        assertEquals("t2", store.getActiveSteps(tok).iterator().next().getStepId());
        
        store.fireTimers(tstamp, run, "fire");
        
        // at end
        assertEquals("e1", store.getActiveSteps(tok).iterator().next().getStepId());
    }
    
    public void testFlow1Active() throws Exception {
    	_testFlow1Active(false);
    	_testFlow1Active(true);
    }


    private void _testFlow1Active(boolean isMulti) throws Exception {
        FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow1.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("flow1", true, isMulti);
        Runner<Token,String> run = new Runner<Token,String>(
                fmod, store, store, new MatchEvtDataEval<Token>(), new AutoExitInvoker<Token>());

        Token tok = new Token("junit1");

        // start and continue
        String txId = run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));

        // all the way in timer
        assertEquals("t1", store.getActiveSteps(tok).iterator().next().getStepId());

        // roll back to last activity (backing through any switches)
        store.rollbackTx(tok, txId);
        if (!isMulti) assertEquals("a1", store.getActiveSteps(tok).iterator().next().getStepId());

    }
    
    public void testFlow2() throws Exception {
    	_testFlow2(false);
    	_testFlow2(true);
    }

    private void _testFlow2(boolean isMulti) throws Exception {
        FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("flow2.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("flow2", true, isMulti);
        Runner<Token,String> run = new Runner<Token,String>(
                fmod, store, store, new MatchEvtDataEval<Token>(), null);

        int counts[] = new int[3];
        for (int ii = 0; ii < 10000; ii++) {
        	Token tok = new Token("junit" + ii);

            // start and continue
            run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));
            String stepId = store.getActiveSteps(tok).iterator().next().getStepId();
            if ("a2a".equals(stepId)) {
                counts[0]++;
            } else if ("a2b".equals(stepId)) {
                counts[1]++;
            } else if ("a2c".equals(stepId)) {
                counts[2]++;
            } else {
                fail();
            }
        }

        // tesing with 5% tolerance
        assertTrue(counts[0] < 3000);
        assertTrue(counts[0] > 2000);
        assertTrue(counts[1] < 5500);
        assertTrue(counts[1] > 4500);
        assertTrue(counts[2] < 3000);
        assertTrue(counts[2] > 2000);
    }
    
    public void testParallel1() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("parallel1.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("parallel1", true, true);
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
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "trackB"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "trackB2"));
        
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
        
        
        
        // loop it again
        evt = new FlowEvent<Token,String>(tok, "join3");
        evt.setEventData("loop=1");
        run.handleEvent(evt);
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        assertEquals("track3B", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "track3B"));
        
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
    
    
    
    public void testParallel3() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("parallel3.xml"));
        SimulatorStateStore<Token,String> store = new SimulatorStateStore<Token,String>("parallel3", true, true);
        Runner<Token,String> run = new Runner<Token,String>(
                fmod, store, store, new MatchEvtDataEval<Token>(), null);
        
        Token tok = new Token("junit1");
        FlowEvent<Token,String> evt;

        // start and continue
        run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));
        
        
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
        
        
        
        // loop it
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
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("trackB2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "trackB2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("join1", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "join1"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());

        assertEquals("End", actStps[0].getStepId());
        assertEquals(SubState.ENTER_END, actStps[0].getSubState());
        
    }
    
    
    public void testGroup1() throws Exception {
    	FlowModel fmod = new FlowModel();
        fmod.loadXML(getClass().getResourceAsStream("group1.xml"));
        SimulatorStateStore<Token,String> store = 
        	new SimulatorStateStore<Token,String>("group1", true, true);
        Runner<Token,String> run = new Runner<Token,String>(
                fmod, store, store, new MatchEvtDataEval<Token>(), null);
        
        Token tok = new Token("junit1");

        // start and continue
        run.handleEvent(new FlowEvent<Token,String>(tok, run.startToken(null, tok)));
        
        // the first
        assertEquals("Step", store.getActiveSteps(tok).iterator().next().getStepId());
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "Step"));

        // should now be inside the group steps
        StepState actStps[] = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("Group/Step", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("Group/Step 2", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());
        
        run.handleEvent(new FlowEvent<Token,String>(tok, "Group/Step 2"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("Group$$E", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("Group/Step", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());

        run.handleEvent(new FlowEvent<Token,String>(tok, "Group/Step"));
        
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(2, actStps.length);
        Arrays.sort(actStps, new SSCompareStepId());
        
        assertEquals("Group$$E", actStps[0].getStepId());
        assertEquals(SubState.WAIT_ENTER_STEP, actStps[0].getSubState());
        
        assertEquals("Group/Step 3", actStps[1].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[1].getSubState());

        run.handleEvent(new FlowEvent<Token,String>(tok, "Group/Step 3"));
        
        // should now be outside the group
        actStps = store.getActiveSteps(tok).toArray(new StepState[0]);
        assertEquals(1, actStps.length);

        assertEquals("Step 3", actStps[0].getStepId());
        assertEquals(SubState.ENTER_STEP, actStps[0].getSubState());
        
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

    static class AutoExitInvoker<Tk extends Token> implements ActivityStepInvoker<Tk,String> {

        public ActivityStepResult activityEnter(String flowId, ActivityStep step, FlowEvent<Tk, String> evt) {
            return ActivityStepResult.EXIT_STEP;
        }

        public ActivityStepResult activityExit(String flowId, ActivityStep step, FlowEvent<Tk, String> evt) {
            return ActivityStepResult.DONE;
        }

		public void enterEnd(String flowId, End step, FlowEvent<Tk, String> evt) throws ActivityStepException {
		}
    }
    
    public static class JuRoot extends AppElement {
    	public String getAttr1() { return getAttribute("attr1"); }
    }
    
    public static class JuStart extends AppElement {
    	public String getRef() { return getAttribute("ref"); }

    	// sample validation:   ref = ../@id
		public void validate(ValidationLogger vlogger) {
			if (!((ContainerElement) getParent()).getId().equals(getRef())) {
				vlogger.error(this, "junit", "ref doesn't match parent id");
			}
			super.validate(vlogger);
		}    	
    }
    
    public static class JuAct extends AppElement {
    	public String getRef() { return getAttribute("ref"); }
    }
    
    public static class JuSubAct extends AppElement {
    }
    
}
