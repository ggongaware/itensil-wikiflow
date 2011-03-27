ALTER TABLE `iten_wf_activity`
	ADD COLUMN `variationId` varchar(20) binary default NULL;

# set the new version
UPDATE iten_config SET vNumber = '1.6' WHERE component = 'schema';
