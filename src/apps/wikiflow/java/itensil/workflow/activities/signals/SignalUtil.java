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
package itensil.workflow.activities.signals;

import itensil.security.User;
import itensil.util.Keys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This class provides a light-weight web-inteface level util for statusing
 * signals
 * 
 * 
 * @author ejones@itensil.com
 */
public class SignalUtil {

	// type mask
	// the license has been activated
	public static final long SIGNAL_INITIAL_MASK = 0x00000000;
	public static final long SIGNAL_ACTIVE_ALERT_MASK = 0x00000002;

	public static boolean hasAlert(HttpServletRequest hreq) {
		HttpSession session = hreq.getSession();
		Long signals = (Long) session.getAttribute("signals");
		if (signals != null) {
			if ((signals.longValue() & SIGNAL_ACTIVE_ALERT_MASK) == SIGNAL_ACTIVE_ALERT_MASK) {
				return true;
			}
		}
		return false;
	}
}
