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

import itensil.util.Check;
import itensil.util.StringHelper;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class USpaceUserEntity implements Serializable {
	
	private UserEntity userEntity;
	private UserSpaceEntity userSpaceEntity;
	
	private String roleStr;
	private Date createTime;
	
	private Set<String> roles;
	
    private String custom1;
    private String custom2;
    private String custom3;
    private String custom4;
	
	public String getRoleStr() {
		return roleStr;
	}
	
	public void setRoleStr(String roleStr) {
		this.roleStr = Check.maxLength(roleStr, 255);
	}
	
	public Set<String> getRoles() {
		if (roles == null) {
			roles = StringHelper.setFromString(getRoleStr());
		}
		return roles;
	}

	
	public void setRoles(Set<String> roles) {
		this.roles = roles;
		roleStr = Check.maxLength(StringHelper.stringFromSet(roles), 255);
	}
	
	public UserEntity getUserEntity() {
		return userEntity;
	}
	
	public void setUserEntity(UserEntity userEntity) {
		this.userEntity = userEntity;
	}
	
	public UserSpaceEntity getUserSpaceEntity() {
		return userSpaceEntity;
	}
	
	public void setUserSpaceEntity(UserSpaceEntity userSpaceEntity) {
		this.userSpaceEntity = userSpaceEntity;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
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

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((userEntity == null) ? 0 : userEntity.hashCode());
		result = PRIME * result + ((userSpaceEntity == null) ? 0 : userSpaceEntity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final USpaceUserEntity other = (USpaceUserEntity) obj;
		if (userEntity == null) {
			if (other.userEntity != null)
				return false;
		} else if (!userEntity.equals(other.userEntity))
			return false;
		if (userSpaceEntity == null) {
			if (other.userSpaceEntity != null)
				return false;
		} else if (!userSpaceEntity.equals(other.userSpaceEntity))
			return false;
		return true;
	}
	
}
