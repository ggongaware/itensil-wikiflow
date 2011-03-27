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

import java.util.Date;

import itensil.security.User;

/**
 * @author ggongaware@itensil.com
 */
public interface NodeLock {

	/**
     * @return Unique id lock
     */
    public String getLockId();    

	/**
     * @return who locked
     */
    public User getOwner();

    /**
     * @return lock time out
     */
    public Date getExpireTime();

	/**
     * @return shared lock?
     */
    public boolean isExclusive();

	/**
     * @return deep lock
     */
    public boolean isInheritable();

    /**
     * External owner info from <d:owner>
     * @return string
     */
    public String getOwnerInfo();
}
