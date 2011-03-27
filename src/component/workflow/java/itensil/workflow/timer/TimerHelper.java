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
package itensil.workflow.timer;

import itensil.workflow.model.element.Wait;
import itensil.workflow.model.element.Until;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.util.Check;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * @author ggongaware@itensil.com
 *
 */
public class TimerHelper {

    public static Date calcWait(Date fromTime, Wait waitDef) {
    	long offset;
    	
    	offset = ((((waitDef.getDays() * 24) +
    				waitDef.getHours()) * 60) +
    				waitDef.getMinutes()) * 60000;
       
    	return new Date(fromTime.getTime() + offset);
    }

    public static Date calcUntil(Date fromTime, Until untilDef) {
        
    	User usr = SecurityAssociation.getUser();
    	Calendar cal = usr == null ? new GregorianCalendar() : new GregorianCalendar(usr.getTimeZone());
    	cal.setTime(fromTime);
    	int fHH = cal.get(Calendar.HOUR_OF_DAY);
    	int fMM = cal.get(Calendar.MINUTE);
    	int at[] = untilDef.getAt();
    	boolean isHoursPassed = !(at[0] > fHH || (at[0] == fHH && at[1] > fMM));
    	
       swType : 
    	switch (untilDef.getType()) {
    	
		case daily:		
			// does today still qualify?
			if (isHoursPassed) {
				// go to tomorrow
				cal.add(Calendar.DAY_OF_YEAR, 1);
			}
			break;
			
			
		case weekly: {
    			Until.ON ons[] = untilDef.getOn();
    			int dow = cal.get(Calendar.DAY_OF_WEEK);
    			if (ons.length == 0) ons = new Until.ON[]{Until.ON.sun};
    			Arrays.sort(ons);
    			
    			int dayDiff = 0;
    			
    			// check this week
    			for (int ii = 0; ii < ons.length; ii++) {
    				if (ons[ii].cal > dow) {
    					dayDiff = ons[ii].cal - dow;
    				} else if (!isHoursPassed && ons[ii].cal == dow) {
    					// today works!
    					break swType;
    				}
    			}
    			
    			// if none from this week
    			if (dayDiff == 0) {
    				// first 'on' day of next week
    				dayDiff = (Calendar.SATURDAY - dow) + ons[0].cal;
    			}
    			cal.add(Calendar.DAY_OF_YEAR, dayDiff);
			}
			break;
			
		case monthly: {
			int num = -1;
			Until.UNIT unit = untilDef.getUnit();
			Until.ALPHA alpha = untilDef.getAlpha();
			Until.ON uon;
			Until.ON ons[] = untilDef.getOn();
			if (ons.length == 0) uon = Until.ON.day;
			else uon = ons[0];
			
			
			// check id this day/week-day of the month
			if (untilDef.getOrd() == Until.ORD.number) {
				num = untilDef.getNumber();
			} else if (alpha.ordinal() < Until.ALPHA.last.ordinal()) { // is 1st, 2nd 3rd ?
				num = alpha.ordinal() + 1;
				
				// map Until.ON to Until.UNIT
				if (uon == Until.ON.day) unit = Until.UNIT.day;
				else if (uon == Until.ON.wday) unit = Until.UNIT.wday;
				else unit = null;
				
			} else if (alpha == Until.ALPHA.next) { // next will pretend to be a numeric
				num = 0;
				
				// map Until.ON to Until.UNIT
				if (uon == Until.ON.day) unit = Until.UNIT.day;
				else if (uon == Until.ON.wday) unit = Until.UNIT.wday;
				else unit = null;
			}
			
			// is this numeric day
			if (num >= 0 && unit != null) {
				int dom = cal.get(Calendar.DAY_OF_MONTH);
				
				switch (unit) {
				case day: {
					
					// next check
					if (alpha == Until.ALPHA.next) num = dom + 1;
					
					// next month?
				    if (!(num > dom || (!isHoursPassed && dom == num))) {
				    	cal.add(Calendar.MONTH, 1);
				    }
				    cal.set(Calendar.DAY_OF_MONTH, num);
				}
				break;
					
				case wday:
					int wdom = 0;
					Calendar wcal = (Calendar)cal.clone();
					
					// count current
					for (wcal.set(Calendar.DAY_OF_MONTH, 1); wcal.compareTo(cal) <= 0; wcal.add(Calendar.DAY_OF_MONTH, 1)) {
						int dow = wcal.get(Calendar.DAY_OF_WEEK);
						if (!(dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)) wdom++;
					}
					
					// next check part 1
					if (alpha == Until.ALPHA.next) num = wdom;
					
					// if not today
					if (!(!isHoursPassed && wdom == num)) {

						// next check part 2
						if (alpha == Until.ALPHA.next) num = wdom + 1;
						
						// is today past this months number?
						if (wdom >= num) {
					    	cal.add(Calendar.MONTH, 1);
					    }
						wdom = 0;
						
						// set target
						cal.set(Calendar.DAY_OF_MONTH, 1); 
						while (wdom < num) {
							int dow = cal.get(Calendar.DAY_OF_WEEK);
							if (!(dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)) wdom++;
							if (wdom < num) cal.add(Calendar.DAY_OF_MONTH, 1);
						}
					}
					break;
				}
				
			} else {
				
				
				if (alpha == Until.ALPHA.last) {
					
					// this month pass
					int dom = cal.get(Calendar.DAY_OF_MONTH);
					int ld = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
					if (uon != Until.ON.day) {
						Calendar wcal = (Calendar)cal.clone();
						wcal.set(Calendar.DAY_OF_MONTH, ld);
						int dow = wcal.get(Calendar.DAY_OF_WEEK);
						if (uon == Until.ON.wday) { // seek weekday
							while (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
								wcal.add(Calendar.DAY_OF_MONTH, -1);
								dow = wcal.get(Calendar.DAY_OF_WEEK);
								ld--;
							}
						} else if (dow != uon.cal) {
							
							// last week?
							if (uon.cal > dow) ld -= (dow + (Calendar.SATURDAY - uon.cal));
							else ld -= dow - uon.cal;
						}
					}
					
					// if not today
					if (!(!isHoursPassed && dom == ld)) {
						if (dom >= ld) {
					    	cal.add(Calendar.MONTH, 1);
					    	 
					    	// next month pass
					    	ld = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
							if (uon != Until.ON.day) {
								Calendar wcal = (Calendar)cal.clone();
								wcal.set(Calendar.DAY_OF_MONTH, ld);
								int dow = wcal.get(Calendar.DAY_OF_WEEK);
								if (uon == Until.ON.wday) { // seek weekday
									while (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
										wcal.add(Calendar.DAY_OF_MONTH, -1);
										dow = wcal.get(Calendar.DAY_OF_WEEK);
										ld--;
									}
								} else if (dow != uon.cal) {
									
									// last week?
									if (uon.cal > dow) ld -= (dow + (Calendar.SATURDAY - uon.cal));
									else ld -= dow - uon.cal;
								}
							}
					    }
						
					    cal.set(Calendar.DAY_OF_MONTH, ld);   
					}
					
				} else if (num >= 0) {
					
					// at this point is should be a day of week test
					if (uon == Until.ON.day) uon = Until.ON.sun;
					int count = 0;
					Calendar wcal = (Calendar)cal.clone();
					
					// count current
					for (wcal.set(Calendar.DAY_OF_MONTH, 1); wcal.compareTo(cal) <= 0; wcal.add(Calendar.DAY_OF_MONTH, 1)) {
						if (wcal.get(Calendar.DAY_OF_WEEK) == uon.cal) count++;
					}
					
					// next check part 1
					if (alpha == Until.ALPHA.next) num = count;
					
				    // if not today
					if (!(!isHoursPassed && count == num && cal.get(Calendar.DAY_OF_WEEK) == uon.cal)) {
						
						// next check part 2
						if (alpha == Until.ALPHA.next) num = count + 1;
						
						// is today past this months number?
						if (count >= num) {
					    	cal.add(Calendar.MONTH, 1);
					    }
						count = 0;
						
						// set target
						cal.set(Calendar.DAY_OF_MONTH, 1); 
						while (count < num) {
							if (cal.get(Calendar.DAY_OF_WEEK) == uon.cal) count++;
							if (count < num) cal.add(Calendar.DAY_OF_MONTH, 1);
						}
					}
					
				}
			}
			
			} // case block
			break;
    	}
    
    	cal.set(Calendar.HOUR_OF_DAY, at[0]);
		cal.set(Calendar.MINUTE, at[1]);
    	
        return cal.getTime();
    }

    public static Date calcExpire(Date fromTime, String expire) {
        return null;
    }

}
