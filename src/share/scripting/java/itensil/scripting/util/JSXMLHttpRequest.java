package itensil.scripting.util;


import itensil.scripting.ScriptHost;
import itensil.util.Check;

import java.io.IOException;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSXMLHttpRequest extends ScriptableObject {

	
	static class AnyMethod extends EntityEnclosingMethod {

		String mName;
		
		public AnyMethod(String mName) {
			this.mName = mName;
		}
		
		public String getName() {
			return  mName;
		}
		
	}


	HttpClient http; 
	AnyMethod method;
	String url;
	String response;
	
	public JSXMLHttpRequest() {
		String funcs[] = {
				"open",
				"setRequestHeader",
				"send",
				"getAllResponseHeaders",
				"getResponseHeader"
				};
		
		try {
		   	this.defineFunctionProperties(
	                funcs,
	                JSXMLHttpRequest.class,
	                ScriptableObject.PERMANENT |
	                ScriptableObject.READONLY);
		   	
		   	this.defineProperty("responseText", JSXMLHttpRequest.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		   	this.defineProperty("responseXML", JSXMLHttpRequest.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		   	this.defineProperty("status", JSXMLHttpRequest.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		   	this.defineProperty("statusText", JSXMLHttpRequest.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		   	
        } catch (RhinoException e) {
            e.printStackTrace();
        }
	}
	
	public String getClassName() {
		return "JSXMLHttpRequest";
	}
	
	public void open(String method, String url) {
		this.method = new AnyMethod(method);
		this.url = url;
	}

	public void send(Object obj) throws HttpException, IOException {
		
		if (method == null) return;
		
		response = null;
		
		http = new HttpClient();
		
		// 4 second time outs
    	http.getParams().setSoTimeout(4000);
    	http.getParams().setConnectionManagerTimeout(4000);
    	
		if (obj != null) {
			if (Context.toBoolean(obj)) {
				String str= "";
				if (obj instanceof JSDomData) {
					str = ((JSDomData)obj).doc.asXML();
				} else if (obj instanceof Scriptable) {
					org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		    		Context.jsToJava(obj, org.w3c.dom.Document.class);
					DOMReader dr = new DOMReader();
			    	Document doc = dr.read(sdoc);
			    	str = doc.asXML();
				} else {
					str = Context.toString(obj);
				}
				Header ctype = method.getRequestHeader("Content-Type");
				StringRequestEntity body = new StringRequestEntity(str, ctype != null? ctype.getValue() : "text/xml", method.getRequestCharSet());
				method.setRequestEntity(body);
			}
		}
		method.setDoAuthentication(true);
		method.setURI(new URI(url, true));
		
		try {
			long startTime = System.currentTimeMillis();

			http.executeMethod(method);
			response = method.getResponseBodyAsString();
			
			long callTime = System.currentTimeMillis() - startTime;
			
			ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
        	
        	// 80% call time refund on web calls
        	icx.extendExpireTime((long)(callTime * 0.8));
        	
		} finally {
			method.releaseConnection();
		}

	}
	
	public void setRequestHeader(String header, String value) {
		if (method != null) method.setRequestHeader(header, value);
	}
	
	public String getResponseText() {
		return response;
	}
	
	public org.w3c.dom.Document getResponseXML() throws DocumentException {
		if (Check.isEmpty(response)) {
			return null;
		} else {
			return JSDomData.parseXML(response);
		}
	}

	public String getResponseHeader(String header) {
		Header rhead = method != null ? method.getRequestHeader(header) : null;
		return rhead != null ? rhead.getValue() : null;
	}
	
	public int getStatus() {
		return method != null ? method.getStatusCode() : 0;
	}
	
	public String getStatusText() {
		return method != null ? method.getStatusText() : null;
	}
	
	public String getAllResponseHeaders() {
		if (method != null) {
			StringBuffer buf = new StringBuffer();
			for (Header hd : method.getResponseHeaders()) {
				buf.append(hd.toExternalForm());
			}
			return buf.toString();
		}
		return null;
	}
}
