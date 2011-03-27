/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 * Lib: brwsr.DND
 *
 *
 */

var dndCanvases = [];

// global means you drag between canvases
function dndGetCanvas(hElem) {
    if (hElem._dndCanvas != null) {
        return hElem._dndCanvas;
    }
    var dndc = new DNDCanvas(hElem);
    dndCanvases.push(dndc);
    return dndc;
}

/**
 * Canvas
 */
function DNDCanvas(canvasElement) {
    this.__hElem = canvasElement;
    canvasElement._dndCanvas = this;
    this.__dndTypes = new Object();
    this.__dropTargets = [];
    this.__draggables = [];
    this.__autoScroll = null;
    this.__groupType = null;
    this.dragElem = null;
    this.dropElem = null;
    this.cancelDrag = false;
    this.disableDrag = false;
    this.parentCanvas = null;
    if (canvasElement.nodeName.toUpperCase() == "BODY") {
        this.__isBody = true;
    } else {
        this.__isBody = false;
    }
    
    var holdThis = this;
    this.__msDownEvt = function(evt) { holdThis.mouseDown(evt); };
    this.__msMoveEvt = function(evt) { holdThis.mouseMove(evt); };
    this.__msUpEvt = function(evt) { holdThis.mouseUp(evt); };
    this.__scrollEvt = function(evt) { holdThis.onScroll(evt); };
    addEventHandler(canvasElement, "mousedown", this.__msDownEvt);
    addEventHandler(canvasElement, "mousemove", this.__msMoveEvt);
    addEventHandler(canvasElement, "mouseup", this.__msUpEvt);
    addEventHandler(canvasElement, "scroll", this.__scrollEvt);
    
    
    if (SH.is_ie && !this.__isBody) {
       canvasElement.onselectstart = function() {
               return holdThis.disableDrag || holdThis.cancelDrag
                   || "INPUT TEXTAREA".indexOf(getEventElement(event).nodeName) >= 0;
           };
   }
    canvasElement = null; // IE enclosure clean-up
    this.__bounds = this.__isBody ?
            getVisibleBounds() : getBounds(this.__hElem);
}

/**
 * parentDrag = true, means dragElems are promoted to the parent canvas
 * false means this canvas is only a dropTarget
 */
DNDCanvas.prototype.setParent = function(parentCanvas, /*Boolean*/ parentDrag) {
    this.parentCanvas = parentCanvas;
    this.parentDrag = parentDrag;
    if (DNDCanvas.__subCanDndType == null) {
        DNDCanvas.__subCanDndType = new DNDTypeSubCanvas();
    }
    var dndType = DNDCanvas.__subCanDndType;
    parentCanvas.addDNDType(dndType);
    parentCanvas.makeDropTarget(this.__hElem, dndType.type);
};

DNDCanvas.prototype.getBounds = function(hElem) {
    if (this.__isBody) {
        return getViewBounds(hElem);
    } else {
        return getLocalBounds(this.__hElem, hElem);
    }
};

DNDCanvas.prototype.makeDraggable = function(hElem, type) {
    if (hElem._dndType == null) {
        hElem._dndType = this.__dndTypes[type];
    }
    hElem._dndDrag = true;
    if (this.__groupType != null || SH.test) {
        this.__draggables.push(hElem);
    }
};


DNDCanvas.prototype.disposeDraggable = function(hElem) {
	if (this.dragElem === hElem) this.dragElem = null;
    hElem._dndType = null;
    hElem._dndDrag = null;
    if (this.__groupType != null || SH.test) {
        arrayRemoveStrict(this.__draggables, hElem);
        if (this.__groupType != null) this.__groupType.remove(hElem);
    }
};

DNDCanvas.prototype.makeDropTarget = function(hElem, type) {
    if (hElem._dndType == null) {
        hElem._dndType = this.__dndTypes[type];
    }
    hElem._dndDropBounds = this.getBounds(hElem);
    this.__dropTargets.push(hElem);
};

DNDCanvas.prototype.disposeDropTarget = function(hElem) {
    hElem._dndDropBounds = null;
    if (this.__dropTargets != null) arrayRemoveStrict(this.__dropTargets, hElem);
    if (this.dropElem === hElem) this.dropElem = null;
};

DNDCanvas.prototype.addDNDType = function(dndType) {
    this.__dndTypes[dndType.type] = dndType;
    dndType.__canvas = this;
};

DNDCanvas.prototype.removeDNDType = function(type) {
    if (this.__dndTypes) delete this.__dndTypes[type];
};

DNDCanvas.prototype.setGroup = function(dndGroupType) {
    if (dndGroupType != null) {
        dndGroupType.__canvas = this;
        this.addDNDType(new DNDTypeGrouper());
        if (this.__grouperElem == null) {
            this.__grouperElem =
                makeElementNbSpd(this.__hElem, "div", "groupSel");
            this.__grouperElem.style.display = "none";
            this.__groupType = null;
            this.makeDraggable(this.__grouperElem, DNDTypeGrouper.TYPE);
        }
        this.__groupType = dndGroupType;
    } else {
        this.__groupType = null;
    }
};

DNDCanvas.prototype.getGroupElements = function() {
	var grpEls = [];
	if (this.__groupType) {
		arrayAddAll(grpEls, this.__groupType.includes);
	}
	return grpEls;
};

DNDCanvas.prototype.localizePoint = function(pnt) {
    var r = this.__isBody ? getVisibleBounds() : getBounds(this.__hElem);
    this.__bounds = r;
    this.offsetX = -r.x;
    this.offsetY = -r.y;
    return new Point(this.offsetX + this.__hElem.scrollLeft + pnt.x,
            this.offsetY + this.__hElem.scrollTop + pnt.y);
};

DNDCanvas.prototype.mouseMove = function(evt) {
    if (this.dragElem != null) {
    	if (SH.is_ie && evt.button != 1) {
    		this.mouseUp(evt);
    		return true;
    	}
        clearSelection();

        var eX = getMouseX(evt);
        var eY = getMouseY(evt);

        var msX = this.offsetX + this.__hElem.scrollLeft + eX;
        var msY = this.offsetY + this.__hElem.scrollTop + eY;
        this.dmX = msX > 0 ? msX : 0;
        this.dmY = msY > 0 ? msY : 0;

        this.__checkAutoScroll(eX, eY);

        var holdThis = this;
        window.setTimeout(function() { holdThis.doMove(); }, 50);
        evt.cancelBubble = true;
        if (SH.is_safari) evt.cancelDefault();
        
        return false;
    }
    return true;
};

DNDCanvas.prototype.__checkAutoScroll = function(eX, eY) {
	if (SH.is_ie && this.__autoScroll != null) {
        var ats = this.__autoScroll;

        // check scroll thresholds
        var r = this.__bounds;
        if (eX >= (r.x  + r.w - ats[0])) {
            this.__hElem.scrollLeft += ats[0];
        } else if (eX <= (r.x + ats[0])) {
        	var sl = this.__hElem.scrollLeft;
        	sl -= ats[0];
            this.__hElem.scrollLeft = sl > 0 ? sl : 0;
        } else if (eY >= (r.y + r.h - ats[1])) {
            this.__hElem.scrollTop += ats[1];
        }  else if (eY <= (r.y + ats[1])) {
        	var st = this.__hElem.scrollTop;
        	st -= ats[1];
            this.__hElem.scrollTop = st > 0 ? st : 0;
        }
    }
};

DNDCanvas.prototype.doMove = function() {
    var de = this.dragElem;
    if (de != null) {
        var msX = this.dmX;
        var msY = this.dmY;
        if (this.lmX != msX || this.lmY != msY) {
            de._dndType.dragMove(msX, msY, de);
            this.lmX = msX;
            this.lmY = msY;
            this.findDropTarget(de, this);
        }
    }
};

DNDCanvas.prototype.mouseUp = function(evt) {
    if (this.dragElem != null) {
        var de = this.dragElem;
        this.dragElem = null;
        this.endDrag(de);
        if (evt) evt.cancelBubble = true;
        if (SH.is_ie) {
            this.__hElem.releaseCapture();
        }
        return false;
    }
    return true;
};

DNDCanvas.prototype.endDrag = function(dragElem) {
    DNDCanvas.__frameUncover();
    var dndType = dragElem._dndType;
    var drop = this.dropElem;
    if (drop == null) {
        dndType.noTargetDrop(dragElem, this);
    } else {
        var dropType = drop._dndType;
        dropType.dropExec(drop, dragElem);
        dropType.dropDone(drop);
        this.dropElem = null;
    }
    dndType.dragDone(dragElem);
};

DNDCanvas.prototype.startDrag = function(evt, target) {
    var dndType = target._dndType;
    if (dndType.__canvas !== this && dndType.__canvas.parentCanvas !== this) return true;
    if (dndType.__canvas.cancelDrag || dndType.__canvas.disableDrag) {
        dndType.__canvas.cancelDrag = false;
        return true;
    }
    if (!dndType.canDrag(target)) return true; 
    Ephemeral.hideAll();
    var holdThis = this;
    if (this.__groupType != null && this.__groupType.inGroup(target)) {
        if (this.parentDrag) {
            return this.parentCanvas.startDrag(evt, this.__groupType.getDragElement(null, this.parentCanvas, evt));
        }
        this.dragElem = this.__groupType.getDragElement(null, this, evt);
        dndType = this.__groupType;
    } else {
        if (this.__groupType != null) {
           this.__groupType.clear();
        }
        if (this.parentDrag && target !== this.__grouperElem) {
            return this.parentCanvas.startDrag(evt, target);
        }
        
        target = dndType.getDragElement(target, this, evt);
        this.dragElem = target;
    }
    this.calibrate();

    // setup mouse points
    var msX = this.offsetX + this.__hElem.scrollLeft + getMouseX(evt);
    var msY = this.offsetY + this.__hElem.scrollTop + getMouseY(evt);

    this.lmX = msX;
    this.lmY = msY;

    dndType.startDrag(msX, msY, this.dragElem);
    evt.cancelBubble = true;
    if (this.__isBody) {
        this.refreshDropTargetBounds(this.dragElem, this);
    } else {
        window.setTimeout(
            function() {holdThis.refreshDropTargetBounds(holdThis.dragElem, holdThis);}, 1);
    }
    target = null; // IE enclosure clean-up
    if (SH.is_ie) {
        window.setTimeout(
            function() {if (holdThis.dragElem != null) holdThis.__hElem.setCapture();}, 350);
    }
    window.setTimeout(
            function() {if (holdThis.dragElem != null) DNDCanvas.__frameCover(holdThis.__hElem);}, 350);
    return false;
};

DNDCanvas.prototype.calibrate = function() {
    var r = this.__bounds;
    this.offsetX = -r.x;
    this.offsetY = -r.y;
};

DNDCanvas.prototype.mouseDown = function(evt) {
    var target = getEventElement(evt);
    if ((SH.is_ie && evt.button != 1)
    		|| (!SH.is_ie && evt.button != 0)) { 
    	return false;
    }
    var rawX = getMouseX(evt);
    var rawY = getMouseY(evt);
    var r = this.__isBody ? getVisibleBounds() : getBounds(this.__hElem);
    this.__bounds = r;
    if (this.__groupType != null && target === this.__hElem) {
        if ((rawX > (r.x + r.w - 14)) || (rawY > (r.y + r.h - 14))) {
            return true;
        }
        target = this.__grouperElem;
    }
    do {
        if (target._dndType != null && target._dndDrag) {
            return this.startDrag(evt, target);
        } else if (target._dndNoDrag) {
        	return false;
        }
        target = target.parentNode;
    } while (target != null && target !== this.__hElem);
    return true;
};

DNDCanvas.prototype.getDndElem = function(evt) {
    var target = getEventElement(evt);
    do {
        if (target._dndType != null) return target;
        target = target.parentNode;
    } while (target != null && target !== this.__hElem);
    return null;
};

DNDCanvas.prototype.getMousePoint = function(evt) {
    return this.localizePoint(new Point(getMouseX(evt), getMouseY(evt)));
};

DNDCanvas.prototype.findDropTarget = function(dragElem, canvas) {
    var dTar;
    var de = dragElem;
    if (!de._dndType.canDropTest(de)) return;
    var dRect = canvas.getBounds(de);
    var drpEl = this.dropElem;
    if (drpEl != null && drpEl._dndType !== DNDCanvas.__subCanDndType) {
        if (Rectangle.intersects(drpEl._dndDropBounds, dRect)
                && drpEl !== de
                && drpEl._dndType.dropTest(drpEl, de)) {
            return;
        } else {
            drpEl._dndType.dropCancel(drpEl);
            this.dropElem = null;
        }
    }
    var dtargs = this.__dropTargets;
    for (var ii = dtargs.length - 1; ii >= 0 ; ii--) {
        dTar = dtargs[ii];
        if (dTar !== de) {
            if (Rectangle.intersects(dTar._dndDropBounds, dRect)) {
                if (dTar._dndType.dropTest(dTar, de)) {
                    dTar._dndType.dropReady(dTar);
                    this.dropElem = dTar;

                    // stop if this is good
                    return;
                }
            }
        }
    }
};

DNDCanvas.prototype.refreshDropTargetBounds = function(dragElem, canvas) {
    if (dragElem != null) {
        var de = dragElem;
        var dndType = dragElem._dndType;
        if (!dndType.canDropTest(de)) return;
        var dtargs = this.__dropTargets;
        var dTar;
        for (var ii = 0; ii < dtargs.length; ii++) {
            dTar = dtargs[ii];
            if (dTar !== de) {
                dTar._dndDropBounds = canvas.getBounds(dTar);
            }
        }
    }
};

DNDCanvas.prototype.groupArea = function(rect) {
    var group = this.__groupType;
    group.clear();
    var hElem;
    for (var ii = 0; ii < this.__draggables.length; ii++) {
        hElem = this.__draggables[ii];
        if (Rectangle.intersects(this.getBounds(hElem), rect)) {
           group.add(hElem);
        }
    }
};

DNDCanvas.prototype.isDragging = function() {
	return this.dragElem != null;
};

DNDCanvas.prototype.scrollToView = function(hElem) {
    var cr = this.__isBody ? getVisibleBounds() : getBounds(this.__hElem);
    var r = this.getBounds(hElem);

    // attempt to center
    var sx = r.x - (cr.w / 2);
    var sy = r.y - (cr.h / 2);
    this.__hElem.scrollTop = sy > 0 ? sy : 0;
    this.__hElem.scrollLeft = sx > 0 ? sx : 0;
};

DNDCanvas.prototype.initAutoScroll = function(bottomZone, rightZone) {
    this.__autoScroll = [rightZone, bottomZone];
};

DNDCanvas.prototype.simulateDrag = function(dragElem, toX, toY, toCanvas) {
	var evt = createMouseEvent(dragElem, 0, 0, 0);
	this.mouseDown(evt, dragElem);
	evt.pageX = toX;
	evt.pageY = toY;
	if (!toCanvas) toCanvas = this;
	setTimeout(function() { 
		toCanvas.mouseMove(evt);
		setTimeout(function() { toCanvas.mouseUp(evt); }, 50);
	}, 50);
	dragElem = null;
};

DNDCanvas.prototype.onScroll = function(evt) {
	if (this.isDragging()) {
		var holdThis = this;
		window.setTimeout(
            function() { holdThis.refreshDropTargetBounds(holdThis.dragElem, holdThis);}, 50);
	} else if (this.parentCanvas && this.parentCanvas.isDragging()) {
		var holdThis = this;
		window.setTimeout(
            function() { holdThis.refreshDropTargetBounds(holdThis.parentCanvas.dragElem, holdThis.parentCanvas);}, 50);
	}
};

DNDCanvas.prototype.dispose = function() {
	if (this.parentCanvas && this.__hElem) {
		this.parentCanvas.disposeDropTarget(this.__hElem);
		this.parentCanvas = null;
	}
	if (this.__hElem) {
		this.__hElem._dndType = null;
    	this.__hElem._dndCanvas = null;
    	removeEventHandler(this.__hElem, "mousedown", this.__msDownEvt);
    	removeEventHandler(this.__hElem, "mousemove", this.__msMoveEvt);
    	removeEventHandler(this.__hElem, "mouseup", this.__msUpEvt);
    this.__hElem = null;
	}
    this.__dndTypes = null;
    this.__dropTargets = null;
    this.__autoScroll = null;
    this.__groupType = null;
    this.__grouperElem = null;
    this.__draggables = null;
    this.__bounds = null;
    this.dragElem = null;
    this.dropElem = null;
};

DNDCanvas.stopAllDrags = function() {
	for (var ii = 0; ii < dndCanvases.length; ii++) {
		dndCanvases[ii].mouseUp();
	}
};

DNDCanvas.__frameUncover = function () {
    if (this.__fcovers != null) {
    	var elem = this.__fcovers;
    	this.__fcovers = null;
    	elem.parentNode.removeChild(elem);
    }
};

DNDCanvas.__frameCover = function(uiParent) {
    if (this.__fcovers == null) {
      	var elem = makeElement(uiParent, "div", "frameCover");
      	this.__fcovers = elem;
    	var frect = (uiParent === document.body) ? getVisibleBounds() : getSize(uiParent);
    	elem.style.left = "0px";
        elem.style.top = "0px";
        elem.style.width = frect.w + "px";
        elem.style.height = frect.h + "px";
    }
};

/**
 * TypeHandler
 */
DNDTypeHandler.prototype.constructor = DNDTypeHandler;

function DNDTypeHandler() {
    this.type = "default";
    this.offX = 0;
    this.offY = 0;
    this.limitY = null;
    this.limitX = null;
}

DNDTypeHandler.prototype.canDrag = function(dragElem) {
    return true;
};

DNDTypeHandler.prototype.getDragElement = function(dragElem, canvas, evt) {
    return dragElem;
};

DNDTypeHandler.prototype.canDropTest = function(dragElem) {
    return true;
};

DNDTypeHandler.prototype.dragDone = function(dragElem) {
    // empty
};

DNDTypeHandler.prototype.startDrag = function(x, y, dragElem) {
    var rect = this.__canvas.getBounds(dragElem);
    this.offX = rect.x - x;
    this.offY = rect.y - y;

    dragElem.style.cursor = "default";
};

DNDTypeHandler.prototype.dragMove = function(x, y, dragElem) {
    if (this.limitX == null) dragElem.style.left = (this.offX + x) + "px";
    else dragElem.style.left = this.limitX + "px";

    if (this.limitY == null) dragElem.style.top = (this.offY + y) + "px";
    else dragElem.style.top = this.limitY + "px";
};

DNDTypeHandler.prototype.dropTest = function(dropElem, dragElem) {
    return false;
};

DNDTypeHandler.prototype.dropReady = function(dropElem) {
    this.setDropCss(dropElem, true);
};

DNDTypeHandler.prototype.dropExec = function(dropElem, dragElem) {
    dragElem.style.cursor = "";
};

DNDTypeHandler.prototype.noTargetDrop = function(dragElem, canvas) {
    dragElem.style.cursor = "";
    if (canvas && canvas.canvasDrop != null) {
        canvas.canvasDrop(dragElem);
    }
};

DNDTypeHandler.prototype.dropDone = function(dropElem) {
    this.setDropCss(dropElem, false);
};

DNDTypeHandler.prototype.dropCancel = function(dropElem) {
    this.setDropCss(dropElem, false);
};

DNDTypeHandler.prototype.setGrouped = function(dragElem, grouped) {
    exAddClass(dragElem, "dndGroup", !grouped);
};

DNDTypeHandler.prototype.setDropCss = function(dropElem, on) {
    exAddClass(dropElem, "dndDrop", !on);
};


DNDGroup.prototype = new DNDTypeHandler();
DNDGroup.prototype.constructor = DNDGroup;

/**
 * Group
 */
function DNDGroup() {
    this.includes = [];
    this.type = "dndGroup"
}

DNDGroup.prototype.canDropTest = function(dragElem) {
    return false;
};

DNDGroup.prototype.isEmpty = function() {
    return this.includes.length < 1;
};

DNDGroup.prototype.size = function() {
    return this.includes.length;
};

DNDGroup.prototype.inGroup = function(hElem) {
    return arrayFindStrict(this.includes, hElem) >= 0;
};

DNDGroup.prototype.add = function(hElem) {
    arrayAdd(this.includes, hElem);
    hElem._dndType.setGrouped(hElem, true);
};

DNDGroup.prototype.remove = function(hElem) {
    arrayRemoveStrict(this.includes, hElem);
};

DNDGroup.prototype.clear = function() {
    var hElem;
    for (var ii = 0; ii < this.includes.length; ii++) {
        hElem = this.includes[ii];
        hElem._dndType.setGrouped(hElem, false);
    }
    this.includes = [];
};

DNDGroup.prototype.getDragElement = function(dragElem, canvas, evt) {
    return { _dndType : this };
};

DNDGroup.prototype.getBounds = function(canvas) {
    if (this.includes.length > 0) {
        var bounds = canvas.getBounds(this.includes[0]);
        for (var ii = 1; ii < this.includes.length; ii++) {
            bounds = bounds.union(canvas.getBounds(this.includes[ii]));
        }
        return bounds;
    } else {
        return new Rectangle();
    }
};

DNDGroup.prototype.startDrag = function(x, y) {
    var rect = this.getBounds(this.__canvas);
    this.offX = rect.x - x;
    this.offY = rect.y - y;
    var dragElem;
    for (var ii = 0; ii < this.includes.length; ii++) {
        dragElem = this.includes[ii];
        dragElem._dndType.startDrag(x, y, dragElem);
    }
};

DNDGroup.prototype.dragMove = function(x, y) {
    if (this.offX + x < 0) x = -this.offX;
    if (this.offY + y < 0) y = -this.offY;

    var dragElem;
    for (var ii = 0; ii < this.includes.length; ii++) {
        dragElem = this.includes[ii];
        dragElem._dndType.dragMove(x, y, dragElem);
    }
};

DNDGroup.prototype.noTargetDrop = function(ignore, canvas) {
    var dragElem;
    for (var ii = 0; ii < this.includes.length; ii++) {
        dragElem = this.includes[ii];
        dragElem._dndType.noTargetDrop(dragElem);
    }
};


DNDTypeGrouper.prototype = new DNDTypeHandler();

/**
 * Grouper
 */
DNDTypeGrouper.TYPE = "dndTypeGrouper";

function DNDTypeGrouper() {
    this.type = DNDTypeGrouper.TYPE;
}

DNDTypeGrouper.prototype.canDropTest = function(dragElem) {
    return false;
};

DNDTypeGrouper.prototype.startDrag = function(x, y, dragElem) {
    dragElem._dRec = dragElem._startRec = new Rectangle(x, y, 1, 1);
    this.drawSize(dragElem);
    dragElem.style.display = "block";
};

DNDTypeGrouper.prototype.drawSize = function(dragElem) {
    var rect = dragElem._dRec;
    var style = dragElem.style;
    style.left = rect.x + "px";
    style.top = rect.y + "px";
    style.width = rect.w + "px";
    style.height = rect.h + "px"
};

DNDTypeGrouper.prototype.noTargetDrop = function(dragElem) {
    dragElem.style.display = "none";
    this.__canvas.groupArea(dragElem._dRec);
};

DNDTypeGrouper.prototype.dragMove = function(x, y, dragElem) {
    dragElem._dRec = dragElem._startRec.union(new Rectangle(x, y, 1, 1));
    this.drawSize(dragElem);
};


DNDTypeDummy.prototype = new DNDTypeHandler();

/**
 * DragDummy
 **/
function DNDTypeDummy() {
    this.type = "dndDummy";
}
/*
DNDTypeDummy.prototype.startDrag = function(x, y, dragElem) {
    var rect = this.__canvas.getBounds(dragElem._actElem);
    if (this.__canvas !== this.actualCanvas) {
        rect.x -= this.__canvas.offsetX - this.__canvas.__hElem.scrollLeft;
        rect.y -= this.__canvas.offsetY - this.__canvas.__hElem.scrollTop;
    }
    this.offX = rect.x - x;
    this.offY = rect.y - y;

    dragElem.style.cursor = "default";
    this.dragMove(x, y, dragElem);
};*/

DNDTypeDummy.prototype.getDragElement = function(dragElem, canvas, evt) {
    //if (dragElem._dummy) return true;
    var dumElem = makeElement(canvas.__hElem, "div",
            "dndDummy" + (this.cssClass != null ? " " + this.cssClass : ""));
    //dumElem._dummy = true;
    dumElem._actElem = dragElem;
    dumElem._dndType = this;
    dumElem.appendChild(dragElem.cloneNode(true));
    var rect = canvas.getBounds(dragElem);
    dumElem.style.left = rect.x + "px";
    dumElem.style.top = rect.y + "px";
    window.setTimeout(function() {
            dumElem.style.visibility = "visible";
            dumElem = null; dragElem = null; // enclosure clean up
        }, 200);
    return dumElem;
};

DNDTypeDummy.prototype.dropTest = function(dropElem, dragElem) {
    return dragElem.style.visibility == "visible";
};

DNDTypeDummy.prototype.dragDone = function(dragElem) {
    if (dragElem.parentNode)
        dragElem.parentNode.removeChild(dragElem);
};

DNDGroupDummy.prototype = new DNDGroup();

/**
 * Group
 */
function DNDGroupDummy() {
    this.includes = [];
    this.type = "dndGrpDum"
}

DNDGroupDummy.prototype.canDropTest = function(dragElem) {
    return true;
};

DNDGroupDummy.prototype.getDragElement = function(dragElem, canvas, evt) {
    if (dragElem != null) return dragElem;
    var dumElem = makeElement(canvas.__hElem, "div", "dndDummy",
            "Group (" + this.includes.length + ")");
    //dumElem._dummy = true;
    dumElem._dndType = this;
    var rect = this.getBounds(canvas);
    dumElem.style.left = rect.x + "px";
    dumElem.style.top = rect.y + "px";
    dumElem.style.width = rect.w + "px";
    dumElem.style.height = rect.h + "px";
    window.setTimeout(function() {
            dumElem.style.visibility = "visible";
            dumElem = null; // enclosure clean up
        }, 200);
    this.dumElem = dumElem;
    return dumElem;
};

DNDGroupDummy.prototype.startDrag = function(x, y) {
    DNDTypeHandler.prototype.startDrag.apply(this, [x, y, this.dumElem]);
};

DNDGroupDummy.prototype.dragMove = function(x, y) {
    DNDTypeHandler.prototype.dragMove.apply(this, [x, y, this.dumElem]);
};

DNDGroupDummy.prototype.inGroup = function(hElem) {
    if (hElem === this.dumElem) return true;
    return DNDGroup.prototype.inGroup.apply(this, [hElem]);
};

DNDGroupDummy.prototype.dragDone = function(dragElem) {
    if (dragElem.parentNode)
        dragElem.parentNode.removeChild(dragElem);
};

DNDTypeSubCanvas.prototype = new DNDTypeHandler();

/**
 * SubCanvas
 **/
function DNDTypeSubCanvas() {
    this.type = "dndSubCan";
    this.lastDrop = null;
}

DNDTypeSubCanvas.prototype.canDrag = function(dragElem) {
    return false;
};

DNDTypeSubCanvas.prototype.dropTest = function(dropElem, dragElem) {
    var subCanvas = dropElem._dndCanvas;
    if (subCanvas.disableDrag) return false;
    if (this.lastDrop !== dropElem) {
        this.lastDrop = dropElem;
        dropElem = null;
        window.setTimeout( function() {
                subCanvas.refreshDropTargetBounds(dragElem, subCanvas.parentCanvas);
                dragElem = null;
                }, 1);
    }
    var der = getBounds(dragElem);
    subCanvas.__checkAutoScroll(der.x, der.y);
    subCanvas.findDropTarget(dragElem, subCanvas.parentCanvas);
    return true;
};

DNDTypeSubCanvas.prototype.dropExec = function(dropElem, dragElem) {
    var subCanvas = dropElem._dndCanvas;
    subCanvas.endDrag(dragElem);
};

/*
DNDTypeSubCanvas.prototype.dropReady = function(dropElem) {
    this.setDropCss(dropElem, true);
    var subCanvas = dropElem._dndCanvas;
    var subDrop = subCanvas.dropElem;
    if (subDrop != null) {
        subDrop._dndType.dropReady(subDrop);
    }
};
*/

DNDTypeSubCanvas.prototype.dropDone = function(dropElem) {
    this.lastDrop = null;
};

DNDTypeSubCanvas.prototype.dropCancel = function(dropElem) {
    this.setDropCss(dropElem, false);
    var subCanvas = dropElem._dndCanvas;
    var subDrop = subCanvas.dropElem;
    if (subDrop != null) {
        subDrop._dndType.dropCancel(subDrop);
    }
};
