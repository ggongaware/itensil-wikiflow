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
 * Created on Jan 26, 2004
 *
 */
package itensil.io.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author ggongaware@itensil.com
 *
 */
public class XMLWriter {

    private Writer out;
    private int indent;

    private boolean whitespace;

    /**
     * @param out
     */
    public XMLWriter(Writer out) {
        this.out = out;
        indent = 0;
        whitespace = true;
    }

    /**
     * Write XML Header.
     */
    public void writeXMLHeader() throws IOException {
        out.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    }

    /**
     * @param name
     * @param value
     * @throws IOException
     */
    public void startElement(String name, String value) throws IOException {
        startElement(name, null, value, false);
    }


    /**
     * @param name
     * @param attributes
     * @param value
     * @param empty
     * @throws IOException
     */
    public void startElement(
        String name,
        String[][] attributes,
        String value,
        boolean empty)
        throws IOException {

        StringBuffer buf = whitespace ? tabs(indent) : new StringBuffer();
        buf.append('<').append(name);
        out.write(buf.toString());
        if (attributes != null) {
            for (String[] attribute : attributes) {
                if (attribute.length >= 2) {
                    out.write(' ');
                    out.write(attribute[0]);
                    out.write("=\"");
                    encode(attribute[1], out);
                    out.write('"');
                }
            }
        }
        if (empty) {
            out.write("/>");
            if (whitespace) out.write('\n');
        } else {
            out.write('>');
            if (value != null) {
                encode(value, out);
            } else if (whitespace) {
                out.write('\n');
            }
            indent++;
        }
    }

    /**
     * Write raw XML, does no checking
     * @param xml
     * @throws IOException
     */
    public void rawXML(String xml) throws IOException {
        out.write(xml);
    }

    /**
     * Write some text, normalizes
     * @param txt
     * @throws IOException
     */
    public void text(String txt) throws IOException {
        encode(txt, out);
    }

    /**
     * @param name
     * @throws IOException
     */
    public void endElement(String name, boolean newline) throws IOException {

        indent--;
        StringBuffer buf;
        if (newline && whitespace) {
            buf = tabs(indent);
        } else {
            buf = new StringBuffer();
        }
        buf.append("</").append(name).append(">");
        out.write(buf.toString());
        if (whitespace) out.write('\n');
    }

    public void endElement(String name) throws IOException {
        endElement(name, false);
    }

    /**
     * @param count
     * @return a buffer filled with tabs
     */
    public static StringBuffer tabs(int count) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < count; i++) {
            buf.append('\t');
        }
        return buf;
    }

    /**
     * @param c
     * @param out
     * @throws IOException
     */
    public static void encode(char c, Writer out) throws IOException {

        switch (c) {
            case '<':
                out.write("&lt;");
                break;
            case '>':
                out.write("&gt;");
                break;
            case '&':
                out.write("&amp;");
                break;
            case '"':
                out.write("&quot;");
                break;
            default:
                out.write(c);
        }
    }

    /**
     * @param s
     * @param out
     * @throws IOException
     */
    public static void encode(String s, Writer out) throws IOException {

        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            encode(c, out);
        }
    }

    /**
     * @param s
     * @return a string with entities encoded
     */
    public static String encode(String s) {

        int len = (s != null) ? s.length() : 0;
        StringWriter sOut = new StringWriter(len);
        try {
            encode(s, sOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sOut.toString();
    }

    public void endDocument() throws IOException {
        out.flush();
    }

    public void setWhitespace(boolean whitespace) {
        this.whitespace = whitespace;
    }
    
    public static final String FILTER_CHARS = " :*?<>|\\/\"";
    
    /**
     * Clean out the characters that don't work with Win32 filenames
     * 
     * @param name
     * @return filtered name
     */
    public static String nmTokenFilter(String name) {
    	name = name.trim();
        int len = name.length();
		StringBuffer buf = new StringBuffer(len);
		for (int i=0; i < len; i++) {
		 	char ch = name.charAt(i);
		 	if (FILTER_CHARS.indexOf(ch) >= 0) ch = '_';
		 	buf.append(ch);
		}
		return buf.toString();
    }
}
