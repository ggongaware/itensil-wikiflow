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
package itensil.workflow.activities;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;
import itensil.workflow.activities.state.ActivityPlan;
import itensil.workflow.activities.state.ActivityStateStore;
import itensil.workflow.activities.state.ActivityStepState;
import itensil.workflow.activities.state.ActivityCurrentPlan;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.activities.state.FlowStepLog;
import itensil.workflow.state.StateException;
import itensil.workflow.state.StepLog;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityXML {

	protected static DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static DateFormat dateFmtZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    protected static DateFormat shortDateFmt = new SimpleDateFormat("yyyy-MM-dd");
    protected static DateFormat dateFmts[] = {dateFmtZ, dateFmt, shortDateFmt};
    static {
        dateFmtZ.setTimeZone(TimeZone.getTimeZone("GMT"));
        shortDateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static Element display(Activity activity) {
        Element elem = DocumentHelper.createElement("activity");
        elem.addAttribute("id", activity.getId());
        if (activity.getParent() != null) elem.addAttribute("parent", activity.getParent().getId());
        FlowState fstate = activity.getFlow();
        elem.addAttribute("flow", fstate.getId());
        try {
			elem.addAttribute("flowName", UriHelper.name(fstate.getNode().getUri()));
		} catch (Exception ex) { // eat it
		}
        String icon = "defIco";
        try {
        	String style = activity.getFlow().getNode().getPropertyValue(PropertyHelper.itensilQName("style"));
        	if (!Check.isEmpty(style) && style.startsWith("icon:"))
        		icon = style.substring(5);
        } catch (Exception ex) {}
        	
        elem.addAttribute("icon", icon);
        elem.addAttribute("name", activity.getName());
        elem.addAttribute("description", activity.getDescription());
        elem.addAttribute("activeKids", String.valueOf(activity.getActiveChildren().size()));
        elem.addAttribute("submitId", activity.getSubmitId());
        elem.addAttribute("contextGroup", activity.getContextGroupId());
        if (activity.getTimeStamp() != null) elem.addAttribute("timeStamp", dateFmtZ.format(activity.getTimeStamp()));
        elem.addAttribute("priority", String.valueOf(activity.getPriority()));
        elem.addAttribute("startDate", activity.getStartDate() != null ? dateFmtZ.format(activity.getStartDate()) : "");
        elem.addAttribute("dueDate", activity.getDueDate() != null ? dateFmtZ.format(activity.getDueDate()) : "");
        elem.addAttribute("duration", String.valueOf(activity.getDuration()));
        elem.addAttribute("variationId", activity.getVariationId());

        for (ActivityStepState state : activity.getStates().values()) {
        	Element stElem = elem.addElement("state");
            stElem.addAttribute("txId", state.getTxId());
            stElem.addAttribute("stepId", state.getStepId());
            stElem.addAttribute("assignId", state.getAssignId());
            stElem.addAttribute("subState", state.getSubState().toString());
            stElem.addAttribute("progress", String.valueOf(state.getProgress()));
            stElem.addAttribute("subActivityId", state.getSubActivityId());
            
            if (state.getTimeStamp() != null) stElem.addAttribute("timeStamp", dateFmtZ.format(state.getTimeStamp()));
            if (state.getExpireTime() != null) stElem.addAttribute("expireTime", dateFmtZ.format(state.getExpireTime()));
            
            stElem.addAttribute("userStatus", String.valueOf(state.getUserStatus()));
            
            ActivityCurrentPlan plan = state.getCurrentPlan();
            if (plan != null) {
                Element curElem = stElem.addElement("current");
                curElem.addAttribute("priority", String.valueOf(plan.getPriority()));
                curElem.addAttribute("startDate", plan.getStartDate() != null ? dateFmtZ.format(plan.getStartDate()) : "");
                curElem.addAttribute("dueDate", plan.getDueDate() != null ? dateFmtZ.format(plan.getDueDate()) : "");
                curElem.addAttribute("duration", String.valueOf(plan.getDuration()));
            }
            
        }
        return elem;
    }
    
    /**
     * 
     * @param activity
     * @return
     * @throws StateException 
     */
    public static Element logs(Activity activity, Pair<Date,Date> minMaxDates) throws StateException {
    	Element elem = DocumentHelper.createElement("logs");

    	ActivityStateStore store = new ActivityStateStore(activity.getFlow());
    	 
    	for (StepLog<Activity> slog : store.getLogSteps(activity, null)) {
    		FlowStepLog fslog = (FlowStepLog)slog;
    		Element lElem = elem.addElement("log");
    		lElem.addAttribute("stepId", Check.emptyIfNull(fslog.getStepId()));
    		lElem.addAttribute("subState", fslog.getSubState().toString());
    		if (fslog.getTimeStamp() != null) {
    			if (minMaxDates.first == null || minMaxDates.first.after(fslog.getTimeStamp())) {
    				minMaxDates.first = fslog.getTimeStamp();
    			}
    			if (minMaxDates.second == null || minMaxDates.second.before(fslog.getTimeStamp())) {
    				minMaxDates.second = fslog.getTimeStamp();
    			}
    			lElem.addAttribute("timeStamp", dateFmtZ.format(fslog.getTimeStamp()));
    		} else {
    			lElem.addAttribute("timeStamp", "");
    		}
    			
    		lElem.addAttribute("userId", fslog.getUserId());
    	}
    	
    	return elem;
    }
    
    /**
     * 
     * @param activity
     * @return
     */
    public static Element plans(Activity activity, Pair<Date,Date> minMaxDates) {
    	Element elem = DocumentHelper.createElement("plans");
    	for (ActivityPlan plan : activity.getPlans().values()) {
    		Element pElem = elem.addElement("plan");
    		pElem.addAttribute("priority", String.valueOf(plan.getPriority()));
    		if (plan.getStartDate() != null) {
    			if (minMaxDates.first == null || minMaxDates.first.after(plan.getStartDate())) {
    				minMaxDates.first = plan.getStartDate();
    			}
    			pElem.addAttribute("startDate", dateFmtZ.format(plan.getStartDate()));
    		} else {
    			pElem.addAttribute("startDate","");
    		}
    		if (plan.getDueDate() != null) {
    			if (minMaxDates.second == null || minMaxDates.second.before(plan.getDueDate())) {
    				minMaxDates.second = plan.getDueDate();
    			}
    			pElem.addAttribute("dueDate", dateFmtZ.format(plan.getDueDate()));
    		} else {
    			pElem.addAttribute("dueDate","");
    		}
    		pElem.addAttribute("duration", String.valueOf(plan.getDuration()));
    		pElem.addAttribute("stepId", Check.emptyIfNull(plan.getStepId()));
    		pElem.addAttribute("skip", plan.isSkip() ? "1" : "0");
    		pElem.addAttribute("assignId", Check.emptyIfNull(plan.getAssignId()));
    	}
    	return elem;
    }

    public static void main(String[] args) {
    	System.out.println("2007-01-15T16:30:00Z: " + dateFmtZ.format(ActivityXML.parseDate("2007-01-15T16:30:00Z")));
        System.out.println("2006-10-03: " + dateFmtZ.format(ActivityXML.parseDate("2006-10-03")));
    }

    public static Date parseDate(String dateStr) {
        if (Check.isEmpty(dateStr)) return null;
        Date dVal = null;
        for (DateFormat dateFormat : dateFmts) {
            try {
                dVal = dateFormat.parse(dateStr);
                if (dVal != null)
                    return dVal;
            } catch (ParseException e) {
                // eat it
            }
        }
        return dVal;
    }


	public static Element data(Activity act, Map<String, Integer> columnMap) {
		Element elem = DocumentHelper.createElement("data");
		String flowId = act.getFlow().getId();
		mapColumn(act.getCust0Val(), "0", flowId, elem, columnMap);
		mapColumn(act.getCust1Val(), "1", flowId, elem, columnMap);
		mapColumn(act.getCust2Val(), "2", flowId, elem, columnMap);
		mapColumn(act.getCust3Val(), "3", flowId, elem, columnMap);
		mapColumn(act.getCust4Val(), "4", flowId, elem, columnMap);
		mapColumn(act.getCust5Val(), "5", flowId, elem, columnMap);
		mapColumn(act.getCust6Val(), "6", flowId, elem, columnMap);
		mapColumn(act.getCust7Val(), "7", flowId, elem, columnMap);
		mapColumn(act.getCust8Val(), "8", flowId, elem, columnMap);
		mapColumn(act.getCust9Val(), "9", flowId, elem, columnMap);
		mapColumn(act.getCustAVal(), "A", flowId, elem, columnMap);
		mapColumn(act.getCustBVal(), "B", flowId, elem, columnMap);
		return elem;
	}
	
	public static void updatePlans(
			Element pups, Activity activity, UserActivities uacts, MutableRepositoryNode permNode) 
			throws UserSpaceException, AccessDeniedException {
		
		boolean saveAct = false;
		for (Object obj : pups.elements("update")) {
			Element upElem = (Element)obj;
			String stepId = upElem.attributeValue("step");
			String assign = upElem.attributeValue("assign");
			if (assign != null) {
				uacts.assignStep(activity, stepId, assign, permNode);
			}
			String startDate = upElem.attributeValue("startDate");
			String dueDate = upElem.attributeValue("dueDate");
			
			if (startDate != null || dueDate != null) {
				saveAct = uacts.setStepDates(activity, stepId,
						startDate.length() > 0 ? parseDate(startDate) : null,
						dueDate.length() > 0 ? parseDate(dueDate) : null) || saveAct;
			}
		}
		
		if (saveAct) HibernateUtil.getSession().saveOrUpdate(activity);
		
	}
	
	private static void mapColumn(
			String colVal, String colIdx, String flowId, Element parent,  Map<String, Integer> columnMap) {
		
		Integer mapInt = columnMap.get(flowId + colIdx);
		if (colVal != null && mapInt != null) {
			Element valElem = parent.addElement("val" + mapInt);
			valElem.setText(colVal);
		}
	}


}
