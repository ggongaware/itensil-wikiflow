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

import itensil.workflow.state.Token;
import itensil.workflow.state.StepState;

import java.util.Set;
import java.util.HashSet;

/**
 * @author ggongaware@itensil.com
 *
 */
public class StatefulToken extends Token {

    protected Set<StepState<StatefulToken>> states = new HashSet<StepState<StatefulToken>>();

    public StatefulToken(String id) {
        super(id);
    }

    public Set<StepState<StatefulToken>> getStates() {
        return states;
    }

    public void setStates(Set<StepState<StatefulToken>> states) {
        this.states = states;
    }

}
