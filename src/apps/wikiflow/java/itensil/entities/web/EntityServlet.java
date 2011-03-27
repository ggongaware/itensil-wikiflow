package itensil.entities.web;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocumentFactory;

import itensil.entities.EntityManager;
import itensil.entities.EntityModelUpdater;
import itensil.entities.EntityRecordIndexer;
import itensil.entities.EntityRecordSet;
import itensil.entities.hibernate.EntityActivity;
import itensil.io.HibernateUtil;
import itensil.io.StreamUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodePermission;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.NodeProperties;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryNode;
import itensil.repository.hibernate.NodeEntity;
import itensil.repository.hibernate.VersionEntity;
import itensil.repository.web.WebdavServlet;
import itensil.security.DefaultGroup;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;
import itensil.web.ContentType;
import itensil.web.HTMLEncode;
import itensil.web.MethodServlet;
import itensil.web.ServletUtil;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.FlowModel;

public class EntityServlet extends MethodServlet {
	
	
	/**
     *  /page
     *
     * Direct page
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ServletUtil.forward("/view-entity/index.jsp", request, response);
    }
	
	
    
	/**
     *  /edit
     *
     * Direct page
     *
     */
    public void webEdit(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	ServletUtil.forward("/view-entity/edit.jsp", request, response);
    }
    
    
    /**
     *  /recordList
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webRecordList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String entity = request.getParameter("entity");
    	
    	if (Check.isEmpty(entity)) throw new NotFoundException("[blank]");

    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        EntityManager entMan = new EntityManager((User)request.getUserPrincipal());
        
    	// EntityRecordSet
        Document doc = entMan.recordList(entity);
    	
    	RepositoryHelper.commitTransaction();
    	ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    /**
     *  /recordsFind
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webRecordsFind(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String entity = request.getParameter("entity");
    	if (Check.isEmpty(entity)) throw new NotFoundException("[blank]");
    	
    	String ids[] = request.getParameterValues("id");
    	String activity = request.getParameter("activity");
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        EntityManager entMan = new EntityManager((User)request.getUserPrincipal());

    	
        //  EntityRecordSet
        Document doc;
    	
        if (!Check.isEmpty(ids)) {
        	doc = entMan.recordsFind(entity, ids);
        } else if ((!Check.isEmpty(activity))) {
        	doc = entMan.recordsFindByActivity(entity, activity, request.getParameter("relName"));
        } else {
        	doc = entMan.emptyRecords(entity);
        }
        RepositoryHelper.commitTransaction();
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    /**
     *  /createRecord
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webCreateRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String entity = request.getParameter("entity");
    	
    	if (Check.isEmpty(entity)) throw new NotFoundException("[blank]");
    	
    	RepositoryHelper.beginTransaction();
    	EntityManager entMan = new EntityManager((User)request.getUserPrincipal());
        
        MutableRepositoryNode resNod;
        
        String activity = request.getParameter("activity");
        if ((!Check.isEmpty(activity))) {
        	resNod = entMan.createRecordForActivity(entity, activity, request.getParameter("relName"));
        } else {
        	resNod = entMan.createRecord(entity);
        }
        
        
        RepositoryHelper.commitTransaction();
        
        // expire since probably get
        ServletUtil.setExpired(response);
        
        Document doc = DocumentHelper.createDocument();
        doc.addElement("ok").addAttribute("result", resNod.getUri());
        doc.write(response.getWriter());
    }
    
    /**
     *  /joinRecord
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webJoinRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String entity = request.getParameter("entity");
    	String recordId = request.getParameter("recordId");
    	String activity = request.getParameter("activity");
    	
    	if (Check.isEmpty(entity)
    			|| Check.isEmpty(recordId) 
    			|| Check.isEmpty(activity)) throw new NotFoundException("[blank]");
    	
    	RepositoryHelper.beginTransaction();
    	
    	User user = (User)request.getUserPrincipal();
    	
    	EntityManager entMan = new EntityManager(user);
    	
    	String entRootUri = entMan.getModelRootUri(entity);
    	
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
    	
    	String relName = Check.emptyIfNull(request.getParameter("relName"));
    	
    	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        Activity actEnt = uActivities.getActivity(activity);
        if (actEnt == null) throw new NotFoundException(activity);
        
        
        if (!Check.isEmpty(actEnt.getContextGroupId())) {
        	MutableRepositoryNode entRecNod = 
        		RepositoryHelper.getNode(UriHelper.getParent(entMan.recordDataUri(entity, recordId)), true);
        	if (entRecNod.getContextGroup() == null) {
        		entRecNod.setContextGroup(new DefaultGroup(actEnt.getContextGroupId()));
        		RepositoryHelper.saveNode(entRecNod);
        	}
        }
        
        EntityActivity entAct = new EntityActivity();
        entAct.initNew();
        entAct.setActivity(actEnt);
        entAct.setEntityId(entRepoRoot.getNodeId());
        entAct.setName(relName);
        entAct.setRecordId(Long.parseLong(recordId));
        
        HibernateUtil.getSession().save(entAct);
        
        RepositoryHelper.commitTransaction();
        
        // expire since probably get
        ServletUtil.setExpired(response);
    	
    	
    	Document doc = DocumentHelper.createDocument();
        doc.addElement("ok");
        doc.write(response.getWriter());
    }
    
    
    
    /**
     *  /leaveRecord
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webLeaveRecord(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	String entity = request.getParameter("entity");
    	String recordId = request.getParameter("recordId");
    	String activity = request.getParameter("activity");
    	
    	if (Check.isEmpty(entity)
    			|| Check.isEmpty(recordId) 
    			|| Check.isEmpty(activity)) throw new NotFoundException("[blank]");
    	
    	RepositoryHelper.beginTransaction();
    	
    	User user = (User)request.getUserPrincipal();
    	
    	EntityManager entMan = new EntityManager(user);
    	
    	String entRootUri = entMan.getModelRootUri(entity);
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
    	
    	String relName = Check.emptyIfNull(request.getParameter("relName"));
    	
    	UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        Activity actEnt = uActivities.getActivity(activity);
        if (actEnt == null) throw new NotFoundException(activity);
        EntityActivity entAct = new EntityActivity();
        entAct.setActivity(actEnt);
        entAct.setEntityId(entRepoRoot.getNodeId());
        entAct.setName(relName);
        entAct.setRecordId(Long.parseLong(recordId));
        
        HibernateUtil.getSession().delete(entAct);
        
        RepositoryHelper.commitTransaction();
        
        // expire since probably get
        ServletUtil.setExpired(response);
    	
    	
    	Document doc = DocumentHelper.createDocument();
        doc.addElement("ok");
        doc.write(response.getWriter());
    }

    /**
     *  /getModel
     *
     * 
     *
     */
    @ContentType("text/xml")
    public void webGetModel(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	String id = request.getParameter("id");
    	String name = request.getParameter("name");
    	
    	if (Check.isEmpty(id) && Check.isEmpty(name)) throw new NotFoundException("[blank]");
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        MutableRepositoryNode modelNode;
        if (Check.isEmpty(id)) {
        	modelNode = RepositoryHelper.getNode(
        			UriHelper.absoluteUri(RepositoryHelper.getPrimaryRepository().getMount() + "/entity", name), false);
        } else {
        	modelNode = RepositoryHelper.getNodeById(id, false);
        }
        if (modelNode.isCollection()) {
        	modelNode = RepositoryHelper.getNode(UriHelper.absoluteUri(modelNode.getUri(), "model.entity"), false);
        }
        
        RepositoryHelper.commitTransaction();
        
        response.addHeader("EntityURI", UriHelper.getParent(modelNode.getUri()));
        response.addHeader("EntityID", modelNode.getNodeId());
        
        ServletUtil.cacheTimeout(response, 37);
        
        StreamUtil.copyStream(
        		RepositoryHelper.loadContent(modelNode), response.getOutputStream());
    }
    
    /**
     * 	/listEntities
     *
     *
     */
    @ContentType("text/xml")
    public void webListEntities(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        String uri = request.getParameter("uri");
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        if (Check.isEmpty(uri)) uri = "/home/entity";
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
     * 	/userEvent
     *
     *
     */
	@ContentType("text/xml")
    public void webUserEvent(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String entity = request.getParameter("entity");
    	String recordId = request.getParameter("recordId");
    	String event = request.getParameter("event");

    		
    	if (Check.isEmpty(entity)
    			|| Check.isEmpty(recordId) 
    			|| Check.isEmpty(event)) throw new NotFoundException("[blank]");
        
    	HibernateUtil.beginTransaction();
        User user = (User)request.getUserPrincipal();
        
        EntityManager entMan = new EntityManager(user);
        
        List<Activity> resActs = entMan.userEvent(entity, recordId, event);
        
        Document resDoc = DocumentHelper.createDocument();
    	Element resRoot = resDoc.addElement("event-results");
    	
        for (Activity actEnt : resActs) {
        	// response
			resRoot.add(ActivityXML.display(actEnt));
        }
        
    	HibernateUtil.commitTransaction();
    	
    	// expire since probably get
        ServletUtil.setExpired(response);
        resDoc.write(response.getWriter());
        
    }
	
	/**
     * 	/recordActivities
     *
     *
     */
	@ContentType("text/xml")
    public void webRecordActivities(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String entity = request.getParameter("entity");
    	String recordId = request.getParameter("recordId");
    		
    	if (Check.isEmpty(entity)
    			|| Check.isEmpty(recordId)) throw new NotFoundException("[blank]");
        
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
    	
        User user = (User)request.getUserPrincipal();
        
	    EntityManager entMan = new EntityManager(user);
	    List<Activity> resActs = entMan.recordActivities(entity, recordId);
	    
	    Document resDoc = DocumentHelper.createDocument();
		Element resRoot = resDoc.addElement("entity-activities");
		
	    for (Activity actEnt : resActs) {
	    	// response
			resRoot.add(ActivityXML.display(actEnt));
	    }
	    
	    HibernateUtil.commitTransaction();
	    
	    // expire since probably get
        ServletUtil.setExpired(response);
        resDoc.write(response.getWriter());
	}
    
    /**
     * Method called for empty and non matching paths
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void defaultWebMethod(
	        HttpServletRequest request,
	        HttpServletResponse response)
        throws ServletException, IOException {
    	
    	String path = request.getPathInfo();
    	if (path.endsWith(".entity")) {

    		if ("GET".equals(request.getMethod())) {
    			path =  RepositoryHelper.resolveUri(path);
    			response.addHeader("EntityURI", UriHelper.getParent(path));
    			ServletUtil.forward("/fil" + path, request, response);    			
                
    		} else if ("PUT".equals(request.getMethod())) {
    		
    			try {
					Document doc = XMLDocument.readStream(request.getInputStream());
					User user = (User)request.getUserPrincipal();
					
					HibernateUtil.beginTransaction();
					
					EntityModelUpdater modUp = new EntityModelUpdater(doc, RepositoryHelper.resolveUri(path));
					
					modUp.saveUpdate(user);
					
					if (modUp.needsDataUpdate())
						modUp.updateData(user);
					
					HibernateUtil.commitTransaction();
					
					if (modUp.needsReIndexing())
						modUp.asyncIndexer(user);
					
				} catch (Exception ex) {
					throw new ServletException("Entity model update error", ex);
				}

    		}
    		
    	} else {
    		super.defaultWebMethod(request, response);
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
