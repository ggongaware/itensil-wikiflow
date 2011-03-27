CREATE TABLE `iten_tcard_contract` (
  `id` bigint NOT NULL auto_increment,
  `userSpaceId` varchar(20) binary NOT NULL,
  `clientName` varchar(50),
  `name` varchar(50),
  `team` varchar(50),
  `status` varchar(25),
  `billTerms` mediumtext,
  `billOverages` tinyint NOT NULL default '0',
  `moneyBudget` float NOT NULL default '0',
  `hoursBudget` float NOT NULL default '0',
  `openDate` datetime,
  `closeDate` datetime,
  `projects` mediumtext,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB;

CREATE TABLE `iten_tcard_crole` (
  `id` bigint NOT NULL auto_increment,
  `contractId` bigint NOT NULL,
  `role` varchar(50) NOT NULL,
  `rate` float NOT NULL default '0',
  PRIMARY KEY  (`id`),
  KEY `FK61E9C4336F40FA43` (`contractId`),
  CONSTRAINT `FK61E9C4336F40FA43` FOREIGN KEY (`contractId`) REFERENCES `iten_tcard_contract` (`id`)
) ENGINE=InnoDB;

CREATE TABLE  `iten_tcard_rolelog` (
  `id` bigint NOT NULL auto_increment,
  `contractId` bigint NOT NULL,
  `billable` tinyint NOT NULL default '1',
  `project` varchar(200),
  `role` varchar(50),
  `userId` varchar(20) binary,
  PRIMARY KEY  (`id`),
  KEY `FKA2BB25686F40FA43` (`contractId`),
  CONSTRAINT `FKA2BB25686F40FA43` FOREIGN KEY (`contractId`) REFERENCES `iten_tcard_contract` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `iten_tcard_timelog` (
  `id` bigint NOT NULL auto_increment,
  `roleLogId` bigint NOT NULL,
  `logDate` date NOT NULL,
  `hours` float NOT NULL default '0',
  `activityId` varchar(20) binary,
  `appUserId` varchar(20) binary,
  `appDate` datetime,
  PRIMARY KEY  (`id`),
  KEY `FK2589FB1235E4BF9` (`roleLogId`),
  CONSTRAINT `FK2589FB1235E4BF9` FOREIGN KEY (`roleLogId`) REFERENCES `iten_tcard_rolelog` (`id`)
) ENGINE=InnoDB;


# Current timecard create version
INSERT INTO iten_config VALUES ('tcard_schema','1.1',NULL);


