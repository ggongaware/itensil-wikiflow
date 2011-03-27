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
package itensil.workflow.state;


/**
 * @author ggongaware@itensil.com
 *
 *
 *
 */
public class StateException extends Exception  {

    public StateException(String s) {
        super(s);
    }

    public StateException(Throwable ex) {
        super(ex);
    }

    public StateException(String s, Throwable ex) {
        super(s, ex);
    }
}
