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

import javax.mail.Message;
import javax.sql.DataSource;

/**
 * Author: grant@gongaware.com
 * 
 */
public interface DeliveryHandler {

    public boolean handle(Message msg);

    public void setService(MailService service);

    public void setDataSource(DataSource ds);

}
