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
package itensil.security.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;

import itensil.web.ContentType;
import itensil.web.MethodServlet;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.config.ConfigManager;
import itensil.config.Property;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.mail.web.MailHoster;
import itensil.security.*;
import itensil.repository.*;
import itensil.repository.hibernate.RepositoryEntity;
import itensil.util.*;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;

public class CommunityAdmin extends MethodServlet {

	@Override
	protected void performMethod(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// enforce sysadmin only Access
		if (SysAdmin.isSysAdmin((User)request.getUserPrincipal())) {
			super.performMethod(request, response);			
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	/**
     *  /page
     *
     * Direct page
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	HibernateUtil.beginTransaction();
    	
    	HibernateUtil.readOnlySession();
    	
    	Query qry = HibernateUtil.getSession().getNamedQuery("USpace.getUSpaces");
    	request.setAttribute("uspaceList", qry.list()); 
    	
    	ServletUtil.setExpired(response);
    	ServletUtil.forward("/view-comadmin/index.jsp", request, response);
    	
    	HibernateUtil.commitTransaction();
    	
    }
	
    /**
     *  /create
     *
     *
     */
    @ContentType("text/xml")
    public void webCreate(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("community".equals(root.getName())) {
        	HibernateUtil.beginTransaction();
        	
        	String spaceName = root.elementTextTrim("name");
        	
        	// create userspace
            UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(spaceName);
            uspace.setBaseUrl(root.elementTextTrim("baseUrl"));
            uspace.setBrand(root.elementTextTrim("brand"));
            uspace.setFeaturesStr(root.elementTextTrim("features"));
            HibernateUtil.getSession().update(uspace);

            HashSet<String> roles = new HashSet<String>();
            roles.add("admin");
            
            String userText = root.elementTextTrim("user");
            String email = root.elementTextTrim("email");
            String pass = root.elementTextTrim("pass");

            // create owner user
            User cUser = uspace.createUser(
            			email,
            			userText,
            			pass,
                        roles,
                        Locale.getDefault(),
                        TimeZone.getDefault());

            // generate editor group
            Group editGroup = uspace.createGroup("Editors", "Editors");
            uspace.addGroupUser(editGroup, cUser);

            // generate create group
            Group createGroup = uspace.createGroup("Creators", "Creators");

            // generate read group
            Group readGroup = uspace.createGroup("Readers", "Readers");
            
            // generate read group
            Group guestGroup = uspace.createGroup("Guests", "Guests");

            // make file system
            String mount = root.elementTextTrim("mount");
            RepositoryManager repoMan = RepositoryManagerFactory.getManager(cUser);
            
            // Repository repo
            repoMan.createRepository(mount, cUser, editGroup, createGroup, readGroup, guestGroup);
            repoMan.addRepositoryMount(mount, true);
            repoMan.addRepositoryMount("/system", false);
            
            // config default files
            SecurityAssociation.setUser(cUser);
            try {
            	MutableRepositoryNode inColNode = (MutableRepositoryNode) RepositoryHelper.getNode("/system/.init", false);
            	if (inColNode.isCollection()) {
            		for (RepositoryNode kid : inColNode.getChildren()) {
            			
            			MutableRepositoryNode nKid = (MutableRepositoryNode)
            				((MutableRepositoryNode) kid).copy(
            					UriHelper.absoluteUri(mount, UriHelper.name(kid.getUri())), 
            					true);
            			
            			// add write to all non-collections
            			if (!kid.isCollection()) {
            				nKid.grantPermission(DefaultNodePermission.writePermission(createGroup));
            			}
            		}
            	}
            } catch (NotFoundException nfe) {
            	log("Create warning.", nfe);
            } catch (AccessDeniedException ade) {
            	log("Create warning.", ade);
            }
                   	
        	HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
        
    }
    
    /**
     *  /getConfigProp
     *
     *
     */
    @ContentType("text/xml")
    public void webGetConfigProp(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	HibernateUtil.beginTransaction();
    	
    	HibernateUtil.readOnlySession();
    	
    	Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("config-property");
        Property prop = ConfigManager.getProperty(request.getParameter("component"));
        root.addElement("component").setText(prop.getComponent());
        root.addElement("properties").setText(prop.getProperties());
    	
    	HibernateUtil.commitTransaction();
    	
    	ServletUtil.noCache(response);
    	doc.write(response.getWriter());
    }
    
    
    /**
     *  /setConfigProp
     *
     *
     */
    @ContentType("text/xml")
    public void webSetConfigProp(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	

    	Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("config-property".equals(root.getName())) {
    	
	    	HibernateUtil.beginTransaction();
	    	
	    	Property prop = ConfigManager.getProperty(root.elementTextTrim("component"));
	    	if (prop != null) {
	    		Element pElem = root.element("properties");
	    		prop.setProperties(pElem.getText());
	    		HibernateUtil.commitTransaction();
	    		
	    		if ("mailer-default".equals(prop.getComponent())) {
	    			MailHoster mhost = (MailHoster)
	                	getServletContext().getAttribute("mailer-default");
	    			if (mhost != null) {
	    				mhost.reloadMailService();
	    			}
	    		}
	    	} else {
	    		HibernateUtil.commitTransaction();
	    	}
	    	response.getWriter().print("<ok/>");
        }
    	
    }
    
    /**
     *  /switchUser
     *
     * Switch the user in this session. Only works once per session.
     *
     */
    public void webSwitchUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	
    	HibernateUtil.readOnlySession();
    	UserEntity self = (UserEntity)request.getUserPrincipal();
    	String uspaceId  = request.getParameter("uspaceId");
    	if (SysAdmin.isSysAdmin(self) && !Check.isEmpty(uspaceId)) {
	    	String userId = request.getParameter("userId");
	    	
	    	if (!Check.isEmpty(userId)) {
	    		UserEntity swUser = (UserEntity)SignOnFactory.getSignOn().switchableUser(userId, "comm-admin");
	    		UserSpaceEntity uspace = (UserSpaceEntity)HibernateUtil.getSession().get(UserSpaceEntity.class, uspaceId);
	    		swUser.setActiveUserSpace(uspace);
	    		SecurityAssociation.setUser(swUser);
	    		HttpSession session = request.getSession();
	    		session.setAttribute(Keys.USER_OBJECT, swUser);
	    		
	    		String brand = swUser.getUserSpace().getBrand();
            	session.setAttribute("brand", brand != null ? brand : "");
            	Cookie ck = new Cookie(Keys.COOKIE_AUTH, "-");
            	ck.setPath(request.getContextPath());
	    		response.addCookie(ck);
	    		ck = new Cookie(Keys.COOKIE_NAME, swUser.getName());
	    		ck.setPath(request.getContextPath());
	    		response.addCookie(ck);
	    		response.sendRedirect(ServletUtil.getServletPath(request, "/"));
	    	}
    	}
    	HibernateUtil.commitTransaction();
    	
    }
    
    /**
     *  /destroyComm
     *
     * Destroy Community
     *
     */
    public void webDestroyComm(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	UserEntity self = (UserEntity)request.getUserPrincipal();
    	String uspaceId = request.getParameter("uspaceId");
    	if (SysAdmin.isSysAdmin(self) && !Check.isEmpty(uspaceId)) {
    		UserSpaceEntity uspace = (UserSpaceEntity)HibernateUtil.getSession().get(UserSpaceEntity.class, uspaceId);
    		if (uspace != null) {
    		
	    		// decorate to the repo name to free it for re-use
	    		RepositoryEntity repo = (RepositoryEntity)RepositoryManagerFactory.getManager(uspace).getPrimaryRepository();
	    		if (repo != null) {
	    			repo.setMount(repo.getMount() + uspace.getUserSpaceId());
	    			HibernateUtil.getSession().update(repo);
	    		}
	    		
	    		HibernateUtil.getSession().delete(uspace);
    		}
    	}
    	HibernateUtil.commitTransaction();
    	response.sendRedirect("page");
    }
    
    
    /**
     *  /getInfo
     *
     *  Community Info
     *
     */
    public void webGetInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
    	
    	UserEntity self = (UserEntity)request.getUserPrincipal();
    	Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("community");
    	String uspaceId = request.getParameter("uspaceId");
    	if (SysAdmin.isSysAdmin(self) && !Check.isEmpty(uspaceId)) {
    		UserSpaceEntity uspace = (UserSpaceEntity)HibernateUtil.getSession().get(UserSpaceEntity.class, uspaceId);
    		if (uspace != null) {
    			root.addElement("uspaceId").setText(uspace.getUserSpaceId());
    			root.addElement("name").setText(Check.emptyIfNull(uspace.getName()));
    	        root.addElement("baseUrl").setText(Check.emptyIfNull(uspace.getBaseUrl()));
    	        root.addElement("brand").setText(Check.emptyIfNull(uspace.getBrand()));
    	        root.addElement("features").setText(Check.emptyIfNull(uspace.getFeaturesStr()));
    		}
    	}
    	HibernateUtil.commitTransaction();
    	
    	ServletUtil.noCache(response);
    	doc.write(response.getWriter());
    }
    
    /**
     *  /setInfo
     *
     *  Update Community Info
     *
     */
    public void webSetInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	HibernateUtil.beginTransaction();
    	Map<String, String> params = RequestUtil.readParameters(request, new String[]{"uspaceId","name","baseUrl","brand","features"});
    	
    	UserEntity self = (UserEntity)request.getUserPrincipal();
    	String uspaceId = params.get("uspaceId");
    	if (SysAdmin.isSysAdmin(self) && !Check.isEmpty(uspaceId)) {
    		UserSpaceEntity uspace = (UserSpaceEntity)HibernateUtil.getSession().get(UserSpaceEntity.class, uspaceId);
    		if (uspace != null) {
    			uspace.setName(params.get("name"));
    			uspace.setBaseUrl(params.get("baseUrl"));
    			uspace.setBrand(params.get("brand"));
    			uspace.setFeaturesStr(params.get("features"));
    		}
    	}
    	HibernateUtil.commitTransaction();
    	
    	response.getWriter().print("<ok/>");
    }
    
    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        HibernateUtil.rollbackTransaction();
    }
    
    public void afterMethod() {
        HibernateUtil.closeSession();
    }
	

}

