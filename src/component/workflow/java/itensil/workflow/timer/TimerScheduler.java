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
package itensil.workflow.timer;

import itensil.workflow.model.element.Timer;
import itensil.workflow.state.Token;
import itensil.workflow.state.StateException;
import itensil.workflow.state.StepState;

import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface TimerScheduler<Tk extends Token> {

    /**
     *
     * @param state
     * @throws StateException
     */
    public void trackExpire(StepState<Tk> state) throws StateException;

    /**
     *
     * @param state
     * @throws StateException
     */
    public void clearExpire(StepState<Tk> state) throws StateException;

    /**
     *
     * @param timer
     * @param token
     * @throws itensil.workflow.state.StateException
     */
    public void setTimer(Timer timer, Tk token, Date fromTime) throws StateException;

    /**
     *
     * @param timer
     * @param token
     * @throws StateException
     */
    public void clearTimer(Timer timer, Tk token) throws StateException;
    
    /**
     *
     * @param timer
     * @param token
     * @throws itensil.workflow.state.StateException
     */
    public void setManualTimer(Timer timer, Tk token, Date atTime) throws StateException;

    /**
     *
     * @throws StateException
     */
    public void clearAllTimers() throws StateException;

    /**
     * @return the current flow id
     */
    public String getFlowId();
}
