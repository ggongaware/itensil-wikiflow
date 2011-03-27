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
 * Created on Nov 13, 2003
 *
 */
package itensil.security;

import java.util.Set;
import java.util.Locale;
import java.util.TimeZone;


/**
 * @author ggongaware@itensil.com
 *
 */
public interface UserSpace {

    /**
     * @return id object
     */
    public String getUserSpaceId();

    /**
     * @return string name
     */
    public String getName();


    /**
     * @return string base URL
     */
    public String getBaseUrl();
    
    
    /**
     * @return active status
     */
    public boolean isDisabled();
    
    /**
     * @return branding
     */
    public String getBrand();
    
    
    /**
     * 
     * @return
     */
    public Set<String> getFeatures();
    

    /**
     * @return users
     */
    public Set<? extends User> getUsers() throws UserSpaceException;

    /**
     * @param userId
     * @return user object
     */
    public User getUser(String userId) throws UserSpaceException;

    /**
     *
     * @param userName
     * @param simpleName
     * @param password
     * @param locale
     * @param timezone
     * @return user object
     */
    public User createUser(
            String userName, String simpleName, String password, Set<String> roles, Locale locale, TimeZone timezone)
        throws UserSpaceException;

    /**
     *
     * @param user
     * @throws UserSpaceException
     */
    public void removeUser(User user) throws UserSpaceException;

    /**
     * @param user
     * @return groups
     */
    public Set<? extends Group> getGroupsForUser(User user) throws UserSpaceException;

    /**
     * @param group
     * @return users
     */
    public Set<? extends User> getGroupUsers(Group group) throws UserSpaceException;
    
    
    /**
     * 
     * @param contextGroup
     * @param axis
     * @param role
     * @return
     * @throws UserSpaceException
     */
    public Set<? extends User> findGroupRoleUsers(Group contextGroup, GroupAxis axis, String role) throws UserSpaceException;

    /**
     *
     * @param group
     * @param user
     * @return true if in group
     */
    public boolean isUserInGroup(Group group, User user) throws UserSpaceException;

    /**
     * @param groupId
     * @return group object
     */
    public Group getGroup(String groupId) throws UserSpaceException;

    /**
     *
     * @param groupName
     * @param simpleName
     * @return group object
     */
    public Group createGroup(String groupName, String simpleName) throws UserSpaceException;

    /**
     *
     * @param group
     * @throws UserSpaceException
     */
    public void removeGroup(Group group) throws UserSpaceException;

    /**
     *
     * @param group
     * @param user
     * @throws UserSpaceException
     */
    public Object addGroupUser(Group group, User user) throws UserSpaceException;

    /**
     *
     * @param group
     * @param user
     * @throws UserSpaceException
     */
    public void removeGroupUser(Group group, User user) throws UserSpaceException;

    /**
     * @return all groups
     */
    public Set<? extends Group> getGroups() throws UserSpaceException;

    /**
     * @param group
     * @return group object
     */
    public Group resolve(Group group) throws UserSpaceException;

    /**
     * @param user
     * @return user object
     */
    public User resolve(User user) throws UserSpaceException;

    /**
     * @param user
     * @return role names
     */
    public Set<String> getUserRoles(User user) throws UserSpaceException;

    /**
     * 
     * @param user
     * @param roles
     * @throws UserSpaceException
     */
    public void setUserRoles(User user, Set<String> roles) throws UserSpaceException;
    
   /**
    * @return users
    */
   public Set<? extends User> getUsersInRole(String role) throws UserSpaceException;
   
   
   /**
    * 
    * @param group
    * @param user
    * @return role names
    */
   public Set<String> getGroupRoles(Group group, User user) throws UserSpaceException;
   
   /**
    * 
    * @param group
    * @param user
    * @return role names
    */
   public void setGroupRoles(Group group, User user, Set<String> roles) throws UserSpaceException;

}
