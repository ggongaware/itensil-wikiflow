#
#  Add relative permission support
#
	
ALTER TABLE iten_repo_permission 
	ADD COLUMN `role` VARCHAR(64) DEFAULT NULL,
	ADD COLUMN `axis` VARCHAR(32) DEFAULT NULL;
	
ALTER TABLE iten_repo_node
	ADD COLUMN `contextGroupId` VARCHAR(20)  DEFAULT NULL AFTER `collection`;

# Database relative group stub
INSERT INTO iten_group VALUES (
	'AAAAAAAAAAAArelative','[Relative]','AAAAAAAAAAAAAAAAAAAA','[Relative]', 0, NULL, 
	NULL, NOW(), NULL, NULL, NULL, NULL);

# set the new version  	
UPDATE iten_config SET vNumber = '1.18' WHERE component = 'schema';