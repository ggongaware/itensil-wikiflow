#
# Auth token support
#
ALTER TABLE `iten_user` ADD COLUMN `token` varchar(40) AFTER `lastLogin`,
 	ADD UNIQUE INDEX token(`token`);

 	
# set the new version  	
UPDATE iten_config SET vNumber = '1.16' WHERE component = 'schema';