/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Member Server Interface
 */
 
 /**
  * MemberInterface stub
  * 
  * Used by UserInterface and GroupInterface
  * 
  * @constructor
  */
 function MemberInterface() {}
 
 MemberInterface.prototype = {
 	
 	/**
 	 * The time the member joined the group. Read-only.
 	 * @type Date
 	 */
 	joinTime : "2008-07-10T08:02:12",
 	
 	/**
	 * @return {UserInterface} - owning user
	 */
	getUser : function() {
	 	return new UserInterface();
	},
	
	/**
	 * Get the roles / positions of the member in the group
	 * 
	 * @return {String []} role list
	 */
	getRoles : function() {
	 	return ["role1", "role2"];
	},
	 	
 	/**
 	 * set the roles / positions of the member in the group
 	 * 
 	 * @param {String []} roles - list of roles
 	 */
 	setRoles : function(roles) {
 		/* no return */
 	},
 	
	/**
	 * @return {GroupInterface} - owning group
	 */
	getGroup : function() {
	 	return new GroupInterface();
	},
	 	
 	/**
	 * Save property changes to the member, userspace admin role required
	 */
	save : function() {
		/* no return */
	}
			
 };