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
 * Created on Nov 13, 2003
 *
 */
package itensil.security;

import java.io.Serializable;
import java.util.TimeZone;
import java.util.Locale;


/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultUser implements User, Serializable {

    protected String userId;
    protected String userName;
    protected String simpleName;
    protected long timestamp;
    protected Locale locale;
    protected TimeZone timeZone;

    /**
     * @param userId
     * @param userName
     * @param simpleName
     * @param timestamp
     */
    public DefaultUser(
            String userId,
            String userName,
            String simpleName,
            Locale locale,
            TimeZone timeZone,
            long timestamp) {

        this.userId = userId;
        this.userName = userName;
        this.simpleName = simpleName;
        this.locale = locale;
        this.timeZone = timeZone;
        this.timestamp = timestamp;
    }

    /**
     * @param userId
     * @param userName
     * @param simpleName
     * @param locale
     * @param timeZone
     */
    public DefaultUser(
        String userId,
        String userName,
        String simpleName,
        Locale locale,
        TimeZone timeZone) {

        this(
            userId,
            userName,
            simpleName,
            locale,
            timeZone,
            System.currentTimeMillis());
    }

    /**
     * @param userId
     * @param userName
     * @param simpleName
     */
    public DefaultUser(
        String userId, String userName, String simpleName) {

        this(
            userId,
            userName,
            simpleName,
            Locale.getDefault(),
            TimeZone.getDefault());
    }

    /**
     * @param userId
     * @param userName
     */
    public DefaultUser(String userId, String userName) {
        this(userId, userName, userName);
    }

    /**
     * @param userId
     * @param userName
     * @param locale
     * @param timeZone
     */
    public DefaultUser(
        String userId,
        String userName,
        Locale locale,
        TimeZone timeZone) {
        this(userId, userName, userName, locale, timeZone);
    }


    /**
     * @param userId
     */
    public DefaultUser(String userId) {
        this(userId, null, null);
    }


    /*
     * @see itensil.security.User#getUserId()
     */
    public String getUserId() {
        return userId;
    }

    /*
     * @see itensil.security.User#getSimpleName()
     */
    public String getSimpleName() {
        return simpleName;
    }

    /*
     * @see itensil.security.User#isUserInGroup(itensil.security.Group)
     */
    public boolean isUserInGroup(Group group) {
        return false;
    }

    /*
     * @see itensil.security.User#getGroups()
     */
    public Group[] getGroups() {
        return new Group[0];
    }

    /*
     * @see itensil.security.User#timeStamp()
     */
    public long timeStamp() {
        return timestamp;
    }


    /*
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return userName;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
    	if (this == obj)
			return true;
        if (obj instanceof User) {
            User usr = (User)obj;
            String oID = usr.getUserId();
            if (userId != null && oID != null) {
                return userId.equals(oID);
            } else if (userName != null) {
                return userName.equals(usr.getName());
            }
        }
        return false;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return userId;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        if (userId != null) {
            return userId.hashCode();
        }
        return 0;
    }

    /*
     * @see itensil.security.User#getUserSpace()
     */
    public UserSpace getUserSpace() {
        return null;
    }


    /**
     *
     * @return User's timezone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     *
     * @return User's locale
     */
    public Locale getLocale() {
        return locale;
    }

    public String getUserSpaceId() {
        return null;
    }

	public User getReference() {
		return this;
	}

}
