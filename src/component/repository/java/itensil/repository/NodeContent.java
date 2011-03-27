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
 * Created on Aug 27, 2003
 *
 */
package itensil.repository;

import java.io.InputStream;

/**
 * @author ggongaware@itensil.com
 */
public interface NodeContent {

	/**
     * @return byte size
     */
    public int getLength();
	
	/**
     * @return byte stream
     */
    public InputStream getStream();	
    
    /**
     * @return version related to this content
     */
    public NodeVersion getVersion();
    
    /**
     * @return byte array
     */
    public byte [] getBytes();
    	
}
