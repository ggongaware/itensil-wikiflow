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
 * Created on Jan 5, 2004
 *
 */
package itensil.repository;

import itensil.security.User;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultNodeLock implements NodeLock, Serializable {


    protected String lockId;
    protected User owner;
    protected Date expireTime;
    protected boolean exclusive;
    protected boolean inheritable;
    protected String ownerInfo;

    /**
     * For renewing
     * @param lock
     * @param expireTime
     */
    public DefaultNodeLock(
        NodeLock lock,
        Date expireTime) {

        this(
            lock.getLockId(),
            lock.getOwner(),
            expireTime,
            lock.isExclusive(),
            lock.isInheritable(),
            lock.getOwnerInfo()
        );
    }

    /**
     * For referencing
     * @param lockId
     * @param owner
     */
    public DefaultNodeLock(String lockId, User owner) {
        this (lockId, owner, null, false, false, "");
    }

    /**
     * For referencing
     * @param lockId
     */
    public DefaultNodeLock(String lockId) {
        this (lockId, null, null, false, false, "");
    }

    /**
     * @param lockId
     * @param owner
     * @param expireTime
     * @param exclusive
     * @param inheritable
     */
    public DefaultNodeLock(
        String lockId,
        User owner,
        Date expireTime,
        boolean exclusive,
        boolean inheritable,
        String ownerInfo) {

        this.lockId = lockId;
        this.owner = owner;
        this.expireTime = expireTime;
        this.exclusive = exclusive;
        this.inheritable = inheritable;
        this.ownerInfo = ownerInfo;
    }

    /*
     * @see NodeLock#getLockId()
     */
    public String getLockId() {
        return lockId;
    }

    /*
     * @see NodeLock#getOwner()
     */
    public User getOwner() {
        return owner;
    }

    /*
     * @see NodeLock#getExpireTime()
     */
    public Date getExpireTime() {
        return expireTime;
    }


    /*
     * @see NodeLock#isExclusive()
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /*
     * @see NodeLock#isInheritable()
     */
    public boolean isInheritable() {
        return inheritable;
    }

    public String getOwnerInfo() {
        return ownerInfo;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof NodeLock) {
            return getLockId().equals(((NodeLock)obj).getLockId());
        }
        return false;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getLockId().hashCode();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Lock " + getLockId();
    }

}
