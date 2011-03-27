# New activity state columns to support progress and sub-process

ALTER TABLE `iten_wf_activity_state`
	ADD COLUMN `subActivityId` varchar(20) binary default NULL,
	ADD COLUMN `progress` int(11) NOT NULL default 0;
	
# set the new version  	
UPDATE iten_config SET vNumber = '1.9' WHERE component = 'schema';