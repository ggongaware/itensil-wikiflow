# Table: 'iten_config'
#
CREATE TABLE `iten_config` (
	`component` varchar(64) NOT NULL default '',
	`vNumber` varchar(64) NOT NULL default '',
	`properties` text default NULL,
	PRIMARY KEY  (`component`)
) ENGINE=InnoDB;

# Current complete create version
INSERT INTO iten_config VALUES ('schema','1.2',NULL);
INSERT INTO iten_config VALUES ('sys_repo','','skip');

# Table: 'iten_uspace_user'
#
CREATE TABLE `iten_uspace_user` (
	`userId` varchar(20) binary NOT NULL,
	`userSpaceId` varchar(20) binary NOT NULL,
	`roleStr` varchar(255) default NULL,
	PRIMARY KEY  (`userId`, `userSpaceId`)
) ENGINE=InnoDB;


ALTER TABLE `iten_userspace` ADD COLUMN `brand` varchar(16) default NULL,
	ADD COLUMN  `disabled` tinyint(1) default 0;

# copy userspaceids
INSERT INTO iten_uspace_user (userId, userSpaceId, roleStr)
	SELECT id, userSpaceId, roleStr FROM iten_user;
	
# Add userSpaceId to activity
#
ALTER TABLE `iten_wf_activity` 
	ADD COLUMN `userSpaceId` VARCHAR(20) BINARY DEFAULT NULL AFTER `flowId`;	

# copy the submitter's userspaceId
UPDATE iten_wf_activity, iten_user
	SET iten_wf_activity.userSpaceId = iten_user.userSpaceId
	WHERE iten_wf_activity.submitId = iten_user.id;


# drop userspace from user
ALTER TABLE iten_user DROP COLUMN userSpaceId,
	DROP COLUMN roleStr;