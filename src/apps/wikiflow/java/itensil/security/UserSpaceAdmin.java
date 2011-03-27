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

import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.io.HibernateUtil;

/**
 * @author ggongaware@itensil.com
 *
 */
public final class UserSpaceAdmin {

    public static AuthenticatedUser getAuthUser(String userId) throws UserSpaceException {
        checkAccess();
        UserEntity usrEnt = (UserEntity)HibernateUtil.getSession().get(UserEntity.class, userId);
        return usrEnt.isDeleted() ? null : (AuthenticatedUser)usrEnt.getReference();
    }
    
    public static UserSpace getUserSpace(String userSpaceId) {
    	 return (UserSpaceEntity)HibernateUtil.getSession().get(UserSpaceEntity.class, userSpaceId);
    }

    protected static void checkAccess() throws UserSpaceException  {
        if(!SysAdmin.isSysAdmin(SecurityAssociation.getUser()))
            throw new UserSpaceException("Access Denied");
    }
}
