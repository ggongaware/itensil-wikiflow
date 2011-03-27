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
package itensil.config.web;

import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.PropertyConfigurator;

import itensil.config.ConfigManager;
import itensil.config.ConfigManagerImpl;
import itensil.config.MimeMapper;
import itensil.config.Property;
import itensil.repository.RepositoryManager;
import itensil.repository.RepositoryManagerFactory;
import itensil.web.MethodServlet;

public class ConfigServlet extends MethodServlet {

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		URL url = ConfigServlet.class.getResource("/itensil_log4j.properties");
		if (url != null) {
			PropertyConfigurator.configureAndWatch(url.getPath());
		}
		
		ConfigManagerImpl manager = new ConfigManagerImpl();
		ConfigManager.init();
		if (ConfigManager.isReady()) {
			Property prop = ConfigManager.getProperty("sys_repo");
			if (prop == null || !"skip".equals(prop.getProperties())) {
				log("Synching system repository...");
				manager.initSystemRepo(
					config.getServletContext().getRealPath("/WEB-INF/sys-repo"),
					new MimeMapper(config.getServletContext()));
			} else {
			 	RepositoryManagerFactory.setMimeMapper(new MimeMapper(config.getServletContext()));
				log("Skipping system repository synch.");
			}
		}
	}

}
