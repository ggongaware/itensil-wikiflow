/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 * Entity Modeling
 *
 */
function EntityCanvas(panel, xfrmUri, dataUri, path) {

    // form for validation, serialization, and form widgets
    var xb = new XMLBuilder();
    this.xfrm = new XForm(xb.loadURI(xfrmUri), "entcan", xb, xfrmUri);
    this.xfrm.setDefaultUris(dataUri, dataUri);
    if (path) this.xfrm.__defPath = path;
    this.xfrm.render(panel.contentElement);
    this.uri = dataUri;
}

EntityCanvas.prototype.save = function() {
	this.xfrm.getDefaultModel().submit("save");
};


EntityCanvas.prototype.saveNewRuleset = function() {
	var holdThis = this;
	var diag = Dialog.prompt("Query file name:",{body:"MyQuery", suffix:".rule"}, function(name) {
			var xfMod = holdThis.xfrm.getDefaultModel();
			var doc = xfMod.getDefaultInstance().ownerDocument;
			
			var dat = Xml.matchOne(doc.documentElement, "data");
			if (!dat) return;
			
			var xfDoc = Data.rulesFromAttrs(dat);
			var dstUri = Uri.absolute(holdThis.xfrm.__defPath, name);
			Data.saveDoc(xfDoc, "../fil" + dstUri, function(resDoc, uri) {
					var dup = xfMod.duplicateNode("instance('pal')/ruleset", "queries");
					xfMod.setValue("@src", name, dup);
					xfMod.rebuild();
				});
			doc = null; dat = null; xfDoc = null;
		});
	diag.show(200, 200);
};


EntityCanvas.prototype.saveNewXform = function() {
	var diag = xfTemplateDialog("Entity Form", true, document.body, this.xfrm, "formGenOptions", null, true);
	diag.show(200, 200);
};

EntityCanvas.prototype.dispose = function() {
    this.xfrm.dispose();
};

EntityCanvas.unloadHandler = function() {
	return EntityCanvas.live.xfrm.isDirty();
};