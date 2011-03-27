var ProcOutline = {
	count : 0, lastOutline : null, teamRoaster : null, dirty : false,
	
	dialog : function(procCan) {
				
		var diag = new Dialog((ActivityTree.isCourse ? "Course" : "Process") + " Outliner", true);
		diag.initHelp(App.chromeHelp);
    	diag.render(document.body);
    	
    	var poev = ProcOutline.__renderView(diag.contentElement, procCan, "Save to Chart", 620, 420);
    	poev.onSaved = function() {
				diag.destroy();
			};
    	
    	var offset = (ProcOutline.count * 5);
    	
    	diag.show(190, 100 + offset);
    	diag.linkResize(poev);
	},
	
	panelized : function(procCan, panel, savLabel, layout) {
		if (!savLabel) savLabel = "Save & Start";
		var rect = getBounds(panel.contentElement);
		var poev = ProcOutline.__renderView(panel.contentElement, procCan, savLabel, rect.w - 10, rect.h - 30, layout);
		panel.linkResize(poev);
		procCan._poev = poev;
		
		// other save options
		poev.saveAction = function() {
				var diag = xfTemplateDialog(
					"App Save Options", true, document.body, procCan.xfrm, "save-meet", null, true, null, App.chromeHelp);
				diag.showModal(300, 150);
			};
	},
	
	// called from flow.xfrm
	meetSave : function(procCan, xfMod) {
		procCan._poev.saveToChart();
		ActivityTree.planning.saveToDoc(procCan.getModelDocument());
		var meetRoot = xfMod.duplicateNode("instance('meet')", ".");
		ProcOutline.initDraft(procCan, meetRoot, xfMod);
		procCan.commitSave();
		ProcOutline.dirty = false;
		location.href = "../act/meetStat?meet=" + Uri.escape(Uri.parent(App.activeFlow));
	},
	
	// <meet> doc segment populator
	initDraft : function(procCan, docRoot, xfMod) {
		xfMod.setValue("activity", App.activeActivityId, docRoot);
		xfMod.setValue("body", procCan._poev.getHTML(), docRoot);
		
		var oldMems = xfMod.selectNodeList("member", docRoot);
		if (oldMems != null) {
			for (var ii = 0; ii < oldMems.length; ii++) {
				xfMod.destroyNode(".", oldMems[ii]);
			}
		}
		var tmr = ProcOutline.teamRoaster;
		if (tmr) {
			var dup;
			for (var uid in tmr.members) {
				dup = xfMod.duplicateNode("instance('pal')/member", ".", null, docRoot);
				xfMod.setValue(".", uid, dup);
			}
		}
		ProcOutline.dirty = false;
		//console.dirxml(docRoot);
	},
	
	buildClick : function(evt) {
		ProcOutline.dialog(this._procCan);
	},
	
	macMenuClick : function(evt, context) {
        context.poev[this.meth](evt, context.elem);
    },
    
    isDirty : function() {
    	return ProcOutline.dirty;
    },
    
    __renderView : function(hElem, procCan, saveLabel, width, height, layout) {
    	
   		if (!ProcOutline.macroMenu) {
			var procFunc = function(evt, poev) {
					poev.insertMacro({id:'launch', args:[this.uri, Uri.name(this.uri)]}); 
				};
			ProcOutline.macroMenu = new Menu(new MenuModel([
	            { label : "Approval Loop" , act : function(evt, poev) { poev.insertMacro({id:'loop', args:['Approval']}); } },
	            { label : "Add Process Launch", sub : new Menu(new TNavProcMenuModel("activeProcesses", procFunc), procFunc)},
	            { isSep : true},
	            { label : "Attach Input Document" , act : function(evt, poev) { poev.insertMacro({id:'input', args:['Document']}); } },
	            { isSep : true},
	            { label : "Role" , act : function(evt, poev) { poev.insertMacro({id:'role',  args:['New Role']}); } }
	            ]));
		}
		
		if (SH.is_ie) hElem.style.overflow = "hidden";
		
    	var tBar = makeElement(hElem, "div", "pmOutTbar");
    	var pDiv = makeElement(hElem, "div", "pmOutline");
    	WikiEdit.__edCount++;
    	var frameName = "edit" + WikiEdit.__edCount;
    	var ifElem = makeElement(pDiv, "iframe", "pmOutline", null, null,
		         	{
		            src : "../view-wf/blank-out.html",
		            frameborder : "0",
		            border : "0",
		            application : "yes",
                	name : frameName
		         	} );
    	
    	ifElem.style.width = width + "px";
    	ifElem.style.height = height + "px";
    	
    	var poev = new ProcOutEditView(procCan, ifElem, saveLabel,layout);
    	WikiEdit.__wysIndex[frameName] = poev;
    	
    	poev.drawToolbar(tBar);
    	return poev;
    }
    
};


function ProcOutEditView(procCan, iframe, saveLabel,layout) {
    this.__iframe = iframe;
    this.__initDone = false;
    this.__initStart = 0;
    this.__hidden = false;
    this.__procCan = procCan;
    this.__saveLabel = saveLabel;
    this.hasLayout = layout;
    this.macroMenu = new Menu(new MenuModel(
         [
                 {label : "Properties", icon : "mb_optIco", act : ProcOutline.macMenuClick, meth : "macProps"},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : ProcOutline.macMenuClick, meth : "macRemove"}
         ]));
}

ProcOutEditView.prototype.drawToolbar = function(parElement) {
	this.editBar = parElement;
	var holdThis = this;
	var link;
	link =              makeElement(this.editBar, "div", "wikiSaveBtn", this.__saveLabel);
	link.style.width = "84px";
	WikiView.initEditLink(link, function() { holdThis.saveAction(); return false; });
	link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edClean", null, { title : "Clean Formatting" });
	WikiView.initEditLink(link, function() { holdThis.cleaner(); return false; });
	link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edUlist", null, { title : "Unordered List" });
    WikiView.initEditLink(link, function() { holdThis.edUlist(); return false; });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edOdent", null, { title : "Outdent" });
    WikiView.initEditLink(link, function() { holdThis.edOdent(); return false; });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edIdent", null, { title : "Indent" });
    WikiView.initEditLink(link, function() { holdThis.edIdent(); return false; });
    
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edPara", null, { title : "Toggle Parallel" });
    WikiView.initEditLink(link, function() { holdThis.togglePara(); });
    
    if (ActivityTree.isMeet) {
    	var dateMenu = new Menu(new MenuModel([
    			{label : "Due Date", isInput : true, isDate : true,
    				inputValue : DateUtil.toLocaleShort(new Date(), false),
    				getTime : function() { return DateUtil.parseLocaleShort(this.__inpElem.value); },
    				setTime : function(dstr) {
    					this._menu.__revive();
    					this.__inpElem.value = dstr; 
    				},
    				act : function(evt, val) {
    					holdThis.setTime(val);
    				} }]));
    			
    	link =       	makeElementNbSpd(this.editBar, "div", "wikEdBtn edDue", null, { title : "Due Date" });
    	WikiView.initEditLink(link, function() {
    				var rect = getBounds(this);
	            	dateMenu.show(rect.x + 1, rect.y + rect.h + 1, null, holdThis); });
    }
    
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edTags", null, { title : "Macro Menu" });
    WikiView.initEditLink(link, function() {
	                    var rect = getBounds(this);
	                    ProcOutline.macroMenu.show(rect.x + 1, rect.y + rect.h + 1, null, holdThis); });

	parElement = null; link = null; // IE enclosure clean-up
};

ProcOutEditView.prototype.saveAction = function() {
	this.saveToChart();
};

ProcOutEditView.prototype.init = function() {
	if (this.__initDone) return;
	
    var holdThis = this;
    window.setTimeout(function() { holdThis.__init1(); }, 20);
};

ProcOutEditView.prototype.__init1 = function() {
	if (this.__initDone) return;
	
    var holdThis = this;    
    var dndType = new ProcOutDNDHand(this);
    var dnd = dndGetCanvas(document.body);
    dnd.addDNDType(dndType);
    dnd.makeDropTarget(this.__iframe, dndType.type);

    this.win = this.__iframe.contentWindow;
    this.doc = this.win.document;
    this.doc.designMode = "on";
    
    window.setTimeout(function() { holdThis.__init2(); }, 100);
};

ProcOutEditView.prototype.__init2 = function() {
	try {
        this.doc.execCommand("undo", false, null);
    }  catch (e) {
    	if (this.__initStart < 3) {
    		this.__initStart++;
    		window.setTimeout(function() { holdThis.__init2(); }, this.__initStart * 100);
    	} else {
    		alert("This editor is not supported on your browser");
    	}
    	return;
    }
    var holdThis = this;
    
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

ProcOutEditView.prototype.__init3 = function() {
	var holdThis = this;
    if (SH.is_ie) {
      	try {
            this.lastRange = this.doc.body.createTextRange();
        } catch (e) {
        	if (this.__initStart < 3) {
	    		this.__initStart++;
	    		window.setTimeout(function() { holdThis.__init3(); }, this.__initStart * 100);
	    	} else {
	    		 alert("Problem starting editor, error detail:\n" + e.description);
	    	}
	    	return;
        }
       	this.doc.body.onresizestart = WikiEdit.ieCancelEvt;
      	this.addEventHandler("beforeeditfocus", WikiEdit.ieBlockMacroEdit);
    }
    this.__initDone = true;
    
    if (ProcOutline.meetDraft) {
    	
    	var xmlHttp = XMLBuilder.getXMLHTTP();
        var async = false;
        xmlHttp.open("GET", "../fil" + ProcOutline.meetDraft + "?" + (new Date()).getTime(), async);
        xmlHttp.send("");
        var docTxt = xmlHttp.responseText;
        var spos = docTxt.indexOf("<!--#start_meet-->");
        var epos = docTxt.indexOf("<!--#end_meet-->");
        if (spos > 0 && epos > 0) {
        	this.insertHTML(docTxt.substring(spos + 18, epos + 16));
        }
    	this.moveSelectFirst();
    } else if (ProcOutline.meetLDraft) {
    	var xmlHttp = XMLBuilder.getXMLHTTP();
        var async = false;
        xmlHttp.open("GET", "../fil" + ProcOutline.meetLDraft, async);
        xmlHttp.send("");
        var docTxt = xmlHttp.responseText;
        var noteRx = new RegExp("class=[\"]?row2[\"]?>([\\S\\s]*)</td>", "im");
        var mm = noteRx.exec(docTxt);
        var noteStr = mm ? mm[1] : "";
        
       	var agendRx = new RegExp("class=[\"]?row0[\"]?>([\\S\\s]*?)</td>", "im");
        mm = agendRx.exec(docTxt);
        var agendStr = mm ? mm[1] : "";
        
        var td = this.__makeLayout();
        if (td) {
        	Xml.firstElement(Xml.nextSiblingElement(td.parentNode)).innerHTML = noteStr;
        	Xml.firstElement(Xml.prevSiblingElement(td.parentNode)).innerHTML = agendStr;
        	td.innerHTML = "<ul><li>Start here</li></ul>";
        	this.moveSelectFirst();
        } else {
        	this.moveSelectFirst();
        	this.insertHTML("<ul><li>Start here</li></ul>");
        }
        
    } else {
    
	    this.moveSelectFirst();
	 	
	 	if (!this.hasOutline && ProcOutline.lastOutline) this.insertHTML(ProcOutline.lastOutline);
	    else this.insertHTML(this.hasLayout ? "<ul><li>Start here</li></ul>" :
	    	"<ul><li>Start here</li></ul>");
    }

    if (SH.is_gecko) {
        var holdThis = this;
        window.setTimeout(function() {holdThis.win.focus();},10);
    }
};


ProcOutEditView.prototype.__geckoInit = function() {
    if (this.doc == null) return;
    this.doc.execCommand("useCSS", false, true);
    this.doc.execCommand("styleWithCSS", false, false);
    this.doc.execCommand("enableObjectResizing", false, false);
    this.doc.execCommand("enableInlineTableEditing", false, false);
};

ProcOutEditView.prototype.selectEvent = function(evt) {
    if (SH.is_ie) {
        try {
            this.lastRange = this.doc.selection.createRange().duplicate();
        } catch (e) {
        }
    }
    DNDCanvas.stopAllDrags();
    Ephemeral.hideAll();
};

ProcOutEditView.prototype.getLayout = function() {
	return this.hasLayout;
};

ProcOutEditView.prototype.moveSelectToLayout = function() {
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
			td = this.__makeLayout();
	        if (tElem != null) {
	            td.appendChild(tElem);
	        } else {
	        	makeElementNbSpd(td, "div");
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

ProcOutEditView.prototype.__makeLayout = function() {
	if (!this.hasLayout) return null;
	var layout = this.getLayout();
    var tbody = makeLayoutTable(this.doc.body, "mlayout lo_" + layout);
    
	// defaut 4 rows for now
	makeElementNbSpd(makeElement(makeElement(tbody, "tr"), "td", "row3"), "div");
    makeElementNbSpd(makeElement(makeElement(tbody, "tr"), "td", "row0"), "div");
    var td = makeElement(makeElement(tbody, "tr"), "td", "row1");
   	makeElementNbSpd(makeElement(makeElement(tbody, "tr"), "td", "row2"), "div");
   	return td;
};

ProcOutEditView.prototype.moveSelectFirst = function() {
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
	            window.setTimeout(function() { holdThis.moveSelectFirst(); }, 10);
	            return;
	        }
	        var rng = this.doc.createRange();
	        rng.setStart(this.doc.body, 0);
	        sel.removeAllRanges();
	        sel.addRange(rng);
	    }
    }
};

ProcOutEditView.prototype.addEventHandler = function(type, handler) {

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

ProcOutEditView.prototype.show = function() {
    this.__iframe.style.display = "";
    if (SH.is_gecko) {
        var holdThis = this;
        window.setTimeout(function(){ holdThis.__geckoInit(); }, 10);
    }
    this.__hidden = false;
};

ProcOutEditView.prototype.isHidden = function() {
    return this.__hidden;
};

ProcOutEditView.prototype.hide = function() {
    this.__iframe.style.display = "none";
    this.__hidden = true;
};

ProcOutEditView.prototype.doCmd = function(cmd, val) {
	this.checkRange();
    if (SH.is_gecko) this.win.focus();
    this.doc.execCommand(cmd, false, val);
};

ProcOutEditView.prototype.menuEvent = function(evt) {
    if (SH.is_ie) {
        this.selectEvent(evt);
    }
    var elem = getEventElement(evt);
    if (elem && elem.className.indexOf("iten_emac") >= 0) {
        stopEvent(evt);
        var rect = getBounds(this.__iframe);
        rect.x -= this.win.scrollX ?
            this.win.scrollX : this.doc.documentElement.scrollLeft;
        rect.y -= this.win.scrollY ?
            this.win.scrollY : this.doc.documentElement.scrollTop;
        
        this.macroMenu.setDisable(this.macroMenu.model.items[0], "role dueDate input".indexOf(elem.getAttribute("extid")) < 0);
        this.macroMenu.show(getMouseX(evt) + rect.x, getMouseY(evt) + rect.y, null, {poev:this, elem:elem});
        return false;
    }
    return true;
};

ProcOutEditView.prototype.keyEvent = function(evt) {
	ProcOutline.dirty = true;
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
    } else if (SH.is_ie && code == 9) { // TAB key in IE
    	if (evt.shiftKey) this.edOdent();
    	else this.edIdent();
    	stopEvent(evt);
    }
};


ProcOutEditView.prototype.getSelectElement = function() {
    if (SH.is_gecko) {
        var elem = this.win.getSelection().anchorNode;
        if (elem && elem.nodeType != 1) elem = elem.parentNode;
        return elem;
    } else if (SH.is_ie) {
        return this.lastRange.parentElement();
    }
};

ProcOutEditView.prototype.selectAnElement = function(elem, collapse) {
    if (SH.is_ie) {
        var rng;
        if ("button".indexOf(elem.nodeName.toLowerCase()) >= 0) {
            rng = this.doc.body.createControlRange();
            rng.addElement(elem);
        } else {
            rng = this.doc.body.createTextRange();
            rng.moveToElementText(elem);
            if (collapse) rng.collapse();
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

ProcOutEditView.prototype.dispose = function() {
    this.__iframe = null;
    this.doc = null;
    this.editBar = null;
    this.__appLoopMasters = null;
    this.__stpMaster = null;
	this.__notMaster = null;
	this.__pthMaster = null;
	this.__grpMaster = null;
};

ProcOutEditView.prototype.insertHTML = function(html) {
    var elem = this.getSelectElement();
    if (elem && elem.className.indexOf("iten_emac") >= 0) {
        // TODO - maybe re-arrange
        return;
    }
    if (SH.is_ie) {
        // TODO - fix IE undo
        var rng = this.lastRange;
        if (this.doc.body.createTextRange().inRange(rng))
        	res = rng.pasteHTML(html);
    } else {
        this.doc.execCommand('insertHtml', false, html);
        if (SH.is_gecko) {
	        var holdThis = this;
	        window.setTimeout(function() {holdThis.win.focus();},10);
	      //  res = this.win.getSelection().anchorNode;   
	    }
    }
};

ProcOutEditView.prototype.checkRange = function() {
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

ProcOutEditView.prototype.setHTML = function(html) {
    this.doc.body.innerHTML = html;
};

ProcOutEditView.prototype.getHTML = function() {
    return this.doc.body.innerHTML;
};

ProcOutEditView.prototype.edUlist = function() {
	var cmd = "insertunorderedlist";
	if (this.hasLayout) {
		var pElem = this.getSelectElement();
		while (pElem.offsetParent != null) {
            if (__poevTag(pElem) == "td" && " row0row2row3".indexOf(pElem.className) > 0) {
            	cmd = "insertorderedlist";
            	break;
            }
            pElem = pElem.offsetParent;
		}
	}
    this.doCmd(cmd);
};

ProcOutEditView.prototype.edOdent = function() {
	this.edListCmd("outdent");
};

ProcOutEditView.prototype.edListCmd = function(op) {
	var elem = this.getSelectElement();
	var lnm = elem ? elem.nodeName.toLowerCase() : "";
	if (SH.is_gecko && lnm == "li") {
		if (this.win.getSelection().isCollapsed)
			this.selectAnElement(elem);
			
		ProcOutEditView.__preDentSeqChecks(elem, op);
		this.doCmd(op);
		return;
	} else if (SH.is_ie  &&  lnm == "li") {
		ProcOutEditView.__preDentSeqChecks(elem, op);
    	this.doCmd(op);
    	return;
	} else if (SH.is_ie  && (lnm == "ul" || lnm == "ol")) {
		this.doCmd(op);
		return;
	}
	while (elem && elem.nodeName.toLowerCase() != "li") {
		elem = elem.parentNode;
	}
	if (elem) {
		this.selectAnElement(elem, true);
		ProcOutEditView.__preDentSeqChecks(elem, op);
    	this.doCmd(op);
	}
};

ProcOutEditView.prototype.edDueDate = function(btnElem) {
	calPopShow(null, btnElem, this, false);
};

// Due date call back
ProcOutEditView.prototype.setTime = function(str) {
	var dt = DateUtil.parseLocaleShort(str);
	this._lastDue = dt;
	calPopHide();
	this.insertMacro({id:"dueDate", args:[str] });
};

// Due date call back
ProcOutEditView.prototype.getTime = function() {
	if (this._lastDue == null) this._lastDue = new Date();
	return this._lastDue;
};

ProcOutEditView.__preDentSeqChecks = function (elem, op) {
	if (op == "indent") {		
		var prev = Xml.prevSiblingElement(elem);
	    if (prev && prev.nodeName.toLowerCase() == "li") exAddClass(prev, "procPar", true);
	}
};

ProcOutEditView.prototype.edIdent = function() {
	this.edListCmd("indent");
};


ProcOutEditView.prototype.togglePara = function() {
	var elem = this.getSelectElement();
	while (elem && elem.nodeName.toLowerCase() != "li") {
		elem = elem.parentNode;
	}
	if (elem) {
		var nxt =  Xml.nextSiblingElement(elem);
		if (!nxt || nxt.nodeName.toLowerCase() == "li") {
			exAddClass(elem, "procPar", elem.className.indexOf("procPar") >= 0);
		}
	}
};

ProcOutEditView.prototype.insertMacro = function(macro) {
    var parent = document.createElement("div");
    var elem = makeElement(parent, "button", "iten_emac", macro.args[macro.args.length-1]);
    elementNoSelect(makeElementNbSpd(elem, "div", "badge em_" + macro.id));
    elem.setAttribute("extid", macro.id);
    elem.setAttribute("args", macro.args.join("|"));
    this.insertHTML(parent.innerHTML + "\u00a0");
    parent = null; elem = null;
};

var ProcOut_role = {
    id : "role",
    propLabels : ["Role"]
};
objectExtend(ProcOut_role, WikiExCommon);


var ProcOut_input = {
    id : "input",
    propLabels : ["Label"]
};
objectExtend(ProcOut_input, WikiExCommon);


var ProcOut_dueDate = {
    id : "dueDate",
    propLabels : ["Due Date"],
    
    wysPropsRenderRow : function(tbody, argIdx, label, value) {
        var row = makeElement(tbody, "tr");
        makeElement(row, "td", "label", label + ":");
        ProcOut_dueDate.inp = makeElement(makeElement(row, "td", "field"), "input", "text", "text", null, {name:("arg" + argIdx)})
        ProcOut_dueDate.inp.value = (value == null) ? "" : value;
        var calElem = makeElementNbSpd(ProcOut_dueDate.inp.parentNode, "span", "xfCal");
        setEventHandler(calElem, "onclick",
            function (evt) {
                calPopShow(evt, calElem, ProcOut_dueDate, false);
            });
        row = null; // IE enclosure clean-up
        
        return ProcOut_dueDate.inp;
    },
    
    getTime : function() {
    	return ProcOut_dueDate.inp.value;
    },
    
    setTime : function(val) {
    	ProcOut_dueDate.inp.value = val;
    }
};
objectExtend(ProcOut_dueDate, WikiExCommon);


ProcOutEditView.prototype.macProps = function(evt, elem) {
	var extid = elem.getAttribute("extid");
	var macObj = eval("ProcOut_" + extid);
    var diag =  macObj.wysPropDialog(elem.getAttribute("args"));
    diag.wexElem = elem;
    diag.show(getMouseX(evt), getMouseY(evt));
};

ProcOutEditView.prototype.macRemove = function(evt, elem) {
    this.selectAnElement(elem);
    this.doCmd("delete");
    if (SH.is_gecko) {
        var holdThis = this;
        window.setTimeout(function() {holdThis.win.focus();},10);
    }
};

ProcOutEditView.prototype.resize = function(rect) {
	this.__iframe.style.width = (rect.w >= 4 ? (rect.w - 4) : 0) + "px";
	this.__iframe.style.height = (rect.h >= 26 ? (rect.h - 26) : 0) + "px";
};

ProcOutEditView.prototype.cleaner = function() {
	var cleanPar = this.doc.createElement("div");
	if (this.hasLayout) {
		var tElem = getFirstChildElement(this.doc.body);
		while (__poevTag(tElem) != "table") {
			if (__poevHasContent(tElem)) {
				break;
			} else {
				this.doc.body.removeChild(tElem);
				tElem = getFirstChildElement(this.doc.body);
			}
		}
		var td;
	    if (tElem == null || __poevTag(tElem) != "table") {
	    	var kids = [];
    	    var kid = this.doc.body.firstChild;
		    while (kid != null) {
		    	if (kid.nodeType == 1 && __poevTag(kid) == "table") {
		    		var tr = getFirstChildElement(getFirstChildElement(kid));
	   				td = getFirstChildElement(Xml.nextSiblingElement(Xml.nextSiblingElement(tr)));
		    		break;
		    	}
		        this.doc.body.removeChild(kid);
		        kids.push(kid);
		        kid = this.doc.body.firstChild;
		    }
		    if (!td) td = this.__makeLayout();
		    for (var ii = 0; ii < kids.length; ii++) {
		    	td.appendChild(kids[ii]);
		    }
	    } else {
	   		var tr = getFirstChildElement(getFirstChildElement(tElem));
	   		td = getFirstChildElement(Xml.nextSiblingElement(Xml.nextSiblingElement(tr)));
	    }
	    
	    this.__recurseClean(td, cleanPar);
	    td.innerHTML = cleanPar.innerHTML;
	    
	} else {
		this.__recurseClean(this.doc.body, cleanPar);
		this.setHTML(cleanPar.innerHTML);
	}
	
	
};

function __poevTag(node) {
	return node.nodeName.toLowerCase();
}

function __poevHasKid(node, names) {
	for (var nk = node.firstChild; nk != null; nk = nk.nextSibling) {
		if (nk.nodeType == 1) {
			if (names.indexOf(__poevTag(nk) + " ") >= 0) {
				return true;
			}
		}
	}
	return false;
}

function __poevIsCharBullet(ch, fuzzy) {
	switch (ch) {
		case "\u2022":
		case "\u00B7":
		case "\u00E0":
		case "\u006F":
		case "\u00D8":
		case "\u00FC":
		case "\u00FD":
		case "\u00FE":
			return true;
			
		case "-":
		case "*":
		case "+":
			return fuzzy;
	}
	return false;
}

function __poevHasContent(node) {
	for (var nk = node.firstChild; nk != null; nk = nk.nextSibling) {
		if (nk.nodeType == 1) {
			if (__poevHasContent(nk)) return true;
		} else if (nk.nodeType != 8) {
			if (trim(nk.nodeValue) != "")
			return true;
		}
	}
	return false;
}

ProcOutEditView.prototype.__recurseClean = function(inPar, outPar) {	

	var virList = null, outNode = null;
	for (var nk = inPar.firstChild; nk != null; nk = nk.nextSibling) {
		if (nk.nodeType == 1) {
			var tag = __poevTag(nk);
			var klass, html, lastCh;
			switch (tag) {
				
				// lists
        		case "ul":
        		case "ol":
        			outNode = virList = this.doc.createElement(tag);
        			outPar.appendChild(virList);
        			
        				// has real bullets?
        			if (__poevHasKid(nk, "li ")) {
        				this.__recurseClean(nk, virList);
        				
        			}  // has only sub lists ?
        				else if (__poevHasKid(nk, "ul ol ")) {
        					
        				for (var sk = nk.firstChild; sk != null; sk = sk.nextSibling) {
        					if (sk.nodeType == 1  && ("ul ol ".indexOf(__poevTag(sk) + " ") >= 0)) {
        						this.__recurseClean(sk, virList);
        					}
        				}
        				
        			}
        			break;
        			
        		case "li":
        			if (__poevHasContent(nk)) {
	        			outNode = this.doc.createElement("li");
	        			klass = nk.className;
	        			if (klass.indexOf("procPar") >= 0) {
	        				outNode.className = "procPar";
	        			}
	        			
	        			// in list ?
	        			if ("ul ol ".indexOf(__poevTag(inPar) + " ") >= 0) {
	        				
	        				outPar.appendChild(outNode);
	        			} else {
	        				if (!virList) virList = this.doc.createElement("ul");
	        				outPar.appendChild(virList);
	        				virList.appendChild(outNode);
	        			}
	        			this.__recurseClean(nk, outNode);
	        			html = trim(outNode.innerHTML);
	        			if (__poevIsCharBullet(html.charAt(0), true)) {
	        				 outNode.innerHTML = trim(html.substring(1));
	        			}
        			}
        			break;
        		
        		case "br":
        			outNode = this.doc.createElement("br");
        			outPar.appendChild(outNode);
        			break;
        		
        		case "a":
        			outNode = this.doc.createElement("a");
        			outNode.setAttribute("href", nk.getAttribute("href"));
        			outPar.appendChild(outNode);
        			this.__recurseClean(nk, outNode);
        			break;
        			
        		case "button":
                    klass = nk.className;
                    if (klass.indexOf("iten_emac") == 0) {
                    	outPar.appendChild(this.doc.createTextNode(H.nbsp));
                    	outPar.appendChild(nk.cloneNode(true));
                    } else {
                    	this.__recurseClean(nk, outPar);
                    }
                    break;
        		
        		case "td":
        			if (__poevHasContent(nk)) {
	        			outPar.appendChild(this.doc.createTextNode(" "));
		        		this.__recurseClean(nk, outPar);
		        		outPar.appendChild(this.doc.createTextNode(". "));
        			}
        			break;
        		
        		
        		case "tr":
        			outNode = this.doc.createElement("p");
        			this.__recurseClean(nk, outNode);
        			outPar.appendChild(outNode);
        			break;
        		
        		case "p":
        		case "div":
        		case "h1":
        		case "h2":
        		case "h3":
        		case "h4":
        		
        			if (__poevHasContent(nk)) {
        				var isBody = (__poevTag(inPar) == "body"
        					|| (this.hasLayout && __poevTag(inPar) == "td" && inPar.className == "row1"));
        				if (isBody) {
        					outNode = this.doc.createElement(tag);
        				} else {
        					outNode = this.doc.createElement("p");
        				}
        				this.__recurseClean(nk, outNode);
        				html = trim(outNode.innerHTML);
        				
        				// detect a loose bullet
	        			if (__poevIsCharBullet(html.charAt(0), isBody)) {
	        				outNode = this.doc.createElement("li");
	        				
	        				outNode.innerHTML = trim(html.substring(1));
	        				
	        				// in list ?
		        			if ("ul ol ".indexOf(__poevTag(inPar) + " ") >= 0) {        				
		        				outPar.appendChild(outNode);
		        			} else if (__poevTag(inPar) == "li" && inPar.parentNode) {
		        				inPar.parentNode.appendChild(outNode);
		        			} else {
		        				if (!virList) virList = this.doc.createElement("ul");
		        				outPar.appendChild(virList);
		        				virList.appendChild(outNode);
		        			}
	        				
	        			} else if (!isBody) {
	        				// just stuff the out into the parent
	        				outPar.appendChild(this.doc.createTextNode(" "));
	        				this.__recurseClean(outNode, outPar);
							lastCh = html.charAt(html.length - 1);
	        				if (".!?".indexOf(lastCh) < 0) {
	        					outPar.appendChild(this.doc.createTextNode(". "));
	        				}
	        			} else {
	        				outPar.appendChild(outNode);
	        			}
        			} else {
        				virList = null;
        				outNode = this.doc.createElement("br");
        				outPar.appendChild(outNode);
        			}
        			break;
        			
        		default:
        			this.__recurseClean(nk, outPar);
        			break;
        			
			}
		} else if  (nk.nodeType != 8) {
			var txt = WikiEdit.cleanWhite(nk.nodeValue);
			// clean-out fake bullets
			if (this.hasLayout && __poevTag(inPar) == "td" && inPar.className == "row1" && __poevIsCharBullet(txt.charAt(0), true)) {
				outNode = this.doc.createElement("li");
				
				// in list ?
    			if ("ul ol ".indexOf(__poevTag(inPar) + " ") >= 0) {
    				
    				outPar.appendChild(outNode);
    			} else {
    				if (!virList) virList = this.doc.createElement("ul");
    				outPar.appendChild(virList);
    				virList.appendChild(outNode);
    			}
    			txt = txt.substring(1);
    			outNode.appendChild(this.doc.createTextNode(txt));
			} else {
            	outPar.appendChild(this.doc.createTextNode(txt));
			}
        }
	}
	
};


ProcOutEditView.prototype.saveToChart = function() {
	var flow = this.__procCan.getFlow();
	var root = {id:"", seq:[], par:null};
	if (this.hasLayout) {
		var tElem = getFirstChildElement(this.doc.body);
		var td;
	    if (tElem == null || "table" != tElem.nodeName.toLowerCase()) {
	    	td = this.doc.body;
	    } else {
	   		var tr = getFirstChildElement(getFirstChildElement(tElem));
	   		td = getFirstChildElement(Xml.nextSiblingElement(Xml.nextSiblingElement(tr)));
	    }
		ProcOutEditView.__recurseNodes(td, root, new XmlId());
	} else {
		var ss = ProcOutEditView.__recurseNodes(this.doc.body, root, new XmlId());
		if (ss != "" && /\S+/.test(ss)) {
			root.seq.push({id: ("note " + root.seq.length), type:"note", text:ss, par:root});
		}
	}
	
	if (!(this.hasLayout && root.seq.length == 1 && !root.seq[0].text &&
		root.seq[0].seq.length == 0 && root.seq[0].id == "Start here")) {
		
		this.__stpMaster = flow.xfMod.selectNodeList("instance('pal')/activity")[0];
		this.__notMaster = flow.xfMod.selectNodeList("instance('pal')/note")[0];
		this.__pthMaster = flow.xfMod.selectNodeList("instance('pal')/path")[0];
		this.__grpMaster = flow.xfMod.selectNodeList("instance('pal')/group")[0];
		
		var sNodes = [];
		var maxY = 0;
		var ii;

	    var startStep = flow.idCtx.prefix ? 
	    	flow.getStepById(Uri.absolute(flow.idCtx.prefix, "$$S")) : flow.getStepById("Start");
	    
	    var endStep = null;
	    var startPthCnt = 0;
	    if (startStep) {
	    	startPthCnt = startStep.fromPaths.length;
		    for (ii = 0; ii < startPthCnt; ii++) {
		        var toStep = startStep.fromPaths[ii].toObj;
		        if (toStep && (toStep.constructor == PmEnd || toStep.constructor == PmExit)) {
		    		endStep = toStep;
		    		startStep.fromPaths[ii].remove();
		    		startPthCnt--;
		    		break;
		        }
		    }
	    }
	    
	    var nearEmpty = true;
	    ii=0; maxY = 0;
	    for (var sid in flow.steps) {
	    	var stpObj = flow.steps[sid];
	    	if (!(stpObj.constructor == PmEnd || stpObj.constructor == PmStart)) {
	    		nearEmpty = false;
	    	}
	    	var styleObj = styleString2Obj(stpObj.xNode.getAttribute("style"));
	    	if (styleObj.top) {
	    		var top = parseInt(styleObj.top);
	    		if (top > maxY) maxY = top + 75;
	    	}
	    }
	    
	    if (isNaN(maxY) || maxY < 0) maxY = 0;
	    
	    
	    // prep input set
	    this._attIdCtx = new XmlId();
	    this._attXfMod = flow.xfMod;
	    var attrNodes = this._attXfMod.selectNodeList("iw:data/iw:attr");
	    for (var ii = 0; ii < attrNodes.length; ii++) {
	    	this._attIdCtx.addVar(attrNodes[ii].getAttribute("name"));
	    }
	    
		this.__recurseSeq(
				root, sNodes, flow.modelNode, flow.idCtx.prefix,
		 		nearEmpty ? 0 : maxY,
		 		startStep ? startStep.xNode : null,
		 		endStep ? endStep.xNode : null);
		
		var sObjs = flow.digestStepList(sNodes, true);
		
		if (startStep) {
			var sPthNodes = Xml.match(startStep.xNode, "path");
			for (ii = startPthCnt; ii < sPthNodes.length; ii++) {
				var sptNode = sPthNodes[ii];
				sptNode.setAttribute("id", flow.idCtx.uniqueVar(sptNode.getAttribute("id")));
				var pObj = new PmPath(flow, sptNode);
				pObj.setFrom(startStep);
				PmPath.parsePath(pObj, true, flow.__lastOrigIdx);
				sObjs.push(pObj);
			}
		}
		
		if (endStep) {
			domSetCss(endStep.element, endStep.xNode.getAttribute("style"));
		}
		
		if (!this.__procCan.hidden) {
			if (sObjs.length > 0) this.__procCan.dndCanvas.scrollToView(sObjs[0].element);
			flow.dndGroupList(sObjs, 0, 0);
		}
		flow.xfMod.rebuild();
	}
	ProcOutline.lastOutline = this.getHTML();
	if (this.onSaved) this.onSaved();
};

ProcOutEditView.prototype.__recurseSeq = function(seqObj, sNodes, parNode, prefix, yOff, startNode, endNode) {
	
	
	var pnt = new Point(100, 20 + yOff);
	var maxPnt = pnt.clone();
	var pStpPnt = new Point(-50, 20 + yOff);
	
	if (startNode) {
		var sbRect = PmStep.getBounds(startNode);
		pStpPnt.x = sbRect.x;
		pnt.x = sbRect.x + sbRect.w + 25;
	}
	var strPnt = pnt.clone();
	
	var prevPath = null;
	var parlCnt = 0;
	var parlOffset = yOff;
	var parlHadLoop = false;
	var pXnode = startNode;
	var tails = [];
	var xNode;
 	for (var ii = 0; ii < seqObj.seq.length; ii++) {
 		var step = seqObj.seq[ii];
 		var pid = Uri.absolute(prefix, step.id);
 		xNode = null;
 		if (step.type == "step") {
 			
 			var pthNod = null;
 			var width = 110;
 			var exNode = null;
 			var wikArt, wikBod;
 			
 			// is group?
 			if (step.seq.length > 0) {
				
 				xNode = Xml.nodeImport(parNode.ownerDocument, null, this.__grpMaster);
 				
 				var enNode = Xml.matchOne(xNode, "enter");
 				enNode.setAttribute("style", "left:10px;top:20px");
 				enNode.setAttribute("id", Uri.absolute(pid, "$$S"));
 				enNode.removeChild(Xml.matchOne(enNode, "path"));
 				
 				exNode = Xml.matchOne(xNode, "exit");
 				exNode.setAttribute("id", Uri.absolute(pid, "$$E"));
 				exNode.setAttribute("style", "left:100px;top:20px");
 				
 				// recurse
 				var ePnt = this.__recurseSeq(step, null, xNode, pid, 0, enNode, exNode);
 				
 			} else {
 				xNode = Xml.nodeImport(parNode.ownerDocument, null, this.__stpMaster);
 				if (step.role) {
 					xNode.setAttribute("role", step.role);
 				}
 				if (step.text) {
 					// Process input docs
 					if (step.inputs) {
 						for (var jj = 0; jj < step.inputs.length; jj++) {
 							var label = step.inputs[jj];
 							
 							// did user change the label?
 							var attId = "";
 							if (label == "Document") {
 								attId = XmlId.makeVar(step.id, 10);
 								attId = this._attIdCtx.uniqueVar(attId);
 							} else {
 								attId = XmlId.makeVar(label, 10);
 							}
 							if (!this._attIdCtx.hasVar(attId)) {
	 							this._attIdCtx.addVar(attId);
	 							attId = attId.replace(" ", "_");
	 							
	 							// add process attribute
	 							var attNode = this._attXfMod.duplicateNode("instance('pal')/iw:attr", "iw:data");
	 							attNode.setAttribute("name", attId);
	 							attNode.setAttribute("type", "ix:file");
 							} else {
 								attId = attId.replace(" ", "_");
 							}
 							
 							// replace the step text
 							step.text = step.text.replace(
 								"[input[" + jj + "]]", "[input[" + attId + "|" + label + "]]");
 						}
 						
 					}
 					
 					
 					wikArt = Xml.matchOne(xNode, "article");
 					if (wikArt) {
 						wikBod = Xml.stringForNode(wikArt);
 						// have text replace "Default workzone."
 						Xml.setNodeValue(wikArt, step.text + wikBod.substring(17));
 					}
 				}
 				if (step.dueDate) {
 					ActivityTree.planning.setDates(pid, DateUtil.parseLocaleShort(step.dueDate));
 				}
 				if (step.assign) {
 					ActivityTree.planning.setAssign(pid, step.assign);
 				}
 			}
 			
 			xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + pnt.y + "px");
 			if (step.text) {
 				var desc = Xml.matchOne(xNode, "description");
 				Xml.setNodeValue(desc, ProcOutEditView.filterMacros(step.text));
 			}

 			if (pXnode) {
 				
 				// is there a previous path?
 				if (prevPath) {
 					var ppnts = prevPath.getAttribute("points").split("-");
 					ppnts[ppnts.length - 1] = pnt.x + "," + (pnt.y + 30);
 					prevPath.setAttribute("to", exNode ? Uri.absolute(pid, "$$S") : pid);
 					prevPath.setAttribute("points", ppnts.join("-"));
 				} else {
					pthNod = Xml.nodeImport(parNode.ownerDocument, null, this.__pthMaster);
					
					if (Xml.getLocalName(pXnode) == "group") {
						Xml.matchOne(pXnode, "exit").appendChild(pthNod);
						width = 100;
					} else {
						pXnode.appendChild(pthNod);
					}
	 				pthNod.setAttribute("id", Uri.absolute(prefix, "pt" + ii));
	 				pthNod.setAttribute("to", exNode ? Uri.absolute(pid, "$$S") : pid);
	 				pthNod.setAttribute("startDir", Point.directions[Point.EAST]);
	 				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.EAST]]);
	 				
	 				var pRect = PmStep.getBounds(pXnode);
	 				
	 				var sPnts = PmPath.pointSolve(
	 						new Point(pRect.x + pRect.w, pRect.y + 30), Point.EAST,
	 				 		new Point(pnt.x, pnt.y + 30), Point.EAST);
	 				
	 				pthNod.setAttribute("points", sPnts.join("-"));
 				}
 			}
 			
 			parNode.appendChild(xNode);
 			
 			var space = 25;
 			if (step.launch) {
 				xNode.setAttribute("apptype", "launch");
 				xNode.setAttribute("flow", step.launch[0]);
 				prevPath = null;
 			} else if (step.loop) {
 				space = 50;
 				
 				// insert loop shapes
 				if (!this.__appLoopMasters) {
 					this.__appLoopMasters = this.__procCan.getFlow().xfMod.selectNodeList(
 						"instance('pal')/bundle[@id='approval-loop']/*");
 				}
 				
 				// very specific to approval loop

 				// review step
 				var rvXNode = Xml.nodeImport(parNode.ownerDocument, null, this.__appLoopMasters[0]);

 				rvXNode.setAttribute("id", Uri.absolute(prefix, step.revId));
 				rvXNode.setAttribute("style", "left:" + pnt.x + "px;top:" + (pnt.y + 90) + "px");
 				
 				// from actual
 				var height = 60;
 				pthNod = Xml.nodeImport(parNode.ownerDocument, null, this.__pthMaster);

				if (Xml.getLocalName(xNode) == "group") {
					Xml.matchOne(xNode, "exit").appendChild(pthNod);
					height = 70;
				} else {
					xNode.appendChild(pthNod);
				}
 				pthNod.setAttribute("id", Uri.absolute(prefix, "rpt" + ii));
 				pthNod.setAttribute("to", Uri.absolute(prefix, step.revId));
 				pthNod.setAttribute("startDir", "S");
 				pthNod.setAttribute("endDir", "N");
 				pthNod.setAttribute("points",
 					(pnt.x + 55) + "," + (pnt.y + height) + "-" +
 					(pnt.x + 55) + "," + (pnt.y + 90));
 				
 				// to switch
 				pthNod = Xml.matchOne(rvXNode, "path");
 				pthNod.setAttribute("id", Uri.absolute(prefix, "srpt" + ii));
 				pthNod.setAttribute("to", Uri.absolute(prefix, step.swId));
 				pthNod.setAttribute("points",
 					(pnt.x + 55) + "," + (pnt.y + 150) + "-" +
 					(pnt.x + 55) + "," + (pnt.y + 170));
 				
 				if (step.text) {
 					wikArt = Xml.matchOne(rvXNode, "article");
 					if (wikArt) {
 						wikBod = Xml.stringForNode(wikArt);
 						// have text replace "Default workzone."
 						Xml.setNodeValue(wikArt, step.text + wikBod.substring(17));
 					}
 				}
 				if (step.role) {
 					rvXNode.setAttribute("role", step.role + " Reviewer");
 				}
 				
 				// switch step
 				var swXNode = Xml.nodeImport(parNode.ownerDocument, null, this.__appLoopMasters[1]);

 				swXNode.setAttribute("id", Uri.absolute(prefix, step.swId));
 				swXNode.setAttribute("style", "left:" + (pnt.x - 5) + "px;top:" + (pnt.y + 170) + "px");
 				var pthNods = Xml.match(swXNode, "path");
 				
 				pthNods[0].setAttribute("id", Uri.absolute(prefix, "Rrpt" + ii));
 				pthNods[0].setAttribute("to", exNode ? Uri.absolute(pid, "$$S") : pid);
 				pthNods[0].setAttribute("points",
 					(pnt.x - 5) + "," + (pnt.y + 210) + "-" +
 					(pnt.x - 15) + "," + (pnt.y + 210) + "-" +
 					(pnt.x - 15) + "," + (pnt.y + 55) + "-" +
 					(pnt.x) + "," + (pnt.y + 55));
 				
 				Xml.matchOne(pthNods[0], "label").setAttribute(
 					"style", "left:" + (pnt.x - 15) + "px;top:" + (pnt.y + 225) + "px");
 				
 				pthNods[1].setAttribute("id", Uri.absolute(prefix, "Arpt" + ii));
 				pthNods[1].setAttribute("points",
 					(pnt.x + 115) + "," + (pnt.y + 210) + "-" +
 					(pnt.x + 130) + "," + (pnt.y + 210) + "-" +
 					(pnt.x + 130) + "," + (pnt.y + 30) + "-" +
 					(pnt.x + 160) + "," + (pnt.y + 30));
 					
 				Xml.matchOne(pthNods[1], "label").setAttribute(
 					"style", "left:" + (pnt.x + 90) + "px;top:" + (pnt.y + 225) + "px");
 					
 				xNode.setAttribute("bundled", "2");

 				prevPath = pthNods[1];
 				
 				parNode.appendChild(rvXNode);
 				parNode.appendChild(swXNode);
 				if (sNodes) {
 					sNodes.push(rvXNode);
 					sNodes.push(swXNode);
 				}
 				parlHadLoop = true;
 				
 			} else {
 				prevPath = null;
 			}
 			
 			// move point right
 			pnt.x += 110 + space;
 			
 			if (step.isParl) {
 				
 				if (maxPnt.x < pnt.x) maxPnt.x = pnt.x;
 				
 				// if this is the last propigate parl up a level
 				if (ii == (seqObj.seq.length - 1)) {
 					seqObj.isParl = true;
 				} else {
					prevPath = null;
					parlCnt++;
					parlOffset += parlHadLoop ? 270 : 90;
					parlHadLoop = false;
					pnt = new Point(strPnt.x, 20 + parlOffset);
					pXnode = startNode;
					tails.push(xNode);
 				}
			} else {
				pXnode = xNode;
			}
			pStpPnt = pnt.clone();	
 			
 			
 		} else if (step.type == "note") {
 			xNode = Xml.nodeImport(parNode.ownerDocument, null, this.__notMaster);
 			
 			xNode.setAttribute("style", "left:" + pnt.x + "px;top:" + (pnt.y-5) + "px");
 			Xml.setNodeValue(xNode, step.text);
 			
 			pnt.x += 145;
 			parNode.appendChild(xNode);
 		}
 		if (xNode != null) {
 			xNode.setAttribute("id", pid);
 			if (sNodes) sNodes.push(xNode);
 		}
 		if (maxPnt.x < pnt.x) maxPnt.x = pnt.x;
 		if (maxPnt.y < pnt.y) maxPnt.y = pnt.y;
 		
 	} // end for
 	
 	if (xNode != null && tails[tails.length - 1] !== xNode) {
 		tails.push(xNode);
 	}
 	if (endNode) {
 		var styObj = styleString2Obj(endNode.getAttribute("style"));
 		styObj.left = maxPnt.x + "px";
 		endNode.setAttribute("style", styleObj2String(styObj));
 		var ePnt = new Point(maxPnt.x, parseInt(styObj.top));
 		for (var jj = 0; jj < tails.length; jj++) {
 			var tNode = tails[jj];
 			var pthNod;
 			if (tNode.getAttribute("bundled")) {
 				var bof = parseInt(tNode.getAttribute("bundled"));
 				while (bof > 0 && tNode) {
 					bof--;
 					tNode = Xml.nextSiblingElement(tNode);
 				}
 				pthNod = Xml.lastElement(tNode);

 			} else {
 				pthNod = Xml.nodeImport(parNode.ownerDocument, null, this.__pthMaster);
				if (Xml.getLocalName(tNode) == "group") {
					Xml.matchOne(tNode, "exit").appendChild(pthNod);
					width = 100;
				} else {
					tNode.appendChild(pthNod);
				}
				pthNod.setAttribute("id", Uri.absolute(prefix, "tpt" + jj));
				pthNod.setAttribute("startDir", Point.directions[Point.EAST]);
				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.EAST]]);
 			}
 			pthNod.setAttribute("to", endNode.getAttribute("id"));
			
			var pRect = PmStep.getBounds(tNode);
			
			var sPnts = PmPath.pointSolve(
					new Point(pRect.x + pRect.w, pRect.y + (Xml.getLocalName(tNode) == "switch" ? 40 : 30)), Point.EAST,
			 		new Point(ePnt.x, ePnt.y + 30), Point.EAST);
			
			pthNod.setAttribute("points", sPnts.join("-"));
 		}
 		
 	}
 	return maxPnt;
};

ProcOutEditView.filterMacros = function(str) {
	// for "est [test[dfdf|ffff]] ggg" returns "est ffff ggg"
	return str.replace(/\[(\w+)\[([^\]]*)\]\]/g, function(str, p1, p2) { 
			var sp = p2.split("|"); 
			return sp.length > 1 ? sp[1] : ""; 
		});
};

ProcOutEditView.__recurseNodes = function(nn, seqObj, idCtx) {
	
	var ss = "";
    if (nn != null) {
        if (nn.nodeType == 1) {
        	var step = seqObj;
        	var idrx = /([.?!][ \u00a0\t\n\r\[]+|>)/;
            for (var nk = nn.firstChild; nk != null; nk = nk.nextSibling) {
                if (nk.nodeType == 1) {
                	var tag = nk.nodeName.toLowerCase();
                	switch (tag) {
		            	case "li":
		            		step = {id:"empty", type:"step",  par:seqObj, seq:[], 
		            			isParl:(nk.className.indexOf("procPar") >= 0)};
		            			
		            		seqObj.seq.push(step);
		            		var kss = ProcOutEditView.__recurseNodes(nk, step, new XmlId());		            		
		            		kss = trim(kss);
		            		var kss2 = WikiEdit.cleanWhite(ProcOutEditView.filterMacros(kss));
		            		var kpts = kss2.split(idrx);
		            		if (kpts.length > 1) {
		            			step.id = kpts[0];
		            			if (step.id.length > 96) {
			            			step.id = step.id.substring(0, 93) + "...";
		            			}
		            			var spStr = kss;
		            			var ii=0;
		            			kpts = [];
		            			var mt = idrx.exec(spStr);
		            			if (mt) {
		            				spStr = spStr.substring(mt.index + mt[1].length);
		            				if (mt[1].charAt(mt[1].length - 1) == "[") kpts[ii++] = "[";
		            				mt = idrx.exec(spStr);
		            				while (mt) {
		            					kpts[ii++] = spStr.substring(0, mt.index);
		            					if (mt[1] == ">") {
		            						kpts[ii++] = "\n";
		            					} else {
		            						kpts[ii++] = mt[1];
		            					}
		            					spStr = spStr.substring(mt.index + mt[1].length);
		            					mt = idrx.exec(spStr);
		            				}
		            				kpts[ii] = spStr; 
		            			}
		            			step.text = kpts.join("");
		            		} else {
			            		if (kss2.length > 96) {
			            			step.id = kss2.substring(0, 93) + "...";
			            			step.text = kss;
			            		} else if (kss2 != "") {
			            			step.id = kss2;
			            			if (kss2 != kss) step.text = kss;
			            		}

		            		}
		            		step.id = step.id.replace('/', '\\');
		            		step.id = idCtx.uniqueVar(step.id);
		            		idCtx.addVar(step.id);
		            		if (step.loop) { // reserve some ids
		            			step.revId = idCtx.uniqueVar(step.id + " Review");
		            			idCtx.addVar(step.revId);
		            			step.swId = idCtx.uniqueVar("Approval Loop");
		            			idCtx.addVar(step.swId);
		            		}
		            		
		            		break;
		            		
		            	case "button":
	                        var klass = nk.className;
	                        if (klass.indexOf("iten_emac") == 0) {
	                        	if (seqObj.type == "step") {
	                        		var mextid = nk.getAttribute("extid");
	                        		switch (mextid) {
	                        			case "role":
	                        				seqObj.role = trim(Xml.stringForNode(nk));
	                        				break;
	                        				
	                        			case "loop":
	                        				seqObj.loop = trim(Xml.stringForNode(nk));
	                        				break;
	                        				
	                        			case "launch":
	                        				seqObj.launch = nk.getAttribute("args").split("|");
	                        				break;
	                        			
	                        			case "dueDate":
	                        				seqObj.dueDate = trim(nk.getAttribute("args"));
	                        				break;
	                        			
	                        			case "assign":
	                        				seqObj.assign = trim(nk.getAttribute("args"));
	                        				break;
	                        				
	                        			case "input":
	                        				var label = trim(Xml.stringForNode(nk));
	                        				if (!seqObj.inputs) seqObj.inputs = [];
	                        				seqObj.inputs.push(label);
	                        				// inserted for replacement on next pass
	                        				ss += " [input[" + (seqObj.inputs.length - 1) +"]]";
	                      					break;
	                        			
	                        			case "form":	
	                        			case "attachEdit":
	                        			case "entity":
	                        				ss += "[" + mextid + "[" + trim(nk.getAttribute("args")) + "]]";
	                        				break;
	                        		}
	                        	}
	                        }
	                        break;
	                        
		            	case "ol":
		            	case "ul":
		            		if (seqObj.par == null && ss != "" && /\S+/.test(ss)) {
		            			seqObj.seq.push({id: ("note " + seqObj.seq.length), type:"note", text:ss, par:seqObj});
		            			ss = "";
		            		}
		            		ProcOutEditView.__recurseNodes(nk, step, idCtx);
		            		break;
		            		
		            	case "br":
		            		ss += "\n";
		            		break;
		            	
		            	case "a":
		            		var href = nk.getAttribute("href");
		            		if (href.indexOf('http') == 0) {
		            			ss += "[attach[" + href + "|" + WikiEdit.cleanWhite(Xml.stringForNode(nk)) + "]]";
		            			break;
		            		}
		            		// fall through
		            		
		            	default:
		            		ss += ProcOutEditView.__recurseNodes(nk, seqObj, idCtx);
		            		if ("p div h1 h2 h3 h4 h5 h6 hr table form".indexOf(tag+" ") >= 0) ss += "\n";
		            		break;
		            }
                } else if (nk.nodeType != 8) {
                    ss += WikiEdit.cleanWhite(nk.nodeValue);
                }
            }
	    } else if (nn.nodeType != 8) { // if not comment
	        ss = WikiEdit.cleanWhite(nn.nodeValue);
	    }
	}
   	return ss;
};



ProcOutDNDHand.prototype = new DNDTypeHandler();
ProcOutDNDHand.prototype.constructor = ProcOutDNDHand;

function ProcOutDNDHand(view) {
    this.type = "procOutDnd";
    this.view = view;
}

ProcOutDNDHand.prototype.canDrag = function(dragElem) {
    return false;
};

ProcOutDNDHand.prototype.dropTest = function(dropElem, dragElem) {
    // file drop
    var type = dragElem._dndType.type;
    if (type == "dndFile" || type == "dndTeam" || type == "dndEntForm") {
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

ProcOutDNDHand.prototype.dropExec = function(dropElem, dragElem) {

    // file drop
    var macro = null;
    var type = dragElem._dndType.type;
    if (type == "dndFile") {
        macro = {id:"", args:[]};
        var dragItem = dragElem._actElem.__item;

        var wUri = Uri.parent(App.activeFlow);
        var uri = Uri.localize(wUri, dragItem.uri);
        var tpos = uri.indexOf("template/");
        if (tpos >= 0)
            uri = "{activity}" + uri.substring(tpos + 8);
      	else if (App.resolver.modelUri && dragItem.uri.indexOf(App.resolver.modelUri) >= 0)
        	 uri = "{model}" + dragItem.uri.substring(App.resolver.modelUri.length);

        var tag;
        var ext = Uri.ext(uri).toLowerCase();
       	if (dragItem.allowsKids) {
       		macro.id = "attachEdit";
            macro.args[0] = uri + "/";
            macro.args[1] = Uri.name(uri);
        } else if (ext == "xfrm") {
            macro.id = "form";
            macro.args[0] = uri;
            macro.args[1] = Uri.name(uri);
        } else {
            macro.id = "attachEdit";
            macro.args[0] = uri;
            macro.args[1] = Uri.name(uri);
        } 
        
    } else if (type == "dndTeam") {
    	var htStr = "<button class='iten_emac2' extid='assign' args=\"" + dragElem._actElem.getAttribute("uid") +
    		"\"><span class='" + dragElem._actElem.className + "'>" +
    		dragElem._actElem.innerHTML + "</span></button>" + H.nbsp;
    	this.view.insertHTML(htStr);
    } else if (type == "dndEntForm") {
    	if (EntFormDropDNDType.getEntityRelation(dragElem) == "1") {
    		var formMenu = EntFormDropDNDType.getFormMenu(
    				EntFormDropDNDType.getEntityId(dragElem),
    				ProcOutDNDHand.entityFormDrop,
    				{label : "Cancel", act : ProcOutDNDHand.attrCancel});
    		var rect = getViewBounds(dragElem);
    		formMenu.show(rect.x + 5, rect.y + 5, null, { view : this.view, dragElem : dragElem});
    		return;
    	} else {
    		macro = {id:"entity", args:[EntFormDropDNDType.getEntityName(dragElem)]};
    	}
    }
    if (macro) this.view.insertMacro(macro);
};


ProcOutDNDHand.entityFormDrop = function(evt, pair) {
	var macro = {id:"entity", args:[EntFormDropDNDType.getEntityName(pair.dragElem)]};
	macro.args.push(this.src);
	pair.view.insertMacro(macro);
};

ProcOutDNDHand.attrCancel = function() {
	return;
};