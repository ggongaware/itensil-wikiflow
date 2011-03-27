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

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public final class Everyone implements Group, Serializable {


    // never change this ID
    private static final String groupId = "0ZkXhvkAANcHwKgAZMjH";
    private static final String groupName = "Everyone";

    /*
     * @see itensil.security.Group#getGroupId()
     */
    public String getGroupId() {
        return groupId;
    }

    /*
     * @see itensil.security.Group#getSimpleName()
     */
    public String getSimpleName() {
        return groupName;
    }

    /*
     * @see itensil.security.Group#timeStamp()
     */
    public long timeStamp() {
        return 0;
    }


    /*
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return groupName;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof Group) {
            Group grp = (Group)obj;
            String oID = grp.getGroupId();
            return groupId.equals(oID);
        }
        return false;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return groupId;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return groupId.hashCode();
    }

}
