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
package itensil.security.hibernate;

import java.io.Serializable;


public class AppComponentMap implements Serializable {
	
	/* unique db id */
	String mapId;
	
    String appId;
    
    String appProcessId;

    String itenOwnerId;
    
    String itenComponentName;
    
    String itenComponentType;
    
    String itenComponentId;
	
	public AppComponentMap() {
		
	}

	
	/**
	 * 
	 * @param appId- the application making the call 
	 * @param appProcessId - unique id process in application
	 * @param user - user that created this map
	 * @param recordId - 
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * 
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * @throws LockException
	 * @throws DuplicateException
	 * 
	 */

	/*
	public AppComponentMap(String appId,
			String appProcessId, String userId, String itenComponentType, String itenComponentName, String itenComponentId) {


			this.appId = appId;
			this.appProcessId = appProcessId;
			this.itenOwnerId = userId;
			this.itenComponentType = itenComponentType;
			this.itenComponentName = itenComponentName;
			this.itenComponentId = itenComponentId;
	}
	
*/
	
	public String getMapId() {
		return mapId;
	}


	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getAppId() {
		return appId;
	}


	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppProcessId() {
		return appProcessId;
	}

	public void setAppProcessId(String appProcessId) {
		this.appProcessId = appProcessId;
	}

	public String getItenOwnerId() {
		return itenOwnerId;
	}

	public void setItenOwnerId(String itenOwnerId) {
		this.itenOwnerId = itenOwnerId;
	}

	public String getItenComponentName() {
		return itenComponentName;
	}

	public void setItenComponentName(String itenComponentName) {
		this.itenComponentName = itenComponentName;
	}

	public String getItenComponentType() {
		return itenComponentType;
	}

	public void setItenComponentType(String itenComponentType) {
		this.itenComponentType = itenComponentType;
	}

	public String getItenComponentId() {
		return itenComponentId;
	}

	public void setItenComponentId(String itenComponentId) {
		this.itenComponentId = itenComponentId;
	}
	
/*
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (appEntityId ^ (appEntityId >>> 32));
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
		final AppEntityRecord other = (AppEntityRecord) obj;
		if (appEntityId != other.appEntityId)
			return false;
		return true;
	}

*/
	
}


