package itensil.document.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.record.RecordFormatException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import itensil.document.ExcelXMLProxy;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.RepositoryHelper;
import itensil.scripting.JSActivity;
import itensil.scripting.JSAuthUser;
import itensil.scripting.JSEntities;
import itensil.scripting.JSFiles;
import itensil.scripting.JSQuery;
import itensil.scripting.ScriptHost;
import itensil.scripting.util.JSDomData;
import itensil.scripting.util.JSWebService;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.web.ContentType;
import itensil.web.MethodServlet;
import itensil.web.RequestUtil;
import itensil.web.ServletUtil;
import com.hp.hpl.jena.query.*;
import ch.uzh.ifi.sparqlml.arq.create.query .*;
public class DocumentServlet extends MethodServlet {

	/**
     * Method called for empty and non matching paths
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void defaultWebMethod(
	        HttpServletRequest request,
	        HttpServletResponse response)
        throws ServletException, IOException {
    	
    	String path = request.getPathInfo();
    	if (path.toLowerCase().endsWith(".xls")) {
    		try {
    			HibernateUtil.beginTransaction();
    			String uri = RepositoryHelper.resolveUri(path);
    			//User user = (User)request.getUserPrincipal();
    			String named[] = request.getParameterValues("named");
    			String cells[] = request.getParameterValues("cells");
    			
	    		if ("GET".equals(request.getMethod())) {
	    			response.setContentType("text/xml");
	    			
	    			HibernateUtil.readOnlySession();
	    			ExcelXMLProxy excel;
	    			try {
	    				excel = new ExcelXMLProxy(RepositoryHelper.loadContent(uri));
	    			} catch (RecordFormatException rfe) {
	    				methodException(rfe);
	    				errorXML(request, response, rfe.getCause().getCause());
	    				return;
	    			}
	    			
	    			Document doc = DocumentHelper.createDocument();
	    			Element root = doc.addElement("excel");
	    			root.addAttribute("uri", uri);
	    			
	    			if (Check.isEmpty(named)) {
	    				excel.getNamedGroups(root);
	    			} else {
	    				for (String name : named) {
	    					excel.getNamedGroup(root, name);
	    				}
	    			}
	    			
	    			if (!Check.isEmpty(cells)) {
	    				for (String cellRef : cells) {
	    					excel.getCells(root.addElement("cells"), cellRef);
	    				}
	    			}
	    			ServletUtil.setExpired(response);
	    			doc.write(response.getWriter());

	    		} else if ("PUT".equals(request.getMethod())) {
	    			
					Document doc = XMLDocument.readStream(request.getInputStream());
					ExcelXMLProxy excel;
	    			try {
	    				excel = new ExcelXMLProxy(RepositoryHelper.loadContent(uri));
	    			} catch (RecordFormatException rfe) {
	    				methodException(rfe);
	    				errorXML(request, response, rfe.getCause().getCause());
	    				return;
	    			}
					
					Element root = doc.getRootElement();
					List cellRoots = root.elements("cells");
					if (cellRoots.size() <= 1 && !Check.isEmpty(cells)) {
						Element cellRoot = root.element("cells");
						if (cellRoot == null) cellRoot = root;
						for (String cellRef : cells) {
							if (!Check.isEmpty(cellRef)) excel.setCells(cellRoot, cellRef);
						}
					} else if (cellRoots.size() > 1) {
						for (Object ob : cellRoots) {
							Element cellRoot = (Element)ob;
							String ref = cellRoot.attributeValue("ref");
							if (!Check.isEmpty(ref)) {
								excel.setCells(cellRoot, ref);
							}
						}
					}
					
					for (Object ob : root.elements("Named")) {
						Element nmEl = (Element)ob;
						String ref = excel.getRefForName(nmEl.attributeValue("name"));
						if (ref != null) {
							excel.setCells(nmEl, ref);
						}
					}
					
					if (excel.isDirty()) {
						
						// recalc before writing (maybe slow)
						excel.evaluateAllFormulas();
						
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						excel.getWorkbook().write(bout);
						byte xb[] = bout.toByteArray();
						RepositoryHelper.createContent(
								 RepositoryHelper.getNode(uri, true),
								 new ByteArrayInputStream(xb), xb.length, "application/vnd.ms-excel");
					}
					
					if ("1".equals(request.getParameter("reload"))) {
						response.setContentType("text/xml");
						Document resDoc = DocumentHelper.createDocument();
		    			Element resRoot = resDoc.addElement("excel");
		    			resRoot.addAttribute("uri", uri);
		    			
		    			if (Check.isEmpty(named)) {
		    				excel.getNamedGroups(resRoot);
		    			} else {
		    				for (String name : named) {
		    					excel.getNamedGroup(resRoot, name);
		    				}
		    			}
		    			
		    			if (!Check.isEmpty(cells)) {
		    				for (String cellRef : cells) {
		    					excel.getCells(resRoot.addElement("cells"), cellRef);
		    				}
		    			}
		    			ServletUtil.setExpired(response);
		    			resDoc.write(response.getWriter());
					}
	    		}
	    		HibernateUtil.commitTransaction();
    		} catch (Exception ex) {
				throw new ServletException("Excel proxy error", ex);
			}
    		
    	}
    }
    
    /**
     *  /runScript
     *
     * List folder contents
     *
     */
    @ContentType("text/xml")
    public void webRunScript(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	
    	Map<String, String> params = 
    		RequestUtil.readParameters(request, new String[]{"uri", "path", "arg"});
    	
    	HibernateUtil.beginTransaction();
		String uri = RepositoryHelper.resolveUri(params.get("uri"));
		String path = RepositoryHelper.resolveUri(params.get("path"));
		String arg = Check.emptyIfNull(params.get("arg"));
		
		MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
        NodeContent cont = node.getContent(new DefaultNodeVersion());
        String script = new String(cont.getBytes());
		
    	Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("result");
		User usr = (User)request.getUserPrincipal();
		ScriptHost scr = new ScriptHost("webRunScript", new JSAuthUser(usr));
		scr.defineObject("activity", new JSActivity(null, true));
		scr.defineObject("ws", new JSWebService());
        scr.defineObject("data", new JSDomData(null, null));
        JSFiles files;
        scr.defineObject("files", files = new JSFiles(Check.isEmpty(path) ? UriHelper.getParent(uri) : path, scr));
        scr.defineObject("entities", new JSEntities(usr));
        scr.defineObject("query", new JSQuery(files));
        scr.defineObject("arg", arg);
		
		root.setText(scr.evaluateToString(script));

		HibernateUtil.commitTransaction();
		
		ServletUtil.noCache(response);
		
		doc.write(response.getWriter());
    }
    
    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        HibernateUtil.rollbackTransaction();
    }
    
    public void afterMethod() {
        HibernateUtil.closeSession();
    }

    
}
