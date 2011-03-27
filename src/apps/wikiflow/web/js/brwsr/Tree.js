/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.TRee
 * Interactive Menu Classes and Functions
 */

var _activeTree = null;


Tree.HINT_FULL = 0;
Tree.HINT_INSERT = 1;
Tree.HINT_REMOVE = 2;

function Tree(model) {
    this.__domElem = null;
    this.__dndFootElem = null;
    this.__lastSel = [];
    this.model = null;
    this.addIcon = true;
    if (model)  {
        this.setModel(model);
    } else {
        this.setModel(new TreeModel());
    }
}

Tree.prototype.setModel = function(model) {
    if (this.model != null) this.model.dispose();
    this.model = model;
    model.setTree(this);
};

Tree.prototype.render = function(domParent, style, cssClass) {
    this.__domElem = makeElement(domParent, "div",
        "tree" + (cssClass != null ? " " + cssClass : ""));
    if (SH.is_opera) this.__domElem.setAttribute("style", style);
    else this.__domElem.style.cssText = style;
    if (this.__dndFoot) {
        this.__renderDndFoot();
    }
    if (this.model.asyncLoader) {
        makeElement(this.__domElem, "div", "load", "Loading...");
    }
    this.model.__kidDomElem = this.__domElem;
    this.model.onReady(this.__renderItems, this, this.model);
};

Tree.prototype.getBounds = function() {
    if (!this.__domElem) return null;
    return getLocalBounds(this.__domElem.parentNode, this.__domElem);
};

Tree.prototype.__renderDndFoot = function() {
    this.__dndFootElem = elementNoSelect(makeElementNbSpd(this.__domElem, "ins"));
    this.__dndFootElem._isFoot = true;
    this.dndCanvas.makeDropTarget(this.__dndFootElem, this.dndType.type);
};

Tree.prototype.redrawAll = function() {
    this.__lastSel = [];
    this.model.__kidDomElem = this.__domElem;
    this.model.onReady(this.__renderItems, this, this.model);
};


Tree.prototype.redraw = function(item, hint) {
    if (item != null) {
        if (item.__icoDomE) item.__icoDomE.className = "icon " +
                    (item.icon ? item.icon : "defIco");
        var row = item.__domElem;
        item.renderLabel(row);
        if (item.__togDomE == null) {
            if (hint == Tree.HINT_INSERT) {
                item.expanded = true;
                item.allowsKids = true;
            }
            item.__togDomE =
                elementNoSelect(makeElementNbSpd(row, "div", "toggle"));
            setEventHandler(item.__togDomE, "onmousedown", __trToggleClick);
            if (item.expanded) {
                row.className = Tree.__cssClasses(item);
                this.__renderKids(item);
            }
        } else if (hint == Tree.HINT_INSERT && item.__kidDomElem == null) {
            this.toggle(item);
        } else {
            if (hint == Tree.HINT_INSERT && !item.expanded) this.toggle(item);
            this.__renderKids(item);
        }
        if (hint == Tree.HINT_REMOVE) {
            if (item.__kidDomElem.firstChild == null) {
                item.allowsKids = false;
                item.__kidDomElem.parentNode.removeChild(item.__kidDomElem);
                item.__kidDomElem = null;
                item.__domElem.removeChild(item.__togDomE);
                item.__togDomE = null;
            }
        }
    }
};

Tree.prototype.toggle = function(item) {
    if (item.expanded) {
        item.expanded = false;
        item.__kidDomElem.style.display = "none";
        item.__domElem.className = Tree.__cssClasses(item);
    } else {
        item.expanded = true;
        if (!item.__kidDomElem) this.__renderKids(item);
        else item.__kidDomElem.style.display = "";
        item.__domElem.className = Tree.__cssClasses(item);
    }
    if (SH.is_ie) Tree.__ieSiblingDrawFix(item.itemParent.getNextKid(item));
    if (item.toggled != null) {
        item.toggled();
    }
};

/**
    item
        .icon - iconStyle Calss
        .label - label
        .kids - sub-item array, null if unloaded
        .hasKids
        .expanded
*/
Tree.prototype.__renderItems = function(items, itemParent) {
    var parElem;
    var i;
    parElem = itemParent.__kidDomElem;
    var kids = [];
    var dKids = parElem.childNodes;
    for (i = dKids.length - 1; i >= 0; i--) {
        var kE = dKids[i];
        if (kE.__item != null) {
            kids.push(kE.__item);
        } else if (!kE._isFoot){
            parElem.removeChild(kE);
        }
    }
    for (i = 0; i < kids.length; i++) {
        arrayRemoveStrict(itemParent.kids, kids[i]);
        this.__removeItem(kids[i]);
    }
    for (i = 0; i < items.length; i++) {
        var item = items[i];
        item.__isLast = (i == items.length - 1);
        item.__isRoot = (itemParent === this.model && i == 0);
        this.__renderItem(item, parElem, null);
    }
};

Tree.prototype.__renderItem = function(item, parElem, beforeElem) {
    if (parElem === this.__domElem) {
        if (!beforeElem) beforeElem = this.__dndFootElem;
    }
    if (item.__domElem) {
        item.__domElem.className = Tree.__cssClasses(item);
        parElem.insertBefore(item.__domElem, beforeElem);
        if (item.__kidDomElem) {
            item.__kidDomElem.className = Tree.__cssClassesKids(item);
            parElem.insertBefore(item.__kidDomElem, beforeElem);
        }
        if (item.__tailDomE) {
        	item.__tailDomE.className = Tree.__cssClassesTail(item);
        	parElem.insertBefore(item.__tailDomE, beforeElem);
        }
        return;
    }
    item._tree = this;
    var row;
    row = makeElement(parElem, "div", Tree.__cssClasses(item), null, beforeElem);
    if (item.tip) row.title = item.tip;
    item.__domElem = row;
    row.__item = item;
    if (item.allowsKids) {
        item.__togDomE =
            elementNoSelect(makeElementNbSpd(row, "div", "toggle"));
        setEventHandler(item.__togDomE, "onmousedown", __trToggleClick);
    } else {
        item.__togDomE = null;
    }
    if(this.addIcon) {
        item.__icoDomE = elementNoSelect(makeElementNbSpd(row, "div",
            "icon " + (item.icon ? item.icon : "defIco")));
    }
    item.renderLabel(row);
    var triggerEvent = this.dndCanvas != null ? "onclick" : "onmousedown";
    if (item.editAct != null) {
        setEventHandler(item.__labDomE, triggerEvent, __trItemEdit);
    }
    if (item.act != null) {
        if (item.editAct == null) {
            item.__labDomE.className += " click";
            setEventHandler(item.__labDomE, triggerEvent, __trItemClick);
        }
        if(item.__icoDomE) {
            item.__icoDomE.className += " click";
            setEventHandler(item.__icoDomE, triggerEvent, __trItemClick);
        }
    }
    if (item.optAct != null) {
        setEventHandler(item.__labDomE, "oncontextmenu", __trItemOptions);
        if (item.__icoDomE) setEventHandler(item.__icoDomE, "oncontextmenu", __trItemOptions);
    }
    if (item.allowsKids && item.expanded) {
        this.__renderKids(item);
    }
    item.__tailDomE = item.renderTail();
    if (item.__tailDomE != null) {
    	item.__tailDomE.className = Tree.__cssClassesTail(item);
    	parElem.insertBefore(item.__tailDomE, beforeElem);
    }
    if (this.dndCanvas != null) {
        if (!item.noDrag) this.dndCanvas.makeDraggable(row, this.dndType.type);

        if (!item.noDrop) {
        	this.dndCanvas.makeDropTarget(row, this.dndType.type);
        	if (item.__tailDomE != null) {
        		this.dndCanvas.makeDropTarget(item.__tailDomE, this.dndType.type);
        		item.__tailDomE.__item = item;
        	}
        }
    }
    if (item.syncSize != null) {
        item.syncSize();
    }
    item.onLayout();
};

Tree.prototype.__renderKids = function(item) {
    if (!item.__kidDomElem) {
        var sib = item.__domElem.nextSibling;
        item.__kidDomElem = makeElement(
            item.__domElem.parentNode,
            "div", Tree.__cssClassesKids(item), null, sib);
    }
    if (this.model.asyncLoader) {
        makeElement(item.__kidDomElem, "div", "load", "Loading...",
            item.__kidDomElem.firstChild);
    }
    this.model.onReady(this.__renderItems, this, item);
};

Tree.prototype.__disposeItem = function(item) {
    item.dispose();
    if (item.__domElem != null) {
        if (this.dndCanvas != null) {
            this.dndCanvas.disposeDraggable(item.__domElem);
            this.dndCanvas.disposeDropTarget(item.__domElem);
            if (item.__tailDomE) this.dndCanvas.disposeDropTarget(item.__tailDomE);
        }
        item.__domElem.__item = null;
        item.__domElem = null;
    }
    var kids = null;
    if (item.kids != null) {
        kids = item.kids;
        item.kids = null;
    }
    if (kids != null) {
        for (var i = 0; i < kids.length; i++) {
            this.__disposeItem(kids[i]);
        }
    }

    item.node = null;
    item._tree = null;
    //item._parent = null;
    item.__togDomE = null;
    item.__icoDomE = null;
    item.__labDomE = null;
    item.__tailDomE = null;
    item.__kidDomElem = null;
};

Tree.prototype.__removeItem = function(item) {
    if (item.remove != null) item.remove();
    if (item.__domElem != null && item.__domElem.parentNode != null) {
        item.__domElem.parentNode.removeChild(item.__domElem);
    }
    if (item.__tailDomE != null && item.__tailDomE.parentNode != null) {
    	item.__tailDomE.parentNode.removeChild(item.__tailDomE);
    }
    var kids = null;
    if (item.kids != null) {
        kids = item.kids;
        item.kids = null;
    }
    if (kids != null) {
        for (var i = 0; i < kids.length; i++) {
            this.__removeItem(kids[i]);
        }
    }
    this.__disposeItem(item);
};

Tree.prototype.remove = function() {
    if (this.__domElem != null) {
        this.__domElem.parentNode.removeChild(this.__domElem);
    }
    this.dispose();
};

Tree.prototype.dispose = function() {
    if (this.model != null) {
        this.model.dispose();
        this.model = null;
    }
    if (this.menu != null) {
        this.menu.dispose();
    }
    if (this.menu2 != null) {
        this.menu2.dispose();
    }
    if (this.dndCanvas) {
        if (this.__dndFootElem) this.dndCanvas.disposeDropTarget(this.__dndFootElem);
        this.dndCanvas.dispose();
    }
    this.__dndFootElem = null;
    this.__domElem = null;
};

Tree.prototype.markSelected = function() {
    var ii, item;
    // unmark
    for (ii = 0; ii < this.__lastSel.length; ii++) {
        item = this.__lastSel[ii];
        if (item.__labDomE != null) {
            exAddClass(item.__labDomE, "treeSel", true);
        }
    }

    this.__lastSel = this.model.getSelected();

    // mark
    for (ii = 0; ii < this.__lastSel.length; ii++) {
        item = this.__lastSel[ii];
        if (item.__labDomE != null) {
            exAddClass(item.__labDomE, "treeSel");
        }
    }
};

Tree.prototype.makeDragCanvas = function(
        canvasElement, /*DNDTypeHandler*/ dndType, /*Boolean*/ global, /*Boolean*/ disabRoot, /*Boolean*/ disabGroup) {
    this.dndCanvas = dndGetCanvas(canvasElement);
    if (canvasElement !== document.body)
    	this.dndCanvas.setParent(dndGetCanvas(document.body), global);
    this.dndCanvas.addDNDType(dndType);
    this.dndType = dndType;
    if (!disabGroup) this.dndCanvas.setGroup(new DNDGroupDummy());
    this.dndCanvas.initAutoScroll(15, 15);
    if (!disabRoot) {
        this.__dndFoot = true;
        var holdThis = this;
        this.dndCanvas.canvasDrop = function(dragElem) {
            if (holdThis.model && !holdThis.model.hasKids())
                dndType.dropExec(holdThis.__dndFootElem, dragElem);
        };
    }
    dndType._tree = this;
    return this.dndCanvas;
};

Tree.prototype.renderItem = function(item, itemParent, beforeItem) {
    item.__isLast = (beforeItem == null);
    item.__isRoot = (itemParent === this.model && itemParent.getIndex(item) == 0);
    this.__renderItem(item, itemParent.__kidDomElem, beforeItem != null ? beforeItem.__domElem : null);
    var preItem = item.itemParent.getPreviousKid(item);
    if (preItem != null) {
        preItem.__isLast = false;
        if (preItem.__domElem) preItem.__domElem.className = Tree.__cssClasses(preItem);
        if (preItem.__kidDomElem) preItem.__kidDomElem.className = Tree.__cssClassesKids(preItem);
    }
    if (SH.is_ie && beforeItem != null) Tree.__ieSiblingDrawFix(beforeItem);
};


// IE adjacent position bug workaround
Tree.__ieSiblingDrawFix = function(item, skipParent) {
    if (item == null) return;
    Tree.__ieTopRefresh(item);
    if (item.expanded && item.kids != null) {
        window.setTimeout(function() {
            for (var ii = 0; ii < item.kids.length; ii++) {
                Tree.__ieSiblingDrawFix(item.kids[ii], true);
            }
        }, 1);
    }
    if (!skipParent) {
        var par = item.itemParent;
        var si = par.getIndex(item);
        for (var ii = si + 1; ii < par.kids.length; ii++) {
            Tree.__ieSiblingDrawFix(par.kids[ii], true);
        }
        if (par.itemParent != null) {
            Tree.__ieSiblingDrawFix(par.itemParent.getNextKid(par), false);
        }
    }
};

Tree.__ieTopRefresh = function(item) {
	if (!item.__domElem) return;
    var t = item.__domElem.style.top;
    item.__domElem.style.top = (t == "0px" ? "0pt" : "0px");
    if (item.__kidDomElem) {
        t = item.__kidDomElem.style.top;
        item.__kidDomElem.style.top = (t == "0px" ? "0pt" : "0px");
    }
   	if (item.__tailDomE) {
        t = item.__tailDomE.style.top;
        item.__tailDomE.style.top = (t == "0px" ? "0pt" : "0px");
    }
};

Tree.removeRender = function(item) {
	var beforeItem;
    if (item.__isLast) {
        item.__isLast = false;
        beforeItem = item.itemParent.getPreviousKid(item);
        if (beforeItem != null) {
            beforeItem.__isLast = true;
            if (beforeItem.__domElem) beforeItem.__domElem.className = Tree.__cssClasses(beforeItem);
            if (beforeItem.__kidDomElem) beforeItem.__kidDomElem.className = Tree.__cssClassesKids(beforeItem);
            if (beforeItem.__tailDomE) beforeItem.__tailDomE.className = Tree.__cssClassesTail(beforeItem);
        }
    }
    if (item.__isRoot) {
        item.__isRoot = false;
        var nextItem = item.itemParent.getNextKid(item);
        if (nextItem != null) {
            nextItem.__isRoot = true;
            if (nextItem.__domElem) nextItem.__domElem.className = Tree.__cssClasses(nextItem);
            if (nextItem.__tailDomE) nextItem.__tailDomE.className = Tree.__cssClassesTail(nextItem);
        }
    }
    if (item.__domElem != null) {
        item.__domElem.parentNode.removeChild(item.__domElem);
    }
    if (item.__kidDomElem != null) {
        item.__kidDomElem.parentNode.removeChild(item.__kidDomElem);
    }
    if (item.__tailDomE != null) {
        item.__tailDomE.parentNode.removeChild(item.__tailDomE);
    }
    if (SH.is_ie) {
    	Tree.__ieTopRefresh(item.itemParent);
   		Tree.__ieSiblingDrawFix(item.itemParent.getNextKid(item));
    }
};

Tree.__cssClasses = function(item, kidsEmpty) {
    var css;
    if (item.__isRoot) {
        css = item.__isLast ? "trRowRootLast" : "trRowRoot";
    } else {
        css = item.__isLast ? "trRowLast" : "trRow";
    }
    if (item.allowsKids && item.expanded && !kidsEmpty) {
        if (item.__isRoot) {
            css += (item.__isLast ? " openRootLast" : " openRoot");
        } else {
            css += (item.__isLast ? " openLast" : " open");
        }
    }
    var dyncss = item.getDynCssClass();
    return css + (item.cssClass != null ? " " + item.cssClass : "") + (dyncss ? " " + dyncss : "");
};

Tree.moveRender = function(item, itemParent, beforeItem) {

    if (itemParent.__kidDomElem != null) {
        var bef = beforeItem != null ? beforeItem.__domElem :
                  (itemParent === item.model ? (item._tree ? item._tree.__dndFootElem : null) : null);
                  
        if (item.__tailDomE != null) {
        	itemParent.__kidDomElem.insertBefore(item.__tailDomE, bef);
        	bef = item.__tailDomE;
        }
        if (item.__kidDomElem != null) {
            itemParent.__kidDomElem.insertBefore(item.__kidDomElem, bef);
            bef = item.__kidDomElem;
        }
        if (item.__domElem) itemParent.__kidDomElem.insertBefore(item.__domElem, bef);
        if (item.__isRoot) {
        	var rootItem = item.model.kid[0];
        	if (rootItem !== item) {
        		item.__isRoot = false;
        		rootItem.__isRoot = true;
        		if (rootItem.__domElem) rootItem.__domElem.className = Tree.__cssClasses(rootItem);
        	}
        }
        if (beforeItem != null) {
            if (beforeItem.__isRoot) {
                beforeItem.__isRoot = false;
                item.__isRoot = true;
            }
            item.__isLast = false;
            if (item.__domElem) item.__domElem.className = Tree.__cssClasses(item);
            if (item.__kidDomElem) item.__kidDomElem.className = Tree.__cssClassesKids(item);
            if (item.__tailDomE) item.__tailDomE.className = Tree.__cssClassesTail(item);
            
            if (beforeItem.__kidDomElem) beforeItem.__kidDomElem.className = Tree.__cssClassesKids(beforeItem);
            if (beforeItem.__domElem) beforeItem.__domElem.className = Tree.__cssClasses(beforeItem);
        } else {
            beforeItem = item.itemParent.getPreviousKid(item);
            if (beforeItem != null) {
                beforeItem.__isLast = false;
                if (beforeItem.__domElem) beforeItem.__domElem.className = Tree.__cssClasses(beforeItem);
                if (beforeItem.__kidDomElem) beforeItem.__kidDomElem.className = Tree.__cssClassesKids(beforeItem);
                if (beforeItem.__tailDomE) beforeItem.__tailDomE.className = Tree.__cssClassesTail(beforeItem);
            }
            item.__isLast = true;
            if (item.__domElem) item.__domElem.className = Tree.__cssClasses(item);
            if (item.__kidDomElem) item.__kidDomElem.className = Tree.__cssClassesKids(item);
            if (item.__tailDomE) item.__tailDomE.className = Tree.__cssClassesTail(item);
            
        }
        item.onLayout();
    }

    if (item.__domElem != null && SH.is_ie) Tree.__ieSiblingDrawFix(item);
};

Tree.__cssClassesKids = function(item) {
    var css = item.__isLast ? "kidsLast" : "kids";
    return css + (item.cssClass != null ? " " + item.cssClass : "");
};

Tree.__cssClassesTail = function(item) {
    var css = item.__isLast ? "tailLast" : "tail";
    return css + (item.cssClass != null ? " " + item.cssClass : "");
};


Tree.updateItemCss = function(item) {
    if (item.__domElem) item.__domElem.className = Tree.__cssClasses(item);
    if (item.__kidDomElem) item.__kidDomElem.className = Tree.__cssClassesKids(item);
    if (item.__tailDomE) item.__tailDomE.className = Tree.__cssClassesTail(item);
};

Tree.selectItem = function(item) {
    if (item) {
        item.model.setSelected([item]);
        item._tree.markSelected();
    }
};

function TreeItem(model, label, allowsKids, icon) {
    if (arguments.length > 0) {
        this.model = model;
        this.label = label;
        this.allowsKids = allowsKids;
        this.itemParent = null;
        this.kids = [];
        this.icon = icon;
    }
}

TreeItem.prototype.add = function(item) {
    this.insert(item, this.kids.length);
};

TreeItem.prototype.insert = function(item, index) {
    if (this.allowsKids) {
        if (item.itemParent === this && this.getIndex(item) < index) {
           index--;
        }
        item.setParent(this);
        var next = this.kids[index];
        arrayInsert(this.kids, index, item);
        Tree.moveRender(item, this, next);
    }
};

TreeItem.prototype.getPreviousKid = function(item) {
    var idx = this.getIndex(item);
    if (idx > 0) {
        return this.kids[idx - 1];
    }
    return null;
};

TreeItem.prototype.getNextKid = function(item) {
    var idx = this.getIndex(item);
    return this.kids[idx + 1];
};

TreeItem.prototype.getIndex = function(item) {
    return arrayFindStrict(this.kids, item);
};

TreeItem.prototype.insertAfter = function(item, afterItem) {
    var idx = arrayFindStrict(this.kids, afterItem);
    this.insert(item, idx + 1);
};

TreeItem.prototype.insertBefore = function(item, beforeItem) {
    var idx = arrayFindStrict(this.kids, beforeItem);
    this.insert(item, idx);
};

TreeItem.prototype.hasKids = function() {
    return this.allowsKids && this.kids.length > 0;
};

TreeItem.prototype.renderKid = function(item) {
    var beforeItem = this.getNextKid(item);
    this.model._tree.renderItem(item, this, beforeItem);
};

TreeItem.prototype.setParent = function(itemParent) {
    this.removeFromParent();
    this.itemParent = itemParent;
};

TreeItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
        this.__labDomE = makeElement(hParent, "div", "label", this.label);
    } else if (this.__labDomE.parentNode !== hParent){
        hParent.appendChild(this.__labDomE);
    }
};

TreeItem.prototype.renderTail = function() {
    return null;
};

TreeItem.prototype.setLabel = function(label) {
    this.label = label;
    if (this.__labDomE != null) {
        setElementText(this.__labDomE, label);
    }
};

TreeItem.prototype.removeFromParent = function() {
    if (this.itemParent != null) {
        Tree.removeRender(this);
        this.itemParent.removeKid(this);
        this.itemParent = null;
    }
};

TreeItem.prototype.removeKid = function(item) {
    arrayRemoveStrict(this.kids, item);
};

TreeItem.prototype.removeItem = function() {
    Tree.removeRender(this);
    if (this.itemParent != null) {
        this.itemParent.removeKid(this);
    }
    if (this._tree != null) this._tree.__removeItem(this);
};


TreeItem.prototype.onLayout = function() {
    // listen here for position changes
};


TreeItem.prototype.getDepth = function() {
    var dep = 0;
    var ip = this.itemParent;
    while (ip !== this.model && ip != null) {
        dep++;
        ip = ip.itemParent;
    }
    return dep;
};

TreeItem.prototype.setCssClass = function(cssClass) {
    this.cssClass = cssClass;
    Tree.updateItemCss(this);
};

/*
 * Override this to return procedural style class selectors
 */
TreeItem.prototype.getDynCssClass = function() {
	return "";
};

TreeItem.prototype.isAncestor = function(item) {
    while (this !== item) {
        item = item.itemParent;
        if (item == null) return false;
    }
    return true;
};

TreeItem.prototype.dispose = function() {
    this.model = null;
    this.itemParent = null;
    this.__labDomE = null;
};





/**
    asyncLoader = function(itemParent). Note: itemParent is null for root
*/
function TreeModel(/*activities, asyncLoader*/) {
    TreeItem.apply(this, [this, "<root>", true]);
    this.selected = [];
    /*if (activities) this.activities = activities;
    else this.activities = [];
    this.asyncLoader = asyncLoader;*/
}

TreeModel.prototype = new TreeItem();
TreeModel.prototype.constructor = TreeModel;

TreeModel.prototype.setTree = function(tree) {
    this._tree = tree;
};

TreeModel.prototype.getSelected = function() {
    return this.selected;
};

TreeModel.prototype.setSelected = function(items) {
    this.selected = items;
};

TreeModel.prototype.onReady = function(callback, tree, itemParent) {
    callback.apply(tree, [itemParent.kids, itemParent]);
};

TreeModel.prototype.dispose = function() {
    if (this.kids != null) {
        for (var ii = 0; ii < this.kids.length; ii++) {
            this._tree.__disposeItem(this.kids[ii]);
        }
    }
    TreeItem.prototype.dispose.apply(this, []);
    this._tree = null;
};


DirTreeModel.prototype = new TreeModel();
DirTreeModel.prototype.constructor = DirTreeModel;

function DirTreeModel(/*String*/ path, /*Array<String>*/ list) {
    TreeModel.apply(this, []);
    this.path = path;
    this.list = list;
}

DirTreeModel.escapeRegExp = function (s) {
    // Escape any special characters with that character preceded by a backslash
    return s.replace(new RegExp("([\\\\\\^\\$\\*\\+\\?\\(\\)\\=\\!\\|\\,\\{\\}\\[\\]\\.])","g"),"\\$1")
}

DirTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    var uniques = new Object();
    var path = "";
    if (itemParent != this) {
        path = itemParent.path + "/";
    }
    var rg = new RegExp("^" + DirTreeModel.escapeRegExp(path) + "([^/]*)([/]?)", "i");
    for (var ii = 0; ii < this.list.length; ii++) {
         var dstr = this.list[ii];
         if(typeof dstr != "string") {     //TreeItem(model, label, allowsKids, icon)
            item = new TreeItem(this);
            item.setCssClass(dstr.cssClass);
            itemParent.add(item);
            item.noDrag = true;
            continue;
         }
         var mr = rg.exec(dstr);
         if (mr) {
            var lm = mr[1].toLowerCase();
            var item;
            if (!(lm in uniques)) {
                var ipath = path + mr[1];
                item = new TreeItem(this, mr[1], mr[2] != "");
                item.path = ipath;
                if (item.allowsKids) item.icon = "fldIco";
                itemParent.add(item);
                uniques[lm] = item;
            }
         }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
};


function __trToggleClick(evt) {
    var item = this.parentNode.__item;
    item._tree.toggle(item);
}

function __trItemClick(evt) {
    if (evt.button > 1) {
        return true;
    }
    var item = this.parentNode.__item;
    if (item.act != null) {
        item.act(evt);
        evt.cancelBubble = true;
        return false;
    }
    return true;
}

function __trItemEdit(evt) {
    if (evt.button > 1) {
        return true;
    }
    var item = this.parentNode.__item;
    if (!item.readonly && item.editAct != null) {
        item.editAct(evt);
        evt.cancelBubble = true;
        return false;
    }
    return true;
}

function __trItemOptions(evt) {
    var item = this.parentNode.__item;
    if (item.optAct != null) {
        item.optAct(evt);
        evt.cancelBubble = true;
        return false;
    }
    return true;
}

Tree.selectAction = function(evt) {
    Tree.selectItem(this);
};

Tree.menuAction = treeMenuAction = function(evt) {
    var menu = this._tree.menu;
    if (menu != null) {
        menu.popUp(evt, this);
    }
};

Tree.menu2Action = treeMenu2Action = function(evt) {
    var menu = this._tree.menu2;
    if (menu != null) {
        menu.popUp(evt, this);
    }
};

// check if dnd ok
if (typeof(DNDTypeDummy) != "undefined") {

function TreeDNDType(cssClass) {
    this.type = "dndTree";
    this.cssClass = cssClass;
}

TreeDNDType.prototype = new DNDTypeDummy();
TreeDNDType.prototype.constructor = TreeDNDType;

TreeDNDType.prototype.dropTest = function(dropElem, dragElem) {
    // default same tree type test
    if (dropElem._dndType.__canvas === dragElem._dndType.__canvas) {
        if (dragElem._dndType instanceof DNDGroup) {
            // TODO support groups
            return false;
        }
        if (dropElem._isFoot) {
            return true;
        }
        var dropItem = dropElem.__item;
        var dragItem = dragElem._actElem.__item;
        return dragItem && !dragItem.isAncestor(dropItem) && DNDTypeDummy.prototype.dropTest.apply(this, [dropElem, dragElem]);
    }
    return false;
};

TreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
    var dragItem = dragElem._actElem.__item;
    if (dropElem._isFoot) {
        this._tree.model.add(dragItem);
        return;
    }
    var dropItem = dropElem.__item;
    if (dropItem.allowsKids) {
        dropItem.add(dragItem);
    } else {
        dropItem.itemParent.insertAfter(dragItem, dropItem);
    }
};


} // endif dnd ok
