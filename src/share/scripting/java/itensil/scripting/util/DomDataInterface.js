/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * DomData Server Interface
 */
 
 /**
  * DomDataInterface stub
  * 
  * Implementation included in all scopes as <b>data</b>. 
  * In an activity enter/exit event, <b>data</b> is pre-populated with 
  * the active <i>rules.xml</i>.
  * 
  * @constructor
  */
function DomDataInterface() {}
 
DomDataInterface.prototype = {

	/**
	 * Get the value of a node
	 * 
	 * @param {string} xpath - expression
	 * @return {string}
	 */
	getValue : function(xpath) {
		return "string";
	}, 
	
	/**
	 * Set the value of a node
	 * 
	 * If this is a simple element path, the element will be constructed 
	 * if it doesn't already exist.
	 * 
	 * @param {string} path - XPath like expression
	 * @param {string} value - text value
	 */
	setValue : function(path, value) {
		/* no return */
	},
	
	/**
	 * Parse text to XML Document
	 * 
	 * @return {w3c-dom} parses text to XML document
	 * @throws Exception on invalid XML
	 */
	parseXML : function(text) {
		return "<w3c-dom-xml/>";
	},
	
	/**
	 * Set the source for this data object
	 * 
	 * @param {w3c-dom} xmlDoc - XML document
	 */
	setDocument : function(xmlDoc) {
		/* no return */
	},
	
	/**
	 * Get the XML contents
	 * 
	 * @return {w3c-dom} contents as an XML document
	 */
	getDocument : function() {
		return "<w3c-dom-xml/>";
	},
	
	/**
	 * Wrap an XML document as a data interface
	 * 
	 * @param {w3c-dom} xmlDoc - XML document
	 * @return {DomDataInterface} new data interface
	 */
	createData : function(xmlDoc) {
		return new DomDataInterface();
	},
	
	/**
	 * Select a list of nodes.
	 * 
	 * @param {string} xpath expression
	 * @return {Node []} node list 
	 */
	selectNodes : function(xpath) {
		return [];
	},
	
	/**
	 * Return a Base64 encoded Hash of an XML node.
	 * XML has been Canonicalized (14).
	 * 
	 * @param {string} xpath - expression
	 * @return {string} hash value
	 */
	sha1HashC14Node : function(xpath) {
		return "string";
	},
	
	/**
	 * Embed a whole document as sub element.
	 * 
	 * @param {string} path - an XPath that resolve to an element
	 * @param {w3c-dom} xmlDoc - XML document
	 */
	embedDoc : function(path, xmlDoc) {
		/* no return */
	},
	
	/**
	 * Get an absolute uri for a relative uri
	 * 
	 * @param {string} uri - in path of this data document
	 * @return {string} uri in path of this data document
	 */
	resolveUri : function(uri) {
		return "string path";
	}

};