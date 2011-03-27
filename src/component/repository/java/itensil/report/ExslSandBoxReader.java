package itensil.report;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.apache.xalan.extensions.ExtensionNamespacesManager;
import org.apache.xalan.extensions.ExtensionNamespaceSupport;

import java.util.Vector;


/**
 *
 * @author  ggongaware@itensil.com
 * @version $Revision: 1.2 $
 *
 * Last updated by $Author: grant $
 */
public class ExslSandBoxReader extends XMLFilterImpl {



    public ExslSandBoxReader(XMLReader parent) {
        super(parent);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (uri.startsWith("http://xml.apache.org/xalan") || uri.startsWith("xalan:/")
                || uri.startsWith("http://xml.apache.org/xslt/java")
                || uri.startsWith("http://xsl.lotus.com/java")) {

            uri = "blocked-extension";
        }
        super.startPrefixMapping(prefix, uri);
    }
    
    /**
     * TODO finish the filters or replace out the xsl:include/xsl:import/document() resolver
     * security filter for
     */
	public void _startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

		
		if ("http://www.w3.org/1999/XSL/Transform".equals(uri)) {
			
			// re-address include and imports
			if ("include".equals(localName) || "import".equals(localName)) {
				
			} else {
				// filter select= and match= document('file:
			}
			
		} else {
			// catch curly brace document('file: expressions
			for (int ii = 0; ii < atts.getLength(); ii++) {
				//String val = atts.getValue(ii);
			}
		}
		super.startElement(uri, localName, qName, atts);
	}

	public static void cleanExtensions(ExtensionNamespacesManager manager) {
        Vector exts = manager.getExtensions();
        /*
         Clear out non-exslt extensions
        */
        for (int i = exts.size() - 1; i >=0; i--) {
            ExtensionNamespaceSupport ens = (ExtensionNamespaceSupport)exts.get(i);
            String ns = ens.getNamespace();
            if (!("http://exslt.org/common".equals(ns)
                    || "http://exslt.org/dates-and-times".equals(ns)
                    || "http://exslt.org/dynamic".equals(ns)
                    || "http://exslt.org/math".equals(ns)
                    || "http://exslt.org/sets".equals(ns)
                    || "http://exslt.org/strings".equals(ns))) {
                exts.remove(i);
            }
        }
        manager.registerExtension(
                new ExtensionNamespaceSupport("http://itensil.com/ns/report-exslt",
                            "org.apache.xalan.extensions.ExtensionHandlerJavaClass",
                             new Object[]{
                                 "http://itensil.com/ns/report-exslt",
                                 "javaclass",
                                 "itensil.report.ExsltReport"}
                ));
    }
}
