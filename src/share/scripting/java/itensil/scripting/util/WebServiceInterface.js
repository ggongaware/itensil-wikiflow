/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Web Service Server Interface
 */
 
 /**
  * WebServiceInterface stub
  * 
  * Implementation included in all scopes as <b>ws</b>. 
  * 
  * @constructor
  */
 function WebServiceInterface() {}
 
 WebServiceInterface.prototype = {

	 /**
	  * Simple soap call
	  * 
	  * @example ws.soapCall('http://www.webservicex.net/CurrencyConvertor.asmx',
	  * 	'http://www.webserviceX.NET/', 'ConversionRate', 
	  * 	{ FromCurrency :'USD', ToCurrency :  'USD'} );
	  * 
	  * 
	  * @param {string} serviceUrl - soap service endpoint
	  * @param {string} namespace - service xml namespace
	  * @param {string} operation - method name
	  * @param {object} parameters - named arguments for the method
	  * @return {string} soap response as a string
	  */
	soapCall : function(serviceUrl, namespace, operation, parameters) {
		return "string";
	},
	
	/**
	 * Soap message creater
	 * 
	 * @return {SoapMessageInterface} create a specific soap message
	 */
	createSoapMessage : function() {
		return new SoapMessageInterface();
	},
	
	/**
	 * Download a url
	 * 
	 * @param {byte-array} bytes - byte array object
	 * @return {string} base
	 */
	sha1HashBytes : function(bytes) {
		return "string";
	},
	
	/**
	 * Download a url as a string
	 * 
	 * @example ws.requestGetString("http://user:pass@www.test.com/myfile.txt")
	 * 
	 * @param {string} url - http address
	 * @return {string} response body
	 * 
	 */
    requestGetString : function(url) {
    	return "string";
	},

	/**
	 * Download a url
	 * 
	 * @param {string} url - http address
	 * @return {byte-array} response body
	 */
	requestGetBytes : function(url) {
		return "{byte-array}";
	},
	
	/**
	 * Get partial implementation of W3 XMLHttpRequest Object
	 * 
	 * http://www.w3.org/TR/XMLHttpRequest
	 * 
	 * @example 
	 *  var xhttp = ws.getXMLHttpRequest();
	 *  xhttp.open('POST','http://www.w3.org');
	 *  xhttp.send('my own encoded stuff or xml doc');
	 *  xhttp.responseText
	 * 
	 */
	getXMLHttpRequest : function() {
		return "{XMLHttpRequest}";
	}
 };