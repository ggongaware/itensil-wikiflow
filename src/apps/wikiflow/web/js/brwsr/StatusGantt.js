/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Activity Status
 * 
 */

var StatusTree = {
	
	isMeet : false,
	__statGrid : null,
	__statTree : null,
	__teamRoster : null,
	__uiParent : null,
	__panel : null,
	__flow : null,
	__statCb: null,
	_refreshCount : 0,

	renderStatus : function(flow, activityId, teamRoster, uiParent, panel) {
		this.__uiParent = uiParent;
		this.__teamRoster = teamRoster;
		this.__flow = flow;
		this.__panel = panel;
		
		uiParent.style.overflow = "hidden";
		
		if (!activityId) {
			uiParent.innerHTML = "<div style='padding:16px;font-size:12px'>&lt; Select or Launch an activity from the To Do List.</div>";
			return;
		} else {
			uiParent.innerHTML = "";
		}
		
		var pdoc = ActivityTree.xb.loadURI(
			ActivityTree.serviceUri + "getLogsAndPlans?id=" + Uri.escape(activityId));
        if (App.checkError(pdoc, true)){
    		if (App.lastErrorType == "itensil.workflow.activities.ActivityInvalidException") {
    			ActivityTree.invalidActivity(activityId);
    		} else {
    			App.showLastError();
    		}
        	return null;
        }
        
        //var hElem = makeElement(uiParent, "div", "statReport");
		
		var modl = new StatusTreeModel(flow, activityId, teamRoster, pdoc);	
		modl.planning = ActivityTree.planning;	
		var gmod = modl;
		
		objectExtend(gmod, GanttModelInterface);
		gmod.minDate = DateUtil.asUTC(DateUtil.parse8601(pdoc.documentElement.getAttribute("minDate")));
		gmod.maxDate = DateUtil.asUTC(DateUtil.parse8601(pdoc.documentElement.getAttribute("maxDate")));
		
		var nextWeek = DateUtil.addTime(DateUtil.asUTC(new Date()), 7 * DateUtil.DAYS);
		
		if (DateUtil.dayDiff(gmod.maxDate, nextWeek) <= 0) {
			gmod.maxDate = nextWeek;
		}
		
		var totalDays = DateUtil.dayDiff(gmod.maxDate, gmod.minDate);
		
		// less than 5 weeks?
		if (totalDays < 35) {
			gmod.maxDate = DateUtil.addTime(gmod.maxDate, (35 - totalDays) * DateUtil.DAYS);
		}
		
		gobj = new Gantt(gmod);
        gobj.grid.addHeader("Action");
        if (!StatusTree.__statCb) {
	       	StatusTree.__statCb = new ComboBox();
	        StatusTree.__statCb.addOption("Status (all)", "", true);
	        StatusTree.__statCb.addOption("Active", "act");
	        StatusTree.__statCb.addOption("Completed", "comp");
	        StatusTree.__statCb.onchange = function() {
	       		StatusTree.gantt.tree.redrawAll();
	        };
        }
        gobj.grid.addHeader("Status", null, StatusTree.__statCb);
        gobj.grid.addHeader("Start");
        gobj.grid.addHeader("End");
        //grid.addHeader("Started\u00a0/\u00a0Ended", "startEnd");
        //grid.addHeader("Progress");
        gobj.grid.addHeader("Assigned\u00a0To", "assign");
        this.gantt = gobj;
        
        panel.linkResize(gobj.ps);
        
        gobj.render(uiParent)
		
		App.addDispose(gobj);

		uiParent = null;
		
		var mopts = [
                 {label : "Workzone", icon : "mb_workIco", act :StatusTree.workzone}];

		StatusTree.gantt.tree.menu = new Menu(new MenuModel(mopts));
		StatusTree.gantt.tree.menu.model.onSetContext = function(item) {
			 this._menu.setDisable(this.items[0], !(!item.isSubProc && Xml.getLocalName(item.node) == "activity"));
		};
		
    	return StatusTree.gantt.tree;
	},
	
	refreshStatus : function(activityId) {
		if (this._refreshCount > 0) {
			if (this.__statGrid) {
				this.__statGrid.remove();
				this.__statGrid = null;
			}
			if (this.__statTree) {
				this.__statTree.remove();
				this.__statTree = null;
			}
			ActivityTree.planning.clearUpdates();
			this.renderStatus(this.__flow, activityId, this.__teamRoster, this.__uiParent, this.__panel);
		}
		this._refreshCount++;
	},
	
	workzone : function(evt, item) {
   		var diag = new Wiki.dialog("../fil" + Uri.absolute(Uri.parent(App.activeFlow), "activities/activities.kb"), "Workzone",
            [{isStatic:true, isFullPage:(App.guest || App.edu), isWorkzone:true,
                linkWiki : ("../fil" + Uri.absolute(Uri.root(App.activeFlow), "Reference.kb"))}], {preSearchCss:"searchActi", noSearch : true});
      	
      	var pCan = StatusTree.__flow.canvasObj;
      	pCan.wiki = diag._wiki;
		diag.show(100, 100);
		diag.onClose = function() {
			pCan.wiki = null;
		};
		pCan.setWFActivity(App.activeActivityNode, item.stepId);
	},
	
	savePlans : function () {
	    
	    if (!ActivityTree.planning.dirty) {
	    	Ephemeral.insideMessage(this.__uiParent , "No changes found, save not required.");
	    	return;
	    }
	    
	    var statDoc = ActivityTree.xb.loadXML("<status/>");
		ActivityTree.planning.saveToDoc(statDoc);
		var res = ActivityTree.xb.loadURIPost(
			ActivityTree.serviceUri + "saveStatus?activity=" + App.activeActivityId, statDoc);
		if (!App.checkError(res)) {
			ActivityTree.planning.clearUpdates();
			Ephemeral.insideMessage(this.__uiParent, "Status Saved.");
			// TODO refresh activity tree
			ActivityTree.updateActivity(ActivityTree.activityInfo(App.activeActivityId, false, true));
			
		}
	}
	
};

StatusTreeModel.prototype = new TreeGridModel();
StatusTreeModel.prototype.constructor = StatusTreeModel;

function StatusTreeModel(flow, activityId, teamRoster, lpDoc) {
	if (flow) {
		TreeGridModel.apply(this, []);
		this.flow = flow;
		this.nCols = 5;
		this.lpElem = lpDoc.documentElement;
		this.teamRoster = teamRoster;
		
		this.plansElem = Xml.matchOne(this.lpElem, "plans");
		this.logsElem = Xml.matchOne(this.lpElem, "logs");
		
		this.activityId = activityId;
	}
}

StatusTreeModel.prototype.onReady = function(callback, tree, itemParent) {
	var startEl, rootEl;
	if (itemParent === this) {
		this.stepIdx = new Object();
		this.filtValue = StatusTree.__statCb ? StatusTree.__statCb.getValue() : null;
		rootEl = this.flow.modelRoot;
		startEl = Xml.matchOne(rootEl, "start");
	} else {
		rootEl = itemParent.node;
		startEl = Xml.matchOne(rootEl, "enter");
	}
	this.followPaths(startEl, itemParent, rootEl);
	callback.apply(tree, [itemParent.kids, itemParent]);
	this.model.addItemRows(itemParent.kids, itemParent);
	if (itemParent === this.model) { // expand top levels
		for (var ii = 0; ii < itemParent.kids.length; ii++) {
			if (itemParent.kids[ii].allowsKids) tree.toggle(itemParent.kids[ii]);
		}
	}
};

StatusTreeModel.prototype.followPaths = function(stepElem, itemParent, elemRoot) {
	var pathEls = Xml.match(stepElem, "path");
	var tItm;
	
	for (var ii = 0; ii < pathEls.length; ii++ ) {
		var toId = pathEls[ii].getAttribute("to");
		// detect group
		if (Uri.name(toId) == "$$S") toId = Uri.parent(toId);
		if (!this.stepIdx[toId]) {
			var nxtStepEl = Xml.matchOne(elemRoot, "*", "id", toId);
			if (nxtStepEl) {
				var nm = Xml.getLocalName(nxtStepEl);
				if (nm == "end" || nm == "exit") continue;
				if (ii > 0) {
					// add parallel sep
					var sepItm = new TreeGridItem(itemParent.model, "", false, "para");
					sepItm.rowCssClass = "paraSep";
					sepItm.cssClass = "paraSep";
					itemParent.add(sepItm);
				}
				
				tItm = new StatusTreeItem(this, itemParent.model, nxtStepEl, nm == "group", nm + "StpIco");
				tItm.node = nxtStepEl;
				this.stepIdx[toId] = tItm;
				if (nm == "group") {
					itemParent.add(tItm);
					nxtStepEl = Xml.matchOne(nxtStepEl, "exit");
				} else if (!this.filtValue || tItm.filterStatus(this.filtValue)) {
					itemParent.add(tItm);
				}
				this.followPaths(nxtStepEl, itemParent, elemRoot);
			}
		}
	}
};

StatusTreeModel.prototype.dispose = function() {
	this.lpElem = null;
	TreeGridModel.prototype.dispose.apply(this, []);
};

StatusTreeModel.prototype.getStepStatus = function(stepId, item) {
	var state = Xml.matchOne(this.lpElem, "state", "stepId", stepId);
	if (state) {
		item._subState = state.getAttribute("subState");
		if (item._subState == "ENTER_STEP") {
	 		item._startTime = state.getAttribute("timeStamp");
	 	}
	} else {
		
		var logs = Xml.match(this.logsElem, "log", "stepId", stepId);
		for (var ii=0; ii < logs.length; ii++) {
		 	item._subState = logs[ii].getAttribute("subState");
			if (item._subState == "ENTER_STEP") {
		 		item._startTime = logs[ii].getAttribute("timeStamp");
		 	} else if (item._subState == "EXIT_STEP") {
		 		item._endNode = logs[ii];
		 		item._endTime = logs[ii].getAttribute("timeStamp");
		 	}
		}
	}
	
	switch (item._subState) {
		case "ENTER_STEP": return "in\u00a0progress";
		case "EXIT_STEP": return "completed";
	}
	
	return "";
};

StatusTreeModel.prototype.getStepStartEnd = function(stepId, item) {
	var dt, txt = "";
	if (item._startTime) {
		dt = DateUtil.asUTC(DateUtil.parse8601(item._startTime, true));
		txt = DateUtil.toUTCShort(dt);
	}
	if (item._endTime) {
		dt = DateUtil.asUTC(DateUtil.parse8601(item._endTime, true));
		txt += "\u00a0/\u00a0" + DateUtil.toUTCShort(dt);
	}
	return txt;
};

StatusTreeModel.prototype.getStepDue = function(stepId, item) {
	var state = Xml.matchOne(this.lpElem, "state", "stepId", stepId);
	var cp = state ? Xml.matchOne(state, "current") : null;
	var dt = null;
	if (cp) {
		dt = cp.getAttribute("dueDate");
	} else {
		
		var plan = Xml.matchOne(this.plansElem, "plan", "stepId", stepId);
		if (plan) dt = plan.getAttribute("dueDate");
		
	}
	if (dt) {
		return DateUtil.toUTCShort(DateUtil.asUTC(DateUtil.parse8601(dt, false)));
	}
	return "";
};


StatusTreeModel.prototype.getStepStart = function(stepId, item) {
	var state = Xml.matchOne(this.lpElem, "state", "stepId", stepId);
	var cp = state ? Xml.matchOne(state, "current") : null;
	var dt = null;
	if (cp) {
		dt = cp.getAttribute("startDate");
	} else {
		
		var plan = Xml.matchOne(this.plansElem, "plan", "stepId", stepId);
		if (plan) dt = plan.getAttribute("startDate");
		
	}
	if (dt) {
		return DateUtil.toUTCShort(DateUtil.asUTC(DateUtil.parse8601(dt, false)));
	}
	return "";
};

StatusTreeModel.prototype.getStepProgress = function(stepId, item) {
	if (item.isSubProc) return "";
	if (item._subState == "ENTER_STEP") {
		var state = Xml.matchOne(this.lpElem, "state", "stepId", stepId);
		return state ? (state.getAttribute("progress") + "%") : "";
	}
	return "";
};

StatusTreeModel.prototype.hasSubProc = function(stepId, item) {
	return false;
};

StatusTreeModel.prototype.findItem = function(id) {
	return new ActivityTreeItem(null, App.activeActivityNode);
};

StatusTreeModel.prototype.getStepAssigned = function(stepId, item) {
	var role = item.node.getAttribute("role");
	var roleUid = null;
	if (role) {
		roleUid = this.teamRoster.rolesAssigned[role];
	}
	
	var state = Xml.matchOne(this.lpElem, "state", "stepId", stepId);
	var uid = null;
	if (state) {
		uid = state.getAttribute("assignId");
	} else {
		
		var plan = Xml.matchOne(this.plansElem, "plan", "stepId", stepId);
		if (plan) uid = plan.getAttribute("assignId");
		
	} 
	
	// historic ?
	if (!uid && !roleUid && item._endNode) {
		uid = item._endNode.getAttribute("userId");
	}
	
	
	if ((!uid && roleUid) || (uid && uid == roleUid)) {
		return { member: this.teamRoster.getMember(roleUid, true),
			role: role };
	} else if (uid) {
		return { member: this.teamRoster.getMember(uid, true) };
	}
	
	return {role:role};
};

StatusTreeModel.prototype.updateSchedule = function(doc) {
	// digest the updates	
};

StatusTreeItem.prototype = new TreeGridItem();
StatusTreeItem.prototype.constructor = StatusTreeItem;

function StatusTreeItem(statusMod, model, node, allowsKids, icon) {
	this.stepId = node.getAttribute("id");
	if (icon == "switchStpIco") {
		this.gtSkipBar = true;
	}
	this.isSubProc = (node.getAttribute("apptype") == "launch") || statusMod.hasSubProc(this.stepId, this);
	TreeGridItem.apply(this, [model, null, allowsKids, this.isSubProc ? "subStpIco": icon]);
    this.rowCssClass = "norm";
    this.node = node;
    this.statusMod = statusMod;
    
    if (statusMod === model) {
	    this.cells.push(new GridCell(statusMod.getStepStatus(this.stepId, this), null, true));
	    this.cells.push(new StatusStartCell(this));
	    this.cellStart = this.cells[1];
	    this.cells.push(new StatusDueCell(this));
	    this.cellEnd = this.cells[2];
    } else {
    	statusMod.getStepStatus(this.stepId, this);
    	
	    this.cells.push(new StatusStartCell(this));
	    this.cellStart = this.cells[0];
	    this.cells.push(new StatusDueCell(this));
	    this.cellEnd = this.cells[1];
    }
    
    //this.cells.push(new GridCell(model.getStepStartEnd(this.stepId, this), null, true));
    //this.cells.push(new GridCell(model.getStepProgress(this.stepId, this), null, true));
    this.cells.push(new StatusAssignCell(this));
}

StatusTreeItem.prototype.act = function(evt) {
	/*if (!this.isSubProc && Xml.getLocalName(this.node) == "activity") 
		StatusTree.workzone(evt, this);
	*/
	if (this.gtHasBar()) {
		this.gtScrollTo();
	}
};

StatusTreeItem.prototype.editAct = null;
StatusTreeItem.prototype.optAct = treeMenuAction;

StatusTreeItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
    	var desc = Xml.matchOne(this.node, "description");
    	desc = desc ? Xml.stringForNode(desc) : "";
    	if (desc.length > 120) desc = desc.substring(0,120) + "...";
        this.__labDomE = H.div({klass:"label " + this.rowCssClass},
                H.div({klass:"name"}, Uri.name(this.stepId)),
                H.div({klass:"desc"}, (desc))
                );
        hParent.appendChild(this.__labDomE);
    } else if (this.__labDomE.parentNode != hParent){
        hParent.appendChild(this.__labDomE);
    }
};

StatusTreeItem.prototype.gtBarStyle = function() {
	return this._subState == "EXIT_STEP" ? "gbDone" : "";
};


StatusTreeItem.prototype.gtDragDone = function() {
	var statusMod = this.statusMod;
	ActivityTree.xb.loadURIAsync(
		ActivityTree.serviceUri + 'ganttSchedule?activity=' + statusMod.activityId +
		'&stepId=' + Uri.escape(this.stepId) +
		'&startDate=' + DateUtil.toUTCShort(this.getStartDate()) + '&dueDate' +  DateUtil.toUTCShort(this.getEndDate()),
		function(xDoc) {
			statusMod.updateSchedule(xDoc);
		});
};


StatusTreeItem.prototype.filterStatus = function(filtVal) {
	if (filtVal == "act") {
		return this._subState != "EXIT_STEP";
	} else if (filtVal == "comp") {
		return this._subState == "EXIT_STEP";
	}
	return true;
};

StatusTreeItem.prototype.getStartDate = function() {
	if (this.cellStart) return DateUtil.parseUTCShort(this.cellStart.getValue());
	var dt = null;
	if (this._startTime) {
		dt = DateUtil.asUTC(DateUtil.parse8601(this._startTime, false));
	} else {
		var sd = this.statusMod.getStepStart(this.stepId, this);
		if (sd) {
			dt = DateUtil.parseUTCShort(sd);
		}
	}
	return dt;
};

StatusTreeItem.prototype.setStartDate = function(dt, nosize) {
	if (this.cellStart) 
		this.cellStart.setValue(DateUtil.toUTCShort(dt), nosize);
};

StatusTreeItem.prototype.setEndDate = function(dt, nosize) {
	if (this.cellEnd) 
		this.cellEnd.setValue(DateUtil.toUTCShort(dt), nosize);
};

StatusTreeItem.prototype.getEndDate = function() {
	if (this.cellEnd) return DateUtil.parseUTCShort(this.cellEnd.getValue());
	var dt = null;
	if (this._endTime) {
		dt = DateUtil.asUTC(DateUtil.parse8601(this._endTime, false));
	} else {
		var sd = this.statusMod.getStepDue(this.stepId, this)
		if (sd) dt = DateUtil.parseUTCShort(sd);
	}
	return dt;
};

/**
 * 
 */
StatusDueCell.prototype = new GridCell();
StatusDueCell.prototype.constructor = StatusDueCell;

function StatusDueCell(item) {
	if (item) {
		this.item = item;
		var dt = item.getEndDate();
		GridCell.apply(this, [dt ? DateUtil.toUTCShort(dt) : "", "due", 
			Xml.getLocalName(item.node) != "activity"]);
	}
}

StatusDueCell.prototype.setValue = function(value, nosize) {
	var dt = DateUtil.parseUTCShort(value);
	if (this.canEdit()) {
		
		if (this.constructor === StatusDueCell) {
			this.item.statusMod.planning.setDates(this.item.stepId, null, DateUtil.asLocal(dt));
		} else if (this.constructor === StatusStartCell) {
			this.item.statusMod.planning.setDates(this.item.stepId, DateUtil.asLocal(dt), null);
		}
		
	}
	GridCell.prototype.setValue.apply(this, [dt ? DateUtil.toUTCShort(dt) : ""]);
	if (!nosize) this.item.gtSizeBar();
};

StatusDueCell.prototype.setTime = function(str) {
	var elem = Grid.__getEditElem()
	elem.value = str;
	elem.focus();
};

StatusDueCell.prototype.getTime = function() {
	return this.getValue();
};

StatusDueCell.prototype.onEdit = function(editElem) {
	calPopHide();
	calPopShow(null, editElem, this, false);	
};

StatusDueCell.prototype.onEditCancel = function(editElem) { calPopHide(); };
StatusDueCell.prototype.onEditFinish = function(editElem) { calPopHide(); };
StatusDueCell.prototype.onEditCanFinish = function(editElem) { return !calPopIsVisible(); };


StatusStartCell.prototype = new StatusDueCell();
StatusStartCell.prototype.constructor = StatusStartCell;

function StatusStartCell(item) {
	 // TODO, smarter start date system
	this.item = item;
	var dt = item.getStartDate();
	GridCell.apply(this, [dt ? DateUtil.toUTCShort(dt) : "", "due", 
		Xml.getLocalName(item.node) != "activity"]);
}


/**
 * 
 */
StatusAssignCell.prototype = new GridCell();
StatusAssignCell.prototype.constructor = StatusAssignCell;

function StatusAssignCell(item) {
	this.item = item;
	GridCell.apply(this, [null, "assign", true]);
}

StatusAssignCell.prototype.getHElem = function() {
	if (Xml.getLocalName(this.item.node) == "activity") {
		if (!StatusAssignCell.__dnd) {
			StatusAssignCell.__dnd = dndGetCanvas(document.body);
			StatusAssignCell.__dndType = new TeamDNDHand();
			StatusAssignCell.__dnd.addDNDType(StatusAssignCell.__dndType);
		}
		var elem;
		var memObj = this.item.statusMod.getStepAssigned(this.item.stepId, this.item);
		if (memObj.member) {
			elem = makeElement(null, "div", "assign");
			memObj.member.render(elem, memObj.role);
		} else {
			if (StatusTree.isMeet || !memObj.role)
				elem = makeElement(null, "div", "assignEmpty", "<drop\u00a0assignment>");
			else
				elem = makeElement(null, "div", "assignEmpty", "Role: " + memObj.role);
				
		}	
		elem.assignObj = this;
		if (StatusTree.isMeet || !memObj.role)
			StatusAssignCell.__dnd.makeDropTarget(elem, StatusAssignCell.__dndType.type);
		return elem;
	} else {
		return makeElementNbSpd(null, "div");
	}
};

StatusAssignCell.prototype.setAssign = function(uid) {
	ActivityTree.planning.setAssign(this.item.stepId, uid);
};


