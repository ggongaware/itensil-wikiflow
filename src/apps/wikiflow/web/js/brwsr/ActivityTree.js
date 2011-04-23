/**
 * (c) 2006 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 *  Activities
 *
 */

var ActivityTree = {

    serviceUri : "../act/",
    projectSvcUri : "../proj/",
    
    __activityInfo : new Object(),
    __tree : null,
    xb : new XMLBuilder(),
    
    planning : new ActivityPlanning(),

    workzone : function(evt, item) {
        var lastAct = App.activeActivityNode;
        if (Modes.mode != "todo") {
            App.setActiveActivity(item.node);
            Modes.todo();
        } else if (lastAct != null && (lastAct.getAttribute("flow") != item.node.getAttribute("flow"))) {
            try {
                location.href = "../act/page?activity=" + item.node.getAttribute("id");
            } catch(e) {}
        } else {
            App.setActiveActivity(item.node);
        }
    },

    refresh : function(evt, item) {
    	ActivityTree.__activityInfo = new Object();
        if (item == null) {
        	if (ActivityTree.__tree) ActivityTree.__tree.redrawAll();
        }  else if (item === item.model) item._tree.redrawAll();
        else item._tree.redraw(item, Tree.HINT_INSERT);
    },

    remove : function(evt, item) {
    	Tree.selectItem(item);
        if (parseInt(item.node.getAttribute("activeKids")) > 0) {
            alert("Please move or remove all sub-activities first.");
            return;
        }
        if (confirm("Are you sure?")) {
            if (!App.checkError(
                ActivityTree.xb.loadURI(ActivityTree.serviceUri + "delete?id=" + Uri.escape(item.node.getAttribute("id"))))) {
            	var parId = item.node.getAttribute("parent");
            	item.removeItem();
     			if (parId) {
     				var infNode = ActivityTree.activityInfo(parId, true, true);
            		ActivityTree.updateActivity(infNode);
            		if (parId == App.activeActivityId) {
            			App.setActiveActivity(infNode, App.activeStepId);
            		}
     			}
            }
        }
    },
    
    removeById : function(id) {
    	if (confirm("Are you sure?")) {
           	if (!App.checkError(
                	ActivityTree.xb.loadURI(ActivityTree.serviceUri + "delete?id=" + Uri.escape(id)))) {
      			location.href = "../home/";     		
           	}
    	}
    },

    activityInfo : function(id, hideErr, forceReload) {
    	var info = forceReload ? null : this.__activityInfo[id];
        if (info) return info;
        var doc = this.xb.loadURI(this.serviceUri + "activityInfo?id=" + Uri.escape(id));
        if (App.checkError(doc, true)){
        	if (!hideErr) {
        		if (App.lastErrorType == "itensil.workflow.activities.ActivityInvalidException") {
        			ActivityTree.invalidActivity(id);
        		} else {
        			App.showLastError();
        		}
        	}
        	return null;
        }
        info = doc.documentElement;
        this.__activityInfo[id] = info;
        return info;
    },

    itemAct : function(evt, stepId) {
        ActivityTree.setActivityItem(this, stepId);
    },
    
    setActivityItem : function(item, stepId) {
    	
    	// is this a step tied sub-process?
    	if (!stepId && item.pStateNode) {
    		stepId = item.pStateNode.getAttribute("stepId");
    		item = item.itemParent;
    	}
        var lastAct = App.activeActivityNode;
        if (Modes.mode == "" || Modes.mode == "proj") {
            App.setActiveActivity(item.node, stepId);
            Modes.todo();
        } else if (lastAct != null && (lastAct.getAttribute("flow") != item.node.getAttribute("flow"))) {
            try{
                location.href = "../act/page?activity=" + item.node.getAttribute("id")
                	+ (stepId != null ? ("&step=" + Uri.escape(stepId)) : "");
            } catch(e){}
        } else {
            App.setActiveActivity(item.node, stepId);
        }
    },
    
    getActiveActivityItem : function() {
    	if (App.activeActivityNode) {
    		return ActivityTree.findItem(App.activeActivityNode.getAttribute("id"));
    	}
    	return null;
    },

	renderActivityTreeMod : function(modl, uiParent, config) {
		var conf = config || {};
		var hElem = makeElement(uiParent, "div", "minorTreeBox");

        var grid = new Grid(modl);
        grid.addHeader((App.edu || ActivityTree.isCourse) ? "Course/Step" : "Activity/Task");
        grid.addHeader("Due", "due");
        if (!(App.edu || ActivityTree.isCourse))  grid.addHeader(modl.mode == "assigned" ? "From" : "To");
        var tree = new Tree(modl);
        App.setActivityMenu(tree);
        if (!App.guest && !conf.nodrag) tree.makeDragCanvas(uiParent, new ActivityTreeDNDType("fileTree activityTree"), true, false, true);
        grid.render(hElem, "activityGrid todo");
        if (uiParent._panel) uiParent._panel.linkResize(grid);
        tree.render(hElem, grid.getTreeStyle(), "fileTree activityTree");
        if (!App.guest && !ActivityTree.isMeet && modl.flowUri) {
    		var flowPath = Uri.parent(modl.flowUri);
    		var launLink = makeElement(hElem, "div", "minorBtn", 
    			ActivityTree.isCourse ? "Enroll Course..." : "Launch Activity...")
    		makeElementNbSpd(launLink, "div", "mbIco mb_launIco");
    		setEventHandler(launLink, "onclick", 
    				function() {
						ActivityTree.flowLaunch(flowPath);
					});
      	}
        App.addDispose(grid);
        App.addDispose(tree);
        
        hElem = null;  uiParent = null; // IE enclosure clean-up
        
        return tree;
	},
	
    renderActivities : function(mode, flowUri, uiParent) {

        var modl = new ActivityTreeModel(mode, flowUri);
        if (!App.guest) {
	        var opLink = makeElement(uiParent, "div", "minorBtn", (mode == "assigned") ? "Requested Tasks" : "Assigned Tasks");
	        opLink._treeMod = modl;
	        opLink._mode = mode;
	        modl.opLink = opLink;
	        setEventHandler(opLink, "onclick", ActivityTreeModel.opClick);
        }
        
        makeElementNbSpd(opLink, "div", "mbIco mb_bakIco");
        modl.title = makeElement(uiParent, "div", "minorHead", 
        	flowUri ?  Uri.name(Uri.parent(flowUri)) : ((App.edu || ActivityTree.isCourse)  ? "Enrolled Courses" : "Assigned Tasks"));

        ActivityTree.__tree = ActivityTree.renderActivityTreeMod(modl, uiParent);
    },

    // Launch
    activityLaunch : function(flow, name, masterFlow, isSub, roles, isMeet) {
    	if (ActivityTree.__preLanDiag) {
    		ActivityTree.__preLanDiag.destroy();
    		ActivityTree.__preLanDiag = null;
    	}
    	var isCourse = flow.indexOf('/course/') > 0; 
        var diag = xfDialog(
        	isMeet ? "New Knowledge App" : ("New " + (isCourse ? "Enrollment" : "Activity") + ": " + name), 
        	true, document.body, "../view-wf/launch.xfrm.jsp", ActivityTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.setValue("flow", flow);
        xfMod.setValue("name", name);
        xfMod.setValue("master-flow", masterFlow);
        xfMod.setValue("meet", isMeet ? "1" : "0");
        if (isSub) {
            if (App.activeActivityNode) {
                xfMod.setValue("parent", App.activeActivityNode.getAttribute("id"));
            }
        }
        if (ActivityTree.projectId) {
        	 xfMod.setValue("project", ActivityTree.projectId);
        	 xfMod.setValue("proj-lock", "1");
        }
        xfMod.revalidate();
        xfMod.addEventListener("xforms-submit-done", { handleEvent : function() {
        		var resDoc = xfMod.getSubmitResponse();
        	   	if (isMeet) {
        			var fname = resDoc.documentElement.getAttribute("flowName");
        			try {
				   		location.href = "../mod/meet?uri=" + Uri.escape("/home/meeting/" + fname + "/chart.flow");
				   	} catch(e) {}
        			return;
        		} else {
        			try {
				   		location.href = "../act/page?activity=" + resDoc.documentElement.getAttribute("id");
				   	} catch(e) {}
        			return;
        		}
            }});

        if (roles) {
        	
        	var roleElem = diag.xform.getElementById("roles");
        	
        	if (roleElem) {
	        	var tmr = new TeamRoster();
	        	
	            // add role elements to the xfMod
	            var context = {roleRoot:xfMod.getInstanceDocument("ins").documentElement};
	            for (var rol in roles) {
	                var rolEl = Xml.element(context.roleRoot, "role");
	                rolEl.setAttribute("role", rol);
	                rolEl.setAttribute("assignId", roles[rol]);
	            }
	            // render roles on dialog
	            tmr.renderRoles(roleElem, roles, roles, function(evt) {
	                    context.hElem = this;
	                    tmr.getTeamMenu(TeamRoster.setLaunchRole, TeamRoster.clearLaunchRole).popUp(evt, context);
	                }, true);
	                
	           	roleElem = null; // IE enclosure clean-up
        	}
        }
        diag.show(100, 100);
    },
    
    // Launch on student for EDU
    addCourse : function(evt, item) {
    	Tree.selectItem(item);
    	var diag = xfDialog("Add Course", true, document.body, "../view-wf/course.xfrm", ActivityTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.setValue("student", item.label);
        xfMod.setValue("submitId", item.node.getAttribute("id"));
        xfMod.revalidate();
        xfMod.addEventListener("xforms-submit-done", { handleEvent : function() {
        			if (ActivityTree.__tree) {
        				if (item) ActivityTree.refresh(null, item);
        				else ActivityTree.__tree.redrawAll();
        			}
        			diag.destroy();
        		}
       	 	});
        diag.show(100, 100);
    },
    
    renderProject : function(project, uiParent, showFull) {
    	var modl = new ProjectTreeModel(project, showFull);
    	var opLink = makeElement(uiParent, "div", "minorBtn", showFull ? "Mini View" : "Full View");
        opLink._treeMod = modl;
        opLink._panel = uiParent._panel;
        opLink._showFull = showFull;
        modl.opLink = opLink;
        makeElementNbSpd(opLink, "div", "mbIco mb_bakIco");
        setEventHandler(opLink, "onclick", ProjectTreeModel.opClick);

        if (showFull) {
        	var colLink = makeElement(uiParent, "div", "minorBtn", "Columns");
        	colLink._treeMod = modl;
        	colLink._panel = uiParent._panel;
        	makeElementNbSpd(colLink, "div", "mbIco mb_viewIco");
        	setEventHandler(colLink, "onclick", ProjectTreeModel.colClick);
        }
    	
    	var grid = new Grid(modl);
        grid.addHeader("Activity");
        grid.addHeader("Task", "step");
        grid.addHeader("Due", "due");
        //grid.addHeader("Priority");
        //if (showFull) grid.addHeader("From");
        grid.addHeader("To");
        //grid.addHeader("Status");
        ActivityTree.projectId = modl.projectId;
        
        if (showFull) {
	        var cust = modl.getCustColumns();
	        for (var ii = 0; ii < cust.length; ii++) {
	        	grid.addHeader(cust[ii].name, "cust", cust[ii].filter);
	        }
        }
	    	
        makeElement(uiParent, "div", "minorHead", modl.doc.documentElement.getAttribute("name"));
        makeElement(uiParent, "div", "minorDesc", 
    	 	Xml.stringForNode(Xml.matchOne(modl.doc.documentElement, "description")));
    	 	
    	var hElem = makeElement(uiParent, "div", "minorTreeBox");
    	 	
    	
    	/*
    	
    	var edLink;
    	edLink = makeElement(titleElem,"span", "editBtn", "Edit");

        makeElementNbSpd(edLink, "div", "mbIco mb_ediIco");
    	edLink.onclick = function() { alert('edit'); };
    	*/
    	
        grid.render(hElem, "activityGrid");
        if (uiParent._panel) uiParent._panel.linkResize(grid);
        var tree = new Tree(modl);
        App.setProjectMenu(tree);
        tree.makeDragCanvas(document.body, new ActivityTreeDNDType("fileTree activityTree"), true, false, true);

        tree.render(hElem, grid.getTreeStyle(), "fileTree activityTree");
        
        ActivityTree.__tree = tree;
        
        modl.projCountElem = makeElement(hElem, "div", "projCount", modl.filtCount + "/" + modl.total + " activities");
        

		var launLink = makeElement(hElem, "div", "minorBtn", "Add New Activity...");
		makeElementNbSpd(launLink, "div", "mbIco mb_launIco");
		setEventHandler(launLink, "onclick", 
				function(evt) {
					ActivityTree.preLaunch(true);
				});
        
        App.addDispose(grid);
        App.addDispose(tree);
        hElem = null; opLink = null; uiParent = null; launLink = null; // IE enclosure clean-up
    },
    
    // launch too
    flowLaunch : function(flow) {
    	// TODO load roles from model
    	var roles = null;
    	ActivityTree.activityLaunch(flow, Uri.name(flow), "", false, roles);
    },
    
    flowDesign : function(flow) {
    	try {
    		location.href = "../mod/page?uri=" + Uri.escape(flow + "/chart.flow");
    	} catch (e) {}
    },
    
    properties : function(evt, item) {
    	Tree.selectItem(item);
        var diag = xfDialog((App.edu ? "Course" : "Activity") + " Properites: " + item.node.getAttribute("name"),
                true, document.body, "../view-wf/activity.xfrm.jsp", ActivityTree.xb, null,
                ActivityTree.serviceUri + "activityInfo?id=" + Uri.escape(item.getId()),
                null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.addEventListener("xforms-close", { handleEvent : function() {
                ActivityTree.refresh(null, item.itemParent);
            }});
        diag.show(100, 100);
    },
    
   	rollBack : function(evt, item) {
   		
   		var actNode = ActivityTree.activityInfo(item.getId());
   		if (actNode == null) return;
   		
   		var diag = new Dialog("Rollback: " + item.node.getAttribute("name"), true);
   		diag.initHelp(App.chromeHelp);
    	diag.render(document.body);
    	
    	var txSet = {};
    	    	
    	var states = Xml.match(actNode, "state");
    	var ii;
    	for ( ii = 0; ii < states.length; ii++) {
    		var stNd = states[ii];
    		var txId = stNd.getAttribute("txId");
    		if (txSet[txId]) {
    			txSet[txId].push(stNd);
    		} else {
    			txSet[txId] = [stNd];
    		}
    	}
    	var pDiv = makeElement(diag.contentElement, "div", "histDiag");
        var histTable = makeLayoutTable(pDiv, "hist");
        var hrow = makeElement(histTable, "tr");
        makeElement(hrow, "th", null, "Active Tasks");
        makeElement(hrow, "th", null, "Start Time");
        makeElement(hrow, "th", null, "Option");
        ii = 0;
        for (var txId in txSet) {
        	var row = makeElement(histTable, "tr", (ii % 2) ? "" : "alt");
        	var txSts = txSet[txId];
        	var tasks = "";
        	for (var jj = 0; jj < txSts.length; jj++) {
        		if (tasks != "") tasks += ", ";
        		tasks += txSts[jj].getAttribute("stepId");
        	}
            makeElement(row, "td", "first", tasks);
            makeElement(row, "td", "date",
                    DateUtil.toLocaleShort(DateUtil.parse8601(txSts[0].getAttribute("timeStamp"), true), true));
            var atd = makeElement(row, "td", "click", "rollback");
            atd._actId = item.getId();
            atd._txId = txId;
            atd._diag = diag;
            setEventHandler(atd, "onclick",  ActivityTree.__rollbackAct);
            ii++;
        }

   		diag.show(getMouseX(evt) + 40, getMouseY(evt));
  	},
  	
  	__rollbackAct : function() {
  		ActivityTree.undo(this._actId, this._txId);
  		this._diag.destroy();
  	},

    updateActivity : function(node) {
    	var aid = node.getAttribute("id");
    	if (App.activeActivityNode && App.activeActivityNode.getAttribute("id") == aid) {
    		App.activeActivityNode = node;
    	}
    	var info = ActivityTree.__activityInfo[aid];
    	if (info) {
    		node.setAttribute("uri", info.getAttribute("uri"));
    		ActivityTree.__activityInfo[aid] = node;
    	}
        var item = ActivityTree.__tree.model.findItem(aid);
        if (item != null) item.update(node);
    },
    
    findItem : function(id) {
    	return ActivityTree.__tree ? ActivityTree.__tree.model.findItem(id) : null;
    },
    
    __saveAttrToDoc : function(doc) {
    	var attrXfrm = PmCanvas.mainCanvas.getAttrXform();
    	var root = doc.documentElement;
    	var data = Xml.element(root, "rule-data");
        if (attrXfrm && attrXfrm._attrSet) {
        	var xfMod = attrXfrm.getDefaultModel();
        	for (var ii = 0; ii < attrXfrm._attrSet.length; ii++) {
        		var attrName = attrXfrm._attrSet[ii];
        		Xml.element(data, attrName, xfMod.getValue(attrName));
        	}
        }
    },

    submit : function(id, step, expr, uiParent, clearUi) {
    	
    	var item = ActivityTree.findItem(id);
    	
    	var swMode = false;
    	if (typeof expr == "object" && expr.constructor === ComboBox) {
    		expr = expr.getValue();
    		if (!expr) {
    			alert("Please select a path.");
    			return;
    		}
    		swMode = true;
    	} else if (UserTree.getSelfId() != item.getAssignId(step)) {
    		if (!confirm("This step is assigned to someone else, are you sure?")) {
    			return;
    		}
    	}
        var doc = ActivityTree.xb.createWithRoot("submit", "");
        var root = doc.documentElement;
        Xml.element(root, "activity", id);
        Xml.element(root, "step", step);
        
        if (swMode) Xml.element(root, "switch-path", expr);
        else Xml.element(root, "expr", expr);
        
        ActivityTree.__saveAttrToDoc(doc);

        var resDoc = ActivityTree.xb.loadURIPost("../act/submit", doc);

        if (App.checkError(resDoc)) return;
        
        // clean up for sequential workzones
        XFModel.clearLoadCache();

        var iNode = resDoc.documentElement;
        ActivityTree.updateActivity(iNode);
        var resTx = iNode.getAttribute("resTx");
        
        var ii;
        var stateNodes = Xml.match(iNode, "state");
        
        // Edu get's instant next-step
        if (ActivityTree.isCourse || ActivityTree.skipTrans) {
        	for (ii = 0; ii < stateNodes.length; ii++) {
        		var stNd = stateNodes[ii];
	        	var subState = stNd.getAttribute("subState");
	        	if ((subState == "ENTER_STEP" && stNd.getAttribute("assignId") == UserTree.getSelfId())
	        			|| subState == "ENTER_SWITCH") {
	        		
	        		App.setActiveActivity(iNode, stNd.getAttribute("stepId"));
	        		return;
	        	}
        	}
        }
        
        var undo = null, msgElem, link;
        if (clearUi) uiParent.innerHTML = "";
        msgElem = H.div({klass:"subMsg"},
        				ActivityTree.isCourse ? H.div({klass:"head"}, "This course is complete!") :
                        H.div({klass:"head"},"Your work on this step has been submitted.",
                                (undo = H.span({klass:"undo"}, "Undo")))
                    );
        uiParent.appendChild(msgElem);

        // still me?
        var movedToken = false, stepLink = false;
        for (ii = 0; ii < stateNodes.length; ii++) {
        	var stNd = stateNodes[ii];
        	var subState = stNd.getAttribute("subState");
        	if ((subState == "ENTER_STEP" && stNd.getAttribute("assignId") == UserTree.getSelfId())
        			|| subState == "ENTER_SWITCH") {
        		var stepId = stNd.getAttribute("stepId");
        		if (!movedToken) {
        			movedToken = true;
        			if (PmCanvas.mainCanvas) PmCanvas.mainCanvas.showToken(stepId);
        		}
        		stepLink = true;
	            link = H.div({klass:"link"}, "Work on: " + stepId + " >");
	            link._iNode = iNode;
	            link._stepId = stepId;
	            link.onclick = function () {
	                    App.setActiveActivity(this._iNode, this._stepId);
	                    this._iNode = null; // IE enclosure clean-up
	                };
	            msgElem.appendChild(link);
        	}
        }
        
        
        if (iNode.getAttribute("parent")) {
        	var pid = iNode.getAttribute("parent");
        	// link to parent activity
        	var pinfNode = ActivityTree.activityInfo(pid, true);
        	
        	var pStepStNd = pinfNode ? Xml.matchOne(pinfNode, "state", "subActivityId", id) : null;
        	
        	// If sub-step link step
        	if (pStepStNd) {
	        	var pStepId = pStepStNd.getAttribute("stepId");
	        	link = H.div({klass:"link"}, "View parent: " + pStepId + " >");
	        	link._iNode = pinfNode;
		        link._stepId = pStepId;
		        link.onclick = function() {
		        		App.setActiveActivity(this._iNode, this._stepId);
		        		App.activeFlow = null;
		        		Modes.todo(null, true);
		               	this._iNode = null; // IE enclosure clean-up
		        	};
		        msgElem.appendChild(link);
	        	
        	} else {
	        	// Otherwise status
	        	link = H.div({klass:"link"}, "View parent status >");
		        link.onclick = function() {
		        		location.href = "stat?activity=" + pid;
		        	};
		        msgElem.appendChild(link);
        	}
        }
        
        if (!stepLink) {
        	// link to home
        	link = H.div({klass:"link"}, App.edu ? "Return home >" : "Find other work on the Home To Do List >");
	        link.onclick = function() {
	        		var lhf = "../home/";
	        		if (App.kiosk) lhf += "kiosk.jsp";
	        		if (App.embed) lhf += "?embed=1";
	        		location.href = lhf;
	        	};
	        msgElem.appendChild(link);
        }
        
        if (!movedToken) {
        	var stNd = Xml.matchOne(iNode, "state");
			if (stNd && PmCanvas.mainCanvas) PmCanvas.mainCanvas.showToken(stNd.getAttribute("stepId"));
		}
		if (undo)
	        undo.onclick = function() {
	                ActivityTree.undo(id, resTx);
	            };
        iNode = null; undo = null; msgElem = null; uiParent = null; root = null; doc = null; link = null; data = null; // IE enclosure clean-up
    },

    undo : function(id, txId) {
        var resDoc = this.xb.loadURI(this.serviceUri + "undo?id=" + id + "&txId=" + txId);
        if (App.checkError(resDoc)) return;
        var node = resDoc.documentElement;
        ActivityTree.updateActivity(node);
        App.setActiveActivity(node);
    },

    getPlan : function(step) {
        if (App.activeActivityNode == null) {
            return null;
        }
        var resDoc = this.xb.loadURI(this.serviceUri + "getPlan?id=" + App.activeActivityNode.getAttribute("id") +
                                     "&step=" + Uri.escape(step));
        if (App.checkError(resDoc)) return null;
        return resDoc.documentElement;
    },

    setPlan : function(step, skip) {
        if (App.activeActivityNode == null) {
            return null;
        }
        App.checkError(this.xb.loadURI(this.serviceUri + "setPlan?id=" + App.activeActivityNode.getAttribute("id") +
                                       "&step=" + Uri.escape(step) + "&skip=" + (skip ? "1" : "0")));
    },

    getActivityTimer : function(id, timerId) {
    	var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "getTimer?activity=" + id + 
    		"&timer=" + Uri.escape(timerId));
        if (App.checkError(doc)) return null;
        return doc.documentElement.getAttribute("atTime");
    },
    
    setManualActivityTimer : function(id, timerId, atTime) {
    	var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "setManualTimer?activity=" + id + 
    		"&timer=" + Uri.escape(timerId) + "&at=" + Uri.escape(atTime));
        if (App.checkError(doc)) return;
    },

    hookWikiActivity : function(wikiView) {
        App.activityListeners.push(function(node) {
            if (node != null) {
                wikiView.goTitle(node.getAttribute("name"),
           			{ lockId:true, titlePrefix:"Log for: ", timeStampEdit:true, layout:""});
            } else {
                wikiView.clear();
            }
        });
    },
    
    renderFlowTree : function(uiParent, uri) {
    	var mod = new FlowTreeModel(uri);
    	var tree = new Tree(mod);
    	tree.render(uiParent, null, "flowTree");
    	tree.menu = new Menu(new MenuModel(
             [
                     {label : "Design", icon : "mb_desIco", act : ActivityTree.flowTreeDesign},
                     {label : "Launch", icon : "mb_launIco", act : ActivityTree.flowTreeLaunch}
             ]));
    	App.addDispose(tree);
    	return tree;
    },
    
    flowTreeDesign : function(evt, item) {
    	ActivityTree.flowDesign(item.uri);
    },
    
    flowTreeLaunch : function(evt, item) {
    	ActivityTree.flowLaunch(item.uri);
    },
    
    addToProjectMnOpt : function(evt, item) {
    	Tree.selectItem(item);
    	if (ActivityTree.addToProject(this.id, item.getId())) {
    		Ephemeral.topMessage(item.__domElem, "Added to project.");
    	}
    },
    
    removeFromProjectMnOpt : function(evt, item) {
    	ActivityTree.removeFromProject(item.model.projectId, item.getId());
    	ActivityTree.refresh(null, item.itemParent);
    },
    
    removeFromProject : function(projId, actId) {
    	ActivityTree.xb.loadURI(
    		ActivityTree.projectSvcUri + "removeActivity?projId=" + Uri.escape(projId) 
    		+ "&actId=" + Uri.escape(actId));
    },
    
    addToProject : function(projId, actId) {
    	return ! App.checkError(ActivityTree.xb.loadURI(
    		ActivityTree.projectSvcUri + "addActivity?projId=" + Uri.escape(projId) 
    		+ "&actId=" + Uri.escape(actId)));
    },
    
    sendToNewMnOpt : function(evt, item) {
    	Tree.selectItem(item);
        var diag = xfDialog("Swap '" + item.node.getAttribute("name") + "'  with new process",
        	true, document.body, "../view-wf/send-to.xfrm", ActivityTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.setValue("activity", item.getId());
        xfMod.addEventListener("xforms-close", { handleEvent : function() {
                ActivityTree.refresh(null, item.itemParent);
            }});
        diag.show(100, 100);
    },
    
    sendToMenu : function(evt, actId) {
    	var item = { needReload: true,
    		getId : function() { return actId; }};
    	var sendFlow = new Menu(new TNavProcMenuModel("modifiedProcesses", ActivityTree.sendToNewMnOpt),
			ActivityTree.sendToFlowMnOpt);
		sendFlow.popUp(evt, item);
    },
    
    sendToFlowMnOpt : function(evt, item) {
    	Tree.selectItem(item);
    	if (confirm("Are you sure?")) {
    		var doc = ActivityTree.xb.loadURI(
		    		ActivityTree.serviceUri + "sendToFlow?flow=" + Uri.escape(this.uri) 
		    		+ "&activity=" + Uri.escape(item.getId()));
    		if (!App.checkError(doc)) {
    			if (item.needReload) {
    				location.href = "../act/page?activity=" + doc.documentElement.getAttribute("id");
    			} else {
	    			Ephemeral.topMessage(item.__domElem, "Sent to process.");
	    			ActivityTree.refresh(null, item.itemParent);
    			}
    		}
    	}
    },
    
    invalidActivity : function(actId) {
    	var diag = xfDialog("Activity folder missing", true, document.body, "../view-wf/invalid-act.xfrm", ActivityTree.xb, actId,
    		null, null, false, App ? App.chromeHelp : null);
    	diag.showModal(200, 200);
    },
    
    renderActivityInfo : function(id, uiParent, title, cssClass) {
    	var itemElem = makeLayoutTable(uiParent, "activity");
    	if (cssClass) exAddClass(itemElem.parentNode, cssClass);
        if (!title) title = "WorkZone";
        itemElem.title = "Activity: " + title;
        window.setTimeout(function() {
                var node = ActivityTree.activityInfo(id, true);
                var tr = makeElement(itemElem, "tr");
                if (!node) {
                    makeElement(tr, "td", "error", App.lastError);
                } else {
                    var states =  Xml.match(node, "state");
                    makeElementNbSpd(makeElement(tr, "td"), "div", "icon " + node.getAttribute("icon") + "Ico");
                    makeElement(tr, "td", "name", node.getAttribute("name"));
                    if (states.length > 0) {
                    	var std = makeElement(tr, "td", "step");
                    	for (var ii = 0; ii < states.length; ii++) {
                    		if (ii > 1 && states[ii].getAttribute("subState") == "ENTER_END") continue;
                    		makeElement(std, "div", (ii < 1) ? "first" : "multi", states[ii].getAttribute("stepId"));
                    	}
                    }
                    tr = makeElement(itemElem, "tr");
                    makeElementNbSpd(tr, "td");
                    makeElement(tr, "td", "desc", node.getAttribute("description"), null, {colSpan : 2});
                    itemElem.onclick = function () {
                        try { window.open("../act/page?activity=" + id, "_blank"); } catch(e){}
                    }
                }
                node = null; tr = null; itemElem = null; // IE enclosure clean-up
            }, 20);
        uiParent = null; // IE enclosure clean-up
        return itemElem.parentNode;
    },
    
    getModelUri : function(actId) {
    	var node = ActivityTree.activityInfo(actId, true);
    	var actUri = node.getAttribute("uri");
    	return "../mod/getModel?uri=" + Uri.escape(Uri.absolute(Uri.parent(Uri.parent(actUri)), "chart.flow")) +
    		"&activity=" + actId;
    },
    
    preLaunch : function(inProject) {
    	 //App.snavMenuProcLan.popUp(event)
    	 
    	ActivityTree.__preLanDiag = xfDialog(inProject ? "Add New Activity" : "Launch a Process",
    	 	true, document.body, "../" + (inProject ? "view-proj" : "view-wf") + "/pre-launch.xfrm", ActivityTree.xb,
    	 	null, null, null, false, App ? App.chromeHelp : null);
    	ActivityTree.__preLanDiag.showModal(200, 35);
    },
    
    progressAssigner : {
    	
    	setAssign : function(uid) {
    		var stepId = App.activeStateNode.getAttribute("stepId");
    		
    		var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "setAssign?step=" +
                           Uri.escape(stepId) + "&activity=" + App.activeActivityId
                           + "&assignId=" + uid);
        	if (App.checkError(doc)) return;
        	App.activeStateNode.setAttribute("assignId", uid);
        	var item = ActivityTree.findItem(App.activeActivityId);
        	ActivityTree.refresh(null, item);
        	ActivityTree.setActivityItem(item, stepId);
    	},
    	
    	assignClick : function(evt) {
    		ActivityTree.todoAssignCtrl.innerHTML = "";
    		var tm = PmCanvas.teamRoster.getMember(this.uid, true);
    		tm.render(ActivityTree.todoAssignCtrl);
    		ActivityTree.progressAssigner.setAssign(this.uid);
    	},
    	
    	menuClick : function(evt) {
    		var usrMenu = UserTree.getUserMenu();
    		var menu = new Menu(new MenuModel(
         		[{label : "Change Assignment", icon : "mb_usrIco", sub : usrMenu }]));
    		App.addDispose(menu);
    		usrMenu.defaultAct = ActivityTree.progressAssigner.assignClick;
    		menu.popUp(evt);
    		return false;
    	}
    },
    
    progressListener : function(stateNode) {
    	//console.log(stateNode);
    	if (stateNode && stateNode.getAttribute("subState") == "ENTER_STEP" && !stateNode.getAttribute("subActivityId")) {
    		ActivityTree.progressCtrl.style.display = "";
    		if (ActivityTree.percInput) ActivityTree.percInput.value = stateNode.getAttribute("progress");
    		if (ActivityTree.todoAssignCtrl && PmCanvas.teamRoster) {
    			ActivityTree.todoAssignCtrl.innerHTML = "";
    			var tm = PmCanvas.teamRoster.getMember(stateNode.getAttribute("assignId"), true);
    			tm.render(ActivityTree.todoAssignCtrl);
    		}
    	} else {
    		ActivityTree.progressCtrl.style.display = "none";
    	}
    },
    
    taskProcPick : function() {
		if (App._tnavAllProcDiag) {
    		App._tnavAllProcDiag.destroy();
    		App._tnavAllProcDiag = null;
    	}
    	if (App.activeStateNode) {
	    	var launDoc = ActivityTree.xb.loadXML("<launch/>");
	    	var root = launDoc.documentElement;
	    	Xml.element(root, "flow", this.uri);
	    	Xml.element(root, "name", Uri.name(App.activeStepId));
	    	Xml.element(root, "parent", App.activeActivityId);
	    	Xml.element(root, "parentStep", App.activeStepId);
	    	var res = ActivityTree.xb.loadURIPost(ActivityTree.serviceUri + "launch", launDoc);
	    	if (!App.checkError(res)) {
	    		ActivityTree.refresh();
	    	}
    	}
    },
    
    saveProgress : function() {
    	var progVal = 0;
    	if (ActivityTree.percInput) {
	    	progVal = parseInt(ActivityTree.percInput.value);
	    	if (isNaN(progVal)) {
	    		progVal = 0;
	    		if (App.activeStateNode) {
	    			progVal = App.activeStateNode.getAttribute("progress");
	    		}
	    		ActivityTree.percInput.value = progVal;
    		}
    	}
    	
    	// send save event to all workzone forms
    	if (PmCanvas.mainCanvas.wiki) {
        	var wikiView = PmCanvas.mainCanvas.wiki.views[0];
        	var dlist = wikiView.disposeList;
        	for (var ii=0; ii < dlist.length; ii++) {
        		var xf = dlist[ii];
        		if (xf.constructor === XForm) {
        			xf.fireEvent("ix-activity-save", xf.getDefaultModel());
        		}
        	}
        }
    	
    	
    	var progDoc = ActivityTree.xb.loadXML("<progress/>");
    	var root = progDoc.documentElement;
    	root.setAttribute("progress", progVal);
    	root.setAttribute("stepId", App.activeStepId);
    	ActivityTree.__saveAttrToDoc(progDoc);
		var res = ActivityTree.xb.loadURIPost(
			ActivityTree.serviceUri + "saveProgress?activity=" + App.activeActivityId, progDoc);
		if (!App.checkError(res)) {
			if (App.activeStateNode) App.activeStateNode.setAttribute("progress", progVal);
    		Ephemeral.insideMessage(ActivityTree.progressPanel.contentElement, "Progress Saved.");
		}
    },
    
    dispose : function() {
    	if (ActivityTree.__activityInfo != null)
    		for (var acti in ActivityTree.__activityInfo)
                delete ActivityTree.__activityInfo[acti];
    	ActivityTree.__activityInfo = null;
    	ActivityTree.progressCtrl = null;
    	ActivityTree.percInput = null;
    }    
};



ActivityTreeModel.prototype = new TreeGridModel();
ActivityTreeModel.prototype.constructor = ActivityTreeModel;

function ActivityTreeModel(mode, flowUri) {
    TreeGridModel.apply(this, []);
    this.mode = mode;
    this.flowUri = flowUri;
    this.xb = ActivityTree.xb;
    this.nCols = 4;
    this.itemIndex = new Object();
    var holdThis = this;
    App.activityListeners.push(function(node, stepId) {

            // find matching node
            var item = null;
            if (node != null) {
                var id = node.getAttribute("id");
                item = holdThis.findItem(id);
            }
            if (item == null) {
                holdThis.setSelected([]);
                holdThis._tree.markSelected();
                return;
            }

            var stItem = (stepId && Modes.mode == "todo") ? item.findStateItem(stepId) : null;
            Tree.selectItem(stItem ? stItem : item);
        });
}

ActivityTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    var holdThis = this;
    if (itemParent === this) { // at root
    	this.kids = [];
    	if (this.idSetDoc) {
    		
    		this.itemIndex = new Object();
    		this.digest(this.idSetDoc, callback, tree, itemParent);
    		
    		// reset idSet
    		this.idSet = [];
    		for (var aid in this.itemIndex) this.idSet.push(aid);
    		this.idSetDoc = null;
    		
	       	this.resize();
    	} else if (this.idSet) {
    		var loadUri = ActivityTree.serviceUri + "setList?id=" + this.idSet.join("&id=");
	        this.xb.loadURIAsync(loadUri,
	            function(doc, arg, xmlHttp) {
	                if (doc == null || App.checkError(doc)) {
	                    // TODO error message
	                } else {
	                    holdThis.itemIndex = new Object();
	                    holdThis.digest(doc, callback, tree, itemParent);
	                    holdThis.resize();
	                }
	            });
    	} else {
	        var loadUri = ActivityTree.serviceUri + this.mode + "List";
	        if (this.flowUri) loadUri += "?flow=" + Uri.escape(this.flowUri);
	        this.xb.loadURIAsync(loadUri,
	            function(doc, arg, xmlHttp) {
	                if (doc == null || App.checkError(doc)) {
	                    // TODO error message
	                } else {
	                    holdThis.itemIndex = new Object();
	                    holdThis.digest(doc, callback, tree, itemParent);
	                    holdThis.resize();
	                }
	            });
    	}
    } else { // child items
        if (itemParent.preloaded) {
            itemParent.preloaded = false;
            callback.apply(tree, [itemParent.kids, itemParent]);
            this.addItemRows(itemParent.kids, itemParent);
            return;
        }
        this.xb.loadURIAsync(ActivityTree.serviceUri + "kidList?id=" + Uri.escape(itemParent.node.getAttribute("id")),
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    }
};

ActivityTreeModel.prototype.digest = function(doc, callback, tree, itemParent) {
    var list = Xml.match(doc.documentElement, "activity");
    var toggles = [];
    var actItm = null;
    var ii;        

    if (itemParent === this) {
        // pass-1 index
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var hasKids = parseInt(nod.getAttribute("activeKids")) > 0;
            if (App.activeActivityId == nod.getAttribute("id")) nod = App.activeActivityNode || nod;
            var tItm = new ActivityTreeItem(this, nod, true, nod.getAttribute("icon") + "Ico");
            if (App.guest) tItm.optAct = null;
            this.itemIndex[tItm.getId()] = tItm;
            list[ii] = tItm;
            if (!hasKids) {
            	tItm.loadStates(this.itemIndex);
            	tItm.preloaded = true;
            	toggles.push(tItm);
            }
        }

        // pass-2 nest
        for (ii = 0; ii < list.length; ii++) {
            var tItm = list[ii];
            var pid = tItm.node.getAttribute("parent");
            if (pid) {
                var par = this.itemIndex[pid];
                if (par) {
                	if (!par.preloaded) {
	                    par.preloaded = true;
	                    par.loadStates(this.itemIndex);
                	}
                    if (!tItm.pStateNode) par.add(tItm);
                    if (arrayFindStrict(toggles, par) < 0) toggles.push(par);
                    // TODO add more button...
                } else {
                    // TODO else parent decoration
                    itemParent.add(tItm);
                }
            } else {
                itemParent.add(tItm);
            }
            if (tItm.getId() == App.activeActivityId) {
                actItm = tItm;
            }
            
        }
    } else {
    	itemParent.loadStates(this.itemIndex);

        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var hasKids = parseInt(nod.getAttribute("activeKids")) > 0;
            var tItm = new ActivityTreeItem(this, nod, true, nod.getAttribute("icon") + "Ico");
            if (App.guest) tItm.optAct = null;
            this.itemIndex[tItm.getId()] = tItm;
            itemParent.add(tItm);
            tItm.loadStates(this.itemIndex);
            tItm.preloaded = !hasKids;
            if (tItm.getId() == App.activeActivityId) {
	            actItm = tItm;
	        }
        }
    }
    
    if (actItm == null && App.activeActivityId && itemParent === this && this.mode != "submitted") {
    	// active activity not in current set, so load it to the root anyway
    	var nod = ActivityTree.activityInfo(App.activeActivityId);
    	if (nod) {
	    	var hasKids = parseInt(nod.getAttribute("activeKids")) > 0;
	        var tItm = new ActivityTreeItem(this, nod, true, nod.getAttribute("icon") + "Ico");
	        itemParent.itemIndex[tItm.getId()] = tItm;
	        itemParent.add(tItm);
	        actItm = tItm;
	       	if (!hasKids) tItm.loadStates(this.itemIndex);
            tItm.preloaded = !hasKids;
    	}
    }

	if (actItm && arrayFindStrict(toggles, actItm) < 0) toggles.push(actItm);
	
    if (itemParent.kids.length == 0 && itemParent === this) {
    	var blank = new ActivityTreeItem(this, null, false, "blankIco");
        blank.act = blank.optAct = null;
        blank.empty = true;
        itemParent.add(blank);
        if (Modes.mode == "") App.setShowMinor(false);
    } else {
    	if (Modes.mode == "") App.setShowMinor(true);
    }
    
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
    toggles.sort(ActivityTreeModel.depthSort);
    
    for (ii = 0; ii < toggles.length; ii++) {
        tree.toggle(toggles[ii]);
    }
    
    if (actItm != null) {
    	 window.setTimeout(function() { actItm.act(null, App.activeStepId); }, 50);
    }
};

ActivityTreeModel.depthSort = function(a, b) {
    return a.getDepth() - b.getDepth();
};

ActivityTreeModel.prototype.findItem = function(id) {
    return this.itemIndex[id];
};

ActivityTreeModel.prototype.getId = function() {
    return "";
};

ActivityTreeModel.opClick = function() {
    var modl = this._treeMod;
    var tText = App.activeFlow ? Uri.name(Uri.parent(App.activeFlow))  : null;
    if (this._mode == "assigned") {
        modl.title.firstChild.nodeValue = tText ? tText : "Requested Tasks";
        this.firstChild.nodeValue = "Assigned Tasks";
        this._mode = "submitted";
        modl.grid.setHeader(2, "To");
    } else {
        modl.title.firstChild.nodeValue = tText ? tText : "Assigned Tasks";
        this.firstChild.nodeValue = "Requested Tasks";
        this._mode = "assigned";
        modl.grid.setHeader(2, "From");
    }
    modl.mode = this._mode;
    modl._tree.redrawAll();
};

ActivityTreeDNDType.prototype = new TreeDNDType();
ActivityTreeDNDType.prototype.constructor = ActivityTreeDNDType;

function ActivityTreeDNDType(cssClass) {
    this.type = "dndWFActivity";
    this.cssClass = cssClass;
    this.menu = new Menu(new MenuModel(
         [
                 {label : "Move Here", icon : "mb_movIco", act : ActivityTreeDNDType.moveItem},
                 {isSep : true },
                 {label : "Cancel", act : ActivityTreeDNDType.cancelItem}
         ]));
}

ActivityTreeDNDType.cancelItem = function (evt, pair) {
    if (!pair.isRoot) {
        pair.dropItem.model.setSelected([]);
        pair.dropItem._tree.markSelected();
    }
};

ActivityTreeDNDType.moveItem = function (evt, pair) {
    if (!App.checkError(ActivityTree.xb.loadURI(ActivityTree.serviceUri + "move?srcId=" +
             Uri.escape(pair.dragItem.node.getAttribute("id")) +
                 "&dstId=" + (pair.isRoot ? "" : Uri.escape(pair.dropItem.node.getAttribute("id")))))) {
        pair.dragItem.removeItem();
        if (pair.isRoot || Modes.mode == "proj") {
            pair.dropItem._tree.redrawAll();
        } else {
            pair.dropItem.allowsKids = true;
            pair.dropItem._tree.redraw(pair.dropItem, Tree.HINT_INSERT);
        }
    }
};


ActivityTreeDNDType.prototype.canDrag = function(dragElem) {
	var dragItem = dragElem.__item;
	if (dragItem.constructor === ActStepTreeItem) return false;
    return true;
};

ActivityTreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
    var pair;
    var dragItem = dragElem._actElem.__item;
    var rect = getViewBounds(dropElem);
    if (dropElem._isFoot) {
        pair = {dragItem:dragItem, isRoot:true, tree:this._tree};
    } else {
        var dropItem = dropElem.__item;
        dropItem.model.setSelected([dropItem, dragItem]);
        dropItem._tree.markSelected();
        pair = {dragItem:dragItem, dropItem:dropItem};
    }
    this.menu.show(rect.x + 102, rect.y, null, pair);
};

ActivityTreeDNDType.prototype.dropTest = function(dropElem, dragElem) {

    // default same tree type test
    if (dropElem._dndType.__canvas === dragElem._dndType.__canvas && dragElem._actElem) {
        if (dragElem._dndType instanceof DNDGroup) {
            // TODO support groups
            return false;
        }
        var dragItem = dragElem._actElem.__item;
        if (dropElem._isFoot) return !(dragItem.model === dragItem.itemParent);
        var dropItem = dropElem.__item;
        if (dropItem.constructor === ActStepTreeItem) return false;
        return !dragItem.isAncestor(dropItem) &&
               DNDTypeDummy.prototype.dropTest.apply(this, [dropElem, dragElem]);
    }
    return false;
};

ActivityTreeItem.prototype = new TreeGridItem();
ActivityTreeItem.prototype.constructor = ActivityTreeItem;

function ActivityTreeItem(model, node, allowsKids, icon) {
	if (arguments.length == 0) return;
	if (model) TreeGridItem.apply(this, [model, null, allowsKids, icon]);
    this.rowCssClass = "actRow";
    this.cssClass = "actItem";
    this.borderH = 3;
    this.node = node;
    if (node) {
	   // this.states = Xml.match(node, "state");
	   // this.plans = Xml.matchDeep(node, "current");
	    this.tip = node.getAttribute("flowName");
	    if (node.getAttribute("variationId")) this.tip = "Variation of: " + this.tip;
    }
    if (model) {
	  /*  if (this.states) {
	        this.cells.push(new ActivityActionCell(this.states, node, "step", this));
	        this.cells.push(new GridCell(this.getDateText(), "due"));
	        this.cells.push(new ActivityUserCell(this.states, node, "user", this, model.mode));
	    } else { */
	    this.cellZero.colSpan = App.edu ? 2 : 3;
	    //    this.cells.push(new ActivityActionCell(null, node, "step", this));
	    //    this.cells.push(new GridCell("", "due"));
	    //    this.cells.push(new ActivityUserCell(null, node, "user", this, model.mode));
	    // }
    }
}

ActivityTreeItem.prototype.getDateText = function() {
    var dStr = null, due = null;
    //if (this.plan) dStr = this.plan.getAttribute("dueDate");
    if (!dStr && this.node) dStr = this.node.getAttribute("dueDate");
    if (dStr) {
        due = DateUtil.parse8601(dStr);
        var dd = DateUtil.dayDiff(due, new Date()) + 1;
        if (dd == 0) {
            this.rowCssClass += " today";
        } else if (dd < 0) {
            this.rowCssClass += " past";
        } else {
            this.rowCssClass += " norm";
        }
    }
    return due ? DateUtil.toLocaleWords(due) : "";
};

ActivityTreeItem.prototype.loadStates = function(activityIdx) {
	if (this.model.mode == "assigned")
		this.states = Xml.match(this.node, "state", "assignId", UserTree.getSelfId())
	else 
		this.states = Xml.match(this.node, "state");
		
	if (this.states.length > 0) {
    	for (var jj = 0; jj < this.states.length; jj++) {
    		var stNd = this.states[jj];
    		var subState = stNd.getAttribute("subState");
    		if (subState == "ENTER_END" || subState == "WAIT_ENTER_STEP") continue;
    		var subAct = stNd.getAttribute("subActivityId");
    		if (subAct) {
    			var subActItm = activityIdx[subAct];
    			if (!subActItm) {
    				
    				// TODO - lazy loader of sub-process...
    				this.add(new ActStepTreeItem(this.model, stNd));
    				
    			} else {
    				
    				// tag this item
    				subActItm.pStateNode = stNd;
    				this.add(subActItm);
    			}
    		} else {
    			this.add(new ActStepTreeItem(this.model, stNd));
    		}
    	}
    }
};

// override
ActivityTreeItem.prototype.removeKid = function(item) {
    arrayRemoveStrict(this.kids, item);
    if (this.kids.length < 1) {
        ActivityTree.refresh(null, this);
    }
};

ActivityTreeItem.prototype.getUserText = function() {
    var usrId = this.model.mode == "assigned" ? this.node.getAttribute("submitId") : this.node.getAttribute("assignId");
    return usrId ? UserTree.getUserName(usrId) : "";
};

ActivityTreeItem.prototype.getAssignId = function(step) {
	var state = this.getStepState(step);
	if (state) return state.getAttribute("assignId");
	return null;
};

ActivityTreeItem.prototype.canSubmit = function(step, stepObj) {
	var state = this.getStepState(step);
	if (state == null) return stepObj.constructor === PmSwitch;
	
	if (state.getAttribute("subActivityId")) {
		if (!ActivityTreeItem.subActDone(state)) return false;
	}
	if (state.getAttribute("subState") == "ENTER_SWITCH") return true;
	return state.getAttribute("assignId") == UserTree.getSelfId()
	 	|| this.node.getAttribute("submitId") == UserTree.getSelfId();
};

ActivityTreeItem.subActDone = function(state) {
	var subActId = state.getAttribute("subActivityId");
	var subActInf = ActivityTree.activityInfo(subActId, true);
	return subActInf && Xml.matchOne(subActInf, "state", "subState", "ENTER_END") != null;
};

ActivityTreeItem.prototype.getStepState = function(step) {
	if (!this.states) this.states = Xml.match(this.node, "state");
	for (var ii = 0; ii < this.states.length; ii++) {
		var state = this.states[ii];
		if (step == state.getAttribute("stepId")) {
			return state;
		}
	}
	return null;
};

ActivityTreeItem.prototype.update = function(node) {
    this.node = node;
    this.states = null;
    if (this.model) {
    	this.model._tree.redraw(this);
    	this.syncSize(true);
    }
};

ActivityTreeItem.prototype.getId = function() {
    return this.node ? this.node.getAttribute("id") : "";
};

ActivityTreeItem.prototype.findStateItem = function(stepId) {
	for (var ii=0; ii < this.kids.length; ii++) {
		var kid = this.kids[ii];
		if (kid.constructor === ActStepTreeItem) {
			if (kid.node.getAttribute("stepId") == stepId) return kid;
		} else if (kid.pStateNode) {
			if (kid.pStateNode.getAttribute("stepId") == stepId) return kid;
		}
	}
	return null;
};

ActivityTreeItem.prototype.isAncestor = function(item) {
    while (this !== item && this.getId() != item.getId()) {
        item = item.itemParent;
        if (item == null) return false;
    }
    return true;
};

ActivityTreeItem.prototype.dispose = function() {
    var id = this.getId();
    if (this.model && this.model.itemIndex[id] === this) {
        delete this.model.itemIndex[id];
    }
    if (this.parMenu) this.parMenu.dispose();
    this.parInf = null;
    this.states = null;
    this.plans = null;
    this.pStateNode = null;
    TreeGridItem.prototype.dispose.apply(this, []);
};

ActivityTreeItem.prototype.act = ActivityTree.itemAct;
ActivityTreeItem.prototype.editAct = null;
ActivityTreeItem.prototype.optAct = treeMenuAction;

ActivityTreeItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
        if (this.node) {
        	if (this.model.constructor === ProjectTreeModel) {
        		  this.__labDomE = H.div({klass:"label " + this.rowCssClass},
		                    H.div({klass:"name"}, this.node.getAttribute("name") + " "),
		                    H.div({klass:"desc"}, this.node.getAttribute("description"))
	                    );
        	} else {
        		var parIndE = null;
        		if (this.node.getAttribute("parent") && this.itemParent === this.model) {
        			
        			parIndE = makeElementNbSpd(null, "div", "actParInd", null, {title:"Parent Activity"});
        			var holdThis = this;
        			setEventHandler(parIndE, "onmousedown", stopEvent);
        			setEventHandler(parIndE, "onclick", stopEvent);
        			setEventHandler(parIndE, "onmouseup", function(evt) {
        					
        					if (!holdThis.parMenu) {
        						holdThis.parInf = 
        							ActivityTree.activityInfo(holdThis.node.getAttribute("parent"), true);
        						if (!holdThis.parInf) return;
        						holdThis.parMenu = new Menu(new MenuModel([
        							{label:"View Parent: "  + holdThis.parInf.getAttribute("name") , icon:"mb_viewIco", act: function() {
        								try {
							                location.href = "../act/page?activity=" + holdThis.node.getAttribute("parent");
							            } catch(e){}
        							  }
        							}
        							]
        						));
        					}
        					holdThis.parMenu.popUp(evt);
        					return stopEvent(evt);
        				});
        			
        		}
        		var descTxt = this.node.getAttribute("description");
        		var nmTxt = this.node.getAttribute("name");
        		var nmLen = nmTxt.length * 1.8 + (descTxt ? descTxt.length : 0);
	            this.__labDomE = H.div({klass:"label " + this.rowCssClass}, H.nbsp,
	            		 	H.div({klass:"actLabel"},
		                    H.span({klass:"name"}, nmTxt + " "),
		                    H.span({klass:"desc"}, descTxt)),
		                    parIndE
	                    );
	           var pxH = 13;
			   if (nmLen > 55) {
			   		if (nmLen > 290) pxH = 120;
			   		else if (nmLen > 210) pxH = 60;
			   		else if (nmLen > 160) pxH = 48;
			   		else if (nmLen > 100) pxH = 38;
			   		else pxH = 30;
			   }
			   var actDue = this.getDateText();

			   if(!actDue) {
			   		actDue = getActRollUpDate(this.node);
			    	if(actDue) {
			    		actDue = DateUtil.toLocaleWords(actDue);
			    	}
			    }
			   if (actDue) {
			   		pxH += 12;
			   		makeElement(this.__labDomE, "div", "actDue", actDue);
			   }
			   this.__labDomE.style.height = pxH + "px";
	           parIndE = null; // IE enclosure clean-up
        	}
        } else {
        	// empty root in a flow and assigned mode
            this.__labDomE = H.div({klass:"label " + this.rowCssClass}, "<empty>");
        }
        hParent.appendChild(this.__labDomE);
    } else if (this.__labDomE.parentNode != hParent){
        hParent.appendChild(this.__labDomE);
    }
};


// no activity due date, get farthest-out due date from child states, if possible
getActRollUpDate = function(node) {
	   var stateAttributes=[]; 
	   var actDueDate = null;
		if (node && node.nodeName=="activity") {
		   stateAttrs = Xml.matchDeep(node, "current");
        for (var ii = 0; ii < stateAttrs.length; ii++) {
     	   var dStr = stateAttrs[ii].getAttribute("dueDate");
     	   if (dStr) {
 	        var stateDueDate = DateUtil.parse8601(dStr);
 	        // put first state value in activity
 	        if(!actDueDate) actDueDate = stateDueDate;

 	        // if state due date farther out make it activity due date
 	        var dd = DateUtil.dayDiff(actDueDate, stateDueDate);

 	        if(dd < 0) {
 	        	actDueDate = stateDueDate;
 	        }
     	   }
        }
	   }
	return actDueDate;
}


ActStepTreeItem.prototype = new TreeGridItem();
ActStepTreeItem.prototype.constructor = ActStepTreeItem;

function ActStepTreeItem(model, node) {
	TreeGridItem.apply(this, [model]);
	this.node = node;
	this.plan = Xml.matchOne(node, "current");
	this.rowCssClass = "norm";
	this.cells.push(new GridCell(this.getDateText(), "due", true));
	if (!App.edu) this.cells.push(new GridCell(this.getUserText(), "user", true));
}

ActStepTreeItem.prototype.getDateText = function() {
    var dStr = null, due = null;
    if (this.plan) {
    	dStr = this.plan.getAttribute("dueDate");
    }
    if (dStr) {
        due = DateUtil.parse8601(dStr);
        var dd = DateUtil.dayDiff(due, new Date()) + 1;
        if (dd == 0) {
            this.rowCssClass = "today";
        } else if (dd < 0) {
            this.rowCssClass = "past";
        } else {
            this.rowCssClass = "norm";
        }
    }
    return due ? DateUtil.toLocaleWords(due) : "";
};

ActStepTreeItem.prototype.getUserText = function() {
	var usrId = null;
	if (this.model.mode == "assigned") {
		usrId = this.node.parentNode.getAttribute("submitId");
	} else {
		usrId = this.node.getAttribute("assignId");
	}
	if (usrId) return UserTree.getUserName(usrId);
	return "";
};

ActStepTreeItem.prototype.act = function() {
	ActivityTree.setActivityItem(this.itemParent, this.node.getAttribute("stepId"));
};

ActStepTreeItem.prototype.editAct = null;
ActStepTreeItem.prototype.optAct = null;


ActStepTreeItem.prototype.getDynCssClass = function() {
	//var idx = this.itemParent.getIndex(this);
	return "freeStep";
};

ActStepTreeItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
    	// place holder
        this.__labDomE = makeElement(hParent, "div", "label " + this.rowCssClass, this.node.getAttribute("stepId"));
    } else if (this.__labDomE.parentNode != hParent){
        hParent.appendChild(this.__labDomE);
    }
};

ActivityMultiCell.prototype = new GridCell();

function ActivityMultiCell() { }

ActivityMultiCell.prototype.__initVal = function(nodes, fbNode, cssClass) {
	this.__hElem = null;
    this.__edit = false;
    this.__css = cssClass;
    this.nodes = nodes;
    this.fbNode = fbNode; // fallback node
}

ActivityMultiCell.prototype.setValue = function(nodes, fbNode) {
	this.nodes = nodes;
	this.fbNode = fbNode;
	var parNode = null;
	if (this.__hElem != null) {
		parNode = this.__hElem.parentNode;
		parNode.removeChild(this.__hElem);
	}
	this.__render();
	parNode.appendChild(this.__hElem);
};

ActivityMultiCell.prototype.__render = function() { };

ActivityMultiCell.prototype.getHElem = function() {
	if (this.__hElem == null) {
		this.__render();
	}
	return this.__hElem;
};

ActivityMultiCell.prototype.dispose = function() {
	this.nodes = null;
	this.fbNode = null;
	this.__hElem = null;
};


ActivityActionCell.prototype = new ActivityMultiCell();
ActivityActionCell.prototype.constructor = ActivityActionCell;

function ActivityActionCell(nodes, fbNode, cssClass, item) {
	this.item = item;
    this.__initVal(nodes, fbNode, cssClass);
}

ActivityActionCell.prototype.__render = function() {
	this.__hElem = makeElement(null, "div", this.__css);
	var count = 0;
	if (this.nodes != null) {
		var isAssigned = (this.item.model.mode == "assigned");
		 
		for (var ii = 0; ii < this.nodes.length; ii++) {
			var state = this.nodes[ii];
			if (isAssigned && state.getAttribute("subState") != "ENTER_SWITCH" 
					&& UserTree.getSelfId() != state.getAttribute("assignId")) {
				continue;
			}
			if (count > 0 && state.getAttribute("subState") == "ENTER_END") continue;
			
			var stepId = state.getAttribute("stepId");
			if (stepId.indexOf("/$$") > 0) {
				continue;
			}
			var stEl = makeElement(this.__hElem, "div", (count > 0 ? "multi" : "first"), stepId);
			count++;
			stEl._item = this.item;
			stEl._step = stepId;
			setEventHandler(stEl, "onclick", ActivityActionCell.__click);
		}
	}
	if (count < 1) {
		setElementText(this.__hElem, "-");
	}
};

ActivityActionCell.__click = function(evt) {
	ActivityTree.setActivityItem(this._item, this._step);
};

ActivityUserCell.prototype = new ActivityMultiCell();
ActivityUserCell.prototype.constructor = ActivityUserCell;

function ActivityUserCell(nodes, fbNode, cssClass, item, mode) {
	this.item = item;
	this.mode = mode;
	this.__initVal(nodes, fbNode, cssClass);
}

ActivityUserCell.prototype.__render = function() {	
	if (this.mode == "assigned") {
		var usrId = this.fbNode ? this.fbNode.getAttribute("submitId") : null;
		this.__hElem = makeElement(null, "div", this.__css, usrId ? UserTree.getUserName(usrId) : "\u00a0");
	} else {
		if (this.nodes == null) {
			this.__hElem = makeElementNbSpd(null, "div", this.__css);
			return;
		} else {
			this.__hElem = makeElement(null, "div", this.__css);
		}
		for (var ii = 0; ii < this.nodes.length; ii++) {
			var state = this.nodes[ii];
			var usrId =  state.getAttribute("assignId");
			if (ii > 0 && state.getAttribute("subState") == "ENTER_END") continue;
			makeElement(this.__hElem, "div", (ii > 0 ? "multi" : "first"), usrId ? UserTree.getUserName(usrId) : "\u00a0");
		}
	}
};


function ActivityPlanning() {
	this.updates = {};
	this.dirty = false;
}

ActivityPlanning.prototype.setDates = function(stepId, startDate, dueDate) {
	var upObj = this.__getUpObj(stepId);
	if (dueDate) upObj.dueDate = dueDate;
	if (startDate) upObj.startDate = startDate;
	upObj.dateDirty = true;
	this.dirty = true;
};
	
ActivityPlanning.prototype.setAssign = function(stepId, userId) {
	var upObj = this.__getUpObj(stepId);
	upObj.assign = userId;
	upObj.assignDirty = true;
	this.dirty = true;
};
	
ActivityPlanning.prototype.saveToDoc = function(xmlDoc) {
	if (this.dirty) {
		var pups = Xml.element(xmlDoc.documentElement, "plan-updates");
		for (var stepId in this.updates) {	
			var upObj = this.updates[stepId];
			var elem = Xml.element(pups, "update");
			elem.setAttribute("step", stepId);
			if (upObj.assignDirty) elem.setAttribute("assign", upObj.assign);
			if (upObj.dateDirty) {
				elem.setAttribute("startDate", 
					upObj.startDate ? DateUtil.to8601(upObj.startDate, false) : '');
				elem.setAttribute("dueDate", 
					upObj.dueDate ? DateUtil.to8601(upObj.dueDate, false) : '');
			}
		}
	}
};
	
ActivityPlanning.prototype.clearUpdates = function() {
	this.updates = {};
	this.dirty = false;
};
	
ActivityPlanning.prototype.__getUpObj = function(stepId) {
	var upObj = this.updates[stepId];
	if (!upObj) {
		upObj = this.updates[stepId] = {};
	}
	return upObj;
};


/**
 * XForm plugin
 */
if (typeof(XFTypeFormat) != "undefined") {

function ActivityXFTypeFmt(){ }

ActivityXFTypeFmt.prototype = new XFTypeFormat();
ActivityXFTypeFmt.prototype.constructor = ActivityXFTypeFmt;


ActivityXFTypeFmt.prototype.format = function(str, ctrl) {
	if (ctrl._actElem && ctrl._actElem.parentNode) 
		ctrl._actElem.parentNode.removeChild(ctrl._actElem);
	if (str == "") {
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "actEmpty", "<Drop an activity here>");
	} else {
		ctrl._actElem = ActivityTree.renderActivityInfo(str, ctrl.__hElem);
	}
	if (!this.__dnd) {
		this.__dnd = dndGetCanvas(document.body);
		this.__dndType = new ActivityXFDNDHand(this);
		this.__dnd.addDNDType(this.__dndType);
	}
	ctrl._actElem._ctrl = ctrl;
    this.__dnd.makeDropTarget(ctrl._actElem, this.__dndType.type);
    return str;
};

ActivityXFTypeFmt.prototype.parse = function(str, ctrl) {
    return str;
};

ActivityXFTypeFmt.prototype.decorate = function(uiElem, ctrl) {
	if (ctrl.constructor === XFControlInput) {
		ctrl.__hWidget.style.display = 'none';
	}
};

ActivityXFTypeFmt.prototype.disposeDecor = function(uiElem, ctrl) {
	if (this.__dnd && ctrl._actElem) {
		this.__dnd.disposeDropTarget(ctrl._actElem);
	}
	ctrl._actElem = null;
};

xsdTypes.itensilXF["activity"] = { mapped : XSD_MAPPED_TYPE.STRING };

XFTypeFormat.addFormat(XFORM_ITENSIL_NAMESPACE, "activity", new ActivityXFTypeFmt());

function ActivityXFDNDHand(view) {
    this.type = "dndXFActivity";
}

ActivityXFDNDHand.prototype = new DNDTypeHandler()
ActivityXFDNDHand.prototype.constructor = ActivityXFDNDHand;

ActivityXFDNDHand.prototype.canDrag = function(dragElem) {
    return false;
};

ActivityXFDNDHand.prototype.dropTest = function(dropElem, dragElem) {
    // activity drop
    var type = dragElem._dndType.type;
    if (type == "dndWFActivity") {
        return true;
    }
    return false;
};

ActivityXFDNDHand.prototype.dropExec = function(dropElem, dragElem) {
    // activity drop
    var dragItem = dragElem._actElem.__item;
    dropElem._ctrl.setValue(dragItem.node.getAttribute("id"));
};

}
/**
 * End XForm plugin
 */


FlowTreeModel.prototype = new TreeModel();
FlowTreeModel.prototype.constructor = FlowTreeModel;

function FlowTreeModel(uri) {
	TreeModel.apply(this, []);
	this.asyncLoader = true;
	this.uri = uri;
}

FlowTreeModel.prototype.onReady = function(callback, tree, itemParent) {
	if (itemParent === this) {
		var holdThis = this;
        ActivityTree.xb.loadURIAsync(ActivityTree.serviceUri + "processList?uri=" + Uri.escape(itemParent.uri),
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                  	var list = Xml.match(doc.documentElement, "node");
                    var tItm;
                    for (var ii = 0; ii < list.length; ii++) {
                        var nod = list[ii];
                        var name = nod.getAttribute("uri");
                        // unix hide
                        if (name.substring(0,1) == ".") continue;
                        
                        var tUri = Uri.absolute(itemParent.uri, name);
                        var style = nod.getAttribute("style");
                        var icon = style ? style.substring(5) : "def";
                        tItm = new TreeItem(holdThis, name, false, icon + "Ico");
               			tItm.act = tItm.optAct = treeMenuAction;
               			tItm.uri = tUri;
               			itemParent.add(tItm);
                    }
                    if (itemParent.kids.length < 1) {
                    	 tItm = new TreeItem(holdThis, "<empty>", false);
						 tItm.icon = "blankIco";
						 tItm.cssClass = "empty";
						 tItm.uri = "";
						 itemParent.add(tItm);
                    }
                    callback.apply(tree, [itemParent.kids, itemParent]);
                }
            });
	}
};

ProjectTreeModel.prototype = new TreeGridModel();
ProjectTreeModel.prototype.constructor = ProjectTreeModel;

function ProjectTreeModel(project, showFull) {
	TreeGridModel.apply(this, []);
	this.showFull = showFull;
	this.proj = project;
	this.itemIndex = new Object();
	this._custCols = null;
	this.reloadDoc = this.loadProject();
	if (showFull) {
		this.nCols = 5 + this.getCustColumns().length;
	} else {
		this.nCols = 4;
	}
	this.total = 0;
	this.filtCount = 0;
}

ProjectTreeModel.opClick = function() {
 	var panel = this._panel;
 	removeElementChildren(panel.contentElement);
 	var ps = panel.getPanelSet();
 	if (this._showFull) {
	 	ps.setMinorWidth(510);
 	} else {
 		var rect = ps.getParentBounds();
	 	ps.setMinorWidth(rect.w - 30);
 	}
 	ActivityTree.renderProject(this._treeMod.proj, panel.contentElement, !this._showFull);
};


ProjectTreeModel.colClick = function(evt) {
	if (ProjectTreeModel.colDiag && ProjectTreeModel.colDiag.isShowing()) return;
	var confUri = Uri.absolute(Uri.absolute("../fil/home/project", this._treeMod.proj), "config.xml");
	XFModel.clearLoadCache();
	var diag = ProjectTreeModel.colDiag = xfDialog("Project Columns",
            true, document.body, "../view-proj/config.xfrm", ActivityTree.xb, null,
            confUri, confUri, true, App ? App.chromeHelp : null);
    var holdThis = this;
    var xfmod = diag.xform.getDefaultModel();
    xfmod.setInstanceId("dat", Xml.matchOne(holdThis._treeMod.doc.documentElement, "dataColumns"));
    xfmod.rebuild();
    xfmod.addEventListener("xforms-close", { handleEvent : function() {
    		ProjectTreeModel.colDiag = null;
    		var panel = holdThis._panel;
 			removeElementChildren(panel.contentElement);
            ActivityTree.renderProject(holdThis._treeMod.proj, panel.contentElement, true);
        }});
    var xp = getMouseX(evt) - 400;
    diag.show(xp > 0 ? xp : 0, getMouseY(evt));
};

ProjectTreeModel.prototype.loadProject = function() {
	var loadUri = ActivityTree.projectSvcUri + "activityList?proj=" + Uri.escape(this.proj) + (this.showFull ? "&full=1" : "");
    this.doc = ActivityTree.xb.loadURI(loadUri);
    this.projectId = this.doc.documentElement.getAttribute("id");
    return this.doc;
};

ProjectTreeModel.prototype.getCustColumns = function() {
	if (this._custCols) return this._custCols; 
	// try to load col config
	var confDoc = ActivityTree.xb.loadURI(
		Uri.absolute(Uri.absolute("../fil/home/project", this.proj), "config.xml"));
	
	this._custCols = []; 
	var dcs = Xml.matchOne(this.doc.documentElement, "dataColumns");
	var cols = Xml.match(dcs, "column");
	var ii;
	if (confDoc && !App.checkError(confDoc, true)) {
		var vwEl = Xml.matchOne(confDoc.documentElement, "view");
		var vCols = vwEl ? Xml.match(vwEl, "column") : [];
		for (ii=0; ii < vCols.length; ii++) {
			var matNam = vCols[ii].getAttribute("name");
			for (var jj = 0; jj < cols.length; jj++) {
				var cNd = cols[jj];
				if (cNd.getAttribute("name") == matNam) {
					var co = { name: cNd.getAttribute("name"), type:  cNd.getAttribute("type"), idx:jj, node : cNd};
					this._custCols.push(co);
					break;
				}
				
			}
		}
	} else {	
		for (ii=0; ii < cols.length; ii++) {
			var co = { name: cols[ii].getAttribute("name"), type: cols[ii].getAttribute("type"), idx:ii, node : cols[ii]};
			this._custCols.push(co);
		}
	}
	var holdThis = this;
	for (ii=0; ii < this._custCols.length; ii++) {
		var co = this._custCols[ii];
		if (co.type == "xsd:NMTOKEN" || co.type == "xsd:NMTOKENS") {
			var cb = new ComboBox();
			cb.addOption(co.name + " (all)", "", true);
			var itms = Xml.match(co.node, "item");
			co.valMap = {};
			for (var jj = 0; jj < itms.length; jj++) {
				var iLab = itms[jj].getAttribute("label")
				var iVal = itms[jj].getAttribute("value");
				co.valMap[iVal] = iLab;
				cb.addOption(iLab, iVal);
				cb.onchange = function() {
					holdThis.reloadDoc = holdThis.doc;
					holdThis._tree.redrawAll();
				};
			}
			co.filter = cb;
		}
		delete co.node;
	}
	return this._custCols;
};

ProjectTreeModel.prototype.onReady = function(callback, tree, itemParent) {
	if (itemParent === this && this.reloadDoc == null) this.reloadDoc = this.loadProject();
	this.digest(this.doc, callback, tree, itemParent);
};

ProjectTreeModel.prototype.digest = function(doc, callback, tree, itemParent) {	
	var list = itemParent === this ? Xml.match(doc.documentElement, "activity") : [];
    var toggles = [];
    var actItm = null;
    var ii;        
    if (list.length > 0) {

        if (itemParent === this) {
        	this.filtCount = 0;
        	
            // pass-1 index
            for (ii = 0; ii < list.length; ii++) {
                var nod = list[ii];
                var tItm = new ProjectTreeItem(this, nod, false, nod.getAttribute("icon") + "Ico");
                this.itemIndex[tItm.getId()] = tItm;
                list[ii] = tItm;
            }
            
            this.total = list.length;
	

            // pass-2 nest
            for (ii = 0; ii < list.length; ii++) {
            	var tItm = list[ii];

                //do not include activity which have all ended state
                // check for un ended states
                var state = Xml.matchDeep(tItm.node, "state");
                var endState = true;
                
                for(var iii=0; iii < state.length; iii++ ){
            	    var subState = state[iii].getAttribute("subState");
            	    if(subState != "ENTER_END") {
            	    	endState = false;
            	    }
                }
                //don't include ended activities
        	    if(endState) {
        	    	continue;
        	    }

                if (!this.showFull || tItm.filtersPass(this._custCols)) {
	                var pid = tItm.node.getAttribute("parent");
	                if (pid) {
	                    var par = this.itemIndex[pid];
	                    if (par && (!this.showFull || par.filtersPass(this._custCols))) {
	                        par.preloaded = true;
	                        par.allowsKids = true;
	                        this.filtCount++;
	                        par.add(tItm);
	                        if (arrayFindStrict(toggles, par) < 0) toggles.push(par);
	                        // TODO add more button...
	                    } else {
	                        // TODO else parent decoration
	                        this.filtCount++;
	                        itemParent.add(tItm);
	                    }
	                } else {
            	    	this.filtCount++;
            	    	itemParent.add(tItm);
	                }
                }
            }
        }
        
        if (this.projCountElem) 
        	setElementText(this.projCountElem, this.filtCount + "/" + this.total + " activities");
        
    } else if (itemParent === this) {
    	var blank = new ProjectTreeItem(this, null, false, "blankIco");
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
    
    
    this.reloadDoc = null;
};

ProjectTreeModel.prototype.getId = function() { return ""; };


ProjectTreeItem.prototype = new ActivityTreeItem();
ProjectTreeItem.prototype.constructor = ProjectTreeItem;

function ProjectTreeItem(model, node, allowsKids, icon) {
    TreeGridItem.apply(this, [model, null, allowsKids, icon]);
    this.rowCssClass = "norm";
    this.node = node;
    
    if (node) {
	    this.states = Xml.match(node, "state");
	    this.plans = Xml.matchDeep(node, "current");
	    this.tip = node.getAttribute("flowName");
	    if (node.getAttribute("variationId")) this.tip = "Variation of: " + this.tip;
    }
    
    this.cells.push(new ActivityActionCell(this.states, node, "step", this));

    var dueDate = this.getDateText();
    // if due date not specified get rollup value from latest finish date of task
    if(!dueDate) {
    	dueDate = getActRollUpDate(node);
    	if(dueDate) {
    		dueDate = DateUtil.toLocaleWords(dueDate);
    	}
    }
    this.cells.push(new GridCell(dueDate, "due"));
    //this.cells.push(new GridCell("", "priority"));
    if (!App.edu) {
    	//if (model.showFull) this.cells.push(new ActivityUserCell(this.states, node, "user", this, "assigned"));
   	 	this.cells.push(new ActivityUserCell(this.states, node, "user", this));
    }
    //this.cells.push(new GridCell("", "status"));
    var ii;
    if (model.showFull) {
	    this.data = node ? Xml.matchOne(node, "data") : null;

	    //var vns = this.data ? Xml.match(this.data, "*") : [];
	   	var custs = model.getCustColumns();
	    for (ii = 0; ii < custs.length; ii++) {
			var co = custs[ii];
	    	var valnd = this.data ? Xml.matchOne(this.data, "val" + co.idx) : null;
	    	if (valnd == null) {
	    		this.cells.push(new GridCell("", ""));
	    		continue;
	    	}
	    	var val  = Xml.stringForNode(valnd);
	    	if (co.type == "ix:file") {
	    		if (val != '' && val.charAt(0) != '/') {
	    			var actInf = ActivityTree.activityInfo(this.getId(), true);
	    			val = Uri.absolute(actInf.getAttribute("uri"), val);
	    		}
	    		this.cells.push(new ProjFileCell(val));
	    	} else if (co.type == "ix:activity") {
	    		this.cells.push(new ProjActCell(val));
	    	} else {
	    		this.cells.push(new ProjCell(val, co));
	    	}
	    }
    }
    for (ii = 0; ii < this.cells.length; ii++) {
        this.cells[ii].__edit = false;
    }
}

ProjectTreeItem.prototype.filtersPass = function(custCols) {
	for (var ii = 0; ii < custCols.length; ii++) {
		var co = custCols[ii];
		if (co.filter) {
			var fval = co.filter.getValue();
			if (fval) {
				var valnd = this.data ? Xml.matchOne(this.data, "val" + co.idx) : null;
		    	if (valnd == null) {
		    		return false;
		    	}
		    	var val = Xml.stringForNode(valnd);
				if (co.type == "xsd:NMTOKENS") {
					var vals = val.split(/[\x0d\x0a\x09\x20]+/)
					if (arrayFind(vals, fval) < 0) return false;
				} else {
					if (val != fval) return false;
				}
			}
		}
	}
	return true;
};

ProjectTreeItem.prototype.act = treeMenuAction;
ProjectTreeItem.prototype.editAct = null;
ProjectTreeItem.prototype.optAct = treeMenuAction;

function ProjCell(value, co) {
	this.type = co.type;
	var fmt = XFTypeFormat.getFormatByQName(co.type);
	switch (co.type) {
		case "ix:percent":
		case "ix:currencyUSD":
		case "xsd:float":
		case "xsd:date":
		case "xsd:dateTime":
			this.__css = "projNum"; break;
		
		case "xsd:NMTOKEN":
			value = co.valMap[value];
			break;
			
		case "xsd:NMTOKENS":
			var vals = value.split(/[\x0d\x0a\x09\x20]+/);
			value = "";
			for (var ii=0; ii < vals.length; ii++) {
				value += co.valMap[vals[ii]] + "\n";
			}
			break;
	
		case "xsd:boolean":
			if (value == "1") value = "Yes"; else value = "No";
			break;
	}
	this.setValue(fmt.format(value));
}

ProjCell.prototype = new GridCell();
ProjCell.prototype.constructor = ProjCell;


function ProjFileCell(uri) {
	this.uri = uri;
	this.__hElem = null;
}

ProjFileCell.prototype = new ActivityMultiCell();
ProjFileCell.prototype.constructor = ProjFileCell;

ProjFileCell.prototype.__render = function() { 
	if (this.uri) {
		this.__hElem = makeElement(null, "div", "file");
		makeElementNbSpd(this.__hElem, "span", "icon " + Uri.ext(this.uri).toLowerCase() + "_Ico");
		makeElement(this.__hElem, "span", "name", Uri.name(this.uri));
		var holdThis = this;
		setEventHandler(this.__hElem, "onclick",
	        function(evt) {
	            FileTree.view(evt, {uri:holdThis.uri});
	            return false;
	        });
	        
	} else {
		this.__hElem = makeElementNbSpd(null, "div");
	}
};

function ProjActCell(id) {
	this.id = id;
	this.__hElem = null;
}

ProjActCell.prototype = new ActivityMultiCell();
ProjActCell.prototype.constructor = ProjActCell;

ProjActCell.prototype.__render = function() {
	if (this.id) {
		this.__hElem = makeElement(null, "div", "projInfo");
		ActivityTree.renderActivityInfo(this.id, this.__hElem);
	} else {
		this.__hElem = makeElementNbSpd(null, "div");
	}
};


/**
 *  LMS (Learning) Support
 * 
 * 
 **/
 window.API = {
	
	values : {},
	
	LMSInitialize : function(param) {
		//if (SH.is_gecko) console.log("LMSInitialize:" + param);
		return "true";
	},
	
	LMSFinish : function (param) {
		//if (SH.is_gecko) console.log("LMSFinish:" + param);
		return "true";
	},

	LMSGetValue : function(name) {
		//if (SH.is_gecko) console.log("LMSGetValue:" + name);
		var val = this.values[name];
		return val || "";
	},
	
	LMSSetValue : function(name, val) {
		//if (SH.is_gecko) console.log("LMSSetValue:" + name + "=" + val);
		this.values[name] = val;
		return "true";
	},
	
	LMSCommit : function (param) {
		//if (SH.is_gecko) console.log("LMSCommit:" + name);
		return "true";
	},
	
	LMSGetLastError : function() {
		return 0;
	},
	
	LMSGetErrorString : function(errCode) {
		return "";
	},
	
	LMSGetDiagnostic : function(errCode) {
		return "";
	}
};
