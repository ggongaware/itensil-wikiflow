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
package itensil.repository.hibernate;

import itensil.repository.NodePermission;
import itensil.repository.DefaultNodePermission;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.User;
import itensil.util.Check;

import java.security.Principal;

/**
 * @author ggongaware@itensil.com
 *
 */
public class PermissionEntity extends DefaultNodePermission {

    private NodeEntity nodeEntity;
    private boolean group;
    private String principalId;

    public PermissionEntity() {
        super(null, DefaultNodePermission.NONE, false);
    }

    /**
     * @param principal
     * @param permission
     * @param inherit
     */
    public PermissionEntity(Principal principal, int permission, boolean inherit) {
        this(principal, permission, inherit, null, null);
    }
    
    public PermissionEntity(Principal principal, int permission, boolean inherit, GroupAxis axis, String role) {
    	super(principal, permission, inherit, axis, role);
    	if (principal instanceof Group) {
            principalId = ((Group) principal).getGroupId();
            group = true;
        } else {
            principalId = ((User) principal).getUserId();
            group = false;
        }
    }

    public PermissionEntity(NodePermission perm) {
        this(perm.getPrincipal(), DefaultNodePermission.permissionToInteger(perm), perm.isInheritable(), perm.getAxis(), perm.getRole());
    }

    public NodeEntity getNodeEntity() {
        return nodeEntity;
    }

    public void setNodeEntity(NodeEntity nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public boolean isInherit() {
        return inherit;
    }

    public void setInherit(boolean inherit) {
        this.inherit = inherit;
    }

    /*
    * @see itensil.repository.NodePermission#getPrincipal()
    */
    public Principal getPrincipal() {
        if (principal == null) {
            principal = group ? new DefaultGroup(principalId) : new DefaultUser(principalId);
        }
        return principal;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }


    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
        principal = null;
    }

    public boolean hasPermission(int testPermission) {
        return permission >= testPermission;
    }
    
    public void setAxis(GroupAxis axis) {
    	this.axis = axis;
    }
    
    public String getAxisStr() {
    	return axis == null ? null : axis.toString();
    }
    
    public void setAxisStr(String axisStr) {
    	this.axis = Check.isEmpty(axisStr) ? null : GroupAxis.valueOf(axisStr);
    }

    public void setRole(String role) {
    	this.role = role;
    }

}
