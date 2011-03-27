/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.ComboBox
 * An enhanced <select/>
 */

function ComboBox(freeEntry, multiSelect, cssClass) {
    this.__menu = new Menu(null, ComboBox.__comboMenuPick, cssClass);
    this.__free = freeEntry;
    this.__selectedItems = [];
    this.__multi = multiSelect;
    this.style = null;
    this.cssClass = cssClass;
}

ComboBox.prototype.addOption = function(label, value, selected) {
    var itm = { label : label, value : value};
    this.__menu.model.items.push(itm);
    if (selected) {
        if (this.__hElem != null) {
            this.__setItem(itm, true);
        } else {
            this.__selectedItems.push(itm);
        }
    }
    this.__menu.refresh();
    return itm;
};

ComboBox.prototype.addSeparator = function() {
	var itm = {isSep:true};
	this.__menu.model.items.push(itm);
	return itm;
};

ComboBox.prototype.addLabelElementOption = function(lblElem, value, selected, iconClass) {
    var itm = { labelElement:lblElem, value:value, icon:iconClass};
    //if (SH.debug) {
    //    SH.println("CB added: " + getElementText(lblElem));
    //}
    lblElem = null;
    this.__menu.model.items.push(itm);
    if (selected) {
        if (this.__hElem != null) {
            this.__setItem(itm, true);
        } else {
            this.__selectedItems.push(itm);
        }
    }
    this.__menu.refresh();
    return itm;
};

ComboBox.prototype.removeOptionItem = function (item) {
    arrayRemoveStrict(this.__menu.model.items, item);
    arrayRemoveStrict(this.__selectedItems, item);
    item.labelElement = null;
    this.__menu.refresh();
};

ComboBox.prototype.render = function(hParent) {

    this.__hElem = makeElement(hParent, "a",
            "combo" + (this.cssClass != null ? " " + this.cssClass : ""),
            null, null, { href : "#select"});
    this.__hElem._combo = this;
    this.__hElem.onclick = ComboBox.__Aonclick;
    var holdThis = this;
    setEventHandler(this.__hElem, "onmousedown",
            function(evt) {
                this.focus();
                holdThis.__mouseDown(evt);
                return false;
            });
    hParent = null;

    var tbody = makeLayoutTable(this.__hElem,
            "combo" + (this.cssClass != null ? " " + this.cssClass : ""));
    this.__hWidget = makeElement(makeElement(tbody, "tr"), "td", "combo");

    //this.__hElem = elementNoSelect(makeElementNbSpd(hParent, "ins",
    //        SH.is_safari ? "comboSafari" : "combo"));
     // IE enclosure clean-up
    //this.__hWidget = makeElement(this.__hElem, "input", "combo",
    //        "text", null, { name : "__combo"});
    //this.__hWidget = makeElement(this.__hElem, "a", "combo",
    //            null, null, { href : "#select"});

    //if (SH.is_gecko) this.__hWidget.setAttribute("autocomplete", "off");
    //this.__hWidget._combo = this;
    setEventHandler(this.__hElem, "onfocus", ComboBox.__inputFocus);
    setEventHandler(this.__hElem, "onkeypress", ComboBox.__inputKeyDown);
    if (this.style != null && this.style != "") {
        if (SH.is_opera) this.__hWidget.setAttribute("style", this.style);
        else this.__hWidget.style.cssText = this.style;
    }

    if (this.__selectedItems.length == 0
            && this.__menu.model.items.length > 0) {
        this.__selectedItems.push(this.__menu.model.items[0]);
    }
    if (this.__selectedItems.length > 0) {
        if (this.__selectedItems[0].labelElement != null)
            this.__hWidget.appendChild(
                this.__selectedItems[0].labelElement.cloneNode(true));
        else
            this.__hWidget.appendChild(
                document.createTextNode(this.__selectedItems[0].label));
    } else {
        this.__hWidget.appendChild(document.createTextNode("\u00a0"));
    }
    this.__menu.minWidth = getBounds(this.__hElem).w - 4;
};

// verify = verify if the value actually changed before firing onchange
ComboBox.prototype.setValue = function(value, verify, noChange) {
    if (value instanceof Array) {
        // TODO implement
    } else  {
        //if (SH.debug) {
        //    SH.println("CB value set: " + value);
        //}
        var items = this.__menu.model.items;
        for (var i = 0 ; i < items.length; i++) {
            if (items[i].value == value) {
                this.__setItem(items[i], noChange, verify);
                break;
            }
        }
    }
};

ComboBox.prototype.getValue = function() {
	if (this.__selectedItems.length >= 1) {
		return this.__selectedItems[0].value;
	}
	return null;
};

ComboBox.prototype.__setItem = function(item, noChange, verify) {
    if (this.__multi) {
        // TODO implement toggle
        // TODO implement menu checks
    } else {
        if (this.__selectedItems.length == 1 &&
                this.__selectedItems[0].value == item.value) {
        	if (this.onselect != null && !noChange && !verify) {
		        this.onselect();
		    }
            return;
        }
        this.__selectedItems = [item];
        removeElementChildren(this.__hWidget);
        if (this.__selectedItems[0].labelElement != null)
            this.__hWidget.appendChild(
                this.__selectedItems[0].labelElement.cloneNode(true));
        else
            this.__hWidget.appendChild(
                document.createTextNode(this.__selectedItems[0].label));
    }
    if (this.onchange != null && !noChange) {
        this.onchange();
    }
};

ComboBox.prototype.__mouseDown = function(evt) {
    var b = getViewBounds(this.__hWidget);
    if (!this.readOnly) this.__menu.show(b.x + 1, b.y + b.h + 1, null, this);
    evt.cancelBubble = true;
};

ComboBox.prototype.remove = function() {
    if (this.__hElem && this.__hElem.parentNode != null)
        this.__hElem.parentNode.removeChild(this.__hElem);
    this.dispose();
};

ComboBox.prototype.dispose = function() {
    this.__menu.dispose();
    this.onchange = null;
    this.onfocus = null;
    this.__selectedItems = null;
    this.__menu = null;
    this.__hElem = null;
    this.__hWidget = null;
};


// this == item, context == ComboBox
ComboBox.__comboMenuPick = function(evt, context) {
    context.__setItem(this);
};

ComboBox.__inputFocus = function(evt) {
    var combo = this._combo;
    if (combo.onfocus != null) {
        combo.onfocus(evt);
    }
};


ComboBox.__inputKeyDown = function(evt) {
    var key = SH.is_ie ? evt.keyCode : evt.which;
	var keychar = String.fromCharCode(key);
	if (key == 0) {
	    return true;
	}
	var combo = this._combo;
	var b = getViewBounds(combo.__hWidget);
    if (!this.readOnly) combo.__menu.show(b.x + 1, b.y + b.h + 1, null, combo);
    evt.cancelBubble = true;
    return false;
};

ComboBox.__Aonclick = function() {
    return false;
};
