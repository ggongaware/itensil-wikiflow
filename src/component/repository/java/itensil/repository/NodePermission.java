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
 * Created on Aug 27, 2003
 *
 */
package itensil.repository;

import itensil.security.GroupAxis;

import java.security.Principal;

/**
 * @author ggongaware@itensil.com
 */
public interface NodePermission {
		
	/**
     * @return for who?
     */
    public Principal getPrincipal();
    
	/**
     * @return create new nodes?
     */
    public boolean canCreate();
    
	/**
     * @return change permissions?
     */
    public boolean canManage();
    
	/**
     * @return read node?
     */
    public boolean canRead();
    
	/**
     * @return edit node?
     */
    public boolean canWrite();
    
	/**
     * @return deep?
     */
    public boolean isInheritable();
    
    /**
     * @return no permissions
     */
    public boolean isNone();

    /**
     * 
     * @return
     */
    public boolean isRelativeRole();
    
    /**
     * 
     * @return
     */
    public String getRole();
    
    /**
     * 
     * @return
     */
    public GroupAxis getAxis();
    
}
