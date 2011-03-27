/**
 * (c) 2009 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Permission Server Interface
 */
 
 /**
  * PermissionInterface stub
  * 
  * Implementation instaces from FilesInterface
  * 
  * @example files.createPermission(user1)
  *
  * @exmaple files.getPermissions("/home/myfolder")[0]
  * 
  * @constructor (private)
  */
 function PermissionInterface(principal) {
 	this.principal = principal;
 }
 
 PermissionInterface.prototype = {
 	
 	/* Constants */
 	NONE : 0,
 	READ : 1,
 	CREATE : 2,
 	WRITE : 3,
 	MANAGE: 4,
 	
 	AXIS_SELF : "SELF",
 	AXIS_PARENT : "PARENT",
 	AXIS_ANCESTOR : "ANCESTOR",
 	AXIS_CHILD : "CHILD",
 	AXIS_ANCESTOR_OR_SELF : "ANCESTOR_OR_SELF",
 	AXIS_CHILD_OR_SELF : "CHILD_OR_SELF",
 	AXIS_SIBLING : "SIBLING",
 	
 	GROUP_RELATIVE : "GROUP_RELATIVE",
 	GROUP_EVERYONE : "GROUP_EVERYONE",
 	
 	
 	/* Read-only properties */
 	
 	/**
 	 * Permission Test. Read-only.
 	 * @type Boolean
 	 */
 	canRead : false,
 	
 	/**
 	 * Permission Test. Read-only.
 	 * @type Boolean
 	 */
 	canCreate : false,
 	
 	/**
 	 * Permission Test. Read-only.
 	 * @type Boolean
 	 */
 	canWrite : false,
 	
 	/**
 	 * Permission Test. Read-only.
 	 * @type Boolean
 	 */
 	canManage : false,
 	
 	/**
 	 * Permission Test. Read-only.
 	 * @type Boolean
 	 */
 	isNone : true,
 	
 	/**
 	 * Owning Principal, the "who". Read-only.
 	 * @type {UserInterface} or {GroupInterface} or String
 	 */
 	 principal : "GROUP_EVERYONE",
 	
 	
 	/* Read/write properties */
 	
 	/**
 	 * Organization / Group Role
 	 * @type String
 	 */
 	 role : "",
 	 
 	/**
 	 * Organization Axis
 	 * @type String
 	 */
 	 axis : "",
 	 
 	 /**
 	 * Permission inertiable from folders to sub-folders and files
 	 * @type Boolean
 	 */
 	 inherit : true

 };