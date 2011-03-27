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
 * Created on Nov 3, 2003
 *
 */
package itensil.security.web;

import itensil.security.DefaultGroup;
import itensil.security.User;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author ggongaware@itensil.com
 */
public class SignedOnRequest extends HttpServletRequestWrapper {

	private User user = null;

	/**
	 * @param request
	 */
	public SignedOnRequest(HttpServletRequest request, User user) {
		super(request);
        this.user = user;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	public Principal getUserPrincipal() {
		return user;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String role) {
		return user.isUserInGroup(new DefaultGroup(role));
	}

    /*
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    public String getAuthType() {
        return HttpServletRequest.BASIC_AUTH;
    }

    /*
     * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
     */
    public String getRemoteUser() {
        return user.getName();
    }

}
