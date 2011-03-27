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
package itensil.repository.hibernate;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Mount implements Serializable {

    private String userSpaceId;
    private RepositoryEntity repoEntity;
    private boolean primary;

    public String getUserSpaceId() {
        return userSpaceId;
    }

    public void setUserSpaceId(String userSpaceId) {
        this.userSpaceId = userSpaceId;
    }

    public RepositoryEntity getRepoEntity() {
        return repoEntity;
    }

    public void setRepoEntity(RepositoryEntity repoEntity) {
        this.repoEntity = repoEntity;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((repoEntity == null) ? 0 : repoEntity.hashCode());
		result = PRIME * result + ((userSpaceId == null) ? 0 : userSpaceId.hashCode());
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
		final Mount other = (Mount) obj;
		if (repoEntity == null) {
			if (other.repoEntity != null)
				return false;
		} else if (!repoEntity.equals(other.repoEntity))
			return false;
		if (userSpaceId == null) {
			if (other.userSpaceId != null)
				return false;
		} else if (!userSpaceId.equals(other.userSpaceId))
			return false;
		return true;
	}
    
}
