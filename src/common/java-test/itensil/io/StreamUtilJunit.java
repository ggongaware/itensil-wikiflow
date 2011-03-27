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
package itensil.io;

import junit.framework.TestCase;

import java.io.StringReader;
import java.io.StringWriter;

import itensil.io.StreamUtil;

/**
 * @author ggongaware@itensil.com
 */
public class StreamUtilJunit extends TestCase {

    public StreamUtilJunit(String s) {
		super(s);
	}

    public void testStreamReplace() throws Exception {

        StringReader in = new StringReader("tttacotacotac otacitaco");
        StringWriter out = new StringWriter();
        StreamUtil.copyStream(in, out, "taco", "hotdog");
        out.close();
        assertEquals("tthotdoghotdogtac otacihotdog", out.toString());
    }

    public void testExact() throws Exception {

        StringReader in = new StringReader("taco");
        StringWriter out = new StringWriter();
        StreamUtil.copyStream(in, out, "taco", "hotdog");
        out.close();
        assertEquals("hotdog", out.toString());
    }

    public void testNoMatch() throws Exception {

        StringReader in = new StringReader("toco too toco");
        StringWriter out = new StringWriter();
        StreamUtil.copyStream(in, out, "taco", "hotdog");
        out.close();
        assertEquals("toco too toco", out.toString());
    }

    public void testTooSmall() throws Exception{

        StringReader in = new StringReader("taco");
        StringWriter out = new StringWriter();
        StreamUtil.copyStream(in, out, "tttacotacotac otacitaco", "hotdog");
        out.close();
        assertEquals("taco", out.toString());
    }
}
