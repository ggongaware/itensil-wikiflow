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
package itensil.repository.filter;

import itensil.repository.RepositoryNode;

public interface NodeFilter {
	
	/**
	 * Tests whether or not the specified node should be inlcuded or excluded
	 * 
	 * @param node
	 * @return true to include
	 */
	boolean accept(RepositoryNode node);
}
