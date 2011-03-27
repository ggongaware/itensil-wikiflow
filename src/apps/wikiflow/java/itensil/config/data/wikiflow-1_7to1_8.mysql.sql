# Fix bits to tinyints

ALTER TABLE `iten_wf_activity_alert`
	MODIFY COLUMN  `mailed` tinyint(1);

ALTER TABLE `iten_wf_activity_alert`
	MODIFY COLUMN  `read` tinyint(1);

ALTER TABLE `iten_wf_activity_timer`
	MODIFY COLUMN  `conditional` tinyint(1);

# set the new version  	
UPDATE iten_config SET vNumber = '1.8' WHERE component = 'schema';