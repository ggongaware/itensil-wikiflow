/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 */
XFDefferedUpdates.prototype.constructor = XFDefferedUpdates;

function XFDefferedUpdates() {
    this.__rebuild = false;
    this.__recalculate = false;
    this.__revalidate = false;
    this.__refresh = false;
}

XFDefferedUpdates.prototype.perform = function(model) {
    if (this.__rebuild) model.__doRebuild();
    if (this.__recalculate) model.__doRecalculate();
    if (this.__revalidate) model.__doRevalidate();
    if (this.__refresh) model.__doRefresh();
};


XFActionBase.prototype = new XFEventModel();
XFActionBase.prototype.constructor = XFActionBase;

function XFActionBase() {
    this.__ignoreText = true;
}

XFActionBase.prototype.__initAction = function(form, elem, pObj) {
    this.__isAction = true;
    if (pObj.__model != null) {
        this.__model = pObj.__model;
    } else {
        this.__model = form.__defaultModel;
    }
    this.__form = form;
    this.__exfIf = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "if");
    this.__exfWhile = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "while");
    if (pObj.addAction != null) {
        pObj.addAction(this);
    } else {
        this.__event = Utilities.getAttributeNS(
            elem, XFORM_EVENT_NAMESPACE, "event");
        if (pObj.addEventListener != null) {
            pObj.addEventListener(this.__event, this);
        }
    }
};

XFActionBase.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFActionBase.prototype.handleEvent = function(evt) {
    var defUp = new XFDefferedUpdates();
    var model = this.__model;
    this.perform(this.__xpCtx, defUp);
    defUp.perform(model);
};

XFActionBase.prototype.__removeEvent = function() {
	if (this.__event && this.__parent && this.__parent.removeEventListener != null) {
		this.__parent.removeEventListener(this.__event, this);
	}
};

XFActionBase.prototype.dispose = function() {
	this.__removeEvent();
    XFEventModel.prototype.dispose.apply(this, []);
    this.__model = null;
    if (this.__xpCtx != null) {
        this.__xpCtx = null;
    }
};

var __XFActUtil = {

    // run exf:if=""
  __exfIf : function(xfActObj) {
    if (!xfActObj.__exfIf) {
        return true;
    }
    try {
        if (xfActObj.__exfIfXP == null) {
            xfActObj.__exfIfXP = xfActObj.__model.__xpathCache(xfActObj.__exfIf);
        }
        var xpCtx = xfActObj.__xpCtx;
        if (xfActObj.__ref && xfActObj.getValueNode != null) {
            var nXpCtx = { node : xfActObj.getValueNode(xpCtx),
                position : xpCtx.position,
                size : xpCtx.size,
                varRes : xpCtx.varRes };
            xpCtx = nXpCtx;
        }
        if (xpCtx.node == null) return false;
        var resExp = xfActObj.__form.__xPathSelectComp(
            xpCtx, xfActObj.__exfIfXP);
        if (SH.debug) {
            SH.println("exf:if on " + xfActObj + " is " + resExp.booleanValue());
        }
        return resExp.booleanValue();
    } catch (e) {
        xfActObj.__model.__form.fireEvent(
            "xforms-compute-exception", xfActObj.__model, e);
        return true;
    }
  },

    // is there a exf:while=""
  __exfHasWhile : function(xfActObj) {
    if (!xfActObj.__exfWhile) {
        return false;
    }
    return true;
  },

  // run exf:while=""
  __exfWhile : function(xfActObj) {
   try {
        if (!xfActObj.__exfWhileXP ) {
            xfActObj.__exfWhileXP = xfActObj.__model.__xpathCache(xfActObj.__exfWhile);
        }
        var xpCtx = xfActObj.__xpCtx;
        if (xfActObj.__ref && xfActObj.getValueNode != null) {
            var nXpCtx = { node : xfActObj.getValueNode(xpCtx),
                position : xpCtx.position,
                size : xpCtx.size,
                varRes : xpCtx.varRes };
            xpCtx = nXpCtx;
        }
        if (xpCtx.node == null) return false;
        var resExp = xfActObj.__form.__xPathSelectComp(
            xpCtx, xfActObj.__exfWhileXP);
        if (SH.debug) {
            SH.println("exf:while on " + xfActObj + " is " + resExp.booleanValue());
        }
        return resExp.booleanValue();
    } catch (e) {
        xfActObj.__model.__form.fireEvent(
            "xforms-compute-exception", xfActObj.__model, e);
        return true;
    }
  },

    // actions with model=""
  __modelActionInit : function(obj, form, elem) {
    var modId = elem.getAttribute("model");
    if (modId)  {
        obj.__model = form.__models[modId];
        if (obj.__model == null) {
            form.fireEvent("xforms-binding-exception", form.__defaultModel);
        } else {
            obj.__model.__checkInit();
        }
    } else {
        form.fireEvent("xforms-binding-exception", form.__defaultModel);
    }
  }

};

XFAction.prototype = new XFActionBase();
XFAction.prototype.constructor = XFAction;

function XFAction(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__actions = [];
}

XFAction.prototype.addAction = function(aObj) {
    this.__actions.push(aObj);
};

XFAction.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        for (var i = 0; i < this.__actions.length; i++) {
            this.__actions[i].perform(xpCtx, defUp);
        }
    }
};


XFBoundActionBase.prototype = new XFSingleNodeBinding();
XFBoundActionBase.prototype.constructor = XFBoundActionBase;

function XFBoundActionBase() {
    this.__ignoreText = true;
}

XFBoundActionBase.prototype.__initAction = function(form, elem, pObj) {
    this.__isAction = true;
    this.__exfIf = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "if");
    this.__exfWhile = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "while");
    if (pObj.addAction != null) {
        pObj.addAction(this);
    } else {
        this.__event = Utilities.getAttributeNS(
            elem, XFORM_EVENT_NAMESPACE, "event");
        if (pObj.addEventListener != null) {
            pObj.addEventListener(this.__event, this);
        }
    }
};

XFBoundActionBase.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFBoundActionBase.prototype.dispose = function() {
	XFActionBase.prototype.__removeEvent.apply(this, []);
    XFSingleNodeBinding.prototype.dispose.apply(this, []);
    this.__xpCtx = null;
};

XFBoundActionBase.prototype.handleEvent = function(evt) {
    var defUp = new XFDefferedUpdates();
    this.perform(this.__xpCtx, defUp);
    defUp.perform(this.__model);
};

XFBoundActionBase.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__model.__form.fireEvent("xforms-value-changed", this);
    }
};

XFBoundActionBase.prototype.setRequired = function(bool) { };

XFBoundActionBase.prototype.setEnabled = function(bool, initMode) { };

XFBoundActionBase.prototype.setValid = function(bool, initMode) { };

XFBoundActionBase.prototype.setReadonly = function(bool, initMode) { };


XFSetBoundActionBase.prototype = new XFNodeSetBinding();
XFSetBoundActionBase.prototype.constructor = XFSetBoundActionBase;

function XFSetBoundActionBase() {
    this.__ignoreText = true;
}

XFSetBoundActionBase.prototype.__initAction = function(form, elem, pObj) {
    this.__isAction = true;
    this.__exfIf = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "if");
    this.__exfWhile = Utilities.getAttributeNS(
        elem, XFORM_EXTENDED_NAMESPACE, "while");
    if (pObj.addAction != null) {
        pObj.addAction(this);
    } else {
        this.__event = Utilities.getAttributeNS(
            elem, XFORM_EVENT_NAMESPACE, "event");
        if (pObj.addEventListener != null) {
            pObj.addEventListener(this.__event, this);
        }
    }
};

XFSetBoundActionBase.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFSetBoundActionBase.prototype.dispose = function() {
	XFActionBase.prototype.__removeEvent.apply(this, []);
    XFNodeSetBinding.prototype.dispose.apply(this, []);
    this.__xpCtx = null;
};

XFSetBoundActionBase.prototype.handleEvent = function(evt) {
    var defUp = new XFDefferedUpdates();
    this.perform(this.__xpCtx, defUp);
    defUp.perform(this.__model);
};

XFSetBoundActionBase.prototype.valueChanged = function() {
    if (this.__xpCtx != null) {
        this.__model.__form.fireEvent("xforms-value-changed", this);
    }
};

XFSetvalue.prototype = new XFBoundActionBase();
XFSetvalue.prototype.constructor = XFSetvalue;

function XFSetvalue(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__value = elem.getAttribute("value");
    this.__inlineText = xmlStringForNode(elem);
}

XFSetvalue.prototype.perform = function(xpCtx, defUp) {

    // refresh dynamic ref cache in performs
    if (this.__dynBind) this.__iNode = null;
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        var v = "";
        var vn = this.getValueNode(xpCtx);

        // refresh dynamic ref cache in performs
        if (this.__dynBind) this.__iNode = null;
        if (vn != null) {
            // TODO - handle the deleted node case, while alerting on others
            if (!this.__value) {
                v = this.__inlineText;
            } else {
                var form = this.__model.__form;
                try {
                    if (this.__valueXP == null) {
                        this.__valueXP = this.__model.__xpathCache(this.__value);
                    }
                    var nXpCtx = { node : vn,
                        position : xpCtx.position,
                        size : xpCtx.size,
                        varRes : xpCtx.varRes };
                    var resExp = form.__xPathSelectComp(nXpCtx, this.__valueXP);
                    v = resExp.stringValue();
                } catch (e) {
                    form.fireEvent("xforms-compute-exception", this.__model, e);
                }
            }
            this.__model.__setNodeValue(vn, v);
        }
    }
    if (didRun) {
        defUp.__recalculate = true;
        defUp.__revalidate = true;
        defUp.__refresh = true;
    }
};

XFSetvalue.prototype.valueChanged = function() {
    // eat it
};

XFSetvalue.prototype.dispose = function() {
    XFBoundActionBase.prototype.dispose.apply(this, []);
    this.__valueXP = null;
};


XFToggle.prototype = new XFActionBase();
XFToggle.prototype.constructor = XFToggle;

function XFToggle(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__case = elem.getAttribute("case");
    if (this.__case == "") this.__case = null;
    this.__caseShutter = null;
    var cp = pObj;
    while (cp !== form) {
    	if (cp.__cases) {
    		this.__caseShutter = cp;
    		break;
    	}
    	cp = cp.__parent;
    }
}

XFToggle.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var caseId = this.__case;
    
    // XF 1.1
    if (this.__caseValue) {
    	var form = this.__model.__form;
        try {
            if (this.__caseValueXP == null) {
                this.__caseValueXP = this.__model.__xpathCache(this.__caseValue);
            }
            var resExp = form.__xPathSelectComp(this.__xpCtx, this.__caseValueXP);
            caseId = resExp.stringValue();
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
    }

    var cs = this.__caseShutter ? this.__caseShutter.__cases[caseId] : null;
    if (cs == null) cs = this.__model.__form.__cases[caseId];
    if (cs != null) {
        cs.activate();
    }
};

XFToggle.prototype.dispose = function() {
    XFActionBase.prototype.dispose.apply(this, []);
    this.__caseValueXP = null;
};


XFClose.prototype = new XFActionBase();
XFClose.prototype.constructor = XFClose;

function XFClose(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
}

XFClose.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var evt1 = this.__form.fireEvent("xforms-close", this);
    if (evt1 && !evt1.__cancelled && this.__form)
    	this.__form.fireEvent("xforms-close", this.__model);
};



XFDispatch.prototype = new XFActionBase();
XFDispatch.prototype.constructor = XFDispatch;

function XFDispatch(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
}

XFDispatch.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var tid = this.__elem.getAttribute("target");
    var targ;
    if (tid) targ = this.__form.__ids[tid];
    else targ = this.__parent; // out of spec, empty target=parent
    if (targ != null) {
        this.__form.fireEvent(this.__elem.getAttribute("name"), targ);
    }
};


XFRebuild.prototype = new XFActionBase();
XFRebuild.prototype.constructor = XFRebuild;

function XFRebuild(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    __XFActUtil.__modelActionInit(this, form, elem);
}

XFRebuild.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    this.__model.__doRebuild();
    defUp.__rebuild = false;
    defUp.__recalculate = true;
    defUp.__revalidate = true;
    defUp.__refresh = true;
};

XFRecalculate.prototype = new XFActionBase();
XFRecalculate.prototype.constructor = XFRecalculate;

function XFRecalculate(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    __XFActUtil.__modelActionInit(this, form, elem);
}

XFRecalculate.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    this.__model.__doRecalculate();
    defUp.__recalculate = false;
    defUp.__revalidate = true;
    defUp.__refresh = true;
};

XFRevalidate.prototype = new XFActionBase();
XFRevalidate.prototype.constructor = XFRevalidate;

function XFRevalidate(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    __XFActUtil.__modelActionInit(this, form, elem);
}

XFRevalidate.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    this.__model.__doRevalidate();
    defUp.__revalidate = false;
    defUp.__refresh = true;
};

XFRefresh.prototype = new XFActionBase();
XFRefresh.prototype.constructor = XFRefresh;

function XFRefresh(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    __XFActUtil.__modelActionInit(this, form, elem);
}

XFRefresh.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    this.__model.__doRefresh();
    defUp.__refresh = false;
};


XFInsert.prototype = new XFSetBoundActionBase();
XFInsert.prototype.constructor = XFInsert;

function XFInsert(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__at = elem.getAttribute("at");
    if (this.__at == "" || this.__at == null) {
        form.fireEvent("xforms-binding-exception",
            this.__model, "insert requires @at");
    }
    this.__position = elem.getAttribute("position");
    if (this.__position != "after" && this.__position != "before") {
        form.fireEvent("xforms-binding-exception",
            this.__model, "insert requires @position");
    }
}


XFInsert.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    this.__masterNode = this.__model.__registerMasterNode(this);
    return hParent;
};

XFInsert.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        var at = 0;
        var form = this.__model.__form;
        try {
            if (this.__atXP == null) {
                this.__atXP = this.__model.__xpathCache(this.__at);
            }
            var resExp = form.__xPathSelectComp(xpCtx, this.__atXP);
            at = resExp.numberValue();
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        this.__model.__insertNode(
            this.getNodeSet(xpCtx), this.__masterNode, at,
            this.__position != "after" ? true : false);

        // refresh dynamic nodeset cache in performs
        if (this.__dynBind || at == 0) this.__iNodeSet = null;
    }
    if (didRun) {
        defUp.__rebuild = true;
        defUp.__recalculate = true;
        defUp.__revalidate = true;
        defUp.__refresh = true;
    }
};

XFInsert.prototype.dispose = function() {
    XFSetBoundActionBase.prototype.dispose.apply(this, []);
    this.__masterNode = null;
    this.__atXP = null;
};


XFDelete.prototype = new XFSetBoundActionBase();
XFDelete.prototype.constructor = XFDelete;

function XFDelete(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__at = elem.getAttribute("at");
    if (this.__at == "" || this.__at == null) {
        form.fireEvent("xforms-binding-exception",
            this.__model, "delete requires @at");
    }
}

XFDelete.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    this.__masterNode = this.__model.__registerMasterNode(this);
    return hParent;
};

XFDelete.prototype.perform = function(xpCtx, defUp) {
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        var at = 0;
        var form = this.__model.__form;
        try {
            if (this.__atXP == null) {
                this.__atXP = this.__model.__xpathCache(this.__at);
            }
            var resExp = form.__xPathSelectComp(xpCtx, this.__atXP);
            at = resExp.numberValue();
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        this.__model.__deleteNode(this.getNodeSet(xpCtx), at, this.__masterNode);

        // refresh nodeset cache in performs
        this.__iNodeSet = null;
    }
    if (didRun) {
        defUp.__rebuild = true;
        defUp.__recalculate = true;
        defUp.__revalidate = true;
        defUp.__refresh = true;
    }
};

XFDelete.prototype.dispose = function() {
    XFSetBoundActionBase.prototype.dispose.apply(this, []);
    this.__masterNode = null;
    this.__atXP = null;
};


XFDestroy.prototype = new XFBoundActionBase();
XFDestroy.prototype.constructor = XFDestroy;

function XFDestroy(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__initBind(form, elem, pObj);
}

XFDestroy.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFDestroy.prototype.perform = function(xpCtx, defUp) {

    // refresh dynamic ref cache in performs
    this.__iNode = null;
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        this.__model.__destroyNode(this.getValueNode(xpCtx));

        // refresh
        this.__iNode = null;
    }
    if (didRun) {
        defUp.__rebuild = true;
        defUp.__recalculate = true;
        defUp.__revalidate = true;
        defUp.__refresh = true;
    }
};



XFDuplicate.prototype = new XFBoundActionBase();
XFDuplicate.prototype.constructor = XFDuplicate;

function XFDuplicate(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__initBind(form, elem, pObj);
    this.__initAction(form, elem, pObj);
    this.__origin = elem.getAttribute("origin");
    if (this.__origin == "") this.__origin = null;
    this.__before = elem.getAttribute("before");
    if (this.__before == "") this.__before = null;
    if (this.__before == null) {
    	this.__before = elem.getAttribute("after");
    	if (this.__before == "") this.__before = null;
    	if (this.__before != null) this.__isAfter = true;
    }
}

XFDuplicate.prototype.render = function(xpCtx, hParent) {
    this.__xpCtx = xpCtx;
    return hParent;
};

XFDuplicate.prototype.perform = function(xpCtx, defUp) {

    // refresh dynamic ref cache in performs
    if (this.__dynBind) this.__iNode = null;
    if (!__XFActUtil.__exfIf(this)) return;
    var hasWhile = __XFActUtil.__exfHasWhile(this);
    var didRun = false;
    while ((!hasWhile && !didRun)
            || (hasWhile && __XFActUtil.__exfWhile(this))) {
        didRun = true;

        var oriNode, befNode = null;
        var form = this.__model.__form;
        var vn = this.getValueNode(xpCtx);
        try {
            if (this.__originXP == null) {
                this.__originXP = this.__model.__xpathCache(this.__origin);
            }
            oriNode = form.__xPathSelectOneComp(xpCtx, this.__originXP);
            if (this.__before != null) {
                if (this.__beforeXP == null) {
                    this.__beforeXP = this.__model.__xpathCache(this.__before);
                }
                var nXpCtx = { node : vn,
                    position : xpCtx.position,
                    size : xpCtx.size,
                    varRes : xpCtx.varRes };
                befNode = form.__xPathSelectOneComp(nXpCtx, this.__beforeXP);
                if (typeof befNode == "string") befNode = null;
                else if (this.__isAfter) befNode = befNode.nextSibling;
            }
        } catch (e) {
            form.fireEvent("xforms-compute-exception", this.__model, e);
        }
        this.__model.__duplicateNode(vn, oriNode, befNode);

        // refresh dynamic ref cache in performs
        if (this.__dynBind) this.__iNode = null;
    }
    if (didRun) {
        defUp.__rebuild = true;
        defUp.__recalculate = true;
        defUp.__revalidate = true;
        defUp.__refresh = true;
    }
};

XFDuplicate.prototype.dispose = function() {
    XFBoundActionBase.prototype.dispose.apply(this, []);
    this.__originXP = null;
};

XFSubmission.prototype = new XFSingleNodeBinding();
XFSubmission.prototype.constructor = XFSubmission;

function XFSubmission(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    this.__bindRequired = false;
    this.__initBind(form, elem, pObj);
    this.__method = elem.getAttribute("method");
    this.__action = elem.getAttribute("action");
    if (this.__action == null) this.__action = "";
    this.__replace = elem.getAttribute("replace");
    this.__instance = elem.getAttribute("instance");
    this.__validate = elem.getAttribute("validate"); // XF 1.1
    this.__constraints = [];

    // no support yet for: version, indent, mediatype, encoding,
    //   omit-xml-declaration, standalone, cdata-section-elements
    //   separator, includenamespaceprefixes
}

XFSubmission.prototype.__addConstraint = function(xfConst) {
	this.__constraints.push(xfConst);
};

XFSubmission.prototype.__delConstraint = function(xfConst) {
	arrayRemoveStrict(this.__constraints, xfConst);
};

XFSubmission.prototype.__submit = function() {

    var meth = this.__method == null ? "post" : this.__method.toLowerCase();
    if (!(meth == "ix:controller" || meth == "post"
            || meth == "put" || meth == "get" || meth == "ix:put-xls")) {
        throw new Error("Submit does not support method: " + this.__method);
    }
    this.__form.fireEvent("xforms-submit", this);
    
    var frmInstans = this.__form.__instances;
    var subInst = null;
    var defInst = this.__model.__defaultInstance;
    if (this.__instance && frmInstans) {
    	subInst = frmInstans[this.__instance];
    }
    if (!subInst) subInst = defInst;
    
    var doVal = this.__validate != "false";
    this.__model.__doRevalidate(doVal);
    if (this.__constraints.length > 0) {
    	var xpCtx = {
	        node : subInst.__root,
	        varRes : this.__form.__xpVarRes};
    	for (var ii = 0; ii < this.__constraints.length; ii++) {
    		this.__constraints[ii].__validate(xpCtx);
    	}
    }
    if (doVal && this.__model.__checkInvalid()) {
        this.__model.__doRefresh();
        this.__submitError();
    } else {
        if (meth == "ix:controller") {
            var ctrl = ctrlGetController();
            this.__submitDone();
            ctrl.act(this.__action, subInst.__root.ownerDocument);
            return;
        }

        var sendDoc = subInst.__root.ownerDocument;
        var actUri = this.__action;
        var partUri = true;
        
        // allow API override of default instance submit
        if (this.__form.__submitActUri != null && subInst === defInst) 
        	actUri = this.__form.__submitActUri;
        
        // blank action is not spec'd (or not following it)
        if (actUri == "")  {
            var src = subInst.__elem.getAttribute("src");
            if (src) {
                actUri = src;
            } else { // internal instace and blank action, send whole form
            	partUri = false;
                actUri = this.__form.documentURI;
                sendDoc = this.__form.__rootElem.ownerDocument;
                
                if (frmInstans != null) {
                    for (var id in frmInstans) {
                        var insObj = frmInstans[id];
                        if (insObj !== defInst) {
                            XFSubmission.__commitInstance(insObj);
                        }
                    }
                }
                XFSubmission.__commitInstance(defInst);
            }
        }
        
        // javascript action not spec'd
        if (__xfjavascriptUriRegEx.test(actUri)) {
            var jsSrc = actUri.substring(actUri.indexOf("javascript:") + 11);
            try {
                __xfEvalJavascript(this.__model, jsSrc);
                this.__submitDone();
            } catch (e) {
            	if (console) console.error(e);
                this.__submitError();
            }
        } else {
            if (partUri && actUri.charAt(0) != "/" && actUri.indexOf("://") < 0) {
                actUri = this.__form.resolveUri(actUri);
            }
            
            // TODO find a better way to do this
            if (meth == "ix:put-xls") {
            	meth = "put";
            	if (actUri.substring(0,6) == "../fil") {
            		actUri = "../docs" + actUri.substring(6);
            	}
            }
            

            // do submitting
            var xmlHttp = XMLBuilder.getXMLHTTP();
            var async = false;
            xmlHttp.open(meth, actUri, async);
            try {
                if (SH.is_ie) {
                    xmlHttp.send(XmlNodeExts.stringBasedCleaner(sendDoc.xml));
                } else {
                    xmlHttp.send(sendDoc);
                }
            } catch (e) {
                this.__submitError();
            }
            if (SH.debug) {
                SH.println("Submit Response: " + xmlHttp.status);
                SH.println(xmlHttp.statusText);
            }
            // if ok
            if (xmlHttp.status < 400) {
                this.__submitDone(xmlHttp);
            } else { // if fail
                this.__submitError();
            }
        }
    }
};

XFSubmission.prototype.__submitDone = function(xmlHttp) {
    var form = this.__model.__form;
    var resDoc = xmlHttp ? form.__xb.__getResponseDoc(xmlHttp) : null;
    this.__model.__submitResDoc = resDoc;
    if (typeof(App) != "undefined" && xmlHttp && xmlHttp.responseText != "") {
        if (App.checkError(resDoc)) return;
    }

    form.fireEvent("xforms-submit-done", this);
    switch (this.__replace) {
        case "none":
            // nothing
            break;

        case "instance":

            // new data
            if (this.__instance) 
            	this.__model.setInstanceId(this.__instance, resDoc.documentElement);
            else this.__model.setDefaultInstance(resDoc.documentElement);
            this.__model.rebuild();
            break;

        case "all":
        default:

            // new page / form
            var hParent = form.__uiParent;
            var xb = form.__xb;
            var name = form.__name;
            form.remove();
            var mime = xmlHttp.getResponseHeader("Content-Type");
            if (mime == "text/xml"
                    || mime == "application/xml"
                    || mime == "application/xhtml+xforms") {
                new XForms(resDoc, name, xb);
            } else {
                hParent.innerHTML = xmlHttp.responseText;
            }
    }

};

XFSubmission.prototype.__submitError = function() {
    this.__model.__form.fireEvent("xforms-submit-error", this);
};

XFSubmission.__commitInstance = function(insObj) {
    // replace the actual document instances with memory instances
    var node = insObj.__elem;
    var kid = node.firstChild;
    while (kid != null) {
        node.removeChild(kid);
        kid = node.firstChild;
    }
    Xml.nodeImport(node.ownerDocument, node, insObj.__root);
};

XFIxConstraint.prototype = new XFEventModel();
XFIxConstraint.prototype.constructor = XFIxConstraint;

function XFIxConstraint(form, elem, pObj) {
    this.__initEvt(form, elem, pObj);
    if (pObj.constructor === XFSubmission) {
    	pObj.__addConstraint(this);
    	this.__model = pObj.__parent;
    }
    this.__nodeset = elem.getAttribute("nodeset");
    if (this.__nodeset == null || this.__nodeset == "") {
        form.fireEvent(
                "xforms-binding-exception", this.__model, "constraint missing @nodeset");
    }
    this.__constraint = elem.getAttribute("constraint");
    if (this.__constraint == null || this.__constraint == "") {
        form.fireEvent(
                "xforms-binding-exception", this.__model, "constraint missing @constraint");
    }
}

XFIxConstraint.prototype.__validate = function(xpCtx) {
	var model = this.__model;
	var form = this.__form;
	try {
		if (!this.__nodesetXP)
		 	this.__nodesetXP = form.__xpParser.parse(this.__nodeset);
		if (!this.__constraintXP)
			this.__constraintXP = form.__xpParser.parse(this.__constraint);

	} catch (ex) {
        form.fireEvent("xforms-compute-exception", model, ex);
        return;
    }
    var resExp;
    try {
        resExp = form.__xPathSelectComp(xpCtx, this.__nodesetXP);
    } catch (ex) {
        form.fireEvent("xforms-compute-exception", model, ex);
        return;
    }
    if (resExp.constructor != XNodeSet) {
        form.fireEvent("xforms-binding-exception", this);
        return;
    }
    var ns = resExp.toArray();
    var nXpCtx = {nXpCtx : ns.length};
    var bool, nn, xc;
    try {
        for (var ii = 0; ii < ns.length; ii++) {
        	nn = ns[ii];
        	nXpCtx.node = nn;
        	nXpCtx.position = ii + 1;
        	nXpCtx.varRes = xpCtx.varRes;
        	xc = model.__getNodeChange(nn);
        	if (!xc) xc = __xfNodeChange(model, nn);
        	resExp = form.__xPathSelectComp(nXpCtx, this.__constraintXP);
            bool = resExp.booleanValue();
            
            // valid
            if (xc[4] != XF_FALSE_CHANGE)
            	xc[4] = bool ? XF_TRUE_CHANGE : XF_FALSE_CHANGE;
            if (!bool) model.__allConstrained = false;
        }
  	} catch (ex) {
        form.fireEvent("xforms-compute-exception", model, ex);
    }
};

XFIxConstraint.prototype.render = function() { };

XFIxConstraint.prototype.remove = function() {
	var pObj = this.__parent;
	if (pObj && pObj.constructor === XFSubmission) {
    	pObj.__delConstraint(this);
    }
    XFEventModel.prototype.remove.apply(this, []);
};

XFIxConstraint.prototype.dispose = function() {
    XFEventModel.prototype.dispose.apply(this, []);
    this.__nodesetXP = null;
    this.__constraintXP = null;
    this.__model = null;
};

XFScriptAction.prototype.constructor = XFScriptAction;

function XFScriptAction(xhObj, func) {
    this.__func = func;
    this.__xhObj = xhObj;
}

XFScriptAction.prototype.perform = function(xpCtx, defUp) {
    var func = this.__func;
    func(this.__xhObj.__form.__defaultModel, this.__xhObj.__hParent, this.__xhObj.__evtCtx.node);
};
