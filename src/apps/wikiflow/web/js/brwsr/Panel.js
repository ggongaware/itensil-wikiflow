/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.Panel
 * Resizable content panels classes and functions
 */

PanelSet.prototype.constructor = PanelSet;

function PanelSet() {
    this.__hElem = null;
    this.header = null;
    this.margins = [0, 0, 0, 0];
    this.padding = [0, 0, 0, 0];
}

PanelSet.prototype.resize = function(rect) {
    if (this.header != null) {
        var hb = getBounds(this.header);
        var foot = hb.y + hb.h;
        rect.y = foot;
        rect.h = foot < rect.h ? rect.h - foot : 0;
    } else {
    	rect.y = 0;
    	rect.x = 0;
    }
    rect.x += this.margins[3];
    rect.y += this.margins[0];
    rect.w -= this.margins[3] + this.margins[1];
    rect.h -= this.margins[0] + this.margins[2];
    if (rect.w < 0) rect.w = 0;
    if (rect.h < 0) rect.h = 0;
    this.__hElem.style.left = rect.x + "px";
    this.__hElem.style.top = rect.y + "px";
    this.__hElem.style.width = rect.w + "px";
    this.__hElem.style.height = rect.h + "px";
};

PanelSet.prototype.getParentBounds = function() {
    if (this.__hParent.nodeName.toUpperCase() == "BODY") {
        return getVisibleBounds();
    } else {
    	var pb = getBounds(this.__hParent);
    	pb.x = 0; pb.y = 0;
        return pb;
    }
};
                                              /* bool */
PanelSet.prototype.render = function(hParent, linkSize , beforeHelem) {
    this.__hParent = hParent;
    this.__hElem = makeElement(hParent, "div", "panelSet "  +  (this.cssClass || ""), null, beforeHelem);
    this.__dnd = dndGetCanvas(hParent);
    this.__pbDndType = new PanBarDNDType(this);
    this.__dnd.addDNDType(this.__pbDndType);
    if (linkSize) {
        this.resize(this.getParentBounds());
        var holdThis = this;
        var resPar = this.__hParent;
        if (resPar.nodeName.toUpperCase() == "BODY") {
            resPar = window;
        }
        addEventHandler(resPar, "resize",
            function(evt) {
                holdThis.resize(holdThis.getParentBounds());
            });
        resPar = null; hParent = null; // IE enclosure clean-up
    }
};

PanelSet.prototype.renderColumn = function(col) {
    col.render(this.__hElem);
};

PanelSet.prototype.renderPanel = function(panel) {
    panel.__parent.renderPanel(panel);
};

PanelSet.prototype.remove = function() {
    if (this.__hElem != null &&  this.__hParent != null)
        this.__hParent.removeChild(this.__hElem);
};

PanelSet.prototype.initHelp = function(callback) {
	this.__helpCall = callback;
};

PanelSet.prototype.doHelp = function(label) {
	if (this.__helpCall) this.__helpCall(label, "Panel");
};

PanelSet.prototype.dispose = function() {
    this.__hElem = null;
    this.__hParent = null;
    this.header = null;
};

PanelColumn.prototype.constructor = PanelColumn;

function PanelColumn(panelSet) {
    this.__ps = panelSet;
    this.__panels = [];
    this.__expandPans = [];
    this.__rect = new Rectangle();
    this.__rect.h = -1;
    this.barHeight = Panel.BAR_HEIGHT;
}

PanelColumn.prototype.render = function(hParent) {
    this.__hElem = makeElement(hParent, "div", "panCol");
    var i, pan;
    for (i = 0; i < this.__panels.length; i++) {
        pan = this.__panels[i];
        if (this.__panels.length > 1) {
            pan.__setCanShrink(true);
        }
        pan.render(this.__hElem);
    }
};

PanelColumn.prototype.hide = function() {
	this.__hElem.style.display = "none";
};

PanelColumn.prototype.renderPanel = function(panel) {
    panel.render(this.__hElem);
};

/**
 * idx = optional 1-based index
 */
PanelColumn.prototype.addPanel = function(panel, idx) {
	if (idx) arrayInsert(this.__panels, idx - 1, panel);
	else this.__panels.push(panel);
    if (!panel.__shrunk) {
        this.__expandPans.push(panel);
    }
    panel.__parent = this;
};

PanelColumn.prototype.resize = function(rect /* rect held interallly */) {
    var minH = this.__panels.length * this.barHeight;
    if (rect.h < minH) {
        rect.h = minH;
    }
    this.__hElem.style.left = rect.x + "px";
    this.__hElem.style.top = rect.y + "px";
    this.__hElem.style.width = rect.w + "px";
    this.__hElem.style.height = rect.h + "px";
    var dH = rect.h - this.__rect.h;
    if (this.__rect.h == -1) {
        dH -= ((this.__panels.length - this.__expandPans.length) *
            this.barHeight) + 1;
    }
    this.__rect = rect;
    this.__panelSizeUpdate(dH);
};

PanelColumn.prototype.getBarHeight = function(panel) {
	if (panel.barHeight !== null) return panel.barHeight;
	return this.barHeight;
};

PanelColumn.prototype.__panelSizeUpdate = function(heightChange) {
    var dH = heightChange;
    var i, pan, prevPan;
    var barH;
    if (this.__expandPans.length > 0) {
        var pH = Math.floor(dH / this.__expandPans.length);
        var remH = dH - (pH * this.__expandPans.length);
        for (i = 0; i < this.__expandPans.length - 1; i++) {
            pan = this.__expandPans[i];
            pan.__rect.h += pH;
            barH = this.getBarHeight(pan);
            if (pan.__rect.h < barH) {
                remH -= barH - pan.__rect.h;
                pan.__rect.h = barH;
            }
        }
        prevPan = pan;
        pan = this.__expandPans[this.__expandPans.length - 1];
        pan.__rect.h += pH + remH;
        barH = this.getBarHeight(pan);
        if (pan.__rect.h < barH) {
            prevPan.__rect.h -= barH - pan.__rect.h;
            if (prevPan.__rect.h < barH) prevPan.__rect.h = barH;
            pan.__rect.h = barH;
        }
    }
    for (i = 0; i < this.__panels.length; i++) {
        pan = this.__panels[i];
        pan.__rect.w = this.__rect.w;
        if (i > 0) {
            pan.__rect.y = prevPan.__rect.y + prevPan.__rect.h;
        }
        pan.__updateSize();
        prevPan = pan;
    }
};

PanelColumn.prototype.__setPanelTop = function(panel, top) {
    var idx = arrayFindStrict(this.__panels, panel);
    var i;
    var prevPanel = this.__panels[0];
    var nextPanel = panel;
    var nextIdx = idx + 1;
    var shrunkPrevPanels = [];
    var shrunkNextPanels = [];
    var barH = this.getBarHeight(panel);
    for (i = idx - 1; i > 0; i--) {
        prevPanel = this.__panels[i];
        if (prevPanel.__shrunk) shrunkPrevPanels.push(prevPanel);
        else break;
    }
    if (prevPanel.__shrunk) {
        prevPanel = this.__panels[0];
    }
    var maxTop = 0;
    if (panel.__shrunk) {
        for (i = idx + 1; i < this.__panels.length; i++) {
            nextPanel = this.__panels[i];
            if (nextPanel.__shrunk)
                shrunkNextPanels.push(nextPanel);
            else { i++; break };
        }
        nextIdx = i;
        if (!nextPanel.__shrunk) maxTop -= this.getBarHeight(nextPanel);
        maxTop -= (shrunkNextPanels.length + 1) * this.barHeight;
    } else {
    	maxTop -= barH;
    }
    
    if (nextIdx < this.__panels.length) {
    	maxTop += this.__panels[nextIdx].__rect.y; 
   	} else {
    	maxTop += this.__rect.h;
   	}
    
    var r = panel.__rect;
    var minTop = prevPanel.__rect.y +
            (shrunkPrevPanels.length * this.barHeight) + barH;
    if (top < minTop) {
        top = minTop;
    } else if (top > maxTop) {
        top = maxTop;
    }
    var dH = top - r.y;
    r.y = top;
    if (panel != nextPanel) {
        panel.__updateSize();
        nextPanel.__rect.y += dH;
    }
    nextPanel.__rect.h -= dH;
    nextPanel.__updateSize();
    prevPanel.__rect.h += dH;
    prevPanel.__updateSize();
    for (i = 0; i < shrunkPrevPanels.length; i++) {
        shrunkPrevPanels[i].__rect.y += dH;
        shrunkPrevPanels[i].__updateSize();
    }
    for (i = 0; i < shrunkNextPanels.length; i++) {
        shrunkNextPanels[i].__rect.y += dH;
        shrunkNextPanels[i].__updateSize();
    }

};

PanelColumn.prototype.__panelShrink = function (panel, shrunk) {
    var dH;
    if (shrunk) {
        arrayRemoveStrict(this.__expandPans, panel);
        if (this.__hElem != null) {
            dH = panel.__rect.h - this.barHeight;
        }
        panel.__rect.h = this.barHeight;
    } else {
        if (this.__hElem != null) {
            if (this.__expandPans.length > 0) {
                dH = this.barHeight - ((this.__rect.h  -
                      ((this.__panels.length - (this.__expandPans.length + 1)) *
                        this.barHeight))
                        / this.__expandPans.length);
                panel.__rect.h = this.barHeight - dH;
            } else {
                dH = this.barHeight - (this.__rect.h  -
                        ((this.__panels.length - 1) * this.barHeight));
                panel.__rect.h = this.barHeight - dH;
                dH = 0;
            }
            if (panel.__rect.h < this.barHeight) {
                panel.__rect.h = this.barHeight;
            }
            this.__expandPans.push(panel);
        }
    }
    if (this.__hElem != null) {
        this.__panelSizeUpdate(dH);
    }
};

PanelColumn.prototype.removePanel = function(panel) {
	var idx = arrayFindStrict(this.__panels, panel);
    arrayRemoveStrict(this.__panels, panel);
    arrayRemoveStrict(this.__expandPans, panel);
    if (idx < this.__panels.length) {
    	this.__panels[idx].__rect.y = panel.__rect.y;
    }
    this.__panelSizeUpdate(panel.__rect.h);
};

PanelColumn.prototype.remove = function() {
    if (this.__hElem != null) {
        this.__hElem.parentNode.removeChild(this.__hElem);
    }
    this.dispose();
};

PanelColumn.prototype.dispose = function() {
    this.__hElem = null;
    for (var i = 0; i < this.__panels.length; i++) {
        this.__panels[i].dispose();
    }
    this.__ps = null;
    this.__panels = null;
    this.__expandPans = null;
};


Panel.BAR_HEIGHT = 17;
Panel.prototype.constructor = Panel;

function Panel(title, canClose, cssClass) {
    this.__title = title;
    this.__canClose = canClose;
    this.__canShrink = false;
    this.__linkedSizeObj = null;
    this.__shrunk = false;
    this.contentElement = null;
    this.__parent = null;
    this.__rect = new Rectangle();
    this.cssClass = cssClass;
    this.barHeight = null;
    this.gutters = [null,null,null,null];
}

Panel.prototype.setGutters = function(top, right, bottom, left) {
    var ii;
    if (this.__hElem != null) {
        for (ii = 0; ii < this.gutters.length; ii++) {
            if (this.gutters[ii]) this.gutters[ii].parentNode.removeChild(this.gutters[ii]);
        }
    }
    this.gutters = [top, right, bottom, left];
    if (this.__hElem != null) {
        for (ii = 0; ii < this.gutters.length; ii++) {
            if (this.gutters[ii]) this.__hElem.appendChild(this.gutters[ii]);
        }
        this.__updateSize();
    }
};

Panel.prototype.resize = function(rect) {
    this.__hElem.style.left = rect.x + "px";
    this.__hElem.style.top = rect.y + "px";
    this.__hElem.style.width = rect.w + "px";
    this.__hElem.style.height = rect.h + "px";
    this.__barElem.style.width = rect.w + "px";
    var ctop = (this.__parent ? this.__parent.getBarHeight(this) : Panel.BAR_HEIGHT);
    var crect = new Rectangle(0, ctop, rect.w, rect.h - ctop);
    var gs, gut;

    // vert
    if ((gut = this.gutters[0])) {
        gs = getSize(gut);
        crect.h -= gs.h;
        crect.y += gs.h;
        gut.style.width = rect.w + "px";
    }
    if ((gut = this.gutters[2])) {
        gs = getSize(gut);
        crect.h -= gs.h;
        gut.style.top = (crect.y + crect.h) + "px";
        gut.style.width = rect.w + "px";
        crect.h -= 2; // TODO - fix border size hack
    }
    if (crect.h < 0) crect.h = 0;

    // horiz
    if ((gut = this.gutters[3])) {
        gs = getSize(gut);
        crect.w -= gs.w;
        crect.x += gs.w;
        gut.style.top = crect.y + "px";
        gut.style.height = crect.h + "px";
    }
    if ((gut = this.gutters[1])) {
        gs = getSize(gut);
        crect.w -= gs.w;
        gut.style.top = crect.y + "px";
        gut.style.left = (crect.x + crect.w) + "px";
        gut.style.height = crect.h + "px";
    }
    if (crect.w < 0) crect.w = 0;

    this.contentElement.style.left = crect.x + "px";
    this.contentElement.style.top = crect.y + "px";
    this.contentElement.style.width = crect.w + "px";
    this.contentElement.style.height = crect.h + "px";
    if (this.__linkedSizeObj != null && !this.__shrunk) {
        var lr = crect.clone();
        this.__linkedSizeObj.resize(lr);
    }
};

Panel.prototype.__updateSize = function() {
    this.resize(this.__rect);
};

Panel.prototype.render = function(hParent) {
    this.__hElem = makeElement(hParent, "div", "panel" + (this.cssClass != null ? " " + this.cssClass : ""));
    this.__hElem._panel = this;
    this.contentElement = makeElement(this.__hElem, "div", "panContent");
    this.contentElement._panel = this;
    for (var ii = 0; ii < this.gutters.length; ii++) {
        if (this.gutters[ii]) this.__hElem.appendChild(this.gutters[ii]);
    }
    this.__barElem =
            makeElementNbSpd(this.__hElem, "div", "panBar");
    this.__barElem._panel = this;
    this.__titleElem =
            makeElement(this.__barElem, "div", "panTitle", this.__title);

    var ps = this.__parent.__ps;
    ps.__dnd.makeDraggable(this.__barElem, ps.__pbDndType.type);

    if (this.__canClose) {
        // add close button
    }
    this.__shrkBtnElem = makeElementNbSpd(
            this.__barElem, "div", "panBarBtn panMin", null, {title : "Shrink"});
    if (!this.__canShrink) {
        this.__shrkBtnElem.style.display = "none";
    }

    var holdThis = this;
    setEventHandler(this.__shrkBtnElem, "onclick",
            function(evt) {
                evt.cancelBubble = true;
                holdThis.shrinkToggle();
                return false;
            });
            
    // add help button
    if (ps.__helpCall) {
    	
   		this.__helpBtnElem = makeElementNbSpd(
            this.__barElem, "div", "panBarBtn panHelp", null, {title : "Help"});
            
      	setEventHandler(this.__helpBtnElem, "onclick",
            function(evt) {
                evt.cancelBubble = true;
                ps.doHelp(holdThis.__title);
                return false;
            });
   	}
   	
    if (this.__shrunk) {
        //this.contentElement.style.display = "none";
        this.__barElem.className = "panBar panBarMin";
        this.__shrkBtnElem.className = "panBarBtn panMax";
        this.__shrkBtnElem.title = "Expand";
    }
    hParent = null; // IE enclosure clean-up
    return this.contentElement;
};

Panel.prototype.__setCanShrink = function(bool) {
    if (bool) {
        this.__canShrink = true;
        if (this.__shrkBtnElem != null)
            this.__shrkBtnElem.style.display = "";
    } else {
        this.__canShrink = false;
        if (this.__shrkBtnElem != null)
            this.__shrkBtnElem.style.display = "none";
    }
};

Panel.prototype.setShrink = function(bool) {
    if (this.__canShrink) {
        if (this.__shrunk != bool) {
            this.shrinkToggle();
        }
    } else if (this.__shrunk != bool)  {
        this.__shrunk = bool;
        if (bool) {
            this.__rect.h = this.__parent ? this.__parent.barHeight : Panel.BAR_HEIGHT;
        }
        if (this.__parent != null) {
            this.__parent.__panelShrink(this, this.__shrunk);
        }
    }
};

Panel.prototype.shrinkToggle = function() {
    var ii;
    if (this.__shrunk) {
        this.__barElem.className = "panBar";
        this.__shrkBtnElem.className = "panBarBtn panMin";
        this.__shrkBtnElem.title = "Shrink";
        this.__shrunk = false;
        for (ii = 0; ii < this.gutters.length; ii++) {
            if (this.gutters[ii]) this.gutters[ii].style.display = "";
        }
        //this.contentElement.style.display = "";
    } else {
        this.__shrunk = true;
        this.__barElem.className = "panBar panBarMin";
        this.__shrkBtnElem.className = "panBarBtn panMax";
        this.__shrkBtnElem.title = "Expand";
        for (ii = 0; ii < this.gutters.length; ii++) {
            if (this.gutters[ii]) this.gutters[ii].style.display = "none";
        }
        //this.contentElement.style.display = "none";
    }
    if (this.__parent != null) {
        this.__parent.__panelShrink(this, this.__shrunk);
    }
};

Panel.prototype.setHeight = function(hPx) {
    var calcH = this.__rect.h;
    this.__rect.h = hPx;
    if (this.__parent.__panelSizeUpdate) {
        this.__parent.__panelSizeUpdate(calcH - hPx);
    } else {
        this.__updateSize();
    }
};

Panel.prototype.getPanelSet = function() {
	return this.__parent ? this.__parent.__ps : null;
};

Panel.prototype.linkResize = function(obj) {
    this.__linkedSizeObj = obj;
};

Panel.prototype.remove = function() {
    if (this.__hElem != null) {
        this.__hElem.parentNode.removeChild(this.__hElem);
    }
    this.__parent.removePanel(this);
    this.dispose();
};

Panel.prototype.dispose = function() {
    if (this.contentElement != null) this.contentElement._panel = null;
    this.contentElement = null;
    this.__barElem = null;
    this.__titleElem = null;
    this.__shrkBtnElem = null;
    this.__helpBtnElem = null;
    this.__hElem = null;
    this.__parent = null;
};

function PanelIframe(panel, frameName, srcUrl, cssClass) {
   panel.contentElement.style.overflow = "hidden";
   this.__hElem = makeElement(panel.contentElement, "iframe", cssClass, null, null,
         {
            src : srcUrl,
            frameborder : "0",
            border : "0",
            name : frameName
         }
        );
   this.__hElem.style.position = "absolute";
   this.__hElem.style.left = "0px";
   this.__hElem.style.top = "0px";
   panel.linkResize(this);
   this.resize(getBounds(panel.contentElement));
}

PanelIframe.prototype.resize = function(rect) {
    this.__hElem.style.width = (rect.w - 5) + "px";
    this.__hElem.style.height = (rect.h - 5) + "px";
};

PanelIframe.prototype.dispose = function() {
    this.__hElem = null;
};

/**
 * PanelSetSingle
 */
PanelSetSingle.prototype = new PanelSet();
PanelSetSingle.prototype.constructor = PanelSetSingle;

function PanelSetSingle() {
     this.__col = new PanelColumn(this);
     this.__colReady = false;
     this.barHeight = Panel.BAR_HEIGHT;
}

PanelSetSingle.prototype.add = function(panel, idx) {
    this.__col.addPanel(panel, idx);
};

PanelSetSingle.prototype.render = function(hParent, linkSize) {
    PanelSet.prototype.render.apply(this, [hParent, linkSize]);
    this.renderColumn(this.__col);
    var cr = getBounds(this.__hElem);
    cr.x = 0;
    cr.y = 0;
    this.__col.resize(cr);
    this.__colReady = true;
};

PanelSetSingle.prototype.resize = function(rect) {
    PanelSet.prototype.resize.apply(this, [rect]);
    var cr = rect.clone();
    cr.x = 0;
    cr.y = 0;
    if (this.__colReady) this.__col.resize(cr);
};

PanelSetSingle.prototype.dispose = function() {
    this.__col.dispose();
    PanelSet.prototype.dispose.apply(this, []);
};

/**
 * PanelSetVSplit
 */
PanelSetVSplit.SPLIT_WIDTH = 5;

PanelSetVSplit.prototype = new PanelSet();
PanelSetVSplit.prototype.constructor = PanelSetVSplit;

function PanelSetVSplit(majorLeft /* false=right */, minorWidth) {
    this.__minorWidth = minorWidth;
    this.__majorLeft = majorLeft;
    this.__minorCol = new PanelColumn(this);
    this.__majorCol = new PanelColumn(this);
    this.__showMinor = true;
    this.splitWidth = PanelSetVSplit.SPLIT_WIDTH;
}

PanelSetVSplit.prototype.addMajor = function(panel, idx) {
    this.__majorCol.addPanel(panel, idx);
};

PanelSetVSplit.prototype.addMinor = function(panel, idx) {
    this.__minorCol.addPanel(panel, idx);
};

PanelSetVSplit.prototype.render = function(hParent, linkSize, beforeHelem) {
    PanelSet.prototype.render.apply(this, [hParent, linkSize, beforeHelem]);
    var vDnd = new PanVSplitDNDType(this);
    this.__dnd.addDNDType(vDnd);
    
	this.renderColumn(this.__majorCol);
	this.renderColumn(this.__minorCol);

    this.__splitElem = makeElementNbSpd(this.__hElem, "div", "panVSplit");
    this.__dnd.makeDraggable(this.__splitElem, vDnd.type);

    this.resize(this.getParentBounds());
    //this.splitResize(getBounds(this.__hElem));
};

PanelSetVSplit.prototype.resize = function(rect) {
    PanelSet.prototype.resize.apply(this, [rect]);
    if (this.__splitElem != null) {
        this.splitResize(rect);
    }
};

PanelSetVSplit.prototype.setShowMinor = function(show, force) {
	if (this.__showMinor == show && !force) return;
    if (show) {
        this.__showMinor = true;
        this.__splitElem.style.display = "";
        this.__minorCol.__hElem.style.display = "";
    } else {
        this.__showMinor = false;
        this.__splitElem.style.display = "none";
        this.__minorCol.__hElem.style.display = "none";
    }
    this.resize(this.getParentBounds());
};

PanelSetVSplit.prototype.setMinorWidth = function(px) {
	this.__minorWidth = px;
	this.resize(this.getParentBounds());
};

PanelSetVSplit.prototype.splitResize = function(br) {
    var splitX;
    br.w -= this.padding[1];
    br.h -= this.padding[2] + this.padding[0];
    if (!this.__showMinor) {
        br.w -= this.padding[3];
        br.x = this.padding[3];
        br.y = this.padding[0];
        this.__majorCol.resize(br);
        return;
    }
    var majWidth = br.w - this.padding[3] - this.__minorWidth - this.splitWidth;
    if (majWidth < 0) {
    	this.__minorWidth += majWidth;
    	majWidth = 0;
    }
    var majRect = new Rectangle();
    var minRect = new Rectangle();
    if (this.__majorLeft) {
        splitX = this.padding[3] + majWidth;
        if (splitX < this.splitWidth)
            splitX = this.splitWidth;

        minRect.x = splitX + this.splitWidth;
        majRect.x = this.padding[3];
    } else {
        splitX = this.padding[3] + this.__minorWidth;
        if (splitX < this.splitWidth)
            splitX = this.splitWidth;

        majRect.x = splitX + this.splitWidth;
        minRect.x = this.padding[3];
    }
    majRect.w = majWidth > 0 ? majWidth : 0;
    minRect.w = this.__minorWidth > 0 ? this.__minorWidth : 0;
    this.__splitElem.style.left = splitX + "px";
    this.__splitElem.style.height = (br.h > 2 ? br.h - 2 : 0) + "px";
    majRect.h = br.h > 0 ? br.h : 0;
    majRect.y = this.padding[0];
    minRect.h = br.h > 0 ? br.h : 0;
    minRect.y = this.padding[0];

    this.__majorCol.resize(majRect);
    this.__minorCol.resize(minRect);
};

PanelSetVSplit.prototype.dispose = function() {
    this.__majorCol.dispose();
    this.__minorCol.dispose();
    this.__dnd.dispose();
    this.__splitElem = null;
    this.__dnd = null;
    this.__majorCol = null;
    this.__minorCol = null;
    PanelSet.prototype.dispose.apply(this, []);
};


/**
 * VSplit DNDTypeHandler
 */
PanVSplitDNDType.prototype = new DNDTypeHandler();
PanVSplitDNDType.prototype.constructor = PanVSplitDNDType;

function PanVSplitDNDType(panelSet) {
    this.type = "vsplit";
    this.__ps = panelSet;
    this.limitY = 0;
}

PanVSplitDNDType.prototype.dragMove = function(x, y, dragElem) {
    DNDTypeHandler.prototype.dragMove.apply(this, [x, y, dragElem]);
    var br = getBounds(this.__ps.__hElem);
    if (this.__ps.__majorLeft) {
        this.__ps.__minorWidth =
            br.w - (this.offX + x + this.__ps.splitWidth);
    } else {
        this.__ps.__minorWidth = this.offX + x;
    }
    if (this.__ps.__minorWidth < 0) this.__ps.__minorWidth = 0;
    var maxW = this.__canvas.__bounds.w - this.__ps.splitWidth;
    if (this.__ps.__minorWidth >= maxW) this.__ps.__minorWidth = maxW;
    this.__ps.splitResize(br);
};

PanVSplitDNDType.prototype.canDropTest = function(dragElem) {
    return false;
};


/**
 * PanelBar DNDTypeHandler
 */
PanBarDNDType.prototype = new DNDTypeHandler();
PanBarDNDType.prototype.constructor = PanBarDNDType;

function PanBarDNDType(panelSet) {
    this.type = "panbar";
    this.__ps = panelSet;
    this.limitX = 0;
}

PanBarDNDType.prototype.canDrag = function(dragElem) {
    var pan = dragElem._panel;
    var idx = arrayFindStrict(pan.__parent.__panels, pan);
    var i;
    var panels = pan.__parent.__panels

    // if first
    if (idx == 0) return false;
    var prevAllShrunk = true;
    for (i = idx - 1; i >= 0; i--) {
        if (!panels[i].__shrunk) {
            prevAllShrunk = false;
            break;
        }
    }
    if (prevAllShrunk) return false;

    // if following shrunk
    for (i = idx; i < panels.length; i++) {
        if (!panels[i].__shrunk) return true;
    }
    return false;
};

PanBarDNDType.prototype.startDrag = function(x, y, dragElem) {
    DNDTypeHandler.prototype.startDrag.apply(this, [x, y, dragElem]);
    var rect = this.__canvas.getBounds(dragElem._panel.__parent.__hElem);
    this.offY -= rect.y;
};

PanBarDNDType.prototype.dragMove = function(x, y, dragElem) {
    var pan = dragElem._panel;
    pan.__parent.__setPanelTop(pan, this.offY + y);
};

PanBarDNDType.prototype.canDropTest = function(dragElem) {
    return false;
};
