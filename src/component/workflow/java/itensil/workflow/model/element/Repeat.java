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

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import itensil.util.Check;
import itensil.workflow.model.BasicElement;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.ContainerElement;
import itensil.workflow.model.element.Until.ALPHA;
import itensil.workflow.model.element.Until.ON;
import itensil.workflow.model.element.Until.ORD;
import itensil.workflow.model.element.Until.TYPE;
import itensil.workflow.model.element.Until.UNIT;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Repeat extends BasicElement {

    public final static String NAME = "repeat";
    public final static String [] ATTRIBUTES = {
    		"start", "end", "type", "ord", "alpha", "number", "unit", "on", "at", "every"};

    public final static Pattern timeRx = Pattern.compile("^0?(\\d+):0?(\\d+)(?::(\\d+(?:\\.\\d+)?))?");
    
    public enum TYPE { daily, weekly, monthly, condition };
    public enum ALPHA { first, second, third, fourth, last, next, before, after };
    public enum ORD { alpha, number, field };
    public enum UNIT { day, wday, current, previous, next };
    public enum ON { 
    	
    	day(-1),
    	wday(-1),
    	sun(Calendar.SUNDAY),
    	mon(Calendar.MONDAY),
    	tue(Calendar.TUESDAY),
    	wed(Calendar.WEDNESDAY),
    	thu(Calendar.THURSDAY),
    	fri(Calendar.FRIDAY),
    	sat(Calendar.SATURDAY);
    	
    	ON(int cal) { this.cal = cal; }
    	
    	public final int cal;
    };
    
    public Repeat(FlowModel owner, ContainerElement parent) {
        super(owner, parent);
    }

    public String getElementName() {
        return Repeat.NAME;
    }

    public String [] getAttributeNames() {
        return Repeat.ATTRIBUTES;
    }
    
    public TYPE getType() {
    	TYPE en = TYPE.daily;
    	String str = getAttribute("type");
    	if (!Check.isEmpty(str)) {
    		try {
    			en = TYPE.valueOf(str);
    		} catch (IllegalArgumentException iae) { }
    	}
    	return en;
    }
    
    public ALPHA getAlpha() {
    	ALPHA en = ALPHA.first;
    	String str = getAttribute("alpha");
    	if (!Check.isEmpty(str)) {
    		try {
    			en = ALPHA.valueOf(str);
    		} catch (IllegalArgumentException iae) { }
    	}
    	return en;
    }
    
    public UNIT getUnit() {
    	UNIT en = null;
    	String str = getAttribute("unit");
    	if (!Check.isEmpty(str)) {
    		try {
    			en = UNIT.valueOf(str);
    		} catch (IllegalArgumentException iae) { }
    	}
    	return en;
    }
    
    public ORD getOrd() {
    	ORD en = ORD.alpha;
    	String str = getAttribute("ord");
    	if (!Check.isEmpty(str)) {
    		try {
    			en = ORD.valueOf(str);
    		} catch (IllegalArgumentException iae) { }
    	}
    	return en;
    }
    
    public ON[] getOn() {
    	String str = getAttribute("on");
    	if (!Check.isEmpty(str)) {
    		StringTokenizer tok = new StringTokenizer(str);
    		ON ens[] = new ON[tok.countTokens()];
    		int ii = 0;
    		while (tok.hasMoreTokens()) {
    			try {
        			ens[ii++] = ON.valueOf(tok.nextToken());
        		} catch (IllegalArgumentException iae) { }
    		}
    		return ens;
    	} else {
    		return new ON[0];
    	}
    }
    
    public int getNumber() {
    	int num = 0;
    	String str = getAttribute("number");
    	if (!Check.isEmpty(str)) {
    		try {
    			num = Integer.parseInt(str);
    			if (num < 0) num = 0;
    		} catch (NumberFormatException nfe) {}
    	}
    	return num;
    }
    
    public int getEvery() {
    	int num = 0;
    	String str = getAttribute("every");
    	if (!Check.isEmpty(str)) {
    		try {
    			num = Integer.parseInt(str);
    			if (num < 0) num = 0;
    		} catch (NumberFormatException nfe) {}
    	}
    	return num;
    }
    
    
    /**
     * Return hour and minutes
     * @return x[0] = HH, x[1] = MM
     */
    public int[] getAt() {
    	int num[] = {0, 0};
    	Matcher mr = timeRx.matcher(getAttribute("at"));
    	if (mr.matches()) {
    		num[0] = Integer.parseInt(mr.group(1));
    		num[1] = Integer.parseInt(mr.group(2));
    		// other resolutions too small for the release
    	}
    	return num;
    }
}
