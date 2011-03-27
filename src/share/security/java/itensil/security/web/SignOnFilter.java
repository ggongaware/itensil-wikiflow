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
package itensil.security.web;

import itensil.security.*;
import itensil.uidgen.IUID;
import itensil.util.Base64;
import itensil.util.Keys;
import itensil.util.Rot13;
import itensil.util.WildcardPattern;
import itensil.web.ServletUtil;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * The main goal for this is to allow a mix of FORM and BASIC auth.
 *
 *
 * @author ggongaware@itensil.com
 */
public class SignOnFilter implements Filter {


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

    protected SignOn signon;

    //protected User anonymous;

	public void init(FilterConfig config) throws ServletException {

		this.config = config;
		requireSSL = Boolean.valueOf(config.getInitParameter(Keys.CFG_REQUIRE_SSL));
		rememberUser = !Boolean.valueOf(config.getInitParameter(Keys.CFG_DIS_REMEMBER_USER));
		rememberAuth = !Boolean.valueOf(config.getInitParameter(Keys.CFG_DIS_REMEMBER_AUTH));
		useWeakMask = !Boolean.valueOf(config.getInitParameter(Keys.CFG_DISABLE_WEAK_MASK));
		realmName = config.getInitParameter(Keys.CFG_REALM_NAME);
		formLoginPage = config.getInitParameter(Keys.CFG_FORM_LOGIN_PAGE);
		formErrorPage = config.getInitParameter(Keys.CFG_FORM_ERROR_PAGE);
		loggedoutPage = config.getInitParameter(Keys.CFG_LOGGED_OUT_PAGE);
        authZone = config.getInitParameter(Keys.CFG_ZONE);

		// FORM patterns
		ArrayList<WildcardPattern> patterns = new ArrayList<WildcardPattern>();
		StringTokenizer tok =
			new StringTokenizer(config.getInitParameter(Keys.CFG_FORM_PATTTERN));
		while(tok.hasMoreTokens()) {
			WildcardPattern pat = new WildcardPattern(tok.nextToken());
			patterns.add(pat);
		}
		formPatterns = patterns.toArray(new WildcardPattern[patterns.size()]);

		// BASIC patterns
		patterns.clear();
		tok = new StringTokenizer(config.getInitParameter(Keys.CFG_BASIC_PATTTERN));
		while(tok.hasMoreTokens()) {
			WildcardPattern pat = new WildcardPattern(tok.nextToken());
			patterns.add(pat);
		}
		basicPatterns = patterns.toArray(new WildcardPattern[patterns.size()]);

        // Anonymous patterns
		patterns.clear();
		tok = new StringTokenizer(config.getInitParameter(Keys.CFG_ANON_PATTTERN));
		while(tok.hasMoreTokens()) {
			WildcardPattern pat = new WildcardPattern(tok.nextToken());
			patterns.add(pat);
		}
		anonPatterns = patterns.toArray(new WildcardPattern[patterns.size()]);

        signon = SignOnFactory.getSignOn();
        config.getServletContext().setAttribute("itensil.SingOn", signon);
	}

	public void doFilter(
			ServletRequest request,
			ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

        SecurityAssociation.clear();
		HttpServletRequest hreq = (HttpServletRequest)request;
		HttpServletResponse hres = (HttpServletResponse)response;

		String context = hreq.getContextPath();
		String currentURL = hreq.getRequestURI();
		String targetURL = currentURL.substring(context.length());

		if (targetURL.endsWith(Keys.URL_SIGNON)) {
			if (validateForm(hreq, hres)) {
				HttpSession session = hreq.getSession();
				String origURL = (String)session.getAttribute(Keys.ORIGINAL_URL);
				if (origURL == null) origURL = "";

				// send to ORIGINAL_URL
				hres.sendRedirect(context + origURL);
				return;
			} else {

                request.setAttribute("error",
                    "User name or password did not match");

				// send to error page
				config.getServletContext().getRequestDispatcher(
						formErrorPage).forward(request, response);
				return;
			}
		} else if (targetURL.endsWith(Keys.URL_SIGNOFF)) {
			doLogout(hreq, hres);
			return;
		}

        // Anonymouse Zone
        for (WildcardPattern pat : anonPatterns) {
            if (pat.match(targetURL)) {
                try {
                    User user = getAnonymous();
                    HttpServletRequest wrapHreq =
                            new SignedOnRequest(hreq, user);
                    SecurityAssociation.setUser(user);
                    chain.doFilter(wrapHreq, response);
                    return;
                } catch (SignOnException soe) {
                    hres.sendError(
                            HttpServletResponse.SC_PRECONDITION_FAILED,
                            soe.getMessage());
                    return;
                }
            }
        }

		HttpSession session = hreq.getSession(false);
		if (session != null && session.getAttribute(Keys.SIGNED_ON_USER) == Boolean.TRUE
				|| validateBasic(hreq, hres)
				|| validateRemember(hreq, hres) 
				|| validateToken(hreq, hres)) {

			// wrap request
            User user;
            if (session != null) {
                user = (User)session.getAttribute(Keys.USER_OBJECT);
            } else {
                user = (User)request.getAttribute(Keys.USER_OBJECT);
            }
			HttpServletRequest wrapHreq = new SignedOnRequest(hreq, user);
            SecurityAssociation.setUser(user);
			chain.doFilter(wrapHreq, response);
			return;
		} else {
			for (WildcardPattern pat : formPatterns) {
				if (pat.match(targetURL)) {
                    String qs = hreq.getQueryString();
                    if (session == null) session = hreq.getSession();
                    if (qs == null) {
                        session.setAttribute(Keys.ORIGINAL_URL, targetURL);
                    } else {
                        session.setAttribute(
                        		Keys.ORIGINAL_URL, targetURL + "?" + qs);
                    }
                    
                    // get the brand if you can
                    String brand = (String)session.getAttribute("brand");
                    if (brand == null) {
                    	UserSpace uspace = resolveUserSpace(hreq);
                    	brand = uspace != null ? uspace.getBrand() : null;
                    	session.setAttribute("brand", brand != null ? brand : "");
                    }

					// send to login page
					config.getServletContext().getRequestDispatcher(
						 	formLoginPage).forward(request, response);
				 	return;
				}
			}
			for (WildcardPattern pat : basicPatterns) {
				if (pat.match(targetURL)) {
					hres.setHeader("WWW-Authenticate", "BASIC realm=\"" + realmName + "\"");
					hres.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					return;
				}
			}
		}
		chain.doFilter(request,response);
	}

    protected User getAnonymous() throws SignOnException {
        /*if (anonymous == null) {
            anonymous = signon.authenticateAnonymous(authZone);
        }
        return anonymous;*/
    	return null;
    }

    public boolean validateRemember(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		if (rememberAuth) {
			Cookie authCookie = null;
			Cookie cookies[] = request.getCookies();
			if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (Keys.COOKIE_AUTH.equals(cookie.getName())) {
                        authCookie = cookie;
                        break;
                    }
                }
			}
			if (authCookie != null) {
				String val = authCookie.getValue();
				if (useWeakMask) {
					val = Rot13.rot13(val);
				}
				String auth = Base64.decode(val);
				int sep = auth.indexOf(':');
				String username = "";
				String password = "";
				if (sep >= 0) {
					username = auth.substring(0, sep).trim();
					password = auth.substring(sep + 1).trim();
				}
				return validate(request, username, password);
			}
		}
		return false;
	}

	public boolean validateForm(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		String username = request.getParameter(Keys.FORM_USER_NAME);
		String password = request.getParameter(Keys.FORM_PASSWORD);
		String remember = request.getParameter(Keys.FORM_REMEMBER);
		String targetUrl = request.getParameter(Keys.FORM_TARGET_URL);
        String token = request.getParameter(Keys.FORM_TOKEN);
		if (username == null) username = "";
		if (password == null) password = "";
		if (targetUrl != null && targetUrl.length() > 0) {
			request.getSession().setAttribute(Keys.ORIGINAL_URL, targetUrl);
		}
        if (token != null) {
            String val = token;
            if (useWeakMask) {
                val = Rot13.rot13(val);
            }
            String auth = Base64.decode(val);
            int sep = auth.indexOf(':');
            if (sep >= 0) {
                username = auth.substring(0, sep).trim();
                password = auth.substring(sep + 1).trim();
            }
        }
		if (validate(request, username, password)) {

			// cookies
			if ("1".equals(remember)) {
				if (rememberUser) {
					Cookie userCookie = new Cookie(Keys.COOKIE_NAME, username);
					userCookie.setMaxAge(60*60*24*30); // 30 days
					userCookie.setPath(request.getContextPath());
					response.addCookie(userCookie);
				}
				if (rememberAuth) {
                    String val = Base64.encode(username + ":" + password, true);
                    if (useWeakMask) {
                        val = Rot13.rot13(val);
                    }
                    Cookie authCookie = new Cookie(Keys.COOKIE_AUTH, val);
                    authCookie.setMaxAge(60*60*24*30); // 30 days
                    authCookie.setPath(request.getContextPath());
                    if (requireSSL) {
                        authCookie.setSecure(true);
                    }
                    response.addCookie(authCookie);
				}
			}
			return true;
		} else {
			request.setAttribute(Keys.REQUEST_SIGNON_USER, username);
			return false;
		}
	}

	public boolean validateBasic(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		String authHeader = request.getHeader("Authorization");
		if (authHeader == null ||
				!authHeader.toLowerCase().startsWith("basic ")) {
			return false;
		} else {
			authHeader = authHeader.substring(6).trim();
			String auth = Base64.decode(authHeader);
			int sep = auth.indexOf(':');
			String username = "";
			String password = "";
			if (sep >= 0) {
				username = auth.substring(0, sep).trim();
				password = auth.substring(sep + 1).trim();
			}
			return validate(request, username, password);
		}

	}

	public boolean validateToken(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		
		String token = request.getParameter(Keys.FORM_TOKEN);
		if (token == null) token = "";
		token =token.trim();
		return validate(request, token);
		}

	
	
	/**
	 * Might resolve to null
	 * @param request
	 * @return
	 */
	protected UserSpace resolveUserSpace(HttpServletRequest request) {
		String hostUrl = ServletUtil.getAbsoluteContextPath(request);
		while (hostUrl.endsWith("/")) 
			hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
		return SignOnFactory.findUserSpaceByHost(hostUrl);
	}

	protected boolean validate(
			HttpServletRequest request,
			String username,
			String password) throws IOException, ServletException {

		HttpSession session = request.getSession();
		UserSpace uspace = resolveUserSpace(request);
		if (username.length() > 0 && password.length() > 0) {
            User user;
            try {
                user = signon.authenticate(username, password, uspace, authZone);
            } catch (SignOnException soe) {
                request.setAttribute("SignOnException", soe);
                if (uspace != null) session.setAttribute("brand", uspace.getBrand());
                return false;
            }
            
            // fill-in user object attributes
            request.setAttribute(Keys.USER_OBJECT, user);
	
            session.setAttribute(Keys.SIGNED_ON_USER, Boolean.TRUE);
            session.setAttribute(Keys.USER_OBJECT, user);
            session.setAttribute("brand", user.getUserSpace().getBrand());

			return true;
		} else {
			session.setAttribute(Keys.SIGNED_ON_USER, Boolean.FALSE);
			if (uspace != null) session.setAttribute("brand", uspace.getBrand());
			return false;
		}
	}


	protected boolean validate(
			HttpServletRequest request,
			String token) throws IOException, ServletException {

		HttpSession session = request.getSession();
		UserSpace uspace = resolveUserSpace(request);
		if (token.length() == IUID.UUID_SIZE ) {
            User user;
            try {
                user = signon.authenticate(token, uspace, authZone);
            } catch (SignOnException soe) {
                request.setAttribute("SignOnException", soe);
                if (uspace != null) session.setAttribute("brand", uspace.getBrand());
                return false;
            }
            
            // fill-in user object attributes
            request.setAttribute(Keys.USER_OBJECT, user);
	
            session.setAttribute(Keys.SIGNED_ON_USER, Boolean.TRUE);
            session.setAttribute(Keys.USER_OBJECT, user);
            session.setAttribute("brand", user.getUserSpace().getBrand());

			return true;
		} else {
			session.setAttribute(Keys.SIGNED_ON_USER, Boolean.FALSE);
			if (uspace != null) session.setAttribute("brand", uspace.getBrand());
			return false;
		}
	}


	public void doLogout(
			HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {

		// eat cookies
		if (rememberAuth) {
			Cookie authCookie = new Cookie(Keys.COOKIE_AUTH, "");
            authCookie.setPath(request.getContextPath());
			authCookie.setMaxAge(0);
			response.addCookie(authCookie);
		}

		// flush session
		HttpSession session = request.getSession();
		session.invalidate();
		
		// hard kill the jsession cookie (TOMCAT version)
		Cookie sessCookie = new Cookie("JSESSIONID", "");
		sessCookie.setPath(request.getContextPath());
		//sessCookie.setMaxAge(0);
		response.addCookie(sessCookie);		

		// send to loggedout
		response.sendRedirect(request.getContextPath() + loggedoutPage);
	}

	public void destroy() {
		config = null;
	}

}
