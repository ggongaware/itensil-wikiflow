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

import itensil.workflow.activities.signals.SignalType;
import itensil.workflow.activities.state.Activity;

import java.io.Serializable;
import java.util.Date;

/**
 * The main goal for this class is to provide an extendable interface for types of Signal (ex. , and other) 
 *
 *
 * @author ejones@itensil.com
 */
public interface Signal extends Serializable {

	public boolean storeEqual(SignalImpl signal);
	
    public String getId();

    public Activity getActivity();
    public void setActivity(Activity activity);

    public String getAssignId();
    public void setAssignId(String assignId);

    public String getStepId();
    public void setStepId(String stepId);

    public String getRole();
    public void setRole(String role);

    public String getNote();
    public void setNote(String note);

    public int getMailed();
    public void setMailed(int mailed);

    public boolean isRead();
    public void setRead(boolean read);
    
    public Date getTimeStamp();
    public void setTimeStamp(Date timeStamp);

    public SignalType getSignalType();

	public String getSignalSubmitId ();
	public void setSignalSubmitId (String signalSubmitId );

	public String getSignalAssignedId();
	public void setSignalAssignedId(String signalAssignedId);

	public String getSignalLink();
	public void setSignalLink(String signalLink);

	public String getSignalMessage();
	public void setSignalMessage(String signalMessage);

	public String getCustom1();
	public void setCustom1(String custom1);

	public String getCustom2();
	public void setCustom2(String custom2);

	public String getCustom3();
	public void setCustom3(String custom3);

	public String getCustom4();
	public void setCustom4(String custom4);
}
