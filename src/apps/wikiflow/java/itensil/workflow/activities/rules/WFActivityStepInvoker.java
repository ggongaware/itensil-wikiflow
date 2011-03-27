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
package itensil.workflow.activities.rules;

import itensil.workflow.rules.ActivityStepInvoker;
import itensil.workflow.rules.ActivityStepResult;
import itensil.workflow.rules.ActivityStepException;
import itensil.workflow.rules.EvalException;
import itensil.workflow.state.SubState;
import itensil.workflow.model.BasicElement;
import itensil.workflow.model.element.ActivityStep;
import itensil.workflow.model.element.End;
import itensil.workflow.model.element.Script;
import itensil.workflow.model.element.Start;
import itensil.workflow.FlowEvent;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.state.*;
import itensil.workflow.activities.signals.AlertSignalImpl;
import itensil.workflow.activities.signals.SignalImpl;
import itensil.workflow.activities.signals.SignalManager;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodePermission;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.scripting.JSActivity;
import itensil.scripting.JSAuthUser;
import itensil.scripting.JSEntities;
import itensil.scripting.JSFiles;
import itensil.scripting.JSQuery;
import itensil.scripting.ScriptError;
import itensil.scripting.ScriptHost;
import itensil.scripting.util.JSDomData;
import itensil.scripting.util.JSWebService;
import itensil.security.DefaultGroup;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.io.HibernateUtil;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Document;

/**
 * @author ggongaware@itensil.com
 *
 */
public class WFActivityStepInvoker<Dt> implements ActivityStepInvoker<Activity, Dt> {
	
	protected static Logger logger = Logger.getLogger(WFActivityStepInvoker.class);

    public ActivityStepResult activityEnter(String flowId, ActivityStep step, FlowEvent<Activity, Dt> evt)
        throws ActivityStepException {

        // assignment via roles
        String assignId = null;
        Activity itm = evt.getToken();
        
        ActivityStepState state = itm.getStates().get(step.getId());

        // 1st: plan
        ActivityPlan plan = itm.getPlans().get(step.getId());
        if (plan != null) {
            // check plan for skip
            if (plan.isSkip()) {
                return ActivityStepResult.EXIT_STEP_SAME_TX;
            }

            ActivityCurrentPlan cp = state.getCurrentPlan();
            if (cp == null) {
                cp = new ActivityCurrentPlan();
                cp.setState(state);
                state.setCurrentPlan(cp);
            }
            cp.setDueDate(plan.getDueDate());
            cp.setStartDate(plan.getStartDate());
            cp.setPriority(plan.getPriority());
            cp.setDuration(plan.getDuration());

            assignId = plan.getAssignId();

        } else {
        	state.setCurrentPlan(null);
        }
        
        

        String role = step.getAttribute("role");

        if (!Check.isEmpty(role)) {

            // 2nd: item role
            if (Check.isEmpty(assignId)) {
                ActivityRole iRole = itm.getRoles().get(role);
                if (iRole != null) assignId = iRole.getAssignId();
            }

            // 3rd: flow role
            if (Check.isEmpty(assignId)) {
                FlowRole fRole = itm.getFlow().getRoles().get(role);
                if (fRole != null) assignId = fRole.getAssignId();
            }
        }
        
        
        // org check
        if (Check.isEmpty(assignId)) {
	        String orgPosition = step.getAttribute("orgPosition");
	        if (!Check.isEmpty(orgPosition)) {
	        	String orgAxis = step.getAttribute("orgAxis");
	        	GroupAxis gax = Check.isEmpty(orgAxis) ? GroupAxis.SELF : GroupAxis.valueOf(orgAxis);
	        	UserSpace uspace = SecurityAssociation.getUser().getUserSpace();
				try {
					String ctxGrpId = itm.getContextGroupId();
					Set<? extends User> posUsers = null;
					if (Check.isEmpty(ctxGrpId)) {
						User submitUser = uspace.getUser(itm.getSubmitId());
			        	for (Group grp : submitUser.getGroups()) {
			        		posUsers = uspace.findGroupRoleUsers(grp, gax, orgPosition);
			        		if (!posUsers.isEmpty()) break;
			        	}
					} else {
						posUsers = uspace.findGroupRoleUsers(new DefaultGroup(ctxGrpId), gax, orgPosition);
					}
		        	if (posUsers != null && !posUsers.isEmpty()) {
		        		assignId = posUsers.iterator().next().getUserId();
		        		if (!Check.isEmpty(role)) {
		        			ActivityRole roleEnt = itm.getRoles().get(role);
		        	        if (roleEnt == null) {
		        	            roleEnt = new ActivityRole();
		        	            roleEnt.setActivity(itm);
		        	            roleEnt.setRole(role);
		        	            roleEnt.setAssignId(assignId);
		        	            itm.getRoles().put(role, roleEnt);
		        	        }
		        	        HibernateUtil.getSession().saveOrUpdate(roleEnt);
		        		}
		        	}
				} catch (UserSpaceException ue) {
					logger.warn("Problem reading orgs", ue);
				}
	        }
        }
       

        // last back to submitter
        if (Check.isEmpty(assignId)) assignId = itm.getSubmitId();
        
        // Check for sub-process launch
        if ("launch".equals(step.getAttribute("apptype"))) {
        	state.setAssignId(assignId);
        	
        	String flowUri = step.getAttribute("flow");
        	if (!Check.isEmpty(flowUri)) {
        		
        		try {
        			// set submitter as the step assign
	        		User launchUser = SecurityAssociation.getUser().getUserSpace().getUser(assignId);
	        		UserActivities uacts = new UserActivities(launchUser, HibernateUtil.getSession());
	        		
	        		// launch
	        		Activity subAct = uacts.launch(UriHelper.name(step.getId()), "", flowUri, null, itm.getId(), null,
	        				itm.getContextGroupId(), plan != null ? plan.getDueDate() : null, 
	        				new HashMap<String,String>());
	        		
	        		// ensure submitters permission
	        		if (!SecurityAssociation.getUser().getUserId().equals(launchUser.getUserId())) {
	        			MutableRepositoryNode repoNode = (MutableRepositoryNode)subAct.getNode();
	        			repoNode.grantPermission(DefaultNodePermission.managePermission(launchUser));
	        		}
	        		
	        		// tie to step state
	        		state.setSubActivityId(subAct.getId());
	        		
        		} catch (Exception ex) {
					throw new ActivityStepException("Sub-process launch error:", ex);
				}
        	}
        	
        } else if (assignId != null) {
        	
        	// Alert process
	        boolean isSameAssign = assignId.equals(state.getAssignId());
	        state.setAssignId(assignId);
	        boolean isStart = step.getOwner().getStep(evt.getStepId()) instanceof Start;
	        
	        SignalImpl sig = new AlertSignalImpl();
	        sig.setActivity(itm);
	        sig.setStepId(step.getId());
	        sig.setAssignId(assignId);

	        // don't resend if the person is already online
	        sig.setMailed(isSameAssign || (isStart && assignId.equals(itm.getSubmitId())) ? SignalManager.SIGNAL_STATUS_ACTIVE_SENT : SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
	        sig.setTimeStamp(new Date());
	        sig.setRole(role);

	        SignalManager.saveOrUpdateSignal(sig);
        }
        

        // look for child script elements with <script on="enter">
        boolean automated = false;
        for (BasicElement kid : step.getChildren()) {
        	if (kid instanceof Script) {
        		Script scrEl = (Script)kid;
        		if (scrEl.getOn() == Script.ON.enter) {
        			automated = true;
        			executeScript(itm, scrEl);
        		}
        	}
        }
        
        return automated ? ActivityStepResult.EXIT_STEP_SAME_TX : ActivityStepResult.DONE;
    }

    public ActivityStepResult activityExit(String flowId, ActivityStep step, FlowEvent<Activity, Dt> evt)
            throws ActivityStepException {

    	// look for child script elements with <script on="exit">
    	for (BasicElement kid : step.getChildren()) {
    		if (kid instanceof Script) {
	    		Script scrEl = (Script)kid;
	    		if (scrEl.getOn() == Script.ON.exit) {
	    			executeScript(evt.getToken(), scrEl);
	    		}
    		}
    	}
        return ActivityStepResult.DONE;
    }
    

    /**
     * Execute Javascript
     * 
     * @param activity
     * @param scrElem
     * @throws ActivityStepException
     */
    protected void executeScript(Activity activity, Script scrElem) throws ActivityStepException {
    	
    	// trim up the source, make sure there is some
    	String src = scrElem.getInnerText();
    	if (src != null) src = src.trim();
    	if (Check.isEmpty(src)) return;
    	
    	Document ddoc;
    	try {
			ddoc = XPathConditionEval.getDefaultData(activity, scrElem.getOwner());
		} catch (EvalException eve) {
			throw new ActivityStepException(eve);
		}
		ScriptHost scr;
		try {
			User usr = SecurityAssociation.getUser();
			scr = new ScriptHost("Activity:" + activity.getName(), new JSAuthUser(usr));
			scr.defineObject("activity", new JSActivity(activity, true));
			scr.defineObject("ws", new JSWebService());
			
			String uri = "";
			try {
				// URI not available in all Junits
				uri = activity.getNode().getUri();
			} catch (Exception ex) { // eat it
			}
			JSFiles files = new JSFiles(uri, scr);
			scr.defineObject("data", new JSDomData(ddoc, UriHelper.absoluteUri(uri, "rules.xml")));
			scr.defineObject("files", files);
			scr.defineObject("entities", new JSEntities(usr));
			scr.defineObject("query", new JSQuery(files));
			
		} catch (Exception ex) {
			throw new ActivityStepException(ex);
		}
        
        try {
			scr.evaluate(src);
		} catch (ScriptError se) {
			throw new ActivityStepException(se);
		}
    }

    /**
     * Checks for parent process link
     */
	public void enterEnd(String flowId, End step, FlowEvent<Activity, Dt> evt) throws ActivityStepException {
		
		/*
		 * Change, by request... not automatic, leave the parent step up for inspection
		 * 
		Activity act = evt.getToken();
		Activity parent = act.getParent();
		if (parent != null) {
			// find a potentially tied parent step
			for (ActivityStepState state : parent.getStates().values()) {
				if (act.getId().equals(state.getSubActivityId())) {
	        		UserActivities uacts = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
	        		try {
	        			uacts.submit(parent, state.getStepId(), null, null);
					} catch (Exception ex) {
						throw new ActivityStepException("Parent process submit error:", ex);
					}
					return;
				}
			}
		}*/
	}
	
}
