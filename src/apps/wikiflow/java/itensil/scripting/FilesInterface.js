/**
 * (c) 2009 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Files Server Interface
 */
 
 
/**
  * FilesInterface stub
  * 
  * Implementation included in all scopes as <b>files</b>. 
  * 
  * @constructor (private)
  */
function FilesInterface() { }
 
FilesInterface.prototype = {
	
	/**
	 * Load XML file contents
	 * 
	 * @param {String} uri - file path
	 * @return {w3c-dom} XML file contents
	 * @throws Exception on file missing, permission restriction, malformed XML content
	 */
	loadXML : function(uri) {
		return "<w3c-dom-xml/>";
	},
	
	/**
	 * Save XML File contents
	 * 
	 * @param {String} uri - file path
	 * @param {w3c-dom} xmlDoc - XML document
	 * @throws Exception on file missing and permission restrictions
	 * 
	 */
	saveXML : function(uri, xmlDoc) {
		/* no return */
	}, 
	
	/**
	 * @param {String} uri - file path
	 * @return {byte-array} - file contents
	 * @throws Exception on file missing and permission restrictions
	 */
	loadBytes : function(uri) {
		return "{byte-array}";
	},
	
	/**
	 * @param {String} uri - file path
	 * @param {byte-array} bytes - file contents
	 * @throws Exception on file missing and permission restrictions
	 */
	saveBytes : function(uri, bytes) {
		/* no return */
	}, 
	
	/**
	 * Get the server recognized mime-type for a file name
	 * 
	 * @param {String} uri - file path
	 * @return {String} mime-type
	 */
	getMimeType : function(uri) {
		return "mime/type";
	},
	
	/**
	 * Does a file or folder exist?
	 * 
	 * @param {String} uri - file path
	 * @return {Boolean} true if the file exists
	 */
	exists : function(uri) {
		return false;
	},
	
	/**
	 * Create a new file
	 * 
	 * @param {String} uri - file path
	 * @return {String} file path
	 * @throws Exception on duplicate file, parent missing, and permission restrictions
	 */
	createFile : function(uri) {
		return "string path"
	},
	
	/**
	 * Create a new folder
	 *
	 * @param {String} uri - file path
	 * @return {String} file path
	 * @throws Exception on duplicate file, parent missing, and permission restrictions
	 */
	createFolder : function(uri) {
		return "string path"
	},
	
	/**
	 * Create a new file, with permission Everyone deny
	 * 
	 * @param {String} uri - file path
	 * @return {String} file path
	 * @throws Exception on duplicate file, parent missing, and permission restrictions
	 */
	createPrivateFile : function(uri) {
		return "string path"
	},
	
	/**
	 * Create a new folder, with permission Everyone deny
	 *
	 * @param {String} uri - file path
	 * @return {String} file path
	 * @throws Exception on duplicate file, parent missing, and permission restrictions
	 */
	createPrivateFolder : function(uri) {
		return "string path"
	},
	
	/**
	 * Copy file or folder
	 * 
	 * @param {String} uri - source file path
	 * @param {String} dstUri - destination file path
	 * @throws Exception on duplicate file and permission restrictions
	 */
	copy : function(uri, dstUri) {
		/* no return */
	},
	
	/**
	 * Move file or folder
	 * 
	 * @param {String} uri - source file path
	 * @param {String} dstUri - destination file path
	 * @throws Exception on duplicate file and permission restrictions
	 */
	move : function(uri, dstUri) {
		/* no return */
	},
	
	/**
	 * Remove file or folder
	 * 
	 * @param {String} uri - file path
	 * @throws Exception on permission restrictions
	 */
	remove : function(uri) {
		/* no return */
	},
	
	/**
	 * Get the node id for a file or folder
	 * 
	 * @param {String} uri - file path
	 * @throws Exception on missing and permission restrictions
	 */
	getId : function(uri) {
		return "string id";
	},
	
	/**
	 * Get the uri for a node id
	 * 
	 * @param {String} id - file or folder node id
	 * @throws Exception on missing and permission restrictions
	 */
	getUri : function(id) {
		return "string path";
	},
	
	
	/**
	 * Get the current context path
	 * 
	 * @return {String} file path
	 */
	getPath : function() {
		return "string path";
	},
	
	/**
	 * Set the current context path
	 * 
	 * @param {String} uri - file path
	 * @return {String} file path
	 */
	setPath : function(uri) {
		return "string path";
	},
	
	
	/**
	 * Set the current context path
	 * 
	 * @param {UserInterface} or {GroupInterface} or {String} principal - the "who" for the permission
	 * @return {PermissionInterface} permission object
	 */
	createPermission : function(principal) {
		return new PermissionInterface(principal);
	},
	
	/**
	 * Get permissions for a folder or file
	 * 
	 * @param {String} uri - file path
	 * @return {PermissionInterface []} permission objects
	 * @throws Exception on missing and permission restriction
	 */
	getPermissions : function(uri) {
		return [ new PermissionInterface() ];
	},
	
	/**
	 * Grant a permission on a folder or file
	 * 
	 * @param {String} uri - file path
	 * @param {PermissionInterface} perm - permission object
	 * @throws Exception on missing and permission restriction
	 */
	grantPermission : function(uri, perm) {
		/* no return */
	},
	
	/**
	 * Revoke a permission on a folder or file
	 * 
	 * @param {String} uri - file path
	 * @param {PermissionInterface} perm - permission object
	 * @throws Exception on missing and permission restriction
	 */
	revokePermission : function(uri, perm) {
		/* no return */
	},
	
	
	/**
	 * Tranform XML document to a different XML document
	 * 
	 * @example files.xslt(myData, myXslt, {"param1":"val1", "param2":"val2"});
	 * 
	 * @param {w3c-dom} xmlData - XML document to be transformed
	 * @param {w3c-dom} xmlTransform - XSLT XML document
	 * @param {Object} params - pass named parameters to XSLT
	 * @return {w3c-dom} resulting XML contents
	 * @throws Exception on invalid XSLT 
	 */
	xslt : function(xmlData, xmlTransform, params) {
		return "<w3c-dom-xml/>";
	},
	
	
	/**
	 * Tranform XML document to any XSLT support output format
	 * 
	 * @param {w3c-dom} xmlData - XML document to be transformed
	 * @param {w3c-dom} xmlTransform - XSLT XML document
	 * @param {Object} params - pass named parameters to XSLT
	 * @return {byte-array} resulting contents
	 * @throws Exception on invalid XSLT
	 */
	xsltBytes : function(xmlData, xmlTransform, params) {
		return "{byte-array}";
	},
	
	/**
	 * List the file and folders within a path.
	 * 
	 * @example files.list('/home/path')
	 * > ["file1", "file2", "folder/"]
	 * 
	 * @param {String} uri - file path
	 * @return {String []} file path array
	 * @throws Exception on folder missing and permission restrictions
	 */
	list : function(uri) {
		return ["file1", "folder/"];
	},
	
	/**
	 * Map Excel file into XML
	 * 
	 * @example files.loadExcel('/home/sample.xls', ['A1:D7', 'F19:L99']);
	 * 
	 * Sample output:
	 * 
	 *  &lt;excel>
	 * 		&lt;cells refs="B2:D4">
	 *			&lt;B2>b2&lt;/B2>&lt;C2>c2&lt;/C2>&lt;D2>d2&lt;/D2>
	 *			&lt;B3>b3&lt;/B3>&lt;C3>c3&lt;/C3>&lt;D3>d3&lt;/D3>
	 *			&lt;B4>b4&lt;/B4>&lt;C4>c4&lt;/C4>&lt;D4>d4&lt;/D4>
	 *		&lt;/cells>
	 *  &lt;/excel>
	 * 
	 * 
	 * @param {String} uri - file path of .xls file
	 * @param {String []} cellRefs - array of cell references
	 * @return {w3c-dom} XML document mapping cell contents
	 * @throws Exception on file missing, permission restrictions, and invalid .xls file
	 */
	loadExcel : function(uri, cellRefs) {
		return "<w3c-dom-xml/>";
	}, 	
	
	/**
	 * Map XML into Excel file. Format same as sample output of loadExcel
	 * 
	 * @param {String} uri - file path of .xls file
	 * @param {w3c-dom} xmlDoc - XML document mapping cell contents
	 * @throws Exception on file missing, permission restrictions, and invalid .xls file
	 */
	saveExcel : function(uri, xmlDoc) { 
		/* no return */
	},
	
	/**
	 * Name portion of uri
	 * 
	 * @param {String} uri - file path
	 * @return {String} name portion of file path
	 */
	uriName : function(uri) {
		return "string name";
	},
	
	/**
	 * Parent of uri
	 * 
	 * @param {String} uri - file path
	 * @return {String} parent path portion of file path
	 */
	uriParent : function(uri) {
		return "string path";
	},
	
	/**
	 * Get the revision number list for a file
	 * 
	 * @param {String} uri - file path
	 * @return {String []} revision numbers array
	 * @throws Exception on file missing and permission restrictions
	 */
	versionList : function(uri) {
		return ["1.0", "1.1"];
	},
	
	/**
	 * Set the active revision number for a file
	 * 
	 * @param {String} uri - file path
	 * @param {String} version - revision number
	 * @throws Exception on file missing and permission restrictions
	 */
	versionSet : function(uri, version) {
		/* no return */
	},
	
	/**
	 * Remove all but the X most recent revisions of a file
	 * 
	 * @param {String} uri - file path
	 * @param {int} keepRecentCount - keep how many versions total
	 * @throws Exception on file missing and permission restrictions
	 */
	versionsPrune : function(uri, keepRecentCount) {
		/* no return */
	},

	/**
	 * Execution a ECMAScript file with-in the current scope.
	 * 
	 * @param {String} uri - file path of script source file
	 * @return {String} result of execution script file
	 * @throws Exception on file missing, permission restrictions, invalid script source, and sub-exceptions
	 */
	runScript : function(uri) {
		return "string result";
	},
	
	/**
	 * Execution a ECMAScript file with-in the current scope with security of the file's owner.
	 * 
	 * For security, the source code of the script file must have a header with the comment:
	 * 
	 * // RUN_AS_OWNER=1
	 * 
	 * And must have the permssion Everyone = Deny
	 * 	- This forces individual user and group access to be granted read on this script
	 * 
	 * 
	 * @param {String} uri - file path of script source file
	 * @return {String} result of execution script file
	 * @throws Exception on file missing, permission restrictions, invalid script source, and sub-exceptions
	 */
	runScriptAsOwner : function(uri) {
		return "string result";
	}

};

 
 