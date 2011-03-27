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

/**
 * @author ggongaware@itensil.com
 *
 */
public class HTMLEncode {


    static public String encode(String rawText) {

        if (rawText == null)
            return "";

        char[] chs = rawText.toCharArray();
        StringBuffer strbuf = new StringBuffer(chs.length);

        for (int i = 0; i < chs.length; i++) {
            switch (chs[i]) {
                case '"':
                    strbuf.append("&quot;");
                    break;
                case '>':
                    strbuf.append("&gt;");
                    break;
                case '<':
                    strbuf.append("&lt;");
                    break;
                case '&':
                    strbuf.append("&amp;");
                    break;
                default:
                    strbuf.append(chs[i]);
            }
        }
        return strbuf.toString();
    }


    static public String dblQuoteEncode(String rawText) {

        if (rawText == null)
            return "";

        char[] chs = rawText.toCharArray();
        StringBuffer strbuf = new StringBuffer(chs.length);

        for (int i = 0; i < chs.length; i++) {
            switch (chs[i]) {
                case '\n':
                    strbuf.append("\\n");
                    break;
                case '\r':
                    strbuf.append("\\r");
                    break;
                case '"':
                    strbuf.append("\\\"");
                    break;
                case '\\':
                    strbuf.append("\\\\");
                    break;
                default:
                    strbuf.append(chs[i]);
            }
        }

        return strbuf.toString();
    }

    static public String sglQuoteEncode(String rawText) {

        if (rawText == null)
            return "";

        char[] chs = rawText.toCharArray();
        StringBuffer strbuf = new StringBuffer(chs.length);

        for (int i = 0; i < chs.length; i++) {
            switch (chs[i]) {
                case '\n':
                    strbuf.append("\\n");
                    break;
                case '\r':
                    strbuf.append("\\r");
                    break;
                case '\'':
                    strbuf.append("\\'");
                    break;
                case '\\':
                    strbuf.append("\\\\");
                    break;
                default:
                    strbuf.append(chs[i]);
            }
        }
        return strbuf.toString();
    }

    static public String jsQuoteEncode(String rawText) {
        String txt = dblQuoteEncode(rawText);
        return txt.replace("</script>", "</sc\" + \"ript>");
    }
}