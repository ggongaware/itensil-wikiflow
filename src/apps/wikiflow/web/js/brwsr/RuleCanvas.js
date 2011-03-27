/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Custom Rule Canvas (that's really a Tree + XForm)
 */
function RuleCanvas(panel, xfrmUri, dataUri) {
	
	this.canPath = makeElement(panel.contentElement, "div", "canPath", noBreakString(">  Main"));

    // map form for validation, serialization, and form widgets
    var xb = new XMLBuilder();
    this.xfrm = new XForm(xb.loadURI(xfrmUri), "rulecan", xb, xfrmUri);
    this.xfrm.setDefaultUris(dataUri, dataUri);
    this.xfrm.render(panel.contentElement);
    this.uri = dataUri;

    // draw a tree with form block leafs
    this.tree = new Tree(new RuleTreeModel(this.xfrm));
    this.tree.makeDragCanvas(panel.contentElement, new RuleDstTreeDNDType(), false, false, true);
    this.tree.render(panel.contentElement, null, "ruleTree");

    // developer link
    /*
    var link = makeElement(panel.contentElement, "a", "devLink", "View XML Structure", null, { href :"#"});
    var holdThis = this;
    link.onclick = function() {
            //holdThis.viewXMLStruct();
            console.dirxml(holdThis.xfrm.getDefaultModel().getDefaultInstance());
            return false;
        };
	
    link = null; // IE enclosure clean-up
    */
}

RuleCanvas.prototype.doSave = function() {
    this.xfrm.getDefaultModel().submit("save");
};

RuleCanvas.showDefaultRule = function() {
	RuleCanvas.live.showRule("default");
};

RuleCanvas.unloadHandler = function() {
	return RuleCanvas.live.xfrm.isDirty();
};

RuleCanvas.prototype.showRule = function(id) {
	this.lastTrace = null;
	if (id == "default") {
		setElementText(this.canPath, noBreakString(">  Main"));
	} else {
		removeElementChildren(this.canPath);
		makeElement(this.canPath, "span", "", noBreakString(">  "));
		setEventHandler(makeElement(this.canPath, "u", "click", "Main"), "onclick", RuleCanvas.showDefaultRule);
		makeElement(this.canPath, "span", "active", noBreakString("  >  ") + id);
	}
	
	this.tree.model.ruleId = id;
	this.tree.redrawAll();
};

RuleCanvas.prototype.dispose = function() {
    this.tree.dispose();
    this.xfrm.dispose();
    if (this.tstXfrm) this.tstXfrm.dispose();
    this.tstXFDoc = null;
    this.lastTrace = null;
};

// Developer view
RuleCanvas.prototype.viewXMLStruct = function() {
    var mod = new XmlTreeGridModel(this.xfrm.getDefaultModel().getDefaultInstance(), ["type"]);
    mod.setFormModel(this.xfrm.getDefaultModel());
    if (this.edDatDiag != null) this.edDatDiag.remove();
    var grid = new Grid(mod);
    var tree = new Tree(mod);
    this.edDatDiag = new Dialog("View XML", true);
    this.edDatDiag.addDisposable(grid);
    this.edDatDiag.addDisposable(tree);
    this.edDatDiag.render(document.body);
    grid.render(this.edDatDiag.contentElement);
    tree.render(this.edDatDiag.contentElement, grid.getTreeStyle());
    this.edDatDiag.showModal(250, 150);
    var holdThis = this;
    grid.onresize = function(rect) {
            holdThis.edDatDiag.contentResized(rect);
        };
    this.edDatDiag.onClose = function() {
            holdThis.xfrm.getDefaultModel().rebuild();
        };
};


RuleCanvas.prototype.initTestXform = function(panel) {
	this.testPanel = panel;
	this.updateTestXform();
};

RuleCanvas.prototype.updateTestXform = function() {
	var doc = this.xfrm.getDefaultModel().getDefaultInstance().ownerDocument;
	var dat = Xml.matchOne(doc.documentElement, "data");
	if (!dat) return;
	
	var formUri = "rule-test.xfrm";
	var xfDoc = Data.xformFromAttrs(dat, formUri);

    if (this.tstXfrm) this.tstXfrm.dispose();

    this.tstXfrm = new XForm(xfDoc, "testdatxf", Data.xb, formUri);
	this.tstXfrm.setDefaultUris(null, "javascript:RuleCanvas.live.testRules(model.getDefaultInstance())");
	
	removeElementChildren(this.testPanel.contentElement);
	
	this.tstXfrm.render(makeElement(this.testPanel.contentElement, "div", "xidePreview"));
	
	this.tstXFDoc = xfDoc;
};

RuleCanvas.prototype.testRules = function(contextNode) {
	var xfMod = this.xfrm.getDefaultModel();
	var ii;
	if (this.lastTrace) {
		for (ii=0; ii < this.lastTrace.length; ii++) {
			var trc = this.lastTrace[ii];
			xfMod.sendDataEvent(trc[0], "rule-clear", trc);
		}
	}
	var rules = new Rules(this.tree.model.xNode, xfMod.getDefaultInstance());
	var res = null;
	try {
		res = rules.match(contextNode);
		this.lastTrace = rules.trace;
		for (ii=0; ii < this.lastTrace.length; ii++) {
			var trc = this.lastTrace[ii];
			xfMod.sendDataEvent(trc[0], "rule-trace", trc);
		}
		
		// update test form?
		if (this.tstXfrm && rules.setNodes.length > 0) {
			var tstMod = this.tstXfrm.getDefaultModel();
			for (ii=0; ii < rules.setNodes.length; ii++) {
				tstMod.markChanged(rules.setNodes[ii]);
			}
			tstMod.refresh();
		}
		
		if (res === null) {
			alert("No return matched.");
		} else {
			alert("Return: " + res);
		}
	} catch (e) {
		alert("Error processing: " + e);
	}
	rules.dispose();	
	return res;
};

RuleCanvas.prototype.clearTrace = function() {
	var xfMod = this.xfrm.getDefaultModel();
	var ii;
	if (this.lastTrace) {
		for (ii=0; ii < this.lastTrace.length; ii++) {
			var trc = this.lastTrace[ii];
			xfMod.sendDataEvent(trc[0], "rule-clear", trc);
		}
	}
};

RuleCanvas.prototype.saveTestXform = function() {
	var holdThis = this;
	var diag = Dialog.prompt("Form file name:",{body:"MyForm", suffix:".xfrm"}, function(name) {
			var tstUri = Uri.absolute(Uri.parent(holdThis.uri), name);
			Data.saveDoc(holdThis.tstXFDoc, tstUri, function(resDoc, uri) {
					Ephemeral.topMessage(null, "Form file " + name + " saved.");
				});
		});
	diag.show(200, 200);
};

RuleCanvas.prototype.xpEditor = function(evt, xpathNode) {
	var xfMod = this.xfrm.getDefaultModel();
	var rules = new Rules(this.tree.model.xNode, xfMod.getDefaultInstance());
	var diag = XPathEditor.dialog("XPath Editor", 
		rules, this.tstXfrm.getDefaultModel().getDefaultInstance(), App.chromeHelp);
	var btnTr = makeElement(makeLayoutTable(diag.contentElement, "diagBtnRow"), "tr");
	setEventHandler(makeElement(makeElement(btnTr, "td"), "button", "diagBtn dbSave"),
		"onclick", function() {
			xfMod.setValue("@test", diag.xpeditor.getText(), xpathNode.node)
			xfMod.refresh();
			diag.destroy();
		});
	setEventHandler(makeElement(makeElement(btnTr, "td"), "button", "diagBtn dbCancel"),
		"onclick", function() {
			diag.destroy();
		});
	diag.addDisposable(rules);
	diag.xpeditor.setText(xpathNode.node.getAttribute("test"));
	diag.show(200, getMouseY(evt));
	
	btnTr = null; // IE enclosure clean-up
};

function RuletAttrListener(rc) {
	this.rc = rc;
}

RuletAttrListener.prototype.handleEvent = function(evt) {
    if (evt.type == "xforms-value-changed") {
    	this.rc.updateTestXform();
    }
};



/**
 * Tree Model Classes
 */
RuleTreeModel.prototype = new TreeModel();
RuleTreeModel.prototype.constructor = RuleTreeModel;

function RuleTreeModel(xfrm) {
    TreeModel.apply(this, []);
    this.xfrm = xfrm;
	this.model = this; // self reference for DND
	this.xNode = xfrm.getDefaultModel().getDefaultInstance();
	this.ruleId = "default";
}

RuleTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    var xfmod = this.xfrm.getDefaultModel();
    var nList;
    if (itemParent === this) {
    	this.xNode = xfmod.selectNodeList("rule[@id='" + this.ruleId + "']")[0];
    }
    if (!this.xNode) {
    	alert("Missing rule for: " + this.ruleId);
    	RuleCanvas.showDefaultRule();
    	return;
    }
    nList = xfmod.selectNodeList("when|otherwise|find|sub|return|set", itemParent.xNode);

    var ii;
    for (ii = 0; ii < nList.length; ii++) {
        var item = new RuleTreeItem(this, nList[ii]);
        if (item.allowsKids) item.icon = "fldIco";
        itemParent.add(item);
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
    for (ii = 0; ii < itemParent.kids.length; ii++) {
		if (itemParent.kids[ii].allowsKids) tree.toggle(itemParent.kids[ii]);
	}
};

RuleTreeItem.prototype = new TreeItem();
objectExtend(RuleTreeItem.prototype, XFDirectBinding.prototype);
RuleTreeItem.prototype.constructor = RuleTreeItem;

function RuleTreeItem(model, xNode) {
	var locnm = Xml.getLocalName(xNode);
    TreeItem.apply(this, [model, null, (locnm == "when" || locnm == "otherwise" || locnm == "find")]);
    this.initBind(model.xfrm, xNode);
    this.addEventListener("rule-trace", this);
    this.addEventListener("rule-clear", this);
    this.xNode = xNode;
}

RuleTreeItem.prototype.renderLabel = function(hParent) {
    if (this.__labDomE == null) {
        var xf = this.model.xfrm;
        var tName = Xml.getLocalName(this.xNode);
        if (tName == "when") {
	        var aType = this.xNode.getAttribute("type");
	        if (aType) tName += "-" + aType;
        }
        var tmpl = xf.getIxTemplate(tName);
        if (tmpl) {
        	this.__labDomE = tmpl.renderTemplate(this.xNode, hParent);
        } else {
        	this.__labDomE = makeElement(hParent, "div", "unrec", "Unrecognized rule element");
        }
    } else if (this.__labDomE.parentNode !== hParent){
        hParent.appendChild(this.__labDomE);
    }
};

RuleTreeItem.prototype.handleEvent = function(evt) {
	if (!this.__labDomE) return;
	var klass = evt.detail[1] ? "ruleTrace1" : "ruleTrace2";
	if (evt.type == "rule-trace") {
		exAddClass(this.__labDomE, klass);
		var trMsg = classMatchOneDeep(this.__labDomE, "traceMsg");
		if (trMsg) setElementText(trMsg, (evt.detail[1] ? "QUALIFIED: " : "") + evt.detail[2]);
	} else if (evt.type == "rule-clear") {
		exAddClass(this.__labDomE, klass, true);
	}
};

// override from XFDirectBinding
RuleTreeItem.prototype.nodeRemoved = function() {
	this.removeEventListener("rule-trace", this);
    this.removeEventListener("rule-clear", this);
    this.removeFromParent();
    if (this.model) this.model._tree.__disposeItem(this);
};


RuleTreeItem.prototype.renderTail = function() {
	this.tailEl = null;
	var tree = this.model._tree;
	if (this.allowsKids && tree.dndCanvas) {
		this.tailEl = makeElement(null, "div");
		makeElementNbSpd(this.tailEl, "div", "foldTailBar");
		this.tailEl._tailItem = this;
		tree.dndCanvas.makeDropTarget(this.tailEl, tree.dndType.type);
	}
    return this.tailEl;
};


RuleTreeItem.prototype.dispose = function() {
    this.xNode = null;
    if (this.tailEl) {
    	var tree = this.model._tree;
    	if (tree && tree.dndCanvas) 
    		tree.dndCanvas.disposeDropTarget(this.tailEl);
    	this.tailEl = null;
    }
    XFDirectBinding.prototype.dispose.apply(this, []);
    TreeItem.prototype.dispose.apply(this, []);
};


/**
 * Drag-N-Drop Classes
 */

/**
 * Rule Attribute DNDTypeHandler
 */
RuleSrcTreeDNDType.prototype = new DNDTypeDummy();
RuleSrcTreeDNDType.prototype.constructor = RuleSrcTreeDNDType;

function RuleSrcTreeDNDType() {
    this.type = "dndRuleSrcTr";
}

RuleSrcTreeDNDType.getName = function(dragElem) {
	return dragElem._actElem._ctxNode.node.getAttribute("name");
};

RuleSrcTreeDNDType.prototype.dropTest = function(dropElem, dragElem) {
    return false;
};

RuleSrcTreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
    //empty
};



// For the destination Tree
function RuleDstTreeDNDType() {
    this.type = "dndRuleDstTr";
}

RuleDstTreeDNDType.prototype = new TreeDNDType();
RuleDstTreeDNDType.prototype.constructor = RuleDstTreeDNDType;

RuleDstTreeDNDType.prototype.dropTest = function(dropElem, dragElem) {
    if (dragElem._dndType.constructor === RuleSrcTreeDNDType) {
        return true;
    }
    return TreeDNDType.prototype.dropTest.apply(this, [dropElem, dragElem]);
};

RuleDstTreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
	
	// non-array attr fields have if / set option
	if (dragElem._dndType.constructor === RuleSrcTreeDNDType &&
			dragElem._actElem && dragElem._actElem._ctxNode && 
			Xml.getLocalName(dragElem._actElem._ctxNode.node) == "attr" &&
			dragElem._actElem._ctxNode.node.getAttribute("type").indexOf(":array") < 0) {
	
		if (!this.attMenu) {
    		this.attMenu = new Menu(new MenuModel(
	         [
	                 {label : "If Test", icon : "mb_viewIco", act : RuleDstTreeDNDType.attrTest},
	                 {label : "Set Value", icon : "mb_movIco", act : RuleDstTreeDNDType.attrSet},
	                 {isSep : true },
	                 {label : "Cancel", act : RuleDstTreeDNDType.attrCancel}
	         ]));
	      	App.addDispose(this.attMenu);
    	}
    	var rect = getViewBounds(dragElem);
	    this.attMenu.show(rect.x + 5, rect.y + 5, null, { dnd : this, dropElem : dropElem, dragElem : dragElem});
		return;	
	}
	this.ruleDropExec(dropElem, dragElem);
};

RuleDstTreeDNDType.prototype.ruleDropExec = function(dropElem, dragElem, isSet) {
	
	RuleCanvas.live.clearTrace();
	
    var dropItem = dropElem.__item;
    var dragSrcElem = dragElem._actElem;
    var dragItem = dragSrcElem.__item;
    
    var isNew = false;
	if (dropElem._isFoot) {
		dropItem = this._tree.model;
	}
	
	var model = dropItem.model;
	var xfMod = model.xfrm.getDefaultModel();
    if (dragElem._dndType.constructor === RuleSrcTreeDNDType) {
        isNew = true;
		
		var dstNode = dropItem.xNode;
        var xNode;
      
        var befXpath = null;
        if (!dropElem._tailItem && dropItem !== model) {
            var at = dropItem.itemParent.getIndex(dropItem);
            dstNode = dstNode.parentNode;
            befXpath = "*[" + (at + 1) + "]"; // element at index
        }
        
        if (dragSrcElem._ctxNode) {
        	var srcNode = dragSrcElem._ctxNode.node;
        	switch (Xml.getLocalName(srcNode)) {
        		case "return":
        			xNode = xfMod.duplicateNode("instance('pal')/return", ".", befXpath, dstNode);
        			xfMod.setValue("@id", srcNode.getAttribute("id"), xNode);
        			break;
        			
        		case "rule":
        			xNode = xfMod.duplicateNode("instance('pal')/sub", ".", befXpath, dstNode);
        			xfMod.setValue("@id",  srcNode.getAttribute("id"), xNode);
        			break;
        			
        		case "attr":
        			var name = srcNode.getAttribute("name");
        			var par = srcNode.parentNode;
        			while (Xml.getLocalName(par) == "attr") {
        				name = par.getAttribute("name") + "/" + name;
        				par = par.parentNode;
        			}
        			if (isSet) {
        				xNode = xfMod.duplicateNode("instance('pal')/set", ".", befXpath, dstNode);
	        			xfMod.setValue("@field", name, xNode);
        			} else {
	        			xNode = xfMod.duplicateNode("instance('pal')/when[@type='']", ".", befXpath, dstNode);
	        			xfMod.setValue("@type", srcNode.getAttribute("type"), xNode);
	        			xfMod.setValue("@field", name, xNode);
        			}
        			break;
        	}
        	
        } else if (dragSrcElem._emptyType) {
        	var srcPath;
        	switch (dragSrcElem._emptyType) {
        		case "xpath":
        			srcPath = "instance('pal')/when[@type='xpath']"; break;
        		case "otherwise":
        			srcPath = "instance('pal')/otherwise"; break;
        		case "find":
        			srcPath = "instance('pal')/find"; break;	
        	}
        	xNode = srcPath ? xfMod.duplicateNode(srcPath, ".", befXpath, dstNode) : "";
        }
    
    	if (xNode) {
    		// mark that the document model has changed
    		xfMod.rebuild();
	        dragItem = new RuleTreeItem(model, xNode);
	        if (dragItem.allowsKids) dragItem.icon = "fldIco"; 
    	}
            
    }
	if (dragItem) {
	    if (dropElem._tailItem || dropItem === model) {
	    	if (dropItem.__kidDomElem) {
	        	dropItem.add(dragItem);
	        	if (isNew) dropItem.renderKid(dragItem);
	    	} else {
	    		dragItem.setParent(dropItem);
	    	}

	    	if (!isNew) {
	    		xfMod.__moveNode(dragItem.xNode, dropItem.xNode);
	    		xfMod.rebuild();
	    	}
	    	if (dropItem !== model && !dropItem.expanded) {
	    		window.setTimeout(function() {
	    				model._tree.toggle(dropItem);
	    			}, 50);
	    	}

	    } else {
	        dropItem.itemParent.insertBefore(dragItem, dropItem);
	        if (isNew) dropItem.itemParent.renderKid(dragItem);
	        else {
	        	xfMod.__moveNode(dragItem.xNode, dropItem.itemParent.xNode, dropItem.xNode);
	        	xfMod.rebuild();
	        }
	    }
	}
	dragSrcElem = null; xNode = null; dragElem = null; dropElem = null; // IE enclosure clean-up
};

RuleDstTreeDNDType.attrTest = function(evt, triplet) {
	triplet.dnd.ruleDropExec(triplet.dropElem, triplet.dragElem);
};

RuleDstTreeDNDType.attrSet = function(evt, triplet) {
	triplet.dnd.ruleDropExec(triplet.dropElem, triplet.dragElem, true);
};

RuleDstTreeDNDType.attrCancel = function() {
	return;
};
