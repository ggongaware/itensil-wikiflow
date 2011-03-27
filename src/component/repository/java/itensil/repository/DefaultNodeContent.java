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
 * Created on Dec 8, 2003
 *
 */
package itensil.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultNodeContent implements NodeContent, Serializable {


    private int length;
    private byte [] bytes;
    private NodeVersion version;

    /**
     * @param bytes
     */
    public DefaultNodeContent(byte [] bytes, NodeVersion version) {
        length = bytes.length;
        this.bytes = bytes;
        this.version = version;
    }

    /*
     * @see itensil.repository.NodeContent#getLength()
     */
    public int getLength() {
        return length;
    }

    /*
     * @see itensil.repository.NodeContent#getStream()
     */
    public InputStream getStream() {
        return new ByteArrayInputStream(bytes);
    }

    /*
     * @see itensil.repository.NodeContent#getVersion()
     */
    public NodeVersion getVersion() {
        return version;
    }

    /*
     * @see itensil.repository.NodeContent#getBytes()
     */
    public byte[] getBytes() {
        return bytes;
    }

}
