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

import itensil.workflow.state.Token;

import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class FlowEvent<Tk extends Token, Dt> {

    protected Tk token;
    protected String stepId;
    protected Dt eventData;
    protected Object result;
    protected Date time;

    public FlowEvent(Tk token, String stepId) {
        this.token = token;
        this.stepId = stepId;
        time = new Date();
    }

    public Tk getToken() {
        return token;
    }

    public String getStepId() {
        return stepId;
    }

    public Date getTime() {
        return time;
    }

    public Dt getEventData() {
        return eventData;
    }

    public void setEventData(Dt eventData) {
        this.eventData = eventData;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

}
