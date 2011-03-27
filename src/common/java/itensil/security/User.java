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
package itensil.security;


import java.security.Principal;
import java.util.TimeZone;
import java.util.Locale;

/**
 * @author ggongaware@itensil.com
 */
public interface User extends Principal {

	/**
     * @return unique user id
     */
    public String getUserId();

	/**
     * @return easier to read name string
     */
    public String getSimpleName();

	/**
     * @param group
     * @return true if users is group member
     */
    public boolean isUserInGroup(Group group);

    /**
     * @return groups the user is in
     */
    public Group[] getGroups();

    /**
     * @return time of last modification
     */
    public long timeStamp();

    /**
     *
     * @return userspace object
     */
    public UserSpace getUserSpace();

    /**
     *
     * @return User's timezone
     */
    public TimeZone getTimeZone();

    /**
     *
     * @return User's locale
     */
    public Locale getLocale();

    /**
     * 
     * @return
     */
    public String getUserSpaceId();
    
    /**
     * Get a low security risk user model
     * @return
     */
    public User getReference();
    
}
