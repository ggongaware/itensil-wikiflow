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
package itensil.repository.event;

public interface ContentChangeListener {

	/**
	 * Invoked when an event is dispatched.
	 * 
	 * @param evt
	 */
	public void contentChanged(ContentEvent evt);
	
}
