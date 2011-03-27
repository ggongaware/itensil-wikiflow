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

import itensil.workflow.activities.state.Activity;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ejones@itensil.com
 *
 */
public class SignalImpl implements Signal {

	protected String id;
	protected Activity activity;
	protected String assignId;
	protected String stepId;
	protected String role;
	protected String note;
	protected Date timeStamp;
	protected int mailed;
	protected boolean read;
	protected SignalType signalType;
	protected String signalSubmitId;
	protected String signalAssignedId;
	protected String signalLink;
	protected String signalMessage;
	protected String custom1;
	protected String custom2;
	protected String custom3;
	protected String custom4;

	SignalImpl() {
	}

	SignalImpl(SignalImpl impl) {
		updateFrom(impl);
	}

	public boolean storeEqual(SignalImpl impl) {
		return ( this.id != null && this.id.equals(impl.id) && 	this.signalType != null && this.signalType.equals(impl.signalType) && 

				(
					this.activity != null && this.activity.equals(impl.activity) &&
					this.assignId != null && this.assignId.equals(impl.assignId)
				)
				|| 
				(
					this.signalSubmitId != null && this.signalSubmitId.equals(impl.signalSubmitId) && 
					this.signalAssignedId != null && this.signalAssignedId.equals(impl.signalAssignedId)
				)		
				);
	}
	
	protected void updateFrom(SignalImpl impl) {
		this.id = impl.id;
		this.activity = impl.activity;
		this.assignId = impl.assignId;
		this.stepId = impl.stepId;
		this.role = impl.role;
		this.note = impl.note;
		this.timeStamp = impl.timeStamp;
		this.mailed = impl.mailed;
		this.read = impl.read;
		this.signalType = impl.signalType;
		this.signalSubmitId = impl.signalSubmitId;
		this.signalAssignedId = impl.signalAssignedId;
		this.signalLink = impl.signalLink;
		this.signalMessage = impl.signalMessage;
		this.custom1 = impl.custom1;
		this.custom2 = impl.custom2;
		this.custom3 = impl.custom3;
		this.custom4 = impl.custom4;
	}
	

	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public String getAssignId() {
		return assignId;
	}

	public void setAssignId(String assignId) {
		this.assignId = assignId;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public int getMailed() {
		return mailed;
	}

	public void setMailed(int mailed) {
		this.mailed = mailed;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public SignalType getSignalType() {
		return signalType;
	}

	protected void setSignalType(SignalType signalType) {
		this.signalType = signalType;
	}

	public String getSignalSubmitId() {
		return signalSubmitId;
	}

	public void setSignalSubmitId(String signalSubmitId) {
		this.signalSubmitId = signalSubmitId;
	}

	public String getSignalAssignedId() {
		return signalAssignedId;
	}

	public void setSignalAssignedId(String signalAssignedId) {
		this.signalAssignedId = signalAssignedId;
	}

	public String getSignalLink() {
		return signalLink;
	}

	public void setSignalLink(String signalLink) {
		this.signalLink = signalLink;
	}

	public String getSignalMessage() {
		return signalMessage;
	}

	public void setSignalMessage(String signalMessage) {
		this.signalMessage = signalMessage;
	}

	public String getCustom1() {
		return custom1;
	}

	public void setCustom1(String custom1) {
		this.custom1 = custom1;
	}

	public String getCustom2() {
		return custom2;
	}

	public void setCustom2(String custom2) {
		this.custom2 = custom2;
	}

	public String getCustom3() {
		return custom3;
	}

	public void setCustom3(String custom3) {
		this.custom3 = custom3;
	}

	public String getCustom4() {
		return custom4;
	}

	public void setCustom4(String custom4) {
		this.custom4 = custom4;
	}

}
