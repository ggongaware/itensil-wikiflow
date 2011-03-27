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

import itensil.repository.DefaultNodeLock;
import itensil.security.User;
import itensil.security.DefaultUser;

import java.util.Date;

/**
 * @author ggongaware@itensil.com
 *
 */
public class LockEntity extends DefaultNodeLock {

    private NodeEntity nodeEntity;
    private String ownerId;

    public LockEntity() {
        super(null);
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public NodeEntity getNodeEntity() {
        return nodeEntity;
    }

    public void setNodeEntity(NodeEntity nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void setInheritable(boolean inheritable) {
        this.inheritable = inheritable;
    }

    public void setOwnerInfo(String ownerInfo) {
        this.ownerInfo = ownerInfo;
    }

    public User getOwner() {
        if (owner == null) owner = new DefaultUser(getOwnerId());
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
        setOwnerId(owner.getUserId());
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

}
