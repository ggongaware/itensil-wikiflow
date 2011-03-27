/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * User Space Server Interface, also known as a <b>Community</b>
 * 
 */
 
 /**
  * UserSpaceInterface stub
  * 
  * Used by UserInterface and GroupInterface
  * 
  * @constructor
  */
 function UserSpaceInterface() {}
 
 UserSpaceInterface.prototype = {
 	
 	/**
 	 * System defined name. Read-only.
 	 * @type String
 	 */
 	name : "userspace name",
 	
 	/**
 	 * System base URL. Read-only.
 	 * @type String
 	 */
 	baseUrl : "http://xyz.itensil.net",
 	
 	/**
 	 * System defined brand style. Read-only.
 	 * @type String
 	 */
 	brand : "my brand",
 	
 	/**
 	 * Time the userspace was created. Read-only.
 	 * @type Date
 	 */
 	createTime : "2008-07-10T08:02:12",
 	
 	/**
 	 * Create a new group, admin role required.
 	 * 
 	 * @param {String} simpleName - display name
 	 * @param {String} fullName - must be unique to the userspace / community
 	 * @param {String} remoteKey - a user defined indexed key
 	 * @param {int} groupType - 0 = Standard, 1 = Organization
 	 * @param {GroupInterface} parent - Optional. Parent group.
 	 * @return {GroupInterface} the new group
 	 */
	createGroup : function(simpleName, fullName, remoteKey, groupType, parent) {
		return new GroupInterface();
	},
	 
	/**
 	 * Create a new user, admin role required.
 	 * 
 	 * If the user already exists, the user is added to this userspace, but the
 	 * password, locale, timezone and simpleName are ignored.
 	 * 
 	 * @param {String} userName - must be globally unique
 	 * @param {String} simpleName - display name
 	 * @param {String} password - login password
 	 * @param {String []} roles - Optional. userspace roles
 	 * 
 	 * @return {UserInterface} the new user
 	 */
	createUser : function(userName, simpleName, password, roles, locale, timezone) {
		return new UserInterface();
	},
	
	/**
	 * Find a user in a group role, also known as an org postion
	 * 
	 * @example Host.getCurrentUser().getUserSpace().findGroupRoleUsers(ctxGroup, "PARENT", "Manager");
	 * 
	 * @param {GroupInterface} contextGroup - the starting group or org
	 * @param {String} axis - search schope
	 * 		options: SELF,PARENT,ANCESTOR,CHILD,ANCESTOR_OR_SELF,CHILD_OR_SELF,SIBLING
	 * @param {String} role - role or position name
	 * @return {UserInterface []} zero or more matching users
	 */
	findGroupRoleUsers : function(contextGroup, axis, role) {
		return [new UserInterface()];
	},
	
	/**
	 * Generate a semi-friendly random password.
	 * 
	 * @return {String} password 
	 */
	genPassword : function() {
		return "password";	
	},
	
	/**
	 * Get the software features enabled for this userspace
	 * 
	 * @return {String []} list of feature names
	 */
	getFeatures : function() {
		return ["feature1", "feature2"];	
	},
	
	/**
	 * Find a group by name
	 * 
	 * @return {GroupInterface}
	 */
	getGroup : function(groupName) {
		return GroupInterface();
	},
	
	/**
	 * Find a group by groupId
	 * 
	 * @return {GroupInterface}
	 */
	getGroupById : function(groupId) {
		return GroupInterface();
	},
	
	/**
	 * Find a group by user defined remote key
	 * 
	 * @return {GroupInterface}
	 */
	getGroupByRemote : function(remoteKey) {
		return GroupInterface();
	},
	
	/**
	 * Get all the groups and orgs in the userspace / community
	 * 
	 * @return {GroupInterface []}
	 */
	getGroups : function() {
		return [GroupInterface()];
	},
	
	/**
	 * Find a user by userName / login
	 * 
	 * @return {UserInterface}
	 */
	getUser : function(userName) {
		return new UserInterface();
	},
	
	/**
	 * Find a user by userId
	 * 
	 * @return {UserInterface} 
	 */
	getUserById : function(userId) {
		return new UserInterface();
	},
	
	/**
	 * Get a list of all the users in the userspace / community
	 * 
	 * @return {UserInterface []}
	 */
	getUsers : function() {
		return [new UserInterface()];
	},
	
	/**
	 * Remove a group, admin role required
	 * 
	 * @param {GroupInterface} group
	 */
	removeGroup : function(group) {
		/* no return */
	},
	
	/**
	 * Remove a user, admin role required
	 * 
	 * @param {UserInterface} user
	 */
	removeUser : function(user) {
		/* no return */
	}
			
 };