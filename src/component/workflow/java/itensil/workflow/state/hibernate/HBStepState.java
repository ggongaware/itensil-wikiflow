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

import itensil.workflow.state.SubState;
import itensil.workflow.state.StepState;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class HBStepState extends StepState<StatefulToken> implements Serializable {

    protected String flowId;

    public HBStepState() {
    }

    public HBStepState(String flowId) {
        this.flowId = flowId;
    }

    public int getSubStateInt() {
        return subState.ordinal();
    }

    public void setSubStateInt(int subState) {
        this.subState = SubState.values()[subState];
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getTokenId() {
        return getToken().getId();
    }

    public void setTokenId(String tokenId) {
        setToken(new StatefulToken(tokenId));
    }

    public boolean equals(Object o) {
        if (o instanceof HBStepState) {
            return getFlowId().equals(((HBStepState)o).getFlowId()) && super.equals(o);
        }
        return false;
    }
}
