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
package itensil.workflow.activities.state;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ActivityRole implements Serializable {

    private String id;
    private String role;
    private String assignId;
    private Activity activity;

    public ActivityRole() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String val) {
        this.role = val;
    }

    public String getAssignId() {
        return assignId;
    }

    public void setAssignId(String val) {
        this.assignId = val;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ActivityRole activityRole = (ActivityRole) o;

        return id.equals(activityRole.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
