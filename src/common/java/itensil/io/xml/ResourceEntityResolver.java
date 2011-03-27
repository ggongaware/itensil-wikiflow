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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;


/**
 * @author ggongaware@itensil.com
 *
 */
public class ResourceEntityResolver implements EntityResolver {

    HashMap<String,String> entityResources;
    Class resClass;

    public ResourceEntityResolver(Class resClass) {
        entityResources = new HashMap<String,String>();
        this.resClass = resClass;
    }

    public void addResource(String id, String resPath) {
        entityResources.put(id, resPath);
    }

    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        
        String resPath = entityResources.get(publicId);
        if (resPath == null) resPath = entityResources.get(systemId);
        if (resPath != null) {
            return new InputSource(resClass.getResourceAsStream(resPath));
        }
        return null;
    }
}
