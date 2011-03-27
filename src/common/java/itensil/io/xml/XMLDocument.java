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

import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class XMLDocument {

    public static Document readStream(InputStream in) throws DocumentException {
        SAXReader reader = new SAXReader(false);
        return reader.read(in);
    }

    public static Document readStream(Reader in) throws DocumentException {
        SAXReader reader = new SAXReader(false);
        return reader.read(in);
    }

    public static void writeStream(Document doc, OutputStream out) throws IOException {
        XMLWriter dxw = new XMLWriter(out, new OutputFormat("\t", true));
        dxw.write(doc);
    }
    
    protected static DocumentBuilder docBuilder;
    static {
        DocumentBuilderFactory documentBuilderFactory = null;
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        try {
			docBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
    }
    
    public synchronized static org.w3c.dom.Document readStreamDOM(InputStream in) 
    		throws SAXException, IOException {
        
    	return docBuilder.parse(in);
    }
    
    public synchronized static org.w3c.dom.Document convertDOM4jtoTrueDOM(Document doc)
    		throws SAXException, IOException {
    	return docBuilder.parse((new DocumentSource(doc)).getInputSource());
    }
    
}
