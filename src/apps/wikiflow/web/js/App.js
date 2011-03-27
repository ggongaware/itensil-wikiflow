var App = {

    disposelist : [],
    psMargins : [3,3,3,3],
    psPadding : [0,6,4,3],

    getPanelSet : function(leftWidth) {
    	App.init();
        var ps = new PanelSetVSplit(false, leftWidth);
        ps.margins = App.psMargins;
        ps.padding = App.psPadding;
        ps.header = document.getElementById("mast");
        ps.initHelp(App.chromeHelp);
        this.addDispose(ps);
        App.__panelSet = ps;
        return ps;
    },
    
    setShowMinor : function(show) {
    	if (App.__panelSet) {
    		if (!show && !App.__hidBefore) {
    			App.__hidBefore = true;
    			window.setTimeout(function() {
    				App.__panelSet.setMinorWidth(5);
    				}, 10);
    		} else if (show && App.__panelSet.__minorWidth < 10) {
    			window.setTimeout(function() {
    				App.__panelSet.setMinorWidth(370);
    				}, 10);
    		}
    	}
    },
    
    init : function() {
    	if (SH.is_gecko) {
    		// skype fix
    		window.setTimeout(function() {
	    		if (itslGetBounds !== getBounds) {
	    			getBounds = itslGetBounds;
	    		}}, 10);
    	}
    },

    addDispose : function(obj) {
        this.disposelist.push(obj);
    },
    
    disposableNode : function(node) {
    	var dn = new AppDisposableNode(node);
    	App.addDispose(dn);
    	return dn;
    },

    dispose : function () {
        for (var ii = 0; ii < this.disposelist.length; ii++) {
            this.disposelist[ii].dispose();
        }
        this.activeActivityNode = null;
        this.activeStateNode = null; 
    },

    setActivityMenu : function(tree) {
       	var subProject = new Menu(new TNavPrjtMenuModel(
			{label:"New Project", icon:"mb_newIco", act:App.tnavNewPrjt}),
			ActivityTree.addToProjectMnOpt);
			
		var subSendFlow = new Menu(new TNavProcMenuModel("modifiedProcesses", ActivityTree.sendToNewMnOpt,
			[{label:"New Process", icon:"mb_newIco", act:ActivityTree.sendToNewMnOpt}]),
			ActivityTree.sendToFlowMnOpt);
		
		var mopts = [
                 {label : "Workzone", icon : "mb_workIco", act :ActivityTree.workzone},
                 {label : "Properties", icon : "mb_optIco", act : ActivityTree.properties},
                 {label : "Refresh", icon : "mb_refIco", act : ActivityTree.refresh},
                 {isSep : true }];
                 
       	if (!App.meet) {
      		arrayAddAll(mopts,
      			[{label : "Add to project", icon : "mb_copyIco", sub : subProject},
                 {isSep : true },
                 {label : "Swap with process", icon : "mb_movIco", sub : subSendFlow},
                 {isSep : true }]);
       	}

		mopts.push({label : "Remove", icon : "mb_remIco", act : ActivityTree.remove});

        if (Modes.mode == "todo") { 
        	var advMen = new Menu(new MenuModel(
             	[
                 {label : "Rollback Activity", icon : "mb_histIco", act : ActivityTree.rollBack}
                ]));
        	arrayInsert(mopts, mopts.length - 1, {label : "Advanced Options", sub : advMen});
        	arrayInsert(mopts, mopts.length - 1, {isSep : true });
        }     
        tree.menu = new Menu(new MenuModel(mopts));
  	},
    
    setProjectMenu : function(tree) {
    	var subSendFlow = new Menu(new TNavProcMenuModel("modifiedProcesses", ActivityTree.sendToNewMnOpt,
			[{label:"New Process", icon:"mb_newIco", act:ActivityTree.sendToNewMnOpt}]),
			ActivityTree.sendToFlowMnOpt);
        tree.menu = new Menu(new MenuModel(
             [
                 {label : "Workzone", icon : "mb_workIco", act : ActivityTree.workzone},
                 {label : "Properties", icon : "mb_optIco", act : ActivityTree.properties},
                 {isSep : true },
                 {label : "Remove from project", icon : "mb_outIco", act : ActivityTree.removeFromProjectMnOpt},
                 {isSep : true },
                 {label : "Swap with process", icon : "mb_movIco", sub : subSendFlow},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : ActivityTree.remove}
             ]));
             
        
    },

    setFileMenu : function(tree) {
        tree.menu = new Menu(new MenuModel(
         	[
                 {label : "Properties", icon : "mb_optIco", act : FileTree.properties},
                 {label : "Refresh", icon : "mb_refIco", act : FileTree.refresh},
                 {isSep : true },
                 {label : "New Folder", icon : "mb_foldIco", act : FileTree.newFolder},
                 {label : "Add Files", icon : "mb_addIco", act : FileTree.addFiles},
                 {label : "Permissions", icon : "mb_permIco", act : FileTree.permissions},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : FileTree.remove}
         	]));
        tree.menu.model.onSetContext = FileTree.onMenuContext;
        tree.menu2 = new Menu(new MenuModel(
         	[
                 {label : "View", icon : "mb_viewIco", act : FileTree.view},
                 {label : "Edit", icon : "mb_ediIco", act : FileTree.edit},
                 {isSep : true },
                 {label : "Properties", icon : "mb_optIco", act : FileTree.properties},
                 {label : "History",icon : "mb_histIco", act : FileTree.history},
                 {label : "Permissions", icon : "mb_permIco", act : FileTree.permissions},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : FileTree.remove}
         	]));
      	tree.menu2.model.onSetContext = FileTree.onMenuContext2;
    },

    activeActivityNode : null,
    activeStepId : null,
    activeFlow : null,
    activeStateNode : null,

    // listener is a function(node, stepId)
    // if stepId == null, there are 0 or 2+ steps
    activityListeners : [],
    activeStateListeners : [],

    setActiveActivity : function(node, stepId) {
        this.activeActivityNode = node;
        var ii;
        if (!stepId && node != null) {
        	var states = Xml.match(node, "state");
        	for (ii = 0; ii < states.length; ii++) {
        		var sub = states[ii].getAttribute("subState");
        		if (sub.indexOf("ENTER") == 0) {
        			stepId = states[ii].getAttribute("stepId");
        			if (sub == "ENTER_STEP") break;
        		}
        	}
        }  
        this.activeActivityId = node != null ? node.getAttribute("id") : "";
        this.setActiveStep(stepId);
        for (ii = 0; ii < this.activityListeners.length; ii++) {
            this.activityListeners[ii](node, stepId);
        }
    },
    
    setActiveStep : function(stepId) {
    	var activeState = null;
    	if (!stepId) stepId = null;
        else if (this.activeActivityNode != null) {
        	activeState = Xml.matchOne(this.activeActivityNode, "state", "stepId", stepId);
        }
        this.activeStateNode = activeState;
        this.activeStepId = stepId;
        for (var ii = 0; ii < this.activeStateListeners.length; ii++) {
            this.activeStateListeners[ii](activeState, stepId);
        }
    },

    // listener is a function()
    //   return true if unload should abort
    unloadListeners : [],

    unloadCatch : function(evt) {
        for (var ii = 0; ii < this.unloadListeners.length; ii++) {
            if (this.unloadListeners[ii]()) {
                evt.returnValue = "There are unsaved changes.";
                return "There are unsaved changes.";
            }
        }
    },

    lastError : null, lastErrorType : null,

    checkError : function (doc, hideErr) {
        if (doc) {
            if (doc.documentElement == null) {
                this.lastError = "Sorry, unable to connect: the network or server are temporarily unavailable. Try again in a few moments.";
                if (!hideErr) alert(this.lastError);
                return true;
            }
            if (Xml.getLocalName(doc.documentElement) == "error") {
            	var errType = doc.documentElement.getAttribute("exception");
				App.lastErrorType = errType;
                switch (errType) {
                case "itensil.repository.NotFoundException":
                case "itensil.repository.LockException":
                case "itensil.repository.DuplicateException":
                case "itensil.workflow.RunException":
                case "itensil.workflow.rules.EvalException":
                case "itensil.workflow.state.StateException":
                case "itensil.security.UserSpaceException":
                case "itensil.security.DuplicatePrincipalException":
                    this.lastError = "Sorry, " + Xml.stringForNode(Xml.matchOne(doc.documentElement, "message"));
                    break;
                case "itensil.repository.AccessDeniedException":
                    this.lastError = "Sorry, your account does not have permissions to do this.";
                    break;
                case "itensil.workflow.RunException":
                    this.lastError = "Sorry, your account does not have permissions to do this.";
                    break;
               	case "itensil.workflow.activities.ActivityInvalidException":
               		this.lastError = "Sorry, this activity is missing its content folder";
               		break;
               	case "itensil.web.ErrorMessage":
               		this.lastError = Xml.stringForNode(Xml.matchOne(doc.documentElement, "message"));
               		break;
                default:
                    this.lastError = "Sorry, a system error occured, please try again later.";
                }
                if (!hideErr) App.showLastError();
                return true;
            }
        }
        return false;
    },
    
    showLastError : function() {
    	alert(App.lastError);	
    },
    
    tnavInit : function() {
    	
		
		if (!App.edu) {
			var desMn = new Menu(new TNavProcMenuModel("modifiedProcesses", App.tnavActProcDesign,
				[{label:"New Process", icon:"mb_newIco", act:App.tnavNewProc},
				{isSep:true},
				{label:"Library", icon:"mb_foldIco", act:App.tnavProcLib}
				]),
				App.tnavActProcDesign);
			var lanMn = new Menu(new TNavProcMenuModel("activeProcesses", App.tnavActProcLaunch,
				[{label:"Library", icon:"mb_foldIco", act:App.tnavProcLib}]),
				App.tnavActProcLaunch);
			App.tnavMenuProc = new Menu(new MenuModel([
				{label:"Design", icon:"mb_desIco", sub:desMn},
				{label:"Launch", icon:"mb_launIco", sub:lanMn}
				]));
			App.snavMenuProcDes = desMn;
			App.snavMenuProcLan = lanMn;
			
			App.addDispose(App.tnavMenuProc);
			App.tnavMenuMeet = new Menu(new TNavMeetMenuModel(
				{label:"New Knowledge App", icon:"mb_newIco", act:App.tnavNewMeet}),
				App.tnavMeetPage);
				
			if (!App.meet) {
				App.tnavMenuPrjt = new Menu(new TNavPrjtMenuModel(
					{label:"New Project", icon:"mb_newIco", act:App.tnavNewPrjt}),
					App.tnavPrjtPage);
					
				App.tnavMenuEntity = new Menu(new TNavEntMenuModel(
					{label:"New Entity", icon:"mb_newIco", act:App.tnavNewEnt}),
					App.tnavEntPage);
			}
		}
		var desCMn = new Menu(new TNavProcMenuModel("modifiedCourses", App.tnavActProcDesign,
			[{label:"New Course", icon:"mb_newIco", act:App.tnavNewCour},
			{isSep:true},
			{label:"Library", icon:"mb_foldIco", act:App.tnavProcLib}
			]),
			App.tnavActProcDesign);
		var lanCMn = new Menu(new TNavProcMenuModel("modifiedCourses", App.tnavActProcLaunch,
			[{label:"Library", icon:"mb_foldIco", act:App.tnavProcLib}]),
			App.tnavActProcLaunch);
		App.tnavMenuCourse = new Menu(new MenuModel([
			{label:"Design", icon:"mb_desIco", sub:desCMn},
			{label:"Enroll", icon:"mb_launIco", sub:lanCMn}
			]));
		App.snavMenuCourDes = desCMn;
		App.snavMenuCourLan = lanCMn;
		
		App.addDispose(App.tnavMenuCourse);
    },
    
    tnavShowMenu : function(link, menu) {
		var rect = getBounds(link);
		link.blur();
		menu.show(rect.x, rect.y + rect.h);
		exAddClass(link, "onTnav");
		menu.onHide = function() {
			if (link) exAddClass(link, "onTnav", true);
			link = null;
		}
		return false;
	},
	
	tnavNewProc : function() {
		FileTree.doCopy("/system/sysproc/Blank", "/home/process/New Process", "chart.flow");
	},
	
	tnavNewCour : function() {
		FileTree.doCopy("/system/sysproc/Blank", "/home/course/New Course", "chart.flow");
	},
	
	tnavProcLib : function() {
		var diag = Wiki.dialog("../fil/system/Help/Public.kb", "Library");
		diag.show(150, 100);
	},
	
	tnavActProcDesign : function() {
		ActivityTree.flowDesign(this.uri);
	},
	
	tnavActProcLaunch : function() {
		ActivityTree.flowLaunch(this.uri);
	},
	
	tnavTeamPage : function() {
		try {
			location.href = "../home/?page=" + Uri.escape(this.label);
		} catch (e) {}
	},
	
	tnavNewPrjt : function() {
		var diag = xfDialog(
			"New Project", true, document.body, 
			"../view-proj/project.xfrm", ActivityTree.xb,
			null, null, null, false, App.chromeHelp);
		App.addDispose(diag);
		diag.show(100,100);
	},
	
	tnavPrjtPage : function() {
		try {
			location.href = "../proj/page?proj=" + Uri.escape(this.name);
		} catch (e) {}
	},
	
	tnavEntPage : function() {
		try {
			location.href = "../entity/page?entity=" + Uri.escape(this.name);
		} catch (e) {}
	},
	
	tnavNewEnt : function() {
		FileTree.doCopy("/system/sysproc/Entity", "/home/entity/New Entity", "model.entity");
	},
	
	tnavNewMeet : function() {
		ActivityTree.activityLaunch("", "New Knowledge App", "/system/sysproc/Blank", false, null, true);
	},
	
	tnavMeetPage : function() {
		location.href = "../act/meetStat?meet=" + Uri.escape("/home/meeting/" + this.name);
	},
	
	tnavAllProc : function(evt, item) {
		App._tnavAllProcDiag = xfDialog(
			this.procAct === App.tnavActProcLaunch ? 
				"Launch Process List" : "Design Process List", 
			true, document.body, 
			"../view-wf/list.xfrm", ActivityTree.xb, {act:this.procAct, item:item}, null, null, true, App.chromeHelp);
		App._tnavAllProcDiag.show(80,80);
	},
	
	tnavAllCours : function(evt, item) {
		App._tnavAllProcDiag = xfDialog(
			this.procAct === App.tnavActProcLaunch ? 
				"Enroll Course List" : "Design Course List", 
			true, document.body, 
			"../view-wf/list.xfrm", ActivityTree.xb, {act:this.procAct, item:item}, 
			"../act/processList?uri=/home/course", null, true, App.chromeHelp);
		App._tnavAllProcDiag.show(80,80);
	},

	helpBase : '../fil/system/Help/Help.kb',

	chromeHelp : function(label, type) {
		var topic = label.split(':')[0] + " " + type;
		Wiki.popup(App.helpBase,topic,890,500, {showIndex:1});		
	},
	
	resolver : new AppUriResolver(".")
	
};

function AppDisposableNode(node) {
	this.node = node;
}

AppDisposableNode.prototype.dispose = function() { this.node = null; };


AppUriResolver.__resUriRx = new RegExp("\\{([^\\}]+)\\}");
function AppUriResolver(modelUri) {
	this.modelUri = modelUri;
}

AppUriResolver.prototype.resolveUri = function(uri) {
	if (!uri) return "";
	var mm = AppUriResolver.__resUriRx.exec(uri);
	this.isTemplate = false;
	if (mm) {
		var preUri = mm.index > 0 ? uri.substring(0, mm.index) : "";
		var endUri = uri.substring(mm.index + mm[0].length);
		var isParent = false;
		switch (mm[1]) {
			
			case "activity-parent":
				isParent = true;
			case "activity":
				if (App.activeActivityNode == null) {
					if (Modes.mode == "edit") {
						this.isTemplate = true;
						return preUri + "template" + endUri;
					}
				} else {
					var id = App.activeActivityId;
					var pid = App.activeActivityNode.getAttribute("parent");
	             	if (pid) id = pid;
					var info = ActivityTree.activityInfo(id);
					var actPath = info.getAttribute("uri");
					return preUri + actPath + endUri;
				}
				return uri;
				
			case "model":
				return preUri + this.modelUri + endUri;
			
			case "record":
				return this.recUri ? preUri + this.recUri + endUri : preUri + "template" + endUri;
				
			default:
				return uri;
		}
	}
	return uri;
};

// Process Menu
TNavProcMenuModel.prototype = new MenuModel();
TNavProcMenuModel.prototype.constructor = TNavProcMenuModel;

function TNavProcMenuModel(uriPart, procAct, topItems) {
	this.uriPart = uriPart;
	this.procAct = procAct;
	this.topItems = topItems;
	this.isCourse = uriPart.indexOf("Courses") > 0;
}

TNavProcMenuModel.prototype.onReady = function(callback, menu) {
	var holdThis = this;
	ActivityTree.xb.loadURIAsync(ActivityTree.serviceUri + this.uriPart,
        function(doc, arg, xmlHttp) {
            if (doc != null && !App.checkError(doc)) {
            	holdThis.digest(doc, callback, menu);
            }
        });
};

TNavProcMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var parUri = doc.documentElement.getAttribute("uri");
	var list = Xml.match(doc.documentElement, "node");
	var ii;
	if (this.topItems) {
		for (ii = 0; ii < this.topItems.length; ii++) {
			items.push(this.topItems[ii]);
		}
		items.push({isSep:true});
	}
	items.push({label:"All " + (this.isCourse ? "Courses" : "Processes") + "...", 
		icon:"mb_viewIco", act:(this.isCourse ? App.tnavAllCours : App.tnavAllProc), procAct:this.procAct});
	if (list.length > 0 ) items.push({isSep:true, 
		labelElement : makeElement(null, "div", "mnHead", 
			(this.uriPart == "activeProcesses" ? "Recently Used:" : "Recently Edited:"))});
    for (ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var name = nod.getAttribute("uri");
        // unix hide
        if (name.substring(0,1) == ".") continue;
        
        var tUri = Uri.absolute(parUri, name);
        var style = nod.getAttribute("style");
        var icon = style ? style.substring(5) : "def";
        var mItm = {label:null, uri:tUri};
        mItm.labelElement = makeElement(null, "div", "mnTNavLab");
        makeElement(mItm.labelElement, "div", "labTxt", name);
        makeElementNbSpd(mItm.labelElement, "div", "icon " + icon + "Ico");
        items.push(mItm);
    }
    callback.apply(menu, [items]);
};



// Project Menu
TNavPrjtMenuModel.prototype = new MenuModel();
TNavPrjtMenuModel.prototype.constructor = TNavPrjtMenuModel;
TNavPrjtMenuModel.__doc = null;

function TNavPrjtMenuModel(lastItem) {
	this.lastItem = lastItem;
}

TNavPrjtMenuModel.prototype.onReady = function(callback, menu) {
	if (TNavPrjtMenuModel.__doc == null) {
		var holdThis = this;
		ActivityTree.xb.loadURIAsync(ActivityTree.projectSvcUri + "listProjects?uri=/home/project",
            function(doc, arg, xmlHttp) { 
                if (doc != null && !App.checkError(doc)) {
                	TNavPrjtMenuModel.__doc = doc;
                	holdThis.digest(TNavPrjtMenuModel.__doc, callback, menu);
                }
            });
	} else {
		this.digest(TNavPrjtMenuModel.__doc, callback, menu);
	}
};

TNavPrjtMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var parUri = doc.documentElement.getAttribute("uri");
	var list = Xml.match(doc.documentElement, "node");
    for (var ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var name = nod.getAttribute("uri");
        // unix hide
        if (name.substring(0,1) == ".") continue;
        
        var tUri = Uri.absolute(parUri, name);
        var style = nod.getAttribute("style");
        var icon = style ? style.substring(5) : "def";
        var mItm = {label:name, uri:tUri, name:name, id:nod.getAttribute("id")};
        items.push(mItm);
    }
	items.push({isSep:true});
	items.push(this.lastItem);
    callback.apply(menu, [items]);
};


// Entity Menu
TNavEntMenuModel.prototype = new MenuModel();
TNavEntMenuModel.prototype.constructor = TNavEntMenuModel;
TNavEntMenuModel.__doc = null;

function TNavEntMenuModel(lastItem) {
	this.lastItem = lastItem;
}

TNavEntMenuModel.prototype.onReady = function(callback, menu) {
	if (TNavEntMenuModel.__doc == null) {
		var holdThis = this;
		ActivityTree.xb.loadURIAsync("../entity/listEntities?uri=/home/entity",
            function(doc, arg, xmlHttp) { 
                if (doc != null && !App.checkError(doc)) {
                	TNavEntMenuModel.__doc = doc;
                	holdThis.digest(TNavEntMenuModel.__doc, callback, menu);
                }
            });
	} else {
		this.digest(TNavEntMenuModel.__doc, callback, menu);
	}
};

TNavEntMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var parUri = doc.documentElement.getAttribute("uri");
	var list = Xml.match(doc.documentElement, "node");
    for (var ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var name = nod.getAttribute("uri");
        // unix hide
        if (name.substring(0,1) == ".") continue;
        
        var tUri = Uri.absolute(parUri, name);
        var style = nod.getAttribute("style");
        var icon = style ? style.substring(5) : "def";
        var mItm = {label:name, uri:tUri, name:name, id:nod.getAttribute("id")};
        items.push(mItm);
    }
	items.push({isSep:true});
	items.push(this.lastItem);
    callback.apply(menu, [items]);
};




// Meeting Menu
TNavMeetMenuModel.prototype = new MenuModel();
TNavMeetMenuModel.prototype.constructor = TNavMeetMenuModel;
TNavMeetMenuModel.__doc = null;

function TNavMeetMenuModel(lastItem) {
	this.lastItem = lastItem;
}

TNavMeetMenuModel.prototype.onReady = function(callback, menu) {
	if (TNavMeetMenuModel.__doc == null) {
		var holdThis = this;
		ActivityTree.xb.loadURIAsync(ActivityTree.projectSvcUri + "listProjects?uri=/home/meeting",
            function(doc, arg, xmlHttp) { 
                if (doc != null && !App.checkError(doc)) {
                	TNavMeetMenuModel.__doc = doc;
                	holdThis.digest(TNavMeetMenuModel.__doc, callback, menu);
                }
            });
	} else {
		this.digest(TNavMeetMenuModel.__doc, callback, menu);
	}
};

TNavMeetMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var parUri = doc.documentElement.getAttribute("uri");
	var list = Xml.match(doc.documentElement, "node");
    for (var ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var name = nod.getAttribute("uri");
        // unix hide
        if (name.substring(0,1) == ".") continue;
        
        var tUri = Uri.absolute(parUri, name);
        var style = nod.getAttribute("style");
        var icon = style ? style.substring(5) : "def";
        var mItm = {label:name, uri:tUri, name:name, id:nod.getAttribute("id")};
        items.push(mItm);
    }
	items.push({isSep:true});
	items.push(this.lastItem);
    callback.apply(menu, [items]);
};




var Modes = {

    actEl : null,
    mode : "",

    reAct : function() {
       if (this.actEl != null) this.actEl.onclick();
    },

    drawModes : function (parElem, act) {
        var mds = App.guest ? {} : {
                todo : H.div({klass:"md todo", onclick:"Modes.todo(this)"}, (App.ahm ? "Manage" : "Run")),
                stat : H.div({klass:"md stat", onclick:"Modes.stat(this)"}, "Status"),
                edit : H.div({klass:"md edit", onclick:"Modes.edit(this)"}, "Design")
            };
        var mElem = H.div({klass:"modes"}, mds.todo, mds.stat, mds.edit);
        if (App.activeFlow) {
        	mElem.appendChild(H.div({klass:"crumb"}, (ActivityTree.isMeet ? "Meeting: " : (App.ahm ? "Template: " : (ActivityTree.isCourse ? "Course: " : "Process: "))) 
        		+ Uri.name(Uri.parent(App.activeFlow))));
        	if (App.activeActivityId) {
        		var node = ActivityTree.activityInfo(App.activeActivityId, true);
        		App.activeActivityNode = node;
        		if (node && (node.getAttribute("variationId") || act == "edit")) {
        			var vid = node.getAttribute("variationId");
        			mElem.appendChild(H.div({klass:"crumbAct" + (vid ? " actVar" : "")}, 
        				(vid ? "Variation for activity: " : (App.ahm ? "Incident: " : "Activity: ")) + node.getAttribute("name")));
        		}
        	}
        }
        parElem.appendChild(mElem);

        this.mode = act;
        this._setAct(mds[act]);
    },
    
   	drawProjModes : function (parElem, act) {
        var mds = App.guest ? {} : {
                proj : H.div({klass:"md todo", onclick:"Modes.proj(this)"}, "Project"),
                projGantt : H.div({klass:"md stat", onclick:"Modes.projGantt(this)"}, "Timeline")
            };
        var mElem = H.div({klass:"modes"}, mds.proj /*, mds.projGantt */);
        
        parElem.appendChild(mElem);

        this.mode = act;
        this._setAct(mds[act]);
   	},
   	
    _setAct : function (el) {
        if (this.actEl != null)
            exAddClass(this.actEl, "act", true);
        this.actEl = el;
        if (el != null) exAddClass(el, "act");
    },

    todo : function(el, force) {
        //this._setAct(el);
        if (this.mode != "todo" || force) {
            try {
                if (App.activeActivityNode != null || App.activeActivityId) {
                	var aid = App.activeActivityNode ? 
                		App.activeActivityNode.getAttribute("id") : App.activeActivityId;
                    location.href = "../act/page?activity=" + aid
                    	+ (App.activeStepId != null ? ("&step=" + Uri.escape(App.activeStepId)) : "") 
                    	+ (App.activeFlow != null ? ("&flowUri=" + Uri.escape(App.activeFlow)) : "")
                    	+ (App.kiosk ? "&kiosk=1" : "");
                } else if (App.activeFlow != null) {
                    location.href = "../act/page?flowUri=" + Uri.escape(App.activeFlow);
                }
            } catch(e) {}
        }
        
    },

    stat : function(el) {
        //this._setAct(el);
        this.mode = "stat";
        try {
            if (App.activeActivityNode != null || App.activeActivityId) {
            	var aid = App.activeActivityNode ? 
            		App.activeActivityNode.getAttribute("id") : App.activeActivityId;
                location.href = "../act/stat?activity=" + aid;
            } else if (App.activeFlow != null) {
                location.href = "../act/stat?flowUri=" + Uri.escape(App.activeFlow);
            }
       	} catch(e) {}
    },

    edit : function(el) {
        //this._setAct(el);
        if (this.mode != "edit") {
            try {
            	var aid = App.activeActivityNode ? 
            		App.activeActivityNode.getAttribute("id") : App.activeActivityId;
                if (App.activeFlow != null) {
                    location.href = "../mod/page?uri=" + Uri.escape(App.activeFlow)
                           + (aid != null ? ("&activity=" + aid) : "")
                           + (App.activeStepId != null ? ("&step=" + Uri.escape(App.activeStepId)) : "");
                } else if (App.activeActivityNode != null) {
                    location.href = "../mod/page?flow=" + App.activeActivityNode.getAttibute("flow")
                            + "&activity=" + App.activeActivityNode.getAttribute("id")
                            + (App.activeStepId != null ? ("&step=" + Uri.escape(App.activeStepId)) : "");
                }
            } catch(e) {}
        }
    },
    
    proj : function(el, force) {
    	if (this.mode != "proj") {
	    	try {
				location.href = "../proj/page?proj=" + Uri.escape(project);
			} catch (e) {}
    	}
    },
    
    projGantt : function(el, force) {
    	if (this.mode != "projGantt") {
	    	try {
				location.href = "../proj/gantt?proj=" + Uri.escape(project);
			} catch (e) {}
    	}
    }
    
};

FileTree.viewHandlers["flow"] = function(evt, item) {
	try {
   		location.href = "../mod/page?uri=" + Uri.escape(item.uri);
   	} catch(e) {}
};

FileTree.viewHandlers["meet"] = function(evt, item) {
	try {
   		location.href = "../mod/meet?uri=" + Uri.escape(Uri.parent(item.uri) + "/chart.flow");
   	} catch(e) {}
};

FileTree.viewHandlers["kb"] = function(evt, item) {
    Wiki.popup("../fil" + item.uri, "", 620, 470);
};

FileTree.viewHandlers["art"] = function(evt, item) {
	//TODO - elj - change article name hard coded in call - elj
    Wiki.popupArt("../fil" + item.uri, "article", 800, 800);
};

FileTree.viewHandlers["xfrm"] = function(evt, item) {
    window.open("../view-xfrm/?uri=" + Uri.escape(item.uri), "_blank");
};

FileTree.viewHandlers["rule"] = function(evt, item) {
    window.open("../view-rule/?uri=" + Uri.escape(item.uri), "_blank");
};

FileTree.viewHandlers["entity"] = function(evt, item) {
    window.open("../entity/edit?entity=" + Uri.escape(Uri.name(Uri.parent(item.uri))), "_blank");
};

if (SH.is_ie) {

    FileTree.editHandlers["vsd"] =
    FileTree.editHandlers["xls"] =
    FileTree.editHandlers["ppt"] =
    FileTree.editHandlers["doc"] = function(evt, item) {
        try {
        	var uri = item.uri;
        	var isXml = uri.substring(uri.length - 4)  == ".xml";
        	if (isXml) uri = uri.substring(0, uri.length - 4);
            var sharp = new ActiveXObject("SharePoint.OpenDocuments.2");
            var href = Uri.reduce(getHttpPath() + (isXml ? "../filx" : "../fil") + uri);
            sharp.EditDocument2(window, href);
        } catch (e) {
        	var diag = xfDialog("Editing Office Documents", true, document.body, "../view-repo/mssharp.xfrm", FileTree.xb);
        	diag.show(200,200);
        	App.addDispose(diag);
       }
    };

}
