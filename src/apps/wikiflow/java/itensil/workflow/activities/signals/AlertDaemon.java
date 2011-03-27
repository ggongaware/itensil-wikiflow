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

import itensil.security.*;
import itensil.io.HibernateUtil;
import itensil.mail.MailService;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityStepState;
import itensil.workflow.state.StepState;
import itensil.util.Check;
import itensil.util.UriHelper;
import org.hibernate.Query;
import org.hibernate.Session;
import org.apache.log4j.Logger;

import javax.mail.internet.InternetAddress;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.io.UnsupportedEncodingException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class AlertDaemon implements Runnable {

    Thread _thread;
    boolean keepRunning;
    MailService mailer;
    InternetAddress fromAddr;

    static Logger log = Logger.getLogger(AlertDaemon.class);

    // TODO branding
    public AlertDaemon(MailService mailer) {
        this.mailer = mailer;
        try {
            fromAddr = new InternetAddress(
            		mailer.getProperties().getProperty("alert.from.email", "alert@itensil.com"),
            		mailer.getProperties().getProperty("alert.from.name", "Alert"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
    }

    public void start() {
        keepRunning = true;
        _thread = new Thread(this);
        _thread.setPriority(Thread.MIN_PRIORITY);
        _thread.start();
    }

    @SuppressWarnings("unchecked")
	public void run() {
    	
    	log.info("Started");

        // super user thread
        SecurityAssociation.setUser(SysAdmin.getUser());

        while (keepRunning) {
        	try { 
	            // retrieve unmailed alerts ordered by user
	            HibernateUtil.beginTransaction();
	            Query qry = HibernateUtil.getSession().getNamedQuery("Signal.getAllUnMailedAlerts");
	            List<SignalImpl> alerts = qry.list();
	            AuthenticatedUser lastUser = null;
	            ArrayList<SignalImpl> usrAlts = new ArrayList<SignalImpl>();
	            for (SignalImpl alt : alerts) {
	            	
	            	if (Check.isEmpty(alt.getAssignId())) {
	            		 HibernateUtil.getSession().delete(alt);
	            		 continue;
	            	}
 	
	                // for each user
	                if (lastUser == null || !lastUser.getUserId().equals(alt.getAssignId())) {
	                    if (lastUser != null) {
	                        sendUserAlerts(lastUser, usrAlts);
	                        usrAlts.clear();
	                    }
	                    try {
	                        lastUser = UserSpaceAdmin.getAuthUser(alt.getAssignId());
	                    } catch (Exception ex) {
	                        log.error(ex);
	                        lastUser = null;
	                        continue;
	                    }
	                }
	                usrAlts.add(alt);
	            }
	            if (lastUser != null) sendUserAlerts(lastUser, usrAlts);
	            HibernateUtil.commitTransaction();
	            HibernateUtil.closeSession();
	
	            // wait some time
	            try {
	                Thread.sleep(5 * 60 * 1000);
	                //Thread.sleep(5000);
	            } catch (InterruptedException e) {
	                log.info(e);
	                keepRunning = false;
	            }
        	}
        	catch (Exception ex) {
        		log.error(ex); // probably DB offline, keep trying
        		try { Thread.sleep(100); } catch (InterruptedException e) {
	                log.info(e);
	                keepRunning = false;
	            }
        	}
        }
    }

    protected void sendUserAlerts(AuthenticatedUser user, ArrayList<SignalImpl> alerts) {

        StringBuffer msgHtml = new StringBuffer();
        StringBuffer msgTxt = new StringBuffer();
        SimpleDateFormat dFmt = new SimpleDateFormat("EEE MM/dd/yy");
        dFmt.setTimeZone(user.getTimeZone());
        try {
            Session session = HibernateUtil.getSession();
            InternetAddress toAddr = new InternetAddress(
            		Check.emptyIfNull(user.getName()), 
            		Check.emptyIfNull(user.getSimpleName()));
            
            boolean sendMail = !user.getFlags().contains("noeml");
            if (!sendMail) return;
            
            String baseUrl = "";


            msgHtml.append("<html>\n" +
                        "<body link='#000099' vlink='#000099' style='font:12px Arial,Helvetica,sans-serif'>\n");
            int aCount = 0;

            // format alerts
            msgHtml.append("<table rules='cols' border='1' cellpadding='4' cellspacing='0' bordercolor='#9BACCC' width='450'" +
                    " style='font: 12px Arial,Helvetica,sans-serif;border-collapse:collapse'>\n" +
                    "<tbody>\n" +
                    "<tr style='font-size:11px' bgcolor='#D0DCF6'><th width='170'>Activity</th>" +
                    "<th width='75'>Action</th><th width='75'>Due</th><th>From</th></tr>\n");
            
            HashSet<String> uspaceNames = new HashSet<String>();
            HashSet<String> uspaceBrands = new HashSet<String>();
            
            for (SignalImpl alt : alerts) {
                Activity act = alt.getActivity();
                // check if alert in same step, and to the same person
                // activity wasn't deleted
                Map<String, ActivityStepState> acSt = (act == null) ? null : act.getStates();
                ActivityStepState state = (acSt == null) ? null : acSt.get(alt.getStepId());
                if (act != null && sendMail && state != null && state.getAssignId() != null
                        && state.getAssignId().equals(alt.getAssignId())) {
                	
                	UserSpace uspace = UserSpaceAdmin.getUserSpace(act.getUserSpaceId());
                	if (uspace != null) {
                		baseUrl = Check.emptyIfNull(uspace.getBaseUrl());
                		uspaceNames.add(Check.emptyIfNull(uspace.getName()));
                		if (!Check.isEmpty(uspace.getBrand()))
                			uspaceBrands.add(uspace.getBrand());
                	}
                	
                    Date dueDate = act.getDueDate();
                    User from = UserSpaceAdmin.getAuthUser(act.getSubmitId());

                    // todo support per-step due dates
                    String dueStr = "Whenever";
                    if (dueDate != null) {
                        dueStr = dFmt.format(dueDate);
                    }
                    msgHtml.append("<tr valign='top'");
                    msgHtml.append((aCount % 2) == 0 ? ">" : " bgcolor='#eeeeee'>");
                    msgHtml.append(
                            String.format("<td><a href=\"%4$s?activity=%5$s\"><b>%1$s</b></a>\n" +
                                    "<div style='font-size:11px'>%6$s</div></td><td>%2$s</td><td>%3$s</td></td><td>%7$s</td>",
                                    act.getName(), alt.getStepId(), dueStr,
                                    UriHelper.absoluteUri(baseUrl,"act/page"), act.getId(), act.getDescription(),
                                    from != null ? from.getSimpleName() : ""));
                    msgHtml.append("</tr>\n");

                    msgTxt.append(String.format(
                            "- %1$s\t%2$s\t%3$s\t%5$s\n  %4$s\n",
                            act.getName(), alt.getStepId(), dueStr, act.getDescription(),
                            from != null ? from.getSimpleName() : ""));
                    msgTxt.append(String.format("  %1$s?activity=%2$s\n\n",
                            UriHelper.absoluteUri(baseUrl,"act/page"), act.getId()));

                    aCount++;
                }

                // mark sent
                alt.setMailed(SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
                session.update(alt);
            }

            // todo check for additional late activities

            msgHtml.append("</tbody>\n</table>\n");
            msgHtml.append(
                   String.format(
                   "<p style='font-size:11px'>This update contains new and past due items in your to-do list." +
                   "To view your complete to-do list, click on any activity above. " +
                   "If you do not wish to receive future email alerts like this, <a href=\"%1$s\">click here</a>.</p>\n",
                   UriHelper.absoluteUri(baseUrl,"uspace/settings")));
            msgHtml.append("</body>\n</html>");
            
            String unames = "Itensil";
            if (!uspaceNames.isEmpty()) {
            	StringBuffer usBuf = new StringBuffer();
            	for (String nm : uspaceNames) {
            		if (usBuf.length() > 0)  usBuf.append(" / ");
            		usBuf.append(nm);
            	}
            	unames = usBuf.toString();
            }

            msgTxt.append(String.format(
                   "This update contains new and past due items in your to-do list." +
                   "To view your complete to-do list, click on any activity above.\n" +
                   "If you do not wish to receive future email alerts like this, visit:\n%1$s\n",
                   UriHelper.absoluteUri(baseUrl,"uspace/settings")));

            msgHtml.insert(0, String.format("<b>%1$s To-do List Update</b><br>\n" +
            		"<i>Click on activity name to perform step</i><br><br>\n", unames));

            msgTxt.insert(0, String.format("%1$s To-do List Update\n\n", unames));

    
            // send email
            if (aCount > 0) {
            	InternetAddress altFrom = fromAddr;
                mailer.send(toAddr, altFrom, user.getUserId(),
                		String.format("%1$s to-do list update", unames), 
                		msgHtml.toString(), msgTxt.toString());
            }
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public boolean isAlive() {
        return keepRunning && _thread.isAlive();
    }

    public void die() {
        keepRunning = false;
        if (_thread != null) {
        	_thread.interrupt();
        	_thread = null;
        }
    }
}
