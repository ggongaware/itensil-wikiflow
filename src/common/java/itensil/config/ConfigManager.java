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


public abstract class ConfigManager {

	protected static ConfigManager manager;
	
	public static boolean isReady() {
		if (manager == null) return false;
		return getManager().isReadyImpl();
	}
	
	protected abstract boolean isReadyImpl();
	
	protected abstract void initImpl();
	
	protected abstract String getErrorImpl();
	
	protected abstract Property getPropertyImpl(String component);
	
	protected abstract Property setPropertyImpl(Property cProp);
	
	protected static ConfigManager getManager() {
		if (manager == null) {
			throw new ConfigManagerException();
		}
		return manager;
	}
	
	public static void init() {
		getManager().initImpl();
	}
	
	public static String getError() {
		if (manager == null) return "Configuration manager missing";
		return getManager().getErrorImpl();
	}
	
	/**
	 * 
	 * @param component
	 * @return
	 */
	public static Property getProperty(String component) {
		return getManager().getPropertyImpl(component);
	}
	
	/**
	 * 
	 * @param component
	 * @return
	 */
	public static Property setProperty(Property cProp) {
		return getManager().setPropertyImpl(cProp);
	}
	
}
