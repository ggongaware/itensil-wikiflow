package itensil.security.hibernate;

import itensil.util.StringHelper;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

public class GroupUserEntity implements Serializable {

	private GroupEntity groupEntity;
	private UserEntity userEntity;
	
	private String roleStr;
	private Set<String> roles;
	private Date joinTime;
	
	
	public GroupUserEntity() {
		
	}
	
	public GroupEntity getGroupEntity() {
		return groupEntity;
	}
	
	public void setGroupEntity(GroupEntity groupEntity) {
		this.groupEntity = groupEntity;
	}
	
	public Date getJoinTime() {
		return joinTime;
	}
	
	public void setJoinTime(Date joinTime) {
		this.joinTime = joinTime;
	}
	
	public String getRoleStr() {
		return roleStr;
	}
	
	public void setRoleStr(String roleStr) {
		roles = null;
		this.roleStr = roleStr;
	}
	
	public Set<String> getRoles() {
		if (roles == null) {
			roles  = StringHelper.setFromString(getRoleStr());
		}
		return roles;
	}

	
	public void setRoles(Set<String> roles) {
		this.roles = roles;
		roleStr = StringHelper.stringFromSet(roles);
	}
	
	
	public UserEntity getUserEntity() {
		return userEntity;
	}
	
	public void setUserEntity(UserEntity userEntity) {
		this.userEntity = userEntity;
	}

	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((groupEntity == null) ? 0 : groupEntity.hashCode());
		result = PRIME * result + ((userEntity == null) ? 0 : userEntity.hashCode());
		return result;
	}


	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GroupUserEntity other = (GroupUserEntity) obj;
		if (groupEntity == null) {
			if (other.groupEntity != null)
				return false;
		} else if (!groupEntity.equals(other.groupEntity))
			return false;
		if (userEntity == null) {
			if (other.userEntity != null)
				return false;
		} else if (!userEntity.equals(other.userEntity))
			return false;
		return true;
	}
}
