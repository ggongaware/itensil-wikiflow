# Table: 'iten_app_cross_map'
#
CREATE TABLE `iten_app_component_map` (
  `id` varchar(20) binary NOT NULL default '',
  `appId` varchar(10) NOT NULL default '',
  `appProcessId` varchar(128) NOT NULL default '',
  `itenOwnerId` varchar(20) NOT NULL default '',
  `itenComponentType` varchar(10) default NULL,
  `itenComponentName` varchar(256) default NULL,
  `itenComponentId` varchar(256) default NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB;

# set the new version
UPDATE iten_config SET vNumber = '1.19' WHERE component = 'schema';