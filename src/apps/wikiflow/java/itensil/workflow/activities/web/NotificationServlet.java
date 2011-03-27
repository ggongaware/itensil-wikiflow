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

package itensil.workflow.activities.web;

import itensil.io.HibernateUtil;
import itensil.repository.NotFoundException;
import itensil.security.User;
import itensil.util.Keys;
import itensil.web.MethodServlet;
import itensil.workflow.activities.signals.AlertSignalImpl;
import itensil.workflow.activities.signals.SignalImpl;
import itensil.workflow.activities.signals.SignalHelper;
import itensil.workflow.activities.signals.SignalManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This servlet manages requests for notification (Signals, Alerts)
 * 
 * 
 * @author ejones@itensil.com
 */
public class NotificationServlet extends MethodServlet {

	private final HashMap<String, String> SUPPORTED_FEED_TYPE_MAPPING = instantiateSupportedFeedTypes();
	private static HashMap<String,String> instantiateSupportedFeedTypes(){ 
				HashMap<String, String> hm= new HashMap<String, String>(); 
				hm.put("rss_0.9", "rss_0.9"); 
				hm.put("rss_0.91", "rss_0.91"); 
				hm.put("rss_0.92", "rss_0.92"); 
				hm.put("rss_0.93", "rss_0.93"); 
				hm.put("rss_0.94", "rss_0.94"); 
				hm.put("rss_1.0", "rss_1.0"); 
				hm.put("rss_2.0", "rss_2.0"); 
				hm.put("atom_0.3", "atom_0.3");
				hm.put("atom_0.3", "atom_0.3");
				hm.put("_defaultFeedType", "rss_2.0"); 
				return hm; 
				} 
			
	private static final String FEED_TYPE = "type";
	private static final String MIME_TYPE = "application/xml; charset=UTF-8";
	private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

	private static final DateFormat DATE_PARSER = new SimpleDateFormat(
			"yyyy-MM-dd");

	public void webYourRSS(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		try {
			HttpServletRequest hreq = (HttpServletRequest) request;
			HttpServletResponse hres = (HttpServletResponse) response;

			HttpSession session = hreq.getSession(false);

			if (session == null
					|| session.getAttribute(Keys.SIGNED_ON_USER) == Boolean.FALSE) {
				return;
			}
			HibernateUtil.beginTransaction();
			
			User user = (User) hreq.getUserPrincipal();

			SyndFeed feed = getFeed(user);
			
			HibernateUtil.commitTransaction();

			String feedType = request.getParameter(FEED_TYPE);
			feedType = getValidFeedType(feedType);
			feed.setFeedType(feedType);

			response.setContentType(MIME_TYPE);
			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed, response.getWriter());
		} catch (FeedException ex) {
			String msg = COULD_NOT_GENERATE_FEED_ERROR;
			log(msg, ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					msg);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected SyndFeed getFeed(User user) throws IOException, FeedException {

		SignalManager signalManager = SignalManager.getInstance();
		try {
			List<SignalImpl> alerts = signalManager.getAlerts(user);
			SyndFeed feed = SignalHelper.buildSyndFeed(user, alerts);
			return feed;
		} catch (NotFoundException nfe) {
			logger.info("User or user ID is null on call to SignalManager.getAlerts");
			throw new IOException(nfe.getMessage());
		}
	}
	
	private String getValidFeedType(String feedType) {
		if (SUPPORTED_FEED_TYPE_MAPPING.containsKey(feedType)) {
			return SUPPORTED_FEED_TYPE_MAPPING.get(feedType);
		}
		else {
			return SUPPORTED_FEED_TYPE_MAPPING.get("_defaultFeedType");
		}
	}
	
    /**
     * Called after an InvocationTargetException
     */
    public void methodException(Throwable t) {
        HibernateUtil.rollbackTransaction();
    }

    /**
     * Clean-up
     */
    public void afterMethod() {
        HibernateUtil.closeSession();
    }

}

