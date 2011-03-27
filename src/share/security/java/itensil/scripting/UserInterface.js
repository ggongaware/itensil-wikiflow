/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * User Server Interface
 */
 
 /**
  * UserInterface stub
  * 
  * Used by HostInterface
  * 
  * @constructor
  */
 function UserInterface() {}
 
 UserInterface.prototype = {
 	
	
 	/**
 	 * Storage for user defined values.
 	 * @type String
 	 */
 	custom1 : "cust 1",
 	
 	/**
 	 * Storage for user defined values.
 	 * @type String
 	 */
	custom2 : "cust 2",
	
	/**
 	 * Storage for user defined values.
 	 * @type String
 	 */
	custom3 : "cust 3",
	
	/**
 	 * Storage for user defined values.
 	 * @type String
 	 */
	custom4 : "cust 4",
	
	/**
 	 * User login name, globally unique.
 	 * @type String
 	 */
	userName : "user@login",
	
	/**
 	 * Admin defined indexed key
 	 * @type String
 	 */
	remoteKey : "remote key",
	
	/**
 	 * User's display name.
 	 * @type String
 	 */
	simpleName : "name",
	
	/**
 	 * Time-zone id
 	 * @type String
 	 */
	timezone : "America/Los_Angeles",
	
	/**
 	 * Language and variation
 	 * @type String
 	 */
	locale : "en_US",
	
	/**
 	 * Unique id of user. Read-only.
 	 */
	userId : "UserID",
	
	/**
 	 * Time user was created. Read-only.
 	 * @type Date
 	 */
	createTime : "2008-07-10T08:02:12",
	
	/**
 	 * Time user last logged in. Read-only.
 	 * @type Date
 	 */
	lastLogin : "2008-07-10T08:02:12",
	
	/**
 	 * Number of logins. Read-only.
 	 * @type int
 	 */
	loginCount : 5,
	
	/**
	 * Get the owning userspace / community
	 * 
	 * @return {UserSpaceInterface}
	 */
	getUserSpace : function() {
		return [new UserSpaceInterface()];
	},
	
	/**
	 * Get the roles user in the userspace / community,  userspace admin role required
	 * 
	 * @return {String []} role list
	 */
	getRoles : function() {
	 	return ["role1", "role2"];
	},
	 	
 	/**
 	 * Set the roles user in the userspace / community,  userspace admin role required
 	 * 
 	 * @example Built-in roles:
 	 * 
 	 * admin - create/edit users
 	 * invite - add users
 	 * guest - limited guest access
 	 * actlog - activity logs for guests
 	 * noproj - hide projects menu
 	 * noproc - hide process menu
 	 * nomeet - hide meeting menu
 	 * nocourse - hide course menu
 	 * noentity - hide entity/data menu
 	 * 
 	 * @param {String []} roles - list of roles
 	 */
 	setRoles : function(roles) {
 		/* no return */
 	},
 		
	/**
	 * Get the roles user in the userspace / community, self or userspace admin role required
	 * 
	 * @return {String []} flag list
	 */
	getFlags : function() {
	 	return ["flag1", "flag1"];
	},
	 	
 	/**
 	 * Set the roles user in the userspace / community, self or userspace admin role required
 	 * 
 	 * @param {String []} flags - list of flags
 	 */
 	setFlags : function(flags) {
 		/* no return */
 	},
 	
 	/**
 	 * Get the user's groups / organizations
 	 * 
 	 * @return {GroupInterface} list of groups
 	 */
 	getGroups : function() {
 		return [new GroupInterface()];
 	},
 	
 	/**
 	 * Get member info for a group
 	 * 
 	 * @param {GroupInterface} group - group object
 	 * @return {MemberInterface} - null if not a member
 	 */
 	getMemberInfo : function(group) {
 		return new MemberInterface();
 	},
 	
 	/**
 	 * Test if this user is in a group
 	 * 
 	 * @param {GroupInterface} group - group object
 	 * @return {Boolean} true if user in this group
 	 */
 	isUserInGroup : function(group) {
 		return false;
 	},
 	
 	/**
 	 * Set a password
 	 * Length between 6 and 32
 	 * 
 	 * @param {String} pass - plain text password, length between 6 and 32
 	 */
 	 resetPassword : function(pass) {
 	 	/* no return */
 	 },
 	
	/**
	 * Save property changes to the user, self or userspace admin role required
	 */
	save : function() {
		/* no return */
	}
	
 };