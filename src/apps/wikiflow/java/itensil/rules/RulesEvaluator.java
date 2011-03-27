package itensil.rules;

import itensil.util.Check;
import itensil.workflow.activities.ActivityXML;

import java.util.Date;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.XPath;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.StringFunction;

public class RulesEvaluator implements RulesXPathFunctions.XPHost{

	protected Element contextNode;
	protected Document ruleDoc;
	protected boolean modified;
	protected Element ruleBase;
	
	public RulesEvaluator(Document ruleDoc) {
		this.ruleDoc = ruleDoc;
	}
	
	public String match(Element contextNode) {
		return match("default", contextNode);
	}
	
	public String match(String ruleId, Element contextNode) {
		Element ruleBase = ruleDoc.getRootElement();
		Element rule = findRule(ruleId, ruleBase);
		return match(rule, ruleBase, contextNode);
	}
	
	/**
	  * Eval and return a <return/> match
	  */
	@SuppressWarnings("unchecked")
	public String match(Element rule, Element ruleBase, Element contextNode) {
		String res = null;
		modified = false;
		this.ruleBase = ruleBase;
		this.contextNode = contextNode;
		
		if (rule != null) {
			List<Element> tests = rule.elements();
			for (Element test : tests) {
				res = this.deepMatch(test, contextNode);
				if (res != null) break;
			}
		}
		return res;
	}
	
	@SuppressWarnings("unchecked")
	protected String deepMatch(Element testNode, Element contextNode) {
		String res = null;
		List<Element> tests = null;
		String tName = testNode.getName();
		XPath xp;
		
		// Main if tree
		if (		"return".equals(tName)) {
			
			return testNode.attributeValue("id");
			
		} else if (	"when".equals(tName)) {
			
			String xpExpr;
			if ("xpath".equals(testNode.attributeValue("type"))) {
				xpExpr = testNode.attributeValue("test");
			} else {
				xpExpr = testNode.attributeValue("aggregate") +
					"(" + testNode.attributeValue("field") + ") " +
					testNode.attributeValue("op") + " " + testNode.attributeValue("arg");
			}
			
			if (!Check.isEmpty(xpExpr)) {
				xp = createXPath(contextNode, xpExpr);
				
				// Did it qualify ?
				if (xp.booleanValueOf(contextNode)) {
					tests = testNode.elements();
				}
			}
			
			
			
		} else if (	"otherwise".equals(tName)) {
			
			tests = testNode.elements();
			
		} else if (	"set".equals(tName)) {

			//	 Element created if it did not exist
	        Element field = DocumentHelper.makeElement(contextNode, testNode.attributeValue("field"));
			if (field != null) {
				xp = createXPath(contextNode, testNode.attributeValue("value"));
				String val = xp.valueOf(contextNode);
				field.setText(val);
				this.modified = true;
			}
			
		} else if (	"sub".equals(tName)) {
			
			Element ruleNode = this.findRule(testNode.attributeValue("id"), this.ruleBase);
			if (ruleNode != null) {
				tests = ruleNode.elements();
			}
			
		}
		// End if

		
		if (tests != null) {
			for (int ii=0; ii < tests.size(); ii++) {
		 		res = this.deepMatch(tests.get(ii), contextNode);
		 		if (res != null) break;
		 	}
		}
		
		return res;
	}
	
	
	protected XPath createXPath(Element contextNode, String expr) {
		XPath xp = contextNode.createXPath(expr);
		XPathFunctionContext xpfc = new XPathFunctionContext(false);
		RulesXPathFunctions.initFunctionContext(xpfc, this, null);
		xp.setFunctionContext(xpfc);
		return xp;
	}
	
	
	@SuppressWarnings("unchecked")
	protected Element findRule(String ruleId, Element ruleBase) {
		if (!Check.isEmpty(ruleId)) {
			for (Element rule : ((List<Element>)ruleBase.elements("rule"))) {
				if (ruleId.equals(rule.attributeValue("id"))) {
					return rule;
				}
			}
		}
		return null;
	}

	public boolean isModified() {
		return modified;
	}

	public Object getConextNode() {
		return contextNode;
	}
	
}
