/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * User Group / Organization Server Interface
 */
 
 /**
  * GroupInterface stub
  * 
  * Used by UserInterface and UserSpaceInterface
  * 
  * @constructor
  */
 function GroupInterface() {}
 
 GroupInterface.prototype = {

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
 	 * Full display name of a group.
 	 */
	groupName : "full name",
	
	/**
 	 * Admin defined indexed key
 	 * @type String
 	 */
	remoteKey : "remote key",
	
	/**
 	 * Short group name.
 	 * @type String
 	 */
	simpleName : "name",
	
	/**
 	 * Group type numer: 0 = Standard, 1 = Organization. Read-only.
 	 * @type int
 	 */           
	groupType : 0,
	
	/**
 	 * Unique id of group. Read-only.
 	 * @type String
 	 */
	groupId : "GroupID",
	
	/**
 	 * Time group was created. Read-only.
 	 * @type Date
 	 */
	createTime : "2008-07-10T08:02:12",
	                           
	/**
	 * Get group's member users
	 * 
	 * @return {UserInterface []} list of members
	 */
	getMembers : function() {
 		return [new UserInterface()];
 	},
 	
 	/**
 	 * Get the parent group
 	 * 
 	 * @return {GroupInterface} null if root group
 	 */
	getParent : function() {
		return new GroupInterface();
	},
	
	/**
	 * Get the owning userspace / community
	 * 
	 * @return {UserSpaceInterface}
	 */
	getUserSpace : function() {
		return [new UserSpaceInterface()];
	},
	
	/**
	 * Add a group member, userspace admin role required
	 * 
	 * @param {UserInterface} user - member user
	 * @return {MemberInterface} the new member
	 */
	memberJoin : function(user) {
		return new MemberInterface();
	},
	
	/**
	 * Remove a group member, userspace admin role required
	 * 
	 * @param {UserInterface} user - member user
	 */
	memberLeave : function(user) {
		/* no return */
	},
	
	/**
	 * Save property changes to the group, userspace admin role required
	 */
	save : function() {
		/* no return */
	}
	
 };