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
public class AccessDeniedException extends Exception {

    static final long serialVersionUID = 1079554220539L;


	protected String id;
	protected String required;

	public AccessDeniedException(String id, String required) {
		super("Access Denied on '" + id + "' for " + required);
		this.id = id;
		this.required = required;
	}


	/**
	 * @return action attempted
	 */
	public String getRequired() {
		return required;
	}

	/**
	 * @return id of resource
	 */
	public String getId() {
		return id;
	}

}
