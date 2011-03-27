/**
 * (c) 2006 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 */

var FileTree = {

    serviceUri : "../shell/",
    loadUri : "../fil",
	unixHide : true,
    xb : new XMLBuilder(),

    view : function(evt, item) {
        var hand = FileTree.viewHandlers[Uri.ext(item.uri).toLowerCase()];
        if (hand != null) {
            hand(evt, item);
        } else {
            if (Uri.ext(item.uri).toLowerCase() in FileTree.mimeNewWin || item.uri.charAt(item.uri.length - 1) == "/") {
                window.open(FileTree.loadUri + item.uri, "_blank");
            } else {
                try {
                    location.href = FileTree.loadUri + item.uri;
                } catch(e){}
            }
        }
    },

    edit : function(evt, item) {
        var hand = FileTree.editHandlers[Uri.ext(item.uri).toLowerCase()];
        if (hand != null) {
            hand(evt, item);
        } else {

            // default to view
            FileTree.view(evt, item);
        }
    },

    properties : function(evt, item) {
        Tree.selectItem(item);
        var diag = xfDialog("File Properties: " + Uri.name(item.uri),
                true, document.body, "../view-repo/props.xfrm", item.model.xb, null,
                FileTree.serviceUri + "getProps?uri=" + Uri.escape(item.uri),
                null, false, App ? App.chromeHelp : null);
        diag.xform.getDefaultModel().addEventListener("xforms-close", { handleEvent : function() {
                FileTree.refresh(null, item.itemParent);
            }});
        diag.show(getMouseX(evt), getMouseY(evt));
    },

    addFiles : function(evt, item) {
        Tree.selectItem(item);
        FileTree.diagUploadAct(evt, item);
    },

    permissions : function(evt, item) {
        Tree.selectItem(item);
        //FilePerm.dialog(evt, item);
        var permUri = FileTree.serviceUri + "getPerms?uri=" + Uri.escape(item.uri);
        XFModel.clearLoadCache();
        var diag = xfDialog("File Permissions: " + Uri.name(item.uri),
                true, document.body, "../view-repo/perms.xfrm.jsp", item.model.xb, null,
                permUri,
                null, false, App ? App.chromeHelp : null);
        diag.xform.getDefaultModel().addEventListener("xforms-close", { handleEvent : function() {
                if (item.itemParent !== item.model) FileTree.refresh(null, item.itemParent);
            }});
        diag.show(getMouseX(evt), getMouseY(evt), null, 530, 300);
    },

    newFolder : function(evt, item) {
        Tree.selectItem(item);
        var diag = Dialog.prompt("Folder name:","New Folder", function(name) {
   					App.checkError(item.model.xb.loadURI(FileTree.serviceUri + "newFolder?uri=" +
		            Uri.escape(item.uri) + "&name=" + Uri.escape(name)));
		            item._tree.redraw(item, Tree.HINT_INSERT);
   				});
		diag.show(getMouseX(evt) + 20, getMouseY(evt) - 20);
    },

    history : function(evt, item) {
        Tree.selectItem(item);
        var uri = item.uri;
        var diag = new Dialog("History: " + Uri.name(uri), true);
        diag.initHelp(App ? App.chromeHelp : null);
        diag.render(document.body);
        if (FileTree.historyRender({uiParent: diag.contentElement}, uri))
            diag.show(getMouseX(evt), getMouseY(evt));
    },

    historyRender : function(config, uri) {
        var doc = FileTree.xb.loadURI(FileTree.serviceUri + "status?uri=" + Uri.escape(uri));
        if (App.checkError(doc)) return false;
        config._uri = uri;
        var pDiv = makeElement(config.uiParent, "div", "histDiag");
        var histTable = makeLayoutTable(pDiv, "hist");
        var hrow = makeElement(histTable, "tr");
        makeElement(hrow, "th", null, "Version");
        makeElement(hrow, "th", null, "Date");
        makeElement(hrow, "th", null, "Who");
        makeElement(hrow, "th", null, "Active");
        var histElems = Xml.match(doc.documentElement, "version");
        for (var ii = 0; ii < histElems.length; ii++) {
            var verElem = histElems[ii];
            var row = makeElement(histTable, "tr", (ii % 2) ? "" : "alt");
            makeElement(row, "td", "first", verElem.getAttribute("number"));
            makeElement(row, "td", "date",
                    DateUtil.toLocaleShort(DateUtil.parse8601(verElem.getAttribute("time"), true), true));
            makeElement(row, "td", "who", UserTree.getUserName(verElem.getAttribute("modifier")));
            if (verElem.getAttribute("default") == "1") {
                row.className += " default";
                makeElement(row, "td", "default", "active");
            } else {
                var atd = makeElement(row, "td", "click", "make active");
                atd._config = config;
                atd._version = verElem.getAttribute("number");
                setEventHandler(atd, "onclick",  FileTree.__historyAct);
            }
        }
        return true;
    },

    __historyAct : function() {
        var config = this._config;
        var version = this._version;
        removeElementChildren(config.uiParent);
        makeElement(config.uiParent, "div", "loading", "Loading...");
        window.setTimeout( function() {
	        var doc = FileTree.xb.loadURI(FileTree.serviceUri + "setVersion?uri=" + Uri.escape(config._uri) +
	                                      "&version=" + Uri.escape(version));
	        if (App.checkError(doc)) return;
	        removeElementChildren(config.uiParent);
	        FileTree.historyRender(config, config._uri);
        }, 50);
    },

    refresh : function(evt, item) {
        if (item) item._tree.redraw(item.allowsKids ? item : item.itemParent, Tree.HINT_INSERT);
    },

    remove : function(evt, item) {
        Tree.selectItem(item);
        if (confirm("Are you sure?")) {
            if (!App.checkError(item.model.xb.loadURI(FileTree.serviceUri + "delete?uri=" + Uri.escape(item.uri)))) {
                item.removeItem();
            }
        }
    },

     renderFiles : function(title, uri, showRoot, uiParent, optList) {
        var fileMod = new FileTreeModel(uri, showRoot);
        if (optList) {
            arrayInsert(optList, 0, {title : title, uri : uri, showRoot : showRoot});
            fileMod.optList = optList;
            var opLink = makeElement(uiParent, "div", "minorBtn", optList[1].title);
            opLink._fileMod = fileMod;
            opLink._idx = 1;
            fileMod.opLink = opLink;
            setEventHandler(opLink, "onclick", FileTreeModel.opClick);
            makeElementNbSpd(opLink, "div", "mbIco mb_bakIco");
        }
        fileMod.title = makeElement(uiParent, "div", "minorHead", title + " Files");
        var hElem = makeElement(uiParent, "div", "minorTreeBox");
        var fileTree = new Tree(fileMod);
        App.setFileMenu(fileTree);
        fileTree.makeDragCanvas(uiParent, new FileTreeDNDType("fileTree"), true, true, true);
        fileTree.render(hElem, null, "fileTree");
        App.addDispose(fileTree);
        return fileMod;
    },

    activityListenActivityFiles : function(fileMod) {
        return function(node) {
              fileMod.activityFiles(node);
        };
    },

	// folder menu
    onMenuContext : function (item /* context */) {
        // this == menu.model
        var prot = item.isProtected();
        this._menu.setDisable(this.items[0], prot);
        this._menu.setDisable(this.items[7], prot);
    },
    
    // file menu
    onMenuContext2 : function (item /* context */) {
        // this == menu.model
        var prot = item.isProtected();
        var ext = Uri.ext(item.uri).toLowerCase();
        this._menu.setDisable(this.items[0], FileTree.extEditOnly[ext]);
        this._menu.setDisable(this.items[1], FileTree.extViewOnly[ext]);
        this._menu.setDisable(this.items[3], prot);
        this._menu.setDisable(this.items[7], prot);
    },
    
    __isProtected : function() {
    	var prot = (this.getDepth() == 0);
        if (FileTree.unixHide) {
        	prot = prot || /^\/[^\/]+\/(process|project|meeting|course)$/.test(this.uri);
        	prot = prot || /^\/[^\/]+\/(process|meeting|course)\/[^\/]+\/(template|activities)$/.test(this.uri);
        }
        return prot;
    },
    
    __isProtected2 : function() {
    	var prot = false;
        if (FileTree.unixHide) {
        	prot = /^\/[^\/]+\/(Community\.kb|Reference\.kb|Guest\.kb)$/.test(this.uri);
        	prot = prot || /^\/[^\/]+\/(process|meeting|course)\/.+\/(chart\.flow|rules\.xml|activities\.kb)$/.test(this.uri);
        }
        return prot;
    },

    tryRefresh : function(uri) {
        // TODO make this scan the tree and refresh
    },
    
   	doCopy : function(srcUri, dstUri, tarUri) {
   		var diag = xfDialog(
			"Create", true, document.body, 
			"../view-repo/copy.xfrm", FileTree.xb, {tarUri: tarUri, dstUri: dstUri},
			null, null, false, App ? App.chromeHelp : null);
		var xfMod = diag.xform.getDefaultModel();
		xfMod.setValue("dstUri", dstUri);
		xfMod.setValue("dstBase", Uri.parent(dstUri));
		xfMod.setValue("name", Uri.name(dstUri));
		xfMod.setValue("srcUri", srcUri);
		xfMod.refresh();
		App.addDispose(diag);
		diag.show(100,100);
   	},
   	
   	copyDone : function(uris, resDoc) {
   		var pDst = Uri.parent(uris.dstUri);
   		if (uris.tarUri)  {
            var rslUri = resDoc.documentElement.getAttribute("result");
            rslUri = Uri.absolute(rslUri, uris.tarUri);
            FileTree.view(null, {uri : rslUri});
        }
        FileTree.tryRefresh(pDst);
   	},

    // handler type:  function (evt, item)
    editHandlers : new Object(),
    viewHandlers : new Object(),

    __upCount : 0,
    __upDiags : new Object()
};


FileTreeModel.prototype = new TreeModel();
FileTreeModel.prototype.constructor = FileTreeModel;

function FileTreeModel(uri, showRoot) {
    TreeModel.apply(this, []);
    this.uri = uri;
    this.showRoot = showRoot;
    this.xb = FileTree.xb;
}

FileTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    if (this.showRoot && itemParent === this) {
        var tItm = new TreeItem(this, Uri.name(itemParent.uri), true);
        tItm.isProtected = FileTree.__isProtected;
        tItm.uri = itemParent.uri;
        tItm.act = tItm.optAct = treeMenuAction;
        tItm.icon = "fldIco";
        tItm.expanded = true;
        itemParent.add(tItm);
        callback.apply(tree, [itemParent.kids, itemParent]);
    } else {
        var holdThis = this;
        this.xb.loadURIAsync(FileTree.serviceUri + "list?uri=" + Uri.escape(itemParent.uri),
                function(doc, arg, xmlHttp) {
                    if (doc == null || App.checkError(doc)) {
                        // TODO error message
                    } else {
                        var list = Xml.match(doc.documentElement, "node");
                        var tItm;
                        var empty = true;
                        
                        // check if this wasn't async disposed
                        if (!itemParent.kids) return;

                        for (var ii = 0; ii < list.length; ii++) {
                            var nod = list[ii];
                            var name = nod.getAttribute("uri");
                            // unix hide
                            if (FileTree.unixHide && name.substring(0,1) == ".") continue;

                            empty = false;
                            var isFold = nod.getAttribute("collection") == "1";
                            tItm = new TreeItem(holdThis, name, isFold);
                            tItm.uri = Uri.absolute(itemParent.uri, name);
                            if (isFold) {
                                tItm.act = tItm.optAct = treeMenuAction;
                                tItm.isProtected = FileTree.__isProtected;
                                tItm.icon = "fldIco";
                            } else {
                                tItm.act = tItm.optAct = treeMenu2Action;
                                tItm.isProtected = FileTree.__isProtected2;
                                tItm.noDrop = true;
                                tItm.icon = Uri.ext(name).toLowerCase() + "_Ico";
                            }
                            itemParent.add(tItm);
                        }
                        if (empty) {
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

FileTreeModel.prototype.setOptIndex = function(idx) {
    var opt = this.optList[idx];
    this.title.firstChild.nodeValue = opt.title + " Files";
    this.uri = opt.uri;
    this.showRoot = opt.showRoot;
    this._tree.redrawAll();
    idx++;
    if (idx >= this.optList.length) idx = 0;
    this.opLink.firstChild.nodeValue = this.optList[idx].title;
    this.opLink._idx = idx;
};

FileTreeModel.prototype.activityFiles = function(node) {
    if (node) {
        var info = ActivityTree.activityInfo(node.getAttribute("id"));
        if (info) {
	        this.optList[2] = {
	            title: ActivityTree.isMeet ? "App" : "Activity",
	            uri:info.getAttribute("uri"),
	            showRoot:true };
	        this.setOptIndex(2);
        }
    }
};

FileTreeModel.prototype.dispose = function() {
    this.title = null;
    this.opLink = null;
    TreeModel.prototype.dispose.apply(this, []);
};

FileTreeModel.opClick = function() {
    var fileMod = this._fileMod;
    fileMod.setOptIndex(this._idx);
};

FileTreeDNDType.prototype = new TreeDNDType();
FileTreeDNDType.prototype.constructor = FileTreeDNDType;

function FileTreeDNDType(cssClass) {
    this.type = "dndFile";
    this.cssClass = cssClass;
    this.menu = new Menu(new MenuModel(
         [
                 {label : "Move Here", icon : "mb_movIco", act : FileTreeDNDType.moveFile},
                 {label : "Copy Here", icon : "mb_copyIco", act : FileTreeDNDType.copyFile},
                 {isSep : true },
                 {label : "Cancel", act : FileTreeDNDType.cancelFile}
         ]));
}

FileTreeDNDType.moveFile = function (evt, pair) {
    var dstUri = Uri.absolute(pair.dropItem.uri, Uri.name(pair.dragItem.uri));
    if (!App.checkError(pair.dragItem.model.xb.loadURI(FileTree.serviceUri + "move?srcUri=" +
             Uri.escape(pair.dragItem.uri) + "&dstUri=" + Uri.escape(dstUri)))) {
        pair.dragItem.removeItem();
        pair.dragItem.dispose();
        pair.dropItem._tree.redraw(pair.dropItem, Tree.HINT_INSERT);
    }
};

FileTreeDNDType.copyFile = function (evt, pair) {
    var dstUri = Uri.absolute(pair.dropItem.uri, Uri.name(pair.dragItem.uri));
    App.checkError(pair.dragItem.model.xb.loadURI(FileTree.serviceUri + "copy?srcUri=" +
             Uri.escape(pair.dragItem.uri) + "&dstUri=" + Uri.escape(dstUri)));
    pair.dropItem._tree.redraw(pair.dropItem, Tree.HINT_INSERT);
};

FileTreeDNDType.cancelFile = function (evt, pair) {
    pair.dropItem.model.setSelected([]);
    pair.dropItem._tree.markSelected();
};

FileTreeDNDType.prototype.canDrag = function(dragElem) {
    var item = dragElem.__item;
    return Uri.parent(item.uri) != "";
};

FileTreeDNDType.prototype.dropExec = function(dropElem, dragElem) {
    var dropItem = dropElem.__item;
    var dragItem = dragElem._actElem.__item;
    var rect = getViewBounds(dropElem);
    dropItem.model.setSelected([dropItem, dragItem]);
    dropItem._tree.markSelected();
    this.menu.setDisable(this.menu.model.items[0], dragItem.isProtected());
    this.menu.show(rect.x + 102, rect.y, null, {dropItem : dropItem, dragItem : dragItem});
};

FileTreeDNDType.prototype.dropTest = function(dropElem, dragElem) {

    // default same tree type test
    if (dropElem._dndType.__canvas === dragElem._dndType.__canvas) {
        if (dragElem._dndType instanceof DNDGroup) {
            // TODO support groups
            return false;
        }
        var dropItem = dropElem.__item;
        var dragItem = dragElem._actElem.__item;
        return dropItem.allowsKids && !dragItem.isAncestor(dropItem) &&
               DNDTypeDummy.prototype.dropTest.apply(this, [dropElem, dragElem]);
    }
    return false;
};


/**
 * XForm plugin
 */
if (typeof(XFTypeFormat) != "undefined") {

function FileXFTypeFmt(){ }

FileXFTypeFmt.prototype = new XFTypeFormat();
FileXFTypeFmt.prototype.constructor = FileXFTypeFmt;


FileXFTypeFmt.prototype.format = function(str, ctrl) {
	if (ctrl._actElem && ctrl._actElem.parentNode) 
		ctrl._actElem.parentNode.removeChild(ctrl._actElem);
	
	if (!this.__dnd) {
		this.__dnd = dndGetCanvas(document.body);
		this.__dndType = new FileXFDNDHand(this);
	}
	this.__dnd.addDNDType(this.__dndType);
	var baseUri = ctrl.__model.getInstancePath(ctrl.__iNode);
	if (baseUri.substring(0, FileTree.loadUri.length) == FileTree.loadUri) baseUri = baseUri.substring(FileTree.loadUri.length);
	var showPar = ctrl.getAttribute("display") == "parent";
	if (str == "") {
		var note = ctrl.getAttribute("note");
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "filEmpty", note ? "<" + note + ">" :
			(App.guest ? "<Upload file here>" : "<Drop a file here>"))
	} else {
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "file");
		makeElementNbSpd(ctrl._actElem, "span", "icon " + Uri.ext(str).toLowerCase() + "_Ico");
		makeElement(ctrl._actElem, "span", "name", Uri.name(showPar ? Uri.parent(str) : str));
		this.__dnd.makeDraggable(ctrl._actElem, this.__dndType.type);
		var uri = str;
		if (str != "" && str.charAt(0) != '/') {
			uri = Uri.absolute(baseUri, str);
		}		
		setEventHandler(ctrl._actElem, "onclick",
                            function(evt) {
                                FileTree.edit(evt, {uri:uri});
                                return false;
                            });
	}
	var uploadClick = function(evt) {
			FileTree.diagUpload(evt, baseUri, false, function(uri) { ctrl.setValue(uri); });
		};
	
	// add context menu
	var menu = new Menu(new MenuModel(
         	[
             {label : "Upload File", icon : "mb_addIco", act : uploadClick},
             {isSep : true },
             {label : "Clear File", icon : "mb_remIco", act : function(uri) { ctrl.setValue(""); }}
     	]));
  	App.addDispose(menu);
	setEventHandler(ctrl._actElem, "oncontextmenu", function(evt) { menu.popUp(evt, ctrl); return false; });
	
	
	ctrl._actElem._ctrl = ctrl;
    if (!App.guest) {
    	this.__dnd.makeDropTarget(ctrl._actElem, this.__dndType.type);
    }
    if (str == "") {
    	setEventHandler(ctrl._actElem, "onclick", uploadClick);
    }
    return str;
};

FileXFTypeFmt.prototype.parse = function(str, ctrl) {
	var uri = str;
	if (uri != "") {
		var base = ctrl.__model.getInstancePath(ctrl.__iNode);
		if (base.substring(0, FileTree.loadUri.length) == FileTree.loadUri) base = base.substring(FileTree.loadUri.length);
		uri = Uri.localize(base, uri);
	}
    return uri;
};

FileXFTypeFmt.prototype.decorate = function(uiElem, ctrl) {
	if (ctrl.constructor == XFControlInput) {
		ctrl.__hWidget.style.display = 'none';
	}
};

FileXFTypeFmt.prototype.disposeDecor = function(uiElem, ctrl) {
	if (this.__dnd && ctrl._actElem) {
		this.__dnd.disposeDropTarget(ctrl._actElem);
	}
	ctrl._actElem = null;
};


xsdTypes.itensilXF["file"] = { mapped : XSD_MAPPED_TYPE.STRING };

XFTypeFormat.addFormat(XFORM_ITENSIL_NAMESPACE, "file", new FileXFTypeFmt());

function FileXFDNDHand(view) {
    this.type = "dndXFFile";
}

FileXFDNDHand.prototype = new DNDTypeDummy()
FileXFDNDHand.prototype.constructor = FileXFDNDHand;


FileXFDNDHand.prototype.dropTest = function(dropElem, dragElem) {
    // activity drop
    var type = dragElem._dndType.type;
    if (type == "dndFile" || type == "dndXFFile") {
    	var exts = dropElem._ctrl.getAttribute("extensions");
    	if (exts) {
    		return exts.toLowerCase().indexOf(Uri.ext(this.getUri(dragElem)).toLowerCase()) >= 0;
    	}
        return true;
    }
    return false;
};

FileXFDNDHand.prototype.getUri = function(dragElem) {
	if (dragElem._dndType.type == "dndFile") {
    	return dragElem._actElem.__item.uri;
    } else {
    	var ctrl = dragElem._actElem._ctrl;
    	var uri = ctrl.getValue();
		if (uri != "" && uri.charAt(0) != '/') {
			uri = Uri.absolute(ctrl.__model.getInstancePath(ctrl.__iNode), uri);
			if (uri.substring(0, FileTree.loadUri.length) == FileTree.loadUri) uri = uri.substring(FileTree.loadUri.length);
		}
		return uri;
    }
};

FileXFDNDHand.prototype.dropExec = function(dropElem, dragElem) {
    // file drop
    dropElem._ctrl.setValue(this.getUri(dragElem));
};


}
/**
 * End XForm plugin
 */
 

function FilePerm(node) {
    this.node = node;
    this.hovLevel = 0;
    this.bpX = 0;
}

FilePerm.prototype.render = function(uiParent, canManage) {
    this.hElem = makeElementNbSpd(uiParent, "div", "perm");
    this.hElem.peer = this;
    if (this.node.getAttribute("owner") != "1" && canManage) {
        setEventHandler(this.hElem, "onmouseover", FilePerm.mouseOver);
        setEventHandler(this.hElem, "onmouseout", FilePerm.mouseOut);
        setEventHandler(this.hElem, "onmousemove", FilePerm.mouseMove);
        setEventHandler(this.hElem, "onclick", FilePerm.mouseClick);
    }
    this.show(parseInt(this.node.getAttribute("level")));
};

FilePerm.prototype.doSet = function() {
    this.node.setAttribute("level", this.hovLevel);
    this.node.setAttribute("change", "1");
};

FilePerm.prototype.show = function(level) {
    var bpY = 0;
    switch (level) {
    case 1: bpY = -28; break;
    case 2: bpY = -56; break;
    case 3: bpY = -84; break;
    case 4: bpY = -112; break;
    }
    this.hElem.style.backgroundPosition = this.bpX + "px " + bpY + "px";
};

FilePerm.prototype.hover = function() {
    this.bpX = -97;
    this.show(this.hovLevel);
};

FilePerm.prototype.unhover = function() {
    this.bpX = 0;
    this.show(parseInt(this.node.getAttribute("level")));
};

FilePerm.prototype.dispose = function() {
    this.node = null;
    if (this.hElem) this.hElem.peer = null;
    this.hElem = null;
    this.rowElem = null;
};

FilePerm.prototype.revoke = function() {
    this.node.setAttribute("revoke", "1");
    this.hElem.parentNode.removeChild(this.hElem);
    if (this.rowElem) {
        this.rowElem.parentNode.removeChild(this.rowElem);
    }
    this.dispose();
};

FilePerm.mouseOver = function(evt) {
    this.peer.hover();
};

FilePerm.mouseOut = function(evt) {
    this.peer.unhover();
};

FilePerm.mouseMove = function(evt) {
    var pnt = getLocalMousePnt(evt);
    if (pnt.x <= 17) this.peer.hovLevel = 0;
    else if (pnt.x <= 35) this.peer.hovLevel = 1;
    else if (pnt.x <= 57) this.peer.hovLevel = 2;
    else if (pnt.x <= 76) this.peer.hovLevel = 3;
    else this.peer.hovLevel = 4;
    this.peer.hover();
};

FilePerm.mouseClick = function(evt) {
    this.peer.doSet();
    this.peer.unhover();
};


FileTree.diagUploadAct = function(evt, item) {
	var diag = FileTree.diagUpload(evt, item.uri, true);
	diag.item = item;
};

FileTree.diagUpload = function(evt, uri, multi, callback) {
    var diag = new Dialog((multi ? "Add files: " : "Upload file: ") + Uri.name(uri), true);
    diag.initHelp(App.chromeHelp);
    diag.render(document.body);
    diag.upCallback = callback;
    
    FileTree.__upCount++;
    var upId = "upf" + FileTree.__upCount;

    var enclose = new Object();
    diag.contentElement.appendChild(
        H.div({klass:"upload"},
            H.iframe({src:"../blank.html", name:upId, border:0,
                    frameborder:0, width:1, height:1, style:"width:1px;height:1px;margin:0"}),

            (enclose.form = H.form({action:FileTree.serviceUri + "upload", target:upId,
                    method:"post", enctype:"multipart/form-data"},

            (enclose.loading = H.div({klass:"loading"}, "Uploading...")),
                H.input({type:"hidden", name:"clientId", value:upId}),
                H.input({type:"hidden", name:"uri", value:uri}),
				H.div({klass:"note"}, "Choose " + (multi ? "one or more of your files" : "a file") + " to add online."),
	
                H.div({klass:"field"}, H.input({type:"file", name:"upload1"})),
                !multi ? null : H.div({klass:"field"}, H.input({type:"file", name:"upload2"})),
                !multi ? null : H.div({klass:"field"}, H.input({type:"file", name:"upload3"})),

                H.div({klass:"submit"}, H.input({type:"submit", klass:"diagBtn dbUpload", value:""}))
                ))
        ));
    if (multi && SH.is_ie) {
        var weElem = new H.a("Windows Explorer");
        
        var href = Uri.reduce(getHttpPath() + "../dav/fil" + uri + "?") ;
        weElem.href = href;
        weElem.target = "_blank";
        weElem.folder = href;
        weElem.style.behavior = "url(#default#AnchorClick)";
        diag.contentElement.appendChild(H.div({klass:"fieldOpt"}, weElem));
        
        if (SH.is_ie) {
        	H.div({klass:"fieldOpt"}).innerHTML = 
        	diag.contentElement.appendChild(H.div({klass:"fieldOpt"})).innerHTML = 
        	"<div style='width:250px'><b>Note:</b> Some versions of Internet Explorer may have a problem opening 'Windows Explorer'." +
        	" If you experience a problem please install the fix provided by Microsoft " +
        	"<a href='http://www.microsoft.com/downloads/details.aspx?familyid=17C36612-632E-4C04-9382-987622ED1D64' target='_blank'>here</a>.</div>";
        }
        
    }
    enclose.loading.style.visibility = "hidden";
    enclose.form.onsubmit = function () {
            enclose.loading.style.visibility = "visible";
            enclose = null;
        };
    diag.show(getMouseX(evt) + 20, getMouseY(evt));
    FileTree.__upDiags[upId] = diag;
    return diag;
};

function fileUploaded(upId, uri) {
    var diag = FileTree.__upDiags[upId];
    if (diag != null) {
        window.setTimeout(function() {
        		if (diag.item) diag.item._tree.redraw(diag.item, Tree.HINT_INSERT);
            }, 10);
       	if (diag.upCallback) {
       		diag.upCallback(uri);
       	}
        diag.destroy();
    }
}

FileTree.extViewOnly = {
	pdf:1,gif:1,jpg:1,png:1,html:1,htm:1,xml:1,kb:1,txt:1,xsl:1,
	doc:!SH.is_ie,ppt:!SH.is_ie,xls:!SH.is_ie,vsd:!SH.is_ie,
	docx:!SH.is_ie,pptx:!SH.is_ie,xlsx:!SH.is_ie
};

FileTree.extEditOnly = {
	xfrm:1,flow:1,rule:1,entity:1
};

FileTree.mimeNewWin = {
/*@cc_on //IE only new winds
  	@if (@_jscript)
    doc:1,ppt:1,xls:1,vsd:1,
	@end
@*/
pdf:1,gif:1,jpg:1,png:1,html:1,htm:1,rprt:1,kb:1,xml:1,xsl:1,xfrm:1,txt:1,rule:1};
