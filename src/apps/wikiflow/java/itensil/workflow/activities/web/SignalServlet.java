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

import itensil.workflow.activities.signals.AlertDaemon;
import itensil.workflow.activities.timer.TimerDaemon;
import itensil.config.ConfigManager;
import itensil.mail.web.MailHoster;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SignalServlet extends HttpServlet {

    AlertDaemon alertd;

    public void init() throws ServletException {
    	if (!ConfigManager.isReady()) {
    		log("Config not ready, no alerts or timers will be available.");
    		return;
    	}
    	InputStream imp = getClass().getResourceAsStream("/itensil_cluster.properties");
    	boolean runAlerts = true;
    	boolean runTimers = true;
    	if (imp != null) {
    		Properties props = new Properties();
    		try {
				props.load(imp);
				
				runAlerts = Boolean.parseBoolean(props.getProperty("itensil.daemon.alerts", "true"));
				runTimers = Boolean.parseBoolean(props.getProperty("itensil.daemon.timers", "true"));
				
    		} catch (IOException ioe) {
                throw new ServletException(ioe);
            }
    	}
    	
    	if (runAlerts) {
    		log("Starting alert daemon");
	        MailHoster mailHost = (MailHoster)getServletContext().getAttribute("mailer-default");
	        if (mailHost != null) {
	            alertd = new AlertDaemon(mailHost.getMailService());
	            alertd.start();
	        }
    	}
    	
        // TODO find a better place of TimerDaemon
        if (runTimers) {
        	log("Starting timer daemon");
        	TimerDaemon.initInstance();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (alertd != null) {
            response.getWriter().print("Alerts are: " + (alertd.isAlive() ? "running" : "not-runnning"));
        } else {
            response.getWriter().print("Alerts offline");
        }
    }

    public void destroy() {
        if (alertd != null) alertd.die();
        
        // TODO find a better place of TimerDaemon
        if (TimerDaemon.getInstance() != null) TimerDaemon.getInstance().die();
    }

}
