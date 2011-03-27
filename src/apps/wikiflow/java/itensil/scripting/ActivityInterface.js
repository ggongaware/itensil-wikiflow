/**
 * (c) 2010 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Activity Server Interface
 */
 
 /**
  * ActivityInterface stub
  * 
  * Implementation included in all scopes as <b>activity</b>. 
  * 
  * @constructor (private)
  */
 function ActivityInterface() {}
 
 ActivityInterface.prototype = {
 	
 	/**
 	 * Activity ID, null if not in activity. Read-only. 
 	 * @type String
 	 */
 	id : "Activity ID",
 	
 	/**
 	 * Activity Name, null if not in activity.
 	 * @type String
 	 */
 	name : "Activity Name",
 	
 	/**
 	 * Activity file path, null if not in activity. Read-only.
 	 * @type String
 	 */
 	uri : "Activity file path",
 	
 	/**
 	 * Activity submiter's User ID, null if not in activity.
 	 * @type String
 	 */
 	submitId : "User ID",
 	
 	/**
 	 * Activity Context Group ID, null if not in activity.
 	 * @type String
 	 */
 	contextGroupId : "Group ID",
 	
 	/**
 	 * Activity Start Date, null if not in activity.
 	 * ISO8601 format date string
 	 * @type String 
 	 */
 	startDate : "2009-05-15",
 	
 	/**
 	 * Activity Due Date, null if not in activity.
 	 * ISO8601 format date string
 	 * @type String
 	 */
 	dueDate : "2010-05-15",
 	
 	/**
 	 * Activity Role assignments
 	 * A read-only array of assignment objects:
 	 * <ul>
 	 * <li>		.role {String} Role name
 	 * <li>		.assignId {String} User id
 	 * </ul>
 	 * @type Array
 	 */
 	roles : [{role: "Manager", assignId: "123"}],
 	
 	/**
	 * Activity States, the currently active steps
	 * A read-only array of state objects:
	 * <ul>
	 * <li>		.stepId {String} Active step id
	 * <li>		.assignId {String} User id assigned to step
	 * <li>		.txId {String} Transaction id of step
	 * <li>		.currentPlan {Object} the plan for this step
	 * <ul>
	 * <li>			.startDate {Date} planned start 
	 * <li>			.dueDate {Date} planned due
	 * </ul>
	 * </ul>
	 * @type Array
	 */
	states : [{stepId: "Fix it", assignId: "123", txId: "456", currentPlan : {startDate : new Date(), dueDate: new Date()} }],
 
	/**
	 * Activity Plans, for up-coming steps
	 * A read-only array of plan objects:
	 * <ul>
	 * <li>		.stepId {String} Active step id
	 * <li>		.assignId {String} User id assigned to step
	 * <li>		.startDate {Date} planned start 
	 * <li>		.dueDate {Date} planned due
	 * </ul>
	 * @type Array
	 */
	plans : [{stepId: "Fix it", assignId: "123", startDate : new Date(), dueDate: new Date()}],

 
 	/**
 	 * Is the activity the context of this script?
 	 * 
 	 * @return {Boolean} true if executing in an activity
 	 */
 	isContext : function() {
 		return false;
 	},
 	
 	/**
 	 */
 	save : function() {
 		
 	},
 	
 	/**
 	 * Launch a new activity
 	 * 
 	 * @example 
 	 * 	 activity.launch(
 	 * 		"Bug 17", 				//name
 	 * 		"Fix it", 				//description
 	 * 		"/home/process/BugFix", //flow
 	 * 		null, 					//master
 	 * 		null, 					//parentId
 	 * 		null, 					//projectId 
 	 * 		null, 					//contextGroupId  
 	 * 		"2008-07-10",			//dueDate
 	 * 								//roles 
 	 * 		{ "Fixer" : usr1.userId, 
 	 * 		  "Tester" : usr2.userId } 
 	 * 		);
 	 * 
	 * @param {String} name - User defined activity name
	 * @param {String} description - - Optional. Activity description
	 * @param {String} flowUri - Process model folder path
	 * @param {String} masterUri - Optional. Template model folder path, needed when flowUri doesn't exist yet
	 * @param {String} parentActId - Optional. id of parent activity
	 * @param {String} projectId - Optional. id of project folder node
	 * @param {String} contextGroupId - Optional. id of user group or org
	 * @param {String} dueDate - Optional. ISO8601 format date string
	 * @param {Object} roles - Optional. Role name, user id map
	 * @return {ActivityInterface} running activity
 	 * @throws Exceptions on missing or invalid process models, permissions, missing parents, projects, and groups
 	 */
 	launch : function(name, description, flowUri, masterUri, parentActId, projectId, contextGroupId, dueDate, roles) {
 		return new ActivityInterface();
 	},
 	
 	/**
 	 * Change an activities role assignment
 	 * 
 	 * @example
 	 * 
 	 * 	 activity.setRole("Manager", usr1.userId);
 	 * 
 	 * @param {String} roleName - process model define role name
 	 * @param {String} userId - system user id for role
 	 * 
 	 * @return {Boolean} true if role setting completes
 	 */
 	setRole : function (roleName, userId) {
 		return true;
 	},
 	
 	/**
 	 * Assign on step to a user. Step maybe a current state or a future plan.
 	 * 
 	 * @example
 	 * 		activity.assignStep("Fix it", "123");
 	 * 
 	 * @param {String} stepId - state transaction id
 	 * @param {String} userId - system user id for assignment
 	 * 
 	 * @return {Boolean} true if assign completes
 	 */
 	assignStep : function (stepId, userId) {
 		return true;
 	},
 	
 	/**
 	 * Submit a current step as complete
 	 * 
 	 * @example
 	 * 		activity.submitStep("Fix it", "changeCount=7");
 	 * 
 	 * @param {String} stepId - state transaction id
 	 * @param {String} ruleExpression - optional flow variable assignment, a semi-XPath expression pair:
 	 * 							<XPath 1 excluding '='> '=' <XPath 2>
 	 * 
 	 * @return {String} transaction id of resulting state
 	 */
 	submitStep : function (stepId, ruleExpression) {
 		return "456";
 	},
 	
 	/**
 	 * Roll-back a state
 	 * 
 	 * @example
 	 * 		activity.undo("456");
 	 * 
 	 * @param {String} txId - state transaction id
 	 * 
 	 * @return {Boolean} true if undo completes
 	 * @throws Exception on missing or invalid transaction id
 	 */
 	undo : function (txId) {
 		return true;
 	}
 			
 };