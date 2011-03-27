package itensil.io.xml;

import itensil.util.Check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;

import org.dom4j.Attribute;
import org.dom4j.CDATA;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Entity;
import org.dom4j.Namespace;
import org.dom4j.ProcessingInstruction;
import org.dom4j.Text;
import org.dom4j.tree.NamespaceStack;

import org.w3c.dom.DOMImplementation;


public class SoapDOM4jWriter {

    private HashMap<String,String> namespaceMap = new HashMap<String,String>();

    public SoapDOM4jWriter() {
    	namespaceMap.put("", "");
    }
    
    public Map<String,String> getNamespaceMap() {
    	return namespaceMap;
    }
    
    public void importNamespaceMap(Map<String,String> impMap) {
    	namespaceMap.putAll(impMap);
    }
    
    public void resetNamespaceMap() {
    	namespaceMap.clear();
    	namespaceMap.put("", "");
    }
    
    public void write(SOAPEnvelope evenlope, Element src) throws SOAPException {

        Namespace elNs = src.getNamespace();
        
        SOAPBody body = evenlope.getBody();
        
        String prefix = elNs.getPrefix();
        if (Check.isEmpty(prefix)) {
        	prefix = "ins" + namespaceMap.size();
        }
        namespaceMap.put(elNs.getURI(), prefix);
        SOAPElement dst = body.addBodyElement(evenlope.createName(src.getName(), prefix, elNs.getURI()));
        
        // add the additional declared namespaces
        List declaredNamespaces = src.declaredNamespaces();

        for (int i = 0, size = declaredNamespaces.size(); i < size; i++) {
            Namespace namespace = (Namespace) declaredNamespaces.get(i);

            if (isNamespaceDeclaration(namespace)) {
            	prefix = namespaceMap.get(namespace.getURI());
            	if (prefix == null)	{
            		prefix = namespace.getPrefix();
	                if (Check.isEmpty(prefix)) {
	                	prefix = "ins" + namespaceMap.size();
	                }
	                namespaceMap.put(namespace.getURI(), prefix);
	                dst.addNamespaceDeclaration(prefix, namespace.getURI());
            	}
            }
        }
        appendDOMTree(
        		dst,
        		src.content());

    }

    public void write(SOAPElement dst, Element src) throws SOAPException {
        ArrayList<Element> cont = new ArrayList<Element>(1);
        cont.add(src);
        appendDOMTree(dst, cont);
    }

    protected void appendDOMTree(SOAPElement dst, List content) throws SOAPException {
        int size = content.size();

        for (int i = 0; i < size; i++) {
            Object object = content.get(i);

            if (object instanceof Element) {
                appendDOMTree(dst, (Element) object);
            } else if (object instanceof String) {
                appendDOMTree(dst, (String) object);
            } else if (object instanceof Text) {
                Text text = (Text) object;
                appendDOMTree(dst, text.getText());
            } else if (object instanceof CDATA) {
                appendDOMTree(dst, (CDATA) object);
            } else if (object instanceof Comment) {
                appendDOMTree(dst, (Comment) object);
            }
        }
    }

    protected void appendDOMTree(SOAPElement dst, Element element) throws SOAPException {


        // add the namespace of the element first
        Namespace elementNamespace = element.getNamespace();

        String prefix = namespaceMap.get(elementNamespace.getURI());
        if (prefix == null && isNamespaceDeclaration(elementNamespace)) {
    		prefix = elementNamespace.getPrefix();
            if (Check.isEmpty(prefix)) {
            	prefix = "ins" + namespaceMap.size();
            }
            namespaceMap.put(elementNamespace.getURI(), prefix);
            dst.addNamespaceDeclaration(prefix, elementNamespace.getURI());
        }
      	SOAPElement resElem = dst.addChildElement(element.getName(), prefix);
        
        // add the additional declared namespaces
        List declaredNamespaces = element.declaredNamespaces();

        for (int i = 0, size = declaredNamespaces.size(); i < size; i++) {
            Namespace namespace = (Namespace) declaredNamespaces.get(i);

            if (isNamespaceDeclaration(namespace)) {
            	prefix = namespaceMap.get(namespace.getURI());
            	if (prefix == null)	{
            		prefix = namespace.getPrefix();
	                if (Check.isEmpty(prefix)) {
	                	prefix = "ins" + namespaceMap.size();
	                }
	                namespaceMap.put(namespace.getURI(), prefix);
	                resElem.addNamespaceDeclaration(prefix, namespace.getURI());
            	}
            }
        }

        // add the attributes
        for (int i = 0, size = element.attributeCount(); i < size; i++) {
            Attribute attribute = (Attribute) element.attribute(i);
            String attUri = attribute.getNamespaceURI();
            String attName = attribute.getQualifiedName();
            String value = attribute.getValue();
            resElem.setAttributeNS(attUri, attName, value);
        }

        // add content
        appendDOMTree(resElem, element.content());

    }

    protected void appendDOMTree(SOAPElement dst, CDATA cdata) {
        org.w3c.dom.CDATASection domCDATA = dst.getOwnerDocument().createCDATASection(cdata.getText());
    	dst.appendChild(domCDATA);
    }

    protected void appendDOMTree(SOAPElement dst, Comment comment) {
        org.w3c.dom.Comment domComment = dst.getOwnerDocument().createComment(comment
                .getText());
        dst.appendChild(domComment);
    }

    protected void appendDOMTree(SOAPElement dst, String text) throws SOAPException {
    	dst.addTextNode(text);
    }


    protected String attributeNameForNamespace(Namespace namespace) {
        String xmlns = "xmlns";
        String prefix = namespace.getPrefix();

        if (prefix.length() > 0) {
            return xmlns + ":" + prefix;
        }

        return xmlns;
    }

    protected boolean isNamespaceDeclaration(Namespace ns) {
        if ((ns != null) && (ns != Namespace.NO_NAMESPACE)
                && (ns != Namespace.XML_NAMESPACE)) {
            String uri = ns.getURI();

            if ((uri != null) && (uri.length() > 0)) {
                    return true;
            }
        }

        return false;
    }
}

