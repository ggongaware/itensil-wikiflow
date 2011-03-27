package itensil.workflow.activities.web;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;
import org.hibernate.Session;

import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.DuplicateException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryNode;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;
import itensil.web.ContentType;
import itensil.web.MethodServlet;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.FlowColumn;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.activities.state.FlowStepLog;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.FlowSAXHandler;

public class ProjectServlet extends MethodServlet {
	
	/**
     *  /page
     *
     * Direct page
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String proj = request.getParameter("proj");
        if (Check.isEmpty(proj)) {
        	throw new NotFoundException("[blank]");
        }
        request.setAttribute("proj", proj);
        RepositoryHelper.beginTransaction();
        String mount = RepositoryHelper.getPrimaryRepository().getMount();
        String projKbUri = UriHelper.absoluteUri(
        		UriHelper.absoluteUri(UriHelper.absoluteUri(mount, "project"), proj), 
        		"Project.kb");
        try {
        	RepositoryHelper.getNode(projKbUri, false);
        } catch (NotFoundException nfe) {
        	try {
	        	RepositoryHelper.copyAndUpdate(
	        			projKbUri, "/system/sysproc/Project.kb", 
	        			(User)request.getUserPrincipal(), null);
        	} catch (DuplicateException de) { /* eat it */ }
        }
        RepositoryHelper.commitTransaction();
        ServletUtil.forward("/view-proj/index.jsp", request, response);
    }
    
    
	/**
     *  /gantt
     *
     * Timeline page
     *
     */
    public void webGantt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String proj = request.getParameter("proj");
        if (Check.isEmpty(proj)) {
        	throw new NotFoundException("[blank]");
        }
        request.setAttribute("proj", proj);
        ServletUtil.forward("/view-proj/gantt.jsp", request, response);
    }
    
    /**
     *  /activityList
     *
     * Direct page
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webActivityList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String proj = request.getParameter("proj");
    	boolean showFull = "1".equals(request.getParameter("full"));
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
    	String mount = RepositoryHelper.getPrimaryRepository().getMount();
    	String projUri = UriHelper.absoluteUri(UriHelper.absoluteUri(mount, "project"), proj);
    	MutableRepositoryNode projNode = RepositoryHelper.getNode(projUri, false);
    	
    	Session sess = HibernateUtil.getSession();
    	Query qry = sess.getNamedQuery("FlowState.getProjectFlows");
    	qry.setString("projId", projNode.getNodeId());
    	List<FlowState> flows = qry.list();
    	
    	// for each flow collect the columns
    	Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("project");
        
        // identification
        root.addAttribute("name", UriHelper.name(projNode.getUri()));
        root.addAttribute("id", projNode.getNodeId());
        Element descElem = root.addElement("description");
        String desc = projNode.getPropertyValue(PropertyHelper.itensilQName("description"));
        if (desc != null) descElem.setText(desc);
        
        HashMap<String, Integer> columnMap = null;
        if (showFull) {
	        // dataset columns
	        Element deElem = root.addElement("dataColumns");
	        
	        columnMap = new HashMap<String, Integer>();
	        HashMap<FlowColumn, Integer> uniqueMap = new HashMap<FlowColumn, Integer>();
	        for (FlowState flow : flows) {
	        	FlowModel flowMod = new FlowModel();
	        	mapColumn(flow.getCust0(), "0", deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust1(), "1",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust2(), "2",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust3(), "3",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust4(), "4",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust5(), "5",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust6(), "6",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust7(), "7",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust8(), "8",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCust9(), "9",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCustA(), "A",  deElem, columnMap, uniqueMap, flowMod);
	        	mapColumn(flow.getCustB(), "B",  deElem, columnMap, uniqueMap, flowMod);
	        }
        }

        // Project Activities
    	qry = sess.getNamedQuery("Activity.getProjectActivities");
    	qry.setString("projId", projNode.getNodeId());
    	qry.setFirstResult(0); // needed for mind scrambling hibernate bug
    	
    	Pair<Date,Date> minMaxDates = new Pair<Date,Date>(null, null);

    	List<Activity> acts = UserActivities.sortActs(qry.list());
    
    	for (Activity act : acts) {
    		Element actElem = ActivityXML.display(act);
    		if (showFull) {
    			Element dataElem = ActivityXML.data(act, columnMap);
    			actElem.add(dataElem);
    		}
    		root.add(actElem);
    		if (act.getStartDate() != null && 
    				 (minMaxDates.first == null || minMaxDates.first.after(act.getStartDate()))) 
    			 minMaxDates.first = act.getStartDate();
    		
    		if (act.getDueDate() != null && 
   				 	(minMaxDates.second == null || minMaxDates.second.before(act.getDueDate()))) 
   			 	minMaxDates.second = act.getDueDate();
    	}
    	
        if (minMaxDates.first == null) minMaxDates.first = new Date();
        if (minMaxDates.second == null) minMaxDates.second = new Date();
        if (minMaxDates.first.after(minMaxDates.second)) {
        	Date hd = minMaxDates.first;
        	minMaxDates.first = minMaxDates.second;
        	minMaxDates.second = hd;
        }
        root.addAttribute("minDate", ActivityXML.dateFmtZ.format(minMaxDates.first));
        root.addAttribute("maxDate", ActivityXML.dateFmtZ.format(minMaxDates.second));
    	
    	HibernateUtil.commitTransaction();

    	ServletUtil.setExpired(response);
    	doc.write(response.getWriter());
    }
    
    /**
     *  /newProject
     *  
     */
    @ContentType("text/xml")
    public void webNewProject(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"name", "description"});
    	
    	String name = params.get("name");
    	String description = params.get("description");
    	
    	if (Check.isEmpty(name)) throw new NotFoundException("[blank]");
    		
    	
    	RepositoryHelper.beginTransaction();
    	
    	String uri = UriHelper.absoluteUri("/home/project", name);
    	
    	uri = RepositoryHelper.getAvailableUri(RepositoryHelper.resolveUri(uri));
    	
    	MutableRepositoryNode projNode = 
    		RepositoryHelper.createCollection(uri);
    	
    	if (!Check.isEmpty(description)) {
	    	HashMap<QName,String> props = new HashMap<QName,String>();
	    	props.put(PropertyHelper.itensilQName("description"), description);
	    	
	    	PropertyHelper.setNodeValues(projNode, props);
    	}
    	
    	RepositoryHelper.commitTransaction();
    	
    	Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("project");
        root.addAttribute("name", UriHelper.name(projNode.getUri()));
        root.addAttribute("id", projNode.getNodeId());
        doc.write(response.getWriter());
    }
    
    /**
     *  /removeProject
     *  
     */
    @SuppressWarnings("unchecked")
    @ContentType("text/xml")
    public void webRemoveProject(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"projId"});
    	String projId = params.get("projId");
    	if (Check.isEmpty(projId)) throw new NotFoundException("[blank]");
    	
    	HibernateUtil.beginTransaction();
    	MutableRepositoryNode projNode = RepositoryHelper.getNodeById(projId, true);
    	String id = projNode.getNodeId();
    	projNode.remove();
    	
    	Query qry = HibernateUtil.getSession().getNamedQuery("Activity.getProjectActivities");
    	qry.setString("projId", id);
    	qry.setFirstResult(0); // needed for mind scrambling hibernate bug
    	List<Activity> delActs = qry.list();
    	for (Activity delAct : delActs) {
    		delAct.getProjects().remove(delActs);
    	}
    	HibernateUtil.commitTransaction();
    	
    	ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }
    
    /**
     * 	/listProjects
     *
     *
     */
    @ContentType("text/xml")
    public void webListProjects(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        String uri = request.getParameter("uri");
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        if (Check.isEmpty(uri)) uri = "/home/project";
        if (!(uri.length() < 2)) {
        	uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
            if (!node.isCollection()) {
                node = (MutableRepositoryNode)node.getParent();
            }
            String pUri = node.getUri();
            root.addAttribute("uri", pUri);
            for (RepositoryNode kid : node.getChildren()) {
            	if (kid.isCollection()) {
            		String kName = UriHelper.localizeUri(pUri, kid.getUri());
            		Element rnElem = root.addElement("node");
                    rnElem.addAttribute("uri", kName);
                    rnElem.addAttribute("id", kid.getNodeId());
                    rnElem.addAttribute("owner", kid.getOwner().getUserId());
                    String style = ((MutableRepositoryNode)kid).getPropertyValue(
                    		PropertyHelper.itensilQName("style"));
                    rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
            	}
            }
            
        }
        RepositoryHelper.commitTransaction();
        ServletUtil.cacheTimeout(response, 37);
        doc.write(response.getWriter());
    }
    
    /**
     * 	/dashList
     *
     *  dashboard list
     */
    @ContentType("text/xml")
    public void webDashList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("projects");
        UserActivities uActivities = new UserActivities((User)request.getUserPrincipal(), HibernateUtil.getSession());
        
        for (MutableRepositoryNode pNode : uActivities.getAssignedProjectNodes()) {
        	String kName = UriHelper.name(pNode.getUri());
    		Element rnElem = root.addElement("project");
            rnElem.addAttribute("uri", kName);
            rnElem.addAttribute("id", pNode.getNodeId());
            rnElem.addAttribute("owner", pNode.getOwner().getUserId());
            rnElem.addAttribute("description",
            		pNode.getPropertyValue(PropertyHelper.itensilQName("description")));
            String style = pNode.getPropertyValue(
            		PropertyHelper.itensilQName("style"));
            rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
            for (Activity act : uActivities.getActiveProjectActivities(pNode.getNodeId(), 3)) {
            	rnElem.add( ActivityXML.display(act));
            }
        }
        
        HibernateUtil.commitTransaction();
        ServletUtil.cacheTimeout(response, 37);
        doc.write(response.getWriter());
    }
    
    /**
     * 	/meetDash
     *
     *  dashboard list for meetings
     */
    @ContentType("text/xml")
    public void webMeetDash(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("meetings");
        UserActivities uActivities = new UserActivities((User)request.getUserPrincipal(), HibernateUtil.getSession());
        
        // Note: every node is still named 'project' because I'm lazy, and this is
        // 'rapid' developement
        for (MutableRepositoryNode pNode : uActivities.getAssignedMeetingNodes()) {
        	String kName = UriHelper.name(UriHelper.getParent(UriHelper.getParent(pNode.getUri())));
        	Activity act = uActivities.getActivity(pNode.getNodeId());
    		Element rnElem = root.addElement("project");
            rnElem.addAttribute("uri", kName);
            rnElem.addAttribute("id", pNode.getNodeId());
            rnElem.addAttribute("owner", pNode.getOwner().getUserId());
            rnElem.addAttribute("description", act.getDescription());
            String style = pNode.getPropertyValue(
            		PropertyHelper.itensilQName("style"));
            rnElem.addAttribute("style", Check.isEmpty(style) ? "icon:def" : style);
            rnElem.add( ActivityXML.display(act));
        }
        
        HibernateUtil.commitTransaction();
        ServletUtil.cacheTimeout(response, 37);
        doc.write(response.getWriter());
    }
    
    /**
     *  /addActivity
     *	
     *		projId - which project
     *		actId[] - which activities
     */
    @ContentType("text/xml")
    public void webAddActivity(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	Map<String, String[]> params = 
    		RequestUtil.readParameterArrays(request, new String[]{"projId", "actId"});
    	
    	addRemoveProjectActivities(true,
    			(User)request.getUserPrincipal(),
    			RequestUtil.castToString(params.get("projId")),
    			params.get("actId"));
        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }
    
    /**
     *  /removeActivity
     *	
     *		projId - which project
     *		actId[] - which activities
     */
    @ContentType("text/xml")
    public void webRemoveActivity(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String[]> params = 
    		RequestUtil.readParameterArrays(request, new String[]{"projId", "actId"});
    	
    	addRemoveProjectActivities(false,
    			(User)request.getUserPrincipal(),
    			RequestUtil.castToString(params.get("projId")),
    			params.get("actId"));
        ServletUtil.setExpired(response);
        response.getWriter().print("<ok/>");
    }
    
    /**
     * 
     * @param add
     * @param user
     * @param projId
     * @param actIds
     * @throws Exception
     */
    private void addRemoveProjectActivities(boolean add, User user, String projId, String actIds[]) 
    		throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	Session session = HibernateUtil.getSession();
        UserActivities uActivities = new UserActivities(user, session);
        if (Check.isEmpty(projId) || Check.isEmpty(actIds)) {
        	throw new NotFoundException("[blank project or actvity]");
        }
        MutableRepositoryNode projNode = RepositoryHelper.getNodeById(projId, false);
        
        for (int ii = 0; ii < actIds.length; ii++) {
        	if (!Check.isEmpty(actIds[ii])) {
        		Activity act = uActivities.getActivity(actIds[ii]);
        		deepAddRemoveActivity(add, act, projNode);
        	}
        }
        
        HibernateUtil.commitTransaction();
    }
    
    private void deepAddRemoveActivity(boolean add, Activity act, MutableRepositoryNode projNode) {
    	
    	if (add) act.getProjects().add(projNode.getNodeId());
    	else act.getProjects().remove(projNode.getNodeId());
    	
		for (Activity kid : act.getChildren()) {
			deepAddRemoveActivity(add, kid, projNode);
		}
    }
    
    private void mapColumn(
    		FlowColumn col,
    		String colIdx,
    		Element parent, 
    		Map<String, Integer> columnMap, 
    		Map<FlowColumn, Integer> uniqueMap,
    		FlowModel flowMod) {

    	if (col == null) return;
    	Integer dupInt;
    	String key = col.getFlow().getId() + colIdx;
    	if ((dupInt = uniqueMap.get(col)) == null) {
    		Integer mapInt = new Integer(uniqueMap.size());
    		uniqueMap.put(col, mapInt);
    		columnMap.put(key, mapInt);
    		Element colElem = parent.addElement("column");
    		String cName = col.getName();
    		String cType = col.getType();
        	colElem.addAttribute("name", cName);
        	colElem.addAttribute("type", cType);
        	if (cType.startsWith("xsd:NMTOKEN")) {
	        	if (!flowMod.isLoaded()) {
	        		try {
	        			String modUri = UriHelper.absoluteUri(col.getFlow().getNode().getUri(), "chart.flow");
	        			flowMod.loadXML(RepositoryHelper.loadContent(modUri));
	        		} catch (Exception ex) {
	        			log("Process attribute read error", ex);
	        		}
	        	}
    			Collection<AppElement> elems = flowMod.matchAppElements("http://itensil.com/workflow", "data");
    			if (!elems.isEmpty()) {
    				 AppElement dataEl = elems.iterator().next();
    				 Collection<AppElement> attrElems = dataEl.matchChildElements("attr");
    				 for (AppElement attrEl : attrElems) {
    					 if (cName.equals(attrEl.getAttribute("name"))) {
    						 for (AppElement itemEl : attrEl.matchChildElements("item")) {
    							 Element colItemEl = colElem.addElement("item");
    							 colItemEl.addAttribute("label", itemEl.getAttribute("label"));
    							 colItemEl.addAttribute("value", itemEl.getAttribute("value"));    							 
    						 }
    						 break;
    					 }
    				 }
    			}
        	}
    	} else {
    		columnMap.put(key, dupInt);
    	}
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
