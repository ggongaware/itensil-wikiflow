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


# add entity columns to version
ALTER TABLE iten_repo_version
	ADD COLUMN `ieRecordId` bigint(20) NOT NULL default 0,
	ADD COLUMN `ieBrowse0` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse1` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse2` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse3` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse4` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse5` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse6` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse7` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse8` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowse9` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowseA` VARCHAR(255) default NULL,
	ADD COLUMN `ieBrowseB` VARCHAR(255) default NULL,
	ADD INDEX `IDX_IE_RECORD` (`ieRecordId`);
	
# set the new version  	
UPDATE iten_config SET vNumber = '1.14' WHERE component = 'schema';