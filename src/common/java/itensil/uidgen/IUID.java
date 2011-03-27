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
 * Created on Oct 24, 2003
 *
 */
package itensil.uidgen;

import itensil.util.Base64;
import java.util.Arrays;


/**
 * <pre>
 * Itensil Unique Identifier
 * 
 * Record of 15 octets is either guaranteed to be different
 * from all other IUIDs/UUIDs generated until 3400 A.D. or extremely
 * likely to be different.
 * 
 * Upward compatable with UUID INTERNET-DRAFT
 * 
 * 
 * UUID layout
 * The following table gives the format of a UUID for the variant
 * specified herein. The UUID consists of a record of 16 octets. To
 * minimize confusion about bit assignments within octets, the UUID
 * record definition is defined only in terms of fields that are
 * integral numbers of octets. The fields are in order of significance
 * for comparison purposes, with "time_low" the most significant, and
 * "node" the least significant.
 *
 *  Field                  Data Type     Octet  Note
 *                                       #
 *
 *  time_low               unsigned 32   0-3    The low field of the
 *                         bit integer          timestamp.
 *
 *  time_mid               unsigned 16   4-5    The middle field of the
 *                         bit integer          timestamp.
 *
 *  time_hi_and_version    unsigned 16   6-7    The high field of the
 *                         bit integer          timestamp multiplexed
 *                                              with the version number.
 *
 *  clock_seq_hi_and_rese  unsigned 8    8      The high field of the
 *  rved                   bit integer          clock sequence
 *                                              multiplexed with the
 *                                              variant.
 *
 *  clock_seq_low          unsigned 8    9      The low field of the
 *                         bit integer          clock sequence.
 *
 *  node                   unsigned 48   10-15  The spatially unique
 *                         bit integer          node identifier.
 * 
 * </pre>
 * @author ggongaware@itensil.com
 */
public class IUID implements java.io.Serializable, Cloneable, Comparable {

    static final long serialVersionUID = 1079554220539L;
    
	public static final int BYTE_SIZE = 15;
	public static final int UUID_SIZE = 36;
		
	private byte bytes[];
    private int hash = 0;
    private transient String s;

	public IUID(String b64Str) {
		this(Base64.decodeToBytes(b64Str));
        s = b64Str;
	}
	
	public IUID(byte bytes[]) throws UIDDataException {
		if (bytes.length != BYTE_SIZE) {
			throw new UIDDataException("Invalid UID bytes");
		}
		this.bytes = bytes;
        s = null;
	}	

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		return new IUID(getBytes());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o instanceof IUID) {
			IUID uid = ((IUID)o);
            if (uid.hashCode() == hashCode()) {
                return Arrays.equals(this.bytes, uid.bytes);
            }           
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
        if (s == null) {
            s = new String(Base64.encode(bytes, true));
        }
		return s;
	}

	public byte[] getBytes() {
		byte nbytes[] = new byte[BYTE_SIZE];
		System.arraycopy(bytes, 0, nbytes, 0, BYTE_SIZE);
		return nbytes;
	}
	
	public String toUUID() {
		//xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
		//near to spec but defaults spec octet 7
		StringBuffer buf = new StringBuffer(36);
		buf.append(toHex(bytes[0]));
		buf.append(toHex(bytes[1]));
		buf.append(toHex(bytes[2]));
		buf.append(toHex(bytes[3]));
		buf.append('-');
		buf.append(toHex(bytes[4]));
		buf.append(toHex(bytes[5]));
		buf.append('-');
		buf.append(toHex(bytes[6]));
		buf.append("03"); // UUID version 3 (name/address based)
		buf.append('-');
		buf.append(toHex(bytes[7]));
		buf.append(toHex(bytes[8]));
		buf.append('-');
		buf.append(toHex(bytes[9]));
		buf.append(toHex(bytes[10]));
		buf.append(toHex(bytes[11]));
		buf.append(toHex(bytes[12]));
		buf.append(toHex(bytes[13]));
		buf.append(toHex(bytes[14]));
		return buf.toString();
	}
    
	/**
	 * Read from spec UUID (skipping octet 7)
	 * @param uuidStr xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
	 * @return
	 */
	public static IUID fromUUID(String uuidStr) throws UIDDataException {
		
		byte bytes[] = new byte[BYTE_SIZE];
		int b;
		if (uuidStr.length() != 36) {
			throw new UIDDataException("Invalid UUID length");
		}
		try {
			b = (int)(Long.parseLong(uuidStr.substring(0,8), 16) & 0xffffffff);
			bytes[0] = (byte)(0xff & (b >> 24));
			bytes[1] = (byte)(0xff & (b >> 16));
			bytes[2] = (byte)(0xff & (b >>  8));
			bytes[3] = (byte)(0xff & b);
			// -
			b = Integer.parseInt(uuidStr.substring(9,13), 16);
			bytes[4] = (byte)(0xff & (b >>  8));
			bytes[5] = (byte)(0xff & b);
			// -
			b = Integer.parseInt(uuidStr.substring(14,16), 16);
			bytes[6] = (byte)(0xff & b);
			// skip
			// -			
			b = Integer.parseInt(uuidStr.substring(19,23), 16);
			bytes[7] = (byte)(0xff & (b >>  8));
			bytes[8] = (byte)(0xff & b);
			// -
			b = (int)(Long.parseLong(uuidStr.substring(24,32), 16) & 0xffffffff);
			bytes[9] = (byte)(0xff & (b >> 24));
			bytes[10] = (byte)(0xff & (b >> 16));
			bytes[11] = (byte)(0xff & (b >>  8));
			bytes[12] = (byte)(0xff & b);
			b = Integer.parseInt(uuidStr.substring(32,36), 16);
			bytes[13] = (byte)(0xff & (b >>  8));
			bytes[14] = (byte)(0xff & b);
		} catch (NumberFormatException nfe) {
			throw new UIDDataException("Invalid UUID format: " + nfe.getMessage());
		}		
		return new IUID(bytes);
	}
	
	private String toHex(int b) {
		if (b < 0) b += 256;
		if (b < 16) {
			return "0" + Integer.toString(b, 16);
		} else {
			return Integer.toString(b, 16);
		}
	}
    
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
        if (hash == 0) {
           hash = (((bytes[0] & 0xff) << 24) | ((bytes[3] & 0xff) << 16) |
                    ((bytes[7] & 0xff) << 8) | (bytes[12] & 0xff)); 
        }
		return hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		IUID uid = (IUID)o;
		for (int i = 0; i < BYTE_SIZE; i++) {
			int a = bytes[i];
			int b = uid.bytes[i];
			if (a != b) {
				if (a < 0) a += 256;			
				if (b < 0) b += 256;
				return a < b ? -1 : 1;
			}
		}
		return 0;
	}

}
