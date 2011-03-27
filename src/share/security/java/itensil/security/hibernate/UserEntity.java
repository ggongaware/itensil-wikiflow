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
import itensil.security.AuthenticatedUser;
import itensil.security.Everyone;
import itensil.security.Group;
import itensil.security.SignOnException;
import itensil.security.UserSpace;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.LocaleHelper;
import itensil.util.StringHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.hibernate.Session;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UserEntity extends AuthenticatedUser {

    private USpaceUserEntity uSpaceUser;
    private Map<UserSpaceEntity, USpaceUserEntity> userSpaceUsers;
    private Map<GroupEntity, GroupUserEntity> groupUserEntities;
    private byte[] passwordHash;
    private String token;
    private String email;
    private String remoteKey;
    private Date createTime;
    private Date lastLogin;
    private int loginCount;
    private String flagStr;
    private boolean deleted;
    private transient String userSpaceId;

    public UserEntity() {
    }
    
    public void initNew() {
    	userSpaceUsers = new HashMap<UserSpaceEntity, USpaceUserEntity>();
    	groupUserEntities = new HashMap<GroupEntity, GroupUserEntity>();
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = Check.maxLength(userName, 255);
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = Check.maxLength(simpleName, 255);
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Set<GroupEntity> getGroupEntities() {
        return getGroupUserEntities().keySet();
    }

    /*
    * @see itensil.security.User#isUserInGroup(itensil.security.Group)
    */
    public boolean isUserInGroup(Group group) {
        if ((new Everyone()).equals(group)) return true;
        connectUser();
        Map<GroupEntity, GroupUserEntity> guEnts = getGroupUserEntities();
        Group rGrp;
        try {
            rGrp = getUserSpace().resolve(group);
        } catch (UserSpaceException e) {
            return false;
        }
        // fixes a org.hibernate.LazyInitializationException: illegal access to loading collection
        synchronized (guEnts) {
        	return rGrp != null && guEnts.containsKey(rGrp);
        }
    }

    private void connectUser() {
        Session session = HibernateUtil.getSession();
        if (!session.contains(this)) {
            session.refresh(this);
        }
    }

    /*
    * @see itensil.security.User#getGroups()
    */
    public Group[] getGroups() {
        connectUser();
        Set<GroupEntity> grps = getGroupEntities();
        return grps.toArray(new Group[grps.size()]);
    }

    public USpaceUserEntity getUSpaceUser() {
		return uSpaceUser;
	}

	public void setUSpaceUser(USpaceUserEntity uSpaceUser) {
		if (uSpaceUser == null) {
			userSpaceId = null; 
		} else {
			userSpaceId = uSpaceUser.getUserSpaceEntity().getUserSpaceId();
		}
		this.uSpaceUser = uSpaceUser;
	}
	
	
	
    public Set<String> getRoles() {
		return uSpaceUser == null ? null : uSpaceUser.getRoles();
	}

	public void setRoles(Set<String> roles) {
		if (uSpaceUser != null) uSpaceUser.setRoles(roles);
	}

	public String getCustom1() {
		return uSpaceUser == null ? null : uSpaceUser.getCustom1();
	}

	public String getCustom2() {
		return uSpaceUser == null ? null : uSpaceUser.getCustom2();
	}

	public String getCustom3() {
		return uSpaceUser == null ? null : uSpaceUser.getCustom3();
	}

	public String getCustom4() {
		return uSpaceUser == null ? null : uSpaceUser.getCustom4();
	}

	public void setCustom1(String custom1) {
		if (uSpaceUser != null) uSpaceUser.setCustom1(custom1);
	}

	public void setCustom2(String custom2) {
		if (uSpaceUser != null) uSpaceUser.setCustom2(custom2);
	}

	public void setCustom3(String custom3) {
		if (uSpaceUser != null) uSpaceUser.setCustom3(custom3);
	}

	public void setCustom4(String custom4) {
		if (uSpaceUser != null) uSpaceUser.setCustom4(custom4);
	}

	public String getTimezoneStr() {
        return timeZone == null ? null : timeZone.getID();
    }

    public void setTimezoneStr(String timezoneStr) {
        timeZone = LocaleHelper.readTimeZone(timezoneStr);
    }

    public String getLocaleStr() {
        return locale == null ? null : locale.toString();
    }

    public void setLocaleStr(String localeStr) {
        locale = LocaleHelper.readLocal(localeStr);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getUserSpaceId() {
        if (userSpaceId == null) {
            userSpaceId = getUserSpace().getUserSpaceId().intern();
        }
        return userSpaceId;
    }

    public UserSpace getUserSpace() {
        if (uSpaceUser == null) {
        	if (getUserSpaceUsers().isEmpty()) {
    			return null;
    		}
    		return getUserSpaceUsers().entrySet().iterator().next().getKey();
        }
        UserSpaceEntity uspaceEnt = uSpaceUser.getUserSpaceEntity();
        return uspaceEnt;
    }
    
	public void setActiveUserSpace(UserSpace uspace) {
		setUSpaceUser(getUserSpaceUsers().get(uspace));
	}

	public Map<UserSpaceEntity, USpaceUserEntity> getUserSpaceUsers() {
		return userSpaceUsers;
	}

	public void setUserSpaceUsers(
			Map<UserSpaceEntity, USpaceUserEntity> userSpaceUsers) {
		this.userSpaceUsers = userSpaceUsers;
	}

	public String getFlagStr() {
        return flagStr;
    }

    public void setFlagStr(String flagStr) {
        this.flagStr = Check.maxLength(flagStr, 255);
        flags = StringHelper.setFromString(this.flagStr);
    }

    public void setFlags(Set<String> flags) {
        this.flags = flags;
        flagStr = StringHelper.stringFromSet(flags);
    }

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = Check.maxLength(email,128);
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public int getLoginCount() {
		return loginCount;
	}

	public void setLoginCount(int loginCount) {
		this.loginCount = loginCount;
	}

	public String getRemoteKey() {
		return remoteKey;
	}

	public void setRemoteKey(String remoteKey) {
		this.remoteKey = remoteKey;
	}

	public void setUserSpaceId(String userSpaceId) {
		this.userSpaceId = userSpaceId;
	}
	

	public Map<GroupEntity, GroupUserEntity> getGroupUserEntities() {
		return groupUserEntities;
	}

	public void setGroupUserEntities(
			Map<GroupEntity, GroupUserEntity> groupUserEntities) {
		this.groupUserEntities = groupUserEntities;
	}

	public AuthenticatedUser getReference() {
		UserEntity uEnt = new UserEntity();
		uEnt.setUserId(getUserId());
		uEnt.setUserName(getUserName());
		uEnt.setGroupUserEntities(getGroupUserEntities());
		uEnt.setUserSpaceUsers(getUserSpaceUsers());
		uEnt.setTimezoneStr(getTimezoneStr());
		uEnt.setLocaleStr(getLocaleStr());
		uEnt.setFlagStr(getFlagStr());
		uEnt.setSimpleName(getSimpleName());
		uEnt.setEmail(getEmail());
		uEnt.setCreateTime(getCreateTime());
		uEnt.setRemoteKey(getRemoteKey());
		return uEnt; 
	}

	public void upLoginCount() {
		loginCount++;
	}

}
