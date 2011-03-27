package itensil.rules;

import org.dom4j.Document;
import org.dom4j.Element;

import itensil.io.xml.XMLDocument;
import junit.framework.TestCase;

public class RulesEvalJunit extends TestCase {

	/**
	 * 
	 * @throws Exception
	 */
	public void testRule1() throws Exception {
		
		Document dataDoc = XMLDocument.readStream(getClass().getResourceAsStream("test-data1.xml"));
		Document ruleDoc = XMLDocument.readStream(getClass().getResourceAsStream("test1.rule.xml"));
		
		RulesEvaluator rulEval = new RulesEvaluator(ruleDoc);
		
		Element dataRoot = dataDoc.getRootElement();
		assertFalse(rulEval.isModified());
		String res = rulEval.match(dataRoot);
		assertEquals("return", res);
		
		assertTrue(rulEval.isModified());
		assertEquals("7", dataRoot.elementText("mytxt"));
		assertEquals("value4", dataRoot.elementText("test"));
		assertEquals("value4", dataRoot.elementText("mynum"));
		
	}
}
