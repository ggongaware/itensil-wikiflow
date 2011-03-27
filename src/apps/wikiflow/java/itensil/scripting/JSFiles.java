
package itensil.scripting;
import itensil.document.ExcelXMLProxy;
import itensil.report.ExslTransform;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodePermission;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.NodePermission;
import itensil.repository.NodeVersion;
import itensil.repository.NotFoundException;
import itensil.repository.Repository;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryManagerFactory;
import itensil.repository.RepositoryNode;
import itensil.repository.hibernate.PermissionEntity;
import itensil.scripting.util.JSDomData;
import itensil.security.Everyone;
import itensil.security.SecurityAssociation;
import itensil.security.SignOn;
import itensil.security.SignOnFactory;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.UriHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.xml.sax.InputSource;

public class JSFiles extends ScriptableObject  {

	String path;
	ScriptHost host;
	
	static Logger log = Logger.getLogger(JSFiles.class);
	
	public JSFiles(String path, ScriptHost host) {
		
		this.path = path;
		this.host = host;
		
		String funcs[] = {
				"loadXML",
				"saveXML", 
				"loadBytes",
				"saveBytes", 
				"getMimeType",
				"exists",
				"getId",
				"getUri",
				"createFile",
				"createFolder",
				"createPrivateFile",
				"createPrivateFolder",
				"copy",
				"move",
				"remove",
				"getPath",
				"setPath",
				"createPermission",
				"getPermissions",
				"grantPermission",
				"revokePermission",
				"xslt",
				"list",
				"versionList",
				"versionSet",
				"versionsPrune",
				"xsltBytes",
				"loadExcel", 
				"saveExcel",
				"uriName",
				"uriParent",
				"runScript",
				"runScriptAsOwner"};
	    try {
	        this.defineFunctionProperties(
	            funcs,
	            JSFiles.class,
	            ScriptableObject.PERMANENT |
	            ScriptableObject.READONLY);
	    } catch (RhinoException e) {
	        e.printStackTrace();
	    }
	    sealObject();
	}
	
	protected String resolveUri(String uri) {
		String res = UriHelper.absoluteUri(path, uri);
		return RepositoryHelper.resolveUri(res);
	}
	
	public byte [] loadBytes(String uri) 
			throws AccessDeniedException, NotFoundException, LockException {
		
		uri = resolveUri(uri);
		Repository repository = RepositoryHelper.getRepository(uri);
        RepositoryNode node = repository.getNodeByUri(uri, false);
        NodeContent content =
            repository.getContent(node.getNodeId(), new DefaultNodeVersion());
        return content.getBytes();
	}
	
	
    
    public org.w3c.dom.Document loadXML(String uri) throws Exception {
    	Document result = null;
    	uri = resolveUri(uri);
    	SAXReader reader = new SAXReader(DOMDocumentFactory.getInstance());
    	result = reader.read(RepositoryHelper.loadContent(uri));
    	
    /*	String encoding = getEncoding(text);

    	InputSource source = new InputSource(new StringReader(text));
    	source.setEncoding(encoding);

    	result = reader.read(source);

    	// if the XML parser doesn't provide a way to retrieve the encoding,
    	// specify it manually
    	if (result.getXMLEncoding() == null) {
    	    result.setXMLEncoding(encoding);
    	} */

    	return (org.w3c.dom.Document)result;
    }
    
    /**
	 * Does a file or folder exist?
	 * 
	 * @param {String} uri - file path
	 * @return {Boolean} true if the file exists
	 */
    public boolean exists(String uri) throws Exception {
    	uri = resolveUri(uri);
    	try {
    		RepositoryHelper.getNode(uri, false);
    		return true;
    	} catch (NotFoundException nfe) {
    		return false;
    	}
    }
    
    /**
	 * Get the node id of a file or folder
	 * 
	 * @param {String} uri - file path
	 */
    public String getId(String uri) throws Exception {
    	uri = resolveUri(uri);
    	return RepositoryHelper.getNode(uri, false).getNodeId();
    }
    
    /**
	 * Get the node id of a file or folder
	 * 
	 * @param {String} id - node id
	 */
    public String getUri(String id) throws Exception {
    	return RepositoryHelper.getNodeById(id, false).getUri();
    }
    
    public String createFile(String uri) throws Exception {
    	uri = resolveUri(uri);
    	Repository repo = RepositoryHelper.getRepository(uri);
    	return repo.createNode(uri, false, SecurityAssociation.getUser()).getUri();
    }
    
    public String createFolder(String uri) throws Exception {
    	uri = resolveUri(uri);
    	return RepositoryHelper.createCollection(uri).getUri();
    }
    
    
    public String createPrivateFile(String uri) throws Exception {
    	uri = resolveUri(uri);
    	Repository repo = RepositoryHelper.getRepository(uri);
    	MutableRepositoryNode node = repo.createNode(uri, false, SecurityAssociation.getUser());
    	node.grantPermission(DefaultNodePermission.noPermission(new Everyone()));
    	return node.getUri();
    }
    
    public String createPrivateFolder(String uri) throws Exception {
    	uri = resolveUri(uri);
    	MutableRepositoryNode node = RepositoryHelper.createCollection(uri);
    	node.grantPermission(DefaultNodePermission.noPermission(new Everyone()));
    	return node.getUri();
    }
    
    public void copy(String uri, String dstUri) throws Exception {
    	uri = resolveUri(uri);
    	dstUri = resolveUri(dstUri);
    	RepositoryHelper.copy(uri, dstUri, true);
    }
    
    public void move(String uri, String dstUri) throws Exception {
    	uri = resolveUri(uri);
    	dstUri = resolveUri(dstUri);
    	RepositoryHelper.move(uri, dstUri);
    }
   
    public void remove(String uri) throws Exception {
    	uri = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(uri, true);
    	repoNode.remove();
    }
    
    public String getMimeType(String uri) {
    	return RepositoryManagerFactory.getMimeType(uri);
    }
    
    public void saveXML(String uri, Object jsdoc) throws Exception {
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
    		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	Document doc = dr.read(sdoc);
    	byte xb[] = doc.asXML().getBytes("UTF-8");
    	uri = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(uri, true);
    	
        RepositoryHelper.createContent(
        		repoNode, new ByteArrayInputStream(xb), xb.length, 
        		RepositoryManagerFactory.getMimeType(uri));
    }
    
    public void saveBytes(String uri, Object jsByteArray) throws Exception {
    	
    	byte data [] = (byte [])Context.jsToJava(jsByteArray, Object.class);
    	uri = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(uri, true);
    	
        RepositoryHelper.createContent(
        		repoNode, new ByteArrayInputStream(data), data.length, 
        		RepositoryManagerFactory.getMimeType(uri));
    }
    
    public String getPath() {
    	return path;
    }
    
    public String setPath(String path) throws Exception {
    	String resPath = resolveUri(path);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(resPath, false);
    	if (repoNode.isCollection()) {
    		this.path = resPath;
    	}
    	return this.path;
    }
    
    
    public JSPermission createPermission(Object principal) {
    	JSPermission perm = new JSPermission();
    	if (principal instanceof JSAuthUser) {
    		perm.perm.setPrincipalId(((JSAuthUser)principal).getUserId());
    		return perm;
    	} else if (principal instanceof JSGroup) {
    		perm.perm.setPrincipalId(((JSGroup)principal).getGroupId());
    		perm.perm.setGroup(true);
    		return perm;
    	} else {
    		String prstr = Context.toString(principal);
    		if (JSPermission.GROUP_EVERYONE.equals(prstr)) {
    			perm.perm.setPrincipalId(JSPermission.EVERYONE.getGroupId());
    			perm.perm.setGroup(true);
    			return perm;
    		} else if (JSPermission.GROUP_RELATIVE.equals(prstr)) {
    			perm.perm.setPrincipalId(JSPermission.RELATIVE.getGroupId());
    			perm.perm.setGroup(true);
    			return perm;
    		} else if (!Check.isEmpty(prstr) && prstr.length() == 20) {
    			perm.perm.setPrincipalId(prstr);
    			return perm;
    		}
    	}
    	return null;
    }
    
    
    public JSPermission [] getPermissions(String uri) throws NotFoundException, AccessDeniedException {
    	String resPath = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(resPath, false);
    	NodePermission nps[] = repoNode.getPermissions();
    	JSPermission jps[] = new JSPermission[nps.length];
    	int ii = 0;
    	for (NodePermission perm : nps) {
    		jps[ii++] = new JSPermission(new PermissionEntity(perm));
    	}
    	return jps;
    }
    
    public void grantPermission(String uri, JSPermission perm) throws NotFoundException, AccessDeniedException {
    	String resPath = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(resPath, false);
    	repoNode.grantPermission(perm.perm);
    }
    
    public void revokePermission(String uri, JSPermission perm) throws NotFoundException, AccessDeniedException {
    	String resPath = resolveUri(uri);
    	MutableRepositoryNode repoNode = RepositoryHelper.getNode(resPath, false);
    	repoNode.revokePermission(perm.perm);
    }

    public org.w3c.dom.Document xslt(Object data, Object xsl, Object oparams) throws Exception {
    	
    	DocumentResult docRes = new DocumentResult(new SAXContentHandler(DOMDocumentFactory.getInstance()));
    	
    	DocumentSource datXml;
    	DocumentSource xslDoc;
    	
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(data, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	datXml = new DocumentSource(dr.read(sdoc));
    	
    	sdoc = (org.w3c.dom.Document) Context.jsToJava(xsl, org.w3c.dom.Document.class);
    	xslDoc = new DocumentSource(dr.read(sdoc));
    	
    	HashMap<String,String> params = null;
    	
    	if (Context.toBoolean(oparams)) {
    		Scriptable xslparams = (Scriptable)oparams;
	    	Object pids[] = xslparams.getIds();
	    	params = new HashMap<String,String>(pids.length);
	        for (int ii = 0; ii < pids.length; ii++) {
	            String pid = pids[ii].toString();
	            params.put(pid, Context.toString(xslparams.get(pid, xslparams)));
	        }
    	}
    	
    	long startTime = System.currentTimeMillis();
    	try {
    		ExslTransform.transformXML(docRes, datXml, xslDoc, getPath(), params);
    	} catch (Exception ex) {
    		log.warn("User XSL error.", ex);
    		throw ex;
    	}
    	
    	long callTime = System.currentTimeMillis() - startTime;
    	
    	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
    	
    	// 50% call time refund for slow XLSTs
    	icx.extendExpireTime((long)(callTime * 0.5));
    	
    	return (org.w3c.dom.Document) docRes.getDocument();
    }
    
    

    public byte [] xsltBytes(Object data, Object xsl, Object oparams) throws Exception {
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	StreamResult sRes = new StreamResult(out);
    	
    	DocumentSource datXml;
    	DocumentSource xslDoc;
    	
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(data, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	datXml = new DocumentSource(dr.read(sdoc));
    	
    	sdoc = (org.w3c.dom.Document) Context.jsToJava(xsl, org.w3c.dom.Document.class);
    	xslDoc = new DocumentSource(dr.read(sdoc));
    	
    	HashMap<String,String> params = null;
    	
    	if (Context.toBoolean(oparams)) {
    		Scriptable xslparams = (Scriptable)oparams;
	    	Object pids[] = xslparams.getIds();
	    	params = new HashMap<String,String>(pids.length);
	        for (int ii = 0; ii < pids.length; ii++) {
	            String pid = pids[ii].toString();
	            params.put(pid, Context.toString(xslparams.get(pid, xslparams)));
	        }
    	}
    	
    	long startTime = System.currentTimeMillis();
    	try {
    		ExslTransform.transformXML(sRes, datXml, xslDoc, getPath(), params);
    	} catch (Exception ex) {
    		log.warn("User XSL error.", ex);
    		throw ex;
    	}
    	
    	long callTime = System.currentTimeMillis() - startTime;
    	
    	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
    	
    	// 50% call time refund for slow XLSTs
    	icx.extendExpireTime((long)(callTime * 0.5));
    	
    	return out.toByteArray();
    }
    
    public org.w3c.dom.Document loadExcel(String uri, Scriptable jsCellRefs) throws Exception {
    	
    	String cellRefs[];
    	
    	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
    	
    	if ("String".equals(jsCellRefs.getClassName())) {
    		cellRefs = new String[]{ Context.toString(jsCellRefs) };
    	} else {
	    	Object jsObjs[] = icx.getElements(jsCellRefs);
	    	
	    	cellRefs = new String[jsObjs.length];
			for (int ii = 0; ii < jsObjs.length; ii++) {
				cellRefs[ii] = Context.toString(jsObjs[ii]);
			}
    	}
		
    	long startTime = System.currentTimeMillis();
    	
    	ExcelXMLProxy excel = new ExcelXMLProxy(RepositoryHelper.loadContent(resolveUri(uri)));
    	Document doc = DOMDocumentFactory.getInstance().createDocument();
		Element root = doc.addElement("excel");
		
    	if (Check.isEmpty(cellRefs)) {
    		excel.getNamedGroups(root);
    	} else {
    		for (String cr : cellRefs) {
    			if (!Check.isEmpty(cr)) excel.getCells(root.addElement("cells"), cr);
    		}
    	}
    	
    	long callTime = System.currentTimeMillis() - startTime;

    	
    	// 60% call time refund for slow Excel libs
    	icx.extendExpireTime((long)(callTime * 0.6));
    		
    	return (org.w3c.dom.Document) doc;
    }
	
    
    public void saveExcel(String uri, Object jsdoc) throws Exception {
    	
    	org.w3c.dom.Document sdoc = (org.w3c.dom.Document)
		Context.jsToJava(jsdoc, org.w3c.dom.Document.class);
    	DOMReader dr = new DOMReader();
    	Document doc = dr.read(sdoc);
    	Element root = doc.getRootElement();
    	uri = resolveUri(uri);
    	
    	long startTime = System.currentTimeMillis();
    	
    	ExcelXMLProxy excel = new ExcelXMLProxy(RepositoryHelper.loadContent(uri));

    	for (Object ob : root.elements("cells")) {
			Element cellRoot = (Element)ob;
			String ref = cellRoot.attributeValue("ref");
			if (!Check.isEmpty(ref)) {
				excel.setCells(cellRoot, ref);
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
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			excel.getWorkbook().write(bout);
			byte xb[] = bout.toByteArray();
			RepositoryHelper.createContent(
					 RepositoryHelper.getNode(uri, true),
					 new ByteArrayInputStream(xb), xb.length, "application/vnd.ms-excel");
		}
    	
    	long callTime = System.currentTimeMillis() - startTime;
    	
    	ScriptHost.ItensilContext icx = (ScriptHost.ItensilContext)Context.getCurrentContext();
    	
    	// 60% call time refund for slow Excel libs
    	icx.extendExpireTime((long)(callTime * 0.6));
    }
    
    public String[] list(String uri) throws AccessDeniedException, NotFoundException {
    	uri = resolveUri(uri);
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
    	String kUris[];
    	if (node.isCollection()) {
    		RepositoryNode kids[] = node.getChildren();
    		kUris = new String[kids.length];
    		for (int ii = 0; ii < kids.length; ii++) {
    			String ku = kids[ii].getUri();
    			kUris[ii] = kids[ii].isCollection() ? ku + "/" : ku;
    		}
    	} else {
    		kUris = new String[0];
    	}
    	return kUris;
    }
    
    public String[] versionList(String uri) throws AccessDeniedException, NotFoundException {
    	uri = resolveUri(uri);
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
    	NodeVersion vers[] = node.getVersions();
    	String vlist[] = new String[vers.length];
    	for (int ii = 0; ii < vers.length; ii++) {
    		vlist[ii] = vers[ii].getNumber();
    	}
    	return vlist;
    }
    
    public void versionSet(String uri, String version) throws AccessDeniedException, NotFoundException, LockException {
    	uri = resolveUri(uri);
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, true);
    	node.setDefaultVersion(new DefaultNodeVersion(version, false));
    }
    
    public void versionsPrune(String uri, int keepRecentCount) throws AccessDeniedException, LockException, NotFoundException {
    	uri = resolveUri(uri);
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, true);
    	node.pruneVersions(keepRecentCount);
    }
    
    public String runScript(String uri) throws AccessDeniedException, LockException, NotFoundException, ScriptError {
    	
    	uri = resolveUri(uri);
    	
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
        NodeContent cont = node.getContent(new DefaultNodeVersion());
        String script = new String(cont.getBytes());
        
    	return host.evaluateToString(script);
    }
    
    
    public String runScriptAsOwner(String uri) throws AccessDeniedException, LockException, NotFoundException, ScriptError, UserSpaceException {
    	
    	uri = resolveUri(uri);
    	
    	MutableRepositoryNode node = RepositoryHelper.getNode(uri, false);
    	boolean hasEveryone = false;
    	Everyone ev = new Everyone();
    	for (NodePermission perm : node.getPermissions()) {
    		if (ev.equals(perm.getPrincipal()) && perm.isNone()) {
    			hasEveryone = true;
    			break;
    		}	
    	}
    	if (!hasEveryone) {
    		throw new AccessDeniedException(uri, "Run as owner not allowed without Everyone = Deny");
    	}
        NodeContent cont = node.getContent(new DefaultNodeVersion());
        String script = new String(cont.getBytes());
        int idx = script.indexOf("RUN_AS_OWNER=1");
        if (idx < 0 || idx > 100)
        	throw new AccessDeniedException(uri, "Run as owner not allowed from source");
       
        User cur = SecurityAssociation.getUser();
        User owner = SecurityAssociation.getUser().getUserSpace().resolve(node.getOwner());
        
        if (owner != null && !SysAdmin.isSysAdmin(owner)) {
        	try {
        		SecurityAssociation.setUser(owner);
        		return host.evaluateToString(script);
        	} finally {
        		SecurityAssociation.setUser(cur);
        	}
        }
        return null;
    }
    
    
    public String uriName(String uri) {
    	return UriHelper.name(uri);
    }
    
    public String uriParent(String uri) {
    	return UriHelper.getParent(uri);
    }
	
	public String getClassName() {
		return "JSFiles";
	}

}
