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

import java.io.*;
import java.util.HashMap;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ReplaceFilter {

    char tokenMarker;
    HashMap<String, Object> keys;

	/**
	 * Constructor for ReplaceFilter.
	 */
	public ReplaceFilter() {
		this('@');
	}
	
	
	public ReplaceFilter(char tokenMarker) {
		this.tokenMarker = tokenMarker;
        keys = new HashMap<String, Object>();
	}

    public void addReplaceKey(String key, String val) {
        keys.put(key, val);
    }
    
    public void addReplaceKey(String key, InputStream val) {
        addReplaceKey(key, new InputStreamReader(val));
    }
    
    public void addReplaceKey(String key, Reader val) {
        keys.put(key, val);
    }
    
    public void execute(InputStream in, OutputStream out) throws IOException {
        execute(new BufferedReader(new InputStreamReader(in)),
               new BufferedWriter(new OutputStreamWriter(out)));
    }
    
    public void execute(Reader in, Writer out) throws IOException {
        int read;
        boolean inKey = false;
        StringBuffer key = new StringBuffer();
        while ((read = in.read()) >= 0) {
            char ch = (char)read;
            if (ch == tokenMarker) {
                if (inKey) {
                    Object val = keys.get(key.toString());
                    if (val != null) {
                        if (val instanceof Reader) {
                            copyStream((Reader)val, out);
                        } else {
                            out.write((String)val);
                        }
                    } else {
                        out.write(tokenMarker);
                        out.write(key.toString());
                        out.write(tokenMarker);
                    }
                    key.setLength(0);
                    inKey = false;
                } else {
                    inKey = true;
                }
            } else if (inKey) {
                if (Character.isWhitespace(ch)) {
                    inKey = false;
                    out.write(tokenMarker);
                    out.write(key.toString());
                    out.write(ch);
                    key.setLength(0);
                } else {
                    key.append(ch);
                }
            } else {
                out.write(ch);
            }
        }
        out.flush();     
    }

    public String execute(String in) {
        StringWriter out = new StringWriter();
        try {
            execute(new StringReader(in), out);
        } catch (IOException e) {
            return null;
        }
        return out.toString();
    }

    private static void copyStream(Reader in, Writer out)
            throws IOException {
        char[] buf = new char[4 * 1024]; // 4K buf
        int bytesRead;
        while ((bytesRead = in.read(buf)) != -1) {
            out.write(buf, 0, bytesRead);
        }
    }
}
