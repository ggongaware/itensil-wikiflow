package itensil.entities;

import itensil.io.xml.XMLDocument;
import itensil.repository.RepositoryHelper;
import itensil.rules.RulesXPathFunctions;
import itensil.util.UriHelper;

import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.tree.BaseElement;

public class EntityLazyRecordRoot extends BaseElement {

	protected static Logger logger = Logger.getLogger(EntityLazyRecordRoot.class);
			
	EntityManager manager;
	String entity;
	String id;
	String recordUri;
	List recordContent;
	
	public EntityLazyRecordRoot(EntityManager manager, String entity, String id, String name) {
		super(name);
		this.manager = manager;
		this.entity = entity;
		this.id = id;
	}
	
	public EntityLazyRecordRoot(EntityManager manager, String entity, String id) {
		this(manager, entity, id, "entity");
	}

	@Override
	protected List contentList() {
		try {
			if (recordContent == null) {
				recordUri = manager.recordDataUri(entity, id);
				Document doc = XMLDocument.readStream(RepositoryHelper.loadContent(recordUri));
				recordContent = doc.getRootElement().content();
			}
		} catch (Exception ex) {
			logger.warn("Problem with record content", ex);
		}
		if (recordUri != null) {
			RulesXPathFunctions.setRecordUri(UriHelper.getParent(recordUri));
		}
		return recordContent;
	}

	
}
