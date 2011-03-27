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
package itensil.workflow.model;

import itensil.web.MethodServlet;
import itensil.web.ContentType;
import itensil.util.Check;
import itensil.io.StreamUtil;
import itensil.io.xml.XMLDocument;
import itensil.workflow.model.legacy.LegacySupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ModelingServlet extends MethodServlet {

    /**
     *  /getModel
     *
     * Load a flow model
     *
     *  Parameters:
     *      uri = model uri
     *
     *  Output:
     *      Model XML + URI Meta XML
     *
     */
    @ContentType("text/xml")
    public void webGetModel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        if (Check.isEmpty(uri)) {

            // send the start
            StreamUtil.copyStream(ModelingServlet.class.getResourceAsStream("starter.xml"), response.getOutputStream());
            return;
        }

        // TODO app-specific loading...
        Document doc = XMLDocument.readStream(ModelingServlet.class.getResourceAsStream("starter.xml"));

        if (LegacySupport.isLegacyModel(doc)) doc = LegacySupport.upgrade(doc);

        // add meta
        // Element meta = 
        	doc.getRootElement().addElement("meta");
        // TODO app-specific meta...

        doc.write(response.getWriter());
    }

    /**
     *  /setModel
     *
     *  Takes an inputstream of an Flow XML Model + URI Meta XML
     *
     *  Output:
     *      Status Message
     *
     */
    @ContentType("text/xml")
    public void webSetModel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document doc = XMLDocument.readStream(request.getInputStream());
        if (LegacySupport.isLegacyModel(doc)) doc = LegacySupport.upgrade(doc);

        // filter meta
        Element root = doc.getRootElement();
        Element meta = root.element("meta");
        if (meta != null) {
            root.remove(meta);
        }

        // TODO app-specific saving...
    }

    /**
     *  /legacyModel
     *
     *  Takes an inputstream of an Flow XML Model + URI Meta XML
     *
     *  Output:
     *      Status Message
     *
     */
    @ContentType("text/xml")
    public void webLegacyModel(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Document doc = XMLDocument.readStream(request.getInputStream());
        if (LegacySupport.isLegacyModel(doc)) doc = LegacySupport.upgrade(doc);
        doc.write(response.getWriter());
    }

}
