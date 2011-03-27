package itensil.scripting.util;

import itensil.io.xml.XMLDocument;
import itensil.util.Check;
import itensil.util.UriHelper;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.utils.Base64;
import org.apache.xml.security.utils.DigesterOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class JSDomData extends ScriptableObject {
	
	static {
		org.apache.xml.security.Init.init();
	}

    Document doc;
    boolean dirty;
    String uri;

    public JSDomData(Document doc, String uri) {
        this.doc = doc;
        this.uri = uri;
        dirty = false;
        String funcs[] = {"getValue", "setValue", "parseXML",
        		"setDocument", "getDocument", "createData", "selectNodes",
        		"sha1HashC14Node", "embedDoc", "resolveUri"};
        try {
            this.defineFunctionProperties(
                funcs,
                JSDomData.class,
                ScriptableObject.PERMANENT |
                ScriptableObject.READONLY);
            this.defineProperty("uri", JSDomData.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
        } catch (RhinoException e) {
            e.printStackTrace();
        }
    }

    public String getValue(String xpathExpression) {
    	Element root = doc.getRootElement();
        XPath xp = root.createXPath(xpathExpression);
        String val = xp.valueOf(root);
        return val;
    }
    
    @SuppressWarnings("unchecked")
	public Node[] selectNodes(String xpathExpression) {
    	Element root = doc.getRootElement();
        XPath xp = root.createXPath(xpathExpression);
        List nodes = xp.selectNodes(root);
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    public void setValue(String pathExpr, String value) throws TransformerException {
    	
    	 // Element created if it did not exist
    	Node node;
    	Element root = doc.getRootElement();
    	if (pathExpr.indexOf('@') >= 0 || pathExpr.indexOf('[') >= 0 || pathExpr.indexOf(':') >= 0) {
    		XPath xp =  root.createXPath(pathExpr);
    		node = xp.selectSingleNode(root);
    	} else {
    		node = DocumentHelper.makeElement(root, pathExpr);
    	}
        if (node != null) {
            dirty = true;
            node.setText(Check.emptyIfNull(value));
        }
    }
    
    public void embedDoc(String pathExpr, Object jsdoc) throws DocumentException {
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	Document eDoc = dr.read(sdoc);
    	XPath xp = doc.getRootElement().createXPath(pathExpr);
    	Node node = xp.selectSingleNode(doc.getRootElement());
    	if (node != null && node instanceof Element) {
    		Element elem = (Element)node;
            dirty = true;
            elem.add(eDoc.getRootElement());
    	}
    }
    
    public static org.w3c.dom.Document parseXML(String text) throws DocumentException {
    	Document result = null;

    	SAXReader reader = new SAXReader(DOMDocumentFactory.getInstance());
    	String encoding = getEncoding(text);

    	InputSource source = new InputSource(new StringReader(text));
    	source.setEncoding(encoding);

    	result = reader.read(source);

    	// if the XML parser doesn't provide a way to retrieve the encoding,
    	// specify it manually
    	if (result.getXMLEncoding() == null) {
    	    result.setXMLEncoding(encoding);
    	}

    	return (org.w3c.dom.Document)result;
    }
    
    public org.w3c.dom.Document getDocument() throws DocumentException {
    	DOMWriter dw = new DOMWriter();
    	return dw.write(doc);
    }
    
    public void setDocument(Object jsdoc) throws DocumentException {
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
    		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	doc = dr.read(sdoc);
    }

    public JSDomData createData(Object jsdoc) throws DocumentException {
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
		DOMReader dr = new DOMReader();
		return new JSDomData(dr.read(sdoc), null);
    }
    
    /**
     * 
     * @param xpathExpr
     * @return
     * @throws InvalidCanonicalizerException 
     * @throws XMLSignatureException 
     * @throws CanonicalizationException 
     * @throws IOException 
     * @throws JaxenException 
     * @throws SAXException .
     */
    public String sha1HashC14Node(String xpathExpr) 
    		throws InvalidCanonicalizerException, XMLSignatureException, 
    			CanonicalizationException, SAXException, IOException, JaxenException {
    	
    	org.w3c.dom.Document odoc = XMLDocument.convertDOM4jtoTrueDOM(doc);
    	
    	org.jaxen.XPath path = new DOMXPath(xpathExpr);
    	org.w3c.dom.Node node = (org.w3c.dom.Node)path.selectSingleNode(odoc.getDocumentElement());


    	DigesterOutputStream digester = new DigesterOutputStream(MessageDigestAlgorithm
					.getInstance(odoc, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1));
    	Canonicalizer canonicalizer = Canonicalizer
			.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
    	
    	canonicalizer.setWriter(digester);
		canonicalizer.canonicalizeSubtree((org.w3c.dom.Node) node);
		byte[] hash = digester.getDigestValue();
		
    	return Base64.encode(hash);
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public String getUri() {
    	return uri;
    }
    
    public String resolveUri(String uri) {
    	return this.uri == null ? uri : UriHelper.absoluteUri(UriHelper.getParent(this.uri), uri);
    }

    public String getClassName() {
        return "JSDomData";
    }
    
    private static String getEncoding(String text) {
        String result = null;

        String xml = text.trim();

        if (xml.startsWith("<?xml")) {
            int end = xml.indexOf("?>");
            String sub = xml.substring(0, end);
            StringTokenizer tokens = new StringTokenizer(sub, " =\"\'");

            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();

                if ("encoding".equals(token)) {
                    if (tokens.hasMoreTokens()) {
                        result = tokens.nextToken();
                    }

                    break;
                }
            }
        }

        return result;
    }
}
