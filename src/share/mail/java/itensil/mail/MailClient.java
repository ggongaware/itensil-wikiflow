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

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * Author: grant@gongaware.com
 */
public class MailClient {

    public static String BOUNCE_ID_HEADER = "X-Application-bounce-id";

    private Session session;
    private String userName;
    private String password;
    private String bounceAddress;
    private Transport tr;
    private DeliveryHandler deliver;
    private boolean leaveNonbounce;

    public MailClient(Properties mailProps) {
        userName = mailProps.getProperty("mail.user");
        password = mailProps.getProperty("mail.password");
        bounceAddress = mailProps.getProperty("mail.smtp.from");
        leaveNonbounce =
                Boolean.valueOf(mailProps.getProperty(
                        "mail.leave-non-bounces", "false"));
        session = Session.getInstance(mailProps, null);
        deliver = null;
    }

    public void setDeliveryHandler(DeliveryHandler deliver) {
        this.deliver = deliver;
    }

    public void sendConnect() throws MessagingException  {

        tr = session.getTransport();
        tr.connect(tr.getURLName().getHost(), userName, password);
    }



    public void send(Message message) throws MessagingException {

        boolean autoClose = false;
        if (tr == null) {
            sendConnect();
            autoClose = true;
        }
        tr.sendMessage(message, message.getAllRecipients());
        if (autoClose) {
            sendClose();
        }
    }

    public MimeMessage createMessage() {
        return new MimeMessage(session);
    }

    public void send(
                InternetAddress toArray[],
                InternetAddress from,
                String bounceId,
                String subject,
                String htmlMessage,
                String textMessage,
                BodyPart attachment) throws MessagingException {

        MimeMessage message = new MimeMessage(session);
        for (InternetAddress to : toArray) {
        	message.addRecipient(Message.RecipientType.TO, to);
        }
        message.setFrom(from);
        message.addHeader(BOUNCE_ID_HEADER, bounceId);
        message.setSubject(subject);
        if (htmlMessage != null || attachment != null) {
        	BodyPart htmlBodyPart = null;
        	if (htmlMessage != null) {
        		htmlBodyPart = new MimeBodyPart();
        		htmlBodyPart.setContent(htmlMessage, "text/html");
        	}
            BodyPart txtBodyPart = new MimeBodyPart();
            if (textMessage != null) {
                txtBodyPart.setText(textMessage);
            } else {
                txtBodyPart.setText("This is best viewed as HTML.");
            }
            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(txtBodyPart);
            if (htmlBodyPart != null) multipart.addBodyPart(htmlBodyPart);
            if (attachment != null) {
            	MimeMultipart mpRoot = new MimeMultipart("mixed");
            	MimeBodyPart contentBody = new MimeBodyPart();
            	contentBody.setContent(multipart);
            	mpRoot.addBodyPart(contentBody);
            	mpRoot.addBodyPart(attachment);
            	message.setContent(mpRoot);
            } else {
            	//multipart.addBodyPart(attachment);
            	message.setContent(multipart);
            }
        } else {
            message.setText(textMessage);
        }
        send(message);
    }

    public void sendClose() throws MessagingException  {
        tr.close();
        tr = null;
    }

    public Collection<String> checkIncomming() throws MessagingException, IOException  {

        Store store = session.getStore();
        store.connect(store.getURLName().getHost(), userName, password);
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);
        Message msgs[] =  folder.getMessages();
        ArrayList<String> bounceIds = new ArrayList<String>();
        for (Message msg : msgs) {
            // check initial headers
            String hdrs[] = msg.getHeader(BOUNCE_ID_HEADER);
            boolean isBounce = false;
            boolean doDelete = true;
            if (hdrs == null || hdrs.length == 0) {
                Address toAddrs[] = msg.getRecipients(Message.RecipientType.TO);
                boolean doLineScan = false;
                if (toAddrs != null) {
                    for (Address toAddr : toAddrs) {
                        InternetAddress iadd = (InternetAddress) toAddr;
                        if (bounceAddress.equalsIgnoreCase(iadd.getAddress())) {
                            doLineScan = true;
                            break;
                        }
                    }
                }
                if (doLineScan) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(msg.getInputStream()));
                    String line;
                    lines :
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(BOUNCE_ID_HEADER)) {
                            int pos = BOUNCE_ID_HEADER.length();
                            while (!Character.isLetterOrDigit(line.charAt(pos))) {
                                pos++;
                                if (pos >= line.length()) break lines;
                            }
                            bounceIds.add(line.substring(pos).trim());
                            isBounce = true;
                            break;
                        }
                    }
                    reader.close();
                }
            } else {
                isBounce = true;
                bounceIds.add(hdrs[0]);
            }
            if (!isBounce) {
                doDelete = !leaveNonbounce;
                if (deliver != null) {
                    doDelete = deliver.handle(msg) && doDelete;
                } else if (!leaveNonbounce) {
                    System.err.println("<MAIL> Non-bounce:");
                    msg.writeTo(System.err);
                }
            }
            if (doDelete) {
                msg.setFlag(Flags.Flag.DELETED, true);
            }
        }
        folder.close(true);
        return bounceIds;
    }

}
