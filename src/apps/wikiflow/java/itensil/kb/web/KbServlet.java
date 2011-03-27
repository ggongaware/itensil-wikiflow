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
package itensil.kb.web;

import itensil.web.MethodServlet;
import itensil.web.ContentType;
import itensil.web.ServletUtil;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.repository.*;
import itensil.io.xml.XMLDocument;
import itensil.security.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.List;
import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class KbServlet extends MethodServlet {

    /**
     *  /page
     *
     * Direct page - article popup
     *
     */
    public void webArticlepage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletUtil.forward("/view-wf/art.jsp", request, response);
    }
    
    
    /**
     *  /page
     *
     * Direct page - kb popup
     *
     */
    public void webPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletUtil.forward("/view-wf/kb.jsp", request, response);
    }
    
    
    /**
     *  /listByRef
     *
     *  Output:
     *      article header list
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
	public void webListByRef(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	  String uri = request.getParameter("uri");
    	  String refId = request.getParameter("refId");
          if (!Check.isEmpty(uri) && !Check.isEmpty(refId)) {
        	  	RepositoryHelper.beginTransaction();
        	  	RepositoryHelper.useReadOnly();
          		uri = RepositoryHelper.resolveUri(uri);
          		Document doc = XMLDocument.readStream(
          				RepositoryHelper.loadContent(uri));
          		Document resDoc = DocumentHelper.createDocument();
          		Element resRoot = resDoc.addElement("articles");
          		for (Element elem : (List<Element>)doc.getRootElement().elements("article")) {
          			if (refId.equals(elem.attributeValue("refId"))) {
          				// just the node, no content
          				elem.setText("");
          				resRoot.add(elem.createCopy());
          			}
          		}
          		RepositoryHelper.commitTransaction();
          		if (resRoot.hasContent())
          			ServletUtil.cacheTimeout(response, 27);
          		else
          			ServletUtil.noCache(response);
          		resDoc.write(response.getWriter());
          } else {
              throw new NotFoundException("[blank]");
          }
    }

    /**
     *  /save
     *
     *  Output:
     *      Wiki save XML
     *
     */
    @SuppressWarnings("unchecked")
	@ContentType("text/xml")
    public void webSave(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uri = request.getParameter("uri");
        if (!Check.isEmpty(uri)) {
            Document doc = XMLDocument.readStream(request.getInputStream());
            Element reqRoot = doc.getRootElement();

            if ("kb-save".equals(reqRoot.getName())) {
                List<Element> arts;
                boolean hasMods = false;

                RepositoryHelper.beginTransaction();
                uri = RepositoryHelper.resolveUri(uri);
                MutableRepositoryNode node = (MutableRepositoryNode)RepositoryHelper.getNode(uri, true);
                String verStr = request.getParameter("version");
                NodeVersion version = Check.isEmpty(verStr) ? new DefaultNodeVersion() : new DefaultNodeVersion(verStr, false);
                NodeContent content = node.getContent(version);

                Document kbDoc = XMLDocument.readStream(content.getStream());
                Element kbRoot = kbDoc.getRootElement();

                // creates
                arts = reqRoot.elements("create");
                for (Element reqArt : arts) {
                    Element kbArt = kbRoot.addElement("article");
                    kbArt.addAttribute("id", reqArt.attributeValue("id"));
                    kbArt.addAttribute("refId", reqArt.attributeValue("refId"));
                    kbArt.addAttribute("categories", reqArt.attributeValue("categories"));
                    kbArt.addAttribute("createTime", reqArt.attributeValue("createTime"));
                    kbArt.addAttribute("createBy", reqArt.attributeValue("createBy"));
                    kbArt.addAttribute("modifyTime", reqArt.attributeValue("modifyTime"));
                    kbArt.addAttribute("modifyBy", reqArt.attributeValue("modifyBy"));
                    kbArt.addAttribute("layout", reqArt.attributeValue("layout"));
                    kbArt.addText(reqArt.getText());
                }

                // modifies
                arts = reqRoot.elements("modify");
                hasMods = arts.size() > 0;
                for (Element reqArt : arts) {
                    Element kbArt = getElementById(kbRoot, "article", reqArt.attributeValue("origId"));
                    if (kbArt == null) { // try to recover client-proclaimed failed creates
                        kbArt = kbRoot.addElement("article");
                        kbArt.addAttribute("createTime", reqArt.attributeValue("createTime"));
                        kbArt.addAttribute("createBy", reqArt.attributeValue("createBy"));
                    }
                    kbArt.addAttribute("id", reqArt.attributeValue("id"));
                    kbArt.addAttribute("refId", reqArt.attributeValue("refId"));
                    kbArt.addAttribute("categories", reqArt.attributeValue("categories"));
                    kbArt.addAttribute("modifyTime", reqArt.attributeValue("modifyTime"));
                    kbArt.addAttribute("modifyBy", reqArt.attributeValue("modifyBy"));
                    kbArt.addAttribute("layout", reqArt.attributeValue("layout"));
                    kbArt.setText(reqArt.getText());
                }

                // deletes
                arts = reqRoot.elements("delete");
                hasMods = hasMods || arts.size() > 0;
                for (Element reqArt : arts) {
                    Element kbArt = getElementById(kbRoot, "article", reqArt.attributeValue("id"));
                    if (kbArt != null) {
                        kbRoot.remove(kbArt);
                    }
                }

                NodeProperties props = node.getProperties(version);
                byte buf[] = kbDoc.asXML().getBytes("UTF-8");
                if (hasMods) { // no version on creates
                	if (props != null) version = props.getVersion();
                    version = RepositoryHelper.nextVersion(node, version, version.isDefault());
                }
                if (props != null) {
                    if (hasMods) {
                        props = new DefaultNodeProperties(version, props.getPropertyMap());
                    } else {
                        version = props.getVersion();
                    }
                    String modDate = PropertyHelper.dateString(new Date());
                    props.setValue("getlastmodified", modDate);
                    props.setValue("getcontentlength", String.valueOf(buf.length));
                    props.setValue(PropertyHelper.itensilQName("modifier"),
                            ((User)request.getUserPrincipal()).getUserId());
                    props.setValue("getetag",
                            PropertyHelper.makeEtag(node.getUri(), version.getNumber(), modDate, buf.length));
                    node.setProperties(props);
                }
                NodeContent modContent = new DefaultNodeContent(buf, version);
                node.setContent(modContent);
                RepositoryHelper.commitTransaction();
            }

        } else {
            throw new NotFoundException("[blank]");
        }
        response.getWriter().print("<ok/>");
    }

    @SuppressWarnings("unchecked")
	protected Element getElementById(Element kbRoot, String name, String id) {
        for (Element elem : (List<Element>)kbRoot.elements(name)) {
            if (id.equals(elem.attributeValue("id"))) return elem;
        }
        return null;
    }
    
    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
    	RepositoryHelper.rollbackTransaction();
    }

    /**
     * Called after a matched method
     */
    public void afterMethod() {
        RepositoryHelper.closeSession();
    }
}
