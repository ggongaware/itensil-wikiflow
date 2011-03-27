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

import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class StepLog<Tk extends Token> implements Comparable {

    protected Tk token;
    protected String txId;
    protected String stepId;
    protected SubState subState;
    protected Date timeStamp;
    protected Date expireTime;

    public StepLog() {
    }
    
    public void fromState(StepState<Tk> state) {
        stepId = state.getStepId();
        token = state.getToken();
        txId = state.getTxId();
        subState = state.getSubState();
        timeStamp = state.getTimeStamp();
        expireTime = state.getExpireTime();
    }

    public Tk getToken() {
        return token;
    }

    public void setToken(Tk token) {
        this.token = token;
    }

    /**
     * Null if transactions not support by StateStore
     * @return transaction id
     */
    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public SubState getSubState() {
        return subState;
    }

    public void setSubState(SubState subState) {
        this.subState = subState;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final StepLog stepState = (StepLog) o;

        if (!stepId.equals(stepState.stepId)) return false;
        return token.equals(stepState.token);
    }

    public int hashCode() {
        return 29 * token.hashCode() + stepId.hashCode();
    }

    public int compareTo(Object o) {
        if (o instanceof StepLog) {
            return this.getTimeStamp().compareTo(((StepLog) o).getTimeStamp());
        }
        return 0;
    }
}
