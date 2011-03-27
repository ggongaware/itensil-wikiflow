/**
 * (c) 2009 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Soap Message Server Interface
 */
 
 /**
  * SoapMessageInterface stub
  * 
  * @example ws.createSoapMessage()
  * 
  * @constructor (private)
  */
function SoapMessageInterface() {}
 
SoapMessageInterface.prototype = {
	
	/**
	 * Set the message body
	 * 
	 * @param {w3c-dom} xmlDoc - XML document
	 */
	setBody : function(xmlDoc) {
		/* no return */
	},
	
	/**
	 * Send the SOAP message
	 * 
	 * @param {string} uri - soap service endpoint
	 * @return {w3c-dom} response as XML document
	 */	deliverTo : function(uri) {
		return "<w3c-dom-xml/>";
	},
	
	/**
	 * Attach a file to the message
	 * 
	 * @param {string} cid - content id (as referenced in your message body)
	 * @param {byte-array} bytes - file contents
	 * @param {string} mimeType - file content type
	 * 
	 */
	attachFile : function(cid, bytes, mimeType) {
		/* no return */
	}
};