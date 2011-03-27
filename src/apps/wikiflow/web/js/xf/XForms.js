/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *  theoretically name tokes starting with
 *  double-underscore: "__" will be obfuscated
 **/


function __xpFuncIndex(/* string */) {
    var c = arguments[0];
	if (arguments.length != 2) {
		throw new Error("Function index expects (string)");
	}
	var id = arguments[1].evaluate(c).stringValue();
	var n = 1;
	return new XNumber(n);
}

function __xpFuncInstance(/* string */) {
    var c = arguments[0];
	if (arguments.length != 2) {
		throw new Error("Function instance expects (string)");
	}
	var id = arguments[1].evaluate(c).stringValue();
	var ns = new XNodeSet();
	var inst = this.__instances[id];
	if (inst) {
		ns.add(inst.__root);
	} else if (SH.debug) {
		SH.println("Instance missing: " + id);
	}
	
	return ns;
}

function __xpFuncJoin(/* node-set, string */) {
	var c = arguments[0];
	if (arguments.length != 3) {
		throw new Error("Function join expects (node-set, string)");
	}
	var res = arguments[1].evaluate(c);
	var strs = [];
    if (res.constructor === XNodeSet) {
      	var nds = res.toArray();
      	for (var ii =0; ii < nds.length; ii++){
      		strs.push(Xml.stringForNode(nds[ii]));
      	}
    }
    var sep = arguments[2].evaluate(c).stringValue();
    return new XString(strs.join(sep));
}

function __xpFuncDocument(/* string */) {
    var c = arguments[0];
	if (arguments.length != 2) {
		throw new Error("Function document expects (string)");
	}
	var uri = arguments[1].evaluate(c).stringValue();
	var ns = new XNodeSet();
	if (!uri) {
		return ns;
	}
	var fUri = uri;
    if (fUri.charAt(0) != "/" && fUri.indexOf("://") < 0) {
        fUri = this.resolveUri(Uri.absolute(this.__defaultModel.getInstancePath(c.expressionContextNode), uri));
    }
    if (typeof(FileTree) != "undefined" && fUri.indexOf("://") < 0) { 
		if (fUri.substring(0, FileTree.loadUri.length) != FileTree.loadUri) 
			fUri = FileTree.loadUri + fUri;
	}
    
   	var idoc = XFModel.__loadSharedSrc(fUri, this.__xb);
	
	if (idoc) ns.add(idoc);
	return ns;
}

function __xpFuncFormat(/* number [, decimals, dec_point, thousands_sep] */) {
    var c = arguments[0];
    if (arguments.length < 2) {
        throw new Error("Function format expects " +
            "( number [, decimals, dec_point, thousands_sep])");
    }
    return new XString(
        numberFormat(
        arguments[1].evaluate(c).numberValue(),
        arguments.length > 2 ? arguments[2].evaluate(c).numberValue() : null,
        arguments.length > 3 ? arguments[3].evaluate(c).numberValue() : null,
        arguments.length > 4 ? arguments[4].evaluate(c).numberValue() : null
        ));
}

function __xpFuncEvaluate(/* string [, context] */) {
    var c = arguments[0];
    if (arguments.length < 2) {
        throw new Error("Function evaluate expects ( string [, context] )");
    }
    var xpCtx = {
        node : c.expressionContextNode,
        varRes : c.variableResolver
    };
    if (c.depList != null) {
        xpCtx.depList = c.depList;
    }
    var xpath = arguments[1].evaluate(c).string();
    if (xpath == "") {
        return new XString("");
    }
    if (arguments.length > 2) {
        var res = arguments[2].evaluate(c);
        if (res.constructor === XNodeSet && res.booleanValue()) {
            xpCtx.node = res.first();
        } else {
            throw new Error("Context XPath does not evaluate to a node or nodeset");
        }
    }
    return this.__xPathSelectComp(xpCtx, this.__defaultModel.__xpathCache(xpath));
}

function __xpFuncScript(/* string [, context] */) {
	var c = arguments[0];
    if (arguments.length < 2) {
        throw new Error("Function script expects ( string [, context] )");
    }
    var ctxNode = c.expressionContextNode;
    var src = arguments[1].evaluate(c).string();
	if (arguments.length > 2) {
        var res = arguments[2].evaluate(c);
        if (res.constructor === XNodeSet && res.booleanValue()) {
            ctxNode = res.first();
        } else {
            throw new Error("Context XPath does not evaluate to a node or nodeset");
        }
    }
    return new XString(__xpFuncScript_eval(this.__defaultModel, ctxNode, src.toString()));
}

function __xpFuncScript_eval(model, contextNode, __src) {
	return eval(__src);	
}

function __xpFuncFormatDate() {
	var c = arguments[0];
    if (arguments.length < 2) {
        throw new Error("Function formatDate expects ( string )");
    }
    var str = arguments[1].evaluate(c).string();
    var dt = DateUtil.parse8601(str, str.length > 10);
    if (dt == null) return new XString("");
    return new XString(DateUtil.toLocaleShort(dt,false));
}

var __xfDefaultXsdTypeInfo = {
    mapped : XSD_MAPPED_TYPE.STRING,
    namespace : XFORM_SCHEMA_NAMESPACE,
    type : "string" };

XForm.EVENT_CLASSES = {
    "action"        : XFAction,
    "dispatch"      : XFDispatch,
    "setvalue"      : XFSetvalue,
    "toggle"        : XFToggle,
    "rebuild"       : XFRebuild,
    "recalculate"   : XFRecalculate,
    "revalidate"    : XFRevalidate,
    "refresh"       : XFRefresh,
    "message"       : XFMessage,
    "insert"        : XFInsert,
    "duplicate"     : XFDuplicate,
    "delete"        : XFDelete,
    "destroy"       : XFDestroy,
    "close"         : XFClose,
    "script"        : XFHtml,
    "constraint"    : XFIxConstraint,
    "dialog"		: XFIxDialog
    };

XForm.ELEMENT_CLASSES = {
    "input"         : XFControlInput,
    "secret"        : XFControlInput,
    "output"        : XFControlOutput,
    "group"         : XFControlGroup,
    "submit"        : XFControlSubmit,
    "repeat"        : XFRepeat,
    "trigger"       : XFControlTrigger,
    "label"         : XFLabel,
    "switch"        : XFSwitch,
    "case"          : XFCase,
    "action"        : XFAction,
    "dispatch"      : XFDispatch,
    "setvalue"      : XFSetvalue,
    "toggle"        : XFToggle,
    "rebuild"       : XFRebuild,
    "recalculate"   : XFRecalculate,
    "revalidate"    : XFRevalidate,
    "refresh"       : XFRefresh,
    "message"       : XFMessage,
    "textarea"      : XFControlTextarea,
    "select1"       : XFControlSelect1,
    "select"        : XFControlSelect,
    "item"          : XFItem,
    "itemset"       : XFItemset,
    "value"         : XFValue,
    "insert"        : XFInsert,
    "duplicate"     : XFDuplicate,
    "delete"        : XFDelete,
    "destroy"       : XFDestroy,
    "close"         : XFClose,
    "copy"          : XFCopy,
    "hint"          : XFHint,
    "variable"      : XFExfVariable,
    "attr"          : XFIxAttribute,
    "guide"         : XFIxGuide,
    "template"      : XFIxTemplate,
    "include"       : XFIxInclude,
    "entity"		: XFIxEntity,
    "subform"		: XFIxSubform,
    "drop"       	: XFIxDropControl,
    "drag"       	: XFIxDragControl,
    "dialog"		: XFIxDialog
    };


XForm.EVENT_PROCESS = {
 // "type"                          : [Cancelable?, Bubbles?,   Fatal?]
    "xforms-binding-exception"      : [false,       true,       true],
    "xforms-compute-exception"      : [false,       true,       true]
    };

XForm.__xpathFuncs = [];

XForm.addXpathFunc = function(name, func) {
	XForm.__xpathFuncs.push([name, func]);
};

XForm.addXpathFunc("index", __xpFuncIndex);
XForm.addXpathFunc("instance", __xpFuncInstance);
XForm.addXpathFunc("document", __xpFuncDocument); // xslt
XForm.addXpathFunc("format", __xpFuncFormat); // custom
XForm.addXpathFunc("evaluate", __xpFuncEvaluate); // custom
XForm.addXpathFunc("script", __xpFuncScript); // custom
XForm.addXpathFunc("formatDate", __xpFuncFormatDate); // custom
XForm.addXpathFunc("join", __xpFuncJoin); // custom

if (typeof(Rules) != "undefined") {
	
	function __xpFuncRule(/* [string, string, context] */) {
		var c = arguments[0];
		var rid = "", subid = "";
	  	if (arguments.length > 1) {
	  		rid = arguments[1].evaluate(c).string();
	  	}
	  	if (arguments.length > 2) {
	  		subid = arguments[2].evaluate(c).string();
	  	}
	  	var rules = this.__defaultModel.getRules(rid, subid);
	  	if (!rules) {
	  		throw new Error("Ruleset (" + rid + " - " +  subid + ") not found.");
	  	}
	  	var ctxNode = c.expressionContextNode;
	  	if (arguments.length > 3) {
	        var res = arguments[3].evaluate(c);
	        if (res.constructor === XNodeSet && res.booleanValue()) {
	            ctxNode = res.first();
	        } else {
	            throw new Error("Context XPath does not evaluate to a node or nodeset");
	        }
	    }
	    rules.depList = c.depList;
	    var mt = rules.match(ctxNode);
	    if (rules.setNodes.length > 0) {
	    	for (var ii=0; ii < rules.setNodes.length; ii++) {
	    		this.__defaultModel.markChanged(rules.setNodes[ii]);
	    	}
	    }
	  	return new XString(mt || "");
	}
	
	XForm.addXpathFunc("rule", __xpFuncRule); // custom
}


XForm.prototype = new XFormUI();
XForm.prototype.constructor = XForm;

function XForm(doc, name, xb /* XMLBuilder */, uri, context) {
	
	if (!doc || !doc.documentElement) {
		throw new Error("Invalid XForms document");	
	}
	
    this.__rootElem = doc.documentElement;
    this.__xb = xb;
    this.__isInit = false;
    this.__removing = false;
    this.__children = [];
    this.context = context;

    this.documentURI = uri;
    this.__path  = Uri.parent(uri);
    if (this.__path != "") this.__path += "/";
    this.__ids = new Object();
    this.__xpNodeEx = new XmlNodeExts();

    this.__buildModels();
    this.__name = name;
    this.__cases = new Object();
    this.__instances = new Object();
    this.__repeats = new Object();
    this.__tmpls = new Object();
    this.__defInstSrc = null;
    this.__submitActUri = null;
    
    // not in the spec but super useful
    this.autoGenRefs = true;
}


XForm.prototype.setDefaultUris = function(instanceSrc, submitAction) {
    if(instanceSrc)
        this.__defInstSrc = instanceSrc;
    if(submitAction)
        this.__submitActUri = submitAction;
};

XForm.prototype.isInit = function () {
    return this.__isInit;
};

XForm.prototype.isDirty = function () {
    return this.__isInit && this.__defaultModel.__isDirty;
};

XForm.prototype.init = function () {
    if (this.__defaultModel == null) {
        throw new Error("Missing XForms model");
    }
    this.__isInit = true;
    var funRes = new FunctionResolver(this);
    var ii;
    for (ii = 0; ii < XForm.__xpathFuncs.length; ii++) {
    	var pair = XForm.__xpathFuncs[ii];
    	funRes.addFunction("", pair[0], pair[1]);
    }
    this.__xpFuncRes = funRes;
    this.__xpNSRes = new NamespaceResolver();
    if (!this.__xpVarRes) this.__xpVarRes = new XFExfVariableResolver();
	var parAtts = this.__rootElem.getAttribute("params");
	if (parAtts) {
		var params = parAtts.split(/[\x0d\x0a\x09\x20]+/);
		for (ii = 0; ii < params.length; ii++) {
			if (!this.__xpVarRes.getVariableWithName(null, params[ii])) {
				this.__xpVarRes.setVariableWithName(null, params[ii], new XString(""));
			}
		}
	}
    // Create a new parser object
    this.__xpParser = new XPathParser();
    this.__typer = new XsdTypeManager();
    this.__defaultModel.__init();
};

XForm.prototype.__buildModels = function () {
    this.__models = new Object();
    this.__defaultModel = null;
    this.__xfRecurseModels(this.__rootElem);
};

XForm.prototype.__xfRecurseModels = function(node) {
    var kids = node.childNodes;
    for (var i = 0; i < kids.length; i++) {
        var n = kids[i];
        if (n.nodeType == 1) {
            if (n.namespaceURI == XFORM_NAMESPACE) {
                var locName = xmlGetLocalName(n);
                if (locName == "model") {
                    new XFModel(this, n);
                }
            } else {
                this.__xfRecurseModels(n);
            }
        }
    }
};


XForm.prototype.rebuildAll = function() {
    var mods = [];
    this.__removing = true;
    var ii;
    for (ii = this.__children.length - 1; ii >= 0; ii--) {
        var kid = this.__children[ii];
        if (kid.constructor === XFModel) {
            mods.push(kid);
        } else {
            kid.remove();
        }
    }
    this.__children = mods;
    // clear listeners, TODO self cleanup on remove
    this.__lTypes = null;
    this.__removing = false;
    this.__xpNodeEx.reset();
    for (ii = 0; ii < mods.length; ii++) {
    	var mod = mods[ii];
    	var xpCtx = new Object();
        xpCtx.node = mod.__defaultInstance == null ? null :
                mod.__defaultInstance.__root;
        xpCtx.varRes = mod.__form.__xpVarRes;
    	mod.__deps.reset();
        mod.__doRebuild(false);
        mod.__doRecalculate(false);
        XFModel.__rebuildChildren(mod, xpCtx);
    }
    this.render(this.__uiParent);
};

XFModel.__rebuildChildren = function(xfObj, xpCtx) {
	for (var jj = 0; jj < xfObj.__children.length; jj++) {
    	var kid = xfObj.__children[jj];
    	if (kid.rebuild) kid.rebuild();
    	if (kid.__xpCtx) kid.__xpCtx = xpCtx;
    	XFModel.__rebuildChildren(kid, xpCtx);
  	}
};

// uiParent = htmlParentNode
XForm.prototype.render = function(uiParent) {
    if (!this.__isInit) this.init();
    this.__uiParent = uiParent;
    var up = __xfControlUiParentInit(uiParent);
    var kids = this.__rootElem.childNodes;
    var xpCtx = {
        node : this.__defaultModel.__defaultInstance.__root,
        varRes : this.__xpVarRes};
    for (var i=0; i < kids.length; i++) {
        this.__descendForm(kids[i], xpCtx, up, this, null);
    }
    this.__defaultModel.__clearChanges();
    this.fireEvent("xforms-ready", this.__defaultModel);
    for (var modId in this.__models) {
        var mod = this.__models[modId];
        if (mod !== this.__defaultModel) {
            mod.__clearChanges();
            this.fireEvent("xforms-ready", mod);
        }
    }
};

XForm.prototype.setVarString = function(name, val) {
	if (!this.__xpVarRes) this.__xpVarRes = new XFExfVariableResolver();
	this.__xpVarRes.setVariableWithName(null, name, new XString(val));
};


XForm.prototype.setUriResolver = function(uriResolver) {
	this.__uriResolver = uriResolver;
};

XForm.prototype.resolveUri = function(uri) {
	var resUri = uri;
	if (this.__uriResolver) resUri = this.__uriResolver.resolveUri(uri);
	else if (typeof(App) != "undefined") {
		resUri = App.resolver.resolveUri(uri);
	}
	if (resUri.charAt(0) == "/") {
		if (this.__path.substring(0, 6) == "../fil") {
			resUri = "../fil" + resUri;
		}
	} else if (resUri.substring(0, 6) != "../fil") {
		resUri = this.__path + resUri;
	}
	// excel branch
	if (resUri.substring(0, 6) == "../fil") {
		var sq = Uri.splitQuery(resUri);
		if (Uri.ext(sq[0]).toUpperCase() == "XLS") {
			resUri = "../docs" + resUri.substring(6);
		}
	} 
	return resUri;
};

// xpCtx = instanceContextNode
XForm.prototype.__descendForm = function(node, xpCtx, uiParent, pObj, uiBefore) {
    if (node.nodeType == 1) {
        var ns = node.namespaceURI;
        if (ns == XFORM_NAMESPACE || ns == XFORM_EXTENDED_NAMESPACE
                || ns == XFORM_ITENSIL_NAMESPACE) {
            this.__doXformElem(node, xpCtx, uiParent, pObj, uiBefore);
        } else {
            var hp = uiParent;
            var pp = pObj;
            var np = null;
            if (ns == XFORM_XHTML_NAMESPACE || ns == XFORM_XHTML2_NAMESPACE) {
                np = pp = new XFHtml(this, node, pObj);
                if (xpCtx.position != null) {
                    pp.__repeatPos = xpCtx.position - 1;
                }
                hp = pp.render(uiParent, uiBefore, xpCtx);
            }
            var kids = node.childNodes;
            for (var i = 0; i < kids.length; i++) {
                this.__descendForm(kids[i], xpCtx, hp, pp, null);
            }
            if (np != null) np.__endRender();
        }
    } else if (node.nodeType == 3) {
        if (!pObj.__ignoreText)  {
            var txt = new XFText(this, node, pObj);
            txt.render(uiParent, uiBefore);
        }
    }
};

XForm.prototype.__doXformElem = function(elem, xpCtx, uiParent, pObj, uiBefore) {
    var ln = xmlGetLocalName(elem);
    var eClass = XForm.ELEMENT_CLASSES[ln];
    if (eClass != null) {
        var xe = new eClass(this, elem, pObj);
        if (xpCtx.position != null) {
            xe.__repeatPos = xpCtx.position - 1;
        }
        var hElem = xe.render(xpCtx, uiParent, uiBefore);
        var nXpCtx;
        if (xe.getNodeSet != null) {
            var ns = xe.getNodeSet(xpCtx);
            for (var i =0; i < ns.length; i++) {
                nXpCtx = {
                    node : ns[i], position : i + 1, size : ns.length,
                    varRes : xpCtx.varRes.makeChild() };
                this.__doXformBody(elem, nXpCtx, hElem, xe, null, true);
                xe.__endRender();
            }
        } else if (xe.getValueNode != null) {
            var n = xe.getValueNode(xpCtx);
            if (n == null || typeof n == "string") {
                nXpCtx = xpCtx;
            } else {
                nXpCtx = { node : n, varRes : xpCtx.varRes.makeChild() };
            }
            this.__doXformBody(elem, nXpCtx, hElem, xe, null, true);
            xe.__endRender();
        } else if (hElem != null) {
            this.__doXformBody(elem, xpCtx, hElem, xe, null, true);
            xe.__endRender();
        }
    }
};

XForm.prototype.__doXformBody = function (elem, xpCtx, uiParent, pObj, uiBefore, noSpeedUp) {
    var kids = elem.childNodes;
    
    // speed-up redraw temporarily unhooking parent node
    var befn, parn = null;
    if (!noSpeedUp) {
    	befn = uiParent.nextSibling;
    	parn = uiParent.parentNode;
   	 	if (parn) {
   	 		parn.removeChild(uiParent);
   	 		uiParent._parentNode = parn;
   	 	}
    }
    
    for (var i = 0; i < kids.length; i++) {
        this.__descendForm(kids[i], xpCtx, uiParent, pObj, uiBefore);
    }
    
    if (parn) { 
    	parn.insertBefore(uiParent, befn);
    	uiParent._parentNode = null;
    }
};

XForm.prototype.__addIxTemplate = function(name, tmpl) {
    this.__tmpls[name] = tmpl;
};

XForm.prototype.__removeIxTemplate = function(name) {
    delete this.__tmpls[name];
};

XForm.prototype.getIxTemplate = function(name, pObj) {
    var tmpl = this.__tmpls[name];
    if (!pObj) pObj = this; 
    return tmpl == null ? null : tmpl.getInstance(pObj);
};

XForm.prototype.remove = function() {
    this.__removing = true;
    for (var i = this.__children.length - 1; i >= 0; i--) {
        this.__children[i].remove();
    }
    this.dispose();
};

XForm.prototype.dispose = function() {
    if (!this.__removing) {
        for (var i = this.__children.length - 1; i >= 0; i--) {
            this.__children[i].dispose();
        }
    }
    if (this.__instances != null) {
        for (var id in this.__instances) {
            var ins = this.__instances[id];
            ins.__elem = null;
            ins.__root = null;
        }
        this.__instances = null;
    }
    XFormUI.prototype.dispose.apply(this, []);
    this.__uiParent = null;
    this.__rootElem = null;
    this.__cases  = null;
    this.__repeats = null;
    this.__tmpls = null;
    this.context = null;
    Utilities.clearAttributeNodes();
};

XForm.prototype.fireEvent = function(evtType, target, detail, extraProps) {
    if (this.__removing) return null;

    if (SH.debug) {
        SH.println(
            "XFEvent: " + evtType + " [" + target + "]" +
            (detail ? ", " + detail : ""));
    }

    var evt = new XFEvent(target);

    // lookup the bubble/cancel rules
    var proc = XForm.EVENT_PROCESS[evtType];
    if (proc == null) {
        proc = [true, true, false];
    }
    evt.initEvent(evtType, proc[0], proc[1]);
    evt.detail = detail;
    
    if (extraProps) objectExtend(evt, extraProps);

    var i;

    // build event tree
    var tree = [];
    var pObj = target.__parent;
    while (pObj != null) {
        if (pObj.dispatchEvent != null) {
            tree.push(pObj);
        }
        pObj = pObj.__parent;
    }

    // capture phase
    for (i = tree.length - 1; i >= 0; i--) {
        tree[i].dispatchEvent(evt);
        if (evt.__cancelled) break;
    }
    if (!evt.__cancelled) {
        evt.eventPhase = XFEvent.AT_TARGET;
        target.dispatchEvent(evt);
    }
    if (evt.bubbles && !evt.__cancelled) {
        evt.eventPhase = XFEvent.BUBBLING_PHASE;
        for (i = 0; i < tree.length; i++) {
            tree[i].dispatchEvent(evt);
            if (evt.__cancelled) break;
        }
    }
    if (proc[2]) {
        throw new Error("Fatal: [" + evtType + "] " + detail);
    }
    return evt;
};

/**
 * @return default model
 * @type XFModel
 */
XForm.prototype.getDefaultModel = function() {
    return this.__defaultModel;
};

XForm.prototype.__xPathSelect = function(xpCtx, query) {

    // Parse the XPath expression
    var xpath = this.__xpParser.parse(query);
    return this.__xPathSelectComp(xpCtx, xpath);
};

XForm.prototype.__xPathSelectComp = function(xpCtx, xpath) {

    // Create a context for the XPath to be evaluated in
    var context =
            new XPathContext(xpCtx.varRes, this.__xpNSRes, this.__xpFuncRes, this.__xpNodeEx);
    context.depList = xpCtx.depList;
    context.expressionContextNode = xpCtx.node;
    context.virtualPosition = xpCtx.position;
    context.virtualSize = xpCtx.size;

    // Evaluate the XPath expression
    return xpath.evaluate(context);
};

XForm.prototype.__xPathSelectOneComp = function(xpCtx, xpath) {
    var res = this.__xPathSelectComp(xpCtx, xpath);
    if (res.constructor === XNodeSet) {
        return res.first();
    } else {
        return res.stringValue();
    }
};

XForm.prototype.__xPathSelectOne = function(xpCtx, query) {
    var res = this.__xPathSelect(xpCtx, query);
    if (res.constructor === XNodeSet) {
        return res.first();
    } else {
        return res.stringValue();
    }
};


var XF_NO_CHANGE = 0;
var XF_TRUE_CHANGE = 1;
var XF_FALSE_CHANGE = 2;

XFModel.prototype = new XFEventModel();
XFModel.prototype.constructor = XFModel;

function XFModel(form, elem) {
    this.__initEvt(form, elem, form);
    this.context = form.context;
    this.__isInit = false;
    this.__defaultInstance = null;
    this.__submitResDoc = null;
    this.__bindings = [];
    this.__bindIds = new Object();
    this.__controls = 0;
    this.__changedNodes = [];
    this.__rbCtrls = [];
    this.__skipValueChange = null;
    this.__deps = new XFDepGraph(form.__xpNodeEx);
    this.__masterNodes = [];
    this.__submissions = new Object();
    this.__allConstrained = true;
    this.__isDirty = false;
    this.__requiredBinds = [];
    this.__typedBinds = [];
    this.__xpCacheObj = new Object();

    this.id = this.__id;

    if (form.__defaultModel == null) {
        form.__defaultModel = this;
    } else if (!this.id) {
        throw new Error("Additional xform:model without id");
    }
    if (this.id) {
        form.__models[this.id] = this;
    } else {
        this.id = "";
    }
}

XFModel.prototype.remove = function() {
    if (this.__form.__defaultModel == this) {
        this.__form.__defaultModel = null;
    }
    if (this.id != "") {
        this.__form.__models[this.id] = null;
    }
    XFEventModel.prototype.remove.apply(this, []);
};

XFModel.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    // don't dispose controls, controls may still be removing themselves
    this.__bindings = null;
    this.__bindIds = null;
    this.__form = null;
    this.__changedNodes = null;
    this.__rbCtrls = null;
    this.__skipValueChange = null;
    if (this.__deps) this.__deps.reset();
    this.__deps = null;
    this.__masterNodes = null;
    this.__submissions = null;
    this.__requiredBinds = null;
    this.__typedBinds = null;
    this.__defaultInstance = null;
    this.__submitResDoc = null;
    this.context = null;
};

XFModel.__loadSharedSrc = function(srcUri, xb) {
	if (XFModel.__sharedSrcs == null) 
		XFModel.__sharedSrcs = new Object();
	var doc = XFModel.__sharedSrcs[srcUri];
	if (doc == null) {
		doc = xb.loadURI(srcUri);
		XFModel.__sharedSrcs[srcUri] = doc;
	}
	return doc;
};

XFModel.clearLoadCache = function(srcUri) {
	if (!srcUri) XFModel.__sharedSrcs = null;
	if (XFModel.__sharedSrcs != null) {
		delete XFModel.__sharedSrcs[srcUri];
	}
};

XFModel.prototype.__init = function() {
    this.__form.fireEvent("xforms-model-construct", this);
    this.__isInit = true;
    var kids = this.__elem.childNodes;
    var id, src, j, jKids, locName, fUri;
    for (var i = 0; i < kids.length; i++) {
        var n = kids[i];
        if (n.nodeType == 1) {
            locName = xmlGetLocalName(n);
            var ns = n.namespaceURI;
            if (ns == XFORM_NAMESPACE) {
                if (locName == "instance") {
                    id = n.getAttribute("id");
                    if (id == "") id = null;
                    src = n.getAttribute("src");
                    var instObj = { __elem : n };
                    var impNode = null;
                    var idoc;
                    if (this.__defaultInstance == null && this.__form.__defInstSrc != null) {
                        fUri = this.__form.__defInstSrc;
                        if (fUri.charAt(0) != "/" && fUri.indexOf("://") < 0) {
                            fUri = this.__form.resolveUri(this.__form.__defInstSrc);
                        }
                        idoc = XFModel.__loadSharedSrc(fUri, this.__form.__xb);
                        instObj.__root = idoc ? idoc.documentElement : null;
                        instObj.__srcUri = fUri;
                        if (typeof(App) != "undefined") {
                            App.checkError(idoc);
                        }
                    } else if (src) {
                        if (__xfjavascriptUriRegEx.test(src)) {
                            var jsSrc = src.substring(src.indexOf("javascript:") + 11);
                            impNode = __xfEvalJavascript(this, jsSrc);
                            if (impNode.nodeType == 9) {
                            	instObj.__root = impNode.documentElement;
                            } else if (impNode.nodeType == 1) {
                            	instObj.__root =
                                	this.__nodeToNewDoc(impNode).documentElement;
                            } else throw new Error("Javascript src not an xml element or document.");
                        } else {
                            fUri = src;
                            if (fUri.charAt(0) != "/" && fUri.indexOf("://") < 0) {
                                fUri = this.__form.resolveUri(src);
                            }
                            idoc = XFModel.__loadSharedSrc(fUri, this.__form.__xb);
                            instObj.__root = idoc ? idoc.documentElement : null;
                            instObj.__srcUri = fUri;
                            if (typeof(App) != "undefined") {
                                App.checkError(idoc);
                            }
                        }
                    }
                    if (!instObj.__root) {
                        jKids = n.childNodes;
                        for (j = 0; j < jKids.length; j++) {
                            if (jKids[j].nodeType == 1) {
                                impNode = jKids[j];
                                break;
                            }
                        }
                        if (!impNode)
                        	throw new Error("Document missing for xform:instance");
                        	                 	
                        instObj.__root =
                            this.__nodeToNewDoc(impNode).documentElement;
                    }
                    if (this.__defaultInstance == null) {
                        this.__defaultInstance = instObj;
                    } else if (id == null
                            && this.__defaultInstance.__elem != n) {
                        throw new Error("Additional xform:instance without id");
                    }
                    if (id != null && this.__form.__instances[id] == null) {
                        this.__form.__instances[id] = instObj;
                    }
                } else if (locName == "bind") {
                    new XFBind(this, n);
                } else if (locName == "submission") {
                    var sb = new XFSubmission(this.__form, n, this);
                    if (sb.__id != null) {
                        this.__submissions[sb.__id] = sb;
                    }
                    this.__descendEvents(n, sb);
                } else {
                    var eClass = XForm.EVENT_CLASSES[locName];
                    if (eClass != null) {
                        var xe = new eClass(this.__form, n, this);
                        var xpCtx = new Object();
                        xpCtx.node = this.__defaultInstance == null ? null :
                                this.__defaultInstance.__root;
                        xpCtx.varRes = this.__form.__xpVarRes;
                        xe.render(xpCtx, null);
                        this.__descendEvents(n, xe);
                    }
                }
            } else if (ns == XFORM_XHTML_NAMESPACE || ns == XFORM_XHTML2_NAMESPACE) {

                if (locName == "script") {
                    var js = new XFHtml(this.__form, n, this);
                    var xpCtx = new Object();
                    xpCtx.node = this.__defaultInstance == null ? null :
                            this.__defaultInstance.__root;
                    xpCtx.varRes = this.__form.__xpVarRes;
                    js.render(null, null, xpCtx);
                }
            } else if (ns == XFORM_ITENSIL_NAMESPACE) {
            	if (locName == "ruleset") {
            		if (typeof(Rules) != "undefined") {
            			var irul = new IXRuleSet(this, n);
            			if (this.__defaultRules == null) {
            				this.__defaultRules = irul;
            			}
            			var id = irul.getId();
            			if (id) this.__rules[id] = irul;
            		}
            	}
            }
            // else if XSD
        }
    }
    this.__doRebuild(true);
    this.__doRecalculate(true);
    this.__doRevalidate();
    this.__isDirty = false;
    this.__form.fireEvent("xforms-model-construct-done", this);
};

XFModel.prototype.__descendEvents = function(elem, pObj) {
    var kids = elem.childNodes;
    var locName;
    for (var i = 0; i < kids.length; i++) {
        var n = kids[i];
        var ns = n.namespaceURI;
        if (n.nodeType == 1 && (ns== XFORM_NAMESPACE || ns == XFORM_ITENSIL_NAMESPACE
        		 || xmlGetLocalName(n) == "script"))  {
            locName = xmlGetLocalName(n);
            var eClass = XForm.EVENT_CLASSES[locName];
            if (eClass != null) {
                var xe = new eClass(this.__form, n, pObj);
                var xpCtx = new Object();
                xpCtx.node = this.__defaultInstance == null ? null :
                        this.__defaultInstance.__root;
                xpCtx.varRes = this.__form.__xpVarRes;
                if (xe.constructor === XFHtml) {
                    xe.render(null, null, xpCtx);
                } else {
                    xe.render(xpCtx, null);
                }
                this.__descendEvents(n, xe);
            }
        }
    }
};

XFModel.prototype.__checkInit = function() {
    if (!this.__isInit) this.__init();
};

XFModel.prototype.getIdContext = function() {
    if (this.context != null && this.context.getIdContext != null) {
        return this.context.getIdContext();
    } else {
        if (this.__idCtx == null) this.__idCtx = new XmlId();
        return this.__idCtx;
    }
};

XFModel.prototype.getTypeInfo = function(node) {
    var typer = this.__form.__typer;
    for (var i = 0; i < this.__typedBinds.length; i++) {
        var bind = this.__typedBinds[i];
        if (arrayFind(bind.__getNodeSet(), node) >= 0) {
            var inf = typer.getTypeInfo(bind.__type, bind.__elem);
            if (inf == null) return __xfDefaultXsdTypeInfo;
            return inf;
        }
    }
    return __xfDefaultXsdTypeInfo;
};

// currently limited to before init
XFModel.prototype.setDefaultInstanceSrc = function(src) {
    // seek <xf:instance>
    var kids = this.__elem.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var nn = kids[ii];
        if (nn.nodeType == 1) {
            var locName = xmlGetLocalName(nn);
            if (nn.namespaceURI == XFORM_NAMESPACE) {
                if (locName == "instance") {
                    nn.setAttribute("src", src);
                    return;
                }
            }
        }
    }
};

XFModel.prototype.getDefaultInstance = function() {
    return this.__defaultInstance.__root;
};

XFModel.prototype.setDefaultInstance = function(impNode) {
    // seek <xf:instance>
    this.__setInstance(impNode, this.__defaultInstance);
};

XFModel.prototype.__setInstance = function(impNode, instObj) {
   	this.__allDocChange(instObj.__root.ownerDocument);
	instObj.__root = this.__nodeToNewDoc(impNode).documentElement;
};

XFModel.prototype.__allDocChange = function(oDoc) {
	__xfValChangeDeep(this, oDoc.documentElement);
};

XFModel.prototype.setInstanceId = function(id, impNode) {
	var instObj = this.__form.__instances[id];
	if (instObj != null) {
		this.__setInstance(impNode, instObj);
	}
};

XFModel.prototype.reloadInstanceId = function(id) {
	var instObj = this.__form.__instances[id];
	if (instObj != null) {
		if (instObj.__srcUri) {
			XFModel.clearLoadCache(instObj.__srcUri);
			var idoc = XFModel.__loadSharedSrc(instObj.__srcUri, this.__form.__xb);
			this.__allDocChange(instObj.__root.ownerDocument);		
   			instObj.__root = idoc ? idoc.documentElement : null;
   			if (typeof(App) != "undefined") {
		        App.checkError(idoc);
		    }
		}
	}
};

XFModel.prototype.setInstanceIdSrc = function(id, src) {
	var instObj = this.__form.__instances[id];
	if (instObj != null) {
		if (instObj.__elem) {
			instObj.__elem.setAttribute("src", src);
		} else if (instObj.__src == src) {
			// unchanged
			return;
		}
		var xc = __xfNodeChange(this, instObj.__root);
		xc[0] = XF_TRUE_CHANGE; // valChange
	} else {
		instObj = { __elem : null, __src : src };
		this.__form.__instances[id] = instObj;
		if (SH.debug) {
			SH.println("Creating new instance: " + id);
		}
	}

	var fUri = src;
    if (fUri.charAt(0) != "/" && fUri.indexOf("://") < 0) {
        fUri = this.__form.resolveUri(src);
    }
    var idoc = XFModel.__loadSharedSrc(fUri, this.__form.__xb);
    instObj.__root = idoc ? idoc.documentElement : null;
    instObj.__srcUri = fUri;
    if (typeof(App) != "undefined") {
        App.checkError(idoc);
    }
};

XFModel.prototype.getInstancePath = function(node) {
	var instObj = this.__defaultInstance;
	if (node != null && node.ownerDocument !== instObj.__root.ownerDocument) {
		for (var iid in this.__form.__instances) {
			var oinst = this.__form.__instances[iid];
			if (node.ownerDocument === oinst.__root.ownerDocument) {
				instObj = oinst;
				break;
			}
		}
	}
	if (this.__form.__defPath) {
		return this.__form.__defPath;
	}

	if (instObj === this.__defaultInstance && this.__form.__defInstSrc) {
		var defSrc = this.__form.__defInstSrc;
	 	return (defSrc.substring(0,3) == "../") ? Uri.parent(defSrc) : Uri.parent(this.__form.resolveUri(defSrc));
	}
	var src = instObj.__elem.getAttribute("src");
	var fUri = this.__form.__path;
	if (src && !(src.indexOf("://") > 0 || src.indexOf("javascript:") == 0)) {
		fUri = Uri.parent(this.__form.resolveUri(src));
	}	
	return fUri;
};

// true if really changed
XFModel.prototype.__setNodeValue = function(node, val) {
    if (SH.debug) SH.println("sv: " + node.nodeName);
    var origVal = xmlStringForNode(node);
    if (origVal != val) {
        xmlSetNodeValue(node, val);
        var xc = __xfNodeChange(this, node);
        xc[0] = XF_TRUE_CHANGE; // valChange
        this.__isDirty = true;
        return true;
    }
    return false;
};

XFModel.prototype.markChanged = function(node, /* Optional String */attName) {
    if (attName != null) {
        node = this.__form.__xpNodeEx.getAttribute(node, node.getAttributeNode(attName));
        node.syncValue();
    }
    var xc = __xfNodeChange(this, node);
    xc[0] = XF_TRUE_CHANGE; // valChange
    this.__isDirty = true;
    this.__form.fireEvent("xforms-value-changed", this);
};

// a clone of insNode will be inserted
XFModel.prototype.__insertNode = function(ns, insNode, at, before) {
    var p = ns[0].parentNode;
    if (before) at--;
    if (at < 0) at = 0;
    else if (at >= ns.length) at = ns.length - 1;
    var sib = ns[at];

    var xc;
    xc = __xfNodeChange(this, p);
    xc[0] = XF_TRUE_CHANGE;

    // mark the sib as dirty...
    //xc = __xfNodeChange(this, sib);
    //xc[0] = XF_TRUE_CHANGE; // valChange

    if (!before && (at == (ns.length - 1))) {
        sib = null;
    }
    var nn = Xml.cloneNode(insNode, true);
    xc = __xfNodeChange(this, nn);
    xc[0] = XF_TRUE_CHANGE; // valChange
    p.insertBefore(nn, sib);
    if (sib != null) {
        var nxt = sib.nextSibling;
        while (nxt != null) {
            if (nxt.nodeType == 1) {
                xc = __xfNodeChange(this, nxt);
                xc[0] = XF_TRUE_CHANGE; // valChange
            }
            nxt = nxt.nextSibling;
        }
    }
    this.__isDirty = true;
};

XFModel.prototype.__deleteNode = function(ns, at, emptyNode) {
    at--;
    if (at < 0) return;
    if (at >= ns.length) return;
    var p = ns[0].parentNode;
    var n = ns[at];
    var xc = __xfNodeChange(this, n);
    xc[0] = XF_TRUE_CHANGE; // valChange

    xc = __xfNodeChange(this, p);
    xc[0] = XF_TRUE_CHANGE;

    var nxt = n.nextSibling;
    if (ns.length == 1) {
        p.removeChild(n);
        if (emptyNode != null) {
            var nn = emptyNode.cloneNode(true);
            p.appendChild(nn);
            xc = __xfNodeChange(this, nn);
            xc[0] = XF_TRUE_CHANGE; // valChange
        }
    } else {
        p.removeChild(n);
        while (nxt != null) {
            if (nxt.nodeType == 1) {
                __xfValChangeDeep(this, nxt);
            }
            nxt = nxt.nextSibling;
        }
    }
    this.__isDirty = true;
};


XFModel.prototype.__destroyNode = function(node) {
    if (node == null) {
        return;
    }
    var pn = node.parentNode;
    var xc = __xfNodeChange(this, node);
    xc[0] = XF_TRUE_CHANGE; // valChange

    xc = __xfNodeChange(this, pn);
    xc[0] = XF_TRUE_CHANGE;

    var nxt = node.nextSibling;
    pn.removeChild(node);
    while (nxt != null) {
        if (nxt.nodeType == 1) {
            __xfValChangeDeep(this, nxt);
        }
        nxt = nxt.nextSibling;
    }
    this.__isDirty = true;
    this.__form.fireEvent("xforms-destroy", this);
};

XFModel.prototype.__duplicateNode = function(dstParent, srcNode, befNode) {
    var nn;
    if (dstParent.ownerDocument === srcNode.ownerDocument) {
        nn = Xml.cloneNode(srcNode, true);
        dstParent.insertBefore(nn, befNode);
    } else {
        nn = Xml.nodeImport(dstParent.ownerDocument, dstParent, srcNode, befNode);
    }

    var xc = __xfNodeChange(this, nn);
    xc[0] = XF_TRUE_CHANGE; // valChange

    xc = __xfNodeChange(this, dstParent);
    xc[0] = XF_TRUE_CHANGE;

    if (befNode == null) {
        //var prev = nn.previousSibling;
        //while (prev != null && prev.nodeType != 1 /* ELEMENT */) {
        //    prev = prev.previousSibling;
        //}
        //if (prev != null) {
        //   xc = __xfNodeChange(this, prev);
        //    xc[0] = XF_TRUE_CHANGE; // valChange
        //}
    } else {
        var nxt = befNode;
        while (nxt != null) {
            if (nxt.nodeType == 1) {
                __xfValChangeDeep(this, nxt);
            }
            nxt = nxt.nextSibling;
        }
    }
    this.__isDirty = true;
    this.__form.fireEvent("xforms-duplicate", this);
    return nn;
};

XFModel.prototype.__replaceNodesBound = function(node, boundObjs) {
    var doc = node.ownerDocument;
    var ns = [];
    var xc;
    var i;
    for (i = 0; i < boundObjs.length; i++) {
        var bo = boundObjs[i];
        ns.push(bo.getValueNode(bo.__xpCtx));
    }
    var kid = node.firstChild;
    var changed = false;

    // clean out old values
    while (kid != null) {
        var idx = xmlNodeEqualInSet(kid, ns);
		var nxt = kid.nextSibling;
        if (idx >= 0) {
            ns.splice(idx, 1);
        } else {
            changed = true;
            __xfValChangeDeep(this, kid);
            node.removeChild(kid);
        }
        kid = nxt;
    }
    for (i = 0; i < ns.length; i++) {
        var xni = xmlNodeImport(doc, node, ns[i]);
        xc = __xfNodeChange(this, xni);
        xc[0] = XF_TRUE_CHANGE; // valChange
        changed = true;
    }
    if (changed) {
        xc = __xfNodeChange(this, node);
        xc[0] = XF_TRUE_CHANGE;
    }
    this.__isDirty = true;
    return changed;
};

XFModel.prototype.__getNodeChange = function(node) {
	var cns = this.__changedNodes;
    for (var i = 0; i < cns.length; i += 2) {
        var nc = cns[i];
        if (nc === node) {
            return cns[i + 1];
        }
    }
    return null;
};


XFModel.prototype.__addControl = function(ctrl) {
    return this.__form.__name + "_" + this.id + "_" + this.__controls++;
};

XFModel.prototype.__doRebuild = function(skipClean) {
    this.__form.fireEvent("xforms-rebuild", this);
    if (!skipClean) {

        // clear all bind cache
        var i;
        for (i = 0; i < this.__bindings.length; i++) {
            var bind = this.__bindings[i];
            bind.__rebuild();
            if (bind.__bindings.length > 0) {
                this.__doSubRebuild(bind);
            }
        }
        var cns = this.__changedNodes;
        for ( i = 0; i < cns.length; i += 2) {
            var cNode = cns[i];
            var nc = cns[i + 1];
            if (nc[0] != XF_NO_CHANGE) {
                if (SH.debug) SH.println("Rebuild: " + cNode.nodeName);
                var cDeps = this.__deps.getDepends(cNode);
                if (cDeps == null) continue;
                // backwards cause deps will shrink
                for (var j = cDeps.length - 1; j >= 0 ; j--) {
                    var ctrl = cDeps[j];
                    if (ctrl == null || ctrl === this.__skipValueChange) continue;
                    if (arrayFindStrict(this.__rbCtrls, ctrl) < 0) {
                        this.__rbCtrls.push(ctrl);
                    }
                    ctrl.rebuild();
                }
            }
        }
        for (i = this.__deps.nullDeps.length - 1; i >= 0; i--) {
            var ectl = this.__deps.nullDeps[i];
            if (arrayFindStrict(this.__rbCtrls, ectl) < 0) {
                this.__rbCtrls.push(ectl);
            }
            ectl.rebuild();
        }
    }
};

XFModel.prototype.__doSubRebuild = function(bind) {
    for (var j = 0; j < bind.__bindings.length; j++) {
        var subBind = bind.__bindings[j];
        subBind.__rebuild();
        if (subBind.__bindings.length > 0) {
            this.__doSubRebuild(subBind);
        }
    }
};

XFModel.prototype.__doRecalculate = function(skipControls) {
    this.__form.fireEvent("xforms-recalculate", this);

    // loop/recurse binds
    var xpCtx = { node : this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes };
    var i;
    for (i = 0; i < this.__bindings.length; i++) {
        var bind = this.__bindings[i];
        bind.__doCalculate(xpCtx);
        if (bind.__bindings.length > 0) {
            this.__doSubRecalculate(bind, xpCtx);
        }
    }
};

XFModel.prototype.__doSubRecalculate = function(bind, xpCtx) {
    var ns = bind.__getNodeSet(xpCtx);
    if (ns.length > 0) {
        var nXpCtx = {node : ns[0], varRes : this.__form.__xpVarRes};
        for (var j = 0; j < bind.__bindings.length; j++) {
            var subBind = bind.__bindings[j];
            subBind.__doCalculate(nXpCtx);
            if (subBind.__bindings.length > 0) {
                this.__doSubRecalculate(subBind, nXpCtx);
            }
        }
    }
};

XFModel.prototype.__doRevalidate = function(forceCheck) {
    this.__form.fireEvent("xforms-revalidate", this);
    this.__allConstrained = true;
    this.__requiredBinds = [];

    // loop/recurse binds
    var i;
    for (i = 0; i < this.__bindings.length; i++) {
        var bind = this.__bindings[i];
        bind.__revalidate(forceCheck);
    }
};

XFModel.prototype.isValid = function() {
	this.__doRevalidate(true);
	return !this.__checkInvalid();
};


XFModel.prototype.__recalcEmptySetBinds = function() {
    for (var ii = 0; ii < this.__bindings.length; ii++) {
        var bind = this.__bindings[ii];
        if (bind.__ns && bind.__ns.length == 0) {
        	bind.__recalcEmpty();
        }
    }
};

XFModel.prototype.__checkInvalid = function() {
    if (this.__allConstrained) {

        // check requireds
        for (var r = 0; r < this.__requiredBinds.length; r++) {
            var rb = this.__requiredBinds[r];
            var ns = rb.__getNodeSet(rb.__xpCtx);
            for (var i = 0; i < ns.length; i++) {
                if (xmlStringForNode(ns[i]) == "") {
                    return true;
                }
            }
        }
        return false;
    }
    return true;
};

XFModel.prototype.submit = function(subId) {
    var sb = this.__submissions[subId];
    if (sb != null) {
        sb.__submit();
        this.__isDirty = false;
    } else {
        throw new Error("xform:submission not found for id: " + subId);
    }
};

// Returns a xml doc (if available)
XFModel.prototype.getSubmitResponse = function() {
	return this.__submitResDoc;
};

XFModel.prototype.__doRefresh = function() {
    this.__form.fireEvent("xforms-refresh", this);

    var i, ctrl;
    var visited = [];
    var nc, vn;

    var rbCtrls = this.__rbCtrls;
    var skipValueChange = this.__skipValueChange;
    var cns = this.__changedNodes;

    this.__clearChanges();

    for ( i = 0; i < cns.length; i += 2) {
        var cNode = cns[i];
        nc = cns[i + 1];
        var cDeps = this.__deps.getDepends(cNode);
        if (cDeps == null) continue;
        for (var j = 0; j < cDeps.length; j++) {
            ctrl = cDeps[j];
            if (ctrl.getValueNode != null && !ctrl.isValue) {
                vn = ctrl.getValueNode(ctrl.__xpCtx);
                // only the direct node gets state events
                if (vn === cNode) {
                    if (nc[2] != XF_NO_CHANGE) {
                       ctrl.setRequired(nc[2] == XF_TRUE_CHANGE);
                    }
                    if (nc[1] != XF_NO_CHANGE) {
                       ctrl.setEnabled(nc[1] == XF_TRUE_CHANGE);
                    }
                    if (nc[4] != XF_NO_CHANGE) {
                       ctrl.setValid(nc[4] == XF_TRUE_CHANGE);
                    }
                    if (nc[3] != XF_NO_CHANGE) {
                       ctrl.setReadonly(nc[3] == XF_TRUE_CHANGE);
                    }
                }
            }
            if (nc[0] != XF_NO_CHANGE && ctrl != skipValueChange) {
                if (arrayFindStrict(visited, ctrl) >= 0) continue;
                visited.push(ctrl);
                if (ctrl.getValueNode != null && !ctrl.isValue) {
                    vn = ctrl.getValueNode(ctrl.__xpCtx);
                    if (vn === cNode) ctrl.valueChanged();
                } else {
                    ctrl.valueChanged();
                }
            }
        }
    }
    for (i = 0; i < rbCtrls.length; i++) {
        ctrl = rbCtrls[i];
        if (arrayFindStrict(visited, ctrl) >= 0) continue;
        if (ctrl.getValueNode != null && !ctrl.isValue) {
            vn = ctrl.getValueNode(ctrl.__xpCtx);
            ctrl.valueChanged();
        } else {
            ctrl.valueChanged();
        }
    }
};

/**
 * Send an event to the controls tied to a dataNode
 */
XFModel.prototype.sendDataEvent = function(dataNode, evtType, detail) {
	var evt = new XFEvent(this);
    evt.initEvent(evtType, false, false);
    evt.detail = detail;
    evt.eventPhase = XFEvent.AT_TARGET;
	var cDeps = this.__deps.getDepends(dataNode);
	var visited = [];
	var ctrl, vn;
	if (cDeps) {
		
		for (var ii=0; ii < cDeps.length; ii++) {
			var ctrl = cDeps[ii];
			if (arrayFindStrict(visited, ctrl) >= 0) continue;
			visited.push(ctrl);
			if (ctrl.dispatchEvent && ctrl.getValueNode && !ctrl.isValue) {
				evt.target = ctrl;
				vn = ctrl.getValueNode(ctrl.__xpCtx);
              	if (vn === dataNode) ctrl.dispatchEvent(evt);
			}
		}
	}
};

XFModel.prototype.__clearChanges = function() {
    this.__rbCtrls = [];
    this.__changedNodes = [];
    this.__skipValueChange = null;
};

XFModel.prototype.__registerMasterNode = function(xfNsObj) {
    var ns = xfNsObj.getNodeSet(xfNsObj.__xpCtx);
    if (ns.length < 1) {
        if (xfNsObj.constructor === XFInsert) {
            this.__form.fireEvent("xforms-binding-exception",
                this, "insert nodeset refers to empty set");
        }
        return null;
    }
    var n = ns[0];
    var idx = arrayFind(this.__masterNodes, n.parentNode);
    var mTypes;
    if (idx < 0) {
        mTypes = new Object();
        mTypes[n.nodeName] = n.cloneNode(true);
        this.__masterNodes.push(n.parentNode);
        this.__masterNodes.push(mTypes);
    } else {
        mTypes = this.__masterNodes[idx + 1];
        if (!(n.nodeName in mTypes)) {
            mTypes[n.nodeName] = n.cloneNode(true);
        }
    }
    return mTypes[n.nodeName];
};

XFModel.prototype.__getMasterNode = function (node) {
    var idx = arrayFind(this.__masterNodes, node.parentNode);
    if (idx < 0) {
        return null;
    } else {
        return this.__masterNodes[idx + 1][node.nodeName];
    }
};


// API
XFModel.prototype.getInstanceDocument = function(instanceID) {
    var insObj = this.__form.__instances[instanceID];
    if (insObj == null) {
        return null;
    }
    return insObj.__root.ownerDocument;
};

XFModel.prototype.rebuild = function() {
    this.__doRebuild();
    this.__doRecalculate();
    this.__doRevalidate();
    this.__doRefresh();
};

XFModel.prototype.recalculate = function() {
    this.__doRecalculate();
    this.__doRevalidate();
    this.__doRefresh();
};

XFModel.prototype.revalidate = function() {
    this.__doRevalidate();
    this.__doRefresh();
};

XFModel.prototype.refresh = function() {
    this.__doRefresh();
};

XFModel.prototype.__xpathCache = function(xpath) {
    var xpComp = this.__xpCacheObj[xpath];
    if (xpComp == null) {
        xpComp = this.__form.__xpParser.parse(xpath);
        this.__xpCacheObj[xpath] = xpComp;
    }
    return xpComp;
};

XFModel.prototype.getValue = function(xpath, contextNode) {
    var xpCtx = {
        node : contextNode || this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes};
    var res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(xpath));
    return res ? res.stringValue() : null;
};

XFModel.prototype.getFormattedValue = function(node) {
    var inf = this.getTypeInfo(node);
    var fmt = XFTypeFormat.getFormat(inf.namespace, inf.type);
    var val = xmlStringForNode(node);
    if (this.__form.__typer.isValidInf(val,inf)) {
        return fmt.format(val);
    } else {
        return val;
    }
};

XFModel.prototype.selectNodeList = function(xpath, contextNode) {
    var xpCtx = {
        node : contextNode || this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes};
    var res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(xpath));
    if (res.constructor === XNodeSet) {
        return res.toArray();
    }
    return null;
};

XFModel.prototype.setValue = function(xpath, value, contextNode) {
    var xpCtx = {
        node : contextNode || this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes};
    var pXP = this.__xpathCache(xpath);
    var res = this.__form.__xPathSelectComp(xpCtx, pXP);
    if (res.constructor === XNodeSet && res.booleanValue()) {
        this.__setNodeValue(res.first(), value);
        return;
    } else if (this.__form.autoGenRefs) {
    	var ns = XFSingleNodeBinding.autoGen(pXP, xpCtx.node, this.__form);
    	if (ns.length > 0) {
    		this.__setNodeValue(ns[ns.length - 1], value);
    		return;
    	}
    }
    throw new Error("XPath does not evaluate to a node or nodeset");
};

XFModel.prototype.__moveNode = function(srcNode, dstParent, beforeNode) {
    if (dstParent == null) dstParent = this.__defaultInstance.__root;
    // copy-delete if cross document
    if (srcNode.ownerDocument !== dstParent.ownerDocument) {
    	this.__duplicateNode(dstParent, srcNode, beforeNode);
    	this.__destroyNode(srcNode);
    	return;
    }
    dstParent.insertBefore(srcNode, beforeNode);
    var xc = __xfNodeChange(this, srcNode);
    xc[0] = XF_TRUE_CHANGE; // valChange

    xc = __xfNodeChange(this, dstParent);
    xc[0] = XF_TRUE_CHANGE;

    if (beforeNode != null) {
        var nxt = beforeNode;
        while (nxt != null) {
            if (nxt.nodeType == 1) {
                __xfValChangeDeep(this, nxt);
            }
            nxt = nxt.nextSibling;
        }
    }
    this.__isDirty = true;
};

XFModel.prototype.moveNode = function(srcXpath, dstXpath, beforeXpath, contextNode) {
    var xpCtx = {
        node : contextNode || this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes};
    var res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(srcXpath));
    var srcNode, dstParent, beforeNode = null;
    if (res.constructor === XNodeSet && res.booleanValue()) {
        srcNode = res.first();
    } else {
        throw new Error("XPath does not evaluate to a node or nodeset");
    }
    res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(dstXpath));
    if (res.constructor === XNodeSet && res.booleanValue()) {
        dstParent = res.first();
    } else {
        throw new Error("XPath does not evaluate to a node or nodeset");
    }
    if (beforeXpath != null) {
        res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(beforeXpath));
        if (res.constructor === XNodeSet && res.booleanValue()) {
            beforeNode = res.first();
        }
    }
    this.__moveNode(srcNode, dstParent, beforeNode);
};

XFModel.prototype.duplicateNode = function(srcXpath, dstXpath, beforeXpath, contextNode) {
    var xpCtx = {
        node : contextNode || this.__defaultInstance.__root,
        varRes : this.__form.__xpVarRes};
    var res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(srcXpath));
    var srcNode, dstParent, beforeNode = null;
    if (res.constructor === XNodeSet && res.booleanValue()) {
        srcNode = res.first();
    } else {
        throw new Error("XPath \"" + srcXpath + "\" does not evaluate to a node or nodeset");
    }
    res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(dstXpath));
    if (res.constructor === XNodeSet && res.booleanValue()) {
        dstParent = res.first();
    } else {
        throw new Error("XPath does not evaluate to a node or nodeset");
    }
    if (beforeXpath != null) {
        var nXpCtx = { node : dstParent,
                    position : xpCtx.position,
                    size : xpCtx.size,
                    varRes : xpCtx.varRes };
        res = this.__form.__xPathSelectComp(nXpCtx, this.__xpathCache(beforeXpath));
        if (res.constructor === XNodeSet && res.booleanValue()) {
            beforeNode = res.first();
        }
    }
    return this.__duplicateNode(dstParent, srcNode, beforeNode);
};

XFModel.prototype.destroyNode = function(xpath, contextNode) {
	var xpCtx = {
        node : contextNode || this.__defaultInstance,
        varRes : this.__form.__xpVarRes};
    var res = this.__form.__xPathSelectComp(xpCtx, this.__xpathCache(xpath));
    var node = null;
    if (res.constructor === XNodeSet && res.booleanValue()) {
        node = res.first();
    }
    this.__destroyNode(node);
};

XFModel.prototype.activateCase = function(id) {
	var cs = this.__form.__cases[id];
    if (cs != null) {
        cs.activate();
    }
};

XFModel.prototype.__nodeToNewDoc = function(impNode) {
    var insDoc = this.__form.__xb.createWithRoot(
            impNode.nodeName, impNode.namespaceURI);
    Xml.copyElementAttrs(impNode, insDoc.documentElement);
    Xml.importNodes(insDoc, insDoc.documentElement, impNode.childNodes);
    return insDoc;
};

XFModel.prototype.getUiId = function(xfId) {
	return this.__form.__name + "_" + xfId;
};

XFModel.prototype.getUiElementById = function(xfId) {
	return document.getElementById(this.getUiId(xfId));
};

XFModel.prototype.setElementAttribute = function(ownerElement, attName, attValue) {
	var attr = this.__form.__xpNodeEx.getAttributeByName(ownerElement, attName);
	Xml.setNodeValue(attr, attValue);
};

XFModel.prototype.getForm = function() {
	return this.__form;
};

XFModel.prototype.getRules = function(ruleId, subId) {
	var irul = ruleId ? this.__rules[ruleId] : this.__defaultRules;
	var rules = null;
	if (irul) {
		rules = irul.getRules(subId);
	}
	return rules;
};

XFBind.prototype = new XFEventModel();
XFBind.prototype.constructor = XFBind;

function XFBind(model, elem, parentBind) {
    var form = model.__form;
    this.__initEvt(form, elem, parentBind || model);
    this.__model = model;
    this.__bindings = [];
    if (parentBind == null) {
        model.__bindings.push(this);
    } else {
        parentBind.__bindings.push(this);
    }
    this.id = this.__id;
    if (this.id) {
        model.__bindIds[this.id] = this;
    }
    this.__nodeset = elem.getAttribute("nodeset");
    if (!this.__nodeset) {
        form.fireEvent(
                "xforms-binding-exception", model, "bind missing @nodeset");
    } else {
        // if there's a predicate, function or variable
        this.__dynBind = (this.__nodeset.indexOf("[") > 0 ||
            this.__nodeset.indexOf("(") > 0 ||
            this.__nodeset.indexOf("$") >= 0);
    }
    try {
        this.__nodesetXP = form.__xpParser.parse(this.__nodeset);
        this.__type = elem.getAttribute("type");
        if (this.__type) model.__typedBinds.push(this);
        this.__readonly = elem.getAttribute("readonly");
        if (this.__readonly) {
            this.__readonlyXP = form.__xpParser.parse(this.__readonly);
        }
        this.__required = elem.getAttribute("required");
        if (this.__required) {
            this.__requiredXP = form.__xpParser.parse(this.__required);
        }
        this.__relevant = elem.getAttribute("relevant");
        if (this.__relevant) {
            this.__relevantXP = form.__xpParser.parse(this.__relevant);
        }
        this.__calculate = elem.getAttribute("calculate");
        if (this.__calculate) {
            this.__calculateXP = form.__xpParser.parse(this.__calculate);
        }
        this.__constraint = elem.getAttribute("constraint");
        if (this.__constraint) {
            this.__constraintXP = form.__xpParser.parse(this.__constraint);
        }
    } catch (e) {
        form.fireEvent("xforms-compute-exception", model, e);
    }
    var kids = elem.childNodes;
    for (var i = 0; i < kids.length; i++) {
        var n = kids[i];
        if (n.nodeType == 1) {
            if (n.namespaceURI == XFORM_NAMESPACE) {
                var locName = xmlGetLocalName(n);
                if (locName == "bind") {
                    new XFBind(model, n, this);
                }
            }
        }
    }
}

XFBind.prototype.__getNodeSet = function(xpCtx) {
    if (this.__ns == null) {
        var form = this.__model.__form;
        var resExp;
        try {
            var nXpCtx = xpCtx || this.__xpCtx;
            resExp = form.__xPathSelectComp(nXpCtx, this.__nodesetXP);
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        if (resExp.constructor !== XNodeSet) {
            form.fireEvent("xforms-binding-exception", this);
        }
        this.__ns = resExp.toArray();
    }
    return this.__ns;
};

XFBind.prototype.__doCalculate = function (xpCtx) {

    this.__xpCtx = xpCtx;
    if (this.__calculate) {
        var form = this.__model.__form;
        var resExp;
        var ns = this.__getNodeSet(xpCtx);
        var nXpCtx = new Object();
        nXpCtx.size = ns.length;
        try {
            for (var i = 0; i < ns.length; i++) {
                var n = ns[i];
                nXpCtx.node = n;
                nXpCtx.varRes = form.__xpVarRes;
                nXpCtx.position = i + 1;
                resExp = form.__xPathSelectComp(nXpCtx, this.__calculateXP);
                this.__model.__setNodeValue(n, resExp.stringValue());
            }
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
    }
};

XFBind.prototype.__revalidate = function(forceCheck) {

    /*if (this.__lastReadonly == null) this.__lastReadonly = [];
    if (this.__lastRelevant == null) this.__lastRelevant = [];
    if (this.__required != null && this.__lastRequired == null)
        this.__lastRequired = [];
    if ((this.__constraint != null || this.__type != null)
            && (this.__lastConstraint == null || forceCheck)) this.__lastConstraint = [];*/

    var kidsRelevant = true; // Inheritance logical AND
    var kidsReadonly = false; // Inheritance logical OR
    for (var j = 0; j < this.__bindings.length; j++) {
        var bits = this.__bindings[j].__revalidate(forceCheck);
        kidsRelevant = bits[0] && kidsRelevant;
        kidsReadonly = bits[1] || kidsReadonly;
    }

    var model = this.__model;
    var form = model.__form;
    var resExp;
    var ns = this.__getNodeSet(this.__xpCtx);
    var nXpCtx = new Object();
    var bool;
    nXpCtx.size = ns.length;
    try {
        for (var i = 0; i < ns.length; i++) {
            var isRelevant = kidsRelevant;
            var isReadonly = kidsReadonly;
            var n = ns[i];
            nXpCtx.node = n;
            nXpCtx.position = i + 1;
            nXpCtx.varRes = form.__xpVarRes;
            var xc = model.__getNodeChange(n);
            
            // get and init lasts
            var last = form.__xpNodeEx.getProp(n, "_lb");
            var hasLast = true;
            if (!last) {
            	last = new Object();
            	form.__xpNodeEx.setProp(n, "_lb", last);
            	hasLast = false;
            } else if ((this.__constraint || this.__type) && forceCheck) {
		    	delete last.constraint;
            }

            if (isRelevant && this.__relevant) {
                resExp = form.__xPathSelectComp(nXpCtx, this.__relevantXP);
                isRelevant = resExp.booleanValue();
            } else {
                isRelevant = true;
            }
            if (!hasLast || last.relevant !== isRelevant) {
                last.relevant = isRelevant;
                if (hasLast || (!hasLast && !isRelevant)) {
                    if (xc == null) xc = __xfNodeChange(model, n);
                    // relevant
                    xc[1] = isRelevant ? XF_TRUE_CHANGE : XF_FALSE_CHANGE;
                }
            }

            if (this.__required) {
                resExp = form.__xPathSelectComp(nXpCtx, this.__requiredXP);
                bool = resExp.booleanValue();
                if (last.required !== bool) {
                    last.required = bool;
                    if (xc == null) xc = __xfNodeChange(model, n);
                    // required
                    xc[2] = bool ? XF_TRUE_CHANGE : XF_FALSE_CHANGE;
                }
                if (bool) {
                    model.__requiredBinds.push(this);
                }
            }

            if (!isReadonly && this.__readonly) {
                resExp = form.__xPathSelectComp(nXpCtx, this.__readonlyXP);
                isReadonly = resExp.booleanValue();
            }

            if (!hasLast || last.readonly !== isReadonly) {
                last.readonly = isReadonly;
                if (hasLast || (!hasLast && isReadonly)) {
                    if (xc == null) xc = __xfNodeChange(model, n);
                    // readonly
                    xc[3] = isReadonly ? XF_TRUE_CHANGE : XF_FALSE_CHANGE;
                }
            }

            var val = xmlStringForNode(n);

            // if changed or not empty
            if (isRelevant && (forceCheck ||
                    ((xc != null && xc[0] == XF_TRUE_CHANGE && hasLast) || val != ""))) {

                if (this.__type != null && val != "") {
                    bool = form.__typer.isValid(val, this.__type, this.__elem);
                } else {
                    bool = true;
                }

                // TODO XSD type check

                if (bool && this.__constraint != null) {
                    resExp =
                        form.__xPathSelectComp(nXpCtx, this.__constraintXP);
                    bool = resExp.booleanValue();
                }
                if (last.constraint !== bool) {
                    last.constraint = bool;
                    if (xc == null) xc = __xfNodeChange(model, n);
                    // valid
                    xc[4] = bool ? XF_TRUE_CHANGE : XF_FALSE_CHANGE;
                }
                if (!bool) {
                    model.__allConstrained = false;
                }
            } else if (isRelevant && last.constraint === false) {
             	model.__allConstrained = false;
            }
        }
    } catch (e) {
        form.fireEvent("xforms-compute-exception", this.__model, e);
    }
    return [isRelevant, isReadonly];
};


XFBind.prototype.__recalcEmpty = function() {
	this.__ns = null;
	this.__doCalculate(this.__xpCtx);
};

XFBind.prototype.__rebuild = function() {
    this.__ns = null;
};

XFBind.prototype.remove = function() {
    if (this.id) {
        this.__model.__bindIds[this.id] = null;
    }
    XFEventModel.prototype.remove.apply(this, []);
};

XFBind.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__bindings = null;
    this.__model = null;
    this.__nodesetXP = null;
    this.__readonlyXP = null
    this.__lastReadonly = null;
    this.__relevantXP = null;
    this.__lastRelevant = null;
    this.__requiredXP = null;
    this.__lastRequired= null;
    this.__constraintXP = null;
    this.__lastConstraint = null;
    this.__calculateXP = null;
    this.__ns = null;
    this.__xpCtx = null;
};

function __xfValChangeDeep(model, node) {
    var xc = __xfNodeChange(model, node);
    xc[0] = XF_TRUE_CHANGE; // valChange
    var kids = node.childNodes;
    for (var i =0; i < kids.length; i++) {
        var n = kids[i];
        if (n.nodeType == 1) {
            __xfValChangeDeep(model, n);
        }
    }
}

function __xfNodeChange(model, node) {
    var cns = model.__changedNodes;
    var states =
      [ XF_NO_CHANGE,
        XF_NO_CHANGE,
        XF_NO_CHANGE,
        XF_NO_CHANGE,
        XF_NO_CHANGE];
    cns.push(node);
    cns.push(states);
    return states;
}


function XFDepGraph(nodeEx) {
    this.nodeEx = nodeEx;
    this.nullDeps = [];
}

XFDepGraph.prototype.reset = function() {
    this.nullDeps = [];
};

XFDepGraph.prototype.getDepends = function(node) {
    return this.nodeEx.getProp(node, "deps");
};

XFDepGraph.prototype.addNullDepend = function(depObj) {
    if (arrayFindStrict(this.nullDeps, depObj) < 0) {
        if (SH.debug) {
            SH.println(depObj + "+ <null>");
        }
        this.nullDeps.push(depObj);
    }
};

XFDepGraph.prototype.removeNullDepend = function(depObj) {
    if (SH.debug) {
        SH.println(depObj + "- <null>");
    }
    arrayRemoveStrict(this.nullDeps, depObj);
};

XFDepGraph.prototype.addDepend = function(node, depObj) {
    if (node == null) return;
    var deps = this.nodeEx.getProp(node, "deps");
    if (deps == null) {
        deps = [];
        this.nodeEx.setProp(node, "deps", deps);
    }
    deps.push(depObj);
};

XFDepGraph.prototype.addDependSet = function(ns, depObj) {
    var j;

    // DEBUG
    if (SH.debug) {
        SH.print(depObj + "+ ");
        for (j = 0; j < ns.length; j++) {
            SH.print(ns[j].nodeName + " ");
        }
        SH.println("");
    }
    for (j = 0; j < ns.length; j++) {
        this.addDepend(ns[j], depObj);
    }
};

XFDepGraph.prototype.removeDepend = function(node, depObj) {
    if (node == null) return;
    var deps = this.nodeEx.getProp(node, "deps");
    if (deps != null) {
        arrayRemoveStrict(deps, depObj);
    }
};

XFDepGraph.prototype.removeDependSet = function(ns, depObj) {
    var j;

    // DEBUG
    if (SH.debug) {
        SH.print(depObj + "~ ");
        for (j = 0; j < ns.length; j++) {
            SH.print(ns[j].nodeName + " ");
        }
        SH.println("");
    }
    for (j = 0; j < ns.length; j++) {
        this.removeDepend(ns[j], depObj);
    }
};

XFExfVariableResolver.prototype = new VariableResolver();
XFExfVariableResolver.prototype.constructor = XFExfVariableResolver;

function XFExfVariableResolver() {
    this.__parent = null;
    this.__vars = new Object();
}

XFExfVariableResolver.prototype.setVariableWithName = function(ns, ln, val) {
    this.__vars[ln] = val;
};

XFExfVariableResolver.prototype.getVariableWithName = function(ns, ln, c) {
    var val = this.__vars[ln];
    if (val == null && this.__parent != null) {
        return this.__parent.getVariableWithName(ns, ln, c);
    }
    if (val != null && val.constructor === XFExfVariable) {
        return val.evaluate(c);
    }
	return val;
};

XFExfVariableResolver.prototype.makeChild = function() {
    var kidRes = new XFExfVariableResolver();
    kidRes.__parent = this;
    return kidRes;
};


IXRuleSet.prototype = new XFEventModel();
IXRuleSet.prototype.constructor = IXRuleSet;

function IXRuleSet(model, elem) {
    var form = model.__form;
    this.__initEvt(form, elem, model);
    this.__id = elem.getAttribute("id");
    this.__src = elem.getAttribute("src");
}

IXRuleSet.prototype.getId = function() {
	return this.__id;
};

IXRuleSet.prototype.getRules = function(subId) {
	if (!this.__docNode) {
		var src = this.__src;
		if (__xfjavascriptUriRegEx.test(src)) {
            var jsSrc = src.substring(src.indexOf("javascript:") + 11);
            var impNode = __xfEvalJavascript(this, jsSrc);
            if (impNode.nodeType == 9) {
            	this.__docNode = impNode.documentElement;
            } else if (impNode.nodeType == 1) {
            	this.__docNode = this.__nodeToNewDoc(impNode).documentElement;
            } else throw new Error("Javascript src not an xml element or document.");
        } else {
           	var fUri = src;
            if (fUri.charAt(0) != "/" && fUri.indexOf("://") < 0) {
                fUri = this.__form.resolveUri(src);
            }
            var idoc = XFModel.__loadSharedSrc(fUri, this.__form.__xb);
            this.__docNode = idoc ? idoc.documentElement : null;
            if (typeof(App) != "undefined") {
                App.checkError(idoc);
            }
        }
	}
	var rules = null;
	if (this.__docNode) {
		var rulNode = Xml.matchOne(this.__docNode, "rule", "id", subId || "default");
		if (rulNode) {
		 	rules = new Rules(rulNode, this.__docNode);
		 	rules.setOptimizing(true);
		}
    }
	return rules;
};

IXRuleSet.prototype.dispose = function() {
	if (this.__rules) this.__rules.dispose();
	this.__docNode = null;
	this.__rules = null;
	XFEventModel.prototype.dispose.apply(this, []);
};
