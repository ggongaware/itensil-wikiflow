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
package itensil.workflow.activities.signals;

import itensil.repository.NotFoundException;
import itensil.security.SignOn;
import itensil.security.User;
import itensil.util.Keys;
import itensil.util.WildcardPattern;
import itensil.web.MethodServlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;



/**
 * This class provides general Signal status and determines which high-level rule sets need to be applied for the
 * Signals per request required
 * 
 * 
 * @author ejones@itensil.com
 */

public class SignalManagerFilter_DoBefore extends  SignalManagerFilter {

    protected static Logger logger = Logger.getLogger(SignalManagerFilter_DoBefore.class);


	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		 getContext(request, response);

		 if (session != null && session.getAttribute(Keys.SIGNED_ON_USER) == Boolean.TRUE) {
			 try {
				 Long sig = SignalManager.getSignalStatus((User) session.getAttribute(Keys.USER_OBJECT));
				 session.setAttribute(SIGNALS, sig);
			} catch (NotFoundException nfe) {
				logger.info("User or user ID is null on call to getSIgnalStatus");
			}
		 }	

		 chain.doFilter(request, response);
	}

}
