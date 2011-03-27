/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.Menu
 * Interactive Menu Classes and Functions
 */

var _activeMenu = null;

function Menu(model, defaultAct, cssClass) {
    this.__domElem = null;
    this.__parent = null;
    this.__decTimer = null;
    this.__subMenu = null;
    this.__subMTimer = null;
    this.__refreshNext = false;
    this.__x = -1;
    this.__y = -1;
    this.__w = -1;
    this.__h = -1;
    this.minWidth = 0;
    this.__context = null;
    if (model != null)  {
        this.model = model;
    } else {
        this.model = new MenuModel();
    }
    this.model._menu = this;
    if (defaultAct != null) {
        this.defaultAct = defaultAct;
    }
    this.cssClass = cssClass;
}

Menu.GUTTER = 6;
Menu.MIN_SCROLL_HEIGHT = 150; 

Menu.prototype.popUp = function(evt, context) {
    this.show(getMouseX(evt), getMouseY(evt), null, context);
    return false;
};

Menu.prototype.show = function(x, y, mnParent, context) {
    if (mnParent != null) {
        this.__parent = mnParent;
        this.__parent.__revive();
    } else {
        this.__parent = null;
        if (_activeMenu != this) {
            if (_activeMenu != null) _activeMenu.hide();
            Ephemeral.register(this);
            _activeMenu = this;
        }
    }
    if (this.__domElem == null) {
    	this.__refreshNext = false;
        this.render();       
    } else if (this.__refreshNext) {
    	this.__refreshNext = false;
    	this.refresh();
    }
    this.__context = context;
    if (this.model.onSetContext != null) {
        this.model.onSetContext(context);
    }
    this.__lastItemOut();
    if (this.__subMenu != null) {
        this.__subMenu.hide(true);
    }
    this.__subMenu = null;
    this.__x = x;
    this.__y = y;
    if (this.__h >= 0) {
        this.__domElem.style.display = "block";
        this.__screenBounds();
    } else {
        // try to pre position inbounds for width check
        var vr = getVisibleBounds();
        if ((x + 220) > vr.w) {
            x = vr.w - 220;
            if (x < 0) x = 0;
        }
        this.__domElem.style.left = x + "px";
        this.__domElem.style.top = y + "px";
        this.__domElem.style.display = "block";
    }
};

Menu.prototype.hide = function(noRipple) {
    window.clearTimeout(this.__subMTimer);
    window.clearTimeout(this.__decTimer);
    this.__subMTimer = null;
    this.__decTimer = null;
    if (_activeMenu === this) {
        Ephemeral.unregister(this);
        _activeMenu = null;
        if (SH.is_ie) {
            unHideSelects();
        }
    }
    if (this.__subMenu != null) {
        this.__subMenu.hide(true);
    }
    this.__lastItemOut();
    if (this.__domElem != null) this.__domElem.style.display = "none";
    if (!noRipple && this.__parent != null) {
        this.__parent.hide();
    }
    if (this.onHide != null) {
    	this.onHide();
    }
};

Menu.prototype.isShowing = function() {
	return this.__domElem && this.__domElem.style.display == "block";
};

Menu.prototype.refresh = function() {
    if (this.__subMenu != null) {
        this.__subMenu.hide(true);
    }
    if (this.__domElem != null) {
        this.__domElem.style.left = "";
        this.__domElem.style.top = "";
        this.__domTbody.parentNode.style.width = "";
        this.__w = -1;
        this.__h = -1;
        this.model.onReady(this.__renderItems, this);
    }
};

Menu.prototype.asyncRefresh = function() {
	this.__refreshNext = true;
};

Menu.prototype.setDisable = function(item, disable) {
    item.disabled = disable;
    if (item.__hElem != null) {
        exAddClass(item.__hElem, "mnDisable", !disable);
    }
};

Menu.prototype.render = function() {

    this.__domElem = makeElement(document.body, "div",
            "mnMenu" + (this.cssClass != null ? " " + this.cssClass : ""));
  	setEventHandler(this.__domElem, "onmousedown", __mnItemSnapEvt);
  	var holdThis = this;
  	addEventHandler(this.__domElem, "scroll", function(evt) { holdThis.__revive(); evt.cancelBubble = true; });
    this.__domTbody = makeLayoutTable(this.__domElem,
            "mnMenu" + (this.cssClass != null ? " " + this.cssClass : ""));
    var row = makeElement(this.__domTbody, "tr", "mnItem");

    makeElementNbSpd(row, "td", "mnA");
    makeElement(row, "td", "mnB", "Loading...");
    makeElementNbSpd(row, "td", "mnC");

    this.model.onReady(this.__renderItems, this);
};

/**
    item
        .isSep - seperator?
        .label - label
        .isInput - is this a text input?
        .validate - function(value) return error message for text input
        .sub - sub Menu class
        .act - click action function(evt) or if isInput, ok button function
            function(evt, value)

*/
Menu.prototype.__renderItems = function(items) {
    this.__domElem.style.visibility = "hidden";
    var i
    var kids = [];
    var dKids = this.__domTbody.childNodes;
    for (i = dKids.length - 1; i >= 0; i--) {
        var kE = dKids[i];
        if (kE.__item != null) {
            kids.push(kE.__item);
        }
        this.__domTbody.removeChild(kE);
    }
    for (i = 0; i < kids.length; i++) {
        this.__disposeItem(kids[i]);
    }
    for (i = 0; i < items.length; i++) {
        var item = items[i];
        var row;
         if (item.isSep) {
            row = makeElement(this.__domTbody, "tr", "mnSep");
            elementNoSelect(makeElementNbSpd(row, "td", "mnA"));
            var tdSep = elementNoSelect(makeElement(row, "td"));
            tdSep.setAttribute("colSpan", "2");
            makeElementNbSpd(tdSep, "div", "mnSep");
            if (item.labelElement) tdSep.appendChild(item.labelElement);
        } else if (item.isInput) {
            item._menu = this;
            // draw form input
            row = makeElement(this.__domTbody, "tr", "mnForm");
            row.__item = item;
            item.__hElem = row;
            setEventHandler(row, "onmouseover", __mnFormMouseOvr);
            elementNoSelect(makeElementNbSpd(row, "td", "mnA"));
            var tdInp = makeElement(row, "td", "mnB");
            tdInp.setAttribute("colSpan", "2");
            elementNoSelect(makeElement(tdInp, "label", "", item.label));
            var fTbody = makeLayoutTable(tdInp, "");
            var fRow = makeElement(fTbody, "tr");
            var inp = makeElement(makeElement(fRow, "td"),
                    "input", "text", "text", null, { name : "mnItem" + i});
            inp.setAttribute("autocomplete", "off");
            inp.__item = item;
            inp.__input = inp;
            if (item.inputValue) inp.value = item.inputValue;
            item.__inpElem = inp;
            if (item.isDate) {
            	var calElem = makeElementNbSpd(inp.parentNode, "span", "xfCal");
            	calElem.__item = item;
            	setEventHandler(calElem, "onclick", __mnCalClick);
            }
            
            setEventHandler(inp, "onmousedown", __mnInputClick);
            setEventHandler(inp, "onkeydown", __mnInputKeyDown);
            var btn = elementNoSelect(makeElementNbSpd(makeElement(fRow, "td"), "div", "okBtn"));
            setEventHandler(btn, "onmousedown", __mnButtonClick);
            btn.__item = item;
            btn.__input = inp;
            item.__btnElem = btn;
        } else {
            item._menu = this;
            if (item.disabled == null) item.disabled = false;
            var isDual = item.sub && item.act;
            row = makeElement(this.__domTbody, "tr", "mnItem");
            if (item.disabled) {
                SH.println("mnDisable");
                exAddClass(row, "mnDisable");
            }
            item.__hElem = row;
            if (!item.isNote) {
                setEventHandler(row, "onmouseover", __mnItemMouseOvr);
                setEventHandler(row, "onmouseout", __mnItemMouseOut);
            }
            row.__item = item;
            var iCol = elementNoSelect(makeElementNbSpd(row, "td",  isDual ? "mnA2" : "mnA"));
            if (item.icon != null) {
                iCol.className += " " + item.icon;
            }
            var labPar = elementNoSelect(makeElement(row, "td", isDual ? "mnB2" : "mnB", item.label != null ? item.label : null));
            if (item.labelElement != null) {
                labPar.appendChild(item.labelElement);
                if (item.labelElement.tagName.toUpperCase() != "A") {
                    setEventHandler(row, "onmousedown", __mnItemClick);
                } else {
                    setEventHandler(item.labelElement, "onmousedown", __mnItemAnchor);
                }
            } else if (!item.isNote) {
                 setEventHandler(row, "onmousedown", __mnItemClick);
            }
            if (item.sub != null) {
                elementNoSelect(makeElementNbSpd(row, "td", isDual ? "mnCArr2" : "mnCArr"));
            } else {
                elementNoSelect(makeElementNbSpd(row, "td", "mnC"));
            }
        }
    }
    this.__delayScreenBounds();
};

Menu.prototype.__delayScreenBounds = function() {
    var holdThis = this;
    window.setTimeout( function () { holdThis.__screenBounds(); }, 50);
};

Menu.prototype.__revive = function() {
    window.clearTimeout(this.__decTimer);
    this.__decTimer = null;
};

Menu.prototype.__decay = function() {
    if (this.__subMenu == null && this.__decTimer == null &&
            this.__domElem != null && this.__domElem.style.display != "none") {
        var holdThis = this;
        this.__decTimer = window.setTimeout( function () { holdThis.hide(); }, 1500);
    }
};

Menu.prototype.__showSubMenu = function(item, domItem) {
    var holdThis = this;
    var r = getViewBounds(domItem);
    item.sub.__parent = this;
    this.__subMTimer = window.setTimeout( function () {
            holdThis.__subMenu = item.sub;
            holdThis.__subMenu.__revive();
            holdThis.__subMenu.show(r.x + r.w + 2, r.y, holdThis, holdThis.__context);
            holdThis.__subMTimer = null;
        }, 150);
    domItem = null;
};

Menu.prototype.__over = function(domItem, evt, item) {
    this.__revive();
    if (item.disabled) {
        return;
    }
    if (this.__subMTimer != null) {
         window.clearTimeout(this.__subMTimer);
         this.__subMTimer = null;
    }

    var evtElem = getEventElement(evt);
    this.__lastItemOut();
    if (evtElem.className == "mnCArr2") {
        domItem.className = "mnItem mnHover2";
    } else {
        domItem.className = "mnItem mnHover";
    }
    this.__lastDomItem = domItem;
    var cName = evtElem.className;
    if (cName.indexOf("mnA2") < 0 && cName.indexOf("mnB2") < 0  && item.sub) {
        if (this.__subMenu != null) {
            if (this.__subMenu === item.sub) {
                this.__subMenu.__revive();
                return;
            } else {
                this.__subMenu.hide(true);
                this.__subMenu = null;
            }
        }
        this.__showSubMenu(item, domItem);
    } else if (this.__subMenu != null) {
        this.__subMenu.hide(true);
        this.__subMenu = null;
    }
};

Menu.prototype.__lastItemOut = function()  {
    if (this.__lastDomItem != null) {
        this.__lastDomItem.className = "mnItem";
        this.__lastDomItem = null;
    }
};

Menu.prototype.__out = function(domItem, evt, item) {
    if (item.disabled) {
        return;
    }
    if (!(this.__subMenu || this.__subMTimer)) this.__lastItemOut();
    this.__decay();
};

Menu.prototype.__click = function(domItem, evt, item) {
    var evtElem = getEventElement(evt);
    var cName = evtElem.className;

    if (!item.disabled &&
            (cName.indexOf("mnA2") >= 0 || cName.indexOf("mnB2") >= 0
            || !item.sub)) {
        if (item.act != null) {
            item.act(evt, this.__context);
        } else if (this.defaultAct != null) {
            this.defaultAct.call(item, evt, this.__context);
        } else {
            SH.print("No click for: ");
            SH.dump(item);
        }
        this.hide();
    }
    evt.cancelBubble = true;
};

Menu.prototype.__inputEnter = function(domItem, evt, item) {
    var val = domItem.__input.value;
    if (item.validate != null) {
        var msg = item.validate(val, this.__context);
        if (msg) {
            window.alert(msg);
            domItem.__input.focus();
            return;
        }
    }

    if (item.act != null) {
        domItem.__input.value = item.inputValue || "";
        item.act(evt, val, this.__context);
    } else {
        SH.print("No enter action for: ");
        SH.dump(item);
    }
    this.hide();
};


Menu.prototype.__screenBounds = function() {
    var vr = getVisibleBounds();
    // 2px buffer
    var bottom = vr.y + vr.h - Menu.GUTTER;
    var right = vr.x + vr.w - Menu.GUTTER;
    if (this.__h <= 0) {
        var r = getViewBounds(this.__domElem);
        if (r.w < this.minWidth) {
            this.__w = this.minWidth;
            this.__domTbody.parentNode.style.width = this.__w + "px";
        } else {
            this.__w = r.w;
        }
        this.__h = r.h;
    }
    if ((this.__h + this.__y) > bottom) {
    	
    	if (this.__h < Menu.MIN_SCROLL_HEIGHT) {
	        this.__y = bottom - this.__h;
	        if (this.__y < 0) this.__y = 0;
    	} else {
	        if ((this.__y + Menu.MIN_SCROLL_HEIGHT) > bottom) {
	        	this.__y = bottom - 100;
	        	if (this.__y < 0) this.__y = 0;
	        }
	        this.__h = bottom - this.__y;
	        this.__domElem.style.height = this.__h + "px";
	        if (SH.is_ie) {
	        	this.__domElem.style.width = (this.__w + 15) + "px";
	        	this.__domElem.style.overflowY = "auto";
	        } else {
	        	this.__domElem.style.overflow = "auto";
	        }
    	}
        
    }
    if ((this.__w + this.__x) > right) {
        this.__x = right - this.__w;
        if (this.__x < 0) this.__x = 0;
    }

    if (this.__parent && this.__parent.__lastDomItem) {
        var nR = new Rectangle(this.__x, this.__y, this.__w, this.__h);
        var pRi = getViewBounds(this.__parent.__lastDomItem);
        if (Rectangle.intersects(nR, pRi)) {
            this.__y = pRi.y + pRi.h;
            var underY = this.__h + this.__y;
            if (underY > bottom) {
                this.__y = pRi.y - this.__h;
                if (this.__y < 0) this.__y = 0;
            }
        }
    }
    this.__domElem.style.left = this.__x + "px";
    this.__domElem.style.top = this.__y + "px";
    this.__domElem.style.visibility = "";
    if (SH.is_ie) {
        hideSelects(this.__domElem, true);
    }
};

Menu.prototype.dispose = function() {
    this.hide();
    this.model.dispose();
    this.__domTbody = null;
    this.__domElem = null;
    this.__lastDomItem = null;
    this.__context = null;
    this.__subMenu = null;
    this.__parent = null;
};

Menu.prototype.__disposeItem = function(item) {
    if (item.dispose != null) item.dispose();
    if (item.isSep) {
    } else {
        item._menu = null;
        if (item.__hElem != null) {
            item.__hElem.__item = null;
            item.__hElem = null;
        }
        if (item.__btnElem  != null) {
            item.__btnElem.__item = null;
            item.__btnElem.__input = null;
            item.__btnElem = null;
        }
        if (item.__inpElem != null) {
            item.__inpElem.__item = null;
            item.__inpElem.__input = null;
            item.__inpElem = null;
        }
    }
    if (item.sub != null) {
        item.sub.dispose();
    }
};

/**
 *  items(optional) maybe an array, or a function that generates an array
 */
function MenuModel(items) {
    if (items != null) { 
    	if (typeof items == "function") {
    		this._itemGen = items;
    	} else this.items = items;
    }
    else this.items = [];
}

MenuModel.prototype.onReady = function(callback, menu) {
	if (this._itemGen) {
		this.__disposeItems();
		this.items = this._itemGen();
	}
    callback.apply(menu, [this.items]);
};

MenuModel.prototype.dispose = function() {
    this.__disposeItems();
};

MenuModel.prototype.__disposeItems = function() {
	if (this.items != null) {
        for (var i = 0; i < this.items.length; i++) {
            this._menu.__disposeItem(this.items[i]);
        }
        this.items = null;
    }
};

function __mnItemMouseOvr(evt) {
    this.__item._menu.__over(this, evt, this.__item);
}

function __mnItemMouseOut(evt) {
    this.__item._menu.__out(this, evt, this.__item);
}

function __mnFormMouseOvr(evt) {
    this.__item._menu.__revive();
}


function __mnInputClick(evt) {
    evt.cancelBubble = true;
    this.__item._menu.__revive();
}

function __mnButtonClick(evt) {
    evt.cancelBubble = true;
    this.__item._menu.__inputEnter(this, evt, this.__item);
}

function __mnItemSnapEvt(evt) {
    evt.cancelBubble = true;
}

function __mnItemClick(evt) {
    evt.cancelBubble = true;
    this.__item._menu.__click(this, evt, this.__item);
}

function __mnItemAnchor(evt) {
     evt.cancelBubble = true;
}

function __mnBodyClick(evt) {
    if (_activeMenu) {
        _activeMenu.hide();
    }
}

function __mnInputKeyDown(evt) {
    var code;
    if (evt.keyCode) code = evt.keyCode;
	else if (evt.which) code = evt.which;

	if (code == 13) { // return key
	    evt.cancelBubble = true;
	    this.__item._menu.__inputEnter(this, evt, this.__item);
	}
}

function __mnCalClick(evt) {
	this.__item._menu.__revive();
    calPopShow(evt, this, this.__item, false);
}

// listen to document clicks
addEventHandler(document, "mousedown", __mnBodyClick);
