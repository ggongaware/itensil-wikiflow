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
package itensil.workflow.model.legacy;

import org.dom4j.Document;
import itensil.io.xml.XMLTransform;
import itensil.io.xml.XMLDocument;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileInputStream;

/**
 * @author ggongaware@itensil.com
 *
 */
public class LegacySupport {

    protected static XMLTransform pmapV1Trans;

    static {
        try {
            pmapV1Trans = new XMLTransform(new StreamSource(LegacySupport.class.getResourceAsStream("pmap2flow.xsl.xml")));
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static boolean isLegacyModel(Document modelDoc) {

        // current legacy support
        return isPmapV1(modelDoc);
    }

    // process-map version=1.0
    protected static boolean isPmapV1(Document modelDoc) {
        return "process-map".equals(modelDoc.getRootElement().getName());
    }

    public static Document upgrade(Document legacyDoc) throws TransformerException {

        if (isPmapV1(legacyDoc)) {
            return pmapV1Trans.transform(legacyDoc);
        }
        throw new TransformerException("Model format not supported");
    }

    public static void main(String[] args) throws Exception {
       XMLDocument.writeStream(upgrade(XMLDocument.readStream(new FileInputStream(args[0]))), System.out);
    }

}
