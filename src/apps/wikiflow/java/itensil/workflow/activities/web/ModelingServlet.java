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

import itensil.web.ContentType;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.web.UrlUtil;
import itensil.repository.*;
import itensil.repository.web.WebdavServlet;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.io.ByteArrayDataSource;
import itensil.io.ReplaceFilter;
import itensil.io.StreamUtil;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.mail.MailService;
import itensil.mail.web.MailHoster;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.rules.CustValDataContentListener;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.FlowState;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.FlowSAXHandler;

import javax.activation.DataHandler;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.Session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ModelingServlet extends itensil.workflow.model.ModelingServlet {

    /**
     *  /page
     *
     * Direct page
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getParameter("uri");
        if (Check.isEmpty(uri)) {
            String flow = request.getParameter("flow");
            if (Check.isEmpty(flow)) throw new NotFoundException("[blank]");
            HibernateUtil.beginTransaction();
            HibernateUtil.readOnlySession();
            uri = RepositoryHelper.resolveUri(uri);
            FlowState fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flow);
            if (fState == null) throw new NotFoundException(flow);
            uri = UriHelper.absoluteUri(fState.getNode().getUri(), "chart.flow");
            HibernateUtil.commitTransaction();
        }
        request.setAttribute("flowUri", uri);
        ServletUtil.forward("/view-wf/edit.jsp", request, response);
    }

    /**
     *  /meet
     *
     * Direct page
     *
     */
    public void webMeet(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getParameter("uri");
        FlowState fState = null;
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        if (Check.isEmpty(uri)) {
            String flow = request.getParameter("flow");
            if (Check.isEmpty(flow)) throw new NotFoundException("[blank]");
            
            uri = RepositoryHelper.resolveUri(uri);
            fState = (FlowState)HibernateUtil.getSession().get(FlowState.class, flow);
            if (fState == null) throw new NotFoundException(flow);
            uri = UriHelper.absoluteUri(fState.getNode().getUri(), "chart.flow");
            
        }
        if (fState == null) {
        	String flowUri = RepositoryHelper.resolveUri(UriHelper.getParent(uri));
             
        	Session session = HibernateUtil.getSession();
             
            MutableRepositoryNode flowFoldNode = RepositoryHelper.getNode(flowUri, false);
             
            fState = (FlowState)session.get(FlowState.class, flowFoldNode.getNodeId()); 
            if (fState == null) throw new NotFoundException("[Activity for meeting:" + flowUri + "]");
        }
        User user = (User)request.getUserPrincipal();
        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());

                   
        Activity activity = uActivities.getFirstFlowActivity(fState);
        if (activity == null) throw new NotFoundException("[Activity for meeting:" + uri + "]");
        request.setAttribute("activity", activity);
        request.setAttribute("activityId", activity.getId());
        
        MutableRepositoryNode actRepoNode = (MutableRepositoryNode)activity.getNode();
        request.setAttribute("meet-draft", actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-draft")));
        
        HibernateUtil.commitTransaction();
        
        request.setAttribute("flowUri", uri);
        ServletUtil.forward("/view-wf/meet.jsp", request, response);
    }
    
    /**
     * /getModel
     *
     * Load a flow model
     *
     * Parameters:
     *  uri = model uri
     *  activity = activity id (optional, finds variations)
     *
     * Output:
     *  Model XML + URI Meta XML
     */
    @ContentType("text/xml")
    public void webGetModel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        String verStr = request.getParameter("version");
        String actId = request.getParameter("activity");
        NodeVersion version;
        if (Check.isEmpty(verStr)) {
            version = new DefaultNodeVersion();
        } else {
            version = new DefaultNodeVersion(verStr, false);
        }
        HibernateUtil.beginTransaction();
        HibernateUtil.readOnlySession();
        if (Check.isEmpty(uri) && !Check.isEmpty(actId)) {
            User user = (User)request.getUserPrincipal();
            UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            Activity activity = uActivities.getActivity(actId);
            if (activity == null) throw new NotFoundException(actId);
            request.setAttribute("activity", activity);
            FlowState fState = activity.getFlow();
            if (fState == null) {
            	throw new NotFoundException("Activity flow: " + actId);
            }
            try {
            	uri = UriHelper.absoluteUri(fState.getNode().getUri(), "chart.flow");
            } catch (NotFoundException nfe) {
            	throw new NotFoundException("Activity flow: " + actId);
            }
        }
        if (!Check.isEmpty(uri)) {
            MutableRepositoryNode node = null;
            if (!Check.isEmpty(actId)) {
            	User user = (User)request.getUserPrincipal();
                UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
            	Activity actEnt = uActivities.getActivity(actId);
            	node = (MutableRepositoryNode) actEnt.getVariationNode();
            } 
            if (node == null) {
            	uri = RepositoryHelper.resolveUri(uri);
            	node = RepositoryHelper.getNode(uri, false);
            }
            
            NodeProperties props = node.getProperties(version);
            if (props == null)
                throw new NotFoundException("Version number: " + verStr);

            ServletUtil.setExpired(response);
            
            if (node.hasPermission(DefaultNodePermission.writePermission(request.getUserPrincipal()))) {
                response.addHeader("Can-Write", "write");
            }
            
            // Check modify dates and eTags
            if (WebdavServlet.checkNodeNotModified(props, request, response)) {
                response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            NodeContent content = node.getContent(version);
            StreamUtil.copyStream(content.getStream(), response.getOutputStream());
        } else {
            throw new NotFoundException("[blank]");
        }
        RepositoryHelper.commitTransaction();
    }

    /**
     * /setModel
     *
     * Takes an inputstream of an Flow XML Model + URI Meta XML
     *
     * Output:
     *  Status Message
     */
    @ContentType("text/xml")
    public void webSetModel(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getParameter("uri");
        String actId = request.getParameter("activity");
        boolean forceVariation = "1".equals(request.getParameter("forceVar"));
        if (!Check.isEmpty(uri)) {
        	
            RepositoryHelper.beginTransaction();
            
            User user = (User)request.getUserPrincipal();
            MutableRepositoryNode node = null;
            
            Document doc = XMLDocument.readStream(request.getInputStream());
            String variation = "";
            Activity actEnt = null;
            UserActivities uActivities = null;
            if (!Check.isEmpty(actId)) {
            	
                uActivities = new UserActivities(user, HibernateUtil.getSession());
            	actEnt = uActivities.getActivity(actId);
            	node = (MutableRepositoryNode)actEnt.getVariationNode();
            	if (node == null && forceVariation) {
            	
	            	String actUri = RepositoryHelper.getAvailableUri(
	            				UriHelper.absoluteUri(actEnt.getNode().getUri(), "chart.flow"));
	            	uri = RepositoryHelper.resolveUri(uri);
	            	MutableRepositoryNode srcNode = RepositoryHelper.getNode(uri, true);
	            	node = (MutableRepositoryNode)srcNode.copy(actUri, false);
	            	variation = node.getNodeId();
	            	actEnt.setVariationId(variation);
	            	HibernateUtil.getSession().update(actEnt);
            	} else if (node != null) {
            		variation = node.getNodeId();
            	}
            	
            }
            if (node == null){
            	uri = RepositoryHelper.resolveUri(uri);
            	node = RepositoryHelper.getNode(uri, true);
            }
            
            
            uri = RepositoryHelper.resolveUri(uri);
            
            // TODO add schema validation

            // propagate the icon
            MutableRepositoryNode parNode = (MutableRepositoryNode)node.getParent();
            String icon = doc.valueOf("/flow/iw:type/@icon");
            if (Check.isEmpty(icon)) icon = "def";
            NodeProperties props = parNode.getProperties(new DefaultNodeVersion());
            if (props == null) {
            	props = new DefaultNodeProperties(new DefaultNodeVersion());
            }
            String val = props.getValue(PropertyHelper.itensilQName("style"));
            String style = "icon:" + icon;
            if (!style.equals(val)) {
            	props.setValue(PropertyHelper.itensilQName("style"), style);
            }
            
            props.setValue(PropertyHelper.defaultQName("getlastmodified"), PropertyHelper.dateString(new Date()));
            parNode.setProperties(props);
            
            FlowState flowState = actEnt != null ? actEnt.getFlow() :
            	(FlowState)HibernateUtil.getSession().get(FlowState.class, parNode.getNodeId());
            boolean isMeet =  parNode.getUri().indexOf("/meeting/") > 0;
            if (flowState != null) {
	            FlowModel flowMod = new FlowModel();
	            FlowSAXHandler hand = new FlowSAXHandler(flowMod);
	            hand.parse(doc);
	            if (Check.isEmpty(variation)) {
	            	CustValDataContentListener.modelStateSync(flowState, flowMod);
	            	HibernateUtil.getSession().update(flowState);
	            }
	            
	            HashMap<String,String> idMap = new HashMap<String,String>();
	            ArrayList<String> newIds =  new ArrayList<String>();
	            FlowModel.collectIdChanges(doc.getRootElement().element("steps"), idMap, newIds);
	            
	            List remElems = doc.getRootElement().elements("removed");
	            
	            if (!remElems.isEmpty() || !idMap.isEmpty() || !newIds.isEmpty()) {
		            if (uActivities == null) uActivities = new UserActivities(user, HibernateUtil.getSession());
		            Collection<Activity> flowActs = uActivities.getFlowActivities(flowState, variation, isMeet);
		            
		            if (!flowActs.isEmpty()) {
			            for (Object obEl : remElems) {
			            	Element elm = (Element)obEl;
			            	flowState.removeActiveId(flowActs, elm.attributeValue("id"), elm.attributeValue("type"));
			            }
			            if (!idMap.isEmpty()) flowState.activateIdChanges(flowActs, idMap);
			            if (!newIds.isEmpty()) uActivities.enterNewStarts(flowActs, flowState, flowMod, newIds);
		            }
	            }
            }
            Element root = doc.getRootElement();
            Element pups = root.element("plan-updates");
            if (pups != null) {
            	if (uActivities != null && actEnt != null) {
            		ActivityXML.updatePlans(pups, actEnt, uActivities, parNode); 
            	}
            	root.remove(pups);
            }
            
            if (isMeet) {
            	
            	// check for a meeting element included in the model...
            	Element meetRoot = root.element("meet");
            	if (meetRoot != null) {
            		Activity activityEnt = uActivities.getFirstFlowActivity(flowState);
            		saveDraft(request, activityEnt, 
            			RequestUtil.readParameterArrays(meetRoot, 
            					new String[]{"activity", "member", "mail", "body"}), true);
            		root.remove(meetRoot);
            	}

            }

            byte xb[] = doc.asXML().getBytes("UTF-8");
            RepositoryHelper.createContent(
                    node, new ByteArrayInputStream(xb), xb.length, "application/itensil-flow+xml");

            RepositoryHelper.commitTransaction();

            response.getWriter().print("<ok variation='" + variation + "'/>");

         } else {
            throw new NotFoundException("[blank]");
        }
    }

    
    /**
     * /saveMeetDraft
     * 
     * Request:
     * <meet>
     * 	<activity>id</activity>
     *  <member>userid</member>
     *  <member>userid</member>
     *  <mail>1</mail>
     *  <meettime>2008-01-06T08:30:00Z</meetime>
     *  <body><![CDATA[ ... ]]></body>
     * </meet>
     */
    @ContentType("text/xml")
    public void webSaveMeetDraft(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String[]> params = RequestUtil.readParameterArrays(request, 
				new String[]{"activity", "member", "mail", "meetstart", "meetend", "body"});
    	
    	String actId = Check.isEmpty(params.get("activity")) ? null : params.get("activity")[0];
    	if (!Check.isEmpty(actId)) {

            User user = (User)request.getUserPrincipal();

            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            UserActivities uActivities = new UserActivities(user, session);

            // resolve the activity
            Activity activityEnt = uActivities.getActivity(actId);
            if (activityEnt == null) {
                throw new NotFoundException(actId);
            }
	    	
        	
            String draftUri = saveDraft(request, activityEnt, params, false);
	    	
	    	
	    	HibernateUtil.commitTransaction();
	    	response.getWriter().print("<ok uri=\"" + draftUri + "\"/>");
	    } else {
	        throw new NotFoundException("[blank]");
	    }
    	
    }
    
    protected String saveDraft(HttpServletRequest request, Activity activityEnt, Map<String, String[]> params, boolean isFinal) 
    		throws Exception {
    	
    	User user = (User)request.getUserPrincipal();
    	MutableRepositoryNode flowFoldNode = activityEnt.getFlow().getNode();
    	
    	String link = ServletUtil.getAbsoluteContextPath(request) + "act/meetStat?meet=" + UrlUtil.encode(flowFoldNode.getUri());
    	
    	InputStream templateIo = getServletContext().getResourceAsStream("/view-wf/blank-draft.html");	
    	ReplaceFilter filt = new ReplaceFilter();
    	filt.addReplaceKey("base", ServletUtil.getAbsoluteContextPath(request));
    	filt.addReplaceKey("link", link);
    	filt.addReplaceKey("body", Check.isEmpty(params.get("body")) ? "" : params.get("body")[0]);
    	ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    	filt.execute(templateIo, outBuf);
    	
    	MutableRepositoryNode actRepoNode = (MutableRepositoryNode)activityEnt.getNode();
    	
    	String lastDraftUri = actRepoNode.getPropertyValue(PropertyHelper.itensilQName("meet-draft"));
    	Repository repo = actRepoNode.getRepository();
    	String draftUri = lastDraftUri;
    	MutableRepositoryNode node;
    	if (Check.isEmpty(lastDraftUri)) {
	    	String folderUri = actRepoNode.getUri();
	    	DateFormat shortDateFmt = new SimpleDateFormat("yyyy-MM-dd");
	    	shortDateFmt.setTimeZone(user.getTimeZone());
	    	
	    	draftUri = RepositoryHelper.getAvailableUri(UriHelper.absoluteUri(folderUri, 
	    			"snapshot-" + shortDateFmt.format(new Date()) + ".html"));
	    	
	    	node = repo.createNode(draftUri, false, user);
	    	if (isFinal) lastDraftUri = draftUri;
    	} else {
    		try {
    			node = repo.getNodeByUri(lastDraftUri, true);
    		} catch (NotFoundException nfe) {
    			// user has the power to delete... auto-recover
    			node = repo.createNode(lastDraftUri, false, user);
    		}
    	}
    	
    	byte htmlBuf[] = outBuf.toByteArray();
    	RepositoryHelper.createContent(node, new ByteArrayInputStream(htmlBuf), htmlBuf.length, "text/html");
    	
    	HashMap<QName,String> propMap = new HashMap<QName,String>(2);
    	if (isFinal) {
    		propMap.put(PropertyHelper.itensilQName("meet-setup"), "0");
        	propMap.put(PropertyHelper.itensilQName("meet-draft"), "");
        	propMap.put(PropertyHelper.itensilQName("meet-ldraft"), lastDraftUri);
        	PropertyHelper.setNodeValues(actRepoNode, propMap);
    	} else if (Check.isEmpty(lastDraftUri)) {
    		propMap.put(PropertyHelper.itensilQName("meet-draft"), draftUri);
    		PropertyHelper.setNodeValues(actRepoNode, propMap);
    	}
    	
    	boolean sendMail = isFinal || (!Check.isEmpty(params.get("mail")) && "1".equals(params.get("mail")[0]));
    	
    	String members[] = params.get("member");
    	if (!Check.isEmpty(members)) {
    		NodePermission perms[] = flowFoldNode.getPermissions();
    		HashSet<String> existingPerms = new HashSet<String>(perms.length + 1);
    		existingPerms.add(flowFoldNode.getOwner().getUserId());
    		
    		for (NodePermission perm : actRepoNode.getPermissions()) {
    			existingPerms.add(perm.getPrincipal().toString());
    		}
    		UserSpace upsace = user.getUserSpace();
    		ArrayList<InternetAddress> toAddrs =  sendMail ? new ArrayList<InternetAddress>(members.length) : null;
    		for (String memId : params.get("member")) {
    			User memUser = upsace.getUser(memId);
    			if (sendMail) toAddrs.add(new InternetAddress(memUser.getName(), memUser.getSimpleName()));
    			if (!existingPerms.contains(memId)) {
    				try {
	    				if (memUser != null)
	    					flowFoldNode.grantPermission(new DefaultNodePermission(memUser, DefaultNodePermission.WRITE, true));
    				} catch (AccessDeniedException ade) { /* eat it */ }
    			}
    		}
    		if (sendMail) {
    			InternetAddress fromAddr = new InternetAddress(user.getName(), user.getSimpleName());
    			InternetAddress tos[] = toAddrs.toArray(new InternetAddress[toAddrs.size()]);
    			
    			MimeBodyPart icsAttPart = null;
    			String details = "";
    			if (!isFinal && !Check.isEmpty(params.get("meetstart")) && !Check.isEmpty(params.get("meetstart")[0])) {
    				icsAttPart = new MimeBodyPart();
    				
    				String mstart = params.get("meetstart")[0];
    				String mend = Check.isEmpty(params.get("meetend")) ? null : params.get("meetend")[0];
    				
    				// Get start and end, or if no end, start + 1 hour
    				Date meetSDate = ActivityXML.parseDate(mstart);
    				Date meetEDate = Check.isEmpty(mend) ? new Date(meetSDate.getTime() + 60*60*1000) : ActivityXML.parseDate(mend);
    				if (meetEDate.before(meetSDate)) meetEDate = new Date(meetSDate.getTime() + 60*60*1000);
    				
    				SimpleDateFormat dFmt = new SimpleDateFormat("EEE, MMM d yyyy h:mm a (zzzz 'GMT'Z)");
    				dFmt.setTimeZone(user.getTimeZone());
    				details = "Meeting time: " + dFmt.format(meetSDate);
    				
    				/**
    				 * Notes on attachment choices: to have this plug right into out look, 
    				 * I'd have to sacrifice the advanced HTML formating... so my
    				 * compromise seems to be to have an attachment.
    				 */
    				icsAttPart.setFileName("meeting.ics");
    				icsAttPart.setDataHandler(new DataHandler(new ByteArrayDataSource(
    						createICS(activityEnt, fromAddr, tos, link, meetSDate, meetEDate), "text/calendar; method=REQUEST")));
    			}
    			String emailStyle = StreamUtil.streamToString(getServletContext().getResourceAsStream("/include/meeting-email.inc.css"));
    			
    			getMailer().send(tos, fromAddr, "", (isFinal ? "Knowledge App Report: " :  "Knowledge App Plan: ") + activityEnt.getName(),
    					filterEmailHTML(
    							new String(htmlBuf), emailStyle, details, ServletUtil.getAbsoluteContextPath(request) + "fil", flowFoldNode.getUri()), 
    					null, icsAttPart);
    		}
    	}
    	
    	return draftUri;
    }
    
    // <tr class="row1">
    protected static Pattern rowTagPat = Pattern.compile("(class=[\"]?row)([0-3][\"]?>)", Pattern.MULTILINE);
    
    // <button yyy=0 args="xxx" xxx=xx>lalal</button>
    protected static Pattern attachTagPat = Pattern.compile("<button([^>]*)args=\"([^\"]+)([^>]*)>.*?</button>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    
    
    protected static String filterEmailHTML(String html, String styles, String details, String linkBase, String meetFold) {
    	int stylepos1 = html.indexOf("/*snomail*/");
    	int stylepos2 = html.indexOf("/*enomail*/");
    	
    	StringBuffer styleBuf = new StringBuffer(html.substring(0, stylepos1));
    	styleBuf.append(styles);
    	styleBuf.append(html.substring(stylepos2 + 11));
    	
    	Matcher mat = rowTagPat.matcher(styleBuf);
    	StringBuffer labBuf = new StringBuffer();
    	if (mat.find()) {
    		int lp = 0;
    		do {
	    		labBuf.append(styleBuf.substring(lp, mat.start(1)));
	    		labBuf.append(mat.group(1));
	    		labBuf.append(mat.group(2));
	    		switch (mat.group(2).charAt(0)) {
	    		case '0': labBuf.append("\n<div class='ihead'>COMMENTS</div>\n"); break;
	    		case '1': labBuf.append("\n<div class='ihead'>ACTION STEPS</div>\n"); break;
	    		case '2': labBuf.append("\n<div class='ihead'>WORK AREA</div>\n"); break;
	    		case '3': labBuf.append("\n<div class='ihead'>APP DESCRIPTION</div>\n");
	    		labBuf.append("\n<div class='details'>" + details +
	    				"</div>\n"); break;
	    		}
	    		lp = mat.end();
    		} while (mat.find());
    		labBuf.append(styleBuf.substring(lp));
    	} else { // if mal-formed... just leave it alone
    		labBuf = styleBuf;
    	}
    	mat = attachTagPat.matcher(labBuf);
    	StringBuffer hrefBuf = new StringBuffer();
    	int lp = 0;
    	while (mat.find()) {
    		hrefBuf.append(labBuf.substring(lp, mat.start()));
    		lp = mat.end();
    		if (mat.group(1).contains("attachEdit") || mat.group(3).contains("attachEdit")) {
				String args = mat.group(2);
				int pos = args.indexOf('|');
				String uri, label;
				if (pos > 0) {
					uri = args.substring(0, pos);
					label = args.substring(pos + 1);
				} else {
					uri = args;
					label = UriHelper.name(uri);
				}
				uri = UriHelper.absoluteUri(meetFold, uri);
				String href =  linkBase + uri;
				hrefBuf.append("<a href=\"" + href + "\">" + label + "</a>");
    		} else {
    			hrefBuf.append(labBuf.substring(mat.start(), lp));
    		}
    	}
		hrefBuf.append(labBuf.substring(lp));
    	return hrefBuf.toString();
    }
    
    public static void main(String args[]) throws Exception  {
    	
    	String test1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
    		 + "\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
    		 + "\n<head>"
    		 + "\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
    		 + "\n<style>"
    		 + "\n/*snomail*/"
    		 + "\n@import url(http://testlab.itensil.com/css/App.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/brd-itensil.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/Wiki.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/Outline.css);"
    		 + "\n/*enomail*/"
    		 + "\n</style>"
    		 + "\n"
    		 + "\n</head>"
    		 + "\n<body style=\"padding:16px\">"
    		 + "\n	<div class=\"outline wikiView\">"
    		 + "\n	<!--#start_meet-->"
    		 + "\n	"
    		 + "\n	<table class=\"mlayout lo_vert3\" cellpadding=\"0\" cellspacing=\"0\"><tbody><tr><td class=\"row0\"><div>&nbsp;Test the email... <br></div></td></tr><tr><td class=\"row1\"><ul><li>First Action Item<br>"
    		 + "\n<button args=\"activities/Setup Mode/Itensil Software Engineer - rev2.doc|Itensil Software Engineer - rev2.doc\" extid=\"attachEdit\" class=\"iten_emac\">Itensil Software Engineer - rev2.doc<div style=\"-moz-user-select: none;\" class=\"badge em_attachEdit\">&nbsp;</div></button>"
    		 + "\n</li></ul></td></tr><tr><td class=\"row2\"><div>&nbsp;You should configure the email settings....<br></div></td></tr></tbody></table>"
    		 + "\n"
    		 + "\n"
    		 + "\n<button extid=\"attachEdit\" args=\"activities/Setup Mode/Itensil Software Engineer - rev2.doc|Itensil Software Engineer - rev2.doc\" class=\"iten_emac\">Itensil Software Engineer - rev2.doc"
    		 + "\r\n<div style=\"-moz-user-select: none;\" class=\"badge em_attachEdit\">&nbsp;</div></button>"
    		 + "\n	<!--#end_meet-->"
    		 + "\n	</div>"
    		 + "\n	<p><a href=\"http://testlab.itensil.com/act/meetStat?meet=%2Ftestlab%2Fmeeting%2FMaiden%20Voyage\">View Active Meeting &gt;</a></p>"
    		 + "\n</body>"
    		 + "\n</html>";
    	
    	String test2 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
    		 + "\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
    		 + "\n<head>"
    		 + "\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
    		 + "\n<style>"
    		 + "\n/*snomail*/"
    		 + "\n@import url(http://testlab.itensil.com/css/App.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/brd-itensil.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/Wiki.css);"
    		 + "\n@import url(http://testlab.itensil.com/css/Outline.css);"
    		 + "\n/*enomail*/"
    		 + "\n"
    		 + "\n</style>"
    		 + "\n</head>"
    		 + "\n<body style=\"padding:16px\">"
    		 + "\n	<div class=\"outline wikiView\">"
    		 + "\n	<!--#start_meet-->"
    		 + "\n	<TABLE class=\"mlayout lo_vert3\" cellSpacing=0 cellPadding=0>"
    		 + "\n<TBODY>"
    		 + "\n<TR>"
    		 + "\n<TD class=row0>"
    		 + "\n"
    		 + "\n<P>&nbsp;1. review the new meeting in testlab</P>"
    		 + "\n<P>2. report the results</P>"
    		 + "\n<P>&nbsp;</P></TD></TR>"
    		 + "\n<TR>"
    		 + "\n<TD class=row1>"
    		 + "\n<UL>"
    		 + "\n<LI>step 1.&nbsp; instructions<BUTTON class=iten_emac2 args=\"LrygXBcBAGQKQH9wS$r6\" extid=\"assign\"><SPAN class=\"meetMem mtMem0\"><B>K</B><SPAN>eith Patterson</SPAN></SPAN></BUTTON>&nbsp;<BUTTON class=iten_emac args=\"Test doc\" extid=\"input\">"
    		 + "\n<DIV class=\"badge em_input\" UNSELECTABLE=\"on\"></DIV>Test doc</BUTTON>&nbsp;</LI>"
    		 + "\n<LI>Step 2. instructions<BUTTON class=iten_emac2 args=\"FFKkWxcBAE2kQH9wS3cb\" extid=\"assign\"><SPAN class=\"meetMem mtMem1\"><B>G</B><SPAN>rant Test</SPAN></SPAN></BUTTON>&nbsp;<BUTTON class=iten_emac args=\"Approval\" extid=\"loop\">Approval "
    		 + "\n"
    		 + "\n<DIV class=\"badge em_loop\" UNSELECTABLE=\"on\"></DIV></BUTTON>&nbsp;</LI></UL>"
    		 + "\n<DIV>&nbsp;</DIV></TD></TR>"
    		 + "\n<TR>"
    		 + "\n<TD class=row2>"
    		 + "\n<DIV>&nbsp;If you bullet the text in agenda and notes, it creates tasks. Not sure yet if they get added to the meeeting process, but we;ll find out soon enough.&nbsp; In any case, i wonder if we should/could deactivate the task boxes in those sections.</DIV>"
    		 + "\n<DIV>&nbsp;</DIV>"
    		 + "\n<DIV>Ok, I saved the meeting and did not schedule or send emails.&nbsp; left the page and returned from the Dashboard link adn all seems ok.</DIV>"
    		 + "\n<DIV>&nbsp;</DIV>"
    		 + "\n<DIV>Sending an email now.</DIV>"
    		 + "\n<DIV>&nbsp;</DIV>"
    		 + "\n"
    		 + "\n<DIV>Received the Agenda email ok, but nothing posted in my Outlook calendar. </DIV>"
    		 + "\n<DIV>&nbsp;</DIV>"
    		 + "\n<DIV>I suggest some formatting in the email.&nbsp;&nbsp; Definitely headers for Agenda, Action Items, Notes.&nbsp; My first reaction is to msek the&nbsp;tect forn t larger, hide the colored backgroudsn and sue some kind of divider -- dotted line,&nbsp;for example, between the sections.&nbsp;</DIV>"
    		 + "\n<DIV>&nbsp;</DIV>"
    		 + "\n<DIV>going to launch the action items now.</DIV></TD></TR></TBODY></TABLE><!--#end_meet-->"
    		 + "\n	<!--#end_meet-->"
    		 + "\n	</div>"
    		 + "\n"
    		 + "\n	<p><a href=\"http://testlab.itensil.com/act/meetStat?meet=%2Ftestlab%2Fmeeting%2Fkp%20test%20meeting1\">View Active Meeting &gt;</a></p>"
    		 + "\n</body>"
    		 + "\n</html>";
    	
    	
    	String test3 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
    		 + "\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
    		 + "\n<head>"
    		 + "\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"
    		 + "\n<style>"
    		 + "\n/*snomail*/"
    		 + "\n@import url(http://realhost:8080/wf/css/App.css);"
    		 + "\n@import url(http://realhost:8080/wf/css/brd-itensil.css);"
    		 + "\n@import url(http://realhost:8080/wf/css/Wiki.css);"
    		 + "\n@import url(http://realhost:8080/wf/css/Outline.css);"
    		 + "\n/*enomail*/"
    		 + "\n</style>"
    		 + "\n</head>"
    		 + "\n<body style=\"padding:16px\">"
    		 + "\n	<div class=\"outline wikiView\">"
    		 + "\n	<!--#start_meet-->"
    		 + "\n	"
    		 + "\n	<table class=\"mlayout lo_vert3\" cellpadding=\"0\" cellspacing=\"0\">"
    		 + "\n<tbody>"
    		 + "\n<tr>"
    		 + "\n<td class=\"row0\">Roster ok, checking just save, no email.<br></td></tr>"
    		 + "\n"
    		 + "\n<tr>"
    		 + "\n<td class=\"row1\">"
    		 + "\n<ul><li>Draft Item 1&nbsp;</li><li>Draft Item 2 &nbsp;<button class=\"iten_emac2\" extid=\"assign\" args=\"2trfYhUBAAPewKhNZ6nJ\"><span class=\"meetMem mtMem2\"><b>M</b><span>ary Fawcett</span></span></button> <br></li><ul><li class=\"procPar\">Draft Item 3</li><li>Draft 3 &nbsp; <button args=\"activities/Setup Mode/Itensil Software Engineer - rev2.doc|Itensil Software Engineer - rev2.doc\" extid=\"attachEdit\" class=\"iten_emac\">Itensil Software Engineer - rev2.doc<div style=\"-moz-user-select: none;\" class=\"badge em_attachEdit\">&nbsp;</div></button> <br></li></ul></ul></td></tr>"
    		 + "\n<tr>"
    		 + "\n<td class=\"row2\">"
    		 + "\n<div>&nbsp;I like notes!<br></div></td></tr></tbody></table><!--#end_meet-->"
    		 + "\n"
    		 + "\n	<!--#end_meet-->"
    		 + "\n	</div>"
    		 + "\n	<p><a href=\"http://realhost:8080/wf/act/meetStat?meet=%2Fdebug1%2Fmeeting%2FSetup%20Mode\">View Active Meeting &gt;</a></p>"
    		 + "\n</body>"
    		 + "\n</html>";
    	
    	System.out.println(filterEmailHTML(test1, "!!NEWSTYLE!!", "", "http://cool/test", "/mymeet"));
    	System.out.println("------");
    	System.out.println(filterEmailHTML(test2, "!!NEWSTYLE!!", "", "http://cool/test", "/mymeet"));
    	System.out.println("------");
    	System.out.println(filterEmailHTML(test3, "!!NEWSTYLE!!", "", "http://cool/test", "/mymeet"));
    	SimpleDateFormat dFmt = new SimpleDateFormat("EEE, MMM d yyyy HH:mm a (zzzz 'GMT'Z)");
    	System.out.println(dFmt.format(new Date()));
    }
    
    protected String createICS(Activity activityEnt, InternetAddress fromAddr, InternetAddress toAddrs[], String link, Date start, Date end) {
    	
    	SimpleDateFormat dateFmtZ = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    	dateFmtZ.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
    	StringBuffer buf = new StringBuffer(
			"BEGIN:VCALENDAR\n" +
			"PRODID:-//Itensil//ItensilMeeting//EN\n" +
			"VERSION:2.0\n" +
			"CALSCALE:GREGORIAN\n" +
			"METHOD:REQUEST\n" +
			"BEGIN:VEVENT\n");
    	buf.append("DTSTART:").append(dateFmtZ.format(start)).append('\n');
    	buf.append("DTEND:").append(dateFmtZ.format(end)).append('\n');
    	buf.append("DTSTAMP:").append(dateFmtZ.format(new Date())).append('\n');
    	buf.append("ORGANIZER;CN=").append(fromAddr.getPersonal()).append(":MAILTO:").append(fromAddr.getAddress()).append('\n');
    	buf.append("UID:").append(activityEnt.getId()).append('\n');
    	for (InternetAddress to : toAddrs) {
    		buf.append("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=FALSE;CN=") 
    			.append(to.getPersonal()).append(":MAILTO:").append(to.getAddress()).append('\n');
    	}
    	buf.append("CLASS:PRIVATE\n");
    	buf.append("CREATED:").append(dateFmtZ.format(new Date())).append('\n');
    	buf.append("DESCRIPTION:View the meeting at ").append(link).append('\n');
    	buf.append("SEQUENCE:0\n");
    	buf.append("STATUS:CONFIRMED\n");
    	buf.append("SUMMARY:").append(activityEnt.getName()).append('\n');
    	buf.append(
    		"TRANSP:OPAQUE\n" +
    		"END:VEVENT\n" +
			"END:VCALENDAR");
    	return buf.toString();
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

    /**
     * Clean-up
     */
    public void afterMethod() {
        HibernateUtil.closeSession();
    }

}
