package itensil.io.xml;

import itensil.util.Check;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;

import org.apache.log4j.Logger;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.w3c.dom.NodeList;

public class SoapDOM4jReader {
	
	private static Logger log = Logger.getLogger(SoapDOM4jReader.class);
	
	private HashMap<String,String> namespaceMap = new HashMap<String,String>();
	
	public SoapDOM4jReader() {
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
	
	public Document read(SOAPBody body) {
		return read(DocumentHelper.createDocument(), body);
	}
	
	public Document read(Document dstDoc, SOAPBody body) {
		NodeList nl = body.getChildNodes();
		if (nl.getLength() > 0) {
			for (int ii = 0, iisize = nl.getLength(); ii < iisize; ii++) {
				if (nl.item(ii).getNodeType() == 1) {
					SOAPElement root = (SOAPElement)nl.item(ii);
					String prefix = namespaceMap.get(root.getNamespaceURI());
					if (prefix == null) {
						if (Check.isEmpty(prefix)) prefix = root.getPrefix();
						if (Check.isEmpty(prefix)) {
			            	prefix = "ins" + namespaceMap.size();
			            }
						namespaceMap.put(prefix, root.getNamespaceURI());
					}
					Element dstRoot = dstDoc.addElement(QName.get(root.getLocalName(), prefix, root.getNamespaceURI()));
					NodeList nodeList = root.getChildNodes();
					
					for (int jj = 0, size = nodeList.getLength(); jj < size; jj++) {
						readTree(nodeList.item(jj), dstRoot);
					}
				}
			}			
		}
		return dstDoc;
	}
	
	 // Implementation methods
    protected void readTree(org.w3c.dom.Node node, Branch current) {
        Element element = null;
        Document document = null;

        if (current instanceof Element) {
            element = (Element) current;
        } else {
            document = (Document) current;
        }

        switch (node.getNodeType()) {
            case org.w3c.dom.Node.ELEMENT_NODE:
                readElement(node, current);

                break;

            case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:

                if (current instanceof Element) {
                    Element currentEl = (Element) current;
                    currentEl.addProcessingInstruction(node.getNodeName(), node
                            .getNodeValue());
                } else {
                    Document currentDoc = (Document) current;
                    currentDoc.addProcessingInstruction(node.getNodeName(),
                            node.getNodeValue());
                }

                break;

            case org.w3c.dom.Node.COMMENT_NODE:

                if (current instanceof Element) {
                    ((Element) current).addComment(node.getNodeValue());
                } else {
                    ((Document) current).addComment(node.getNodeValue());
                }

                break;

            case org.w3c.dom.Node.DOCUMENT_TYPE_NODE:

                org.w3c.dom.DocumentType domDocType 
                        = (org.w3c.dom.DocumentType) node;
                document.addDocType(domDocType.getName(), domDocType
                        .getPublicId(), domDocType.getSystemId());

                break;

            case org.w3c.dom.Node.TEXT_NODE:
                element.addText(node.getNodeValue());

                break;

            case org.w3c.dom.Node.CDATA_SECTION_NODE:
                element.addCDATA(node.getNodeValue());

                break;

            case org.w3c.dom.Node.ENTITY_REFERENCE_NODE:

                // is there a better way to get the value of an entity?
                org.w3c.dom.Node firstChild = node.getFirstChild();

                if (firstChild != null) {
                    element.addEntity(node.getNodeName(), firstChild
                            .getNodeValue());
                } else {
                    element.addEntity(node.getNodeName(), "");
                }

                break;

            case org.w3c.dom.Node.ENTITY_NODE:
                element.addEntity(node.getNodeName(), node.getNodeValue());

                break;

            default:
            	log.warn("Unknown DOM node type: " + node.getNodeType());

        }
    }

    protected void readElement(org.w3c.dom.Node node, Branch current) {
    	
        String namespaceUri = node.getNamespaceURI();
        String elementPrefix = namespaceMap.get(namespaceUri);

        if (elementPrefix == null) {
            elementPrefix = "";
        }

        org.w3c.dom.NamedNodeMap attributeList = node.getAttributes();

        if ((attributeList != null) && (namespaceUri == null)) {
            // test if we have an "xmlns" attribute
            org.w3c.dom.Node attribute = attributeList.getNamedItem("xmlns");

            if (attribute != null) {
                namespaceUri = attribute.getNodeValue();
                elementPrefix = "";
            }
        }

        QName qName = QName.get(node.getLocalName(), elementPrefix, namespaceUri);
        Element element = current.addElement(qName);

        if (attributeList != null) {
            int size = attributeList.getLength();
            ArrayList<org.w3c.dom.Node> attributes = new ArrayList<org.w3c.dom.Node>(size);

            for (int i = 0; i < size; i++) {
                org.w3c.dom.Node attribute = attributeList.item(i);

                // Define all namespaces first then process attributes later
                String name = attribute.getNodeName();

                if (name.startsWith("xmlns")) {
                    
                    String uri = attribute.getNodeValue();
                    String prefix = namespaceMap.get(uri);
                    if (prefix == null)	{
                    	prefix = getPrefix(name);
                    	if (Check.isEmpty(prefix)) {
    	                	prefix = "ins" + namespaceMap.size();
    	                }
                    	namespaceMap.put(uri, prefix);
                    }
                    element.addNamespace(prefix, uri);
                } else {
                    attributes.add(attribute);
                }
            }

            // now add the attributes, the namespaces should be available
            size = attributes.size();

            for (int i = 0; i < size; i++) {
                org.w3c.dom.Node attribute = (org.w3c.dom.Node) attributes.get(i);
                String name = attribute.getNodeName();
                String attPrefix = namespaceMap.get(attribute.getNamespaceURI());
                if (attPrefix == null)	{
                	attPrefix = getPrefix(name);
                	if (Check.isEmpty(attPrefix)) {
                		attPrefix = "ins" + namespaceMap.size();
	                }
                	namespaceMap.put(attribute.getNamespaceURI(), attPrefix);
                }
                
                QName attributeQName =  QName.get(attribute.getLocalName(), attPrefix, attribute.getNamespaceURI());
                element.addAttribute(attributeQName, attribute.getNodeValue());
            }
        }

        // Recurse on child nodes
        org.w3c.dom.NodeList children = node.getChildNodes();

        for (int i = 0, size = children.getLength(); i < size; i++) {
            org.w3c.dom.Node child = children.item(i);
            readTree(child, element);
        }

    }
    
    private String getPrefix(String xmlnsDecl) {
        int index = xmlnsDecl.indexOf(':', 5);

        if (index != -1) {
            return xmlnsDecl.substring(index + 1);
        } else {
            return "";
        }
    }
}
