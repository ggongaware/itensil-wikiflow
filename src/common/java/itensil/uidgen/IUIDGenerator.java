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

import java.io.Serializable;
import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * @author ggongaware@itensil.com
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
 *  time_hi				   unsigned 16   6    The high field of the
 *                         bit integer          timestamp multiplexed
 *                                              with the version number.
 *
 *  clock_seq_hi_and_rese  unsigned 8    7      The high field of the
 *  rved                   bit integer          clock sequence
 *                                              multiplexed with the
 *                                              variant.
 *
 *  clock_seq_low          unsigned 8    8      The low field of the
 *                         bit integer          clock sequence.
 *
 *  node                   unsigned 48   9-14   The spatially unique
 *                         bit integer          node identifier.
 */
public class IUIDGenerator implements Serializable {

	private int sequence = -1;
	private int hash = -1;
	private byte[] bytesIP = null;
	
	public IUIDGenerator() {
		try {
			bytesIP = InetAddress.getLocalHost().getAddress(); 
		} catch (Exception ex){
			bytesIP = new byte[4];
		}
		SecureRandom srand = new SecureRandom();
		sequence = srand.nextInt();
		hash = System.identityHashCode(this);
	}

	public IUID createID() {
		byte bytes[] = new byte[IUID.BYTE_SIZE];
		long time = System.currentTimeMillis();
		
		// time
		bytes[0] = (byte)(0xff & time);
		bytes[1] = (byte)(0xff & (time >>  8));
		bytes[2] = (byte)(0xff & (time >> 16));
		bytes[3] = (byte)(0xff & (time >> 24));
		bytes[4] = (byte)(0xff & (time >> 32));
		bytes[5] = (byte)(0xff & (time >> 40));
		bytes[6] = (byte)(0xff & (time >> 48));
		//bytes[7] = (byte)(0xff & (time >> 56)); // trade a byte of time for some spatial
				
		//sequence
        int seq;
        synchronized (this) {
            if (sequence == Integer.MAX_VALUE) {
                sequence = 0;
            } else {
                sequence++; 
            }
            seq = sequence;
        }
		
		bytes[7] = (byte)(0xff & seq);
		bytes[8] = (byte)(0xff & (seq >> 8));
		
		//node
		bytes[9] = bytesIP[0];
		bytes[10] = bytesIP[1];
		bytes[11] = bytesIP[2];
		bytes[12] = bytesIP[3];
		bytes[13] = (byte)(0xff & hash);
		bytes[14] = (byte)(0xff & (hash >> 8));
				
		return new IUID(bytes);
	}

	/*
	public static void main(String[] args) {
		
		IUIDGenerator gen = new IUIDGenerator();
		IUIDGenerator gen2 = new IUIDGenerator();
		for(int i=0; i < 100; i++) {
			System.out.println("Generate 3 uniques");
			IUID uidA = gen.createID();
			System.out.println(uidA);
			System.out.println(uidA.toUUID());
			IUID uidB = gen2.createID();
			System.out.println(uidB);
			System.out.println(uidB.toUUID());
			IUID uid = gen.createID();
			System.out.println(uid);
			System.out.println(uid.toUUID());
			System.out.println("Collisions: " + 
					(uid.equals(uidA) || uidA.equals(uidB) || uidB.equals(uid)));
			System.out.println("Read back IUID in and compare");
			IUID uid2 = new IUID(uid.toString());
			System.out.println("Equals: " + uid.equals(uid2));
			System.out.println(uid2);
			//System.out.println(uid2.toUUID());
			System.out.println("Read back UUID in and compare");
			uid2 = IUID.fromUUID(uid.toUUID());
			System.out.println("Equals: " + uid.equals(uid2));
			//System.out.println(uid2);
			System.out.println(uid2.toUUID());
		}
		
	}*/
}
