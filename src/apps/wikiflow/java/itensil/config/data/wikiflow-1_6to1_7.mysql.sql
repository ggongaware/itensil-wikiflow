# Make some key column case sensitive

ALTER TABLE `iten_wf_flow_role`
	MODIFY COLUMN  `role` varchar(255) binary default NULL;

ALTER TABLE `iten_wf_activity_state` 
	MODIFY COLUMN `stStepId` VARCHAR(255) BINARY default NULL;

ALTER TABLE `iten_wf_activity_plan` 
	MODIFY COLUMN `stepId` VARCHAR(255) BINARY default NULL;

ALTER TABLE `iten_wf_activity_role`
	MODIFY COLUMN `role` varchar(255) binary default NULL;

ALTER TABLE `iten_wf_activity_timer`
	MODIFY COLUMN  `timerId` varchar(255) binary;
    
# set the new version  	
UPDATE iten_config SET vNumber = '1.7' WHERE component = 'schema';