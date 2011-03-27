/**
 * Lib: brwsr.Util
 * Browser GUI type utils
 */

var hiddenSelects = []; //hidden buffer

if (SH.is_ie) try { document.execCommand("BackgroundImageCache", false, true);} catch(err) {}

// DOM actual x, y coordinate abstracted function
function getBounds(domElem) {
    var w = 0, h = 0;
    if (SH.is_safari && domElem.nodeName == "TR"){
        var kids = domElem.childNodes;
        var i, n;
        for ( i = 0; i < kids.length; i++) {
            n = kids[i];
            if (n.nodeType == 1) break;
        }
        var cr = getBounds(n);
        var s = getSize(domElem);
        return new Rectangle(cr.x - 1, cr.y - 1, s.w, s.h);
    } else {
        var pElem = domElem;
        var xPos = pElem.offsetLeft;
        var yPos = pElem.offsetTop;
        while (pElem.offsetParent != null) {
            pElem = pElem.offsetParent;
            xPos += pElem.offsetLeft;
            yPos += pElem.offsetTop;
        }
        w = domElem.offsetWidth;
        h = domElem.offsetHeight;
        return new Rectangle(xPos, yPos, w, h);
    }
}


var itslGetBounds = getBounds;


function getSize(domElem) {
    if (SH.is_safari && domElem.nodeName == "TR") {
    	var kids = domElem.childNodes;
        var w = 0, h = 0;
        for (var i = 0; i < kids.length; i++) {
            var n = kids[i];
            if (n.nodeType == 1) {
                w += n.offsetWidth;
                if (h == 0) h = n.offsetHeight;
            }
        }
        return { w : w, h : h };
    }
    return { w : domElem.offsetWidth, h : domElem.offsetHeight };
}

function getLocalBounds(parElem, domElem) {
    var w = 0, h = 0;
    if (SH.is_safari && domElem.nodeName == "TR"){
        var kids = domElem.childNodes;
        var i, n;
        for ( i = 0; i < kids.length; i++) {
            n = kids[i];
            if (n.nodeType == 1) break;

        }
        var cr = getLocalBounds(parElem, n);
        var s = getSize(domElem);
        return new Rectangle(cr.x - 1, cr.y - 1, s.w, s.h);
    } else {
        var pElem = domElem;
        var xPos = pElem.offsetLeft;
        var yPos = pElem.offsetTop;
        while (pElem.offsetParent != null && pElem.offsetParent !== parElem) {
            pElem = pElem.offsetParent;
            xPos += pElem.offsetLeft;
            yPos += pElem.offsetTop;
        }
        w = domElem.offsetWidth;
        h = domElem.offsetHeight;
        return new Rectangle(xPos, yPos, w, h);
    }
}

// offsets scrolled ancestors
function getViewBounds(domElem) {
    var w = 0, h = 0;
    if (SH.is_safari && domElem.nodeName == "TR"){
        var kids = domElem.childNodes;
        var i, n;
        for ( i = 0; i < kids.length; i++) {
            n = kids[i];
            if (n.nodeType == 1) break;

        }
        var cr = getViewBounds(n);
        var s = getSize(domElem);
        return new Rectangle(cr.x - 1, cr.y - 1, s.w, s.h);
    } else {
        var pElem = domElem;
        var xPos = pElem.offsetLeft;
        var yPos = pElem.offsetTop;
        var bod = pElem.ownerDocument.body;
        while (pElem.offsetParent != null) {
            pElem = pElem.offsetParent;
            if (pElem !== bod) {
                xPos -= pElem.scrollLeft;
                yPos -= pElem.scrollTop;
            }
            xPos += pElem.offsetLeft;
            yPos += pElem.offsetTop;
        }
        w = domElem.offsetWidth;
        h = domElem.offsetHeight;
        return new Rectangle(xPos, yPos, w, h);
    }
}

function getPixel(str) {
    if (str == "") return 0;
    return parseInt(str);
}


function getVisibleBounds() {
    var x, y, w, h;
    if (SH.complyMode && !SH.is_opera) {
        x = window.scrollX ?
            window.scrollX : document.documentElement.scrollLeft;
        y = window.scrollY ?
            window.scrollY : document.documentElement.scrollTop;
        w = document.documentElement.clientWidth;
        h = document.documentElement.clientHeight;
    } else {
        x = window.scrollX ?
            window.scrollX : document.body.scrollLeft;
        y = window.scrollY ?
            window.scrollY : document.body.scrollTop;
        w = window.innerWidth ?
            window.innerWidth : document.body.clientWidth;
        h = window.innerHeight ?
            window.innerHeight : document.body.clientHeight;
    }
    return new Rectangle(x, y, w, h);
}

// DOMEvent mouse X-point locator
function getMouseX(evt) {

    // IE and Gecko
    return evt.pageX ? evt.pageX : evt.clientX + document.body.scrollLeft;
}

// DOMEvent mouse Y-point locator
function getMouseY(evt) {

    // IE and Gecko
    return evt.pageY ? evt.pageY : evt.clientY + document.body.scrollTop;
}

function getLocalMousePnt(evt) {
    if (SH.is_ie) {
        return new Point(evt.offsetX, evt.offsetY);
    }
    return new Point(evt.layerX, evt.layerY);
}


// DOMEvent Source DOMElement
function getEventElement(evt) {

    // IE and Gecko
    return evt.target ? getFullElement(evt.target) : evt.srcElement;
}


function createMouseEvent(target, button, offsetX, offsetY) {
	var rect = getBounds(target);
	return {
			srcElement: target,
			target: target,
			button: button + (SH.is_ie ? 1 : 0),
			offsetX: offsetX,
			offsetY: offsetY,
			layerX : offsetX,
			layerY : offsetY,
			pageX : rect.x + offsetX,
			pageY : rect.y + offsetY };
}

// Event System
function ieEventHandler() {
    var ev = window.event;
    var evts = this.evtHandlers[ev.type];
    var retVal;
    for (var i =0; i < evts.length; i++) {
        retVal = evts[i].call(this, ev);
    }
    return retVal;
}

function addEventHandler(domElem, type, handler, useCapture) {

    // IE and Gecko
    if (SH.is_ie) {
        var onType = "on" + type;
        var origHand = domElem[onType];
        domElem[onType] = ieEventHandler;
        var evts = null;
        if (domElem.evtHandlers == null) {
            domElem.evtHandlers = new Object();
        } else {
            evts = domElem.evtHandlers[type];
        }
        if (!evts) {
            evts = [];
            domElem.evtHandlers[type] = evts;
        }
        arrayAdd(evts, handler);
        if (origHand && origHand !== ieEventHandler) arrayAdd(evts, origHand);
    } else {
        domElem.addEventListener(type, handler, useCapture);
    }
}

function setEventHandler(domElem, onType, handler) {
    if (SH.is_ie) {
        domElem[onType] = function () {
            return handler.call(this, window.event);
            };
        domElem = null; // IE enclosure clean-up
    } else {
        domElem[onType] = handler;
    }
}

function stopEvent(evt) {
	if (SH.is_ie) {
		evt.cancelBubble = true;
		evt.returnValue = false;
	} else {
		evt.preventDefault();
		evt.stopPropagation();
	}
	return false;
}

function removeEventHandler(domElem, type, handler) {

    // IE and Gecko
    try {
        if (domElem.removeEventListener == null) {
            if (domElem.evtHandlers
                    && domElem.evtHandlers[type]) {
                domElem.evtHandlers[type] =
                        arrayRemove(domElem.evtHandlers[type], handler);
            }
        } else {
            domElem.removeEventListener(type, handler, false);
        }
    } catch (e) {
        // eat this
    }
}

// TextNode to surronding element tree loop
function getFullElement(node) {
    while (node.nodeType != 1) {
        node = node.parentNode;
    }
    return node;
}

function getElementText(domElem) {
    var txt = "";
    var node = domElem.firstChild;
    while (node != null) {
        txt += node.nodeValue;
        node = node.firstChild;
    }
    return txt;
}

// Child loop to find the first actual element
function getFirstChildElement(node) {

    // IE and Gecko
    node = node.firstChild;
    while (node != null && node.nodeType != 1) {
        node = node.nextSibling;
    }
    return node;
}

function replaceElementText(domElem, txt) {
    var node = domElem.firstChild;
    while (node != null) {
        var rn = node;
        node = node.nextSibling;
        if (rn.nodeType != 1) domElem.removeChild(rn);
    }
    var doc = domElem.ownerDocument;
    domElem.appendChild(doc.createTextNode(txt));
}

function removeElementChildren(domElem) {
    var node = domElem.firstChild;
    while (node != null) {
        domElem.removeChild(node);
        node = domElem.firstChild;
    }
}

/**
* isAncestor - called as a test, returns a boolean, true or false
* Usage: isAncestor(this.__hElem, childHandle);
* @argument domElem - the parent element to test to.
* @argument child - the child element to test, is an ancestor.
*/
function isAncestor(domElem, child) {
    var pElem = child.parentNode;
    while (pElem != null) {
        if (pElem === domElem) {
            return true;
        }
        pElem = pElem.parentNode;
    }
    return false;
}

// returns tbody
function makeLayoutTable(par, className, before) {

    var tble = makeElement(par, "table", className, null, before);
    tble.setAttribute("cellPadding", "0");
    tble.setAttribute("cellSpacing", "0");
    return makeElement(tble, "tbody");
}

/**
* makeElement - called everywhere, creates an element with various other attributes included such as inner text
* Usage: makeElement(this.__hElem, "div", "diagNotch", "this is my element text", null, {title : "Resize"});
* @argument par - the parent element to attach the new element to.
* @argument tag - new element tag name type (DIV, INPUT, etc).
* @argument className - Optional css class to use.
* @argument text - Optional, any desired text will show up inside the element created, or element is an input, the input type string
* @argument before - if not null, insert the new element BEFORE the 'before node' (insertBefore) instead of after it (appendChild)
* @argument attrs - Optional element attributes
*/
function makeElement(par, tag, className, text, before, attrs) {
    var elem;
    var doc = par ? par.ownerDocument : document;
    if (SH.is_ie && tag == "input") {
        elem = doc.createElement(
            "<input type=" + text + " name='" + attrs.name + "'" +
                (attrs.readonly ? " readonly='true'" : "") +
                (attrs.checked ? " checked" : "")  + ">");
    } else if (SH.is_ie && tag == "textarea") {
        elem = doc.createElement("<textarea name='" + attrs.name + "'>");
    } else if (SH.is_ie && tag == "select") {
        var html = "<select name='" + attrs.name + "'";
        if (attrs.size != null) {
            html += " size=" + attrs.size;
        }
        if (attrs.multiple != null) {
            html += " multiple";
        }
        elem = doc.createElement(html + ">");
    } else if (SH.is_ie && tag == "iframe") {
        elem = doc.createElement(
            "<iframe src=" + attrs.src + " name='" + attrs.name + "'>");
        elem.border = attrs.border;
        elem.frameBorder = attrs.frameborder;
        delete attrs["src"]; delete attrs["name"]; delete attrs["border"]; delete attrs["frameBorder"];
    } else if (SH.is_ie && tag == "form" && attrs) {
        elem = doc.createElement(
            "<form action='" + attrs.action + "'" +
                " enctype='" + attrs.enctype + "'" +
                " method='" + attrs.method + "'>");
    } else {
        elem = doc.createElement(tag);
        if (tag == "input" && text != null) {
            elem.setAttribute("type", text);
            elem.type = text;
        } else if (text != null) {
            elem.appendChild(doc.createTextNode(text));
        }
    }
    if (className != null) elem.className = className;
    if (attrs != null) {
        for (var n in attrs) {
            if (SH.is_ie) {
                if (n.substring(0,2) == "on") {
                    elem[n] = new Function(attrs[n]);
                    continue;
                }
            }
            elem.setAttribute(n, attrs[n]);
        }
    }
    if (par != null) {
        if (before != null) {
            par.insertBefore(elem, before);
        } else {
            par.appendChild(elem);
        }
    }
    return elem;
}

function noBreakString(str) {
  return str.replace(noBreakString.regexp, "\u00a0") ;
}
noBreakString.regexp =  new RegExp("[\\n\\r\\t\\f ]","mg");

/**
* makeElementNbSpd - called in making menus, etc, creates an element that has a non-breaking space as its text
* Usage: makeElementNbSpd(this.__hElem, "div", "diagNotch",  null, {title : "Resize"});
* @argument par - the parent element to attach the new element to.
* @argument tag - new element tag name type (DIV, INPUT, etc).
* @argument className - Optional css class to use.
* @argument before - passed to the makeElement function
* @argument attrs - Optional element attributes to pass on to the makeElement function
*/
function makeElementNbSpd(par, tag, className, before, attrs) {
    return makeElement(par, tag, className, "\u00a0", before, attrs);
}

function makeElementPng(par, src, w, h, className, before) {
  var imgElm;
  if (SH.is_ie) {
    if (className == null) className = "iepng";
    else className += " iepng";
    imgElm = makeElementNbSpd(par, "div", className, before);
    imgElm.style.width = w + "px";
    imgElm.style.height = h + "px";
    imgElm.style.filter =
      "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" +
      src + "',sizingMethod='scale')";
  } else {
    imgElm = makeElement(par, "img", className, null, before,
      { src : src, width : w, height : h });
  }
  return imgElm;
}

function drawBox(par, rect, classPrefix) {
    var div;
    div = makeElementNbSpd(par, "div", classPrefix + "V");
    div.style.left = rect.x + "px";
    div.style.top = rect.y + "px";
    div.style.height = rect.h + "px";
    div = makeElementNbSpd(par, "div", classPrefix + "V");
    div.style.left = (rect.x + rect.w) + "px";
    div.style.top = rect.y + "px";
    div.style.height = rect.h + "px";
    div = makeElementNbSpd(par, "div", classPrefix + "H");
    div.style.left = rect.x + "px";
    div.style.top = rect.y + "px";
    div.style.width = rect.w + "px";
    div = makeElementNbSpd(par, "div", classPrefix + "H");
    div.style.left = rect.x + "px";
    div.style.top = (rect.y + rect.h) + "px";
    div.style.width = rect.w + "px";
}

function elementNoSelect(elem) {

   if (SH.is_ie) elem.setAttribute("UNSELECTABLE", "on");
   else if (SH.is_gecko) elem.style.MozUserSelect = "none";
   else if (SH.is_khtml) elem.style.KhtmlUserSelect = "none";

   return elem;
}

function getSelectVal(sel) {
    return sel.options[sel.selectedIndex].value;
}

function getMultiSelectVal(sel) {
    var vals = [];
    var opts = sel.options;
    for (var i=0; i < opts.length; i++) {
        if (opts[i].selected) {
            vals[vals.length] = opts[i].value;
        }
    }
    return vals;
}

function getCheckVals(chk) {
	var vals = [];
	if (chk.length != null) {
		for (var i=0; i < chk.length; i++) {
			if (chk[i].checked) {
				vals[vals.length] = chk[i].value;
			}
		}
	} else {
		if (chk.checked) {
			vals[0] = chk.value;
		}
	}
	return vals;
}

function setElementText(elem, txt) {
    removeElementChildren(elem);
    elem.appendChild(document.createTextNode(txt));
}

function setSelectVal(sel, val) {
    for (var i=0; i < sel.options.length; ++i) {
        if (sel.options[i].value == val) {
            sel.options[i].selected = true;
            return;
        }
    }
}

function hideSelects(domElem, stayHidden) {

    // IE and Gecko
    var domRect = getBounds(domElem);

    var sels = document.getElementsByTagName("SELECT");
    for (var i = 0; i < sels.length; i++) {
        var sel = sels[i];
        if (Rectangle.intersects(domRect, getBounds(sel))
                && !isAncestor(domElem, sel)) {
            hiddenSelects[hiddenSelects.length] = sel;
            sel.style.visibility="hidden";
        } else if (!stayHidden) {
            for (var j = 0; j < hiddenSelects.length; j++) {
                if (hiddenSelects[j] == sel) {
                    hiddenSelects[j] = null;
                    sel.style.visibility="visible";
                    break;
                }
            }
        }
    }

}

function ieMoveCursorToEnd(inp) {
    var rng = inp.createTextRange();
    rng.collapse(false);
    rng.select();
}

function unHideSelects() {

    // IE and Gecko
    for (var i = 0; i < hiddenSelects.length; i++) {
        var sel = hiddenSelects[i];
        if (sel) {
            sel.style.visibility="visible";
        }
    }
    hiddenSelects = new Array();
}

function getHttpPath() {
    var loc = window.location;
    var port = (loc.port != "" && loc.port != "80" && loc.port != "443") ? ":" + loc.port : "";
    var path = loc.pathname;
    if (path.charAt(path.length - 1) != "/") path = Uri.parent(path);
    return loc.protocol + "//" + loc.hostname + port + path;
}

function exAddClass(hElem, clsName, remove) {
    var cls = hElem.className;
    var pos;
    pos = cls.indexOf(clsName);
    if (remove) {
        if (pos >= 0) {
            cls = cls.substring(0, pos) + cls.substring(pos + clsName.length);
        }
    } else if (pos < 0) {
        cls += " " + clsName;
    }
    hElem.className = cls;
}

function domSetCss(hElem, style) {
    if (SH.is_opera) hElem.setAttribute("style", style);
    else hElem.style.cssText = style;
}


function posRelIEFix(relElem) {

	// find first parent node with layout, but no position type;
	if (SH.is_ie) {
		var par = relElem.parentNode;
		while (par && par.nodeType == 1) {
			if (par.currentStyle.hasLayout) {
				if (par.currentStyle.position == "static") {
					par.style.position = "relative";
				} else {
					break;
				}
			}
			par = par.parentNode;
		}
	}
	return relElem;
}


function clearSelection() {

    // IE and Gecko
    try {
        if (document.selection) {
            document.selection.empty();
        } else {
            var sel = window.getSelection();
            sel.removeAllRanges();
        }
    } catch(e) {
    }
}


function classMatchDeep(parentNode, klass) {
    var matches = [];
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1 && kid.className == klass) {
            matches.push(kid);
        }
        var deep = classMatchDeep(kid, klass);
        for (var jj = 0; jj < deep.length; jj++) {
            matches.push(deep[jj]);
        }
    }
    return matches;
}

function classMatchOneDeep(parentNode, klass) {
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1 && kid.className == klass) {
           	return kid;
        }
        var deep = classMatchOneDeep(kid, klass);
        if (deep) return deep;
    }
    return null;
}

function styleString2Obj(str) {
	var pairs = str.split(';');
	var obj = new Object();
	for (var ii = 0; ii < pairs.length; ii++) {
		var nval = pairs[ii].split(':');
		obj[nval[0]] = nval[1];
	}
	return obj;
}

function styleObj2String(obj) {
	var pairs = [];
	for (var jj in obj) {
		pairs.push(jj + ":" + obj[jj]);
	}
	return pairs.join(";");
}

function styleInsertRule(cssSheet, selector, cssText, index) {
	if (SH.is_ie) {
		cssSheet.addRule(selector, cssText, index);
	} else {
		cssSheet.insertRule(selector + " { " + cssText + " } ", index);
	}
}
function styleGetRuleCount(cssSheet) {
	if (SH.is_ie) return cssSheet.rules.length;
	else return cssSheet.cssRules.length;
}

function styleGetRule(cssSheet, index) {
	if (SH.is_ie) {
		return cssSheet.rules[index];
	} else {
		return cssSheet.cssRules[index];
	}
}

var Util = {
	
	toggleDiv : function(par, label, opened) {
		var ctrl = elementNoSelect(makeElement(par, "div", "togCtrl"));
		makeElement(ctrl, "div", "label", label);
		var stat = makeElementNbSpd(ctrl, "div", "togStat" + (opened ? " close" : ""));
		setEventHandler(ctrl, "onmousedown", Util.__toggleClick);
		ctrl._togDiv = makeElement(par, "div", "togDiv");
		ctrl._stat = stat;
		if (!opened) {
			ctrl._togDiv.style.display = "none";
		}
		return ctrl._togDiv;
	},
	
	__toggleClick : function(evt) {
		var opened = this._togDiv.style.display != "none";
		this._togDiv.style.display = opened ? "none" : "";
		this._stat.className = "togStat" + (opened ? "" : " close");
	}
	
};


var Ephemeral = {
	
	// const
	INSIDE : 1,
	RIGHT : 2,
	
    // properties
    __hElem :       null,
    __timer :       null,
    __ephObjs :     [],

    // methods
    showAtTop :     function(alignElem, appendElem, inside, timeout) {
                        if (this.__hElem == null) this.render();
                        else this.revive();
                        removeElementChildren(this.__hElem);
                        this.__hElem.appendChild(appendElem);
                        var r;
                        if (alignElem != null) {
                            r = getViewBounds(alignElem);
                            var mr = getBounds(this.__hElem);
                            var ey = inside == Ephemeral.INSIDE ? (r.y + 1) : (r.y - mr.h);
                            var vbr = getVisibleBounds();
                            if (ey < vbr.y) ey = vbr.y;
                            this.__hElem.style.top = ey + "px";
                        } else {
                            r = getVisibleBounds();
                            this.__hElem.style.top = r.y + "px";
                        }
                        var lft = r.x;
                        switch (inside) {
                        	case Ephemeral.RIGHT:
                        		lft += r.w - 200;
                        		if (lft < 0) lft = 0;
                        		break;
                        	case Ephemeral.INSIDE:
                        		lft++;
                        		break;
                        }
                        this.__hElem.style.left = lft + "px";
                        this.__hElem.style.visibility = "";
                        this.decay(timeout);
                    },

	topMessage : 	function(alignElem, text) {
						this.showAtTop(alignElem, document.createTextNode(text));
					},
					
	insideMessage : function(alignElem, text, timeout) {
						this.showAtTop(alignElem, document.createTextNode(text), Ephemeral.INSIDE, timeout);
					},
					
    render :        function() {
                        this.__hElem = makeElement(document.body, "div", "ephemeral");
                        this.__hElem.style.visibility = "hidden";
                    },

    hide :          function() {
                        if (this.__hElem != null) {
                            this.__hElem.style.visibility = "hidden";
                        }
                    },

    decay :         function(timeout) {
                        var holdThis = this;
                        this.__timer = window.setTimeout(function() {
                                holdThis.hide();
                            }, timeout || 5000);
                    },

    revive :        function() {
                        window.clearTimeout(this.__timer);
                    },

    hideAll :       function(obj) {
                        var objs = this.__ephObjs;
                        this.hide();
                        this.__ephObjs = [];
                        for (var ii = 0; ii < objs.length; ii++) {
                            objs[ii].hide();
                        }
                    },

    register :      function(obj) {
                        arrayAdd(this.__ephObjs, obj);
                    },

    unregister :    function(obj) {
                        arrayRemoveStrict(this.__ephObjs, obj);
                    }
};

var H = {
    _create : function(tag, params) {
        var param = params[0];
        var elem;
        if (param != null && typeof(param) != "string") {
            var className = param["klass"];
            delete param["klass"];
            var type = null;
            if (tag == "input") {
                type = param["type"];
                delete param["type"];
            }
            elem = makeElement(null, tag, className, type, null, param);
        } else {
            elem = makeElement(null, tag, null, param);
        }
        for (var ii = 1; ii < params.length; ii++) {
            param = params[ii];
            if (typeof(param) == "string") {
                elem.appendChild(document.createTextNode(param));
            } else if (param != null) {
                elem.appendChild(param);
            }
        }
        return elem;
    },

    // html elements
    div : function () { return H._create("div", arguments); },
    span : function () { return H._create("span", arguments); },
    p : function () { return H._create("p", arguments); },
    b : function () { return H._create("b", arguments); },
    i : function () { return H._create("i", arguments); },
    a : function () { return H._create("a", arguments); },
    form : function () { return H._create("form", arguments); },
    br : function () { return H._create("br", arguments); },
    ul : function () { return H._create("ul", arguments); },
    ol : function () { return H._create("ol", arguments); },
    table : function () { return H._create("table", arguments); },
    tr : function () { return H._create("tr", arguments); },
    td : function () { return H._create("td", arguments); },
    tbody : function () { return H._create("tbody", arguments); },
    th : function () { return H._create("th", arguments); },
    textarea : function () { return H._create("textarea", arguments); },
    img : function () { return H._create("img", arguments); },
    li : function () { return H._create("li", arguments); },
    input : function () { return H._create("input", arguments); },
    iframe : function () { return H._create("iframe", arguments); },
    nbsp : "\u00a0",
    swf : function(uiParent, uri, id, wh) {
    	var oe;
      	if (SH.is_ie) {
            oe = document.createElement("object");
            oe.setAttribute("classid", "CLSID:d27cdb6e-ae6d-11cf-96b8-444553540000");
            oe.setAttribute("codeBase", "http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=8,0,0,0");
            if (wh) {
                oe.width = wh[0];
                oe.height = wh[1];
            }
            oe.id = id;
            oe.allowScriptAccess = "sameDomain";
            uiParent.appendChild(oe);
            
            // delay setting the movie or the display can get skewed 
            window.setTimeout(function() {
            	oe.movie = uri;
            	oe = null; // IE enclosure clean-up
            	}, 100);
            	
            uiParent = null; // IE enclosure clean-up

        } else {
            oe = document.createElement("embed");
            oe.type = "application/x-shockwave-flash";
            oe.id = id;
            oe.name = id;
            oe.src = uri;
            oe.allowScriptAccess = "sameDomain";
            if (wh) {
                oe.width = wh[0];
                oe.height = wh[1];
            }
            uiParent.appendChild(oe);
        }
        return oe;
    }
};

/* empty appearance stub */
function open_flash_chart_data() {
	return '{ "title":{"text":"Loading..."}, "bg_colour" : "#FFFFFF", "elements":[], "y_axis":{"colour":"#ffffff","grid-colour":"#ffffff", "labels" : {"labels":[]} }, "x_axis":{"colour":"#ffffff","grid-colour":"#ffffff" } }';
}
