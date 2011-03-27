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

import itensil.security.hibernate.SignOnHB;
import itensil.security.hibernate.UserSpaceEntity;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SignOnFactory {

    private static SignOn instance;

    public static SignOn getSignOn() {
        if (instance == null) {
            synchronized(SignOnFactory.class) {
                if (instance == null) {
                    instance = new SignOnHB();
                }
            }
        }
        return instance;
    }
    
    public static UserSpace findUserSpaceByHost(String hostUrl) {
    	return UserSpaceEntity.findUserSpaceByHost(hostUrl);
    }
}
