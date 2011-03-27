# Table: 'iten_wf_activity_state'
#
CREATE TABLE `iten_wf_activity_state` (
  `id` varchar(20) binary NOT NULL default '',
  `activityId` varchar(20) binary NOT NULL,
  `stTxId` varchar(20) binary default NULL,
  `stStepId` varchar(255) default NULL,
  `stSubState` int(11) default NULL,
  `stTimeStamp` datetime default NULL,
  `stExpireTime` datetime default NULL,
  `stAssignId` varchar(20) binary default NULL,
  `cpPriority` int(11) default NULL,
  `cpStartDate` datetime default NULL,
  `cpDueDate` datetime default NULL,
  `cpDuration` int(11) default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `stActivityId` (`activityId`,`stStepId`),
  KEY `FKstActivityId` (`activityId`)
) ENGINE=InnoDB;


INSERT INTO iten_wf_activity_state 
 (
	`id`,
	`activityId`,
	`stTxId`,
	`stStepId`,
	`stSubState`,
	`stTimeStamp`,
	`stExpireTime`,
	`stAssignId`,
	`cpPriority`,
	`cpStartDate`,
	`cpDueDate`,
	`cpDuration`)
 SELECT 
	`id`, 
	`id`,
	`stTxId`,
	`stStepId`,
	`stSubState`,
	`stTimeStamp`,
	`stExpireTime`,
	`assignId`,
	`cpPriority`,
	`cpStartDate`,
	`cpDueDate`,
	`cpDuration` FROM iten_wf_activity;

# Add userId to flow log
#
ALTER TABLE `iten_wf_flow_log` ADD COLUMN `userId` VARCHAR(20) BINARY DEFAULT NULL AFTER `expireTime`;

# Drop the state and current plan columns
#
ALTER TABLE `iten_wf_activity` DROP COLUMN `uri`,
 DROP COLUMN `stTxId`,
 DROP COLUMN `stStepId`,
 DROP COLUMN `stSubState`,
 DROP COLUMN `stTimeStamp`,
 DROP COLUMN `stExpireTime`,
 DROP COLUMN `assignId`,
 DROP COLUMN `cpPriority`,
 DROP COLUMN `cpStartDate`,
 DROP COLUMN `cpDueDate`,
 DROP COLUMN `cpDuration`;
