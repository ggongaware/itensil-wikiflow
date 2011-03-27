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

import itensil.security.*;
import itensil.uidgen.IUID;
import itensil.uidgen.IUIDGenerator;
import itensil.io.HibernateUtil;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SignOnHB implements SignOn {

    protected static Logger logger = Logger.getLogger(SignOnHB.class);
    static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            logger.error(e);
        }
    }

    public User authenticate(String userName, String password, UserSpace fromUSpace, String zone) throws SignOnException {

        HibernateUtil.beginTransaction();
        Query qry = HibernateUtil.getSession().getNamedQuery("SignOn.userByName");
        qry.setString("name", userName);
        qry.setMaxResults(1);

        UserEntity user = (UserEntity)qry.uniqueResult();
        if (user != null && validatePassword(user, password)) {

            // account testing goes here
        	if (fromUSpace != null) {
        		USpaceUserEntity uus = user.getUserSpaceUsers().get((UserSpaceEntity)fromUSpace);
        		user.setUSpaceUser(uus);
        	}
        	if (user.getUSpaceUser() == null) {
        		if (user.getUserSpaceUsers().isEmpty()) {
        			throw new SignOnException("User not in a community");
        		}
        		user.setUSpaceUser(user.getUserSpaceUsers().values().iterator().next());
        	}

        	// create valid token if required
        	// extra security around the sys admin, no token allowed
			if (!SysAdmin.isSysAdmin(user) && (user.getToken() == null
					|| user.getToken().length() != IUID.UUID_SIZE)) {
				IUIDGenerator idGen = new IUIDGenerator();
				user.setToken(idGen.createID().toUUID());
			}

        	user.setTimestamp(System.currentTimeMillis());
            
            user.setLastLogin(new Date());
            user.upLoginCount();
            HibernateUtil.getSession().update(user);

        } else {
            HibernateUtil.closeSession();
            throw new SignOnException("Username or password did not match");
        }
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
        return user;
    }

    public User authenticate(String token, UserSpace fromUSpace, String zone) throws SignOnException {

    	HibernateUtil.beginTransaction();
    	// must be read-only to prevent UserEntity Flag and Role from being updated and user access-rights changed for normal access
    	HibernateUtil.readOnlySession();
        Query qry = HibernateUtil.getSession().getNamedQuery("SignOn.userByToken");
        qry.setString("token", token);
        qry.setMaxResults(1);

        UserEntity user = (UserEntity)qry.uniqueResult();
        //TODO Change type to special only for token
        //TokenAuthenticatedUserEntity user = (TokenAuthenticatedUserEntity)qry.uniqueResult();
        if (user != null && !SysAdmin.isSysAdmin(user)) {

            // account testing goes here
        	if (fromUSpace != null) {
        		USpaceUserEntity uus = user.getUserSpaceUsers().get((UserSpaceEntity)fromUSpace);
        		user.setUSpaceUser(uus);
        	}
        	if (user.getUSpaceUser() == null) {
        		if (user.getUserSpaceUsers().isEmpty()) {
        			throw new SignOnException("id not in a community");
        		}
        		user.setUSpaceUser(user.getUserSpaceUsers().values().iterator().next());
        	}
        	
        	//TODO token authenticated users are guest. But should be switchable to basic authenticated
        	user.setFlagStr("guest");
        	HashSet<String> roles = new HashSet<String>();
        	//TODO user.setRoles requires use uSpaceUser != null to set role confirm logic
        	roles.add("guest");
    		user.setRoles(roles);

        } else {
            HibernateUtil.closeSession();
            throw new SignOnException("id did not match");
        }
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
        return user;
    }

    protected boolean validatePassword(UserEntity user, String password) {
        return Arrays.equals(user.getPasswordHash(), hashPassword(password));
    }

    //TODO add call to token generator to validate token avoid DNS attacks 
    protected boolean validateToken(String token) {
        return (token != null && token.length() > 0);
    }

    public User switchableUser(String userId, String zone) throws SignOnException {
        if (SysAdmin.isSysAdmin(SecurityAssociation.getUser())) {
            UserEntity user = (UserEntity) HibernateUtil.getSession().get(UserEntity.class, userId);
            if (user != null) user.setTimestamp(System.currentTimeMillis());
            return user;
        }
        throw new SignOnException("Access denied");
    }

    public User authenticateAnonymous(UserSpace fromUSpace, String zone) throws SignOnException {
        return null;  //TODO - workout anonymous system
    }

    public boolean changePassword(String userName, String oldPass, String newPass, String zone) throws SignOnException {
        if (SysAdmin.getUser().getName().equals(userName)) {
            logger.warn("[" + zone + "] - attempt to change sysAmin password");
            return false;
        }

        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("SignOn.userByName");
        qry.setString("name", userName);

        UserEntity user = (UserEntity)qry.uniqueResult();

        if (!PasswordGen.checkPassword(newPass))
            throw new SignOnException("Password must be 5-20 characters");
        if (newPass.equals(oldPass))
            throw new SignOnException("New Password cannot match old");

        if (user != null && validatePassword(user, oldPass)) {
            user.setPasswordHash(hashPassword(newPass));
            session.update(user);
            return true;
        } else {
            return false;
        }
    }

    public String resetPassword(String userName, String zone) throws SignOnException {

        if (SysAdmin.getUser().getName().equals(userName)) {
            logger.warn("[" + zone + "] - attempt to change sysAmin password");
            return "";
        }

        Session session = HibernateUtil.getSession();
        Query qry = session.getNamedQuery("SignOn.userByName");
        qry.setString("name", userName);

        UserEntity user = (UserEntity)qry.uniqueResult();
        if (user != null) {
            String newPass = PasswordGen.generatePassword();
            user.setPasswordHash(hashPassword(newPass));
            session.update(user);
            return newPass;
        }
        return null;
    }

    public static byte[] hashPassword(String password) {
        return md.digest(password.getBytes());
    }
}
