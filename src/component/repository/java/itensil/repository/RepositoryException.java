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
 * Created on Nov 17, 2003
 *
 */
package itensil.repository;


/**
 * @author ggongaware@itensil.com
 *
 */
public class RepositoryException extends RuntimeException {

    /**
     * @param message
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public RepositoryException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }

}
