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
package itensil.web;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.BitSet;

/**
 * @author ggongaware@itensil.com
 *
 */
public class UrlUtil {

    static BitSet dontNeedEncoding;
    static final int caseDiff = ('a' - 'A');

    /* The list of characters that are not encoded have been determined by
       referencing O'Reilly's "HTML: The Definitive Guide" (page 164). */
    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }


    /**
     * @param url
     * @param name
     * @param value
     * @return
     */
    public static String appendParameter(
        String url, String name, String value) {

        StringBuffer buf = new StringBuffer(url);
        if (url.indexOf('?') >= 0) {
            buf.append('&');
            buf.append(encode(name));
            buf.append('=');
            buf.append(encode(value));
        } else {
            buf.append('?');
            buf.append(encode(name));
            buf.append('=');
            buf.append(encode(value));
        }
        return buf.toString();
    }

    /**
     * Translates a string into <code>x-www-form-urlencoded</code> format.
     *
     * @param   s   <code>String</code> to be translated.
     * @return  the translated <code>String</code>.
     */
    public static String encode(String s) {

        boolean needToChange = false;
        StringBuffer out = new StringBuffer(s.length());
        Charset charset;
        CharArrayWriter charArrayWriter = new CharArrayWriter();

        try {
            charset = Charset.forName("UTF8");
        } catch (IllegalCharsetNameException e) {
            throw new RuntimeException("UTF8 not working");
        } catch (UnsupportedCharsetException e) {
            throw new RuntimeException("UTF8 not working");
        }

        for (int i = 0; i < s.length();) {
            int c = (int) s.charAt(i);
            //System.out.println("Examining character: " + c);
            if (dontNeedEncoding.get(c)) {
                //System.out.println("Storing: " + c);
                out.append((char)c);
                i++;
            } else {
                // convert to external encoding before hex conversion
                do {
                    charArrayWriter.write(c);
                    /*
                     * If this character represents the start of a Unicode
                     * surrogate pair, then pass in two characters. It's not
                     * clear what should be done if a bytes reserved in the
                     * surrogate pairs range occurs outside of a legal
                     * surrogate pair. For now, just treat it as if it were
                     * any other character.
                     */
                    if (c >= 0xD800 && c <= 0xDBFF) {
                        /*
                          System.out.println(Integer.toHexString(c)
                          + " is high surrogate");
                        */
                        if ( (i+1) < s.length()) {
                            int d = (int) s.charAt(i+1);
                            /*
                              System.out.println("\tExamining "
                              + Integer.toHexString(d));
                            */
                            if (d >= 0xDC00 && d <= 0xDFFF) {
                                /*
                                  System.out.println("\t"
                                  + Integer.toHexString(d)
                                  + " is low surrogate");
                                */
                                charArrayWriter.write(d);
                                i++;
                            }
                        }
                    }
                    i++;
                } while (i < s.length() && !dontNeedEncoding.get((c = (int) s.charAt(i))));

                charArrayWriter.flush();
                String str = new String(charArrayWriter.toCharArray());
                byte[] ba;
				try {
					ba = str.getBytes(charset.name());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return "";
				}
                for (int j = 0; j < ba.length; j++) {
                    out.append('%');
                    char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                    // converting to use uppercase letter as part of
                    // the hex value if ch is a letter.
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                    ch = Character.forDigit(ba[j] & 0xF, 16);
                    if (Character.isLetter(ch)) {
                        ch -= caseDiff;
                    }
                    out.append(ch);
                }
                charArrayWriter.reset();
                needToChange = true;
            }
        }

        return (needToChange? out.toString() : s);
    }

    /**
     * Replace or add the parameter to the URL 
     * @param url
     * @param name
     * @param value
     * @return
     */
    public static String appendReplaceParameter(
        String url, String name, String value) {

        StringBuffer buf = new StringBuffer(url);
        int q = url.indexOf('?');
        if (q >= 0) {
            String encName = encode(name);
            int pos = buf.indexOf(encName, q);
            if (pos >= 0) {
                int start = pos + encName.length() + 1;
                int end = buf.indexOf("&", start);
                if (end == -1) end = buf.length();
                buf.replace(start, end, value);
            } else {
                buf.append('&');
                buf.append(encName);
                buf.append('=');
                buf.append(encode(value));
            }
        } else {
            buf.append('?');
            buf.append(encode(name));
            buf.append('=');
            buf.append(encode(value));
        }
        return buf.toString();
    }
}
