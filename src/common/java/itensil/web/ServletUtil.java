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
import java.util.Iterator;
import java.util.Map;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ServletUtil {

    /**
     * @param response
     */
    public static void noCache(HttpServletResponse response) {
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-cache");
    }

    /**
     * @param response
     * @param seconds
     */
    public static void cacheTimeout(HttpServletResponse response, int seconds) {
        response.setHeader("Cache-Control", "max-age=" + seconds);
        response.setDateHeader(
            "Expires", System.currentTimeMillis() + (seconds*1000));
    }

    /**
     * @param response
     */
    public static void setExpired(HttpServletResponse response) {
    	response.setHeader("Cache-Control", "max-age=0, must-revalidate");
        response.setDateHeader("Expires",  0);
    }

     /**
      * @param request
      * @param lastModified
      */
    public static boolean isModified(HttpServletRequest request, Date lastModified) {
        try {
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (lastModified != null) {
                if (ifModifiedSince > 0) {
                    if (lastModified.getTime() <= (ifModifiedSince + 1000)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception ex) {
            return true;
        }
    }

    /**
     * @param path
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public static void forward(
            String path,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher(path).forward(request, response);
    }

    /**
     * @param path
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public static void include(
            String path,
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher(path).include(request, response);
    }


    /**
     * @param path
     * @param query
     * @param response
     * @throws IOException
     */
    public static void bounce(
        String path,
        String query,
        HttpServletResponse response)
        throws IOException {

        StringBuffer buf = new StringBuffer(path);
        if (query != null && query.length() > 0) {
            buf.append('?');
            buf.append(query);
        }
        response.sendRedirect(
                response.encodeRedirectURL(buf.toString()));
    }


    /**
     * @param path
     * @param params
     * @param response
     * @throws IOException
     */
    public static void bounce(
        String path,
        Map params,
        HttpServletResponse response)
        throws IOException {

        StringBuffer buf = new StringBuffer();
        if (params != null && !params.isEmpty()) {
            Iterator itr = params.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry param = (Map.Entry)itr.next();
                if (param.getValue() != null) {
                    buf.append(String.valueOf(param.getKey()));
                    buf.append('=');
                    buf.append(String.valueOf(param.getValue()));
                    buf.append('&');
                }
            }
            buf.setLength(buf.length() - 1);
        }
        bounce(path, buf.toString(), response);
    }

    /**
     * @param request
     * @return uri after server adderss
     */
    public static String getServletPath(HttpServletRequest request) {
        return getServletPath(request, request.getServletPath());
    }

    /**
     * @param request
     * @param servletPath
     * @return uri after server adderss
     */
    public static String getServletPath(
        HttpServletRequest request, String servletPath) {
        StringBuffer buf = new StringBuffer();
        buf.append(request.getContextPath());
        buf.append(servletPath);
        return buf.toString();
    }

    public static String getAbsoluteServletPath(HttpServletRequest request) {
        return getAbsoluteContextPath(request) +
            request.getServletPath().substring(1);
    }

    /**
     * Get the full server name http://xxx/appcontext/
     * @param request
     * @return
     */
    public static String getAbsoluteContextPath(HttpServletRequest request) {
        StringBuffer url = new StringBuffer();
        if  (request.isSecure()) {
            url.append("https://");
            url.append(request.getServerName());
            if (request.getServerPort() != 443) {
                url.append(':');
                url.append(request.getServerPort());
            }
        } else {
            url.append("http://");
            url.append(request.getServerName());
            if (request.getServerPort() != 80) {
                url.append(':');
                url.append(request.getServerPort());
            }
        }
        String ctx = request.getContextPath();
        if (ctx != null) {
            url.append(ctx);
        }
        url.append('/');
        return url.toString();
    }


    /**
     * Get the context/servlet/pathinfo\?query
     * @return
     */
    public static String getRequestURI(HttpServletRequest request) {

        StringBuffer url = new StringBuffer();
        url.append(request.getRequestURI());
        //url.append(request.getPathInfo());
        String q = request.getQueryString();
        if (!Check.isEmpty(q)){
            url.append('?');
            url.append(q);
        }
        return url.toString();
    }
}
