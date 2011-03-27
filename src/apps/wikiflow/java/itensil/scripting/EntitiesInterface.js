/**
 * (c) 2009 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Entities Server Interface
 */
 
  /**
  * EntitiesInterface stub
  * 
  * Implementation included in all scopes as <b>entities</b>. 
  * 
  * @constructor (private)
  */
 function EntitiesInterface() {} 
  
 EntitiesInterface.prototype = {
 	
 	/**
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @return {String} file path uri of new record
 	 */
 	createRecord : function(entity) {
 		return "file path";
 	},
 	 
 	/**
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @param {String} activityId - activity id string
 	 * @param {String} relName - name of relationshop from process model
 	 * @return {String} file path uri of new record
 	 */
 	createRecordForActivity : function(entity, activityId, relName) {
 		return "file path";
 	},
 	
 	/**
 	 * List records
 	 * 
 	 * @example entities.recordList("xyz")
 	 * 
 	 * Sample output:
 	 * 
 	 * 	&lt;records entityUri="/home/entity/xyx" entityId="2937$8swerhjh">
 	 * 		&lt;record id="1" uri="000001">
 	 * 			&lt;displayColumn1>1&lt;/displayColumn1>
 	 * 			&lt;displayColumn2>2&lt;/displayColumn2>
 	 * 		&lt;/record>
 	 * 	&lt;/records>
 	 * 
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @return {w3c-dom} xmlDoc - XML document of record list
 	 */
	recordList : function(entity) {
		return "<w3c-dom-xml/>";
 	},
 	
 	 	
 	/**
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @param {String} activityId - activity id string
 	 * @param {String} relName - name of relationshop from process model
 	 * @return {w3c-dom} xmlDoc - XML document in the recordList format
 	 */
	recordsFindByActivity : function(entity, activityId, relName) {
		return "<w3c-dom-xml/>";
 	},
 	 
 	/**
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @param {String} id - record id number
 	 * @return {w3c-dom} xmlDoc - XML document in the recordList format
 	 */
	recordFind : function(entity, id) {
		return "<w3c-dom-xml/>";
 	},
 	
 	/**
 	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
 	 * @param {String} id - record id number
 	 * @return {DomDataInterface} - full record data
 	 */
	recordData : function(entity, id) {
		return new DomDataInterface();
 	},
 	
	/**
	 * Fire a user event
	 * 
	 * @example entities.userEvent("customer", 77, "upsell")
	 * 
	 * Sample output:
	 * 
	 *   &lt;event-results>
	 *     &lt;activity id="123" flow="123" flowName="Customer Upsell"
	 *           icon="star1" name="Customer 77 upsell" description=""
	 *           activeKids="0" submitId="123"
	 *           priority="0" startDate="" dueDate="" duration="0">
	 *        &lt;state txId="123" stepId="Follow-Up Call" assignId="123" 
	 *             subState="ENTER_STEP" progress="0" 
	 *             timeStamp="2008-05-22T19:29:37Z" userStatus="0"/>
	 *     &lt;/activity>
	 *   &lt;/event-results>
	 * 
	 * @param {String} entity - enity name (xyz portion of /home/entity/xyz)
	 * @param {String} id - record id number
	 * @param {String} event - event id/name
	 * @return {w3c-dom} xmlDoc - XML document with event results
	 */
	userEvent : function(entity, id, event) {
		return "<w3c-dom-xml/>";
 	}
 	
 };