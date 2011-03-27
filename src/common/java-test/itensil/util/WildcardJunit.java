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
 * Created on Nov 2, 2003
 *
 */
package itensil.util;

import itensil.util.WildcardPattern;
import junit.framework.TestCase;

/**
 * @author ggongaware@itensil.com
 */
public class WildcardJunit extends TestCase {

	/**
	 * Constructor for WildcardJunit.
	 * @param s
	 */
	public WildcardJunit(String s) {
		super(s);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(WildcardJunit.class);
	}

	public void testH_q_qloW_q_s() {
		WildcardPattern pat = new WildcardPattern("H??lo W?*");
		assertTrue	(pat.match("Hello World"));
		assertTrue	(pat.match("Hello Wd"));
		assertFalse	(pat.match("Hlo World"));
		assertFalse (pat.match("Hello W"));
		assertFalse (pat.match(""));
	}

	public void test_s_pgif() {
		WildcardPattern pat = new WildcardPattern("*.gif");
		assertTrue	(pat.match("fish.gif"));
		assertFalse (pat.match("fish.giff"));
		assertFalse (pat.match("fish.giff.gif "));
		assertFalse (pat.match("fish.gif gif"));
		assertTrue 	(pat.match(".gif"));
		assertFalse (pat.match(""));
	}

	public void testnum_q() {
		WildcardPattern pat = new WildcardPattern("num?");
		assertTrue  (pat.match("num1"));
		assertFalse	(pat.match("num"));
		assertFalse (pat.match("num22"));
		assertFalse (pat.match(""));
	}

	public void testfun() {
		WildcardPattern pat = new WildcardPattern("fun");
		assertTrue	(pat.match("fun"));
		assertFalse (pat.match("funn"));
		assertFalse (pat.match("fu"));
		assertFalse (pat.match(""));
	}

	public void testi() {
		WildcardPattern pat = new WildcardPattern("i");
		assertTrue 	(pat.match("i"));
		assertFalse (pat.match("ii"));
		assertFalse (pat.match(""));
	}

	public void test_q() {
		WildcardPattern pat = new WildcardPattern("?");
		assertTrue	(pat.match("i"));
		assertFalse	(pat.match("i?"));
		assertFalse (pat.match(""));
	}

	public void test_q_q() {
		WildcardPattern pat = new WildcardPattern("??");
		assertFalse	(pat.match("i"));
		assertTrue	(pat.match("i?"));
		assertFalse (pat.match(""));
	}

	public void test_s_q() {
		WildcardPattern pat = new WildcardPattern("*?");
		assertTrue	(pat.match("i"));
		assertTrue	(pat.match("go"));
		assertTrue	(pat.match("good"));
		assertFalse (pat.match(""));
	}

	public void test_q_s() {
		WildcardPattern pat = new WildcardPattern("?*");
		assertTrue	(pat.match("i"));
		assertTrue	(pat.match("good"));
		assertFalse (pat.match(""));
	}

	public void test_q_sJ() {
		WildcardPattern pat = new WildcardPattern("?*J");
		assertTrue	(pat.match("iJ"));
		assertTrue	(pat.match("igoodJ"));
		assertFalse (pat.match("J"));
		assertFalse (pat.match("Ji"));
		assertFalse (pat.match(""));
	}

	public void test_s_qJ() {
		WildcardPattern pat = new WildcardPattern("*?J");
		assertTrue	(pat.match("iJ"));
		assertTrue	(pat.match("igoodJ"));
		assertFalse (pat.match("J"));
		assertFalse (pat.match("Ji"));
		assertFalse (pat.match(""));
	}

	public void test_s_qJ_q() {
		WildcardPattern pat = new WildcardPattern("*?J?");
		assertTrue	(pat.match("iJi"));
		assertTrue	(pat.match("igoodJi"));
		assertFalse (pat.match("J"));
		assertFalse (pat.match("Ji"));
		assertFalse (pat.match("ssJis"));
		assertFalse (pat.match(""));
	}

	public void test_s_qJ_q_s_q() {
		WildcardPattern pat = new WildcardPattern("*?J?*?");
		assertTrue	(pat.match("iJii"));
		assertTrue	(pat.match("igoodJii"));
		assertTrue	(pat.match("igoodJioi"));
		assertTrue	(pat.match("igoodJiooi"));
		assertTrue 	(pat.match("ssJis"));
		assertFalse (pat.match("J"));
		assertFalse (pat.match("Jii"));
		assertFalse (pat.match("iJi"));
		assertFalse (pat.match(""));
	}

	public void test_q_q_s_q_q() {
		WildcardPattern pat = new WildcardPattern("??*??");
		assertTrue	(pat.match("good"));
		assertTrue	(pat.match("goood"));
		assertTrue	(pat.match("gooood"));
		assertFalse (pat.match("goo"));
		assertFalse (pat.match("go"));
		assertFalse (pat.match("g"));
		assertFalse (pat.match(""));
	}

	public void testbin_s() {
		WildcardPattern pat = new WildcardPattern("bin*");
		assertTrue 	(pat.match("bingood"));
		assertTrue	(pat.match("bin."));
		assertTrue	(pat.match("bin"));
		assertFalse	(pat.match("bon"));
		assertFalse	(pat.match("bi"));
		assertFalse (pat.match(""));
	}

	public void test_s() {
		WildcardPattern pat = new WildcardPattern("*");
		assertTrue 	(pat.match("good"));
		assertTrue	(pat.match("."));
		assertTrue	(pat.match(""));
	}

	public void test_s_p_s() {
		WildcardPattern pat = new WildcardPattern("*.*");
		assertFalse	(pat.match("bad"));
		assertTrue  (pat.match("."));
		assertTrue  (pat.match("a.b"));
		assertTrue  (pat.match("aa.bb"));
		assertTrue  (pat.match("a."));
		assertTrue  (pat.match("aa."));
		assertTrue  (pat.match(".b"));
		assertTrue  (pat.match(".bb"));
		assertFalse (pat.match(""));
	}

	public void test_q_p_q() {
		WildcardPattern pat = new WildcardPattern("?.?");
		assertFalse	(pat.match("bad"));
		assertFalse (pat.match("."));
		assertTrue  (pat.match("a.b"));
		assertFalse (pat.match("aa.bb"));
		assertFalse (pat.match("a."));
		assertFalse (pat.match("aa."));
		assertFalse (pat.match(".b"));
		assertFalse (pat.match(".bb"));
		assertFalse (pat.match(""));
	}

	public void testH_s__W_s() {
		WildcardPattern pat = new WildcardPattern("H* W*");
		assertTrue	(pat.match("Hello World"));
		assertFalse	(pat.match("HelloWorld"));
		assertTrue	(pat.match("Hello World Hello World"));
		assertTrue	(pat.match("Hello W"));
		assertFalse (pat.match(""));
	}

	public void testFilesGif_Tif() {
		WildcardPattern pat = new WildcardPattern("*.?if");
		assertTrue	(pat.match("/pix/hats.gif"));
		assertFalse	(pat.match("/pix/hats.jpg"));
		assertFalse	(pat.match("/pix/hats.gif.bak"));
		assertTrue	(pat.match("/pix/photo.tif"));
		assertFalse	(pat.match("/pix/photo.tif.bak"));
	}

	public void testFilesJpeg() {
		WildcardPattern pat = new WildcardPattern("*.jp*g");
		assertTrue	(pat.match("/pix/hats.jpg"));
		assertFalse	(pat.match("/pix/hats.gif"));
		assertTrue	(pat.match("/pix/photo.jpeg"));
	}

	public void testFilesSubJSP() {
		WildcardPattern pat = new WildcardPattern("/sub/*.jsp");
		assertTrue	(pat.match("/sub/index.jsp"));
		assertFalse	(pat.match("/index.jsp"));
		assertTrue	(pat.match("/sub/1.jsp"));
		assertFalse	(pat.match("/sub/index.jsp.bak"));
		assertTrue	(pat.match("/sub/sub/test.jsp"));
		assertFalse	(pat.match("/sub.jsp"));
	}

    public void testLIKE() {
        WildcardPattern pat = new WildcardPattern("image/%", '%', '_', true);
        assertTrue  (pat.match("image/jpeg"));
        assertFalse (pat.match("text/html"));
        assertTrue  (pat.match("image/"));

        pat = new WildcardPattern("%\\%", '%', '_', true);
        assertTrue  (pat.match("100%"));
        assertTrue  (pat.match("%"));
        assertFalse (pat.match("bob"));

        pat = new WildcardPattern("_\\__", '%', '_', true);
        assertTrue  (pat.match("A_A"));
        assertFalse (pat.match("A-A"));
        assertFalse (pat.match("BB_BB"));

        pat = new WildcardPattern("%alpha%bravo%tango%", '%', '_', true);
        assertTrue  (pat.match("alpha bravo tango"));
        assertFalse (pat.match("Alpha Bravo tango"));
        assertFalse (pat.match("alpha tango bravo"));
        assertTrue  (pat.match("alpha bravo froxtrot tango"));

        // caseless
        pat = new WildcardPattern("%alpha%bravo%tanGo%", '%', '_', false);
        assertTrue  (pat.match("alpha bravo tango"));
        assertTrue  (pat.match("Alpha BravO TaNgo"));
        assertFalse (pat.match("alpha tango bravo"));
        assertTrue  (pat.match("alpha bravo froxTrot tango"));
    }
}
