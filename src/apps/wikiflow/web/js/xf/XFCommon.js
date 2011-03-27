/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 */
var XFORM_NAMESPACE = "http://www.w3.org/2002/xforms";
var XFORM_XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
var XFORM_XHTML2_NAMESPACE = "http://www.w3.org/2002/06/xhtml2";
var XFORM_EVENT_NAMESPACE = "http://www.w3.org/2001/xml-events";
var XFORM_EXTENDED_NAMESPACE = "http://www.exforms.org/exf/1-0";
var XFORM_ITENSIL_NAMESPACE = "http://itensil.com/ns/xforms";
var XFORM_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";


XFEvent.CAPTURING_PHASE = 1;
XFEvent.AT_TARGET = 2;
XFEvent.BUBBLING_PHASE = 3;

function XFEvent(target) {
    this.target = target;
    this.eventPhase = XFEvent.CAPTURING_PHASE;
}

XFEvent.prototype.initEvent = function(evtType /* string */, canBubble, cancelable) {
    this.type = evtType;
    this.bubbles = canBubble;
    this.cancelable = cancelable;
    this.timeStamp = (new Date()).getTime();
};

XFEvent.prototype.preventDefault = function() {
    if (this.cancelable) {
        this.__cancelled = true;
    }
};

XFEvent.prototype.stopPropagation = function() {
    this.__cancelled = true;
};

function XFEventModel() {
    this.__lTypes = null;
}

XFEventModel.prototype.__initEvt = function(form, elem /* DOMElement */, pObj) {
    this.__form = form;
    var id = elem == null ? null : elem.getAttribute("id");
    if (id != null && id != "") {
        this.__id = id;
        form.__ids[id] = this;
    }
    this.__elem = elem;
    this.__parent = pObj;
    pObj.__appendChild(this);
    this.__children = [];
};

XFEventModel.prototype.__appendChild = function(obj) {
    this.__children.push(obj);
};

XFEventModel.prototype.__removeChild = function(obj) {
    arrayRemoveStrict(this.__children, obj);
};

XFEventModel.prototype.addEventListener = function(evtType, listener /* XFEventModel */, useCapture) {
    var evId = useCapture ? "%" + evtType : evtType;
    var ls;
    if (this.__lTypes == null) {
        ls = [];
        this.__lTypes = new Object();
        this.__lTypes[evId] = ls;
    } else {
        ls = this.__lTypes[evId];
        if (ls == null) {
            ls = [];
            this.__lTypes[evId] = ls;
        }
    }
    ls.push(listener);
};

XFEventModel.prototype.removeEventListener = function(evtType, listener /* XFEventModel */, useCapture) {
    if (this.__lTypes == null) return;
    if (evtType == null) { // remove all
        for (var evId in this.__lTypes) {
            arrayRemove(this.__lTypes[evId], listener);
        }
    } else {
        var evId = useCapture ? "%" + evtType : evtType;
        var ls = this.__lTypes[evId];
        if (ls != null) {
            arrayRemove(ls, listener);
        }
    }
};

XFEventModel.prototype.dispatchEvent = function(evt) {
    if (this.__lTypes == null) return;
    var evId = evt.eventPhase == XFEvent.CAPTURING_PHASE ?
         "%" + evt.type : evt.type;
    var ls = this.__lTypes[evId];
    if (ls != null) {
        evt.currentTarget = this;
        for (var i=0; i < ls.length; i++) {
            ls[i].handleEvent(evt);
        }
    }
};

XFEventModel.prototype.remove = function() {
    if (this.__parent != null) {
        this.__parent.__removeChild(this);
    }
    this.dispose();
};

XFEventModel.prototype.dispose = function() {
    if (this.__children != null) {
        for (var i = this.__children.length - 1; i >= 0; i--) {
            this.__children[i].dispose();
        }
    }
    this.__parent = null;
    this.__elem = null;
    this.__children = null;
    this.__lTypes = null;
    this.__form = null;
};

XFEventModel.prototype.getAttribute = function(name) {
	return this.__elem ? this.__elem.getAttribute(name) : null;
};


XFEventModel.prototype.__endRender = function() {};

XFEventModel.prototype.handleEvent = function(evt) {};

XFEventModel.prototype.toString = function() {
    if (this.__elem != null) return this.__elem.nodeName;
    return "";
};



XFSingleNodeBinding.prototype = new XFEventModel();
XFSingleNodeBinding.prototype.constructor = XFSingleNodeBinding;

function XFSingleNodeBinding() {
    this.__ref = null;
    this.__bind = null;
    this.__bindRequired = true;
    this.__depList = null;
    this.__typeInf = null;
}

XFSingleNodeBinding.prototype.__initBind = function(form, elem, pObj) {
    var modId = elem.getAttribute("model");
    if (modId)  {
        this.__model = form.__models[modId];
        if (this.__model == null) {
            form.fireEvent("xforms-binding-exception", form.__defaultModel);
        } else {
            this.__model.__checkInit();
        }
    } else {
        if (pObj.__model != null) {
            this.__model = pObj.__model;
        } else {
            this.__model = form.__defaultModel;
        }
    }
    var bindId = elem.getAttribute("bind");
    if (bindId) {
        this.__bind = this.__model.__bindIds[bindId];
        if (this.__bind == null) {
            form.fireEvent("xforms-binding-exception", this.__model);
        }
        this.__dynBind = this.__bind.__dynBind;
    } else {
        this.__ref = elem.getAttribute("ref");
        if (this.__ref) {

            // if there's a predicate, function or variable
            this.__dynBind = (this.__ref.indexOf("[") > 0 ||
                this.__ref.indexOf("(") > 0 || this.__ref.indexOf("$") >= 0);
        }
    }
    this.__iNode = null;
};

XFSingleNodeBinding.autoGen = function(refXP, ctxNode, form) {
	var locPath = refXP.expression.locationPath;
	var depList = null;
	if (locPath.steps && locPath.steps.length > 0) {
		depList = [];
		for (var ii = 0; ii < locPath.steps.length; ii++) {
			var pstep = locPath.steps[ii];
			
			// TODO support siblings (only slightly indeterminate)
			if ((pstep.axis == Step.CHILD || pstep.axis == Step.ATTRIBUTE) &&
					pstep.nodeTest.type == NodeTest.NAMETESTQNAME) {
				
				if (SH.debug) SH.println("gen: " + pstep);
				var qname = Utilities.resolveQName(
						pstep.nodeTest.value, 
						form.__xpNSRes, 
						ctxNode, false);
						
				if (pstep.axis == Step.ATTRIBUTE) {
					ctxNode = XmlAttribute.createNode(ctxNode, qname[1], qname[0]);
				} else {
					ctxNode = qname[0] ? Xml.elementNs(ctxNode, qname[1], qname[0]) : 
							Xml.element(ctxNode, qname[1]);
				}
				
				depList.push(ctxNode);
				continue;
			}
			
			if (SH.debug) SH.println("NO gen: " + pstep);
			
			// fall through on paths we can't build
			depList = null;
			break;
		}
	}
	return depList;
};

XFSingleNodeBinding.prototype.getValueNode = function(xpCtx) {
    if (this.__form == null) return null;
    if (this.__iNode == null) {
        if (this.__bind != null) {
            var ns = this.__bind.__getNodeSet();
            if (ns.length >= 1) {
                this.__iNode = ns[0];
                if (!this.__isAction) {
                    this.__depList = [this.__iNode];
                    this.__model.__deps.addDepend(this.__iNode, this);
                }
            } else {
                this.__iNode = "";
                if (!this.__isAction) {
                    this.__nullDep = true;
                    this.__model.__deps.addNullDepend(this);
                }
            }
        } else if (xpCtx) {
            var ret = null;
            if (this.__ref || this.__bindRequired) {
                try {
                    if (this.__refXP == null) {
                        this.__refXP = this.__model.__xpathCache(this.__ref);
                    }
                    // deep analyze if it's dynamic
                    if (!this.__isAction && this.__depList == null && this.__dynBind) {
                        xpCtx.depList = [];
                        ret = this.__form.__xPathSelectOneComp(
                            xpCtx, this.__refXP);
                        if (xpCtx.depList.length == 0) {
                            this.__nullDep = true;
                            this.__model.__deps.addNullDepend(this);
                        } else {
                            this.__model.__deps.addDependSet(xpCtx.depList, this);
                            this.__depList = xpCtx.depList;
                        }
                        xpCtx.depList = null;
                    } else {
                        xpCtx.depList = null;
                        ret = this.__form.__xPathSelectOneComp(
                            xpCtx, this.__refXP);
                        if (ret != null) {
                            if (!this.__isAction && this.__depList == null) {
                                if (ret.nodeType == 2) { // ATTRIBUTE
                                    this.__depList =
                                        [ret, Utilities.getOwnerElement(ret)];
                                    this.__model.__deps.addDependSet(
                                        this.__depList, this);
                                } else {
                                    this.__model.__deps.addDepend(ret, this);
                                    this.__depList = [ret];
                                }
                            }
                        } else if (!this.__isAction) {
                        	if (this.__form.autoGenRefs && !this.__nullDep) {
                        		var locPath = this.__refXP.expression.locationPath;
                        		if ((this.__depList = XFSingleNodeBinding.autoGen(this.__refXP, xpCtx.node, this.__form)) == null) {
                        			this.__nullDep = true;
									this.__model.__deps.addNullDepend(this);
                        		} else {
                        			this.__nullDep = true;
                        			this.__depList = null;
                            		this.__model.__deps.addNullDepend(this);
                        		}
                        		if (!this.__nullDep) {
                        			this.__model.__deps.addDependSet(this.__depList, this);
                        			this.__model.__recalcEmptySetBinds();
                        			ret = this.__depList[this.__depList.length - 1];
                        		}
                        	} else {
                            	this.__nullDep = true;
                            	this.__model.__deps.addNullDepend(this);
                        	}
                        }
                    }
                } catch (e) {
                    this.__form.fireEvent(
                        "xforms-compute-exception", this.__model, e);
                }
            }
            this.__iNode = ret;
        }
    }
    return this.__iNode;
};

XFSingleNodeBinding.prototype.getTypeInfo = function(xpCtx) {
    if (this.__typeInf == null) {
        this.__typeInf = this.__model.getTypeInfo(this.getValueNode(xpCtx));
    }
    return this.__typeInf;
};

XFSingleNodeBinding.prototype.getValue = function(xpCtx) {
    var v = this.getValueNode(xpCtx);
    return typeof v == "string" ? v : xmlStringForNode(v);
};

/**
 * This is a heavy weight convenience function
 */
XFSingleNodeBinding.prototype.setValue = function(val) {
	var vn = this.getValueNode();
	if (vn && typeof vn != "string") {
		if (this.__model.__setNodeValue(vn, val)) {
			this.__model.recalculate();
		}
	}
};

XFSingleNodeBinding.prototype.dispose = function() {
    if (SH.debug) SH.println("dp: " + this);
    if (this.__depList != null)
        this.__model.__deps.removeDependSet(this.__depList, this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    }
    arrayRemoveStrict(this.__model.__rbCtrls, this);
    XFEventModel.prototype.dispose.apply(this, []);
    this.__depList = null;
    this.__iNode = null;
    this.__oldiNode = null;
    this.__bind = null;
    this.__refXP = null;
    this.__model = null;
    this.__typeInf = null;
};

XFSingleNodeBinding.prototype.rebuild = function() {
    if (SH.debug) SH.println("rb: " + this);
    if (this.__depList != null)
        this.__model.__deps.removeDependSet(this.__depList, this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    }
    this.__typeInf = null;
    this.__depList = null;
    this.__oldiNode = this.__iNode;
    this.__iNode = null;
};


XFNodeSetBinding.prototype = new XFEventModel();
XFNodeSetBinding.prototype.constructor = XFNodeSetBinding;

function XFNodeSetBinding() {
    this.__nodeset = null;
    this.__bind = null;
    this.__bindRequired = true;
    this.__depList = null;
}

XFNodeSetBinding.prototype.__initBind = function(form, elem, pObj) {
    var modId = elem.getAttribute("model");
    if (modId)  {
        this.__model = form.__models[modId];
        if (this.__model == null) {
            form.fireEvent("xforms-binding-exception", form.__defaultModel);
        } else {
            this.__model.__checkInit();
        }
    } else {
        if (pObj.__model != null) {
            this.__model = pObj.__model;
        } else {
            this.__model = form.__defaultModel;
        }
    }
    var bindId = elem.getAttribute("bind");
    if (bindId) {
        this.__bind = this.__model.__bindIds[bindId];
        if (this.__bind == null) {
            form.fireEvent("xforms-binding-exception", this.__model);
        }
        this.__dynBind = this.__bind.__dynBind;
    } else {
        this.__nodeset = elem.getAttribute("nodeset");
        if (this.__nodeset) {

            // if there's a predicate, function or variable
            this.__dynBind = (this.__nodeset.indexOf("[") > 0 ||
                this.__nodeset.indexOf("(") > 0 ||
                this.__nodeset.indexOf("$") >= 0);
        }
    }
    this.__iNodeSet = null;
};

XFNodeSetBinding.prototype.getNodeSet = function(xpCtx) {
    if (this.__form == null) return null;
    if (this.__iNodeSet == null) {
        if (this.__bind) {
            this.__iNodeSet = this.__bind.__getNodeSet();
            if (this.__iNodeSet.length >= 1) {
                this.__depList = this.__iNodeSet;
                this.__model.__deps.addDependSet(this.__depList, this);
                this.__model.__deps.addDepend(
                    this.__iNodeSet[0].parentNode, this);
            } else {
                this.__iNodeSet = null;
                this.__nullDep = true;
                this.__model.__deps.addNullDepend(this);
            }
        } else if (this.__nodeset) {
            var resExp;
            var analyzed = false;
            try {
                if (this.__nodesetXP == null) {
                    this.__nodesetXP =
                        this.__model.__xpathCache(this.__nodeset);
                }
                // deep analyze if it's dynamic
                if (this.__depList == null && this.__dynBind) {
                    xpCtx.depList = [];
                    resExp = this.__form.__xPathSelectComp(
                            xpCtx, this.__nodesetXP);
                    analyzed = true;
                    this.__iNodeSet = resExp ? resExp.toArray() : [];
                    if (this.__iNodeSet.length >= 1) {
                        Utilities.addDeps(xpCtx, [this.__iNodeSet[0].parentNode]);
                    }
                    this.__model.__deps.addDependSet(xpCtx.depList, this);
                    this.__depList = xpCtx.depList;
                     xpCtx.depList = null;
                } else {
                    xpCtx.depList = null;
                    resExp = this.__form.__xPathSelectComp(
                            xpCtx, this.__nodesetXP);
                    this.__iNodeSet = resExp ? resExp.toArray() : [];
                }
            } catch (e) {
                this.__form.fireEvent(
                    "xforms-compute-exception", this.__model, e);
            }
            if (resExp.constructor !== XNodeSet) {
                this.__form.fireEvent("xforms-binding-exception", this);
            }
            if (this.__iNodeSet.length == 0) {
                this.__nullDep = true;
                this.__model.__deps.addNullDepend(this);
            } else if (!analyzed && this.__depList == null) {
                this.__depList = this.__iNodeSet;
                this.__model.__deps.addDependSet(this.__depList, this);
                this.__model.__deps.addDepend(
                    this.__iNodeSet[0].parentNode, this);
            }
        } else if (this.__bindRequired) {
            this.__form.fireEvent(
                "xforms-binding-exception", this, "@bind or @nodeset required");
        }
    }
    return this.__iNodeSet;
};

XFNodeSetBinding.prototype.dispose = function() {
    if (this.__depList != null)
        this.__model.__deps.removeDependSet(this.__depList, this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    }
    arrayRemoveStrict(this.__model.__rbCtrls, this);
    XFEventModel.prototype.dispose.apply(this, []);
    this.__depList = null;
    this.__iNodeSet = null;
    this.__bind = null;
    this.__nodesetXP = null;
};

XFNodeSetBinding.prototype.rebuild = function() {
    if (SH.debug) SH.println("rb: " + this);
    if (this.__depList != null)
        this.__model.__deps.removeDependSet(this.__depList, this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    }
    this.__depList = null;
    this.__iNodeSet = null;
};

function __xfFlattenSpace(s) {
    var b = 0;
    var e = s.length;
    var i, ch;
    for (i = 0; i < e; i++) {
        ch = s.charAt(i);
        if ("\t\r\n\f ".indexOf(ch) >= 0) b++;
        else break;
    }
    if (b < e) {
        for (i = e - 1; i >= 0; i--) {
            ch = s.charAt(i);
            if ("\t\r\n\f ".indexOf(ch) >= 0) e--;
            else break;
        }
    }
    return s.substring(b > 0 ? b - 1 : 0, e + 1);
}

XFDirectBinding.prototype = new XFEventModel();
XFDirectBinding.prototype.constructor = XFDirectBinding;

function XFDirectBinding() {
    this.__typeInf = null;
}

XFDirectBinding.prototype.initBind = function(form, node) {
    this.__initEvt(form, null, form);
    this.__model = form.__defaultModel;
    this.setNode(node);
};

XFDirectBinding.prototype.setNode = function(node) {
    this.__iNode = node;
    if (node == null) {
        this.__nullDep = true;
        this.__model.__deps.addNullDepend(this);
    } else {
        this.__model.__deps.addDepend(node, this);
    }
};

XFDirectBinding.prototype.getValueNode = function() {
    return this.__iNode;
};

XFDirectBinding.prototype.getTypeInfo = function() {
    if (this.__typeInf == null) {
        this.__typeInf = this.__model.getTypeInfo(this.__iNode);
    }
    return this.__typeInf;
};

XFDirectBinding.prototype.getValue = function() {
    return xmlStringForNode(this.__iNode);
};

XFDirectBinding.prototype.dispose = function() {
    if (SH.debug) SH.println("dp: " + this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    } else {
        this.__model.__deps.removeDepend(this.__iNode, this);
    }
    this.__iNode = null;
    this.__oldiNode = null;
    XFEventModel.prototype.dispose.apply(this, []);
};

XFDirectBinding.prototype.rebuild = function() {
    if (this.__iNode.parentNode == null) {
        this.nodeRemoved();
        if (this.__nullDep) {
            this.__nullDep = false;
            this.__model.__deps.removeNullDepend(this);
        } else {
            this.__model.__deps.removeDepend(this.__iNode, this);
        }
        this.__oldiNode = this.__iNode;
        this.__iNode = null;
    }
};

XFDirectBinding.prototype.destroyNode = function() {
    this.__model.__destroyNode(this.__iNode);
};

XFDirectBinding.prototype.nodeRemoved = function() {
    // override me
};

XFDirectBinding.prototype.valueChanged = function() {
    if (this.__iNode != null) this.__model.__form.fireEvent("xforms-value-changed", this);
};

XFDirectBinding.prototype.setRequired = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-required" : "xforms-optional", this);
};

XFDirectBinding.prototype.setEnabled = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-enabled" : "xforms-disabled", this);
};

XFDirectBinding.prototype.setValid = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-valid" : "xforms-invalid", this);
};

XFDirectBinding.prototype.setReadonly = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-readonly" : "xforms-readwrite", this);
};

XFDirectBinding.prototype.toString = function() {
    return "*directbind";
};

//XFTypeFormat.prototype.constructor = XFTypeFormat;

function XFTypeFormat() {
}

XFTypeFormat.prototype.format = function(str, ctrl) {
    return str;
};

XFTypeFormat.prototype.parse = function(str, ctrl) {
    return str;
};

XFTypeFormat.prototype.onfocus = function(ctrl) {
};

XFTypeFormat.prototype.decorate = function(uiElem, ctrl) {
};

XFTypeFormat.prototype.disposeDecor = function(uiElem, ctrl) {
};

XFTypeFormat.TYPE_CLASSES = { _default : new XFTypeFormat() };
XFTypeFormat.DEF_PREFIX = { ix : XFORM_ITENSIL_NAMESPACE,
		xsd : XFORM_SCHEMA_NAMESPACE, xs : XFORM_SCHEMA_NAMESPACE};

XFTypeFormat.getFormatByQName = function(qn) {
	var parts = qn.split(":");
	var ns = XFTypeFormat.DEF_PREFIX[parts[0]];
	if (ns == null) return XFTypeFormat.TYPE_CLASSES._default;
	return XFTypeFormat.getFormat(ns, parts[1]);
};

XFTypeFormat.getFormat = function(ns, ln) {
    var cl = XFTypeFormat.TYPE_CLASSES["{" + ns + "}" + ln];
    if (cl == null) return XFTypeFormat.TYPE_CLASSES._default;
    return cl;
};

XFTypeFormat.addFormat = function(ns, ln, cl) {
    XFTypeFormat.TYPE_CLASSES["{" + ns + "}" + ln] = cl;
};



var __xfjavascriptUriRegEx = new RegExp("^\\s*javascript:", "m");

function __xfEvalJavascript(model, __jsSrc) {
    return eval(__jsSrc);
}