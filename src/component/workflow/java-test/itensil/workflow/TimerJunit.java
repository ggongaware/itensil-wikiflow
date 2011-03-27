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
package itensil.workflow;

import itensil.security.AuthenticatedUser;
import itensil.security.SecurityAssociation;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.element.Until;
import itensil.workflow.model.element.Wait;
import itensil.workflow.timer.TimerHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

public class TimerJunit extends TestCase {
	
	static DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
	static {
		dateFmt.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
	}
	
	
	
	protected void setUp() throws Exception {
		SecurityAssociation.setUser(new AuthenticatedUser(
                "AAAAAAAAAAAAAAAAAAAB",
                "junit@itensil.com",
                "junit",
                Locale.getDefault(),
                TimeZone.getTimeZone("America/Los_Angeles"),
                null,
                System.currentTimeMillis()
                ));
	}

	public void testWait() throws ParseException {
		Date fromDate = dateFmt.parse("2007-04-12T13:45");
		Wait waitDef = new Wait(null, null);
		
		
		waitDef.setAttribute("days", "3");
		assertEquals("2007-04-15T13:45", dateFmt.format(TimerHelper.calcWait(fromDate, waitDef)));
		
		waitDef.setAttribute("days", "-7"); // positive only
		assertEquals("2007-04-12T13:45", dateFmt.format(TimerHelper.calcWait(fromDate, waitDef)));
		
		waitDef.setAttribute("days", "1");
		waitDef.setAttribute("hours", "2");
		waitDef.setAttribute("minutes", "5");
		assertEquals("2007-04-13T15:50", dateFmt.format(TimerHelper.calcWait(fromDate, waitDef)));
	}
	
	public void testWait2() throws ParseException {
		Date fromDate = dateFmt.parse("2007-01-31T23:45");
		Wait waitDef = new Wait(null, null);
		
		waitDef.setAttribute("days", "2");
		assertEquals("2007-02-02T23:45", dateFmt.format(TimerHelper.calcWait(fromDate, waitDef)));
		
		waitDef.setAttribute("days", "0");
		waitDef.setAttribute("minutes", "20");
		assertEquals("2007-02-01T00:05", dateFmt.format(TimerHelper.calcWait(fromDate, waitDef)));
	}
	
	public void testUntilDaily() throws ParseException {
		Date fromDate = dateFmt.parse("2007-04-12T13:45");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "daily");
		
		untilDef.setAttribute("at", "13:46");
		assertEquals("2007-04-12T13:46", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("at", "10:01");
		assertEquals("2007-04-13T10:01", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilWeekly() throws ParseException  {
		Date fromDate = dateFmt.parse("2007-04-12T13:45");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "weekly");
		untilDef.setAttribute("at", "13:46");
		
		untilDef.setAttribute("on", "wed thu");
		assertEquals("2007-04-12T13:46", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("on", "fri");
		assertEquals("2007-04-13T13:46", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("on", "mon tue");
		assertEquals("2007-04-16T13:46", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
	}
	
	public void testUntilWeekly2() throws ParseException  {
		Date fromDate = dateFmt.parse("2007-02-26T08:00");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "weekly");
		untilDef.setAttribute("at", "09:00");
		
		untilDef.setAttribute("on", "sat");
		assertEquals("2007-03-03T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("on", "sun");
		assertEquals("2007-03-04T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilMonthlyNumber() throws Exception {
		Date fromDate = dateFmt.parse("2007-05-01T21:56");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "monthly");
		untilDef.setAttribute("at", "12:34");
		untilDef.setAttribute("ord", "number");
		
		untilDef.setAttribute("unit", "day");
		untilDef.setAttribute("number", "3");
		assertEquals("2007-05-03T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("unit", "wday");
		untilDef.setAttribute("number", "3");
		assertEquals("2007-05-03T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		fromDate = dateFmt.parse("2007-05-02T21:56");
		assertEquals("2007-05-03T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("unit", "day");
		untilDef.setAttribute("number", "2");
		assertEquals("2007-06-02T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("unit", "wday");
		untilDef.setAttribute("number", "2");
		assertEquals("2007-06-04T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("unit", "wday");
		untilDef.setAttribute("number", "13");
		assertEquals("2007-05-17T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("unit", "day");
		untilDef.setAttribute("number", "13");
		assertEquals("2007-05-13T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		
		// alpha overlap
		untilDef.setAttribute("ord", "alpha");
		
		untilDef.setAttribute("on", "wday");
		untilDef.setAttribute("alpha", "second");
		assertEquals("2007-06-04T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilMonthlyAlpha() throws Exception {
		Date fromDate = dateFmt.parse("2007-05-02T21:56");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "monthly");
		untilDef.setAttribute("at", "12:34");
		untilDef.setAttribute("ord", "alpha");
	
		untilDef.setAttribute("alpha", "first");
		untilDef.setAttribute("on", "fri");
		assertEquals("2007-05-04T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "second");
		untilDef.setAttribute("on", "thu");
		assertEquals("2007-05-10T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "third");
		untilDef.setAttribute("on", "fri");
		assertEquals("2007-05-18T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "fourth");
		untilDef.setAttribute("on", "fri");
		assertEquals("2007-05-25T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		fromDate = dateFmt.parse("2007-05-07T21:56");
		
		untilDef.setAttribute("alpha", "second");
		untilDef.setAttribute("on", "wed");
		assertEquals("2007-05-09T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "second");
		untilDef.setAttribute("on", "mon");
		assertEquals("2007-05-14T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		fromDate = dateFmt.parse("2007-05-08T21:56");
		untilDef.setAttribute("alpha", "second");
		untilDef.setAttribute("on", "tue");
		assertEquals("2007-06-12T12:34", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilMonthlyNext() throws Exception {
		Date fromDate = dateFmt.parse("2007-02-26T08:00");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "monthly");
		untilDef.setAttribute("at", "09:00");
		untilDef.setAttribute("ord", "alpha");
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "mon");
		assertEquals("2007-02-26T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		fromDate = dateFmt.parse("2007-02-26T09:00");
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "mon");
		assertEquals("2007-03-05T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "fri");
		assertEquals("2007-03-02T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "day");
		assertEquals("2007-02-27T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "wday");
		assertEquals("2007-02-27T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		fromDate = dateFmt.parse("2007-02-23T09:00");
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "wday");
		assertEquals("2007-02-26T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilMonthlyLast() throws Exception {
		Date fromDate = dateFmt.parse("2007-02-26T08:00");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "monthly");
		untilDef.setAttribute("at", "09:00");
		untilDef.setAttribute("ord", "alpha");
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "mon");
		assertEquals("2007-02-26T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "tue");
		assertEquals("2007-02-27T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "day");
		assertEquals("2007-02-28T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "wday");
		assertEquals("2007-02-28T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "wed");
		assertEquals("2007-02-28T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "thu");
		assertEquals("2007-03-29T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "sun");
		assertEquals("2007-03-25T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
		untilDef.setAttribute("alpha", "last");
		untilDef.setAttribute("on", "sat");
		assertEquals("2007-03-31T09:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
	}
	
	public void testUntilMonthlyLastTZ() throws Exception {
		DateFormat dateFmtUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		dateFmtUTC.setTimeZone(TimeZone.getTimeZone("Europe/Dublin"));
		
		Date fromDate = dateFmtUTC.parse("2007-08-13T08:00Z");
		Until untilDef = new Until(null, null);
		
		untilDef.setAttribute("type", "monthly");
		untilDef.setAttribute("at", "19:00");
		untilDef.setAttribute("ord", "alpha");
		
		untilDef.setAttribute("alpha", "next");
		untilDef.setAttribute("on", "tue");
		
		assertEquals("2007-08-14T19:00", dateFmt.format(TimerHelper.calcUntil(fromDate, untilDef)));
		assertEquals("2007-08-15T03:00Z", dateFmtUTC.format(TimerHelper.calcUntil(fromDate, untilDef)));
		
	}
	
}
