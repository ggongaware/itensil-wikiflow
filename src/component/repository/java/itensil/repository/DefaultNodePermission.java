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
 * Created on Nov 20, 2003
 *
 */
package itensil.repository;

import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.RelativeGroup;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultNodePermission implements NodePermission, Serializable {


    public static final int NONE = 0;
    public static final int READ = 1;
    public static final int CREATE = 2;
    public static final int WRITE = 3;
    public static final int MANAGE = 4;

    protected Principal principal;
    protected int permission;
    protected boolean inherit;
    protected GroupAxis axis;
    protected String role;

    /**
     *
     * @param principal
     * @param permission
     * @param inherit
     */
    public DefaultNodePermission(
            Principal principal, int permission, boolean inherit) {

        this.principal = principal;
        this.permission = permission;
        this.inherit = inherit;
    }

    /**
    *
    * @param principal
    * @param permission
    * @param inherit
    */
   public DefaultNodePermission(
           Principal principal, int permission, boolean inherit, GroupAxis axis, String role) {

       this.principal = principal;
       this.permission = permission;
       this.inherit = inherit;
       this.axis = axis;
       this.role = role;
   }
    
    /*
     * @see itensil.repository.NodePermission#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /*
     * @see itensil.repository.NodePermission#canCreate()
     */
    public boolean canCreate() {
        return permission >= CREATE;
    }

    /*
     * @see itensil.repository.NodePermission#canManage()
     */
    public boolean canManage() {
        return permission >= MANAGE;
    }

    /*
     * @see itensil.repository.NodePermission#canRead()
     */
    public boolean canRead() {
        return permission >= READ;
    }

    /*
     * @see itensil.repository.NodePermission#canWrite()
     */
    public boolean canWrite() {
        return permission >= WRITE;
    }

    /*
     * @see itensil.repository.NodePermission#isInheritable()
     */
    public boolean isInheritable() {
        return inherit;
    }

    /*
     * @see itensil.repository.NodePermission#isNone()
     */
    public boolean isNone() {
        return permission == NONE;
    }
    

	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + permission;
		result = PRIME * result + ((principal == null) ? 0 : principal.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof DefaultNodePermission)
			return false;
		final DefaultNodePermission other = (DefaultNodePermission) obj;
		if (permission != other.permission)
			return false;
		if (principal == null) {
			if (other.principal != null)
				return false;
		} else if (!principal.equals(other.principal))
			return false;
		return true;
	}

	/**
     * @param principal
     * @return perm object
     */
    public static NodePermission readPermission(Principal principal) {
        return new DefaultNodePermission(
            principal, DefaultNodePermission.READ, false);
    }

    /**
     * @param principal
     * @return perm object
     */
    public static NodePermission noPermission(Principal principal) {
        return new DefaultNodePermission(
            principal, DefaultNodePermission.NONE, false);
    }

    /**
     * @param principal
     * @return perm object
     */
    public static NodePermission createPermission(Principal principal) {
        return new DefaultNodePermission(
            principal, DefaultNodePermission.CREATE, false);
    }

    /**
     * @param principal
     * @return perm object
     */
    public static NodePermission writePermission(Principal principal) {
        return new DefaultNodePermission(
            principal, DefaultNodePermission.WRITE, false);
    }

    /**
     * @param principal
     * @return perm object
     */
    public static NodePermission managePermission(Principal principal) {
        return new DefaultNodePermission(
            principal, DefaultNodePermission.MANAGE, false);
    }

    public static String permissionIntToString(int perm) {
        switch (perm) {
            case DefaultNodePermission.READ:
                return "read";
            case DefaultNodePermission.CREATE:
                return "create";
            case DefaultNodePermission.WRITE:
                return "write";
            case DefaultNodePermission.MANAGE:
                return "manage";
            default:
                return "none";
        }
    }

    /**
     * Get the Default integer value of the permission
     * @param perm
     * @return perm object
     */
    public static int permissionToInteger(NodePermission perm) {
        if (perm instanceof DefaultNodePermission) {
            return ((DefaultNodePermission) perm).permission;
        }
        if (perm.canManage()) {
            return DefaultNodePermission.MANAGE;
        } else if (perm.canWrite()) {
            return DefaultNodePermission.WRITE;
        } else if (perm.canCreate()) {
            return DefaultNodePermission.CREATE;
        } else if (perm.canRead()) {
            return DefaultNodePermission.READ;
        } else {
            return DefaultNodePermission.NONE;
        }
    }

	public GroupAxis getAxis() {
		return axis;
	}

	public String getRole() {
		return role;
	}

	public boolean isRelativeRole() {
		Principal prin = getPrincipal();
		return prin instanceof Group && RelativeGroup.isRelative((Group)prin);
	}
}
