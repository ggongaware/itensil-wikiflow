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
import itensil.util.UriHelper;
import itensil.util.WildcardPattern;

public class MatchCollectionFilter implements NodeFilter {
	
	WildcardPattern pattern;
	
	public MatchCollectionFilter(WildcardPattern pattern) {
		this.pattern = pattern;
	}

	public boolean accept(RepositoryNode node) {
		if (node.isCollection()) {
			String fName = UriHelper.name(node.getUri());
			return pattern.match(fName);
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((pattern == null) ? 0 : pattern.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final MatchCollectionFilter other = (MatchCollectionFilter) obj;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		return true;
	}
	
}
