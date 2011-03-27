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
package itensil.security;

import junit.framework.TestCase;
import itensil.io.HibernateUtil;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;

import java.util.*;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UserSpaceJunit extends TestCase {

    String userName;
    static Set<String> adminrole = new HashSet<String>();
    static {
        adminrole.add("admin");
    }

    protected void setUp() throws Exception {

        // local mode
        userName = "junit" + System.currentTimeMillis();
    }

    public void testUserSpaceInit() throws Exception {

        SecurityAssociation.setUser(SysAdmin.getUser());

        HibernateUtil.beginTransaction();
        String name = "junit" + System.currentTimeMillis();
        UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        DefaultGroup testGroup = new DefaultGroup("junit");
        DefaultUser user = new DefaultUser("junit");
        assertEquals(uspace.getName(), name);
        assertTrue(uspace.getUsers().isEmpty());
        assertTrue(uspace.getGroups().isEmpty());
        assertNull(uspace.getUser("junit"));
        assertNull(uspace.getGroup("junit"));
        assertFalse(uspace.isUserInGroup(testGroup,user));
        assertTrue(uspace.getGroupsForUser(user).isEmpty());
        assertNull(uspace.getGroupUsers(testGroup));
        assertNull(uspace.resolve(testGroup));
        assertNull(uspace.resolve(user));
        assertTrue(uspace.isUserInGroup(new Everyone(), user));

        


        HibernateUtil.beginTransaction();
        User cUser =
            uspace.createUser(userName, "junit", "pass", adminrole, Locale.getDefault(), TimeZone.getDefault());
        SecurityAssociation.setUser(cUser);
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(uspace);
        assertEquals(1, uspace.getUsers().size());
        assertTrue(adminrole.containsAll(uspace.getUserRoles(cUser)));
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        uspace.removeUser(cUser);
        HibernateUtil.commitTransaction();
    }

    public void testUserSpaceGroups() throws Exception {
        SecurityAssociation.setUser(SysAdmin.getUser());

        HibernateUtil.beginTransaction();
        String name = "junit" + System.currentTimeMillis();
        UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
        User guy = uspace.createUser(userName, "junit", "pass", adminrole, Locale.getDefault(), TimeZone.getDefault());
        HibernateUtil.commitTransaction();

        SecurityAssociation.setUser(guy);
        HibernateUtil.beginTransaction();
        assertEquals(1, uspace.getUsers().size());
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        assertTrue(uspace.getUsers().contains(guy));
        Group testGroup = uspace.createGroup("Junit Group", "Junit");
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        assertTrue(uspace.getGroups().contains(testGroup));

        //resolve people
        User sameGuy = uspace.resolve(guy);
        assertEquals(sameGuy, guy);
        Group sameGroup = uspace.resolve(testGroup);
        assertEquals(sameGroup, testGroup);
        assertFalse(uspace.isUserInGroup(testGroup, guy));
        assertEquals(guy, uspace.getUser(guy.getUserId()));
        assertEquals(guy, uspace.resolve(new DefaultUser(null, userName)));
        assertEquals(testGroup, uspace.getGroup(testGroup.getGroupId()));
        assertEquals(testGroup, uspace.resolve(new DefaultGroup(null, "Junit Group")));

        //join group
        uspace.addGroupUser(testGroup, guy);
        assertTrue(uspace.getGroupsForUser(guy).contains(testGroup));
        assertTrue(uspace.getGroupUsers(testGroup).contains(guy));
        assertTrue(uspace.isUserInGroup(testGroup, guy));
        
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(guy);
        
        //leave group
        uspace.removeGroupUser(testGroup, guy);
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(uspace);
        HibernateUtil.getSession().refresh(guy);
        assertFalse(uspace.isUserInGroup(testGroup, guy));
        assertFalse(uspace.getGroupUsers(testGroup).contains(guy));
        assertFalse(uspace.getGroupsForUser(guy).contains(testGroup));

        //remove group
        uspace.removeGroup(testGroup);
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(uspace);
        assertTrue(uspace.getGroups().isEmpty());
        HibernateUtil.commitTransaction();

        //remove user
        HibernateUtil.beginTransaction();
        uspace.removeUser(guy);
        HibernateUtil.commitTransaction();
    }
    
    public void testMultiUserSpace() throws Exception {
    	 SecurityAssociation.setUser(SysAdmin.getUser());

         HibernateUtil.beginTransaction();
         long idMod = System.currentTimeMillis();
         String name = "junit" + idMod;
         String name2 = "junit2" + idMod;
         UserSpaceEntity uspace = UserSpaceEntity.createUserSpace(name);
         UserEntity guy = 
        	 (UserEntity)
        	 	uspace.createUser(userName, "junit", "pass", adminrole, Locale.getDefault(), TimeZone.getDefault());
         HibernateUtil.commitTransaction();
         
         HibernateUtil.beginTransaction();
         UserSpaceEntity uspace2 = UserSpaceEntity.createUserSpace(name2);
         uspace2.createUser(userName, "junit", "pass", adminrole, Locale.getDefault(), TimeZone.getDefault());
         HibernateUtil.commitTransaction();
         
         
         HibernateUtil.beginTransaction();
         HibernateUtil.getSession().refresh(guy);
         assertEquals(2, guy.getUserSpaceUsers().size());
         assertTrue(guy.getUserSpaceUsers().containsKey(uspace));
         assertTrue(guy.getUserSpaceUsers().containsKey(uspace2));
         HibernateUtil.getSession().refresh(uspace);
         uspace.removeUser(guy);
         HibernateUtil.commitTransaction();
         
         HibernateUtil.beginTransaction();
         HibernateUtil.getSession().refresh(guy);
         assertEquals(1, guy.getUserSpaceUsers().size());
         assertFalse(guy.getUserSpaceUsers().containsKey(uspace));
         assertTrue(guy.getUserSpaceUsers().containsKey(uspace2));
         HibernateUtil.getSession().refresh(uspace2);
         uspace2.removeUser(guy);
         HibernateUtil.commitTransaction();
         
         HibernateUtil.beginTransaction();
         HibernateUtil.getSession().refresh(guy);
         assertEquals(0, guy.getUserSpaceUsers().size());
         HibernateUtil.commitTransaction();
    }

    /*
    * @see TestCase#tearDown()
    */
    protected void tearDown() throws Exception {
        SecurityAssociation.clear();
    }

}
