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

import itensil.workflow.state.*;
import itensil.workflow.rules.*;
import itensil.workflow.model.*;
import itensil.workflow.model.element.*;
import itensil.workflow.timer.TimerScheduler;
import itensil.workflow.timer.TimerHelper;
import itensil.util.Check;

import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Runner<Tk extends Token, Dt> {

    public final SubState WAIT_STATES [] = {SubState.WAIT_ENTER_STEP, SubState.WAIT_EXIT_STEP};

    protected FlowModel model;
    protected StateStore<Tk> states;
    protected TimerScheduler<Tk> timers;
    protected ConditionEval<Tk,Dt> evals;
    protected ActivityStepInvoker<Tk,Dt> actions;
    protected TrafficDirector<Tk,Dt> traffic;

    /**
     * @param model
     * @param states
     * @param timers
     * @param evals
     * @param actions (optional)
     */
    public Runner(
            FlowModel model,
            StateStore<Tk> states,
            TimerScheduler<Tk> timers,
            ConditionEval<Tk,Dt> evals,
            ActivityStepInvoker<Tk,Dt> actions) {

        this.model = model;
        this.states = states;
        this.timers = timers;
        this.evals = evals;
        this.actions = actions;
        if (states.isMultiStep()) {
        	traffic = new DefaultMultiTrafficDirector<Tk,Dt>();
        } else {
        	traffic = new DefaultTrafficDirector<Tk,Dt>();
        }
    }

    /**
     *
     * @param startId (optional) null for default
     * @param token
     * @return startId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public String startToken(String startId, Tk token) throws
        RunException, ActivityStepException, EvalException, StateException  {

        Step step;
        if (Check.isEmpty(startId)) {
            Collection<Start> starts = model.getStartSteps();
            if (starts.isEmpty()) {
                throw new RunException("No start steps defined");
            }
            step = starts.iterator().next();
            startId = step.getId();
        } else {
            step = model.getStep(startId);
            if (!(step instanceof Start)) {
                throw new RunException("Start step not found for id: " + startId);
            }
        }
        states.addToken(token);
        //FlowEvent<Tk,Dt> evt = new FlowEvent<Tk,Dt>(token, step.getId());
        //enterStep(step, evt, states.generateTx());
        return startId;
    }


    /**
     * Primary method
     * @param evt
     * @return resulting txId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public String handleEvent(FlowEvent<Tk,Dt> evt) throws
        RunException, ActivityStepException, EvalException, StateException {

        states.activateToken(evt.getToken());
        String resTx = exitStep(evt.getStepId(), evt, null);
        states.passivateToken(evt.getToken());
        return resTx;
    }

    /**
     * For expiring notifies
     * @param evt
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public void handleEventExpire(FlowEvent<Tk,Dt> evt) throws
        RunException, ActivityStepException, EvalException, StateException {

        states.activateToken(evt.getToken());
        // TODO - what happens on expire?
        states.passivateToken(evt.getToken());
    }


    /**
     * An advanced management function
     *
     * @param evt where stepId = destination activity
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public void moveToken(FlowEvent<Tk,Dt> evt) throws
            RunException, ActivityStepException, EvalException, StateException {

        // Get Step Model
        String actvityId = evt.getStepId();
        Step step = model.getStep(actvityId);
        if (step == null) {
            throw new RunException("Step not found for id: " + actvityId);
        } else if (!(step instanceof ActivityStep)) {
            throw new RunException("Move destination must be an activity");
        }

        states.activateToken(evt.getToken());
        // Cancel current step states
        Collection<? extends StepState<Tk>> css = states.getActiveSteps(evt.getToken());
        for (StepState<Tk> st : new ArrayList<StepState<Tk>>(css)) {
        	Step oldStep = model.getStep(st.getStepId());
        	if (oldStep instanceof Timer) {
            	timers.clearTimer((Timer)oldStep, evt.getToken());
            }
            states.removeActiveStep(st);
            if (states.isLogging()) {
                StepLog<Tk> log = states.createStepLog();
                log.fromState(st);
                log.setTimeStamp(new Date());
                log.setSubState(SubState.CANCEL_STEP);
                states.addLogStep(log);
            }
        }
        enterStep(step, evt, states.generateTx());
        states.passivateToken(evt.getToken());
    }

    /**
     * Force a token to enter a step
     * @param evt
     */
    public void forceEnter(FlowEvent<Tk,Dt> evt) throws
    	RunException, ActivityStepException, EvalException, StateException {
    	
    	 states.activateToken(evt.getToken());
    	 Step step = model.getStep(evt.getStepId());
         if (step == null) {
             throw new RunException("Step not found for id: " + evt.getStepId());
         }
         enterStep(step, evt, states.generateTx());
         states.passivateToken(evt.getToken());
    }
    
    /**
     *
     * @param stepId
     * @param evt
     * @param mulTxId multistep transaction
     * @return resulting txId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    protected String exitStep(String stepId, FlowEvent<Tk,Dt> evt, String mulTxId) throws
        RunException, ActivityStepException, EvalException, StateException {

        // Get Step Model
        Step step = model.getStep(stepId);
        if (step == null) {
            throw new RunException("Step not found for id: " + stepId);
        } else if (step instanceof Switch) {
            throw new RunException("Exit called on switch: " + stepId);
        } else if (step instanceof End) {
            throw new RunException("Exit called on end: " + stepId);
        }

        // Get Step State
        StepState<Tk> sState = null;
        for (StepState<Tk> st : states.getActiveSteps(evt.getToken())) {
            if (stepId.equals(st.getStepId())) {
                sState = st;
                break;
            }
        }
        if (sState == null) {

            // Allow new start events
            if (step instanceof Start) {
                //if (!states.isMultiStep()) {
                    Collection<? extends StepState<Tk>> css = states.getActiveSteps(evt.getToken());
                    for (StepState<Tk> st : new ArrayList<StepState<Tk>>(css)) {
                        states.removeActiveStep(st);
                        if (states.isLogging()) {
                            StepLog<Tk> log = states.createStepLog();
                            log.fromState(st);
                            log.setTimeStamp(new Date());
                            log.setSubState(SubState.CANCEL_STEP);
                            states.addLogStep(log);
                        }
                    }
                //}
                enterStep(step, evt, states.generateTx());
                return exitStep(step.getId(), evt, null);
            }

            // else not a start
            throw new RunException("Token has no active step for id: " + stepId);
        } else if (sState.getSubState() == SubState.WAIT_ENTER_STEP) {
            throw new RunException("Token is waiting for id: " + stepId);
        }

        // Handle Step Types
        if (step instanceof ActivityStep) {
            if (actions != null) {
                ActivityStepResult aRes = actions.activityExit(states.getFlowId(), (ActivityStep)step, evt);
                if (aRes == ActivityStepResult.WAIT) {
                    sState.setSubState(SubState.WAIT_EXIT_STEP);
                    sState.setTimeStamp(new Date());
                    states.updateActiveStep(sState);
                    if (states.isLogging()) {
                        StepLog<Tk> log = states.createStepLog();
                        log.fromState(sState);
                        states.addLogStep(log);
                    }
                    return null;
                }
            }
            if (sState.getExpireTime() != null) {
                timers.clearExpire(sState);
            }
        } else if (step instanceof Timer) {
        	timers.clearTimer((Timer)step, evt.getToken());
        }
        String txId = (mulTxId == null ? states.generateTx() : mulTxId);

        // update states
        states.removeActiveStep(sState);
        if (states.isLogging()) {
            StepLog<Tk> log = states.createStepLog();
            log.fromState(sState);
            log.setSubState(SubState.getExit(step));
            log.setTimeStamp(new Date());
            log.setExpireTime(null);
            log.setTxId(txId);
            states.addLogStep(log);
        }

        // goto following steps
        for (Path pth : traffic.travelPaths(step, evt, states)) {
            Step toStep = pth.getToStep();
            if (toStep == null)
                throw new RunException("To-step missing for path id: " + pth.getId());
            txId = enterStep(toStep, evt, txId);
            if (!states.isMultiStep()) break;
        }
        
        return txId;
    }

    /**
     * For async condition evals
     *
     * @param evt
     * @param returnIds
     * @return resulting txId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public String handleEventCondition(FlowEvent<Tk,Dt> evt, String [] returnIds) throws
        RunException, ActivityStepException, EvalException, StateException {

        Step step = model.getStep(evt.getStepId());
        if (!(step instanceof Switch) && !(step instanceof Timer)) {
            throw new RunException("Switch/Timer step not found for id: " + evt.getStepId());
        }
        states.activateToken(evt.getToken());
        String resTx = null;
        if (step instanceof Switch) {
        	resTx = exitSwitch((Switch)step, evt, returnIds, states.generateTx());
        } else {

            // until timer came true
            if (returnIds.length > 0) {
            	resTx = exitStep(step.getId(), evt, null);
            }
        }
        states.passivateToken(evt.getToken());
        
        return resTx;
    }

    /**
     * Steps in a waiting state (but not timers)
     *
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    public void checkWaiting() throws
        RunException, ActivityStepException, EvalException, StateException {

        // find all waiting steps
        for (StepState<Tk> sStep : states.getActiveSteps(WAIT_STATES)) {
            if (sStep.getSubState().isExit()) {

                // attempt to exit
                FlowEvent<Tk,Dt> evt = new FlowEvent<Tk,Dt>(sStep.getToken(), sStep.getStepId());
                exitStep(sStep.getStepId(), evt, null);

            } 
            // TODO decide if this makes sense for APIs int the future
            /* else {

                // attempt to enter
                Step step = model.getStep(sStep.getStepId());
                if (step == null) {
                    throw new RunException("Step not found for id: " + sStep.getStepId());
                }
                FlowEvent<Tk,Dt> evt = new FlowEvent<Tk,Dt>(sStep.getToken(), sStep.getStepId());
                enterStep(step, evt, sStep.getTxId());
            } */
        }
    }

    /**
     * 
     * @param step
     * @param evt
     * @param txId
     * @return resulting txId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    protected String enterStep(Step step, FlowEvent<Tk,Dt> evt, String txId) throws
        RunException, ActivityStepException, EvalException, StateException {

        // Get Step State
        StepState<Tk> sState = null;
        String stepId = step.getId();
        Collection<? extends StepState<Tk>> sStates = states.getActiveSteps(evt.getToken());
        for (StepState<Tk> st : sStates) {
            if (stepId.equals(st.getStepId())) {
                sState = st;
                break;
            }
        }
        if (sState != null) {
            // already here, be nice about it
            if (sState.getSubState() == SubState.WAIT_ENTER_STEP) {
                states.removeActiveStep(sState);
            } else { // eject before making duplicate
                return txId;
            }
        }
        
        sState = states.createStepState();
        sState.setStepId(stepId);
        sState.setToken(evt.getToken());
        sState.setTimeStamp(new Date());
        sState.setSubState(SubState.getEnter(step));
        sState.setTxId(txId);
        
        states.addActiveStep(sState);

        boolean doExit = false;
        String mulTxId = null;
        
        boolean waiting = traffic.waitRequired(step, evt, states);
        if (waiting) {
        	sState.setSubState(SubState.WAIT_ENTER_STEP);
        	states.updateActiveStep(sState);
        } else {
	        if (step instanceof ActivityStep && actions != null) {
	            switch (actions.activityEnter(states.getFlowId(), (ActivityStep)step, evt)) {
	                case WAIT:
	                    sState.setSubState(SubState.WAIT_ENTER_STEP);
	                    states.updateActiveStep(sState);
	                    waiting = true;
	                    break;
	                case EXIT_STEP:
	                    doExit = true;
	                    break;
	                case EXIT_STEP_SAME_TX:
	                    doExit = true;
	                    mulTxId = txId;
	                    break;
	            }
	        } else if (step instanceof Junction) {
	        	// exit in same tx
	        	doExit = true;
                mulTxId = txId;
	        } else if (step instanceof End && actions != null) {
	        	actions.enterEnd(states.getFlowId(), (End)step, evt);
	        }
        }

        if (states.isLogging()) {
            StepLog<Tk> log = states.createStepLog();
            log.fromState(sState);
            states.addLogStep(log);
        }
        
        // waiting do no more
        if (waiting) return txId;

        if (step instanceof Switch) {
            Switch swStep = (Switch)step;

            Condition conds[] = swStep.getConditions();
            if (conds.length < 1) {
                throw new RunException("No condtions in switch id: " + swStep.getId());
            }
            String pathIds[] = null;

            if ("XOR".equalsIgnoreCase(swStep.getMode())) {
            	String ePid = evals.evalExclusive(states.getFlowId(), conds, evt);
                if (!Check.isEmpty(ePid)) pathIds = new String[]{ePid};
            } else if ("ALLOC".equalsIgnoreCase(swStep.getMode())) {

                // Weights are between 0.0-1.0, totalling 1.0
                double allocs[] = swStep.getAllocs();

                // cumulatively notch a stick
                double cumuls[] = new double[allocs.length];
                cumuls[0] = allocs[0];
                for (int ii = 1; ii < allocs.length; ii++) {
                    cumuls[ii] = cumuls[ii - 1] + allocs[ii];
                }

                // assumes nice distribution
                // then starting with the least
                // checks if it hits
                double rand = Math.random();
                for (int ii = 0; ii < cumuls.length - 1; ii++) {
                    if (rand < cumuls[ii]) {
                        pathIds = new String[] {conds[ii].getReturnId()};
                        break;
                    }
                }
                if (pathIds == null) {
                    pathIds = new String[] {conds[cumuls.length - 1].getReturnId()};
                }

            } else {
                pathIds = evals.evalInclusive(states.getFlowId(), conds, evt);
            }
            if (!evals.isAsync() && !Check.isEmpty(pathIds)) {
                return exitSwitch(swStep, evt, pathIds, txId);
            }

        } else if (step instanceof Timer) {
            timers.setTimer((Timer)step, evt.getToken(), evt.getTime());
        } else if (doExit) {
        	return exitStep(step.getId(), evt, mulTxId);
        } else if (step instanceof ActivityStep) {
            Date expD = TimerHelper.calcExpire(sState.getTimeStamp(), step.getAttribute("expire"));
            sState.setExpireTime(expD);
            if (expD != null) {
                timers.trackExpire(sState);
            }
        }
        return txId;
    }

    /**
     * 
     * @param step
     * @param evt
     * @param pathIds
     * @param txId
     * @return resulting txId
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
    protected String exitSwitch(Switch step, FlowEvent<Tk,Dt> evt, String [] pathIds, String txId) throws
        RunException, ActivityStepException, EvalException, StateException {

        // Get Step State
        StepState<Tk> sState = null;
        String stepId = step.getId();
        Collection<? extends StepState<Tk>> sStates = states.getActiveSteps(evt.getToken());
        for (StepState<Tk> st : sStates) {
            if (stepId.equals(st.getStepId())) {
                sState = st;
                break;
            }
        }
        if (sState == null) {
            throw new RunException("Token has no active step for id: " + stepId);
        }

        // update state
        states.removeActiveStep(sState);
        if (states.isLogging()) {
            StepLog<Tk> log = states.createStepLog();
            log.fromState(sState);
            log.setSubState(SubState.getExit(step));
            log.setTimeStamp(new Date());
            log.setTxId(txId);
            states.addLogStep(log);
        }
        
        for (String pthId : pathIds) {
            Path pth = step.getPath(pthId);
            if (pth == null) {
                throw new RunException("Path not found for id: " + pthId);
            }
            Step toStep = pth.getToStep();
            if (toStep == null) {
            	throw new RunException("Path not connected");
            }
            enterStep(toStep, evt, txId);
            if (!states.isMultiStep()) break;
        }
        
        return txId;
    }

    public FlowModel getModel() {
        return model;
    }

    public ActivityStepInvoker<Tk, Dt> getActions() {
        return actions;
    }

    public StateStore<Tk> getStates() {
        return states;
    }

    public TimerScheduler<Tk> getTimers() {
        return timers;
    }

    public ConditionEval<Tk, Dt> getEvals() {
        return evals;
    }

	public TrafficDirector<Tk, Dt> getTraffic() {
		return traffic;
	}

	public void setTraffic(TrafficDirector<Tk, Dt> traffic) {
		this.traffic = traffic;
	}

}
