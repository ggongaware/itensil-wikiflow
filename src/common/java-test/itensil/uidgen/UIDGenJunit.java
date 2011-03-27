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
 * Created on Oct 30, 2003
 *
 */
package itensil.uidgen;

import java.util.*;


import junit.framework.TestCase;

/**
 * @author ggongaware@itensil.com
 */
public class UIDGenJunit extends TestCase {

	IUIDGenerator gen;
	IUIDGenerator gen2;

	/**
	 * Constructor for UIDGenJunit.
	 * @param s
	 */
	public UIDGenJunit(String s) {
		super(s);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(UIDGenJunit.class);
	}

	public void testGenerate() {
		IUID uid = gen.createID();
		assertEquals(uid.toString().length(), 20);
		assertEquals(uid.toUUID().length(), 36);
		assertEquals(uid.getBytes().length, IUID.BYTE_SIZE);
	}

	public void testCompare() {
		IUID uidA = gen.createID();
		IUID uidB = gen.createID();
		assertFalse(uidA.equals(uidB));
		uidB = (IUID)uidA.clone();
		assertTrue(uidA.equals(uidB));
	}

	public void testReadUUID() {
		IUID uidA = gen.createID();
		String uuidStr = uidA.toUUID();
		IUID uidB = IUID.fromUUID(uuidStr);
		assertTrue(uidA.equals(uidB));
	}

	public void testReadIUID() {
		IUID uidA = gen.createID();
		String uidStr = uidA.toString();
		IUID uidB = new IUID(uidStr);
		assertTrue(uidA.equals(uidB));
	}

	public void testHashUniqueness() {
		IUID uid;
		Set<IUID> set = new HashSet<IUID>();
		for (int i=0; i < Character.MAX_VALUE*2; i++) {
			uid = gen.createID();
			assertTrue(set.add(uid));
			uid = gen2.createID();
			assertTrue(set.add(uid));
		}
	}

	public void testTreeUniqueness() {
		IUID uid;
		Set<IUID> set = new TreeSet<IUID>();
		for (int i=0; i < Character.MAX_VALUE*2; i++) {
			uid = gen.createID();
			assertTrue(set.add(uid));
			uid = gen2.createID();
			assertTrue(set.add(uid));
		}
	}


	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		gen = new IUIDGenerator();
		gen2 = new IUIDGenerator();
	}

}
