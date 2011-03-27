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

/**
 * @author ggongaware@itensil.com
 */
public class DuplicateException extends Exception {

	protected String id;

	public DuplicateException(String id) {
		super("'" + id + "' already exists");
	}



	/**
	 * @return duplicated id
	 */
	public String getID() {
		return id;
	}

}
