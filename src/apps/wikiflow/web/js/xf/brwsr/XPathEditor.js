/**
 * (c) 2008 Itensil, Inc.
 *  ggongaware (at) itensil.com
 */
 
 
/**
 * @xpathObj - Either 'Rules' or 'XForm' object
 * @contextNode - XML DOM Node
 */
function XPathEditor(xpathObj, contextNode) {
	XPathEditor.__initMenus();
	this.xpathObj = xpathObj;
	this.contextNode = contextNode;	
}

XPathEditor.dialog = function(title, xpathObj, contextNode, diagHelp) {
	var diag = new Dialog(title, true);
   	diag.canResize = false;
    if (diagHelp) diag.initHelp(diagHelp);
    diag.render(document.body);
	var xpe = new XPathEditor(xpathObj, contextNode);
	xpe.render(makeElement(diag.contentElement, "div", "diagXpEdit"));	
    diag.xpeditor = xpe;
    diag.addDisposable(xpe);
    return diag;
};


XPathEditor.prototype = {
	constructor : XPathEditor,
	
	menuClick : function(item) {
		this.__taInsert(item.value);
	},
	
	render : function (uiParent) {
		// draw
		var tbody = makeLayoutTable(uiParent, "xpEdit");
		var tr = makeElement(tbody, "tr");
		
		this.edVarMn = new Menu(new XpEdVarMnModel(this.contextNode));
		
		
		var holdThis = this;

		// button row
		setEventHandler(makeElement(makeElement(tr, "td"), "div", "btn", noBreakString("fields ")), 
			"onmousedown", function(evt) {
			    evt.cancelBubble = true;
			    var b = getViewBounds(this);
			    holdThis.edVarMn.show(b.x, b.y + b.h, null, holdThis);
			    return false;
			});
			
		setEventHandler(makeElement(makeElement(tr, "td"), "div", "btn", noBreakString("f(x)     ")), 
			"onmousedown", function(evt) {
			    evt.cancelBubble = true;
			    var b = getViewBounds(this);
			    XPathEditor.menu.show(b.x, b.y + b.h, null, holdThis);
			    return false;
			});
		setEventHandler(makeElement(makeElement(tr, "td"), "div", "btn", noBreakString("test    ")), 
			"onmousedown", function(evt) { holdThis.edTest(evt); });
			
		this.testSelElem = makeElement(makeElement(tr, "td"), "div", "btn", "test selected");
		this.testSelElem.style.visibility = "hidden";
		
		setEventHandler(this.testSelElem, "onmousedown", function(evt) { holdThis.edTestSel(evt); });
		
		// edit row
		tr = makeElement(tbody, "tr");
		this.taElem = makeElement(
			makeElement(tr, "td", null, null, null, {colSpan:4}),
			"textarea", null, null, null, {name:"expression"});
		
		// result row
		tr = makeElement(tbody, "tr");
		this.resElem = makeElement(makeElementNbSpd(tr, "td", "res", null, {colSpan:4}), "span");
		
		// editing events
		var xpTASel = function(evt) {holdThis.__taSel(evt)};
		this.taElem.onkeyup = xpTASel;
	    this.taElem.onselect = xpTASel;
	    this.taElem.onmouseup = xpTASel;
	    this.taElem.onfocus = xpTASel;
	    
	    tbody = null; tr = null; // IE enclosure clean-up
	},
	
	getText : function() {
		return this.taElem.value;
	},
	
	setText : function(txt) {
		this.taElem.value = txt;
	},

 	edTest : function(evt) {
	    this.resElem.style.color = "";
	    setElementText(this.resElem, "Testing...");
	    var holdThis = this;
	    window.setTimeout(function() {
	        	holdThis.__edCalc(holdThis.taElem.value, true);
	        }, 200);
	    return false;
	},
	
	edTestSel : function(evt) {
	    this.resElem.style.color = "";
	    setElementText(this.resElem, "Testing...");
	    var txtExp = "";
	    if (this.taElem.lastRange != null) {
	        txtExp = this.taElem.lastRange.text;
	    } else if (this.taElem.selectionStart != null) {
	        var startPos = this.taElem.selectionStart;
			var endPos = this.taElem.selectionEnd;
			var val = this.taElem.value;
			txtExp = val.substring(startPos, endPos);
		}
		var holdThis = this;
	    window.setTimeout(function() {
	        	holdThis.__edCalc(txtExp, false);
	        }, 200);
	    return false;
	},

	dispose : function() {
	    this.testSelElem = null;
	    this.taElem = null;
	    this.resElem = null;
	    this.xpathObj = null;
	    this.contextNode = null;
	    if (this.edVarMn != null) {
	        this.edVarMn.dispose();
	    }
	},

 	__edCalc : function(txtExp, isBool) {
	   	 if (this.contextNode == null) {
	        this.resElem.style.color = "#900";
	        setElementText(this.resElem, "No data source");
	        return;
	    }
	    try {
	        // Evaluate the XPath expression
	        var res = this.xpathObj.__xPathSelect(this.contextNode, txtExp);
	        var msg = "";
	        if (isBool) {
	            msg = res.bool() + ", ";
	        }
	        if (res.constructor == XNodeSet) {
	            switch (res.size) {
	                case 0: msg += "<empty-set>"; break;
	                case 1: msg += "(" + res.stringValue() + ")";  break;
	                default: msg += "<set size=" + res.size + ">"; break;
	            }
	        } else if (res.constructor == XString) {
	            msg += "'" + res.stringValue() + "'";
	        } else {
	            msg += res.stringValue();
	        }
	        this.resElem.style.color = "#282";
	        setElementText(this.resElem, msg);
	    } catch (e) {
	       	this.resElem.style.color = "#900";
	       	setElementText(this.resElem, e);
	    }
	},		

 	__taInsert : function(txt) {
	    var beginTxt = "";
	    var endTxt = txt;
	    var repPos = txt.indexOf("@@");
	    if (repPos >= 0) {
	        beginTxt = txt.substring(0, repPos);
	        endTxt = txt.substring(repPos + 2);
	    }
	    var selTxt;
	    if (this.taElem.lastRange != null) {
	        selTxt = this.taElem.lastRange.text;
	        this.taElem.lastRange.text = beginTxt + selTxt + endTxt;
	        this.taElem.lastRange.select();
	    } else if (this.taElem.selectionStart != null) {
	
	        // insert
	        var startPos = this.taElem.selectionStart;
			var endPos = this.taElem.selectionEnd;
			var scrollTop = this.taElem.scrollTop;
			var val = this.taElem.value;
			selTxt = val.substring(startPos, endPos);
			var insTxt = beginTxt + selTxt + endTxt
			this.taElem.value =
			        val.substring(0, startPos) + insTxt + val.substring(endPos);
			this.taElem.focus();
	
	        // restore carret
			var cPos = startPos + insTxt.length;
			//if (startPos != endPos) {
			//    __xpTaElem.selectionStart = startPos;
			//    __xpTaElem.selectionEnd = cPos;
			//} else {
			    this.taElem.selectionStart = cPos;
			    this.taElem.selectionEnd = cPos;
			//}
			this.taElem.scrollTop = scrollTop;
	    } else {
	        this.taElem.value += txt;
	        this.taElem.focus();
	    }
	},
	
	__taSel : function(evt) {
	    var hasSelect = false;
	    if (SH.is_ie) {
	        var r = document.selection.createRange().duplicate();
	        this.taElem.lastRange = r;
	        hasSelect = r.text.length > 0;
	    } else if (this.taElem.selectionStart != null) {
	        var size = this.taElem.selectionEnd - this.taElem.selectionStart;
	        hasSelect = size > 0;
	    }
	    if (hasSelect) {
	        this.testSelElem.style.visibility = "";
	    } else {
	        this.testSelElem.style.visibility = "hidden";
	    }
	}
};

XPathEditor.__initMenus = function() {
	if (!XPathEditor.menu) {
		XPathEditor.menu = new Menu(new MenuModel(
		    [   {label : "Math Functions", sub : new Menu(new MenuModel(
		            [   {label : "Sum <sum(dataSet)>", value : "sum(@@)"},
		            	{label : "Average <avg(dataSet)>", value : "avg(@@)"},
		            	{label : "Minimum <min(dataSet)>", value : "min(@@)"},
		            	{label : "Maximum <max(dataSet)>", value : "max(@@)"},
		            	{label : "Standard deviation <stddev(dataSet)>", value : "stddev(@@)"},
		            	{isSep: true },
		                {label : "Round <round(x)>", value : "round(@@)"},
		                {label : "Round-down <floor(x)>", value : "floor(@@)"},
		                {label : "Round-up <ceiling(x)>", value : "ceiling(@@)"},
		                {isSep: true },
		                {label : "Convert <number(x)>", value : "number(@@)"}
		            ]), XPathEditor.__menuClick)},
		        {label : "String Functions", sub : new Menu(new MenuModel(
		            [   {label : "Concatenate <concat('x','y',...)>", value : "concat(@@, )"},
		                {label : "Length <string-length('str')>", value : "string-length(@@)"},
		                {label : "Contains? <contains('str','test')>", value : "contains(@@, )"},
		                {label : "Starts with? <starts-with('str','prefix')>", value : "starts-with(@@, )"},
		                {label : "Substring <substring('str',start,end)>", value : "substring(@@, )"},
		                {label : "Substring before <substring-before('str','endAt')>", value : "substring-before(@@, )"},
		                {label : "Substring after <substring-after('str','startAt')>", value : "substring-after(@@, )"},
		                {isSep: true },
		                {label : "Convert <string(x)>", value : "string(@@)"}
		            ]), XPathEditor.__menuClick)},
		         {label : "Date/Time Functions", sub : new Menu(new MenuModel(
		            [   {label : "Current time <now()>", value : "now()"},
		                {label : "Days since 1970-01-01 <days-from-date('date')>", value : "days-from-date(@@)"}
		            ]), XPathEditor.__menuClick)},
		        {label : "Data Functions", sub : new Menu(new MenuModel(
		            [   {label : "last index <last()>", value : "last()"},
		                {label : "position index <position()>", value : "position()"},
		                {label : "count <count(dataSet)>", value : "count(@@)"},
		                {isSep: true },
		                {label : "Context node <current()>", value : "current()"}
		            ]), XPathEditor.__menuClick)},
		        {label : "Operations", sub : new Menu(new MenuModel(
		            [   {label : "Addition ( + )", value : " + "},
		                {label : "Subtraction ( - )", value : " - "},
		                {label : "Multiply ( * )", value : " * "},
		                {label : "Divide ( div )", value : " div "},
		                {label : "Remainder ( mod )", value : " mod "},
		                {isSep: true },
		                {label : "Union ( | )", value : " | "}
		            ]), XPathEditor.__menuClick)},
		         {label : "Compares", sub : new Menu(new MenuModel(
		            [   {label : "Greater ( > )", value : " > "},
		                {label : "Greater or Equal ( >= )", value : " >= "},
		                {label : "Lesser ( < )", value : " < "},
		                {label : "Lesser or Equal ( <= )", value : " < "},
		                {label : "Equals ( = )", value : " = "},
		                {label : "Not Equal ( != )", value : " != "},
		                {isSep: true },
		                {label : "And ( and )", value : " and "},
		                {label : "Or ( or )", value : " or "},
		                {label : "Not <not(x)>", value : "not(@@)"},
		                {label : "True <true()>", value : "true()"},
		                {label : "False <false()>", value : "false()"},
		                {isSep: true },
		                {label : "If test true X else Y <if(test,X,Y)>", value : "if(@@, , )"},
		                {isSep: true },
		                {label : "Convert <boolean(x)>", value : "boolean(@@)"}
		            ]), XPathEditor.__menuClick)}
		    ]));
		    
		if (typeof(App) != "undefined") App.addDispose(XPathEditor.menu);
	}
};

XPathEditor.__menuClick = function(evt, context) {
	context.menuClick(this);
};



XpEdVarMnModel.prototype = new MenuModel();

function XpEdVarMnModel(node) {
    this.__node = node;
}

XpEdVarMnModel.prototype.onReady = function(callback, menu) {
    var items = [];
    var kids = this.__node.childNodes;
    var lastName = "", lastPos = 0, count = 0;
    this.items = items; // for dispose
    for (var i=0; i < kids.length; i++) {
        var kn = kids[i];
        if (kn.nodeType == 1) {
            count++;
            var itm = { label : kn.nodeName, node: kn, act : XpEdVarMnModel.__xpVarClick  };
            if (lastName == kn.nodeName) {
                itm.pos = count - lastPos + 1;
                itm.label += "[" + itm.pos + "]";
                if (itm.pos == 2) {
                    // replace last with set, and set last as [1]
                    var idx = items.length - 1;
                    var lastItm = items[idx];
                    items[idx] = {
                        label : lastItm.label + " <set>",
                        node: lastItm.node,
                        act : XpEdVarMnModel.__xpVarClick };
                    lastItm.pos = 1;
                    lastItm.label += "[1]";
                    items.push(lastItm);
                }
            } else {
                lastName = kn.nodeName;
                lastPos = count;
            }
            var hasKidElem = false;
            var skn = kn.firstChild;
            while (skn != null) {
                if (skn.nodeType == 1) {
                    hasKidElem = true;
                    break;
                }
                skn = skn.nextSibling;
            }
            if (hasKidElem) {
                itm.sub = new Menu(new XpEdVarMnModel(kn));
            }
            items.push(itm);
        }
    }
    callback.apply(menu, [items]);
};

XpEdVarMnModel.__xpVarClick = function(evt, ctx) {
    var n = this.node;
    var path = n.nodeName;
    n = n.parentNode;
    while (n.parentNode != null && n.parentNode.nodeType == 1) {
        path = n.nodeName + "/" + path;
        n = n.parentNode;
    }
    if (this.pos != null) {
        path += "[" + this.pos + "]";
    }
    ctx.__taInsert(path);
};

