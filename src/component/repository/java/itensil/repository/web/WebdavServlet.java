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
/*
 * Created on Jan 20, 2004
 *
 */
package itensil.repository.web;

import itensil.io.StreamUtil;
import itensil.io.xml.XMLWriter;
import itensil.repository.*;
import itensil.util.*;
import itensil.web.ServletUtil;
import itensil.web.UrlUtil;
import itensil.security.User;
import itensil.security.Anonymous;
import itensil.uidgen.IUID;

import java.io.*;
import java.util.*;
import java.net.URLDecoder;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * @author ggongaware@itensil.com
 *
 */
public class WebdavServlet extends HttpServlet {

    // limit infinity to 3 levels deep
    public static final int INFINITY = 3;

    // propfind ops
    public static final int FIND_ALL_PROP = 0;
    public static final int FIND_BY_PROPERTY = 1;
    public static final int FIND_PROPERTY_NAMES = 2;

    public static final QName AUTO_PROPS[] = {
        PropertyHelper.defaultQName("displayname"),
        PropertyHelper.defaultQName("resourcetype"),
        PropertyHelper.defaultQName("supportedlock"),
        PropertyHelper.defaultQName("lockdiscovery")};

    public static final Set<QName> DATE_PROPS = new HashSet<QName>();
    static {
        DATE_PROPS.add(PropertyHelper.defaultQName("getlastmodified"));
        DATE_PROPS.add(PropertyHelper.defaultQName("creationdate"));
    }

    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PROPFIND = "PROPFIND";
    public static final String METHOD_PROPPATCH = "PROPPATCH";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_MKCOL = "MKCOL";
    public static final String METHOD_COPY = "COPY";
    public static final String METHOD_MOVE = "MOVE";
    public static final String METHOD_LOCK = "LOCK";
    public static final String METHOD_UNLOCK = "UNLOCK";

    public static final String DAV = PropertyHelper.DEFAULT_QNAMESPACE;

    /**
     * Create a new lock.
     */
    public static final int LOCK_CREATION = 0;

    /**
     * Refresh lock.
     */
    public static final int LOCK_REFRESH = 1;

    /**
     * Shared scope lock.
     */
    public static final int LOCK_SHARED = 0;

    /**
     * Exclusive scope lock.
     */
    public static final int LOCK_EXCLUSIVE = 1;

    /**
     * Default lock timeout value.
     */
    public static final int DEFAULT_TIMEOUT = 3600;

    /**
     * Maximum lock timeout.
     */
    public static final int MAX_TIMEOUT = 604800;

    protected static User ANONYMOUS = new Anonymous();

    protected static DocumentBuilderFactory documentBuilderFactory;
    
    static {
    	documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }
   

    private static boolean DEBUG = false;



    /*
     *  Find the node for this URI
     */
    private RepositoryNode getNode(HttpServletRequest request, String uri, boolean isReadOnly)
        throws NotFoundException, AccessDeniedException {
    	
        return RepositoryHelper.getNode(uri, !isReadOnly);
    }

    /*
     * @see HttpServlet#service(HttpServletRequest, HttpServletResponse)
     */
    protected void service(
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        String uri = request.getPathInfo();

        // if /servlet send to /servlet/
        if (Check.isEmpty(uri)) {
        	if ("PROPFIND".equals(method)) {
        		response.setStatus(WebdavStatus.SC_MULTI_STATUS);
                response.setContentType("text/xml; charset=UTF-8");
                response.addHeader("DAV", "1,2");
                response.addHeader("MS-Author-Via", "DAV");
        		response.getWriter().print(
        				"<d:multistatus xmlns:d=\"DAV:\">" 
        	     	  + "<d:response>"
        	          + "<d:href>" + ServletUtil.getAbsoluteServletPath(request) + "/</d:href>"
        	          + "<d:propstat>"
        	          + "<d:status>HTTP/1.1 200 OK</d:status>"
        	          + "<d:prop>"
        	          + "<d:resourcetype><d:collection/></d:resourcetype>"
        	          + "</d:prop>"
        	          + "</d:propstat>"
        	          + "</d:response>"
        	          + "</d:multistatus>");
        	} else {
        	
	            ServletUtil.bounce(
	                ServletUtil.getServletPath(request) + "/",
	                request.getQueryString(),
	                response);
        	}
            return;
        }
        
        String reqPath = request.getServletPath();
        
        // allow an artificial 2nd root (bad MS patch behavior)
        if ("/dav".equals(reqPath)) {
        	int arpos = uri.indexOf('/', 1);
        	if (arpos > 0) {
        		uri = uri.substring(arpos);
        	}
        }

        boolean isGetPostHead = method.equals(METHOD_GET)
            || method.equals(METHOD_POST)
            || method.equals(METHOD_HEAD);

        RepositoryHelper.beginTransaction();
        boolean isReadOnly = isGetPostHead || method.equals(METHOD_OPTIONS) || method.equals(METHOD_PROPFIND);
        if (isReadOnly) {
            RepositoryHelper.useReadOnly();
        }
        RepositoryNode node = null;
        NotFoundException nfe = null;
        if (!uri.equals("/")) {
            while (uri.endsWith("/")) uri = uri.substring(0, uri.length() - 1);
            // MS Office xml extension hack
            if ("/filx".equals(reqPath)) {
            	uri += ".xml";
            }
            uri = RepositoryHelper.resolveUri(uri);	
            try {
                node = getNode(request, uri, isReadOnly);
            } catch (NotFoundException e) {
                node = null;
                nfe = e;
            } catch (AccessDeniedException e) {
                error(request, response, e);
                RepositoryHelper.closeSession();
                return;
            }
        }

        if (DEBUG) {
            System.out.println(method + " " + uri);
        }

        // GET and POST on a collection or root get sent to the shell
        if ((node == null || node.isCollection()) && isGetPostHead) {
        	RepositoryHelper.closeSession();
            if (nfe != null) {
                error(request, response, nfe);
                return;
            }
            if (ANONYMOUS.equals(request.getUserPrincipal())) {
                error(
                    request,
                    response,
                    new AccessDeniedException(uri, "No anonymous access"));
                return;
            }
            ServletUtil.bounce(
                ServletUtil.getServletPath(request, "/shell/page"),
                "uri=" + UrlUtil.encode(uri),
                response);
        } else {
            if (node == null) {
                nfe = new NotFoundException(uri);
            }
            try {
                if (     isGetPostHead) {
                    if (nfe != null) throw nfe;
                    doGet(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_OPTIONS)) {
                    doOptions(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_PUT)) {
                    doPut(request, response, uri, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_PROPFIND)) {
                    if (nfe != null) throw nfe;
                    doPropfind(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_PROPPATCH)) {
                    if (nfe != null) throw nfe;
                    doProppatch(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_DELETE)) {
                    if (nfe != null) throw nfe;
                    doDelete(request, response, (MutableRepositoryNode)node);
                    node = null;
                } else if ( method.equals(METHOD_COPY)) {
                    if (nfe != null) throw nfe;
                    doCopy(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_MOVE)) {
                    if (nfe != null) throw nfe;
                    doMove(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_MKCOL)) {
                    doMkcol(request, response, uri);
                } else if ( method.equals(METHOD_LOCK)) {
                    if (nfe != null) throw nfe;
                    doLock(request, response, (MutableRepositoryNode)node);
                } else if ( method.equals(METHOD_UNLOCK)) {
                    if (nfe != null) throw nfe;
                    doUnlock(request, response, (MutableRepositoryNode)node);
                } else {
                    response.sendError(
                        HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "Not yet implemented");
                }
                RepositoryHelper.commitTransaction();
            } catch (AccessDeniedException e) {
                error(request, response, e);
            }  catch (LockException e) {
                error(request, response, e);
            } catch (NotFoundException e) {
                error(request, response, e);
            } catch (DuplicateException e) {
                error(request, response, e);
            } finally {
            	if (!isReadOnly && node != null) RepositoryHelper.releaseNode(node);
                RepositoryHelper.closeSession();
            }
        }
    }

    protected void doGet(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException,
            LockException {

        NodeVersion version;

        // this version param is not in WebDav standard
        String verStr = request.getParameter("version");
        if (Check.isEmpty(verStr)) {
            version = new DefaultNodeVersion();
        } else {
            version = new DefaultNodeVersion(verStr, false);
        }
        NodeProperties props = node.getProperties(version);
        if (props == null) {
            response.sendError(WebdavStatus.SC_NOT_FOUND, "Version number");
            return;
        }
        
        //      TODO Range support
        String mime = props.getValue("getcontenttype");
        if (mime.indexOf("xml") >= 0 || mime.indexOf("xhtml") >= 0) { // for IE AJAX caching
            ServletUtil.setExpired(response);
            if (node.hasPermission(DefaultNodePermission.writePermission(request.getUserPrincipal()))) {
                response.addHeader("Can-Write", "write");
            }
        }
        
        // because sometimes the Internet Explodes ;-)
        if (node.getUri().endsWith("rules.xml")) {
        	ServletUtil.noCache(response);
        }

        // Check modify dates and eTags
        if (checkNodeNotModified(props, request, response)) {
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince > 0) {
            Date lastModified = PropertyHelper.parseDate(props.getValue("getlastmodified"));
             if (lastModified != null && lastModified.getTime() > ifUnmodifiedSince) {

                // Sorry it's been modified
                response.sendError( HttpServletResponse.SC_PRECONDITION_FAILED);
                return;
            }
        }

        response.setContentType(mime);

        response.setContentLength(
            Integer.parseInt(props.getValue("getcontentlength")));

        NodeContent content = node.getContent(version);
        StreamUtil.copyStream(
                content.getStream(), response.getOutputStream());
    }

    /**
     * OPTIONS Method.
     */
    protected void doOptions(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws ServletException, IOException {

        StringBuffer methodsAllowed = new StringBuffer();
        if (node == null) {
            methodsAllowed.append("OPTIONS, MKCOL, PUT");
        } else {
            methodsAllowed.append("OPTIONS, GET, HEAD, DELETE, PROPFIND"
                + ", PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PUT, POST");
            if (node.isCollection()) {
                 methodsAllowed.append(", MKCOL");
            }
        }
        response.addHeader("Allow", methodsAllowed.toString());
        response.addHeader("DAV", "1,2");
        response.addHeader("MS-Author-Via", "DAV");
    }

    protected void doDelete(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException,
            LockException {

        node.remove();
    }

    protected void doLock(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException,
            LockException {

        //response.sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
        String depthStr = request.getHeader("Depth");
        int depth;
        if (depthStr == null) {
            depth = INFINITY;
        } else {
            if (depthStr.equals("0")) {
                depth = 0;
            } else {
                depth = INFINITY;
            }
        }
        int lockDuration;
        String lockDurationStr = request.getHeader("Timeout");
        if (lockDurationStr == null) {
            lockDuration = DEFAULT_TIMEOUT;
        } else {
            int commaPos = lockDurationStr.indexOf(",");
            // If multiple timeouts, just use the first
            if (commaPos != -1) {
                lockDurationStr = lockDurationStr.substring(0, commaPos);
            }
            if (lockDurationStr.startsWith("Second-")) {
                lockDuration = Integer.parseInt(lockDurationStr.substring(7));
            } else {
                if (lockDurationStr.equalsIgnoreCase("infinity")) {
                    lockDuration = MAX_TIMEOUT;
                } else {
                    try {
                        lockDuration = Integer.parseInt(lockDurationStr);
                    } catch (NumberFormatException e) {
                        lockDuration = MAX_TIMEOUT;
                    }
                }
            }
            if (lockDuration == 0) {
                lockDuration = DEFAULT_TIMEOUT;
            }
            if (lockDuration > MAX_TIMEOUT) {
                lockDuration = MAX_TIMEOUT;
            }
        }

        PropertyHelper.QNamespaces qns = new PropertyHelper.QNamespaces();
        int lockRequestType = LOCK_CREATION;
        String owner = "";
        int scope = LOCK_EXCLUSIVE;
        Node lockInfoNode = null;
        DocumentBuilder documentBuilder = getDocumentBuilder();
        try {
            Document document =
                documentBuilder.parse(
                    new InputSource(request.getInputStream()));

            // Get the root element of the document
            lockInfoNode = document.getDocumentElement();
        } catch(Exception e) {
            // if no body its refresh
            lockRequestType = LOCK_REFRESH;
        }
        if (lockInfoNode != null) {

            // Reading lock information
            NodeList childList = lockInfoNode.getChildNodes();
            Node lockScopeNode = null;
            Node lockTypeNode = null;
            Node lockOwnerNode = null;
            for (int i=0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    if (DAV.equals(currentNode.getNamespaceURI())) {
                        String nodeName = currentNode.getLocalName();
                        if (nodeName.equals("lockscope")) {
                            lockScopeNode = currentNode;
                        }
                        if (nodeName.equals("locktype")) {
                            lockTypeNode = currentNode;
                        }
                        if (nodeName.equals("owner")) {
                            lockOwnerNode = currentNode;
                        }
                    }
                    break;
                }
            }

            if (lockScopeNode != null) {
                childList = lockScopeNode.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (DAV.equals(currentNode.getNamespaceURI())) {
                            String nodeName = currentNode.getLocalName();
                            if (nodeName.equals("shared")) {
                                scope = LOCK_SHARED;
                            }  else {
                                scope = LOCK_EXCLUSIVE;
                            }
                        }
                        break;
                    }
                }
            } else {

                // Bad request
                response.setStatus(WebdavStatus.SC_BAD_REQUEST);
            }

            if (lockTypeNode == null) {

                // Bad request
                response.setStatus(WebdavStatus.SC_BAD_REQUEST);
            }

            if (lockOwnerNode != null) {
                StringWriter ownerOut = new StringWriter();
                XMLWriter ownerXml = new XMLWriter(ownerOut);
                childList = lockOwnerNode.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        ownerXml.text(currentNode.getNodeValue());
                        break;
                    case Node.ELEMENT_NODE:
                        writeDOM(currentNode, ownerXml, qns);
                        break;
                    }
                }
                ownerXml.endDocument();
                owner = ownerOut.toString();
            }
        }
        NodeLock lock = null;
        if (lockRequestType == LOCK_CREATION) {
            lock = RepositoryHelper.lockNode(
                node,
                lockDuration,
                scope == LOCK_EXCLUSIVE,
                depth == INFINITY,
                owner);
            // Add the Lock-Token header as by RFC 2518 8.10.1
            // - only do this for newly created locks
            response.setHeader("Lock-Token", "<opaquelocktoken:"
                           + new IUID(lock.getLockId()).toUUID() + ">");
        } else {

            String ifHeader = request.getHeader("If");
            if (ifHeader == null)
                ifHeader = "";
            if (DEBUG) {
                System.out.println("LOCK(refresh) " + ifHeader);
            }
            Principal caller = request.getUserPrincipal();
            NodeLock locks[] = node.getLocks();
            for (NodeLock lock1 : locks) {
                String uuid = new IUID(lock1.getLockId()).toUUID();
                if (caller.equals(lock1.getOwner())
                        && ifHeader.indexOf(uuid) != -1) {
                    lock = lock1;
                    break;
                }
            }
            if (lock != null) {
                lock = RepositoryHelper.renewLock(
                    node, lock.getLockId(), lockDuration);
            }

            if (lock == null) {
                response.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
                return;
            }
        }
        response.setContentType("text/xml; charset=UTF-8");

        XMLWriter xmlOut;
        StringWriter xBuf = new StringWriter();
        xmlOut = new XMLWriter(xBuf);

        xmlOut.writeXMLHeader();
        String attribs[][] = {
            {"xmlns:" + PropertyHelper.DEFAULT_PREFIX, DAV}};

        // <d:prop xmlns...>
        xmlOut.startElement(
            qns.fullName(DAV, "prop"), attribs, null, false);
        // <d:lockdiscovery>
        xmlOut.startElement(qns.fullName(DAV, "lockdiscovery"), null);

        // active lock
        writeActiveLock(lock, xmlOut, qns);

        // </d:lockdiscovery>
        xmlOut.endElement(qns.fullName(DAV, "lockdiscovery"));
        // </d:prop>
        xmlOut.endElement(qns.fullName(DAV, "prop"));
        xmlOut.endDocument();
        String xStr = xBuf.toString();
        if (DEBUG) {
            System.out.println(xStr);
        }
        byte xByt[] = xStr.getBytes("UTF-8");
        response.setContentLength(xByt.length);
        response.getOutputStream().write(xByt);
    }

    protected void doUnlock(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException,
            LockException {

        String lockTokenHeader = request.getHeader("Lock-Token");
        if (lockTokenHeader == null)
            lockTokenHeader = "";

        if (DEBUG) {
            System.out.println("UNLOCK " + lockTokenHeader);
        }

        NodeLock locks[] = node.getLocks();
        boolean unlocked = false;
        for (NodeLock lock : locks) {
            String uuid = new IUID(lock.getLockId()).toUUID();
            if (lockTokenHeader.indexOf(uuid) != -1) {
                node.killLock(lock);
                unlocked = true;
                if (DEBUG) {
                    System.out.println("UNLOCK ok");
                }
                break;
            }
        }
        if (unlocked) {
            response.setStatus(WebdavStatus.SC_NO_CONTENT);
        } else {
            response.sendError(WebdavStatus.SC_PRECONDITION_FAILED);
        }
    }

    protected void doPropfind(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException {

        // Properties which are to be displayed.
        ArrayList<QName> properties = null;
        // Propfind depth
        int depth = INFINITY;
        // Propfind type
        int type = FIND_ALL_PROP;

        String depthStr = request.getHeader("Depth");
        if (depthStr == null) {
            depth = INFINITY;
        } else {
            if (depthStr.equals("0")) {
                depth = 0;
            } else if (depthStr.equals("1")) {
                depth = 1;
            } else if (depthStr.equalsIgnoreCase("infinity")) {
                depth = INFINITY;
            }
        }

        Node propNode = null;
        DocumentBuilder documentBuilder = getDocumentBuilder();
        try {
            int len = request.getContentLength();
            if (len == -1 || len > 3) {
                Document document = documentBuilder.parse
                    (new InputSource(request.getInputStream()));

                // Get the root element of the document
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (DAV.equals(currentNode.getNamespaceURI())) {
                            if (currentNode.getLocalName().equals("prop")) {
                                type = FIND_BY_PROPERTY;
                                propNode = currentNode;
                            }
                            if (currentNode.getLocalName().equals("propname")) {
                                type = FIND_PROPERTY_NAMES;
                            }
                            if (currentNode.getLocalName().equals("allprop")) {
                                type = FIND_ALL_PROP;
                            }
                        }
                        break;
                    }
                }
            }
        } catch(Exception e) {
            // Most likely there was no content : we use the defaults.
        }

        PropertyHelper.QNamespaces qns = new PropertyHelper.QNamespaces(true);
        if (type == FIND_BY_PROPERTY) {
            properties = new ArrayList<QName>();
            NodeList childList = propNode.getChildNodes();
            for (int i=0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                case Node.TEXT_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    QName propertyName =
                        new QName(
                            currentNode.getNamespaceURI(),
                            currentNode.getLocalName());
                    if (DEBUG) {
                        System.out.println(currentNode.getPrefix() + ":" +
                            currentNode.getLocalName());
                    }
                    if (!qns.hasPrefix(currentNode.getNamespaceURI())) {
                        qns.addPrefix(
                            currentNode.getNamespaceURI(),
                            currentNode.getPrefix());
                    }
                    properties.add(propertyName);
                    break;
                }
            }
        }

        // send response
        response.setStatus(WebdavStatus.SC_MULTI_STATUS);
        response.setContentType("text/xml; charset=UTF-8");

        XMLWriter xmlOut;
        StringWriter xBuf = new StringWriter();
        xmlOut = new XMLWriter(xBuf);

        xmlOut.writeXMLHeader();
        String attribs[][] = {
            {"xmlns", DAV}};

        // <d:multistatus xmlns...>
        xmlOut.startElement(
            qns.fullName(DAV, "multistatus"), attribs, null, false);
        
        // 2nd path hack
        String baseHref = ServletUtil.getAbsoluteServletPath(request);
        if ("/dav".equals(request.getServletPath())) {
        	baseHref += UriHelper.getRoot(request.getPathInfo());
        }

        writePropeties(
            xmlOut,
            (User)request.getUserPrincipal(),
            baseHref,
            qns,
            node,
            type,
            properties,
            depth);

        // </d:multistatus>
        xmlOut.endElement(qns.fullName(DAV, "multistatus"), true);
        xmlOut.endDocument();
        String xStr = xBuf.toString();
        if (DEBUG) {
            System.out.println(xStr);
        }
        byte xByt[] = xStr.getBytes("UTF-8");
        response.setContentLength(xByt.length);
        response.getOutputStream().write(xByt);
    }

    protected void writePropeties(
        XMLWriter xmlOut,
        User user,
        String baseHref,
        PropertyHelper.QNamespaces qns,
        MutableRepositoryNode node,
        int type,
        List properties,
        int depth)
        throws
            IOException {

        // <d:response>
        xmlOut.startElement(qns.fullName(DAV, "response"), null);
        // <d:href>
        String uri = node.getUri();
        String href = UrlUtil.encode(uri.replace('/', '*'));
        xmlOut.startElement(
            qns.fullName(DAV, "href"),
            baseHref + href.replace('*', '/') + (node.isCollection() ? "/" : ""));
        // </d:href>
        xmlOut.endElement(qns.fullName(DAV, "href"));
        // <d:propstat>
        xmlOut.startElement(qns.fullName(DAV, "propstat"), null);
        try {
            NodeProperties props = node.getProperties(new DefaultNodeVersion());
            QName names[] = new QName[0];
            HashSet<String> nsUris = new HashSet<String>();
            if (props != null) {
                names = props.getNames();
                for (QName name : names) {
                    if (!DAV.equals(name.getNamespaceURI())) {
                        if (type != FIND_BY_PROPERTY
                                || properties.contains(name)) {
                            nsUris.add(name.getNamespaceURI());
                        }
                    }
                }
            }
            String nsAttribs[][] = new String[nsUris.size()][2];
            Iterator itr = nsUris.iterator();
            int j = 0;
            while (itr.hasNext()) {
                String nsUri = (String)itr.next();
                nsAttribs[j][0] = "xmlns:" + qns.getPrefix(nsUri);
                nsAttribs[j][1] = nsUri;
                j++;
            }
            // <d:prop xmlns...>
            xmlOut.startElement(
                qns.fullName(DAV, "prop"),
                nsAttribs,
                null,
                false);

            switch (type) {

                case FIND_ALL_PROP:
                    for (QName qn : names) {
                        String val = props.getValue(qn);
                        if (DATE_PROPS.contains(qn)) {
                            val = PropertyHelper.httpDateString(
                                    PropertyHelper.parseDate(val));
                        }
                        //<xxx:yyy>
                        xmlOut.startElement(qns.fullName(qn), val);
                        //</xxx:yyy>
                        xmlOut.endElement(qns.fullName(qn));
                    }
                    for (int i=0; i < AUTO_PROPS.length; i++) {
                        switch (i) {
                            case 0: //displayname
                                xmlOut.startElement(
                                    qns.fullName(AUTO_PROPS[0]),
                                    UriHelper.name(node.getUri()));
                                xmlOut.endElement(
                                    qns.fullName(AUTO_PROPS[0]));
                                break;
                            case 1: //resourcetype
                                if (node.isCollection()) {
                                    xmlOut.setWhitespace(false);
                                    xmlOut.startElement(
                                        qns.fullName(AUTO_PROPS[1]), null);
                                    xmlOut.startElement(
                                        qns.fullName(DAV, "collection"),
                                        null,
                                        null,
                                        true);
                                    xmlOut.endElement(
                                        qns.fullName(AUTO_PROPS[1]), true);
                                    xmlOut.setWhitespace(true);
                                } else {
                                    xmlOut.startElement(
                                        qns.fullName(AUTO_PROPS[1]),
                                        null,
                                        null,
                                        true);
                                }
                                break;
                            case 2: //locks
                                writeLockSupport(xmlOut, qns);
                                break;

                            case 3: //active locks
                                xmlOut.startElement(
                                    qns.fullName(AUTO_PROPS[3]), null);
                                try {
                                    NodeLock locks[] = node.getLocks();
                                    for (NodeLock lock : locks) {
                                        writeActiveLock(lock, xmlOut, qns);
                                    }
                                } catch (AccessDeniedException e) {
                                    // eat it
                                }
                                xmlOut.endElement(qns.fullName(AUTO_PROPS[3]));
                                break;
                        }
                    }
                    break;

                case FIND_BY_PROPERTY:
                    if (props != null) {
                        Map allProps = props.getPropertyMap();
                        for (Object property : properties) {
                            QName qn = (QName) property;
                            if (allProps.containsKey(qn)) {
                                String val = (String) allProps.get(qn);
                                if (DATE_PROPS.contains(qn)) {
                                    val = PropertyHelper.httpDateString(
                                            PropertyHelper.parseDate(val));
                                }
                                //<xxx:yyy>
                                xmlOut.startElement(qns.fullName(qn), val);
                                //</xxx:yyy>
                                xmlOut.endElement(qns.fullName(qn));
                            }
                        }
                    }
                    for (int i=0; i < AUTO_PROPS.length; i++) {

                        if (!properties.contains(AUTO_PROPS[i])) continue;
                        switch (i) {
                            case 0: //displayname
                                xmlOut.startElement(
                                    qns.fullName(AUTO_PROPS[0]),
                                    UriHelper.name(node.getUri()));
                                xmlOut.endElement(
                                    qns.fullName(AUTO_PROPS[0]));
                                break;
                            case 1: //resourcetype
                                if (node.isCollection()) {
                                    xmlOut.setWhitespace(false);
                                    xmlOut.startElement(
                                        qns.fullName(AUTO_PROPS[1]), null);
                                    xmlOut.startElement(
                                        qns.fullName(DAV, "collection"),
                                        null,
                                        null,
                                        true);
                                    xmlOut.endElement(
                                        qns.fullName(AUTO_PROPS[1]), true);
                                     xmlOut.setWhitespace(true);
                                } else {
                                    xmlOut.startElement(
                                        qns.fullName(AUTO_PROPS[1]),
                                        null,
                                        null,
                                        true);
                                }
                                break;
                            case 2: //locks
                                writeLockSupport(xmlOut, qns);
                                break;
                            case 3: //active locks
                                try {
                                    NodeLock locks[] = node.getLocks();
                                    xmlOut.startElement(
                                        qns.fullName(AUTO_PROPS[3]), null);
                                    for (NodeLock lock : locks) {
                                        writeActiveLock(lock, xmlOut, qns);
                                    }
                                    xmlOut.endElement(
                                        qns.fullName(AUTO_PROPS[3]));
                                } catch (AccessDeniedException e) {
                                    // eat it
                                }
                                break;
                        }
                    }
                    break;

                case FIND_PROPERTY_NAMES:
                    for (QName qn : names) {
                        //<xxx:yyy/>
                        xmlOut.startElement(qns.fullName(qn), null, null, true);
                    }
                    for (QName aAUTO_PROPS : AUTO_PROPS) {
                        //<xxx:yyy/>
                        xmlOut.startElement(
                                qns.fullName(aAUTO_PROPS), null, null, true);
                    }
                    break;
            }

            // </d:prop>
            xmlOut.endElement(qns.fullName(DAV, "prop"), true);

            // send locked status on exclusive lock
            if (node.isExclusiveLocked(user)) {
                // <d:status>
                xmlOut.startElement(
                    qns.fullName(DAV, "status"),
                    "HTTP/1.1 " + WebdavStatus.SC_LOCKED + " " +
                    WebdavStatus.getStatusText(WebdavStatus.SC_LOCKED)
                    );
                // </d:status>
                xmlOut.endElement(qns.fullName(DAV, "status"));
            } else {
                // <d:status>
                xmlOut.startElement(
                    qns.fullName(DAV, "status"),
                    "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                    WebdavStatus.getStatusText(WebdavStatus.SC_OK)
                    );
                // </d:status>
                xmlOut.endElement(qns.fullName(DAV, "status"));
            }
        } catch (AccessDeniedException e) {

            // send no access status
            // <d:status>
            xmlOut.startElement(
                qns.fullName(DAV, "status"),
                "HTTP/1.1 " + WebdavStatus.SC_FORBIDDEN + " " +
                e.getMessage()
                );
            // </d:status>
            xmlOut.endElement(qns.fullName(DAV, "status"));
        }
        // </d:propstat>
        xmlOut.endElement(qns.fullName(DAV, "propstat"), true);
        // </d:response>
        xmlOut.endElement(qns.fullName(DAV, "response"), true);

        // recurse kids
        if (depth > 0 && node.isCollection()) {
            try {
                RepositoryNode kids[] = node.getChildren();
                for (RepositoryNode kid : kids) {
                    writePropeties(
                            xmlOut,
                            user,
                            baseHref,
                            qns,
                            (MutableRepositoryNode) kid,
                            type,
                            properties,
                            depth - 1);
                }
            } catch (AccessDeniedException e) {
                // skip kids without access
            }
        }
    }

    protected void doProppatch(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            AccessDeniedException,
            LockException {

        DocumentBuilder documentBuilder = getDocumentBuilder();
        ArrayList<Node> sets = new ArrayList<Node>();
        ArrayList<Node> removes = new ArrayList<Node>();
        HashMap<QName,String> statuses = new HashMap<QName,String>();
        try {
            int len = request.getContentLength();
            if (len == -1 || len > 3) {
                Document document = documentBuilder.parse
                    (new InputSource(request.getInputStream()));

                // Get the root element of the document
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (DAV.equals(currentNode.getNamespaceURI())) {
                            String name = currentNode.getLocalName();
                            NodeList subChilds = currentNode.getChildNodes();
                            for (int j=0; j < subChilds.getLength(); j++) {
                                Node subNd = subChilds.item(j);
                                if (subNd.getNodeType() == Node.ELEMENT_NODE
                                    && DAV.equals(subNd.getNamespaceURI())) {
                                    if (name.equals("set")) {
                                        sets.add(subNd);
                                    } else if (name.equals("remove")) {
                                        removes.add(subNd);
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            } else {
                response.sendError(WebdavStatus.SC_BAD_REQUEST);
                return;
            }
        } catch(Exception e) {
            response.sendError(WebdavStatus.SC_BAD_REQUEST);
            return;
        }
        PropertyHelper.QNamespaces qns = new PropertyHelper.QNamespaces();
        List autoProps = Arrays.asList(AUTO_PROPS);
        NodeProperties props = node.getProperties(new DefaultNodeVersion());
        for (Node set : sets) {
            NodeList childList = set.getChildNodes();
            for (int j = 0; j < childList.getLength(); j++) {
                Node currentNode = childList.item(j);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    QName propertyName =
                            new QName(
                                    currentNode.getNamespaceURI(),
                                    currentNode.getLocalName());
                    if (DEBUG) {
                        System.out.println(
                                "SET " + currentNode.getPrefix() + ":" +
                                        currentNode.getLocalName());
                    }
                    if (!qns.hasPrefix(currentNode.getNamespaceURI())) {
                        qns.addPrefix(
                                currentNode.getNamespaceURI(),
                                currentNode.getPrefix());
                    }
                    if (autoProps.contains(propertyName)) {

                        // conflict on autoProps
                        statuses.put(propertyName,
                                "HTTP/1.1 " + WebdavStatus.SC_CONFLICT + " " +
                                        WebdavStatus.getStatusText(
                                                WebdavStatus.SC_CONFLICT));
                    } else {
                        StringBuffer value = new StringBuffer();
                        NodeList valueList = currentNode.getChildNodes();
                        for (int k = 0; k < valueList.getLength(); k++) {
                            value.append(valueList.item(k).getNodeValue());
                        }
                        if (DATE_PROPS.contains(propertyName)) {
                            props.setValue(propertyName,
                                    PropertyHelper.dateString(
                                            PropertyHelper.parseDate(value.toString()))
                            );
                        } else {
                            props.setValue(propertyName, value.toString());
                        }
                        statuses.put(propertyName,
                                "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                                        WebdavStatus.getStatusText(WebdavStatus.SC_OK));
                    }
                }
            }
        }

        for (Node remove : removes) {
            NodeList childList = remove.getChildNodes();
            for (int j = 0; j < childList.getLength(); j++) {
                Node currentNode = childList.item(j);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    QName propertyName =
                            new QName(
                                    currentNode.getNamespaceURI(),
                                    currentNode.getLocalName());
                    if (DEBUG) {
                        System.out.println(
                                "REMOVE " + currentNode.getPrefix() + ":" +
                                        currentNode.getLocalName());
                    }
                    if (!qns.hasPrefix(currentNode.getNamespaceURI())) {
                        qns.addPrefix(
                                currentNode.getNamespaceURI(),
                                currentNode.getPrefix());
                    }
                    if (autoProps.contains(propertyName)) {

                        // conflict on autoProps
                        statuses.put(propertyName,
                                "HTTP/1.1 " + WebdavStatus.SC_CONFLICT + " " +
                                        WebdavStatus.getStatusText(
                                                WebdavStatus.SC_CONFLICT));
                    } else {
                        props.remove(propertyName);
                        statuses.put(propertyName,
                                "HTTP/1.1 " + WebdavStatus.SC_OK + " " +
                                        WebdavStatus.getStatusText(WebdavStatus.SC_OK));
                    }
                }
            }
        }

        node.setProperties(props);

        if (!statuses.isEmpty()) {

            // send response
            response.setStatus(WebdavStatus.SC_MULTI_STATUS);
            response.setContentType("text/xml; charset=UTF-8");

            XMLWriter xmlOut;
            StringWriter xBuf = new StringWriter();
            xmlOut = new XMLWriter(xBuf);

            xmlOut.writeXMLHeader();
            String attribs[][] = {
                {"xmlns:" + PropertyHelper.DEFAULT_PREFIX, DAV}};

            // <d:multistatus xmlns...>
            xmlOut.startElement(
                qns.fullName(DAV, "multistatus"), attribs, null, false);
            // <d:response>
            xmlOut.startElement(qns.fullName(DAV, "response"), null);
            for (Map.Entry<QName, String> ent : statuses.entrySet()) {
                // <d:propstat>
                xmlOut.startElement(qns.fullName(DAV, "propstat"), null);
                // <d:prop><xxx:yyy/></d:prop>
                xmlOut.startElement(qns.fullName(DAV, "prop"), null);
                xmlOut.startElement(
                        qns.fullName(ent.getKey()), null, null, true);
                xmlOut.endElement(qns.fullName(DAV, "prop"), true);
                // <d:status>HTTP xxxx</d:status>
                xmlOut.startElement(
                        qns.fullName(DAV, "status"), ent.getValue());
                xmlOut.endElement(qns.fullName(DAV, "status"));
                // </d:propstat>
                xmlOut.endElement(qns.fullName(DAV, "propstat"));
            }
            // </d:response>
            xmlOut.endElement(qns.fullName(DAV, "response"));
            // </d:multistatus>
            xmlOut.endElement(qns.fullName(DAV, "multistatus"), true);
            xmlOut.endDocument();
            String xStr = xBuf.toString();
            if (DEBUG) {
                System.out.println(xStr);
            }
            byte xByt[] = xStr.getBytes("UTF-8");
            response.setContentLength(xByt.length);
            response.getOutputStream().write(xByt);
        }
    }

    protected void doCopy(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            NotFoundException,
            AccessDeniedException,
            LockException,
            DuplicateException {


        String dstUri = getDestination(request);
        if (dstUri == null) {
            response.sendError(WebdavStatus.SC_BAD_REQUEST);
            return;
        }
        node.copy(dstUri, true);
        response.setStatus(WebdavStatus.SC_CREATED);
    }

    protected void doMove(
        HttpServletRequest request,
        HttpServletResponse response,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            NotFoundException,
            AccessDeniedException,
            LockException,
            DuplicateException {

        String dstUri = getDestination(request);
        //System.out.println(dstUri);
        if (dstUri == null) {
            response.sendError(WebdavStatus.SC_BAD_REQUEST);
            return;
        }
        node.move(dstUri);
        response.setStatus(WebdavStatus.SC_CREATED);
    }

    protected void doMkcol(
        HttpServletRequest request,
        HttpServletResponse response,
        String uri)
        throws
            ServletException,
            IOException,
            NotFoundException,
            AccessDeniedException,
            LockException,
            DuplicateException {

        Repository repos = RepositoryHelper.getRepository(uri);
        repos.createNode(uri, true, (User)request.getUserPrincipal());
        response.setStatus(WebdavStatus.SC_CREATED);
    }


    protected void doPut(
        HttpServletRequest request,
        HttpServletResponse response,
        String uri,
        MutableRepositoryNode node)
        throws
            ServletException,
            IOException,
            NotFoundException,
            AccessDeniedException,
            LockException,
            DuplicateException {

        int status = WebdavStatus.SC_OK;

        // try no node create
        if (node == null) {
            Repository repos = RepositoryHelper.getRepository(uri);
            node = (MutableRepositoryNode)
                repos.createNode(uri, false, (User)request.getUserPrincipal());
            status = WebdavStatus.SC_CREATED;
        }
        int size = request.getContentLength();
        String mime = request.getContentType();

        // check on application/octet-stream since MS always sends this
        if (Check.isEmpty(mime) || "application/octet-stream".equals(mime)) {
            mime = getServletContext().getMimeType(uri);
            if (Check.isEmpty(mime)) {
                mime = "application/octet-stream";
            }
        } else if ("text/xml".equals(mime) || "application/xml".equals(mime)) {
            mime = getServletContext().getMimeType(uri);
            if (Check.isEmpty(mime)) {
                mime = "text/xml";
            }
        }
        RepositoryHelper.createContent(
            node, request.getInputStream(), size, mime);

        NodeProperties props = node.getProperties(new DefaultNodeVersion());
        response.setHeader("ETag", props.getValue("getetag"));
        response.setStatus(status);
        response.setContentLength(0);
    }


    protected String getDestination(HttpServletRequest req) {

        String destinationPath = req.getHeader("Destination");
        if (destinationPath == null) {
            return null;
        }

        // Remove url encoding from destination
        try {
            destinationPath = URLDecoder.decode(destinationPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        int protocolIndex = destinationPath.indexOf("://");
        if (protocolIndex >= 0) {
            // if the Destination URL contains the protocol, we can safely
            // trim everything upto the first "/" character after "://"
            int firstSeparator =
                destinationPath.indexOf("/", protocolIndex + 4);
            if (firstSeparator < 0) {
                destinationPath = "/";
            } else {
                destinationPath = destinationPath.substring(firstSeparator);
            }
        } else {
            String hostName = req.getServerName();
            if ((hostName != null) && (destinationPath.startsWith(hostName))) {
                destinationPath = destinationPath.substring(hostName.length());
            }

            int portIndex = destinationPath.indexOf(":");
            if (portIndex >= 0) {
                destinationPath = destinationPath.substring(portIndex);
            }

            if (destinationPath.startsWith(":")) {
                int firstSeparator = destinationPath.indexOf("/");
                if (firstSeparator < 0) {
                    destinationPath = "/";
                } else {
                    destinationPath =
                        destinationPath.substring(firstSeparator);
                }
            }
        }

        // Normalise destination path (remove '.' and '..')
        destinationPath = normalize(destinationPath);

        String contextPath = req.getContextPath();
        if ((contextPath != null) &&
            (destinationPath.startsWith(contextPath))) {
            destinationPath = destinationPath.substring(contextPath.length());
        }
        String servletPath = req.getServletPath();
        if ((servletPath != null) &&
            (destinationPath.startsWith(servletPath))) {
            destinationPath = destinationPath.substring(servletPath.length());
        }
        return destinationPath;
    }

    protected void writeActiveLock(
        NodeLock lock,
        XMLWriter xmlOut,
        PropertyHelper.QNamespaces qns) throws IOException {

        // <D:activelock>
        xmlOut.startElement(qns.fullName(DAV, "activelock"), null);
        xmlOut.setWhitespace(false);
        //    <D:locktype><D:write/></D:locktype>
        xmlOut.startElement(qns.fullName(DAV, "locktype"), null);
        xmlOut.startElement(qns.fullName(DAV, "write"), null, null, true);
        xmlOut.endElement(qns.fullName(DAV, "locktype"));

        //     <D:lockscope><D:exclusive/></D:lockscope>
        xmlOut.startElement(qns.fullName(DAV, "lockscope"), null);
        if (lock.isExclusive()) {
            xmlOut.startElement(
                qns.fullName(DAV, "exclusive"), null, null, true);
        } else {
            xmlOut.startElement(
                qns.fullName(DAV, "shared"), null, null, true);
        }
        xmlOut.endElement(qns.fullName(DAV, "lockscope"));
        xmlOut.setWhitespace(true);
        //     <D:depth>0</D:depth>
        xmlOut.startElement(qns.fullName(DAV, "depth"),
            lock.isInheritable() ? "Infinity" : "0" );
        xmlOut.endElement(qns.fullName(DAV, "depth"));
        //     <D:owner>Jane Smith</D:owner>
        xmlOut.startElement(qns.fullName(DAV, "owner"), "");
        xmlOut.rawXML(lock.getOwnerInfo());
        xmlOut.endElement(qns.fullName(DAV, "owner"));
        //     <D:timeout>Second-604800</D:timeout>
        xmlOut.startElement(qns.fullName(DAV, "timeout"),
            "Second-" +
            ((lock.getExpireTime().getTime() - System.currentTimeMillis())
                / 1000)
            );
        xmlOut.endElement(qns.fullName(DAV, "timeout"));
        //     <D:locktoken>
        xmlOut.startElement(qns.fullName(DAV, "locktoken"), null);
        //          <D:href>
        //          opaquelocktoken:f81de2ad-7f3d-a1b2-4f3c-00a0c91a9d76
        xmlOut.startElement(qns.fullName(DAV, "href"),
            "opaquelocktoken:" + new IUID(lock.getLockId()).toUUID());
        //          </D:href>
        xmlOut.endElement(qns.fullName(DAV, "href"));
        //     </D:locktoken>
        xmlOut.endElement(qns.fullName(DAV, "locktoken"));
        // </D:activelock>
        xmlOut.endElement(qns.fullName(DAV, "activelock"));
    }

    protected void writeLockSupport(
        XMLWriter xmlOut, PropertyHelper.QNamespaces qns) throws IOException {

        // <D:supportedlock>
        xmlOut.startElement(qns.fullName(DAV, "supportedlock"), null);
            // <D:lockentry>
            xmlOut.startElement(qns.fullName(DAV, "lockentry"), null);
                // <D:lockscope><D:exclusive/></D:lockscope>
                xmlOut.setWhitespace(false);
                xmlOut.startElement(qns.fullName(DAV, "lockscope"), null);
                xmlOut.startElement(
                    qns.fullName(DAV, "exclusive"), null, null, true);
                xmlOut.endElement(qns.fullName(DAV, "lockscope"));
                // <D:locktype><D:write/></D:locktype>
                xmlOut.startElement(qns.fullName(DAV, "locktype"), null);
                xmlOut.startElement(
                    qns.fullName(DAV, "write"), null, null, true);
                xmlOut.endElement(qns.fullName(DAV, "locktype"));
                xmlOut.setWhitespace(true);
            // </D:lockentry>
            xmlOut.endElement(qns.fullName(DAV, "lockentry"));
            // <D:lockentry>
            xmlOut.startElement(qns.fullName(DAV, "lockentry"), null);
                xmlOut.setWhitespace(false);
                // <D:lockscope><D:shared/></D:lockscope>
                xmlOut.startElement(qns.fullName(DAV, "lockscope"), null);
                xmlOut.startElement(
                    qns.fullName(DAV, "shared"), null, null, true);
                xmlOut.endElement(qns.fullName(DAV, "lockscope"));
                // <D:locktype><D:write/></D:locktype>
                xmlOut.startElement(qns.fullName(DAV, "locktype"), null);
                xmlOut.startElement(
                    qns.fullName(DAV, "write"), null, null, true);
                xmlOut.endElement(qns.fullName(DAV, "locktype"));
               xmlOut.setWhitespace(true);
            // </D:lockentry>
            xmlOut.endElement(qns.fullName(DAV, "lockentry"));
        // </D:supportedlock>
        xmlOut.endElement(qns.fullName(DAV, "supportedlock"));
    }

    protected void writeDOM(
        Node node, XMLWriter xmlOut, PropertyHelper.QNamespaces qns)
        throws IOException {

        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                xmlOut.text(node.getNodeValue());
                break;
            case Node.ELEMENT_NODE:
                String nodeName;
                if (DAV.equals(node.getNamespaceURI())) {
                    nodeName = qns.fullName(DAV, node.getLocalName());
                    xmlOut.startElement(nodeName, "");
                } else {
                    nodeName = node.getLocalName();
                    String attribs[][] = {{"xmlns", node.getNamespaceURI()}};
                    xmlOut.startElement(nodeName, attribs, "", false);
                }
                NodeList childList = node.getChildNodes();
                for (int i=0; i < childList.getLength(); i++) {
                    writeDOM(childList.item(i), xmlOut, qns);
                }
                xmlOut.endElement(nodeName);
                break;
        }
    }

    /**
     * Return a context-relative path, beginning with a "/", that represents
     * the canonical version of the specified path after ".." and "." elements
     * are resolved out.  If the specified path attempts to go outside the
     * boundaries of the current context (i.e. too many ".." path elements
     * are present), return <code>null</code> instead.
     *
     * @param path Path to be normalized
     */
    protected String normalize(String path) {

        if (path == null)
            return null;

        // Create a place for the normalized path
        String normalized = path;

        if (normalized.equals("/."))
            return "/";

        // Normalize the slashes and add leading slash if necessary
        if (normalized.indexOf('\\') >= 0)
            normalized = normalized.replace('\\', '/');
        if (!normalized.startsWith("/"))
            normalized = "/" + normalized;

        // Resolve occurrences of "//" in the normalized path
        while (true) {
            int index = normalized.indexOf("//");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) +
                normalized.substring(index + 1);
        }

        // Resolve occurrences of "/./" in the normalized path
        while (true) {
            int index = normalized.indexOf("/./");
            if (index < 0)
                break;
            normalized = normalized.substring(0, index) +
                normalized.substring(index + 2);
        }

        // Resolve occurrences of "/../" in the normalized path
        while (true) {
            int index = normalized.indexOf("/../");
            if (index < 0)
                break;
            if (index == 0)
                return (null);  // Trying to go outside our context
            int index2 = normalized.lastIndexOf('/', index - 1);
            normalized = normalized.substring(0, index2) +
                normalized.substring(index + 3);
        }

        // Return the normalized path that we have completed
        return (normalized);
    }


    /*
     * @see itensil.web.MethodServlet#error(
     *  HttpServletRequest, HttpServletResponse, Throwable)
     */
    public void error(
        HttpServletRequest request,
        HttpServletResponse response,
        Throwable t)
        throws IOException {

        RepositoryHelper.rollbackTransaction();
        ServletUtil.noCache(response);

        if (t instanceof LockException) {
             response.sendError(
                 WebdavStatus.SC_LOCKED, t.getMessage());
        } else if (t instanceof AccessDeniedException) {
            response.sendError(
                WebdavStatus.SC_FORBIDDEN, t.getMessage());
        } else if (t instanceof NotFoundException) {
            response.sendError(
                WebdavStatus.SC_NOT_FOUND, t.getMessage());
        } else if (t instanceof DuplicateException) {
            response.sendError(
                WebdavStatus.SC_CONFLICT, t.getMessage());
        } else {
            response.sendError(
                WebdavStatus.SC_INTERNAL_SERVER_ERROR, t.getMessage());
        }
    }

    /**
     * Return JAXP document builder instance.
     */
    protected DocumentBuilder getDocumentBuilder() throws ServletException {
        try {
        	return documentBuilderFactory.newDocumentBuilder();
        } catch(ParserConfigurationException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Check and set content modified HTTP headers
     * @param props
     * @param request
     * @param response
     * @return true if NOT modified
     */
    public static boolean checkNodeNotModified(
            NodeProperties props,
            HttpServletRequest request,
            HttpServletResponse response) {

        String ifNoneMatchETags = request.getHeader("If-None-Match");
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");

        // get node props
        String etag = props.getValue("getetag");
        Date lastModified = PropertyHelper.parseDate(props.getValue("getlastmodified"));
        Date lastModResp = (lastModified == null) ? new Date() : lastModified;

        // set headers
        if (!Check.isEmpty(etag)) response.setHeader("ETag", etag);
        response.addDateHeader("Last-Modified", lastModResp.getTime());

        // test values
        if (!Check.isEmpty(ifNoneMatchETags) && !Check.isEmpty(etag)) {
            return etag.equals(ifNoneMatchETags);
        } else if (lastModified != null) {
            return ifModifiedSince > 0 && lastModified.getTime() <= (ifModifiedSince + 1000);
        }
        return false;
    }


    /**
     * Wraps the HttpServletResponse class to abstract the
     * specific protocol used.  To support other protocols
     * we would only need to modify this class and the
     * WebDavRetCode classes.
     *
     * @author              Marc Eaddy
     * @version             1.0, 16 Nov 1997
     */
    static class WebdavStatus {


        // -------------------------------------------------- Instance Variables


        /**
         * This Hashtable contains the mapping of HTTP and WebDAV
         * status codes to descriptive text.  This is a static
         * variable.
         */
        private static Hashtable<Integer,String> mapStatusCodes = new Hashtable<Integer,String>();


        // --------------------------------------------------- HTTP Status Codes


        /**
         * Status code (200) indicating the request succeeded normally.
         */
        public static final int SC_OK = HttpServletResponse.SC_OK;


        /**
         * Status code (201) indicating the request succeeded and created
         * a new resource on the server.
         */
        public static final int SC_CREATED = HttpServletResponse.SC_CREATED;


        /**
         * Status code (202) indicating that a request was accepted for
         * processing, but was not completed.
         */
        public static final int SC_ACCEPTED = HttpServletResponse.SC_ACCEPTED;


        /**
         * Status code (204) indicating that the request succeeded but that
         * there was no new information to return.
         */
        public static final int SC_NO_CONTENT =
            HttpServletResponse.SC_NO_CONTENT;


        /**
         * Status code (301) indicating that the resource has permanently
         * moved to a new location, and that future references should use a
         * new URI with their requests.
         */
        public static final int SC_MOVED_PERMANENTLY =
            HttpServletResponse.SC_MOVED_PERMANENTLY;


        /**
         * Status code (302) indicating that the resource has temporarily
         * moved to another location, but that future references should
         * still use the original URI to access the resource.
         */
        public static final int SC_MOVED_TEMPORARILY =
            HttpServletResponse.SC_MOVED_TEMPORARILY;


        /**
         * Status code (304) indicating that a conditional GET operation
         * found that the resource was available and not modified.
         */
        public static final int SC_NOT_MODIFIED =
            HttpServletResponse.SC_NOT_MODIFIED;


        /**
         * Status code (400) indicating the request sent by the client was
         * syntactically incorrect.
         */
        public static final int SC_BAD_REQUEST =
            HttpServletResponse.SC_BAD_REQUEST;


        /**
         * Status code (401) indicating that the request requires HTTP
         * authentication.
         */
        public static final int SC_UNAUTHORIZED =
            HttpServletResponse.SC_UNAUTHORIZED;


        /**
         * Status code (403) indicating the server understood the request
         * but refused to fulfill it.
         */
        public static final int SC_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;


        /**
         * Status code (404) indicating that the requested resource is not
         * available.
         */
        public static final int SC_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;


        /**
         * Status code (500) indicating an error inside the HTTP service
         * which prevented it from fulfilling the request.
         */
        public static final int SC_INTERNAL_SERVER_ERROR =
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR;


        /**
         * Status code (501) indicating the HTTP service does not support
         * the functionality needed to fulfill the request.
         */
        public static final int SC_NOT_IMPLEMENTED =
            HttpServletResponse.SC_NOT_IMPLEMENTED;


        /**
         * Status code (502) indicating that the HTTP server received an
         * invalid response from a server it consulted when acting as a
         * proxy or gateway.
         */
        public static final int SC_BAD_GATEWAY =
            HttpServletResponse.SC_BAD_GATEWAY;


        /**
         * Status code (503) indicating that the HTTP service is
         * temporarily overloaded, and unable to handle the request.
         */
        public static final int SC_SERVICE_UNAVAILABLE =
            HttpServletResponse.SC_SERVICE_UNAVAILABLE;


        /**
         * Status code (100) indicating the client may continue with
         * its request.  This interim response is used to inform the
         * client that the initial part of the request has been
         * received and has not yet been rejected by the server.
         */
        public static final int SC_CONTINUE = 100;


        /**
         * Status code (405) indicating the method specified is not
         * allowed for the resource.
         */
        public static final int SC_METHOD_NOT_ALLOWED = 405;


        /**
         * Status code (409) indicating that the request could not be
         * completed due to a conflict with the current state of the
         * resource.
         */
        public static final int SC_CONFLICT = 409;


        /**
         * Status code (412) indicating the precondition given in one
         * or more of the request-header fields evaluated to false
         * when it was tested on the server.
         */
        public static final int SC_PRECONDITION_FAILED = 412;


        /**
         * Status code (413) indicating the server is refusing to
         * process a request because the request entity is larger
         * than the server is willing or able to process.
         */
        public static final int SC_REQUEST_TOO_LONG = 413;


        /**
         * Status code (415) indicating the server is refusing to service
         * the request because the entity of the request is in a format
         * not supported by the requested resource for the requested
         * method.
         */
        public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;


        // ----------------------------------------- Extended WebDav status code


        /**
         * Status code (207) indicating that the response requires
         * providing status for multiple independent operations.
         */
        public static final int SC_MULTI_STATUS = 207;
        // This one colides with HTTP 1.1
        // "207 Parital Update OK"


        /**
         * Status code (418) indicating the entity body submitted with
         * the PATCH method was not understood by the resource.
         */
        public static final int SC_UNPROCESSABLE_ENTITY = 418;
        // This one colides with HTTP 1.1
        // "418 Reauthentication Required"


        /**
         * Status code (419) indicating that the resource does not have
         * sufficient space to record the state of the resource after the
         * execution of this method.
         */
        public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
        // This one colides with HTTP 1.1
        // "419 Proxy Reauthentication Required"


        /**
         * Status code (420) indicating the method was not executed on
         * a particular resource within its scope because some part of
         * the method's execution failed causing the entire method to be
         * aborted.
         */
        public static final int SC_METHOD_FAILURE = 420;


        /**
         * Status code (423) indicating the destination resource of a
         * method is locked, and either the request did not contain a
         * valid Lock-Info header, or the Lock-Info header identifies
         * a lock held by another principal.
         */
        public static final int SC_LOCKED = 423;


        // --------------------------------------------------------- Initializer


        static {
            // HTTP 1.0 tatus Code
            addStatusCodeMap(SC_OK, "OK");
            addStatusCodeMap(SC_CREATED, "Created");
            addStatusCodeMap(SC_ACCEPTED, "Accepted");
            addStatusCodeMap(SC_NO_CONTENT, "No Content");
            addStatusCodeMap(SC_MOVED_PERMANENTLY, "Moved Permanently");
            addStatusCodeMap(SC_MOVED_TEMPORARILY, "Moved Temporarily");
            addStatusCodeMap(SC_NOT_MODIFIED, "Not Modified");
            addStatusCodeMap(SC_BAD_REQUEST, "Bad Request");
            addStatusCodeMap(SC_UNAUTHORIZED, "Unauthorized");
            addStatusCodeMap(SC_FORBIDDEN, "Forbidden");
            addStatusCodeMap(SC_NOT_FOUND, "Not Found");
            addStatusCodeMap(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
            addStatusCodeMap(SC_NOT_IMPLEMENTED, "Not Implemented");
            addStatusCodeMap(SC_BAD_GATEWAY, "Bad Gateway");
            addStatusCodeMap(SC_SERVICE_UNAVAILABLE, "Service Unavailable");
            addStatusCodeMap(SC_CONTINUE, "Continue");
            addStatusCodeMap(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
            addStatusCodeMap(SC_CONFLICT, "Conflict");
            addStatusCodeMap(SC_PRECONDITION_FAILED, "Precondition Failed");
            addStatusCodeMap(SC_REQUEST_TOO_LONG, "Request Too Long");
            addStatusCodeMap(SC_UNSUPPORTED_MEDIA_TYPE,
                             "Unsupported Media Type");
            // WebDav Status Codes
            addStatusCodeMap(SC_MULTI_STATUS, "Multi-Status");
            addStatusCodeMap(SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity");
            addStatusCodeMap(SC_INSUFFICIENT_SPACE_ON_RESOURCE,
                             "Insufficient Space On Resource");
            addStatusCodeMap(SC_METHOD_FAILURE, "Method Failure");
            addStatusCodeMap(SC_LOCKED, "Locked");
        }


        // ------------------------------------------------------ Public Methods


        /**
         * Returns the HTTP status text for the HTTP or WebDav status code
         * specified by looking it up in the static mapping.  This is a
         * static function.
         *
         * @param   nHttpStatusCode [IN] HTTP or WebDAV status code
         * @return  A string with a short descriptive phrase for the
         *                  HTTP status code (e.g., "OK").
         */
        public static String getStatusText(int nHttpStatusCode) {
            if (!mapStatusCodes.containsKey(nHttpStatusCode)) {
                return "";
            } else {
                return mapStatusCodes.get(nHttpStatusCode);
            }
        }


        // ----------------------------------------------------- Private Methods


        /**
         * Adds a new status code -> status text mapping.  This is a static
         * method because the mapping is a static variable.
         *
         * @param   nKey    [IN] HTTP or WebDAV status code
         * @param   strVal  [IN] HTTP status text
         */
        private static void addStatusCodeMap(int nKey, String strVal) {
            mapStatusCodes.put(nKey, strVal);
        }

    }

}
