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
package itensil.workflow.model.element;

import itensil.util.Check;
import itensil.workflow.model.BasicElement;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.FlowModel;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Wait extends BasicElement {

    public final static String NAME = "wait";
    public final static String [] ATTRIBUTES = {"days", "hours", "minutes", "rev"};

    public Wait(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Wait.NAME;
    }

    public String [] getAttributeNames() {
        return Wait.ATTRIBUTES;
    }
    
    public int getDays() {
    	int num = 0;
    	String str = getAttribute("days");
    	if (!Check.isEmpty(str)) {
    		try {
    			num = Integer.parseInt(str);
    			if (num < 0) num = 0;
    		} catch (NumberFormatException nfe) {}
    	}
    	return num;
    }
    
    public int getHours() {
    	int num = 0;
    	String str = getAttribute("hours");
    	if (!Check.isEmpty(str)) {
    		try {
    			num = Integer.parseInt(str);
    			if (num < 0) num = 0;
    		} catch (NumberFormatException nfe) {}
    	}
    	return num;
    }
    
    public int getMinutes() {
    	int num = 0;
    	String str = getAttribute("minutes");
    	if (!Check.isEmpty(str)) {
    		try {
    			num = Integer.parseInt(str);
    			if (num < 0) num = 0;
    		} catch (NumberFormatException nfe) {}
    	}
    	return num;
    }

}
