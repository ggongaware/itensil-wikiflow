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
package itensil.util;

import junit.framework.TestCase;
import itensil.util.UriHelper;

/**
 * @author ggongaware@itensil.com
 */
public class UriJunit extends TestCase {

    /**
     * Constructor for UriJunit.
     * @param s
     */
    public UriJunit(String s) {
        super(s);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(UriJunit.class);
    }
    
    public void testLocalize() {
    	assertEquals("/parent1", UriHelper.localizeUri("/parent", "/parent1"));
    	assertEquals("1", UriHelper.localizeUri("/parent/", "/parent/1"));
    	assertEquals("1", UriHelper.localizeUri("/parent", "/parent/1"));
    	assertEquals("", UriHelper.localizeUri("/parent", "/parent/"));
    	assertEquals("", UriHelper.localizeUri("/parent/", "/parent/"));
    	assertEquals("/parent", UriHelper.localizeUri("/parent", "/parent"));
    	assertEquals("/parent", UriHelper.localizeUri("/parent/", "/parent"));
    	assertEquals("/parent/1", UriHelper.localizeUri("", "/parent/1"));
    	assertEquals("parent/1", UriHelper.localizeUri("/", "/parent/1"));
    }

    public void testRelativePath() {
        assertEquals("../file.txt", UriHelper.relativePath("parent/kid", "parent/file.txt"));
        assertEquals("../file.txt",UriHelper.relativePath("/parent/kid", "/parent/file.txt"));
        assertEquals("../kid1/file.txt", UriHelper.relativePath("/parent/kid2", "/parent/kid1/file.txt"));
        assertEquals("../../kid1/file.txt", UriHelper.relativePath("/parent/kid2/sub", "/parent/kid1/file.txt"));
        assertEquals("sub/file.txt", UriHelper.relativePath("/parent/kid", "/parent/kid/sub/file.txt"));
        assertEquals("../kid1/sub/file.txt", UriHelper.relativePath("/parent/kid2", "/parent/kid1/sub/file.txt"));
        assertEquals("/parent1/file.txt", UriHelper.relativePath("/parent", "/parent1/file.txt"));
        assertEquals("/parent1", UriHelper.relativePath("/parent", "/parent1"));
        assertEquals("file.txt", UriHelper.relativePath("/parent/kid", "/parent/kid/file.txt"));
        assertEquals("", UriHelper.relativePath("/parent/kid", "/parent/kid/"));
        assertNull(UriHelper.relativePath("parent", "/parent1/file.txt"));
        assertNull(UriHelper.relativePath("/parent", "parent1/file.txt"));
        assertNull(UriHelper.relativePath("parent1/kid", "parent/file.txt"));
    }


    public void testReducePath() {
        assertEquals("/test.txt", UriHelper.reduce("/root/../test.txt"));
        assertEquals("/root/test.txt", UriHelper.reduce("/root/sub/../test.txt"));
        assertEquals("/root/test.txt", UriHelper.reduce("/root/sub/sub/../../test.txt"));
        assertEquals("../test.txt", UriHelper.reduce("../test.txt"));
        assertEquals("/root/p1/p2/test.txt", UriHelper.reduce("/root/p1/sp1/../p2/sp2/../test.txt"));
        assertEquals("/test.txt", UriHelper.reduce("/../test.txt"));
        assertEquals("/test.txt", UriHelper.reduce("/../../test.txt"));
        assertEquals("/test.txt", UriHelper.reduce("/sub/../../test.txt"));
    }

}
