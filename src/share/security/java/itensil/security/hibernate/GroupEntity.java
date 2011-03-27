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
package itensil.security.hibernate;

import itensil.io.HibernateUtil;
import itensil.security.DefaultGroup;
import itensil.util.Check;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * @author ggongaware@itensil.com
 *
 */
public class GroupEntity extends DefaultGroup {

    private UserSpaceEntity userSpaceEntity;
    private Map<UserEntity, GroupUserEntity> groupUserEntities;
    private int groupType;
    private String parentGroupId;
    private String remoteKey;
    private Date createTime;
    
    private String custom1;
    private String custom2;
    private String custom3;
    private String custom4;

    public GroupEntity() {
        super(null);
    }
    
    public void initNew() {
    	groupUserEntities = new HashMap<UserEntity, GroupUserEntity>();
    	createTime = new Date();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = Check.maxLength(groupName, 128);
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = Check.maxLength(simpleName, 46);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public int getGroupType() {
		return groupType;
	}

	public void setGroupType(int groupType) {
		this.groupType = groupType;
	}

	public GroupEntity getParentGroupEntity() {
		return parentGroupId != null ? (GroupEntity)HibernateUtil.getSession().get(
				GroupEntity.class, parentGroupId) : null;
	}

	public String getRemoteKey() {
		return remoteKey;
	}

	public void setRemoteKey(String remoteKey) {
		this.remoteKey = Check.maxLength(remoteKey, 128);
	}

	public UserSpaceEntity getUserSpaceEntity() {
        return userSpaceEntity;
    }

    public void setUserSpaceEntity(UserSpaceEntity userSpaceEntity) {
        this.userSpaceEntity = userSpaceEntity;
    }

    public Set<UserEntity> getUserEntities() {
        return getGroupUserEntities().keySet();
    }

	public Map<UserEntity, GroupUserEntity> getGroupUserEntities() {
		return groupUserEntities;
	}

	public void setGroupUserEntities(
			Map<UserEntity, GroupUserEntity> groupUserEntities) {
		this.groupUserEntities = groupUserEntities;
	}

	public String getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(String parentGroupId) {
		this.parentGroupId = parentGroupId;
	}

	public String getCustom1() {
		return custom1;
	}

	public void setCustom1(String custom1) {
		this.custom1 = Check.maxLength(custom1, 255);
	}

	public String getCustom2() {
		return custom2;
	}

	public void setCustom2(String custom2) {
		this.custom2 = Check.maxLength(custom2, 255);
	}

	public String getCustom3() {
		return custom3;
	}

	public void setCustom3(String custom3) {
		this.custom3 = Check.maxLength(custom3, 255);
	}

	public String getCustom4() {
		return custom4;
	}

	public void setCustom4(String custom4) {
		this.custom4 = Check.maxLength(custom4, 255);
	}

}
