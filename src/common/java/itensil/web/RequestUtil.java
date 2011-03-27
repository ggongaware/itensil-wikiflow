package itensil.web;

import itensil.io.xml.XMLDocument;
import itensil.util.Check;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

public class RequestUtil {

	/**
	 * Reads normal posts and some XML formatted like:
	 * 
	 * 	<somerootname>
	 * 		<nameX>valueX</nameX>
	 * 		<nameY>valueY</nameY>
	 *		<nameZ>valueZ</nameZ>
	 *  </somerootname>
	 *  
	 * @param request
	 * @param names
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	public static Map<String, String> readParameters(HttpServletRequest request, String names[])
			throws DocumentException, IOException {
		
		HashMap<String, String> vals = new HashMap<String, String>(names.length);
		boolean isXml = !"GET".equalsIgnoreCase(request.getMethod())
			&& !"application/x-www-form-urlencoded".equals(request.getContentType());
		
		if (isXml) {
			Document doc = XMLDocument.readStream(request.getInputStream());
	        Element root = doc.getRootElement();
	        for (String nm : names) {
				vals.put(nm, root.elementText(nm));
	        }
		} else {
			for (String nm : names) {
				vals.put(nm, request.getParameter(nm));
			}
		}
		return vals;
	}
	
	/**
	 * Reads normal posts and some XML formatted like:
	 * 
	 * 	<somerootname>
	 * 		<nameX>valueX1</nameX>
	 * 		<nameX>valueX2</nameX>
	 * 		<nameZ>valueZ1</nameZ>
	 *		<nameZ>valueZ2</nameZ>
	 *  </somerootname>
	 *  
	 * @param request
	 * @param names
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	
	public static Map<String, String[]> readParameterArrays(HttpServletRequest request, String names[])
			throws DocumentException, IOException {
		
		boolean isXml = !"GET".equalsIgnoreCase(request.getMethod())
			&& !"application/x-www-form-urlencoded".equals(request.getContentType());
		
		if (isXml) {
			Document doc = XMLDocument.readStream(request.getInputStream());
			return readParameterArrays(doc.getRootElement(), names);
		} else {
			HashMap<String, String[]> vals = new HashMap<String, String[]>(names.length);
			for (String nm : names) {
				vals.put(nm, request.getParameterValues(nm));
			}
			return vals;
		}
		
	}
	
	/**
	 * 
	 * @param root
	 * @param names
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String[]> readParameterArrays(Element root, String names[]) {
		HashMap<String, String[]> vals = new HashMap<String, String[]>(names.length);
		if (root != null) {
	        for (String nm : names) {
	        	List<Element> elems = root.elements(nm);
	        	String pvals[] = new String[elems.size()];
	        	for (int ii=0; ii < pvals.length; ii++) {
	        		pvals[ii] = elems.get(ii).getText();
	        	}
				vals.put(nm, pvals);
	        }
		}
        return vals;
	}
	
	/**
	 * @param vals
	 * @return
	 */
	public static String castToString(String vals[]) {
		if (!Check.isEmpty(vals)) return vals[0];
		return null;
	}
}
