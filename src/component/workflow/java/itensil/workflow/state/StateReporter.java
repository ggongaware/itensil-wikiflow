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
import java.util.Map;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface StateReporter<Tk extends Token> {

    /**
     * By Step
     * @param stepId
     * @return all tokens currently at this step
     * @throws StateException
     */
    public Collection<StepState<Tk>> getActiveSteps(String stepId) throws StateException;

    /**
     *
     * @param stepId
     * @return count of all token in this step
     * @throws StateException
     */
    public int countStepTokens(String stepId) throws StateException;


    /**
     *
     * @return
     * @throws StateException
     */
    public Map<String, Integer> countBySteps() throws StateException;

    /**
     *
     * @return all acvtive steps
     * @throws StateException
     */
    public Collection<StepState<Tk>> getAllActiveSteps() throws StateException;

    /**
     *
     * @return count of all token + steps
     * @throws StateException
     */
    public int countAllActiveSteps() throws StateException;


     /**
     *
     * @return all tokens in flowId
     * @throws StateException
     */
    public Collection<Tk> getTokens() throws StateException;

    /**
     *
     * @return cound of all tokens
     * @throws StateException
     */
    public int countTokens() throws StateException;

    /**
     * By Step, sorted by timeStamp ascending
     * (optional) if logging state
     * @param stepId
     * @param since null for all time
     * @return log entries
     * @throws StateException
     */
    public Collection<StepLog<Tk>> getLogSteps(String stepId, Date since) throws StateException;
    
    /**
     * By Token, sorted by timeStamp ascending
     * (optional) if logging state
     * @param token
     * @param since null for all time
     * @return log entries
     * @throws StateException
     */
    public Collection<StepLog<Tk>> getLogSteps(Tk token, Date since) throws StateException;

    /**
     * All steps and tokens
     * @param since
     * @return
     * @throws StateException
     */
    public Collection<StepLog<Tk>> getLogSteps(Date since) throws StateException;


}
