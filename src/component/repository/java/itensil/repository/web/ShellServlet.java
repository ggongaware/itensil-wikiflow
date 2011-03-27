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
package itensil.repository.web;

import itensil.io.FixedByteArrayOutputStream;
import itensil.io.StreamUtil;
import itensil.io.xml.XMLDocument;
import itensil.report.ExslTransform;
import itensil.report.ExsltReport;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodeContent;
import itensil.repository.DefaultNodePermission;
import itensil.repository.DefaultNodeProperties;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.NodeLock;
import itensil.repository.NodePermission;
import itensil.repository.NodeProperties;
import itensil.repository.NodeVersion;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.repository.Repository;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryManager;
import itensil.repository.RepositoryManagerFactory;
import itensil.repository.RepositoryNode;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.RelativeGroup;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.util.WildcardPattern;
import itensil.web.ContentType;
import itensil.web.MethodServlet;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import itensil.web.UploadUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ShellServlet extends MethodServlet {

    /**
     *  /page
     *
     * Direct page
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletUtil.forward("/view-repo/list.jsp", request, response);
    }

    /**
     *  /list
     *
     * List folder contents
     *
     */
    @ContentType("text/xml")
    public void webList(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        String uri = request.getParameter("uri");
        boolean alwaysFolders = "1".equals(request.getParameter("folders"));
        Document doc = DocumentHelper.createDocument();
        Element root = doc.addElement("list");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {

            // trim trailing slashes
            while (uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);

            // check for wildcard
            WildcardPattern pat = null;
            String name = UriHelper.name(uri);
            if (name.indexOf("*") >= 0) {
                pat = new WildcardPattern(name);
                uri = UriHelper.getParent(uri);
                root.addAttribute("pattern", name);
            }
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            if (!node.isCollection()) {
                node = (MutableRepositoryNode)node.getParent();
            }
            String pUri = node.getUri();
            root.addAttribute("uri", pUri);
            for (RepositoryNode kid : node.getChildren()) {
                String kName = UriHelper.localizeUri(pUri, kid.getUri());
                if (pat == null || (alwaysFolders && kid.isCollection()) || pat.match(kName)) {
                    Element rnElem = root.addElement("node");
                    rnElem.addAttribute("uri", kName);
                    rnElem.addAttribute("owner", kid.getOwner().getUserId());
                    rnElem.addAttribute("collection", kid.isCollection() ? "1" : "0");
                }
            }
        } else {
            root.addAttribute("uri", "/");
            RepositoryManager man = RepositoryManagerFactory.getManager((User)request.getUserPrincipal());
            for (Repository repo : man.getRepositories()) {
                Element rnElem = root.addElement("node");
                rnElem.addAttribute("uri", repo.getMount().substring(1));
                rnElem.addAttribute("collection", "1");
            }
        }
        RepositoryHelper.commitTransaction();
        ServletUtil.setExpired(response);
        doc.write(response.getWriter());
    }


    /**
     *  /newFolder
     *
     * List folder contents
     *
     */
    @ContentType("text/xml")
    public void webNewFolder(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"uri", "name", "description"});
    	
    	String description = params.get("description");
        String uri = params.get("uri");
        String name = params.get("name");

        if (Check.isEmpty(name) || Check.isEmpty(uri)) {
            throw new NotFoundException("[blank]");
        }
        RepositoryHelper.beginTransaction();
        uri = RepositoryHelper.resolveUri(uri);
        String dstUri = UriHelper.absoluteUri(uri, UriHelper.filterName(name));
        dstUri = RepositoryHelper.getAvailableUri(dstUri);
        MutableRepositoryNode node = RepositoryHelper.createCollection(dstUri);
        
        if (!Check.isEmpty(description)) {
	    	HashMap<QName,String> props = new HashMap<QName,String>();
	    	props.put(PropertyHelper.itensilQName("description"), description);
	    	
	    	PropertyHelper.setNodeValues(node, props);
    	}
        
        RepositoryHelper.commitTransaction();

        // expire since probably get
        ServletUtil.setExpired(response);
        Document doc = DocumentHelper.createDocument();
        doc.addElement("ok").addAttribute("result", dstUri);
        doc.write(response.getWriter());
    }


    /**
     *  /getProps
     *
     * Get node properties
     *
     */
    @ContentType("text/xml")
    public void webGetProps(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
        	
            RepositoryHelper.beginTransaction();
            RepositoryHelper.useReadOnly();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            NodeProperties props = node.getProperties(new DefaultNodeVersion());
            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("properties");
            Namespace davNs =
                    DocumentHelper.createNamespace(PropertyHelper.DEFAULT_PREFIX, PropertyHelper.DEFAULT_QNAMESPACE);
            root.add(davNs);
            root.add(DocumentHelper.createNamespace(PropertyHelper.ITENSIL_PREFIX, PropertyHelper.ITENSIL_QNAMESPACE));
            root.add(DocumentHelper.createNamespace(PropertyHelper.ITENSIL_ENTITY_PREFIX, PropertyHelper.ITENSIL_ENTITY_QNAMESPACE));
            root.addAttribute("uri", node.getUri());
            root.addElement(new org.dom4j.QName("displayname", davNs)).addText(UriHelper.name(uri));
            if (node.isCollection()) {
                root.addElement(new org.dom4j.QName("collection", davNs));
            }
            int nsCount = 0;
            if (props != null) {
                for (Map.Entry<QName,String> prop : props.getPropertyMap().entrySet()) {
                    Namespace ns = root.getNamespaceForURI(prop.getKey().getNamespaceURI());
                    if (ns == null) {
                        nsCount++;
                        root.addNamespace(prop.getKey().getNamespaceURI(), "ns" + nsCount);
                    }
                    Element pElem = root.addElement(new org.dom4j.QName(prop.getKey().getLocalPart(), ns));
                    String val = prop.getValue();
                    if (val != null) pElem.addText(val);
                }
            }
            RepositoryHelper.commitTransaction();
            ServletUtil.setExpired(response);
            doc.write(response.getWriter());
        } else {
            throw new NotFoundException("[blank]");
        }
    }


    /**
     *  /setProps
     *
     * Set node properties
     *
     */
    @ContentType("text/xml")
    public void webSetProps(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document reqDoc = XMLDocument.readStream(request.getInputStream());
        Element root = reqDoc.getRootElement();
        String uri = root.attributeValue("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
        	
            RepositoryHelper.beginTransaction();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            String name = UriHelper.name(node.getUri());

            Namespace davNs = root.getNamespaceForURI(PropertyHelper.DEFAULT_QNAMESPACE);

            // check rename
            String reqName = root.elementTextTrim(new org.dom4j.QName("displayname", davNs));
            if (!Check.isEmpty(reqName)) {
                if (!name.equals(reqName)) {
                    String parUri = UriHelper.getParent(node.getUri());
                    node.move(UriHelper.absoluteUri(parUri, UriHelper.filterName(reqName)));
                }
            }

            Namespace irNs = root.getNamespaceForURI(PropertyHelper.ITENSIL_QNAMESPACE);
            Element desc = root.element(new org.dom4j.QName("description", irNs));
            NodeProperties props = node.getProperties(new DefaultNodeVersion());
            if (props == null) {
                props = new DefaultNodeProperties(new DefaultNodeVersion());
            }
            props.setValue(PropertyHelper.itensilQName("description"), desc != null ? desc.getTextTrim() : "");
            node.setProperties(props);

            RepositoryHelper.commitTransaction();
            response.getWriter().print("<ok/>");

        } else {
            throw new NotFoundException("[blank]");
        }
    }

     /**
     *  /getPerms
     *
     * Get node properties
     *
     */
    @ContentType("text/xml")
    public void webGetPerms(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getParameter("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
            RepositoryHelper.beginTransaction();
            RepositoryHelper.useReadOnly();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);

            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("permissions");
            root.addAttribute("uri", node.getUri());
            root.addAttribute("collection", node.isCollection() ? "1" : "0");
            
            boolean canManage = node.hasPermission(
            		DefaultNodePermission.managePermission(request.getUserPrincipal()));
            
            root.addAttribute("manageable", canManage ? "1" : "0");
            
            String oid = node.getOwner().getUserId();
            root.addAttribute("owner", oid);
            Element ownerElem = root.addElement("perm");
            ownerElem.addAttribute("level", "4");
            ownerElem.addAttribute("user", oid);
            ownerElem.addAttribute("inherit", "1");
            ownerElem.addAttribute("owner", "1");
            

            Group ctxGroup = node.getContextGroup();
            root.addAttribute("contextGroup", ctxGroup != null ? ctxGroup.getGroupId() : "");

            for (NodePermission perm : node.getPermissions()) {
                Element elem = root.addElement("perm");
                elem.addAttribute("level",
                    "" + DefaultNodePermission.permissionToInteger(perm));
                String prinId = "";
                if (perm.getPrincipal() instanceof User) {
                	prinId = ((User)perm.getPrincipal()).getUserId();
                    elem.addAttribute("user", prinId);
                } else if (perm.isRelativeRole()) {
                	GroupAxis axis = perm.getAxis();
                	elem.addAttribute("relative", "1");
                	elem.addAttribute("axis", axis != null && axis != GroupAxis.SELF ? axis.toString() : "");
                	String role = Check.emptyIfNull(perm.getRole());
                	elem.addAttribute("position", role);
                	prinId = ((Group)perm.getPrincipal()).getGroupId();
                } else {
                	prinId = ((Group)perm.getPrincipal()).getGroupId();
                    elem.addAttribute("group", prinId);
                }
                elem.addAttribute("oprin", prinId);
                elem.addAttribute("inherit", perm.isInheritable() ? "1" : "0");
                elem.addAttribute("change", "0");
            }
            RepositoryHelper.commitTransaction();
            ServletUtil.setExpired(response);
            doc.write(response.getWriter());
        }  else {
            throw new NotFoundException("[blank]");
        }
    }


    /**
     *  /setPerms
     *
     * Set node properties
     * 
     * You should always reload the list before saving again
     * 
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webSetPerms(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Document reqDoc = XMLDocument.readStream(request.getInputStream());
        Element root = reqDoc.getRootElement();
        String uri = root.attributeValue("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {

            RepositoryHelper.beginTransaction();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            
            String cgId = root.attributeValue("contextGroup");
            
            if (Check.isEmpty(cgId)) {
            	if (node.getContextGroup() != null) {
            		node.setContextGroup(null);
            		RepositoryHelper.saveNode(node);
            	}
            } else {
            	if (!(node.getContextGroup() != null && node.getContextGroup().getGroupId().equals(cgId))) {
            		node.setContextGroup(new DefaultGroup(cgId));
            		RepositoryHelper.saveNode(node);
            	}
            }
            
            
            int relIndex = 0;
            /**
             * This loop should work reliably if new permissions
             * are always appended to the bottom.
             */
            for (Element pElem : (List<Element>)root.elements("perm")) {
                if (!"1".equals(pElem.attributeValue("owner"))) {
                    String pid = pElem.attributeValue("user");
                    String opid = pElem.attributeValue("oprin");
                    Principal prin = null, oprin = null;
                    GroupAxis axis = null;
                    String role = null;
                    if (Check.isEmpty(pid)) {
                    	if ("1".equals(pElem.attributeValue("relative"))) {
                    		prin = new RelativeGroup(relIndex++);
                    		String sAx = pElem.attributeValue("axis");
                    		axis = Check.isEmpty(sAx) ? GroupAxis.SELF : GroupAxis.valueOf(sAx);
                    		role = pElem.attributeValue("position");
                    		oprin = Check.isEmpty(opid) ? null : new DefaultGroup(opid);
                    	} else {
                    		pid = pElem.attributeValue("group");
                    		prin = Check.isEmpty(pid) ? null : new DefaultGroup(pid);
                    		oprin = Check.isEmpty(opid) ? prin : new DefaultGroup(opid);
                    	}
                    	
                    } else {
                        prin = new DefaultUser(pid);
                        oprin = Check.isEmpty(opid) ? prin : new DefaultUser(opid);
                    }
                    if ("1".equals(pElem.attributeValue("revoke")) || "2".equals(pElem.attributeValue("change"))) {
                    	if (oprin != null) node.revokePermission(DefaultNodePermission.noPermission(oprin));
                    } else {
                        if (oprin != null) node.revokePermission(DefaultNodePermission.noPermission(oprin));
                        if (prin != null) {
	                        DefaultNodePermission perm = new DefaultNodePermission(prin,
	                                Integer.parseInt(pElem.attributeValue("level")),
	                                "1".equals(pElem.attributeValue("inherit")),
	                                axis, role);
	                        node.grantPermission(perm);
                        }
                    }
                }
            }

            RepositoryHelper.commitTransaction();
            response.getWriter().print("<ok/>");

        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /status
     *
     * Node status, including locks and history
     *
     */
    @ContentType("text/xml")
    public void webStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
            RepositoryHelper.beginTransaction();
            RepositoryHelper.useReadOnly();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);

            Document doc = DocumentHelper.createDocument();
            Element root = doc.addElement("status");
            root.addAttribute("uri", node.getUri());

            // locks
            for (NodeLock lock : node.getLocks()) {
                Element lockElem = root.addElement("lock");
                lockElem.addAttribute("ower", lock.getOwner().getUserId());
                lockElem.addAttribute("exclusive", lock.isExclusive() ? "1" : "0");
            }

            // versions
            for (NodeVersion ver : node.getVersions()) {
                Element verElem = root.addElement("version");
                verElem.addAttribute("default", ver.isDefault() ? "1" : "0");
                verElem.addAttribute("label", ver.getLabel() != null ? ver.getLabel() : "");
                verElem.addAttribute("number", ver.getNumber());

                NodeProperties props = node.getProperties(ver);
                if (props != null) {
                    verElem.addAttribute("time", props.getValue("getlastmodified"));
                    verElem.addAttribute("modifier", props.getValue(PropertyHelper.itensilQName("modifier")));
                }
            }

            RepositoryHelper.commitTransaction();
            ServletUtil.setExpired(response);
            doc.write(response.getWriter());
        } else {
            throw new NotFoundException("[blank]");
        }
    }
    
    /**
     *  /xslt
     *
     * 	XSL Transform
     * 
     *		srcUri - input xml file
     *		dstUri - output html/xml/text file
     *		xslUri - style document
     */
    @ContentType("text/xml")
    public void webXslt(HttpServletRequest request, HttpServletResponse response) throws Exception {

    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"srcUri", "dstUri", "xslUri"});
    	
    	String xslUri = params.get("xslUri");
        String srcUri = params.get("srcUri");
        String dstUri = params.get("dstUri");
        if (!(Check.isEmpty(srcUri) || srcUri.length() < 2 || Check.isEmpty(dstUri) || dstUri.length() < 2 ||
        		Check.isEmpty(xslUri) || xslUri.length() < 2)) {
        	
        	xslUri = RepositoryHelper.resolveUri(xslUri);
        	srcUri = RepositoryHelper.resolveUri(srcUri);
        	dstUri = RepositoryHelper.resolveUri(dstUri);
        	
        	RepositoryHelper.beginTransaction();
        	Repository repository = RepositoryHelper.getRepository(dstUri);
        	MutableRepositoryNode node;
            NodeProperties props;
            NodeVersion version = new DefaultNodeVersion();
            try {
                node = repository.getNodeByUri(dstUri, true);
                if (node.isCollection()) throw new AccessDeniedException(dstUri, "collection");
                props = node.getProperties(version);
                if (props != null) {
                    version = RepositoryHelper.nextVersion(node, props.getVersion(), true);
                    props = new DefaultNodeProperties(version, props.getPropertyMap());
                } else {
                    props = new DefaultNodeProperties(version);
                }
            } catch (NotFoundException nfe) {
                node = repository.createNode(
                		dstUri, false, (User)request.getUserPrincipal());
                props = new DefaultNodeProperties(version);
            }
            
            ByteArrayOutputStream ostream = new ByteArrayOutputStream(1024 * 16);
            
            StreamResult result = new StreamResult(ostream);
            
            ExslTransform.transformXML(result,
             		new StreamSource(RepositoryHelper.loadContent(srcUri)), 
             		new StreamSource(RepositoryHelper.loadContent(xslUri)), 
             		UriHelper.getParent(srcUri), null);
            
        	NodeContent content = new DefaultNodeContent(ostream.toByteArray(), version);

            // properties
            String mime = getServletContext().getMimeType(dstUri);
            if (mime == null) mime = "application/octet-stream";
            PropertyHelper.setStandardProperties( props, dstUri, mime, content.getLength());
            node.setProperties(props);
            
            node.setContent(content);

            RepositoryHelper.commitTransaction();
            
            Document doc = DocumentHelper.createDocument();
            doc.addElement("ok").addAttribute("result", node.getUri());
            
            ServletUtil.setExpired(response);
            doc.write(response.getWriter());
        }
    }
   

    /**
     *  /copy
     *
     * Copy a node
     *
     */
    @ContentType("text/xml")
    public void webCopy(HttpServletRequest request, HttpServletResponse response) throws Exception {

    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"srcUri", "dstUri", "description"});
    	
    	String description = params.get("description");
        String srcUri = params.get("srcUri");
        String dstUri = params.get("dstUri");
        if (!(Check.isEmpty(srcUri) || srcUri.length() < 2 || Check.isEmpty(dstUri) || dstUri.length() < 2 )) {
        	dstUri = filterDestUri(dstUri);
            RepositoryHelper.beginTransaction();
            RepositoryNode resNode = RepositoryHelper.copyAndUpdate(
            		dstUri, srcUri, (User)request.getUserPrincipal(), description);
            
            RepositoryHelper.commitTransaction();

            // expire since probably get
            ServletUtil.setExpired(response);
            Document doc = DocumentHelper.createDocument();
            doc.addElement("ok").addAttribute("result", resNode.getUri());
            doc.write(response.getWriter());

        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /move
     *
     * Mode a node
     *
     */
    @ContentType("text/xml")
    public void webMove(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String srcUri = request.getParameter("srcUri");
        String dstUri = request.getParameter("dstUri");
        if (!(Check.isEmpty(srcUri) || srcUri.length() < 2 || Check.isEmpty(dstUri) || dstUri.length() < 2 )) {
        	dstUri = filterDestUri(dstUri);
            RepositoryHelper.beginTransaction();
            dstUri = RepositoryHelper.resolveUri(dstUri);
            srcUri = RepositoryHelper.resolveUri(srcUri);
            MutableRepositoryNode node = getNode(srcUri);
            node.move(dstUri);
            RepositoryHelper.commitTransaction();

            // expire since probably get
            ServletUtil.setExpired(response);
            Document doc = DocumentHelper.createDocument();
            doc.addElement("ok").addAttribute("result", node.getUri());
            doc.write(response.getWriter());

        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /setVersion
     *
     * Change the default node version
     *
     */
    @ContentType("text/xml")
    public void webSetVersion(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        String version = request.getParameter("version");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {

            RepositoryHelper.beginTransaction();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            node.setDefaultVersion(new DefaultNodeVersion(version, false));
            RepositoryHelper.commitTransaction();

            // expire since probably get
            ServletUtil.setExpired(response);
            response.getWriter().print("<ok/>");
        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /delete
     *
     * Delete a node
     *
     */
    @ContentType("text/xml")
    public void webDelete(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        if (!(Check.isEmpty(uri) || uri.length() < 2)) {
            RepositoryHelper.beginTransaction();
            uri = RepositoryHelper.resolveUri(uri);
            MutableRepositoryNode node = getNode(uri);
            node.remove();
            RepositoryHelper.commitTransaction();

            // expire since probably get
            ServletUtil.setExpired(response);
            response.getWriter().print("<ok/>");
        } else {
            throw new NotFoundException("[blank]");
        }
    }

    /**
     *  /upload
     *
     * Browser Uploads
     *
     */
    public void webUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {

        UploadUtil upload = new UploadUtil();
        upload.saveUploads(request);
        Map<String,File> files = upload.getFiles();
        Map<String,String> params = upload.getParameterMap();

        String uri = params.get("uri");
        request.setAttribute("clientId", params.get("clientId"));
        String upUri = "";
        Set<Map.Entry<String,File>> filSet = files.entrySet();

        // grab first file entry
        if (!filSet.isEmpty()) {

            RepositoryHelper.beginTransaction();
            for (Map.Entry<String,File> ent : filSet) {
                File tmp = ent.getValue();
                String name = ent.getKey();

                // stick file into uri
                if (!Check.isEmpty(uri)) {
                	uri = RepositoryHelper.resolveUri(uri);
                    Repository repository = RepositoryHelper.getRepository(uri);
                    upUri = UriHelper.absoluteUri(uri, name);
                    MutableRepositoryNode node;
                    NodeProperties props;
                    NodeVersion version = new DefaultNodeVersion();
                    try {
                        node = repository.getNodeByUri(upUri, true);
                        if (node.isCollection()) throw new AccessDeniedException(upUri, "collection");
                        props = node.getProperties(version);
                        if (props != null) {
                            version = RepositoryHelper.nextVersion(node, props.getVersion(), true);
                            props = new DefaultNodeProperties(version, props.getPropertyMap());
                        } else {
                            props = new DefaultNodeProperties(version);
                        }
                    } catch (NotFoundException nfe) {
                        node = repository.createNode(
                                upUri, false, (User)request.getUserPrincipal());
                        props = new DefaultNodeProperties(version);
                    }

                    // content
                    FixedByteArrayOutputStream ostream = new FixedByteArrayOutputStream((int)tmp.length());
                    StreamUtil.copyStream(new FileInputStream(tmp), ostream);
                    NodeContent content = new DefaultNodeContent(ostream.getBytes(), version);

                    // properties
                    String mime = getServletContext().getMimeType(name);
                    if (mime == null) mime = "application/octet-stream";
                    PropertyHelper.setStandardProperties( props, upUri, mime, content.getLength());
                    node.setProperties(props);
                    
                    // content moved... hibernate slow down
                    node.setContent(content);
                }
                tmp.delete();
                
            }
            RepositoryHelper.commitTransaction();
        }
        request.setAttribute("uri", upUri);
        ServletUtil.forward("/view-repo/uploaded.jsp", request, response);
    }

    private String filterDestUri(String dstUri) {
    	String name = UriHelper.name(dstUri);
    	String parUri = UriHelper.getParent(dstUri);
    	return UriHelper.absoluteUri(parUri, UriHelper.filterName(name));
    }

   /*
    *  Find the node for this URI
    */
    private MutableRepositoryNode getNode(String uri) throws NotFoundException, AccessDeniedException {
        return RepositoryHelper.getNode(uri, !RepositoryHelper.isReadOnly());
    }

    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        RepositoryHelper.rollbackTransaction();
    }

    public void afterMethod() {
        RepositoryHelper.closeSession();
    }

}
