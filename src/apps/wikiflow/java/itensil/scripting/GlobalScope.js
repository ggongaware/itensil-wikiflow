/**
 * Always available Host.
 * @type HostInterface
 */
var Host = new HostInterface();

/**
 * XML Data manager. In activity context, set to rules.xml, and will auto-save rules 
 * after the current flow-events.
 * @type DomDataInterface
 */
var data = new DomDataInterface();

/**
 * Web Services.
 * @type WebServiceInterface
 */
var ws = new WebServiceInterface();

/**
 * File repository.
 * @type FilesInterface
 */
var files = new FilesInterface();

/**
 * Set to actual activity if running in activity.
 * @type ActivityInterface
 */
var activity = new ActivityInterface();

/**
 * Entities manager.
 * @type EntitiesInterface
 */
var entities = new EntitiesInterface();
