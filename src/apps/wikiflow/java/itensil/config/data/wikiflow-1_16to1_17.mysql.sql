#
# generic signaling processing 
#

ALTER TABLE  iten_wf_activity_alert  
    ADD COLUMN  discriminator varchar(20),
    ADD COLUMN signalSubmitId varchar(20) default NULL,
    ADD COLUMN  signalAssignedId varchar(20) default NULL,
    ADD COLUMN  signalLink varchar(64) default NULL,
    ADD COLUMN  signalMessage varchar(256) default NULL,
    ADD COLUMN  `custom1` varchar(128) character set latin1 collate latin1_bin default NULL,
    ADD COLUMN  `custom2` varchar(128) character set latin1 collate latin1_bin default NULL,
    ADD COLUMN  `custom3` varchar(128) character set latin1 collate latin1_bin default NULL,
    ADD COLUMN  `custom4` varchar(128) character set latin1 collate latin1_bin default NULL,
 	ADD UNIQUE INDEX signalSubmitId(`signalSubmitId`),
 	ADD UNIQUE INDEX signalAssignedId(`signalAssignedId`);

# all previously existing alerts were of type activity alert, ste their default value
UPDATE iten_wf_activity_alert SET discriminator='ACTIVITY_ALERT' where discriminator is null; 	

# set the new version  	
UPDATE iten_config SET vNumber = '1.17' WHERE component = 'schema';