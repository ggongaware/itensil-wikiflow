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

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class HBStepLog extends StepLog<StatefulToken> implements Serializable {

    protected long id;
    protected String flowId;

    public HBStepLog() {
    }

    public HBStepLog(String flowId) {
        this.flowId = flowId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int hashCode() {
        return (int)id;
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return getId() == ((HBStepLog)o).getId();
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

}
