package itensil.scripting;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.RepositoryHelper;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.workflow.RunException;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityPlan;
import itensil.workflow.activities.state.ActivityRole;
import itensil.workflow.activities.state.ActivityStateStore;
import itensil.workflow.activities.state.ActivityStepState;
import itensil.workflow.rules.ActivityStepException;
import itensil.workflow.rules.EvalException;
import itensil.workflow.state.StateException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.xml.sax.SAXException;

public class JSActivity extends ScriptableObject {

	Activity act;
	boolean context;
	
	public JSActivity(Activity act, boolean context) {
		this.act = act;
		String funcs[] = {
				"isContext",
				"launch",
				"setRole",
				"setStepDates",
				"assignStep",
				"submitStep",
				"undo",
				"save"
				};
		try {
			this.defineFunctionProperties(
		            funcs,
		            JSActivity.class,
		            ScriptableObject.PERMANENT |
		            ScriptableObject.READONLY);
			
			this.defineProperty("id", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			this.defineProperty("name", JSActivity.class, ScriptableObject.PERMANENT);
			this.defineProperty("description", JSActivity.class, ScriptableObject.PERMANENT);
			this.defineProperty("submitId", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			this.defineProperty("uri", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			this.defineProperty("contextGroupId", JSActivity.class, ScriptableObject.PERMANENT);
			this.defineProperty("startDate", JSActivity.class, ScriptableObject.PERMANENT);
			this.defineProperty("dueDate", JSActivity.class, ScriptableObject.PERMANENT);
			this.defineProperty("roles", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			this.defineProperty("states", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			this.defineProperty("plans", JSActivity.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
			
		} catch (RhinoException e) {
	        e.printStackTrace();
		}
	}
	
	public String getClassName() {
		return "JSActivity";
	}
	
	public String getId() {
		return act == null ? null : act.getId();
	}
	
	public String getName() {
		return act == null ? null : act.getName();
	}
	
	public void setName(String val) {
		if (act != null) act.setName(val);
	}
	
	public String getDescription() {
		return act == null ? null : act.getDescription();
	}
	
	public void setDescription(String val) {
		if (act != null) act.setDescription(val);
	}
	
	public String getSubmitId() {
		return act == null ? null : act.getSubmitId();
	}
	
	public String getUri() throws NotFoundException, AccessDeniedException {
		return act == null ? null : act.getNode().getUri();
	}

	public boolean isContext() {
		return context;
	}
	
	public JSActivity launch(
			String name, 
			Object description, 
			String flowUri, 
			Object masterUri, 
			Object parentActId, 
			Object projectId, 
			Object contextGroupId, 
			Object dueDate,
			Object oroles) 
				throws 
					AccessDeniedException, 
					IOException,
					SAXException, 
					LockException, 
					NotFoundException, 
					DuplicateException, 
					StateException, 
					EvalException, 
					RunException, 
					ActivityStepException, 
					UserSpaceException {
		
		UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
		HashMap<String,String> roles = new HashMap<String,String>();
		if (Context.toBoolean(oroles)) {
    		Scriptable jsroles = (Scriptable)oroles;
	    	Object rids[] = jsroles.getIds();
	        for (int ii = 0; ii < rids.length; ii++) {
	            String rid = rids[ii].toString();
	            roles.put(rid, Context.toString(jsroles.get(rid, jsroles)));
	        }
    	}
		Date dueDateObj = null;
		if (dueDate != null) {
			dueDateObj = ActivityXML.parseDate(Context.toString(dueDate));
		}
		
		return new JSActivity(
				uActs.launch(name, 
						description != null ? Context.toString(description) : null, 
						RepositoryHelper.resolveUri(flowUri), 
						masterUri != null ? RepositoryHelper.resolveUri(Context.toString(masterUri)) : null, 
						parentActId != null ? Context.toString(parentActId) : null, 
						projectId != null ? Context.toString(projectId) : null, 
						contextGroupId != null ? Context.toString(contextGroupId) : null, 
						dueDateObj, 
						roles), 
				false);
	}
	
	
	
	
	public String getContextGroupId() {
		return act == null ? null : act.getContextGroupId();
	}

	public void setContextGroupId(String contextGroupId) {
		if (act != null) act.setContextGroupId(contextGroupId);
	}

	public String getStartDate() {
		return act == null || act.getStartDate() == null ? null : ActivityXML.dateFmtZ.format(act.getStartDate());
	}

	public void setStartDate(Object startDate) {
		Date startDateObj = null;
		if (startDate != null) {
			startDateObj = ActivityXML.parseDate(Context.toString(startDate));
		}
		if (act != null) {
			act.setStartDate(startDateObj);
		}
	}

	
	public String getDueDate() {
		return act == null || act.getDueDate() == null ? null : ActivityXML.dateFmtZ.format(act.getDueDate());
	}

	public void setDueDate(Object dueDate) {
		Date dueDateObj = null;
		if (dueDate != null) {
			dueDateObj = ActivityXML.parseDate(Context.toString(dueDate));
		}
		if (act != null) {
			act.setDueDate(dueDateObj);
		}
	}

	public boolean setRole(String role, String userId) throws 
			AccessDeniedException, UserSpaceException, NotFoundException, IOException, SAXException, LockException {
		if (act != null) {
			UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
			User assignUsr = SecurityAssociation.getUser().getUserSpace().getUser(userId);
			uActs.setActivityRole(act, role, assignUsr, true);
			return true;
		}
		return false;
	}
	
	public boolean setStepDates(String stepId, Object startDate, Object dueDate) {
		if (act != null) {
			UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
			return uActs.setStepDates(act, stepId, 
					startDate != null ? ActivityXML.parseDate(Context.toString(startDate)) : null, 
					dueDate != null ? ActivityXML.parseDate(Context.toString(dueDate)) : null);
		}
		return false;
	}
	
	public boolean assignStep(String stepId, String userId) throws 
			UserSpaceException, AccessDeniedException, NotFoundException {
		if (act != null) {
			UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
			uActs.assignStep(act, stepId, userId, (MutableRepositoryNode)act.getNode());
			return true;
		}
		return false;
	}
	
	public String submitStep(String stepId, String ruleExpression) throws 
			LockException, NotFoundException, IOException, AccessDeniedException, SAXException,
			EvalException, StateException, RunException, ActivityStepException  {
		
		if (act != null) {
			UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
			return uActs.submit(act, stepId, null, ruleExpression);
		}
		return null;
	}
	
	public boolean undo(String txId) throws StateException {
		
		if (act != null) {
			UserActivities uActs = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
			uActs.undo(act, txId);
			return true;
		}
		return false;
	}
	
	public ActivityStepState[] getStates() {
		if (act != null) {
			Collection<ActivityStepState> states = act.getStates().values();
			return states.toArray(new ActivityStepState[states.size()]);
		}
		return new ActivityStepState[0];
	}
	
	public ActivityPlan[] getPlans() {
		if (act != null) {
			Collection<ActivityPlan> plans = act.getPlans().values();
			return plans.toArray(new ActivityPlan[plans.size()]);
		}
		return new ActivityPlan[0];
	}
	
	public ActivityRole[] getRoles() {
		if (act != null) {
			Collection<ActivityRole> roles = act.getRoles().values();
			return roles.toArray(new ActivityRole[roles.size()]);
		}
		return new ActivityRole[0];
	}
	
	public void save() throws UserSpaceException {
		if (act != null) HibernateUtil.getSession().update(act);
	}

}
