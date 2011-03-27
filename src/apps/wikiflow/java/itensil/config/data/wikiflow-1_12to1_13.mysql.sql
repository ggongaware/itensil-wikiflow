# * Community - database structure added for org-charts
# * Users - additional state data (login times and counts)
#

ALTER TABLE `iten_group`
	ADD COLUMN `groupType` int(4) NOT NULL default 0,
	ADD COLUMN `parentId` varchar(20) binary default NULL,
	ADD COLUMN `remoteKey` varchar(128) binary default NULL,
	ADD COLUMN `createTime` datetime NOT NULL default '2008-03-19 10:00:00',
	ADD INDEX `RKGROUP` (`remoteKey`);

ALTER TABLE `iten_user`
	ADD COLUMN `email` varchar(128) default NULL AFTER `simpleName`,
	ADD COLUMN `remoteKey` varchar(128) binary default NULL AFTER `flagStr`,
	ADD COLUMN `lastLogin` datetime default NULL AFTER `timezone`,
	ADD COLUMN `loginCount` int(11) NOT NULL default 0 AFTER `timezone`,
	ADD COLUMN `createTime` datetime NOT NULL default '2008-03-19 10:00:00' AFTER `timezone`,
	ADD INDEX `RKUSER` (`remoteKey`);

ALTER TABLE `iten_userspace`
	ADD COLUMN `createTime` datetime NOT NULL default '2008-03-19 10:00:00' AFTER `brand`,
	ADD COLUMN `featuresStr` varchar(255) default NULL AFTER `brand`;

ALTER TABLE `iten_repo_node`
	ADD COLUMN `createTime` datetime NOT NULL default '2008-03-19 10:00:00' AFTER `collection`;

ALTER TABLE `iten_uspace_user`
	ADD COLUMN `createTime` datetime NOT NULL default '2008-03-19 10:00:00';

ALTER TABLE `iten_group_user`
	ADD COLUMN `roleStr` varchar(128) default NULL,
	ADD COLUMN `joinTime` datetime NOT NULL default '2008-03-19 10:00:00';

# set the new version  	
UPDATE iten_config SET vNumber = '1.13' WHERE component = 'schema';
