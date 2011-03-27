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
 * Created on Jan 23, 2004
 *
 */
package itensil.io.xml;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.io.SAXWriter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author ggongaware@itensil.com
 *
 */
public class SAXHandler extends DefaultHandler {


    private static SAXParserFactory factory = null;

    public SAXHandler() {
    }

    private static SAXParserFactory getFactory() {

        if (factory == null) {
            synchronized(SAXHandler.class) {
                if (factory == null) {
                    factory = SAXParserFactory.newInstance();
                    factory.setValidating(false);
                    factory.setNamespaceAware(true);
                    try {
                        factory.setFeature("http://xml.org/sax/features/string-interning", true);
                    } catch (Exception ex) {
                        // re-throw
                        throw new RuntimeException(ex.toString());
                    }
                }
            }
        }
        return factory;
    }

    public SAXParser getParser() throws SAXException {

        try {
            return getFactory().newSAXParser();
        } catch (ParserConfigurationException pce) {
            throw new SAXException(pce);
        }
    }

    /**
     * @param source
     * @throws IOException
     * @throws SAXException
     */
    public void parse(File source) throws IOException, SAXException {

        getParser().parse(source, this);
    }

    /**
     * @param source
     * @throws IOException
     * @throws SAXException
     */
    public void parse(InputStream source) throws IOException, SAXException {

        getParser().parse(source, this);
    }

    /**
     * @param xml The actual XML content
     * @throws IOException
     * @throws SAXException
     */
    public void parse(String xml) throws IOException, SAXException {

        InputSource source = new InputSource(new StringReader(xml));
        getParser().parse(source, this);
    }
    
    /**
     * 
     * @param doc
     * @throws IOException
     * @throws SAXException
     */
    public void parse(Document doc)  throws IOException, SAXException {
    	SAXWriter adapter = new SAXWriter(this);
    	adapter.write(doc);
    }
}
