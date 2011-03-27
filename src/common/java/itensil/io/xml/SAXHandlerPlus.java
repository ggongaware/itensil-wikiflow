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
package itensil.io.xml;

import itensil.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.HashMap;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SAXHandlerPlus extends SAXHandler {

    private StringBuffer textBuffer;
    private Stack<ElementInfo> elements;
    private String myNameSpaceUri;
    private ElementInfo lastElement;

    public SAXHandlerPlus(String myNameSpaceUri) {
        super();
        this.myNameSpaceUri = myNameSpaceUri;
        elements = new Stack<ElementInfo>();
        textBuffer = new StringBuffer();
    }


    /*
     * @see org.xml.sax.ContentHandler#
     *  startElement(String, .String, String, org.xml.sax.Attributes)
     */
    public void startElement(
        String namespaceUri,
        String sName, // simple name
        String qName, // qualified name
        Attributes attrs) throws SAXException {

        if (myNameSpaceUri == null || myNameSpaceUri.equals(namespaceUri)) {
            ElementInfo info = new ElementInfo(namespaceUri, sName, attrs);
            elements.push(info);
            info.path = elements.getPath('/');
        }
    }

    /*
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(
        String namespaceUri,
        String sName, // simple name
        String qName) // qualified name
        throws SAXException {

        String value = textBuffer.toString();
        textBuffer.setLength(0);
        if (myNameSpaceUri == null || myNameSpaceUri.equals(namespaceUri)) {
            if (!elements.isEmpty()) {
                ElementInfo info = elements.pop();
                info.value = value.trim();
                lastElement = info;
            }
        }
    }

    public void characters(char buf[], int offset, int len)
        throws SAXException {

        textBuffer.append(buf, offset, len);
    }

    public String getPath() {
        return elements.getPath('/');
    }

    public ElementInfo getLastElement() {
        return lastElement;
    }


    // inner class
    protected static class ElementInfo {

        public String path;
        public String sName;
        public String nameSpace;
        public HashMap<String,String> attributes;
        public String value;

        protected ElementInfo(
            String nameSpace, String sName, Attributes attribs) {

            this.nameSpace = nameSpace;
            this.sName = sName;
            int len = attribs.getLength();
            attributes = new HashMap<String,String>(len);
            for (int i =0; i < len; i++) {
                attributes.put(attribs.getLocalName(i), attribs.getValue(i));
            }

        }

        /*
        * @see java.lang.Object#toString()
        */
        public String toString() {
            return sName;
        }

    }
}
