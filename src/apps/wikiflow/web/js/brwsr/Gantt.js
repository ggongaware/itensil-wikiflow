/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Lib: brwsr.Gantt
 * Requires: brwsr.Tree
 * Requires: brwsr.Grid
 * Requires: brwsr.Panel
 * 
 * Gantt Chart Scheduling
 * 
 */

function Gantt(model, unitType) {
	this.model = model;
	model.gantt = this;
	this.unitType = unitType || "days";
	switch (this.unitType) {
		default: this.unitPix = 16; this.unitTime = DateUtil.DAYS;
	}
	this.grid = new Grid(this.model);
	this.ps = new PanelSetVSplit(false, 380);
	this.ps.splitWidth = 2;
	this.ps.cssClass = "ganttPanels";
	
	var holdThis = this;
	this.treePanel = new Panel("", false, "ganTreePanel");
	this.treePanel.barHeight = 0;

	this.canvasPanel = new Panel("", false, "ganCanvPanel");
	this.canvasPanel.barHeight = 0;
	this.canvasPanel.linkResize({
		resize : function(rect) {
			if (holdThis.canvasScrollElem) holdThis.canvasScrollElem.style.height = (rect.h - 29) + "px";
		}
	});
	
	this.ps.addMajor(this.canvasPanel);
	this.ps.addMinor(this.treePanel);
}

Gantt.prototype = {
	constructor : Gantt,
	
	unitToPixel : function(unit) {
		return this.unitPix * unit;
	},
	
 	unitSnap : function(px) {
		return Math.round(px / this.unitPix) * this.unitPix;
	},
	
	pixelToUnit : function(px) {
		return px / this.unitPix;
	},
	
	dateToUnit : function(date) {
		return DateUtil.dayDiff(date, this.alignStart);
	},
	
	unitToDate : function(unit) {
		return new Date(this.alignStart.getTime() + (unit * this.unitTime));
	},
	
	drawWeeklyScale : function(startDate, endDate) {
		this.scaleScrollElem = elementNoSelect(makeElement(this.canvasPanel.contentElement, "div", "scaleScroll"));
		this.scaleElem = makeElement(this.scaleScrollElem, "div", "weeklyScale");
		this.pastElem = makeElement(this.scaleElem, "div", "pastScale");
		var alignStart = new Date(startDate.getTime() - (startDate.getUTCDay() * this.unitTime));
		this.alignStart = alignStart;
		var daysTot = DateUtil.dayDiff(endDate, alignStart);
		var tikElem, slab, objDate, alMillis = alignStart.getTime();
		this.drawWeekTicks(0, daysTot);
		var width = this.unitToPixel(daysTot + 6);
		this.scaleElem.style.width = width + "px";
		this.canvasElem.style.width = width + "px";
		this.pastElem.style.width = this.unitToPixel(this.dateToUnit(DateUtil.asUTC(new Date()))) + "px";
	},
	
	drawWeekTicks : function(startUnit, daysTot) {
		var tikElem, slab, objDate, alMillis = this.alignStart.getTime();
		for (var ii = startUnit; ii < daysTot; ii += 7) {
			objDate = new Date(alMillis + (ii * this.unitTime));
		 	slab = objDate.getUTCFullYear() + "-" + numberPad(objDate.getUTCMonth() + 1, 2) + 
		 		"-" + numberPad(objDate.getUTCDate(), 2);
		 	tikElem = makeElement(this.scaleElem, "div", "week", slab);
		 	this.lastTickPx = this.unitToPixel(ii);
		 	tikElem.style.left = this.lastTickPx + "px";
		}
	},
	
	checkMaxPixel : function(px) {
		var width = parseInt(this.canvasElem.style.width);
		if (px > width) {
			width = this.unitSnap(px + 6);
			this.scaleElem.style.width = width + "px";
			this.canvasElem.style.width = width + "px";
			if (width > this.lastTickPx) {
				var nxt = tot = this.pixelToUnit(this.lastTickPx) + 7;
				var max = this.pixelToUnit(width);
				while (tot < max) tot += 7;
				this.drawWeekTicks(nxt, tot + 1);
			}
		}
	},
	
	drawWeeklyCanvas : function() {
		this.canvasScrollElem = makeElement(this.canvasPanel.contentElement, "div", "canvasScroll");
		var rect = getSize(this.canvasPanel.contentElement)
		this.canvasScrollElem.style.height = (rect.h - 28) + "px";
		this.canvasElem = makeElement(this.canvasScrollElem, "div", "weeklyCanvas");
		this.dndCanvas = dndGetCanvas(this.canvasElem);
		this.dndCanvas.addDNDType(new GanttDNDTypeHand());
		
		var holdThis = this;
		setEventHandler(this.canvasScrollElem, "onscroll", function() {
				holdThis.scaleElem.style.left = (-holdThis.canvasScrollElem.scrollLeft) + "px";
				holdThis.treeElem.style.top = (-holdThis.canvasScrollElem.scrollTop) + "px";
			});
	},
	
	drawTree : function() {
		this.treeScrollElem = elementNoSelect(makeElement(this.treePanel.contentElement, "div", "ganTreeScroll"));
		this.treeElem = elementNoSelect(makeElement(this.treeScrollElem, "div", "ganTree i_grid"));
		
		this.tree = new Tree(this.model);
		this.grid.render(this.treeElem);
    	this.tree.render(this.treeElem, this.grid.getTreeStyle(), this.treeCssClass);
	},
	
	render : function(uiParent) {
		this.ps.render(uiParent);

		this.drawWeeklyCanvas();
		this.model.initGtModel();
		this.drawWeeklyScale(this.model.getMinDate(), this.model.getMaxDate());
		
		this.drawTree();
		
	},
	
	dispose : function() {
		if (this.tree) this.tree.dispose();
		if (this.grid) this.grid.dispose();
		if (this.ps) this.ps.dispose();
		if (this.dndCanvas) this.dndCanvas.dispose();
		this.canvasScrollElem = null;
		this.canvasElem = null;
		this.treeScrollElem = null;
		this.treeElem = null;
		this.scaleScrollElem = null;
		this.scaleElem = null;
	}
	
};

// Works on TreeModel
var GanttModelInterface = {
	
	initGtModel : function() {
		// over load onready
		this._gtOrigOnReady = this.onReady;
		this.onReady = this._gtOnReady;
	},
	
	_gtOnReady : function(callback, tree, itemParent) {
		var holdThis = this;
		var callBackOverload = function(itemsOv, itemParentOv) {
				callback.apply(tree, [itemsOv, itemParentOv]);
				holdThis.initGtItems(itemsOv, itemParent);
			};
		this._gtOrigOnReady(callBackOverload, tree, itemParent);
	},
	
	initGtItems : function(items, itemParent) {
		var itm;
		var cvElem = this.gantt.canvasElem, dndCav = this.gantt.dndCanvas;
		if (itemParent !== this.model) {
			var nxItm = itemParent.itemParent.getNextKid(itemParent);
			cvElem = makeElement(itemParent.itemParent._gtKidsElem, "div", "ganttKids", null,  nxItm ? nxItm._gtRowElem : null);
		}
		itemParent._gtKidsElem = cvElem;
		for (var ii = 0; ii < items.length; ii++) {
			itm = items[ii];
			objectExtend(itm, GanttRowInterface);
			itm.initGtRow();
			itm.drawGtRow(cvElem, dndCav);
		}
	},
	
	getMinDate : function() {
		return this.minDate;
	},
	
	getMaxDate : function() {
		return this.maxDate;
	},
	
	gtBubbleDates : function(cStdt, cEndt) {
		this.gantt.checkMaxPixel(this.gantt.unitToPixel(this.gantt.dateToUnit(cEndt)));
	}
};

// Works with TreeItem
var GanttRowInterface = {
	
	initGtRow : function() {
		// GanttDepend
	    this.predecessors = [];
	    // GanttDepend
	    this.successors = [];
	    this.start = 0;
	    this.end = 1;
	    if (this.toggled !== GanttRowInterface.toggled) {
	    	this._gtOrigToggled = this.toggled;
	    	this.toggled = GanttRowInterface.toggled;
	    }
	    if (this.syncSize !== GanttRowInterface.syncSize) {
	    	this._gtOrigSyncSize = this.syncSize;
	    	this.syncSize = GanttRowInterface.syncSize;
	    }
	    this._gtOrigRemove = this.remove;
	    this.remove = this.gtRemove;
	    if (!this.gtSkipBar) {
	    	this.gtSkipBar = !(this.getStartDate && this.getEndDate);
	    }
	},
	
	syncSize : function (option) {
		if (this._gtOrigSyncSize) this._gtOrigSyncSize(option);
		this.gtSizeBar();
	},
	
	gtSizeBar : function () {
		if (this.gtDragging) return;
		if (this.__domElem) {
			var rb = getSize(this.__domElem);
			this._gtRowElem.style.height = rb.h + "px";
			
			if (! this.gtSkipBar) {
				var stdt = this.getStartDate();
				var endt = this.getEndDate();
					
				if (stdt && endt) {
					if (!this._gtBarElem) this.drawGtBar();
					this._gtBarElem.style.height = (rb.h - (this.allowsKids ? 12 : 8)) + "px";
					
					var gantt = this.model.gantt;

					var left = gantt.unitToPixel(gantt.dateToUnit(stdt));
					var width = gantt.unitToPixel(gantt.dateToUnit(endt) + 1) - left;

					if (width < gantt.unitPix) width = gantt.unitPix;
					
					this._gtBarElem.style.left = left + "px";
					this._gtBarElem.style.width = width + "px";
					this.itemParent.gtBubbleDates(stdt, endt);
				}
			}
		}
	},
	
	gtHasBar : function() {
		return !this.gtSkipBar && this._gtBarElem;
	},
	
	gtBubbleDates : function(cStdt, cEndt, overDelay) {
		if (this.__domElem) {
			if (! this.gtSkipBar) {
				if (overDelay) {
					window.clearTimeout(this.gtBubbleDelay);
					var holdThis = this;
					this.gtBubbleDelay = window.setTimeout(function() {
							holdThis.gtBubbleDelay = null;
							holdThis.gtSizeToKids(cStdt, cEndt);
						}, 120);
				} else if (!this.gtBubbleDelay) {
					var holdThis = this;
					this.gtBubbleDelay = window.setTimeout(function() {
							holdThis.gtBubbleDelay = null;
							holdThis.gtSizeToKids(cStdt, cEndt);
						}, 120);
				}
			} else {
				this.itemParent.gtBubbleDates(cStdt, cEndt, overDelay);
			}
		} else {
			this.itemParent.gtBubbleDates(cStdt, cEndt, overDelay);
		}
	},
	
	gtSizeToKids : function(minStdt, maxEndt) {
		for (var ii = 0; ii < this.kids.length; ii++) {
			var kitm = this.kids[ii];
			if (!kitm.gtSkipBar) {
				var stdt = kitm.getStartDate();
				var endt = kitm.getEndDate();
				if (stdt && (!minStdt || minStdt.getTime() > stdt.getTime())) minStdt = stdt;
				if (endt && (!maxEndt || maxEndt.getTime() < endt.getTime())) maxEndt = endt;
			}
		}
		if (minStdt && maxEndt) {
			this.setStartDate(minStdt, true);
			this.setEndDate(maxEndt, true);
			this.gtSizeBar();
		}
	},
	
	drawGtRow : function(parElem, dndCanvas) {
		this._gtRowElem = makeElement(parElem, "div", "ganttRow");
		this.dndCanvas = dndCanvas;
		if (! this.gtSkipBar) {
			var stdt = this.getStartDate();
			var endt = this.getEndDate();
			if (stdt && endt)
				this.drawGtBar();
		}
		this.gtSizeBar();
	},
	
	drawGtBar : function() {
		var gbClass = this.allowsKids ? "ganttGroup" : "ganttBar";
		if (this.gtBarStyle) gbClass += " " + this.gtBarStyle();
		this._gtBarElem = makeElementNbSpd(this._gtRowElem, "div", gbClass);
		this._gtBarElem._item = this;
		if (! this.allowsKids) {
			this._gtBStartEl = makeElementNbSpd(this._gtBarElem, "div", "gtBStart");
			this._gtBStartEl._itemStart = this;
			this.dndCanvas.makeDraggable(this._gtBStartEl, "ganttBar");
			this._gtBEndEl = makeElementNbSpd(this._gtBarElem, "div", "gtBEnd");
			this._gtBEndEl._itemEnd = this;
			this.dndCanvas.makeDraggable(this._gtBEndEl, "ganttBar");
		}
		this.dndCanvas.makeDraggable(this._gtBarElem, "ganttBar");
	},
	
	toggled : function() {
		if (this._gtOrigToggled) this._gtOrigToggled();
		if (this._gtKidsElem) this._gtKidsElem.style.display = this.expanded ? "" : "none";
	},
	
	gtScrollTo : function() {
		var gantt = this.model.gantt;
		var left = parseInt(this._gtBarElem.style.left) - gantt.unitPix * 2;
		if (left < 0) left = 0;
		gantt.canvasScrollElem.scrollLeft = left;
	},

	gtRemove : function() {
		if (this._gtBarElem) {
			this.dndCanvas.disposeDraggable(this._gtBarElem);
			this._gtBarElem = null;
		}
		if (this._gtRowElem) {
			this._gtRowElem.parentNode.removeChild(this._gtRowElem);
			this._gtRowElem = null;
		}
		if (this._gtKidsElem) {
			this._gtKidsElem.parentNode.removeChild(this._gtKidsElem);
			this._gtKidsElem = null;
		}
		this._gtOrigRemove();
	}
	
};


function GanttDepend() {
	this.offset = 0;
	this.overlap = 0;
	this.depType = 0;
}

GanttDepend.prototype = {
	constructor : GanttDepend
};

GanttDNDTypeHand.prototype = new DNDTypeHandler();

function GanttDNDTypeHand() {
	this.type = "ganttBar";
	this.limitY = 1;
}

GanttDNDTypeHand.prototype.dragDone = function(dragElem) {
    var item = dragElem._item || dragElem._itemStart || dragElem._itemEnd;
	item.gtDragging = false;
	if (item.gtDragDone) item.gtDragDone();
};

GanttDNDTypeHand.prototype.startDrag = function(x, y, dragElem) {
	DNDTypeHandler.prototype.startDrag.apply(this, [x, y, dragElem]);
	var item = dragElem._item || dragElem._itemStart || dragElem._itemEnd;
	item.gtDragging = true;
};

GanttDNDTypeHand.prototype.dragMove = function(x, y, dragElem) {
	var item, gantt, barElem, left, width;
	if (dragElem._item) {
		item = dragElem._item;
		gantt = item.model.gantt;
		barElem = dragElem;
		left = gantt.unitSnap(this.offX + x);
		width = parseInt(barElem.style.width);
	} else if (dragElem._itemStart) {
		item = dragElem._itemStart;
		gantt = item.model.gantt;
		barElem = item._gtBarElem;
		var cleft = parseInt(barElem.style.left);
		var cwidth = parseInt(barElem.style.width);
		var dif = gantt.unitSnap(this.offX + x) - cleft;
	   	left = gantt.unitSnap(this.offX + x);
	   	var nw = cwidth - dif;
	   	width = (nw >= gantt.unitPix ? gantt.unitSnap(nw) : gantt.unitPix);
	} else if (dragElem._itemEnd) {
		item = dragElem._itemEnd;
		gantt = item.model.gantt;
		barElem = item._gtBarElem;
		left = parseInt(barElem.style.left);
		width = gantt.unitSnap(this.offX + x) - left;
		if (width < gantt.unitPix) width = gantt.unitPix;
	} else {
		return;
	}

	barElem.style.left = left + "px";
	barElem.style.width = width + "px";
	var stdt = gantt.unitToDate(gantt.pixelToUnit(left));
	var endt = gantt.unitToDate(gantt.pixelToUnit(left + width) - 1);
	item.setStartDate(stdt);
	item.setEndDate(endt);
	item.itemParent.gtBubbleDates(stdt, endt, true);
};
