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
 * Created on Dec 18, 2003
 *
 */
package itensil.security;


import java.util.TimeZone;
import java.util.Locale;
import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public final class Anonymous implements User, Serializable {


    // never change this ID
    private static final String userId = "1z5kbPkAAIbzwKgAZGWp";
    private static final String userName = "Anonymous";

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
        return userName;
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
        return 0;
    }


    /*
     * @see itensil.security.User#getUserSpace()
     */
    public UserSpace getUserSpace() {
        return null;
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
        if (obj instanceof User) {
            User usr = (User)obj;
            String oID = usr.getUserId();
            if (oID!= null) {
                return userId.equals(oID);
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
        return userId.hashCode();
    }

    /**
     *
     * @return User's timezone
     */
    public TimeZone getTimeZone() {
        return TimeZone.getDefault();
    }

    /**
     *
     * @return User's locale
     */
    public Locale getLocale() {
        return Locale.getDefault();
    }

    public String getUserSpaceId() {
        return null;
    }

	public User getReference() {
		return this;
	}
}
