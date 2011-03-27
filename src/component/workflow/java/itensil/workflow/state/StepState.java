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
public class StepState<Tk extends Token> {

    protected Tk token;
    protected String txId;
    protected String stepId;
    protected SubState subState;
    protected Date timeStamp;
    protected Date expireTime;

    public StepState() {
        timeStamp = new Date();
        subState = SubState.ENTER_STEP;
    }

    public StepState(String stepId, Tk token) {
        super();
        this.token = token;
        this.stepId = stepId;
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

        final StepState stepState = (StepState) o;

        if (!stepId.equals(stepState.stepId)) return false;
        return token.equals(stepState.token);
    }

    public int hashCode() {
        return 29 * token.hashCode() + stepId.hashCode();
    }
}
