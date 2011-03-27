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
package itensil.workflow.state;

import itensil.workflow.model.element.Condition;
import itensil.workflow.model.element.Timer;
import itensil.workflow.model.element.Wait;
import itensil.workflow.model.element.Until;
import itensil.workflow.model.BasicElement;
import itensil.workflow.Runner;
import itensil.workflow.RunException;
import itensil.workflow.FlowEvent;
import itensil.workflow.timer.TimerScheduler;
import itensil.workflow.timer.TimerHelper;
import itensil.workflow.rules.EvalException;
import itensil.workflow.rules.ActivityStepException;

import java.util.*;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SimulatorStateStore<Tk extends Token, Dt>
        implements StateStore<Tk>, StateReporter<Tk>, TimerScheduler<Tk> {

    String flowId;
    HashMap<Tk, ArrayList<StepState<Tk>>> activeSteps;
    ArrayList<StepLog<Tk>> logSteps;
    HashMap<Tk, ArrayList<SetTimer>> timers;
    int txId;
    boolean logging;
    boolean multiStep;

    public SimulatorStateStore(String flowId, boolean logging, boolean multiStep) {
        this.flowId = flowId;
        this.logging = logging;
        this.multiStep = multiStep;
        activeSteps = new HashMap<Tk, ArrayList<StepState<Tk>>>();
        logSteps = new ArrayList<StepLog<Tk>>();
        timers = new HashMap<Tk, ArrayList<SetTimer>>();
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isMultiStep() {
        return multiStep;
    }

    public StepState<Tk> createStepState() {
        return new StepState<Tk>();
    }

    public StepLog<Tk> createStepLog() {
        return new StepLog<Tk>();
    }

    public Collection<StepState<Tk>> getActiveSteps(Tk token) throws StateException {
        return activeSteps.get(token);
    }

    public void activateToken(Tk token) throws StateException {
        // nothing for sim
    }

    public void passivateToken(Tk token) throws StateException {
        // nothing for sim
    }

    public Collection<StepState<Tk>> getActiveSteps(String stepId) throws StateException {
        ArrayList<StepState<Tk>> sSteps = new ArrayList<StepState<Tk>>();
        for (Map.Entry<Tk, ArrayList<StepState<Tk>>> ent : activeSteps.entrySet()) {
            for (StepState<Tk> ss : ent.getValue()) {
                if (stepId.equals(ss.getStepId())) {
                    sSteps.add(ss);
                }
            }
        }
        return sSteps;
    }

    public Collection<StepState<Tk>> getActiveSteps(SubState[] sStates) throws StateException {
        ArrayList<StepState<Tk>> sSteps = new ArrayList<StepState<Tk>>();
        for (Map.Entry<Tk, ArrayList<StepState<Tk>>> ent : activeSteps.entrySet()) {
            for (StepState<Tk> ss : ent.getValue()) {
                for  (SubState st : sStates) {
                    if (ss.getSubState() == st) {
                        sSteps.add(ss);
                        break;
                    }
                }
            }
        }
        return sSteps;
    }

    public int countStepTokens(String stepId) throws StateException {
        return getActiveSteps(stepId).size();
    }

    public Map<String, Integer> countBySteps() throws StateException {
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        for (Map.Entry<Tk, ArrayList<StepState<Tk>>> ent : activeSteps.entrySet()) {
            for (StepState<Tk> ss : ent.getValue()) {
                Integer sc = counts.get(ss.getStepId());
                if (sc == null) {
                    counts.put(ss.getStepId(), 1);
                } else {
                    counts.put(ss.getStepId(), sc + 1);
                }
            }
        }
        return counts;
    }

    public Collection<StepState<Tk>> getAllActiveSteps() throws StateException {
        ArrayList<StepState<Tk>> sSteps = new ArrayList<StepState<Tk>>();
         for (Map.Entry<Tk, ArrayList<StepState<Tk>>> ent : activeSteps.entrySet()) {
            sSteps.addAll(ent.getValue());
        }
        return sSteps;
    }

    public int countAllActiveSteps() throws StateException {
        return getAllActiveSteps().size();
    }

    public void addActiveStep(StepState<Tk> state) throws StateException {
        ArrayList<StepState<Tk>> states = activeSteps.get(state.getToken());
        states.add(state);
    }

    public void updateActiveStep(StepState<Tk> state) throws StateException {
        // all in mem...
    }

    public void removeActiveStep(StepState<Tk> state) throws StateException {
        ArrayList<StepState<Tk>> states = activeSteps.get(state.getToken());
        states.remove(state);
    }

    public void clearAllTokens() throws StateException {
        activeSteps.clear();
    }

    public Collection<Tk> getTokens() throws StateException {
        return activeSteps.keySet();
    }

    public int countTokens() throws StateException {
        return activeSteps.size();
    }

    public void addToken(Tk token) throws StateException {
        activeSteps.put(token, new ArrayList<StepState<Tk>>());
    }

    public void removeToken(Tk token) throws StateException {
        activeSteps.remove(token);
    }

    public String generateTx() {
        return String.valueOf(txId++);
    }

    public void rollbackTx(Tk token, String txId) throws StateException {
    	
        Collection<StepState<Tk>> stepStates = getActiveSteps(token);
        if (!stepStates.isEmpty()) {
        	for (StepState<Tk> state : new ArrayList<StepState<Tk>>(stepStates)) {
        		if (txId.equals(state.getTxId())) {
        			stepStates.remove(state);
        		}
        	}
            
            ArrayList<StepLog<Tk>> tLogs = new ArrayList<StepLog<Tk>>();
            for (StepLog<Tk> log : getLogSteps(token, null)) {
                if (txId.equals(log.getTxId())) {
                	tLogs.add(log);
                }
            }
            
            if (tLogs.isEmpty()) {
                throw new StateException("Cannot rollback, no previous logs");
            }
            
            // find the oldest (initial) exit
            StepLog<Tk> exLog = null;
            for (StepLog<Tk> log : tLogs) {
            	if (log.getSubState().isExit()) {
            		exLog = log;
            		break;
            	}
            }
            if (exLog == null) {
            	throw new StateException("Cannot rollback to a valid state");
            }
            
            // re-enter the inital exited step
            StepState<Tk> state = createStepState();
            state.setToken(token);
            state.setStepId(exLog.getStepId());
            state.setTxId(exLog.getTxId());
            state.setSubState(exLog.getSubState().exitToEnter());
            state.setTimeStamp(exLog.getTimeStamp());
            state.setExpireTime(exLog.getExpireTime());  
            stepStates.add(state);
        }
    }

    public Collection<StepLog<Tk>> getLogSteps(Token token, Date since) throws StateException {
        Collection<StepLog<Tk>> slogs = getLogSteps(since);
        ArrayList<StepLog<Tk>> logs = new ArrayList<StepLog<Tk>>(slogs.size());
        for (StepLog<Tk> step : slogs) {
            if (token.equals(step.getToken())) {
                logs.add(step);
            }
        }
        return logs;
    }

    public Collection<StepLog<Tk>> getLogSteps(String stepId, Date since) throws StateException {
        Collection<StepLog<Tk>> slogs = getLogSteps(since);
        ArrayList<StepLog<Tk>> logs = new ArrayList<StepLog<Tk>>(slogs.size());
        for (StepLog<Tk> step : slogs) {
            if (stepId.equals(step.getStepId())) {
                logs.add(step);
            }
        }
        return logs;
    }

    public Collection<StepLog<Tk>> getLogSteps(Date since) throws StateException {
        if (since == null) {
            return logSteps;
        }
        ArrayList<StepLog<Tk>> logs = new ArrayList<StepLog<Tk>>(logSteps.size());
        for (StepLog<Tk> step : logSteps) {
            if (since.after(step.getTimeStamp())) {
                logs.add(step);
            }
        }
        return logs;
    }
    
    public Collection<StepLog<Tk>> getExitLogSteps(Tk token, String stepId) throws StateException {
    	ArrayList<StepLog<Tk>> logs = new ArrayList<StepLog<Tk>>();
    	for (StepLog<Tk> step : logSteps) {
    		if (token.equals(step.getToken()) 
    				&& stepId.equals(step.getStepId())
    				&& step.getSubState().isExit() 
    				&& step.getSubState() != SubState.WAIT_EXIT_STEP) {
    			logs.add(step);	
    		}
    	}
		return logs;
	}

    public void addLogStep(StepLog<Tk> log) throws StateException {
        // insert sorted by date
        int ii = logSteps.size() - 1;
        for (; ii >= 0; ii--) {
            if (logSteps.get(ii).getTimeStamp().compareTo(log.getTimeStamp()) <= 0) {
                ii++;
                break;
            }
        }
        if (ii < 0) ii = 0;
        logSteps.add(ii, log);
    }

    public void trackExpire(StepState<Tk> state) throws StateException {
        // TODO implement
    }

    public void clearExpire(StepState<Tk> state) throws StateException {
        // TODO implement
    }

    public void setTimer(Timer timer, Tk token, Date fromTime) throws StateException {
        ArrayList<SetTimer> tts =  timers.get(token);
        if (tts == null) {
            tts = new ArrayList<SetTimer>();
            timers.put(token, tts);
        }
        SetTimer st = new SetTimer();
        st.timerId = timer.getId();
        if ("wait".equals(timer.getAttribute("mode"))) {
            Wait wd = timer.selectOneChild(Wait.class);
            if (wd == null) throw new StateException("<wait/> element missing from timer.");
            st.atTime = TimerHelper.calcWait(fromTime, wd);
        } else {
            Until ud = timer.selectOneChild(Until.class);
            if (ud == null) throw new StateException("<until/> element missing from timer.");
            if (ud.getType() == Until.TYPE.condition) {
            	// handle UNTIL conditions
            	st.condition = ud.selectOneChild(Condition.class);
            } else {
            	st.atTime = TimerHelper.calcUntil(fromTime, ud);
            }
        }
        tts.remove(st);
        tts.add(st);
    }

    public void clearTimer(Timer timer, Tk token) throws StateException {
        ArrayList<SetTimer> tts =  timers.get(token);
        if (tts != null) {
            SetTimer st = new SetTimer();
            st.timerId = timer.getId();
            tts.remove(st);
        }
    }
    
    public void setManualTimer(Timer timer, Tk token, Date atTime) throws StateException {
    	ArrayList<SetTimer> tts = timers.get(token);
        if (tts != null) {
        	SetTimer st = new SetTimer();
            st.timerId = timer.getId();
            tts.remove(st);
            st.atTime = atTime;
            tts.add(st);
        }
	}

    public void clearAllTimers() throws StateException {
        timers.clear();
    }

    public String getFlowId() {
        return flowId;
    }

    /**
     *
     * @param atTime null for all timers
     * @param run
     * @return
     * @throws StateException
     * @throws EvalException
     * @throws RunException
     * @throws ActivityStepException
     */
    public int fireTimers(Date atTime, Runner<Tk,Dt> run, Dt evtDat)
            throws StateException, EvalException, RunException, ActivityStepException {

        int count = 0;
        for (Map.Entry<Tk, ArrayList<SetTimer>> ent : timers.entrySet()) {
        	ArrayList<SetTimer> curTimers = new ArrayList<SetTimer>(ent.getValue());
            for (SetTimer st : curTimers) {
            	FlowEvent<Tk,Dt> evt = new FlowEvent<Tk,Dt>(ent.getKey(), st.timerId);
            	evt.setEventData(evtDat);
            	if (st.condition != null) {
            		Condition conds[] = new Condition[]{st.condition, null};
            		String ret = run.getEvals().evalExclusive(getFlowId(), conds, evt);
            		if (ret != null) {
            			run.handleEvent(evt);
            		}
            	} else if (atTime == null || atTime.after(st.atTime)) {
                    run.handleEvent(evt);
                }
                count++;
            }
        }
        return count;
    }

    static class SetTimer {
        Date atTime;
        String timerId;
        Condition condition;

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final SetTimer setTimer = (SetTimer) o;

            return timerId.equals(setTimer.timerId);
        }

        public int hashCode() {
            return timerId.hashCode();
        }
    }

	
}
