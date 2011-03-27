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
package itensil.mail.web;

import itensil.mail.*;
import itensil.config.ConfigManager;
import itensil.config.Property;
import itensil.io.ReplaceFilter;
import itensil.io.StreamUtil;
import itensil.util.Check;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.Map;

/**
 * Author: grant@gongaware.com
 */
public class MailHoster extends HttpServlet {

    private MailService mailserv;
    private String servName;

    protected void doGet(
        HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        if (mailserv != null) {
            out.println("<table border='1'><tr>");
            out.println("<td>Start time:</td>");
            out.println("<td>" + new Date(mailserv.getStartTime()) + "</td>");
            out.println("</tr><tr>");
            out.println("<td>Queued:</td>");
            out.println("<td>" + mailserv.getQueueSize() + "</td>");
            out.println("</tr><tr>");
            out.println("<td>Sent:</td>");
            out.println("<td>" + mailserv.getSendCount() + "</td>");
            out.println("</tr><tr>");
            out.println("<td>Bounces:</td>");
            out.println("<td>" + mailserv.getBounceCount() + "</td>");
            out.println("</tr><tr>");
            out.println("<td>Last Bounce check time:</td>");
            out.println("<td>" + new Date(mailserv.getLastCheckTime()) + "</td>");
            out.println("</tr><tr>");
            out.println("<td>Last error:</td>");
            out.println("<td>" + mailserv.getLastError() + "</td>");
            out.println("</tr></table>");
        }

        out.println("</body></html>");
    }

    public void destroy() {

        // clean up mail gobblins
        MailServicePool pool = MailServicePool.getPool();
        pool.removeService(servName);
        if (!MailServicePool.getPool().hasServices()) {
            pool.shutdown();
        }
    }

    public void init() throws ServletException {

        ServletConfig conf = getServletConfig();
        MailServicePool mPool = MailServicePool.getPool();
        servName = conf.getInitParameter("name");
        mailserv = mPool.getService(servName);
        if (mailserv == null) {
            BounceHandler bHandle = null;
            DeliveryHandler dHandle = null;
            String bClassName = conf.getInitParameter("bouncer");
            if (!Check.isEmpty(bClassName)) {
                try {
                    Class bClass = getClass().getClassLoader().loadClass(bClassName);
                    bHandle = (BounceHandler)bClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new ServletException("Mail bouncer class error", e);
                } catch (IllegalAccessException e) {
                    throw new ServletException("Mail bouncer class error", e);
                } catch (InstantiationException e) {
                    throw new ServletException("Mail bouncer class error", e);
                }
            }

            String dClassName =  conf.getInitParameter("delivery");
            if (!Check.isEmpty(dClassName)) {
                try {
                    Class bClass = getClass().getClassLoader().loadClass(dClassName);
                    dHandle = (DeliveryHandler)bClass.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new ServletException("Mail delivery class error", e);
                } catch (IllegalAccessException e) {
                    throw new ServletException("Mail delivery class error", e);
                } catch (InstantiationException e) {
                    throw new ServletException("Mail delivery class error", e);
                }
            }

            Properties props = new Properties(System.getProperties());
            
            // Check path first for deploy time mail properties
            InputStream imp = getClass().getResourceAsStream("/itensil_mail.properties");
            if (imp != null) {

            	try {

	            	props.load(imp);
	            	Property cProp = new Property();
	            	cProp.setComponent("mailer-" + servName);
	            	cProp.setVersion("");
	            	StringWriter sw = new StringWriter();
	            	StreamUtil.copyStream(getClass().getResourceAsStream("/itensil_mail.properties"), sw);
	            	cProp.setProperties(sw.toString());
	            	ConfigManager.setProperty(cProp);
	            	
	            } catch (IOException ioe) {
	                throw new ServletException(ioe);
	            }

            } else {
            	
	            Property cProp = ConfigManager.getProperty("mailer-" + servName);
	            String mailStr;
	            if (cProp != null) {
	            	mailStr = cProp.getProperties();
	            } else {
	            	mailStr = conf.getInitParameter("properties");
	            	cProp = new Property();
	            	cProp.setComponent("mailer-" + servName);
	            	cProp.setVersion("");
	            	cProp.setProperties(mailStr);
	            	ConfigManager.setProperty(cProp);
	            }
   
	            try { 
	                if (mailStr != null) {
	                    props.load(new ByteArrayInputStream(mailStr.getBytes()));
	                }
	            } catch (IOException ioe) {
	                throw new ServletException(ioe);
	            }
	            
            }
            mailserv = new MailService(bHandle, dHandle, props);
            mPool.setService(servName, mailserv);
        }
        getServletContext().setAttribute("mailer-" + servName, this);
    }

    public MailService getMailService() {
        return mailserv;
    }
    
    public void reloadMailService() {
    	 Property cProp = ConfigManager.getProperty("mailer-" + servName);
    	 if (cProp != null) {
    		 Properties props = new Properties(System.getProperties());
    		 String mailStr = cProp.getProperties();
    		 try { 
                 if (mailStr != null) {
                     props.load(new ByteArrayInputStream(mailStr.getBytes()));
                 }
                 MailServicePool mPool = MailServicePool.getPool();
                 mailserv = new MailService(mailserv.getBouncer(), mailserv.getDeliver(), props);
                 mPool.setService(servName, mailserv);
                 
             } catch (IOException ioe) {
            	 log("Reload error", ioe);
             }
    	 }
    }

    /**
     * @param mapMsg
     */
    public void sendMessage(Map<String, String> mapMsg) {
        try {
            ReplaceFilter filter = new ReplaceFilter();
            for (Map.Entry<String, String> mEnt : mapMsg.entrySet()) {
                String name = mEnt.getKey();
                if (!name.startsWith("mail-")) {
                    filter.addReplaceKey(name, mEnt.getValue());
                }
            }
            ServletContext ctx = getServletContext();
            StringWriter htmlOut = new StringWriter();
            StringWriter txtOut = new StringWriter();
            filter.execute(
                new InputStreamReader(ctx.getResourceAsStream(
                    "/eml/" + mapMsg.get("mail-template-html"))),
                htmlOut);
            filter.execute(
                new InputStreamReader(ctx.getResourceAsStream(
                    "/eml/" + mapMsg.get("mail-template-text"))),
                txtOut);
            mailserv.send(
                MailService.address(
                    mapMsg.get("mail-to-address"),
                    mapMsg.get("mail-to-name")),
                MailService.address(
                    mapMsg.get("mail-from-address"),
                    mapMsg.get("mail-from-name")),
                "",
                mapMsg.get("mail-subject"),
                htmlOut.toString(),
                txtOut.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
