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
import itensil.security.DefaultUser;
import itensil.security.DuplicatePrincipalException;
import itensil.security.Everyone;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.StringHelper;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UserSpaceEntity implements UserSpace, Serializable {

    public static final Set<String> ADMIN_ROLE = new HashSet<String>();
    protected static final Set<String> DEF_GROUPS = new HashSet<String>();
    static {
        ADMIN_ROLE.add("admin");
        
        DEF_GROUPS.add("Guests");
        DEF_GROUPS.add("Readers");
        DEF_GROUPS.add("Creators");
        DEF_GROUPS.add("Editors");
    }
    protected static final Everyone EVERYONE = new Everyone();


    protected Map<UserEntity, USpaceUserEntity> userSpaceUsers = new HashMap<UserEntity, USpaceUserEntity>();
    protected Set<GroupEntity> groupEntities = new HashSet<GroupEntity>();
    protected String name;
    protected String baseUrl;
    protected String userSpaceId;
    protected String brand;
    protected String featuresStr;
    protected Set<String> features;
    protected Date createTime;
    protected boolean disabled;
    
    protected String alertEmailer;

    public String getUserSpaceId() {
        return userSpaceId;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Set<? extends User> getUsers() throws UserSpaceException {
        checkAccess();
        Map<UserEntity, USpaceUserEntity> uspus = getUserSpaceUsers();
        return uspus.keySet();
    }

    public User getUser(String userId) throws UserSpaceException {
        checkAccess();
        return resolveUser(new DefaultUser(userId));
    }

    public User createUser(
            String userName, String simpleName, String password, Set<String> roles, Locale locale, TimeZone timezone)
        throws UserSpaceException {

        HashSet<String> inviteRoles = new HashSet<String>(ADMIN_ROLE);
        inviteRoles.add("inviter");

        checkAccess(inviteRoles);

        // check for duplicates
        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("USpace.anyUserByName");
        qry.setString("name", userName);
        UserEntity userEnt = (UserEntity)qry.uniqueResult();
        if (userEnt != null) {
        	if (userEnt.getUserSpaceUsers().containsKey(this))
        		throw new DuplicatePrincipalException("Duplicate user");
        } else {
        	
	        userEnt = new UserEntity();
	        userEnt.initNew();
	        userEnt.setUserName(userName);
	        userEnt.setSimpleName(simpleName);
	        userEnt.setLocale(locale);
	        userEnt.setTimeZone(timezone);
	        userEnt.setCreateTime(new Date());
	        userEnt.setPasswordHash(SignOnHB.hashPassword(password));
	        session.persist(userEnt);
        }
        
        USpaceUserEntity uus = new USpaceUserEntity();
        uus.setUserSpaceEntity(this);
        uus.setUserEntity(userEnt);
        uus.setRoleStr(StringHelper.stringFromSet(roles));
        uus.setCreateTime(new Date());
        session.persist(uus);
        
        if (userEnt.getUSpaceUser() == null) {
        	userEnt.setUSpaceUser(uus);
        }
        
        userEnt.getUserSpaceUsers().put(this, uus);
        
        Map<UserEntity, USpaceUserEntity> uspus = getUserSpaceUsers();
        if (Hibernate.isInitialized(uspus)) {
        	uspus.put(userEnt, uus);
        }
        userEnt.setActiveUserSpace(this);
        
        return userEnt;
    }

    public void removeUser(User user) throws UserSpaceException {
        checkAccess(ADMIN_ROLE);

        UserEntity usrEnt = resolveUser(user);
        if (usrEnt == null) throw new UserSpaceException("User not found");
        for (Group grp : getGroupsForUser(usrEnt)) {
        	removeGroupUser(grp, usrEnt);
        }
        USpaceUserEntity uus = usrEnt.getUserSpaceUsers().get(this);
        if (uus != null) {
        	
        	// load the full user (all userspaceuser records)
        	HibernateUtil.getSession().refresh(usrEnt);
        	
        	usrEnt.getUserSpaceUsers().remove(this);
        	HibernateUtil.getSession().delete(uus);

        	if (usrEnt.getUserSpaceUsers().isEmpty()) {
        		usrEnt.setUserName(usrEnt.getUserName() + usrEnt.getUserId());
        		usrEnt.setDeleted(true);
        	}
        }
        HibernateUtil.getSession().update(usrEnt);
        Map<UserEntity, USpaceUserEntity> uSpaceUserEntities = getUserSpaceUsers();
        if (Hibernate.isInitialized(uSpaceUserEntities)) {
        	uSpaceUserEntities.remove(usrEnt);
        }
    }

    public Set<? extends Group> getGroupsForUser(User user) throws UserSpaceException {
        checkAccess();
        UserEntity usrEnt = resolveUser(user);
        HashSet<Group> usGroups = new HashSet<Group>();
        if (usrEnt != null) {
        	for (GroupEntity grp : usrEnt.getGroupEntities()) {
        		 if (grp.getUserSpaceEntity() != null && grp.getUserSpaceEntity().equals(this)) {
        			 usGroups.add(grp);
        		 }
        	 }
        }
        return usGroups;
    }

    public Set<? extends User> getGroupUsers(Group group) throws UserSpaceException {
        checkAccess();
        GroupEntity groupEnt = resolveGroup(group);
        return groupEnt == null ? null : groupEnt.getUserEntities();
    }

    public boolean isUserInGroup(Group group, User user) throws UserSpaceException {
        checkAccess();
        if (EVERYONE.equals(group)) return true;
        UserEntity usrEnt = resolveUser(user);
        return usrEnt != null && usrEnt.isUserInGroup(group);
    }

    public Group getGroup(String groupId) throws UserSpaceException {
        checkAccess();
        return resolveGroup(new DefaultGroup(groupId));
    }

    public Group createGroup(String groupName, String simpleName) throws UserSpaceException {

    	HashSet<String> addGrpRoles = new HashSet<String>(ADMIN_ROLE);
        // special cases
        if (DEF_GROUPS.contains(simpleName)) {
            addGrpRoles.add("inviter");
        }
        checkAccess(addGrpRoles);

        // check for duplicates
        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("USpace.groupByName");
        qry.setString("name", groupName);
        qry.setEntity("uspace", this);
        if (qry.uniqueResult() != null) {
            throw new DuplicatePrincipalException("Duplicate group name");
        }

        GroupEntity groupEnt = new GroupEntity();
        groupEnt.initNew();
        groupEnt.setGroupName(groupName);
        groupEnt.setSimpleName(simpleName);
        groupEnt.setUserSpaceEntity(this);
        
        session.persist(groupEnt);
        Set<GroupEntity> groups = getGroupEntities();
        if (Hibernate.isInitialized(groups)) {
            groups.add(groupEnt);
        }
        return groupEnt;
    }

    public void removeGroup(Group group) throws UserSpaceException {
        checkAccess(ADMIN_ROLE);
        GroupEntity groupEnt = resolveGroup(group);
        if (groupEnt == null) throw new UserSpaceException("Group not found");
        for (UserEntity uEnt : groupEnt.getUserEntities()) {
            uEnt.getGroupEntities().remove(groupEnt);
        }
        HibernateUtil.getSession().delete(groupEnt);
        Set<GroupEntity> groups = getGroupEntities();
        if (Hibernate.isInitialized(groups)) {
            groups.remove(groupEnt);
        }
    }

    public Object addGroupUser(Group group, User user) throws UserSpaceException {

        HashSet<String> addGrpRoles = new HashSet<String>(ADMIN_ROLE);
        // special cases
        if (DEF_GROUPS.contains(group.getSimpleName())) {
            addGrpRoles.add("inviter");
        }
        checkAccess(addGrpRoles);
        GroupEntity groupEnt = resolveGroup(group);
        UserEntity usrEnt = resolveUser(user);
        Map<GroupEntity, GroupUserEntity> groupUserEntities = usrEnt.getGroupUserEntities();
        GroupUserEntity guEnt = groupUserEntities.get(groupEnt);
        if (guEnt == null) {
        	
        	guEnt = new GroupUserEntity();
        	guEnt.setGroupEntity(groupEnt);
        	guEnt.setUserEntity(usrEnt);
        	guEnt.setJoinTime(new Date());
        	
        	HibernateUtil.getSession().persist(guEnt);
        	
        	groupUserEntities.put(groupEnt, guEnt);

        	Map<UserEntity, GroupUserEntity> groupUserEntities2 = groupEnt.getGroupUserEntities();
            if (Hibernate.isInitialized(groupUserEntities2)) {
            	groupUserEntities2.put(usrEnt, guEnt);
            }
        }
        return guEnt;
    }

    public void removeGroupUser(Group group, User user) throws UserSpaceException {
        checkAccess(ADMIN_ROLE);
        GroupEntity groupEnt = resolveGroup(group);
        UserEntity usrEnt = resolveUser(user);
        
        Map<GroupEntity, GroupUserEntity> groupUserEntities = usrEnt.getGroupUserEntities();
        GroupUserEntity guEnt = groupUserEntities.get(groupEnt);
        if (guEnt != null) {
        	HibernateUtil.getSession().delete(guEnt);
        	groupUserEntities.remove(guEnt);
        	Map<UserEntity, GroupUserEntity> groupUserEntities2 = groupEnt.getGroupUserEntities();
            if (Hibernate.isInitialized(groupUserEntities2)) {
            	groupUserEntities2.remove(usrEnt);
            }
        }
    }

    public Set<? extends Group> getGroups() throws UserSpaceException {
        checkAccess();
        Set<GroupEntity> grps = getGroupEntities();
        return grps;
    }

    public Group resolve(Group group) throws UserSpaceException {
        checkAccess();
        return resolveGroup(group);
    }

    public User resolve(User user) throws UserSpaceException {
        checkAccess();
        return resolveUser(user);
    }

    public Set<String> getUserRoles(User user) throws UserSpaceException {
        checkAccess();
        UserEntity usrEnt = resolveUser(user);
        USpaceUserEntity uus = usrEnt.getUserSpaceUsers().get(this);
        return StringHelper.setFromString(uus.getRoleStr());
    }

    public void setUserRoles(User user, Set<String> roles) throws UserSpaceException {
        checkAccess(ADMIN_ROLE);
        UserEntity usrEnt = resolveUser(user);
        USpaceUserEntity uus = usrEnt.getUserSpaceUsers().get(this);
        uus.setRoleStr(StringHelper.stringFromSet(roles));
    }
    

	public Set<? extends User> getUsersInRole(String role) throws UserSpaceException {
		Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("USpace.usersByRole");        
        qry.setEntity("uspace", this);
        qry.setString("role", "%" + role + "%");
        List res = qry.list();
        HashSet<UserEntity> users = new HashSet<UserEntity>();
        for (Object obj : res) {
        	users.add((UserEntity)obj);
        }
		return users;
	}


    /*
     * Check if the user is part of the userspace and role check
     */
    public void checkAccess() throws UserSpaceException  {
        User caller = SecurityAssociation.getUser();
        if (!equals(caller.getUserSpace()) && !SysAdmin.isSysAdmin(caller)) {
            throw new UserSpaceException("Access denied");
        }
    }

    public void checkAccess(Set<String> roles) throws UserSpaceException  {
        checkAccess();
        User caller = SecurityAssociation.getUser();
        if (!SysAdmin.isSysAdmin(caller) && !isUserInAnyRole(resolveUser(caller), roles))
            throw new UserSpaceException("Role access denied");
    }

    protected boolean isUserInAnyRole(UserEntity user, Set<String> roles) throws UserSpaceException {
        if (SysAdmin.isSysAdmin(user)) {
            return true;
        }
        Set<String> uRols = user.getRoles();
        for (String tRol : roles) {
            if (uRols.contains(tRol)) return true;
        }
        return false;
    }

    protected GroupEntity resolveGroup(Group group) {
        if (group instanceof GroupEntity) {
            return (GroupEntity)group;
        }
        String groupId = group.getGroupId();
        Session session = HibernateUtil.getSession();
        Query qry;
        if (!Check.isEmpty(groupId)) {
            qry = session.getNamedQuery("USpace.groupById");
            qry.setString("id", groupId);
        } else {
            qry = session.getNamedQuery("USpace.groupByName");
            qry.setString("name", group.getName());
        }
        qry.setEntity("uspace", this);
        return (GroupEntity)qry.uniqueResult();
    }

    protected UserEntity resolveUser(User user) {
        if (user instanceof UserEntity) {
            return (UserEntity)user;
        }
        String userId = user.getUserId();
        Session session = HibernateUtil.getSession();
        Query qry;
        if (!Check.isEmpty(userId)) {
            qry = session.getNamedQuery("USpace.userById");
            qry.setString("id", userId);
        } else {
            qry = session.getNamedQuery("USpace.userByName");
            qry.setString("name", user.getName());
        }
        qry.setEntity("uspace", this);
        return (UserEntity)qry.uniqueResult();
    }

    public Map<UserEntity, USpaceUserEntity> getUserSpaceUsers() {
		return userSpaceUsers;
	}

	public void setUserSpaceUsers(
			Map<UserEntity, USpaceUserEntity> userSpaceUsers) {
		this.userSpaceUsers = userSpaceUsers;
	}

	public Set<GroupEntity> getGroupEntities() {
        return groupEntities;
    }

    public void setGroupEntities(Set<GroupEntity> groupEntities) {
        this.groupEntities = groupEntities;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getFeaturesStr() {
		return featuresStr;
	}

	public void setFeaturesStr(String featuresStr) {
		this.features = null;
		this.featuresStr = featuresStr;
	}

	public void setUserSpaceId(String userSpaceId) {
        this.userSpaceId = userSpaceId;
    }

    public static UserSpaceEntity createUserSpace(String name) {
        UserSpaceEntity uspace = new UserSpaceEntity();
        uspace.setName(name);
        uspace.setCreateTime(new Date());
        HibernateUtil.getSession().persist(uspace);
        return uspace;
    }
    
    public static UserSpaceEntity findUserSpaceByHost(String hostUrl) {
    	HibernateUtil.beginTransaction();
    	HibernateUtil.readOnlySession();
        Query qry = HibernateUtil.getSession().getNamedQuery("USpace.byHost");
        qry.setString("host", hostUrl);
        qry.setMaxResults(1);
        UserSpaceEntity uspace = (UserSpaceEntity)qry.uniqueResult();
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
        return uspace;
    }

    public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserSpace)) return false;

        final UserSpace that = (UserSpace) o;

        return userSpaceId.equals(that.getUserSpaceId());
    }

    public int hashCode() {
        return userSpaceId.hashCode();
    }

	public Set<String> getFeatures() {
		if (features == null) {
			features = StringHelper.setFromString(getFeaturesStr());
		}
		return features;
	}

	public String getAlertEmailer() {
		return alertEmailer;
	}

	public void setAlertEmailer(String alertEmailer) {
		this.alertEmailer = alertEmailer;
	}

	public Set<? extends User> findGroupRoleUsers(Group contextGroup, GroupAxis axis, String role) throws UserSpaceException {
		HashSet<UserEntity> roleUsers = new HashSet<UserEntity>();
		GroupEntity cGrp = resolveGroup(contextGroup);
		if (axis == GroupAxis.SELF || axis == GroupAxis.ANCESTOR_OR_SELF || axis == GroupAxis.CHILD_OR_SELF) {
			roleUsers = findGroupRoleUsers(cGrp, role);
		}
		if (roleUsers.isEmpty()) {
			if (axis == GroupAxis.PARENT) {
				cGrp = cGrp.getParentGroupEntity();
				if (cGrp != null) roleUsers = findGroupRoleUsers(cGrp, role);
			} else if (axis == GroupAxis.ANCESTOR_OR_SELF || axis == GroupAxis.ANCESTOR) {
				cGrp = cGrp.getParentGroupEntity();
				while (cGrp != null) {
					roleUsers = findGroupRoleUsers(cGrp, role);
					if (!roleUsers.isEmpty()) break;
					cGrp = cGrp.getParentGroupEntity();
				}
			} else if (axis == GroupAxis.CHILD || axis == GroupAxis.CHILD_OR_SELF) {
				
				// Sub Group query
				Query qry = HibernateUtil.getSession().getNamedQuery("USpace.subGroups");
				qry.setString("parentId", cGrp.getGroupId());
				for (Object kid : qry.list()) {
					roleUsers = findGroupRoleUsers((GroupEntity)kid, role);
					if (!roleUsers.isEmpty()) break;
				}
			} else if (axis == GroupAxis.SIBLING) {
				GroupEntity pGrp = cGrp.getParentGroupEntity();
				
				// Sub Group query
				Query qry = HibernateUtil.getSession().getNamedQuery("USpace.subGroups");
				qry.setString("parentId", pGrp != null ? pGrp.getGroupId() : null);
				for (Object kid : qry.list()) {
					if (!cGrp.equals(kid)) {
						roleUsers = findGroupRoleUsers((GroupEntity)kid, role);
						if (!roleUsers.isEmpty()) break;
					}
				}
			}
		}
		
		return roleUsers;
	}
	
	protected HashSet<UserEntity> findGroupRoleUsers(GroupEntity cGrp, String role) throws UserSpaceException {
		// Group Memeber query
		HashSet<UserEntity> roleUsers = new HashSet<UserEntity>();
		Query qry = HibernateUtil.getSession().getNamedQuery("USpace.groupUsersByRole");
		qry.setEntity("grp", cGrp);
		qry.setString("role", "%" + role + "%");
		for (Object obj : qry.list()) {
			GroupUserEntity gusr = (GroupUserEntity)obj;
			// recheck to remove partial string matches 
			if (gusr.getRoles().contains(role)) {
				roleUsers.add(gusr.getUserEntity());
			}
		}
		return roleUsers;
	}

	public Set<String> getGroupRoles(Group group, User user) throws UserSpaceException {
		GroupEntity groupEnt = resolveGroup(group);
        UserEntity usrEnt = resolveUser(user);
        Map<GroupEntity, GroupUserEntity> groupUserEntities = usrEnt.getGroupUserEntities();
        GroupUserEntity guser = groupUserEntities.get(groupEnt);
		return guser != null ? guser.getRoles() : null;
	}

	public void setGroupRoles(Group group, User user, Set<String> roles) throws UserSpaceException {
		GroupEntity groupEnt = resolveGroup(group);
        UserEntity usrEnt = resolveUser(user);
        Map<GroupEntity, GroupUserEntity> groupUserEntities = usrEnt.getGroupUserEntities();
        GroupUserEntity guser = groupUserEntities.get(groupEnt);
		if (guser != null) {
			guser.setRoleStr(StringHelper.stringFromSet(roles));
			HibernateUtil.getSession().update(guser);
		}
	}


}
