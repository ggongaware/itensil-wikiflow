/**
 * Lib: ProcMap
 * Process Map Builder Components
 * Prefix: pm, Pm, PM
 */

////////////////////////////////// CONSTANTS ///////////////////////////////////
var PM = {
 ARR_CENTER : -4,
 HOOK_CENTER : -4,
 JOIN_CENTER_X : -21,
 JOIN_CENTER_Y : -31,
 LINEWIDTH : 2,
 UP : "U",
 DOWN : "D",
 LEFT : "L",
 RIGHT : "R",
 JOIN : "J",
 STYLECLASSES : {
    "activity" : "pmAct",
    "activity-launch" : "pmSub",
    "start" : "pmStart",
    "end" : "pmEnd",
    "switch" : "pmSwitch",
    "timer" : "pmTimer",
    "join" : "pmJoin",
    "group" : "pmGroup",
    "enter" : "pmEnter",
    "exit" : "pmExit"},
 GRID_SIZE : 10,
 SNAP : function(pixel) {
    if (pixel == 0) return 0;
    return Math.round(pixel / 5) * 5;
 },
 CLOSE_PROPS:true,
 LOCK_START:true,

 // 14 second auto update delay
 AUTO_DELAY : 14
};

////////////////////////////////// CLASSES /////////////////////////////////////

/**
 * Path Hook
 */
 
function PmPathHook(model, path, isStart, pnt, direction) {
    if (arguments.length > 0) {
        this.model = model;
        this.isStart = isStart;
        this.path = path;
        var element = makeElementNbSpd(model.canvas, "div", "pmHookN" +
        	(path.constructor === PmLine ? "not" : ""));
        this.element = element
        element.peer = this;
        model.dndCanvas.makeDraggable(element, PmObjDNDType.TYPE);
        this.pnt = pnt;
        this.direction = -1;
        this.setDirection(direction);
        this.draw();
    }
}

PmPathHook.prototype.dispose = function() {
    this.model.dndCanvas.disposeDropTarget(this.element);
    this.model.dndCanvas.disposeDraggable(this.element);
    this.pnt = null;
    this.model = null;
    this.path = null;
    this.element.peer = null;
    this.element = null;
};

PmPathHook.prototype.setDirection = function(dir) {
    if (!this.isStart) {
        var rdir = Point.reverse[dir];
        if (rdir != this.direction)  {
            this.path.dirChanged = this.direction >= 0;
            this.direction = rdir;
            this.element.className = "pmArr pmArr" + Point.directions[rdir] + 
            	(this.path.constructor === PmLine ? "not" : "");
            this.path.xNode.setAttribute("endDir", Point.directions[dir]);
        }
    } else {
        if (dir != this.direction) {
            this.path.dirChanged = this.direction >= 0;
            this.direction = dir;
            this.element.className = "pmHook" + Point.directions[dir] + 
            	(this.path.constructor === PmLine ? "not" : "") ;
            this.path.xNode.setAttribute("startDir",  Point.directions[dir]);
        }
    }
};

PmPathHook.prototype.setPoint = function(pnt, dir) {
    this.pnt = pnt;
    if (dir != -1) {
       this.setDirection(dir);
    }
    this.path.fullSolve();
    this.draw();
};

PmPathHook.prototype.setDragPosition = function(x, y, asGroup) {
    var path = this.path;
    if (asGroup && path.allInGroup()) {
        this.pnt.x = this.dndOffX + x;
        this.pnt.y = this.dndOffY + y;
        this.draw();
    } else if (!asGroup
         || (!this.isStart && path.toObj != null && path.toObj._inDndGroup)
         || (this.isStart && path.fromObj != null && path.fromObj._inDndGroup)){

        // calc new direction
        var np = new Point(this.dndOffX + x, this.dndOffY + y);
        if (path.toObj == null || path.fromObj == null)
            this.setDirection(
                this.isStart ? np.direction(path.endHook.pnt) : np.direction(this.pnt));

        this.pnt = np;
        path.quickSolve();
        this.draw();
    } else if (asGroup && this.constructor === PmJoinHook) {
        this.pnt.x = this.dndOffX + x;
        this.pnt.y = this.dndOffY + y;
        path.quickSolve();
        this.draw();
    }
};

PmPathHook.prototype.limit = function(edge) {
    var obj;
    var pA;
    var pB;
    if (this.isStart) {
        obj = this.path.fromObj;
        pA = edge.p1;
        pB = edge.p2;
    } else {
        obj = this.path.toObj;
        pA = edge.p2;
        pB = edge.p1;
    }
    if (obj != null) {
        obj.limitHook(pA, this.direction);
    }
    if (edge.isVertical) {
        this.pnt.x = pA.x;
        pB.x = pA.x;
    } else {
        this.pnt.y = pA.y;
        pB.y = pA.y;
    }
    this.draw();
};

PmPathHook.prototype.calcOffset = function(x, y) {
    this.dndOffX = PM.SNAP(this.pnt.x - x);
    this.dndOffY = PM.SNAP(this.pnt.y - y);
};

PmPathHook.prototype.noTargetDrop = function() {
    if (this.path == this.model.tempPath) {
        this.model.tempPath.remove();
        this.model.tempPath = null;
    } else {
        if (this.isStart) {
            this.path.setFrom(null, true);
        } else {
            this.path.setTo(null, true);
        }
        this.path.fullSolve();
   }
};

PmPathHook.prototype.groupDrop = function() {
    this.updateModel();
};

PmPathHook.prototype.draw = function () {
    var style = this.element.style;
    style.left = (PM.HOOK_CENTER + this.pnt.x) + "px";
    style.top = (PM.HOOK_CENTER + this.pnt.y) + "px";
};

PmPathHook.prototype.remove = function() {
    this.model.canvas.removeChild(this.element);
    this.dispose();
};

PmPathHook.prototype.updateModel = function() {
    this.path.updateModel();
};


/**
 * Path Edge
 */
PmPathEdge.prototype = new LinkListNode;
PmPathEdge.prototype.constructor = PmPathEdge;

function PmPathEdge(model, path, p1, p2) {
    this.model = model;
    this.path = path;
    var element = makeElementNbSpd(model.canvas, "div", this.path.vlineClass);
    this.element = element;
    this.setPoints(p1, p2);
    element.peer = this;
    model.dndCanvas.makeDraggable(element, PmObjDNDType.TYPE);
    model.dndCanvas.makeDropTarget(element, PmObjDNDType.TYPE);
    // @todo only  attach events on full solve
}

PmPathEdge.prototype.dispose = function() {
    this.model.dndCanvas.disposeDropTarget(this.element);
    this.model.dndCanvas.disposeDraggable(this.element);
    this.model = null;
    this.path = null;
    this.element.peer = null;
    this.element = null;
};

PmPathEdge.prototype.draw = function(dist) {
    var p = this.p1;
    if (dist < 0) {
        dist = -dist;
        p = this.p2;
    }
    var style = this.element.style;
    style.left = p.x + "px";
    style.top = p.y + "px";
    if (this.isVertical) {
        style.height = (dist + PM.LINEWIDTH) + "px";
    } else {
        style.width = (dist + PM.LINEWIDTH) + "px";
    }
};

PmPathEdge.prototype.dropTest = function(dragObj) {
    if (this.path.constructor != PmLine && dragObj instanceof PmStep) {
        if (this.path.toObj != dragObj && this.path.fromObj != dragObj) {
            if (dragObj.constructor === PmEnd || dragObj.constructor === PmStart
            	 	|| (dragObj.toPaths.length > 0 && dragObj.fromPaths.length > 0)) {
                return false;
            }
            if (this.path.toObj && this.path.fromObj &&
             		(dragObj.toPaths.length > 0 || dragObj.fromPaths.length > 0)) {
            	return false;
            }
            return true;
        }
    }
    // @todo implement all the UI problems of path to path
    /*if (dragObj.constructor == PmPathHook && !dragObj.isStart) {
        if (dragObj.path != this.path) {
            return true;
        }
    }*/
    return false;
};

PmPathEdge.prototype.dropExec = function(dragObj) {
    if (dragObj instanceof PmStep) {
        if (this.path.constructor != PmJoin &&
                this.path.fromObj == null && dragObj.mode != "end") {
            this.path.setFrom(dragObj, true);
            dragObj.snapHook(this.path.startHook);
        } else if (this.path.toObj == null) {
            this.path.setTo(dragObj, true);
            dragObj.snapHook(this.path.endHook);
        } else if (this.path.toObj != dragObj && this.path.fromObj != dragObj) {

            //divide path
            var rect = this.model.dndCanvas.getBounds(dragObj.element);
            var dir = this.p1.direction(this.p2);
            var op1r = this.p1.toRect();
            var op2r = this.p2.toRect();
            var pnt;
            if (this.isVertical) {
                pnt = new Point(this.p1.x, rect.y + (rect.h/2));
            } else {
                pnt = new Point(rect.x + (rect.w/2), this.p1.y);
            }
            var path = this.path;
            var path2 = path.divide(pnt, dir);

            // hook
            path2.setTo(path.toObj, true);
            path2.setFrom(dragObj, true);
            dragObj.snapHook(path2.startHook, null, !Rectangle.intersects(rect, op2r));
            path.setTo(dragObj, true);
            dragObj.snapHook(path.endHook, null, !Rectangle.intersects(rect, op1r));
        }
        dragObj.updateModel();
    } else if (dragObj.constructor === PmPathHook && !dragObj.isStart) {
        if (dragObj.path != this.path) {
            dragObj.path.setTo(this.path, true);
            this.model.setChanged(dragObj.path);
            if (dragObj.path == this.model.tempPath) {
                this.model.tempPath = null;
                this.model.canvasObj.setClickMode(null);
            }
            dragObj.path.fullSolve();
        }
    }
};

PmPathEdge.prototype.calcOffset = function(x, y) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.dndOffX = PM.SNAP(rect.x - x);
    this.dndOffY = PM.SNAP(rect.y - y);
};

PmPathEdge.prototype.setDragPosition = function(x, y, asGroup) {
    if (this.element == null) return;
    var dist = this.p1.distance(this.p2);
    var didGroup = false;
    var pA = this.p1;
    var pB = this.p2;
    var pDist = dist;
    if (dist < 0) {
        pDist = -dist;
        pA = this.p2;
        pB = this.p1;
    }
    if (asGroup && this.path.allInGroup()) {
        pA.x = this.dndOffX + x;
        pA.y = this.dndOffY + y;
        if (this.isVertical) {
            pB.x = pA.x;
            pB.y = pA.y + pDist;
        } else {
            pB.y = pA.y;
            pB.x = pA.x + pDist;
        }
        didGroup = true;
    } else {
        if (this.isVertical) {
            pA.x = this.dndOffX + x;
            pB.x = pA.x;
        } else {
            pA.y = this.dndOffY + y;
            pB.y = pA.y;
        }
    }
    if (!didGroup) {
        if (this.nextNode == null) {
            this.path.endHook.limit(this);
        }
        if (this.prevNode != null) {
            this.prevNode.stretchPrev(this.p1);
        } else {
            this.path.startHook.limit(this);
            if (this.nextNode == null) {
                this.path.endHook.limit(this);
            }
        }
        if (this.nextNode != null) {
            this.nextNode.stretchNext(this.p2);
        }
    }
    this.draw(dist);
};

PmPathEdge.prototype.stretchPrev = function (p) {
    if (this.isVertical) {
        this.p2.y = p.y;
    } else {
        this.p2.x = p.x;
    }
    this.draw(this.p1.distance(this.p2));
};

PmPathEdge.prototype.stretchNext = function (p) {
    if (this.isVertical) {
        this.p1.y = p.y;
    } else {
        this.p1.x = p.x;
    }
    this.draw(this.p1.distance(this.p2));
};

PmPathEdge.prototype.alignEnd = function(pnt) {
    var exact = 0;
    var dist1 = this.p1.distance(this.p2);
    if (this.isVertical) {
        if (this.p1.x != pnt.x) {
            this.p1.x = pnt.x;
            this.p2.x = pnt.x;
            exact = 1;
        }
        this.p2.y = pnt.y;
    } else {
        if (this.p1.y != pnt.y) {
            this.p1.y = pnt.y;
            this.p2.y = pnt.y;
            exact = 1;
        }
        this.p2.x = pnt.x;
    }
    var dist2 = this.p1.distance(this.p2);
    if ((dist1 < 0 && dist2 < 0) || (dist1 > 0 && dist2 > 0)) {
        this.draw(dist2);
        return exact;
    }
    return 2;
};

PmPathEdge.prototype.alignStart = function(pnt) {
    var exact = 0;
    var dist1 = this.p1.distance(this.p2);
    if (this.isVertical) {
        if (this.p2.x != pnt.x) {
            this.p2.x = pnt.x;
            this.p1.x = pnt.x;
            exact = 1;
        }
        this.p1.y = pnt.y;
    } else {
        if (this.p2.y != pnt.y) {
            this.p2.y = pnt.y;
            this.p1.y = pnt.y;
            exact = 1;
        }
        this.p1.x = pnt.x;
    }
    var dist2 = this.p1.distance(this.p2);
    if ((dist1 < 0 && dist2 < 0) || (dist1 > 0 && dist2 > 0)) {
        this.draw(dist2);
        return exact;
    }
    return 2;
};

PmPathEdge.prototype.setPoints = function(p1, p2) {
    this.isVertical = false;
    var dir = p2.direction(p1);
    if (dir == Point.NORTH || dir == Point.SOUTH) {
        p2.alignVertical(p1);
        this.isVertical = true;
        this.element.className =  this.path.vlineClass;
    } else {
        p2.alignHorizontal(p2);
        this.element.className =  this.path.hlineClass;
    }
    var dist = p1.distance(p2);
    this.p1 = p1;
    this.p2 = p2;
    this.draw(dist);
};

PmPathEdge.prototype.remove = function() {
    if (this.path.startEdge == this) {
        this.path.startEdge = this.nextNode;
    }
    if (this.path.lastEdge == this) {
        this.path.lastEdge = this.prevNode;
    }
    this.unlink();
    this.model.canvas.removeChild(this.element);
    this.dispose();
};

PmPathEdge.prototype.updateModel = function() {
    this.path.updateModel();
};

PmPathEdge.prototype.noTargetDrop = function() {
    this.updateModel();
};

PmPathEdge.prototype.groupDrop = function() {
};


PmPath.prototype = new XFDirectBinding();
PmPath.prototype.constructor = PmPath;

/**
 * Path
 */
function PmPath(model, xNode) {
	this.vlineClass = "pmPthV pmPthV";
	this.hlineClass = "pmPthH pmPthH";
    if (arguments.length > 0) {
        this.model = model;
        var id = xNode.getAttribute("id");
        this.oId = id;
        this.model.idCtx.addVar(id);
        this.startHook = null;
        this.endHook = null;
        this.startEdge = null;
        this.lastEdge = null;
        this.fromObj = null;
        this.toObj = null;
        this.toPaths = [];
        this.xNode = xNode;
        this.initBind(model.xfrm, xNode);
        this.edgeCount = 0;
        arrayAdd(model.paths, this);
        var labNd = Xml.matchOne(this.xNode, "label");
        if (labNd != null && Xml.stringForNode(labNd) != "") {
            this.label = new PmPathLabel(model, labNd, this);
        }
    }
}

PmPath.prototype.dispose = function() {
    if (this.model == null) return;
    this.xNode = null;
    this.startHook = null;
    this.endHook = null;
    this.label = null;
    this.model = null;
    this.toPaths = null;
    if (this._diag != null) this._diag.dispose();
    XFDirectBinding.prototype.dispose.apply(this, []);
};

PmPath.prototype.disposeAll = function() {
    if (this.startHook != null) this.startHook.dispose();
    if (this.endHook != null) this.endHook.dispose();
    if (this.label != null) this.label.dispose();
    var edge = this.startEdge;
    while (edge != null) {
        var ed = edge;
        edge = edge.nextNode;
        ed.dispose();
    }
    this.startEdge = null;
    this.lastEdge = null;
    this.dispose();
};

PmPath.prototype.setTrace = function(bOn) {
	if (this.endHook) PmPath.__traceClass(this.endHook.element, bOn);
	var edge = this.startEdge;
    while (edge != null) {
        var ed = edge;
        edge = edge.nextNode;
        PmPath.__traceClass(ed.element, bOn);
    }
};

PmPath.__traceClass = function(hElem, bOn) {
	if (hElem) {
		var cn = hElem.className;
		if (bOn) hElem.className = cn + "_tra";
		else  hElem.className = cn.substring(0, cn.length - 4);
	}
};

PmPath.prototype.clear = function() {
	if (this._diag != null && this._diag.isShowing()) {
        this._diag.destroy();
    }
    this._diag = null;
	this.clearEdges();
	if (this.startHook != null) this.startHook.remove();
	this.startHook = null;
    if (this.endHook != null) this.endHook.remove();
    this.endHook = null;
    if (this.label != null) this.label.remove();
    this.label = null;
};

PmPath.prototype.remove = function() {
    this.clear();
    if (this.fromObj != null) {
        this.setFrom(null);
    }
    this.destroyNode();
    this.setTo(null);
    this.model.idCtx.removeVar(this.getId());
    while (this.toPaths.length > 0) {
        this.toPaths[0].setTo(null);
    }
    arrayRemove(this.model.paths, this);
    this.model.setChanged();
    this.model.xfMod.rebuild();
    XFDirectBinding.prototype.remove.apply(this, []);
};

PmPath.prototype.clearEdges = function() {
    while (this.lastEdge != null) {
        var edge = this.lastEdge;
        edge.remove();
    }
    this.edgeCount = 0;
};

PmPath.prototype.setStartHook = function(pnt, direction) {
    this.startHook = PM.JOIN == direction ?
            new PmJoinHook(this.model, this, pnt) :
            new PmPathHook(this.model, this, true, pnt, direction);
    this.lastSp = pnt.clone();
};

PmPath.prototype.setEndHook = function(pnt, direction) {
    this.endHook = new PmPathHook(this.model, this, false, pnt, direction);
    this.lastEp = pnt.clone();
};

PmPath.prototype.setFrom = function(obj, isNewOrMove) {
    if (this.fromObj != null) {
        this.fromObj.removePath(this);
    }
    this.fromObj = obj;
    if (obj != null) {
        obj.addPath(this, isNewOrMove);
    }
    if (isNewOrMove) 
    	this.model.xfMod.markChanged(this.xNode);
};

PmPath.prototype.setTo = function(obj, isNewOrMove) {
    if (this.toObj != null) {
        arrayRemoveStrict(this.toObj.toPaths, this);
    }
    this.toObj = obj;
    if (obj != null) {
        this.xNode.setAttribute("to", obj.getId());
        arrayAdd(obj.toPaths, this);
    } else {
        this.xNode.setAttribute("to", "");
    }
    if (isNewOrMove) {
    	this.model.xfMod.markChanged(this.xNode, "to");
    	this.model.xfMod.markChanged(this.xNode);
    }
};

PmPath.prototype.setId = function(id) {
	if (this.oId != id) {
		this.model.idCtx.removeVar(this.oId);
		this.model.idCtx.addVar(id);
	    this.xNode.setAttribute("id", id);
	}
};

PmPath.prototype.getId = function() {
    return this.oId;
};

PmPath.prototype.getLabel = function() {
    var nd = Xml.matchOne(this.xNode, "label");
    return nd != null ? Xml.stringForNode(nd) : "";
};

PmPath.prototype.autoPositionLabel = function() {
    if (this.label != null) {
        var big = this.startEdge;
        var posX;
        var posY;
        if (big != null) {
	        var edge = big;
	        var max = 0;
	        var dist;
	
	        //get biggest edge
	        while (edge != null) {
	            dist = Math.abs(edge.p1.distance(edge.p2));
	            if (dist > max) {
	                big = edge;
	                max = dist;
	            }
	            edge = edge.nextNode;
	        }
	        dist = big.p1.distance(big.p2);
	        
	        if (big.isVertical) {
	           posX = big.p1.x + 10;
	           posY = PM.SNAP(big.p1.y + (dist / 2));
	        } else {
	           posY = big.p1.y + 10;
	           posX = PM.SNAP(big.p1.x + (dist / 2));
	        }
        } else {
        	posX = this.startHook.pnt.x + 10;
        	posY = this.startHook.pnt.y + 10;
        }
        var labElem = this.label.element;
        labElem.style.left = posX + "px";
        labElem.style.top = posY + "px";
        this.label.updateModel();
    }
};

PmPath.prototype.addEdge = function(p1 ,p2) {
    var edge = new PmPathEdge(this.model, this, p1 ,p2);
    if (this.startEdge == null) {
        this.lastEdge = this.startEdge = edge;
    } else {
        this.lastEdge.append(edge);
        this.lastEdge = edge;
    }
    this.edgeCount++;
    return edge;
};

PmPath.prototype.allInGroup = function() {
    var edge = this.startEdge;
    if (!this.startHook._inDndGroup || !this.endHook._inDndGroup
            || (this.toObj != null && !this.toObj._inDndGroup)
            || (this.fromObj != null && !this.fromObj._inDndGroup)) {
        return false;
    }
    while (edge != null) {
        if (!edge._inDndGroup) return false;
        edge = edge.nextNode;
    }
    return true;
};

PmPath.prototype.anyInGroup = function() {
    var edge = this.startEdge;
    if (this.startHook._inDndGroup || this.endHook._inDndGroup) {
        return true;
    }
    while (edge != null) {
        if (edge._inDndGroup) return true;
        edge = edge.nextNode;
    }
    return false;
};

/*
            C
         |-----|
      B  |     |  D
         |     V
    x----|
      A
*/
PmPath.pointSolve = function(sp, sdir, ep, edir) {
	var eBuf = [];
	var pCnt = 0;
	
    // A
    var pnt = sp.move(PM.GRID_SIZE, sdir);
    eBuf[pCnt++] = pnt;

    // A2 (possible 180)
    var pnt3 = ep.move(PM.GRID_SIZE, Point.reverse[edir]);
    var pnt2 = new Point(pnt3.x, pnt.y);
    if (pnt.direction(pnt2) == Point.reverse[sdir] && !pnt.equals(pnt2)) {
        pnt = pnt.move(PM.GRID_SIZE, Point.rightturn[sdir]);
        pnt2 = new Point(pnt3.x, pnt.y);
        eBuf[pCnt++] = pnt;
    }
    // D2 (possible 180)
    if (pnt2.direction(pnt3) == Point.reverse[edir] && !pnt2.equals(pnt3)) {
        var pnt3a = pnt3.move(PM.GRID_SIZE, Point.rightturn[edir]);
        pnt2 = new Point(pnt3a.x, pnt.y);
        // B
        eBuf[pCnt++] = pnt2;
        // C
        eBuf[pCnt++] = pnt3a;
        // D2
        eBuf[pCnt++] = pnt3;
    } else {
         // B
        eBuf[pCnt++] = pnt2;
         // C
        eBuf[pCnt++] = pnt3;
    }
    
    eBuf[pCnt++] = ep;
    
    // compress line points
    var cCnt = 1;
    var lDir = sdir;
    var lPnt = eBuf[0];
    var cBuf = [sp];
    for (var ii = 1; ii < pCnt; ii++) {
    	pnt = eBuf[ii];
    	if (!lPnt.equals(pnt)) {
    		var dir = lPnt.direction(pnt);
    		if (dir != lDir) {
    			lDir = dir;
    			cBuf[cCnt++] = lPnt;
    		}
    		lPnt = pnt;
    	}
    }
    cBuf[cCnt++] = ep.clone();
    return cBuf;
};



PmPath.prototype.quickSolve = function() {

    // attempt solve with existing edges
    var sp = this.startHook.pnt;
    var ep = this.endHook.pnt;

    // stickiness tuned at 4 edgecount
    if (!this.dirChanged && this.edgeCount > 0 && this.edgeCount < 4) {
        if (!this.lastEp.equals(ep)) {

            // squeeze/stretch the last 2 edges
            var cDir = ep.direction(this.lastEp);
            this.lastEp = ep.clone();
            var cVert = (cDir == Point.NORTH || cDir == Point.SOUTH);
            if (this.lastEdge.isVertical == cVert || this.lastEdge.prevNode != null) {
                var ar = this.lastEdge.alignEnd(ep);
                if (ar == 0) return this.needFull;
                if (ar == 1 && this.lastEdge.prevNode != null)
                    if (this.lastEdge.prevNode.alignEnd(this.lastEdge.p1) == 0)
                        return this.needFull;
            }

        } else if (!this.lastSp.equals(sp)) {

            // squeeze/stretch the first 2 edges
            var cDir = sp.direction(this.lastSp);
            this.lastSp = sp.clone();
            var cVert = (cDir == Point.NORTH || cDir == Point.SOUTH);
            if (this.startEdge.isVertical == cVert || this.startEdge.nextNode != null) {
                var ar = this.startEdge.alignStart(sp);
                if (ar == 0) return this.needFull;
                if (ar == 1 && this.startEdge.nextNode != null)
                    if (this.startEdge.nextNode.alignStart(this.startEdge.p2) == 0)
                        return this.needFull;
            }

        } else {
            return this.needFull;
        }
    } else if (!this.dirChanged && this.edgeCount > 0 && this.lastEp.equals(ep) && this.lastSp.equals(sp)) {
        return this.needFull;
    }
    this.dirChanged = false;
    this.needFull = true;
    this.lastSp = sp.clone();
    this.lastEp = ep.clone();

    this.clearEdges();
    var eBuf = PmPath.pointSolve(sp, this.startHook.direction, ep, this.endHook.direction);
    if (eBuf.length > 1) {
    	for (var ii=1; ii < eBuf.length; ii++) {
    		this.addEdge(eBuf[ii-1].clone(), eBuf[ii]);
    	}
    } else {
    	throw "Not enough edges";
    }
    
    return true;
};

PmPath.prototype.fullSolve = function() {
    // @todo reimplement
    if (this.quickSolve()) this.autoPositionLabel();
    this.needFull = false;
    this.updateModel();
};

PmPath.prototype.updateModel = function() {
    var edge = this.startEdge;
    var points = edge.p1.toString() + "-" + edge.p2.toString();
    edge = edge.nextNode;
    while (edge != null) {
        points += "-" + edge.p2.toString();
        edge = edge.nextNode;
    }
    this.xNode.setAttribute("points", points);
};

PmPath.prototype.limitHook = function(pnt, dir) { };

PmPath.prototype.divide = function(pnt, dir) {
    var xpNode = this.model.xfMod.duplicateNode("instance('pal')/path", "steps");
    var id  = this.model.idCtx.uniqueVar(
        this.constructor === PmJoin ? "pth_" + this.getId() : this.getId());
    xpNode.setAttribute("id", id);
    var path = new PmPath(this.model, xpNode);
    var p1 = pnt.move(PM.GRID_SIZE, Point.reverse[dir]);
    var p2 = pnt.move(PM.GRID_SIZE, dir);
    path.setStartHook(p2, dir);
    path.setEndHook(this.endHook.pnt.clone(), Point.reverse[this.endHook.direction]);
    this.endHook.setPoint(p1, dir);
    return path;
};

PmPath.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/path", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("path"));
    if (PmPath.initNewCustom != null) {
        PmPath.initNewCustom(xNode, arg, canvas);
    }
    var path = new PmPath(canvas.model, xNode);
    //model.tempPath = path;
    //var pnt = canvas.getMousePoint(evt);
    path.setStartHook(pnt, Point.EAST);
    path.setEndHook(new Point(pnt.x + 30, pnt.y + 20), Point.WEST);
    path.fullSolve();
    //path.setFrom(step, true);
    //step.snapHook(path.startHook);
    //canvas.startDrag(evt, path.endHook.element);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(path);
    return path;
};

PmPath.prototype.showProperties = function(evt) {
    if (this._diag != null && this._diag.isShowing()) return;
    this._diag = xfTemplateDialog(
    	"Path Properties", PM.CLOSE_PROPS, document.body, this.model.xfrm, "path", this.xNode, false, null, App ? App.chromeHelp : null);
    this._diag.xfTmpl.addEventListener("xforms-value-changed", this);
    this._diag.show(getMouseX(evt) + 25, getMouseY(evt) + 25);
};

PmPath.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-value-changed") {
        this.setId(this.xNode.getAttribute("id"));
        this.renderUpdate();
    }
};

PmPath.prototype.renderUpdate = function() {
    var labNd = Xml.matchOne(this.xNode, "label");
    var lTxt = labNd != null ? Xml.stringForNode(labNd) : "";
    if (lTxt != "") {
        if (this.label == null) {
            this.label = new PmPathLabel(this.model, labNd, this);
            this.autoPositionLabel();
        } else {
            this.label.setNode(labNd);
        }
    } else if (this.label != null) {
        this.label.remove();
    } /*else if (labNd != null) {
        labNd.parentNode.removeChild(labNd);
    }*/
};

PmPath.mapPathTo = function (pNode, origIdx) {
	var to = pNode.getAttribute("to");
   	if (to != "" && to != null) {
   		 to = origIdx[to];
   		 pNode.setAttribute("to", to);
   	}
};

PmPath.rxPnt = new RegExp("(-?[0-9]+,-?[0-9]+)-?", "g");

PmPath.parsePath = function(pObj, uniqIds, origIdx) {
	var model = pObj.model;
    pth = pObj.xNode;
    var ptStr = pth.getAttribute("points");
    PmPath.rxPnt.lastIndex = 0;
    var pm = ptStr ? PmPath.rxPnt.exec(ptStr) : null;
    if (!pm) {
        // auto draw
        pObj.setStartHook(new Point(10,10), Point.EAST);
        pObj.setEndHook(new Point(20,10), Point.EAST);
        if (pObj.fromObj) pObj.fromObj.snapHook(pObj.startHook);
		if (uniqIds) PmPath.mapPathTo(pth, origIdx);
        var to = pth.getAttribute("to");
        if (to != "" && to != null) {
            var toObj = model.steps[to];
            pObj.setTo(toObj);
            if (toObj != null) toObj.snapHook(pObj.endHook);
        }
        pObj.fullSolve();

    } else {
    	
    	var pnts = [];
    	do {
    		pnts.push(pm[1]);
    		pm = PmPath.rxPnt.exec(ptStr);
    	} while (pm);
    	
        
        if (pObj.constructor != PmJoin) {
            pObj.setStartHook(Point.decode(pnts[0]),
                Point.dirCodes[pth.getAttribute("startDir")]);
        }
        pObj.setEndHook(Point.decode(pnts[pnts.length - 1]),
                Point.dirCodes[pth.getAttribute("endDir")]);
        var to = pth.getAttribute("to");
        if (to != "" && to != null) {
            var toId = pth.getAttribute("to");
            if (uniqIds && origIdx[toId]) toId = origIdx[toId];
            pObj.setTo(toId ? model.steps[toId] : null);
        }
        for (jj = 0; jj < pnts.length - 1; jj++) {
            pObj.addEdge(Point.decode(pnts[jj]), Point.decode(pnts[jj+1]));
        }
    }
};


/**
 * PmPathLabel
 */
function PmPathLabel(model, xNode, path) {
    this.model = model;
    this.xNode = xNode;
    this.path = path;
    this.element = makeElement(
        model.canvas, "div", "pmPathLabel", Xml.stringForNode(this.xNode));
    model.dndCanvas.makeDraggable(this.element, PmObjDNDType.TYPE);
    domSetCss(this.element, xNode.getAttribute("style"));
    this.element.peer = this;
}

PmPathLabel.prototype.setNode = function(xNode) {
    this.xNode = xNode;
    setElementText(this.element, Xml.stringForNode(this.xNode));
};

PmPathLabel.prototype.updateModel = function() {
    var rect = this.path.model.dndCanvas.getBounds(this.element);
    this.xNode.setAttribute("style", "left:" + rect.x + "px;top:" + rect.y + "px");
};

PmPathLabel.prototype.setDragPosition = function(x, y, asGroup) {
    var style = this.element.style;
    style.left = (this.dndOffX + x) + "px";
    style.top = (this.dndOffY + y) + "px";
};

PmPathLabel.prototype.noTargetDrop = function() {
    this.updateModel();
};

PmPathLabel.prototype.groupDrop = function() {
    this.updateModel();
};

PmPathLabel.prototype.calcOffset = function(x, y) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.dndOffX = PM.SNAP(rect.x - x);
    this.dndOffY = PM.SNAP(rect.y - y);
};

PmPathLabel.prototype.dispose = function() {
    if (this.model == null) return;
    this.model.dndCanvas.disposeDraggable(this.element);
    this.element.peer = null;
    this.element = null;
    this.xNode = null;
    this.model = null;
    this.path = null;
};

PmPathLabel.prototype.remove = function() {
    this.path.label = null;
    this.model.canvas.removeChild(this.element);
    //this.xNode.parentNode.removeChild(this.xNode);
    this.dispose();
};


/**
 * PmLine
 */
PmLine.prototype = new PmPath();
PmLine.prototype.constructor = PmLine;

function PmLine(model, xNode) {
    PmPath.apply(this, [model, xNode]);
    this.vlineClass = "pmPthV pmPthVnot";
	this.hlineClass = "pmPthH pmPthHnot";
}

PmLine.prototype.cloneNode = function(asGroup) {
	var cn = Xml.cloneNode(this.xNode, true);
	// move to model node just incase
	var parNode = this.model.modelNode;
	if (parNode === this.model.modelRoot) {
		parNode = Xml.matchOne(this.model.modelRoot.parentNode, "notes"); 
	}
	parNode.appendChild(cn);
	return cn;
};

PmLine.initNew = function(pnt, canvas, arg) {
	var xNode;
   	if (canvas.model.modelNode === canvas.model.modelRoot) {
   		xNode = canvas.model.xfMod.duplicateNode("instance('pal')/line", "notes");
   	} else {
    	xNode = canvas.model.xfMod.duplicateNode(
    		"instance('pal')/line", ".", null, canvas.model.modelNode);
   	}
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("line"));
    if (PmLine.initNewCustom != null) {
        PmLine.initNewCustom(xNode, arg, canvas);
    }
    var path = new PmLine(canvas.model, xNode);
    path.setStartHook(pnt, Point.EAST);
    path.setEndHook(new Point(pnt.x + 30, pnt.y + 20), Point.WEST);
    path.fullSolve();
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(path);
    return path;
};

/**
 * PmJoin
 */
PmJoin.prototype = new PmPath();
PmJoin.prototype.constructor = PmJoin;

function PmJoin(model, xNode) {
    PmPath.apply(this, [model, xNode]);
    model.steps[this.getId()] = this;
}

PmJoin.prototype.setFrom = function(obj) {
    // skip
};

PmJoin.prototype.setId = function(id) {
	if (this.oId != id) {
	    delete this.model.steps[this.oId];
	    PmPath.prototype.setId.apply(this, [id]);
	    this.model.steps[id] = this;
	}
};

PmJoin.prototype.autoPositionLabel = function() {
    if (this.label != null) {
        var labElem = this.label.element;
        labElem.style.left = (this.startHook.pnt.x + 30) + "px";
        labElem.style.top = (this.startHook.pnt.y - 10) + "px";
        this.label.updateModel();
    }
};

PmJoin.prototype.remove = function() {
    this.xNode.parentNode.removeChild(this.xNode);
    PmPath.prototype.remove.apply(this, []);
};

PmJoin.prototype.limitHook = function(pnt, dir) {
    this.startHook.limitHook(pnt, dir);
};

PmJoin.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/join", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("join"));
    if (PmJoin.initNewCustom != null) {
        PmJoin.initNewCustom(xNode, arg, canvas);
    }
    var join = new PmJoin(canvas.model, xNode);
    join.setStartHook(pnt.clone(), PM.JOIN);
    join.setEndHook(new Point(pnt.x, pnt.y + 10), Point.NORTH);
    join.quickSolve();
    join.updateModel();
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(join);
    return join;
};



/**
 * PmJoinHook
 */
function PmJoinHook(model, path, pnt) {
    PmPathHook.apply(this, [model, path, true, pnt, Point.SOUTH]);
    this.element.className = "pmJoin";
    model.dndCanvas.makeDropTarget(this.element, PmObjDNDType.TYPE);
    this.toPaths = path.toPaths;
}

PmJoinHook.prototype = new PmPathHook();
PmJoinHook.prototype.constructor = PmJoinHook;

PmJoinHook.prototype.getId = function() {
    return this.path.getId();
};

PmJoinHook.prototype.draw = function () {
    var style = this.element.style;
    style.left = (PM.JOIN_CENTER_X + this.pnt.x) + "px";
    style.top = (PM.JOIN_CENTER_Y + this.pnt.y) + "px";
};

PmJoinHook.prototype.setDirection = function(dir) {
    // skip
};

PmJoinHook.prototype.calcOffset = function (x, y) {
    PmPathHook.prototype.calcOffset.apply(this, [x, y]);
    var ii;
    for (ii = 0; ii < this.toPaths.length; ii++) {
        this.toPaths[ii].endHook.calcOffset(x, y);
    }
};

PmJoinHook.prototype.noTargetDrop = function() {
    PmPathHook.prototype.noTargetDrop.apply(this, []);
    var i;
    for (i = 0; i < this.toPaths.length; i++) {
        this.toPaths[i].fullSolve();
    }
};

PmJoinHook.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor === PmPathHook) {
        if (dragObj.isStart || dragObj.path === this.path) {
            return false;
        }
        return true;
    }
    return false;
};

PmJoinHook.prototype.dropExec = function(dragObj) {
    if (dragObj.constructor === PmPathHook) {
        if (dragObj.isStart) {
            return;
        }
        dragObj.path.setTo(this, true);
        this.snapHook(dragObj);
        this.model.setChanged(dragObj.path);
        if (dragObj.path === this.model.tempPath) {
            this.model.canvasObj.setClickMode(null);
        }
    }
};

PmJoinHook.prototype.groupDrop = function() {
    if (!this.path.endHook._inDndGroup) {
        this.path.fullSolve();
    }
    var path;
    var i;
    for (i = 0; i < this.toPaths.length; i++) {
        path = this.toPaths[i];
        if (!path.endHook._inDndGroup) {
            path.fullSolve();
        }
    }
};

PmJoinHook.prototype.snapHook = function(hook) {
    PmStep.prototype.snapHook.apply(this, [hook, Point.SOUTH]);
};

PmJoinHook.prototype.limitHook = function (pnt, dir) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    rect.h = 10;
    if (dir == Point.NORTH || dir == Point.SOUTH) {
        if (pnt.x < rect.x) {
            pnt.x = PM.SNAP(rect.x);
        } else if (pnt.x > rect.x + rect.w) {
            pnt.x = PM.SNAP(rect.x + rect.w);
        }
    } else {
        if( pnt.y < rect.y) {
            pnt.y = PM.SNAP(rect.y);
        } else if (pnt.y > rect.y + rect.h) {
            pnt.y = PM.SNAP(rect.y + rect.h);
        }
    }
};

PmJoinHook.prototype.setDragPosition = function(x, y, asGroup) {
    var path;
    var pnt;
    var i;
    PmPathHook.prototype.setDragPosition.apply(this, [x, y, asGroup]);
    if (asGroup) {
        for (i = 0; i < this.toPaths.length; i++) {
            path = this.toPaths[i];
            if (!path.endHook._inDndGroup) {
                path.endHook.setDragPosition(x, y, false);
            }
        }
    } else {
        for (i = 0; i < this.toPaths.length; i++) {
            path = this.toPaths[i];
            path.endHook.setDragPosition(x, y, false);
        }
    }
};

PmJoinHook.prototype.limit = function(edge) {
    if (edge.path == this.path) {
        edge.p1.x = this.pnt.x;
        edge.p2.x = this.pnt.x;
    } else {
        PmPathHook.prototype.limit.apply(this, [edge]);
    }
};


PmStep.prototype = new XFDirectBinding();
PmStep.prototype.constructor = PmStep;

/**
 * Step - default is activity
 */
function PmStep(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "step";
    if (this.apptype) {
    	this.xfTmplName += "-" + this.apptype;
    }
}

PmStep.prototype.init = function(model, xNode) {
    this.model = model;
    this.xNode = xNode;
    this.fromPaths = [];
    this.toPaths = [];
    var id = xNode.getAttribute("id");
    this.oId = id;
    this.size = xNode.getAttribute("size");
    this.apptype = xNode.getAttribute("apptype");
    model.idCtx.addVar(id);
    this.render(model, xNode);
    this.element.peer = this;
    model.steps[id] = this;
    this.initBind(model.xfrm, xNode);
};

PmStep.prototype.render = function(model, xNode) {
    var name = Xml.getLocalName(xNode);
    if (this.apptype) name += "-" + this.apptype;
    this.element = makeElementNbSpd(model.canvas, "div", PM.STYLECLASSES[name] + (this.size ? this.size : ""));
    addEventHandler(this.element, "dblclick", PmStep.__onDblClick);
    model.dndCanvas.makeDraggable(this.element, PmObjDNDType.TYPE);
    model.dndCanvas.makeDropTarget(this.element, PmObjDNDType.TYPE);
    domSetCss(this.element, xNode.getAttribute("style"));

    if (name == "activity") {
        // TODO - counts
        //this.elem_txtCount = makeElement(this.element, "div", "txtCount", "0");
        this.elem_txtRole = makeElement(this.element, "div", "txtRole", this.getRole());
        this.elem_txtLabel = makeElement(this.element, "div", "txtLabel", this.getLabel());
    } else if (name == "start") {
        makeElement(this.element, "div", "text", this.getLabel());
        this.elem_deco = makeElementNbSpd(this.element, "div",  "deco " + model.getIcon() + "Ico");
    } else {
        makeElement(this.element, "div", "text", this.getLabel());
    }
    this.element.title = this.getDescription();
    makeElementNbSpd(this.element, "div", "pBox");
    this.initHover();
};


PmStep.prototype.initHover = function() {
	addEventHandler(this.element, "mouseover", PmStep.mouseOver);
	addEventHandler(this.element, "mouseout", PmStep.mouseOut);
};

PmStep.prototype.hoverOver = function(evt) {
	if (this.model.dndCanvas.isDragging() || this._inDndGroup || this.model.dndCanvas.disableDrag) return;
	if (this.constructor != PmSwitch && this.fromPaths.length > 0) return;
	this.hoverRecover();
	if (this.hovCHooks != null) return;
	
	// calc avail cardinals
	var cards = [Point.NORTH, Point.EAST, Point.SOUTH, Point.WEST];
	var ii;
	for (ii = 0; ii < this.toPaths.length && cards.length > 0; ii++) {
		arrayRemove(cards, Point.reverse[this.toPaths[ii].endHook.direction]);
	}
	for (ii = 0; ii < this.fromPaths.length && cards.length > 0; ii++) {
		arrayRemove(cards, this.fromPaths[ii].startHook.direction);
	}
	this.hovCHooks = this.renderHoverHooks(cards);
};

PmStep.prototype.renderHoverHooks = function(cards) {
	var hhooks = [];
	var rect = this.model.dndCanvas.getBounds(this.element);
	for (var ii = 0; ii < cards.length; ii++) {
		var hk = makeElementNbSpd(this.element.parentNode, "div", "pmHovHook", null, {title : "Drag new path"});
		hk.peer = "hovHook";
		hk.step = this;
		this.model.dndCanvas.makeDraggable(hk, PmObjDNDType.TYPE);
		addEventHandler(hk, "mouseover", PmStep.hkMouseOver);
		addEventHandler(hk, "mouseout", PmStep.hkMouseOut);		
		var pnt = new Point(rect.x, rect.y);
		switch (cards[ii]) {
			case Point.NORTH:
				pnt.x += (rect.w / 2) - 5; break;
			case Point.EAST:
				pnt.x += rect.w; 
				pnt.y += rect.h / 2; break;
			case Point.SOUTH:
				pnt.x += (rect.w / 2) - 5; 
				pnt.y += rect.h; break;
			case Point.WEST:
				pnt.y += rect.h / 2; break;
		}
		this.limitHook(pnt, cards[ii]);
		hk.style.top = (pnt.y + PM.HOOK_CENTER) + "px";
		hk.style.left = (pnt.x + PM.HOOK_CENTER) + "px";
		arrayAdd(hhooks, hk);
	}
	return hhooks;
};

PmStep.prototype.clearHoverHooks = function() {
	if (this.hovCHooks == null) return;
	for (var ii = 0; ii < this.hovCHooks.length; ii++) {
		var hk = this.hovCHooks[ii];
		this.model.dndCanvas.disposeDraggable(hk);
		hk.parentNode.removeChild(hk);
	}
	this.hovCHooks = null;
};

PmStep.prototype.hoverOut = function(evt) {
	this.hoverDecay();
};

PmStep.prototype.hoverDecay = function() {
	var holdThis = this;
	this.__hovDecTimer = window.setTimeout(function () { holdThis.clearHoverHooks(); }, 500);
};

PmStep.prototype.hoverRecover = function() {
	window.clearTimeout(this.__hovDecTimer);
    this.__hovDecTimer = null;
};

PmStep.prototype.dispose = function() {
    if (this.model == null) return;
    if (this._diag != null) this._diag.dispose();
    this.model.dndCanvas.disposeDropTarget(this.element);
    this.model.dndCanvas.disposeDraggable(this.element);
    this.element.peer = null;
    this.element = null;
    this.fromPaths = null;
    this.toPaths = null;
    this.model = null;
    this.xNode = null;
    XFDirectBinding.prototype.dispose.apply(this, []);
};

PmStep.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor === PmPathHook 
     		&& dragObj.path.constructor !== PmLine) {
        return true;
    }
    return false;
};

PmStep.prototype.dropExec = function(dragObj) {
    if (this.dropTest(dragObj)) {
    	var path = dragObj.path;
        if (dragObj.isStart) {
            path.setFrom(this, true);
        } else {
            path.setTo(this, true);
        }
        this.snapHook(dragObj);
       /* if (!dragObj.isStart && path.fromObj &&
        	path.fromObj.constructor != PmSwitch) {
        	path.fromObj.snapHook(path.startHook, null, 2);
        } */
        this.model.setChanged(path);
        if (path == this.model.tempPath) {
            this.model.tempPath = null;
            this.model.canvasObj.setClickMode(null);
        }
    }
};

PmStep.prototype.limitHook = function(pnt, dir, coreRect) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    coreRect = coreRect || rect;
    if (dir == Point.NORTH || dir == Point.SOUTH) {
    	var maxX = coreRect.x + coreRect.w;
        if (pnt.x < coreRect.x) {
            pnt.x = coreRect.x;
        } else if (pnt.x > maxX) {
            pnt.x = maxX;
        }
        var maxY = rect.y + rect.h - 3;
        if (pnt.y > maxY) pnt.y = maxY;
    } else {
    	var maxY = coreRect.y + coreRect.h;
        if (pnt.y < coreRect.y) pnt.y = coreRect.y;
        else if (pnt.y > maxY) pnt.y = maxY;
    }
};

PmStep.prototype.snapHook = function(hook, avoid, forceEdge) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    var coreRect = rect.clone().inflate(-15);
    var p = hook.pnt;
    var dir;
    
    // edge drop
    if (forceEdge != 2 && (forceEdge || !Rectangle.intersects(coreRect, p.toRect()))) {
    	
	    var dy = (rect.y + (rect.h / 2)) - p.y;
	    var dx = (rect.x + (rect.w / 2)) - p.x;
	    if (Math.abs(dx) > Math.abs(dy)) {
	        if ((dx < 0 && Point.EAST != avoid) || Point.WEST == avoid) {
	            dir = Point.EAST;
	            p.x = rect.x + rect.w;
	        } else {
	            dir = Point.WEST;
	            p.x = rect.x;
	        }
	    } else {
	        if ((dy < 0 && Point.SOUTH != avoid) || Point.NORTH == avoid) {
	            dir = Point.SOUTH;
	            p.y = rect.y + rect.h;
	        } else {
	            dir = Point.NORTH;
	            p.y = rect.y;
	        }
	    }

 	} else {

	    var path = hook.path;
	    var otherHook = path.startHook === hook ? path.endHook : path.startHook;
	    dir = p.direction(otherHook.pnt);
	    switch (dir) {
	    	case Point.NORTH: p.y = rect.y; break;
	    	case Point.EAST: p.x = rect.x + rect.w; break;
	    	case Point.SOUTH: p.y = rect.y + rect.h; break;
	    	case Point.WEST: p.x = rect.x; break;
	    }
	    p.align(otherHook.pnt, dir);
    }
    	
    p.x = PM.SNAP(p.x);
    p.y = PM.SNAP(p.y);
    this.limitHook(p, dir, coreRect);
    hook.setPoint(p, dir);
};

PmStep.prototype.setDragPosition = function(x, y, asGroup) {
    var path;
    var pnt;
    var i;
    if (asGroup) {
        for (i = 0; i < this.fromPaths.length; i++) {
            path = this.fromPaths[i];
            if (!path.startHook._inDndGroup) {
                path.startHook.setDragPosition(x, y, false);
            }
        }
        for (i = 0; i < this.toPaths.length; i++) {
            path = this.toPaths[i];
            if (!path.endHook._inDndGroup) {
                path.endHook.setDragPosition(x, y, false);
            }
        }
    } else {
        for (i = 0; i < this.fromPaths.length; i++) {
            path = this.fromPaths[i];
            path.startHook.setDragPosition(x, y, false);
        }
        for (i = 0; i < this.toPaths.length; i++) {
            path = this.toPaths[i];
            path.endHook.setDragPosition(x, y, false);
        }
    }
    //this.model.clearIfViewing(this);
    var style = this.element.style;
    style.left = (this.dndOffX + x) + "px";
    style.top = (this.dndOffY + y) + "px";
};

PmStep.prototype.noTargetDrop = function() {
    var i;
    for (i = 0; i < this.fromPaths.length; i++) {
        this.fromPaths[i].fullSolve();
    }
    for (i = 0; i < this.toPaths.length; i++) {
        this.toPaths[i].fullSolve();
    }
    this.updateModel();
};

PmStep.prototype.groupDrop = function() {
    var path;
    var i;
    for (i = 0; i < this.fromPaths.length; i++) {
        path = this.fromPaths[i];
        if (!path.startHook._inDndGroup) {
            path.fullSolve();
        }
    }
    for (i = 0; i < this.toPaths.length; i++) {
        path = this.toPaths[i];
        if (!path.endHook._inDndGroup) {
            path.fullSolve();
        }
    }
    this.updateModel();
};


PmStep.prototype.calcOffset = function(x, y) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.dndOffX = PM.SNAP(rect.x - x);
    this.dndOffY = PM.SNAP(rect.y - y);
    var ii;
    for (ii = 0; ii < this.fromPaths.length; ii++) {
        this.fromPaths[ii].startHook.calcOffset(x, y);
    }
    for (ii = 0; ii < this.toPaths.length; ii++) {
        this.toPaths[ii].endHook.calcOffset(x, y);
    }
};

PmStep.prototype.addPath = function(path, isNewOrMove) {
    arrayAdd(this.fromPaths, path);
    if (isNewOrMove && !Xml.isAncestor(this.xNode, path.xNode)) {
        Xml.insertChildBefore(this.xNode, path.xNode, ["attachment"]);
    }
    if (isNewOrMove) {
        this.model.xfMod.markChanged(this.xNode);
        this.model.xfMod.markChanged(path.xNode);
        this.model.xfMod.rebuild();
        path.renderUpdate();
    }
};

PmStep.prototype.removePath = function(path) {
   this.fromPaths = arrayRemoveStrict(this.fromPaths, path);
   this.xNode.removeChild(path.xNode);
   this.xNode.parentNode.appendChild(path.xNode);
   this.model.xfMod.markChanged(this.xNode.parentNode);
   this.model.xfMod.markChanged(this.xNode);
   this.model.xfMod.markChanged(path.xNode);
   this.model.xfMod.rebuild();
};

PmStep.prototype.clear = function() {
	if (this.model == null) return;
	//this.model.clearIfViewing(this);
    if (this._diag != null && this._diag.isShowing()) {
        this._diag.destroy();
    }
    this._diag = null;
    this.clearHoverHooks();
    this.model.canvas.removeChild(this.element);
};

PmStep.prototype.remove = function() {
   	this.clear();
    this.model.idCtx.removeVar(this.oId);
    if (this.constructor === PmGroup) {
    	var gid = this.getId();
    	this.model.idCtx.removeVar(gid);
    	delete this.model.steps[gid];
    }
    
    // heal path
    var healed = false;
    if (this.fromPaths.length == 1 && this.toPaths.length == 1) {
        var frPth = this.fromPaths[0];
        var toPth = this.toPaths[0];
        if (toPth.endHook.direction == frPth.startHook.direction) {
            toPth.endHook.setPoint(frPth.endHook.pnt, Point.reverse[frPth.endHook.direction]);
            toPth.setTo(frPth.toObj, true);
            frPth.remove();
            healed = true;
        }
    }
    if (!healed) {
        while (this.fromPaths.length > 0) {
            this.fromPaths[0].setFrom(null, true);
        }
        while (this.toPaths.length > 0) {
            this.toPaths[0].setTo(null, true);
        }
    }
    delete this.model.steps[this.oId];
    this.model.setStepRemoved(this.xNode);
    this.destroyNode();
    this.model.setChanged();
    this.model.xfMod.rebuild();
    XFDirectBinding.prototype.remove.apply(this, []);
};

PmStep.prototype.setId = function(id) {
    if (this.oId != id) {
	    delete this.model.steps[this.oId];
		this.model.idCtx.removeVar(this.oId);
		this.model.idCtx.addVar(id);
		PmStep.changeNodeId(this.xNode, this.oId);
	    this.xNode.setAttribute("id", id);
	    this.model.steps[id] = this;
	    this.oId = id;
	    var tid = this.getId();
	    for (var ii = 0; ii < this.toPaths.length; ii++) {
	        this.toPaths[ii].xNode.setAttribute("to", tid);
	    }
    }
};


PmStep.changeNodeId = function(xNode, oid) {
	var oldid = xNode.getAttribute("oldid");
	if (!oldid) {
		xNode.setAttribute("oldid", oid);
	}
};


PmStep.prototype.getId = function() {
    return this.oId;
};

PmStep.prototype.getLabel = function() {
    return Uri.name(this.oId);
};

PmStep.prototype.getRole = function() {
    return this.xNode.getAttribute("role");
};

PmStep.prototype.getDescription = function() {
    var nd = Xml.matchOne(this.xNode, "description");
    return nd != null ? Xml.stringForNode(nd) : "";
};

PmStep.prototype.updateModel = function() {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.xNode.setAttribute("style",
        "left:" + rect.x + "px;top:" + rect.y + "px");
    this.model.xfMod.markChanged(this.xNode, "style");
    this.model.xfMod.recalculate();
};

PmStep.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-value-changed") {
        this.setId(this.xNode.getAttribute("id"));
        this.renderUpdate();
        this.model.setChanged(this);
    }
};

PmStep.prototype.renderUpdate = function() {
    var name = Xml.getLocalName(this.xNode);
    if (name == "activity" && !this.apptype) {
        setElementText(this.elem_txtRole, this.getRole());
        setElementText(this.elem_txtLabel, this.getLabel());
        if (this.size != this.xNode.getAttribute("size")) {
        	this.size = this.xNode.getAttribute("size");
        	this.element.className = PM.STYLECLASSES[name] + (this.size ? this.size : "");
        	var holdThis = this;
        	window.setTimeout(function() {holdThis.snapAllHooks()}, 10);
        }
    } else if (name == "start") {
        if (this.elem_deco) this.elem_deco.className = "deco " + this.model.getIcon() + "Ico";
        setElementText(getFirstChildElement(this.element), this.getLabel());
    } else {
        setElementText(getFirstChildElement(this.element), this.getLabel());
    }
    this.element.title = this.getDescription();
};

PmStep.prototype.snapAllHooks = function() {
	var ii;
	for (ii = 0; ii < this.toPaths.length; ii++) {
		this.snapHook(this.toPaths[ii].endHook, null, 1);
	}
	for (ii = 0; ii < this.fromPaths.length; ii++) {
		this.snapHook(this.fromPaths[ii].startHook, null, 1);
	}
};

PmStep.prototype.showProperties = function(evt) {
    if (this._diag != null && this._diag.isShowing()) return;
    if (!this.size) this.xNode.setAttribute("size", "");
    this._diag = xfTemplateDialog(
			{ step : "Step", "step-launch" : "Sub Process", event : "Timer",  "switch" : "Switch",
				start : "Start", end : "End", group : "Group", enter : "Enter", exit : "Exit"}[this.xfTmplName] +
    		" Properties", PM.CLOSE_PROPS, document.body, this.model.xfrm, this.xfTmplName, this.xNode, false, null, App ? App.chromeHelp : null);
    this._diag.xfTmpl.addEventListener("xforms-value-changed", this);
    this._diag.show(getMouseX(evt) + 25, getMouseY(evt) + 25);
};

PmStep.prototype.cloneNode = function(asGroup) {
	var cn = Xml.cloneNode(this.xNode, true);
	this.xNode.parentNode.appendChild(cn);
	if (asGroup) {
		var paths = Xml.match(cn, "path");
		// remove path nodes that aren't grouped
		for (var ii = 0; ii < paths.length; ii++) {
			var pn = paths[ii];
			var pthObj = this.model.getPathById(pn.getAttribute("id"));
			if (!pthObj || !pthObj.anyInGroup()) {
				cn.removeChild(pn);
			}
		}
	}
	return cn;
};

PmStep.prototype.canRemove = function() {
	return true;
};

PmStep.prototype.pathNodes = function() {
	return Xml.match(this.xNode, "path");
};

PmStep.prototype.createPathTo = function(step) {
	var rect = this.model.dndCanvas.getBounds(this.element);
	var pnt = new Point(rect.x + (rect.w / 2), rect.y + (rect.y / 2));
	var path = PmPath.initNew(pnt, this.model.dndCanvas);
	path.setFrom(this, true);
    this.snapHook(path.startHook);
    path.setTo(step, true);
};

PmStep.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/activity", ".", null, canvas.model.modelNode);
    
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (arg == "launch") {
    	xNode.setAttribute("apptype", "launch");
    	xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Sub Process"));
    } else {
    	xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Step"));
    }

    if (PmStep.initNewCutom != null) {
        PmStep.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmStep(canvas.model, xNode);
    canvas.model.setChanged(step);
    canvas.model.xfMod.rebuild();
    return step;
};

PmStep.mouseOver = function(evt) {
	var step = this.peer;
	if (step) step.hoverOver(evt);
};

PmStep.mouseOut = function(evt) {
	var step = this.peer;
	if (step) step.hoverOut(evt);
};

PmStep.hkMouseOver = function(evt) {
	var step = this.step;
	if (step) step.hoverRecover();
};

PmStep.hkMouseOut = function(evt) {
	var step = this.step;
	if (step) step.hoverDecay();
};

PmStep.__onDblClick = function(evt) {
	if (this.peer) {
		var dndCanv = this.peer.model.dndCanvas;
		if (dndCanv.isDragging() || dndCanv.disableDrag) return;
		
		if (this.peer.constructor === PmGroup) this.peer.viewGroup();		
		else this.peer.showProperties(evt);
	}
};

PmStep.getBounds = function(xNode) {
	
	var styStr = xNode.getAttribute("style");
	var x=0, y=0, w=0, h=0;

	if (styStr) {
		var styObj = styleString2Obj(styStr);
		x = parseInt(styObj.left);
		y = parseInt(styObj.top);
	}
	
	switch (Xml.getLocalName(xNode)) {
		case "end":
		case "exit":
		case "start":
		case "enter":
		case "timer":
			w = 50; h = 50;
			break;
			
		case "switch":
			w = 120; h = 80;
			break;
		
		case "group":
			w = 100; h = 70;
			break;
			
		case "activity":
			var size = xNode.getAttribute("size");
			
			switch (xNode.getAttribute("size")) {
				case "L": w = 160; h = 90; break;
				case "M": w = 130; h = 75; break;
				default: w = 110; h = 60; break;
			}
			break;
	}
	return new Rectangle(x,y,w,h);
};

/*
 * Custom initNew stub
 */
PmStep.initNewCustom = null; // function(xNode, arg, canvas)


PmNote.prototype = new XFDirectBinding();
PmNote.prototype.constructor = PmNote;

/**
 * Note
 */
function PmNote(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
}

PmNote.prototype.init = function(model, xNode) {
    this.model = model;
    this.xNode = xNode;
    var id = xNode.getAttribute("id");
    this.oId = id;
    this.model.idCtx.addVar(id);
    this.element = makeElementNbSpd(this.model.canvas, "div", "pmNote");
    model.dndCanvas.makeDraggable(this.element, PmObjDNDType.TYPE);
    this.element.peer = this;
    makeElementNbSpd(this.element, "div", "corner");
    makeElement(this.element, "div", "text", this.getText());
    addEventHandler(this.element, "dblclick", PmStep.__onDblClick);
    var holdThis = this;
    domSetCss(holdThis.element, holdThis.xNode.getAttribute("style"));
    arrayAdd(this.model.notes, this);
    this.initBind(model.xfrm, xNode);
    xNode =  null;
};

PmNote.prototype.dispose = function() {
    if (this.model == null) return;
    if (this._diag != null) this._diag.dispose();
    this.model.dndCanvas.disposeDraggable(this.element);
    this.element.peer = null;
    this.element = null;
    this.xNode = null;
    this.model = null;
    XFDirectBinding.prototype.dispose.apply(this, []);
};

PmNote.prototype.getId = function() {
    return this.oId;
};

PmNote.prototype.setId = function(id) {
	if (this.oId != id) {
		this.model.idCtx.removeVar(this.oId);
		this.model.idCtx.addVar(id);
    	this.xNode.setAttribute("id", id);
	}
};

PmNote.prototype.getText = function() {
    return Xml.stringForNode(this.xNode);
};

PmNote.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-value-changed") {
        this.setId(this.xNode.getAttribute("id"));
        this.renderUpdate();
    }
};

PmNote.prototype.renderUpdate = function() {
    var corner = getFirstChildElement(this.element);
    var txt = corner.nextSibling;
    setElementText(txt, Xml.stringForNode(this.xNode));
};

PmNote.prototype.showProperties = function(evt) {
    if (this._diag != null && this._diag.isShowing()) return;
    this._diag = xfTemplateDialog(
    "Note Properties", PM.CLOSE_PROPS, document.body, this.model.xfrm, "note", this.xNode, false, null, App ? App.chromeHelp : null);
    this._diag.xfTmpl.addEventListener("xforms-value-changed", this);
    this._diag.show(getMouseX(evt) + 25, getMouseY(evt) + 25);
};

PmNote.prototype.updateModel = function() {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.xNode.setAttribute("style",
            "left:" + rect.x + "px;top:" + rect.y + "px");
};

PmNote.prototype.setDragPosition = function(x, y, asGroup) {
    var style = this.element.style;
    style.left = (this.dndOffX + x) + "px";
    style.top = (this.dndOffY + y) + "px";
};

PmNote.prototype.noTargetDrop = function() {
    this.updateModel();
};

PmNote.prototype.groupDrop = function() {
    this.updateModel();
};

PmNote.prototype.calcOffset = function(x, y) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    this.dndOffX = PM.SNAP(rect.x - x);
    this.dndOffY = PM.SNAP(rect.y - y);
};

PmNote.prototype.clear = function() {
	if (this._diag != null && this._diag.isShowing()) {
        this._diag.destroy();
    }
    this.model.canvas.removeChild(this.element);
};

PmNote.prototype.remove = function() {
   	this.clear();
    this.model.idCtx.removeVar(this.getId());
    this.destroyNode();
    this.model.setChanged();
    this.model.xfMod.rebuild();
    arrayRemove(this.model.notes, this);
    XFDirectBinding.prototype.remove.apply(this, []);
};

PmNote.prototype.cloneNode = function(asGroup) {
	var cn = Xml.cloneNode(this.xNode, true);
	// move to model node just incase
	var parNode = this.model.modelNode;
	if (parNode === this.model.modelRoot) {
		parNode = Xml.matchOne(this.model.modelRoot.parentNode, "notes"); 
	}
	parNode.appendChild(cn);
	return cn;
};

PmNote.prototype.canRemove = function() {
	return true;
};

PmNote.initNew = function(pnt, canvas, arg) {
    var xNode;
   	if (canvas.model.modelNode === canvas.model.modelRoot) {
   		xNode = canvas.model.xfMod.duplicateNode("instance('pal')/note", "notes");
   	} else {
    	xNode = canvas.model.xfMod.duplicateNode(
    		"instance('pal')/note", ".", null, canvas.model.modelNode);
   	}
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Note"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (PmNote.initNewCustom != null) {
        PmNote.initNewCustom(xNode, arg, canvas);
    }
    var note = new PmNote(canvas.model, xNode);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(note);
    return note;
};


/**
 * Model
 */
function PmModel(canvas, uri, xform) {
    this.paths = [];
    this.steps = new Object();
    this.notes = [];
    this.tempPath = null;
    this.changed = false;
    this.xfrm = xform;
    this.xfMod = xform.getDefaultModel();
    this.xfMod.addEventListener("xforms-submit-done", this);
    this.pmDocument = null;
    this.uri = uri;
    this.user = "";
    this.canvasObj = canvas;
    this.canvas = canvas.element;
    this.dndCanvas = canvas.dndCanvas;
    this.dndCanvas.model = this;
    this.editControl = false;
    this.previewing = null;
    this.idCtx = new XmlId();
    this.trace = [];
   // this.xb = new XMLBuilder();
    this.dndCanvas.addDNDType(new PmObjDNDType(), PmObjDNDType.TYPE);
    this.load(uri);
}

PmModel.prototype.setChanged = function(obj) {
    this.changed = true;
};

PmModel.prototype.clearChanged = function() {
    this.changed = false;
};

PmModel.prototype.getPathById = function(id) {
    for (var ii = 0; ii < this.paths.length; ii++) {
        var pth = this.paths[ii];
        if (pth.getId() == id) return pth;
    }
    return null;
};

/**
 * Careful this will dive into groups
 */
PmModel.prototype.getStepById = function(id) {
	var parId = Uri.parent(id);
	if (parId != this.idCtx.prefix) {
		if (parId == "") {
			this.setModelNode(this.modelRoot);
		} else {
			var gpNode = Xml.matchOneDeep(this.modelRoot, "group", "id", parId);
			if (gpNode) {
				this.setModelNode(gpNode);
			} else {
				return null;
			}
		}
	}
    return this.steps[id];
};

PmModel.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-submit-done") {
        this.clearChanged();
    }
};

PmModel.prototype.clearFocus = function() {
    if (this.focusBox) {
        this.focusBox.parentNode.removeChild(this.focusBox);
        this.focusBox = null;
    }
};

PmModel.prototype.focusStep = function(step) {
    this.clearFocus();
    var rect = this.dndCanvas.getBounds(step.element);
    this.dndCanvas.scrollToView(step.element);
    this.focusBox = makeElement(this.canvas, "div", "focusBox");
    rect.x--;
    rect.y--;
    drawBox(this.focusBox, rect, "focus");
};

PmModel.prototype.load = function() {
    var node = this.xfrm.getDefaultModel().getDefaultInstance();
    this.pmDocument = node.ownerDocument;
    this.digest(node);
};

PmModel.prototype.digestStepList = function(sNodes, uniqIds) {
 	var paths = [];
 	var sObjs = [];
 	var origIdx = uniqIds ? new Object() : null;
    var elem, obj, id, pObj, pth, pnts;
    var sType;
    var ii, jj;
    for (ii = 0; ii < sNodes.length; ii++) {
        var stp = sNodes[ii];
        if (stp.nodeType != 1) continue;
        sType = Xml.getLocalName(stp);
        if (uniqIds) {
        	id = stp.getAttribute("id");
        	if (id) {
        		var uid = this.idCtx.uniqueVar(id);
        		origIdx[id] = uid;
        		origIdx[Uri.absolute(id,"$$S")] = Uri.absolute(uid,"$$S");
        		PmStep.changeNodeId(stp, id);
        		this.xfMod.setElementAttribute(stp, "id", uid);
        		if (sType == "group") {
        			origIdx[Uri.absolute(id,"$$S")] = Uri.absolute(uid,"$$S");
        			if (uid != id) PmGroup.changeId(stp, id, uid, this.xfMod);
        		}
        	}
        }

        if (sType == "join") {
            pObj = new PmJoin(this, stp);
            pnts = stp.getAttribute("points").split("-");
            pObj.setStartHook(Point.decode(pnts[0]), PM.JOIN); // for peer
            arrayAdd(paths, pObj);
        } else {

            switch (sType) {
            	case "activity": obj = new PmStep(this, stp); break;
                case "start": obj = new PmStart(this, stp); break;
                case "end": obj = new PmEnd(this, stp); break;
                case "timer": obj = new PmTimer(this, stp); break;
                case "switch": obj = new PmSwitch(this, stp); break;
                case "group": obj = new PmGroup(this, stp); break;
                case "enter": obj = new PmEnter(this, stp); break;
                case "exit": obj = new PmExit(this, stp); break;
                case "path": arrayAdd(paths, new PmPath(this, stp)); continue;
                case "line": arrayAdd(paths, new PmLine(this, stp)); continue;
                case "note": arrayAdd(sObjs, new PmNote(this, stp));  continue;
                default: continue;
            }
            var pthKids = obj.pathNodes();
            for (jj = 0; jj < pthKids.length; jj++) {
                pth = pthKids[jj];
                if (uniqIds) {
		        	id = pth.getAttribute("id");
		        	if (id) {
		        		var uid = this.idCtx.uniqueVar(id);
		        		origIdx[id] = uid;
		        		pth.setAttribute("id", uid);
		        	}
		        }
                pObj = new PmPath(this, pth);
                pObj.setFrom(obj);
                arrayAdd(paths, pObj);
            }
        }
        arrayAdd(sObjs, obj);
    }

    for (ii = 0; ii < paths.length; ii++) {
        pObj = paths[ii];
        arrayAdd(sObjs, pObj);
        PmPath.parsePath(pObj, uniqIds, origIdx);
    }
    this.__lastOrigIdx = origIdx;
	return sObjs;
};

PmModel.prototype.digest = function(node) {
	var ii;
    this.steps = new Object();
    this.notes = [];
    this.paths = [];

    var sequence = Xml.matchOne(node, "steps");
    if (!sequence) return;
    this.modelRoot = this.modelNode = sequence;
    
    // clear oldids for a fresh start
    var oldids = Xml.matchDeep(sequence, "*", "oldid");
    for (ii = 0; ii < oldids.length; ii++) {
    	oldids[ii].setAttribute("oldid", "");
    }
    
    var rems = Xml.match(node, "removed");
    for (ii = 0; ii < rems.length; ii++) {
    	node.removeChild(rems[ii]);
    }
    
    var kids = sequence.childNodes;
   	this.digestStepList(kids, false);
    var notes = Xml.matchOne(node, "notes");
    kids = notes.childNodes;
    for (ii = 0; ii < kids.length; ii++) {
        var note = kids[ii];
        if (note.nodeType != 1) continue;
        switch (Xml.getLocalName(note)) {
        	case "line":
        		obj = new PmLine(this, note);
        		PmPath.parsePath(obj);
        		break;
        	default:
        		obj = new PmNote(this, note);
        		break;
        } 
    }
};

PmModel.prototype.getIcon = function() {
    return this.xfMod.getValue("iw:type/@icon");
};

PmModel.prototype.clearToken = function() {
    if (this.tokElem != null) {
        this.tokElem.parentNode.removeChild(this.tokElem);
        this.tokElem = null;
    }
};

PmModel.prototype.showToken = function(step) {
    if (this.tokElem == null) {
        this.tokElem = makeElement(this.canvas, "div", "token " + this.getIcon() + "Ico");
        makeElement(this.tokElem, "div", "icon " + this.getIcon() + "Ico");
    }
    var rect = this.dndCanvas.getBounds(step.element);
    this.tokElem.style.top = (rect.y - 10) + "px";
    this.tokElem.style.left = (rect.x - 20) + "px";
};

PmModel.prototype.clearTrace = function() {
	var tr = this.trace;
	this.trace = [];
	for (var ii = 0; ii < tr.length; ii++) {
		tr[ii].setTrace(false);
	}
};


PmModel.prototype.addTrace = function(pmObj) {
	if (pmObj.constructor === PmPath) {
		this.trace.push(pmObj);
		pmObj.setTrace(true);
	}
};

PmModel.prototype.removeSelected = function() {
	var hElems = this.dndCanvas.getGroupElements();
	var remPaths = [];
	for (var ii = 0; ii < hElems.length; ii++) {
		var peer = hElems[ii].peer;
		if (peer) {
			if (!peer.path) {
				if (peer.canRemove())
					peer.remove();
			} else if (arrayFindStrict(remPaths, peer.path) < 0) {
				peer.path.remove();
				arrayAdd(remPaths, peer.path);
			}
		}
	}
};

PmModel.prototype.dndGroupList = function(sObjs, posX, posY) {
	// clear group
	var group = this.dndCanvas.__groupType;
	group.clear();
	
 	// add to group
	for (var ii = 0; ii < sObjs.length; ii++) {
		var obj = sObjs[ii];
		this.setChanged(obj);
		if (obj instanceof PmStep || obj instanceof PmNote) {
			group.add(obj.element);
		} else { // path
			if (obj.startHook != null) group.add(obj.startHook.element);
			if (obj.endHook != null) group.add(obj.endHook.element);
			if (obj.label != null) group.add(obj.label.element);
			var edge = obj.startEdge;
		    while (edge != null) {
		       	group.add(edge.element);
		        edge = edge.nextNode;
		    }
		}
	}
	
	// move group
	group.startDrag(0,0);
	group.dragMove(posX, posY);
	group.noTargetDrop();
};

PmModel.prototype.duplicateSelected = function(pnt) {
	var hElems = this.dndCanvas.getGroupElements();
	var dupSNodes = [], lineDups = [];
	for (var ii = 0; ii < hElems.length; ii++) {
		var peer = hElems[ii].peer;
		var cn;
		if (peer) {
			if (!peer.path) {
				cn = peer.cloneNode(true);
				if (cn) {
					arrayAdd(dupSNodes, cn);
					cn.setAttribute("oldid", "//N");
				}
			} else if (peer.path.constructor === PmLine) {
				if (arrayFindStrict(lineDups, peer.path) < 0) {
					arrayAdd(lineDups, peer.path);
					cn = peer.path.cloneNode(true);
					if (cn) arrayAdd(dupSNodes, cn);
				}
			}
		}
	}
	var sObjs = this.digestStepList(dupSNodes, true);
	this.dndGroupList(sObjs, 50, 50);
	this.xfMod.rebuild();
};

PmModel.prototype.groupSelected = function(id) {
	if (!id) return;
	id = id.replace('/', '\\');
	
	// clear group
	var group = this.dndCanvas.__groupType;
	var rect = group.getBounds(this.dndCanvas);
	var uid = this.idCtx.uniqueVar(id);
	var xNode = this.xfMod.duplicateNode("instance('pal')/group", ".", null, this.modelNode);
    xNode.setAttribute("id", uid);
    xNode.setAttribute("style", "left:" + rect.x + "px;top:" + rect.y + "px");
	
	// update enter/exit ids
	var en = Xml.matchOne(xNode, "enter");
	en.setAttribute("id", Uri.absolute(uid, "$$S"));
	var pth = Xml.matchOne(en, "path");
	pth.setAttribute("id", this.idCtx.uniqueVar(pth.getAttribute("id")));
	pth.setAttribute("to", Uri.absolute(uid, "$$E"));
	Xml.matchOne(xNode, "exit").setAttribute("id", Uri.absolute(uid, "$$E"));
	
	var hElems = this.dndCanvas.getGroupElements();
	var dupSNodes = [];
	var rPaths = [], rSteps = [];
	var origIdx = new Object();
	var ii;
	for (ii = 0; ii < hElems.length; ii++) {
		var peer = hElems[ii].peer;
		if (peer) {
			if (!peer.path && peer.canRemove()) {
				var kn = peer.cloneNode(true);
				if (kn) {
					var oid = kn.getAttribute("id");
					var nid = Uri.absolute(uid, Uri.localize(this.idCtx.prefix, oid));
					if (peer.constructor === PmGroup) {
						PmGroup.changeId(kn, oid, nid, this.xfMod);
						origIdx[Uri.absolute(oid, "$$S")] = Uri.absolute(nid, "$$S");
					}
					origIdx[oid] = nid;
					kn.setAttribute("id", nid);
					xNode.appendChild(kn);
					arrayAdd(rSteps, peer);
				}
			} else if (peer.path) {
				if (arrayFindStrict(rPaths, peer.path) < 0) {
					arrayAdd(rPaths, peer.path);
				}
			}
		}
	}
	
	for (ii = 0; ii < rSteps.length; ii++) {
		rSteps[ii].remove();
	}
	for (ii = 0; ii < rPaths.length; ii++) {
		if (rPaths[ii].xNode) rPaths[ii].remove();
	}

	PmGroup.mapSteps(xNode.childNodes, origIdx, this.idCtx.prefix, uid);
	
	// draw group
	var grp = new PmGroup(this, xNode);
	this.setChanged(grp);
    this.xfMod.rebuild();
    
	group.clear();
};

PmModel.prototype.clear = function() {
	this.clearFocus();
	this.clearToken();
	var ii;
    for (ii = 0; ii < this.paths.length; ii++) {
    	this.paths[ii].clear();
        this.paths[ii].disposeAll();
    }
    this.paths = [];
    for (ii = 0; ii < this.notes.length; ii++) {
    	this.notes[ii].clear();
        this.notes[ii].dispose();
    }
    this.notes = [];
    for (ii in this.steps) {
    	this.steps[ii].clear();
        this.steps[ii].dispose();
        delete this.steps[ii];
    }
	this.steps = new Object();
};

PmModel.prototype.setStepRemoved = function(xNode) {
	var oldid = xNode.getAttribute("oldid");
	var id;
	if (oldid) {
		// new test
		if (oldid == "//N") return;
		id = oldid;
	} else {
		id = xNode.getAttribute("id");
	}
	var rmEl = Xml.element(this.pmDocument.documentElement, "removed");
	rmEl.setAttribute("id", id);
	rmEl.setAttribute("type", Xml.getLocalName(xNode));
};



PmModel.prototype.dispose = function() {
	var ii;
    for (ii = 0; ii < this.paths.length; ii++) {
        this.paths[ii].disposeAll();
    }
    this.paths = null;
    for (ii = 0; ii < this.notes.length; ii++) {
        this.notes[ii].dispose();
    }
    this.notes = null;
    for (ii in this.steps) {
        this.steps[ii].dispose();
        delete this.steps[ii];
    }
    this.steps = null;
    this.canvas = null;
    this.canvasObj = null;
    this.dndCanvas = null;
    this.xb = null;
    this.previewing = null;
    this.modelNode = null;
    this.pmDocument = null;
    this.tokElem = null;
    this.focusBox = null;
};

PmModel.prototype.setModelNode = function(node) {
	if (this.modelNode === node) return;
	this.clear();
	var idPre = node.getAttribute("id") || "";
	this.idCtx = new XmlId(idPre);
	this.digestStepList(node.childNodes);
	this.modelNode = node;
	var crumbElem = this.xfMod.getUiElementById("canvasPath");
	if (crumbElem) removeElementChildren(crumbElem);
	if (node !== this.modelRoot) {
		if (crumbElem) {
			var se = makeElement(crumbElem, "span", "crumbClick", "Main");
			se._model = this;
			se._modNode = this.modelRoot;
			setEventHandler(se, "onclick", PmModel.__crumbClick);
			var stack = [];
			while (node && node !== this.modelRoot) {
				stack.push(node);
				node = node.parentNode;	
			}
			for (var ii = stack.length - 1; ii >= 1; ii--) {
				makeElement(crumbElem, "span", "sep", "/");
				se = makeElement(crumbElem, "span", "crumbClick", Uri.name(stack[ii].getAttribute("id")));
				se._model = this;
				se._modNode = stack[ii];
				setEventHandler(se, "onclick", PmModel.__crumbClick);
			}
			makeElement(crumbElem, "span", "sep", "/");
			makeElement(crumbElem, "span", "crumb", Uri.name(stack[0].getAttribute("id")));
		}
	} else { // read root notes too
		this.digestStepList(Xml.matchOne(this.modelRoot.parentNode, "notes").childNodes);
	}
};

PmModel.__crumbClick = function(evt) {
	this._model.setModelNode(this._modNode);
	return false;
};

/**
 * Abstract Event Shape
 */
PmEvent.prototype = new PmStep();
PmEvent.prototype.constructor = PmEvent;

function PmEvent() {
    this.xfTmplName = "event";
}

PmEvent.prototype.limitHook = function(pnt, dir) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    rect.y += 15;
    rect.h -= 30;
    if (dir == Point.NORTH || dir == Point.SOUTH) {
        rect.w -= 30;
        rect.x += 15;
    } else {
        rect.w -= 3;
        if( pnt.y < rect.y) {
            pnt.y = rect.y;
        } else if (pnt.y > rect.y + rect.h) {
            pnt.y = PM.SNAP(rect.y + rect.h);
        }
    }
    var maxX = rect.x + rect.w - 3;
    if (pnt.x < rect.x) pnt.x = rect.x;
    else if (pnt.x > maxX) pnt.x = maxX;
};


PmStart.prototype = new PmEvent();
PmStart.prototype.constructor = PmStart;

/**
 * Start shape
 */
function PmStart(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "start";
}

PmStart.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor == PmPathHook) {
        return dragObj.isStart;
    }
    return false;
};

PmStart.prototype.cloneNode = function(asGroup) {
	if (PM.LOCK_START) return null;
	return PmStep.prototype.cloneNode.apply(this, [asGroup]);
};

PmStart.prototype.canRemove = function() {
	return !PM.LOCK_START;
};

PmStart.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/start", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Start"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (PmStart.initNewCustom != null) {
        PmStart.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmStart(canvas.model, xNode);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(step);
    return step;
};



PmEnd.prototype = new PmEvent();
PmEnd.prototype.constructor = PmEnd;

/**
 * End shape
 */
function PmEnd(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "end";
}

PmEnd.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor == PmPathHook
    		&& dragObj.path.constructor !== PmLine) {
        return !dragObj.isStart;
    }
    return false;
};

PmEnd.prototype.hoverOver = function(evt) { // no out paths
};

PmEnd.prototype.hoverOut = function(evt) { // no out paths
};


PmEnd.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/end", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("End"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (PmEnd.initNewCustom != null) {
        PmEnd.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmEnd(canvas.model, xNode);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(step);
    return step;
};

PmTimer.prototype = new PmEvent();
PmTimer.prototype.constructor = PmTimer;

/**
 * Timer
 */
function PmTimer(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
}

PmTimer.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/timer", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Timer"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (PmTimer.initNewCustom != null) {
        PmTimer.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmTimer(canvas.model, xNode);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(step);
    return step;
};

PmSwitch.prototype = new PmStep();
PmSwitch.prototype.constructor = PmSwitch;

/**
 * Switch
 */
function PmSwitch(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "switch";
}

PmSwitch.prototype.limitHook = function(pnt, dir) {
    var rect = this.model.dndCanvas.getBounds(this.element);
    if (dir == Point.NORTH || dir == Point.SOUTH) {
        pnt.x = rect.x + 60;
        if (dir == Point.SOUTH) pnt.y = rect.y + rect.h - 3;
    } else {
        pnt.y = rect.y + 40;
    }
};

PmSwitch.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/switch", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Switch"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
    if (PmSwitch.initNewCustom != null) {
        PmSwitch.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmSwitch(canvas.model, xNode);
    canvas.model.xfMod.rebuild();
    canvas.model.setChanged(step);
    return step;
};

PmSwitch.prototype.addPath = function(path, isNewOrMove) {
    if (Xml.matchOne(path.xNode, "condition") == null) {
        this.model.xfMod.duplicateNode("instance('pal')/condition", ".", "label", path.xNode);
    }
    PmStep.prototype.addPath.apply(this, [path, isNewOrMove]);
};

PmSwitch.prototype.renderUpdate = function() {
    PmStep.prototype.renderUpdate.apply(this,[]);
    for (var ii = 0; ii < this.fromPaths.length; ii++) {
        this.fromPaths[ii].renderUpdate();
    }
};



PmGroup.prototype = new PmStep();
PmGroup.prototype.constructor = PmGroup;

/**
 * Group shape
 */
function PmGroup(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
        // index the enter
        model.steps[this.getId()] = this;
    }
    this.xfTmplName = "group";
}

PmGroup.prototype.getExitNode = function() {
	if (!this.exitNode) 
		this.exitNode = Xml.matchOne(this.xNode, "exit");
	return this.exitNode;
};

PmGroup.prototype.getEnterNode = function() {
	if (!this.enterNode) 
		this.enterNode = Xml.matchOne(this.xNode, "enter");
	return this.enterNode;
};

PmGroup.prototype.pathNodes = function() {
	return Xml.match(this.getExitNode(), "path");
};

PmGroup.prototype.setId = function(id) {
    if (this.oId != id) {
    	// re-index the enter
    	delete this.model.steps[this.getId()];
    	PmGroup.changeId(this.xNode, this.oId, id, this.model.xfMod);
    	PmStep.prototype.setId.apply(this, [id]);
    	this.model.steps[this.getId()] = this;
    }
};

PmGroup.prototype.getId = function() {
	// return the internal enter id
	return Uri.absolute(this.oId, "$$S");
};

PmGroup.prototype.addPath = function(path, isNewOrMove) {
    arrayAdd(this.fromPaths, path);
    var en = this.getExitNode();
    if (isNewOrMove && !Xml.isAncestor(en, path.xNode)) {
        Xml.insertChildBefore(en, path.xNode, ["attachment"]);
    }
    if (isNewOrMove) {
        this.model.xfMod.markChanged(en);
        this.model.xfMod.markChanged(path.xNode);
        this.model.xfMod.rebuild();
        path.renderUpdate();
    }
};

PmGroup.prototype.removePath = function(path) {
   this.fromPaths = arrayRemoveStrict(this.fromPaths, path);
   var en = this.getExitNode();
   en.removeChild(path.xNode);
   this.xNode.parentNode.appendChild(path.xNode);
   this.model.xfMod.markChanged(this.xNode.parentNode);
   this.model.xfMod.markChanged(en);
   this.model.xfMod.markChanged(path.xNode);
   this.model.xfMod.rebuild();
};

PmGroup.prototype.viewGroup = function() {
	this.model.setModelNode(this.xNode);
};

PmGroup.prototype.remove = function() {
	this.model.idCtx.removeVar(this.xNode.getAttribute("id"));
	PmStep.prototype.remove.apply(this, []);
};

PmGroup.prototype.unGroup = function() {
	var ii;
	var grpId = this.xNode.getAttribute("id");
	var kids = this.xNode.childNodes;
	var gSteps = [];
	var origIdx = new Object();
	for (ii = 0; ii < kids.length; ii++) {
		var stp = kids[ii];
		if (stp.nodeType == 1) {
			var ln = Xml.getLocalName(stp);
			var sId = stp.getAttribute("id");
			if (sId && ln !== "enter" && ln !== "exit") {
				gSteps.push(stp);
				var lId = Uri.localize(grpId, sId);
				if (ln == "group") {
					PmGroup.changeId(stp, sId, lId, this.model.xfMod);
					origIdx[Uri.absolute(sId, "$$S")] = Uri.absolute(lId, "$$S");
				}
				origIdx[sId] = lId;
				PmStep.changeNodeId(stp, sId);
				this.model.xfMod.setElementAttribute(stp, "id", lId);
			}
		}
	}
	
	for (ii = 0; ii < gSteps.length; ii++) {
		this.model.modelNode.appendChild(gSteps[ii]);
	}
	
	PmGroup.mapSteps(gSteps, origIdx, grpId, this.model.idCtx.prefix);
	
	var sObjs = this.model.digestStepList(gSteps, true);
	
	// clear group
	var group = this.model.dndCanvas.__groupType;
	group.clear();
	
	// add to group
	for (ii = 0; ii < sObjs.length; ii++) {
		var obj = sObjs[ii];
		this.model.setChanged(obj);
		if (obj instanceof PmStep || obj instanceof PmNote) {
			group.add(obj.element);
		} else { // path
			if (obj.startHook != null) group.add(obj.startHook.element);
			if (obj.endHook != null) group.add(obj.endHook.element);
			if (obj.label != null) group.add(obj.label.element);
			var edge = obj.startEdge;
		    while (edge != null) {
		       	group.add(edge.element);
		        edge = edge.nextNode;
		    }
		}
	}
	this.remove();
};

PmGroup.mapSteps = function(sNodes, origIdx, oid, nid) {
	for (var ii = 0; ii < sNodes.length; ii++) {
		var stp = sNodes[ii];
		if (stp.nodeType == 1) {
			var ln = Xml.getLocalName(stp);
			var sId = stp.getAttribute("id");
			if (ln == "group") {
				var spths = Xml.match(Xml.matchOne(stp, "exit"), "path");
				for (var jj = 0; jj < spths.length; jj++) {
					var pth = spths[jj];
					var pId = pth.getAttribute("id");
					var cId = Uri.absolute(nid, Uri.localize(oid, pId));
					pth.setAttribute("id", cId);
					PmPath.mapPathTo(pth, origIdx);
				}
			} else if (sId && ln !== "enter" && ln !== "exit") {
				var spths = Xml.match(stp, "path");
				for (var jj = 0; jj < spths.length; jj++) {
					var pth = spths[jj];
					var pId = pth.getAttribute("id");
					var cId = Uri.absolute(nid, Uri.localize(oid, pId));
					pth.setAttribute("id", cId);
					PmPath.mapPathTo(spths[jj], origIdx);
				}
			}
		}
	}
};

PmGroup.changeId = function(grpNode, oid, nid, xfMod) {
	// re-id all decendants and their paths
	var sNodes = Xml.match(grpNode, "*", "id");
	for (var ii = 0; ii < sNodes.length; ii++) {
		var stp = sNodes[ii];
		var ln = Xml.getLocalName(stp);
		var pId = stp.getAttribute("id");
		var cId = Uri.absolute(nid, Uri.localize(oid, pId));
		PmStep.changeNodeId(stp, pId);
		xfMod.setElementAttribute(stp, "id", cId);
		var spths = [];
		if (ln == "group") { // get the exit step of the sub group
			PmGroup.changeId(stp, pId, cId, xfMod); // recurse
			spths = Xml.match(Xml.matchOne(stp, "exit"), "path");
		} else if (ln != "exit") {
			spths = Xml.match(stp, "path");
		}
		for (var jj = 0; jj < spths.length; jj++) {
			var pth = spths[jj];
			
			// path id
			pId = pth.getAttribute("id");
			cId = Uri.absolute(nid, Uri.localize(oid, pId));		
			pth.setAttribute("id", cId);
			
			// path to
			pId = pth.getAttribute("to");
			cId = Uri.absolute(nid, Uri.localize(oid, pId));
			pth.setAttribute("to", cId);
		}	
	}
};

PmGroup.initNew = function(pnt, canvas, arg) {
    var xNode = canvas.model.xfMod.duplicateNode("instance('pal')/group", ".", null, canvas.model.modelNode);
    xNode.setAttribute("id", canvas.model.idCtx.uniqueVar("Group"));
    xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");

    if (PmGroup.initNewCustom != null) {
        PmGroup.initNewCustom(xNode, arg, canvas);
    }
    var step = new PmGroup(canvas.model, xNode);
    canvas.model.setChanged(step);
    canvas.model.xfMod.rebuild();
    return step;
};


PmEnter.prototype = new PmEvent();
PmEnter.prototype.constructor = PmEnter;

/**
 * Enter shape
 */
function PmEnter(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "enter";
}

PmEnter.prototype.canRemove = function() {
	return false;
};

PmEnter.prototype.getLabel = function() {
	return Xml.stringForNode(Xml.matchOne(this.xNode, "label"));
};

PmEnter.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor === PmPathHook) {
        return dragObj.isStart;
    }
    return false;
};

PmEnter.prototype.cloneNode = function(asGroup) {
	return null;
};



PmExit.prototype = new PmEvent();
PmExit.prototype.constructor = PmExit;

/**
 * Exit shape
 */
function PmExit(model, xNode) {
    if (arguments.length > 0) {
        this.init(model, xNode);
    }
    this.xfTmplName = "exit";
}

PmExit.prototype.canRemove = function() {
	return false;
};

PmExit.prototype.getLabel = function() {
	return Xml.stringForNode(Xml.matchOne(this.xNode, "label"));
};

PmExit.prototype.pathNodes = function() {
	return [];
};

PmExit.prototype.dropTest = function(dragObj) {
    if (dragObj.constructor === PmPathHook
    		&& dragObj.path.constructor !== PmLine) {
        return !dragObj.isStart;
    }
    return false;
};

PmExit.prototype.hoverOver = function(evt) { // no out paths
};

PmExit.prototype.hoverOut = function(evt) { // no out paths
};

PmExit.prototype.cloneNode = function(asGroup) {
	return null;
};

