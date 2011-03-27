package itensil.report;

import itensil.io.xml.SAXHandler;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.transformer.TransformerImpl;
import org.xml.sax.SAXException;

public class ExslTransform {
	
	protected static TransformerFactory _tFactory = TransformerFactory.newInstance();
	
    public static void transformXML(
    		Result out, Source xml, Source xsl, String baseUri, Map<String,String> params)
	    	throws TransformerException, SAXException, ParserConfigurationException {
	
		Transformer trans;
		try {
		    SAXSource sXsl = new SAXSource(
		            new ExslSandBoxReader((new SAXHandler()).getParser().getXMLReader()),
		            SAXSource.sourceToInputSource(xsl));
		    trans = _tFactory.newTransformer(sXsl);
		    trans.setURIResolver(new ExslURIResolver(baseUri));
		    if (trans instanceof TransformerImpl) {
		        TransformerImpl ti = (TransformerImpl)trans;
		        ExslSandBoxReader.cleanExtensions(ti.getStylesheet().getExtensionNamespacesManager());
		    }
		} catch(TransformerConfigurationException tce) {
		    throw new TransformerException("XSL setup error", tce);
		}
		if (params != null && !params.isEmpty()) {
			for (Map.Entry<String,String> param : params.entrySet()) {
		        trans.setParameter(param.getKey(), param.getValue());
		    }
		}
		trans.transform(xml, out);
	}
}
