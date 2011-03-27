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

/**
 * @author ggongaware@itensil.com
 *
 */
public interface SignOn {

    /**
     * @param userName
     * @param password
     * @param fromUSpace - maybe null
     * @return
     * @throws SignOnException
     */
    public User authenticate(String userName, String password, UserSpace fromUSpace, String zone) throws SignOnException;

    /**
     * @param token
     * @param fromUSpace - maybe null
     * @return
     * @throws SignOnException
     */
    public User authenticate(String token, UserSpace fromUSpace, String zone) throws SignOnException;

    /**
     * System admin can retrieve an authenticated User for other usernames
     *
     * @param userId
     * @return authenicated user
     * @throws SignOnException
     */
    public User switchableUser(String userId, String zone) throws SignOnException;

    /**
     * @param fromUSpace required
     * 
     * @return
     * @throws SignOnException
     */
    public User authenticateAnonymous(UserSpace fromUSpace, String zone) throws SignOnException;


    /**
     * @param userName
     * @param oldPass
     * @param newPass
     * @return
     * @throws SignOnException
     */
    public boolean changePassword(
        String userName,
        String oldPass,
        String newPass,
        String zone)
        throws SignOnException;

    /**
     * @param userName
     * @return
     * @throws SignOnException
     */
    public String resetPassword(String userName, String zone) throws SignOnException;

}

