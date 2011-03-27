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
package itensil.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import itensil.security.User;
import itensil.security.UserSpace;
import itensil.config.MimeMapper;
import itensil.repository.event.ContentChangeListener;
import itensil.repository.event.ContentEvent;
import itensil.repository.filter.NodeFilter;
import itensil.repository.hibernate.EntityManager;

/**
 * @author ggongaware@itensil.com
 */
public class RepositoryManagerFactory {

    public static RepositoryManager getManager(User user) {
        // hibernate only for now
        return new EntityManager(user.getUserSpaceId(), factory);
    }
    
    public static RepositoryManager getManager(UserSpace uspace) {
        // hibernate only for now
        return new EntityManager(uspace.getUserSpaceId(), factory);
    }
    
    
    /**
     * 
     * @param eventFilter
     * @param listener
     */
    public static synchronized void addContentChangeListener(
    		NodeFilter eventFilter, ContentChangeListener listener) {
    	
    	ArrayList<ContentChangeListener> list = factory.cListeners.get(eventFilter);
    	if (list == null) {
    		list = new ArrayList<ContentChangeListener>();
    		factory.cListeners.put(eventFilter, list);
    	}
    	list.add(listener);
    }

    /**
     * 
     * @param evt
     */
	public void fireContentChangeEvent(ContentEvent evt) {
		for (Entry<NodeFilter, ArrayList<ContentChangeListener>> ent : factory.cListeners.entrySet()) {
			if (ent.getKey().accept(evt.getNode())) {
				for (ContentChangeListener ccLs : ent.getValue()) {
					ccLs.contentChanged(evt);
				}
			}
		}
	}
	
	private static RepositoryManagerFactory factory = new RepositoryManagerFactory();
	private static MimeMapper mimes = new MimeMapper();
	
	HashMap<NodeFilter, ArrayList<ContentChangeListener>> cListeners;
	
	private RepositoryManagerFactory() {
		cListeners = new HashMap<NodeFilter, ArrayList<ContentChangeListener>>();
	}

	public static void setMimeMapper(MimeMapper mimes) {
		RepositoryManagerFactory.mimes = mimes;
	}
	
	public static String getMimeType(String uri) {
		return mimes.getMimeType(uri);
	}

}

