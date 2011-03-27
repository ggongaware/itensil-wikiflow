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

public abstract class SignalManagerFilter implements Filter {

	public static final String SIGNALS = "signals"; 
	public static final String ALERT_ID = "aid";
	public static final String ACTIVITY_PAGE = "/act/page";

	// current config
	private FilterConfig config = null;

	protected boolean rememberUser;
	protected boolean rememberAuth;
	protected boolean useWeakMask;
	protected boolean requireSSL;

	protected String realmName;
	protected String formLoginPage;
	protected String formErrorPage;
	protected String loggedoutPage;
	protected String authZone;

	protected WildcardPattern basicPatterns[];
	protected WildcardPattern formPatterns[];
	protected WildcardPattern anonPatterns[];

	HttpServletRequest hreq;
	HttpServletResponse hres;

	String context;
	String currentURL;
	String targetURL;

	HttpSession session;

	public void init(FilterConfig config) throws ServletException {
		this.config = config;
	}
	
	public void destroy() {
		config = null;
	}

	protected void getContext(ServletRequest request, ServletResponse response) {
		hreq = (HttpServletRequest) request;
		hres = (HttpServletResponse) response;
	
		context = hreq.getContextPath();
		currentURL = hreq.getRequestURI();
		targetURL = currentURL.substring(context.length());
	
		session = hreq.getSession(false);
	}

	protected boolean activityPageURL() {
		return targetURL.equals(ACTIVITY_PAGE);
	}
	
}
