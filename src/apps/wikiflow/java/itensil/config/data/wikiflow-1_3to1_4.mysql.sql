ALTER TABLE `iten_wf_flow_state`
	ADD COLUMN `cust0Name` varchar(64) default NULL,
	ADD COLUMN `cust0Type` varchar(64) default NULL,
	ADD COLUMN `cust1Name` varchar(64) default NULL,
	ADD COLUMN `cust1Type` varchar(64) default NULL,
	ADD COLUMN `cust2Name` varchar(64) default NULL,
	ADD COLUMN `cust2Type` varchar(64) default NULL,
	ADD COLUMN `cust3Name` varchar(64) default NULL,
	ADD COLUMN `cust3Type` varchar(64) default NULL,
	ADD COLUMN `cust4Name` varchar(64) default NULL,
	ADD COLUMN `cust4Type` varchar(64) default NULL,
	ADD COLUMN `cust5Name` varchar(64) default NULL,
	ADD COLUMN `cust5Type` varchar(64) default NULL,
	ADD COLUMN `cust6Name` varchar(64) default NULL,
	ADD COLUMN `cust6Type` varchar(64) default NULL,
	ADD COLUMN `cust7Name` varchar(64) default NULL,
	ADD COLUMN `cust7Type` varchar(64) default NULL,
	ADD COLUMN `cust8Name` varchar(64) default NULL,
	ADD COLUMN `cust8Type` varchar(64) default NULL,
	ADD COLUMN `cust9Name` varchar(64) default NULL,
	ADD COLUMN `cust9Type` varchar(64) default NULL,
	ADD COLUMN `custAName` varchar(64) default NULL,
	ADD COLUMN `custAType` varchar(64) default NULL,
	ADD COLUMN `custBName` varchar(64) default NULL,
	ADD COLUMN `custBType` varchar(64) default NULL;

ALTER TABLE `iten_wf_activity`
	ADD COLUMN `cust0Val` varchar(255) default NULL,
	ADD COLUMN `cust1Val` varchar(255) default NULL,
	ADD COLUMN `cust2Val` varchar(255) default NULL,
	ADD COLUMN `cust3Val` varchar(255) default NULL,
	ADD COLUMN `cust4Val` varchar(255) default NULL,
	ADD COLUMN `cust5Val` varchar(255) default NULL,
	ADD COLUMN `cust6Val` varchar(255) default NULL,
	ADD COLUMN `cust7Val` varchar(255) default NULL,
	ADD COLUMN `cust8Val` varchar(255) default NULL,
	ADD COLUMN `cust9Val` varchar(255) default NULL,
	ADD COLUMN `custAVal` varchar(255) default NULL,
	ADD COLUMN `custBVal` varchar(255) default NULL;

ALTER TABLE `iten_wf_activity_state`
	ADD COLUMN `stUserStatus` int(11) default 0;

# Table: 'iten_wf_project_activities'
#
CREATE TABLE `iten_wf_project_activities` (
	`id` varchar(20) binary NOT NULL,
    `activityId` varchar(20) binary NOT NULL,
    PRIMARY KEY (`id`, `activityId`)
) ENGINE=InnoDB;

# set the new version
UPDATE iten_config SET vNumber = '1.4' WHERE component = 'schema';