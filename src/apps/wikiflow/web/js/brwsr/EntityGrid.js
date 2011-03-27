
function EntityGrid(uiParent, modelUri, name, config) {
	var headerSet = {"EntityURI" : null, "EntityID" : null};
	this.modDoc = Data.xb.loadURI("../entity" + modelUri, headerSet);
	this.modelUri = modelUri;
	this.config = config || {};
	this.uiParent = uiParent;
	if (App.checkError(this.modDoc, true)) {
		this.loadError = App.lastError;
		return;
	}
	if (name) {
		this.name = name;
		this.modelUri = Uri.absolute(headerSet["EntityURI"], "model.entity");
	} else {
		this.modelUri = Uri.absolute(headerSet["EntityURI"], "model.entity");
		this.name = Uri.name(headerSet["EntityURI"]);
	}
}

EntityGrid.forPanel = function(panel, modelUri, name, config) {
	var eg = new EntityGrid(panel.contentElement, modelUri, name, config);
	eg.panel = panel;
	eg.render();
	panel.linkResize(eg.grid);
	return eg;
};

EntityGrid.dialog = function(modelUri, name, config) {
	var eg = new EntityGrid(null, modelUri, name, config);
	var diag = new Dialog("Entities: " + eg.name, true);
	diag.render(document.body);
	eg.dialog = diag;
	eg.uiParent = diag.contentElement;
	eg.render();
	diag.addDisposable(eg);
	diag.linkResize(eg.grid);
	return diag;
};


/// XForm xpath feature
EntityGrid.__xpEntCache = {};

EntityGrid.xpFuncEntity = function(/* string, string */) {
	var c = arguments[0];
	if (arguments.length != 3) {
		throw new Error("Function entity expects (string type, string id)");
	}
	var ename = arguments[1].evaluate(c).stringValue();
	var rid = arguments[2].evaluate(c).stringValue();
	var ckey = ename + "|" + rid;
	var dataUri = EntityGrid.__xpEntCache[ckey];
	if (!dataUri) {
	   	var recDoc = EntityGridModel.loadIdSetRecords(ename, [rid]);
	    var recNode = recDoc ? Xml.matchOne(recDoc.documentElement, "record") : null;
	    var entityUri = recDoc ? recDoc.documentElement.getAttribute("entityUri") : "";
	    	
	    dataUri = recNode ? Uri.absolute(entityUri, "records/" + recNode.getAttribute("uri") + "/data.xml") : null;
	    
	    EntityGrid.__xpEntCache[ckey] = dataUri;
	}
    
	var ns = new XNodeSet();
	if (!dataUri) {
		return ns;
	}
	
	var idoc = XFModel.__loadSharedSrc("../fil" + dataUri, Data.xb);
	
	if (idoc) ns.add(idoc.documentElement);
	return ns;
};

XForm.addXpathFunc("entity", EntityGrid.xpFuncEntity);
///

EntityGrid.embed = function(uiParent, modelUri, name, config) {

    var eg = new EntityGrid(uiParent, modelUri, name, config);
    var ak = config.activityKeys;

    // if 1orMore render grid
    if (config.relation == "1orMore" || !config.formInfo) {
    	var isAdd = (config.relation == "1orMore");
    	config.removeMenuItem = {
    		label:"Leave Relation",
    		icon:"mb_outIco",
    		act: function(evt, context) {
    			var cell0 = context.cell0;
				cell0.grid.model.doQuickLock();
				var resDoc;
				if (eg.config.leaveFunc)
					eg.config.leaveFunc(cell0.xNode.getAttribute("id"), cell0.setXNode);
				
				if (ak) {
					resDoc = Data.xb.loadURI("../entity/leaveRecord?entity=" + Uri.escape(eg.name) +
							"&activity=" + Uri.escape(ak.activity) + "&relName=" + Uri.escape(ak.relName) +
							"&recordId=" + cell0.xNode.getAttribute("id"));
				}
				if (eg.config.leaveFunc || !App.checkError(resDoc)) {
					cell0.grid.model.refresh();
					cell0.grid.renderReplace(0, eg.grid.model.rows.length);
				}
				if (!isAdd) {
					var btns = Xml.nextSiblingElement(cell0.grid.__hElem);
					btns.style.display = "";
				}
    		}
    	};
    	
    	if (config.designMode) {
    		makeElement(uiParent, "div", "soon", "For display only in design mode.");
    	}
    	posRelIEFix(uiParent);
    	eg.render();
    	if (!config.designMode) {
    		// include add new or exsiting?
    		EntityGrid.embedRenderButtons(uiParent, isAdd, config.condition, eg, ak);
    	}
    // else (1)
    } else {
    	var recNode;
    	if (!config.designMode) {
    		var recDoc = config.activityKeys ? EntityGridModel.loadActRecords(eg.name, config.activityKeys)
    				: EntityGridModel.loadIdSetRecords(eg.name, config.idSet);
    	 	var recNode = recDoc ? Xml.matchOne(recDoc.documentElement, "record") : null;
    	}
    	// if filled or template?
    	config.formInfo.formUri = Uri.absolute(Uri.parent(eg.modelUri), config.form);
    	if (recNode || config.designMode) {
    		
    		// render form
    		
    		var dataUri = !config.designMode ? ("records/" + recNode.getAttribute("uri") + "/data.xml") : null;
    		if (config.designMode) makeElement(uiParent, "div", "soon", "For display only in design mode:");
    		var uriRes = new AppUriResolver(".");
    		if (dataUri) uriRes.recUri = Uri.parent(dataUri);
    		config.formInfo.renderFunc(config.formInfo, dataUri, uriRes);
    	// else (pick one)
    	} else {
    		// set new or exsiting?
    		makeElement(uiParent, "div", "soon", "Relationship '" + ak.relName + "' is currently empty.");
    		EntityGrid.embedRenderButtons(uiParent, false, config.condition, eg, ak, config.formInfo);
    	}
    }
    uiParent = null;
    return eg;
};


EntityGrid.prototype.createNew = function() {
	var resDoc = Data.xb.loadURI("createRecord?entity=" + Uri.escape(this.name));
	if (!App.checkError(resDoc)) {
		var uri = resDoc.documentElement.getAttribute("result");
		this.grid.model.refresh();
		this.grid.renderReplace(0, this.grid.model.rows.length);
	}
};

EntityGrid.prototype.render = function() {
	if (this.loadError) {
		makeElement(this.uiParent, "div", "entityErr", this.loadError);
		return;
	}
	
	var dat = Xml.matchOne(this.modDoc.documentElement, "data");
	var a1 = Xml.matchDeep(dat, "attr");
	this.attrs = a1.concat(Xml.match(dat, "entity"));
	
	this.grid = new Grid(new EntityGridModel(this));
	if (this.config.idSet) {
		this.grid.model.setIdSet(this.config.idSet);
	} else if (this.config.activityKeys) {
		this.grid.model.activityKeys = this.config.activityKeys;
	}
	this.grid.addHeader("ID\u00a0\u00a0");
	
	var heads = [];
	var ii;
	for (ii = 0; ii < this.attrs.length; ii++) {
		var bidx = parseInt(this.attrs[ii].getAttribute("browse"));
		if (bidx > 0) {
			heads[bidx] = this.attrs[ii];
		}
	}
	this.colInfs = [];
	var holdThis = this;
	for (ii = 0; ii < heads.length; ii++) {
		if (heads[ii]) {
			var label = heads[ii].getAttribute("label") || heads[ii].getAttribute("name");
			this.grid.model.nCols++;
			var cb = null;
			var co = { type : heads[ii].getAttribute("type") };
			this.colInfs[ii] = co;
			if (co.type == "xsd:NMTOKEN" || co.type == "xsd:NMTOKENS") {
				cb = new ComboBox();
				cb.addOption(label + " (all)", "", true);
				var itms = Xml.match(heads[ii], "item");
				co.valMap = {};
				for (var jj = 0; jj < itms.length; jj++) {
					var iLab = itms[jj].getAttribute("label")
					var iVal = itms[jj].getAttribute("value");
					co.valMap[iVal] = iLab;
					cb.addOption(iLab, iVal);
					cb.onchange = function() {
						holdThis.grid.model.doQuickLock();
						holdThis.grid.model.filter();
						holdThis.grid.renderReplace(0, holdThis.grid.model.rows.length);
					};
				}
				co.filter = cb;
			}
			this.grid.addHeader(label, null, cb);
		}
	}
	if (this.config.extColCells) {
		for (ii = 0; ii < this.config.extColCells.length; ii++) {
			this.grid.model.nCols++;
			this.grid.addHeader(this.config.extColCells[ii].head);
		}
	}
	
	this.grid.render(this.uiParent, "entityGrid");
	setEventHandler(this.grid.__hElem, "oncontextmenu", function(evt) { return stopEvent(evt); });
};

EntityGrid.prototype.dispose = function() {
	if (this.grid) { 
		this.grid.dispose();
		this.grid = null;
	}
	this.modDoc = null;
	this.attrs = null;
};

EntityGrid.embedRenderButtons = function(uiParent, isAdd, condition, eg, activityKeys, formInfo) {
    var tbodyObj = App.disposableNode(makeLayoutTable(uiParent, "entButtons"));
    if (!formInfo && !isAdd && eg.grid.model.rows.length > 0) {
    	tbodyObj.node.parentNode.style.display = "none";
    }
	var tr = makeElement(tbodyObj.node, "tr");
	if (condition == "new" || condition == "newOrExisting") {
		
		/////
		// Create record method
		eg.createRecord = function() {
			var ak = activityKeys;
			var servUri;
			if (!ak) {
				servUri = "../entity/createRecord?entity=" + Uri.escape(eg.name);
			} else {
				servUri = "../entity/createRecord?entity=" + Uri.escape(eg.name) +
					"&activity=" + Uri.escape(ak.activity) + "&relName=" + Uri.escape(ak.relName);
			}
			var resDoc = Data.xb.loadURI(servUri);
			if (!App.checkError(resDoc)) {
				var uri = resDoc.documentElement.getAttribute("result");
				if (!ak && eg.config.joinFunc) eg.config.joinFunc(parseInt(Uri.name(uri), 10));
				if (isAdd || !formInfo) {
					eg.grid.model.refresh();
					eg.grid.renderReplace(0, eg.grid.model.rows.length);
					if (!isAdd) {
						tbodyObj.node.parentNode.style.display = "none";
					}
				} else {
					if (formInfo.entDiv) removeElementChildren(formInfo.entDiv);
					formInfo.renderFunc(formInfo, 
						Uri.localize(Uri.parent(formInfo.formUri), Uri.absolute(uri, "data.xml")));
				}
				if (ak && eg.config.joinFunc) eg.config.joinFunc(parseInt(Uri.name(uri), 10));
				return true;
			}
			return false;
		};
		
    	var newLink = makeElement(makeElement(tr, "td"), "div", "minorBtn", noBreakString(isAdd ? "Add New Information" : "Enter New Information"))
		makeElementNbSpd(newLink, "div", "mbIco mb_newIco");
		setEventHandler(newLink, "onclick", 
				function() {
					eg.createRecord();
				});
		newLink = null;
	}
	
	
	
	if (condition == "existing" || condition == "newOrExisting") {
		
		/////
		// Join record method
		eg.joinRecord = function(recId, recUri) {
			var ak = activityKeys;
			var resDoc;
			if (ak) {
				resDoc = Data.xb.loadURI("../entity/joinRecord?entity=" + Uri.escape(eg.name) +
					"&activity=" + Uri.escape(ak.activity) + "&relName=" + Uri.escape(ak.relName) +
					"&recordId=" + recId);
			}
			if (eg.config.joinFunc || !App.checkError(resDoc)) {
				if (!ak && eg.config.joinFunc) {
					eg.config.joinFunc(recId);
				}
				if (isAdd || !formInfo) {
					eg.grid.model.doQuickLock();
					eg.grid.model.refresh();
					eg.grid.renderReplace(0, eg.grid.model.rows.length);
					if (!isAdd) {
						tbodyObj.node.parentNode.style.display = "none";
					}
				} else if (recUri) {
					var datUri = "records/" + recUri + "/data.xml";
					if (formInfo.entDiv) removeElementChildren(formInfo.entDiv);
					formInfo.renderFunc(formInfo, datUri);
				}
				
				
				if (ak && eg.config.joinFunc) {
					eg.config.joinFunc(recId);
				}
				return true;
			}
			return false;
		};
		
    	var estLink = makeElement(makeElement(tr, "td"), "div", "minorBtn", noBreakString(isAdd ? "Choose Existing Information" : "Edit Existing Information"))
		makeElementNbSpd(estLink, "div", "mbIco mb_refIco");
		setEventHandler(estLink, "onclick", 
				function(evt) {
					var diag = EntityGrid.dialog(eg.modelUri, eg.name, {
						selectMenuItem : {label : "Select This", icon: "mb_movIco", act: function(evt, context) {
							var cell0 = context.cell0;
							if (eg.joinRecord(cell0.xNode.getAttribute("id"), cell0.xNode.getAttribute("uri"))) {
								diag.destroy();
							}
						}}});
					diag.show(getMouseX(evt) - 100, getMouseY(evt) - 100, null, 400, 300);
				});
		estLink = null;
	}
	tr = null; uiParent = null;
};



EntityGridModel.prototype = new GridModel();
EntityGridModel.prototype.constructor = EntityGridModel;

function EntityGridModel(egrid) {
	this.egrid = egrid;
	this.nCols = 1;
	this.quickLock = false;
	this.idSet = null;
	this.activityKeys = null;
}

EntityGridModel.prototype.setIdSet = function(idSet) {
	this.idSet = idSet;
};
EntityGridModel.loadCount = 0;

EntityGridModel.loadActRecords = function(entName, activityKeys) {
	// is there a live activity?
	if (!activityKeys.activity) return null;
		
	return Data.xb.loadURI("../entity/recordsFind?entity=" + Uri.escape(entName) +
			 "&activity=" + Uri.escape(activityKeys.activity) + 
			 "&relName=" + Uri.escape(activityKeys.relName) +
			 "&" + (EntityGridModel.loadCount++)) ;
};

EntityGridModel.loadIdSetRecords = function(entName, idSet) {
	if (!idSet || idSet.length == 0) return null;
	return Data.xb.loadURI("../entity/recordsFind?entity=" + Uri.escape(entName) +
			 "&id=" + idSet.join("&id="));
}

EntityGridModel.prototype.loadRows = function() {
	this.rows = [];
	var recDoc, recs, recsUri;
	var ii;
	if (this.idSet) {
		if (this.idSet.length == 0) return;

		recDoc = EntityGridModel.loadIdSetRecords(this.egrid.name, this.idSet);
	
		if (this.idSet[0].constructor === EntIdPair) {
			var recMap = {};
			recs = Xml.match(recDoc.documentElement, "record");
			recsUri = Uri.absolute(recDoc.documentElement.getAttribute("entityUri"), "records");
			for (ii = 0; ii < recs.length; ii++) {
				recMap[recs[ii].getAttribute("id")] = Xml.match(recs[ii], "*");
			}
			var extColCells = this.egrid.config.extColCells;
			for (ii = this.idSet.length - 1; ii >= 0; ii--) {
				var idPair = this.idSet[ii];
				var cols = recMap[idPair.id];
				if (cols) {
					var row = [];
					var cell0 = new EGridCell0(this.egrid, idPair.id, null, true);
					cell0.xNode = cols[0].parentNode;
					cell0.recsUri = recsUri;
					var rUri = Uri.absolute(cell0.recsUri, cell0.xNode.getAttribute("uri"));
					cell0.setXNode = idPair.xNode;
					row.push(cell0);
					var jj;
					for (jj = 0; jj < cols.length; jj++) {
						var cNode = cols[jj];
						var co = this.egrid.colInfs[jj + 1];
						var gc = EntityGridModel.getCell(co, cNode, rUri);
						row.push(gc);
					}
					this.addEntRow(row);
					if (extColCells) {
						 for (jj = 0; jj < extColCells.length; jj++) {
						 	row.push(new extColCells[jj].cellConst(extColCells[jj].constArg, idPair, this.egrid));
						 }
					}
				}
			}
			return;
		}

	} else if (this.activityKeys) { 
		recDoc = EntityGridModel.loadActRecords(this.egrid.name, this.activityKeys);
		if (!recDoc) return;
	} else {
		// all
		recDoc = Data.xb.loadURI("../entity/recordList?entity=" + Uri.escape(this.egrid.name) + 
			"&" + (EntityGridModel.loadCount++));
	}
	
	if (App.checkError(recDoc)) {
		return;
	}
	 
	recs = Xml.match(recDoc.documentElement, "record");
	recsUri = Uri.absolute(recDoc.documentElement.getAttribute("entityUri"), "records");
	for (ii = 0; ii < recs.length; ii++) {
		var cols = Xml.match(recs[ii], "*");
		var row = [];
		var cell0 = new EGridCell0(this.egrid, recs[ii].getAttribute("id"), null, true);
		cell0.xNode = recs[ii];
		cell0.recsUri = recsUri;
		var rUri = Uri.absolute(cell0.recsUri, cell0.xNode.getAttribute("uri"));
		row.push(cell0);
		for (var jj = 0; jj < cols.length; jj++) {
			var cNode = cols[jj];
			var co = this.egrid.colInfs[jj + 1];
			var gc = EntityGridModel.getCell(co, cNode, rUri);
			row.push(gc);
		}
		this.addEntRow(row);
	}
	//GridCell
};

EntityGridModel.getCell = function(co, cNode, rUri) {
	var val = Xml.stringForNode(cNode);
	var gc;
	var cssClass = null;
	if (co) {
		if (co.valMap) {
		 	val = co.valMap[val];
		} else {
			switch (co.type) {
				case "ix:user": val = UserTree.getUserName(val); break;
				case "ix:userGroup": val = UserTree.getGroupName(val); break;
				case "ix:file":
					if (val) {
						gc = new GridCell();
						gc.uri = Uri.absolute(rUri, val);
						gc.__hElem = makeElement(null, "div", "file");
						makeElementNbSpd(gc.__hElem, "span", "icon " + Uri.ext(gc.uri).toLowerCase() + "_Ico");
						makeElement(gc.__hElem, "span", "name", Uri.name(gc.uri));
						setEventHandler(gc.__hElem, "onclick",
					        function(evt) {
					            FileTree.view(evt, {uri:gc.uri});
					            return false;
					        });
					 	setEventHandler(gc.__hElem, "onmouseup", stopEvent);
					}
					break;
				case "ix:http":
					if (val) {
						gc = new GridCell();
						gc.uri = val;
						gc.__hElem = makeElement(null, "div", "xftmpl");
						var gc_xfctrl = makeElement(gc.__hElem, "div", "xfctrl");
						var gc_link = makeElement(gc_xfctrl, "span", "attrlink");
						makeElement(gc_link, "text", "xftext", val);
						setEventHandler(gc_link, "onclick", 
	     			        function(evt) {
						     try {
								 window.open(val, "_blank");
 		   						} 
							catch(e) {
 									alert("Please turn off any pop-up blockers for this website, and try again.");
								}

					        return false;
					        });
					   	setEventHandler(gc_xfctrl, "onmouseup", stopEvent);
					   	gc_xfctrl = null;  gc_link = null; // IE enclosure clean-up
					}
					break;

					
				default:
					var xfFmt = XFTypeFormat.getFormatByQName(co.type);
					if (xfFmt) {
						val = xfFmt.format(val);
						cssClass = xfFmt.cssClass;
					}
					
			}
		}
	}
	if (!gc) gc = new GridCell(val, cssClass, true);
	gc.xNode = cNode;
	return gc;
};

EntityGridModel.prototype.onCellsReady = function(grid) {
	this.loadRows();
    grid.renderCells(this.rows.length, this.nCols);
};

EntityGridModel.prototype.refresh = function() {
	this.loadRows();
	this.unfiltRows = [];
	for (var uu=0; uu < this.rows.length; uu++)
		if (!this.rows[uu].fake) this.unfiltRows.push(this.rows[uu]);
	var colInfs = this.egrid.colInfs;
	for (var ii = 0; ii < colInfs.length; ii++) {
		var co = colInfs[ii];
		if (co && co.filter) {
			var fval = co.filter.setValue("", true, true);
		}
	}
};

EntityGridModel.prototype.filter = function() {
	if (!this.unfiltRows) {
		this.unfiltRows = [];
		for (var uu=0; uu < this.rows.length; uu++)
			if (!this.rows[uu].fake) this.unfiltRows.push(this.rows[uu]);
	}
	this.rows = [];
	var colInfs = this.egrid.colInfs;
	for (var rr = 0; rr < this.unfiltRows.length; rr++) {
		var pass = true;
		var rowArr = this.unfiltRows[rr];
		for (var ii = 0; ii < colInfs.length; ii++) {
			var co = colInfs[ii];
			if (co && co.filter) {
				var fval = co.filter.getValue();
				if (fval) {
					var cell = rowArr[ii];
					if (!cell && cell.xNode) {
						pass = false;
						break;
					}
			    	var val = Xml.stringForNode(cell.xNode);
					if (co.type == "xsd:NMTOKENS") {
						var vals = val.split(/[\x0d\x0a\x09\x20]+/)
						if (arrayFind(vals, fval) < 0) {
							 pass = false;
							 break;
						}
					} else {
						if (val != fval) {
							pass = false;
							break;
						}
					}
				}
			}
			
		}
		if (pass) this.addEntRow(rowArr);
	}
};

EntityGridModel.prototype.addEntRow = function(row) {
	 this.rows.push(row);
	 if (this.egrid.config.relActs) {
	 	var cell0 = row[0];
	 	var sgCell = new GridCell("Loading...", "entActRow");
	 	setEventHandler(sgCell.__hElem, "onmouseup", stopEvent);
	 	sgCell.colSpan = this.nCols;
	 	cell0.sgCell = sgCell;
	 	var frow = [sgCell];
	 	frow.fake = true;
	 	this.rows.push(frow);
	 }
};

EntityGridModel.prototype.getRowCssClass = function(ii) {
	 if (this.egrid.config.relActs && (ii % 2)) {
	 	return  "entActRow";
	 }
	 return null;
};

// an auto unlocking ui lock
EntityGridModel.prototype.doQuickLock = function () {
	this.quickLock = true;
	var holdThis = this;
	window.setTimeout(function() {
			holdThis.quickLock = false;
		}, 400);
};

EntityGridModel.removeMnAct = function(evt, context) {
	if (confirm("Are you sure?")) {
		var cell0 = context.cell0;
		var uri = Uri.absolute(cell0.recsUri, cell0.xNode.getAttribute("uri"));
        if (!App.checkError(Data.xb.loadURI(FileTree.serviceUri + "delete?uri=" + Uri.escape(uri)))) {
        	cell0.grid.model.refresh();
			cell0.grid.renderReplace(0, cell0.grid.model.rows.length);
        }
	}
};


EntityGridModel.viewEditAct = function(evt, context) {	
	var cell0 = context.cell0;
	cell0.grid.model.doQuickLock();
	var datUri = "records/" + cell0.xNode.getAttribute("uri") + "/data.xml";
	var frmUri = "../fil" + this.uri;
	var uriRes = new AppUriResolver(Uri.parent(frmUri));
    uriRes.recUri = Uri.parent(datUri);
	var diag = xfDialogConfig(this.label + " - " + cell0.xNode.getAttribute("id"), frmUri, Data.xb,
		{instanceSrc : datUri, submitAction: datUri, uriResolver : uriRes});
	var mod = diag.xform.getDefaultModel();
	
    mod.addEventListener("xforms-submit-done", { handleEvent : function() {
    		diag.destroy();
    		cell0.grid.model.refresh();
			cell0.grid.renderReplace(0, cell0.grid.model.rows.length);
    	}});
    diag.show(100, 100, null, 500, 300);
};

EntityGridModel.eventAct = function(evt, context) {	
	var cell0 = context.cell0;
	cell0.grid.model.doQuickLock();
	var eventName = this.label;
	Ephemeral.insideMessage(cell0.grid.__hElem , "Processing event " + eventName + "...");
	var recId = cell0.xNode.getAttribute("id");
	
	Data.xb.loadURIAsync("../entity/userEvent?event=" + Uri.escape(eventName) +
			"&entity=" + Uri.escape(cell0.grid.model.egrid.name) +
			"&recordId=" + recId, 
			function(xmlDoc) {
				
				if (!App.checkError(xmlDoc)) {
					var diag = new Dialog("Event " + eventName + " Results" , true);
					diag.render(document.body);
					var amod = new ActivityTreeModel("assigned", null);
					amod.idSetDoc = xmlDoc;
					var lastTree = ActivityTree.__tree;
					ActivityTree.__tree = ActivityTree.renderActivityTreeMod(amod, diag.contentElement);
					diag.showModal(100, 100, null, null, 400, 300);
					diag.onClose = function() {
						ActivityTree.__tree = lastTree;
					};
					xmlDoc = null; // IE enclosure clean-up
					App.addDispose(diag);
				}
				
			});
};

EntityGridModel.prototype.getMenu = function() {
	if (!this.entMenu) {
		var entFormMen = new Menu(new EntFormMenuModel(
			this.egrid.modDoc, Uri.parent(this.egrid.modelUri)), EntityGridModel.viewEditAct);
		var mopts = [];
		if (this.egrid.config.selectMenuItem) {
			mopts.push(this.egrid.config.selectMenuItem);
			mopts.push({isSep:true});
		}
		mopts.push({label:"View/Edit", icon:"mb_optIco", sub: entFormMen});
		mopts.push({label:"Events", icon:"mb_launIco", sub: new Menu(new EntEventMenuModel(this.egrid.modDoc),  EntityGridModel.eventAct)});
		mopts.push({isSep:true});
		if (this.egrid.config.removeMenuItem) {
			mopts.push(this.egrid.config.removeMenuItem);
		} else {
			mopts.push({label:"Remove", icon:"mb_remIco", act: EntityGridModel.removeMnAct});
		}
		this.entMenu = new Menu(new MenuModel(mopts));
	}
	return this.entMenu;
};


EntityGridModel.prototype.onRowMouseUp = function(evt, row) {
	if (this.quickLock) return;
	var menu = this.getMenu();
	GridModel.prototype.onRowMouseUp.apply(this, [evt, row]);	
	menu.popUp(evt, {row: row, cell0: this.getCell(row, 0)});
	return stopEvent(evt);
};


EntityGridModel.prototype.getRecordCell = function(recId) {
	var cell0;
	for (var ii = 0; ii < this.rows.length; ii++) {
		 cell0 = this.getCell(ii, 0);
		 if (cell0.getValue() == recId) {
		 	return cell0;
		 }
	}
	return null;
};

function EntIdPair(id, xNode) {
	this.id = id;
	this.xNode = xNode;
}

EntIdPair.prototype.toString = function() {
	return this.id;
};


// Entity Form Menu
EntFormMenuModel.prototype = new MenuModel();
EntFormMenuModel.prototype.constructor = EntFormMenuModel;

function EntFormMenuModel(modDoc, rootUri, showForm, menuFootItem) {
	this.modDoc = modDoc;
	this.rootUri = rootUri;
	this.showForm = showForm;
	this.menuFootItem = menuFootItem;
}

EntFormMenuModel.prototype.onReady = function(callback, menu) {
	this.digest(this.modDoc, callback, menu);
};

EntFormMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var frmsEl = Xml.matchOne(doc.documentElement, "forms");

	var list = Xml.match(frmsEl, "form");
	var ii;
    for (ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var src = nod.getAttribute("src");
        var name = nod.getAttribute("label");
        if (!name) {
        	name = Uri.name(src);
        	name = name.substring(0, name.length - 5);
        }
        if (this.showForm) name += " (" + Uri.name(src) + ")";
        var mItm = {label:"Form: " + name, uri:Uri.absolute(this.rootUri, src), src:src};
        items.push(mItm);
    }
    if (this.menuFootItem) {
    	items.push( {isSep : true });
    	items.push(this.menuFootItem);
    }
    callback.apply(menu, [items]);
};


// Entity Event Menu
EntEventMenuModel.prototype = new MenuModel();
EntEventMenuModel.prototype.constructor = EntEventMenuModel;

function EntEventMenuModel(modDoc, menuFootItem) {
	this.modDoc = modDoc;
	this.menuFootItem = menuFootItem;
}

EntEventMenuModel.prototype.onReady = function(callback, menu) {
	this.digest(this.modDoc, callback, menu);
};

EntEventMenuModel.prototype.digest = function(doc, callback, menu) {
	var items = [];
	var frmsEl = Xml.matchOne(doc.documentElement, "events");

	var list = Xml.match(frmsEl, "event");
	var ii;
    for (ii = 0; ii < list.length; ii++) {
        var nod = list[ii];
        var name = nod.getAttribute("type");
        var mItm = {label:name};
        items.push(mItm);
    }
    if (this.menuFootItem) {
    	items.push( {isSep : true });
    	items.push(this.menuFootItem);
    }
    callback.apply(menu, [items]);
};



EntFormDropDNDType.prototype = new DNDTypeDummy();
EntFormDropDNDType.prototype.constructor = EntFormDropDNDType;

function EntFormDropDNDType() {
    this.type = "dndEntForm";
}

EntFormDropDNDType.getEntityName = function(dragElem) {
	return dragElem._actElem._ctxNode.node.getAttribute("name");
};

EntFormDropDNDType.getEntityId = function(dragElem) {
	return dragElem._actElem._ctxNode.node.getAttribute("type");
};

EntFormDropDNDType.getEntityRelation = function(dragElem) {
	return dragElem._actElem._ctxNode.node.getAttribute("relation");
};

EntFormDropDNDType.menus = {};

EntFormDropDNDType.getFormMenu = function(entityId, menuAct, menuFootItem) {
	var entFormMen = EntFormDropDNDType.menus[entityId];
	if (!entFormMen) {
		var eg = new EntityGrid(null, "/getModel?id=" + Uri.escape(entityId));
		entFormMen = new Menu(new EntFormMenuModel(
				eg.modDoc, Uri.parent(eg.modelUri), true, menuFootItem), menuAct);
		App.addDispose(entFormMen);
		App.addDispose(eg);
		EntFormDropDNDType.menus[entityId] = entFormMen;
	}
	return entFormMen;
};


function EGridCell0(egrid, value, cssClass, noEdit) {
	this.__edit = false;
    this.__css = cssClass || "";
	this.__hElem = makeElement(null, "div", cssClass + " i_g");
	this.__value = value;
	if (egrid.config.relActs) {
		this.__hElem.style.whiteSpace = "nowrap";
		var stat = makeElementNbSpd(this.__hElem, "div", "togStat");
		stat._cell = this;
		setEventHandler(stat, "onmousedown", EGridCell0.__toggleClick);
		setEventHandler(stat, "onmouseup", stopEvent);
	}
	this.__hElem.appendChild(document.createTextNode(value == "" ? "\u00a0" : value));
}

EGridCell0.prototype = new GridCell();

EGridCell0.__toggleClick = function(evt) {
	var cell0 = this._cell;
	if (cell0.sgCell) {
		var opened = cell0.sgCell.opened;
		cell0.sgCell.opened = ! opened;
		cell0.sgCell.__hElem.parentNode.parentNode.style.display = opened ? "none" : (SH.is_ie ? "block" : "table-row");
		this.className = "togStat" + (opened ? "" : " close");
		if (!opened) {
			Data.xb.loadURIAsync("../entity/recordActivities?entity=" + Uri.escape(cell0.grid.model.egrid.name) +
					"&recordId=" + cell0.xNode.getAttribute("id"), 
				function(xmlDoc) {
					if (!App.checkError(xmlDoc)) {
						if (!Xml.matchOne(xmlDoc.documentElement, "activity")) {
							cell0.sgCell.__hElem.innerHTML = "No activities for this record.";
						} else {
							cell0.sgCell.__hElem.innerHTML = "";
							var amod = new ActivityTreeModel("assigned", null);
							amod.idSetDoc = xmlDoc;
							var tree = ActivityTree.renderActivityTreeMod(amod, cell0.sgCell.__hElem, 
								{nodrag : 1});
							App.addDispose(tree);
						}
					}
				
			});
		}
	}
	return stopEvent(evt);
};