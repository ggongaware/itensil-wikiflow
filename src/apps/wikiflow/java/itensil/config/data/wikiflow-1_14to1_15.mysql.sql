#
# custom fields to users and groups
#
ALTER TABLE `iten_uspace_user`
	ADD COLUMN `custom1` varchar(128) binary default NULL,
	ADD COLUMN `custom2` varchar(128) binary default NULL,
	ADD COLUMN `custom3` varchar(128) binary default NULL,
	ADD COLUMN `custom4` varchar(128) binary default NULL;


ALTER TABLE `iten_group`
	ADD COLUMN `custom1` varchar(128) binary default NULL,
	ADD COLUMN `custom2` varchar(128) binary default NULL,
	ADD COLUMN `custom3` varchar(128) binary default NULL,
	ADD COLUMN `custom4` varchar(128) binary default NULL;
	
ALTER TABLE `iten_userspace`
	ADD COLUMN `alertEmailer` varchar(255) default NULL;

#
# Context group/org to activities
#
ALTER TABLE `iten_wf_activity`
	ADD COLUMN `contextGroupId` varchar(20) binary default NULL AFTER `submitId`;

# set the new version  	
UPDATE iten_config SET vNumber = '1.15' WHERE component = 'schema';
