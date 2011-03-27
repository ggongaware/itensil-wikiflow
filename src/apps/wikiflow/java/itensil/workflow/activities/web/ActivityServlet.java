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
package itensil.workflow.activities.web;

import itensil.web.MethodServlet;
import itensil.web.ContentType;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.web.UrlUtil;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.workflow.activities.*;
import itensil.workflow.activities.state.*;
import itensil.workflow.activities.timer.ActivityTimer;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.FlowSAXHandler;
import itensil.workflow.model.AppElement;
import itensil.workflow.state.SubState;
import itensil.workflow.Runner;
import itensil.workflow.FlowEvent;
import itensil.workflow.activities.rules.XPathConditionEval;
import itensil.workflow.activities.rules.WFActivityStepInvoker;
import itensil.security.AuthenticatedUser;
import itensil.security.User;
import itensil.security.DefaultUser;
import itensil.security.web.UserUtil;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;
import itensil.repository.*;
import itensil.repository.hibernate.RepositoryEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;
import org.hibernate.Session;
import org.xml.sax.SAXException;

import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityServlet extends MethodServlet {



    /**
     *  /page
     *
     * Direct page - the workzone
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String activityId = request.getParameter("activity");
        String flow = request.getParameter("flowUri");
        String flowUri = null;
        if (!Check.isEmpty(flow)) {
            flowUri = flow;
        } else if (!Check.isEmpty(activityId)) {
            HibernateUtil.beginTransaction();
            HibernateUtil.readOnlySession();
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            Activity activity = uActivities.getActivity(activityId);
            if (activity == null) throw new NotFoundException(activityId);
            request.setAttribute("activity", activity);
            FlowState fState = activity.getFlow();
            if (fState == null) {
            	ServletUtil.forward("/view-wf/missing.jsp", request, response);
            }
            try {
            	flowUri = UriHelper.absoluteUri(fState.getNode().getUri(), "chart.flow");
            } catch (NotFoundException nfe) {
            	ServletUtil.forward("/view-wf/missing.jsp", request, response);
            }
            
            HibernateUtil.commitTransaction();
        } else {
            throw new NotFoundException("[blank]");
        }
        if (flowUri != null) {
        	request.setAttribute("flowUri", flowUri);
        	ServletUtil.forward(
        			"1".equals(request.getParameter("kiosk")) ?
        				"/view-wf/kiosk.jsp" : "/view-wf/todo.jsp", request, response);
        }
    }
    

    /**
     *  /stat
     *
     * Status page - state grid
     *
     */
    public void webStat(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String activityId = request.getParameter("activity");
        String flow = request.getParameter("flowUri");
        String flowUri = null;

        Activity activity = null;
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        if (!Check.isEmpty(flow)) {
            flowUri = flow;
        } else if (!Check.isEmpty(activityId)) {
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            activity = uActivities.getActivity(activityId);
            if (activity == null) throw new NotFoundException(activityId);
            request.setAttribute("activity", activity);
            FlowState fState = activity.getFlow();
            if (fState == null) {
            	ServletUtil.forward("/view-wf/missing.jsp", request, response);
            }
            try {
            	flowUri = UriHelper.absoluteUri(fState.getNode().getUri(), "chart.flow");
            } catch (NotFoundException nfe) {
            	ServletUtil.forward("/view-wf/missing.jsp", request, response);
            }
        } else {
        	HibernateUtil.commitTransaction();
            throw new NotFoundException("[blank]");
        }
        if (flowUri != null) {
        	request.setAttribute("flowUri", flowUri);
        	
        	boolean isMeet = flowUri.indexOf("/meeting/") > 0;
        	if (isMeet) {
        		// If this is a meeting get the latest outline draft	
	        	if (activity == null) {
	        		 MutableRepositoryNode flowFoldNode = RepositoryHelper.getNode(UriHelper.getParent(flowUri), false);
	        		 FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flowFoldNode.getNodeId());
	                 UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
	                 activity = uActivities.getFirstFlowActivity(fState);
	        	}
	        	MutableRepositoryNode actRepoNode = (MutableRepositoryNode)activity.getNode();
	         	request.setAttribute("meet-draft", actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-draft")));
	         	request.setAttribute("meet-ldraft", actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-ldraft")));
        	}
        	
        	ServletUtil.forward("/view-wf/status.jsp", request, response);
        }
        HibernateUtil.commitTransaction();
    }
    

    /**
     *  /meetStat
     * 
     *
     * Meeting Status page - singleton state grid
     *
     */
    public void webMeetStat(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
        String flowUri = request.getParameter("meet");
        if (!Check.isEmpty(flowUri)) {
        	
            HibernateUtil.beginTransaction();
            HibernateUtil.readOnlySession();
            
            flowUri = RepositoryHelper.resolveUri(flowUri);
            
            Session session = HibernateUtil.getSession();
            
            MutableRepositoryNode flowFoldNode = RepositoryHelper.getNode(flowUri, false);
            
            FlowState fState = (FlowState)session.get(FlowState.class, flowFoldNode.getNodeId()); 
            if (fState == null) throw new NotFoundException("[Activity for meeting:" + flowUri + "]");
            
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());

                       
            Activity activity = uActivities.getFirstFlowActivity(fState);
            if (activity == null) throw new NotFoundException("[Activity for meeting:" + flowUri + "]");
            
            flowUri = UriHelper.absoluteUri(flowFoldNode.getUri(), "chart.flow");
            
            request.setAttribute("activity", activity);
            request.setAttribute("activityId", activity.getId());
            
            MutableRepositoryNode actRepoNode = (MutableRepositoryNode)activity.getNode();
            
            // check if still in setup
            if ("1".equals(actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-setup")))) {
            	ServletUtil.bounce(ServletUtil.getServletPath(request, "/mod/meet"), "uri=" + UrlUtil.encode(flowUri), response);
            	return;
            }
            
            request.setAttribute("meet-draft", actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-draft")));
            request.setAttribute("meet-ldraft", actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-ldraft")));
            
            HibernateUtil.commitTransaction();
            request.setAttribute("flowUri", flowUri);
        	ServletUtil.forward("/view-wf/status.jsp", request, response);
        } else {
            throw new NotFoundException("[blank]");
        }
    }
    
    /**
     *  /assignedList
     *
     * List Assign Activities
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webAssignedList(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String flow = request.getParameter("flow");
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        
        String userId = request.getParameter("userId");
        boolean includeEnds = "1".equals(request.getParameter("ends"));
        
        if (!Check.isEmpty(userId) && UserUtil.isAdmin((AuthenticatedUser)user)) {
        	user = user.getUserSpace().resolve(new DefaultUser(userId));
        }
        
        Session session = HibernateUtil.getSession();
        UserActivities uActivities = new UserActivities(user, session);
        uActivities.setPageSize(100);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");

        List<Activity> aActivities;
        if (Check.isEmpty(flow)) {
            aActivities = uActivities.getAssignActivities(0, includeEnds);
        } else {

            // resolve flow
            FlowState fState = (FlowState)session.get(FlowState.class, RepositoryHelper.getNode(flow, false).getParentNodeId());
            aActivities = fState == null ? null : uActivities.getFlowAssignActivities(fState, 0, includeEnds);
        }

        if (aActivities != null) {
            for (Activity act : aActivities) {
                root.add(ActivityXML.display(act));
            }
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }


    /**
     *  /submittedList
     *
     * List Submit Activities
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webSubmittedList(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String flow = request.getParameter("flow");
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        
        String userId = request.getParameter("userId");
        boolean includeEnds = "1".equals(request.getParameter("ends"));
        
        if (!Check.isEmpty(userId) && UserUtil.isAdmin((AuthenticatedUser)user)) {
        	user = user.getUserSpace().resolve(new DefaultUser(userId));
        }
        
        Session session = HibernateUtil.getSession();
        UserActivities uActivities = new UserActivities(user, session);
        uActivities.setPageSize(100);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");

        List<Activity> aActivities;
        if (Check.isEmpty(flow)) {
            aActivities = uActivities.getSubmitActivities(0, includeEnds);
        } else {
        	flow = RepositoryHelper.resolveUri(flow);
        	
            // resolve flow
            FlowState fState = (FlowState)session.get(FlowState.class, RepositoryHelper.getNode(flow, false).getParentNodeId());
            aActivities = fState == null ? null : uActivities.getFlowSubmitActivities(fState, 0, includeEnds);
        }

        if (aActivities != null) {
            for (Activity act : aActivities) {
                root.add(ActivityXML.display(act));
            }
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /kidList
     *
     * List Activity Kids
     *
     */
    @ContentType("text/xml")
    public void webKidList(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());

        String id = request.getParameter("id");
        Activity activity = uActivities.getActivity(id);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        for (Activity kid : activity.getActiveChildren()) {
            root.add(ActivityXML.display(kid));
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /launch
     *
     * Inject a new activity into a flow.
     *
     * Request XML:
     *
     *      <launch>
     *
     *          <flow>/path/xxx.flow</flow>
     *          <master-flow>/optional/master-path/xxx.flow</master-flow>
     *          <name>text</name>
     *          <description>text</description>
     *          <start>date</start>
     *          <end>date</end>
     *          <priority>number</priority>
     *          <parent>activity.id</parent>
     *          <parentStep>step.id</parentStep>
     *          <project>id</project>
     *          <meet>0</meet>          
     *
     *          <!-- maybe this for more file inputs? -->
     *          <file name="...">uri</file> ...
     *      </launch>
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webLaunch(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("launch".equals(root.getName())) {

            HibernateUtil.beginTransaction();
            User user = (User)request.getUserPrincipal();
            User subUser = null;
            
            String submitId = root.elementTextTrim("submitId");
            
            if (!Check.isEmpty(submitId) && UserUtil.isAdmin((AuthenticatedUser)user)) {
            	user = user.getUserSpace().resolve(new DefaultUser(submitId));
            	subUser = user;
            }
            
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());

            
            boolean isMeet = "1".equals(root.elementTextTrim("meet"));
            
            String name = root.elementTextTrim("name");
            if (Check.isEmpty(name)) throw new NotFoundException("[blank activity]");
            
            // resolve the flow
            String flowUri = root.elementTextTrim("flow");
            String path = root.elementTextTrim("path");
            if (!Check.isEmpty(flowUri)) {
            	if (!flowUri.startsWith("/")) 
            		flowUri = UriHelper.absoluteUri(Check.isEmpty(path) ? "/home/process" : path, flowUri);
            	flowUri = RepositoryHelper.resolveUri(flowUri);
            } else {
            	flowUri = UriHelper.absoluteUri(
            			RepositoryHelper.getPrimaryRepository().getMount() + (isMeet ? "/meeting" : "/process"),
            			UriHelper.filterName(name));
            	
            	// auto meeting folder
            	if (isMeet) {
            		String meetFolder = RepositoryHelper.getPrimaryRepository().getMount() + "/meeting";
            		try {
            			RepositoryHelper.getNode(meetFolder, false);
            		} catch (NotFoundException nfe) {
            			RepositoryHelper.createCollection(meetFolder);
            		}
            	}
            	
            	flowUri = RepositoryHelper.getAvailableUri(flowUri);
            }
            
            
            HashMap<String, String> roles = new HashMap<String, String>();

            // Pre-assign some roles
            for (Element rolEl : (List<Element>)root.elements("role")) {
                String role = rolEl.attributeValue("role");
                String assignId = rolEl.attributeValue("assignId");
                if (role != null) role = role.trim();
                if (!Check.isEmpty(role) && !Check.isEmpty(assignId)) {
                    roles.put(role, assignId);
                }
            }
            
            String parentId = root.elementTextTrim("parent");
            String parentStep = root.elementTextTrim("parentStep");

            // Launch it!
            Activity activityEnt =
                uActivities.launch(
                	name,
                    root.elementTextTrim("description"),
                    flowUri,
                    RepositoryHelper.resolveUri(root.elementTextTrim("master-flow")),
                    parentId,
                    root.elementTextTrim("project"),
                    root.elementTextTrim("contextGroup"),
                    ActivityXML.parseDate(root.elementTextTrim("dueDate")),
                    roles);
            
            // is the submit user different than the thread user?
            if (subUser != null) {
            	((MutableRepositoryNode)activityEnt.getNode()).grantPermission(
            			new DefaultNodePermission(subUser, DefaultNodePermission.WRITE, true));
            }

            // Connect the parent step... this probably shouldn't be in the servlet
            if (!Check.isEmpty(parentStep) && !Check.isEmpty(parentId)) {
            	Activity parAct = uActivities.getActivity(parentId);
            	ActivityStepState aStat = parAct.getStates().get(parentStep);
            	if (aStat != null) {
            		aStat.setSubActivityId(activityEnt.getId());
            		HibernateUtil.getSession().update(aStat);
            	}
            }
            
            if (isMeet) {
            	
            	// flag repo node
            	MutableRepositoryNode actRepoNode = (MutableRepositoryNode)activityEnt.getNode();
            	HashMap<QName,String> propMap = new HashMap<QName,String>(1);
            	propMap.put(PropertyHelper.itensilQName("meet-setup"), "1");
            	PropertyHelper.setNodeValues(actRepoNode, propMap);
            	
            	// throw out initial end state
            	ActivityStepState endState = null;
            	for ( ActivityStepState actState : activityEnt.getStates().values()) {
            		if (actState.getSubState() == SubState.ENTER_END) {
            			endState = actState;
            			break;
            		}
            	}
            	if (endState != null) {
            		activityEnt.getStates().remove(endState.getStepId());
            		HibernateUtil.getSession().delete(endState);
            	}
            }
            
            // return activity's status
            Document retDoc = DocumentHelper.createDocument(ActivityXML.display(activityEnt));
            HibernateUtil.commitTransaction();

            retDoc.write(response.getWriter());
        }

    }

    /**
     *  /submit
     *
     * Submit an Activity for flow routing
     *
     * Request XML:
     *      <submit>
     *          <activity>activity.id</activity>
     *          <step>step.id</step>
     *          <expr>expression</expr>
     *
     *          <!-- maybe this for more wiki inputs? -->
     *          <data name="...">value</data> ...
     *      </submit>
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webSubmit(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("submit".equals(root.getName())) {

            User user = (User)request.getUserPrincipal();

            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            UserActivities uActivities = new UserActivities(user, session);

            // resolve the activity
            String activityId = root.elementTextTrim("activity");
            Activity activityEnt = uActivities.getActivity(activityId);
            if (activityEnt == null) {
                throw new NotFoundException(activityId);
            }
            String step = root.elementTextTrim("step");
            if (Check.isEmpty(step)) {
                throw new NotFoundException("[empty step]");
            }

            HashMap<String,String> submitValues = null;
            Element data = root.element("rule-data");
            if (data != null){
            	List<Element> kids = data.elements();
            	if (!kids.isEmpty()) {
            		submitValues = new HashMap<String,String>();
            		for (Element attrElem : kids) {
            			submitValues.put(attrElem.getName(), attrElem.getText());
            		}
            	}
            }
            Element swPath = root.element("switch-path");
            String resTx;
            if (swPath != null && swPath.hasContent()) {
            	resTx = uActivities.exitSwitch(activityEnt, step, swPath.getTextTrim());
            } else  {
            	resTx = uActivities.submit(activityEnt, step, submitValues, root.elementTextTrim("expr"));
            }

            // return activity's status
            Element actElem = ActivityXML.display(activityEnt);
            actElem.addAttribute("resTx", resTx);
            
            Document retDoc = DocumentHelper.createDocument(actElem);
            HibernateUtil.commitTransaction();

            retDoc.write(response.getWriter());
        }

    }



    /**
     *  /undo
     *
     * Undo last activity event
     *
     * Parameters:
     *  activity - id
     *
     */
    @ContentType("text/xml")
    public void webUndo(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        String txId = request.getParameter("txId");
        
        if (Check.isEmpty(txId)) {
        	throw new NotFoundException("[Transaction not found]");
        }
        
        Activity activityEnt = uActivities.getActivity(id);
        if (activityEnt == null) {
        	throw new NotFoundException(id);
        }

        uActivities.undo(activityEnt, txId);

        Element iElem = ActivityXML.display(activityEnt);
        HibernateUtil.commitTransaction();

        Document doc = DocumentHelper.createDocument(iElem);
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    
    
    /**
     *  /sendToFlow
     *
     * Move activity to the begining of a different process
     *
     * Parameters:
     *  activity - id
     *	flow
     *  master-flow
     */
    @ContentType("text/xml")
    public void webSendToFlow(HttpServletRequest request, HttpServletResponse response) throws Exception {

    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"activity", "flow", "master-flow"});
    	
        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = params.get("activity");
        Activity activityEnt = uActivities.getActivity(id);
        
        activityEnt = uActivities.sendToFlow(activityEnt, 
        		RepositoryHelper.resolveUri(params.get("flow")), 
        		RepositoryHelper.resolveUri(params.get("master-flow")));

        Element iElem = ActivityXML.display(activityEnt);
        HibernateUtil.commitTransaction();

        Document doc = DocumentHelper.createDocument(iElem);
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /rebuildFolder
     *
     * Rebuild an activity's folder
     *
     * Parameters:
     *  activity - id
     *
     */
    @ContentType("text/xml")
    public void webRebuildFolder(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        Activity activity = uActivities.getActivity(id);
        if (activity == null) {
            throw new NotFoundException(id);
        }
        
        try {
        	activity = uActivities.rebuildActivityFolder(activity);
	    } catch (NotFoundException nfe) {
	    	response.getWriter().print("<missing-flow/>\n");
	    }
        
        Element iElem = ActivityXML.display(activity);
        try {
        	iElem.addAttribute("uri", activity.getNode().getUri());
        } catch (NotFoundException nfe) {
        	throw new ActivityInvalidException("Missing repository node", nfe);
        }
        
        HibernateUtil.commitTransaction();

        Document doc = DocumentHelper.createDocument(iElem);
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    /**
     *  /actvitiyInfo
     *
     * Get actvitiy info
     *
     * Parameters:
     *  activity - id
     *
     */
    @ContentType("text/xml")
    public void webActivityInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        Activity activity = uActivities.getActivity(id);
        if (activity == null) {
            throw new NotFoundException(id);
        }
        Element iElem = ActivityXML.display(activity);
        try {
        	iElem.addAttribute("uri", activity.getNode().getUri());
        } catch (NotFoundException nfe) {
        	throw new ActivityInvalidException("Missing repository node", nfe);
        }
        if (activity.getDescription() == null) {
        	iElem.addAttribute("description", "");
        }
        if (activity.getContextGroupId() == null) {
        	iElem.addAttribute("contextGroup", "");
        }
        
        HibernateUtil.commitTransaction();

        Document doc = DocumentHelper.createDocument(iElem);
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    /**
     *  /setList
     *
     * Get actvitiy info
     *
     * Parameters:
     *  activity - id[]
     *
     */
    @ContentType("text/xml")
    public void webSetList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String ids[] = request.getParameterValues("id");
        
        Document resDoc = DocumentHelper.createDocument();
    	Element resRoot = resDoc.addElement("activities");
    	
        for (String id : ids) {
	        Activity activity = uActivities.getActivity(id);
	        if (activity != null) {
	        	resRoot.add(ActivityXML.display(activity));
	        }
        }
        
        HibernateUtil.commitTransaction();
        ServletUtil.setExpired(response);
        resDoc.write(response.getWriter());
    }
    
    /**
     *  /getLogsAndPlans
     *
     * Get actvitiy info
     *
     * Parameters:
     *  activity - id
     *
     */
    @ContentType("text/xml")
    public void webGetLogsAndPlans(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        Activity activity = uActivities.getActivity(id);
        if (activity == null) {
            throw new NotFoundException(id);
        }
        Element iElem = ActivityXML.display(activity);
        try {
        	iElem.addAttribute("uri", activity.getNode().getUri());
        } catch (NotFoundException nfe) {
        	throw new ActivityInvalidException("Missing repository node", nfe);
        }
        Pair<Date,Date> minMaxDates = new Pair<Date,Date>(activity.getStartDate(), activity.getDueDate());
        
        iElem.add(ActivityXML.logs(activity, minMaxDates));
        iElem.add(ActivityXML.plans(activity, minMaxDates));
        
        
        HibernateUtil.commitTransaction();
        if (minMaxDates.first == null) minMaxDates.first = new Date();
        if (minMaxDates.second == null) minMaxDates.second = new Date();
        if (minMaxDates.first.after(minMaxDates.second)) {
        	Date hd = minMaxDates.first;
        	minMaxDates.first = minMaxDates.second;
        	minMaxDates.second = hd;
        }
        iElem.addAttribute("minDate", ActivityXML.dateFmtZ.format(minMaxDates.first));
        iElem.addAttribute("maxDate", ActivityXML.dateFmtZ.format(minMaxDates.second));

        Document doc = DocumentHelper.createDocument(iElem);
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }


    /**
     *  /getPlan
     *
     * Get activity plan
     *
     * Parameters:
     *  activity - id
     *  step - step
     *
     */
    @ContentType("text/xml")
    public void webGetPlan(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        String step = request.getParameter("step");
        if (Check.isEmpty(step)) throw new NotFoundException("[blank step]");

        Activity activity = uActivities.getActivity(id);

        ActivityPlan plan = activity.getPlans().get(step);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("plan");
        root.addAttribute("stepId", step);
        root.addAttribute("activity", activity.getId());
        if (plan != null) {
            root.addAttribute("skip", plan.isSkip() ? "1" : "0");
            if (plan.getDueDate() != null) root.addAttribute("dueDate", ActivityXML.dateFmtZ.format(plan.getDueDate()));
            if (plan.getAssignId() != null) root.addAttribute("assignId", plan.getAssignId());
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /setPlan
     *
     * Set activity plan
     *
     * Parameters:
     *  activity - id
     *  step - step
     *  skip - 1/0
     *
     */
    @ContentType("text/xml")
    public void webSetPlan(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        String step = request.getParameter("step");
        String skip = request.getParameter("skip");
        String dueDate = request.getParameter("dueDate");
        if (Check.isEmpty(step)) throw new NotFoundException("[blank step]");

        Activity activity = uActivities.getActivity(id);
        ActivityPlan plan = activity.getPlans().get(step);
        if (plan == null) {
            plan = new ActivityPlan();
            plan.setActivity(activity);
            plan.setStepId(step);
            activity.getPlans().put(step, plan);
        }
        plan.setSkip("1".equals(skip));
        HibernateUtil.getSession().saveOrUpdate(plan);
        
        if (dueDate != null)  {
        	boolean saveAct = uActivities.setStepDates(activity, step, null, dueDate.length() > 0 ? ActivityXML.parseDate(dueDate) : null);
        	if (saveAct) HibernateUtil.getSession().saveOrUpdate(activity);
        }

        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }

    
    /**
     *  /setAssign
     *
     * Set activity plan
     *
     * Parameters:
     *  activity - id
     *  step - step
     *  assignId - userId
     *
     */
    @ContentType("text/xml")
    public void webSetAssign(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("activity");
        String step = request.getParameter("step");
        String assignId = request.getParameter("assignId");
        if (Check.isEmpty(step) || Check.isEmpty(assignId)) throw new NotFoundException("[blank step]");

        Activity activity = uActivities.getActivity(id);
        uActivities.assignStep(activity, step, assignId, (MutableRepositoryNode)activity.getNode());
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }
    
    /**
     *  /setProps
     *
     * Get activity info
     *
     *  <activity/>
     *
     */
    @ContentType("text/xml")
    public void webSetProps(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document reqDoc = XMLDocument.readStream(request.getInputStream());
        Element root = reqDoc.getRootElement();
        String id = root.attributeValue("id");
        if (!Check.isEmpty(id)) {
            HibernateUtil.beginTransaction();
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            Activity activityEnt = uActivities.getActivity(id);

            String reqName = root.attributeValue("name");
            if (!Check.isEmpty(reqName)) {
                if (!activityEnt.getName().equals(reqName)) {
                    MutableRepositoryNode node = (MutableRepositoryNode)activityEnt.getNode();
                    String parUri = UriHelper.getParent(node.getUri());
                    String uri = RepositoryHelper.getAvailableUri(
                            UriHelper.absoluteUri(parUri, UriHelper.filterName(reqName)));
                    node.move(uri);
                    reqName = UriHelper.name(uri);
                }
                activityEnt.setName(reqName);
            }
            activityEnt.setDescription(Check.emptyIfNull(root.attributeValue("description")).trim());
            activityEnt.setDueDate(ActivityXML.parseDate(root.attributeValue("dueDate")));
            activityEnt.setStartDate(ActivityXML.parseDate(root.attributeValue("startDate")));
            activityEnt.setContextGroupId(Check.emptyIfNull(root.attributeValue("contextGroup")).trim());
            
            HibernateUtil.getSession().update(activityEnt);
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /flowInfo
     *
     * Get flow info
     *
     * Parameters:
     *  flow - uri
     *  masterFlow - optional uri
     *
     */
    @ContentType("text/xml")
    public void webFlowInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // resolve the flow
        String flowUri = request.getParameter("flow");
        if (Check.isEmpty(flowUri)) throw new NotFoundException("[blank]");
        MutableRepositoryNode flowNode;
        FlowState flowState = null;
        FlowModel flowMod = new FlowModel();
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        flowUri = RepositoryHelper.resolveUri(flowUri);
        String icon = null;
        try {
            flowNode = RepositoryHelper.getNode(flowUri, false);
            flowState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flowNode.getNodeId());
            
            String style = flowNode.getPropertyValue(PropertyHelper.itensilQName("style"));
            if (Check.isEmpty(style)) {
                FlowSAXHandler hand = new FlowSAXHandler(flowMod);
                hand.parse(RepositoryHelper.loadContent(UriHelper.absoluteUri(flowUri, "chart.flow")));
            } else {
            	if (style.startsWith("icon:"))
            		icon = style.substring(5);
            }

        } catch (NotFoundException nfe) {

            // not-found, check for master-flow
            String mastUri = request.getParameter("masterFlow");
            if (Check.isEmpty(mastUri)) throw nfe;
            MutableRepositoryNode mastNode = RepositoryHelper.getNode(mastUri, false);
            String style = mastNode.getPropertyValue(PropertyHelper.itensilQName("style"));
            if (Check.isEmpty(style)) {
            	FlowSAXHandler hand = new FlowSAXHandler(flowMod);
            	hand.parse(RepositoryHelper.loadContent(UriHelper.absoluteUri(mastUri, "chart.flow")));
            } else {
            	if (style.startsWith("icon:"))
            		icon = style.substring(5);
            }

        }
        if (icon == null) {
        	AppElement activityType = flowMod.matchAppElements("http://itensil.com/workflow", "type").iterator().next();
            if (activityType != null) {
                icon = activityType.getAttribute("icon");
            }
        }

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("flow-info");

        // get icon
        root.addAttribute("icon", Check.isEmpty(icon) ? "def" : icon);
        root.addAttribute("active", (flowState != null && flowState.isActive()) ? "1" : "0");

        HibernateUtil.commitTransaction();

        ServletUtil.cacheTimeout(response, 37);
        // send info
        doc.write(response.getWriter());
    }

    /**
     *  /move
     *
     * Move a sub-activity
     *
     * Parameters:
     *  srcId - activity being moved
     *  dstId - destination
     */
    @ContentType("text/xml")
    public void webMove(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String srcId = request.getParameter("srcId");
        String dstId = request.getParameter("dstId");
        Activity activity = uActivities.getActivity(srcId);
        if (Check.isEmpty(dstId)) {
            activity.changeParent(null);
        } else {
            Activity dstActivity = uActivities.getActivity(dstId);
            activity.changeParent(dstActivity);
        }
        HibernateUtil.getSession().saveOrUpdate(activity);
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }

    /**
     *  /delete
     *
     * Delete an activity
     *
     * Parameters:
     *  id - activity.id
     *
     */
    @ContentType("text/xml")
    public void webDelete(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        String id = request.getParameter("id");
        Activity activity = uActivities.getActivity(id);
        if (activity == null) {
            throw new NotFoundException(id);
        }
        uActivities.delete(activity);
        HibernateUtil.commitTransaction();
        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }

    /**
     *  /roleList
     *
     * Get roles
     *
     * Parameters:
     *  flowUri
     *
     */
    @ContentType("text/xml")
    public void webRoleList(HttpServletRequest request, HttpServletResponse response) throws Exception {

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        String activityId = request.getParameter("activity");
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("role-list");
        if (Check.isEmpty(activityId)) {
            FlowState fState = getFlowState(request.getParameter("flowUri"));
            for (FlowRole role : fState.getRoles().values()) {
                Element elem = root.addElement("role");
                elem.addAttribute("role", role.getRole());
                elem.addAttribute("assignId", role.getAssignId());
            }
        } else {
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            Activity activity = uActivities.getActivity(activityId);
            for (ActivityRole role : activity.getRoles().values()) {
                Element elem = root.addElement("role");
                elem.addAttribute("role", role.getRole());
                elem.addAttribute("assignId", role.getAssignId());
            }
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /setRole
     *
     * Set a role
     *
     * Parameters:
     *  flowUri
     *  role
     *  assignId
     *
     */
    @ContentType("text/xml")
    public void webSetRole(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // validation
        String role = request.getParameter("role");
        if (Check.isEmpty(role)) throw new NotFoundException("[blank role]");
        String assignId = request.getParameter("assignId");

        HibernateUtil.beginTransaction();
        User self = (User)request.getUserPrincipal();
        User assignUsr = Check.isEmpty(assignId) ? null : self.getUserSpace().resolve(new DefaultUser(assignId));
        if (assignUsr == null) throw new NotFoundException("[user]");

        String activityId = request.getParameter("activity");
        UserActivities uActivities = new UserActivities(self, HibernateUtil.getSession());
        
        if (Check.isEmpty(activityId)) {
            FlowState fState = getFlowState(request.getParameter("flowUri"));
            uActivities.setFlowRole(fState, role, assignUsr);
        } else {
            Activity activity = uActivities.getActivity(activityId);
            uActivities.setActivityRole(activity, role, assignUsr, true);
        }
        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }


    /**
     *  /clearRole
     *
     * Clear a role
     *
     * Parameters:
     *  flowUri
     *  role
     *
     */
    @ContentType("text/xml")
    public void webClearRole(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // validation
        String role = request.getParameter("role");
        if (Check.isEmpty(role)) throw new NotFoundException("[blank role]");

        HibernateUtil.beginTransaction();

        String activityId = request.getParameter("activity");
        if (Check.isEmpty(activityId)) {
            FlowState fState = getFlowState(request.getParameter("flowUri"));
            FlowRole rolEnt = fState.getRoles().get(role);
            if (rolEnt != null) {
                fState.getRoles().remove(rolEnt.getRole());
                HibernateUtil.getSession().delete(rolEnt);
            }
        } else {
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            Activity activity = uActivities.getActivity(activityId);
            ActivityRole rolEnt = activity.getRoles().get(role);
            if (rolEnt != null) {
                activity.getRoles().remove(rolEnt.getRole());
                HibernateUtil.getSession().delete(rolEnt);
            }
        }

        HibernateUtil.commitTransaction();

        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }
    
    
    /**
     *  /getTimer
     *
     * Get an Activity Timer
     *
     * Parameters:
     *  activity
     *  tiemrId
     *
     */
    @ContentType("text/xml")
    public void webGetTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String activityId = request.getParameter("activity");
    	String timerId = request.getParameter("timer");
        if (!Check.isEmpty(activityId) && !Check.isEmpty(timerId)) {
        	
        	HibernateUtil.beginTransaction();
        	HibernateUtil.readOnlySession();
        	
        	User user = (User)request.getUserPrincipal();
        	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        	Activity activity = uActivities.getActivity(activityId);
        	ActivityTimer att = uActivities.getActivityTimer(activity, timerId);
        	Document doc = DocumentHelper.createDocument();
        	Element elem = doc.addElement("activity-timer");
        	elem.addAttribute("activity", activityId);
        	elem.addAttribute("timerId", timerId);
        	if (att != null) {
        		elem.addAttribute("conditional", att.isConditional() ? "1" : "0");
        		if (att.getAtTime() != null) elem.addAttribute("atTime", ActivityXML.dateFmtZ.format(att.getAtTime()));
        	} else {
        		elem.addAttribute("conditional", "0");
        		elem.addAttribute("atTime", "");
        	}
        	HibernateUtil.commitTransaction();
        	
        	ServletUtil.setExpired(response);
        	doc.write(response.getWriter());

        } else {
        	throw new NotFoundException("[blank activity or timer]");
        }
    }
    
    /**
     *  /setManualTimer
     *
     * Maually Set an Activity Timer
     *
     * Parameters:
     *  activity
     *  timer
     *
     */
    @ContentType("text/xml")
    public void webSetManualTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String activityId = request.getParameter("activity");
    	String timerId = request.getParameter("timer");
    	String atTime = request.getParameter("at"); 
        if (!Check.isEmpty(activityId) && !Check.isEmpty(timerId) && !Check.isEmpty(atTime)) {
        	
        	HibernateUtil.beginTransaction();
        	
        	User user = (User)request.getUserPrincipal();
        	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        	Activity activity = uActivities.getActivity(activityId);
        	uActivities.setManualTimer(activity, timerId, ActivityXML.parseDate(atTime));
        	
        	HibernateUtil.commitTransaction();
        	
        	ServletUtil.setExpired(response);
            response.getWriter().print("<ok/>");
        } else {
        	throw new NotFoundException("[blank activity or timer]");
        }
    }
    
    /**
     *  /processList
     *
     * Get a list of process and include their icons
     *
     * Parameters:
     *  uri - processes folder uri
     *
     */
    @ContentType("text/xml")
    public void webProcessList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        String uri = request.getParameter("uri");
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
        	uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
            if (!node.isCollection()) {
                node = (MutableRepositoryNode)node.getParent();
            }
            String pUri = node.getUri();
            root.addAttribute("uri", pUri);
            for (RepositoryNode kid : node.getChildren()) {
            	if (kid.isCollection()) {
            		MutableRepositoryNode mkid = (MutableRepositoryNode)kid;
            		String kName = UriHelper.localizeUri(pUri, kid.getUri());
            		Element rnElem = root.addElement("node");
                    rnElem.addAttribute("uri", kName);
                    rnElem.addAttribute("owner", kid.getOwner().getUserId());
                    String style = mkid.getPropertyValue(PropertyHelper.itensilQName("style"));
                    rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
                    String val;
                    
                    val =  mkid.getPropertyValue(PropertyHelper.itensilQName("description"));
                    rnElem.addAttribute("description", val == null ? "" : val);
                    
                    val =  mkid.getPropertyValue(PropertyHelper.defaultQName("getlastmodified"));
                    rnElem.addAttribute("lastmodified", val == null ? "" : val);
            	}
            }
            
        }
        RepositoryHelper.commitTransaction();
        ServletUtil.cacheTimeout(response, 37);
        doc.write(response.getWriter());
    }
    
    /**
     *  /activeProcesses
     *
     * Get a list of active process and include their icons
     *
     *
     */
    @ContentType("text/xml")
    public void webActiveProcesses(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
    	User user = (User)request.getUserPrincipal();
    	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
    	
    	root.addAttribute("uri", UriHelper.absoluteUri(RepositoryHelper.getPrimaryRepository().getMount(), "process"));

        for (RepositoryNode kid : uActivities.getActiveFlows(22)) {
        	if (kid.isCollection()) {
        		String kName = UriHelper.name(kid.getUri());
        		Element rnElem = root.addElement("node");
                rnElem.addAttribute("uri", kName);
                rnElem.addAttribute("owner", kid.getOwner().getUserId());
                String style = ((MutableRepositoryNode)kid).getPropertyValue(
                		PropertyHelper.itensilQName("style"));
                rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
        	}
        }
            
        RepositoryHelper.commitTransaction();
        ServletUtil.cacheTimeout(response, 13);
        doc.write(response.getWriter());
    }
    
    /**
     *  /modifiedProcesses
     *
     * Get a list of active process and include their icons
     *
     *
     */
    @ContentType("text/xml")
    public void webModifiedProcesses(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        
        
        NodePermission creatPerm = DefaultNodePermission.createPermission(request.getUserPrincipal());
        Repository repo =  RepositoryHelper.getPrimaryRepository();
        String procUri = UriHelper.absoluteUri(repo.getMount(), "process");
        root.addAttribute("uri", procUri);
        RepositoryNode procFold = repo.getNodeByUri(procUri, false);
        int count = 0;
        for (MutableRepositoryNode kid : repo.getRecentlyModified(
        		UriHelper.absoluteUri(repo.getMount(), "process/%/chart.flow"), 22 * 2, creatPerm)) {
        	
        	RepositoryNode fold = kid.getParent();
        	if (fold.getParentNodeId().endsWith(procFold.getNodeId())) {
	    		String kName = UriHelper.name(fold.getUri());
	    		Element rnElem = root.addElement("node");
	            rnElem.addAttribute("uri", kName);
	            rnElem.addAttribute("owner", fold.getOwner().getUserId());
	            String style = ((MutableRepositoryNode)fold).getPropertyValue(
	            		PropertyHelper.itensilQName("style"));
	            rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
	            count++;
	            if (count >= 22) break;
        	}
        }
            
        RepositoryHelper.commitTransaction();
        ServletUtil.cacheTimeout(response, 13);
        doc.write(response.getWriter());
    }
    
    
    /**
     *  /modifiedCourses
     *
     * Get a list of active process and include their icons
     *
     *
     */
    @ContentType("text/xml")
    public void webModifiedCourses(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        
        
        NodePermission creatPerm = DefaultNodePermission.createPermission(request.getUserPrincipal());
        Repository repo =  RepositoryHelper.getPrimaryRepository();
        String procUri = UriHelper.absoluteUri(repo.getMount(), "course");
        root.addAttribute("uri", procUri);
        RepositoryNode procFold = repo.getNodeByUri(procUri, false);
        int count = 0;
        for (MutableRepositoryNode kid : repo.getRecentlyModified(
        		UriHelper.absoluteUri(repo.getMount(), "course/%/chart.flow"), 22 * 2, creatPerm)) {
        	
        	RepositoryNode fold = kid.getParent();
        	if (fold.getParentNodeId().endsWith(procFold.getNodeId())) {
	    		String kName = UriHelper.name(fold.getUri());
	    		Element rnElem = root.addElement("node");
	            rnElem.addAttribute("uri", kName);
	            rnElem.addAttribute("owner", fold.getOwner().getUserId());
	            String style = ((MutableRepositoryNode)fold).getPropertyValue(
	            		PropertyHelper.itensilQName("style"));
	            rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
	            count++;
	            if (count >= 22) break;
        	}
        }
            
        RepositoryHelper.commitTransaction();
        ServletUtil.cacheTimeout(response, 13);
        doc.write(response.getWriter());
    }
    
    /**
     *  /saveStatus
     *
     * Save due dates and assignments
     *
     *
     */
    @ContentType("text/xml")
    public void webSaveStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String activityId = request.getParameter("activity");
        if (!Check.isEmpty(activityId)) {
        	
        	Document doc = XMLDocument.readStream(request.getInputStream());
        	
            Element root = doc.getRootElement();
            if ("status".equals(root.getName())) {
            	HibernateUtil.beginTransaction();
            	
            	User user = (User)request.getUserPrincipal();
            	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            	Activity activity = uActivities.getActivity(activityId);
            	
            	if (((MutableRepositoryNode)activity.getNode()).hasPermission(DefaultNodePermission.writePermission(user))) {
	            	ActivityXML.updatePlans(
	            			root.element("plan-updates"), activity, uActivities, activity.getFlow().getNode());
            	}
            	
            	HibernateUtil.commitTransaction();
            	
            }
            
            ServletUtil.setExpired(response);
            response.getWriter().print("<ok/>");
            
        } else {
        	throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /saveProgress
     *
     * Save progress
     *
     *
     */
    @SuppressWarnings("unchecked")
    @ContentType("text/xml")
    public void webSaveProgress(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String activityId = request.getParameter("activity");
        if (!Check.isEmpty(activityId)) {
        	Document doc = XMLDocument.readStream(request.getInputStream());
        	Element root = doc.getRootElement();
        	
        	if ("progress".equals(root.getName())) {
				User user = (User)request.getUserPrincipal();
				
				HibernateUtil.beginTransaction();
				
				String stepId = root.attributeValue("stepId");
				String progress = root.attributeValue("progress"); 
				
				Session session = HibernateUtil.getSession();
				UserActivities uActivities = new UserActivities(user, session);
				Activity activity = uActivities.getActivity(activityId);
        		
				
				Element data = root.element("rule-data");
				if (data != null){
					List<Element> kids = data.elements();
					if (!kids.isEmpty()) {
						HashMap<String,String> submitValues = new HashMap<String,String>();
						for (Element attrElem : kids) {
							submitValues.put(attrElem.getName(), attrElem.getText());
						}
						XPathConditionEval<String> xpEval = uActivities.getConditionEval(activity);
						Document dataDoc = xpEval.getDefaultData(activity);
						xpEval.setDataValues(activity, dataDoc, submitValues);
						xpEval.saveDefaultData(activity, dataDoc);
					}
				}
				if (!Check.isEmpty(stepId) && !Check.isEmpty(progress)) {
					ActivityStepState actState = activity.getStates().get(stepId);
					if (actState != null) {
						actState.setProgress(Integer.parseInt(progress));
						session.update(actState);
					}
				}

				HibernateUtil.commitTransaction();
        	}
        	
        	ServletUtil.setExpired(response);
            response.getWriter().print("<ok/>");
            
        } else {
        	throw new NotFoundException("[blank]");
        }
    }
    
    @SuppressWarnings("unchecked")
    @ContentType("text/xml")
    public void webGanttSchedule(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String activityId = request.getParameter("activity");
    	String stepId = request.getParameter("step");
    	String startStr= request.getParameter("startDate");
    	String dueStr = request.getParameter("dueDate");
        if (!Check.isEmpty(activityId)) {
        	Date startDate = ActivityXML.parseDate(startStr);
        	Date dueDate = ActivityXML.parseDate(dueStr);
        	// TODO schedule call back
        	
        	
        	Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("step-dates");
            root.addAttribute("activity", activityId);
            root.addAttribute("forStep", stepId);
            // add Step sub-elements
            
            ServletUtil.setExpired(response);
            
            doc.write(response.getWriter());
             
        } else {
        	throw new NotFoundException("[blank]");
        }
    }
    
    protected FlowState getFlowState(String flowUri) throws NotFoundException, AccessDeniedException {
        if (Check.isEmpty(flowUri)) throw new NotFoundException("[blank]");
        flowUri = RepositoryHelper.resolveUri(flowUri);
        MutableRepositoryNode flowNode = RepositoryHelper.getNode(flowUri, false);
        if (!flowNode.isCollection()) flowNode = (MutableRepositoryNode)flowNode.getParent();
        FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flowNode.getNodeId());
        if (fState == null) {
            fState = new FlowState(flowNode.getNodeId());
            fState.setActive(true);
            HibernateUtil.getSession().persist(fState);
        }
        fState.setNode(flowNode);
        return fState;
    }

    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        HibernateUtil.rollbackTransaction();
    }

    /**
     * Clean-up
     */
    public void afterMethod() {
        HibernateUtil.closeSession();
    }

}
