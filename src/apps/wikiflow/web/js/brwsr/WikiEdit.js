/**
 * (c) 2006 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.WikiEdit
 *
 * WYSIWYG editing for brwsr.Wiki
 */


function wysEditInit(frameName) {
	var wev = WikiEdit.__wysIndex[frameName];
	if (wev) {
		delete WikiEdit.__wysIndex[frameName];
		wev.init();
	}
}

var WikiEdit = {

	__wysIndex : new Object(),
	
    __edCount : 0,

    createEditFrame : function(hParent, wikiView) {
       WikiEdit.__edCount++;
       var frameName = "edit" + WikiEdit.__edCount;
       var iframe = makeElement(hParent, "iframe", "wikiEd", null, null,
             {
                src : "../view-wf/blank.html",
                frameborder : "0",
                border : "0",
                application : "yes",
                name : frameName
             });
       /*iframe.style.position = "absolute";
       iframe.style.left = "0px";
       iframe.style.top = "0px";*/
       var wev = new WikiEditView(iframe, name, wikiView);

       // double init coverage
       //iframe.onload = function() { wev.init(); };
       //window.setTimeout( function() { wev.init(); }, 400);
       WikiEdit.__wysIndex[frameName] = wev;
       

       iframe = null; hParent = null;  // IE enclosure clean-up
       return wev;
    },


    ieCancelEvt : function() {
        return false;
    },

    ieBlockMacroEdit : function(evt) {
        var elem = getEventElement(evt);
        if (elem && (elem.className.indexOf("iten_emac") >= 0)) {
            stopEvent(evt);
            return false;
        }
    },

    _wsRegEx : new RegExp("[ \u00a0\\t\\n\\r]+", "mg"),

    cleanWhite : function(str) {
        return str.replace(WikiEdit._wsRegEx, " ");
    },

    macMenuClick : function(evt, context) {
        context.wev[this.meth](evt, context.elem);
    }
};


function WikiEditView(iframe, name, wikiView) {
    this.__iframe = iframe;
    this.name = name;
    this.wikiView = wikiView;
    this.hasLayout = false;
    this.__initDone = false;
    this.__initStart = 0;
    this.__hidden = false;
    this.macroMenu = new Menu(new MenuModel(
             [
                     {label : "Properties", icon : "mb_optIco", act : WikiEdit.macMenuClick, meth : "macProps"},
                     {isSep : true },
                     {label : "Remove", icon : "mb_remIco", act : WikiEdit.macMenuClick, meth : "macRemove"}
             ]));
}

WikiEditView.prototype.init = function() {
	
	if (this.__initDone) return;
	
    var holdThis = this;
    window.setTimeout(function() { holdThis.__init1(); }, 50);
};

WikiEditView.prototype.__init1 = function() {
	var holdThis = this;

    var dndType = new WikiEditWysDNDHand(this);
    var dnd = dndGetCanvas(document.body);
    dnd.addDNDType(dndType);
    dnd.makeDropTarget(this.__iframe, dndType.type);

    this.win = this.__iframe.contentWindow;
    this.doc = this.win.document;
    this.doc.designMode = "on";
	window.setTimeout(function() { holdThis.__init2(); }, 150);
};

WikiEditView.prototype.__init2 = function() {
	var holdThis = this;
	try {
        this.doc.execCommand("undo", false, null);
    }  catch (e) {
    	if (this.__initStart < 6) {
    		this.__initStart++;
    		window.setTimeout(function() { holdThis.__init2(); }, this.__initStart * 150);
    	} else {
    		alert("This editor is not supported by your browser");
    	}
    	return;
    }
    
	if (SH.is_ie) {
		this.__initStart = 0;
        // wait for IE to get into the edit mood
        window.setTimeout(function() { holdThis.__init3(); }, 250);
    } else {
        if (SH.is_gecko) {
            // TODO - link in Bold/Italic/Underline hotkeys
            this.__geckoInit();
        }
        this.__init3();
    }
    var keyFunc = function(evt) { holdThis.keyEvent(evt); };
    this.addEventHandler("keypress", keyFunc);
    this.addEventHandler("keydown", keyFunc);

    var selectFunc = function(evt) { holdThis.selectEvent(evt); };
    this.addEventHandler("select", selectFunc);
    this.addEventHandler("mouseup", selectFunc);
    this.addEventHandler("keyup", selectFunc);

    this.addEventHandler("contextmenu", function(evt) { return holdThis.menuEvent(evt); });
    
  	// TODO - detect paste, and filter HTML
};

WikiEditView.prototype.__init3 = function() {
	var holdThis = this;
    if (SH.is_ie) {
      	try {
            this.lastRange = this.doc.body.createTextRange();
        } catch (e) {
        	if (this.__initStart < 6) {
	    		this.__initStart++;
	    		window.setTimeout(function() { holdThis.__init3(); }, this.__initStart * 150);
	    	} else {
	    		 alert("Problem starting editor, error detail:\n" + e.description);
	    	}
	    	return;
        }
       	this.doc.body.onresizestart = WikiEdit.ieCancelEvt;
      	this.addEventHandler("beforeeditfocus", WikiEdit.ieBlockMacroEdit);
    }
    this.__initDone = true;
    if (this.article) {
        this.__setArticle();
    }
    if (SH.is_gecko) {
        window.setTimeout(function() {holdThis.win.focus();},10);
    }
};


WikiEditView.prototype.__geckoInit = function() {
    if (this.doc == null) return;
    this.doc.execCommand("useCSS", false, true);
    this.doc.execCommand("styleWithCSS", false, false);
    this.doc.execCommand("enableObjectResizing", false, false);
    this.doc.execCommand("enableInlineTableEditing", false, false);
};

WikiEditView.prototype.selectEvent = function(evt) {
    if (SH.is_ie) {
        try {
            this.lastRange = this.doc.selection.createRange().duplicate();
        } catch (e) {
        }
    }
    if (this.hasLayout) {
        var elem = this.getSelectElement();
        if (elem === this.doc.body || elem.parentNode === this.doc.body) {
            this.moveSelectToLayout();
        }
    }
   	DNDCanvas.stopAllDrags();
    Ephemeral.hideAll();
};

WikiEditView.prototype.moveSelectToLayout = function() {
    // check for layout table
    var tElem = getFirstChildElement(this.doc.body);
    var td;
    if (tElem == null || "table" != tElem.nodeName.toLowerCase()) {
    	var mkTble = true;
    	if (SH.is_gecko) {
    		var lstEl = Xml.lastElement(this.doc.body)
    		if (lstEl != null && "table" == lstEl.nodeName.toLowerCase()) {			
    			td = getFirstChildElement(getFirstChildElement(getFirstChildElement(lstEl)));
    			if (td) {
    				mkTble = false;  			
	    			var node = this.doc.body.firstChild;
	    			while (node != null && node != lstEl) {
	    				var cn = node;
				        node = node.nextSibling;
				        this.doc.body.removeChild(cn);
				        td.appendChild(cn);
				    }
    			}
    		}
    	}
    	if (mkTble) {
    		if (tElem != null) {
    			removeElementChildren(this.doc.body);
    		}
	        var layout = this.article.getAttribute("layout");
	        var tbody = makeLayoutTable(this.doc.body, "layout lo_" + layout);
	        var row = makeElement(tbody, "tr");
	        td = makeElement(row, "td", "column0");
	        makeElement(row, "td", "column1");
	        if (tElem != null) {
	            td.appendChild(tElem);
	        }
    	}
    } else {
        td = getFirstChildElement(getFirstChildElement(getFirstChildElement(tElem)));
    }

    // move select inside
    if (SH.is_ie) {
        this.lastRange.moveToElementText(td);
        this.lastRange.collapse(true);
        this.lastRange.duplicate().select();
    } else if (SH.is_gecko) {
        var sel = this.win.getSelection();
        if (!sel) {
            var holdThis = this;
            window.setTimeout(function() {holdThis.moveSelectToLayout()}, 10);
            return;
        }
        var rng = this.doc.createRange();
        rng.setStart(td, 0);
        sel.removeAllRanges();
        sel.addRange(rng);
    }
};

WikiEditView.prototype.moveSelectFirst = function() {
    if (this.hasLayout) {
        this.moveSelectToLayout();
    } else {
        if (SH.is_ie) {
            this.lastRange.moveToElementText(this.doc.body);
            this.lastRange.collapse(true);
            this.lastRange.duplicate().select();
        } else if (SH.is_gecko) {
            var sel = this.win.getSelection();
            if (!sel) {
                var holdThis = this;
                window.setTimeout(function() {holdThis.moveSelectFirst()}, 10);
                return;
            }
            var rng = this.doc.createRange();
            rng.setStart(this.doc.body, 0);
            sel.removeAllRanges();
            sel.addRange(rng);
        }
    }
};

WikiEditView.prototype.addEventHandler = function(type, handler) {

    // IE and Gecko
    if (SH.is_ie) {
        var holdThis = this;
        var onType = "on" + type;
        this.doc.attachEvent(onType, function() {
                handler(holdThis.win.event);
            });
    } else {
        this.doc.addEventListener(type, handler, false);
    }
};

WikiEditView.prototype.show = function() {
    this.__iframe.style.display = "";
    if (SH.is_gecko) {
        var holdThis = this;
        window.setTimeout(function(){ holdThis.__geckoInit(); }, 10);
    }
    this.__hidden = false;
};

WikiEditView.prototype.isHidden = function() {
    return this.__hidden;
};

WikiEditView.prototype.hide = function() {
    this.__iframe.style.display = "none";
    this.__hidden = true;
};

WikiEditView.prototype.__setArticle = function() {
    this.hasLayout = this.article.getAttribute("layout") ? true : false;
    this.setContent(this.article.getContent(true));
};


WikiEditView.prototype.setContent = function(markup) {
    if (this.article) {
    	exAddClass(this.doc.body, "layout", !this.hasLayout);
        var dest = document.createElement("div");
        var ctx = Wiki.createContext(this.wikiView, this.article);
        ctx.wev = this;
        Wiki.wikify(markup, dest, ctx);
        this.setHTML(dest.innerHTML);
        this.moveSelectFirst();
        dest = null;
    }
};

WikiEditView.prototype.setArticle = function(article) {
    this.article = article;
    if (this.__initDone) {
        this.__setArticle();
    }
};

WikiEditView.prototype.checkRange = function() {
	if (SH.is_ie) {
		try {
			if (this.doc.selection.type == "text") {
				var rng = this.doc.selection.createRange();
				if (!this.doc.body.createTextRange().inRange(rng))
					this.lastRange.select();
			}
		} catch (e) {
			this.lastRange.select();
		}
	}
};

WikiEditView.prototype.doCmd = function(cmd, val) {
    if (SH.is_gecko) this.win.focus();
    this.checkRange();
    this.doc.execCommand(cmd, false, val);
};

WikiEditView.prototype.getContent = function() {
    var markup = "";
    if (this.hasLayout) {
        var tElem = getFirstChildElement(this.doc.body);
        if ("table" == tElem.nodeName.toLowerCase()) {
            var tbody = getFirstChildElement(tElem);
            for (var row = getFirstChildElement(tbody); row != null; row = row.nextSibling) {
                if (row.nodeType == 1) {
                    for (var td = getFirstChildElement(row); td != null; td = td.nextSibling) {
                        if (td.nodeType == 1) {
                            if (markup != "") {
                                if (markup.substring(markup.length - 1) != "\n") markup += "\n";
                                markup += "++++\n";
                            }
                            markup += this.recurseNodes(td, [], "");
                        }
                    }
                }
            }
        }
    } else {
        markup = this.recurseNodes(this.doc.body, [], "");
    }
    //console.log(markup);
    return markup;
};

WikiEditView.prototype.menuEvent = function(evt) {
    if (SH.is_ie) {
        this.selectEvent(evt);
    }
    var elem = getEventElement(evt);
    if (elem && (elem.className.indexOf("iten_emac") >= 0 || elem.nodeName.toLowerCase() == "a")) {
        stopEvent(evt);
        this.macroMenu.setDisable(this.macroMenu.model.items[0], elem.nodeName.toLowerCase() == "a");
        var rect = getBounds(this.__iframe);
        rect.x -= this.win.scrollX ?
            this.win.scrollX : this.doc.documentElement.scrollLeft;
        rect.y -= this.win.scrollY ?
            this.win.scrollY : this.doc.documentElement.scrollTop;
        this.macroMenu.show(getMouseX(evt) + rect.x, getMouseY(evt) + rect.y, null, {wev:this, elem:elem});
        return false;
    }
    return true;
};

WikiEditView.prototype.keyEvent = function(evt) {
    var code;
    if (evt.keyCode) code = evt.keyCode;
	else if (evt.which) code = evt.which;
	if (code == 13) {
	    var elem = this.getSelectElement();
	    // cancel newline in macro
	    if (elem && elem.className.indexOf("iten_emac") >= 0) {
	        // TODO find a way to get IE onto an external new line
            stopEvent(evt);
            return;
        }
    }
};

WikiEditView.prototype.getSelectElement = function() {
    if (SH.is_gecko) {
        var elem = this.win.getSelection().anchorNode;
        if (elem && elem.nodeType != 1) elem = elem.parentNode;
        return elem;
    } else if (SH.is_ie) {
        return this.lastRange.parentElement();
    }
};

WikiEditView.prototype.selectAnElement = function(elem) {
    if (SH.is_ie) {
        var rng;
        if ("button".indexOf(elem.nodeName.toLowerCase()) >= 0) {
            rng = this.doc.body.createControlRange();
            rng.addElement(elem);
        } else {
            rng = this.doc.body.createTextRange();
            rng.moveToElementText(elem);
        }
        rng.select();
    } else if (SH.is_gecko) {
        var rng = this.doc.createRange();
        rng.selectNode(elem);
        var sel = this.win.getSelection();
        sel.removeAllRanges();
        sel.addRange(rng);
    }
};

WikiEditView.prototype.dispose = function() {
    var dnd = dndGetCanvas(document.body);
    dnd.disposeDropTarget(this.__iframe);
    dnd.removeDNDType(this.name);
    this.__iframe = null;
    if (this.macroMenu != null) this.macroMenu.dispose();
    this.doc = null;
};

WikiEditView.prototype.insertHTML = function(html) {
    var elem = this.getSelectElement();
    if (elem && elem.className.indexOf("iten_emac") >= 0) {
        // TODO - maybe re-arrange
        return;
    }
    if (SH.is_ie) {
        // TODO - fix IE undo
        var rng = this.lastRange;
        if (this.doc.body.createTextRange().inRange(rng))
        	rng.pasteHTML(html);
    } else {
        this.doc.execCommand('insertHtml', false, html);
    }
};

WikiEditView.prototype.insertMacro = function(macro) {
    var wexObj = WikiExUtil.getExt(macro.id);
    var elem = document.createElement("div");
    wexObj.wysRender(elem, this, macro);
    this.insertHTML(elem.innerHTML + "\u00a0");
    elem = null;
};

WikiEditView.prototype.setHTML = function(html) {
    this.doc.body.innerHTML = html;
};

WikiEditView.prototype.getHTML = function() {
    return this.doc.body.innerHTML;
};

WikiEditView.prototype.macProps = function(evt, elem) {
    var id = elem.getAttribute("extid");
    var wexObj = id ? WikiExUtil.getExt(id) : null;
    if (wexObj) {
        var diag = wexObj.wysPropDialog(elem.getAttribute("args"));
        diag.wexElem = elem;
        diag.show(getMouseX(evt), getMouseY(evt));
    }
};

WikiEditView.prototype.macRemove = function(evt, elem) {
    this.selectAnElement(elem);
    this.doCmd("delete");
    if (SH.is_gecko) {
        var holdThis = this;
        window.setTimeout(function() {holdThis.win.focus();},10);
    }
};

WikiEditView.prototype.recurseNodes = function(nn, stack, sibtxt) {
    var ss = "";
    if (nn != null) {
        if (nn.nodeType == 1) {
            var tag = nn.nodeName.toLowerCase();

            var inTable = arrayFind(stack, "table") >= 0;
            var hasKids = true;
            if (!inTable) {
	            switch (tag) {
	                case "br":
                        if (stack[stack.length - 1] == "li" && nn.nextSibling == null) {
                            ss += "\n ";
                        } else {
                            ss += "\n";
                        }
                        hasKids = false; break;

                    case "hr": ss += "----\n"; hasKids = false; break;
                    
                }
	        }
	        
	        switch (tag) {
	        	case "button":
                        var klass = nn.className;
                        if (klass.indexOf("iten_emac") == 0) {
                            var id = nn.getAttribute("extid");
                            var args = nn.getAttribute("args");
                            var wexObj = WikiExUtil.getExt(id);
                            ss += wexObj.markup(args) + " ";
                            hasKids = false;
                        }
                        break;

                case "img":
                    var wexObj = WikiExUtil.getExt("img");
                    ss += wexObj.markup(nn);
                    hasKids = false; break;
	        }
	        

            if (hasKids) {
                var pre = "";
                var post = "";
                var skipEmpty = false;
                var cleanWhite = false;
                var autoNewLines = false;
                switch (tag) {

                    case "h1": if (!inTable) {pre = "!"; post = "\n";} break;
                    case "h2": if (!inTable) {pre = "!!"; post = "\n";} break;
                    case "h3": if (!inTable) {pre = "!!!"; post = "\n";} break;
                    case "h4": if (!inTable) {pre = "!!!!"; post = "\n";} break;
                    case "h5": if (!inTable) {pre = "!!!!!"; post = "\n";} break;

                    case "strong":
                    case "b": pre = "''"; post = "''"; cleanWhite = true; skipEmpty = true; break;

                    case "em":
                    case "i": pre = "\\\\"; post = "\\\\"; cleanWhite = true; skipEmpty = true; break;

                    case "u": pre = "__"; post = "__"; cleanWhite = true; skipEmpty = true; break;

                    case "strike": pre = "=="; post = "=="; cleanWhite = true; skipEmpty = true; break;

                    case "a": pre = "[["; post = "]]"; cleanWhite = true; skipEmpty = true; break;

                    case "p": if (inTable) break; post = "\n\n"; break;

                    case "div": if (inTable) break;
                        var klass = nn.className;
                        if (klass.indexOf("float") == 0 && arrayFind(stack, "div_float") < 0) {
                            tag = "div_float";
                            var sub;
                            if (klass.indexOf("floatRight float_") == 0) {
                                sub = klass.substring(17);
                                pre = "}}}"; post = "}}}\n";
                            } else if (klass.indexOf("floatLeft float_") == 0) {
                                sub = klass.substring(16);
                                pre = "{{{"; post = "{{{\n";
                            }
                            if (sub) pre += sub + ":\n";
                            else pre += "\n";
                            autoNewLines = true;
                        } else {
                            post = "\n";
                        }
                        break;

                    case "li": if (inTable) break;
                        {
                            var ch = (stack[stack.length - 1] == "ol") ? "#" : "*";
	                        for (var ii = 0; ii < stack.length; ii++) {
	                            if (stack[ii] == "ol" || stack[ii] == "ul") pre += ch;
	                        }
	                    }
	                    if (arrayFind(stack, "li") >= 0) {
	                        if (Xml.nextSiblingElement(nn) != null) {
	                            post = "\n";
	                        }
	                    } else {
                            post = "\n";
                        }
                        break;

                    case "ol":
                    case "ul": if (inTable) break;
                        var ptag = stack[stack.length - 1];
                        if (ptag == "li") {
                            pre = "\n";
                        } else if ("ol ul".indexOf(ptag) < 0) {
                            post = "\n";
                        }
                        break;

                    case "table": if (inTable) break; autoNewLines = true; break;

                    case "td": if (inTable) { pre = "|"; cleanWhite = true; } break;

                    case "th": pre = "|!"; break;

                    case "tr": post = "|\n"; break;

                    case "blockquote": if (inTable) break;
                        if (arrayFind(stack, "blockquote") < 0) {
                            pre = "<<<\n"; post = "<<<\n"; autoNewLines = true;
                        }
                        break;

                    case "pre": if (inTable) break; pre = "%%%\n"; post = "%%%\n"; autoNewLines = true; break;
                }

                if (tag == "a") {
                    var url = Xml.stringForNode(nn);
                    // TODO - (label != url) == [attach[url|label]]
                    if (url.indexOf("://") >= 2) {
                        ss += url;
                    } else {
                        ss += pre + url + post;
                    }
                } else {

                    if (autoNewLines && sibtxt.substring(sibtxt.length - 1) != "\n") ss += "\n";
                    ss += pre;
                    var kidss = "";
	                for (var nk = nn.firstChild; nk != null; nk = nk.nextSibling) {
	                    if (nk.nodeType == 1) {
	                        kidss += this.recurseNodes(nk, stack.concat([tag]), kidss);
	                    } else if (nk.nodeType != 8 && " tr tbody table".indexOf(" " + tag) < 0) {
	                        kidss += WikiEdit.cleanWhite(nk.nodeValue);
	                    }
	                }
	                if (skipEmpty && kidss == "") return "";
	                if (cleanWhite) kidss = WikiEdit.cleanWhite(kidss);
                    if (autoNewLines && kidss.substring(kidss.length - 1) != "\n") kidss += "\n";
                    ss += kidss + post;
	            }
            }

        } else if (nn.nodeType != 8) { // if not comment
            ss = WikiEdit.cleanWhite(nn.nodeValue);
        }
    }
    return ss;
};


WikiEditWysDNDHand.prototype = new DNDTypeHandler();
WikiEditWysDNDHand.prototype.constructor = WikiEditWysDNDHand;

function WikiEditWysDNDHand(view) {
    this.type = view.name;
    this.view = view;
}

WikiEditWysDNDHand.prototype.canDrag = function(dragElem) {
    return false;
};

WikiEditWysDNDHand.prototype.dropTest = function(dropElem, dragElem) {
    // file drop
    var type = dragElem._dndType.type;
    if (type == "dndFile" || type == "dndWFActivity" || type == "dndWfAttr" || type == "dndEntForm") {
    	if (type == "dndWfAttr") {
    		if (!this.view.wikiView.isWorkzone || !this.view.wikiView.article.step) return false;
    	}
        if (SH.is_ie) {
            this.view.lastRange.select();
        } else {
            this.view.win.focus();
            // fake event caret not working, maybe isTrusted = true required?
            //doc.createEvent("MouseEvent");
        }
        return true;
    }
    return false;
};

WikiEditWysDNDHand.prototype.dropExec = function(dropElem, dragElem) {

    // file drop
    var macro = null;
    var wikiView = this.view.wikiView;
    var type = dragElem._dndType.type;
    if (type == "dndFile") {
        macro = {id:"", args:[]};
        var dragItem = dragElem._actElem.__item;

        var wUri = Uri.parent(wikiView.wiki.getUri());
        var uri = Uri.localize(wUri, dragItem.uri);
        var tpos = uri.indexOf("template/");
        if (wikiView.isWorkzone && tpos >= 0)
            uri = "{activity}" + uri.substring(tpos + 8);
        if (wikiView.isWorkzone && App.resolver.modelUri && uri.indexOf(App.resolver.modelUri) >= 0)
        	 uri = "{model}" + uri.substring(App.resolver.modelUri.length);

        var tag;
        var ext = Uri.ext(uri).toLowerCase();
        if (dragItem.allowsKids) {
        	macro.id = "attachEdit";
            macro.args[0] = uri + "/";
            macro.args[1] = Uri.name(uri);
        } else if (wikiView.isWorkzone && uri == "activities.kb") {
            macro.id = "kb";
            macro.args[0] = "{activity}";
            macro.args[1] = "Activity Wiki";
        } else if ("gif jpg jpeg png".indexOf(ext) >= 0) {
            macro.id = "img";
            macro.args[0] = uri;
        } else if ("swf mov avi wav mp3".indexOf(ext) >= 0 || uri.substring(uri.length - 11) == "_config.xml") {
            macro.id = "media";
            macro.args[0] = uri;
            macro.args[1] = "400x300";
        } else if (ext == "flow") {
            uri = Uri.parent(uri);
            if (uri == "") uri = wUri;
            if (wikiView.isWorkzone) macro.id = "launchSub";
            else macro.id = "launch";
            macro.args[0] = uri;
            macro.args[1] = Uri.name(uri);

        } else if (ext == "xfrm") {
            macro.id = "form";
            macro.args[0] = uri;
        } else {
            macro.id = "attachEdit";
            macro.args[0] = uri;
            macro.args[1] = Uri.name(uri);
        }
    } else if (type == "dndWFActivity") {
        var dragItem = dragElem._actElem.__item;
        macro = {id:"activity", args:[]};
        macro.args[0] = dragItem.node.getAttribute("id");
    }  else if (type == "dndWfAttr") {
    	if (!this.attMenu) {
    		this.attMenu = new Menu(new MenuModel(
	         [
	                 {label : "Input", icon : "mb_movIco", act : WikiEditWysDNDHand.attrInput},
	                 {label : "Output", icon : "mb_viewIco", act : WikiEditWysDNDHand.attrOutput},
	                 {isSep : true },
	                 {label : "Cancel", act : WikiEditWysDNDHand.attrCancel}
	         ]));
	        App.addDispose(this.attMenu);
    	}
    	var rect = getViewBounds(dragElem);
	    this.attMenu.show(rect.x + 5, rect.y + 5, null, { view : this.view, dragElem : dragElem});
		return;
    } else if (type == "dndEntForm") {
    	if (EntFormDropDNDType.getEntityRelation(dragElem) == "1") {
    		var formMenu = EntFormDropDNDType.getFormMenu(
    				EntFormDropDNDType.getEntityId(dragElem),
    				WikiEditWysDNDHand.entityFormDrop,
    				{label : "Cancel", act : WikiEditWysDNDHand.attrCancel});
    		var rect = getViewBounds(dragElem);
    		formMenu.show(rect.x + 5, rect.y + 5, null, { view : this.view, dragElem : dragElem});
    		return;
    	} else {
    		macro = {id:"entity", args:[EntFormDropDNDType.getEntityName(dragElem)]};
    	}
    }
    if (macro) this.view.insertMacro(macro);
};

WikiEditWysDNDHand.attrInput = function(evt, pair) {
	var macro = {id:"input", args:[WfAttrDNDType.getName(pair.dragElem)]};
	pair.view.insertMacro(macro);
};

WikiEditWysDNDHand.attrOutput = function(evt, pair) {
	var macro = {id:"output", args:[WfAttrDNDType.getName(pair.dragElem)]};
	pair.view.insertMacro(macro);
};

WikiEditWysDNDHand.entityFormDrop = function(evt, pair) {
	var macro = {id:"entity", args:[EntFormDropDNDType.getEntityName(pair.dragElem)]};
	macro.args.push(this.src);
	pair.view.insertMacro(macro);
};

WikiEditWysDNDHand.attrCancel = function() {
	return;
};
