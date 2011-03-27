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
package itensil.workflow.track;

import itensil.workflow.state.Token;
import itensil.workflow.rules.ConditionEval;
import itensil.workflow.rules.EvalException;
import itensil.workflow.model.element.Condition;
import itensil.workflow.FlowEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author ggongaware@itensil.com
 *
 */
public class TrackingManualEval<Tk extends Token, Dt> implements ConditionEval<Tk, Dt> {

    HashMap<Tk, List<Condition>> pending = new HashMap<Tk, List<Condition>>();

    public boolean isAsync() {
        return true;
    }

    public String evalExclusive(String flowId, Condition[] conditions, FlowEvent<Tk, Dt> evt) throws EvalException {
        List<Condition> conds = pending.get(evt.getToken());
        if (conds == null) {
            conds = new ArrayList<Condition>();
            pending.put(evt.getToken(), conds);
        }
        for (Condition cond : conditions) {
            conds.add(cond);
        }
        return null;
    }

    public String[] evalInclusive(String flowId, Condition[] conditions, FlowEvent<Tk, Dt> evt) throws EvalException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearPendingConditions(Tk tok) {
        pending.remove(tok);
    }

    public Collection<Condition> getPendingConditions(Tk tok) {
        return pending.get(tok);
    }
}
