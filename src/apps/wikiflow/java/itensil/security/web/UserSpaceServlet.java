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

import itensil.web.MethodServlet;
import itensil.web.ContentType;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.security.*;
import itensil.security.hibernate.USpaceUserEntity;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.SignOnHB;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.security.hibernate.GroupEntity;
import itensil.util.Check;
import itensil.util.StringHelper;
import itensil.util.TimeZoneList;
import itensil.mail.MailService;
import itensil.mail.web.MailHoster;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;

import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UserSpaceServlet extends MethodServlet  {

    /**
     *  /list
     *
     *  Output:
     *      User List XML
     *
     */
    @ContentType("text/xml")
    public void webList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	UserEntity self = (UserEntity)request.getUserPrincipal();

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        UserSpace uspace = self.getUserSpace();
        
        HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        root.addAttribute("name", uspace.getName());
        root.addAttribute("self", self.getUserId());
        root.addAttribute("self-roles", self.getUSpaceUser().getRoleStr());
        for (User user : uspace.getUsers()) {
            Element uElem = root.addElement("user");
            uElem.addAttribute("id", user.getUserId());

            // email source may change here....
            uElem.addAttribute("email", user.getName());
            uElem.addAttribute("name", user.getSimpleName());
        }

        for (Group grp : uspace.getGroups()) {
            Element gElem = root.addElement("group");
            gElem.addAttribute("id", grp.getGroupId());
            gElem.addAttribute("name", grp.getSimpleName());
        }

        // include everyone for lookup
        Everyone evo = new Everyone();
        Element gElem = root.addElement("group");
        gElem.addAttribute("id", evo.getGroupId());
        gElem.addAttribute("name", evo.getSimpleName());
        gElem.addAttribute("everyone", "1");

        HibernateUtil.commitTransaction();

        // let the list live a bit
        ServletUtil.cacheTimeout(response, 12);
        doc.write(response.getWriter());
    }

    /**
     *  /userManList
     *
     *  Output:
     *      User/Groups List XML
     *
     */
    @ContentType("text/xml")
    public void webUserManList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();

        if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");

        UserSpaceEntity uspace = (UserSpaceEntity)self.getUserSpace();
        
        HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");

        root.addAttribute("name", uspace.getName());
        root.addAttribute("self", self.getUserId());
        for (USpaceUserEntity uus : uspace.getUserSpaceUsers().values()) {
        	UserEntity user = uus.getUserEntity();
        	
            Element uElem = root.addElement("user");
            uElem.addAttribute("id", user.getUserId());

            // email source may change here....
            uElem.addAttribute("email", user.getName());
            uElem.addAttribute("name", user.getSimpleName());
            uElem.addAttribute("roles", uus.getRoleStr());
            
            for (Group grp : uspace.getGroupsForUser(user)) {
                Element gElem = uElem.addElement("group");
                gElem.addAttribute("id", grp.getGroupId());
                gElem.addAttribute("name", grp.getSimpleName());
            }
        }
        HibernateUtil.commitTransaction();
        ServletUtil.noCache(response);
        doc.write(response.getWriter());
    }


    /**
     *  /groupManList
     *
     *  Output:
     *      Group/User List XML
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webGroupManList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();

        if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");

        UserSpaceEntity uspace = (UserSpaceEntity)self.getUserSpace();
        
        //HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        Query qry = HibernateUtil.getSession().getNamedQuery("USpace.groupsByType");
        qry.setInteger("gtype", 0);
        qry.setEntity("uspace", uspace);
        

        for (GroupEntity grp : (List<GroupEntity>)qry.list()) {
            Element gElem = root.addElement("group");
            gElem.addAttribute("id", grp.getGroupId());
            gElem.addAttribute("name", grp.getSimpleName());
            gElem.addAttribute("parentId", Check.emptyIfNull(grp.getParentGroupId()));
            for (User user : grp.getUserEntities()) {
                Element uElem = gElem.addElement("user");
                uElem.addAttribute("id", user.getUserId());
                uElem.addAttribute("name", user.getSimpleName());
                uElem.addAttribute("email", user.getName());
            }
        }
        HibernateUtil.commitTransaction();
        
        ServletUtil.noCache(response);
        doc.write(response.getWriter());
    }

    /**
     *  /orgList
     *
     *  Output:
     *      Group/User List XML
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webOrgList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();

        UserSpaceEntity uspace = (UserSpaceEntity)self.getUserSpace();
        
        if (!uspace.getFeatures().contains("orgs")) throw new UserSpaceException("Org feature required");

        //HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        Query qry = HibernateUtil.getSession().getNamedQuery("USpace.groupsByType");
        qry.setInteger("gtype", 1);
        qry.setEntity("uspace", uspace);
        
        for (GroupEntity grp : (List<GroupEntity>)qry.list()) {
            Element gElem = root.addElement("group");
            gElem.addAttribute("id", grp.getGroupId());
            gElem.addAttribute("name", grp.getSimpleName());
            gElem.addAttribute("fullName", grp.getGroupName());
            gElem.addAttribute("remoteKey", grp.getRemoteKey());
            gElem.addAttribute("parentId", grp.getParentGroupId());
        }
        
        HibernateUtil.commitTransaction();
        
        ServletUtil.noCache(response);
        doc.write(response.getWriter());
    }
    
    
    
    /**
     *  /addOrg
     *
     *  Input:
     *   name
     *   fullName
     *   remoteKey
     *   parentId or parentRemoteKey
     *   
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webAddOrg(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String> params = RequestUtil.readParameters(request, 
    			new String[]{"name","fullName","remoteKey","parentId","parentRemoteKey"});
    	
    	HibernateUtil.beginTransaction();
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();

        UserSpaceEntity uspace = (UserSpaceEntity)self.getUserSpace();
        
        if (!uspace.getFeatures().contains("orgs")) throw new UserSpaceException("Org feature required");

        HibernateUtil.getSession().refresh(uspace);
        GroupEntity grp = new GroupEntity();
        grp.initNew();
        grp.setUserSpaceEntity(uspace);
        grp.setGroupType(1);
        grp.setSimpleName(params.get("name"));
        grp.setGroupName(params.get("fullName"));
        if (Check.isEmpty(grp.getGroupName()))
        	grp.setGroupName(grp.getSimpleName());
        grp.setRemoteKey(params.get("remoteKey"));
        GroupEntity parGrp = null;
        if (!Check.isEmpty(params.get("parentId"))) {
        	parGrp = (GroupEntity)uspace.resolve(new DefaultGroup(params.get("parentId")));
        } else if (!Check.isEmpty(params.get("parentRemoteKey"))) {
        	Query qry = HibernateUtil.getSession().getNamedQuery("USpace.groupByRemote");
            qry.setString("rkey", params.get("parentRemoteKey"));
            qry.setEntity("uspace", uspace);
            parGrp = (GroupEntity)qry.uniqueResult();
        }
        if (parGrp != null) {
        	grp.setParentGroupId(parGrp.getGroupId());
        }
        HibernateUtil.getSession().save(grp);
                
        HibernateUtil.commitTransaction();
        
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("ok");
        root.addAttribute("id", grp.getGroupId());

        ServletUtil.noCache(response);
        doc.write(response.getWriter());
    }

    
    
    /**
     *  /groupUsers
     *
     *  Output:
     *      User List XML
     *
     */
    @ContentType("text/xml")
    public void webGroupUsers(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User self = (User)request.getUserPrincipal();

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        UserSpace uspace = self.getUserSpace();
        
        HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("group");

        String groupId = request.getParameter("groupId");
        if (!Check.isEmpty(groupId)) {
            Group grp = uspace.getGroup(groupId);
            if (grp != null) {
                root.addAttribute("id", grp.getGroupId());
                root.addAttribute("name", grp.getSimpleName());
            }
            for (User user : uspace.getGroupUsers(grp)) {
                Element uElem = root.addElement("user");
                uElem.addAttribute("id", user.getUserId());
                uElem.addAttribute("name", user.getSimpleName());
                uElem.addAttribute("email", user.getName());
                uElem.addAttribute("positions", 
                		StringHelper.stringFromSet(uspace.getGroupRoles(grp,user)));
            }
        }

        HibernateUtil.commitTransaction();
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    /**
     *  /guestList
     *
     *  Output:
     *      Guest User List XML
     *
     */
    @ContentType("text/xml")
    public void webGuestList(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	UserEntity self = (UserEntity)request.getUserPrincipal();

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        UserSpace uspace = self.getUserSpace();
        
        HibernateUtil.getSession().refresh(uspace);

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        root.addAttribute("name", uspace.getName());
        root.addAttribute("self", self.getUserId());
        root.addAttribute("self-roles", self.getUSpaceUser().getRoleStr());
        for (User user : uspace.getUsersInRole("guest")) {
            Element uElem = root.addElement("user");
            uElem.addAttribute("id", user.getUserId());

            // email source may change here....
            uElem.addAttribute("email", user.getName());
            uElem.addAttribute("name", user.getSimpleName());
        }

        HibernateUtil.commitTransaction();

        // let the list live a bit
        ServletUtil.cacheTimeout(response, 12);
        doc.write(response.getWriter());
    }


    /**
     *  /settings
     *
     *
     */
    public void webSettings(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletUtil.forward("/view-usr/index.jsp", request, response);
    }

    /**
     *  /getUserMan
     *
     *
     */
    @ContentType("text/xml")
    public void webGetUserMan(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();

        if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");

        String id = request.getParameter("id");
        if (!Check.isEmpty(id)) {
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            UserEntity uEnt = (UserEntity)uspace.resolve(new DefaultUser(id));
            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("user");
            root.addAttribute("id", id);
            root.addElement("name").setText(uEnt.getSimpleName());
            root.addElement("email").setText(uEnt.getName());
            root.addElement("roles").setText(uEnt.getUserSpaceUsers().get(uspace).getRoleStr());
            HibernateUtil.commitTransaction();
            ServletUtil.setExpired(response);
            doc.write(response.getWriter());
        }
        
    }

    /**
     *  /setUserMan
     *
     *
     */
    @ContentType("text/xml")
    public void webSetUserMan(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        HibernateUtil.beginTransaction();

        if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");
        Document doc = XMLDocument.readStream(request.getInputStream());

        Element root = doc.getRootElement();
        if ("user".equals(root.getName())) {
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            UserEntity uEnt = (UserEntity)uspace.resolve(new DefaultUser(root.attributeValue("id")));

            if (uEnt.getUserSpaceUsers().size() == 1) {
            	uEnt.setUserName(root.elementTextTrim("email"));
            	uEnt.setSimpleName(root.elementTextTrim("name"));
            }
            
            USpaceUserEntity uus = uEnt.getUserSpaceUsers().get(uspace);
            uus.setRoleStr(root.elementTextTrim("roles"));
            
            HibernateUtil.getSession().update(uus);
            HibernateUtil.getSession().update(uEnt);

            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }

    /**
     *  /removeUser
     *      -id
     */
    @ContentType("text/xml")
    public void webRemoveUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        String id = request.getParameter("id");
        if (!Check.isEmpty(id)) {
            HibernateUtil.beginTransaction();
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            uspace.removeUser(new DefaultUser(id));
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }


    /**
     *  /leaveGroup
     *      -group
     *      -user
     */
    @ContentType("text/xml")
    public void webLeaveGroup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        String userId = request.getParameter("user");
        String groupId = request.getParameter("group");
        if (!Check.isEmpty(userId) && !Check.isEmpty(groupId)) {
            HibernateUtil.beginTransaction();
            if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            uspace.removeGroupUser(new DefaultGroup(groupId), new DefaultUser(userId));
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }


    /**
     *  /joinGroup
     *      -group
     *      -user
     */
    @ContentType("text/xml")
    public void webJoinGroup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        String userId = request.getParameter("user");
        String groupId = request.getParameter("group");
        if (!Check.isEmpty(userId) && !Check.isEmpty(groupId)) {
            HibernateUtil.beginTransaction();
            if (!self.getRoles().contains("admin")) throw new UserSpaceException("Admin role required");
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            uspace.addGroupUser(new DefaultGroup(groupId), new DefaultUser(userId));
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }
    
    /**
     * /getMember
     *  	<member xmlns="">
     *           <group/>
     *           <user/>
     *           <positions/>
     *      </member>
     */
    @ContentType("text/xml")
    public void webGetMember(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
    	Map<String, String> params = RequestUtil.readParameters(request, new String[]{"user","group"});
        String userId = params.get("user");
        String groupId = params.get("group");
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("member");
        if (!Check.isEmpty(userId) && !Check.isEmpty(groupId)) {
        	HibernateUtil.beginTransaction();
        	HibernateUtil.readOnlySession();
        	root.addElement("user").setText(userId);
        	root.addElement("group").setText(groupId);
        	UserSpace uspace = self.getUserSpace();
        	Set<String> roles = uspace.getGroupRoles(new DefaultGroup(groupId), new DefaultUser(userId));
        	if (!Check.isEmpty(roles)) {
        		root.addElement("positions").setText(StringHelper.stringFromSet(roles));
        	}
        	HibernateUtil.commitTransaction();
        }
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }
    
    
    /**
     * /setMember
     *  	<member xmlns="">
     *           <group/>
     *           <user/>
     *           <positions/>
     *      </member>
     */
    @ContentType("text/xml")
    public void webSetMember(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
    	Map<String, String> params = RequestUtil.readParameters(request, new String[]{"user","group", "positions"});
        String userId = params.get("user");
        String groupId = params.get("group");
        if (!Check.isEmpty(userId) && !Check.isEmpty(groupId)) {
        	HibernateUtil.beginTransaction();
        	UserSpace uspace = self.getUserSpace();
        	uspace.setGroupRoles(new DefaultGroup(groupId), new DefaultUser(userId),
        			StringHelper.setFromString(params.get("positions")));
        	HibernateUtil.commitTransaction();
        }
        response.getWriter().print("<ok/>");
    }

    /**
     *  /addGroup
     *      -name
     */
    @ContentType("text/xml")
    public void webAddGroup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        String name = request.getParameter("name");
         if (!Check.isEmpty(name)) {
            HibernateUtil.beginTransaction();
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            if (uspace.resolve(new DefaultGroup(null, name)) == null) {
                uspace.createGroup(name, name);
            }
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
         }
    }

    /**
     *  /removeGroup
     *      -id
     */
    @ContentType("text/xml")
    public void webRemoveGroup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AuthenticatedUser self = (AuthenticatedUser)request.getUserPrincipal();
        String id = request.getParameter("id");
         if (!Check.isEmpty(id)) {
            HibernateUtil.beginTransaction();
            UserSpace uspace = self.getUserSpace();
            HibernateUtil.getSession().refresh(uspace);
            uspace.removeGroup(new DefaultGroup(id));
            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
         }
    }


    /**
     *  /getSelf
     *
     *
     */
    @ContentType("text/xml")
    public void webGetSelf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = (User)request.getUserPrincipal();

        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        UserEntity uEnt = (UserEntity)user;

        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("user");
        root.addElement("name").setText(uEnt.getSimpleName());
        root.addElement("email").setText(uEnt.getName());
        root.addElement("flags").setText(uEnt.getFlagStr() != null ? uEnt.getFlagStr() : "");
        root.addElement("timezone").setText(uEnt.getTimezoneStr());
        HibernateUtil.commitTransaction();
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }

    /**
     *  /setSelf
     *
     *
     */
    @ContentType("text/xml")
    public void webSetSelf(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = (User)request.getUserPrincipal();
        Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("user".equals(root.getName())) {
            HibernateUtil.beginTransaction();

            UserEntity uEnt = (UserEntity)user;

            uEnt.setUserName(root.elementTextTrim("email"));
            uEnt.setSimpleName(root.elementTextTrim("name"));
            uEnt.setFlagStr(root.elementTextTrim("flags"));
            uEnt.setTimezoneStr(root.elementTextTrim("timezone"));

            HibernateUtil.getSession().update(uEnt);

            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }

    /**
     *  /setPass
     *
     *
     */
    @ContentType("text/xml")
    public void webSetPass(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = (User)request.getUserPrincipal();
        Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("pass".equals(root.getName())) {
            HibernateUtil.beginTransaction();
            UserEntity uEnt = (UserEntity)user;
            if (!Arrays.equals(uEnt.getPasswordHash(), SignOnHB.hashPassword(root.elementTextTrim("oldPass")))) {
                throw new UserSpaceException("Old password doesn't match.");
            }
            uEnt.setPasswordHash(SignOnHB.hashPassword(root.elementTextTrim("newPass")));
            HibernateUtil.getSession().update(uEnt);

            HibernateUtil.commitTransaction();
            response.getWriter().print("<ok/>");
        }
    }

    /**
     *  /invite
     *
     *
     */
    @ContentType("text/xml")
    public void webInvite(HttpServletRequest request, HttpServletResponse response) throws Exception {

        User user = (User)request.getUserPrincipal();
        Document doc = XMLDocument.readStream(request.getInputStream());
        Element root = doc.getRootElement();
        if ("invite".equals(root.getName())) {
        	MailService mailer = getMailer();
        	InternetAddress fromAddr = new InternetAddress(
              		mailer.getProperties().getProperty("alert.from.email", "alert@itensil.com"),
              		mailer.getProperties().getProperty("alert.from.name", "Alert"));

            HibernateUtil.beginTransaction();
            UserSpace uspace = user.getUserSpace();
            String name = root.elementTextTrim("name");
            String email = root.elementTextTrim("email");
            boolean asGuest = "1".equals(root.elementTextTrim("guest"));
            boolean guestLog = "1".equals(root.elementTextTrim("log"));
            
            String password = root.elementTextTrim("password");
            
            String comment = root.elementText("comment");
            
            StringBuffer buf = new StringBuffer("You are invited to join ");
            buf.append(user.getSimpleName());
            buf.append("'s community.\n");
            buf.append(uspace.getBaseUrl());
            buf.append("\n\n");
            HibernateUtil.getSession().refresh(uspace);
            String pass = Check.isEmpty(password) ? PasswordGen.generatePassword() : password;
            HashSet<String> roles = new HashSet<String>(1);
            if (asGuest) {
            	roles.add("guest");
            	if (guestLog) roles.add("actlog");
            } else {
            	roles.add("inviter");
            }
            UserEntity newUser = (UserEntity)uspace.createUser(
                    email, name, pass, roles, user.getLocale(), user.getTimeZone());
            
            Group createGroup = null;
            Group editGroup = null;
            Group guestGroup = null;
            for (Group grp : uspace.getGroups()) {
                if ("Creators".equals(grp.getSimpleName())) createGroup = grp;
                else if ("Editors".equals(grp.getSimpleName())) editGroup = grp;
                else if ("Guests".equals(grp.getSimpleName())) guestGroup = grp; 
            }
            if (asGuest) {
            	if (guestGroup != null) uspace.addGroupUser(guestGroup, newUser);
            } else if (editGroup != null && user.isUserInGroup(editGroup)) {
            	uspace.addGroupUser(editGroup, newUser);
            } else {
            	if (createGroup != null) uspace.addGroupUser(createGroup, newUser);
            }

            // created users have non-null provider
            if (newUser != null) {
                buf.append("Username:\n");
                buf.append(email.trim());
                
                if (newUser.getUserSpaceUsers().size() == 1) {
	                buf.append('\n');
	                buf.append("\nPassword:\n");
	                buf.append(pass.trim());
                } else {
                	buf.append("\nPlease use your existing password for this community.");
                }
            }
            
            if (!Check.isEmpty(comment)) {
            	buf.append("\n\n");
            	buf.append(comment);
            	buf.append('\n');
            }
            
            // email only a generated password
            
            
            
            HibernateUtil.commitTransaction();
            
            Document resDoc = DocumentHelper.createDocument();
            Element resRoot = resDoc.addElement("invite");
            resRoot.addAttribute("userId", newUser.getUserId());
            resRoot.addAttribute("email", email);
            resRoot.addElement("body").setText(buf.toString());

            resDoc.write(response.getWriter());
        }
    }

    /**
     *  /timezones
     *
     *
     */
    @ContentType("text/xml")
    public void webTimezones(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("timezones");
        for (Map.Entry<String, String> zone : TimeZoneList.zoneNames.entrySet()) {
        	Element zEl = root.addElement("zone");
        	
        	zEl.addAttribute("id", zone.getValue());
        	zEl.addAttribute("name",zone.getKey());
        }
        doc.write(response.getWriter());
    }
    
    
    protected MailService getMailer() {
        return ((MailHoster)
            getServletContext().getAttribute("mailer-default")).getMailService();
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
