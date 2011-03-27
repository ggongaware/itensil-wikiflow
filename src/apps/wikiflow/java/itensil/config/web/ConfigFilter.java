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
package itensil.config.web;

import itensil.config.ConfigManager;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ConfigFilter implements Filter {

	// current servlet config
	private FilterConfig sConfig = null;
	
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
			throws IOException, ServletException {

		if (ConfigManager.isReady()) {
			chain.doFilter(req, res);
		} else {
			// send to error page
			sConfig.getServletContext().getRequestDispatcher("/config-error.jsp").forward(req, res);
		}
	}

	public void init(FilterConfig config) throws ServletException { 
		sConfig = config;
	}
	
	public void destroy() { }

}
