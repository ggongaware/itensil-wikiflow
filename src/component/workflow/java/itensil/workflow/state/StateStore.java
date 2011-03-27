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

import java.util.Collection;
import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface StateStore<Tk extends Token> {


    /**
     * Are state transitions being logged
     * Also, no logging means no transactions and rollback
     * @return true if logging supported and active
     */
    public boolean isLogging();

    /**
     * Can a Token be in more than one step at time?
     * @return true if multi supported and active
     */
    public boolean isMultiStep();

    /**
     * StepState factory
     * @return new state object
     */
    public StepState<Tk> createStepState();

    /**
     * StepLog factory
     * @return new log object
     */
    public StepLog<Tk> createStepLog();

    /**
     * By Token
     * @param token
     * @return currently active steps
     * @throws StateException
     */
    public Collection<? extends StepState<Tk>> getActiveSteps(Tk token) throws StateException;

    /**
     * Called by runner before reading/writing states
     * @param token
     * @throws StateException
     */
    public void activateToken(Tk token) throws StateException;

    /**
     * Called by runner after reading/writing all states for an event
     * @param token
     * @throws StateException
     */
    public void passivateToken(Tk token) throws StateException;

    /**
     * By Sub State
     * @param sStates
     * @return all tokens currently at these sub states
     * @throws StateException
     */
    public Collection<StepState<Tk>> getActiveSteps(SubState sStates[]) throws StateException;

    /**
     *
     * @param state
     * @throws StateException
     */
    public void addActiveStep(StepState<Tk> state) throws StateException;

    /**
     *
     * @param state
     * @throws StateException
     */
    public void updateActiveStep(StepState<Tk> state) throws StateException;

    /**
     *
     * @param state
     * @throws StateException
     */
    public void removeActiveStep(StepState<Tk> state) throws StateException;

    /**
     *
     * @throws StateException
     */
    public void clearAllTokens() throws StateException;


    /**
     * @throws StateException
     */
    public void addToken(Tk token) throws StateException;


    /**
     * @throws StateException
     */
    public void removeToken(Tk token) throws StateException;


    /**
     * (optional) if supporting transactions
     * @return transaction id
     */
    public String generateTx();

    /**
     * User log to rollback to prevoius states for a token
     * (optional) if supporting transactions
     * @param token
     * @throws StateException
     */
    public void rollbackTx(Tk token, String txId) throws StateException;

    /**
     * (optional) if logging state
     * @param log
     * @throws StateException
     */
    public void addLogStep(StepLog<Tk> log) throws StateException;
    
    
    /**
     * By Token, sorted by timeStamp ascending
     * (optional) if logging state
     * @param token
     * @param stepId
     * @return log entries
     * @throws StateException
     */
    public Collection<StepLog<Tk>> getExitLogSteps(Tk token, String stepId) throws StateException;
    

    /**
     * @return the current flow id
     */
    public String getFlowId();

}