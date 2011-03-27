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


/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultGroup implements Group, Serializable {

    protected String groupId;
    protected String groupName;
    protected String simpleName;
    protected long timestamp;

    /**
     * @param groupId
     * @param groupName
     * @param simpleName
     * @param timestamp
     */
    public DefaultGroup(
            String groupId,
            String groupName,
            String simpleName,
            long timestamp) {

        this.groupId = groupId;
        this.groupName = groupName;
        this.simpleName = simpleName;
        this.timestamp = timestamp;
    }

    /**
     * @param groupId
     * @param groupName
     * @param simpleName
     */
    public DefaultGroup(
        String groupId, String groupName, String simpleName) {
        this(
            groupId,
            groupName,
            simpleName,
            System.currentTimeMillis());
    }

    /**
     * @param groupId
     * @param groupName
     */
    public DefaultGroup(String groupId, String groupName) {
        this(groupId, groupName, groupName);
    }


    /**
     * @param groupId
     */
    public DefaultGroup(String groupId) {
        this(groupId, null, null);
    }


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
        return simpleName;
    }

    /*
     * @see itensil.security.Group#timeStamp()
     */
    public long timeStamp() {
        return timestamp;
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
    	if (this == obj)
			return true;
        if (obj instanceof Group) {
            Group grp = (Group)obj;
            String oID = grp.getGroupId();
            if (groupId != null && oID != null) {
                return groupId.equals(oID);
            } else if (groupName != null) {
                return groupName.equals(grp.getName());
            }
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
        if (groupId != null) {
            return groupId.hashCode();
        }
        return 0;
    }

}
