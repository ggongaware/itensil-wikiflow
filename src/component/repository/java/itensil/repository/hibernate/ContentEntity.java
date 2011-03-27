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

import itensil.repository.NodeVersion;
import itensil.repository.NodeContent;
import itensil.repository.RepositoryException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import org.hibernate.Hibernate;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ContentEntity implements NodeContent, Serializable {

    private VersionEntity versionEntity;
    private byte [] bytes = new byte[0];
    private int length;

    public ContentEntity() {
    }

    public VersionEntity getVersionEntity() {
        return versionEntity;
    }

    public void setVersionEntity(VersionEntity versionEntity) {
        this.versionEntity = versionEntity;
    }

    public int getLength() {
        return length;
    }

    public InputStream getStream() {
    	return new ByteArrayInputStream(getBytes());
    }

    public NodeVersion getVersion() {
        return versionEntity;
    }

    public byte [] getBytes() {
        return bytes;
    }
    
    public void setBytes(byte bytes[]) {
    	this.bytes = bytes;
    }

    public void replaceContent(NodeContent nCont) {
        setLength(nCont.getLength());
        setBytes(nCont.getBytes());
    }

    public void setLength(int length) {
        this.length = length;
    }

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((versionEntity == null) ? 0 : versionEntity.hashCode());
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
		final ContentEntity other = (ContentEntity) obj;
		if (versionEntity == null) {
			if (other.versionEntity != null)
				return false;
		} else if (!versionEntity.equals(other.versionEntity))
			return false;
		return true;
	}
    
}
