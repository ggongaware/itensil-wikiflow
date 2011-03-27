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
 * Created on Dec 30, 2003
 *
 */
package itensil.web;

import itensil.util.Check;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.apache.log4j.Logger;

/**
 * @author ggongaware@itensil.com
 *
 * Maps sub class methods prefixed with <code>web</code>
 * to URL handlers. Methods must have
 * <code>(HttpServletRequest, HttpServletResponse)</code> arguments.
 *
 */
public abstract class MethodServlet extends HttpServlet {

    protected static Logger logger = Logger.getLogger(MethodServlet.class);

    protected HashMap<String, Method> webMethods;

    /*
     * @see HttpServlet#service(HttpServletRequest, HttpServletResponse)
     */
    protected void service(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        // The MEAT
        performMethod(request, response);
    }

    /**
     * Method-path mapper
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void performMethod(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

        String methodStr = request.getPathInfo();
        if (Check.isEmpty(methodStr)) {
            ServletUtil.bounce(
                ServletUtil.getServletPath(request) + "/",
                request.getQueryString(),
                response);
            return;
        }
        Method meth = webMethods.get(methodStr);
        try {
            beforeMethod();
            if (meth != null) {
                ContentType contType = meth.getAnnotation(ContentType.class);
                if (contType != null) {
                    response.setContentType(contType.value());
                    response.setCharacterEncoding(contType.encode());
                }
                Object [] args = {request, response};
                meth.invoke(this, args);
            } else {
                defaultWebMethod(request, response);
            }
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getTargetException();
            if (t instanceof ServletException) {
                throw (ServletException)t;
            } else if (t instanceof IOException) {
                throw (IOException)t;
            } else {
                error(request, response, t, meth);
            }
            methodException(t);
        } catch (IllegalAccessException iacce) {
            throw new ServletException(iacce);
        } catch (IllegalArgumentException iarge) {
            throw new ServletException(iarge);
        } finally {
            afterMethod();
        }
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

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }


    /**
     * Error handler, probably want to override this
     *
     * @param request
     * @param response
     * @param th
     * @param meth
     * @throws IOException
     */
    protected void error(
            HttpServletRequest request,
            HttpServletResponse response,
            Throwable th,
            Method meth) throws ServletException, IOException {

        if (th instanceof RuntimeException) {
            logger.error(th);
        } else {
            logger.info(th);
        }

        if (meth != null) {
             ContentType contType = meth.getAnnotation(ContentType.class);
             if (contType != null && (contType.value().indexOf("xml") >= 0)) {
                errorXML(request, response, th);
                return;
             }
        }
        throw new ServletException(th);
    }

    protected void errorXML(HttpServletRequest request, HttpServletResponse response, Throwable th)
            throws IOException {

        Document doc = DocumentHelper.createDocument();
        Element error = doc.addElement("error");
        error.addAttribute("exception", th.getClass().getName());
        Element message = error.addElement("message");
        String msg = th.getMessage();
        message.addText(msg == null ? th.toString() : msg);
        if (logger.isInfoEnabled()) {
            Element details = error.addElement("details");
            StringWriter sw = new StringWriter();
            th.printStackTrace(new PrintWriter(sw));
            details.addCDATA(sw.toString());
        }
        if (th.getCause() != null) {
        	error.addAttribute("cause", th.getCause().getClass().getName());
        }
        ServletUtil.noCache(response);
        doc.write(response.getWriter());
    }

    /**
     * Called before a matched method
     *  For overriding, default does nothing
     */
    public void beforeMethod() { }

    /**
     * Called after an InvocationTargetException
     *  For overriding, default does nothing
     */
    public void methodException(Throwable t) { }

    /**
     * Called after a matched method
     *  For overriding, default does nothing
     */
    public void afterMethod() { }

    /**
     * @see javax.servlet.Servlet#init(ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        // reflection hash cache
        webMethods = new HashMap<String, Method>();
        StringBuffer mName = new StringBuffer();
        Method mArr[] = getClass().getDeclaredMethods();
        for (Method meth : mArr) {
            String nStr = meth.getName();
            if (nStr.length() > 3 && nStr.startsWith("web")) {
                mName.setLength(0);
                mName.append('/');
                mName.append(Character.toLowerCase(nStr.charAt(3)));
                mName.append(nStr.substring(4));
                webMethods.put(mName.toString(), meth);
                if (logger.isDebugEnabled()) {
                    logger.debug("Web method added: " + mName.toString());
                }
            }
        }
    }



}
