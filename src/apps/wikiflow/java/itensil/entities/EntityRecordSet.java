package itensil.entities;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;

import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NotFoundException;
import itensil.repository.Repository;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryNode;
import itensil.repository.hibernate.NodeEntity;
import itensil.repository.hibernate.VersionEntity;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.Pair;
import itensil.util.UriHelper;
import itensil.workflow.activities.state.Activity;

public class EntityRecordSet {

	MutableRepositoryNode modelRepoRoot;
	NodeEntity recsNode;
	VersionEntity recsVersion;
	
	public EntityRecordSet(MutableRepositoryNode modelRepoRoot) 
			throws NotFoundException, AccessDeniedException {
		this.modelRepoRoot = modelRepoRoot;
		recsNode = (NodeEntity)
			RepositoryHelper.getNode(UriHelper.absoluteUri(modelRepoRoot.getUri(), "records"), true);
	
	}
	
	public MutableRepositoryNode createRecord(User owner) 
			throws AccessDeniedException, NotFoundException, DuplicateException, LockException {
		
		recsVersion = recsNode.getDefaultVersionEnt();
		if (recsVersion == null) {
			recsVersion = recsNode.createVersion(new DefaultNodeVersion());
			
			// just incase the folder lost its record ID, well head-start it
			int count = recsNode.getChildEntities().size();
			if (count > 0)
				recsVersion.setIeRecordId(count + 10);
		}
		
		long recId = recsVersion.getIeRecordId() + 1;
		
		Repository repo = recsNode.getRepository();
		String cRecUri = null;
		
		while (recId < 999999) {
			
			// 000001
			String name = String.format("%1$06d", recId);
			cRecUri = UriHelper.absoluteUri(recsNode.getUri(), name);
			try {
				repo.getNodeByUri(cRecUri, false);
			} catch (AccessDeniedException ade) {
				// skip this one...
			} catch (NotFoundException nfe) {
				// use this one!
				break;
			}
			recId++;
		}
		
		if (recId >= 999999) {
			throw new AccessDeniedException("New Record", "Maximum record limit reached");
		}
		
		recsVersion.setIeRecordId(recId);
		HibernateUtil.getSession().update(recsVersion);
		
		// create folder
		NodeEntity recNode = (NodeEntity)repo.createNode(cRecUri, true, owner);
		
		// set record id meta
		VersionEntity recVer = recNode.getDefaultVersionEnt();
		if (recVer == null) {
			recVer = recNode.createVersion(new DefaultNodeVersion());
		}
		recVer.setIeRecordId(recId);
		HibernateUtil.getSession().update(recVer);
		
		// copy default data.xml
		RepositoryHelper.copy(
				UriHelper.absoluteUri(modelRepoRoot.getUri(), "data.xml"), 
				UriHelper.absoluteUri(recNode.getUri(), "data.xml"), false);
		
		// entity template
    	try {
            MutableRepositoryNode templFold = RepositoryHelper.getNode(
                   UriHelper.absoluteUri(modelRepoRoot.getUri(), "template"), false);
            
            String recUri = recNode.getUri();
            
            for (RepositoryNode kid : templFold.getChildren()) {
         	   try {
         		   String kidDstUri = UriHelper.absoluteUri(recUri, UriHelper.name(kid.getUri()));
         		   ((MutableRepositoryNode)kid).copy(kidDstUri, true);
         	   } catch (DuplicateException de) {
         		   // eat it
         		   // TODO - merge duplicate xml files
         	   }
            }
            
     	} catch (NotFoundException nfe) {
     		// eat it
     	}
		
		
		return recNode;
	}
	
	@SuppressWarnings("unchecked")
	public List<VersionEntity> listAll() {
		Query qry = HibernateUtil.getSession().getNamedQuery("Entity.allRecords");
		qry.setEntity("recsNode", recsNode);
		return filterAccess(qry.list());
	}
	
	public static Element recordXML(Element recParent, VersionEntity recVer, EntityRecordIndexer recIdx) {
		Element recEl = recParent.addElement("record");
		recEl.addAttribute("id", String.valueOf(recVer.getIeRecordId()));
		recEl.addAttribute("uri", UriHelper.name(recVer.getNodeEntity().getLocalUri()));
		List<Pair<String, String>> vals = recIdx.getValues(recVer);
		for (Pair<String, String> valPair : vals) {
			DocumentHelper.makeElement(recEl, valPair.first).setText(Check.emptyIfNull(valPair.second));
		}
		return recEl;
	}
	
	public List<VersionEntity> findIds(String[] ids) {
		Long lids[] = new Long[ids.length];
		for (int ii = 0; ii < ids.length; ii++) {
			lids[ii] = Long.valueOf(ids[ii]);
		}
		return findIds(lids);
	}
	
	@SuppressWarnings("unchecked")
	public List<VersionEntity> findIds(Long[] ids) {
		Query qry = HibernateUtil.getSession().getNamedQuery("Entity.recsById");
		qry.setEntity("recsNode", recsNode);
		qry.setParameterList("recIds", ids);
		return filterAccess(qry.list());
	}
	
	@SuppressWarnings("unchecked")
	public List<VersionEntity> activityRelation(Activity actEnt, String relName) {
		Query qry = HibernateUtil.getSession().getNamedQuery("Entity.recsByActivity");
		qry.setEntity("act", actEnt);
		qry.setEntity("recsNode", recsNode);
		qry.setString("relName", relName);
		
		return filterAccess(qry.list());
	}
	
	protected List<VersionEntity> filterAccess(List<VersionEntity> vers) {
		ArrayList<VersionEntity> fVers = new ArrayList<VersionEntity>(vers.size());
		for (VersionEntity vent : vers) {
			if (vent.getNodeEntity().readAccess()) {
				fVers.add(vent);
			}
			
		}
		return fVers;
	}
}

