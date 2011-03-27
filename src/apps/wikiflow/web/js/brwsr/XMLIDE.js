/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 * Lib: brwsr.XMLIDE
 *
 * @requires ../XMLEdit.js
 * @requires ../Controller.js
 * @requires Tree.js
 * @requires Menu.js
 * @requires Panel.js
 * @requires Dialog.js
 */

function XmlIDE() {
    this.docTree = null;
    this.srcWidget = null;
    this.propWidget = null;
    this.panelset = null;
    this.previewUIParent = null;
    this.previewXf = null;
    this.doc = null;
    this.header = null;
    this.path = "";
    this.__hilitBox = null;
}

XmlIDE.prototype.render = function(hParent) {
    var ps = new PanelSetVSplit(true, 200);
    ps.header = this.header;
    this.previewPanel = new Panel("Preview", false);
    ps.addMajor(this.previewPanel);
    var panProp = new Panel("Properties", false);
    panProp.setShrink(true);
    ps.addMajor(panProp);
    var panSrc = new Panel("XML Source", false);
    panSrc.setShrink(true);
    ps.addMajor(panSrc);
    var panTree = new Panel("Document Structure", false);
    ps.addMinor(panTree);

    ps.render(hParent, true);
    this.panelset = ps;

    this.propWidget = new XmlEdPropWidget(this, panProp);
    this.propWidget.render(panProp.contentElement);

    this.srcWidget = new XmlEdSrcWidget(this, panSrc);
    this.srcWidget.render(panSrc.contentElement);

    this.previewUIParent =
        makeElement(this.previewPanel.contentElement, "div", "xidePreview", "Loading...");

    this.previewMenu = new Menu(
        new MenuModel(
            [
                {label : "Properties", act : function(evt, ctx) {
                        holdThis.propWidget.edit(ctx.elem, ctx.xfObj);
                    }},
                {isSep : true },
                {label : "Edit XML", act : function(evt, ctx) {
                        holdThis.srcWidget.edit(ctx.elem, ctx.xfObj);
                    }},
                {label : "Append XML", act : function(evt, ctx) {
                        holdThis.srcWidget.append(ctx.elem);
                    }},
                {isSep : true },
                {label : "Edit Parent XML", act : function(evt, ctx) {
                       holdThis.srcWidget.edit(ctx.elem.parentNode);
                    }}
            ]
            )
        );

    var holdThis = this;

    // preview menu
    var onPreviewClick = function(evt) {
                var elem = getEventElement(evt);
                while (elem != null && elem != this && elem.__xfObj == null) {
                    elem = elem.parentNode;
                }
                if (elem != null && elem != this) {
                    holdThis.boxHiLite(elem);
                    holdThis.docTree.model.setSelected([]);
                    holdThis.docTree.markSelected();
                    evt.cancelBubble = true;
                    if (!SH.is_ie) {
                        evt.preventDefault();
                        evt.stopPropagation();
                    }
                    holdThis.previewMenu.popUp(evt, 
                    	{xfObj:elem.__xfObj, elem:elem.__xfObj.__elem});
                    return false;
                }
                holdThis.clearBoxHiLite();
                return false;
             };
    addEventHandler(this.previewUIParent, "contextmenu", onPreviewClick);

    this.doc = XMLEdit.xb.loadXML("<Blank/>");

    this.docTree = new Tree(new XmlEdTreeModel(this.doc.documentElement));
    this.docTree.menu = new Menu(
        new MenuModel(
            [
                {label : "Properties", act : function() {
                        holdThis.clearBoxHiLite();
                        Tree.selectItem(this._menu.__context);
                        holdThis.propWidget.edit(this._menu.__context.node);
                    }},
                {isSep : true },
                {label : "Copy", act : function() {
                        holdThis.clearBoxHiLite();
                        Tree.selectItem(this._menu.__context);
                        holdThis.clipBoardCopy(this._menu.__context.node);
                    }},
                {label : "Paste", act : function() {
                        holdThis.clearBoxHiLite();
                        holdThis.clipBoardPaste(this._menu.__context.node);
                    }},
                {isSep : true },
                {label : "Edit XML", act : function() {
                        holdThis.clearBoxHiLite();
                        holdThis.srcWidget.edit(this._menu.__context.node);
                    }},
                {label : "Append XML", act : function() {
                        holdThis.clearBoxHiLite();
                        Tree.selectItem(this._menu.__context);
                        holdThis.srcWidget.append(this._menu.__context.node);
                    }},
                {isSep : true },
                {label : "Edit Data", act : function() {
                        holdThis.clearBoxHiLite();
                        holdThis.editData(this._menu.__context.node);
                    }},
                {isSep : true },
                {label : "Remove", act : function() {
                        if (window.confirm("Are you sure?")) {
                            holdThis.clearBoxHiLite();
                            Tree.selectItem(this._menu.__context);
                            holdThis.removeNode(this._menu.__context.node);
                        }
                    }}
            ]
            )
        );
    this.docTree.menu.model.onSetContext = function(context) {
            // paste == 3
            this._menu.setDisable(this.items[3], holdThis.clipBoardEmpty());

            // Edit Data == 8
            this._menu.setDisable(
                this.items[8],
                xmlGetLocalName(context.node) != "instance");

            // remove == 10
            this._menu.setDisable(this.items[10], context.__isRoot);
        };
    this.docTree.render(panTree.contentElement);
    hParent = null; // IE enclosure clean-up
};


XmlIDE.prototype.openDialog = function(canCancel) {
    var diag = xfDialog(
        "Open Form", canCancel, document.body, "./formOpen.xfrm", XMLEdit.xb);
    diag.showModal(250, 150);
    this.lastDialog = diag;
};

XmlIDE.prototype.doOpen = function(action, model) {
    if (model == null) return;
    var nNode = XMLEdit.selectSingleNode(model.documentElement, "new");
    if (xmlStringForNode(nNode) == "1") {
        this.newDocument();
    } else {
        var uriNode = XMLEdit.selectSingleNode(model.documentElement, "uri");
        this.loadDocument(xmlStringForNode(uriNode));
    }
};

XmlIDE.prototype.loadDocument = function(uri) {
    this.srcWidget.__cancel();
    this.propWidget.__cancel();
    setElementText(document.getElementById("xideStat"), "Editing: " + uri);
    this.docUri = uri;
    this.doc = XMLEdit.xb.loadURI(this.path + uri);
    this.docTree.setModel(new XmlEdTreeModel(this.doc.documentElement));
    this.docTree.redrawAll();
    this.updatePreview();
};

XmlIDE.prototype.newDocument = function() {
    this.srcWidget.__cancel();
    this.propWidget.__cancel();
    setElementText(document.getElementById("xideStat"), "Editing: [New Form]");
    this.docUri = null;
    this.doc = XMLEdit.xb.loadXML(
        '<form xmlns="http://www.w3.org/1999/xhtml"\n' +
        ' xmlns:xf="http://www.w3.org/2002/xforms"\n' +
        ' xmlns:ev="http://www.w3.org/2001/xml-events">\n' +
        ' <xf:model>\n' +
        '   <xf:instance>\n' +
        '      <mydata xmlns="">\n' +
        '           <myvalue>default</myvalue>\n' +
        '      </mydata>\n'+
        '   </xf:instance>\n' +
        ' </xf:model>\n' +
        ' <h1>Sample</h1>\n' +
        ' <xf:input ref="myvalue"><xf:label>value</xf:label></xf:input>\n' +
        '</form>\n');
    this.docTree.setModel(new XmlEdTreeModel(this.doc.documentElement));
    this.docTree.redrawAll();
    this.updatePreview();
};


XmlIDE.prototype.doSave = function(action, model) {
    if (this.docUri == null && model == null) {
        var diag = xfDialog(
                "Save Form", true, document.body, "formSave.xfrm", XMLEdit.xb);
        diag.showModal(250, 150);
        this.lastDialog = diag;
    } else {
        var uri = this.path + this.docUri;
        if (model != null) {
            var uriNode =
                XMLEdit.selectSingleNode(model.documentElement, "uri");
            uri = this.path + xmlStringForNode(uriNode);
        }

        var xmlHttp = XMLBuilder.getXMLHTTP();
        var async = false;
        xmlHttp.open("PUT", uri, async);
        try {
            xmlHttp.send(this.doc);
            if (xmlHttp.status < 400) {
                window.alert("Save OK");
            } else {
                window.alert("Problem saving: " + xmlHttp.responseText);
            }
        } catch (e) {
            window.alert("Problem saving: " + e.message ? e.message : e);
        }
    }
};

XmlIDE.prototype.clipBoardEmpty = function() {
    return this.__cbNode == null;
};

XmlIDE.prototype.clipBoardCopy = function(node) {
    this.__cbNode = node;
};

XmlIDE.prototype.clipBoardPaste = function(node) {
    if (this.__cbNode != null) {
        node.appendChild(this.__cbNode.cloneNode(true));
        this.docTree.redraw(this.docTree.model.getItem(node, this.docTree.model));
        this.updatePreview();
    }
};

XmlIDE.prototype.clearBoxHiLite = function() {
    if (this.__hilitBox != null) {
        this.__hilitBox.parentNode.removeChild(this.__hilitBox);
        this.__hilitBox = null;
    }
};

XmlIDE.prototype.boxHiLite = function(hElem) {
    this.clearBoxHiLite();
    if (!hElem) return;
    var br = getLocalBounds(this.previewPanel.contentElement, hElem);
    this.__hilitBox = makeElement(this.previewPanel.contentElement, "div", "xideHiLit");
    br.x++;
    br.y++;
    br.w += 2;
    br.h+= 2;
    drawBox(this.__hilitBox, br, "line");
};

XmlIDE.prototype.removeNode = function(node) {
    var pn = node.parentNode;
    if (this.srcWidget.__currentNode != null &&
            (this.srcWidget.__currentNode === node
            || XMLEdit.isAncestor(node, this.srcWidget.__currentNode)
            || XMLEdit.isAncestor(this.srcWidget.__currentNode, node))) {
        this.srcWidget.__cancel();
    }
    if (this.propWidget.__currentNode != null &&
        (this.propWidget.__currentNode === node
            || XMLEdit.isAncestor(node, this.propWidget.__currentNode))) {
        this.propWidget.__cancel();
    }
    pn.removeChild(node);
    this.docTree.redraw(this.docTree.model.getItem(pn, this.docTree.model));
    this.updatePreview();
};

XmlIDE.prototype.updatePreview = function() {
    this.clearBoxHiLite();
    var holdThis = this;
    window.setTimeout(function() {
        if (holdThis.previewXf != null) {
            holdThis.previewXf.remove();
        }
        removeElementChildren(holdThis.previewUIParent);
        try {
            var uri = holdThis.path;
            if (holdThis.docUri != null) {
                uri += holdThis.docUri;
            } else {
                uri += "/";
            }

            holdThis.previewXf =
                  new XForm(holdThis.doc, "preview", XMLEdit.xb, uri);
            holdThis.previewXf.render(holdThis.previewUIParent);
        } catch (e) {
            holdThis.previewXf.dispose();
            holdThis.previewXf = null;
            if (e.message != null) {
                setElementText(holdThis.previewUIParent, e.message);
            } else {
                setElementText(holdThis.previewUIParent, e.toString());
            }
        }
    }, 100);
};

XmlIDE.prototype.editDataSave = function(node) {
    if (this.__edDatSrc != null) {
        var xmlHttp = XMLBuilder.getXMLHTTP();
        var async = false;
        xmlHttp.open("PUT", this.__edDatSrc, async);
        try {
            xmlHttp.send(node.ownerDocument);
            if (xmlHttp.status < 400) {
                window.alert("Save OK");
            } else {
                window.alert("Problem saving: " + xmlHttp.responseText);
            }
        } catch (e) {
            window.alert("Problem saving: " + e.message ? e.message : e);
        }
    } else {
        this.docTree.redrawAll();
    }
    this.updatePreview();
};

XmlIDE.prototype.editData = function(instNode) {
    var node;
    var src = instNode.getAttribute("src");
    if (src != null && src != "") {
        this.__edDatSrc = this.previewXf.resolveUri(src);
        node = XMLEdit.xb.loadURI(this.__edDatSrc).documentElement;
    } else {
        this.__edDatSrc = null;
        src = "internal";
        node = xmlFirstElement(instNode);
    }
    this.editDataNode(node, src);
};

XmlIDE.prototype.editDataNode = function(node, name) {
    var mod = new XmlTreeGridModel(node);
    if (this.edDatDiag != null) this.edDatDiag.remove();
    var grid = new Grid(mod);
    var tree = new Tree(mod);
    this.edDatDiag = new Dialog("Edit data: " + name, true);
    this.edDatDiag.addDisposable(grid);
    this.edDatDiag.addDisposable(tree);
    this.edDatDiag.render(document.body);

    tree.menu = new Menu(
        new MenuModel(
            [
                {label : "Insert", act : function(evt, context) {
                    Grid.__editCancel();
                    if (!context.hasKids) {
                        context.cells[0].setValue("<set>", true);
                        context.cells[0].setReadonly(true);
                    }
                    var doc = context.node.ownerDocument;
                    context.node.appendChild(doc.createElement("NewItem"));
                    context._tree.redraw(context, Tree.HINT_INSERT);
                }},
                {isSep : true },
                {label : "Remove", act : function(evt, context) {
                    Grid.__editCancel();
                    if (window.confirm("Are you sure?")) {
                        context.node.parentNode.removeChild(context.node);
                        var par = context._parent;
                        context._tree.redraw(par, Tree.HINT_REMOVE);
                        if (!par.hasKids) {
                            par.cells[0].setReadonly(false);
                            par.cells[0].setValue(
                                xmlStringForNode(par.node), true);
                        }
                    }
                }}
             ]));

    tree.menu.model.onSetContext = function(context) {
            // remove == 2
            this._menu.setDisable(this.items[2], context.__isRoot);
        };
    exAddClass(this.edDatDiag.contentElement, "i_grid", false);
    grid.render(this.edDatDiag.contentElement);
    tree.render(this.edDatDiag.contentElement, grid.getTreeStyle());
    this.__edDatSave =
        makeElement(this.edDatDiag.contentElement, "button", "xide", "Save");
    this.__edDatSave.style.margin = "4px 0px 0px 16px";

    this.edDatDiag.show(250, 150);
    var holdThis = this;
    this.__edDatSave.onclick = function (evt) {
            holdThis.editDataSave(mod.node);
        };
    grid.onresize = function(rect) {
            rect.h += 24;
            holdThis.edDatDiag.contentResized(rect);
        };
    node = null; // IE enclosure clean-up
};

XmlIDE.prototype.findXFObj = function(node) {
	if (!this.previewXf) return null;
	for (var ii = 0; ii < this.previewXf.__children.length; ii++) {
		var xfo = XmlIDE.__findXFObj(node, this.previewXf.__children[ii]);
		if (xfo) return xfo;		
	}
	return null;
};

XmlIDE.__findXFObj = function(node, xfObj) {
	if (xfObj.__elem === node) return xfObj;
	if (!xfObj.__children) return null; 
	for (var ii = 0; ii < xfObj.__children.length; ii++) {
		var xfo = XmlIDE.__findXFObj(node, xfObj.__children[ii]);
		if (xfo) return xfo;
	}
	return null;
};

XmlIDE.prototype.dispose = function() {
    if (this.edDatDiag != null) this.edDatDiag.dispose();
    if (this.docTree != null) this.docTree.dispose();
    if (this.previewXf != null) this.previewXf.dispose();
    if (this.srcWidget != null) this.srcWidget.dispose();
    if (this.propWidget != null) this.propWidget.dispose();
    if (this.lastDialog != null) this.lastDialog.dispose();
    if (this.panelset != null) this.panelset.dispose();
    if (this.previewMenu != null) this.previewMenu.dispose();
    this.__edDatSave = null;
    this.__cbNode = null;
    this.previewUIParent = null;
    this.doc = null;
    this.header = null;
    this.__hilitBox = null;
};

XmlEdTreeModel.prototype = new TreeModel();
XmlEdTreeModel.prototype.constructor = XmlEdTreeModel;

function XmlEdTreeModel(node) {
    TreeModel.apply(this, []);
    this.node = node;
}

XmlEdTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    var ns;
    if (itemParent != this) {
        ns = itemParent.node.childNodes;
    } else {
        ns = [this.node];
    }
    for (var i = 0; i < ns.length; i++) {
        var n = ns[i];
        if (n.nodeType == 1) {
            var item = new TreeItem(this, n.nodeName, (xmlLastElement(n) != null));
            item.node = n;
            item.act = treeMenuAction;
            item.optAct = treeMenuAction;
            if (itemParent == this) item.expanded = true;
            itemParent.add(item);
        } else if (n.nodeType == 3) {
            // maybe have text...
        }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
};

XmlEdTreeModel.prototype.dispose = function() {
    TreeModel.prototype.dispose.apply(this, []);
    this.node = null;
};

XmlEdTreeModel.prototype.getItem = function(node, itemParent) {
    var items = itemParent.kids;
    if (items == null) {
        return null;
    }
    for (var i = 0; i < items.length; i++) {
        if (items[i].node === node) {
            return items[i];
        } else {
            var si = this.getItem(node, items[i]);
            if (si != null) {
                return si;
            }
        }
    }
    return null;
};

function XmlEdSrcWidget(ide, panel) {
    this.__panel = panel;
    this.__ide = ide;
    if (panel != null) {
        panel.linkResize(this);
    }
    this.__currentNode = null;
}

XmlEdSrcWidget.prototype.render = function(hParent) {
    hParent.style.overflow = "hidden";
    this.__statElem = makeElement(hParent, "div", "xideSrcStat",
        "Select an item to edit");
    this.__txtElem = makeElement(hParent, "textarea", "xideSrc", null, null,
        { name : "srcTxt", wrap : "off" });
    this.__txtElem.style.visibility = "hidden";
    this.__btnsElem = makeElement(hParent, "div", "xideSrcBtn");
    this.__btnsElem.style.visibility = "hidden";
    var holdThis = this;
    this.__applBtnElem =
        makeElement(this.__btnsElem, "button", null, "Apply");
    this.__applCloseBtnElem =
        makeElement(this.__btnsElem, "button", null, "Apply & Close");
    this.__applBtnElem.onclick = function (evt) {
            holdThis.__apply();
        };
    this.__applCloseBtnElem.onclick = function (evt) {
            holdThis.__apply(true);
        };
    this.__cancBtnElem =
        makeElement(this.__btnsElem, "button", null, "Cancel");
    this.__cancBtnElem.onclick = function (evt) {
            holdThis.__cancel();
        };
    hParent = null; // IE enclosure clean-up
};

XmlEdSrcWidget.prototype.resize = function(rect) {
    var rb1 = getBounds(this.__statElem);
    var rb2 = getBounds(this.__btnsElem);
    this.__txtElem.style.width = (rect.w > 9 ? (rect.w - 9) : 0) + "px";
    var h = rect.h - (rb1.h + rb2.h + 5);
    if (h < 0) h = 0;
    this.__txtElem.style.height = h + "px";
};

XmlEdSrcWidget.prototype.__apply = function(doClose) {
    var tree = this.__ide.docTree;
    var drawNode = this.__currentNode;
    var str = this.__txtElem.value;
    if (str == "" || str.indexOf("<") < 0) {
        window.alert("XML is empty or has no tags");
        return;
    }
    try {
        if (this.__append) {
            XMLEdit.appendXML(this.__currentNode, str);
        } else {
            if (this.__currentNode === this.__ide.doc.documentElement) {
                var doc = XMLEdit.xb.loadXML(str);
                if (doc == null || doc.documentElement == null
                    || doc.documentElement.namespaceURI ==
                    "http://www.mozilla.org/newlayout/xml/parsererror.xml") {
                    window.alert("Invalid XML format");
                    return;
                }
                this.__ide.doc = doc;
                this.__currentNode = doc.documentElement;
                tree.setModel(
                        new XmlEdTreeModel(this.__ide.doc.documentElement));
                tree.redrawAll();
                this.__ide.updatePreview();
                if (doClose) this.__done();
                return;
            }
            drawNode = this.__currentNode.parentNode;
            var rNode =
                XMLEdit.replaceXML(this.__currentNode, str);
            if (rNode == null || rNode.namespaceURI ==
                "http://www.mozilla.org/newlayout/xml/parsererror.xml") {
                window.alert("Invalid XML format");
                return;
            }
            this.__currentNode = rNode;
        }
    } catch (e) {
        if (e.message != null) window.alert(e);
        else window.alert(e.toString());
        return;
    }
    this.__ide.updatePreview();
    tree.redraw(tree.model.getItem(drawNode, tree.model));
    if (doClose) this.__done();
};

XmlEdSrcWidget.prototype.__done = function() {
    this.__currentNode = null;
    this.__txtElem.style.visibility = "hidden";
    this.__btnsElem.style.visibility = "hidden";
    if (this.__panel != null) {
        this.__panel.setShrink(true);
    }
    setElementText(this.__statElem, "Select an item to edit");
};

XmlEdSrcWidget.prototype.__cancel = function() {
    this.__done();
};

XmlEdSrcWidget.prototype.edit = function(node, xfObj) {
	if (!xfObj) xfObj = this.__ide.findXFObj(node);
    if (xfObj) this.__ide.boxHiLite(xfObj.__hElem);
    var path = XMLEdit.getPath(node);
    setElementText(this.__statElem, "Edit:  /" + path.join("/"));
    this.__txtElem.style.visibility = "";
    this.__btnsElem.style.visibility = "";
    this.__txtElem.value = XMLEdit.outterXML(node);
    this.__currentNode = node;
    if (this.__ide.propWidget.__currentNode != null &&
            (this.__ide.propWidget.__currentNode === node
            || XMLEdit.isAncestor(node, this.__ide.propWidget.__currentNode))) {
        this.__ide.propWidget.__cancel();
    }
    this.__append = false;
    if (this.__panel != null) {
        this.__panel.setShrink(false);
    }
};

XmlEdSrcWidget.prototype.append = function(node, xfObj) {
	if (!xfObj) xfObj = this.__ide.findXFObj(node);
    if (xfObj) this.__ide.boxHiLite(xfObj.__hElem);
    var path = XMLEdit.getPath(node);
    setElementText(this.__statElem, "Append to:  /" + path.join("/"));
    this.__txtElem.style.visibility = "";
    this.__btnsElem.style.visibility = "";
    this.__txtElem.value = "";
    this.__currentNode = node;
    this.__append = true;
    if (this.__panel != null) {
        this.__panel.setShrink(false);
    }
};

XmlEdSrcWidget.prototype.dispose = function() {
    this.__currentNode = null;
    this.__panel = null;
    this.__ide = null;
    this.__statElem = null;
    this.__txtElem = null;
    this.__btnsElem = null;
    this.__applBtnElem = null;
    this.__applCloseBtnElem = null;
    this.__cancBtnElem = null;
};

function XmlEdPropWidget(ide, panel) {
    this.__panel = panel;
    this.__ide = ide;
    this.__currentNode = null;
}

XmlEdPropWidget.prototype.render = function(hParent) {
    this.__statElem = makeElement(hParent, "div", "xideSrcStat",
        "Select an item to edit");
    this.__frmElem = makeElement(hParent, "form", "xideProp");
    this.__frmElem.style.visibility = "hidden";
    this.__btnsElem = makeElement(hParent, "div", "xideSrcBtn");
    this.__btnsElem.style.visibility = "hidden";
    var holdThis = this;
    this.__applBtnElem =
        makeElement(this.__btnsElem, "button", null, "Apply");
    this.__applCloseBtnElem =
        makeElement(this.__btnsElem, "button", null, "Apply & Close");
    this.__applBtnElem.onclick = function (evt) {
            holdThis.__apply();
        };
    this.__applCloseBtnElem.onclick = function (evt) {
            holdThis.__apply(true);
        };
    this.__cancBtnElem =
        makeElement(this.__btnsElem, "button", null, "Cancel");
    this.__cancBtnElem.onclick = function (evt) {
            holdThis.__cancel();
        };
    hParent = null; // IE enclosure clean-up
};

XmlEdPropWidget.prototype.edit = function(node, xfObj) {
    this.__currentNode = node;
    if (!xfObj) xfObj = this.__ide.findXFObj(node);
    if (xfObj) this.__ide.boxHiLite(xfObj.__hElem);
    
    var path = XMLEdit.getPath(node);
    setElementText(this.__statElem, "Attributes:  /" + path.join("/"));
    if (this.__ide.srcWidget.__currentNode != null &&
            (this.__ide.srcWidget.__currentNode === node
            || XMLEdit.isDescendant(this.__ide.srcWidget.__currentNode, node))) {
        this.__ide.srcWidget.__cancel();
    }
    removeElementChildren(this.__frmElem);

    // read attributes...
    var txtInp, inpPar;
    var atts = node.attributes;
    var count = 0;
    for (var i = 0; i < atts.length; i++) {
        var a = atts[i];
        if (a.nodeName.indexOf("xmlns") != 0) {
            count++;
            inpPar = makeElement(this.__frmElem, "div", "xideProp");
            makeElement(inpPar, "label", null, a.nodeName);
            var isXpath = " ref nodeset value calculate relevant constraint before origin ".indexOf(
            		" " + a.nodeName + " ") >= 0;
            txtInp = makeElement(inpPar, "textarea", null, null, null,
                { "name" : a.nodeName });
            txtInp.value = a.nodeValue;
            if (isXpath) {
            	makeElement(inpPar, "div", "xpthInfo", "xpath info");
            }
        }
    }
    if (count > 0) {
        this.__btnsElem.style.visibility = "";
    } else {
        inpPar = makeElement(this.__frmElem, "div", null, "No attributes");
    }
    if (xmlFirstElement(node) == null) {
        // innerText
        inpPar = makeElement(this.__frmElem, "div", "xideProp");
        makeElement(inpPar, "label", "xidePropTxt", "Inner Text:");
        txtInp = makeElement(inpPar, "textarea", "xidePropTxt", null, null,
            { "name" : "$innerText" });
        txtInp.value = xmlStringForNode(node);
        this.__btnsElem.style.visibility = "";
    }
    this.__frmElem.style.visibility = "";
    if (this.__panel != null) {
        this.__panel.setShrink(false);
    }
};

XmlEdPropWidget.prototype.__apply = function(doClose) {
    var frm = this.__frmElem;
    var node = this.__currentNode;
    for (var i =0; i < frm.elements.length; i++) {
        var elem = frm.elements[i];
        if (elem.name == "$innerText") {
            var doc = node.ownerDocument;
            var kid = node.firstChild;
            while (kid != null) {
                node.removeChild(kid);
                kid = node.firstChild;
            }
            node.appendChild(doc.createTextNode(elem.value));
        } else {
            node.setAttribute(elem.name, elem.value);
        }
    }
    this.__ide.updatePreview();
    if (doClose) this.__done();
};

XmlEdPropWidget.prototype.__done = function() {
    this.__currentNode = null;
    this.__frmElem.style.visibility = "hidden";
    this.__btnsElem.style.visibility = "hidden";
    if (this.__panel != null) {
        this.__panel.setShrink(true);
    }
    setElementText(this.__statElem, "Select an item to edit");
};

XmlEdPropWidget.prototype.__cancel = function() {
    this.__done();
};

XmlEdPropWidget.prototype.dispose = function() {
    this.__currentNode = null;
    this.__panel = null;
    this.__ide = null;
    this.__statElem = null;
    this.__frmElem = null;
    this.__btnsElem = null;
    this.__applBtnElem = null;
    this.__applCloseBtnElem = null;
    this.__cancBtnElem = null;
};


XmlTreeGridModel.prototype = new TreeGridModel();
XmlTreeGridModel.prototype.constructor = XmlTreeGridModel;

function XmlTreeGridModel(/*XMLNode*/ node, /*Array<String>*/ showAttributes) {
    TreeGridModel.apply(this, []);
    this.node = node;
    if (showAttributes == null) this.showAtts = [];
    else this.showAtts = showAttributes;
}

XmlTreeGridModel.prototype.onReady = function(callback, tree, itemParent) {
    var ns;
    if (itemParent != this) {
        ns = itemParent.node.childNodes;
    } else {
        ns = [this.node];
    }
    for (var i = 0; i < ns.length; i++) {
        var n = ns[i];
        if (n.nodeType == 1) {
            var hasKids = (xmlLastElement(n) != null);
            var item = new TreeGridItem(this, n.nodeName, hasKids);
            itemParent.add(item);
            item.node = n;
            var cell = new GridCell(hasKids ?
                    (itemParent == null ? "<root>" : "<set>")
                    : xmlStringForNode(n));
            cell.setReadonly(hasKids);
            cell._item = item;
            if (!hasKids) cell.setValue = XmlTreeGridModel.__setCellValue;
            item.cells.push(cell);
            for (var jj = 0; jj < this.showAtts.length; jj++) {
                var attVal = n.getAttribute(this.showAtts[jj]);
                if (attVal == null) attVal = "";
                var attCell = new GridCell(attVal);
                attCell._item = item;
                attCell._attribute = this.showAtts[jj];
                attCell.setValue = XmlTreeGridModel.__setAttCellValue;
                item.cells.push(attCell);
            }
            item.setValue = XmlTreeGridModel.__setItemValue;

            if (itemParent == this) {
                item.readonly = true;
                window.setTimeout(function() {
                        tree.toggle(item);
                    }, 100);
                n = null; // IE enclosure clean-up
            }
        }
    }
    ns = null; // IE enclosure clean-up
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
};

XmlTreeGridModel.__setCellValue = function(value, keepXml) {
    if (!keepXml && this.canEdit()) {
        xmlSetNodeValue(this._item.node, value);
        var mod = this._item.model;
        if (mod.xfMod != null) mod.xfMod.markChanged(this._item.node);
    }
    GridCell.prototype.setValue.apply(this, [value]);
};

XmlTreeGridModel.__setAttCellValue = function(value) {
    if (this.canEdit()) {
        this._item.node.setAttribute(this._attribute, value);
        var mod = this._item.model;
        if (mod.xfMod != null) mod.xfMod.markChanged(this._item.node, this._attribute);
    }
    GridCell.prototype.setValue.apply(this, [value]);
};

XmlTreeGridModel.prototype.setFormModel = function(model) {
    this.xfMod = model;
};

XmlTreeGridModel.prototype.dispose = function() {
    TreeModel.prototype.dispose.apply(this, []);
    this.rows = null;
    this.grid = null;
    this.node = null;
};

XmlTreeGridModel.__nameRegEx =
    new RegExp("^[A-Za-z][A-Za-z0-9_\\-\\.]*(?::[A-Za-z][A-Za-z0-9_\\-\\.]*)?$");

XmlTreeGridModel.__setItemValue = function (value) {
    if (!XmlTreeGridModel.__nameRegEx.test(value)) {
        alert("Names cannot contain spaces, extended characters or start with a number.");
        return;
    }
    if (this.label != value) {
        this.node = XMLEdit.renameElement(this.node, value);
        TreeGridItem.prototype.setValue.apply(this, [value]);
    }
};

