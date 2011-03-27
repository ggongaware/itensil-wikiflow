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

import itensil.workflow.model.element.ActivityStep;
import itensil.workflow.model.element.End;
import itensil.workflow.state.Token;
import itensil.workflow.FlowEvent;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface ActivityStepInvoker<Tk extends Token, Dt> {


    /**
     *
     * @param step
     * @return true on enter ok, false to wait and retry
     */
    public ActivityStepResult activityEnter(String flowId, ActivityStep step, FlowEvent<Tk, Dt> evt)
            throws ActivityStepException;

    /**
     *
     * @param step
     * @return true on exit ok, false to wait and retry
     */
    public ActivityStepResult activityExit(String flowId, ActivityStep step, FlowEvent<Tk, Dt> evt)
            throws ActivityStepException;


    /**
     * 
     * @param flowId
     * @param step
     * @param evt
     * @throws ActivityStepException
     */
    public void enterEnd(String flowId, End step, FlowEvent<Tk, Dt> evt)
    	throws ActivityStepException;

}
