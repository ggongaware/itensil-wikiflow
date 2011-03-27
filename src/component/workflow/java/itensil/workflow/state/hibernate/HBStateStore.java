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
package itensil.workflow.state.hibernate;

import itensil.workflow.state.*;
import itensil.io.HibernateUtil;
import itensil.uidgen.IUIDGenerator;

import java.util.*;

import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.HibernateException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class HBStateStore implements StateStore<StatefulToken> {

    private static IUIDGenerator txGen = new IUIDGenerator();

    String flowId;
    boolean logging;
    boolean halfLog;

    public HBStateStore(String flowId, boolean logging, boolean halfLog) {
        this.flowId = flowId;
        this.logging = logging;
        this.halfLog = halfLog;
    }

    protected void saveStates(StatefulToken token) throws StateException {
        //removeToken(token);
        if (!token.getStates().isEmpty()) {
            getSession().save(token.getStates().iterator().next());
        }
    }


    protected Set<StepState<StatefulToken>> loadStates(StatefulToken token) throws StateException {
        HashSet<StepState<StatefulToken>> states = new HashSet<StepState<StatefulToken>>(1);
        Query qry = getSession().getNamedQuery("StepState.loadStates");
        qry.setString("flowId", getFlowId());
        qry.setString("tokenId", token.getId());

        List hStates = qry.list();
        for (Object os : hStates) {
            HBStepState state = (HBStepState)os;
            state.setToken(token); // set to single token object
            states.add(state);
        }
        return states;
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isMultiStep() {
        return false;
    }

    public StepState<StatefulToken> createStepState() {
        return new HBStepState(getFlowId());
    }

    public StepLog<StatefulToken> createStepLog() {
        return new HBStepLog(getFlowId());
    }

    public Collection<StepState<StatefulToken>> getActiveSteps(SubState[] sStates) throws StateException {

        Integer sStateInts[] = new Integer[sStates.length];
        for (int ii = 0; ii < sStates.length; ii++) sStateInts[ii] = sStates[ii].ordinal();
        Query qry = getSession().getNamedQuery("StepState.getActiveSteps");
        qry.setString("flowId", getFlowId());
        qry.setParameterList("subStates", sStateInts);
        List hStates = qry.list();

        ArrayList<StepState<StatefulToken>> states = new ArrayList<StepState<StatefulToken>>(hStates.size());
        for (Object os : hStates) {
            states.add((HBStepState)os);
        }
        return states;
    }

    public void clearAllTokens() throws StateException {
        Query qry = getSession().getNamedQuery("StepState.clearAllTokens");
        qry.setString("flowId", getFlowId());
        qry.executeUpdate();
    }

    public void addToken(StatefulToken token) throws StateException {
        removeToken(token); // clean slate
        token.setStates(new HashSet<StepState<StatefulToken>>(1));
    }

    public void removeToken(StatefulToken token) throws StateException {
        Session sess = getSession();
        Query qry = sess.getNamedQuery("StepState.removeToken");
        qry.setString("flowId", getFlowId());
        qry.setString("tokenId", token.getId());
        qry.executeUpdate();
    }

    public String generateTx() {
        return txGen.createID().toString();
    }

    /**
     * This implementations assumes log ids always count up
     * @param token
     * @throws StateException
     */
    public void rollbackTx(StatefulToken token, String txId) throws StateException {
    	throw new StateException("Rollback implementation for HBStateStore not implemented");
    }

    public void addLogStep(StepLog<StatefulToken> log) throws StateException {
        if (!halfLog || log.getSubState().isExit())
            getSession().save(log);
    }

    protected Session getSession() throws StateException {
        try {
            return HibernateUtil.getSession();
        } catch (HibernateException he) {
            throw new StateException("Error starting Hibernate", he);
        }
    }

    public String getFlowId() {
        return flowId;
    }

    public void passivateToken(StatefulToken token) throws StateException {
        saveStates(token);
    }

    public void activateToken(StatefulToken token) throws StateException {
        if (token.getStates() == null)
            token.setStates(loadStates(token));
    }

    public Collection<StepState<StatefulToken>> getActiveSteps(StatefulToken token) throws StateException {
        return token.getStates();
    }

    public void addActiveStep(StepState<StatefulToken> state) throws StateException {
        state.getToken().getStates().add(state);
    }

    public void updateActiveStep(StepState<StatefulToken> state) throws StateException {
        Session sess = getSession();
        if (sess.contains(state)) {
            sess.saveOrUpdate(state);
        }
    }

    public void removeActiveStep(StepState<StatefulToken> state) throws StateException {
        state.getToken().getStates().remove(state);
        Session sess = getSession();
        if (sess.contains(state)) sess.delete(state);
    }

	public Collection<StepLog<StatefulToken>> getExitLogSteps(StatefulToken token, String stepId) throws StateException {
		// TODO Auto-generated method stub
		return Collections.emptyList();
	}

}
