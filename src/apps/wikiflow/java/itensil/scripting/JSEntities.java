package itensil.scripting;


import java.io.IOException;
import java.util.List;

import itensil.entities.EntityManager;
import itensil.repository.AccessDeniedException;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.NotFoundException;
import itensil.repository.RepositoryHelper;
import itensil.scripting.util.JSDomData;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.workflow.RunException;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.rules.ActivityStepException;
import itensil.workflow.rules.EvalException;
import itensil.workflow.state.StateException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.SAXReader;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.xml.sax.SAXException;

public class JSEntities extends ScriptableObject {

	EntityManager entMan;
	
	public JSEntities(User user) {
		this.entMan = new EntityManager(user, DOMDocumentFactory.getInstance());
		
		String funcs[] = {"createRecord", "createRecordForActivity",
				"recordsFindByActivity", "recordList", "recordFind", "recordActivities",
				"recordData", "userEvent" };
        try {
            this.defineFunctionProperties(
                funcs,
                JSEntities.class,
                ScriptableObject.PERMANENT |
                ScriptableObject.READONLY);
        } catch (RhinoException e) {
            e.printStackTrace();
        }
        sealObject();
	}
	
	/**
	 * 
	 * @param entity
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DuplicateException
	 * @throws LockException
	 */
	public String createRecord(String entity) 
			throws NotFoundException, AccessDeniedException, DuplicateException, LockException {
		
		return entMan.createRecord(entity).getUri();
	}
	
	/**
	 * 
	 * @param entity
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DocumentException
	 * @throws LockException
	 */
	public org.w3c.dom.Document recordList(String entity) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		return (org.w3c.dom.Document)entMan.recordList(entity);
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
	public org.w3c.dom.Document recordsFindByActivity(String entity, String activity, String relName) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		
		return (org.w3c.dom.Document)entMan.recordsFindByActivity(entity, activity, relName);
	}
	
	/**
	 * 
	 * @param entity
	 * @param id
	 * 
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DocumentException
	 * @throws LockException
	 */
	public org.w3c.dom.Document recordFind(String entity, String id) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException {
		
		return (org.w3c.dom.Document)entMan.recordsFind(entity, new String[]{id});
	}
	
	/**
	 * 
	 * @param entity
	 * @param id
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DocumentException
	 * @throws LockException
	 */
	public JSDomData recordData(String entity, String id) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException  {
		
		String datUri = entMan.recordDataUri(entity, id);
		
		if (!Check.isEmpty(datUri)) {
			SAXReader reader = new SAXReader(DOMDocumentFactory.getInstance());
			return new JSDomData(reader.read(RepositoryHelper.loadContent(datUri)), datUri);
		}
		
		return null;
	}
	
	
	public org.w3c.dom.Document recordActivities(String entity, String id) 
			throws NotFoundException, AccessDeniedException, DocumentException, LockException  {
	
		List<Activity> resActs = entMan.recordActivities(entity, id);
		
		Document resDoc = DOMDocumentFactory.getInstance().createDocument();
    	Element resRoot = resDoc.addElement("entity-activities");
    	
        for (Activity actEnt : resActs) {
        	// response
			resRoot.add(ActivityXML.display(actEnt));
        }
        
        return (org.w3c.dom.Document) resDoc;
	}
	
	/**
	 * 
	 * @param entity
	 * @param activity
	 * @param relName
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 * @throws DuplicateException
	 * @throws LockException
	 */
	public String createRecordForActivity(String entity, String activity, String relName) 
			throws NotFoundException, AccessDeniedException, DuplicateException, LockException {
		
		return entMan.createRecordForActivity(entity, activity, relName).getUri();
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
	public org.w3c.dom.Document userEvent(String entity, String recordId, String event) 
			throws AccessDeniedException, NotFoundException, DocumentException, LockException, 
			IOException, SAXException, DuplicateException, StateException, EvalException, 
			RunException, ActivityStepException, UserSpaceException {
	
		List<Activity> resActs = entMan.userEvent(entity, recordId, event);
        
        Document resDoc = DOMDocumentFactory.getInstance().createDocument();
    	Element resRoot = resDoc.addElement("event-results");
    	
        for (Activity actEnt : resActs) {
        	// response
			resRoot.add(ActivityXML.display(actEnt));
        }
        
        return (org.w3c.dom.Document) resDoc;
	}
	
	
	public String getClassName() {
		return "JSEntities";
	}

}
