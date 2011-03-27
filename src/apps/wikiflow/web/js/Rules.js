/**
 * (c) 2008 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 *
 * @requires xpath.js
 *
 */
 
 /**
  * new Rule object using a model node
  */
 function Rules(ruleNode, rulesSetNode) {
 	this.ruleNode = ruleNode;
 	this.rulesSetNode = rulesSetNode;
 	Rules.init();
 	this.doTrace = true;
 	this.depList = null;
 }
 
 Rules.init = function() {
 	if (!Rules.__xpParser) {
 		Rules.__xpFuncRes = new FunctionResolver(Rules);
 		Rules.__xpVarRes = new VariableResolver();
 		Rules.__xpNSRes = new NamespaceResolver();
 		Rules.__xpParser = new XPathParser();
 		Rules.__xpCacheObj = new Object();
 		if (typeof(App) != "undefined") App.addDispose(Rules);
 	}
 };
 
 Rules.dispose = function() {
 	Rules.__xpCacheObj = null;
 };
 
 Rules.prototype.setOptimizing = function(setOn) {
 	if (setOn) {
 		this.doTrace = false;
 		this.__xpCacheObj = Rules.__xpCacheObj;
 	} else {
 		this.doTrace = true;
 		this.__xpCacheObj = null;
 	}
 };
 
 /**
  * Eval and return a <return/> match
  */
 Rules.prototype.match = function(contextNode) {
 	if (this.doTrace) this.trace = [];
 	this.setNodes = [];
 	var tests = Xml.match(this.ruleNode, "*");
 	var res = null;
 	for (var ii=0; ii < tests.length; ii++) {
 		res = this.__deepMatch(tests[ii], contextNode);
 		if (res != null) break;
 	}
 	return res;
 };
 
Rules.prototype.__deepMatch = function(testNode, contextNode) {
	var tests = null;
	var trc = [testNode, false];
	if (this.doTrace) {
		this.trace.push(trc);
	}
 	switch (Xml.getLocalName(testNode)) {
 		case "return":
 			trc[1] = true;
 			return testNode.getAttribute("id");
 			
 		case "when": {
 				var xpExpr = "";
 				var testArg = null;
 				if (testNode.getAttribute("type") == "xpath") {
 					xpExpr = testNode.getAttribute("test");
 				} else {
 					testArg = testNode.getAttribute("aggregate") +
 						"(" + testNode.getAttribute("field") + ") ";
 					xpExpr = testArg + testNode.getAttribute("op") + " " + testNode.getAttribute("arg");
 				}
 				
 				if (xpExpr) {
 					try {
	 					var resExp = this.__xPathSelect(contextNode, xpExpr);
	 					if (this.doTrace && testArg) {
	 						var testExp = this.__xPathSelect(contextNode, testArg);
	 						trc[2] = testExp.stringValue();
	 					} else if (this.doTrace) {
	 						trc[2] = resExp.booleanValue() ? "true" : "false";
	 					}
	 					if (resExp.booleanValue()) {
	 						trc[1] = true;
	 						tests = Xml.match(testNode, "*");
	 					}
 					} catch (ee) {
 						trc[2] = "Error: " + (ee.message || ee.detail || ee.toString());
 					}
 				}
 		
 				break;
 			}
 			
 		case "otherwise":
 			trc[1] = true;
 			tests = Xml.match(testNode, "*");
 			break;
 			
 		case "set":
 			try {
 				
				var resExp = this.__xPathSelect(contextNode, testNode.getAttribute("field"));
				var valExp = this.__xPathSelect(contextNode, testNode.getAttribute("value"));
				trc[1] = true;
				var fNod = resExp.first();
				if (fNod) {
					this.setNodes.push(fNod);
					trc[2] = valExp.stringValue();
					Xml.setNodeValue(fNod, trc[2]);
				}

			} catch (ee) {
				trc[2] = "Error: " + (ee.message || ee.detail || ee.toString());
			}
 			break;
 			
 		case "sub": {
 				trc[1] = true;
 				var rulNode = this.rulesSetNode ? 
 					Xml.matchOne(this.rulesSetNode, "rule", "id", testNode.getAttribute("id")) : null;
 				if (rulNode) {
 					 tests = Xml.match(rulNode, "*");
 				}
 				break;
 			}
 	}
 	var res = null;
 	if (tests) {
 		for (var ii=0; ii < tests.length; ii++) {
	 		res = this.__deepMatch(tests[ii], contextNode);
	 		if (res != null) break;
	 	}
 	}
 	return res;
};
 
 
Rules.prototype.__xPathSelect = function(contextNode, query) {
	
    // Parse the XPath expression
    var xpath;
    if (this.__xpCacheObj) {
    	xpath = this.__xpCacheObj[query];
    	if (!xpath) {
     		xpath = Rules.__xpParser.parse(query);
     		this.__xpCacheObj[query] = xpath;
    	}
    } else {
    	xpath = Rules.__xpParser.parse(query);
    }
    return this.__xPathSelectComp(contextNode, xpath);
};

Rules.prototype.__xPathSelectComp = function(contextNode, xpath) {

    // Create a context for the XPath to be evaluated in
    var context = new XPathContext(Rules.__xpVarRes, Rules.__xpNSRes, Rules.__xpFuncRes);
    context.depList = this.depList;
    context.expressionContextNode = contextNode;

    // Evaluate the XPath expression
    return xpath.evaluate(context);
};

Rules.prototype.__xPathSelectOneComp = function(contextNode, xpath) {
    var res = this.__xPathSelectComp(contextNode, xpath);
    if (res.constructor === XNodeSet) {
        return res.first();
    } else {
        return res.stringValue();
    }
};

Rules.prototype.__xPathSelectOne = function(contextNode, query) {
    var res = this.__xPathSelect(contextNode, query);
    if (res.constructor === XNodeSet) {
        return res.first();
    } else {
        return res.stringValue();
    }
};

Rules.prototype.dispose = function() {
	this.ruleNode = null;
	this.rulesSetNode = null;
	this.trace = null;
	this.setNodes = null;
};
 
 
 