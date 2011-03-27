package itensil.scripting.util;

import java.io.IOException;

import itensil.scripting.ScriptHost;
import itensil.scripting.ScriptHost.ItensilContext;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.xml.security.utils.Base64;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author ggongaware@itensil.com
 *
 */
public class JSWebService extends ScriptableObject {

	OMFactory fac;
	ServiceClient client;
	
	static java.security.MessageDigest messageDigester = null;
	
	static {
		try {
			messageDigester = java.security.MessageDigest.getInstance("SHA-1");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    public JSWebService() {
    	fac = OMAbstractFactory.getOMFactory();
    	 
        String funcs[] = {"soapCall", "createSoapMessage", "sha1HashBytes",
        		"requestGetString", "requestGetBytes", "getXMLHttpRequest"};
        try {
        	client = new ServiceClient();
        	
            this.defineFunctionProperties(
                funcs,
                JSWebService.class,
                ScriptableObject.PERMANENT |
                ScriptableObject.READONLY);
        } catch (RhinoException e) {
            e.printStackTrace();
        }  catch (AxisFault ae) {
			ae.printStackTrace();
		}
    }

    public String soapCall(String serviceUrl, String namespace, String operation, Scriptable parameters)
            throws Exception {
    	
    	EndpointReference targetEPR = new EndpointReference(serviceUrl);
    	OMNamespace omNs = fac.createOMNamespace(namespace, "ns1");
    	
    	OMElement method = fac.createOMElement(operation, omNs);

        // enum the JSobject into SOAP parametes
        Object pids[] = parameters.getIds();
        for (int ii = 0; ii < pids.length; ii++) {
            String pid = pids[ii].toString();
            OMElement paramEl = fac.createOMElement(pid, omNs);
            paramEl.setText(Context.toString(parameters.get(pid, parameters)));
            method.addChild(paramEl);
        }
        
        Options options = new Options();
        options.setTo(targetEPR);
        options.setAction(namespace + operation);

    	client.setOptions(options);
    	
    	long startTime = System.currentTimeMillis();
    	
    	OMElement result = client.sendReceive(method);
    	
    	long callTime = System.currentTimeMillis() - startTime;
    	
    	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
    	
    	// 80% call time refund on web service calls
    	icx.extendExpireTime((long)(callTime * 0.8));
        
        return result == null ? null : result.getFirstElement().getText();
    }
   
    public String requestGetString(String url) throws HttpException, IOException {
    	return (String)requestGet(url, true);
    }
    
    public byte [] requestGetBytes(String url) throws HttpException, IOException {
    	return (byte [])requestGet(url, false);
    }
    
    protected Object requestGet(String url, boolean asString) throws HttpException, IOException {

    	HttpClient http = new HttpClient();
    	
    	// 4 second time outs
    	http.getParams().setSoTimeout(4000);
    	http.getParams().setConnectionManagerTimeout(4000);

    	GetMethod get = new GetMethod(url);

        // Tell the GET method to automatically handle authentication. The
        // method will use any appropriate credentials to handle basic
        // authentication requests.  Setting this value to false will cause
        // any request for authentication to return with a status of 401.
        // It will then be up to the client to handle the authentication.
        get.setDoAuthentication( true );
        Object body = null;
        try {
        	long startTime = System.currentTimeMillis();
            // execute the GET
            http.executeMethod( get );

            // print the status and response
            body = asString ? get.getResponseBodyAsString() : get.getResponseBody();
            
        	long callTime = System.currentTimeMillis() - startTime;
        	
        	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
        	
        	// 80% call time refund on web service calls
        	icx.extendExpireTime((long)(callTime * 0.8));

        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
        return body;
    }
    
    public String sha1HashBytes(Object jsByteArray) {
    	byte data [] = (byte [])Context.jsToJava(jsByteArray, Object.class);
    	byte[] rawDigest = messageDigester.digest(data);
		return Base64.encode(rawDigest);
    }
    
    public JSSoapMessage createSoapMessage() {
    	return new JSSoapMessage();
    }
    
    public JSXMLHttpRequest getXMLHttpRequest() {
    	return new JSXMLHttpRequest();
    }
    

    public String getClassName() {
        return "JSWebService";
    }
    
}
