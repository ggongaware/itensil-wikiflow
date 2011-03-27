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
package itensil.workflow.rules;

import itensil.workflow.model.element.Condition;
import itensil.workflow.state.Token;
import itensil.workflow.FlowEvent;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface ConditionEval<Tk extends Token, Dt> {

    /**
     *
     * @return is this implementation asynchronous?
     */
    public boolean isAsync();

    /**
     * 
     * @param contions - last contion may be null
     * @return statifying returnId
     */
    public String evalExclusive(String flowId, Condition [] conditions,  FlowEvent<Tk,Dt> evt)
            throws EvalException;


    /**
     * 
     * @param contions - last contion may be null
     * @return statifying returnId(s)
     */
    public String [] evalInclusive(String flowId, Condition [] conditions,  FlowEvent<Tk,Dt> evt)
            throws EvalException;

}
