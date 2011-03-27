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
public interface NodeVersion {
    
    /**
     * Is this the default active version
     * @return  true if active version
     */
    public boolean isDefault();
    
    /**
     * @return revision number ex. "2.3"
     */
    public String getNumber();
    
    /**
     * @return version label ex. "q4-release"
     */
    public String getLabel();
    
}
