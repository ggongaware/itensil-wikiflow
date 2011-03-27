package itensil.scripting.util;

import itensil.io.xml.SoapConnectionImpl;
import itensil.io.xml.SoapDOM4jReader;
import itensil.io.xml.SoapDOM4jWriter;
import itensil.scripting.ScriptHost;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.dom4j.Document;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.DOMReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

public class JSSoapMessage extends ScriptableObject {

	SOAPMessage message;
	SoapDOM4jWriter soapWriter;
	
	
	public JSSoapMessage() {
    	 
        String funcs[] = {"setBody", "deliverTo", "attachFile"};
        try {
        	MessageFactory messageFactory = new org.apache.axis2.saaj.MessageFactoryImpl();
            message = messageFactory.createMessage();
            soapWriter = new SoapDOM4jWriter();
            
            this.defineFunctionProperties(
                funcs,
                JSSoapMessage.class,
                ScriptableObject.PERMANENT |
                ScriptableObject.READONLY);
            
        } catch (RhinoException e) {
            e.printStackTrace();
        } catch (SOAPException e) {
			e.printStackTrace();
		}
    }
	
	public void setBody(Object jsdoc) throws SOAPException {
		org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
		DOMReader dr = new DOMReader();
		Document doc = dr.read(sdoc);
		
		SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        soapWriter.write(envelope, doc.getRootElement());
	}
	
	public org.w3c.dom.Document deliverTo(String uri)
		throws UnsupportedOperationException, SOAPException {
		
		Document doc = DOMDocumentFactory.getInstance().createDocument();
		

		SOAPConnection connection = new SoapConnectionImpl();
		
		try {
			long startTime = System.currentTimeMillis();
			
			SOAPMessage reply = connection.call(message, uri);
			
			long callTime = System.currentTimeMillis() - startTime;
	    	
			ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
	    	
	    	// 80% call time refund on web service calls
	    	icx.extendExpireTime((long)(callTime * 0.8));
			
			SoapDOM4jReader sdr = new SoapDOM4jReader();
	        sdr.importNamespaceMap(soapWriter.getNamespaceMap());
	        sdr.read(doc, reply.getSOAPBody());
		} catch (Exception ex) {
	        ex.printStackTrace();
		} finally {
			connection.close();
		}
		
		return (org.w3c.dom.Document)doc;
	}
	
	public void attachFile(String cid, Object jsByteArray, String mimeType) throws SOAPException {
		
		byte data [] = (byte [])Context.jsToJava(jsByteArray, Object.class);
		AttachmentPart ap = message.createAttachmentPart();
		ap.setContentId(cid);
		ap.setRawContentBytes(data, 0, data.length, mimeType);
	    message.addAttachmentPart(ap);
	}
	
    public String getClassName() {
        return "JSSoapMessage";
    }
}
