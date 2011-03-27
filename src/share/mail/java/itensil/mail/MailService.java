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
package itensil.mail;

import itensil.util.Check;
import itensil.util.RefLong;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.internet.ParseException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.io.UnsupportedEncodingException;

/**
 * Author: grant@gongaware.com
 */
public class MailService {

    private static final String CHECK_PAUSE = "20"; //20 seconds;
    private static final String SEND_BATCH_SIZE = "10";
    private static final String SEND_PAUSE = "1";

    private final LinkedList<QueuedMessage> mailQueue = new LinkedList<QueuedMessage>();

    private long checkPause;
    private int batchSize;
    private long batchPause;
    private long lastBatch;
    private long bounceCount;
    private long sendCount;
    private long startTime;

    private BounceHandler bouncer;
    private DeliveryHandler deliver;
    private MailClient mailer;
    private final LinkedList<MailFeed> feedQueue = new LinkedList<MailFeed>();
    private MessagingException lastError = null;
    private String inboxId;
    private Properties mailProps;
    private boolean checkIncomming;
    
    private final static HashMap<String,RefLong> inboxCheckTimers = new HashMap<String,RefLong>();


    /**
     * @param bouncer
     * @param mailProps
     */
    public MailService(
        BounceHandler bouncer, DeliveryHandler deliver, Properties mailProps) {

        //keepRunning = true;
        bounceCount = 0;
        this.bouncer = bouncer;
        this.deliver = deliver;
        checkPause = Long.parseLong(
            mailProps.getProperty("mail.check.pause", CHECK_PAUSE)) * 1000;
        batchSize = Integer.parseInt(
            mailProps.getProperty("mail.send.batch", SEND_BATCH_SIZE));
        batchPause = Long.parseLong(
            mailProps.getProperty("mail.send.pause", SEND_PAUSE)) * 1000;

        // create inboxId (thumbprint)
        String inProto = mailProps.getProperty("mail.store.protocol");
        checkIncomming = !Check.isEmpty(inProto);
        
        String host = mailProps.getProperty("mail." + inProto + ".host", "");
        inboxId = inProto + "_" + host.toLowerCase() + "_" +
            mailProps.getProperty("mail.user");

        mailer = new MailClient(mailProps);
        mailer.setDeliveryHandler(deliver);

        this.mailProps = mailProps;

        if (bouncer != null) bouncer.setService(this);
        if (deliver != null) deliver.setService(this);

        startTime = System.currentTimeMillis();
        lastBatch = 0;

    }

    /**
     * @see     Thread#run()
     */
    public void run() {

        //lastCheckTime = System.currentTimeMillis();
        //while (keepRunning || !mailQueue.isEmpty()) {
        long runTime = System.currentTimeMillis();

        if ((runTime - lastBatch) >= batchPause) {

            lastBatch = runTime;
            if (!mailQueue.isEmpty()) {

                int sent = 0;
                QueuedMessage msg = null;
                try {
                    mailer.sendConnect();
                    while (sent < batchSize && !mailQueue.isEmpty()) {
                        synchronized (mailQueue) {
                            msg = mailQueue.removeFirst();
                        }

                        try {
                            mailer.send(
                                    msg.toArray == null ? new InternetAddress[]{msg.to} : msg.toArray,
                                    msg.from,
                                    msg.bounceId,
                                    msg.subject,
                                    msg.htmlMessage,
                                    msg.textMessage,
                                    msg.attachment
                                );
                            msg = null;
                        } catch (SendFailedException sfe) {
                            System.err.println(sfe.getMessage());
                            bounceCount++;
                            if (bouncer != null) {
                                bouncer.handle(msg.bounceId);
                            }
                        } catch (ParseException pe) {
                            System.err.println(pe.getMessage());
                            bounceCount++;
                            if (bouncer != null) {
                                bouncer.handle(msg.bounceId);
                            }
                        }
                        sent++;
                        sendCount++;
                    }
                    mailer.sendClose();
                } catch (MessagingException me) {
                    try {
                        mailer.sendClose();
                    } catch (MessagingException me2) {
                        me2.printStackTrace();
                    }

                    // transport failed requeue at end just in case
                    // this first one is stuck
                    lastError = me;
                    System.err.println(me.getMessage());
                    if (msg != null) {
                        synchronized (mailQueue) {
                            mailQueue.add(msg);
                        }
                    }
                }
            } else {

                // check feeds
                if (!feedQueue.isEmpty()) {
                    MailFeed feed;
                    synchronized (feedQueue) {
                        feed = feedQueue.removeFirst();
                    }
                    if (feed.eatMails(this, batchSize)) {
                        synchronized (feedQueue) {
                            feedQueue.addLast(feed);
                        }
                    }
                }
            }
        }

        // Inbox
        if (checkIncomming) {
	        RefLong lastCheckTime = getLastCheckTime(inboxId);
	        if ((runTime - lastCheckTime.value) >= checkPause) {
	            lastCheckTime.value = runTime;
	            try {
	                Collection<String> bounces = mailer.checkIncomming();
	                for (String bounceId : bounces) {
	                    bounceCount++;
	                    if (bouncer != null) {
	                        bouncer.handle(bounceId);
	                    }
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
        }
    }

    public void send(
                InternetAddress to,
                InternetAddress from,
                String bounceId,
                String subject,
                String htmlMessage,
                String textMessage) {

        QueuedMessage msg = new QueuedMessage();
        msg.to = to;
        msg.from = from;
        msg.bounceId = bounceId;
        msg.subject = subject;
        msg.htmlMessage = htmlMessage;
        msg.textMessage = textMessage;
        synchronized (mailQueue) {
            mailQueue.addLast(msg);
        }
    }
    
    public void send(
            InternetAddress to[],
            InternetAddress from,
            String bounceId,
            String subject,
            String htmlMessage,
            String textMessage,
            BodyPart attachment) {

    QueuedMessage msg = new QueuedMessage();
    msg.toArray = to;
    msg.from = from;
    msg.bounceId = bounceId;
    msg.subject = subject;
    msg.htmlMessage = htmlMessage;
    msg.textMessage = textMessage;
    msg.attachment = attachment;
    synchronized (mailQueue) {
        mailQueue.addLast(msg);
    }
}

    public void addFeed(MailFeed feed) {
        synchronized (feedQueue) {
            feedQueue.addLast(feed);
        }
    }

    public List<MailFeed> getFeedsByClass(Class cls) {

        ArrayList<MailFeed> feeds = new ArrayList<MailFeed>();
        synchronized (feedQueue) {
            for (MailFeed o : feedQueue) {
                if (cls.isInstance(o)) {
                    feeds.add(o);
                }
            }
        }
        return feeds;
    }

    public int getQueueSize() {
        return mailQueue.size();
    }

    public MessagingException getLastError() {
        return lastError;
    }

    public void clearQueue() {
        mailQueue.clear();
    }

    public long getSendCount() {
        return sendCount;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastCheckTime() {
        return getLastCheckTime(inboxId).value;
    }

    public long getBounceCount() {
        return bounceCount;
    }

    public MimeMessage createMessage() {
        return mailer.createMessage();
    }

    public Properties getProperties() {
        return mailProps;
    }
    
    

    private static class QueuedMessage {
        InternetAddress to;
        InternetAddress toArray[];
        InternetAddress from;
        String bounceId;
        String subject;
        String htmlMessage;
        String textMessage;
        BodyPart attachment;
    }

    public static InternetAddress address(String email, String name) {

        try {
            return new InternetAddress(email, name);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static RefLong getLastCheckTime(String inboxId) {

        RefLong cTime = inboxCheckTimers.get(inboxId);
        if (cTime == null) {
            synchronized(inboxCheckTimers) {
                cTime = inboxCheckTimers.get(inboxId);
                if (cTime == null) {
                    cTime = new RefLong(0);
                    inboxCheckTimers.put(inboxId, cTime);
                }
            }
        }
        return cTime;
    }

	public BounceHandler getBouncer() {
		return bouncer;
	}

	public DeliveryHandler getDeliver() {
		return deliver;
	}
}
