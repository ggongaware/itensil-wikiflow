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

import org.dom4j.Document;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;

import javax.xml.transform.*;

/**
 * @author ggongaware@itensil.com
 *
 */
public class XMLTransform {

    protected Transformer transformer;

    public XMLTransform(Source xsltSource) throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        transformer = factory.newTransformer(xsltSource);
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public Document transform(Document srcDoc) throws TransformerException {
        DocumentSource source = new DocumentSource(srcDoc);
        DocumentResult result = new DocumentResult();
        transformer.transform(source, result);
        return result.getDocument();
    }

}
