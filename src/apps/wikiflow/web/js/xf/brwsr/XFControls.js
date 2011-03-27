/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 */

function xfDialog(title, canClose, uiParent, frmUri, xb, context, instanceSrc, submitAction, noResize, diagHelp) {
    var diag = new Dialog(title, canClose);
    if (noResize && noResize != 2) diag.canResize = false;
    if (diagHelp) diag.initHelp(diagHelp);
    diag.render(uiParent);
    xfDialog.__count++;
    var dXf = new XForm(xb.loadURI(frmUri), "xfdiag" + xfDialog.__count, xb, frmUri, context);
    if (instanceSrc) dXf.setDefaultUris(instanceSrc, submitAction);
    exAddClass(diag.contentElement, "diagXf", false);
    dXf.render(diag.contentElement);
    var mod = dXf.getDefaultModel();
    mod.addEventListener("xforms-close", diag);
    if (!noResize) mod.addEventListener("xforms-rebuild", diag);
    diag.xform = dXf;
    diag.addListening(mod);
    diag.addDisposable(dXf);
    return diag;
}

function xfDialogConfig(title, frmUri, xb, config) {
	if (!config) config = {};
    var diag = new Dialog(title, !config.closeLocked);
    if (config.noResize && config.noResize != 2) diag.canResize = false;
    if (config.diagHelp) diag.initHelp(config.diagHelp);
    var uiParent = config.uiParent || document.body;
    diag.render(uiParent);
    xfDialog.__count++;
    var dXf = new XForm(xb.loadURI(frmUri), "xfdiag" + xfDialog.__count, xb, frmUri, config.context);
    if (config.uriResolver) dXf.setUriResolver(config.uriResolver);
    if (config.instanceSrc) dXf.setDefaultUris(config.instanceSrc, config.submitAction);
   	if (config.varStrings) {
    	for (var nm in config.varStrings) {
    		dXf.setVarString(nm, config.varStrings[nm]);
    	}
    }
    if (config.defPath) dXf.__defPath = config.defPath;
    
    exAddClass(diag.contentElement, "diagXf", false);
    dXf.render(diag.contentElement);
    var mod = dXf.getDefaultModel();
    mod.addEventListener("xforms-close", diag);
    if (!config.noResize) mod.addEventListener("xforms-rebuild", diag);
    diag.xform = dXf;
    diag.addListening(mod);
    diag.addDisposable(dXf);
    return diag;
}

xfDialog.__count = 0;

function xfTemplateDialog(title, canClose, uiParent, xform, tmplName, xNode, noResize, varStrings, diagHelp) {
    var tmpl = xform.getIxTemplate(tmplName);
    var diag = new Dialog(title, canClose);
    if (diagHelp) diag.initHelp(diagHelp);
    if (noResize) diag.canResize = false;
    diag.render(uiParent);
    exAddClass(diag.contentElement, "diagXf", false);
    
    var mod = xform.getDefaultModel();
    mod.addEventListener("xforms-rebuild", diag);
    diag.addListening(mod);
    
    if (varStrings) {
    	for (var nm in varStrings) {
    		tmpl.setVarString(nm, varStrings[nm]);
    	}
    }
    
    var hElem = tmpl.renderTemplate(xNode, diag.contentElement);
    
    tmpl.addEventListener("xforms-close", diag);
    diag.addListening(tmpl);

    // peekabo prevent
    if (SH.is_ie && hElem && hElem.nodeType == 1
			&& hElem.currentStyle.hasLayout
			&& hElem.currentStyle.position == "static") {
		hElem.style.position = "relative";
    }
    
    diag.addDisposable(tmpl);
    diag.xfTmpl = tmpl;
    return diag;
}

function __xfControlUiParentInit(uiParent) {
    if (SH.is_safari) {
        var frm = makeElement(uiParent, "form");
        frm.onsubmit = function (evt) { return false; };
        return frm;
    } else {
        return uiParent;
    }
}

function __xfCtrlCommonUI(ctrl) {
    var style = ctrl.__elem.getAttribute("style");
    if (style && ctrl.__widget == null) {
        if (ctrl.__hWidget != null) {
            __xfSetCss(ctrl.__form, ctrl.__hWidget, style);
        } else {
            __xfSetCss(ctrl.__form, ctrl.__hElem, style);
        }
    }
    if (ctrl.__elem.getAttribute("stopdrag") == "true") {
    	if (ctrl.__hElem != null) {
    		ctrl.__hElem._dndNoDrag = true;
    	}
    }
}

function __xfSetCss(form, hElem, style) {
    var strep = __xfCssUrlReplace(form.__path, style);
    if (SH.is_opera) hElem.setAttribute("style", strep);
    else hElem.style.cssText = strep;
}

var __xfCssUrlRgx = new RegExp("(url\\(\\s*['\"]?)(.+)(['\"]?\\s*\\))", "g");

function __xfCssUrlReplace(baseUri, txt) {
    __xfCssUrlRgx.lastIndex = 0;
    var mat = __xfCssUrlRgx.exec(txt);
    if (!mat) {
        return txt;
    }
    var eIdx = 0;
    var ftxt = [];
    while (mat) {
        ftxt.push(txt.substring(eIdx, mat.index));
        ftxt.push(mat[1]);
        ftxt.push(Uri.reduce(Uri.absolute(baseUri, mat[2])));
        ftxt.push(mat[3]);

        eIdx = __xfCssUrlRgx.lastIndex;
        mat = __xfCssUrlRgx.exec(txt);
    }
    ftxt.push(txt.substring(eIdx));
    return ftxt.join("");
}

function __xfHideHints() {
    Ephemeral.hide();
}

XFText.prototype.constructor = XFText;

function XFText(form, node, pObj) {
    this.__node = node;
    this.__parent = pObj;
    pObj.__appendChild(this);
}

XFText.prototype.render = function(hParent, hBefore) {
    var txt = SH.is_ie ? __xfFlattenSpace(this.__node.nodeValue) : this.__node.nodeValue;
    var doc = hParent ? hParent.ownerDocument : document;
    this.__hNode = doc.createTextNode(txt);
    hParent.insertBefore(this.__hNode, hBefore);
};

XFText.prototype.remove = function() {
    this.__hNode.parentNode.removeChild(this.__hNode);
    this.__parent.__removeChild(this);
    this.dispose();
};

XFText.prototype.dispose = function() {
    this.__hNode = null;
    this.__node = null;
    this.__parent = null;
};

XFText.prototype.toString = function() {
    return "#text";
};

XFHtml.prototype = new XFEventModel();
XFHtml.prototype.constructor = XFHtml;

function XFHtml(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__ignoreText = false;
}

XFHtml.prototype.render = function(hParent, hBefore, xpCtx) {
    var node = this.__elem;
    var tag = xmlGetLocalName(node).toLowerCase();
    if (tag == "style") {
        this.__ignoreText = true;
        var cssText = __xfCssUrlReplace(
            this.__form.__path, xmlStringForNode(node));
        if (SH.is_ie) {
            hParent.insertAdjacentHTML("beforeEnd",
                "<span style='display:none'>&nbsp;</span><style>" +
                cssText + "</style>");
            this.__hElem = hParent.lastChild;
        } else {
        	var doc = hParent ? hParent.ownerDocument : document;
        	var par = doc.getElementsByTagName("head");
        	if (par.length > 0) par = par[0];
        	else par = hParent;
            this.__hElem = makeElement(par, "style", null, cssText, null, {type:"text/css"});
        }
        return hParent;
    }
    if (tag == "script") {
        this.__ignoreText = true;
        var src = xmlStringForNode(node);
        var evt = Utilities.getAttributeNS(
                node, XFORM_EVENT_NAMESPACE, "event");
        if (evt != null && evt != "") {
            this.__evtFunc = new Function("event", "model", "uiParent", "contextNode", src);
            this.__evtCtx = xpCtx;
            this.__parent.addEventListener(evt, this);
        } else {
            var func = new Function("model", "uiParent", "contextNode", src);
            if (this.__parent.constructor === XFAction) {
                this.__evtCtx = xpCtx;
                this.__parent.addAction(new XFScriptAction(this, func));
            } else {
                func(this.__form.__defaultModel, hParent, xpCtx.node);
            }
        }
        this.__hParent = hParent;
        return hParent;
    }

    if ("table thead tbody tfoot tr select ".indexOf(tag + " ") >= 0) {
        this.__ignoreText = true;
    }
    var atts = node.attributes;
    var i;
    var hAttrs = new Object();
    var className = node.getAttribute("class") || "";
    for (i = 0; i < atts.length; i++) {
        var a = atts[i];
        if (!a.prefix) {
            var name = a.nodeName;
            var value = a.nodeValue;
            switch (name.toLowerCase()) {
                case "colspan":  name = "colSpan"; break;
                case "rowspan":  name = "rowSpan"; break;
                case "cellpadding":  name = "cellPadding"; break;
                case "cellspacing":  name = "cellSpacing"; break;
                case "id": // decorate html id and mirror it as extra CSS class
                    className += " id_" + value;
                    value = this.__form.__name + "_" + value;
                    break;
                case "src": // localize paths
                	if (!/(?:http|https):/.test(value)) {
                		value = Uri.reduce(this.__form.resolveUri(value));
                	}
                    break;
               	case "href": // localize paths
               		if (tag == "link")
                    	value = Uri.reduce(this.__form.resolveUri(value));
                    break;
                case "class":
                case "style":
                    continue; //skip
            }
            hAttrs[name] = value;
        }
    }
    var hElem = makeElement(hParent, tag, className, null, hBefore, hAttrs);
    this.__hElem = hElem;
    __xfCtrlCommonUI(this);
    if (tag == "table") {
        var hasTbody = false;
        var kids = node.childNodes;
        for (i = 0; i < kids.length; i++) {
            var kn = kids[i];
            if (kn.nodeType == 1) {
                tag = xmlGetLocalName(kn).toLowerCase();
                if (tag == "tbody" || tag == "thead" || tag == "tfoot") {
                    hasTbody = true;
                    break;
                } else if (tag == "tr") {
                    break;
                }
            }
        }
        if (!hasTbody) {
            hElem = makeElement(hElem, "tbody");
       }
    }
    return hElem;
};

XFHtml.prototype.addEventListener = function(evtType, listener /* XFEventModel */, useCapture) {
    if (this.__hElem.__xfObj == null) {
        this.__hElem.__xfObj = this;
    }
    addEventHandler(this.__hElem, evtType, __xfHtmlFireEvent);
    XFEventModel.prototype.addEventListener.apply(
        this, [evtType, listener, useCapture]);
};

XFHtml.prototype.handleEvent = function(evt) {
    if (this.__evtFunc != null) {
        this.__evtFunc(evt, this.__form.__defaultModel, this.__hParent, this.__evtCtx.node);
    }
};

XFHtml.prototype.remove = function() {
	XFHtml.__removeElement(this.__hElem);
    XFEventModel.prototype.remove.apply(this, []);
};

XFHtml.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    if (this.__hElem != null) this.__hElem.__xfObj = null;
    this.__hElem = null;
    this.__hParent = null;
    this.__evtCtx = null;
};

XFHtml.prototype.toString = function() {
    return this.__elem.nodeName;
};

XFHtml.__removeElement = function(hElem) {
	if (hElem != null && hElem.parentNode != null)
        hElem.parentNode.removeChild(hElem);
};


function __xfHtmlFireEvent(evt) {
    this.__xfObj.__form.fireEvent(evt.type, this.__xfObj, null, {uiEvent:evt});
}


XFCtrlBase.prototype = new XFSingleNodeBinding();
XFCtrlBase.prototype.constructor = XFCtrlBase;

function XFCtrlBase() {
    this.__ignoreText = true;
    this.isValue = false;
}

XFCtrlBase.prototype.__initCtrl = function() {
    this.__ctrlId = this.__model.__addControl(this);
};

XFCtrlBase.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__model.__form.fireEvent("xforms-value-changed", this);
    }
};

XFCtrlBase.prototype.setRequired = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-required" : "xforms-optional", this);
};

XFCtrlBase.prototype.setEnabled = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-enabled" : "xforms-disabled", this);
    if (this.__hWidget != null) {
        this.__hWidget.disabled = !bool;
    }
    if (this.__hElem != null) {
        exAddClass(this.__hElem, "disab", bool);
    }
};

XFCtrlBase.prototype.setValid = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-valid" : "xforms-invalid", this);
    if (this.__hElem != null) {
        exAddClass(this.__hElem, "invalid", bool);
    }
};

XFCtrlBase.prototype.setReadonly = function(bool, initMode) {
    if (!initMode)
        this.__model.__form.fireEvent(
            bool ? "xforms-readonly" : "xforms-readwrite", this);
    if (this.__hWidget != null) {
        this.__hWidget.readOnly = bool;
    } else if (this.__widget != null) {
    	this.__widget.readOnly = bool;
    }
    if (this.__hElem != null) {
        exAddClass(this.__hElem, "readonly", !bool);
    }
};

XFCtrlBase.prototype.__endRender = function() {
    var nd = this.getValueNode(this.__xpCtx);
    var last = nd ? this.__model.__form.__xpNodeEx.getProp(nd, "_lb") : null;
    if (last != null) {
        if ("required" in last) {
           this.setRequired(last.required, true);
        }
        if ("relevant" in last) {
           this.setEnabled(last.relevant, true);
        }
        if ("constraint" in last) {
           this.setValid(last.constraint, true);
        }
        if ("readonly" in last) {
           this.setReadonly(last.readonly, true);
        }
    }
};

XFCtrlBase.prototype.remove = function() {
    XFHtml.__removeElement(this.__hElem);
    XFEventModel.prototype.remove.apply(this, []);
};

XFCtrlBase.prototype.dispose = function() {
    XFSingleNodeBinding.prototype.dispose.apply(this, []);
    this.__typeFmt = null;
    if (this.__hWidget != null) {
        this.__hWidget.__xfObj = null;
        this.__hWidget = null;
    } else if (this.__hWidgetSet != null) {
        for (var i = 0; i <  this.__hWidgetSet.length; i++) {
            this.__hWidgetSet[i].__xfObj = null;
            this.__hWidgetSet[i] = null;
        }
        this.__hWidgetSet = null;
    }
    if (this.__hElem != null) {
        this.__hElem.__xfObj = null;
        this.__hElem = null;
    }
    if (this.__xpCtx != null) {
        this.__xpCtx = null;
    }
};

XFCtrlBase.prototype.rebuild = function() {
    XFSingleNodeBinding.prototype.rebuild.apply(this, []);
    this.__typeFmt = null;
};

XFCtrlBase.prototype.getTypeFormat = function(xpCtx) {
    if (this.__typeFmt == null) {
        var inf = this.getTypeInfo(xpCtx);
        this.__typeFmt = XFTypeFormat.getFormat(inf.namespace, inf.type);
    }
    return this.__typeFmt;
};

// for use in user script
XFCtrlBase.getControl = function(hElem) {
	if (hElem) return hElem.__xfObj;
	return null;
};

XFSetCtrlBase.prototype = new XFNodeSetBinding();
XFSetCtrlBase.prototype.constructor = XFSetCtrlBase;

function XFSetCtrlBase() {
    this.__ignoreText = true;
    this.isValue = false;
}

XFSetCtrlBase.prototype.__initCtrl = function() {
    this.__model.__addControl(this);
    this.__renderedNs = [];
};

XFSetCtrlBase.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__model.__form.fireEvent("xforms-value-changed", this);

        // clean old
        var oldNs = this.__renderedNs;
        var startPos = 0;
        var ii;
        var ns = this.getNodeSet(this.__xpCtx);
        for (ii = 0; ii < ns.length; ii++) {
            if (oldNs.length > ii)  {
                if (oldNs[ii] === ns[ii]) startPos = ii + 1;
                else break;
            } else {
                startPos = ii;
                break;
            }
        }
        for (ii = this.__children.length - 1; ii >= 0; ii--) {
            var kid = this.__children[ii];
            if (kid.__repeatPos >= startPos) {
                kid.remove();
            }
        }

        // draw new
        var form = this.__model.__form;
        var hBefore = null;
        if (this.__children.length > 0) {
            // before element after the last in set
            for (ii = this.__children.length - 1; ii >= 0; ii--) {
                kid = this.__children[ii];
                if (kid.__hElem != null) {
                    hBefore = kid.__hElem.nextSibling;
                    break;
                }
            }
        } else {
            var idx = arrayFindStrict(this.__parent.__children, this) + 1;
            if (idx < this.__parent.__children.length) {
                kid = this.__parent.__children[idx];
                if (kid.__hElem != null) hBefore = kid.__hElem;
                else hBefore = kid.__hNode;
            }
        }
        if (hBefore && hBefore.parentNode !== this.__hParent) hBefore = null;
        for (ii = startPos; ii < ns.length; ii++) {
            var nXpCtx = {
                node : ns[ii], position : ii + 1, size : ns.length,
                varRes : this.__xpCtx.varRes.makeChild() };
            form.__doXformBody(
                this.__elem, nXpCtx, this.__hParent, this, hBefore);
            this.__endRender();
        }
        //this.__startPos = ns.length;
        this.__renderedNs = ns;
        
        // a potential height redraw bug in FF2.0 maybe others
        if (SH.is_gecko) {
        	// just calling this makes it draw ok
        	this.__hParent.ownerDocument.defaultView.getComputedStyle(
        		this.__hParent, null).getPropertyValue("padding-top");
        }
    }
};

XFSetCtrlBase.prototype.setRequired = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-required" : "xforms-optional", this);
};

XFSetCtrlBase.prototype.setEnabled = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-enabled" : "xforms-disabled", this);
};

XFSetCtrlBase.prototype.setValid = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-valid" : "xforms-invalid", this);
};

XFSetCtrlBase.prototype.setReadonly = function(bool) {
    this.__model.__form.fireEvent(
        bool ? "xforms-readonly" : "xforms-readwrite", this);
};

XFSetCtrlBase.prototype.remove = function() {
    XFHtml.__removeElement(this.__hElem);
    for (var ii = this.__children.length - 1; ii >= 0; ii--) {
        this.__children[ii].remove();
    }
    XFEventModel.prototype.remove.apply(this, []);
};

XFSetCtrlBase.prototype.dispose = function() {
    XFNodeSetBinding.prototype.dispose.apply(this, []);
    if (this.__hElem != null) {
        this.__hElem.__xfObj = null;
        this.__hElem = null;
    }
    if (this.__xpCtx != null) {
        this.__xpCtx = null;
    }
    this.__hParent = null;
};

/*
XFSetCtrlBase.prototype.rebuild = function() {
    XFNodeSetBinding.prototype.rebuild.apply(this, []);
};*/


function __xfControlValueProcess(blur, isSelect) {
    if (this.__model == null) return;
    var form = this.__model.__form;
    var model = this.__model;
    model.__skipValueChange = this;
    model.__doRecalculate();
    model.__doRevalidate();
    form.fireEvent("xforms-value-changed", this);
    if (blur) form.fireEvent("DOMFocusOut", this);
    if (isSelect) form.fireEvent("xforms-select", this);
    model.__doRefresh();
}


XFControlInput.prototype = new XFCtrlBase();
XFControlInput.prototype.constructor = XFControlInput;

function XFControlInput(form, elem, pObj) {
    this.__hint = null;
    if (arguments.length > 0) {
        this.__initEvt(form, elem, pObj);
        this.__initBind(form, elem, pObj);
        this.__initCtrl();
    }
}

XFControlInput.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    var className = this.__elem.getAttribute("class");
    if (className) {
        className = "xfctrl " + className;
    } else{
        className = "xfctrl";
    }
    this.__hElem = makeElement(hParent, "div", className, null, hBefore);
    var iid = this.__elem.getAttribute("id");
    if (iid) {
    	className += " id_" + iid;
        iid = this.__form.__name + "_" + iid;
        this.__hElem.id = iid;
    }
    if (SH.is_gecko && this.__hElem.style.overflow == "") this.__hElem.style.overflow = "show";
    this.__hElem.__xfObj = this;
    this.__hWidget = makeElement(this.__hElem, "input", "xftext",
        Xml.getLocalName(this.__elem) == "secret" ? "password" : "text",
        null, { "name" : this.__ctrlId });
    if (iid) this.__hWidget.id = iid + "_val";
    this.__hWidget.value = this.getValue(xpCtx);
    if (SH.is_gecko) this.__hWidget.setAttribute("autocomplete", "off");
    this.__hWidget.__xfObj = this;
    this.__hWidget.onfocus = __xfControlInputFocus;
    this.__hWidget.onblur = __xfControlInputBlur;
    var typeFmt = this.getTypeFormat(xpCtx);
    typeFmt.decorate(this.__hWidget, this);
    __xfCtrlCommonUI(this);
    return this.__hElem; // return the parent since html input don't have kids
};

// timeObj functions
XFControlInput.prototype.getTime = function() {
    return this.__hWidget.value;
};

XFControlInput.prototype.setTime = function(dStr) {
    this.__hWidget.value = dStr;
    this.changeValue(false);
};

XFControlInput.prototype.getValue = function(xpCtx) {
    var val = XFSingleNodeBinding.prototype.getValue.apply(this, [xpCtx]);
    var typeFmt = this.getTypeFormat(xpCtx);
    return typeFmt.format(val, this);
};

XFControlInput.prototype.setValue = function(val) {
	if (this.__xpCtx != null) {
	 	var typeFmt = this.getTypeFormat(this.__xpCtx);
		val = typeFmt.parse(val, this);
	}
	XFSingleNodeBinding.prototype.setValue.apply(this, [val]);
};

XFControlInput.prototype.setHint = function(xfHint) {
    this.__hint = xfHint;
};

XFControlInput.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__hWidget.value = this.getValue(this.__xpCtx);
        XFCtrlBase.prototype.valueChanged.apply(this, []);
    }
};

XFControlInput.prototype.changeValue = function(blur) {
    var model = this.__model;
    var val = this.__hWidget.value;
    var typeFmt = this.getTypeFormat(this.__xpCtx);
    var pval = typeFmt.parse(val, this);
    if (pval != null) {
        val = pval;
        this.__hWidget.value = typeFmt.format(pval, this);
    }
    var holdThis = this;
    if (model.__setNodeValue(this.getValueNode(this.__xpCtx), val)) {
        window.setTimeout(
           function() { __xfControlValueProcess.apply(holdThis, [blur]); }, 10);
    }
};

XFControlInput.prototype.dispose = function() {
    var typeFmt = this.getTypeFormat(this.__xpCtx);
    typeFmt.disposeDecor(this.__hWidget, this);
    XFCtrlBase.prototype.dispose.apply(this, []);
};

XFControlTextarea.prototype = new XFControlInput();
XFControlTextarea.prototype.constructor = XFControlTextarea;

function XFControlTextarea(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
}

XFControlTextarea.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    var className = this.__elem.getAttribute("class");
    if (className) {
        className =  "xfctrl " + className;
    } else {
        className =  "xfctrl";
    }
    this.__hElem = makeElement(hParent, "div", className, null, hBefore);
    var iid = this.__elem.getAttribute("id");
    if (iid) {
    	className += " id_" + iid;
        iid = this.__form.__name + "_" + iid;
        this.__hElem.id = iid;
    }
    if (SH.is_gecko && this.__hElem.style.overflow == "") this.__hElem.style.overflow = "show";
    this.__hWidget = makeElement(this.__hElem, "textarea", null, null, null,
            { "name" : this.__ctrlId });
    if (iid) this.__hWidget.id = iid + "_val";
    this.__hWidget.value = this.getValue(xpCtx);
    this.__hWidget.__xfObj = this;
    this.__hWidget.onfocus = __xfControlInputFocus;
    this.__hWidget.onblur = __xfControlInputBlur;
    __xfCtrlCommonUI(this);
    return this.__hElem; // return the parent since html input don't have kids
};

function __xfControlInputFocus() {
    var xfo = this.__xfObj;
    var typeFmt = xfo.getTypeFormat(xfo.__xpCtx);
    typeFmt.onfocus(xfo);
    if (xfo.__hint != null) {
        xfo.__hint.show(this);
    } else {
        __xfHideHints();
    }
    window.setTimeout(
           function() { xfo.__model.__form.fireEvent("DOMFocusIn", xfo); }, 10);

}

function __xfControlInputBlur() {
     __xfHideHints();
    this.__xfObj.changeValue(true);
}

function __xfControlSelectFocus() {
    var xfo = this.__xfObj;
    if (xfo.__hint != null) {
        xfo.__hint.show(this.__hElem);
    } else {
        __xfHideHints();
    }
    window.setTimeout(
           function() { xfo.__model.__form.fireEvent("DOMFocusIn", xfo); }, 10);
}

XFControlSelect1.prototype = new XFCtrlBase();
XFControlSelect1.prototype.constructor = XFControlSelect1;

function XFControlSelect1(form, elem, pObj) {
    if (arguments.length > 0) {
        this.__initEvt(form, elem, pObj);
        this.__initBind(form, elem, pObj);
        this.__initCtrl();
        this.__appearance = elem.getAttribute("appearance");
        this.__columns = parseInt(elem.getAttribute("columns"));
        if (isNaN(this.__columns) || this.__columns < 1) this.__columns = 1;
        this.__multi = false;
    }
}

XFControlSelect1.prototype.__isSelected = function(value) {
    var selected = false;
    if (typeof value == "string") {
        if (this.__selVal instanceof Array) {
            selected = arrayFind(this.__selVal, value) >= 0;
        } else {
            if (this.__selVal == value) selected = true;
        }
    } else {
        var vNode = value.getValueNode(value.__xpCtx);
        var selNode = this.getValueNode(this.__xpCtx);
        var sKids = selNode.childNodes;
        selected = xmlNodeEqualInSet(vNode, sKids) >= 0;
    }
    return selected;
};

XFControlSelect1.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    var className = this.__elem.getAttribute("class");
    if (className) {
        className = "xfctrl " + className;
    } else{
        className = "xfctrl";
    }
    this.__items = [];
    this.__hElem = makeElement(hParent, "div", className, null, hBefore);
    var iid = this.__elem.getAttribute("id");
    if (iid) {
    	className += " id_" + iid;
        iid = this.__form.__name + "_" + iid;
        this.__hElem.id = iid;
    }
    if (SH.is_gecko && this.__hElem.style.overflow == "") this.__hElem.style.overflow = "show";
    this.__hElem.__xfObj = this;
    if (this.__appearance != "full") {
        /*if (this.__multi) {
            this.__hWidget = makeElement(
                this.__hElem, "select", null, null, null,
                {"multiple" : "multiple", "name" : this.__ctrlId,
                 "size" : 5});
        } else {
            this.__hWidget = makeElement(
                this.__hElem, "select", null, null, null,
                {"name" : this.__ctrlId});
        }*/
        this.__widget = new ComboBox(false, this.__multi, this.__elem.getAttribute("class"));
        this.__widget.onfocus = __xfControlSelectFocus;
        this.__widget.onchange = __xfControlSelectChange;
        this.__widget.onselect = __xfControlOnSelect;
        this.__widget.style = this.__elem.getAttribute("style");
        this.__widget.__xfObj = this;
        this.__widget.render(this.__hElem);
    } else {
        this.__hWidget = null;
        this.__className = className;
        this.__hGrid = makeLayoutTable(this.__hElem, "xfselect " + className);
        this.__colIdx = 0;
        this.__hWidgetSet = [];
    }
    this.__setSelVal(this.__multi ?
            this.getValue(xpCtx).split(/[\x0d\x0a\x09\x20]+/) :
            this.getValue(xpCtx));
    __xfCtrlCommonUI(this);
    return this.__hElem; // return the parent since html input don't have kids
};

XFControlSelect1.prototype.__setSelVal = function(val) {
	this.__selVal = val;
	if (this.__hElem) { // this is for testing/debugging/spying tools
		this.__hElem.setAttribute("value", (val instanceof Array) ? val.join(" ") : val);
	}
};


XFControlSelect1.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {

        this.__setSelVal(this.__multi ?
                this.getValue(this.__xpCtx).split(/[\x0d\x0a\x09\x20]+/) :
                this.getValue(this.__xpCtx));
        if (this.__widget) {
            this.__widget.setValue(this.__selVal, true);
        } else {
            for (var ii = 0; ii < this.__hWidgetSet.length; ii++) {
                var widget = this.__hWidgetSet[ii];
                if (widget.__xfCopyObj != null) {
                    // TODO - finish set checked or selected
                } else {
                    if ((this.__multi && arrayFind(this.__selVal, widget.value) >= 0)
                        || (!this.__multi && widget.value == this.__selVal)) {
                        widget.checked = true;
                    } else{
                        widget.checked = false;
                    }
                }
            }
        }
    }
    XFCtrlBase.prototype.valueChanged.apply(this, []);
};

XFControlSelect1.prototype.setHint = function(xfHint) {
    this.__hint = xfHint;
};

XFControlSelect1.prototype.dispose = function() {
    if (this.__widget != null) {
        this.__widget.__xfObj = null;
        this.__widget.dispose();
        this.__xfCopyObjs = null;
    } else if (this.__hWidgetSet != null ) {
        for (var i = 0; i < this.__hWidgetSet.length; i++) {
            this.__hWidgetSet[i].__xfCopyObj = null;
        }
        this.__hGrid = null;
    }
    this.__items = null;
    XFCtrlBase.prototype.dispose.apply(this, []);
};

function __xfGetCheckSet(xfo) {
    var vals = [];
    for (var i = 0; i < xfo.__hWidgetSet.length; i++) {
        var widget = xfo.__hWidgetSet[i];
        if (widget.checked) {
            if (widget.__xfCopyObj != null) vals.push(widget.__xfCopyObj);
            else vals.push(widget.value);
        }
    }
    return vals;
}

function __xfControlItemCopyProcess() {
	var model = this.__model;
    var form = model.__form;
    model.__doRebuild();
    model.__doRecalculate();
    model.__doRevalidate();
    form.fireEvent("xforms-value-changed", this);
    form.fireEvent("xforms-select", this);
    model.__skipValueChange = this;
    model.__doRefresh();
}

function __xfControlCheckClick(evt) {
    var xfo = this.__xfObj;
    var model = xfo.__model;
    var holdThis = this;
    window.setTimeout(
        function() {
        	var form = model.__form;
        	form.fireEvent("xforms-deselect", xfo);
            if (holdThis.__xfCopyObj != null) {
                var vals = __xfGetCheckSet(xfo);
                if (model.__replaceNodesBound(
                        xfo.getValueNode(xfo.__xpCtx), vals)) {
                    window.setTimeout(function() { __xfControlItemCopyProcess.apply(xfo, []); }, 10);
                } else {
                	window.setTimeout(function() { form.fireEvent("xforms-select", xfo); }, 10);
                }
            } else {
                xfo.__selVal = __xfGetCheckSet(xfo);
                if (model.__setNodeValue(
                        xfo.getValueNode(xfo.__xpCtx),
                        xfo.__selVal.join(" "))) {
                    window.setTimeout(function() { __xfControlValueProcess.apply(xfo, [false, true]); }, 10);
                } else {
                	window.setTimeout(function() { form.fireEvent("xforms-select", xfo); }, 10);
                }
            }
            
        }, 50);
}

function __xfControlSelectChange(evt) {
    var xfo = this.__xfObj;
    var model = xfo.__model;
    var form = model.__form;
    form.fireEvent("xforms-deselect", xfo);
    if (xfo.__xfCopyObjs != null) {
        var sv = this.__selectedItems;
        var vals = [];
        for (var ii = 0; ii < sv.length; ii++) {
            vals.push(xfo.__xfCopyObjs[sv[ii].value]);
        }
        if (model.__replaceNodesBound(xfo.getValueNode(xfo.__xpCtx), vals)) {
            window.setTimeout(function() { __xfControlItemCopyProcess.apply(xfo, []); }, 10);
        } else {
        	window.setTimeout(function() { form.fireEvent("xforms-select", xfo); }, 10);
        }
    } else {
        //xfo.__selVal = xfo.__multi ?
        //    getMultiSelectVal(this) : getSelectVal(this);
        xfo.__setSelVal(this.__selectedItems[0].value);
        if (model.__setNodeValue(xfo.getValueNode(xfo.__xpCtx), xfo.__selVal)) {
            window.setTimeout(function() { __xfControlValueProcess.apply(xfo, [true, true]); }, 10);
        } else {
        	window.setTimeout(function() { form.fireEvent("xforms-select", xfo); }, 10);
        }
    }
}

function __xfControlOnSelect(evt) {
	var xfo = this.__xfObj;
	window.setTimeout(function() {
			var model = xfo.__model;
    		var form = model.__form;
			form.fireEvent("xforms-deselect", xfo); 
			form.fireEvent("xforms-select", xfo);
			form.fireEvent("DOMFocusOut", xfo);
		}, 10);
}

XFControlSelect.prototype = new XFControlSelect1();
XFControlSelect.prototype.constructor = XFControlSelect;

function XFControlSelect(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
    //this.__appearance = elem.getAttribute("appearance");
    // TODO - multi-combobox
    this.__appearance = "full";
    this.__columns = parseInt(elem.getAttribute("columns"));
    if (isNaN(this.__columns) || this.__columns < 1) this.__columns = 1;
    this.__multi = true;
}



XFControlOutput.prototype = new XFCtrlBase();
XFControlOutput.prototype.constructor = XFControlOutput;

function XFControlOutput(form, elem, pObj) {
    this.noEdit = true;
    if (arguments.length > 0) {
        this.__initEvt(form, elem, pObj);
        this.__bindRequired = false;
        this.__initBind(form, elem, pObj);
        this.__initCtrl();
        this.__value = elem.getAttribute("value");
        this.__appearance = elem.getAttribute("appearance");
    }
}

XFControlOutput.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    var attrs = new Object();
    if (this.__appearance == "link") {
        attrs.href = this.getValue(xpCtx);
        var targ = this.__elem.getAttribute("target");
        if (targ) {
            attrs.target = targ;
        }
    }
    this.__hElem = makeElement(
        hParent, this.__appearance == "link" ? "a" : "span",
        this.__elem.getAttribute("class"),
        this.__appearance == "link" ? "" : this.getValue(xpCtx),
        hBefore, attrs);
   	var iid = this.__elem.getAttribute("id");
    if (iid) {
    	className += " id_" + iid;
        iid = this.__form.__name + "_" + iid;
        this.__hElem.id = iid;
    }
    this.__hElem.__xfObj = this;
    __xfCtrlCommonUI(this);
    return this.__appearance == "link" ? this.__hElem : hParent;
};

XFControlOutput.prototype.getValue = function(xpCtx) {
    if (this.__ref || this.__bind) {
        var val = XFSingleNodeBinding.prototype.getValue.apply(this, [xpCtx]);
        var typeFmt = this.getTypeFormat(xpCtx);
        val = typeFmt.format(val, this);
        this.__lastValue = val;
        return val;
    } else if (this.__value) {
        this.isValue = true;
        var resExp;
        try {
            if (this.__valueXP == null) {
                this.__valueXP = this.__model.__xpathCache(this.__value);
            }
            if (this.__depList == null)  {
                xpCtx.depList = [];
                resExp = this.__form.__xPathSelectComp(xpCtx, this.__valueXP);
                if (xpCtx.depList.length == 0) {
                    this.__nullDep = true;
                    this.__model.__deps.addNullDepend(this);
                } else {
                    this.__model.__deps.addDependSet(xpCtx.depList, this);
                    this.__depList = xpCtx.depList;
                }
                xpCtx.depList = null;
            } else {
                resExp = this.__form.__xPathSelectComp(xpCtx, this.__valueXP);
            }
        } catch (e) {
            this.__model.__form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        this.__lastValue = resExp ? resExp.stringValue() : null;
        return this.__lastValue;
    }
    return "";
};

XFControlOutput.prototype.rebuild = function() {
    XFCtrlBase.prototype.rebuild.apply(this, []);
    this.__lastValue = null;
};

XFControlOutput.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        var lv = this.__lastValue;
        if (lv != this.getValue(this.__xpCtx)) {
            if (this.__appearance == "link") {
                this.__hElem.href = this.__lastValue;
            } else {
                setElementText(this.__hElem, this.__lastValue);
            }
        }
        if (!this.isValue) {
            XFCtrlBase.prototype.valueChanged.apply(this, []);
        } else if (SH.debug) {
            SH.println("vc: xf:output");
        }
    }
};

XFControlOutput.prototype.dispose = function() {
    XFCtrlBase.prototype.dispose.apply(this, []);
    this.__lastValue = null;
    this.__valueXP = null;
};

XFLabel.prototype = new XFCtrlBase();
XFLabel.prototype.constructor = XFLabel;

function XFLabel(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
    this.__ignoreText = false;
}

XFLabel.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    if (this.__elem.firstChild != null
	        || this.__bind
	        || this.__ref) {
        var par = this.__elem.parentNode;
        var pln = xmlGetLocalName(par);
        var val = this.getValue(xpCtx);
        var doc = hParent ? hParent.ownerDocument : document;
        if (this.__parent.__setXFLabel != null) {
            this.__hElem = makeElement(null, "span",
                this.__elem.getAttribute("class"));
            if (val) {
                this.__hText = doc.createTextNode(val);
                this.__hElem.appendChild(this.__hText);
            }
            this.__parent.__setXFLabel(this);
        } else {
            var tag = "label";
            if (pln == "group") {
                 tag = "legend";
            } else if (this.__parent.constructor === XFControlOutput &&
                this.__parent.__appearance == "link") {
                tag = "span";
            }
            this.__hElem = makeElement(hParent, tag,
                this.__elem.getAttribute("class"), null,
                pln == "group" ? hBefore : xmlFirstElement(hParent));
            if (val) {
                this.__hText = doc.createTextNode(val);
                this.__hElem.appendChild(this.__hText);
            }
            this.__hElem.__xfObj = this;
        }
        __xfCtrlCommonUI(this);
        return this.__hElem;
    }
    return hParent;
};

XFLabel.prototype.__endRender = function() {
	if (this.__ctrlId) XFCtrlBase.prototype.__endRender.apply(this, []);
};

XFLabel.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
    	if (!this.__hText) {
    		this.__hText = this.__hElem.ownerDocument.createTextNode("");
          	this.__hElem.appendChild(this.__hText);
    	}
        this.__hText.nodeValue = this.getValue(this.__xpCtx);
    }
    XFCtrlBase.prototype.valueChanged.apply(this, []);
};


XFItem.prototype = new XFEventModel();
XFItem.prototype.constructor = XFItem;

function XFItem(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__ignoreText = true;
    var p = pObj;
    while (p != null) {
        if (p.constructor === XFControlSelect1
                || p.constructor === XFControlSelect) {
            this.__select = p;
            break;
        }
        p = p.__parent;
    }
}

XFItem.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFItem.prototype.__setXFLabel = function(xfo) {
    this.__labelObj = xfo;
};

XFItem.prototype.__setXFValue = function(xfo) {
    this.__valueObj = xfo;
    xfo.__itemObj = this;
};

XFItem.prototype.__calcValue = function() {
    var value;
    if (this.__valueObj.constructor === XFCopy) {
        value = this.__valueObj;
    } else {
        value = xmlStringForNode(this.__valueObj.__hElem);
        if (value == null) value = "";
    }
    if (arrayFindStrict(this.__select.__items, this) < 0)
    this.__select.__items.push(this);
    this.__curVal = value;
    if (this.__wItem) this.__wItem.value = value;
};

XFItem.prototype.__endRender = function() {
    var label = this.__labelObj ? this.__labelObj.__hElem : null;
    var isCopy = (this.__valueObj && this.__valueObj.constructor === XFCopy);
    var isSep = (this.__elem && this.__elem.getAttribute("separator") == "true");
    var value, i, selected  = false;
    if (!isSep) {
    	this.__calcValue();
  		value = this.__curVal;
  		selected = this.__select.__isSelected(value);
    }
    
    if (this.__select.__appearance != "full") {
    	if (isSep) {
    		this.__wItem = this.__select.__widget.addSeparator();
    	} else {
	        var v;
	        if (isCopy) {
	            var co = this.__select.__xfCopyObjs;
	            if (co == null) {
	                co = [];
	               this.__select.__xfCopyObjs = co;
	            }
	            var idx = co.length;
	            co.push(value);
	            v = idx;
	        } else {
	            v = value;
	        }
	        var icon = this.__elem ? this.__elem.getAttribute("icon") : null;
	        this.__wItem = this.__select.__widget.addLabelElementOption(
	        	label, v, selected, icon);
    	}
    } else {
    	
    	var tr;
    	if (this.__select.__colIdx < 1) {
    		this.__select.__colIdx = 1;
    		tr = makeElement(this.__select.__hGrid, "tr", "item");
    	}  else if (this.__select.__colIdx < this.__select.__columns) {
    		this.__select.__colIdx++;
    		tr = this.__select.__hGrid.lastChild;
    	} else {
    		tr = makeElement(this.__select.__hGrid, "tr", "item");
    		this.__select.__colIdx = 1;
    	}
        if (isSep) {
        	this.__hElem1 = makeElement(makeElement(tr, "td", "xfSep", null, null, {colspan : 2}), "hr");
        } else {
	        this.__hElem1 = makeElement(tr, "td");
	        var attrs = {"name" : this.__select.__ctrlId};
	        if (selected) {
	            attrs.checked = selected;
	        }
	        var widget = makeElement(this.__hElem1, "input", "xfitem",
	                this.__select.__multi ? "checkbox" : "radio", null, attrs);
	        widget.value = isCopy ? "cc" : value;
	        widget.onclick = __xfControlCheckClick;
	        if (isCopy) widget.__xfCopyObj = value;
	        widget.__xfObj = this.__select;
	        this.__select.__hWidgetSet.push(widget);
	        this.__hElem2 = makeElement(tr, "td");
	        if (label) this.__hElem2.appendChild(label);
        }
    }
};

XFItem.prototype.remove = function() {
	if (this.__hElem1 != null) {
		// remove whole row?
		if (this.__hElem1.parentNode != null
				&& this.__hElem1.parentNode.childNodes.length <= 2) {
			XFHtml.__removeElement(this.__hElem1.parentNode);
		} else {
			XFHtml.__removeElement(this.__hElem1);
   		 	XFHtml.__removeElement(this.__hElem2);
		}
	}
	
    if (this.__select.__widget != null &&  this.__wItem != null) {
        this.__select.__widget.removeOptionItem(this.__wItem);
    }
    arrayRemoveStrict(this.__select.__items, this);
    XFEventModel.prototype.remove.apply(this, []);
};

XFItem.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__hElem1 = null;
    this.__hElem2 = null;
    this.__select = null;
    this.__wItem = null;
    this.__curVal = null;
    this.__labelObj = null;
    this.__valueObj = null;
};


XFValue.prototype = new XFCtrlBase();
XFValue.prototype.constructor = XFValue;

function XFValue(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
    this.__ignoreText = false;
    this.__value = elem.getAttribute("value");
}

// copy outputs getValue function
XFValue.prototype.getValue = XFControlOutput.prototype.getValue;

XFValue.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(null, "span");
    var val = this.getValue(xpCtx);
    if (val) {
    	var doc = hParent ? hParent.ownerDocument : document;
        this.__hText = doc.createTextNode(val);
        this.__hElem.appendChild(this.__hText);
    }
    this.__parent.__setXFValue(this);
    return this.__hElem;
};

XFValue.prototype.__endRender = function() {
	if (this.__ctrlId) XFCtrlBase.prototype.__endRender.apply(this, []);
};

XFValue.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
    	if (!this.__hText) {
    		this.__hText = this.__hElem.ownerDocument.createTextNode("");
          	this.__hElem.appendChild(this.__hText);
    	}
        this.__hText.nodeValue = this.getValue(this.__xpCtx);
        if (this.__itemObj != null) this.__itemObj.__calcValue();
    }
    XFCtrlBase.prototype.valueChanged.apply(this, []);
};

XFValue.prototype.dispose = function() {
    XFCtrlBase.prototype.dispose.apply(this, []);
    this.__itemObj = null;
};


XFCopy.prototype = new XFCtrlBase();
XFCopy.prototype.constructor = XFCopy;

function XFCopy(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
}

XFCopy.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(null, "span");
    var val = this.getValue(xpCtx);
    if (val) {
    	var doc = hParent ? hParent.ownerDocument : document;
        this.__hText = doc.createTextNode(val);
        this.__hElem.appendChild(this.__hText);
    }
    this.__parent.__setXFValue(this);
    return this.__hElem;
};

XFHint.prototype = new XFEventModel();
XFHint.prototype.constructor = XFHint;

function XFHint(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
}

XFHint.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(null, "span");
    if (this.__parent.setHint != null) {
        this.__parent.setHint(this);
    }
    return this.__hElem;
};

XFHint.prototype.show = function(hParent) {
    Ephemeral.showAtTop(hParent, this.__hElem);
};

XFHint.prototype.remove = function() {
    XFHtml.__removeElement(this.__hElem);
    XFEventModel.prototype.remove.apply(this, []);
};

XFHint.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__hElem = null;
};

XFControlGroup.prototype = new XFCtrlBase();
XFControlGroup.prototype.constructor = XFControlGroup;

function XFControlGroup(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
    this.__ignoreText = false;
}

XFControlGroup.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(
        hParent, "fieldset", this.__elem.getAttribute("class"), null, hBefore);
    this.__hElem.__xfObj = this;
    __xfCtrlCommonUI(this);
    return this.__hElem;
};


XFControlSubmit.prototype = new XFCtrlBase();
XFControlSubmit.prototype.constructor = XFControlSubmit;

function XFControlSubmit(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
    this.__submission = elem.getAttribute("submission");
}

XFControlSubmit.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(
        hParent, "button", "xfctrl " + this.__elem.getAttribute("class"), null, hBefore);
    this.__hWidget = this.__hElem;
    this.__hElem.setAttribute("type", "button");
    this.__hElem.__xfObj = this;
    this.__hElem.onclick = __xfControlSubmitClick;
    __xfCtrlCommonUI(this);
    return this.__hElem;
};

function __xfControlSubmitClick(evt) {
    var xfo = this.__xfObj;
    xfo.__model.__form.fireEvent("DOMActivate", xfo);
   	if (xfo.__hElem) exAddClass(xfo.__hElem, "submitting");
   	window.setTimeout(function() {
    xfo.__model.submit(xfo.__submission);
   	  	if (xfo.__hElem) exAddClass(xfo.__hElem, "submitting", true); 
   	}, 50);
}


XFControlTrigger.prototype = new XFCtrlBase();
XFControlTrigger.prototype.constructor = XFControlTrigger;

function XFControlTrigger(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
}

XFControlTrigger.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    var cn = this.__elem.getAttribute("class");
    var fcn = "xfctrl";
    if (cn) fcn += " " + cn;
    this.__hElem = makeElement(hParent, "button", fcn, null, hBefore);
    this.__hElem.setAttribute("type", "button");
    this.__hElem.__xfObj = this;
    this.__hWidget = this.__hElem;
    this.__hElem.onclick = __xfControlTriggerClick;
    __xfCtrlCommonUI(this);
    return this.__hElem;
};

function __xfControlTriggerClick(evt) {
    this.__xfObj.__model.__form.fireEvent("DOMActivate", this.__xfObj);
}


XFItemset.prototype = new XFSetCtrlBase();
XFItemset.prototype.constructor = XFItemset;

function XFItemset(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
}

XFItemset.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hParent = hParent;
    this.__renderedNs = this.getNodeSet(xpCtx);
    return hParent;
};

XFItemset.prototype.__setXFLabel = function(xfo) {
    this.__labelObj = xfo;
};

XFItemset.prototype.__setXFValue = function(xfo) {
    this.__valueObj = xfo;
};

XFItemset.prototype.__endRender = function() {
    var itm = new XFItem(this.__model.__form, null, this);
    itm.__setXFLabel(this.__labelObj);
    itm.__setXFValue(this.__valueObj);
    itm.__repeatPos = this.__valueObj.__xpCtx.position - 1;
    itm.render(this.__xpCtx, this.__hParent);
    itm.__endRender();
    this.__labelObj = null;
    this.__valueObj = null;
};

XFItemset.prototype.valueChanged = function() {
    XFSetCtrlBase.prototype.valueChanged.apply(this, []);
    if (this.__xpCtx == null) return;
    var sel = this.__parent;
    var i, jj;
    var missing = [];
    var keep = [];
    var isCopy = false;
    if (sel.__xfCopyObjs == null) {
        if (sel.__selVal instanceof Array) {
            for (i = 0; i < sel.__selVal.length; i++) {
                missing.push(sel.__selVal[i]);
            }
        } else {
            missing.push(sel.__selVal);
        }
        for (i = missing.length - 1; i >= 0; i--) {
            var mval = missing[i];
            for (jj = 0; jj < sel.__items.length; jj++) {
                var val = sel.__items[jj].__curVal;
                if (val == mval) {
                    keep.push(missing[i]);
                    missing.splice(i, 1);
                    break;
                }
            }
        }
    } else {
        isCopy = true;
        var ns = [];
        for (i = 0; i < sel.__items.length; i++) {
            var bo = sel.__items[i].__valueObj;
            ns.push(bo.getValueNode(bo.__xpCtx));
        }
        var kids = sel.getValueNode(sel.__xpCtx).childNodes;
        for (i = 0; i < kids.length; i++) {
            missing.push(kids[i]);
        }
        for (i = missing.length - 1; i >= 0; i--) {
            var idx = xmlNodeEqualInSet(missing[i], ns);
            if (idx >= 0) {
                keep.push(sel.__items[idx].__valueObj);
                missing.splice(i, 1);
            }
        }
    }
    if (missing.length > 0) {
        if (!sel.__multi && sel.__items[0]) {
            keep = [sel.__items[0].__curVal];
        }
        if (isCopy) {
            sel.__model.__replaceNodesBound(
                    sel.getValueNode(sel.__xpCtx), keep);
        } else {
            sel.__selVal = sel.__multi ? keep : keep[0];
            sel.__model.__setNodeValue(
                sel.getValueNode(sel.__xpCtx), keep.join(" "));
        }
        sel.valueChanged();
    }
};


XFRepeat.prototype = new XFSetCtrlBase();
XFRepeat.prototype.constructor = XFRepeat;

function XFRepeat(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
    var id = this.__id;
    if (id) {
        form.__repeats[id] = this;
    }
}

XFRepeat.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    if (this.__parent === this.__model.__form
        	|| this.__parent.constructor === XFRepeat) {
        this.__hParent = makeElement(hParent, "div");
    } else {
        this.__hParent = hParent;
    }
    this.__renderedNs = this.getNodeSet(xpCtx);
    return this.__hParent;
};


XFSwitch.prototype = new XFCtrlBase();
XFSwitch.prototype.constructor = XFSwitch;

function XFSwitch(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
    this.__caseShutter = null;
    var cp = pObj;
    while (cp !== form) {
    	if (cp.__cases) {
    		this.__caseShutter = cp;
    		break;
    	}
    	cp = cp.__parent;
    }
    this.__selectedCase = null;
}

XFSwitch.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFCase.prototype = new XFEventModel();
XFCase.prototype.constructor = XFCase;

function XFCase(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__ignoreText = true;
    this.__active = false;
    // XF 1.1 <toggle><case value=""/></toggle>
    if (pObj.constructor === XFToggle) {
		pObj.__caseValue = elem.getAttribute("value");
		return;
	}
    this.__selected = elem.getAttribute("selected");
    if (pObj.constructor === XFSwitch) {
        this.__switch = pObj;
        this.__exfIf = Utilities.getAttributeNS(elem, XFORM_EXTENDED_NAMESPACE, "if");
        this.__model = form.__defaultModel;
        if (!this.__selected) {
            if (this.__switch.__selectedCase == null) {
                this.__switch.__selectedCase = this;
                this.__active = true;
            }
        } else {
            var ch = this.__selected ? this.__selected.charAt(0) : "f";
            if ("tT1".indexOf(ch) >= 0) {
                if (this.__switch.__selectedCase == null) {
                    this.__switch.__selectedCase = this;
                    this.__active = true;
                } else {
                    var sc = this.__switch.__selectedCase;
                    if (!sc.__selected) {
                        this.__switch.__selectedCase = this;
                        this.__active = true;
                        sc.deactivate(true);
                    }
                }
            }
        }
    }
    if (this.__switch.__caseShutter) this.__switch.__caseShutter.__cases[this.__id] = this;
    form.__cases[this.__id] = this;
}

XFCase.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hParent = hParent;
     if (!__XFActUtil.__exfIf(this)) {
     	this.__active = false;
     	if (this.__switch.__selectedCase === this)
     		this.__switch.__selectedCase = null;
     }
    return this.__active ? hParent : null;
};

XFCase.prototype.activate = function() {
    var lastCase = this.__switch.__selectedCase;
    if (lastCase !== this) {
        if (lastCase != null) {
            lastCase.deactivate();
        }
       	var hBefore = null;
       	var parSw = this.__switch;
       	if (parSw.__parent) {
			var idx = arrayFindStrict(parSw.__parent.__children, parSw) + 1;
			if (idx < parSw.__parent.__children.length) {
			    kid = parSw.__parent.__children[idx];
			    if (kid.__hElem != null) hBefore = kid.__hElem;
			    else hBefore = kid.__hNode;
			}
       	}
        this.__form.__doXformBody(this.__elem, this.__xpCtx, this.__hParent, this, hBefore);
       
       
        this.__switch.__selectedCase = this;
        this.__form.fireEvent("xforms-select", this);
    }
};

XFCase.prototype.deactivate = function(skipEvent) {
    for (var ii = this.__children.length - 1; ii >= 0; ii--) {
    	this.__children[ii].remove();
    }
    if (skipEvent == null) this.__form.fireEvent("xforms-deselect", this);
};

XFCase.prototype.remove = function() {
    if (this.__form.__cases[this.__id] == this) {
        this.__form.__cases[this.__id] = null;
    }
    if (this.__switch.__selectedCase == this) {
        this.__switch.__selectedCase = null;
    }
    XFEventModel.prototype.remove.apply(this, []);
};


XFCase.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__switch = null;
    this.__hParent = null;
    this.__xpCtx = null;
};

XFMessage.prototype = new XFBoundActionBase();
XFMessage.prototype.constructor = XFMessage;

function XFMessage(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__level = elem.getAttribute("level");
    this.__inlineText = __xfFlattenSpace(xmlStringForNode(elem));
}

XFMessage.prototype.perform = function(xpCtx, defUp) {
    var msg;
    if (this.__ref || this.__bind) {
        msg  = this.getValue(xpCtx);
    } else {
        msg = this.__inlineText;
    }
    if (this.__level == "ephemeral") {
        setElementText(this.__hElem, msg);
        Ephemeral.showAtTop(this.__hParent, this.__hElem);
    } else {
        window.alert(msg);
    }
};

XFMessage.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    if (this.__level == "ephemeral") {
        this.__hElem = makeElement(null, "span");
        this.__hParent = hParent;
    }
    return hParent;
};

XFMessage.prototype.dispose = function() {
    XFBoundActionBase.prototype.dispose.apply(this, []);
    if (this.__level == "ephemeral") {
        this.__hElem = null;
        this.__hParent = null;
    }
};


XFExfVariable.prototype = new XFCtrlBase();
XFExfVariable.prototype.constructor = XFExfVariable;

function XFExfVariable(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
    this.__name = elem.getAttribute("name");
    this.__value = elem.getAttribute("value");
    this.isValue = true;
}

XFExfVariable.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = {
        node : xpCtx.node,
        position : xpCtx.position,
        size : xpCtx.size,
        varRes : xpCtx.varRes }; // make copy since may recurse on call
    this.__xpCtx.varRes.setVariableWithName(null, this.__name, this);
    return hParent;
};

XFExfVariable.prototype.evaluate = function(cCtx) {
    if (this.__xpCtx != null) {
        if (SH.debug) {
            SH.println("eval $" + this.__name);
        }
        return this.getValue(this.__xpCtx, cCtx);
    }
    return null;
};

XFExfVariable.prototype.getValue = function(xpCtx, cCtx) {
    var retVal = "";
    if (this.__lastValue != null) {
        retVal = this.__lastValue;
    } else if (this.__value) {
        var resExp;
        try {
            if (this.__valueXP == null) {
                this.__valueXP = this.__model.__xpathCache(this.__value);
            }
            if (this.__depList == null)  {
                xpCtx.depList = [];
                resExp = this.__form.__xPathSelectComp(xpCtx, this.__valueXP);
                if (resExp == null ||
                    (resExp.constructor === XNodeSet && resExp.first() != null)) {
                    Utilities.addDeps(xpCtx, [resExp.first().parentNode]);
                }
                this.__model.__deps.addDependSet(xpCtx.depList, this);
                this.__depList = xpCtx.depList;
                xpCtx.depList = null;
           } else {
                resExp = this.__form.__xPathSelectComp(xpCtx, this.__valueXP);
            }
            if (resExp == null ||
                (resExp.constructor === XNodeSet && resExp.first() == null)) {
                this.__nullDep = true;
                this.__model.__deps.addNullDepend(this);
                if (this.__depList != null)
                    this.__model.__deps.removeDependSet(this.__depList, this);
                this.__depList = null;
            }
        } catch (e) {
            this.__model.__form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        this.__lastValue = resExp;
        retVal = resExp;
    }
    if (cCtx.depList != null && this.__depList != null) {
        Utilities.addDeps(cCtx, this.__depList);
    }
    return retVal;
};

XFExfVariable.prototype.rebuild = function() {
    if (SH.debug) SH.println("rb: " + this);
    if (this.__nullDep) {
        this.__nullDep = false;
        this.__model.__deps.removeNullDepend(this);
    }
    if (this.__depList != null)
        this.__model.__deps.removeDependSet(this.__depList, this);
    this.__depList = null;
    this.__lastValue = null;
};


XFExfVariable.prototype.dispose = function() {
    XFCtrlBase.prototype.dispose.apply(this, []);
    this.__lastValue = null;
    this.__valueXP = null;
};

XFExfVariable.prototype.valueChanged = function() {
    if (SH.debug) SH.println("vc: exf:variable");
    this.__lastValue = null;
};

XFIxAttribute.prototype = new XFControlOutput();
XFIxAttribute.prototype.constructor = XFIxAttribute;

function XFIxAttribute(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__initCtrl();
    this.__name = elem.getAttribute("name");
    this.__value = elem.getAttribute("value");
}

XFIxAttribute.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = hParent;
    this.__setAttribute(this.getValue(xpCtx));
    return hParent;
};

XFIxAttribute.prototype.__setAttribute = function(val) {
    if (this.__name == "style") {
        __xfSetCss(this.__form, this.__hElem, val);
    } else if (this.__name == "class") {
        if (this.__lastClass) { //remove old
            exAddClass(this.__hElem, this.__lastClass, true);
        }
        if (val != "") exAddClass(this.__hElem, val);
        this.__lastClass = val;
    } else if (this.__name == "src") {
        this.__hElem.setAttribute(this.__name,
                Uri.reduce(this.__form.resolveUri(val)));
    } else if (this.__name == "rowspan") {
        this.__hElem.setAttribute("rowSpan", val);
    } else {
        this.__hElem.setAttribute(this.__name, val);
    }
};

XFIxAttribute.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        var lv = this.__lastValue;
        var nv = this.getValue(this.__xpCtx);
        if (lv != nv) {
            this.__setAttribute(nv);
        }
        if (!this.isValue) {
            XFCtrlBase.prototype.valueChanged.apply(this, []);
        } else if (SH.debug) {
            SH.println("vc: ix:attr");
        }
    }
};


XFTypeDateFmt.prototype = new XFTypeFormat();
XFTypeDateFmt.prototype.constructor = XFTypeDateFmt;
function XFTypeDateFmt(){
    this.__useTime = false;
}

XFTypeDateFmt.prototype.format = function(str) {
    var d = DateUtil.parse8601(str, this.__useTime);
    if (d == null) return "";
    return DateUtil.toLocaleShort(d, this.__useTime);
};

XFTypeDateFmt.prototype.parse = function(str) {
    var d = DateUtil.parseLocaleShort(str);
    if (d != null) {
        return DateUtil.to8601(d, this.__useTime);
    }
    return null;
};

XFTypeDateFmt.prototype.decorate = function(uiElem, ctrl) {
    if (!ctrl.noEdit) {
        var calElem = makeElementNbSpd(uiElem.parentNode, "span", "xfCal");
        ctrl._fmtDateElem = uiElem;
        var useTime = this.__useTime;
        setEventHandler(calElem, "onclick",
            function (evt) {
                calPopShow(evt, this, ctrl, useTime);
                ctrl._fmtDateElem.focus();
            });
        calElem = null; uiElem = null; // IE enclosure clean-up
    }
};

XFTypeDateFmt.prototype.disposeDecor = function(uiElem, ctrl) {
    if (!ctrl.noEdit) {
        ctrl._fmtDateElem = null;
    }
};


XFTypeDateTimeFmt.prototype = new XFTypeDateFmt();
XFTypeDateTimeFmt.prototype.constructor = XFTypeDateTimeFmt;

function XFTypeDateTimeFmt() {
    this.__useTime = this;
}


XFTypeTimeFmt.prototype = new XFTypeFormat();
XFTypeTimeFmt.prototype.constructor = XFTypeTimeFmt;
function XFTypeTimeFmt() {
}

XFTypeTimeFmt.prototype.format = function(str) {
    var val = DateUtil.toLocaleTime(str);
    return val == null ? "" : val;
};

XFTypeTimeFmt.prototype.parse = function(str) {
    return DateUtil.parseLocaleTime(str);
};

XFTypeFormat.addFormat(XFORM_SCHEMA_NAMESPACE, "date", new XFTypeDateFmt());
XFTypeFormat.addFormat(XFORM_SCHEMA_NAMESPACE, "dateTime", new XFTypeDateTimeFmt());
XFTypeFormat.addFormat(XFORM_SCHEMA_NAMESPACE, "time", new XFTypeTimeFmt());


//ix:currency* 
XFIXTypeCurrencyFmt.prototype = new XFTypeFormat();
XFIXTypeCurrencyFmt.prototype.constructor = XFIXTypeCurrencyFmt;
function XFIXTypeCurrencyFmt(symb) {
	this.symb = symb;
	this.cssClass = "xfnum";
}

XFIXTypeCurrencyFmt.prototype.format = function(str) {
    var val = parseFloat(str);
    if (isNaN(val)) return str;
    return this.symb + numberFormat(val, 2);
};

XFIXTypeCurrencyFmt.prototype.parse = function(str) {
	return stringTr(str, this.symb + ",", "");
};

XFIXTypeCurrencyFmt.prototype.onfocus = function(ctrl) {
    if (ctrl.constructor === XFControlInput
            || ctrl.constructor === XFControlTextarea) {
        ctrl.__hWidget.value =
            XFSingleNodeBinding.prototype.getValue.apply(ctrl, [ctrl.__xpCtx]);
        if (SH.is_ie) ieMoveCursorToEnd(ctrl.__hWidget);
    }
};

XFIXTypeCurrencyFmt.prototype.decorate = function(uiElem, ctrl) {
	uiElem.className += " xfnum";
};


//ix:percent
XFIXTypePercentFmt.prototype = new XFTypeFormat();
XFIXTypePercentFmt.prototype.constructor = XFIXTypePercentFmt;
function XFIXTypePercentFmt() {
	this.cssClass = "xfnum";
}

XFIXTypePercentFmt.prototype.format = function(str) {
    var val = parseFloat(str);
    if (isNaN(val)) return str;
    return numberFormat(val * 100, 1, null, null, true) + "%";
};

XFIXTypePercentFmt.prototype.parse = function(str) {
	str = stringTr(str, "%,", "");
    return parseFloat(str) / 100;
};

XFIXTypePercentFmt.prototype.onfocus = function(ctrl) {
    if (ctrl.constructor === XFControlInput
            || ctrl.constructor === XFControlTextarea) {
        var str =
            XFSingleNodeBinding.prototype.getValue.apply(ctrl, [ctrl.__xpCtx]);
        var val = parseFloat(str);
        if (!isNaN(val)) str = val * 100;
        ctrl.__hWidget.value = str;
        if (SH.is_ie) ieMoveCursorToEnd(ctrl.__hWidget);
    }
};

XFIXTypePercentFmt.prototype.decorate = function(uiElem, ctrl) {
	uiElem.className += " xfnum";
};


XFIXTypeEstimateFmt.prototype = new XFTypeFormat();
XFIXTypeEstimateFmt.prototype.constructor = XFIXTypeEstimateFmt;
function XFIXTypeEstimateFmt() {
	this.cssClass = "xfnum";
}

XFIXTypeEstimateFmt.prototype.format = function(str) {
    var val = parseFloat(str);
    if (isNaN(val)) return str;
    return numberFormat(val, 2, null, null, true);
};

XFIXTypeEstimateFmt.prototype.onfocus = function(ctrl) {
    if (ctrl.constructor === XFControlInput
            || ctrl.constructor === XFControlTextarea) {
        var str =
            XFSingleNodeBinding.prototype.getValue.apply(ctrl, [ctrl.__xpCtx]);
        var val = parseFloat(str);
        if (!isNaN(val)) str = numberFormat(val, 2, ".", "", true);
        ctrl.__hWidget.value = str;
        if (SH.is_ie) ieMoveCursorToEnd(ctrl.__hWidget);
    }
};

XFIXTypeEstimateFmt.prototype.decorate = function(uiElem, ctrl) {
	uiElem.className += " xfnum";
};

XFIXUniqueId.prototype = new XFTypeFormat();
XFIXUniqueId.prototype.constructor = XFIXUniqueId;

function XFIXUniqueId() {
}

XFIXUniqueId.prototype.parse = function(str, ctrl) {
    if (ctrl.__oldUId != str) {
        var idCtx = ctrl.__model.getIdContext();
        idCtx.removeVar(ctrl.__oldUId);
        str = idCtx.uniqueVar(str);
        idCtx.addVar(str);
        ctrl.__oldUId  = str;
    }
    return str;
};

XFIXUniqueId.prototype.format = function(str, ctrl) {
    ctrl.__oldUId = str;
    return str;
};

XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "currencyUSD", new XFIXTypeCurrencyFmt("$"));
XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "currencyGBP", new XFIXTypeCurrencyFmt("\u00A3"));
XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "currencyEUR", new XFIXTypeCurrencyFmt("\u20AC"));

XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "percent", new XFIXTypePercentFmt());

XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "estimate", new XFIXTypeEstimateFmt());

XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "uniqueId", new XFIXUniqueId());

XFormUI.prototype = new XFEventModel();
XFormUI.prototype.constructor = XFormUI;
XFormUI.guidFlashUri = "../view-xfrm/form-guide.swf";

function XFormUI() {
    this.__fgStepNum = 0;
}

XFormUI.prototype.__formGuideCreateFlash = function() {

    if (this.__fgFlash == null) {
        var uri = XFormUI.guidFlashUri;
        var oe;
        var div = makeElement(
            this.__uiParent, "div", null, null, this.__uiParent.firstChild);
        div.style.position = "relative";
        div.style.zIndex = 50;
        if (SH.is_ie) {
            div.innerHTML ='<object classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000" codebase="http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,0,0" width="379" height="65" style="position:absolute;z-index:50">'
            + '<param name="allowScriptAccess" value="sameDomain" />'
            + '<param name="movie" value="' + uri + '" />'
            + '<param name="quality" value="high" />'
            + '<param name="scale" value="noscale" />'
            + '<param name="salign" value="lt" />'
            + '<param name="wmode" value="transparent" />'
            + '</object>';
            oe = div.firstChild;
        } else {
            oe = document.createElement("embed");
            oe.type = "application/x-shockwave-flash";
            oe.setAttribute("swLiveConnect", "true");
            oe.setAttribute("salign", "lt");
            oe.setAttribute("scale", "noscale");
            oe.setAttribute("allowScriptAccess", "sameDomain");
            oe.setAttribute("style", "position:absolute;top:0px;left:0px;z-index:50");
            oe.setAttribute("width", "368");
            oe.setAttribute("height", "54");
            oe.setAttribute("bgcolor", "#bbbbbb");
            oe.setAttribute("src", uri);
            div.appendChild(oe);
        }
        this.__fgFlash = oe;
    }
};

XFormUI.prototype.dispose = function() {
     this.__fgFlash = null;
     window._fgObj = null;
};

XFormUI.prototype.__formGuideRemoveStep = function(stepObj) {
    if (this.__fgSteps != null) {
        arrayRemoveStrict(this.__fgSteps, stepObj);
    }
};

XFormUI.prototype.__formGuideAddStep = function(stepObj) {
    if (this.__fgSteps == null) {
        this.__formGuideCreateFlash();
        this.__fgSteps = [];
        var holdThis = this;
        window._fgObj = this;
        window.setTimeout("_fgObj.formGuideNext()", 500);
    }
    this.__fgSteps.push(stepObj);
};

XFormUI.prototype.formGuideHide = function() {
    var flashObj = this.__fgFlash;
    // display none bug in IE
    flashObj.style.visibility = "hidden";
};

XFormUI.prototype.formGuideNext = function() {
    var flashObj = this.__fgFlash;
    if (flashObj.TCurrentFrame("/") < 1) {
        window.setTimeout("_fgObj.formGuideNext()", 200);
        return;
    }
    var stepNum = this.__fgStepNum;
    var flGuideSteps = this.__fgSteps;
    var fgs = flGuideSteps[this.__fgStepNum];
    flashObj.style.top = fgs.getY() + "px";
    flashObj.style.left = fgs.getX() + "px";
    flashObj.SetVariable("guidebar.action", fgs.getMessage());
    flashObj.SetVariable("guidebar.step", stepNum + 1);
    flashObj.TSetProperty("guidebar", 7 /*FL_VISIBILITY*/, 1);
    flashObj.TGotoLabel("guidebar", "start");
    flashObj.TPlay("guidebar");
    stepNum++;
    this.__fgStepNum = stepNum;
    if (stepNum < flGuideSteps.length) {
        flashObj.TSetProperty("guidebar.nextlink", 7 /*FL_VISIBILITY*/, 1);
    } else {
        flashObj.TSetProperty("guidebar.nextlink", 7 /*FL_VISIBILITY*/, 0);
    }
};



XFormUI.prototype.getElementById = function(id) {
    return document.getElementById(this.__name + "_" + id);
};

XFIxGuide.prototype = new XFEventModel();
XFIxGuide.prototype.constructor = XFIxGuide;

function XFIxGuide(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
}

XFIxGuide.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    this.__hElem = makeElement(null, "span");
    this.__hElem.setAttribute("x", this.__elem.getAttribute("x"));
    this.__hElem.setAttribute("y", this.__elem.getAttribute("y"));
    this.__form.__formGuideAddStep(this);
    return this.__hElem;
};

XFIxGuide.prototype.getX = function() {
    return this.__hElem.getAttribute("x");
};

XFIxGuide.prototype.getMessage = function() {
    return this.__hElem.innerHTML;
};

XFIxGuide.prototype.getY = function() {
    return this.__hElem.getAttribute("y");
};

XFIxGuide.prototype.remove = function() {
    this.__form.__formGuideRemoveStep(this);
    XFEventModel.prototype.remove.apply(this, []);
};

XFIxGuide.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__hElem = null;
    this.__xpCtx = null;
};

XFIxTemplate.prototype = new XFEventModel();
XFIxTemplate.prototype.constructor = XFIxTemplate;

function XFIxTemplate(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    form.__addIxTemplate(elem.getAttribute("name"), this);
}

XFIxTemplate.prototype.getInstance = function(pObj) {
    var inst = new XFIxTemplateInst(this.__form, this.__elem, pObj ? pObj : this.__parent);
    inst.__setXpCtx(this.__xpCtx);
    return inst;
};

XFIxTemplate.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    return null;
};

XFIxTemplate.prototype.remove = function() {
    this.__form.__removeIxTemplate(this.__elem.getAttribute("name"));
    XFEventModel.prototype.remove.apply(this, []);
};

XFIxTemplateInst.prototype = new XFEventModel();
XFIxTemplateInst.prototype.constructor = XFIxTemplateInst;

function XFIxTemplateInst(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    
    // case shutter
    this.__cases = {};
}

XFIxTemplateInst.prototype.__setXpCtx = function(xpCtx) {
	this.__xpCtx = xpCtx;
	this.__varRes = xpCtx.varRes.makeChild();
};

XFIxTemplateInst.prototype.setVarString = function(name, val) {
	this.__varRes.setVariableWithName(null, name, new XString(val));
};

XFIxTemplateInst.prototype.renderTemplate = function(xNode, hParent, hBefore, nodiv) {
    var nXpCtx = {
        node : xNode || this.__xpCtx.node,
        varRes : this.__varRes };
	var hElem, uiBefore = null;
	if (nodiv) {
	    hElem = hParent;
		uiBefore = hBefore;
	} else {
		var className = this.__elem.getAttribute("class");
	    if (className) {
	        className = "xftmpl " + className;
	    } else{
	        className = "xftmpl";
	    }
	    var hElem = makeElement(hParent, "div", className, null, hBefore);
	    var style = this.__elem.getAttribute("style");
	    if (style) __xfSetCss(this.__form, hElem, style);
	    this.__hElem = hElem;
	    if (this.__elem.getAttribute("stopdrag") == "true") {
	    	hElem._dndNoDrag = true;
	    }
	}
    this.__form.__doXformBody(this.__elem, nXpCtx, hElem, this, uiBefore);
    return hElem;
};

XFIxTemplateInst.prototype.remove = function() {
	XFHtml.__removeElement(this.__hElem);
    XFEventModel.prototype.remove.apply(this, []);
};


XFIxTemplate.prototype.dispose = function() {
    this.__hElem = null;
    XFEventModel.prototype.dispose.apply(this, []);
};

XFIxInclude.prototype = new XFCtrlBase();
XFIxInclude.prototype.constructor = XFIxInclude;

function XFIxInclude(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__exfIf = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "if");
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
}

XFIxInclude.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    if (__XFActUtil.__exfIf(this)) {
	    this.__itmp = this.__form.getIxTemplate(this.__elem.getAttribute("template"), this);
	    var nodiv = (this.__elem.getAttribute("nodiv") == "1");
	    if (this.__itmp != null) {
	        var xNode = xpCtx.node;
	        if (this.__ref || this.__bind) {
	            xNode = this.getValueNode(xpCtx);
	        }
	        var params = Xml.match(this.__elem, "param");
	        if (params.length > 0) {
		        var nXpCtx = {
	       		 	node : xNode,
	        		varRes : xpCtx.varRes};
		        for (var ii= 0; ii < params.length; ii++) {
		        	var res = this.__form.__xPathSelectComp(nXpCtx, this.__xpathCache(params[ii].getAttribute("value")));
		        	this.__itmp.setVarString(params[ii].getAttribute("name"), res ? res.stringValue() : "");
		        }
	        }
	        if (nodiv) {
	        	this.__itmp.renderTemplate(xNode, hParent, hBefore, true);
	        } else {
		        this.__hElem = makeElement(hParent, "div", this.__elem.getAttribute("class"), null, hBefore);
		        var style = this.__elem.getAttribute("style");
	    		if (style) __xfSetCss(this.__form, this.__hElem, style);
		        this.__hElem.__xfObj = this;
	        	__xfCtrlCommonUI(this);
	        	this.__itmp.renderTemplate(xNode, this.__hElem);
	        }
	        this.__itmp.__incParent = this;
	    }
    }
    return hParent;
};

XFIxInclude.prototype.dispose = function() {
    if (this.__itmp != null) {
        this.__itmp.dispose();
        this.__itmp = null;
    }
    XFCtrlBase.prototype.dispose.apply(this, []);
};


if (typeof(GridCell) != "undefined") {

function XFIxEntGridCell(columnElem, idPair, egrid) {
	this.columnElem = columnElem;
	this.idPair = idPair;
	this.egrid = egrid;
}

XFIxEntGridCell.prototype = new GridCell();
XFIxEntGridCell.prototype.constructor = XFIxEntGridCell;

XFIxEntGridCell.prototype.getHElem = function() {
	if (!this.__hElem) {
		this.__hElem = makeElement(null, "div", "entXFctrl");
		setEventHandler(this.__hElem, "onmouseup", stopEvent);
		var egConfig = this.egrid.config;
		var nXpCtx = {
        	node : this.idPair.xNode,
        	varRes : egConfig.xpCtx.varRes };
		
		egConfig.xformObj.__doXformBody(this.columnElem, nXpCtx, this.__hElem, egConfig.xfCtrl);
	}
    return this.__hElem;
};

}


XFIxEntity.prototype = new XFSetCtrlBase();
XFIxEntity.prototype.constructor = XFIxEntity;

function XFIxEntity(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__idNode = this.__elem.getAttribute("idNode") || "";
}

XFIxEntity.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__model.__form.fireEvent("xforms-value-changed", this);
    }
};

XFIxEntity.prototype.render = function(xpCtx, hParent, hBefore) {
	var egConf = { 
	        condition : this.__elem.getAttribute("condition"),
	        relation : this.__elem.getAttribute("relation"),
	        xfCtrl : this
    	};
    var actRelName = this.__elem.getAttribute("activityRelations");
    if (actRelName) {
    	egConf.activityKeys = { activity : App.activeActivityId, relName : actRelName };
    	egConf.designMode = (Modes.mode == "edit");
    } else {
		var ns = this.getNodeSet(xpCtx);
		
		var idSet = [];
		var ii;
		
		if (this.__idNode.charAt(0) == "@") {
			var idAtt = this.__idNode.substring(1);
			for (ii = 0; ii < ns.length; ii++) {
				var recId = ns[ii].getAttribute(idAtt);
				if (recId) idSet.push(new EntIdPair(recId, ns[ii]));				
			}
		} else {
			for (ii = 0; ii < ns.length; ii++) {
				var recId = this.__model.getValue(this.__idNode, ns[ii]);
				if (recId) idSet.push(new EntIdPair(recId, ns[ii]));		
			}
		}
		egConf.xformObj = this.__form;
		egConf.xpCtx = xpCtx;
		egConf.idSet = idSet;
		var colTmpls = Xml.match(this.__elem, "column");
		if (colTmpls.length > 0) {
			egConf.extColCells = [];
			for (ii = 0; ii < colTmpls.length; ii++) {
				egConf.extColCells.push({
					head : colTmpls[ii].getAttribute("head"),
					cellConst : XFIxEntGridCell,
					constArg : colTmpls[ii]
					});
			}
		}
    }
    var holdThis = this;
	egConf.joinFunc = function(recId) {
			if (!actRelName) {
				var ns = holdThis.getNodeSet(xpCtx);
				var dupNd;
				if (ns.length == 0) {
					ns = XFSingleNodeBinding.autoGen(holdThis.__nodesetXP, xpCtx.node, holdThis.__form);
					dupNd = ns[ns.length - 1];
				} else {
					var desNd = ns[ns.length - 1];
					dupNd = egConf.relation == "1" ? desNd :  holdThis.__model.duplicateNode(".", "..", null, desNd);
				}
				holdThis.__model.setValue(holdThis.__idNode, recId, dupNd);
				egConf.idSet.push(new EntIdPair(recId, dupNd));
			}
			holdThis.__model.__form.fireEvent("ix-entity-join", holdThis, recId);
			holdThis.__model.rebuild();
		};
	egConf.leaveFunc = function(leavId, setXNode) {
			if (!actRelName) {
				for (var ii = 0; ii < egConf.idSet.length; ii++) {
					var idPair = egConf.idSet[ii];
					if (idPair.xNode === setXNode) {
						if (egConf.relation == "1") {
							holdThis.__model.setValue(holdThis.__idNode, "", idPair.xNode);
						} else {
							holdThis.__model.__destroyNode(idPair.xNode);
						}
						
						holdThis.__model.rebuild();
						arrayRemoveStrict(egConf.idSet, idPair);
						break;
					}
				}
			}
			holdThis.__model.__form.fireEvent("ix-entity-leave", holdThis, leavId);
		};
	var entDiv = makeElement(hParent, "div", "xfrmEntity");
	
	var iid = this.__elem.getAttribute("id");
    if (iid) {
        iid = this.__form.__name + "_" + iid;
        entDiv.id = iid;
    }
    entDiv.__xfObj = this;
	
	var entityId = this.__elem.getAttribute("type");
	var entityName = this.__elem.getAttribute("typeName");
	
   	this.eg = EntityGrid.embed(entDiv, 
   		"/getModel?" + (entityId ? ("id=" + Uri.escape(entityId)) : ("name=" + Uri.escape(entityName))), 
   		null, egConf);
   	hParent = null; entDiv = null;
	return null;
};


XFIxEntity.prototype.getNodeSet = function(xpCtx) {
	if (this.__elem.getAttribute("activityRelations")) return [];
	return XFSetCtrlBase.prototype.getNodeSet.apply(this, [xpCtx]);
};

XFIxEntity.prototype.getEntityUri = function() {
	return this.eg.modelUri;
};

XFIxEntity.prototype.getRecordUri = function(recId) {
	var cell0 = this.eg.grid.model.getRecordCell(recId);
	if (cell0) {
		return Uri.absolute(cell0.recsUri, cell0.xNode.getAttribute("uri") + "/data.xml");
	}
	return null;
};

XFIxEntity.prototype.containsRecord = function(recId) {
	var cell0 = this.eg.grid.model.getRecordCell(recId);
	return cell0 ? true : false;
};

XFIxEntity.prototype.importRecordNodes = function(recId, dstNode, replace) {
	if (replace) {
		var kid = dstNode.firstChild;
        while (kid != null) {
        	if (kid.nodeType == 1) {
	            __xfValChangeDeep(this.__model, kid);
	        }
            dstNode.removeChild(kid);
            kid = dstNode.firstChild;
        }
	}
	Xml.importNodes(dstNode.ownerDocument, dstNode,
	 	Xml.match(this.__form.__xb.loadURI(
	 	this.__form.resolveUri(this.getRecordUri(recId))).documentElement, "*"));
	if (this.__idNode) this.__model.setValue(this.__idNode, recId, dstNode);
	this.__model.markChanged(dstNode);
};

XFIxEntity.prototype.dispose = function() {
	XFSetCtrlBase.prototype.dispose.apply(this, []);
	if (this.eg) this.eg.dispose();
};



/**
 * 
 */
XFIxSubform.prototype = new XFCtrlBase();
XFIxSubform.prototype.constructor = XFIxSubform;

function XFIxSubform(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__exfIf = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "if");
    if (this.__ref || this.__bind) {
        this.__initCtrl();
    }
}

XFIxSubform.prototype.render = function(xpCtx, hParent, hBefore) {
    this.__xpCtx = xpCtx;
    if (__XFActUtil.__exfIf(this)) {
    	this.__hElem = makeElement(hParent, "div", this.__elem.getAttribute("class"), null, hBefore);
    	posRelIEFix(this.__hElem);
    	var xb = this.__form.__xb;
    	var frmSrc = this.__elem.getAttribute("form");
    	var dataSrc = this.__elem.getAttribute("src");
    	var frmUri = this.__form.resolveUri(frmSrc);
		this.__sfrm = new XForm(xb.loadURI(frmUri), this.__form.__name + frmSrc, xb, frmUri, this.__form.__context);
		if (dataSrc) {
			dataSrc = this.__form.resolveUri(dataSrc);
			this.__sfrm.setDefaultUris(Uri.localize(Uri.parent(frmUri), dataSrc));
		}
		var params = Xml.match(this.__elem, "param");
        if (params.length > 0) {
	        var nXpCtx = {
       		 	node : xNode,
        		varRes : xpCtx.varRes};
	        for (var ii= 0; ii < params.length; ii++) {
	        	var res = this.__form.__xPathSelectComp(nXpCtx, this.__xpathCache(params[ii].getAttribute("value")));
	        	this.__sfrm.setVarString(params[ii].getAttribute("name"), res ? res.stringValue() : "");
	        }
        }
        this.__sfrm.parentForm = this.__form;
		this.__sfrm.render(this.__hElem);
    }
    return hParent;
};

XFIxSubform.prototype.dispose = function() {
    if (this.__sfrm != null) {
        this.__sfrm.dispose();
        this.__sfrm = null;
    }
    XFCtrlBase.prototype.dispose.apply(this, []);
};




XFIxDialog.prototype = new XFActionBase();
XFIxDialog.prototype.constructor = XFIxDialog;

function XFIxDialog(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
}

XFIxDialog.prototype.perform = function(xpCtx, defUp) {
	if (this.__diag) {
		if (this.__diag.isShowing()) return;
		this.__diag.dispose();
		this.removeEventListener("xforms-close", this.__diag);
		this.__diag = null;
	}
	
	var title = this.__elem.getAttribute("title") || "Form Dialog";
    var diag = new Dialog(title, true);
    diag.render(document.body);
    exAddClass(diag.contentElement, "diagXf", false);
    this.__form.__doXformBody(this.__elem, xpCtx, diag.contentElement, this);
    this.addEventListener("xforms-close", diag);
  	this.__diag = diag;
  	diag.__xfctrl = this;
  	diag.onClose = XFIxDialog.__onClose;
  	var styleObj = styleString2Obj(this.__elem.getAttribute("style") || "");
  	var rect;
  	if (this.__hParent) {
  		rect = getBounds(this.__hParent);
  		rect.w = 0;
  		rect.h = 0;
  	} else {
  		rect = new Rectangle(100, 100, 0, 0);
  	}
  	if (styleObj.top) rect.y = parseInt(styleObj.top);
  	if (styleObj.left) rect.x = parseInt(styleObj.left);
  	if (styleObj.width) rect.w = parseInt(styleObj.width);
  	if (styleObj.height) rect.h = parseInt(styleObj.height);
  	diag.show(rect.x, rect.y, this, rect.w, rect.h);
};

XFIxDialog.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
   	this.__hParent = hParent;
    return null;
};

XFIxDialog.prototype.dispose = function() {
    XFBoundActionBase.prototype.dispose.apply(this, []);
    if (this.__diag) this.__diag.destroy();
    this.__diag = null;
    this.__hParent = null;
};

XFIxDialog.__onClose  = function() {
	if (this.__xfctrl) {
		this.__xfctrl.__diag = null;
		this.__xfctrl.removeEventListener("xforms-close", this);
	}
};

/***
 * Drag and drop Extensions
 */
// check if dnd ok
if (typeof(DNDTypeDummy) != "undefined") {
	
	function XFIxDNDHand() {
	    this.type = "XFIxdnd";
	}
	
	XFIxDNDHand.prototype = new DNDTypeDummy();
	XFIxDNDHand.prototype.constructor = XFIxDNDHand;
	
	XFIxDNDHand.prototype.canDrag = function(dragElem) {
		var dragXf = dragElem.__xfObj;
    	return dragXf.xfCanDrag();
	};
	
	XFIxDNDHand.prototype.dropTest = function(dropElem, dragElem) {
	    var type = dragElem._dndType.type;
	    if (type == "XFIxdnd") {
	    	var dragXf = dragElem._actElem.__xfObj;
	    	var dropXf = dropElem.__xfObj;
	        return dropXf.xfDropTest(dragXf);
	    }
	    return false;
	};
	
	XFIxDNDHand.prototype.dropExec = function(dropElem, dragElem) {
	    // drop
	    var dragXf = dragElem._actElem.__xfObj;
	    var dropXf = dropElem.__xfObj;
	    dropXf.xfDropExec(dragXf);
	};
	
	
	XFIxDNDHand.__getTypeAndCanvas = function() {
		if (!XFIxDNDHand.__dnd) {
			var dndt = new XFIxDNDHand();
			var canv = dndGetCanvas(document.body);
			XFIxDNDHand.__dnd = { canvas : canv, type : dndt.type };
			canv.addDNDType(dndt);
		}
		return XFIxDNDHand.__dnd;
	};
	
	
	
	function XFIxDropControl(form, elem, pObj) {
	    this.__initEvt(form, elem, pObj);
	    this.__bindRequired = false;
	    this.__initBind(form, elem, pObj);
	    if (this.__ref || this.__bind) {
	        this.__initCtrl();
	    }
	    this.__qualify = elem.getAttribute("qualify");
        this.__dragged = elem.getAttribute("dragged");
	}
	XFIxDropControl.prototype = new XFCtrlBase();
	XFIxDropControl.prototype.constructor = XFIxDropControl;
	
	
	XFIxDropControl.prototype.render = function(xpCtx, hParent, hBefore) {
	    this.__xpCtx = xpCtx;
	    var className = this.__elem.getAttribute("class");
	    if (className) className = "xfdrop " + className;
	    else className = "xfdrop";
	  	this.__hElem = makeElement(hParent, "div", className, null, hBefore);
	  	
	    this.__hElem.__xfObj = this;
	    __xfCtrlCommonUI(this);
	    var tac = XFIxDNDHand.__getTypeAndCanvas();
		tac.canvas.makeDropTarget(this.__hElem, tac.type);
		return this.__hElem;
	};
	
	XFIxDropControl.prototype.xfDropTest = function(dragObj) {
		if (!this.__qualify) return true;
		var resExp;
        try {
            if (this.__qualifyXP == null) {
                this.__qualifyXP = this.__model.__xpathCache(this.__qualify);
            }
 			resExp = this.__form.__xPathSelectComp(dragObj.__xpCtx, this.__qualifyXP);
            return resExp.booleanValue(); 
        } catch (e) {
            this.__model.__form.fireEvent("xforms-compute-exception", this.__model, e);
        }
		return false;
	};
	
	XFIxDropControl.prototype.xfDropExec = function(dragObj) {
		var dragVNode = dragObj.getValueNode();
		var dropVNode = this.getValueNode();
		if (this.__dragged == "move") {
			this.__model.__moveNode(dragVNode, dropVNode);
		} else if (this.__dragged == "set-dragvalue" || this.__dragged == "set-dropvalue") {
			
			var dpstr = this.__elem.getAttribute("dropvalue");
			var dgstr = this.__elem.getAttribute("dragvalue");
			var val, tarNode;
			if (this.__dragged == "set-dragvalue") {
				if (dpstr) val = this.__model.getValue(dpstr, dropVNode);
				else val = Xml.stringForNode(dropVNode);

				if (dgstr) tarNode = this.__model.selectNodeList(dgstr, dragVNode)[0];
				else tarNode = dragVNode;
				
			} else {
				if (dgstr) val = this.__model.getValue(dgstr, dragVNode);
				else val = Xml.stringForNode(dragVNode);
				
				if (dpstr) tarNode = this.__model.selectNodeList(dpstr, dropVNode)[0];
				else tarNode = dropVNode;
			}
			
			this.__model.__setNodeValue(tarNode, val);

		} else { // default copy
			this.__model.__duplicateNode(dropVNode, dragVNode);
		}
		this.__model.rebuild();
	};
	
	XFIxDropControl.prototype.dispose = function() {
		var tac = XFIxDNDHand.__getTypeAndCanvas();
		tac.canvas.disposeDropTarget(this.__hElem);
		XFCtrlBase.prototype.dispose.apply(this, []);
	};
	
	
	function XFIxDragControl(form, elem, pObj) {
		this.__ignoreText = false;
	    this.__initEvt(form, elem, pObj);
	    this.__bindRequired = false;
	    this.__initBind(form, elem, pObj);
	    if (this.__ref || this.__bind) {
	        this.__initCtrl();
	    }
	    this.__candrag = elem.getAttribute("candrag");
	}
	XFIxDragControl.prototype = new XFCtrlBase();
	XFIxDragControl.prototype.constructor = XFIxDragControl;
	
	XFIxDragControl.prototype.render = function(xpCtx, hParent, hBefore) {
	    this.__xpCtx = xpCtx;
	    var className = this.__elem.getAttribute("class");
	  	if (className) className = "xfdrag " + className;
	    else className = "xfdrag";
	  	this.__hElem = makeElement(hParent, "div", className, null, hBefore);
	    this.__hElem.__xfObj = this;
	    __xfCtrlCommonUI(this);
	    var tac = XFIxDNDHand.__getTypeAndCanvas();
		tac.canvas.makeDraggable(this.__hElem, tac.type);
		return this.__hElem;
	};
	
	XFIxDragControl.prototype.xfCanDrag = function() {
		if (!this.__candrag) return true;
		var resExp;
        try {
            if (this.__candragXP == null) {
                this.__candragXP = this.__model.__xpathCache(this.__candrag);
            }
 			resExp = this.__form.__xPathSelectComp(this.__xpCtx, this.__candragXP);
            return resExp.booleanValue(); 
        } catch (e) {
            this.__model.__form.fireEvent("xforms-compute-exception", this.__model, e);
        }
		return false;
	};
	
	XFIxDragControl.prototype.dispose = function() {
		var tac = XFIxDNDHand.__getTypeAndCanvas();
		tac.canvas.disposeDraggable(this.__hElem);
		XFCtrlBase.prototype.dispose.apply(this, []);
	};
	
	
} else {
	var XFIxDragControl = null;
	var XFIxDropControl = null;
}


