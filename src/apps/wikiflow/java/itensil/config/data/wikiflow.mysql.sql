# Database schema for workflow


# Table: 'iten_config'
#
CREATE TABLE `iten_config` (
	`component` varchar(64) NOT NULL default '',
	`vNumber` varchar(64) NOT NULL default '',
	`properties` text default NULL,
	PRIMARY KEY  (`component`)
) ENGINE=InnoDB;

# Current complete create version
INSERT INTO iten_config VALUES ('schema','1.16',NULL);
INSERT INTO iten_config VALUES ('repository','1.3',NULL);

# Table: 'iten_group'
#
CREATE TABLE `iten_group` (
  `id` varchar(20) binary NOT NULL default '',
  `groupName` varchar(128) default NULL,
  `userSpaceId` varchar(20) binary default NULL,
  `simpleName` varchar(64) default NULL,
  `groupType` int(4) NOT NULL default 0,
  `parentId` varchar(20) binary default NULL,
  `remoteKey` varchar(128) binary default NULL,
  `createTime` datetime NOT NULL default '2008-03-19 10:00:00',
  `custom1` varchar(128) binary default NULL,
  `custom2` varchar(128) binary default NULL,
  `custom3` varchar(128) binary default NULL,
  `custom4` varchar(128) binary default NULL,
  PRIMARY KEY  (`id`),
  KEY `FKBFE33594B3E08776` (`userSpaceId`),
  INDEX `RKGROUP` (`remoteKey`)
) ENGINE=InnoDB;


# Table: 'iten_group_user'
#
CREATE TABLE `iten_group_user` (
  `userId` varchar(20) binary NOT NULL default '',
  `groupId` varchar(20) binary NOT NULL default '',
  `roleStr` varchar(128) default NULL,
  `joinTime` datetime NOT NULL default '2008-03-19 10:00:00',
  PRIMARY KEY  (`userId`,`groupId`),
  KEY `FK28D4CB96F72CD3AA` (`userId`),
  KEY `FK28D4CB96C2E9E332` (`userId`),
  KEY `FK28D4CB963A7F015E` (`groupId`)
) ENGINE=InnoDB;


# Table: 'iten_repo'
#
CREATE TABLE `iten_repo` (
  `id` int(11) NOT NULL auto_increment,
  `mount` varchar(32) binary default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `mount` (`mount`)
) ENGINE=InnoDB;


# Table: 'iten_repo_content'
#
CREATE TABLE `iten_repo_content` (
  `versionId` bigint(20) NOT NULL default '0',
  `cLength` int(11) default NULL,
  `content` longblob,
  PRIMARY KEY  (`versionId`),
  KEY `FK69AC33F772D0F79A` (`versionId`)
) ENGINE=InnoDB;


# Table: 'iten_repo_lock'
#
CREATE TABLE `iten_repo_lock` (
  `id` varchar(20) NOT NULL default '',
  `nodeId` varchar(20) binary default NULL,
  `ownerId` varchar(20) binary default NULL,
  `expireTime` datetime default NULL,
  `ownerInfo` varchar(255) default NULL,
  `inheritable` tinyint(1) default NULL,
  `exclusive` tinyint(1) default NULL,
  PRIMARY KEY  (`id`),
  KEY `FK4BD4266D26B991D6` (`nodeId`)
) ENGINE=InnoDB;


# Table: 'iten_repo_mount'
#
CREATE TABLE `iten_repo_mount` (
  `userSpaceId` varchar(20) binary NOT NULL default '',
  `repoId` int(11) NOT NULL default '0',
  `isPrimary` tinyint(1) default NULL,
  PRIMARY KEY  (`userSpaceId`,`repoId`),
  KEY `FK2EBF03175C59E38E` (`repoId`)
) ENGINE=InnoDB;


# Table: 'iten_repo_node'
#
CREATE TABLE `iten_repo_node` (
  `id` varchar(20) binary NOT NULL default '',
  `parentId` varchar(20) binary default NULL,
  `repoId` int(11) default NULL,
  `uri` varchar(255) binary default NULL,
  `ownerId` varchar(20) binary default NULL,
  `collection` tinyint(1) default NULL,
  `createTime` datetime NOT NULL default '2008-03-19 10:00:00',
  `defVersionId` bigint(20) default NULL,
  `deleted` bigint(20) default '0',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `repoId` (`repoId`,`uri`),
  KEY `FK4BD50F445C59E38E` (`repoId`),
  KEY `FK4BD50F44AAC4E27E` (`parentId`)
) ENGINE=InnoDB;


# Table: 'iten_repo_permission'
#
CREATE TABLE `iten_repo_permission` (
  `nodeId` varchar(20) binary NOT NULL default '',
  `principalId` varchar(20) binary NOT NULL default '',
  `is_group` tinyint(1) default NULL,
  `permission` int(11) default NULL,
  `inherit` tinyint(1) default NULL,
  PRIMARY KEY  (`nodeId`,`principalId`),
  KEY `FKFD5AD9126B991D6` (`nodeId`)
) ENGINE=InnoDB;


# Table: 'iten_repo_version'
#
CREATE TABLE `iten_repo_version` (
  `id` bigint(20) NOT NULL auto_increment,
  `nodeId` varchar(20) binary default NULL,
  `vNumber` varchar(64) default NULL,
  `vLabel` varchar(64) default NULL,
  `davContentType` VARCHAR(128) DEFAULT NULL,
  `davContentLang` VARCHAR(32) DEFAULT NULL,
  `davContentLen` INT DEFAULT NULL,
  `davEtag` VARCHAR(64) DEFAULT NULL,
  `davLastMod` datetime DEFAULT NULL,
  `irModifier` VARCHAR(20) binary default NULL,
  `irDescription` VARCHAR(255) default NULL,
  `irKeywords` VARCHAR(255) default NULL,
  `irTags` VARCHAR(255) default NULL,
  `irStyle` VARCHAR(255) default NULL,
  `cust1Ns` VARCHAR(255) default NULL,
  `cust1Val` VARCHAR(255) default NULL,
  `cust2Ns` VARCHAR(255) default NULL,
  `cust2Val` VARCHAR(255) default NULL,
  `cust3Ns` VARCHAR(255) default NULL,
  `cust3Val` VARCHAR(255) default NULL,
  `cust4Ns` VARCHAR(255) default NULL,
  `cust4Val` VARCHAR(255) default NULL,
  `ieRecordId` bigint(20) NOT NULL default 0,
  `ieBrowse0` VARCHAR(255) default NULL,
  `ieBrowse1` VARCHAR(255) default NULL,
  `ieBrowse2` VARCHAR(255) default NULL,
  `ieBrowse3` VARCHAR(255) default NULL,
  `ieBrowse4` VARCHAR(255) default NULL,
  `ieBrowse5` VARCHAR(255) default NULL,
  `ieBrowse6` VARCHAR(255) default NULL,
  `ieBrowse7` VARCHAR(255) default NULL,
  `ieBrowse8` VARCHAR(255) default NULL,
  `ieBrowse9` VARCHAR(255) default NULL,
  `ieBrowseA` VARCHAR(255) default NULL,
  `ieBrowseB` VARCHAR(255) default NULL,
  PRIMARY KEY  (`id`),
  KEY `FK45EA1C5626B991D6` (`nodeId`),
  INDEX `IDX_IE_RECORD` (`ieRecordId`)
) ENGINE=InnoDB;


# Table: 'iten_user'
#
CREATE TABLE `iten_user` (
  `id` varchar(20) binary NOT NULL default '',
  `userName` varchar(128) default NULL,
  `simpleName` varchar(64) default NULL,
  `email` varchar(128) default NULL,
  `flagStr` varchar(255) default NULL,
  `remoteKey` varchar(128) binary default NULL,
  `passwordHash` tinyblob,
  `locale` varchar(128) default NULL,
  `timezone` varchar(128) default NULL,
  `createTime` datetime NOT NULL default '2008-03-19 10:00:00',
  `loginCount` int(11) NOT NULL default 0,
  `lastLogin` datetime default NULL,
  `token` varchar(40)  default NULL,
  `deleted` tinyint(1) default 0,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `userName` (`userName`),
  UNIQUE KEY `token` (`token`),
  INDEX `RKUSER` (`remoteKey`)
) ENGINE=InnoDB;


# Table: 'iten_userspace'
#
CREATE TABLE `iten_userspace` (
  `id` varchar(20) binary NOT NULL default '',
  `name` varchar(64) default NULL,
  `baseUrl` varchar(64) default NULL,
  `brand` varchar(16) default NULL,
  `featuresStr` varchar(255) default NULL,
  `createTime` datetime NOT NULL default '2008-03-19 10:00:00',
  `disabled` tinyint(1) default 0,
  `alertEmailer` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

# Table: 'iten_uspace_user'
#
CREATE TABLE `iten_uspace_user` (
	`userId` varchar(20) binary NOT NULL,
	`userSpaceId` varchar(20) binary NOT NULL,
	`roleStr` varchar(255) default NULL,
	`createTime` datetime NOT NULL default '2008-03-19 10:00:00',
	`custom1` varchar(128) binary default NULL,
  	`custom2` varchar(128) binary default NULL,
  	`custom3` varchar(128) binary default NULL,
  	`custom4` varchar(128) binary default NULL,
	PRIMARY KEY  (`userId`, `userSpaceId`)
) ENGINE=InnoDB;
 	
# Table: 'iten_wf_flow_log'
#
CREATE TABLE `iten_wf_flow_log` (
  `id` bigint(20) NOT NULL auto_increment,
  `flowId` varchar(20) binary default NULL,
  `tokenId` varchar(20) binary default NULL,
  `txId` varchar(20) binary default NULL,
  `stepId` varchar(255) default NULL,
  `subState` int(11) default NULL,
  `timeStamp` datetime default NULL,
  `expireTime` datetime default NULL,
  `userId` VARCHAR(20) BINARY DEFAULT NULL,
  PRIMARY KEY  (`id`),
  KEY `FK9F3E8237E735858` (`tokenId`),
  KEY `FK9F3E8238C13C8FB` (`flowId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_flow_role'
#
CREATE TABLE `iten_wf_flow_role` (
  `id` varchar(20) binary NOT NULL default '',
  `flowId` varchar(20) binary default NULL,
  `role` varchar(255) binary default NULL,
  `assignId` varchar(20) binary default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `flowId` (`flowId`,`role`),
  KEY `FK348BD7778C13C8FB` (`flowId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_flow_state'
#
CREATE TABLE `iten_wf_flow_state` (
  `id` varchar(20) binary NOT NULL default '',
  `active` tinyint(1) default NULL,
  `cust0Name` varchar(64),
  `cust0Type` varchar(64),
  `cust1Name` varchar(64),
  `cust1Type` varchar(64),
  `cust2Name` varchar(64),
  `cust2Type` varchar(64),
  `cust3Name` varchar(64),
  `cust3Type` varchar(64),
  `cust4Name` varchar(64),
  `cust4Type` varchar(64),
  `cust5Name` varchar(64),
  `cust5Type` varchar(64),
  `cust6Name` varchar(64),
  `cust6Type` varchar(64),
  `cust7Name` varchar(64),
  `cust7Type` varchar(64),
  `cust8Name` varchar(64),
  `cust8Type` varchar(64),
  `cust9Name` varchar(64),
  `cust9Type` varchar(64),
  `custAName` varchar(64),
  `custAType` varchar(64),
  `custBName` varchar(64),
  `custBType` varchar(64),
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;


# Table: 'iten_wf_activity'
#
CREATE TABLE `iten_wf_activity` (
  `id` varchar(20) binary NOT NULL default '',
  `parentId` varchar(20) binary default NULL,
  `flowId` varchar(20) binary default NULL,
  `userSpaceId` varchar(20) binary default NULL,
  `variationId` varchar(20) binary default NULL,
  `name` varchar(255) default NULL,
  `description` varchar(255) default NULL,
  `submitId` varchar(20) binary default NULL,
  `contextGroupId` varchar(20) binary default NULL,
  `timeStamp` datetime default NULL,
  `priority` int(11) default NULL,
  `startDate` datetime default NULL,
  `dueDate` datetime default NULL,
  `duration` int(11) default NULL,
  `cust0Val` varchar(255) default NULL,
  `cust1Val` varchar(255) default NULL,
  `cust2Val` varchar(255) default NULL,
  `cust3Val` varchar(255) default NULL,
  `cust4Val` varchar(255) default NULL,
  `cust5Val` varchar(255) default NULL,
  `cust6Val` varchar(255) default NULL,
  `cust7Val` varchar(255) default NULL,
  `cust8Val` varchar(255) default NULL,
  `cust9Val` varchar(255) default NULL,
  `custAVal` varchar(255) default NULL,
  `custBVal` varchar(255) default NULL,
  PRIMARY KEY  (`id`),
  KEY `FK4A800E838AEBEC9` (`parentId`),
  KEY `FK4A800E838C13C8FB` (`flowId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_activity_state'
#
CREATE TABLE `iten_wf_activity_state` (
  `id` varchar(20) binary NOT NULL default '',
  `activityId` varchar(20) binary NOT NULL,
  `stTxId` varchar(20) binary default NULL,
  `stStepId` varchar(255) BINARY default NULL,
  `stSubState` int(11) default NULL,
  `stTimeStamp` datetime default NULL,
  `stExpireTime` datetime default NULL,
  `stAssignId` varchar(20) binary default NULL,
  `stUserStatus` int(11) default 0,
  `subActivityId` varchar(20) binary default NULL,
  `progress` int(11) NOT NULL default 0,
  `cpPriority` int(11) default NULL,
  `cpStartDate` datetime default NULL,
  `cpDueDate` datetime default NULL,
  `cpDuration` int(11) default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `stActivityId` (`activityId`,`stStepId`),
  KEY `FKstActivityId` (`activityId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_activity_plan'
#
CREATE TABLE `iten_wf_activity_plan` (
  `id` varchar(20) binary NOT NULL default '',
  `activityId` varchar(20) binary default NULL,
  `stepId` varchar(255) BINARY default NULL,
  `assignId` varchar(20) binary default NULL,
  `priority` int(11) default NULL,
  `startDate` datetime default NULL,
  `skip` tinyint(1) default '0',
  `dueDate` datetime default NULL,
  `duration` int(11) default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `activityId` (`activityId`,`stepId`),
  KEY `FKE52CF657C623AF2` (`activityId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_activity_role'
#
CREATE TABLE `iten_wf_activity_role` (
  `id` varchar(20) binary NOT NULL default '',
  `activityId` varchar(20) binary default NULL,
  `role` varchar(255) binary default NULL,
  `assignId` varchar(20) binary default NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `activityId` (`activityId`,`role`),
  KEY `FKE53C4B27C623AF2` (`activityId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_activity_alert'
#
CREATE TABLE `iten_wf_activity_alert` (
    `id` varchar(20) binary NOT NULL,
    `activityId` varchar(20) binary,
    `assignId` varchar(20) binary,
    `stepId` varchar(255),
    `role` varchar(255),
    `note` varchar(255),
    `timeStamp` datetime,
    `mailed` tinyint(1),
    `read` tinyint(1),
    PRIMARY KEY (`id`),
    KEY `FKD2496731D99B1CF7` (`activityId`)
) ENGINE=InnoDB;

# Table: 'iten_wf_activity_alert'
#
CREATE TABLE `iten_wf_activity_timer` (
    `id` varchar(20) binary NOT NULL,
    `activityId` varchar(20) binary,
    `timerId` varchar(255) binary,
    `atTime` datetime,
    `conditional` tinyint(1),
    `checkTime` datetime,
	PRIMARY KEY (`id`),
	KEY `FKD2496731D99B1C6` (`activityId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_project_activities'
#
CREATE TABLE `iten_wf_project_activities` (
	`id` varchar(20) binary NOT NULL,
    `activityId` varchar(20) binary NOT NULL,
    PRIMARY KEY (`id`, `activityId`)
) ENGINE=InnoDB;


# Table: 'iten_wf_entity_activity'
#
CREATE TABLE `iten_wf_entity_activity` (
    `activityId` varchar(20) binary NOT NULL,
    `entityId` varchar(20) binary NOT NULL,
    `name` varchar(64) binary NOT NULL,
    `recordId` bigint(20) NOT NULL,
    `createTime` datetime NOT NULL default '2008-03-22 10:00:00',
    
    PRIMARY KEY (`activityId`, `entityId`, `recordId`, `name`)
) ENGINE=InnoDB;


# Create system userspace
INSERT INTO iten_userspace VALUES ('ZCs7FA4BAFOrChMBS_e$', 'system', 'http://localhost', NULL, NULL, NOW(), 0, NULL);

# Create sysadmin user
INSERT INTO iten_user VALUES 
	('fycRVw0BAF3mChMBS$fk','sysadmin@itensil.net','Sys Admin','sysadmin@itensil.net',
	'', NULL, 0x1A1DC91C907325C69271DDF0C944BC72,'en_US',
	'America/Los_Angeles', NOW(), 0, NULL, NULL, 0);

# put sysadmin in system userspace
INSERT INTO iten_uspace_user VALUES ('fycRVw0BAF3mChMBS$fk', 'ZCs7FA4BAFOrChMBS_e$', 'admin', NOW(), NULL, NULL, NULL, NULL);


