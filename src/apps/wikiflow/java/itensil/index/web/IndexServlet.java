package itensil.index.web;

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
import org.hibernate.Query;

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
import itensil.security.hibernate.AppComponentMap;
import itensil.security.hibernate.UserSpaceEntity;
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
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.FlowModel;

public class IndexServlet extends MethodServlet {

	public static final String TYPE_ACTIVITY = "ACT";
	public static final String TYPE_ENTITY = "ENT";
	public static final String TYPE_DEFAULT = "DEF";

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
	 * /joinRecord
	 * 
	 * 
	 * 
	 */
	@ContentType("text/xml")
	public void webFind(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String mapId = request.getParameter("mapId");
		String appId = request.getParameter("appId");
		String appProcessId = request.getParameter("appProcessId");

		boolean hasMapId = !Check.isEmpty(mapId);
		boolean hasAppInfo = !Check.isEmpty(appId) && !Check.isEmpty(appProcessId);

		if (!hasMapId && !hasAppInfo) {
			throw new NotFoundException("[blank]");
		}
		HibernateUtil.beginTransaction();
		HibernateUtil.readOnlySession();

		AppComponentMap appComponent;

		if (hasMapId) {
			appComponent = (AppComponentMap) HibernateUtil.getSession().get(AppComponentMap.class, mapId);
		} else {
			Query qry = HibernateUtil.getSession().getNamedQuery("AppCmp.byAppIdProcessId");
			qry.setString("appId", appId);
			qry.setString("appProcessId", appProcessId);
			appComponent = (AppComponentMap) qry.uniqueResult();
		}

		HibernateUtil.commitTransaction();

		// expire since probably get
		ServletUtil.setExpired(response);

		Document doc = DocumentHelper.createDocument();
		doc.addElement("root");
		recordXML(doc.getRootElement(), appComponent);
		doc.write(response.getWriter());
	}

	/**
	 * /joinRecord
	 * 
	 * 
	 * 
	 */
	@ContentType("text/xml")
	public void webJoin(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String appId = request.getParameter("appId");
		String appProcessId = request.getParameter("appProcessId");

		if (Check.isEmpty(appId) || Check.isEmpty(appProcessId)) {
			throw new NotFoundException("[blank]");
		}

		HibernateUtil.beginTransaction();

		Query qry = HibernateUtil.getSession().getNamedQuery("AppCmp.byAppIdProcessId");
		qry.setString("appId", appId);
		qry.setString("appProcessId", appProcessId);
		AppComponentMap appComponent = (AppComponentMap) qry.uniqueResult();

		String type = request.getParameter("cmpType");
		String name = request.getParameter("cmpName");
		String id = request.getParameter("cmpId");

		User user = (User) request.getUserPrincipal();

		String itenRecordType = null;
		if (Check.isEmpty(type)) {
			// don't set type
		} else if (type.equals(TYPE_ENTITY)) {
			itenRecordType = TYPE_ENTITY;
		} else if (type.equals(TYPE_ACTIVITY)) {
			itenRecordType = TYPE_ACTIVITY;
		} else {
			itenRecordType = TYPE_DEFAULT;
		}

		if (appComponent == null) {
			appComponent = new AppComponentMap();
			appComponent.setAppId(appId);
			appComponent.setAppProcessId(appProcessId);
		}
		String flowUri = null;
		if (appComponent.getItenOwnerId() == null || appComponent.getItenOwnerId().equals(user.getUserId())) {
			appComponent.setItenOwnerId(user.getUserId());
			appComponent.setItenComponentType(itenRecordType);
			if (itenRecordType != null) {
				appComponent.setItenComponentName(name);
				if (appComponent.getItenComponentName() != null) {
					// for activity replace id with activity id
					if (TYPE_ACTIVITY.equals(itenRecordType)) {
						// create new activity for process name and activity
						// name

						// follow ActivityServlet.webLaunch
						/*
						 * <?xml version="1.0" encoding="UTF-8"?> <launch>
						 * <flow>/gadget1/process/process2</flow> <master-flow/>
						 * <name>process2-2010-03-11</name> <description/>
						 * <startDate/> <dueDate/> <priority/> <parent/>
						 * <parentStep/> <project/> <proj-lock/> <meet>0</meet>
						 * <contextGroup/> </launch>
						 */
						// really only need this and then store the activity id
						// in the cross index

						// Launch it!
			            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
			           	flowUri = UriHelper.absoluteUri(
		            			RepositoryHelper.getPrimaryRepository().getMount() + "/process",
		            			UriHelper.filterName(name));
		            	
						HashMap<String, String> roles = new HashMap<String, String>();
				     
						try {
						Activity activityEnt =
				                uActivities.launch(
				                	id,
				                    null /* root.elementTextTrim("description") */,
				                    flowUri,
				                    null /* RepositoryHelper.resolveUri(root.elementTextTrim("master-flow")) */,
				                    null /* parentId */,
				                    null /* root.elementTextTrim("project") */,
				                    null /* root.elementTextTrim("contextGroup") */,
				                    null /* ActivityXML.parseDate(root.elementTextTrim("dueDate")) */,
				                    roles /*  roles */ );

				           id = activityEnt.getId();
						}
						catch(NotFoundException nfe) {
							methodException(nfe);
		    				errorXML(request, response, nfe.getCause().getCause());
		    				return;
						}
					}
					appComponent.setItenComponentId(id);
				}
			}
		}

		HibernateUtil.getSession().saveOrUpdate(appComponent);
		HibernateUtil.commitTransaction();

		// expire since probably get
		ServletUtil.setExpired(response); 
		
		request.setAttribute("flowUri", flowUri);

		Document doc = DocumentHelper.createDocument();
		doc.addElement("root");
		recordXML(doc.getRootElement(), appComponent);
		doc.write(response.getWriter());
	}

	public static Element recordXML(Element recParent, AppComponentMap map) {
		if (map == null || recParent == null)
			return null;
		Element mapElem = recParent.addElement("map");
		mapElem.addAttribute("mapId", map.getMapId());
		mapElem.addAttribute("ownerId", map.getItenOwnerId());
		mapElem.addAttribute("appId", map.getAppId());
		mapElem.addAttribute("appProcessId", map.getAppProcessId());
		mapElem.addAttribute("cmpType", map.getItenComponentType());
		mapElem.addAttribute("cmpName", map.getItenComponentName());
		mapElem.addAttribute("cmpId", map.getItenComponentId());
		return mapElem;
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
