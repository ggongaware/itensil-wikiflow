
# add version columns to version
ALTER TABLE iten_repo_version
	ADD COLUMN `davContentType` VARCHAR(128) DEFAULT NULL,
	ADD COLUMN `davContentLang` VARCHAR(32) DEFAULT NULL,
	ADD COLUMN `davContentLen` INT DEFAULT NULL,
	ADD COLUMN `davEtag` VARCHAR(64) DEFAULT NULL,
	ADD COLUMN `davLastMod` datetime DEFAULT NULL,
	ADD COLUMN `irModifier` VARCHAR(20) binary default NULL,
	ADD COLUMN `irDescription` VARCHAR(255) default NULL,
	ADD COLUMN `irKeywords` VARCHAR(255) default NULL,
	ADD COLUMN `irTags` VARCHAR(255) default NULL,
	ADD COLUMN `irStyle` VARCHAR(255) default NULL,
	ADD COLUMN `cust1Ns` VARCHAR(255) default NULL,
	ADD COLUMN `cust1Val` VARCHAR(255) default NULL,
	ADD COLUMN `cust2Ns` VARCHAR(255) default NULL,
	ADD COLUMN `cust2Val` VARCHAR(255) default NULL,
	ADD COLUMN `cust3Ns` VARCHAR(255) default NULL,
	ADD COLUMN `cust3Val` VARCHAR(255) default NULL,
	ADD COLUMN `cust4Ns` VARCHAR(255) default NULL,
	ADD COLUMN `cust4Val` VARCHAR(255) default NULL;

# move DAV values
UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.davContentType = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'DAV:'
		AND nam.localName = 'getcontenttype';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.davContentLang = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'DAV:'
		AND nam.localName = 'getcontentlanguage';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.davContentLen = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'DAV:'
		AND nam.localName = 'getcontentlength';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.davEtag = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'DAV:'
		AND nam.localName = 'getetag';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.davLastMod = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'DAV:'
		AND nam.localName = 'getlastmodified';


# move Itensil values
UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.irModifier = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'http://itensil.com/repository'
		AND nam.localName = 'modifier';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.irKeywords = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'http://itensil.com/repository'
		AND nam.localName = 'keywords';

UPDATE iten_repo_version ver, iten_repo_property pro, iten_repo_property_name nam
	SET ver.irDescription = pro.pValue
	WHERE ver.id = pro.versionId AND pro.nameId = nam.id
		AND nam.namespaceUri = 'http://itensil.com/repository'
		AND nam.localName = 'description';
		

# drop old property tables
DROP TABLE iten_repo_property;
DROP TABLE iten_repo_property_name;

# Move flow icon
# update existing version
UPDATE iten_repo_version ver, iten_wf_flow_state flo
	SET ver.irStyle = CONCAT('icon:', flo.icon)
	WHERE ver.nodeId = flo.id;

# create missing versions
INSERT INTO iten_repo_version (nodeId, isDefault, vNumber, irStyle)
	SELECT flo.id, 1, '1.0', CONCAT('icon:', flo.icon)
	FROM iten_wf_flow_state flo 
		LEFT JOIN iten_repo_version ver ON ver.nodeId = flo.id
	WHERE ver.id IS NULL;
	
ALTER TABLE iten_wf_flow_state DROP COLUMN icon;


# move version default
ALTER TABLE iten_repo_node ADD COLUMN 
	`defVersionId` bigint(20)  DEFAULT NULL AFTER `collection`;

UPDATE iten_repo_node nod, iten_repo_version ver
	SET nod.defVersionId = ver.id
	WHERE nod.id = ver.nodeId
		AND ver.isDefault = 1;
	
ALTER TABLE iten_repo_version DROP COLUMN isDefault;


# set the new version
UPDATE iten_config SET vNumber = '1.3' WHERE component = 'schema';
