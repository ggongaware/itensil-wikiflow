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
 * Created on Nov 17, 2003
 *
 */
package itensil.security;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SecurityAssociation {

    private static InheritableThreadLocal<User> userAssociation;

    static {
        userAssociation = new InheritableThreadLocal<User>();
    }

    /**
     *
     * @return the user associated with this thread
     */
    public static User getUser() {
        return userAssociation.get();
    }

    /**
     *
     * @param user authenticated user
     */
    public static void setUser(User user) {
        userAssociation.set(user);
    }

    public static void clear() {
        userAssociation.set(null);
    }

}
