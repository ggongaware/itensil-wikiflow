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

import itensil.io.HibernateUtil;
import itensil.security.AuthenticatedUser;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceAdmin;
import itensil.security.hibernate.UserEntity;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityCurrentPlan;
import itensil.workflow.activities.state.ActivityStepState;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.hibernate.Session;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;

/**
 * This type provides a centralized utility for Signal manipulation and transformations  
 *
 *
 * @author ejones@itensil.com
 */
public class SignalHelper {
	private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd");
    public static final String ALERT_HTML_BODY_START = "<html>\n" + "<body link='#000099' vlink='#000099' style='font:12px Arial,Helvetica,sans-serif'>\n";

    // format alerts
    public static final String ALERT_HTML_TABLE_HEADER = "<table rules='cols' border='1' cellpadding='4' cellspacing='0' bordercolor='#9BACCC' width='450'" 
    	+ " style='font: 12px Arial,Helvetica,sans-serif;border-collapse:collapse'>\n" 
    	+ "<tbody>\n" + "<tr style='font-size:11px' bgcolor='#D0DCF6'><th width='170'>Activity</th>" 
    	+ "<th width='75'>Action</th><th width='75'>Due</th><th>From</th></tr>\n";

    public static final String ALERT_DUE_DATE_ALTERNATE = "";
    public static final String ACTIVITY_DESCRIPTION_ALTERNATE = "";
    public static final String ACTIVITY_NAME_ALTERNATE = "Untitled Activity Name";

	@SuppressWarnings("unchecked")

	public static SyndFeed buildSyndFeed(User user, List<SignalImpl> alerts) throws IOException, FeedException {
		SyndFeed feed = new SyndFeedImpl();

		feed.setTitle("Alerts");
		//feed.setLink("http://itensil.net");
		feed.setDescription("Alert(s) for User: " + (user.getSimpleName()==null ? user.getName() : user.getSimpleName()) );

		List entries = new ArrayList();
		SyndEntry entry;
		SyndContent description;

        StringBuffer msgHtml = new StringBuffer();
        SimpleDateFormat dFmt = new SimpleDateFormat("EEE MM/dd/yy");
        dFmt.setTimeZone(user.getTimeZone());
        try {
            Session session = HibernateUtil.getSession();
           /* InternetAddress toAddr = new InternetAddress(
            		Check.emptyIfNull(user.getName()), 
            		Check.emptyIfNull(user.getSimpleName())); */
            String baseUrl = "";

           // int aCount = 0;

            HashSet<String> uspaceNames = new HashSet<String>();
            HashSet<String> uspaceBrands = new HashSet<String>();
            HashSet<String> uspaceBaseUrls = new HashSet<String>();
            
            for (SignalImpl alt : alerts) {
                msgHtml.setLength(0);
                Activity act = alt.getActivity();

                msgHtml.append(SignalHelper.ALERT_HTML_TABLE_HEADER);

                // check if alert in same step, and to the same person
                // activity wasn't deleted
                Map<String, ActivityStepState> acSt = (act == null) ? null : act.getStates();
                ActivityStepState state = (acSt == null) ? null : acSt.get(alt.getStepId());
                if (act != null && state != null && state.getAssignId() != null
                        && state.getAssignId().equals(alt.getAssignId())) {
                	
                	UserSpace uspace = UserSpaceAdmin.getUserSpace(act.getUserSpaceId());
                	if (uspace != null) {
                		baseUrl = Check.emptyIfNull(uspace.getBaseUrl());
                		uspaceBaseUrls.add(baseUrl);
                		uspaceNames.add(Check.emptyIfNull(uspace.getName()));
                		if (!Check.isEmpty(uspace.getBrand()))
                			uspaceBrands.add(uspace.getBrand());
                	}
                	Date dueDate = act.getDueDate();
                	ActivityCurrentPlan acp = state.getCurrentPlan();
                	
                	// support per-step due dates
                	if (acp != null && acp.getDueDate() != null) {
                		dueDate = acp.getDueDate();
                	}

                    
                    String dueStr = (dueDate == null) ?  SignalHelper.ALERT_DUE_DATE_ALTERNATE : dFmt.format(dueDate) ;

                    String alertURI = String.format("%1$s?activity=%2$s&aid=%3$s", 
                    		// 1
                    		UriHelper.absoluteUri(baseUrl,"act/page"), 
                    		//2
                    		act.getId(),
                    		// 3
                    		alt.getId());
                    
                    msgHtml.append("<tr valign='top'>");

                    UserEntity fromUser = (UserEntity)HibernateUtil.getSession().get(UserEntity.class, act.getSubmitId());
                    String fromName = Check.isEmpty(fromUser) ?  
                    					"" : 
                    					Check.isEmpty(fromUser.getSimpleName()) ? fromUser.getName() : fromUser.getSimpleName();

                    String activityName = Check.isEmpty(act.getName()) ? ACTIVITY_NAME_ALTERNATE : act.getName();
					
                    msgHtml.append(
                    		// 
                            String.format("<td><a href=\"%4$s\"><b>%1$s</b></a>\n" +
                                    "<div style='font-size:11px'>%6$s</div></td><td>%2$s</td><td>%3$s</td><td>%7$s</td>",
                                    // 1
                                    activityName,
                                    // 2
                                    alt.getStepId(),
                                    // 3
                                    dueStr,
                                    // 4
                                    alertURI, 
                                    // 5
                                    act.getId(), 
                                    // 6
                                    Check.isEmpty(act.getDescription()) ? ACTIVITY_DESCRIPTION_ALTERNATE : act.getDescription(), 
                                    // 7
                                    fromName)
                    				);

                    msgHtml.append("</tr>\n");
                    msgHtml.append("</tbody>\n</table>\n");

                    

	                // mark mail as sent
	                alt.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
	                session.update(alt);
	
	                entry = new SyndEntryImpl();
	
					entry.setTitle(activityName);
	
					// call back URL goes into workzone screen
					entry.setLink(alertURI);
					entry.setPublishedDate(alt.getTimeStamp());
					description = new SyndContentImpl();
					description.setType("text/html");
					description.setValue(msgHtml.toString()); 
					entry.setDescription(description);
					entries.add(entry);
	            } else {
	            	// TODO support alternate states for invalid alerts
	            	alt.setRead(true);
	            	session.saveOrUpdate(alt);
	            }
            
            }

            // TODO check for additional late activities
            
            if (!uspaceBaseUrls.isEmpty()) {
            	feed.setLink(uspaceBaseUrls.iterator().next());
            } else {
            	feed.setLink("http://itensil.net");
            }
            

            feed.setEntries(entries);

        } catch (Exception ex) {
        	 ex.getMessage();
        }

		return feed;
	}

}
                	