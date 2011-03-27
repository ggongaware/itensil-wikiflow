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
 * Created on Dec 12, 2003
 *
 */
package itensil.security;


/**
 * @author ggongaware@itensil.com
 *
 */
public class UserSpaceException extends Exception {

    /**
     * @param s
     */
    public UserSpaceException(String s) {
        super(s);
    }

    /**
     * @param ex
     */
    public UserSpaceException(Throwable ex) {
        super(ex);
    }

    /**
     * @param s
     * @param ex
     */
    public UserSpaceException(String s, Throwable ex) {
        super(s, ex);
    }

}
