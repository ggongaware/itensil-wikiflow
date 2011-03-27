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

import itensil.workflow.state.*;

import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.dom4j.Element;
import org.dom4j.Branch;


/**
 * @author ggongaware@itensil.com
 *
 */
public class TrackingXML<Tk extends Token> {

    protected StateReporter<Tk> stateReport;
    protected StateStore<Tk> stateStore;
    protected static DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public TrackingXML(StateReporter<Tk> stateReport, StateStore<Tk> stateStore) {
        this.stateReport = stateReport;
        this.stateStore = stateStore;
    }

    public void appendStepCounts(Branch parent) throws StateException {
        Element setElem = parent.addElement("step-counts");
        for (Map.Entry<String,Integer> sc : stateReport.countBySteps().entrySet()) {
            Element elem = setElem.addElement("count");
            elem.addAttribute("id", sc.getKey());
            elem.addText(sc.getValue().toString());
        }
    }

    public void appendAllActiveSteps(Branch parent)  throws StateException {
        for (StepState<Tk> state : stateReport.getAllActiveSteps()) {
           addStateAttributes(parent.addElement("state"), state);
        }
    }

    protected void addStateAttributes(Element elem, StepState<Tk> state) {
        elem.addAttribute("id", state.getStepId());
        elem.addAttribute("token", state.getToken().getId());
        elem.addAttribute("subState", state.getSubState().name());
        elem.addAttribute("stamp", dateFmt.format(state.getTimeStamp()));
        elem.addAttribute("expires", dateFmt.format(state.getTimeStamp()));
    }

    public void appendActiveSteps(Tk token, Branch parent) throws StateException {
        for (StepState<Tk> state : stateStore.getActiveSteps(token)) {
             addStateAttributes(parent.addElement("state"), state);
        }
    }

}
