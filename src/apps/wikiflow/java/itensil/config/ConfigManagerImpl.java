/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import javax.xml.namespace.QName;

import itensil.config.data.SqlLoader;
import itensil.config.hibernate.ConfigEntity;
import itensil.entities.EntityContentListener;
import itensil.entities.EntityDataNodeFilter;
import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodePermission;
import itensil.repository.DefaultNodeProperties;
import itensil.repository.DefaultNodeVersion;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeProperties;
import itensil.repository.NotFoundException;
import itensil.repository.PropertyHelper;
import itensil.repository.Repository;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryManager;
import itensil.repository.RepositoryManagerFactory;
import itensil.repository.filter.MatchFileFilter;
import itensil.repository.hibernate.NodeEntity;
import itensil.security.AuthenticatedUser;
import itensil.security.Everyone;
import itensil.security.Group;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.workflow.activities.rules.CustValDataContentListener;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.exception.SQLGrammarException;

public class ConfigManagerImpl extends ConfigManager {
	
	private static Logger log = Logger.getLogger(ConfigManagerImpl.class);
	
	private boolean ready;
	
	private String configError;
	
	public ConfigManagerImpl() {
		ready = false;
		ConfigManager.manager = this;
	}

	protected void initImpl() {
		if (!ready) {
			synchronized (ConfigManager.class) {
				
				if (!ready) {
					
					// test for config table
					Session sess = null;
					try {
						sess = HibernateUtil.getSession();
					} catch (Exception ex) {
						log.error("Cannot create Hibernate Session.", ex);
						setErrorEx(ex);
					}
					
					try {
						
						
						/*
						 * 
						 *  Initial Schema check
						 * 
						 */
						ConfigEntity schemaConf = null;
						try {
							HibernateUtil.beginTransaction();
							schemaConf = (ConfigEntity)sess.get(ConfigEntity. class, "schema");
							HibernateUtil.commitTransaction();
						} catch (SQLGrammarException ex) { // this exception comes when the tables is missing
							HibernateUtil.commitTransaction();
							
							/*
							 *  Create initial schema
							 */
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow.mysql.sql");
							
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							schemaConf = (ConfigEntity)sess.get(ConfigEntity.class, "schema");
							HibernateUtil.commitTransaction();
						}
						
						if (schemaConf == null) {
							configError = "Could not determine schema version";
							return;
						}
						
						// schema version check here
						if ("1.2".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_2to1_3.mysql.sql");
							schemaConf.setVersion("1.3");
						}
						
						if ("1.3".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_3to1_4.mysql.sql");
							schemaConf.setVersion("1.4");
						}
						
						// update to 1.5 propegates lastmodified of chart.flow dates
						if ("1.4".equals(schemaConf.getVersion())) {
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							SecurityAssociation.setUser(SysAdmin.getUser());
							Query qry = sess.createQuery(
										"FROM NodeEntity node " +
								    	"	JOIN FETCH node.defaultVersionEnt " +
								        "WHERE node.localUri LIKE :uriPat " +
								        "    AND node.deleted = 0 " +
								        "    AND node.defaultVersionEnt.davLastMod IS NOT NULL " +
								       	"ORDER BY node.defaultVersionEnt.davLastMod DESC");
							
							qry.setString("uriPat", "%/chart.flow");
							QName qnLastMod = PropertyHelper.defaultQName("getlastmodified");
							for (Object obj : qry.list()) {
								NodeEntity nod = (NodeEntity)obj;
								NodeEntity parNode = nod.getParentNode();
								if (parNode != null) {
									String lstDate = nod.getPropertyValue(qnLastMod);
									if (!Check.isEmpty(lstDate)) {
										NodeProperties props = parNode.getProperties(new DefaultNodeVersion());
										if (props == null) {
											props = new DefaultNodeProperties(new DefaultNodeVersion());
										}
										props.setValue(qnLastMod, lstDate);
										parNode.setProperties(props);
									}
								}
							}
							
							schemaConf.setVersion("1.5");
							sess.update(schemaConf);
							HibernateUtil.commitTransaction();
							SecurityAssociation.clear();
						}
						
						if ("1.5".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_5to1_6.mysql.sql");
							schemaConf.setVersion("1.6");
						}
						
						if ("1.6".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_6to1_7.mysql.sql");
							schemaConf.setVersion("1.7");
						}
						
						if ("1.7".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_7to1_8.mysql.sql");
							schemaConf.setVersion("1.8");
						}
						
						if ("1.8".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_8to1_9.mysql.sql");
							schemaConf.setVersion("1.9");
						}
						
						// Add new Guest/Editor groups
						if ("1.9".equals(schemaConf.getVersion())) {

							// moved to repository versioning
							
							schemaConf.setVersion("1.10");
						}
						
						
						// Add new Guest.kb
						if ("1.10".equals(schemaConf.getVersion())) {
							
							// moved to repository versioning
							
							schemaConf.setVersion("1.11");
						}
						
						// Add new course folder
						if ("1.11".equals(schemaConf.getVersion())) {
							
							// moved to repository versioning
							
							schemaConf.setVersion("1.11");
						}
						
						if ("1.12".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_12to1_13.mysql.sql");
							schemaConf.setVersion("1.13");
						}
						
						if ("1.13".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_13to1_14.mysql.sql");
							schemaConf.setVersion("1.14");
						}
						
						if ("1.14".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_14to1_15.mysql.sql");
							schemaConf.setVersion("1.15");
						}
						
						if ("1.15".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_15to1_16.mysql.sql");
							schemaConf.setVersion("1.16");
						}

						if ("1.16".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_16to1_17.mysql.sql");
							schemaConf.setVersion("1.17");
						}
						
						if ("1.17".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_17to1_18.mysql.sql");
							schemaConf.setVersion("1.18");
						}
						
						if ("1.18".equals(schemaConf.getVersion())) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("wikiflow-1_18to1_19.mysql.sql");
							schemaConf.setVersion("1.19");
						}
						
						////////////////////////////////////////////////////////////////////////////////////////
						
						// check timecard
						Property prop = getProperty("tcard_schema");
						if (prop == null) {
							SqlLoader sqlLdr = new SqlLoader();
							sqlLdr.execResource("timecard.mysql.sql");
						}
						
						// Future timecard schema version check here
						
						
						////////////////////////////////////////////////////////////////////////////////////////
						
						
						// Repository (files/folders) verioning
						Property repoProp = getProperty("repository");
						if (repoProp == null) {
							repoProp = new Property();
							repoProp.setComponent("repository");
							repoProp.setVersion("1.0");
							repoProp = setProperty(repoProp);
						}
						
						

						// Add new Guest/Editor groups
						if ("1.0".equals(repoProp.getVersion())) {
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							SecurityAssociation.setUser(SysAdmin.getUser());
							
							// get all userspaces
							Query qry = sess.createQuery("FROM UserSpaceEntity");
							
							Iterator itr = qry.iterate();
							while (itr.hasNext()) {
								
								// discover the groups
								UserSpaceEntity uspace = (UserSpaceEntity)itr.next();
								boolean hasEditor = false;
								boolean hasGuest = false;
								for (Group grp : uspace.getGroups()) {
									if ("Editors".equals(grp.getSimpleName())) hasEditor = true;
									else if ("Guests".equals(grp.getSimpleName())) hasGuest = true;
								}
								
								// add missing groups to repo
								RepositoryManager repoMan = RepositoryManagerFactory.getManager(uspace);
								Repository repo = repoMan.getPrimaryRepository();
								if (repo != null) {
									MutableRepositoryNode rootNode = repo.getNodeByUri(repo.getMount(), true);
									if (!hasEditor) {
										Group editGroup = uspace.createGroup("Editors", "Editors");
										rootNode.grantPermission(
												new DefaultNodePermission(editGroup, DefaultNodePermission.WRITE, true));
									}
									if (!hasGuest) {
										Group guestGroup = uspace.createGroup("Guests", "Guests");
										rootNode.grantPermission(
												new DefaultNodePermission(guestGroup, DefaultNodePermission.READ, true));
									}
								}
								
							}
							
							
							repoProp.setVersion("1.1");
							setProperty(repoProp);
							HibernateUtil.commitTransaction();
							
							SecurityAssociation.clear();
						}
						
						
						// Add new Guest.kb
						if ("1.1".equals(repoProp.getVersion())) {
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							SecurityAssociation.setUser(SysAdmin.getUser());
							
							Repository sysRepo = null;
							MutableRepositoryNode guestKbNode = null; 
							
							// get all userspaces
							Query qry = sess.createQuery("FROM UserSpaceEntity");
							
							Iterator itr = qry.iterate();
							while (itr.hasNext()) {
								UserSpaceEntity uspace = (UserSpaceEntity)itr.next();
								RepositoryManager repoMan = RepositoryManagerFactory.getManager(uspace);
								Repository repo = repoMan.getPrimaryRepository();
								if (repo != null) {
									
									// load up the guest node once
									if (guestKbNode == null) {
										sysRepo = repoMan.getRepository("/system");
										guestKbNode = sysRepo.getNodeByUri("/system/.init/Guest.kb", false);
									}
									try {
										repoMan.copy(sysRepo, guestKbNode.getNodeId(),
												repo, UriHelper.absoluteUri(repo.getMount(), "Guest.kb"), false);
									} catch (DuplicateException de) {
										// ok, skip it
									}
								}
							}
							
							repoProp.setVersion("1.2");
							setProperty(repoProp);
							HibernateUtil.commitTransaction();
							
							SecurityAssociation.clear();
						}
						
						// Add new course folder
						if ("1.2".equals(repoProp.getVersion())) {
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							SecurityAssociation.setUser(SysAdmin.getUser());
							
							// get all userspaces
							Query qry = sess.createQuery("FROM UserSpaceEntity");
							
							Iterator itr = qry.iterate();
							while (itr.hasNext()) {
								UserSpaceEntity uspace = (UserSpaceEntity)itr.next();
								RepositoryManager repoMan = RepositoryManagerFactory.getManager(uspace);
								Repository repo = repoMan.getPrimaryRepository();
								if (repo != null) {
									try {
										repo.createNode(UriHelper.absoluteUri(repo.getMount(), "course"), true, 
											repo.getNodeByUri(repo.getMount(), false).getOwner());
									} catch (DuplicateException de) {
										// ok, skip it
									}
								}
							}
							
							repoProp.setVersion("1.3");
							setProperty(repoProp);
							HibernateUtil.commitTransaction();
							
							SecurityAssociation.clear();
						}
						
						
						// Add new entity folder
						if ("1.3".equals(repoProp.getVersion())) {
							sess = HibernateUtil.getSession();
							HibernateUtil.beginTransaction();
							SecurityAssociation.setUser(SysAdmin.getUser());
							
							// get all userspaces
							Query qry = sess.createQuery("FROM UserSpaceEntity");
							
							Iterator itr = qry.iterate();
							while (itr.hasNext()) {
								UserSpaceEntity uspace = (UserSpaceEntity)itr.next();
								RepositoryManager repoMan = RepositoryManagerFactory.getManager(uspace);
								Repository repo = repoMan.getPrimaryRepository();
								if (repo != null) {
									try {
										repo.createNode(UriHelper.absoluteUri(repo.getMount(), "entity"), true, 
											repo.getNodeByUri(repo.getMount(), false).getOwner());
									} catch (DuplicateException de) {
										// ok, skip it
									}
								}
							}
							
							repoProp.setVersion("1.4");
							setProperty(repoProp);
							HibernateUtil.commitTransaction();
							
							SecurityAssociation.clear();
						}
						
						
						ready = true;
						
					} catch (Exception ex) {
						log.error("Schema init error.", ex);
						setErrorEx(ex);
					} finally {
						HibernateUtil.closeSession();
					}
					
					/*
					 * Add some app specific repository listeners
					 */
					RepositoryManagerFactory.addContentChangeListener(
							new MatchFileFilter("rules.xml"), new CustValDataContentListener());
					
					RepositoryManagerFactory.addContentChangeListener(
							new EntityDataNodeFilter(), new EntityContentListener());
				}
			}
		}
	}

	protected boolean isReadyImpl() {
		return ready;
	}

	public void initSystemRepo(String filePath, MimeMapper mimes) {
		
		User sysuser = SysAdmin.getUser();
		
		HibernateUtil.beginTransaction();
		
		AuthenticatedUser user = new AuthenticatedUser(
				sysuser.getUserId(),
				sysuser.getName(),
				sysuser.getSimpleName(),
				sysuser.getLocale(),
				sysuser.getTimeZone(),
				(UserSpaceEntity)HibernateUtil.getSession().get(
						UserSpaceEntity.class, "ZCs7FA4BAFOrChMBS_e$"),
				sysuser.timeStamp());
		
		SecurityAssociation.setUser(user);
		
		try {
		 	RepositoryManager repoMan = RepositoryManagerFactory.getManager(user);
		 	RepositoryManagerFactory.setMimeMapper(mimes);
		 	Repository repo;
	        try {
	        	repo =  repoMan.getRepository("/system");
	        } catch (NotFoundException e) {	            
	        	repo = repoMan.createRepository("/system", user, null, null, null, null);
	            repoMan.addRepositoryMount("/system", true);
	            MutableRepositoryNode root = repo.getNodeByUri("/system", true);
	            root.grantPermission(
	            	new DefaultNodePermission(
	            		new Everyone(), DefaultNodePermission.READ, true));
	        }
		        
			File dir = new File(filePath);
			if (dir.isDirectory())
				deepRepoLoad(repo, user, dir, "/system", mimes);
			
			HibernateUtil.commitTransaction();
		} catch (Exception ex) {
			setErrorEx(ex);
		} finally {
			HibernateUtil.closeSession();
			SecurityAssociation.setUser(null);
		}
	}
	
	private  void deepRepoLoad(Repository repo, User user, File dir, String uri, MimeMapper mimes) 
			throws AccessDeniedException, NotFoundException, DuplicateException, LockException,
			FileNotFoundException, IOException {
		
		for (File ff : dir.listFiles()) {
			String dn = ff.getName();
			if (ff.isDirectory()) {
				
				// filter
				if (dn.startsWith(".") || dn.equals("CVS")) continue;
				
				// replace @ with .
				if (dn.startsWith("@")) dn = "." + dn.substring(1);
				
				String dUri = UriHelper.absoluteUri(uri, dn);
				MutableRepositoryNode dNode;
				try {
					dNode = RepositoryHelper.getNode(dUri, false);
				} catch (NotFoundException nfe) {
					dNode = repo.createNode(dUri, true, user);
				}
				deepRepoLoad(repo, user, ff, dNode.getUri(), mimes);
			} else {
		
				String fUri = UriHelper.absoluteUri(uri, dn);
				MutableRepositoryNode fNode;
				try {
					fNode = RepositoryHelper.getNode(fUri, true);
					
					/*
					 * Test if repo is newer
					 */
					String rDateStr = fNode.getPropertyValue(
							PropertyHelper.defaultQName("getlastmodified"));
					Date rDate = Check.isEmpty(rDateStr) ? null : PropertyHelper.parseDate(rDateStr);
					
					if (rDate != null && rDate.getTime() >= ff.lastModified()) {
						fNode = null; // no update
					}
					
				} catch (NotFoundException nfe) {
					fNode = repo.createNode(fUri, false, user);
				}
				if (fNode != null) {
					RepositoryHelper.createContent(
							fNode, new FileInputStream(ff), (int) ff.length(), mimes.getMimeType(dn));
				}
			}
		}

	}
	
	protected void setErrorEx(Exception ex) {
		if (ex instanceof HibernateException 
				|| ex instanceof SQLException) {
			configError = "Database problem: " + ex.getMessage();
		} else {
			configError = ex.getMessage();
		}
	}
	
	/**
	 * 
	 * @param component
	 * @return
	 */
	public Property getPropertyImpl(String component) {
		Session sess = HibernateUtil.getSession();
		boolean autoTxt = !sess.getTransaction().isActive();
		if (autoTxt) HibernateUtil.beginTransaction();
		Property prop = (Property)HibernateUtil.getSession().get(ConfigEntity.class, component);
		if (autoTxt) {
			HibernateUtil.commitTransaction();
			HibernateUtil.closeSession();
		}
		return prop;
	}

	@Override
	protected String getErrorImpl() {
		return configError;
	}

	@Override
	protected Property setPropertyImpl(Property cProp) {
		ConfigEntity conf = (cProp instanceof ConfigEntity) ?
				(ConfigEntity)cProp : new ConfigEntity(cProp);
		Session sess = HibernateUtil.getSession();
		boolean autoTxt = !sess.getTransaction().isActive();
		if (autoTxt) HibernateUtil.beginTransaction();
		sess.saveOrUpdate(conf);
		if (autoTxt) {
			HibernateUtil.commitTransaction();
			HibernateUtil.closeSession();
		}
		return conf;
	}
	
}
