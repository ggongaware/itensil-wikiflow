package itensil.entities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import itensil.entities.hibernate.EntityActivity;
import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.repository.RepositoryHelper;
import itensil.repository.hibernate.VersionEntity;
import itensil.security.DefaultGroup;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.workflow.RunException;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.model.AppElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.rules.ActivityStepException;
import itensil.workflow.rules.EvalException;
import itensil.workflow.state.StateException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;
import org.xml.sax.SAXException;

public class EntityManager {

	User user;
	DocumentFactory docFac;
	
	public EntityManager(User user) {
		this(user, DocumentFactory.getInstance());
	}
	
	public EntityManager(User user, DocumentFactory docFac) {
		this.user = user;
		this.docFac = docFac;
	}
	
	/**
	 * 
	 * @param entity - the name (Folder name)
	 * 
	 * @return
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DocumentException 
	 */
	public Document recordList(String entity) 
		throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        
        String modelUri = UriHelper.absoluteUri(entRootUri, "model.entity");
       
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        
        EntityRecordIndexer recIdx = new EntityRecordIndexer(XMLDocument.readStream(RepositoryHelper.loadContent(modelUri)));
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
        
        Document doc = emptyRecords(entRepoRoot);
        Element root = doc.getRootElement();
        
        // XMLize the records
        for (VersionEntity vent : recSet.listAll()) {
        	EntityRecordSet.recordXML(root, vent, recIdx);
        }
        
        return doc;
	}
	
	/**
	 * 
	 * @param entity - the name (Folder name)
	 * 
	 * @return
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DocumentException 
	 */
	public Document recordsFind(String entity, String ids[]) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        
        String modelUri = UriHelper.absoluteUri(entRootUri, "model.entity");
       
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        
        EntityRecordIndexer recIdx = new EntityRecordIndexer(XMLDocument.readStream(RepositoryHelper.loadContent(modelUri)));
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
    	
        Document doc = emptyRecords(entRepoRoot);
        Element root = doc.getRootElement();
        
        for (VersionEntity vent : recSet.findIds(ids)) {
        	EntityRecordSet.recordXML(root, vent, recIdx);
        }
        
        return doc;
	}
	
	/**
	 * 
	 *
	 * 
	 * @return
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DocumentException 
	 */
	public String recordDataUri(String entity, String id) 
			throws NotFoundException, AccessDeniedException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
      
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
        
        for (VersionEntity vent : recSet.findIds(new String[]{id})) {
        	return UriHelper.absoluteUri(vent.getNodeEntity().getUri(), "data.xml");
        }
        
        return null;
	}
	
	/**
	 * 
	 * @param entity
	 * @param activity
	 * @param relName
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DocumentException
	 * @throws LockException
	 */
	public Document recordsFindByActivity(String entity, String activity, String relName) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        
        String modelUri = UriHelper.absoluteUri(entRootUri, "model.entity");
       
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        
        EntityRecordIndexer recIdx = new EntityRecordIndexer(XMLDocument.readStream(RepositoryHelper.loadContent(modelUri)));
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
    	
        Document doc = emptyRecords(entRepoRoot);
        Element root = doc.getRootElement();

        UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        Activity actEnt = uActivities.getActivity(activity);
        if (actEnt == null) throw new NotFoundException(activity);

        for (VersionEntity vent : recSet.activityRelation(actEnt, Check.emptyIfNull(relName))) {
        	EntityRecordSet.recordXML(root, vent, recIdx);
        }
        
        return doc;
	}
	
	/**
	 * 
	 * @param actEnt
	 * @return list sorted by entityId then date
	 */
	@SuppressWarnings("unchecked")
	public List<EntityActivity> recordsAllInActivity(Activity actEnt) {
		Query qry = HibernateUtil.getSession().getNamedQuery("Entity.allRecsActivity");
		qry.setEntity("act", actEnt);
		return (List<EntityActivity>)qry.list();
	}
	
	/**
	 * 
	 * @param entity - the name (Folder name)
	 * 
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DuplicateException 
	 * 
	 */
	public MutableRepositoryNode createRecord(String entity) 
			throws NotFoundException, AccessDeniedException, DuplicateException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
        
        MutableRepositoryNode resNod = recSet.createRecord(user);
        
        return resNod;
	}
	
	
	/**
	 * 
	 * @param entity - the name (Folder name)
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * 
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DuplicateException 
	 * 
	 */
	public MutableRepositoryNode createRecordForActivity(String entity, String activity, String relName) 
			throws NotFoundException, AccessDeniedException, DuplicateException, LockException {
		
		String entRootUri = getModelRootUri(entity);
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        
        EntityRecordSet recSet = new EntityRecordSet(entRepoRoot);
        
        MutableRepositoryNode resNod = recSet.createRecord(user);
        
		UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
        Activity actEnt = uActivities.getActivity(activity);
        if (actEnt == null) throw new NotFoundException(activity);
        if (!Check.isEmpty(actEnt.getContextGroupId())) {
        	resNod.setContextGroup(new DefaultGroup(actEnt.getContextGroupId()));
        	RepositoryHelper.saveNode(resNod);
        }
        
        EntityActivity entAct = new EntityActivity();
        entAct.initNew();
        entAct.setActivity(actEnt);
        entAct.setEntityId(entRepoRoot.getNodeId());
        entAct.setName(Check.emptyIfNull(relName));
        entAct.setRecordId(Long.parseLong(
        		resNod.getPropertyValue(PropertyHelper.itensilEntityQName("recordId"))
        		));
        HibernateUtil.getSession().save(entAct);
        
        return resNod;
	}
	
	/**
	 * 
	 * @param entityNameOrId - the name (Folder name) or nodeId
	 * @throws AccessDeniedException 
	 * 
	 */
	public String getModelRootUri(String entityNameOrId) throws AccessDeniedException {
		
		// test for id
		if (entityNameOrId.length() == 20) {
			try {
				MutableRepositoryNode entRepoRoot = RepositoryHelper.getNodeById(entityNameOrId, false);
				return entRepoRoot.getUri();
			} catch (NotFoundException e) { /* continue and try as name */ }
		}
    	
    	return  UriHelper.absoluteUri(
        		UriHelper.absoluteUri(
        				RepositoryHelper.getPrimaryRepository().getMount(), "entity"),
        				entityNameOrId);
    }
	
	/**
	 * 
	 * @param entity
	 * @param recordId
	 * @param event
	 * @return
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * @throws DocumentException
	 * @throws LockException
	 * @throws IOException
	 * @throws SAXException
	 * @throws DuplicateException
	 * @throws StateException
	 * @throws EvalException
	 * @throws RunException
	 * @throws ActivityStepException
	 * @throws UserSpaceException
	 */
	@SuppressWarnings("unchecked")
	public List<Activity> userEvent(String entity, String recordId, String event) 
			throws AccessDeniedException, NotFoundException, DocumentException, LockException, 
			IOException, SAXException, DuplicateException, StateException, EvalException, 
			RunException, ActivityStepException, UserSpaceException {

		List<Activity> resActs = new ArrayList<Activity>();
		
    	String entRootUri = getModelRootUri(entity);
    	MutableRepositoryNode entModNode = RepositoryHelper.getNode(UriHelper.absoluteUri(entRootUri, "model.entity"), false);
    	Document modDoc = XMLDocument.readStream(RepositoryHelper.loadContent(entModNode));
    	//String xpath = "events/event[@type = '" + HTMLEncode.sglQuoteEncode(event) + "']";
    	Element evtsElem = modDoc.getRootElement().element("events");
    	Element evtElem = null;
    	if (evtsElem != null) {	
    		for (Element tstElem : (List<Element>)evtsElem.elements("event")) {
    			if (event.equals(tstElem.attributeValue("type"))) {
    				evtElem = tstElem;
    				break;
    			}
    		}
    	}
    	
    	
    	if (evtElem == null) throw new NotFoundException("Event: " + event);

    	UserActivities uActs = new UserActivities(user, HibernateUtil.getSession());

    	for (Element actElem : (List<Element>)evtElem.elements("action")) {
    		if ("flow".equals(actElem.attributeValue("type"))) {
    			String entName = UriHelper.name(UriHelper.getParent(entModNode.getUri()));
    			String flowUri = RepositoryHelper.resolveUri(UriHelper.getParent(actElem.attributeValue("flow")));
    			
    			// launch it
    			Activity actEnt = uActs.launch(event + " " + entName + " " + recordId, 
    					"Running " + UriHelper.name(flowUri), flowUri, null, null, null, null, null, new HashMap<String,String>());
    			
    			FlowModel flowMod = uActs.getLastModel();
    			String relName = "";
    			
    			for (AppElement dataElem : flowMod.matchAppElements("http://itensil.com/workflow", "data")) {
    				for (AppElement entElem : dataElem.matchChildElements("entity")) {
    					String eType = entElem.getAttribute("type");
    					if (!Check.isEmpty(eType) && (eType.equals(entModNode.getParentNodeId()) || eType.equals(entName))) {
    						relName = entElem.getAttribute("name");
    						break;
    					}
    				}
    			}

    			// relate it
    			EntityActivity entAct = new EntityActivity();
    	        entAct.initNew();
    	        entAct.setActivity(actEnt);
    	        entAct.setEntityId(entModNode.getParentNodeId());
    	        entAct.setName(relName);
    	        entAct.setRecordId(Long.parseLong(recordId));
    	        
    	        HibernateUtil.getSession().save(entAct);
    	        
    			// response
    	        resActs.add(actEnt);
    		}
    	}
    	
    	return resActs;
	}

	
	/**
	 * 
	 * @param entity
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 */
	public Document emptyRecords(String entity) throws NotFoundException, AccessDeniedException {
		String entRootUri = getModelRootUri(entity);
		return emptyRecords(RepositoryHelper.getNode(entRootUri, false));
	} 
	
	
	/**
	 * 
	 * @param entRepoRoot
	 * @return
	 */
	public Document emptyRecords(MutableRepositoryNode entRepoRoot) {
		Document doc = docFac.createDocument();
        Element root = doc.addElement("records");
        root.addAttribute("entityUri", entRepoRoot.getUri());
        root.addAttribute("entityId", entRepoRoot.getNodeId());
        return doc;
	}

	@SuppressWarnings("unchecked")
	public List<Activity> recordActivities(String entity, String id) throws AccessDeniedException, NotFoundException {
		String entRootUri = getModelRootUri(entity);
        MutableRepositoryNode entRepoRoot = RepositoryHelper.getNode(entRootUri, false);
        Query qry = HibernateUtil.getSession().getNamedQuery("Entity.activitiesByRec");
		qry.setString("entityId", entRepoRoot.getNodeId());
		qry.setString("recordId", id);
		List<EntityActivity> entActs = (List<EntityActivity>)qry.list();
		List<Activity> acts = new ArrayList<Activity>(entActs.size());
		for (EntityActivity ea : entActs) {
			acts.add(ea.getActivity());
		}
		return acts;
	}
}
