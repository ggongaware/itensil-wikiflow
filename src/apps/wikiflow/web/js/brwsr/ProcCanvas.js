/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Process Modeling
 * 
 */
function PmCanvas(panel, formHParent, formUri, procUri, saveUri, flowUri) {   
    var xb = new XMLBuilder();
    this.formHParent = formHParent;
    this.xfrm = new XForm(xb.loadURI(formUri), "proccan", xb, formUri, this);
	this.__procUri = procUri;
    this.xfrm.setDefaultUris(procUri, saveUri);
    if (flowUri) this.xfrm.__defPath = Uri.parent(flowUri);
    this.xfrm.render(formHParent);

    var holdThis = this;
    
    if (panel) {
    	panel.contentElement.style.overflow = "hidden";
    	this.__hElem = makeElement(panel.contentElement, "div", "pmCanvas pmRead");
    	panel.linkResize(this);
    	this.resize(getBounds(panel.contentElement));
    	    
   		addEventHandler(this.__hElem, "mousedown", function(evt) {
	            holdThis.mouseDown(evt);
	        });
	    setEventHandler(this.__hElem, "oncontextmenu", function(evt) {
	            return holdThis.contextMenu(evt);
	        });
    } else {
    	this.__hElem = makeElement(document.body, "div", "pmCanvas pmRead");
    	this.__hElem.style.display = "none";
    }
    
	this.dndCanvas = dndGetCanvas(this.__hElem);
    this.dndCanvas.setParent(dndGetCanvas(document.body), false);
    this.dndCanvas.disableDrag = true;
    this.dndCanvas.setGroup(new DNDGroup());
    this.dndCanvas.initAutoScroll(15, 15);
    

    this.clickMode = null;
    this.element = makeElement(this.__hElem, "div", "pmInner");
    
    this.__editMode = false;

    if (App) {
        App.activityListeners.push(function(node, stepId) {
           holdThis.setWFActivity(node, stepId);
        });
        App.unloadListeners.push(function() {
           return holdThis.dndCanvas != null && holdThis.dndCanvas.model != null && holdThis.dndCanvas.model.changed;
        });
        this.lastActiveNode = null;
    }
    if (!PmCanvas.mainCanvas) {
    	PmCanvas.mainCanvas = this;
    }

    panel = null; formHParent = null; // IE enclosure clean-up
}

PmCanvas.prototype.getIdContext = function() {
    return this.dndCanvas.model == null ? null : this.dndCanvas.model.idCtx;
};

PmCanvas.prototype.dispose = function() {
    if (this.dndCanvas.model != null) this.dndCanvas.model.dispose();
    this.dndCanvas.model = null;
    this.dndCanvas.dispose();
    this.dndCanvas = null;
    this.__palElem = null;
    this.clickBtnElem = null;
    this.clickArg = null;
    this.element = null;
    this.__hElem = null;
    this._attXfDoc = null;
    this.lastActiveNode = null;
    if (this._attXfrm) {
    	this._attXfrm.dispose();
    	this._attXfrm = null;
    }
    if (PmCanvas.editStepMenu != null) PmCanvas.editStepMenu.dispose();
    PmCanvas.editStepMenu = null;
    if (PmCanvas.playStepMenu != null) PmCanvas.playStepMenu.dispose();
    PmCanvas.playStepMenu = null;
    if (PmCanvas.editNoteMenu != null) PmCanvas.editNoteMenu.dispose();
    PmCanvas.editNoteMenu = null;
    if (PmCanvas.editPathMenu != null) PmCanvas.editPathMenu.dispose();
    PmCanvas.editPathMenu = null;
    if (PmCanvas.editGroupMenu != null) PmCanvas.editGroupMenu.dispose();
    PmCanvas.editGroupMenu = null;
    if (PmCanvas.selectedMenu != null) PmCanvas.selectedMenu.dispose();
    PmCanvas.selectedMenu = null;
};

PmCanvas.prototype.setClickMode = function(clickMode, btnElem, clickArg) {
    if (this.clickBtnElem != null) {
        exAddClass(this.clickBtnElem, "pmPalOn", true);
    }
    if (this.clickMode == clickMode && this.clickArg == clickArg) {
        clickMode = null;
    }
    this.clickMode = clickMode;
    this.clickArg = clickArg;
    if (clickMode == null) {
        this.clickBtnElem = null;
    } else {
        exAddClass(btnElem, "pmPalOn");
        this.clickBtnElem = btnElem;
    }
};

PmCanvas.prototype.setEditMode = function(edMode) {
    if (this.__editMode == edMode) return;
    this.__editMode = edMode;
    var rect = getBounds(this.__hElem);
    if (edMode) {
        this.dndCanvas.disableDrag = false;
        exAddClass(this.__hElem, "pmRead", true);
        exAddClass(this.__hElem, "pmEdit", false);
        exAddClass(this.formHParent,"pmRead", true);
        //this.__hElem.style.height = (rect.h - 19) + "px";
    } else {
        this.dndCanvas.__groupType.clear();
        this.dndCanvas.disableDrag = true;
        exAddClass(this.__hElem, "pmEdit", true);
        exAddClass(this.__hElem, "pmRead", false);
        exAddClass(this.formHParent,"pmRead", false);
        //this.__hElem.style.height = (rect.h + 19) + "px";
    }
};

PmCanvas.prototype.resize = function(rect) {
    var h = rect.h - 19;
    if (h < 0) h = 0;
    this.__hElem.style.width = rect.w + "px";
    this.__hElem.style.height = h + "px";
    //this.__palElem.style.height = rect.h + "px";
};

PmCanvas.prototype.mouseDown = function(evt) {
    if (this.clickMode != null) {
        this.dndCanvas.cancelDrag = true;
        var iniFunc = eval(this.clickMode + ".initNew");
        if (iniFunc != null) {
            if (iniFunc(evt, this.dndCanvas, this.clickArg)) {
                this.setClickMode(null);
            }
        } else {
            this.setClickMode(null);
        }
        evt.cancelBubble = true;
    }
};

PmCanvas.prototype.beginSave = function() {
	if (App.activeActivityId && !ActivityTree.isMeet) {
		// detect variation...
		var actNode = ActivityTree.activityInfo(App.activeActivityId);
		if (actNode.getAttribute("variationId")) {
			this.commitSave();
		} else {
			var diag = xfTemplateDialog(
				"Process Save Options", true, document.body, this.xfrm, "save", null, true,
				 { activityName : actNode.getAttribute("name") }, App ? App.chromeHelp : null);
			diag.showModal(300, 150);
		}
	} else {
		this.commitSave();
	}
};

PmCanvas.prototype.commitSave = function() {
	this.xfrm.getDefaultModel().submit("submission");
};

PmCanvas.prototype.commitVariationSave = function() {
	this.xfrm.setDefaultUris(null, modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + 
		"&activity=" + App.activeActivityId + "&forceVar=1");
	this.commitSave();
	var vid = this.xfrm.getDefaultModel().getSubmitResponse().documentElement.getAttribute("variation");
	if (vid) {
		var actNode = ActivityTree.activityInfo(App.activeActivityId);
		actNode.setAttribute("variationId", vid);
	}
};

PmCanvas.prototype.getModelDocument = function() {
	return this.dndCanvas.model.pmDocument;
};

PmCanvas.prototype.getFlow = function() {
	return this.dndCanvas.model;
};

PmCanvas.prototype.isEmpty = function() {
	for (var st in this.getFlow().steps) {
		return false;
	}
	return true;
};

PmCanvas.showWorkzone = function(evt, step) {
	if (step == null) step = PmCanvas._lastStep;
	else PmCanvas._lastStep = step;
	if (step == null) return;
    var canvas = step.model.canvasObj;
    if (canvas.panelWork) canvas.panelWork.setShrink(false);
    if (canvas.wiki.showArticle(new PmWikiArt(step), true))
    	step.model.focusStep(step);
};

PmCanvas.prototype.contextMenu = function(evt) {
    var dndElem = this.dndCanvas.getDndElem(evt);
    if (dndElem != null && dndElem.peer != null) {
        var isStep = !(dndElem.peer.addPath == null);
        var cnst = dndElem.peer.constructor;
        if (this.__editMode) {
        	if (dndElem.peer._inDndGroup) {
    			PmCanvas.selectedMenu.popUp(evt, dndElem.peer);
    		} else if (cnst === PmNote) {
                PmCanvas.editNoteMenu.popUp(evt, dndElem.peer);
            } else if (cnst === PmGroup) {
                PmCanvas.editGroupMenu.popUp(evt, dndElem.peer);
            } else if (isStep) {

                // disable remove for
                PmCanvas.editStepMenu.setDisable(PmCanvas.editStepMenu.model.items[3], !dndElem.peer.canRemove());

                // disable workzone for everything buts activities
                PmCanvas.editStepMenu.setDisable(PmCanvas.editStepMenu.model.items[1], cnst != PmStep);

                PmCanvas.editStepMenu.popUp(evt, dndElem.peer);
            } else {
                // hide conditions
                PmCanvas.editPathMenu.setDisable(
                    PmCanvas.editPathMenu.model.items[1],
                    !(dndElem.peer.path.fromObj != null &&
                        dndElem.peer.path.fromObj.constructor === PmSwitch));
                PmCanvas.editPathMenu.popUp(evt, dndElem.peer);
            }
        } else if (isStep) {
        	if (cnst === PmGroup) {
                PmCanvas.playGroupMenu.popUp(evt, dndElem.peer);
            } else {
            	// disable workzone for everything buts activities
                PmCanvas.playStepMenu.setDisable(PmCanvas.playStepMenu.model.items[0], cnst !== PmStep && cnst !== PmTimer)
            	PmCanvas.playStepMenu.popUp(evt, dndElem.peer);
            }
        }
        evt.cancelBubble = true;
        return false;
    }
    return true;
};

PmCanvas.selectedMenu = new Menu(
    new MenuModel([
        {label : "Duplicate Selected", icon : "mb_copyIco", act : function(evt, step) {
             step.model.duplicateSelected(); } },
        {label : "Group Selected", icon : "mb_foldIco", sub : new Menu(
		   		new MenuModel([{isInput : true, label : "New Group", act : function(evt, name, step) {
            		step.model.groupSelected(name); } 
            	}]
		  	), null, "procMn")},
        {isSep : true},
        {label : "Remove Selected", icon : "mb_remIco", act : function(evt, step) {
            if (confirm("Are you sure?")) step.model.removeSelected(); }}]
        ), null, "procMn"
    );


PmCanvas.editStepMenu = new Menu(
    new MenuModel([
        {label : "Properties", icon : "mb_optIco", act : function(evt, step) {
            step.showProperties(evt); } },
        {label : "Workzone", icon : "mb_workIco",  act : PmCanvas.showWorkzone },
        {isSep : true},
        {label : "Remove", icon : "mb_remIco", act : function(evt, step) {
            if (confirm("Are you sure?")) step.remove(); }}]
        ), null, "procMn"
    );

PmCanvas.playStepMenu = new Menu(
    new MenuModel([
        {label : "Workzone", icon : "mb_workIco", act : function(evt, step) {
        		App.setActiveStep(step.getId());
        		PmCanvas.showWorkzone(evt, step);
        	}},
        {isSep : true},
        {label : "Design Mode", icon : "mb_ediIco",  
        	act : function(evt, step) { 
	        	App.setActiveStep(step.getId());
	        	Modes.edit(); }}]
        ), null, "procMn"
    );

PmCanvas.editNoteMenu = new Menu(
    new MenuModel([
        {label : "Properties", icon : "mb_optIco", act : function(evt, note) {
            note.showProperties(evt); } },
        {isSep : true},
        {label : "Remove", icon : "mb_remIco", act : function(evt, note) {
            if (confirm("Are you sure?")) note.remove();
        }}]
       ), null, "procMn"
    );


PmCanvas.editPathMenu = new Menu(
    new MenuModel([
        {label : "Properties", icon : "mb_optIco", act : function(evt, pathPart) {
            pathPart.path.showProperties(evt); } },
        //{label : "Conditions", icon : "mb_condIco", act : function() { alert("under construction"); } },
        {isSep : true},
        {label : "Remove", icon : "mb_remIco", act : function(evt, pathPart) {
            if (confirm("Are you sure?")) {
                if (pathPart.constructor === PmPathLabel) pathPart.remove();
                else pathPart.path.remove();
            }
        }}]
       ), null, "procMn"
    );
    
PmCanvas.editGroupMenu = new Menu(
    new MenuModel([
		{label : "Properties", icon : "mb_optIco", act : function(evt, step) {
            step.showProperties(evt); } },
        {label : "View Group", icon : "mb_viewIco",  act : function(evt, step) {
            step.viewGroup(); }  },
        {isSep : true},
        {label : "Un-Group", icon : "mb_outIco",  act : function(evt, step) {
            step.unGroup(); }  },
        {isSep : true},
        {label : "Remove", icon : "mb_remIco", act : function(evt, step) {
            if (confirm("Are you sure?")) step.remove(); }}]
        ), null, "procMn"
    );

PmCanvas.playGroupMenu = new Menu(
    new MenuModel([
        {label : "View Group", icon : "mb_viewIco",  act : function(evt, step) {
            step.viewGroup(); }  },
        {isSep : true},
        {label : "Design Mode", icon : "mb_ediIco",  act : function() { Modes.edit(); }}]
        ), null, "procMn"
    );
    

PmCanvas.prototype.setWFActivity = function(node, stepId) {
    //if (Modes.mode == "todo") {
      	if (this._attXfrm) {
        	this._attXfrm.dispose();
        	this._attXfrm = null;
        }
        // reload map?
        if ((Modes.mode == "todo" || Modes.mode == "stat")
        	&& this.lastActiveNode != null && ((node && node.getAttribute("variationId"))
        		|| this.lastActiveNode.getAttribute("variationId"))) {
        	if (node.getAttribute("id") != this.lastActiveNode.getAttribute("id")) {
        		Modes.mode == "stat" ? Modes.stat(null) : Modes.todo(null, true);
        		App.activityListeners = []; // hack to abort other listeners
        		return;
        	}
        }
        if (node == null) {
            if (Modes.mode == "todo") this.dndCanvas.model.clearToken();
        } else if (this.wiki){
            if (stepId) {
                var step = this.dndCanvas.model.getStepById(stepId);
                if (step) {
                    PmCanvas.showWorkzone(null, step);
                    if (Modes.mode == "todo") {
                    	var item = ActivityTree.getActiveActivityItem();
                    	if (item) {
                    		if (item.getStepState(stepId) != null) {
                    			this.dndCanvas.model.showToken(step);
                    		} else {
                    			this.dndCanvas.model.clearToken();
                    		}
                    	} else {
                    		this.dndCanvas.model.clearToken();
                    	}
                    }
                }
            }
        }
        this.lastActiveNode = node;
   //}
};

PmCanvas.prototype.showToken = function(stepId) {
	var step = this.dndCanvas.model.getStepById(stepId);
	if (step) {
		if (Modes.mode == "todo") this.dndCanvas.model.showToken(step);
	}
};

function PmSetAttrListener(pm) {
	this.pm = pm;
}

PmSetAttrListener.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-value-changed") {
    	if (this.pm._attXfrm) {
    		this.pm._attXfrm.dispose();
    		this.pm._attXfrm = null;
    		this.pm._attXfDoc = null;
    		PmCanvas.showWorkzone(null, null);
    	}
    }
};

PmCanvas.prototype.setEntityPanel = function(panel) {
	var tmpl = this.xfrm.getIxTemplate("entityManager");
	tmpl.renderTemplate(null, panel.contentElement);
};

PmCanvas.prototype.setAttrPanel = function(panel) {
	var tmpl = this.xfrm.getIxTemplate("attrManager");
	tmpl.renderTemplate(null, panel.contentElement);
	tmpl.addEventListener("xforms-value-changed", new PmSetAttrListener(this));
};

PmCanvas.prototype.getAttrType = function(name) {
	var doc = this.getModelDocument();
	var dat = Xml.matchOne(doc.documentElement, "data");
	if (!dat) return null;
	var attr = Xml.matchOne(dat, "attr", "name", name);
	return attr ? attr.getAttribute("type") : null;
};

PmCanvas.prototype.__buildAttrXform = function(flowdoc, activityNode, sub) {
	var doc = flowdoc;
	var dat = Xml.matchOne(doc.documentElement, "data");
	if (!dat) return null;
	
	var xb = new XMLBuilder();
	var formUri = "../view-wf/attr.xfrm";
	
	// Subs always get fresh doc
	var xfDoc = sub ? null : this._attXfDoc;
	
	if (!xfDoc) {
		xfDoc = xb.loadURI(formUri);
		var fMod = Xml.matchOne(xfDoc.documentElement, "model");
		if (!fMod) return null;
		
		if (!sub) this._attXfDoc = xfDoc;
		
		var optIns = Xml.matchOne(fMod, "instance", "id", "opt");
		var optRoot = Xml.matchOne(optIns, "options");
		
		var attrs = Xml.match(dat, "attr");
		
		// convert types to binds
	    for (var ii = 0; ii < attrs.length; ii++) {
	    	var attr = attrs[ii];
	    	var atName = attr.getAttribute("name");
    		if (xsdTypes.itensilXF.varName.regex.test(atName)) {
		    	var bnEl = SH.is_ie ?
		    		xfDoc.createNode(1, "bind", "http://www.w3.org/2002/xforms") :
		    	 	xfDoc.createElementNS("http://www.w3.org/2002/xforms", "bind");
		    	 	
		    	var attType = attr.getAttribute("type");
		    	bnEl.setAttribute("type", attType);
		    	bnEl.setAttribute("nodeset", atName);
		    	fMod.appendChild(bnEl);
		    	if (attType == "xsd:NMTOKEN" || attType == "xsd:NMTOKENS") {
		    		var itmset = Xml.element(optRoot, "itemset");
		    		itmset.setAttribute("name", atName);
		    		Xml.importNodes(xfDoc, itmset, Xml.match(attr, "item"));
		    	}
	    	}
	    }
	}
	
	var srcUri;
	if (activityNode == null) {
		// not meant for sub-process
		srcUri = Uri.absolute(Uri.parent(this.getFlow().uri), "template");
        srcUri = Uri.absolute(srcUri, dat.getAttribute("src"));
    } else {
        var id = activityNode.getAttribute("id");

        // link to item folder's file
        var info = ActivityTree.activityInfo(id);
        srcUri = Uri.absolute(info.getAttribute("uri"), dat.getAttribute("src"));
    }
    var xfrm = new XForm(xfDoc, "attrxf", xb, formUri);
	xfrm.setDefaultUris("../fil" + srcUri, "../fil" + srcUri);
	xfrm.render(makeElement(null,"div"));
	
	return xfrm;
};


PmCanvas.prototype.getAttrXform = function() {
	if (this._attXfrm) return this._attXfrm;
	this._attXfrm = this.__buildAttrXform(this.getModelDocument(), App.activeActivityNode);
	return this._attXfrm;
};


PmCanvas.prototype.saveRuleset = function() {
	var holdThis = this;
	var diag = Dialog.prompt("Rule file name:",{body:"MyRules", suffix:".rule"}, function(name) {
		
		var tstUri = Uri.absolute(holdThis.xfrm.__defPath, name);
		
		// transfer attributes
		var srcDat = Xml.matchOne(holdThis.getModelDocument().documentElement, "data");
		
		var sendDoc = Data.rulesFromAttrs(srcDat);
		
		Data.saveDoc(sendDoc, "../fil" + tstUri, PmCanvas.ruleSaved);
		
	});
	diag.show(200, 200);
};

PmCanvas.ruleSaved = function(doc, uri) {
	FileTree.edit(null, {uri:uri.substring(6)});
};


PmCanvas.flowRoleClick = function(evt) {
	var tmr = PmCanvas.teamRoster;
    tmr.getTeamFlowMenu(
    	tmr.setFlowRole, tmr.clearFlowRole, tmr.setActivityRole2).popUp(evt, this);
    return false;
};

PmCanvas.activityRoleClick = function(evt) {
	var tmr = PmCanvas.teamRoster;
    tmr.getTeamMenu(tmr.setActivityRole, tmr.clearActivityRole).popUp(evt, this);
    return false;
};

PmCanvas.__collectGroupRoles = function(grpNode, roles) {
	var sNodes = Xml.match(grpNode, "*", "id");
	for (var ii = 0; ii < sNodes.length; ii++) {
		var sn = sNodes[ii];
		var ln = Xml.getLocalName(sn);
		if (ln == "activity") {
			var rn = sn.getAttribute("role");
            if (rn) roles[rn] = true;
		} else if (ln == "group") {
			PmCanvas.__collectGroupRoles(sn, roles);
		}
	}
};

PmCanvas.renderTeam = function(procMod, uiParent, skipLoad) {
    uiParent.innerHTML = "";
    if (!PmCanvas.teamRoster) {
    	PmCanvas.teamRoster = new TeamRoster(uiParent, dndGetCanvas(document.body));
    }
    var tmr = PmCanvas.teamRoster;
    
    if (skipLoad) return;
    
    makeElement(uiParent, "div", "minorHead", "Default Roles");
    var hElem = makeElement(uiParent, "div", "minorTreeBox");
    var roles = new Object();
    if (procMod.modelNode !== procMod.modelRoot) {
    	PmCanvas.__collectGroupRoles(procMod.modelRoot, roles);
    } else {
	    for (var stId in procMod.steps) {
	        var step = procMod.steps[stId];
	        if (step.constructor === PmStep) {
	            var rn = step.getRole();
	            if (rn) roles[rn] = true;
	        } else if (step.constructor === PmGroup) {
	        	PmCanvas.__collectGroupRoles(step.xNode, roles);
	        }
	    }
    }
    
    tmr.renderRoles(hElem, roles, tmr.getFlowRoles(App.activeFlow), PmCanvas.flowRoleClick);

    if (App.activeActivityNode != null || App.activeActivityId) {
    	
        var iRoles = tmr.getActivityRoles(App.activeActivityNode ? App.activeActivityNode.getAttribute("id") :
        		App.activeActivityId);
        if (iRoles != null) {
            makeElement(uiParent, "div", "minorHead", "Activity Roles");
            hElem = makeElement(uiParent, "div", "minorTreeBox");
            tmr.renderRoles(hElem, iRoles, iRoles, PmCanvas.activityRoleClick);
        }
    }
};


PmWikiArt.prototype = new WikiArticle();
PmWikiArt.prototype.constructor = PmWikiArt;

function PmWikiArt(step) {
    this.step = step;
    this.id = step.getId();
    if (step.constructor === PmTimer) {
    	this.editLock = true;
    } else {
	    this.node = Xml.matchOne(step.xNode, "article");
	    if (this.node == null) {
	       this.node = step.model.xfMod.duplicateNode("instance('pal')/activity/iw:article", ".", null, step.xNode);
	    }
	    this.path = Uri.parent(step.model.uri);
	    this.lockId = true;
	    this.editLock = Modes.mode != "edit";
    }
}

PmWikiArt.prototype.getDisplayTitle = function() {
    var title = this.id;
    if (!App.edu && this.editLock && App.activeActivityNode != null) {
        title = App.activeActivityNode.getAttribute("name") + " - " + title;
    }
    return title;
};

PmWikiArt.prototype.setAttribute = function(name, value) {
    if (name != "id") {
        this.node.setAttribute(name, value);
    }
};

PmWikiArt.prototype.getAttribute = function(name) {
	if (this.step.constructor === PmTimer) {
		return "";
	} else {
		return WikiArticle.prototype.getAttribute.apply(this, [name]);
	}
};

PmWikiArt.prototype.getContent = function() {
	if (App.activeActivityNode != null) {
		var state = Xml.matchOne(App.activeActivityNode, "state", "stepId", this.id);
		if (state) {
			if (state.getAttribute("subState") == "WAIT_ENTER_STEP") {
				return "{{{warning:\nWaiting for preceding steps.\n{{{\n";
			} else if (state.getAttribute("subActivityId")
				|| this.step.xNode.getAttribute("apptype") == "launch") {
				return "[script[PmWikiArt.renderSubProcess(parent,context)]]";
			}
		}
	}
	if (this.step.constructor === PmEnd) {
		return "{{{warning:\nThis " + (ActivityTree.isCourse ? "course" : "activity") + " is at the end.\n{{{\n";
	} else if (this.step.constructor === PmTimer) {
		return "[script[PmWikiArt.renderTimer(parent,context)]]";
	} else if (this.step.constructor === PmSwitch) {
		return "[script[PmWikiArt.renderSwitch(parent,context)]]";
	} else {
		return WikiArticle.prototype.getContent.apply(this, []);
	}
};

PmWikiArt.prototype.save = function() {
    this.step.model.setChanged(this.step);
};


PmWikiArt.renderSwitch = function(parent, context) {
	if (App.activeActivityNode != null) {
		var state = Xml.matchOne(App.activeActivityNode, "state", "stepId", context.art.id);
        if (state) {
        	makeElement(parent, "div", null, "Please select a path:");
        	var sel = new ComboBox();
    		sel.render(makeElement(parent, "div", "pathSel"));
    		var step = context.art.step;
    		var paths = Xml.match(step.xNode, "path");
    		sel.addOption(" - Select -", "", true);
    		for (var ii = 0; ii < paths.length; ii++) {
    			var lab = Xml.stringForNode(Xml.matchOne(paths[ii], "label"));
    			if (!lab) lab = "To: " + Uri.name(paths[ii].getAttribute("to"));
    			sel.addOption(lab, paths[ii].getAttribute("id"));
    		}
        	WikiEx_submit.render(parent, context, "Select and continue >", sel);
        } else {
        	makeElement(parent, "div", null, "Current activity not in this switch.");
        }
	} else {
		makeElement(parent, "div", null, "No current activity selected.");
	}
};

PmWikiArt.renderSubProcess = function(parent, context) {
	if (App.activeActivityNode != null) {

		var state = Xml.matchOne(App.activeActivityNode, "state", "stepId", context.art.id);
		
		// render original article
		Wiki.wikify(WikiArticle.prototype.getContent.apply(context.art, []),
			Util.toggleDiv(parent, "Step Workzone", state && ActivityTreeItem.subActDone(state)), context);
		
		if (state) {
			// check if sub-process running
			var subActId = state.getAttribute("subActivityId");
			if (subActId) {

				var helpBtn = makeElementNbSpd(parent, "div", "wikiHelp", null, {title : "Help"});
				makeElement(parent, "h5", "", "Sub-process Status:");
		      	setEventHandler(helpBtn, "onclick",
		            function(evt) {
		                evt.cancelBubble = true;
		                App.chromeHelp("Sub-process", "Workzone");
		                return false;
		            });
		            
				// Link to sub-process
				ActivityTree.renderActivityInfo(subActId, makeElement(parent, "div", "sectSubProc"), "Sub-process", "wiki");
			
				// Form to set sub-process Attributes
				var arg = App.disposableNode(makeElement(parent, "div", "", "Loading attributes..."));
				arg.pm = context.art.step.model.canvasObj;
				arg.subActId = subActId;
				ActivityTree.xb.loadURIAsync(ActivityTree.getModelUri(subActId), PmWikiArt.renderSubAttr, arg);
			
			} else {
				makeElement(parent, "div", "", "There is no sub-process running yet, " +
					"click the 'Use a process for this task' link above.");
			}
			
		} else {
			makeElement(parent, "div", "", "This step will link to a sub-process when it starts.");
		}

		
	}
	
};

PmWikiArt.renderSubAttr = function(doc, arg) {
	var uiParent = arg.node;
	uiParent.innerHTML = "";
	var dat = Xml.matchOne(doc.documentElement, "data");
	if (!dat) return;
	var attrs = Xml.match(dat, "attr");
	if (attrs.length < 1) {
		return;	
	}
	makeElement(uiParent, "p");
	makeElement(uiParent, "h5", "", "Sub-process Attributes:");
	var attElem = makeElement(uiParent, "div", "sectSubProc");
	var xfrm = arg.pm.__buildAttrXform(doc, ActivityTree.activityInfo(arg.subActId, true), true);
		
	// convert types to binds
    for (var ii = 0; ii < attrs.length; ii++) {
    	var attr = attrs[ii];
    	var atName = attr.getAttribute("name");
    	if (xsdTypes.itensilXF.varName.regex.test(atName)) {
    		WikiEx_input.attRender(attElem, xfrm, arg.pm, attr.getAttribute("type"), atName);
    	}
    }

	var tmp = xfrm.getIxTemplate("save");
	tmp.renderTemplate(null, attElem);
};

PmWikiArt.renderTimer = function(parent, context) {
	if (App.activeActivityNode != null) {
		var state = Xml.matchOne(App.activeActivityNode, "state", "stepId", context.art.id);
        if (state) {
        	var step = context.art.step;
        	
        	// check for conditional
        	if (step.xNode.getAttribute("mode") == "until") {
        		var until = Xml.matchOne(step.xNode, "until");
        		if (until && until.getAttribute("type") == "condition") {
        			makeElement(parent, "div", null, 
        				"This timer uses a business rule, which may depend on other activities and services.");
        			return;
        		}
        	}
        	var actTimer = {actId:App.activeActivityNode.getAttribute("id"), timId:step.getId(), _step:step};
        	var atTime = ActivityTree.getActivityTimer(actTimer.actId, actTimer.timId);
        	var div = makeElement(parent, "div", "actTimer");
        	makeElement(div, "span", null, "Timer set for ");
        	var dt = DateUtil.parse8601(atTime, true);
        	if (dt) {
	        	actTimer.dt = DateUtil.to8601(dt, true);
        		makeElement(div, "b", null, DateUtil.getShortDay(dt) + ", " + DateUtil.toLocaleShort(dt, true));
        	} else {
        		actTimer.dt = "";
        		makeElement(div, "b", null, "Not Set");
        	}
			var link = makeElement(div, "div", "actTimLink", "Manually adjust timer >");
			link._actTimer = actTimer;
			setEventHandler(link, "onclick", PmWikiArt.clickTimerLink);
        } else {
        	makeElement(parent, "div", null, "Current activity not in this timer.");
        }
	} else {
		makeElement(parent, "div", null, "No current activity selected.");
	}
};

PmWikiArt.clickTimerLink = function(evt) {
	var diag = xfDialog(
		"Adjust Timer", true, document.body, "../view-wf/timer.xfrm", ActivityTree.xb, this._actTimer, 
			null, null, false, App ? App.chromeHelp : null);
	diag.show(getMouseX(evt), getMouseY(evt));
};


/**
 * Step DNDTypeHandler
 */
PmObjDNDType.prototype = new DNDTypeHandler();
PmObjDNDType.TYPE = "pmObj";

function PmObjDNDType(pmCanvas) {
    this.type = PmObjDNDType.TYPE;
    this.pmCanvas = pmCanvas;
}

PmObjDNDType.prototype.startDrag = function(x, y, dragElem) {
    var peer = dragElem.peer;
    peer.calcOffset(x, y);
    peer.lastSX = PM.SNAP(x);
    peer.lastSY = PM.SNAP(y);
    peer.model.clearFocus();
};

PmObjDNDType.prototype.dragMove = function(x, y, dragElem) {
    var peer = dragElem.peer;
    if (peer.dndOffX + x < 0) x = -peer.dndOffX;
    if (peer.dndOffY + y < 0) y = -peer.dndOffY;
    var sx = PM.SNAP(x);
    var sy = PM.SNAP(y);
    if (peer.lastSX != sx || peer.lastSY != sy) {
        peer.lastSX = sx;
        peer.lastSY = sy;
        peer.setDragPosition(sx, sy, peer._inDndGroup != null && peer._inDndGroup);
    }
};

PmObjDNDType.prototype.dropTest = function(dropElem, dragElem) {
    if (dragElem._dndType.constructor === PmPalTreeDNDType) {
        // return true;
        // TODO external checks
        return false;
    } else if (dragElem.peer != null) {
        return dropElem.peer.dropTest(dragElem.peer);
    }
    return false;
};

PmObjDNDType.prototype.dropCancel = function(dropElem) {
    var cn = dropElem.className;
    dropElem.className = cn.substring(0, cn.length - 4);
};

PmObjDNDType.prototype.dropReady = function(dropElem) {
    var cn = dropElem.className;
    dropElem.className = cn + "_drp";
};

PmObjDNDType.prototype.dropDone = function(dropElem) {
    var cn = dropElem.className;
    dropElem.className = cn.substring(0, cn.length - 4);
};

PmObjDNDType.prototype.dropExec = function(dropElem, dragElem) {
    dropElem.peer.dropExec(dragElem.peer);
};

PmObjDNDType.prototype.noTargetDrop = function(dragElem) {
    var peer = dragElem.peer;
    if (peer._inDndGroup) {
        peer.groupDrop();
    } else {
        peer.noTargetDrop();
    }
};

PmObjDNDType.prototype.getDragElement = function(dragElem, canvas, evt) {
    var peer = dragElem.peer;

	var pthDrg = false;
	if (peer == "hovHook") {
		pthDrg = true;
		peer = dragElem.step;
	} else if (evt.altKey && (peer.constructor === PmStep
			|| peer.constructor === PmGroup
            || peer.constructor === PmStart
            || peer.constructor === PmTimer
            || peer.constructor === PmSwitch)) {
		pthDrg = true;	
	}
	
	if (peer.clearHoverHooks) peer.clearHoverHooks();
	
    // alt drag draws paths
    if (pthDrg) {
        var mPnt = canvas.getMousePoint(evt);
        var path = PmPath.initNew(mPnt.clone(), canvas);
        path.setFrom(peer, true);
        peer.snapHook(path.startHook);
        path.endHook.setPoint(mPnt, -1);
        return path.endHook.element;
    }
    return dragElem;
};


PmObjDNDType.prototype.canDropTest = function(dragElem) {
    var peer = dragElem.peer;
    return !(peer.constructor === PmPathEdge
            || peer.constructor === PmNote);
};

PmObjDNDType.prototype.setGrouped = function(dragElem, grouped) {
    var peer = dragElem.peer;
    var cn = dragElem.className;
    if (grouped) {
        peer._inDndGroup = true;
        if (peer.constructor === PmJoinHook) peer.path._inDndGroup = true;
        dragElem.className = cn + "_grp";
   } else {
        peer._inDndGroup = false;
        if (peer.constructor === PmJoinHook) peer.path._inDndGroup = false;
        dragElem.className = cn.substring(0, cn.length - 4);
    }
};

var PmPalBundle = {

	initNew : function(pnt, canvas, bundId) {
		var ii;
		var pmod = canvas.model;
		var xfMod = pmod.xfMod;
		var ssNode = pmod.modelNode;
		
		var srcList = xfMod.selectNodeList("instance('pal')/bundle[@id='" + bundId + "']/*");
		var bList = [];
		for (ii = 0; ii < srcList.length; ii++) {
			bList.push(xfMod.__duplicateNode(ssNode, srcList[ii]));
		}

		var sObjs = pmod.digestStepList(bList, true);
		
		pmod.dndGroupList(sObjs, pnt.x, pnt.y);
		
		xfMod.rebuild();
	}

};

var PmPalBundleBranch = {

	initNew : function(pnt, canvas, arg) {
		PmPalBundle.initNew(pnt, canvas, "branch");
	}

};

var PmPalBundleLoop = {

	initNew : function(pnt, canvas, arg) {
		PmPalBundle.initNew(pnt, canvas, "loop");
	}

};

PmPalTreeModel.prototype = new TreeModel();
PmPalTreeModel.prototype.constructor = PmPalTreeModel;

function PmPalTreeModel() {
    TreeModel.apply(this, []);
    var core = new TreeItem(this, "Core", true, "fldIco");
    core.renderLabel = PmPalTreeModel.palTreeRenderLabel;
    core.expanded = true;
    this.add(core);
    var ti = new TreeItem(this, "Path", false, "pathIco");
    ti.pmClass = PmPath;
    core.add(ti);

    ti = new TreeItem(this, "Step", false, "stepIco");
    ti.pmClass = PmStep;
    core.add(ti);

    ti = new TreeItem(this, "Decision", false, "decisIco");
    ti.pmClass = PmSwitch;
    core.add(ti);

    ti = new TreeItem(this, "End", false, "endIco");
    core.add(ti);
    ti.pmClass = PmEnd;
    
    ti = new TreeItem(this, "Group", false, "sGroupIco");
    core.add(ti);
    ti.pmClass = PmGroup;
    

    ti = new TreeItem(this, "Sub Process", false, "subIco");
    core.add(ti);
    ti.pmArg = "launch";
    ti.pmClass = PmStep;
    
    var bund = new TreeItem(this, "Bundles", true);
    bund.renderLabel = PmPalTreeModel.palTreeRenderLabel;
    bund.expanded = true;
    this.add(bund);
    
    ti = new TreeItem(this, "Branch", false, "bnBranchIco");
    ti.pmClass = PmPalBundleBranch;
    bund.add(ti);
    
    ti = new TreeItem(this, "Loop", false, "bnLoopIco");
    ti.pmClass = PmPalBundleLoop;
    bund.add(ti);

    var annotate = new TreeItem(this, "Annotate", true);
    annotate.renderLabel = PmPalTreeModel.palTreeRenderLabel;
    annotate.expanded = true;
    this.add(annotate);

    ti = new TreeItem(this, "Note", false, "noteIco");
    ti.pmClass = PmNote;
    annotate.add(ti);
    ti = new TreeItem(this, "Note Line", false, "lineIco");
    ti.pmClass = PmLine;
    annotate.add(ti);

    var advanced = new TreeItem(this, "Advanced", true);
    advanced.renderLabel = PmPalTreeModel.palTreeRenderLabel;
    ti = new TreeItem(this, "Timer", false, "timerIco");
    ti.pmClass = PmTimer;
    advanced.add(ti);



    /*
    ti = new TreeItem(this, "Join", false, "joinIco");
    ti.pmClass = PmJoin;
    advanced.add(ti);
    */
    this.add(advanced);
}

PmPalTreeModel.palTreeRenderLabel = function(hParent) {
    if (this.__labDomE == null) {
        this.__labDomE = makeElement(hParent, "div", "palLabel", this.label);
    } else if (this.__labDomE.parentNode != hParent){
        hParent.appendChild(this.__labDomE);
    }
};

PmPalTreeDNDType.prototype = new TreeDNDType();
PmPalTreeDNDType.prototype.constructor = PmPalTreeDNDType;

function PmPalTreeDNDType(cssClass) {
    this.type = "pmPalDnd";
    this.cssClass = cssClass;
}

PmPalTreeDNDType.prototype.canDrag = function(dragElem) {
    var item = dragElem.__item;
    return !item.allowsKids; // no folder drag
};

PmPalTreeDNDType.prototype.dropTest = function(dropElem, dragElem) {
    return false;
};

PmPalTreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
    //empty
};

PmPalTreeDNDType.prototype.noTargetDrop = function(dragElem, canvas) {
    if (canvas.model != null) {
        var rect = getBounds(dragElem); // global bounds
        var pnt = canvas.localizePoint(new Point(rect.x, rect.y));
        pnt.x = PM.SNAP(pnt.x);
        pnt.y = PM.SNAP(pnt.y);
        var dragItem = dragElem._actElem.__item;
        if (dragItem.pmClass != null) {
            dragItem.pmClass.initNew(pnt, canvas, dragItem.pmArg);
        }
    }
};


/**
 * Step DNDTypeHandler
 */
WfAttrDNDType.prototype = new DNDTypeDummy();

function WfAttrDNDType() {
    this.type = "dndWfAttr";
}

WfAttrDNDType.getName = function(dragElem) {
	return dragElem._actElem._ctxNode.node.getAttribute("name");
};

/**
 * XForm boosters
 */
XForm.addXpathFunc("parent", function() {
	var ns = new XNodeSet();
	if (App.activeActivityNode) {
		var id = App.activeActivityNode.getAttribute("id");
	    var pid = App.activeActivityNode.getAttribute("parent");
        if (pid) id = pid;
        var info = ActivityTree.activityInfo(id);
        var src = Uri.absolute(info.getAttribute("uri"), "rules.xml");
        var doc = this.__xb.loadURI(src);
        ns.add(doc.documentElement);
  	}
	return ns;
});
 

 
 
XFIXUniquePath.prototype = new XFTypeFormat();
XFIXUniquePath.prototype.constructor = XFIXUniquePath;

function XFIXUniquePath() {
}

XFIXUniquePath.prototype.decorate = function(uiElem, ctrl) {
	ctrl.__pathElem = makeElement(uiElem.parentNode, "div", "uniPath", this.getPath(ctrl), uiElem);
};

XFIXUniquePath.prototype.disposeDecor = function(uiElem, ctrl) {
	ctrl.__pathElem = null;
};

XFIXUniquePath.prototype.getPath = function(ctrl) {
	if (ctrl.__baseUid) return ctrl.__baseUid + "/";
	return "";
};

XFIXUniquePath.prototype.format = function(str, ctrl) {
	if (!Uri.name(str)) return "";
	ctrl.__oldUId = str;
    ctrl.__baseUid = Uri.parent(str);
    if (ctrl.__pathElem) {
    	setElementText(ctrl.__pathElem, this.getPath(ctrl));
    }
    return Uri.name(str);
};

XFIXUniquePath.prototype.parse = function(str, ctrl) {
	if (!Uri.name(str)) return ctrl.__baseUid + "/";
	str = str.replace('/', '\\');
	var fullId = Uri.absolute(ctrl.__baseUid, str);
    if (ctrl.__oldUId != fullId) {
        var idCtx = ctrl.__model.getIdContext();
        idCtx.removeVar(ctrl.__oldUId);
        fullId = idCtx.uniqueVar(fullId);
        idCtx.addVar(fullId);
        ctrl.__oldUId = fullId;
    }
    return fullId;
};

XFTypeFormat.addFormat(
        XFORM_ITENSIL_NAMESPACE, "uniquePath", new XFIXUniquePath());

 
