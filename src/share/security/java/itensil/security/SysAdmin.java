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

/**
 * @author ggongaware@itensil.com
 *
 */
public final class SysAdmin {

    private static AuthenticatedUser sysAdmin;

    public static AuthenticatedUser getUser() {

        if (sysAdmin == null) {
            synchronized (SysAdmin.class) {
                if (sysAdmin == null) {

                    // TODO load sysadmin values from config

                    sysAdmin = new AuthenticatedUser(
                            "fycRVw0BAF3mChMBS$fk",
                            "sysadmin@itensil.net",
                            "SysAdmin",
                            Locale.getDefault(),
                            TimeZone.getDefault(),
                            null,
                            System.currentTimeMillis()
                        );
                }
            }
        }
        return sysAdmin;
    }

    public static boolean isSysAdmin(User user) {
        return user instanceof AuthenticatedUser && getUser().equals(user);
    }

}
