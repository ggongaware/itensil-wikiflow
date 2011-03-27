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

import itensil.io.HibernateUtil;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.PropertyHelper;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryManager;
import itensil.repository.RepositoryManagerFactory;
import itensil.repository.RepositoryNode;
import itensil.security.AuthenticatedUser;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.hibernate.UserSpaceEntity;
import junit.framework.TestCase;

public class ConfigJunit extends TestCase {

	public void testConfigMan() throws Exception {
		new ConfigManagerImpl();
		ConfigManager.init();
		assertTrue(ConfigManager.isReady());
		assertEquals("1.18", ConfigManager.getProperty("schema").getVersion());
		assertEquals("1.1", ConfigManager.getProperty("tcard_schema").getVersion());
		assertEquals("1.4", ConfigManager.getProperty("repository").getVersion());
	}
	
	/**
	 * Assumes you're executing at project root
	 * @throws Exception
	 */
	public void testConfigSysRepo() throws Exception {
		ConfigManagerImpl manager = new ConfigManagerImpl();
		manager.initSystemRepo("src/apps/wikiflow/web/WEB-INF/sys-repo", new MimeMapper());
		
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
		
		RepositoryNode procNode = RepositoryHelper.getNode("/system/sysproc", false);
		
		assertTrue(procNode.isCollection());
		
		procNode = RepositoryHelper.getNode("/system/.init/process", false);
		
		assertTrue(procNode.isCollection());
		
		MutableRepositoryNode xNode = 
			RepositoryHelper.getNode("/system/sysproc/Blank/template/rules.xml", false);
		
		assertFalse(xNode.isCollection());
		
		assertEquals("text/xml", xNode.getPropertyValue(PropertyHelper.defaultQName("getcontenttype")));
		
		HibernateUtil.commitTransaction();
		HibernateUtil.closeSession();
	}
}
