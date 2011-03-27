/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Host Server Interface
 */
 
 /**
  * HostInterface stub
  * 
  * Implementation included in all scopes as <b>Host</b>. 
  * 
  * @constructor
  */
 function HostInterface() {}
 
 HostInterface.prototype = {
 	
 	/**
 	 * Current version of the script host. Read-only.
 	 * @type String
 	 */
 	version : "1.6",
 	
 	/**
 	 * Get the user.
 	 * 
 	 * @example Host.getCurrentUser().userName;
 	 * 
 	 * @return {UserInterface} the current execution user
 	 */
 	getCurrentUser : function() {
 		return new UserInterface();
 	}
			
 };