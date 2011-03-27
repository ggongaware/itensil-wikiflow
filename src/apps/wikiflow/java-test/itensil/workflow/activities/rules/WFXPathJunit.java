package itensil.workflow.activities.rules;

import java.util.Date;

import itensil.workflow.activities.ActivityXML;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;

import junit.framework.TestCase;

public class WFXPathJunit extends TestCase {
	
	Document datDoc;
	
	public WFXPathJunit() {
		try {
			datDoc = DocumentHelper.parseText("<root><num>1</num><date>2007-05-10</date><text>text</text></root>");
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public void testDatefuncs() throws Exception {
		XPathConditionEval<String> eval = new XPathConditionEval<String>(null);
		XPath xp = eval.getXPath("days-from-date('2002-01-01')", datDoc, null);
		assertEquals(11688, xp.numberValueOf(datDoc.getRootElement()).intValue());
		xp = eval.getXPath("days-from-date('1969-12-31')", datDoc, null);
		assertEquals(-1, xp.numberValueOf(datDoc.getRootElement()).intValue());
		xp = eval.getXPath("now()", datDoc, null);
		assertEquals(ActivityXML.dateFmtZ.format(new Date()), xp.valueOf(datDoc.getRootElement()));
		xp = eval.getXPath("days-from-date(now())", datDoc, null);
		assertTrue(xp.numberValueOf(datDoc.getRootElement()).intValue() >= 13881);
	}
	
}
