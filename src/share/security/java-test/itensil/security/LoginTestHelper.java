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

import itensil.security.hibernate.UserSpaceEntity;
import itensil.io.HibernateUtil;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;
import java.util.HashSet;

/**
 * @author ggongaware@itensil.com
 *
 */
public class LoginTestHelper {

    public static User createOrLogin(String userName, String password) throws UserSpaceException {
        SignOn signOn = SignOnFactory.getSignOn();
        try {
            return signOn.authenticate(userName, password, null, "junit");
        } catch (SignOnException e) {
        	HibernateUtil.beginTransaction();
            return createUser(userName, password);
        }
    }

    protected static User createUser(String userName, String password) throws UserSpaceException {
        UserSpaceEntity uspace = new UserSpaceEntity();
        uspace.setName("junit");
        uspace.setCreateTime(new Date());
        HibernateUtil.getSession().persist(uspace);
        Set<String> roles = new HashSet<String>();
        roles.add("admin");
        return uspace.createUser(
                userName, userName, password, roles, Locale.getDefault(), TimeZone.getDefault());
    }
}
