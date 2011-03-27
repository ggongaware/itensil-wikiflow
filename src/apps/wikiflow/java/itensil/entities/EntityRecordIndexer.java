package itensil.entities;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.RepositoryHelper;
import itensil.repository.hibernate.NodeEntity;
import itensil.repository.hibernate.VersionEntity;
import itensil.security.SecurityAssociation;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

public class EntityRecordIndexer {

	public static final int MAX_INDEX = 12;
	
	protected static Logger logger = Logger.getLogger(EntityRecordIndexer.class);
	
	Document modelDoc;
	EntityManager entMan;
	ArrayList<Pair<String, Element>> browseIndex = new ArrayList<Pair<String, Element>>(MAX_INDEX);
	HashMap<String, Pair<EntityRecordIndexer, EntityRecordSet>> relatedIndexes = 
		new HashMap<String, Pair<EntityRecordIndexer, EntityRecordSet>>(MAX_INDEX);
	
	public EntityRecordIndexer(Document modelDoc) {
		this.modelDoc = modelDoc;
		Element modDat = modelDoc.getRootElement().element("data");
		// XPath namespace help
		modelDoc.getRootElement().addNamespace("ent", "http://itensil.com/ns/entity");
		Pair sparseIndex[] = new Pair[MAX_INDEX];
		eattr : for (Object attrObj : modDat.selectNodes(".//ent:attr|ent:entity")) {
			Element attrEl = (Element)attrObj;
			String strIdx = attrEl.attributeValue("browse");
			if (!Check.isEmpty(strIdx)) {
				int idx = Integer.parseInt(strIdx) - 1;
				if (idx < MAX_INDEX) {
					String path = attrEl.attributeValue("name");
					if (!Check.isEmpty(path) && EntityModelUpdater.varNameRx.matcher(path).matches()) {
						Element parEl = attrEl.getParent();
						while (parEl != modDat) {
							String elName = parEl.attributeValue("name");
							if (Check.isEmpty(elName) || !EntityModelUpdater.varNameRx.matcher(elName).matches())
								continue eattr;
							path = parEl.attributeValue("name") + "/" + path;
							parEl = parEl.getParent();
						}
						sparseIndex[idx] = new Pair<String, Element>(path, attrEl);
					}
				}
			}
		}
		
		// compressed index
		for (Pair<String, Element> idxPair : sparseIndex) {
			if (idxPair != null) browseIndex.add(idxPair);
		}
	}
	
	public void indexRecord(MutableRepositoryNode recordNode) 
			throws AccessDeniedException, NotFoundException, LockException {
		
		String datUri = UriHelper.absoluteUri(recordNode.getUri(), "data.xml");
		indexRecord(recordNode, RepositoryHelper.loadContent(datUri));
	}
	
	public void indexRecord(MutableRepositoryNode recordNode, InputStream dataIn) {
		Document doc;
		try {
			doc = XMLDocument.readStream(dataIn);
		} catch (DocumentException ex) {
			logger.warn("Problem parsing entity data record [" + recordNode.getUri() + "]", ex);
			return;
		}
		indexRecord(recordNode, doc);
	}
	
	public void indexRecord(MutableRepositoryNode recordNode, Document dataDoc) {
		
		if (recordNode instanceof NodeEntity) {
			
			VersionEntity vent = ((NodeEntity)recordNode).getDefaultVersionEnt();
			if (vent == null) {
				vent = ((NodeEntity)recordNode).createVersion(new DefaultNodeVersion());
				vent.setIeRecordId(Long.parseLong(UriHelper.name(recordNode.getUri())));
			}
			Element dataRoot = dataDoc.getRootElement();
			Iterator<Pair<String, Element>> itr = browseIndex.iterator();
			vent.setIeBrowse0(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse1(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse2(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse3(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse4(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse5(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse6(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse7(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse8(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowse9(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowseA(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			vent.setIeBrowseB(itr.hasNext() ? indexValue(itr.next(), dataRoot) : null);
			
			HibernateUtil.getSession().update(vent);
		}
		
		// MAYBE_TODO - Other node repostiory types support??
	}
	
	private String getEntityValue(String name, Element entityType, String recordId) {
		if (entMan == null) {
			entMan = new EntityManager(SecurityAssociation.getUser());
		}
		Pair<EntityRecordIndexer, EntityRecordSet> idxSetPair = relatedIndexes.get(name);
		if (idxSetPair == null) {
			String entity = entityType.attributeValue("type");
			if (!Check.isEmpty(entity)) {
				try {
					String entRootUri = entMan.getModelRootUri(entity);
			        
			        String modelUri = UriHelper.absoluteUri(entRootUri, "model.entity");
			       
			        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
			        
			        EntityRecordIndexer recIdx = new EntityRecordIndexer(XMLDocument.readStream(RepositoryHelper.loadContent(modelUri)));
			        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
			        
			        idxSetPair = new Pair<EntityRecordIndexer, EntityRecordSet>(recIdx,recSet);
			        
			        relatedIndexes.put(name, idxSetPair);
			        
				} catch (Exception ex) {
					logger.warn("Problem indexing related entity: " + entity, ex);
					return null;
				}
			} else {
				return null;
			}
		}
		List<VersionEntity> vents = idxSetPair.second.findIds(new String[]{recordId});
		if (!vents.isEmpty()) {
			List<Pair<String, String>> vals = idxSetPair.first.getValues(vents.get(0));
			String fbc = vals.isEmpty() ? "" : vals.get(0).second;
			return recordId + ":" + (fbc.length() > 100  ? Check.maxLength(fbc, 100) + "..." : fbc);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private String indexValue(Pair<String, Element> idxPair, Element dataParent) {
		
		List<Element> datElems = dataParent.selectNodes(idxPair.first);
		if (datElems.isEmpty()) return null;
		Element elem = idxPair.second;
		
		if ("entity".equals(elem.getName())) {
			String recId = null;
			for (Element datElem : datElems) {
				recId = datElem.attributeValue("ie_recId");
				if (!Check.isEmpty(recId)) break;
			}
			return Check.isEmpty(recId) ? null : getEntityValue(idxPair.first, elem, recId) ;
			
		} else {

			return datElems.get(0).getText();
		}
	}

	public List<Pair<String, String>> getValues(VersionEntity vent) {
		ArrayList<Pair<String, String>> values = new ArrayList<Pair<String, String>>(MAX_INDEX);
		Iterator<Pair<String, Element>> itr = browseIndex.iterator();
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse0()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse1()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse2()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse3()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse4()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse5()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse6()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse7()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse8()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowse9()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowseA()));
		if (itr.hasNext()) values.add(new Pair<String, String>(itr.next().first, vent.getIeBrowseB()));
		return values;
		
	}
	
}
