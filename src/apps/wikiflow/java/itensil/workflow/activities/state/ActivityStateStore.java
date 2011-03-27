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
package itensil.workflow.activities.state;

import itensil.uidgen.IUIDGenerator;
import itensil.workflow.state.*;
import itensil.workflow.timer.TimerHelper;
import itensil.workflow.timer.TimerScheduler;
import itensil.workflow.activities.timer.ActivityTimer;
import itensil.workflow.activities.timer.TimerDaemon;
import itensil.workflow.model.element.Timer;
import itensil.workflow.model.element.Until;
import itensil.workflow.model.element.Wait;
import itensil.io.HibernateUtil;


import java.util.*;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.HibernateException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityStateStore implements StateStore<Activity>, StateReporter<Activity>, TimerScheduler<Activity>  {

    protected FlowState flow;
    private static IUIDGenerator txGen = new IUIDGenerator();
    public static Object timerSync = new Object();

    public ActivityStateStore(FlowState flow) {
        this.flow = flow;
    }

    public boolean isLogging() {
        return true;
    }

    public boolean isMultiStep() {
        return true;
    }

    public StepState<Activity> createStepState() {
    	ActivityStepState state = new ActivityStepState();
    	state.setFlowDirty(true);
        return state;
    }

    public StepLog<Activity> createStepLog() {
        return new FlowStepLog(getFlow());
    }

    public Collection<? extends StepState<Activity>> getActiveSteps(Activity token) throws StateException {
        return token.getStates().values();
    }

    public void activateToken(Activity token) throws StateException {
    	getSession().lock(token, LockMode.UPGRADE);
    }

    public void passivateToken(Activity token) throws StateException {
    	Session sess = getSession();
    	for (ActivityStepState state : token.getStates().values()) {
    		if (state.getId() == null) {
    			sess.save(state);
    		} else if (state.isFlowDirty()) {
    			sess.update(state);
    		}
    		state.setFlowDirty(false);
    	}
    	sess.lock(token, LockMode.NONE);
    }

    public Collection<StepState<Activity>> getActiveSteps(SubState[] sStates) throws StateException {
        Integer sStateInts[] = new Integer[sStates.length];
        for (int ii = 0; ii < sStates.length; ii++) sStateInts[ii] = sStates[ii].ordinal();
        Query qry = getSession().getNamedQuery("FlowState.getActiveSteps");
        qry.setEntity("flow", getFlow());
        qry.setParameterList("subStates", sStateInts);

        List items = qry.list();
        ArrayList<StepState<Activity>> states = new ArrayList<StepState<Activity>>(items.size());
        for (Object os : items) {
            states.addAll(((Activity)os).getStates().values());
        }
        return states;
    }

    public void addActiveStep(StepState<Activity> state) throws StateException {
        state.getToken().getStates().put(state.getStepId(), (ActivityStepState) state);
    }

    public void updateActiveStep(StepState<Activity> state) throws StateException {
    	((ActivityStepState)state).setFlowDirty(true);
    	state.getToken().getStates().put(state.getStepId(), (ActivityStepState) state);
    }

    public void removeActiveStep(StepState<Activity> state) throws StateException {
    	state.getToken().getStates().remove(state.getStepId());
    	if (((ActivityStepState)state).getId() != null) {
    		getSession().delete(state);
    		
    		/*
    		 * some kind of bug here, I suspect hibernate...
    		 * need to flush to ensure delete
    		 */
    		getSession().flush();
    	}
    }

    public void clearAllTokens() throws StateException {
    	Collection<Activity> allToks = getTokens();
    	Session sess = getSession();
        for (Activity tok : allToks) {
        	sess.delete(tok);
        }
    }

    public void addToken(Activity token) throws StateException {
        token.setFlow(flow);
        getSession().saveOrUpdate(token);
    }

    public void removeToken(Activity token) throws StateException {
        getSession().delete(token);
    }

    public void trackExpire(StepState<Activity> state) throws StateException {
        // TODO implement
    }

    public void clearExpire(StepState<Activity> state) throws StateException {
        // TODO implement
    }
    
    protected ActivityTimer getActivityTimer(Timer timer, Activity token, boolean create) throws StateException {
    	ActivityTimer att = null;
    	Query qry = getSession().getNamedQuery("Timer.getTimer");
    	qry.setString("tid", timer.getId());
    	qry.setEntity("act", token);
    	att = (ActivityTimer)qry.uniqueResult();
    	if (att == null && create) {
    		att = new ActivityTimer();
    		att.setActivity(token);
    		att.setTimerId(timer.getId());
    	}
    	return att;
    }

    public void setTimer(Timer timer, Activity token, Date fromTime) throws StateException {
    	ActivityTimer att = getActivityTimer(timer, token, true);
    	if ("wait".equals(timer.getAttribute("mode"))) {
    		Wait wd = timer.selectOneChild(Wait.class);
    		if (wd == null) throw new StateException("<wait/> element missing from timer.");
    		att.setAtTime(TimerHelper.calcWait(fromTime, wd));
    	} else {
    		 Until ud = timer.selectOneChild(Until.class);
             if (ud == null) throw new StateException("<until/> element missing from timer.");
             if (ud.getType() == Until.TYPE.condition) {
            	 att.setConditional(true);
             } else {
            	 att.setAtTime(TimerHelper.calcUntil(fromTime, ud));
             }
    	}
    	getSession().saveOrUpdate(att);
    	if (!att.isConditional()) {
    		TimerDaemon td = TimerDaemon.getInstance();
    		if (td != null) td.scheduledTimer(att.getAtTime());
    	}
    }

    public void clearTimer(Timer timer, Activity token) throws StateException {
    	ActivityTimer att = getActivityTimer(timer, token, false);
    	if (att != null) getSession().delete(att);
    }
    
    public void setManualTimer(Timer timer, Activity token, Date atTime) throws StateException {
    	ActivityTimer att = getActivityTimer(timer, token, true);
    	att.setAtTime(atTime);
    	att.setConditional(false);
    	getSession().saveOrUpdate(att);
    	
    	TimerDaemon td = TimerDaemon.getInstance();
		if (td != null) td.scheduledTimer(att.getAtTime());
	}

    public void clearAllTimers() throws StateException {
    	Query qry = getSession().getNamedQuery("Timer.clearTimers");
    	qry.setEntity("flow", getFlow());
    	qry.executeUpdate();
    }

    public String getFlowId() {
        return flow.getId();
    }

    public String generateTx() {
        return txGen.createID().toString();
    }

    /**
     * This implementations assumes log ids always count up
     * @param token
     * @throws StateException
     */
    @SuppressWarnings("unchecked")
	public void rollbackTx(Activity token, String txId) throws StateException {
    	
    	
        if (!token.getStates().isEmpty()) {
            Session sess = getSession();
            ActivityStepState state = null;
            
            // clear all states from this TX
            for (ActivityStepState ast : new ArrayList<ActivityStepState>(token.getStates().values())) {
            	if (txId.equals(ast.getTxId())) {
            		token.getStates().remove(ast.getStepId());
                    sess.delete(ast);
            	}
            }
            
            // may instert
            sess.flush();
            
            Query qry = sess.getNamedQuery("FlowState.lastLogTx");
            qry.setEntity("flow", getFlow());
            qry.setEntity("token", token);
            qry.setString("txId", txId);
            List<FlowStepLog> rLogs = qry.list();
            FlowStepLog exLog = null;
            
            if (rLogs.isEmpty()) {
                throw new StateException("Cannot rollback, no previous logs");
            }
            
            // find the oldest (initial) exit
            for (FlowStepLog rLog : rLogs) {
            	if (rLog.getSubState().isExit()) {
            		exLog = rLog;
            		break;
            	}
            }
            
            if (exLog == null) {
            	// let the state removal be the end result
            	return;
            }
            
            /*
             * Find the tx that matches an exit from the rolledback tx
             */
            qry = sess.getNamedQuery("FlowState.nextToLastLogTx");
            qry.setEntity("flow", getFlow());
            qry.setEntity("token", token);
            qry.setLong("lastId", exLog.getId());
            qry.setString("enterStepId", exLog.getStepId());
            qry.setMaxResults(1);
    
            List<FlowStepLog> pLogs = qry.list();
            String reTxId;
            if (pLogs.isEmpty()) {
            	reTxId = generateTx();
            } else {
            	reTxId = pLogs.get(0).getTxId();
            }
            
            if (!token.getStates().containsKey(exLog.getStepId())) {
	            state = (ActivityStepState)createStepState();
	            state.setToken(token);
	            state.setStepId(exLog.getStepId());
	            state.setTxId(reTxId);
	            state.setSubState(exLog.getSubState().exitToEnter());
	            state.setTimeStamp(exLog.getTimeStamp());
	            state.setExpireTime(exLog.getExpireTime());
	            state.setAssignId(exLog.getUserId());
	            sess.save(state);
	            token.getStates().put(state.getStepId(), (ActivityStepState) state);
            }
        }
    }

    public void addLogStep(StepLog<Activity> log) throws StateException {
        getSession().save(log);
    }

    protected Session getSession() throws StateException {
        try {
            return HibernateUtil.getSession();
        } catch (HibernateException he) {
            throw new StateException("Error starting Hibernate", he);
        }
    }

    public FlowState getFlow() {
        return flow;
    }

    public Collection<StepState<Activity>> getActiveSteps(String stepId) throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getActiveStepsByStep");
        qry.setEntity("flow", getFlow());
        qry.setString("stepId", stepId);
        List items = qry.list();
        ArrayList<StepState<Activity>> states = new ArrayList<StepState<Activity>>(items.size());
        for (Object os : items) {
            states.addAll(((Activity)os).getStates().values());
        }
        return states;
    }

    public int countStepTokens(String stepId) throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.countStepTokens");
        qry.setEntity("flow", getFlow());
        qry.setString("stepId", stepId);
        return ((Long)qry.iterate().next()).intValue();
    }

    public Map<String, Integer> countBySteps() throws StateException {
        return null;  //TODO change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<StepState<Activity>> getAllActiveSteps() throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getTokens");
        qry.setEntity("flow", getFlow());
        List items = qry.list();
        ArrayList<StepState<Activity>> states = new ArrayList<StepState<Activity>>(items.size());
        for (Object itm : items) {
            states.addAll(((Activity) itm).getStates().values());
        }
        return states;
    }

    public int countAllActiveSteps() throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.countAllActiveSteps");
        qry.setEntity("flow", getFlow());
        qry.setInteger("endSubState", SubState.ENTER_END.ordinal());
        return ((Long)qry.iterate().next()).intValue();
    }

    @SuppressWarnings("unchecked")
	public Collection<Activity> getTokens() throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getTokens");
        qry.setEntity("flow", getFlow());
        return qry.list();
    }

    public int countTokens() throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.countTokens");
        qry.setEntity("flow", getFlow());
        return ((Long)qry.iterate().next()).intValue();
    }

    @SuppressWarnings("unchecked")
	public Collection<StepLog<Activity>> getLogSteps(Activity token, Date since) throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getLogStepsByToken");
        qry.setEntity("token", token);
        qry.setTimestamp("since", since);
        return qry.list();
    }

    @SuppressWarnings("unchecked")
	public Collection<StepLog<Activity>> getLogSteps(String stepId, Date since) throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getLogStepsByStep");
        qry.setEntity("flow", getFlow());
        qry.setString("stepId", stepId);
        qry.setTimestamp("since", since);
        return qry.list();
    }

    @SuppressWarnings("unchecked")
	public Collection<StepLog<Activity>> getLogSteps(Date since) throws StateException {
        Query qry = getSession().getNamedQuery("FlowState.getLogSteps");
        qry.setEntity("flow", getFlow());
        qry.setTimestamp("since", since);
        return qry.list();
    }

    @SuppressWarnings("unchecked")
	public Collection<StepLog<Activity>> getExitLogSteps(Activity token, String stepId) throws StateException {
		Query qry = getSession().getNamedQuery("FlowState.getExitLogSteps");
        qry.setEntity("token", token);
        qry.setString("stepId", stepId);
        Integer sStateInts[] = {
        		SubState.EXIT_STEP.ordinal(), 
        		SubState.EXIT_SWITCH.ordinal(),
        		SubState.EXIT_TIMER.ordinal() };
        qry.setParameterList("exitSubStates", sStateInts);
        return qry.list();
	}
}
