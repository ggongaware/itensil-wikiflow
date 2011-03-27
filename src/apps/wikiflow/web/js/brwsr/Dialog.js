/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.Dialog
 * Resizable / Movable content dialog windows
 */

Dialog.BAR_HEIGHT = 17;
Dialog.NOTCH_WIDTH = 12;
Dialog.MIN_WIDTH = 75;
Dialog.MIN_HEIGHT = 75;
Dialog.GUTTER = 6;
Dialog.prototype.constructor = Dialog;

// singleton mask
Dialog.modalMask = null;

function Dialog(title, canClose) {
    this.__title = title;
    this.__hElem = null;
    this.__canClose = canClose;
    this.contentElement = null;
    this.__h = -1;
    this.__w = -1;
    this.__sizeMatch = true;
    this.__disposable = [];
    this.__linkedSizeObj = null;
    this.canResize = true;
    this.autoRemove = true;
}

// estW/H = estimated width/height for window edges tests
Dialog.prototype.show = function(x, y, context, estW, estH) {
    if (this.__hElem == null) {
        this.render(document.body);
    }
    this.context = context;
    this.autoSize(x, y, estW, estH, true);
};

Dialog.prototype.showModal = function(x, y, context, hParent, estW, estH) {
    if (hParent == null) {
        hParent = document.body;
    }
    if (this.__hElem == null) {
        this.render(hParent);
    }
    if (Dialog.modalMask == null) {
        var resPar = hParent;
        if (resPar.nodeName.toUpperCase() == "BODY") resPar = window;
        Dialog.modalMask = elementNoSelect(
                makeElementNbSpd(hParent, "div", "diagModal"));
        addEventHandler(resPar, "resize", __diagModalResize);
        __diagModalResize();
    } else {
        Dialog.modalMask.style.display = "";
        __diagModalResize();
    }
    this.context = context;
    this.autoSize(x, y, estW, estH, true);
};

Dialog.prototype.isShowing = function() {
    return this.__hElem != null && this.__hElem.style.display != "none";
};

Dialog.prototype.initHelp = function(callback) {
	this.__helpCall = callback;
};

Dialog.prototype.doHelp = function(label) {
	if (this.__helpCall) this.__helpCall(label, "Dialog");
};

Dialog.prototype.render = function(hParent) {
    this.__shadElem = makeElement(hParent, "img", "diagShadow", null, null,
        { src : SH.is_ie ? "../pix/nil.gif" : "../pix/panel/shadow.png"});
    this.__shadElem.style.visibility = "hidden";
    this.__hElem = makeElement(hParent, "div", "dialog");
    if (SH.is_gecko && !SH.is_gecko1_9) this.__hElem.style.overflow = "auto";
    this.__hElem.style.visibility = "hidden";
    this.__hElem._dialog = this;
    this.__barElem = makeElementNbSpd(this.__hElem, "div", "diagBar", null,
                 {title : "Move"});
    var tbody = makeLayoutTable(this.__hElem, "diagContent");
    this.contentElement = makeElement(
            makeElement(makeElement(tbody, "tr"), "td", "diagContent"), "div", "diagContent");
    this.contentElement._dialog = this;
    this.__barElem._diag = this;
    this.__titleElem =
            makeElement(this.__barElem, "div", "diagTitle", this.__title);
    var holdThis = this;
    // add help button
    if (this.__helpCall) {
    	
   		this.__helpBtnElem = makeElementNbSpd(
            this.__barElem, "div", "diagBarBtn diagHelp", null, {title : "Help"});
            
      	setEventHandler(this.__helpBtnElem, "onclick",
            function(evt) {
                evt.cancelBubble = true;
                holdThis.doHelp(holdThis.__title);
                return false;
            });
   	}
    
    if (this.__canClose) {
        this.__closeElem = makeElementNbSpd(
            this.__barElem, "div", "diagBarBtn diagClose", null,
            {title : "Close"});
        setEventHandler(this.__closeElem, "onclick",
            function(evt) {
                evt.cancelBubble = true;
                holdThis.close();
                return false;
            });
    }
    this.__dnd = dndGetCanvas(hParent);
    if (this.canResize) {
        this.__notchElem = makeElementNbSpd(this.__hElem, "div", "diagNotch",  null,
                 {title : "Resize"});
        this.__notchElem._diag = this;
        this.__dnd.addDNDType(Dialog.NOTCH_DND_TYPE);
        this.__dnd.makeDraggable(this.__notchElem, Dialog.NOTCH_DND_TYPE.type);
    }

    this.__dnd.addDNDType(Dialog.BAR_DND_TYPE);
    this.__dnd.makeDraggable(this.__barElem, Dialog.BAR_DND_TYPE.type);

    tbody = null; hParent = null; // IE enclosure clean-up    
    if (this.autoRemove && typeof(App) != "undefined") {
        App.addDispose(this);
    }
    return this.contentElement;
};

Dialog.prototype.close = function() {
    this.__shadElem.style.display = "none";
    this.__hElem.style.display = "none";
    Ephemeral.hideAll();
    if (Dialog.modalMask != null) {
        Dialog.modalMask.style.display = "none";
    }
    if (this.onClose != null) {
        this.onClose();
    }
    if (this.autoRemove) this.remove();
};

Dialog.prototype.destroy = function() {
    this.close();
    this.remove();
};

Dialog.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-close") {
        this.destroy();
    } else if (evt.type == "xforms-rebuild") {
    	if (this.canResize && this.__sizeMatch) {
	        var holdThis = this;
	        holdThis.__hElem.style.overflow = "hidden";
	        holdThis.__hElem.style.width = "auto";
	    	holdThis.__hElem.style.height = "auto";
	        holdThis.contentElement.style.width = "auto";
	        holdThis.contentElement.style.height = "auto";
	        window.setTimeout( function () {
	        	 var fc = holdThis.contentElement ? getFirstChildElement(holdThis.contentElement) : null;
	             if (fc) holdThis.contentResized(getBounds(fc));
				 if (holdThis.__hElem) holdThis.__hElem.style.overflow = "";
	            }, 200);
    	}
    }
};

Dialog.prototype.addListening = function(evtDisp) {
    if (this.__listens == null) this.__listens = [];
    this.__listens.push(evtDisp);
};

Dialog.prototype.autoSize = function(x, y, estW, estH, doShow) {
    this.__x = x;
    this.__y = y;
    this.__hElem.left = x + "px";
    this.__hElem.top = y + "px";
    var holdThis = this;
    window.setTimeout( function () { 
    		holdThis.__screenBounds(estW, estH); 
    		if (doShow && holdThis.onShow) holdThis.onShow();
    	}, 100);
};

Dialog.prototype.__screenBounds = function(estW, estH) {
	if (!this.contentElement) return;
    var vr = getVisibleBounds();
    var bottom = vr.y + vr.h - Dialog.GUTTER;
    var right = vr.x + vr.w - Dialog.GUTTER;
    if (this.__h < 0) {
        var r = getBounds(this.contentElement);
        this.__w = r.w + 2;
        if (r.h >= vr.h) {
            this.__h = vr.h - 100;
        } else {
            this.__h = r.h + 29;
        }
    }
    if (!estW) estW = Dialog.MIN_WIDTH;
    if (!estH) estH = Dialog.MIN_HEIGHT;
     
    if (this.__w < estW) this.__w = estW;
    if (this.__h < estH) this.__h = estH;
    if ((this.__h + this.__y) > bottom) {
        this.__y = bottom - this.__h;
        if (this.__y < 0) this.__y = 0;
    }
    if ((this.__w + this.__x) > right) {
        this.__x = right - this.__w;
        if (this.__x < 0) this.__x = 0;
    }
    this.__barElem.style.width = (this.__w) + "px";
    this.__hElem.style.left = this.__x + "px";
    this.__hElem.style.top = this.__y + "px";
    this.__hElem.style.width = this.__w + "px";
    this.__hElem.style.height = this.__h + "px";
    this.contentElement.style.width = (this.__w - 2) + "px";
    this.contentElement.style.height = (this.__h - 28) + "px";
    if (this.__linkedSizeObj == null) this.contentElement.style.overflow = "auto";
    this.__hElem.style.visibility = "";
    this.__shadElem.style.left = (this.__x + 2) + "px";
    this.__shadElem.style.top = (this.__y + 2) + "px";
    this.__shadElem.style.width = (this.__w + 3) + "px";
    this.__shadElem.style.height = (this.__h + 3) + "px";
    this.__shadElem.style.visibility = "";
    if (this.__notchElem != null) {
        this.__notchElem.style.left = (this.__w - Dialog.NOTCH_WIDTH) + "px";
        this.__notchElem.style.top = (this.__h - Dialog.NOTCH_WIDTH) + "px";
    }
    if (this.__linkedSizeObj != null) {
        this.__linkedSizeObj.resize(getLocalBounds(this.__hElem, this.contentElement), this.contentElement);
    }
};

Dialog.prototype.resizeTo = function(right, bottom) {
    // stay on screen
    var vr = getVisibleBounds();
    var vbottom = vr.y + vr.h - Dialog.GUTTER;
    var vright = vr.x + vr.w - Dialog.GUTTER;
    if (right > vright) right = vright;
    if (bottom > vbottom) bottom = vbottom;

    this.__w = right - this.__x;
    if (this.__w < Dialog.MIN_WIDTH) this.__w = Dialog.MIN_WIDTH;
    this.__h = bottom - this.__y;
    if (this.__h < Dialog.MIN_HEIGHT) this.__h = Dialog.MIN_HEIGHT;
    this.__hElem.style.width = this.__w + "px";
    this.__hElem.style.height = this.__h + "px";
    this.contentElement.style.width = (this.__w - 2) + "px";
    this.contentElement.style.height = (this.__h - 28) + "px";
    this.__shadElem.style.width = (this.__w + 3) + "px";
    this.__shadElem.style.height = (this.__h + 3) + "px";
    this.__barElem.style.width = (this.__w - 2) + "px";
    if (this.__notchElem != null) {
        this.__notchElem.style.left = (this.__w - Dialog.NOTCH_WIDTH) + "px";
        this.__notchElem.style.top = (this.__h - Dialog.NOTCH_WIDTH) + "px";
    }
    this.__sizeMatch = false;
    if (this.__linkedSizeObj != null) {
        this.__linkedSizeObj.resize(getLocalBounds(this.__hElem, this.contentElement), this.contentElement);
    }
};

Dialog.prototype.contentResized = function(rect) {
    if (this.__sizeMatch) {
        var vr = getVisibleBounds();
        var sb = vr.y + vr.h - Dialog.GUTTER;
        var sr = vr.x + vr.w - Dialog.GUTTER;
        this.contentElement.style.overflow = "hidden";
        var right = this.__x + rect.w + 2;
        var bottom = this.__y + rect.h + 29;
        var sbTimeout = false;
        if (sr < right) right = sr;
        if (sb < bottom) {
            bottom = sb;
            right += 20
            sbTimeout = true;
        }
        this.resizeTo(right, bottom);
        if (sbTimeout) {
            var holdThis = this;
            window.setTimeout( function () { holdThis.__screenBounds(); }, 10);
        } else {
            if (this.__linkedSizeObj == null) this.contentElement.style.overflow = "auto";
        }
        this.__sizeMatch = true;
    }
};

Dialog.prototype.move = function(x, y) {
    this.__x = x;
    this.__y = y;

    // stay on screen
    var vr = getVisibleBounds();
    var bottom = vr.y + vr.h - Dialog.GUTTER;
    var right = vr.x + vr.w - Dialog.GUTTER;
    if (this.__y < 0) {
        this.__y = 0;
    } else if ((this.__h + this.__y) > bottom) {
        this.__y = bottom - this.__h;
        if (this.__y < 0) this.__y = 0;
    }
    if (this.__x < 0) {
        this.__x = 0;
    } else if ((this.__w + this.__x) > right) {
        this.__x = right - this.__w;
        if (this.__x < 0) this.__x = 0;
    }

    this.__hElem.style.left = this.__x + "px";
    this.__hElem.style.top = this.__y + "px";
    this.__shadElem.style.left = (this.__x + 2) + "px";
    this.__shadElem.style.top = (this.__y + 2) + "px";
};

Dialog.prototype.linkResize = function(obj) {
    if (obj != null) this.contentElement.style.overflow = "hidden";
    this.__linkedSizeObj = obj;
};

Dialog.prototype.remove = function() {
    if (this.__hElem != null)
        this.__hElem.parentNode.removeChild(this.__hElem);
    if (this.__shadElem != null)
        this.__shadElem.parentNode.removeChild(this.__shadElem);
    if (Dialog.modalMask != null) // not the best
        Dialog.modalMask.parentNode.removeChild(Dialog.modalMask);
    this.dispose();
};

Dialog.prototype.addDisposable = function(obj) {
    this.__disposable.push(obj);
};

Dialog.prototype.dispose = function() {
    this.__dnd = null;
    if (this.__disposable != null) {
        for (var i = 0; i < this.__disposable.length; i++) {
            this.__disposable[i].dispose();
        }
        this.__disposable = null;
    }
    if (this.__listens != null) {
        for (var i = 0; i < this.__listens.length; i++) {
            this.__listens[i].removeEventListener(null, this);
        }
    }
    this.__hElem = null;
    this.__titleElem = null;
    this.__barElem = null;
    this.__notchElem = null;
    this.contentElement = null;
    this.__shadElem = null;
    this.__closeElem = null;
    this.context = null;
    this.__helpBtnElem = null;
    Dialog.modalMask = null; // not the best
};


Dialog.prompt = function(msg, reply, onOk, onCancel) {
	
	var diag = new Dialog("" , true);
	diag.canResize = false;
	diag.render(document.body);
	
	var dbod = makeElement(diag.contentElement, "div", "prompt");
	
	makeElement(dbod, "div", "msg", msg);
	
	var tbod = makeLayoutTable(dbod, "prompt");
	var tr = makeElement(tbod, "tr");
	var prefix = "";
	var suffix = "";
	var replyBod = reply;
	if (typeof(reply) != "string") {
		prefix = reply.prefix || "";
		replyBod = reply.body || "";
		suffix = reply.suffix || "";
	}
	
	makeElement(tr, "td", "prefix", prefix);
	diag.repCtrl = makeElement(makeElement(tr, "td", "body"), "input", "prompt", "text", null, {name:"diagPrompt"});
	diag.repCtrl._diag = diag;
	
	diag.onShow = function() {
			diag.repCtrl.focus();
		};
	
	diag.doOk = function() {
			if (diag.repCtrl.value) {
				diag.onClose = null;
				if (onOk) {
					onOk(prefix + diag.repCtrl.value + suffix);
				}
				diag.repCtrl = null;
			}
			diag.destroy();
		};
		
	setEventHandler(diag.repCtrl, "onkeydown", Dialog.__prmptInputKeyDown);
	diag.repCtrl.value = replyBod;
	makeElement(tr, "td", "suffix", suffix);

	tbod = makeLayoutTable(dbod, "buttons");
	tr = makeElement(tbod, "tr");
	setEventHandler(makeElement(makeElement(tr, "td"), "button", "diagBtn dbOk"), "onclick", diag.doOk);
	diag.onClose = onCancel;
	setEventHandler(makeElement(makeElement(tr, "td"), "button", "diagBtn dbCancel"), "onclick", 
		function() { diag.repCtrl = null; diag.destroy(); });
	
	dbod = null; tbod = null; tr = null; // IE enclosure clean-up   
	return diag;
};


Dialog.__prmptInputKeyDown = function(evt) {
    var code;
    if (evt.keyCode) code = evt.keyCode;
	else if (evt.which) code = evt.which;

	if (code == 13) { // return key
	    evt.cancelBubble = true;
	    this._diag.doOk();
	}
};


/**
 * Dialog bar DNDTypeHandler
 */
DiagBarDNDType.prototype = new DNDTypeHandler();
DiagBarDNDType.prototype.constructor = DiagBarDNDType;

function DiagBarDNDType() {
    this.type = "diagBar";
}

DiagBarDNDType.prototype.dragMove = function(x, y, dragElem) {
    dragElem._diag.move(this.offX + x, this.offY + y);
};

/**
 * Dialog notch DNDTypeHandler
 */
DiagNotchDNDType.prototype = new DNDTypeHandler();
DiagNotchDNDType.prototype.constructor = DiagNotchDNDType;

function DiagNotchDNDType() {
    this.type = "diagNotch";
}

DiagNotchDNDType.prototype.dragMove = function(x, y, dragElem) {
    dragElem._diag.resizeTo(
        this.offX + x + Dialog.NOTCH_WIDTH, this.offY + y + Dialog.NOTCH_WIDTH);
};

DiagNotchDNDType.prototype.startDrag = function(x, y, dragElem) {
    DNDTypeHandler.prototype.startDrag.apply(this, [x, y, dragElem]);
    dragElem._diag.contentElement.style.overflow = "hidden";
};

DiagNotchDNDType.prototype.noTargetDrop = function(dragElem) {
    DNDTypeHandler.prototype.noTargetDrop.apply(this, [dragElem]);
    if (dragElem._diag.__linkedSizeObj == null)
        dragElem._diag.contentElement.style.overflow = "auto";
};

function __diagModalResize(evt) {
    if (Dialog.modalMask != null) {
        var hParent = Dialog.modalMask.parentNode;
        var b = hParent.nodeName.toUpperCase() == "BODY" ?
                getVisibleBounds() : getBounds(hParent);
        Dialog.modalMask.style.left = b.x + "px";
        Dialog.modalMask.style.top = b.y + "px";
        Dialog.modalMask.style.width = b.w + "px";
        Dialog.modalMask.style.height = b.h + "px";
    }
}

// global Dialog DND types
Dialog.BAR_DND_TYPE = new DiagBarDNDType();
Dialog.NOTCH_DND_TYPE = new DiagNotchDNDType();




/**
 * Wizard
 */
Wizard.prototype.constructor = Wizard;

function Wizard() {
    this.__pages = [];
    this.__idx = 0;
    this.__lastPage = null;
    this.dialog = null;
}

Wizard.prototype.addPage = function(label, page) {
    this.__pages.push([label, page]);
};

Wizard.prototype.render = function(dialog) {
    this.__hFrame = makeLayoutTable(dialog.contentElement, "wizFrame");
    var tr = makeElement(this.__hFrame, "tr");
    this.__hPrv = makeElement(tr, "td", "wizPreview");
    this.__hPage = makeElement(tr, "td", "wizPage");
    tr = makeElement(this.__hFrame, "tr");
    this.__hBtns =
            makeElement(tr, "td", "wizButtons", null, null, { colSpan : 2});
    this.__hBakBtn = makeElement(this.__hBtns, "button", "wiz", "< Back");
    this.__hBakBtn.style.visibility = "hidden";
    this.__hNxtBtn = makeElement(this.__hBtns, "button", "wiz", "Next >");
    tr = null;
    var holdThis = this;
    setEventHandler(this.__hBakBtn, "onclick", function(evt) {
        holdThis.prevPage();
        });
    setEventHandler(this.__hNxtBtn, "onclick", function(evt) {
        holdThis.nextPage();
        });
    this.dialog = dialog;
    dialog.addDisposable(this);
    this.setPage(this.__idx);
    this.updatePreview();
};

Wizard.prototype.updatePreview = function() {
    removeElementChildren(this.__hPrv);
    var div = makeElement(this.__hPrv, "div", "wizPreview");
    for (var ii = 0; ii < this.__pages.length; ii++) {
        var pg = this.__pages[ii];
        var li = makeElement(div, "div",
            "wizLabel" + (this.__idx == ii ? " wizOn" : ""),
            (ii+1) + ". " + pg[0]);
        var msg = pg[1].getPreview();
        if (msg.length > 0) {
            for (var jj = 0; jj < msg.length; jj++) {
                makeElement(div, "div", "wizMsg", msg[jj]);
            }
        }
    }
};

Wizard.prototype.setPage = function(index) {
    if (this.__lastPage != null) {
        if (!this.__lastPage[1].commit()) {
            return;
        }
        this.__lastPage[2].style.display = "none";
    }
    if (index >= this.__pages.length) {
        this.finish();
        return;
    } else if (index >= (this.__pages.length - 1)) {
        index = this.__pages.length - 1;
        setElementText(this.__hNxtBtn, "Finish");
    } else {
        setElementText(this.__hNxtBtn, "Next >");
    }
    if (index <= 0) {
        index = 0;
        this.__hBakBtn.style.visibility = "hidden";
    } else {
        this.__hBakBtn.style.visibility = "";
    }
    var pg = this.__pages[index];
    this.__idx = index;
    if (pg[2] == null) {
        pg[2] = makeElement(this.__hPage, "div", "wizPage");
        pg[1].render(pg[2], this);
    } else {
        pg[1].refresh();
        pg[2].style.display = "";
    }
    this.dialog.contentResized(getBounds(this.__hFrame));
    this.updatePreview();
    this.__lastPage = pg;
};

Wizard.prototype.nextPage = function() {
    this.setPage(this.__idx + 1);
};

Wizard.prototype.prevPage = function() {
    this.setPage(this.__idx - 1);
};

Wizard.prototype.finish = function() {
    if (this.onfinish != null) {
        this.onfinish();
    }
    this.dialog.close();
};

Wizard.prototype.dispose = function() {
    for (var ii = 0; ii < this.__pages.length; ii++) {
        this.__pages[ii][1].dispose();
        this.__pages[ii][2] = null;
    }
    this.__hFrame = null;
    this.__hPrv  = null;
    this.__hPage = null;
};

/**
 * WizardPage
 */
WizardPage.prototype.constructor = WizardPage;

function WizardPage() {
}

WizardPage.prototype.isReady = function() {
    return true;
};

// return array of strings
WizardPage.prototype.getPreview = function() {
    return [];
};

WizardPage.prototype.refresh = function() {
};

WizardPage.prototype.commit = function() {
    return true;
};

WizardPage.prototype.render = function(hParent, wizard) {
    makeElement(hParent, "div", "wizContent", "A wizard page " + (new Date()).getTime());
};

WizardPage.prototype.dispose = function() {
};

WizardXFormPage.prototype = new WizardPage();

WizardXFormPage.prototype.constructor = WizardXFormPage;

function WizardXFormPage(xf) {
    this.xf = xf;
}

WizardXFormPage.prototype.getPreview = function() {
    if (!this.xf.isInit()) return [];
    var xfMod = this.xf.getDefaultModel();
    var nl = xfMod.selectNodeList("//*[@preview]");
    var msgs = [];
    var str;
    if (nl != null) {
        for (var ii = 0; ii < nl.length; ii++) {
            str = xfMod.getFormattedValue(nl[ii]);
            if (str != "") {
                msgs.push(nl[ii].getAttribute("preview") + ": " + str);
            }
        }
    }
    return msgs;
};

WizardXFormPage.prototype.render = function(hParent, wizard) {
    var elem = makeElement(hParent, "div",  "wizContent");
    this.xf.render(elem);
    this.xf.getDefaultModel().addEventListener("xforms-submit-done", this);
    this.xf.getDefaultModel().addEventListener("xforms-submit-error", this);
};

WizardXFormPage.prototype.commit = function() {
    this.__subDone = false;
    try {
        this.xf.getDefaultModel().submit("submission");
    } catch (e) {
        alert(e);
        return false;
    }
    return this.__subDone;
};

WizardXFormPage.prototype.dispose = function() {
    this.xf.dispose();
};

WizardXFormPage.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-submit-done") {
        this.__subDone = true;
    }
};
