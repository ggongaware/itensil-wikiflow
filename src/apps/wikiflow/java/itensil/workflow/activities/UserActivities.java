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

import itensil.io.DAOException;
import itensil.workflow.state.SubState;
import itensil.workflow.state.StateException;
import itensil.workflow.activities.signals.AlertSignalImpl;
import itensil.workflow.activities.signals.SignalManager;
import itensil.workflow.activities.state.ActivityCurrentPlan;
import itensil.workflow.activities.state.ActivityPlan;
import itensil.workflow.activities.state.ActivityStepState;
import itensil.workflow.activities.state.FlowRole;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityRole;
import itensil.workflow.activities.state.ActivityStateStore;
import itensil.workflow.activities.timer.ActivityTimer;
import itensil.workflow.activities.rules.CustValDataContentListener;
import itensil.workflow.activities.rules.XPathConditionEval;
import itensil.workflow.activities.rules.WFActivityStepInvoker;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.FlowSAXHandler;
import itensil.workflow.model.element.Path;
import itensil.workflow.model.element.Start;
import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Timer;
import itensil.workflow.Runner;
import itensil.workflow.FlowEvent;
import itensil.workflow.RunException;
import itensil.workflow.rules.EvalException;
import itensil.workflow.rules.ActivityStepException;
import itensil.repository.*;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.util.UriHelper;
import itensil.util.Check;

import java.util.*;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Query;
import org.xml.sax.SAXException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UserActivities {

    private User user;
    private Session session;
    private int pageSize = 10;
    private FlowModel lastModel;
    
    static Logger log = Logger.getLogger(UserActivities.class);

    public UserActivities(User user, Session session) {
        this.user = user;
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getUserId() {
        return user.getUserId();
    }
    
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 
     * @param pageNum
     * @param includeEnded
     * @return
     * @throws DAOException
     */
    public List getAssignActivities(int pageNum, boolean includeEnded) throws DAOException {
        Query qry = getSession().getNamedQuery("Activity.getAssignActivities");
        qry.setString("assignId", getUserId());
        qry.setString("userSpaceId", user.getUserSpaceId());
        qry.setInteger("filtSubState", includeEnded ? -1 : SubState.ENTER_END.ordinal());
        qry.setMaxResults(getPageSize() + 1);
        qry.setFirstResult(getPageSize() * pageNum);
        return qry.list();
    }

    /**
     * 
     * @param pageNum
     * @param includeEnded
     * @return
     * @throws DAOException
     */
    public List getSubmitActivities(int pageNum, boolean includeEnded) throws DAOException {
        Query qry = getSession().getNamedQuery("Activity.getSubmitActivities");
        qry.setString("submitId", getUserId());
        qry.setString("userSpaceId", user.getUserSpaceId());
        qry.setInteger("filtSubState", includeEnded ? -1 : SubState.ENTER_END.ordinal());
        qry.setMaxResults(getPageSize() + 1);
        qry.setFirstResult(getPageSize() * pageNum);
        return qry.list();
    }
    
    /**
     * 
     * @param flow
     * @param pageNum
     * @param includeEnded
     * @return
     * @throws DAOException
     */
    public List<Activity> getFlowAssignActivities(FlowState flow, int pageNum, boolean includeEnded) throws DAOException {
		Query qry = getSession().getNamedQuery("Activity.getFlowAssignActivities");
		qry.setString("assignId", getUserId());
		qry.setString("userSpaceId", user.getUserSpaceId());
		qry.setEntity("flow", flow);
		qry.setInteger("filtSubState", includeEnded ? -1 : SubState.ENTER_END.ordinal());
		qry.setMaxResults(getPageSize() + 1);
		qry.setFirstResult(getPageSize() * pageNum);
		return prepareActivities(qry.list());
    }
    
    
    protected List<Activity> prepareActivities(List res) {
    	ArrayList<Activity> acts = new ArrayList<Activity>(res.size());
		HashSet<String> subActs = new HashSet<String>();
		HashSet<String> loadedActs = new HashSet<String>();
		for (Object oAct : res) {
		 	Activity act = (Activity)oAct;
			acts.add(act);
			loadedActs.add(act.getId());
			for (ActivityStepState state : act.getStates().values()) {
				if (!Check.isEmpty(state.getSubActivityId())) {
					subActs.add(state.getSubActivityId());
				}
			}
		}
		 
		if (!subActs.isEmpty()) {
		     subActs.removeAll(loadedActs);
		     for (String subActId : subActs) {
		     	acts.add(getActivity(subActId));
		     }
		}
		 
		return sortActs(acts);
    }


    // TODO create comparator for activity class - mineful of all cases for rules - see grant
    public static List<Activity> sortActs(List<Activity> acts) {
    	if(acts == null) return null;
    	
    	ArrayList<Activity> actSorted = new ArrayList<Activity>(acts.size());
    	ArrayList<Activity> actAppend = new ArrayList<Activity>(acts.size());
    	Map<Activity,Date>  actToInsert = new HashMap<Activity,Date>();

    	for(Activity act : acts) {
    		String actId = act.getId();
    		Date actDueDate = act.getDueDate();
    		// activities with dates are already sorted properly by query order
    		if(actDueDate != null) {
    			actSorted.add(act);
    			continue;
    		}
    		
    		// get due date from states in activity
   			actDueDate = getLastStateDueDate(act);

   			// if due date found in states put activity in hash to sort later
    		if(actDueDate != null) {
    			actToInsert.put(act, actDueDate);    		
    			continue;
    		}
    		
   			// if no due date in states save activity to append after sort
   			actAppend.add(act);
    	}

    	insertByDate(actSorted, actToInsert);
    	actSorted.addAll(actAppend);
    	return actSorted;
  }

    
    public static Date getLastStateDueDate(Activity act) {
    	
    	Date lastStateDueDate = null;
    	for (ActivityStepState state : act.getStates().values()) {
            ActivityCurrentPlan plan = state.getCurrentPlan();
            if (plan != null) {
               Date currStateDueDate = plan.getDueDate();
               if(currStateDueDate == null) continue;

               if( lastStateDueDate == null || currStateDueDate.compareTo(lastStateDueDate) > 0) {
            	   lastStateDueDate = currStateDueDate;
               }
            }
    	}
    	return lastStateDueDate;
    }

    public static void insertByDate(List<Activity> sortedActs, Map<Activity,Date>  actToInsert) {
    	if(actToInsert == null || actToInsert.size() < 1) return;
    	
    	if(sortedActs == null) {
    		sortedActs = new ArrayList<Activity>();
    	}
    	
    	Set set = actToInsert.keySet();
    	Iterator it = set.iterator();
    	
    	while (it.hasNext()) {
    		Activity act = (Activity)it.next();
    		Date date = actToInsert.get(act);
    		
    		//temporarily put date in act so that sorting is easier
    		// remove it before exiting
    		act.setDueDate(date);
    		
    		boolean found = false;
    		for(int i=0, sortedLen=sortedActs.size(); i < sortedLen; i++ ) {
    			// insert before item with later due date
    			if(date.before(sortedActs.get(i).getDueDate())) {
    				sortedActs.add(i, act);
    				found= true;
    				break; 
    			}
    		}
    		// insert last if latest due date
    		if(!found) {
    			sortedActs.add(act);
    		}
    	}
    	
        // remove rolled-up added due dates from activities
    	it = set.iterator();
    	while (it.hasNext()) {
    		Activity act = (Activity)it.next();
    		int index = sortedActs.indexOf(act);
    		sortedActs.get(index).setDueDate(null);
    	}
    	
    }

    /**
     * 
     * @param flow
     * @param pageNum
     * @param includeEnded
     * @return
     * @throws DAOException
     */
    public List<Activity> getFlowSubmitActivities(FlowState flow, int pageNum, boolean includeEnded) throws DAOException {
        Query qry = getSession().getNamedQuery("Activity.getFlowSubmitActivities");
        qry.setString("submitId", getUserId());
        qry.setString("userSpaceId", user.getUserSpaceId());
        qry.setEntity("flow", flow);
        qry.setInteger("filtSubState", includeEnded ? -1 : SubState.ENTER_END.ordinal());
        qry.setMaxResults(getPageSize() + 1);
        qry.setFirstResult(getPageSize() * pageNum);
        return prepareActivities(qry.list());
    }

    /**
     * 
     * @param id
     * @return
     */
    public Activity getActivity(String id) {

        // TODO security check...
        Activity activity = (Activity)getSession().get(Activity.class, id);
        return activity;
    }

    /**
     * 
     * @param act
     * @throws AccessDeniedException
     */
    public void delete(Activity act) throws AccessDeniedException {
        try {
            MutableRepositoryNode node = (MutableRepositoryNode)act.getNode();
            node.remove();
        } catch (NotFoundException e) {
            // not fatal
        } catch (LockException e) {
            // not fatal
        }
        Query qry = getSession().getNamedQuery("Activity.clearSubActStates");
        qry.setString("subActId", act.getId());
        qry.executeUpdate();
        session.delete(act);
    }

    /**
     * Launch an activity
     *
     * @param name
     * @param description
     * @param flowUri
     * @param mastUri
     * @param roles
     * @throws UserSpaceException 
     */
    public Activity launch(
                String name,
                String description,
                String flowUri,
                String mastUri,
                String parentId,
                String projectId,
                String contextGroupId,
                Date dueDate,
                HashMap<String,String> roles)
            throws
                AccessDeniedException, IOException, SAXException, LockException, NotFoundException, DuplicateException,
                StateException, EvalException, RunException, ActivityStepException, UserSpaceException {

        RepositoryNode flowNode;
        FlowState flowState = null;
        
        try {
            flowNode = RepositoryHelper.getNode(flowUri, false);
            flowState = (FlowState)getSession().get(FlowState.class, flowNode.getNodeId());
        } catch (NotFoundException nfe) {

            // not-found, check for master-flow
            if (Check.isEmpty(mastUri)) throw nfe;

            MutableRepositoryNode mastNode = RepositoryHelper.getNode(mastUri, false);

            // copy master-flow
            // in the future this may do `crazy' inheritance stuff
            flowNode = mastNode.copy(flowUri, true);
        }
        
        FlowModel flowMod = new FlowModel();
        FlowSAXHandler hand = new FlowSAXHandler(flowMod);
        hand.parse(RepositoryHelper.loadContent(UriHelper.absoluteUri(flowUri, "chart.flow")));
        
        // cache this
        this.lastModel = flowMod;
        
        // verify flowstate
        if (flowState == null) {
            flowState = new FlowState(flowNode.getNodeId());
            flowState.setActive(true);
            CustValDataContentListener.modelStateSync(flowState, flowMod);
            getSession().persist(flowState);
        }

        MutableRepositoryNode activityNode = createActivityFolder(flowUri, name, null);
        if (!Check.isEmpty(contextGroupId)) {
        	activityNode.setContextGroup(new DefaultGroup(contextGroupId));
        }
        
        name = UriHelper.name(activityNode.getUri());

        // create activity entity
        Activity activityEnt = new Activity(activityNode.getNodeId());
        activityEnt.setUserSpaceId(user.getUserSpaceId());
        activityEnt.setName(name);
        if (!Check.isEmpty(parentId)) {
            Activity parActivity = (Activity)session.get(Activity.class, parentId);
            activityEnt.changeParent(parActivity);
        }
        if (!Check.isEmpty(projectId)) {
        	// try to get this (permission and existance check
        	RepositoryHelper.getNodeById(projectId, false);
        	
        	activityEnt.getProjects().add(projectId);        	
        }
        activityEnt.setDescription(description);
        activityEnt.setContextGroupId(contextGroupId);
        activityEnt.setDueDate(dueDate);
        activityEnt.setSubmitId(user.getUserId());
        session.persist(activityEnt);

        // assign roles
        for (Map.Entry<String, String> ent : roles.entrySet()) {
            String role = ent.getKey();
            String assignId = ent.getValue();
            User assignUsr = user.getUserSpace().resolve(new DefaultUser(assignId));
            if (assignUsr != null) setActivityRole(activityEnt, role, assignUsr, false);
        }

        // start the activity in the process
        ActivityStateStore store = new  ActivityStateStore(flowState);
        Runner<Activity,String> run = new Runner<Activity,String>(
                flowMod, store, store, new XPathConditionEval<String>(flowMod),
                new WFActivityStepInvoker<String>());

        run.handleEvent(new FlowEvent<Activity,String>(activityEnt, run.startToken(null, activityEnt)));

        return activityEnt;
    }
    
    /**
     * Send an activity from one process to another
     * 
     * @param activityEnt
     * @param flowUri
     * @param mastUri when not empty/null, this will force process creation and uniquify the flowUri
     * @throws AccessDeniedException 
     * @throws NotFoundException
     * @throws LockException 
     * @throws DuplicateException 
     * @throws SAXException 
     * @throws IOException 
     * @throws StateException 
     * @throws EvalException 
     * @throws ActivityStepException 
     * @throws RunException 
     */
    public Activity sendToFlow(
    		Activity activityEnt, 
    		String flowUri,
            String mastUri) 
    			throws 
    				NotFoundException, AccessDeniedException, DuplicateException, LockException, 
    				IOException, SAXException, RunException, ActivityStepException, EvalException, StateException {
    	
    	// check if same process
    	boolean create = !Check.isEmpty(mastUri);
    	try {
    		if (!create && activityEnt.getFlow().getNode().getUri().equals(flowUri)) 
    			return activityEnt;
    	} catch (NotFoundException nfe) {
    		// eat it
    	}
    	
    	MutableRepositoryNode flowNode;
    	
    	FlowState flowState = null;
    	Session sess = getSession();
    	
    	if (create) {
    		flowUri = RepositoryHelper.getAvailableUri(flowUri);
    		MutableRepositoryNode mastNode = RepositoryHelper.getNode(mastUri, false);

            // copy master-flow
            // in the future this may do `crazy' inheritance stuff
            flowNode = (MutableRepositoryNode) mastNode.copy(flowUri, true);
    	} else {
    		flowNode = RepositoryHelper.getNode(flowUri, false);
    		flowState = (FlowState)sess.get(FlowState.class, flowNode.getNodeId());
            
    	}
    	
    	FlowModel flowMod = new FlowModel();
    	FlowSAXHandler hand = new FlowSAXHandler(flowMod);
    	hand.parse(RepositoryHelper.loadContent(UriHelper.absoluteUri(flowUri, "chart.flow")));
    	
    	// verify flowstate
        if (flowState == null) {
            flowState = new FlowState(flowNode.getNodeId());
            flowState.setActive(true);
            CustValDataContentListener.modelStateSync(flowState, flowMod);
            sess.persist(flowState);
        }
        MutableRepositoryNode activityNode;
        boolean needReId = false;
        try {
        	activityNode = (MutableRepositoryNode)activityEnt.getNode();
        } catch (NotFoundException nfe) {
        	activityNode = null;
        	needReId = true;
        }
    	
        activityNode = createActivityFolder(flowUri, activityEnt.getName(), activityNode);
        
        activityEnt.setName(UriHelper.name(activityNode.getUri()));
        activityEnt.setFlow(flowState);
        
        // reset the states
        
        for (ActivityStepState actState : activityEnt.getStates().values()) {
        	sess.delete(actState);
        }
        activityEnt.getStates().clear();
        
        activityEnt.setVariationId(null);
        
        // TODO - decide to clear plans?
        
        if (needReId) {
        	activityEnt = activityEnt.changeActivityId(activityNode.getNodeId());
        }
        
        /*
		 * some kind of bug here, I suspect hibernate...
		 * need to flush to ensure delete
		 */
		getSession().flush();

        
        // start the activity in the process
        ActivityStateStore store = new  ActivityStateStore(flowState);
        Runner<Activity,String> run = new Runner<Activity,String>(
                flowMod, store, store, new XPathConditionEval<String>(flowMod),
                new WFActivityStepInvoker<String>());

        run.handleEvent(new FlowEvent<Activity,String>(activityEnt, run.startToken(null, activityEnt)));
        
    	return activityEnt;
    }

    /**
     * Warning: activity will end up with a new id
     * 
     * @param activityEnt
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws DuplicateException
     * @throws LockException
     */
	public Activity rebuildActivityFolder(Activity activityEnt) 
    	throws AccessDeniedException, NotFoundException, DuplicateException, LockException {
    	
    	MutableRepositoryNode node = 
    		createActivityFolder(activityEnt.getFlow().getNode().getUri(), 
    				activityEnt.getName(), null);
    	activityEnt.setVariationId(null);
    	return activityEnt.changeActivityId(node.getNodeId());
    }
    
    protected MutableRepositoryNode createActivityFolder(
    		String flowUri, String name, MutableRepositoryNode activityNode) 
    			throws AccessDeniedException, NotFoundException, DuplicateException, LockException {
    	
    	// verify "activities" folder
        String activitiesUri = UriHelper.absoluteUri(flowUri, "activities");
        try {
            RepositoryHelper.getNode(activitiesUri, false);
        } catch (NotFoundException nfe) {
            RepositoryHelper.getRepository(flowUri).createNode(activitiesUri, true, user);
        }
        name = UriHelper.filterName(name);
        String iNameUri = RepositoryHelper.getAvailableUri(UriHelper.absoluteUri(activitiesUri, name));
         
        if (activityNode != null) {
        	activityNode.move(iNameUri);
        	try {
               MutableRepositoryNode templFold = RepositoryHelper.getNode(
                      UriHelper.absoluteUri(flowUri, "template"), false);
               
               for (RepositoryNode kid : templFold.getChildren()) {
            	   try {
            		   String kidDstUri = UriHelper.absoluteUri(iNameUri, UriHelper.name(kid.getUri()));
            		   ((MutableRepositoryNode)kid).copy(kidDstUri, true);
            	   } catch (DuplicateException de) {
            		   // eat it
            		   // TODO - merge duplicate xml files
            	   }
               }
               
        	} catch (NotFoundException nfe) {
        		// eat it
        	}
        	return activityNode;
        }

        // find activity template folder
        try {
            MutableRepositoryNode templFold = RepositoryHelper.getNode(
                   UriHelper.absoluteUri(flowUri, "template"), false);

            // copy to unique activity folder
            activityNode = (MutableRepositoryNode)templFold.copy(iNameUri, true);

        } catch (NotFoundException nfe) {

            // no template, create unique activity folder
            activityNode = RepositoryHelper.getRepository(flowUri).createNode(iNameUri, true, user);
        }
        return activityNode;
    }

    /**
     *
     * @param activityEnt
     * @throws StateException
     */
    public void undo(Activity activityEnt, String txId) throws StateException {
        FlowState flowState = activityEnt.getFlow();
        ActivityStateStore store = new ActivityStateStore(flowState);
        store.rollbackTx(activityEnt, txId);
    }

    /**
     *
     * @param activityEnt
     * @param step
     * @param expr - set rule expression
     * @param submitValues - additional rule sets
     * @throws LockException
     * @throws NotFoundException
     * @throws IOException
     * @throws AccessDeniedException
     * @throws SAXException
     */
    public String submit(Activity activityEnt, String step, Map<String,String> submitValues, String expr)
            throws LockException, NotFoundException, IOException, AccessDeniedException, SAXException,
            EvalException, StateException, RunException, ActivityStepException {

        Runner<Activity,String> run = getRunner(activityEnt);
       
        XPathConditionEval<String> xpathEval = (XPathConditionEval<String>)run.getEvals();
        
        if (submitValues != null || !Check.isEmpty(expr)) {
	        
	        if (submitValues != null)
	        	xpathEval.setDataValues(activityEnt, xpathEval.getDefaultData(activityEnt), submitValues);
	        
	
	        if (!Check.isEmpty(expr)) {
	            int pos = expr.indexOf('=');
	            if (pos > 0) {
	                String pathExpr = expr.substring(0, pos).trim();
	                String valExpr = expr.substring(pos + 1).trim();
	                xpathEval.setDataValueExpr(activityEnt, pathExpr, valExpr);
	            }
	        }
	        
        }

        // dispatch flow event
        String resTx = run.handleEvent(new FlowEvent<Activity,String>(activityEnt, step));
        
        // handle saves of data updates
        // not very accurate, but if the cache is not empty, the doc may be dirty
        if (activityEnt.getDataCache() != null) {
        	xpathEval.saveDefaultData(activityEnt, activityEnt.getDataCache());
        }
        
        activityEnt.clearCache();
        
        return resTx;
    }

    /**
     * 
     * @param activityEnt
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     * @throws IOException
     * @throws SAXException
     * @throws LockException
     */
    public Runner<Activity,String> getRunner(Activity activityEnt)
            throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException {

        // resolve the flow model
        FlowState flowState = activityEnt.getFlow();
        FlowModel flowMod = getModel(flowState, activityEnt);
        
        // cache this
        this.lastModel = flowMod;

        // evaluate submit expression
        XPathConditionEval<String> xpathEval = new XPathConditionEval<String>(flowMod);

        ActivityStateStore store = new  ActivityStateStore(flowState);
        return new Runner<Activity,String>(
                flowMod, store, store, xpathEval,
                new WFActivityStepInvoker<String>());
    }
    
    public XPathConditionEval<String> getConditionEval(Activity activityEnt) 
    		throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException {
    	
    	//  resolve the flow model
        FlowState flowState = activityEnt.getFlow();
        FlowModel flowMod = getModel(flowState, activityEnt);
        return new XPathConditionEval<String>(flowMod);
    }
    
    public FlowModel getModel(FlowState flowState, Activity activityEnt) 
    		throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException {
    	
    	RepositoryNode vNod = activityEnt == null ? null : activityEnt.getVariationNode();
    	InputStream in;
    	if (vNod != null) {
    		in = RepositoryHelper.loadContent((MutableRepositoryNode)vNod);
    	} else {
    		String modelUri = UriHelper.absoluteUri(flowState.getNode().getUri(), "chart.flow");
    		in = RepositoryHelper.loadContent(modelUri);
    	}
    	FlowModel flowMod = new FlowModel();
    	FlowSAXHandler hand = new FlowSAXHandler(flowMod);
    	hand.parse(in);
    	return flowMod;
    }
    
    /**
     * 
     * @param fState
     * @param role
     * @param assignUsr
     * @return
     * @throws AccessDeniedException
     * @throws NotFoundException
     * @throws LockException 
     * @throws SAXException 
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
	public FlowRole setFlowRole(FlowState fState, String role, User assignUsr) 
    		throws AccessDeniedException, NotFoundException, IOException, SAXException, LockException {
    	
    	MutableRepositoryNode flowNode = (MutableRepositoryNode)fState.getNode();

        // grant file access
        if (!flowNode.hasPermission(DefaultNodePermission.writePermission(assignUsr))) {
            try {
                flowNode.grantPermission(new DefaultNodePermission(assignUsr, DefaultNodePermission.WRITE, true));
            } catch (AccessDeniedException ade) {
                // TODO figure out what to do when assigner can't manage
            }
        }

        String prevAssignId = null;
        FlowRole roleEnt = fState.getRoles().get(role);
        if (roleEnt == null) {
            roleEnt = new FlowRole();
            roleEnt.setFlow(fState);
            roleEnt.setRole(role);
            fState.getRoles().put(role, roleEnt);
        }
        else {
        	prevAssignId = roleEnt.getAssignId();
        }
        roleEnt.setAssignId(assignUsr.getUserId());

        Session sess = getSession();
        sess.saveOrUpdate(roleEnt);
        
        if (!Check.isEmpty(prevAssignId)) {
        	
        	// load up current activities assigned to previous user
        	Query qry = sess.getNamedQuery("Activity.getFlowAssignActivities");
        	qry.setEntity("flow", fState);
            qry.setString("assignId", prevAssignId);
            qry.setString("userSpaceId", user.getUserSpaceId());
            qry.setInteger("filtSubState", SubState.ENTER_END.ordinal());
            
            List<Activity> actList = (List<Activity>)qry.list();
            if (!actList.isEmpty()) {
            	FlowModel flowMod = getModel(fState, null);
          
	            for (Activity act : actList)  {
	            	FlowModel actFlowMod = flowMod;
	            	//  check for activity variation
	            	try {
	            		if (act.getVariationNode() != null) actFlowMod = getModel(fState, act);
	            		if (!act.getRoles().containsKey(role)) {
		            		reAssignActivityRoles(actFlowMod, act, role, prevAssignId, assignUsr.getUserId());
		            	}
	            	} catch (AccessDeniedException ade) {
	            		log.warn("Activity Role assignment", ade);
	                }
	            }
            }
        }
        
        return roleEnt;
    }
    
    /**
     * 
     * @param flowMod
     * @param activity
     * @param role
     * @param assignUsr
     */
    protected void reAssignActivityRoles(
    		FlowModel flowMod, Activity activity, String role, String prevAssignId, String assignId) {
    	
    	Session sess = getSession();
    	for (ActivityStepState state : activity.getStates().values()) {
    		if (Check.isEmpty(prevAssignId) || prevAssignId.equals(state.getAssignId())) {
    			Step step = flowMod.getStep(state.getStepId());
    			if (step == null) continue;
    			String sRol = step.getAttribute("role");
    			if (role.equals(sRol)) {
    				state.setAssignId(assignId);
    				sess.update(state);

    		        AlertSignalImpl sig = new AlertSignalImpl();
    		        sig.setActivity(activity);
    		        sig.setStepId(step.getId());
    		        sig.setAssignId(assignId);

    		        // don't resend if the person is already online
    		        sig.setMailed(getUserId().equals(assignId) ? SignalManager.SIGNAL_STATUS_ACTIVE_SENT : SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
    		        sig.setTimeStamp(new Date());
    		        sig.setRole(role);

    		        SignalManager.saveOrUpdateSignal(sig);
    			}
    		}
    	}
    }
    
    /**
     * 
     * @param activity
     * @param role
     * @param assignUsr
     * @param reAssign
     * @return
     * @throws NotFoundException
     * @throws AccessDeniedException
     * @throws LockException 
     * @throws SAXException 
     * @throws IOException 
     */
    public ActivityRole setActivityRole(Activity activity, String role, User assignUsr, boolean reAssign) 
    		throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException {
    	
    	MutableRepositoryNode activityNode = (MutableRepositoryNode)activity.getNode();
    	
    	MutableRepositoryNode parNode = (MutableRepositoryNode)activityNode.getParent();

        // grant file access to parent for activity.kb writing
        if (!parNode.hasPermission(DefaultNodePermission.writePermission(assignUsr))) {
            try {
            	parNode.grantPermission(new DefaultNodePermission(assignUsr, DefaultNodePermission.WRITE, true));
            } catch (AccessDeniedException ade) {
                // TODO figure out what to do when assigner can't manage
            }
        }
        
        ActivityRole roleEnt = activity.getRoles().get(role);
        if (roleEnt == null) {
            roleEnt = new ActivityRole();
            roleEnt.setActivity(activity);
            roleEnt.setRole(role);
            activity.getRoles().put(role, roleEnt);
        }

        roleEnt.setAssignId(assignUsr.getUserId());

        getSession().saveOrUpdate(roleEnt);
        
        if (reAssign) {
	        reAssignActivityRoles(
	        	getModel(activity.getFlow(), activity),
	        	activity, 
	        	role, 
	        	null, 
	        	assignUsr.getUserId());
        }
        
        return roleEnt;
    }
    
    /**
     * 
     * @param activity
     * @param timerId
     * @return
     */
    public ActivityTimer getActivityTimer(Activity activity, String timerId) {
    	Query qry = getSession().getNamedQuery("Timer.getTimer");
    	qry.setString("tid", timerId);
    	qry.setEntity("act", activity);
    	return (ActivityTimer)qry.uniqueResult();
    }
    
    /**
     * 
     * @return
     */
    public List<MutableRepositoryNode> getActiveFlows(int limit) {
 	   Query qry = getSession().getNamedQuery("FlowState.recentlyActiveFlows");
	   qry.setString("userSpaceId", user.getUserSpaceId());
	   qry.setMaxResults(limit * 2);
	   ArrayList<MutableRepositoryNode> nodes = new ArrayList<MutableRepositoryNode>(limit);
	   int qualified = 0;
	   NodePermission creatPerm = DefaultNodePermission.createPermission(user);
	   for (Object obj : qry.list()) {
		   MutableRepositoryNode nod = (MutableRepositoryNode)obj;
		   try {
			   if (nod.hasPermission(creatPerm)) {
				   nodes.add(nod);
				   qualified++;
				   if (qualified >= limit) break;
			   }
			} catch (AccessDeniedException e) {
				// eat it
			}
	   }
	   return nodes;
    }
    
    /**
     * 
     * @param activity
     * @param timerId
     * @param atTime
     * @throws StateException
     * @throws NotFoundException
     * @throws AccessDeniedException
     * @throws IOException
     * @throws SAXException
     * @throws LockException
     */
    public void setManualTimer(Activity activity, String timerId, Date atTime) 
    	throws StateException, NotFoundException, AccessDeniedException, 
    		IOException, SAXException, LockException {
    	
    	Runner<Activity,String> run = getRunner(activity);
    	Step step = run.getModel().getStep(timerId);
    	if (step instanceof Timer) {
    		run.getTimers().setManualTimer((Timer)step, activity, atTime);
    	}
    }

    /**
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<MutableRepositoryNode> getAssignedProjectNodes() {
    	Query qry = getSession().getNamedQuery("Activity.getAssignedProjects");
		qry.setString("userId", user.getUserId());
		qry.setString("userSpaceId", user.getUserSpaceId()); 
		return (List<MutableRepositoryNode>)qry.list();
    }
    
    /**
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<MutableRepositoryNode> getAssignedMeetingNodes() {
    	Query qry = getSession().getNamedQuery("Activity.getAssignedMeetings");
		qry.setString("userId", user.getUserId());
		qry.setString("userSpaceId", user.getUserSpaceId()); 
		return (List<MutableRepositoryNode>)qry.list();
	}
    
    
    /**
     * 
     * @param projectId
     * @param limit
     * @return
     */
    public List<Activity> getActiveProjectActivities(String projectId, int limit) {
    	Query qry = getSession().getNamedQuery("Activity.activeProjectActivities");
		qry.setString("projId", projectId);
		qry.setMaxResults(limit);
		ArrayList<Activity> acts = new ArrayList<Activity>(limit);
		for (Object obj : qry.list()) {
			acts.add((Activity)obj);
		}
		return prepareActivities(acts);
    }

    /**
     * 
     * @param activity
     * @param switchId
     * @param returnId
     * @return resulting txId
     * @throws NotFoundException
     * @throws AccessDeniedException
     * @throws IOException
     * @throws SAXException
     * @throws LockException
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
	public String exitSwitch(Activity activity, String switchId, String returnId) 
			throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException, 
			RunException, ActivityStepException, EvalException, StateException {
		
		Runner<Activity,String> run = getRunner(activity);
		return run.handleEventCondition(
				new FlowEvent<Activity,String>(activity, switchId), new String[]{returnId});
	}

	/**
	 * 
	 * @param fstate
	 * @return
	 */
	public Activity getFirstFlowActivity(FlowState fstate) {
		Query qry = getSession().getNamedQuery("Activity.getFlowActivity");
		qry.setEntity("flow", fstate);
		Iterator itr = qry.iterate();
		return itr.hasNext() ? (Activity)itr.next() : null;
	}
	
	/**
     * 
     * @param flow
     * @param variation
     * @param includeEnded
     * @return
     * @throws DAOException
     */
    public Collection<Activity> getFlowActivities(FlowState flow, String variation, boolean includeEnded) 
    		throws DAOException {
    	
        Query qry;
        if (Check.isEmpty(variation)) {
        	qry = getSession().getNamedQuery("Activity.getFlowActivities");
        } else {
        	qry = getSession().getNamedQuery("Activity.getFlowActivitiesVar");
        	qry.setString("variation", variation);
        }
        qry.setString("userSpaceId", user.getUserSpaceId());
        qry.setEntity("flow", flow);
        qry.setInteger("filtSubState", includeEnded ? -1 : SubState.ENTER_END.ordinal());
        List res = qry.list();
        ArrayList<Activity> acts = new ArrayList<Activity>(res.size());
        for (Object oAct :  res) {
        	Activity act = (Activity)oAct;
        	if (includeEnded || !act.getStates().isEmpty()) {
        		acts.add(act);
        	}
        }
        return acts;
    }

    /**
     * 
     * @param activities
     * @param flowState
     * @param flowMod
     * @param newIds
     * @throws RunException
     * @throws ActivityStepException
     * @throws EvalException
     * @throws StateException
     */
	public void enterNewStarts(
			Collection<Activity> activities, 
			FlowState flowState, 
			FlowModel flowMod, 
			ArrayList<String> newIds) 
				throws RunException, ActivityStepException, EvalException, StateException {
		
		XPathConditionEval<String> xpathEval = new XPathConditionEval<String>(flowMod);
        ActivityStateStore store = new  ActivityStateStore(flowState);
        Runner<Activity,String> runner = new Runner<Activity,String>(
                flowMod, store, store, xpathEval,
                new WFActivityStepInvoker<String>());

        for (Start strt : flowMod.getStartSteps()) {
        	for (Path pth : strt.getPaths()) {
        		if (newIds.contains(pth.getTo())) {
        			for (Activity activityEnt :  activities) {
        				runner.forceEnter(new FlowEvent<Activity,String>(activityEnt, pth.getTo()));
        			}
        		}
        	}
        }
	}

	/**
	 * Assign a user (outside of roles) also, send alert if qualified
	 * 
	 * @param activity
	 * @param stepId
	 * @param userId
	 * @param permNode
	 * @throws UserSpaceException
	 * @throws AccessDeniedException 
	 */
	public void assignStep(Activity activity, String stepId, String userId, MutableRepositoryNode permNode) 
			throws UserSpaceException, AccessDeniedException {
		
		User assignee = this.user.getUserSpace().resolve(new DefaultUser(userId));
		if (assignee != null) {
			ActivityStepState state = activity.getStates().get(stepId);
			if (state != null && !assignee.getUserId().equals(state.getAssignId())) {
				state.setAssignId(assignee.getUserId());
				getSession().update(state);

		        AlertSignalImpl sig = new AlertSignalImpl();
		        sig.setActivity(activity);
		        sig.setStepId(stepId);
		        sig.setAssignId(assignee.getUserId());

		        // don't resend if the person is already online
		        sig.setMailed(getUserId().equals(assignee.getUserId()) ? SignalManager.SIGNAL_STATUS_ACTIVE_SENT : SignalManager.SIGNAL_STATUS_ACTIVE_PENDING); 
		        sig.setTimeStamp(new Date());
		

		        SignalManager.saveOrUpdateSignal(sig);
			}
			ActivityPlan plan = activity.getPlans().get(stepId);
			if (plan != null && !assignee.getUserId().equals(plan.getAssignId())) {
				plan.setAssignId(assignee.getUserId());
				getSession().update(plan);
			} else if (plan == null && state == null) {
				plan = new ActivityPlan();
				plan.setActivity(activity);
				plan.setStepId(stepId);
				activity.getPlans().put(stepId, plan);
				plan.setAssignId(assignee.getUserId());
				getSession().save(plan);
			}
			
			if (permNode != null) {
				// grant file access to parent for activity.kb writing
		        if (!permNode.hasPermission(DefaultNodePermission.writePermission(assignee))) {
		            try {
		            	permNode.grantPermission(new DefaultNodePermission(assignee, DefaultNodePermission.WRITE, true));
		            } catch (AccessDeniedException ade) {
		                // TODO figure out what to do when assigner can't manage
		            }
		        }
		        
			}
		}
		
	}

	/**
	 * 
	 * @param activity
	 * @param stepId
	 * @param date
	 * @return true if activity needs to be saved
	 */
	public boolean setStepDates(Activity activity, String stepId, Date startDate, Date dueDate) {
		ActivityStepState state = activity.getStates().get(stepId);
		if (state != null) {
			ActivityCurrentPlan cp = state.getCurrentPlan();
			if (cp == null) {
				cp = new ActivityCurrentPlan();
				cp.setState(state);
				state.setCurrentPlan(cp);
			}
			startDate = null; // its already started...
			if (dueDate != null) cp.setDueDate(dueDate);
			getSession().update(state);
		}
		ActivityPlan plan = activity.getPlans().get(stepId);
		if (plan != null) {
			if (startDate != null) plan.setStartDate(startDate);
			if (dueDate != null)  plan.setDueDate(dueDate);
			getSession().update(plan);
		} else if (state == null) {
			plan = new ActivityPlan();
			plan.setActivity(activity);
			plan.setStepId(stepId);
			activity.getPlans().put(stepId, plan);
			plan.setStartDate(startDate);
			plan.setDueDate(dueDate);
			getSession().save(plan);
		}
		// move out the activity date to encapsulate
		Date actDue = activity.getDueDate();
		boolean dateMoved = false;
		if (dueDate != null && actDue != null && actDue.before(dueDate)) {
			activity.setDueDate(dueDate);
			dateMoved = true;
		}
		Date actStart = activity.getStartDate();
		if (startDate != null && (actStart == null || actStart.after(startDate))) {
			activity.setStartDate(startDate);
			dateMoved = true;
		}
		return dateMoved;
	}

	public FlowModel getLastModel() {
		return lastModel;
	}
    
	
}
