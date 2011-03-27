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
public class SignOnException extends Exception {

    private String category;

    public SignOnException(String msg) {
        this("auth", msg);
    }

    public SignOnException(String category, String msg) {
        super(msg);
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

}
