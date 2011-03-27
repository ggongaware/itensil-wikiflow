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
package itensil.security;


import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;

/**
 * @author ggongaware@itensil.com
 *
 */
public class AuthenticatedUser extends DefaultUser {

    private UserSpace userSpace;
    protected Set<String> flags;
    protected Set<String> roles;

    public AuthenticatedUser() {
        super(null);
    }

    public AuthenticatedUser(
            String userId,
            String userName,
            String simpleName,
            Locale locale,
            TimeZone timeZone,
            UserSpace userSpace,
            long timestamp) {
        super(userId, userName, simpleName, locale, timeZone, timestamp);
        this.userSpace = userSpace;
    }

    public UserSpace getUserSpace() {
        return userSpace;
    }

    public String getUserSpaceId() {
        return getUserSpace().getUserSpaceId();
    }

    public Set<String> getFlags() {
        return flags;
    }

    public Set<String> getRoles() {
        return roles;
    }

	public void setActiveUserSpace(UserSpace uspace) {
		this.userSpace = uspace;
	}
}
