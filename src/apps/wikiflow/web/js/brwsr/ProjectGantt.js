/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Activity Status
 * 
 */

var ProjectGantt = {
	
	__uiParent : null,
	__panel : null,
	__statCb: null,
	_refreshCount : 0,
	teamRoster : new TeamRoster(),
	

	renderProject : function(project, uiParent, panel) {
		this.__uiParent = uiParent;
		this.__panel = panel;
		
		uiParent.style.overflow = "hidden";
		
		var modl = new ProjectGanttModel(project, false);
		pdoc = modl.reloadDoc;
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
        gobj.grid.addHeader("Activity / Action");
        gobj.grid.addHeader("Start");
        gobj.grid.addHeader("End");
        gobj.grid.addHeader("Assigned\u00a0To", "assign");
        this.gantt = gobj;
        
        panel.linkResize(gobj.ps);
        gobj.treeCssClass = "fileTree";
        
        gobj.render(uiParent)
		
		App.addDispose(gobj);

		uiParent = null;
		/*
		var mopts = [
                 {label : "Workzone", icon : "mb_workIco", act :ProjectGantt.workzone}];

		ProjectGantt.gantt.tree.menu = new Menu(new MenuModel(mopts));
		ProjectGantt.gantt.tree.menu.model.onSetContext = function(item) {
			 this._menu.setDisable(this.items[0], !(!item.isSubProc && Xml.getLocalName(item.node) == "activity"));
		};*/
		
    	return ProjectGantt.gantt.tree;
	},
	
	
	getFlow : function(actId) {
	    var pCan = new PmCanvas(null,
	            makeElement(null,"div"),
	            formUri,
	            modelSvcUri + "getModel?activity=" + actId,
	            modelSvcUri + "setModel?activity=" + actId,
	            actId);
	    pCan.panelSet = this.__panel.__parent.__ps;
	    pCan.hidden = true;
	    var proc = new PmModel(pCan, actId, pCan.xfrm);
	    return proc;
	},
	
	savePlans : function () {
		
		var dirtyPlans = [];
		var itemIndex = this.gantt.model.itemIndex;
		
		for (var id in itemIndex) {
			var itm = itemIndex[id];
			if (itm.planning && itm.planning.dirty) {
				dirtyPlans.push({id:id, planning : itm.planning});
			}
		}
	    
	    if (dirtyPlans.length < 1) {
	    	Ephemeral.insideMessage(this.__uiParent , "No changes found, save not required.");
	    	return;
	    }
	    for (var ii = 0; ii < dirtyPlans.length; ii++) { 
		    var statDoc = ActivityTree.xb.loadXML("<status/>");
		    var dp = dirtyPlans[ii];
			dp.planning.saveToDoc(statDoc);
			var res = ActivityTree.xb.loadURIPost(
				ActivityTree.serviceUri + "saveStatus?activity=" + dp.id, statDoc);
			if (!App.checkError(res)) {
				dp.planning.clearUpdates();
				Ephemeral.insideMessage(this.__uiParent, "Project Schedule Saved.");
			}	
		}
	}
	
};

ProjectGanttModel.prototype = new TreeGridModel();
ProjectGanttModel.prototype.constructor = ProjectTreeModel;

function ProjectGanttModel(project, showFull) {
	TreeGridModel.apply(this, []);
	this.nCols = 5;
	this.showFull = showFull;
	this.proj = project;
	this.itemIndex = new Object();
	this.reloadDoc = this.loadProject();
}

ProjectGanttModel.prototype.loadProject = ProjectTreeModel.prototype.loadProject;


ProjectGanttModel.prototype.onReady = function(callback, tree, itemParent) {
	if (itemParent === this && this.reloadDoc == null) this.reloadDoc = this.loadProject();
	if (itemParent.constructor === ProjectGanttItem) {
		itemParent.loadActStatus(callback, tree, itemParent);
	} else {
		this.digest(this.doc, callback, tree, itemParent);
	}
};

ProjectGanttModel.prototype.digest = function(doc, callback, tree, itemParent) {	
	var list = itemParent === this ? Xml.match(doc.documentElement, "activity") : [];
    var toggles = [];
    var actItm = null;
    var ii;        
    if (list.length > 0) {

        if (itemParent === this) {
        	
            // pass-1 index
            for (ii = 0; ii < list.length; ii++) {
                var nod = list[ii];
                var tItm = new ProjectGanttItem(this, nod, true, nod.getAttribute("icon") + "Ico");
                this.itemIndex[tItm.getId()] = tItm;
                list[ii] = tItm;
            }

            // pass-2 nest
            for (ii = 0; ii < list.length; ii++) {
                var tItm = list[ii];
                var pid = tItm.node.getAttribute("parent");
                if (pid) {
                    var par = this.itemIndex[pid];
                    if (par) {
                        par.add(tItm);
                        if (arrayFindStrict(toggles, par) < 0) toggles.push(par);
                        // TODO add more button...
                    } else {
                        // TODO else parent decoration
                        itemParent.add(tItm);
                    }
                } else {
                    itemParent.add(tItm);
                }
            }
        }
        
    } else if (itemParent === this) {
    	var blank = new ProjectGanttItem(this, null, false, "blankIco");
        blank.act = blank.optAct = null;
        blank.empty = true;
        itemParent.add(blank);
    }
    
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
    toggles.sort(ActivityTreeModel.depthSort);
    for (ii = 0; ii < toggles.length; ii++) {
        tree.toggle(toggles[ii]);
    }
};

/**
 * 
 */
ProjectGanttItem.prototype = new StatusTreeModel(); 
ProjectGanttItem.prototype.constructor = ProjectGanttItem;

function ProjectGanttItem(model, node, allowsKids, icon) {
	if (arguments.length == 0) return;
	if (model) TreeGridItem.apply(this, [model, null, allowsKids, icon]);
	this.rowCssClass = "norm";
    this.node = node;
    this._startTime = node.getAttribute("startDate") || "-";
    this._endTime = node.getAttribute("dueDate") || "-";
    
    this.cells.push(new ProjectDateCell(this, this.getStartDate()));
    this.cellStart = this.cells[0];
    this.cells.push(new ProjectDateCell(this, this.getEndDate()));
    this.cellEnd = this.cells[1];
    this.activityId = this.getId();
    this.cells.push(new GridCell(""));
}

objectExtend(ProjectGanttItem.prototype, TreeGridItem.prototype);
ProjectGanttItem.prototype.getCell = TreeGridItem.prototype.getCell;
ProjectGanttItem.prototype.editAct = null;


ProjectGanttItem.prototype.getId = function() {
    return this.node ? this.node.getAttribute("id") : "";
};

ProjectGanttItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
        if (this.node) {
		  	this.__labDomE = H.div({klass:"label " + this.rowCssClass},
                    H.div({klass:"name"}, this.node.getAttribute("name") + " "),
                    H.div({klass:"desc"}, this.node.getAttribute("description"))
                );
        }
    }
    hParent.appendChild(this.__labDomE);
};

ProjectGanttItem.prototype.loadActStatus = function(callback, tree, itemParent) {
	if (!this.flow) this.flow = ProjectGantt.getFlow(this.getId());
	if (!this.lpElem) {	
		var pdoc = ActivityTree.xb.loadURI(
			ActivityTree.serviceUri + "getLogsAndPlans?id=" + Uri.escape(this.getId()));
        if (App.checkError(pdoc, true)){
    		if (App.lastErrorType == "itensil.workflow.activities.ActivityInvalidException") {
    			ActivityTree.invalidActivity(this.getId());
    		} else {
    			App.showLastError();
    		}
        	return;
        }
        this.lpElem = pdoc.documentElement;
		this.teamRoster = ProjectGantt.teamRoster;
		
		this.plansElem = Xml.matchOne(this.lpElem, "plans");
		this.logsElem = Xml.matchOne(this.lpElem, "logs");
		this.planning = new ActivityPlanning();
	}
	this.onReady(callback, tree, itemParent);
};

ProjectGanttItem.prototype.getStartDate = StatusTreeItem.prototype.getStartDate;
ProjectGanttItem.prototype.setStartDate = StatusTreeItem.prototype.setStartDate;
ProjectGanttItem.prototype.getEndDate = StatusTreeItem.prototype.getEndDate;
ProjectGanttItem.prototype.setEndDate = StatusTreeItem.prototype.setEndDate;


/**
 * 
 */
ProjectDateCell.prototype = new GridCell();
ProjectDateCell.prototype.constructor = ProjectDateCell;
function ProjectDateCell(item, dt) {
	this.item = item;
	GridCell.apply(this, [dt ? DateUtil.toUTCShort(dt) : "", "due", true]);
}


ProjectDateCell.prototype.setValue = function(value, nosize) {
	var dt = DateUtil.parseUTCShort(value);
	GridCell.prototype.setValue.apply(this, [dt ? DateUtil.toUTCShort(dt) : ""]);
	if (!nosize) this.item.gtSizeBar();
};
