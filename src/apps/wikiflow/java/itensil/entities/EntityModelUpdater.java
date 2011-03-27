package itensil.entities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.repository.AccessDeniedException;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.Repository;
import itensil.repository.RepositoryHelper;
import itensil.repository.hibernate.VersionEntity;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.UriHelper;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Session;

public class EntityModelUpdater implements Runnable {
	
	public final static Pattern varNameRx = Pattern.compile("^[a-z_]+[a-z0-9_\\.]*$", Pattern.CASE_INSENSITIVE);
	
	protected static Logger logger = Logger.getLogger(EntityModelUpdater.class);
	
	private static HashMap<String, EntityModelUpdater> runningIndexers = 
			new HashMap<String, EntityModelUpdater>();
	
	Document updateDoc;
	String uri;
	User indexUser;
	
	boolean cancelIndex;
	
	boolean browseChanged;
	boolean dataChanged;

	public EntityModelUpdater(Document updateDoc, String uri) {
		this.updateDoc = updateDoc;
		this.uri = uri;
	}

	/**
	 * 
	 * @param user
	 * @throws AccessDeniedException
	 * @throws DocumentException
	 * @throws LockException
	 * @throws NotFoundException
	 * @throws DuplicateException
	 * @throws IOException
	 */
	public void saveUpdate(User user) 
			throws AccessDeniedException, DocumentException,
			LockException, NotFoundException, DuplicateException, IOException {
		
		// load current model
		MutableRepositoryNode repoNode = null;
		Document prevDoc = null;
		try {
			repoNode = RepositoryHelper.getNode(uri, true);
			prevDoc = XMLDocument.readStream(
					RepositoryHelper.loadContent(repoNode));
			
		} catch (NotFoundException nfe) {
			
			// create node
			Repository repo = RepositoryHelper.getRepository(uri);
			repoNode = repo.createNode(uri, false, user);
			
		}
		
		// save new model
		byte xb[] = updateDoc.asXML().getBytes("UTF-8");
		RepositoryHelper.createContent(
				repoNode, new ByteArrayInputStream(xb), xb.length, "application/itensil-entity+xml");
		
		
		if (prevDoc != null) {
			Element prevData = prevDoc.getRootElement().element("data");
			Element upData = updateDoc.getRootElement().element("data");
			
			if (prevData == null || upData == null) return;
			
			// decide to update default data.xml
			dataChanged = !Check.equalStrings(prevData.attributeValue("dataRev"), upData.attributeValue("dataRev"));
						
			// decide to re-index
			browseChanged = !Check.equalStrings(
					prevData.attributeValue("browseRev"), upData.attributeValue("browseRev"));
			
		} else {
			dataChanged = true;
		}
	}

	public boolean needsDataUpdate() {
		return dataChanged;
	}

	/**
	 * Create new default data.xml document
	 * 
	 * @param user
	 * @throws AccessDeniedException 
	 * @throws NotFoundException 
	 * @throws LockException 
	 * @throws DuplicateException 
	 * @throws IOException 
	 */
	public void updateData(User user)
			throws AccessDeniedException, NotFoundException, 
				DuplicateException, LockException, IOException {
		
		String dataUri = UriHelper.absoluteUri(UriHelper.getParent(uri), "data.xml");
		MutableRepositoryNode repoNode = null;
		try {
			
			repoNode = RepositoryHelper.getNode(dataUri, true);

		} catch (NotFoundException nfe) {
			
			// create node
			Repository repo = RepositoryHelper.getRepository(uri);
			repoNode = repo.createNode(uri, false, user);
			
		}
		
		Element upData = updateDoc.getRootElement().element("data");
		
		Document ddoc = DocumentHelper.createDocument();
		Element datRoot = ddoc.addElement("data");
		
		genData(datRoot, upData);
		
		//save new data doc
		byte xb[] = ddoc.asXML().getBytes("UTF-8");
		RepositoryHelper.createContent(
				repoNode, new ByteArrayInputStream(xb), xb.length, "text/xml");
		
		
	}
	
	/**
	 * 
	 * @param datRoot
	 * @param upData
	 */
	protected void genData(Element datRoot, Element upData) {
		// create declared fields as elements
		for (Object obj : upData.elements()) {
			Element upDatEl = (Element)obj;
			if ("attr".equals(upDatEl.getName())) {
				String elName = upDatEl.attributeValue("name");
				if (!Check.isEmpty(elName) && varNameRx.matcher(elName).matches()) {
					
					Element datEl = datRoot.addElement(elName);
					String type = upDatEl.attributeValue("type");
					if (!Check.isEmpty(type) && type.startsWith("ix:composite")) {
						genData(datEl, upDatEl);
					} else {
						String def = upDatEl.attributeValue("default");
						if (!Check.isEmpty(def)) datEl.setText(def);
					}
				}
				
			} else if ("entity".equals(upDatEl.getName())) {
				String elName = upDatEl.attributeValue("name");
				if (!Check.isEmpty(elName) && varNameRx.matcher(elName).matches()) {
					Element entEl = datRoot.addElement(elName);
					
					// sysattr
					entEl.addAttribute("ie_recId", "");
					entEl.addAttribute("ie_createTime", "");
					
					for (Object subObj : upDatEl.elements("attr")) {
						Element upSubAttr = (Element)subObj;
						entEl.addAttribute(upSubAttr.attributeValue("name"), 
								Check.emptyIfNull(upSubAttr.attributeValue("default")));
					}
				}
			}
			
		}
	}

	public boolean needsReIndexing() {
		return browseChanged;
	}

	/**
	 * 
	 * @param user
	 */
	public void asyncIndexer(User user) {
		indexUser = user;
		
		// cancel running jobs for this model
		EntityModelUpdater lastIndex;
		synchronized (runningIndexers) {
			lastIndex = runningIndexers.get(uri);
		}
		
		if (lastIndex != null) 
			lastIndex.stopIndexing();
			
		Thread idxThread = new Thread(this);
		
		synchronized (runningIndexers) {
			runningIndexers.put(uri, this);
		}
		idxThread.setPriority(Thread.MIN_PRIORITY);
		
		idxThread.start();
	}
	
	public void stopIndexing() {
		cancelIndex = true;
	}
	
	/**
	 * 
	 * @param user
	 */
	public void indexRecords(User user) {
		EntityRecordIndexer recIdx = new EntityRecordIndexer(updateDoc);
		try {
			EntityRecordSet recSet = new EntityRecordSet(RepositoryHelper.getNode(UriHelper.getParent(uri), false));
			for (VersionEntity vent : recSet.listAll()) {
				if (cancelIndex) break;
				recIdx.indexRecord(vent.getNodeEntity());
			}
		} catch (Exception ex) {
			logger.warn("Problem re-indexing records", ex);
		}
	}

	public void run() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
        if (!session.contains(indexUser)) {
            session.refresh(indexUser);
        }
		SecurityAssociation.setUser(indexUser);
		indexRecords(indexUser);
		synchronized (runningIndexers) {
			runningIndexers.remove(uri);
		}
		HibernateUtil.commitTransaction();
		HibernateUtil.closeSession();
		SecurityAssociation.clear();
	}

}
