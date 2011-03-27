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

import java.security.Principal;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface Group extends Principal {

    /**
     * @return unqiue group id
     */
    public String getGroupId();

    /**
     * @return easier to read name string
     */
    public String getSimpleName();

    /**
     * @return last modified time
     */
    public long timeStamp();

}
