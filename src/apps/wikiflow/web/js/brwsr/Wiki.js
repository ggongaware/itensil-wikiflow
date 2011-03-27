/**
 * Portions (c) 2005, 2006 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 * Other portions:
 *
 *  TiddlyWiki 1.2.6 by Jeremy Ruston, (jeremy [at] osmosoft [dot] com)
 *  Licsense in body above TiddlyWiki portions
 *
 */



/**
 * Wiki
 * 
 * @param {String} uri The uri to be loaded into this wiki. ="../fil/debug1/Community.kb"
 * @param {String} uiParent The document element that is the parent of this wiki node.  
 * @param {String} title The title to be display for the wiki.
 * @param {} view
 * @param {} config 
 */
function Wiki(uri, uiParent, title, views, config)  {
    this.uri = uri;
    this.articleIdx = new Object();
    this.loadedUris = [];
    this.views = [];
    if (views) {
        for(var ii = 0; ii < views.length; ii++)
            this.views.push(new WikiView(this, views[ii]));
    }
    this.uiParent = uiParent;
    this.currentIndexMode = null;
    this.startTitle = title;
    this.firstArticle = null;
    this.config = {
        user : "Anonymous",
        iconPath : "../pix/fico/",
        attachIndex : true,
        locked : false,
        noSearch : false
        };
   	if (config) objectExtend(this.config, config);
    this.frameInit(uiParent);
    this.doc = null;
    this.xb = new XMLBuilder();
    this.loading = 0;
    this.lastHits = 0
    this.searchKeyTimer = null;
    this.layoutMenu = new Menu(new MenuModel([
            { label : "One Column", act : new Function("evt", "view", "view.setLayout('')") },
            { label : "Two Column", act : new Function("evt", "view", "view.setLayout('2colA')") }
            ]));
    this.styleMenu = new Menu(new MenuModel([
            { label : "Shaded - Right" , act : new Function("evt", "view", "view.insertStyle('right', 'shaded', 'Text here.')")},
            { label : "Tip - Right" , act : new Function("evt", "view", "view.insertStyle('right', 'tip', 'Text here.')")},
            { label : "Warning - Right" , act : new Function("evt", "view", "view.insertStyle('right', 'warning', 'Text here.')")},
            { label : "Glance - Right" , act : new Function("evt", "view", "view.insertStyle('right', 'glance', 'Text here.')")},
            { isSep : true},
            { label : "Shaded - Left" , act : new Function("evt", "view", "view.insertStyle('left', 'shaded', 'Text here.')")},
            { label : "Tip - Left" , act : new Function("evt", "view", "view.insertStyle('left', 'tip', 'Text here.')")},
            { label : "Warning - Left" , act : new Function("evt", "view", "view.insertStyle('left', 'warning', 'Text here.')")},
            { label : "Glance - Left" , act : new Function("evt", "view", "view.insertStyle('left', 'glance', 'Text here.')")},
            { isSep : true},
            { label : "Include" , act : new Function("evt", "view", "view.insertMacro('include', 'Article')")}
            ]));
   	this.simpleMenu = new Menu(new MenuModel([
   			{ label : "Styles", sub : this.styleMenu},
            { label : "Layouts", sub : this.layoutMenu}
            ]));
    this.workzoneMenu = new Menu(new MenuModel([
            { label : "Submit Work" , act : new Function("evt", "view", "view.insertMacro('submit', 'Continue')")},
            { label : "Bold Submit Work" , act : new Function("evt", "view", "view.insertMacro('submit','!Continue')")},
            { label : "Plan a Step" , act : new Function("evt", "view", "view.insertMacro('plan', 'Step Name')")},
            { isSep : true},
            { label : "Styles", sub : this.styleMenu},
            { label : "Layouts", sub : this.layoutMenu}
            ]));
    this.load(uri, "");
}

Wiki.dialog = function(uri, title, views, config) {
    var diag = new Dialog(title || "Knowledgebase", true);
    diag.render(document.body);
    diag.contentElement.className += " wikiDiag";
    var wik = new Wiki(uri, diag.contentElement, title, views, config);
    diag._wiki = wik;
    diag.addDisposable(wik);
    diag.linkResize(wik);
    return diag;
};

/*
 * @param {String} uri
 * @param {String} title
 * @param {int} width
 * @param {int} height
 * @param {String[]} params url parameters to append
 */

Wiki.popupArt = function(uri, title, width, height, params) {
    var url = "../kb/articlepage?uri=" + Uri.escape(uri) + "&title=" + Uri.escape(title);
    for (var pm in params) {
    	url += "&" + Uri.escape(pm) + "=" + Uri.escape(params[pm]) ;
    }

	//TODO -elj- set wiki.popupArt open window to standard popup
	var win = window.open(url, "_blank");
	win.focus();
/*
    try {
	    var win = window.open(url, "_blank",
	    "height= " + "890" + "," +
//	    "height= " + height + "," +
	    "width= "  + "800" + ",resizable=yes,status=yes,toolbar=yes,menubar=yes,location=yes,scrollbars=yes");
//	    "width= "  + width + ",resizable=yes,status=no,toolbar=no,menubar=no,location=no,scrollbars=no");
	    win.focus();
	} catch(e) {
		alert("Please turn off any pop-up blockers for this website, and try again.");
	}
*/
 };

Wiki.popup = function(uri, title, width, height, params) {
    var url = "../kb/page?uri=" + Uri.escape(uri) + "&title=" + Uri.escape(title);
    for (var pm in params) {
    	url += "&" + Uri.escape(pm) + "=" + Uri.escape(params[pm]) ;
    }
    try {
	    var win = window.open(url, "_blank",
	    "height= " + height + "," +
	    "width= "  + width + ",resizable=yes,status=no,toolbar=no,menubar=no,location=no,scrollbars=no");
	    win.focus();
	} catch(e) {
		alert("Please turn off any pop-up blockers for this website, and try again.");
	}
};

Wiki.prototype.load = function(uri, dupPrefix) {
    if (arrayFind(this.loadedUris, uri) >= 0) return;
    this.loadedUris.push(uri);
    var holdThis = this; //current wiki
    this.loading++;
    this.xb.loadURIAsync(uri, function(doc, arg, xmlHttp) {
            if (doc == null) {
                alert("Error loading knowledgebase");
                return;
            }
            if (App.checkError(doc)) return;
            if (holdThis.doc == null) holdThis.doc = doc;
            if (holdThis.firstArticle == null) {
            	if (!holdThis.config.locked) {
	            	holdThis.config.locked = 
	            		(xmlHttp.getResponseHeader("Can-Write") != "write");
            	}
            }
            holdThis.digest(doc.documentElement, uri, dupPrefix);
            holdThis.loading--;
            if (holdThis.loading <= 0) {
            	holdThis.init(holdThis.startTitle);
            }
        });
};

Wiki.prototype.digest = function(node, uri, dupPrefix) {
    var path = Uri.parent(uri);
    var kids = node.childNodes;
    var inherits = [];
    var ii;
    for (ii = 0; ii < kids.length; ii++) {
        var kNd = kids[ii];
        if (kNd.nodeType == 1 /* ELEMENT */
               && kNd.namespaceURI == "http://itensil.com/xml/knowledgebase") {
            var locNam = Xml.getLocalName(kNd);
            if (        locNam == "inherit") {
                inherits.push(kNd);
            } else if ( locNam == "article") {
                this.addArticle(new WikiArticle(kNd, path), dupPrefix);
            }
        }
        else if (kNd.nodeType == 1 && node.nodeName == "article") /* article */ {
        	    var id = node.getAttribute("id")
        		var art = new WikiArticle(kNd, "article");
        		//TODO -elj- fix article display name into map 
        		//var art = new WikiArticle(kNd, id);
	            this.addArticle(art, dupPrefix);
        }
    }

    // load inherited
    for (ii = 0; ii < inherits.length; ii++) {
        this.load("../fil" + inherits[ii].getAttribute("uri"), "Original: ");
    }
};

Wiki.prototype.addArticle = function(wikiArt, dupPrefix) {
    if (this.firstArticle == null) this.firstArticle = wikiArt;
    while (this.articleIdx[wikiArt.id] != null) {
        wikiArt.id = dupPrefix + wikiArt.id;
    }
    this.articleIdx[wikiArt.id] = wikiArt;
};

Wiki.prototype.getArticle = function(title) {
    return this.articleIdx[title];
};

Wiki.prototype.showArticle = function(art, clearOthers) {
	if (this.views.length == 0) {
        var view = new WikiView(this);
        this.views.push(view);
        view.render(this.viewBox);
    } 
    if (this.views[0].showArticle(art)) {
	    if (clearOthers) {
	        for (var ii = 1; ii < this.views.length; ii++)
	            this.views[ii].reset();
	    }
	    return true;
    }
    return false;
};

Wiki.prototype.frameInit = function(uiParent) {
    this.col1w = 480;
    if (uiParent._panel != null) {
        uiParent.style.overflow = "hidden";
        uiParent._panel.linkResize(this);
    }
    var div, link;
    var holdThis = this;

    this.mast = makeElement(uiParent, "div", "wikiMast");
	
    // search
    if (!this.config.noSearch) {
	    this.searchMast =   makeElement(uiParent, "div", "wikiSearchMast");
	    this.searchText =   makeElement(this.searchMast, "input", "wikiSearchText",
	                        "text", null, { name : "wikiSearch", maxlength : 40});
	    this.searchText.onkeyup = function() {
	            clearTimeout(holdThis.searchKeyTimer);
	            holdThis.searchKeyTimer = setTimeout(function() { holdThis.doSearch(); }, 600);
	        };
	    if (this.config.preSearchCss) {
	    	exAddClass(this.searchText, this.config.preSearchCss);
	    	this.searchText.onfocus = function() {
	    			exAddClass(this, holdThis.config.preSearchCss, true);
	    		};
	    }
	    link =              makeElement(this.searchMast, "a", "wikiS", "Index", null, { href : "#"});
	    link.onclick =      function() { holdThis.showIndex(true); return false;};
	    
	   
	    this.searchRes =    makeElement(uiParent, "div", "wikiSearchRes");
	                        this.searchRes.style.display = "none";
    

	    this.indexSel = new ComboBox();
	    this.indexSel.render(makeElement(this.searchRes, "div", "wikiSel"));
	
	    this.resultsBox =   makeElement(this.searchRes, "div", "wikiResultsBox");
    }

    // content
    this.viewBox =      makeElement(uiParent, "div", "wikiViewBox");

    for (var ii = 0; ii < this.views.length; ii++) {
        this.views[ii].render(this.viewBox);
    }

    this.frameResize(getBounds(uiParent));
    uiParent = null; link = null; div = null; // IE enclosure clean-up
};

Wiki.prototype.frameDispose = function() {
    this.mast = null;
    this.searchMast = null;
    this.searchRes = null;
    this.resultsBox = null;
    if (this.layoutMenu != null) this.layoutMenu.dispose();
    this.layoutMenu = null;
    if (this.simpleMenu != null) this.simpleMenu.dispose();
    this.simpleMenu = null;
    if (this.styleMenu != null) this.styleMenu.dispose();
    this.styleMenu = null;
    if (this.workzoneMenu != null) this.workzoneMenu.dispose();
    this.workzoneMenu = null;
};

Wiki.prototype.hideIndex = function() {
    var vb = this.viewBox;
    var sr = this.searchRes;
   /* var ev = this.editView;
    var eb = this.editBar;
    var ta = this.editBox;
    var et = this.editTitle;*/

    exAddClass(this.searchMast, "wikSearchShow", true);

    var w = this.col1w + 200;
    vb.style.width = w + "px";
    /* ev.style.width =
        ta.style.width =
        eb.style.width =
        et.style.width = (w - 14) + "px";

    */
    sr.style.display = "none";
    var vrect = getBounds(vb);
    vrect.w = w;
    for (var ii = 0; ii < this.views.length; ii++) {
        this.views[ii].resize(vrect);
    }
};

Wiki.prototype.showIndex = function(toggle) {
    /*var eb = this.editBar;
    var ev = this.editView;
    var ta = this.editBox;
    var et = this.editTitle; */
    var vb = this.viewBox;
    var sr = this.searchRes;
    var m = this.mast;
    var sm = this.searchMast;
    var rb = this.resultsBox;
    if (sr.style.display == "none") {
        if (this.currentIndexMode == null) {
            if (this.config.showDate) this.indexMode("byDate");
            else if (this.config.showAuthor)  this.indexMode("byAuthor");
            else this.indexMode(this.getArticle("Table of Contents") ? "toc" : "all");
        }

        var vrect = getBounds(vb);
        vrect.w = this.col1w;
        for (var ii = 0; ii < this.views.length; ii++) {
            this.views[ii].resize(vrect);
        }

        vb.style.width = this.col1w + "px";
        /*
        ev.style.width =
            eb.style.width =
            ta.style.width =
            et.style.width = (this.col1w - 14) + "px";
        */
        sr.style.display = "";
        exAddClass(sm, "wikSearchShow");

    } else if (toggle) {
        this.hideIndex();
    }
};

Wiki.prototype.frameResize = function(rect) {
    var w = rect.w;
    var h = rect.h;

    this.col1w = w - 200;
    if (this.col1w < 14) this.col1w = 14;
    if (w < 14) w = 14;
    var col1w = this.col1w;

    var row2h = h - 20;
    if (row2h < 70) row2h = 70;

    var vb = this.viewBox;
    var sr = this.searchRes;
    var m = this.mast;
    var sm = this.searchMast;
    var rb = this.resultsBox;

    m.style.width = col1w + "px";
    if (sr) {
	    sr.style.left = col1w + "px";
	    sr.style.height = row2h + "px";
	    sm.style.left = col1w + "px";
	    rb.style.height = (row2h - 30) + "px";
    }

    var vw;
    if (!sr || sr.style.display == "none") vw = w;
    else vw = col1w;

    var vrect = new Rectangle(0, 0, vw, row2h);
    for (var ii = 0; ii < this.views.length; ii++) {
        this.views[ii].resize(vrect);
    }
    vb.style.width = vw + "px";
    vb.style.height = row2h + "px";
};

Wiki.prototype.resize = function(rect) {
    this.frameResize(rect);
};

Wiki.prototype.setWindowResize = function() {
    var holdThis = this;
    holdThis.resize(getVisibleBounds());
    addEventHandler(window, "resize",
            function(evt) {
                holdThis.resize(getVisibleBounds());
            });
};

Wiki.prototype.dispose = function() {
    for (var ii = 0; ii < this.views.length; ii++) {
        this.views[ii].dispose();
    }
    this.views = null;
    this.frameDispose();
    if (this.manMenu != null) this.manMenu.dispose();
    this.firstArticle = null;
    this.doc = null;
};

    // TODO - start sync thread
Wiki.prototype.init = function(title) {
    var tocArt = this.getArticle("Table of Contents");
    
    if (tocArt) {
    	this.indexSel.addOption("Table of Contents", "toc", true);
    }
	
	var holdThis = this;
	 
	if (this.indexSel) {
		this.indexSel.addOption("View All Articles", "all", tocArt == null);
	    this.indexSel.addOption("View Articles By Date", "byDate");
	    this.indexSel.addOption("View Articles By Author", "byAuthor");
	    this.indexSel.addSeparator();
	    
	    
	    
	    /* sel.options[sel.options.length] = new Option("Attachments", "attach");
	    sel.options[sel.options.length] = new Option("References", "refer");*/
	    
	    this.indexSel.addOption("View Search Results", "search");
	    
	   
	    this.indexSel.onchange = function() {
		    	holdThis.indexMode(this.getValue());
		    };
	}
	    
	App.unloadListeners.push(function() {
			if (holdThis.views) {
			   	for (var ii=0; ii < holdThis.views.length; ii++) {
	           		if (holdThis.views[ii].isEdit) return true;
			   	}
			}
		   	return false;
        });

    if (title == null || title == "") {
    	if(this.firstArticle) {
        	title = this.firstArticle.id;
    	}
    }
    //this.enableHistory(false);
    //this.trail.push(title);
    var view;
    if (this.views.length == 0) {
        view = new WikiView(this);
        this.views.push(view);
        view.render(this.viewBox);
    } else {
        view = this.views[0];
    }
    if (view.article == null)
        view.goTitle(title);

	if (this.config.showIndex) {
		this.showIndex();
	}
};


// TODO - read XML updates

// post XML
Wiki.prototype.send = function(wikiEditArt, oldId) {
    var doc = this.xb.createWithRoot("kb-save", "");
    var root = doc.documentElement;
    var elem = doc.createElement(oldId ? "modify" : "create");

    elem.setAttribute("id", wikiEditArt.id);
    if (oldId) elem.setAttribute("origId", oldId);
    elem.setAttribute("createBy", wikiEditArt.getAttribute("createBy"));
    elem.setAttribute("createTime", wikiEditArt.getAttribute("createTime"));
    elem.setAttribute("modifyBy", wikiEditArt.getAttribute("modifyBy"));
    elem.setAttribute("modifyTime", wikiEditArt.getAttribute("modifyTime"));
    var refId = wikiEditArt.getAttribute("refId");
    elem.setAttribute("refId", refId ?  refId : "");
    var layo = wikiEditArt.getAttribute("layout");
    elem.setAttribute("layout", layo ?  layo : "");

    elem.appendChild(doc.createTextNode(wikiEditArt.getContent()));

    root.appendChild(elem);
    var res = this.xb.loadURIPost("../kb/save?uri=" + Uri.escape(this.getUri()), doc);
    if (App.checkError(res)) return false;
    if (res == null) {
        alert("Problem saving, please try again...");
        return false;
    }
    return true;
};

Wiki.prototype.getUri = function() {
    var uri = this.uri;
    if (uri.substring(0, 6) == "../fil") return uri.substring(6);
    return uri;
};

Wiki.prototype.enableHistory = function(enabled) {
    this.frameBack.style.display = enabled ? "" : "none";
};

Wiki.prototype.getSearchText = function() {
    if (this.searchText != null) {
        var s = trim(this.searchText.value);
        if (s != "" || s.length >= 2) {
            return s;
        }
    }
    return null;
};

Wiki.prototype.getTargetView = function(isList) {
    if (isList) {
        for (var ii=0; ii < this.views.length; ii++) {
            if (!this.views[ii].isStatic) return this.views[ii];
        }
    }
    return this.views[0];
};

Wiki.prototype.indexMode = function(mode) {
	if (this.config.noSearch) return;
	
    this.currentIndexMode = mode;

    var resBox = this.resultsBox;
    resBox.innerHTML = "";
    var artRoot, arts, artElem, row, aObj, tList;
    var i, a;
    this.indexSel.setValue(mode, true);
    switch (mode) {

     case "refer":

        var title =  this.current();
        title = "[" + title + "]]"; // one start bracket should catch includes
        return this.searchArticles(title, true);


     case "search":

        var searchStr = this.getSearchText();
        if (searchStr) {
            return this.searchArticles(searchStr);
        } else {
            row = document.createElement("div");
            row.className = "row";
            row.appendChild(document.createTextNode("Enter some search words above."));
            resBox.appendChild(row);
        }
        return -1;

     case "byDate":
        tList = [];
        for (a in this.articleIdx) {
            artElem = this.articleIdx[a];
            tList.push([a, DateUtil.parse8601(artElem.getAttribute("modifyTime"), true)]);
        }
        tList.sort(function (a,b) { 
        		if (!b[1]) return a[1] ? -1 : 0;
        		if (!a[1]) return 1;
        		return b[1].getTime() - a[1].getTime(); 
        	});
        wikiShowList(resBox, tList, function (state, item) {
            var t = item[1];
            var days = t ? Math.floor((t.getTime() - (t.getTimezoneOffset() * 60000))/ 86400000) : 1;
            if (!state.days || state.days != days) {
                state.days = days;
                return t ? DateUtil.toLocaleWords(t, false, 6) : "No Date";
            }
            return null;
            }, this);
        artRoot = null; arts = null; artElem = null; resBox = null; // IE enclosure clean-up
        return tList.length;

    case "byAuthor":
        tList = [];
        for (a in this.articleIdx) {
            artElem = this.articleIdx[a];
            tList.push([a, UserTree.getUserName(artElem.getAttribute("modifyBy"))]);
        }
        tList.sort(function (a,b) { if(a[1] == b[1]) return 0; else return a[1] > b[1] ? 1 : -1; });
        wikiShowList(resBox, tList, function (state, item) {
            if (!state.author || state.author != item[1]) {
                state.author = item[1];
                return state.author;
            }
            return null;
            }, this);
        artElem = null; resBox = null; // IE enclosure clean-up
        return tList.length;

	 case "toc":
	 	var tocArt = this.getArticle("Table of Contents");
	 	Wiki.wikify(tocArt.getContent(), makeElement(resBox, "div", "toc"), 
	 		Wiki.createContext(this.getTargetView(true), tocArt, "", false));
	 	return 1;
	 	
     default: // all index
        tList = [];
        for (a in this.articleIdx) {
            tList.push([a]);
        }
        tList.sort(function (a,b) { if(a[0] == b[0]) return 0; else return a[0] > b[0] ? 1 : -1; });
        wikiShowList(resBox, tList, null, this);
        artRoot = null; resBox = null; // IE enclosure clean-up
        return tList.length;
    } 
};

Wiki.prototype.doSearch = function() {
    var searchStr = this.getSearchText();
    var hits = 0;
    if (!searchStr) {
    	if (this.config.preSearchCss) 
    		exAddClass(this.searchText, this.config.preSearchCss);
        hits = -1;
        if (this.currentIndexMode == "search") {
            this.hideIndex();
            this.indexMode("all");
        }
    } else {
   		if (this.config.preSearchCss) 
    		exAddClass(this.searchText, this.config.preSearchCss, true);
       // var sel = document.getElementById("indexSelect");
        this.showIndex(false);
       // setSelectVal(sel, "search");
        hits = this.indexMode("search");
    }
    if (hits > 0 || (hits <= 0 && this.lastHits > 0)){
        this.getTargetView(true).refresh();
    }
    this.lastHits = hits;
};

Wiki.prototype.searchArticles = function(text, refer) {

    var escText = escapeRegExp(text);
    var regEx = new RegExp(escText, "img");
    var context = { highlightRegExp : regEx, wiki : this, wikiView : this.getTargetView(true) };

    var resBox = this.resultsBox;
    resBox.innerHTML = "";
    var hits = 0;
    for (var a in this.articleIdx) {
        var artElem = this.articleIdx[a];
        context.art = artElem;
        var theLink;
        regEx.lastIndex = 0;
        var title = a;
        var row = null;
        if (!refer) {
            var titleRes = regEx.exec(title);
            if (titleRes) {
                row = document.createElement("div");
                row.className = "row";
                theLink = createArticleLink(row, title, context);
                context.highlightMatch = titleRes;
                subWikify(theLink, title, 0, title.length, context);
            }
        }

        // search body
        regEx.lastIndex = 0;
        var content = artElem.getContent();
        var contRes = regEx.exec(content);
        if (contRes) {
            if (!row) {
                row = document.createElement("div");
                row.className = "row";

                theLink = createArticleLink(row, title, context);
                theLink.appendChild(document.createTextNode(title));
                row.appendChild(theLink);
            }

            var rItem = document.createElement("div");
            rItem.className = "ritem";

            row.appendChild(rItem);

            var charCount = 0;
            do {
                // gen summary
                var start = contRes.index;
                var rxLastIndex = regEx.lastIndex;
                var last = rxLastIndex + 40;
                if (start < 40) {
                    start = 0;
                } else {
                    while (start >= 0  &&
                        ("\r\n\f".indexOf(content.charAt(start)) < 0))
                        start--;

                    if (charCount == 0) start++;
                }

                if (last >= content.length) {
                    last = content.length;
                } else {
                    while (last < content.length  &&
                        ("\r\n\f".indexOf(content.charAt(last)) < 0))
                        last++;
                }

                if (charCount > 0) {
                    charCount += 4;
                    rItem.appendChild(document.createTextNode("... "));
                }
                charCount += last - start;
                var cBit = content.substring(start,last);
                Wiki.wikify(cBit.replace(summaryFilterRegExp,""), rItem, Wiki.createContext(context.wikiView, context.art,escText, true));
                regEx.lastIndex = last;
                contRes = regEx.exec(content);

            } while (contRes && charCount < 200);

            if (contRes) {
                rItem.appendChild(document.createTextNode("..."));
            }
            var re = document.createElement("div");
            re.className = "rowEnd";
            rItem.appendChild(re);
        }

        if (row) {
            hits++;
            resBox.appendChild(row);
        }
    }
    if (hits < 1) {
        resBox.innerHTML = "<div class='row'>" +
            (refer ? "No references found" : "No matches found for: "  + text)
            +"</div>";
    }
    return hits;
};


Wiki.prepInsertText = function(tagOpen, tagClose, text) {
    var after = "";
    var before = "";

    if (SH.is_ie) {
       text = text.replace(/\r\n/mg,"\n");
    }

    if (text.length > 1 && ("{}".indexOf(tagOpen.charAt(1)) < 0)) {
        //TODO - real multi line
        var pos = text.indexOf("\n");
        if (pos >= 0) {
            after = text.substring(pos);
            text = text.substring(0, pos);
        }
        if (text.length > 1) {
            while ("*#!>".indexOf(text.charAt(0)) >= 0) {
                before += text.charAt(0);
                text = text.substring(1);
            }
        }
    }

    if (text.charAt(text.length - 1) == " "){// exclude ending space char, if any
		text = text.substring(0, text.length - 1);
		return before + tagOpen + text + tagClose + " " + after;
	} else {
		return before + tagOpen + text + tagClose + after;
	}
};

/**
 * WikiView
 */
function WikiView(wiki, opts) {
    this.wiki = wiki;
    this.trail = [];
    this.disposeList = [];
    this.includeStack = null;
    this.article = null;
    this.editView = null;
    this.canEdit = true;
    this.isEdit = false;
    this.isWysi = false;
    this.isStatic = false;
    this.isFullPage = true;
    if (opts) {
        for (var opt in opts) this[opt] = opts[opt];
    }
}

WikiView.prototype.render = function(uiParent) {
    this.uiParent = uiParent;
    var div, holdThis = this;

    // content
    this.viewSet =      makeElement(uiParent, "div", "wikiViewSet" + (this.cssClass ? " " + this.cssClass : ""));
    div =               makeElement(this.viewSet, "div", "wikiHead");
    this.edLink =       makeElement(div, "div", "wikiEditBtn", "Edit");
                        makeElementNbSpd(this.edLink, "div", "mbIco mb_ediIco");
                        this.edLink.style.display = "none";
    this.edLink.onclick = function() { holdThis.edit(); };

    if (!this.isStatic) {
        this.backLink = makeElement(div, "div", "wikiBackBtn", "Prev");
                        makeElementNbSpd(this.backLink, "div", "mbIco mb_bakIco");
                        this.backLink.style.display = "none";
        this.backLink.onclick = function() { holdThis.goBack(); };
    }

    this.head =         makeElement(div, "span", null);
    this.contentElem =  makeElement(this.viewSet, "div", "wikiView");

    this.resize(getBounds(uiParent));
    uiParent = null; div = null; // IE enclosure clean-up
};

WikiView.initEditLink = function(link, method) {
	setEventHandler(link, "onmousedown", function(evt) { 
			evt.cancelBubble = true; method.apply(this, [evt]); return false;
			});
	elementNoSelect(link);
	link = null; // IE enclosure clean-up
};

WikiView.prototype.renderEdit = function(uiParent) {
    var div, link, holdThis = this;

    // edit
    this.editView =     makeElement(uiParent, "div", "wikiEditView");
                        this.editView.style.display = "none";
    this.editTitle =    makeElement(this.editView, "input", "wikiEditTitle", "text", null, { name : "wikiTitle"});
    this.editBar =      makeElement(this.editView, "div", "wikiEditBar");
    elementNoSelect(this.editBar);
    this.editBox =      makeElement(this.editView, "textarea", "wikiEditBox", null, null, { name : "wikiBody"});
    if (SH.is_ie) {
        this.editBox.onkeyup =
            this.editBox.onselect =
            this.editBox.onmouseup =
            function () {
                this.lastRange = document.selection.createRange().duplicate();
            };
    }
    var dndType = new WikiEditDNDTypeHandler();
    var dnd = dndGetCanvas(document.body);
    dnd.addDNDType(dndType);
    dnd.makeDropTarget(this.editBox, dndType.type);
    this.editBox.wikiView = this;

    this.editBox.style.display = "none";

    this.wev = WikiEdit.createEditFrame(this.editView, this);

    // editbar
    link =              makeElement(this.editBar, "div", "wikiSaveBtn", "Save");
                        makeElementNbSpd(link, "div", "mbIco mb_savIco");
    WikiView.initEditLink(link, function() { holdThis.save(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edBold", null, { title : "Bold" });
    WikiView.initEditLink(link, function() { holdThis.edBold(); });

	

    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edItal", null, { title : "Italics" });
    WikiView.initEditLink(link, function() { holdThis.edItal(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edUnder", null, { title : "Underline" });
    WikiView.initEditLink(link, function() { holdThis.edUnder(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edStrik", null, { title : "Strike" });
    WikiView.initEditLink(link, function() { holdThis.edStrik(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edLink", null, { title : "Link Article" });
    WikiView.initEditLink(link, function() { holdThis.edDoLink(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edUlist", null, { title : "Unordered List" });
    WikiView.initEditLink(link, function() { holdThis.edUlist(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edOlist", null, { title : "Ordered List" });
    WikiView.initEditLink(link, function() { holdThis.edOlist(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edOdent", null, { title : "Outdent" });
    WikiView.initEditLink(link, function() { holdThis.edOdent(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edIdent", null, { title : "Indent" });
    WikiView.initEditLink(link, function() { holdThis.edIdent(); });

  /* link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edHttp", null, { title : "Web Link" });
    link.onclick =      function() { holdThis.insertTags("[attach[", "]]", "http://itensil.com"); return false; };  */

    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edTags", null, { title : "Macro/Style Menu" });
    WikiView.initEditLink(link, function() {
	                    var rect = getBounds(this);
	                    var menu = holdThis.isWorkzone ? holdThis.wiki.workzoneMenu : holdThis.wiki.simpleMenu;
	                    menu.show(rect.x + 1, rect.y + rect.h + 1, null, holdThis); });

    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edPrev", null, { title : "Preview" });
    WikiView.initEditLink(link, function() { holdThis.showPreview(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edMark", null, { title : "Edit Markup" });
    WikiView.initEditLink(link, function() { holdThis.showMarkup(); });
    link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edCanc", null, { title : "Cancel" });
    WikiView.initEditLink(link, function() { holdThis.editCancel(); });

    if (!this.isWorkzone) {
        link =              makeElementNbSpd(this.editBar, "div", "wikEdBtn edRem", null, { title : "Remove Article" });
        WikiView.initEditLink(link, function() { if (confirm("Are you sure?")) holdThis.deleteArticle(); });
    }

    uiParent = null; link = null; div = null; // IE enclosure clean-up
};

WikiView.prototype.setLayout = function(layoutId) {
	
  	// is changed?
  	if (this.article.getAttribute("layout") != layoutId) {
  		
  		// get markup
  		var content;
        if (this.isWysi) {
            content = this.wev.getContent();
        } else {
            content = this.editBox.value;
           	if (SH.is_ie || SH.is_opera) content = WikiTempArt.cleanDosLines(content);
        }
        
        this.article.setAttribute("layout", layoutId);
  		
	  	if (layoutId == "") {
	  		this.wev.hasLayout = false;
	  		// clear ++++
	  		content = content.replace(/\+\+\+\+\n/m, "\n");
	  	} else {
	  		this.wev.hasLayout = true;
	  		// add ++++
	  		if (content.indexOf("++++\n") < 0) {
	  			content += "\n++++\n";
	  		}
	  	}
	  	
	  	// put markup
	  	if (this.isWysi) this.wev.setContent(content);
	  	else this.editBox.value = content;
  	}
};

WikiView.prototype.insertStyle = function(dir, style, sampleText) {
    if (this.isWysi) {
        var klass = ((dir == "right") ?
            "floatRight float_" : "floatLeft float_") + style;
        this.wev.insertHTML("<div class=\"" + klass + "\">" + sampleText + "</div>");
    } else {
        var tag = (dir == "right") ? "\n}}}" : "\n{{{";
        this.insertTags(tag + style + ":\n", tag + "\n", sampleText);
    }
};

WikiView.prototype.insertMacro = function(id, arg) {
    if (this.isWysi) this.wev.insertMacro({id:id, args:[arg]});
    else this.insertTags("[" + id + "[", "]]", arg);
};

WikiView.prototype.edBold = function() {
    if (this.isWysi) this.wev.doCmd("bold");
    else this.insertTags("''", "''", "sample");
};

WikiView.prototype.edItal = function() {
    if (this.isWysi) this.wev.doCmd("italic");
    else this.insertTags("\\\\", "\\\\", "sample");
};

WikiView.prototype.edUnder = function() {
    if (this.isWysi) this.wev.doCmd("underline");
    else this.insertTags("__", "__", "sample");
};

WikiView.prototype.edStrik = function() {
    if (this.isWysi) this.wev.doCmd("strikethrough");
    else this.insertTags("==", "==", "sample");
};

WikiView.prototype.edOlist = function() {
    if (this.isWysi) this.wev.doCmd("insertorderedlist");
    else this.insertTags("\n#", "", "sample");
};

WikiView.prototype.edUlist = function() {
    if (this.isWysi) this.wev.doCmd("insertunorderedlist");
    else this.insertTags("\n*", "", "sample");
};

WikiView.prototype.edOdent = function() {
    if (this.isWysi) this.wev.doCmd("outdent");
    else this.insertTags("\n<<<\n", "\n<<<\n", "Text here.");
};

WikiView.prototype.edIdent = function() {
    if (this.isWysi) this.wev.doCmd("indent");
};

WikiView.prototype.edDoLink = function() {
    if (this.isWysi) this.wev.doCmd("createlink", "#");
    else this.insertTags("[[", "]]", "sample");
};

WikiView.prototype.resize = function(rect) {
    if (this.isFullPage) {
        this.viewSet.style.height = rect.h + "px";
    }
    if (SH.is_ie) {
       // TODO find fix for position:relative moves
    }
    this.sizeEdit();
};

WikiView.prototype.sizeEdit = function() {
    var rect = getLocalBounds(this.uiParent, this.isFullPage ? this.uiParent : this.viewSet);
    if (this.editView != null) {
        var ev = this.editView;
        if (!this.isFullPage) ev.style.top = rect.y + "px";
        var eb = this.editBar;
        var ta = this.editBox;
        var et = this.editTitle;
        var h = rect.h;
        var hMax = this.wiki.views.length > 1 ? 250 : 58;
        if (h < hMax) h = hMax;
        this.wev.__iframe.style.height =
            ta.style.height = (h - 58) + "px";
        this.wev.__iframe.style.width =
            ev.style.width =
            ta.style.width =
            et.style.width =
            eb.style.width = (rect.w - 14) + "px";
    }
};

WikiView.prototype.edit = function() {
    if (this.wev == null) this.renderEdit(this.uiParent);
    this.editTitle.value = this.article.id;
    this.editTitle.readOnly = this.article.lockId ? true : false;
    this.showEdit();
    if (this.article.constructor == WikiTempArt) {
        //this.editBox.value = "New Article...";
        var layout = "2colA";
        if ("layout" in this.article) layout = this.article.layout;
        this.article.setAttribute("layout", layout);
        if (!this.article.timeStampEdit)
        	this.article.setContent("New Article...\n++++\n\n");
        else this.article.setContent("");
        	
    }
    this.wev.setArticle(this.article);
};

WikiView.prototype.showEdit = function() {
    var ev = this.editView;
    var vb = this.viewSet;
    if (this.wiki.views.length == 1)
    	this.uiParent.style.overflow = "hidden";
    if (ev.style.display == "none") {
        if (this.isFullPage) vb.style.display = "none";
        else vb.style.visibility = "hidden";

        ev.style.display = "";
        this.editBox.style.display = "none";
        this.wev.show();
    }
    this.sizeEdit();
    this.isEdit = true;
    this.isPreview = false;
    this.isWysi = true;
};


WikiView.prototype.hideEdit = function() {
    var ev = this.editView;
    var vb = this.viewSet;
    this.uiParent.style.overflow = "";
    if (ev.style.display == "") {
        this.wev.hide();
        vb.style.visibility = "visible";
        if (this.isFullPage) vb.style.display = "";
        else vb.style.visibility = "visible";
        ev.style.display = "none";
    }
    this.isEdit = false;
};

WikiView.prototype.editCancel = function() {
	this.hideEdit();
    this.showArticle(this.article);
};

WikiView.prototype.showMarkup = function() {
    if (this.isPreview) this.showPreview();
    if (this.isWysi) {
        this.isWysi = false;
        this.editBox.value = this.wev.getContent();
        this.wev.hide();
        this.editBox.style.display = "";
    } else {
        this.isWysi = true;
        this.wev.show();
        this.wev.setContent((SH.is_ie || SH.is_opera) ? WikiTempArt.cleanDosLines(this.editBox.value) : this.editBox.value);
        this.editBox.style.display = "none";
    }
};

WikiView.prototype.showPreview = function() {
    var vb = this.viewSet;
    if (this.isPreview) {
        this.isPreview = false;
        if (this.isWysi) {
            this.wev.show();
        } else {
            this.editBox.style.display = "";
        }
        if (this.isFullPage) vb.style.display = "none";
        else vb.style.visibility = "hidden";
        if (this.wiki.views.length == 1)
    		this.uiParent.style.overflow = "hidden";
    } else {
        this.isPreview = true;
        if (this.wiki.views.length == 1)
    		this.uiParent.style.overflow = "";
        var content;
        if (this.isWysi) {
            content = this.wev.getContent();
            this.wev.hide();
        } else {
            content = this.editBox.value;
            this.editBox.style.display = "none";
        }
        if (this.isFullPage) vb.style.display = "";
        else vb.style.visibility = "visible";
        var art = new WikiTempArt(content, this.article.path);
        art.setAttribute("layout", this.article.getAttribute("layout"));
        wikiDrawArticle(this.editTitle.value, "Preview", art, null, this);
    }
};

WikiView.prototype.goTitle = function(title, extras) {
    if (title && (this.article == null || this.linkWiki || title != this.article.id)) {
        if (this.isEdit) {
            title = "[[" + title + "]]";
            if (!this.isWysi) this.insertTags("", title, "\n");
        } else if (this.article != null && this.linkWiki) {

            // TODO support link to already open pop
            var diag = Wiki.dialog(this.linkWiki, title);
            diag.show(300, 100);

        } else {
            this.trail.push(title);
            this.enableHistory(this.article != null);
            var art = this.wiki.getArticle(title);
            if (art == null) {
                art = new WikiTempArt(
                    "This article is currently empty, to create content click edit.",
                     Uri.parent(this.wiki.uri));
                art.id = title;
            }
            if (extras) objectExtend(art, extras);
            this.showArticle(art);
        }
    }
};

WikiView.prototype.enableHistory = function(enable) {
    if (!this.isStatic) {
        this.backLink.style.display = enable ? "" : "none";
    }
};

WikiView.prototype.goBack = function() {
    if (this.trail.length > 1) {
        this.trail.pop();
        this.enableHistory(this.trail.length > 1);
        var title = this.trail[this.trail.length - 1];
        var art = this.wiki.getArticle(title);
        if (art == null) {
            art = new WikiTempArt(
                "This article is currently empty, to create content click edit.",
                 Uri.parent(this.wiki.uri));
            art.id = title;
        }
        this.showArticle(art);
    }
};

WikiView.prototype.refresh = function() {
    this.showArticle(this.article);
};

WikiView.prototype.showArticle = function(art) {
	if (this.isEdit) return false;
	if (this.isStatic || this.isFullPage) this.scrollTop();
    this.article = art;
    if (!art || !this.canEdit || art.editLock || this.wiki.config.locked) {
        this.edLink.style.display = "none";
    } else {
        this.edLink.style.display = "";
    }

    var search = this.wiki.getSearchText();
    if (search && search.length > 1) search = escapeRegExp(search);
    else search = null;

    var meta = "";
    if (art) {
    	//TODO - finish upgrades
        var modifyBy = this.wiki.config.showAuthor ? UserTree.getUserName(art.getAttribute("modifyBy")) : "";
        var modifyTime = this.wiki.config.showDate ? art.getAttribute("modifyTime") : "";
        if (modifyBy && modifyTime) {
            meta = modifyBy + " on " +
                 (new Date(modifyTime)).toLocaleDateString();
        } else if (modifyBy) {
            meta = modifyBy;
        } else if (modifyTime) {
            meta = (new Date(modifyTime)).toLocaleDateString();
        }
        var title = art.getDisplayTitle();
        wikiDrawArticle(title, meta, art, search, this);
    }
    return true;
};

WikiView.prototype.deleteArticle = function() {
    var art = this.article;
    if (art!= null) {

        if (art.constructor != WikiTempArt) {
            var id = art.id;
            // server delete
            var doc = this.wiki.xb.createWithRoot("kb-save", "");
            var root = doc.documentElement;
            var elem = doc.createElement("delete");
            elem.setAttribute("id", id);
            root.appendChild(elem);
            var res = this.wiki.xb.loadURIPost("../kb/save?uri=" + Uri.escape(this.wiki.getUri()), doc);
            if (App.checkError(res)) return false;
            if (res == null) {
                alert("Problem deleting, please try again...");
                return false;
            }
            delete this.wiki.articleIdx[id];
        }
        this.hideEdit();
        if (this.trail.length > 1) {
            this.goBack();
        } else {
            this.reset();
        }
        this.wiki.indexMode(this.wiki.currentIndexMode);
    }
    return true;
};

WikiView.prototype.scrollTop = function() {
    var holdThis = this;
    window.setTimeout(function() {
        var vElem = holdThis.uiParent;
        var vr = getLocalBounds(vElem, vElem);
        var rect = getLocalBounds(vElem, holdThis.viewSet);
        var y = rect.y + 50 - vr.h;
        vElem.scrollTop = y > 0 ? y : 0;
        vElem.scrollLeft = 0;
        }, 20);
};

WikiView.prototype.reset = function() {
    this.article = null;
    this.clear();
    this.trail = [];
    this.edLink.style.display = "none";
    this.enableHistory(false);
};

WikiView.prototype.clear = function() {
    // reset dispose
    for (var ii = 0; ii < this.disposeList.length; ii++) {
        this.disposeList[ii].dispose();
    }
    this.disposeList = [];
    this.head.innerHTML = "";
    this.contentElem.innerHTML = "";
};


WikiView.prototype.save = function() {
    var title = trim(this.editTitle.value);
    if (!title) {
        alert("Title required");
        return false;
    }
    if ((!this.article || this.article.id != title 
    	|| this.article.constructor == WikiTempArt) && this.wiki.getArticle(title)) {
           alert("There is already an article with the title: " + title);
           return false;
    }
    var oldId = null;
    var artNod = null;
    if (this.article.constructor == WikiTempArt) {
        artNode = this.wiki.doc.createElement("article");
        this.wiki.doc.documentElement.appendChild(artNode);
        artNode.setAttribute("createBy", UserTree.getSelfId());
        artNode.setAttribute("createTime", DateUtil.to8601(new Date(), true));
        artNode.setAttribute("id", title);
        var refId = this.article.getAttribute("refId");
        artNode.setAttribute("refId", refId ? refId : "");
        artNode.setAttribute("layout", this.article.getAttribute("layout"));
        this.article = new WikiArticle(artNode, Uri.parent(this.wiki.uri));
    } else {
        oldId = this.article.id;
        delete this.wiki.articleIdx[oldId];
    }

    this.article.setAttribute("modifyBy", UserTree.getSelfId());
    this.article.setAttribute("modifyTime", DateUtil.to8601(new Date(), true));
    this.article.setAttribute("id", title);
    this.article.id = title;
    var content = this.isWysi ? this.wev.getContent() : this.editBox.value;
    this.article.setContent(content);
    if (this.article.constructor == WikiArticle) {
        this.wiki.addArticle(this.article);
    }
    this.trail[this.trail.length - 1] = title;
    this.wiki.indexMode(this.wiki.currentIndexMode);

    // does article object have its own save?
    if (this.article.save != null) {
        this.article.save();
        this.hideEdit();
        this.showArticle(this.article);
        return true;
    } else if (this.wiki.send(this.article, oldId)) {
        this.hideEdit();
        this.showArticle(this.article);
        return true;
    }
    return false;
};


WikiView.prototype.insertTags = function(tagOpen, tagClose, sampleText) {
    if (this.isPreview) return;

	var txtarea = this.editBox;

	// IE
	if (SH.is_ie) {

        var r = null;
        var theSelection = sampleText;
        if (txtarea.lastRange) {
            r = txtarea.lastRange;
            theSelection = r.text;
            if (theSelection.length == 0) theSelection = sampleText;
        }

		txtarea.focus();
		var txt = Wiki.prepInsertText(tagOpen, tagClose, theSelection);
        if (r) {
		    r.text = txt;
		} else {
		    txtarea.value += txt;
		}

	// Mozilla
	} else if (SH.is_gecko) {
 		var startPos = txtarea.selectionStart;
		var endPos = txtarea.selectionEnd;
		var scrollTop = txtarea.scrollTop;
		var myText = (txtarea.value).substring(startPos, endPos);
		if(!myText) { myText=sampleText;}
		var subst = Wiki.prepInsertText(tagOpen, tagClose, myText);
		var tval = txtarea.value;
	    txtarea.value = tval.substring(0, startPos) + subst + tval.substring(endPos, tval.length);
		txtarea.focus();

		var cPos=startPos+(tagOpen.length+myText.length+tagClose.length);
		txtarea.selectionStart=cPos;
		txtarea.selectionEnd=cPos;
		txtarea.scrollTop=scrollTop;

	// All others
	} else {
		txtarea.value += tagOpen + sampleText + tagClose;

		// in Safari this causes scrolling
		if(!SH.is_safari) {
			txtarea.focus();
		}
	}
	// reposition cursor if possible
	if (txtarea.createTextRange) txtarea.caretPos = document.selection.createRange().duplicate();
};

WikiView.prototype.dispose = function() {
    this.viewBox = null;
    this.viewSet = null;
    this.edLink = null;
    this.head = null;
    this.contentElem = null;
    this.editBar = null;
    this.editView = null;
    this.editTitle = null;
    if (this.editBox != null) {
        dndGetCanvas(document.body).disposeDropTarget(this.editBox);
        this.editBox = null;
    }
    if (this.wev != null) {
        this.wev.dispose();
        this.wev = null;
    }
    for (var ii = 0; ii < this.disposeList.length; ii++) {
        this.disposeList[ii].dispose();
    }
    this.disposeList = null;
};


/**
 * WikiArticle
 */
function WikiArticle(node, path) {
    if (arguments.length > 0) {
        this.node = node;
        this.path = path;
        //TODO -elj- clean up node type / id for article
		try {
        	this.id = node.getAttribute("id");
		}
		catch(e) {}
        if(this.id == null) {
        	this.id=path;
        }
     }
}

WikiArticle.prototype.getDisplayTitle = function() {
    return this.titlePrefix ? this.titlePrefix + this.id : this.id;
};

WikiArticle.prototype.editContent = function(txt) {
	if (this.timeStampEdit) {
		var dt = new Date();
		return "''" + UserTree.getUserName(UserTree.getSelfId()) + " - " +
		DateUtil.getShortDay(dt) + ", " + DateUtil.toLocaleShort(dt, true) + 
		":''\n----\nNewEntry\n\n" + txt;
	}
	return txt;
};

WikiArticle.prototype.getContent = function(forEdit) {
	var val = Xml.stringForNode(this.node);
    return forEdit ? this.editContent(val) : val;
};

WikiArticle.prototype.setContent = function(txt) {
    Xml.setNodeValue(this.node, txt);
};

WikiArticle.prototype.getAttribute = function(name) {
    return this.node.getAttribute(name);
};

WikiArticle.prototype.setAttribute = function(name, value) {
    this.node.setAttribute(name, value);
};

WikiArticle.prototype.getPath = function() {
    var path = this.path;
    if (path.substring(0, 6) == "../fil") return path.substring(6);
    return path;
};

WikiArticle.prototype.dispose = function() {
    this.node = null;
};

WikiArticle.prototype.remove = function() {
    this.node.parentNode.removeChild(this.node);
    this.dispose();
};

WikiTempArt.prototype = new WikiArticle();
WikiTempArt.prototype.constructor = WikiTempArt;

function WikiTempArt(txt, path) {
    this.txt = txt;
    this.path = path;
    this.attrs = new Object();
}

WikiTempArt.cleanDosLines = function(txt) {
    return txt.replace(/\r\n/mg,"\n");
};

WikiTempArt.prototype.getAttribute = function(name) {
    return this.attrs[name];
};

WikiTempArt.prototype.setAttribute = function(name, value) {
    if (value !== null) this.attrs[name] = "" + value;
    else this.attrs[name] = null;
};


WikiTempArt.prototype.getContent = function(forEdit) {
	var val = this.txt;
    if (SH.is_ie || SH.is_opera) {
       val = WikiTempArt.cleanDosLines(this.txt);
    }
    return forEdit ? this.editContent(val) : val;
};

WikiTempArt.prototype.setContent = function(txt) {
    this.txt = txt;
};


WikiEditDNDTypeHandler.prototype = new DNDTypeHandler()
WikiEditDNDTypeHandler.prototype.constructor = WikiEditDNDTypeHandler;
function WikiEditDNDTypeHandler() {
    this.type = "wikiDnd";
}

WikiEditDNDTypeHandler.prototype.canDrag = function(dragElem) {
    return false;
};

WikiEditDNDTypeHandler.prototype.dropTest = function(dropElem, dragElem) {
    // file drop
    var type = dragElem._dndType.type;
    if (type == "dndFile" || type == "dndWFActivity" || type == "dndWfAttr" || type == "dndEntForm") {
    	if (type == "dndWfAttr") {
    		if (!dropElem.wikiView.isWorkzone || !dropElem.wikiView.article.step) return false;
    	}
         return true;
    }
    return false;
};

WikiEditDNDTypeHandler.prototype.dropExec = function(dropElem, dragElem) {

    var type = dragElem._dndType.type;
    var wikiView = dropElem.wikiView;

    // file drop
    if (type == "dndFile") {
        var dragItem = dragElem._actElem.__item;

        var wUri = Uri.parent(wikiView.wiki.getUri());
        var uri = Uri.localize(wUri, dragItem.uri);
        var tpos = uri.indexOf("template/");
        if (wikiView.isWorkzone && tpos >= 0)
            uri = "{activity}" + uri.substring(tpos + 8);
		if (wikiView.isWorkzone && App.resolver.modelUri && uri.indexOf(App.resolver.modelUri) >= 0)
        	 uri = "{model}" + uri.substring(App.resolver.modelUri.length);

        var tag;
        var ext = Uri.ext(uri).toLowerCase();
        if (dragItem.allowsKids) {
        	tag = "[attachEdit[" + uri + "/]]";
        } else if (wikiView.isWorkzone && uri == "activities.kb") {
            tag = "[kb[{activity}]]";
        } else if ("gif jpg jpeg png".indexOf(ext) >= 0) {
            tag = "[img[" + uri + "]]";
        } else if ("swf mov avi wav mp3".indexOf(ext) >= 0 || uri.substring(uri.length - 11) == "_config.xml") {
            tag = "[media[" + uri + "|400x300]]";
        } else if (ext == "flow") {
            uri = Uri.parent(uri);
            if (uri == "") uri = wUri;
            if (wikiView.isWorkzone) tag = "[launchSub[" + uri + "]]";
            else tag = "[launch[" + uri + "]]";
        } else if (ext == "xfrm") {
            tag = "[form[" + uri + "]]";
        } else {
            tag = "[attachEdit[" + uri + "]]";
        }
        wikiView.insertTags("", tag, "");
    } else if (type == "dndWFActivity") {
        var dragItem = dragElem._actElem.__item;
        tag = "[activity[" + dragItem.node.getAttribute("id") + "]]";
        wikiView.insertTags("", tag, "");
    } else if (type == "dndWfAttr") {
        tag = "[input[" + WfAttrDNDType.getName(dragElem) + "]]";
        wikiView.insertTags("", tag, "");
    } else if (type == "dndEntForm") {
    	tag = "[entity[" + EntFormDropDNDType.getEntityName(dragElem) + "]]";
    	wikiView.insertTags("", tag, "");
    }
}; 

function wikiLink() {
    var wikiView = this._wikiView;
    var wiki = wikiView.wiki;
    var title = this.getAttribute("article");
    if (wiki.views[0].isEdit) {
        if (!wiki.views[0].isWysi) wiki.views[0].insertTags("", "[[" + title + "]]", "");
    } else if (wikiView.isEdit) {
        if (!wikiView.isWysi) wikiView.insertTags("", "[[" + title + "]]", "");
    } else {
        wikiView.goTitle(title);
    }
    return false;
}

WikiAttachListMenuModel.prototype = new MenuModel();
WikiAttachListMenuModel.prototype.constructor = WikiAttachListMenuModel;

function WikiAttachListMenuModel(uri, ext, editMode, target) {
    this.uri = uri;
    this.ext = ext;
    this.editMode = editMode;
    this.target = target;
}

WikiAttachListMenuModel.prototype.onReady = function(callback, menu) {
    var holdThis = this;
    var absUri = this.uri.charAt(0) == "/" ? this.uri : wikiConfig.path + "/" + this.uri;
    xmlLoadAsync("/shell/listXML?uri=" + encodeURIComponent(absUri)
            + "&time=" + (new Date()).getTime(),
            function (xmlDoc) {
                holdThis.xmlReady(xmlDoc, callback, menu);
            });
};

WikiAttachListMenuModel.prototype.xmlReady = function(xmlDoc, callback, menu) {
    var root = xmlDoc.documentElement;
    var kids = root.childNodes;
    var items = [];
    this.items = items; // for dispose
    for (var i = 0; i < kids.length; i++) {
        var kid = kids[i];
        if (kid.nodeType == 1) {

            if (kid.nodeName == "node") {
                var kUri = kid.getAttribute("uri");
                var name;
                var slash = this.uri.indexOf("/");
                if (slash > 0 && slash < (this.uri.length - 1)) {
                    name = kUri.substring(kUri.lastIndexOf("/", kUri.lastIndexOf("/") - 1) + 1);
                } else {
                    name = kUri.substring(kUri.lastIndexOf("/") + 1);
                }
                var theLink = makeElement(null, "a", null, name);
                var href = wikiEx_attachHref(kUri, this.ext, this.editMode, this.context);
                theLink.href = href;
                theLink.target = this.target;
                items.push({ labelElement : theLink });

            } else if (kid.nodeName == "error") {

                items.push(
                    {   label : xmlGetElementText(kid),
                        isNote : true });
            }
        }
    }
    if (items.length < 1) {
         items.push(
                    {   label : "No matching documents...",
                        isNote : true });
    }
    callback.apply(menu, [items]);
};


WikiReportListMenuModel.prototype = new MenuModel();

function WikiReportListMenuModel(uri) {
    this.uri = uri;
}

WikiReportListMenuModel.prototype.onReady = function(callback, menu) {
    var holdThis = this;
    var absUri = this.uri.charAt(0) == "/" ? this.uri : wikiConfig.path + "/" + this.uri;
    xmlLoadAsync("/report/listPast?uri=" + encodeURIComponent(absUri),
            function (xmlDoc) {
                holdThis.xmlReady(xmlDoc, callback, menu);
            });
};

WikiReportListMenuModel.prototype.xmlReady = function(xmlDoc, callback, menu) {
    var root = xmlDoc.documentElement;
    var kids = root.childNodes;
    var items = [];
    this.items = items; // for dispose
    for (var i = 0; i < kids.length; i++) {
        var kid = kids[i];
        if (kid.nodeType == 1) {
            if (kid.nodeName == "file") {
                var kUri = kid.getAttribute("uri");
                var name = Uri.name(kUri);
                items.push(
                    {   label : name,
                        uri : kUri,
                        act : wikiEx_reportClick
                    });

            } else if (kid.nodeName == "error") {
                items.push(
                    {   label : xmlGetElementText(kid),
                        isNote : true });
            }
        }
    }
    if (items.length > 0) {
        items.push({ isSep : true });
    }
    items.push({
        label : "Generate Report",
        act : wikiEx_repGenClick,
        uri : this.uri });
    callback.apply(menu, [items]);
};


///////////// HELPERS /////////////
function trim(str) {
    if (!str) return str;
    var out = str;
    while (out.length > 0 && " \n\r\t\f\u00a0".indexOf(out.charAt(out.length - 1)) >= 0) out = out.substring(0, out.length - 1);
    while (out.length > 0 && " \n\r\t\f\u00a0".indexOf(out.charAt(0)) >= 0) out = out.substring(1);
    return out;
}

///////////// HELPERS /////////////


function wikiGetArticleSubtitle(title, context) {
    var wikiView = context.wikiView;
    if (wikiView.linkWiki) {
        return "Link to: " + title;
    }
    var wiki = context.wiki;
    var art = wiki.getArticle(title);
    if (art != null) {
        var meta = "Article: " + title;
        var modifyBy = wiki.config.showAuthor ? "By " + art.getAttribute("modifyBy") : "";
        var modifyTime = wiki.config.showDate ? art.getAttribute("modifyTime") : "";
        if (modifyBy && modifyTime) {
            meta = modifyBy + " on " +
                 (new Date(modifyTime)).toLocaleDateString();
        } else if (modifyBy) {
            meta = modifyBy;
        } else if (modifyTime) {
            meta = (new Date(modifyTime)).toLocaleDateString();
        }
        return meta;
    }
    return null;
}

function createArticleLink(place, title, context) {
    var subTitle = wikiGetArticleSubtitle(title, context);
    var theClass = subTitle ? "exists" : "missing";
    if(!subTitle)
        subTitle = title + " doesn't yet exist";

    var theLink = document.createElement(context.isSummary ? "div" : "a");
    if (!context.isSummary) {
        theLink._wikiView = context.wikiView;
        theLink.onclick = wikiLink;
        theLink.title = subTitle;
        theLink.setAttribute("article", title);
        theLink.className = theClass;
        theLink.href = "#" + title;
    }
    place.appendChild(theLink);

    return theLink;
}

function wikiDrawArticle(title, meta, art, search, wikiView) {
    wikiView.clear();
    // reset include stack
    wikiView.includeStack = [title];
    wikiView.head.innerHTML = "<h1>" + title + "</h1><div class='meta'>" +
         meta + "</div>";

    var dest = wikiView.contentElem;
    if (art) {
        Wiki.wikify(art.getContent(), dest, Wiki.createContext(wikiView, art, search));
    } else {
        dest.innerHTML = "This article is currently empty, to create content click edit.";
    }
}




// tlist[x][0] is title
function wikiShowList(resBox, tList, groupFunc, wiki) {

    var groupState = new Object();
    var context = { wiki : wiki, wikiView : wiki.getTargetView(true) };
    for (var i = 0; i < tList.length; i++) {
        var item = tList[i];
        if (groupFunc) {
            var gLabel = groupFunc(groupState, item);
            if (gLabel) {
                var gRow = document.createElement("div");
                gRow.className = "group";
                gRow.appendChild(document.createTextNode(gLabel));
                resBox.appendChild(gRow);
            }
        }
        var row = document.createElement("div");
        row.className = "row";
        var title = item[0];
        var theLink = createArticleLink(row, title, context);
        theLink.className = "index";
        theLink.appendChild(document.createTextNode(title));
        resBox.appendChild(row);
    }
}

function wikiCodeSyntax(code,parent,text,targetText,startPos,endPos,context) {

    if (!code) {
        return subWikify(parent,text,startPos,endPos,context);
    }

    // The start of the fragment of the text being considered
    var nextPos = 0;
    var syntaxRegExp = new RegExp(
        "(" + "^ *\\*+|^ *#+|^ *!+|^ *\\|+|%%%|<<<|\\}\\}\\}|\\{\\{\\{" +
        ")|(" + "''|__|\\\\\\\\|~~(?!~~)|==|@@" +
        ")|(" + "\\[[A-Za-z_0-9 ]*\\[[^\\n]+\\]\\]" +
        ")|(" + "----|~~~~" +
        ")", "mg");
    do {
        // Get the next formatting match
        var formatMatch = syntaxRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;

        var rxLastIndex = syntaxRegExp.lastIndex;

        // Subwikify the plain text before the match
        if(nextPos < matchPos)
            context.highlightMatch = subWikify(parent,text,startPos+nextPos,startPos+matchPos,context);
        // Dump out the formatted match
        if(formatMatch)
            {
            var theSpan = document.createElement("span");

            if(formatMatch[1])
                {
                    theSpan.className = "syntax1";
                    theSpan.appendChild(document.createTextNode(formatMatch[1]));
                }
            else if(formatMatch[2])
                {
                    theSpan.className = "syntax2";
                    theSpan.appendChild(document.createTextNode(formatMatch[2]));
                }
            else if(formatMatch[3])
                {
                    theSpan.className = "syntax3";
                    theSpan.appendChild(document.createTextNode(formatMatch[3]));
                }
            else if(formatMatch[4])
                {
                    theSpan.className = "syntax4";
                    theSpan.appendChild(document.createTextNode(formatMatch[4]));
                }
            parent.appendChild(theSpan);
            }

        // Move the next position past the formatting match
        nextPos = syntaxRegExp.lastIndex = rxLastIndex;
    } while(formatMatch);

    return context.highlightMatch;
}

function wikiRegisterExt(tag, func) {
    Wiki.exts[tag] = func;
}

function wikiExtension(parent, context, id, args) {
    var weObj = WikiExUtil.getExt(id);
    if (weObj) {
        args.unshift(context);
        if (context.wev) {
            var macro = weObj.wysMacro(args);
            weObj.wysRender(parent, context.wev, macro);
            parent.appendChild(document.createTextNode(" "));
        } else {
            args.unshift(parent);
            weObj.render.apply(weObj, args);
        }
    }
}

///
///////////////////////////// EXTENSIONS //////////////////////////////////////
///

var WikiExUtil = {
    resolveUri : function(resUri) {
    	resUri.uri = App.resolver.resolveUri(resUri.uri);
    	if (App.resolver.isTemplate) resUri.suffix = "<t>";
    },

    getExt : function(id) {
        return Wiki.exts[id.toLowerCase()];
    }
};


var WikiExCommon = {
    wysMacro : function(margs) {
        var macro = {id:this.id, args:[]};
        for (var ii=1; ii < margs.length; ii++) {
            macro.args.push(margs[ii]);
        }
        return macro;
    },

    wysRender : function(parent, wev, macro) {
        var elem = makeElement(parent, "button", "iten_emac", this.wysDisplayText(macro));
        elementNoSelect(makeElementNbSpd(elem, "div", "badge em_" + macro.id));
        elem.setAttribute("extid", macro.id);
        elem.setAttribute("args", macro.args.join("|"));
    },

    wysDisplayText : function(macro) {
        var txt = macro.args.length > 1 ? macro.args[1] : macro.args[0];
        if (!txt) return macro.args[0];
        return txt;
    },

    wysPropDialog : function(argStr) {
        var args = argStr.split("|");
        var diag = new Dialog(this.id + " properties", true);
        diag.wexObj = this;
        diag.initHelp(App ? App.chromeHelp : null);
        diag.render(document.body);
        var pDiv = makeElement(diag.contentElement, "div", "wysPropDiag");
        var tbody = makeLayoutTable(pDiv, "wysProps");
        diag.elements = [];
        for (var ii = 0; ii < this.propLabels.length; ii++) {
            diag.elements.push(this.wysPropsRenderRow(tbody, ii, this.propLabels[ii], args[ii]));
        }
        // TODO dispose elements
        var btn = makeElement(pDiv, "button", "save", "Save");
        btn.onclick = this.wysPropSave;
        btn._diag = diag;
        return diag;
    },

    wysPropSave : function() {
        var diag = this._diag;
        var wexObj = diag.wexObj;
        var macro = {id:wexObj.id, args:[]};
        for (var ii = 0; ii < diag.elements.length; ii++) {
            macro.args[ii] = diag.elements[ii].value;
        }
        diag.wexElem.setAttribute("args", macro.args.join("|"));
        replaceElementText(diag.wexElem, wexObj.wysDisplayText(macro));
        diag.wexElem = null;
        diag.destroy();
    },

    wysPropsRenderRow : function(tbody, argIdx, label, value) {
        var row = makeElement(tbody, "tr");
        makeElement(row, "td", "label", label + ":");
        var inp = makeElement(makeElement(row, "td", "field"), "input", "text", "text", null, {name:("arg" + argIdx)})
        inp.value = (value == null) ? "" : value;
        return inp;
    },

    markup : function(argStr) {
        return "[" + this.id + "[" + argStr + "]]";
    }
};


var WikiEx_revisions = {
    id : "revisions",
    render : function(parent, context, uri) {
        var div = document.createElement("div");
        div.className = "revision";
        div.setAttribute("revLabel", trim(uri));
        if (!context.isSummary) {
            div.appendChild(document.createTextNode("Loading..."));
            xmlLoadAsync("/shell/statusXML?uri=" + encodeURIComponent(wikiConfig.path + "/" + uri)
                + "&time=" + (new Date()).getTime(),
                WikiEx_revisions.callback, div);
        } else {
            div.appendChild(document.createTextNode("Revision history for: " + uri));
        }
        parent.appendChild(div);
    },

    propLabels : ["Path"],

    callback : function(xmlDoc, div) {
        if (xmlDoc && div) {
            div.removeChild(div.firstChild);

            var root = xmlDoc.documentElement;
            var uri = root.getAttribute("uri");

            // file head
            var theTable = document.createElement("table");
            var tHead = document.createElement("tHead");
            var tr = document.createElement("tr");
            var cell = document.createElement("th");
            cell.setAttribute("colSpan", 3);
            cell.setAttribute("colspan", 3);

            WikiEx_attach.attachRegExp.lastIndex = 0;
            var matches = WikiEx_attach.attachRegExp.exec(uri);

            var ico = document.createElement("img");
            var ext = matches[3].toLowerCase();
            var iSrc = mimeExtIcos[ext];
            if (!iSrc) iSrc = mimeExtIcos._default;
            ico.src = wikiConfig.iconPath + iSrc;
            ico.className = "fico";
            ico.align = "absmiddle";
            cell.appendChild(ico);
            cell.appendChild(document.createTextNode(div.getAttribute("revLabel")));

            tr.appendChild(cell);
            tHead.appendChild(tr);
            theTable.appendChild(tHead);

            // col heads
            var tBody = document.createElement("tBody");
            tr = document.createElement("tr");
            cell = document.createElement("th");

            cell.appendChild(document.createTextNode("#"));
            tr.appendChild(cell);
            cell = document.createElement("th");
            cell.appendChild(document.createTextNode("Who"));
            tr.appendChild(cell);
            cell = document.createElement("th");
            cell.appendChild(document.createTextNode("At"));
            tr.appendChild(cell);
            tBody.appendChild(tr);

            var count = 0;
            var kids = root.childNodes;
            for (var i = 0; i < kids.length; i++) {
                var kid = kids[i];
                if (kid.nodeType == 1) {
                    tr = document.createElement("tr");
                    if (kid.nodeName == "version") {
                        tr.className = "version";
                        if (count > 4) {
                            cell = document.createElement("td");
                            cell.setAttribute("colSpan", 3);
                            cell.setAttribute("colspan", 3);
                            cell.appendChild(
                                document.createTextNode("..."));
                            tr.appendChild(cell);
                            tBody.appendChild(tr);
                            break;
                        }
                        cell = document.createElement("td");
                        cell.appendChild(
                            document.createTextNode(kid.getAttribute("number")));
                        if (kid.getAttribute("default") == "true") {
                            cell.className = "default";
                        }
                        tr.appendChild(cell);
                        cell = document.createElement("td");
                        cell.appendChild(
                            document.createTextNode(kid.getAttribute("modifier")));
                        tr.appendChild(cell);
                        cell = document.createElement("td");
                        cell.appendChild(
                            document.createTextNode(kid.getAttribute("time")));
                        tr.appendChild(cell);
                        count++;
                    } else if (kid.nodeName == "lock") {
                        cell = document.createElement("td");
                        cell.setAttribute("colSpan", 3);
                        cell.setAttribute("colspan", 3);
                        cell.appendChild(
                            document.createTextNode(
                            "Checked out by: " + kid.getAttribute("user")));
                        tr.appendChild(cell);
                        tBody.appendChild(tr);
                    } else if (kid.nodeName == "error") {
                        tr.className = "error";
                        cell = document.createElement("td");
                        cell.setAttribute("colSpan", 3);
                        cell.setAttribute("colspan", 3);
                        cell.appendChild(
                            document.createTextNode(xmlGetElementText(kid)));
                        tr.appendChild(cell);
                        tBody.appendChild(tr);
                        break;
                    }
                    tBody.appendChild(tr);
                }
            }
            theTable.appendChild(tBody);
            div.appendChild(theTable);
        }
    }
};
objectExtend(WikiEx_revisions, WikiExCommon);


var WikiEx_form = {
    id : "form",
    render : function(parent, context, uri, varSets) {
        var div = posRelIEFix(makeElement(parent, "div", "form"));
        if (!context.isSummary) {
            var resUri = {uri : uri, suffix : ""};
        	var artPath = context.art.getPath();
       		WikiExUtil.resolveUri(resUri);
       		WikiEx_form.renderForm(div, context, 
       			"../fil" + (resUri.uri.charAt(0) == "/" ? resUri.uri : artPath + "/" + resUri.uri),
       			null, varSets);
        } else {
            div.appendChild(document.createTextNode("Form: " + uri));
        }
        
    },
    
    renderForm : function(parent, context, formUri, dataUri, varSets, uriResolver) {
    	var art = context.art;
        var wikiView = context.wikiView;
        var wiki = context.wiki;
        parent.appendChild(document.createTextNode("Loading..."));
        wiki.xb.loadURIAsync(formUri,
            function(doc) {
                if (doc == null || doc.documentElement == null) {
                    parent.removeChild(parent.firstChild);
                    parent.appendChild(document.createTextNode(
                        "[Form '" + formUri + "' not available]"));
                } else {
                    var xf = new XForm(
                        doc, "xf_" + wikiView.disposeList.length, wiki.xb, formUri);
                    if (dataUri) xf.setDefaultUris(dataUri, dataUri);
                    if (uriResolver) xf.setUriResolver(uriResolver);
                    if (App.activeActivityId && App.activeActivityNode) {
                    	xf.setVarString("activityId", App.activeActivityId);
                    	xf.setVarString("activityName", App.activeActivityNode.getAttribute("name"));
                    	xf.setVarString("activityDescription", App.activeActivityNode.getAttribute("description"));
                    	xf.setVarString("activityOwner", App.activeActivityNode.getAttribute("submitId"));
                    } else {
                    	xf.setVarString("activityId", "<null>");
                    	xf.setVarString("activityName", "");
                    	xf.setVarString("activityDescription", "");
                    	xf.setVarString("activityOwner", "");
                    }
                    if (varSets) {
                    	var pairs = varSets.split(",");
                    	for (var ii = 0; ii < pairs.length; ii++) {
                    		var nm = pairs[ii].split("=");
                    		if (nm.length == 2) {
                    			xf.setVarString(nm[0], nm[1]);
                    		}
                    	}
                    }
                        
                    wikiView.disposeList.push(xf);
                    if (parent.firstChild) parent.removeChild(parent.firstChild);
                    xf.render(parent);
                }
                parent = null; // IE enclosure clean-up
            });
    },
    
    wysDisplayText : function(macro) {
            return macro.args[0];
    },

    propLabels : ["Path", "Value Sets"]
};
objectExtend(WikiEx_form, WikiExCommon);


var WikiEx_include = {
    id : "include",
    frameRegEx : new RegExp("(http|https):|/.+\\.(html|jsp)", ""),
   
    render : function(parent, context, title, size) {
        var wiki = context.wiki;
        var div = document.createElement("div");
        div.className = "include";
        if (!context.isSummary) {
        	var fmat = WikiEx_include.frameRegEx.exec(title);
        	// title
        	if (fmat) {
        		if (!fmat[1] && fmat[2] != "jsp") {
        			var resUri = {uri : title, suffix : ""};
        			WikiExUtil.resolveUri(resUri);
        			var artPath = context.art.getPath();
                	title = "../fil" + escape(resUri.uri.charAt(0) == "/" ? resUri.uri : artPath + "/" + resUri.uri);
        		}
        		var wh = null;
        		if (size && WikiEx_media.dimRegExp.test(size)) {
                	wh = size.toLowerCase().split("x");
            	}
            	var ifElem = makeElement(parent, "iframe", "wiki", null, null,
		         	{
		            src : (fmat[2] == "jsp" ? ".." : "") + title,
		            frameborder : "0",
		            border : "0"
		         	} );
		        if (wh) {
		       		ifElem.style.width = wh[0] + "px";
		       		ifElem.style.height = wh[1] + "px";
		        }
        	} else if (arrayFind(context.wikiView.includeStack, title) < 0) {
                context.wikiView.includeStack.push(title);
                var art = wiki.getArticle(title);
                if (art != null) {
                    Wiki.wikify(art.getContent(), div, Wiki.createContext(context.wikiView, art,
                        context.highlightRegExp ? context.highlightRegExp.source : ""));
                } else {
                    div.appendChild(document.createTextNode("Include: "));
                    var lnk = createArticleLink(div, title, context);
                    lnk.appendChild(document.createTextNode(title));
                }
                context.wikiView.includeStack.pop();
            }
        } else {
           var label = "Include: " + title;
            if (context.highlightRegExp)
                context.highlightMatch = context.highlightRegExp.exec(label);
            context.highlightMatch = subWikify(div, label, 0, label.length, context);
        }
        parent.appendChild(div);
    },

    propLabels : ["Article/Url", "Size (WxH)"],
    
    wysDisplayText : function(macro) {
            return macro.args[0];
    }
};
objectExtend(WikiEx_include, WikiExCommon);


var WikiEx_attach = {
    id : "attach",
    render : function(parent, context, uri, label) {
        if (uri) {
            if (!context.isSummary) {
                var aObj = {uri : uri, label : label, edit: false};
            }
            this.draw(parent, context, uri, label, false);
        }
    },

    attachRegExp : new RegExp("((?:http|https|ftp|file):/)|(.+/$)|(?:[^\\?#]+\\.([A-Za-z_0-9\\-]{1,6})(?:[\\?#].+)?$)", "i"),

    draw : function(parent, context, uri, label, editMode) {
        var theLink = makeElement(parent, context.isSummary ? "div" : "a", "attach");
        uri = trim(uri);
        var ext = null;
        var wiki = context.wiki;
        var resUri = {uri : uri, suffix : ""};
        WikiExUtil.resolveUri(resUri);
        uri = resUri.uri;
        theLink.title = (editMode ? "Edit: " : "Attachment: ") + uri;

        this.attachRegExp.lastIndex = 0;
        var matches = this.attachRegExp.exec(uri);
        var ico;
        if (matches) {
            ico = makeElement(theLink, "div", "icon");
            if (matches[1]) {
                ico.className += " linkIco";
                theLink.target = "_blank";
                theLink.href = uri;
            } else {
                 if (matches[2]) {
                    ico.className += " fldIco";
                } else {
                	
                    ext = Uri.ext(uri).toLowerCase();
                    ico.className += " " + ext + "_Ico";
                    //if (!(editMode && !(ext in FileTree.mimeNewWin)) && (ext in FileTree.mimeNewWin))
                    //    theLink.target = "_blank";
                }
                theLink.href = "#";
                var artPath = context.art.getPath();
                var absUri = uri.charAt(0) == "/" ? uri : artPath + "/" + uri;

                if (uri.indexOf('*') >= 0){
                    theLink.onclick = function () {
                        alert('Wildcard not yet supported.');
                        return false;
                    };
                   // theLink.href = "#";
                   /* theLink._menu = new Menu(
                        new WikiAttachListMenuModel(
                            uri, ext, editMode, theLink.target));
                    wiki.disposeList.push(theLink._menu);
                    theLink.target = "";
                    setEventHandler(theLink, "onclick",
                        function(evt) {
                            this._menu.popUp(evt);
                            return false;
                        }); */
                } else {
                    var item = {uri : absUri};
                    if (editMode) {
                        setEventHandler(theLink, "onclick",
                            function(evt) {
                                FileTree.edit(evt, item)
                                return false;
                            });
                    } else {
                        setEventHandler(theLink, "onclick",
                            function(evt) {
                                FileTree.view(evt, item)
                                return false;
                            });

                    }
                }
            }
            if (uri.indexOf("{activity") == 0 || uri.indexOf("{item") == 0) {
                theLink.onclick = function () {
                        alert('This document will be available for an active work item.');
                        return false;
                    };
            }
        }

        if (!label) label = uri;
        label += resUri.suffix;
        if (context.highlightRegExp)
           context.highlightMatch = context.highlightRegExp.exec(label);

        context.highlightMatch = subWikify(theLink, label, 0, label.length, context);

        parent = null; theLink = null; ico = null; // IE enclosure clean-up
    },

    propLabels : ["Path", "Label"]
};
objectExtend(WikiEx_attach, WikiExCommon);


var WikiEx_attachEdit = {
    id : "attachEdit",
    render : function(parent, context, uri, label) {
        if (uri) {
            if (!context.isSummary) {
                var aObj = {uri : uri, label : label, edit: true};
            }
            this.draw(parent, context, uri, label, true);
        }
    }
};
objectExtend(WikiEx_attachEdit, WikiEx_attach);


var WikiEx_media = {
    id : "media",

    dimRegExp : new RegExp("\\d+[xX]\\d+", ""),
    netRegExp :  new RegExp("^((?:http|https|ftp|file):/)", ""),
    mediaRegExp : new RegExp("(?:([^\\?#]+)\\.([A-Za-z_0-9\\-]{1,6})(?:[\\?#].+)?$)", ""),

    render : function(parent, context, puri, size, type) {
        this.mediaRegExp.lastIndex = 0;
        var uri = puri;
        var matches = this.mediaRegExp.exec(uri);
        var wh = null;
		var isNet = this.netRegExp.test(uri);
		if (!isNet) {
	        var artPath = context.art.getPath();
	        var resUri = {uri : uri, suffix : ""};
	        WikiExUtil.resolveUri(resUri);
	        uri = resUri.uri.charAt(0) == "/" ? resUri.uri : artPath + "/" + resUri.uri;
		}

        if (matches || type) {
        	
            var ext = type || matches[2].toLowerCase();
            if (ext == "xml") {
                ext = "swf";
                var camUri = "../view-repo/cam31_controller.swf?csConfigFile=../view-repo/cam31_conf_xml.jsp";
                var camQs = "?url=" + Uri.escape(uri);
                uri = camUri + encodeURIComponent(camQs);
                matches[1] = matches[1].substring(0, matches[1].lastIndexOf("_config"));
            } else if (!isNet && ext == "flv") {
            	ext = "swf";
            	uri = "../view-repo/scrubvid.swf?svcURL=../view-repo/flv.jsp&vidName=" + Uri.escape(puri);
            }  else if (!isNet && ext == "mp3") {
            	ext = "swf";
            	uri = "../view-repo/scrubaud.swf?mp3Name=../fil" + Uri.escape(uri);
            	size = "376x38";
            } else if (!isNet) {
                uri = "../fil" + uri;
            }

            if (context.isSummary) {
                var mediaSum = document.createElement("span");
                mediaSum.className = "media";
                makeElement(mediaSum, "div", "icon " + ext + "_Ico");
                var label = "media: " + matches[1];
                if (context.highlightRegExp)
                    context.highlightMatch = context.highlightRegExp.exec(label);
                context.highlightMatch = subWikify(mediaSum, label, 0, label.length, context);
                parent.appendChild(mediaSum);
                return;
            }

            if (size && this.dimRegExp.test(size)) {
                wh = size.toLowerCase().split("x");
            }

            var oe;
            if (ext == "swf") {
                if (SH.is_ie) {
                    oe = document.createElement("object");
                    oe.setAttribute("classid", "CLSID:d27cdb6e-ae6d-11cf-96b8-444553540000");
                    oe.setAttribute("codeBase", "http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=8,0,0,0");
                    if (wh) {
                        oe.width = wh[0];
                        oe.height = wh[1];
                    }
                    parent.appendChild(oe);
                    oe.movie = uri;
                    oe = null;
                } else {
                    oe = document.createElement("embed");
                    oe.type = "application/x-shockwave-flash";
                    oe.src = uri;
                }
            } else {
                oe = document.createElement("embed");
                oe.src = uri;
            }
            if (oe) {
                if (wh) {
                    oe.width = wh[0];
                    oe.height = wh[1];
                }
                parent.appendChild(oe);
            }
        }
    },

    propLabels : ["Path", "Size (WxH)", "Type"]
};
objectExtend(WikiEx_media, WikiExCommon);


var WikiEx_img = {
    id : "img",
    render : function(parent, context, src, alt, align, size) {
        var art = context.art;
        var theImage = makeElement(parent, "img");
        var resUri = {uri : src, suffix : ""};
        var artPath = context.art.getPath();
        WikiExUtil.resolveUri(resUri);
        src = resUri.uri;
        theImage.src = "../fil" + (src.charAt(0) == "/" ? src : artPath + "/" + src);
        if (alt) {
            theImage.alt = alt + resUri.suffix;
        } else {
            theImage.alt = resUri.suffix;
        }
        if (context.highlightRegExp && (context.highlightMatch = context.highlightRegExp.exec(src + "|" + alt))) {
            theImage.className = "highlight";
        } else if (size && WikiEx_media.dimRegExp.test(size)) {
            var wh = size.toLowerCase().split("x");
            if (wh.length == 2) {
                theImage.width = wh[0];
                theImage.height = wh[1];
            }
        }
        if (align) {
            theImage.align = align;
        }
        return theImage;
    },

    propLabels : ["Path", "Alt Text", "Align (left,right)", "Size (WxH)"],

    wysRender : function(parent, wev, macro) {
        var context = Wiki.createContext(wev.wikiView, wev.article);
        var img = this.render(parent, context, macro.args[0], macro.args[1], macro.args[2], macro.args[3]);
        img.setAttribute("argSrc", macro.args[0]);
    },

    markup : function(imgNode) {
        return "[img[" + this.argsFromNode(imgNode).join("|") + "]]";
    },

    argsFromNode : function(imgNode) {
        var args = [];
        var src =  imgNode.getAttribute("argSrc");
        if (!src) {
            src = imgNode.src;
        }
        args[0] = src;
        var alt = imgNode.getAttribute("alt");
        if (alt) args[1] = alt;
        var align = imgNode.getAttribute("align");
        if (align) args[2] = align;
        var w = imgNode.getAttribute("width");
        var h = imgNode.getAttribute("height");
        if (h && w) args[3] = w + "x" + h;
        return args;
    }
};
objectExtend(WikiEx_img, WikiExCommon);

var WikiEx_launch = {
    id : "launch",
    render : function(parent, context, flow, label, masterFlow, roles) {
        var theLink = this.draw(parent, context, flow, label, masterFlow, roles);
    },

    propLabels : ["Flow path", "Label", "Master flow path", "Roles (role1,role2...)"],

    draw : function(parent, context, flow, label, masterFlow, roles) {
        if (SH.is_ie) makeElementNbSpd(parent, "span", "marginHack");
        var ispan = posRelIEFix(makeElement(parent, "span", "pbutton"));
        var theLink = makeElement(ispan, context.isSummary ? "div" : "a", "launch");
        if (SH.is_safari) theLink.className += " safari";
        makeElement(theLink, "div", "cap");
        // in path of top wiki
        var uri = Uri.parent(context.wiki.getUri());
        if (!label) label = flow;
        if (!masterFlow) masterFlow = "";
        else masterFlow = Uri.reduce(Uri.absolute(uri, App.resolver.resolveUri(masterFlow)));
        theLink.title = label;
        theLink.href = "#";
        flow = Uri.reduce(Uri.absolute(uri, App.resolver.resolveUri(flow)));
        theLink.setAttribute("flow", flow);
        theLink.setAttribute("masterFlow", masterFlow);
        if (roles) theLink.setAttribute("roles", roles);
        setEventHandler(theLink, "onclick", this.click);
        if (context.highlightRegExp)
           context.highlightMatch = context.highlightRegExp.exec(label);
        context.highlightMatch = subWikify(theLink, noBreakString(label), 0, label.length, context);

        ActivityTree.xb.loadURIAsync("../act/flowInfo?flow=" + Uri.escape(flow) +
                "&masterFlow=" + Uri.escape(masterFlow), this.info, theLink);
        return theLink;
    },

    info : function(xmlDoc, theLink) {
        if (xmlDoc != null) {
            var ico = makeElement(theLink, "div", "icon");
            ico.className += " " + xmlDoc.documentElement.getAttribute("icon") + "Ico";
        }
    },

    click : function(evt) {
        var roleStr = this.getAttribute("roles");
        var roles = null;
        if (roleStr) {
           var roleList = roleStr.split(",");
           roles = new Object();
           for (var ii = 0; ii < roleList.length; ii++) {
                roles[roleList[ii]] = "";
           }
        }
        ActivityTree.activityLaunch(
                this.getAttribute("flow"),
                Uri.name(this.getAttribute("flow")),
                this.getAttribute("masterFlow"),
                this.getAttribute("sub") == "1",
                roles);
        return false;
    }
};
objectExtend(WikiEx_launch, WikiExCommon);


var WikiEx_launchSub = {
    id : "launchSub",
    render : function(parent, context, flow, label, masterFlow, roles) {
        var theLink = this.draw(parent, context, flow, label, masterFlow, roles);
        theLink.setAttribute("sub", "1");
    }
};
objectExtend(WikiEx_launchSub, WikiEx_launch);


var WikiEx_submit = {
    id : "submit",
    render : function(parent, context, label, expr, img) {
        label = trim(label);
        img = trim(img);
        var mode1 = label.charAt(0) == "!";
        if (mode1) label = label.substring(1);
        
        var theLink;
        var item = ActivityTree.getActiveActivityItem();
        var inActive = Modes.mode != "edit" &&
        	(item == null || !item.canSubmit(context.art.step.getId(), context.art.step));
        	
        if (img) {
        	var src = img;
        	theLink = makeElement(parent, "img");
        	theLink.style.cursor = "pointer";
	        var resUri = {uri : src, suffix : ""};
	        var artPath = context.art.getPath();
	        WikiExUtil.resolveUri(resUri);
	        src = resUri.uri;
	        theLink.src = "../fil" + (src.charAt(0) == "/" ? src : artPath + "/" + src);
	        if (label) {
	            theLink.alt = label + resUri.suffix;
	        } else {
	            theLink.alt = resUri.suffix;
	        }
        } else {
        	if (SH.is_ie) makeElementNbSpd(parent, "span", "marginHack");
	        var ispan = posRelIEFix(makeElement(parent, "span", "pbutton2"));
	        var klass = mode1 ? "submit1" : "submit2";
	        
	        if (inActive) klass = "submit0";
	        theLink = makeElement(ispan, context.isSummary ? "div" : "a", klass);
	        if (SH.is_safari) theLink.className += " safari";
	        makeElement(theLink, "div", "cap");
	        
	      	if (!label) label = H.nbsp;
	        
	        if (context.highlightRegExp)
	           context.highlightMatch = context.highlightRegExp.exec(label);
	
	        context.highlightMatch = 
	        	subWikify(theLink, noBreakString(label), 0, label.length, context);
	        	
	        theLink.href = "#";
        }
        theLink.title = "Submit: " + label;
        
        
        if (!inActive) {
        	theLink._step = context.art.step;
        }
        theLink._expr = expr;
        theLink._wikiView = context.wikiView;
        setEventHandler(theLink, "onmousedown", this.click);

        parent.appendChild(theLink);
    },

    propLabels : ["Label", "Business rule", "Image"],

    click : function() {
        if (!this._step) return false;
        this.focus();
        if (Modes.mode == "edit") {
            alert("Edit Mode Message:\n\tThis will Submit: " + this._expr + "\n\tFor Step: " + this._step.getId());
        } else if (App.activeActivityNode) {
        	var holdThis = this;
        	window.setTimeout(function() {
	        	// dispatch submit event to forms in the view
	        	var dlist = holdThis._wikiView.disposeList;
	        	for (var ii=0; ii < dlist.length; ii++) {
	        		var xf = dlist[ii];
	        		if (xf.constructor === XForm) {
	        			xf.fireEvent("ix-activity-save", xf.getDefaultModel());
	        		}
	        	}
	            ActivityTree.submit(App.activeActivityNode.getAttribute("id"),
	                    holdThis._step.getId(), holdThis._expr, holdThis._wikiView.contentElem, true);
	            holdThis = null;
        	}, 40);
        } else {
            alert("There's no current activity to submit");
        }
        return false;
    },

    wysDisplayText : function(macro) {
            return macro.args[0];
    }
};
objectExtend(WikiEx_submit, WikiExCommon);


var WikiEx_activity = {
    id : "activity",

    render : function(parent, context, id, title) {
    	ActivityTree.renderActivityInfo(id, parent, title, "wiki");
    },

    propLabels : ["Activity ID"],

    wysDisplayText : function(macro) {
        var node = ActivityTree.activityInfo(macro.args[0], true);
        if (!node) {
            return App.lastError;
        } else {
            return node.getAttribute("name");
        }
    }
};
objectExtend(WikiEx_activity, WikiExCommon);

var WikiEx_kb = {
    id : "kb",
    render : function(parent, context, uri, article, label, pop) {
        var theLink = makeElement(parent, context.isSummary ? "div" : "a", "attach");
        theLink.href = "#";
        makeElement(theLink, "div", "icon kb_Ico");
        var resUri = {uri : uri, suffix : ""};
        WikiExUtil.resolveUri(resUri);
    	if (Uri.ext(resUri.uri).toLowerCase() == "kb") {
    		theLink.setAttribute("title", article);
    		if (!label) label = article;
    		if (!label) label = Uri.name(resUri.uri);
    		uri = resUri.uri;
    	} else {
    		if (uri.indexOf("{activity") == 0 || uri.indexOf("{item") == 0) {
        		resUri.uri = "activities/activities.kb";
	            if (App.activeActivityNode != null) {
	                var iNode = App.activeActivityNode;
	                if (uri.indexOf("-parent}") > 0) {
	                    uri = "activities/activities.kb";
	                    var pid = iNode.getAttribute("parent");
	                    if (pid) {
	                        var pNode = ActivityTree.activityInfo(pid);
	                        if (pNode.getAttribute("flow") != iNode.getAttribute("flow")) {
	                            uri = Uri.absolute(Uri.parent(pNode.getAttribute("uri")), "activities.kb");
	                        }
	                        iNode = pNode;
	                    }
	                } else {
	                    uri = "activities/activities.kb";
	                }
	                theLink.setAttribute("title", iNode.getAttribute("name"));
               	 	if (!label) label = iNode.getAttribute("name");
	            
            	} else {
                	if (!label) label = "Activities";
            	}
	        }
        }
        var artPath = context.art.getPath();
        theLink.setAttribute("kbUri", "../fil" + (uri.charAt(0) == "/" ? uri : artPath + "/" + uri));
        theLink.setAttribute("pop", pop ? "1" : "0");

        if (!label) label = uri;
        label = label + resUri.suffix;
        
        if (context.highlightRegExp)
           context.highlightMatch = context.highlightRegExp.exec(label);

        context.highlightMatch = subWikify(theLink, label, 0, label.length, context);
        theLink.onclick = this.click;
    },

    propLabels : ["Path", "Article", "Label"],

    click : function() {
        var uri = this.getAttribute("kbUri");
        var pop  = this.getAttribute("pop") == "1";
        if (!uri) {
            alert("Active activity required.");
            return false;
        }
        if (pop) {
        	Wiki.popup(uri, this.getAttribute("title"), 740, 500);
        } else {
	        var diag = Wiki.dialog(uri, this.getAttribute("title"));
	        diag.show(300, 100);
        }
        return false;
    },
    
    wysDisplayText : function(macro) {
    	var idx = macro.args.length  - 1;
        return macro.args[idx < 3 ? idx : 2];
    }
};
objectExtend(WikiEx_kb, WikiExCommon);


var WikiEx_upload = {
    id : "upload",
    render : function(parent, context, uri, label) {
        var itemDiv = makeElement(parent, "div", "soon", "Upload coming soon.");
        var resUri = {uri : srcUri};
        WikiExUtil.resolveUri(resUri);
    },

    propLabels : ["Path", "Label"]
};
objectExtend(WikiEx_upload, WikiExCommon);

// tarUri = view/edit target
var WikiEx_copy = {
    id : "copy",
    render : function(parent, context, srcUri, dstUri, label, tarUri) {
        if (SH.is_ie) makeElementNbSpd(parent, "span", "marginHack");
        var ispan = posRelIEFix(makeElement(parent, "span", "pbutton2"));
        var theLink = makeElement(ispan, context.isSummary ? "div" : "a", "copy");
        if (SH.is_safari) theLink.className += " safari";
        makeElement(theLink, "div", "cap");
        var resUri = {uri : trim(srcUri), suffix : ""};
        WikiExUtil.resolveUri(resUri);
        srcUri = resUri.uri;
        resUri.uri = trim(dstUri);
        WikiExUtil.resolveUri(resUri);
        dstUri = resUri.uri;

        // in path of main wiki
        var wuri = Uri.parent(context.wiki.getUri());
        srcUri = Uri.reduce(Uri.absolute(wuri, srcUri));
        dstUri = Uri.reduce(Uri.absolute(wuri, dstUri));

        WikiEx_attach.attachRegExp.lastIndex = 0;
        var matches = WikiEx_attach.attachRegExp.exec(tarUri ? tarUri : srcUri);
        var ico;
        if (matches) {
            ico = makeElement(theLink, "div", "icon");
            if (matches[2]) {
                ico.className += " fldIco";
                srcUri = srcUri.substring(0, srcUri.length - 1);
            } else {
                var ext = matches[3].toLowerCase();
                ico.className += " " + ext + "_Ico"
                if (srcUri.charAt(srcUri.length - 1) == "/")
                    srcUri = srcUri.substring(0, srcUri.length - 1);
            }

        }
        theLink.href = "#";
        theLink.onclick = function() {
        		FileTree.doCopy(srcUri, dstUri, tarUri);
                return false;
            };
        theLink.title = "Copy: " + Uri.name(srcUri) + " to " + Uri.name(dstUri);

        if (!label) label = Uri.name(srcUri);
        label += resUri.suffix;
        if (!label) label = H.nbsp;
        context.highlightMatch = subWikify(theLink, noBreakString(label), 0, label.length, context);

        parent = null; theLink = null; ico = null; ispan = null; // IE enclosure clean-up
    },

    propLabels : ["Source path", "Destination", "Label", "Open path"],

    wysDisplayText : function(macro) {
        var txt = macro.args.length > 2 ? macro.args[2] : macro.args[0];
        if (!txt) return macro.args[0];
        return txt;
    }
};
objectExtend(WikiEx_copy, WikiExCommon);


var WikiEx_plan = {
    id : "plan",
    render : function(parent, context, stepId, options) {
        var planDiv = makeElement(parent, "div", "plan");
        var pNode = ActivityTree.getPlan(stepId);
        if (pNode == null) {
            setElementText(planDiv, "Activity plan for: " + stepId);
        } else {
        	var optSkip = true, optDue = true;
        	if (options) {
        		optSkip = options.indexOf("skip") >= 0;
        		optDue = options.indexOf("due") >= 0;
        	}
        	if (optSkip) {
	            var chk = makeElement(planDiv, "input", "plan", "checkbox", null, {name:"pl"+stepId})
	            chk.checked = (pNode.getAttribute("skip") != "1");
	            chk._step = stepId;
	            chk.onclick = this.click1;
        	}
        	if (optDue) {
        		var dueDate = pNode.getAttribute("dueDate");
        	}
            var lbl = makeElement(planDiv, "label", "plan");
            makeElement(lbl, "span", "step", stepId);
            var stpOb = context.art.step.model.getStepById(stepId);
            if (stpOb) {
                var desc = stpOb.getDescription();
                makeElement(lbl, "span", "desc", desc);
            }
        }
    },

    propLabels : ["Step name", "Options"],

    click1 : function() {
        var holdThis = this;
        setTimeout(function() {
                WikiEx_plan.click2(holdThis);
            }, 50);
    },

    click2 : function(chk) {
        ActivityTree.setPlan(chk._step, !chk.checked);
    }
};
objectExtend(WikiEx_plan, WikiExCommon);


var WikiEx_input = {
    id : "input",
    
    render : function(parent, context, attName, label, appearance) {
    	var pm = context.art.step ? context.art.step.model.canvasObj : null;
    	var attType = pm ? pm.getAttrType(attName) : null;
    	if (!attType) {
    		makeElement(parent, "div", "err", "Attribute not found: " + attName);
    		return;
    	}
    	WikiEx_input.attRender(parent, pm.getAttrXform(), pm, attType, attName, label, appearance);
    },
    
    attRender : function(uiParent, xfrm, pm, attType, attName, label, appearance) {
    	var tmplNm;
    	var isList = false;
    	switch (attType) {
			case "xsd:boolean":
				tmplNm = "check"; break;
				
			case "xsd:date":
			case "xsd:dateTime":
				tmplNm = "date"; break;
			
			case "xsd:float":
			case "ix:percent":
			case "ix:currencyUSD":
				tmplNm = "number"; break;
			
			case "xsd:NMTOKEN":
				isList = true;
				tmplNm = appearance == "full" ? "select1_full" : "select1"; break;
				
			case "xsd:NMTOKENS":
				isList = true;
				tmplNm = "select"; break;
			
			case "ix:email":
				tmplNm = "email"; break;
				
			case "ix:http":
				tmplNm = "http"; break;
			
			case "xsd:string":
				tmplNm = appearance == "full" ? "text_full" : "default"; break;
				
			default:
				tmplNm = "default"; break;
    	}
		WikiEx_input.attTemplateRender(uiParent, xfrm, pm, tmplNm, attName, label, isList);
    },
    
    attTemplateRender : function(uiParent, xfrm, pm, tmplNm, attName, label, isList) {
    	var tmp = xfrm.getIxTemplate(tmplNm);
		var xfMod = xfrm.getDefaultModel();
		var nds = xfMod.selectNodeList(attName);
		if (nds.length == 0) { //auto make
			nds[0] = Xml.element(xfMod.getDefaultInstance(), attName);
			xfMod.rebuild();
		}
		if (xfrm._attrSet == null) xfrm._attrSet = [];
		xfrm._attrSet.push(attName);
		tmp.setVarString("label", label ? label : attName);
		if (isList) {
			tmp.setVarString("itemset", attName);
		}
		tmp.renderTemplate(nds[0], uiParent);
    },
    
    propLabels : ["Attribute", "Label", "Appearance"]
};
objectExtend(WikiEx_input, WikiExCommon);


var WikiEx_output = {
    id : "output",
    
    render : function(parent, context, attName, label, appearance) {
    	var pm = context.art.step ? context.art.step.model.canvasObj : null;
    	var attType = pm ? pm.getAttrType(attName) : null;
    	if (!attType) {
    		makeElement(parent, "div", "err", "Attribute not found: " + attName);
    		return;
    	}
    	WikiEx_output.attRender(parent, pm.getAttrXform(), pm, attType, attName, label);
    },
    
    attRender : function(uiParent, xfrm, pm, attType, attName, label) {
    	var tmplNm;
    	switch (attType) {
			case "xsd:boolean":
				tmplNm = "check"; break;
			
			case "ix:email":
				tmplNm = "email"; break;
				
			case "ix:http":
				tmplNm = "http"; break;
				
			case "ix:activity":
			case "ix:file":
				tmplNm = "decorate"; break;
				
			default:
				tmplNm = "default"; break;
    	}
		WikiEx_input.attTemplateRender(uiParent, xfrm, pm, "out_" + tmplNm, attName, label);
    },
    
    propLabels : ["Attribute", "Label"]
};
objectExtend(WikiEx_output, WikiExCommon);

Wiki.createContext = function(wikiView, art, highlightText, isSummary) {
    return {
        wikiView : wikiView,
        wiki : wikiView.wiki,
        art : art,
        column : 0,
        highlightText : highlightText,
        highlightRegExp : null,
        highlightMatch : null,
        isSummary : isSummary};
};

var WikiEx_flows = {
    id : "flows",
    render : function(parent, context, uri, label) {
        var listDiv = makeElement(parent, "div", "flows");
        makeElement(listDiv, "div", "head", label);
        var resUri = {uri : trim(uri)};
        WikiExUtil.resolveUri(resUri);
        ActivityTree.renderFlowTree(listDiv, Uri.absolute(Uri.parent(context.wiki.getUri()), resUri.uri));
    },

    propLabels : ["Path", "Label"]
};
objectExtend(WikiEx_flows, WikiExCommon);

var WikiEx_script = {
    id : "script",
    render : function(parent, context, script) {
        eval(script);
    },

    propLabels : ["Script"]
};
objectExtend(WikiEx_script, WikiExCommon);


var WikiEx_extWinLink = {
    id : "extWinLink",
    render : function(parent, context, label, extParams) {
       	var theLink = makeElement(parent, context.isSummary ? "div" : "a", "attach");
        theLink.href = "#";
        theLink.setAttribute("extParams", extParams);
       	if (context.highlightRegExp)
           context.highlightMatch = context.highlightRegExp.exec(label);

        context.highlightMatch = subWikify(theLink, label, 0, label.length, context);
        theLink.onclick = this.click;
    },
    
    click : function() {
    	try {
    		window.external.itensilExtConnect(this.getAttribute("extParams"));
    	} catch (e) {
    		alert("Sorry, MS Windows application host missing or other error occured.");
    	}
    },

    propLabels : ["Label", "Ext Params"]
};
objectExtend(WikiEx_extWinLink, WikiExCommon);


var WikiEx_entity = {
    id : "entity",
    
    render : function(parent, context, name, form, recIds) {
        
        // resolve entity
        if (context.art.step) {
        
	        var procDoc = context.art.step.model.pmDocument;
	        var dat = procDoc ? Xml.matchOne(procDoc.documentElement, "data") : null;
	        var entNode = dat ? Xml.matchOne(dat, "entity", "name", name) : null;
	        if (!entNode) {
	        	makeElement(parent, "div", "soon", "Entity information missing.");
	        	return;
	        }
	        var entDiv =  makeElement(parent, "div", "wikEntity");
	        var entityId = entNode.getAttribute("type");
	        var egConf = { 
		        	activityKeys : { activity : App.activeActivityId, relName : name },
			        condition : entNode.getAttribute("condition"),
			        relation : entNode.getAttribute("relation"),
			        designMode : (Modes.mode == "edit"),
			        form : form,
			        formInfo : {
			        		renderFunc : WikiEx_entity.renderForm,
			        		context : context,
			        		entDiv :entDiv
			        	}
	        	};
	        
	        var eg = EntityGrid.embed(entDiv, "/getModel?id=" + Uri.escape(entityId), null, egConf);
	        context.wikiView.disposeList.push(eg);
	        
        } else {
        	var entDiv =  makeElement(parent, "div", "wikEntity");
        	var egConf = { idSet : (recIds ? recIds.split(",") : null) };
        	if (form) {
        		egConf.form = form;
        		egConf.formInfo = {
		        		renderFunc : WikiEx_entity.renderForm,
		        		context : context,
		        		entDiv :entDiv
		        	};
       		} else {
       			egConf.relActs = 1;
       		}
        	var entUri = Uri.absolute(Uri.absolute("/home/entity", name), "model.entity");
        	var eg = EntityGrid.embed(entDiv, entUri, name, egConf);
	        context.wikiView.disposeList.push(eg);
        }
    },
    
    renderForm : function(formInfo, dataUri) {
    	var uriRes = new AppUriResolver(Uri.parent(formInfo.formUri));
   	 	if (dataUri) uriRes.recUri = Uri.parent(dataUri);
    	WikiEx_form.renderForm(
			posRelIEFix(makeElement(formInfo.entDiv, "div", "form")), formInfo.context,
			"../fil" + formInfo.formUri, dataUri, null, uriRes);
    },
    
  	wysDisplayText : function(macro) {
    	var name = macro.args[0];
    	var form = macro.args[1];
        return name + (form ? (" > " + Uri.name(form)) : "");
    },

    propLabels : ["Name", "Form", "Record ID (non-workzone)"]
};
objectExtend(WikiEx_entity, WikiExCommon);

// Extensions
Wiki.exts = {
    "img" : WikiEx_img,
    "include" : WikiEx_include,
    "media" : WikiEx_media,
    "attach" : WikiEx_attach,
    "attachedit" : WikiEx_attachEdit,
    "revisions" : WikiEx_revisions,
    "form" : WikiEx_form,
 //   "report" : WikiEx_report,
    "inject" : WikiEx_launch, //legacy
    "injectsub" : WikiEx_launchSub, //legacy
    "launch" : WikiEx_launch,
    "launchsub" : WikiEx_launchSub,
    "submit" : WikiEx_submit,
    "input" : WikiEx_input,
    "output" : WikiEx_output,
    "kb" : WikiEx_kb,
    "upload" : WikiEx_upload,
    "copy" : WikiEx_copy,
    "item" : WikiEx_activity, //legacy
    "activity" : WikiEx_activity,
    "plan" : WikiEx_plan,
    "flows" : WikiEx_flows,
    "script" : WikiEx_script,
    "extwinlink" : WikiEx_extWinLink,
    "entity" : WikiEx_entity
    };


// Create child text nodes and link elements to represent a wiki-fied version of some text
    // Prepare the regexp for the highlighted selection
Wiki.wikify = function(text, parent, context) {
    if (context.highlightText) {
        context.highlightRegExp = new RegExp(context.highlightText, "img");
        context.highlightMatch = context.highlightRegExp.exec(text);
    }
	//TODO -elj- fix article Node type should be type 1
    //if(context.art != null && context.art.nodeType == 3) 
    if (context.art != null) {
	    var layout = context.art.getAttribute("layout");
	    if (!context.isSummary && layout) {
	        var tbody = makeLayoutTable(parent, "layout lo_" + layout);
	        var row = makeElement(tbody, "tr");
	        parent = makeElement(row, "td", "layout column" + context.column);
	    }
	    wikifyStructures(parent,text,text,0,text.length,context);
    }
};

Wiki.addColumn = function(parent, context) {
    context.column++;
    var row = parent.parentNode;
    return makeElement(row, "td", "layout column" + context.column);
};

/**
Following Wiki text processing based on:
  TiddlyWiki 1.2.6 by Jeremy Ruston, (jeremy [at] osmosoft [dot] com)
  Incorporating improvements by Isao Sonobe, http://www-gauge.scphys.kyoto-u.ac.jp/~sonobe/OgreKit/OgreKitWiki.html
  Copyright (c) Osmosoft Limited, 14 April 2005
  All rights reserved.
  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
  Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
  Neither the name of the Osmosoft Limited nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 */
var wikiNameRegExp;
var tableRegExp;
var tableRowColRegExp;
var structureRegExp;
var formatRegExp;
var extArgsRegExp;
var newlineRegExp;
var summaryFilterRegExp;

function wikiSetupRegexp()
{
    var upperLetter = "[A-ZÀ-ß]";
    var lowerLetter = "[a-zà-ÿ_0-9\\-]";
    var anyLetter = "[A-Za-zÀ-ßà-ÿ_0-9\\-]";
    var anyDigit = "[0-9]";
    var anyNumberChar = "[0-9\\.E]";
    var urlPattern = "(?:http|https|mailto|ftp):[^\\s'\"]+(?:/|\\b)";
    var explicitLinkPattern = "\\[\\[(.+?)\\|(.+?)\\]\\]";
    var bracketNamePattern = "\\[\\[(.+?)\\]\\]";
    var extendPattern = "\\[([A-Za-z_0-9]+) *\\[(.+?)\\]\\]";

    // Table rows pattern
    var rowPattern = "^\\|(.*\\|)([fhc]?)$";
    tableRegExp = new RegExp(rowPattern,"mg");
    // Table columns pattern
    var elementPattern = "(?:[Bb][Gg][Cc][Oo][Ll][Oo][Rr]\\(([^\\)]+)\\):)?((?:[^\\|]*(?:[^\\[]*\\]\\])?)*)\\|";
    tableRowColRegExp = new RegExp(elementPattern,"g");

    // Link patterns
    var wikiNamePatterns = "(" + urlPattern +
        ")|(?:" + explicitLinkPattern +
        ")|(?:" + bracketNamePattern +
        ")|(?:" + extendPattern +
        ")";
    wikiNameRegExp = new RegExp(wikiNamePatterns,"mg");

    // Structural patterns
    var breakPattern = "[\\n]";
    var horizontalRulePattern = "^----$" + breakPattern  + "?";
    var hardBreakPattern = "^~~~~$" + breakPattern  + "?";
    var columnSepPattern = "^\\+\\+\\+\\+$" + breakPattern  + "?";
    var headerPattern = "^!{1,5}";
    var bulletListItemPattern = "^\\*+";
    var numberedListItemPattern = "^#+";
    var tablePattern = "(?:^\\|.*$" + breakPattern + "?)+";
    var blockquotePattern = "(?:^>.*$" + breakPattern + "?)+";
    var blockquotePattern2 = "^<<<" + breakPattern + "([\\S\\s]*?)(^<<<$" + breakPattern  + "?)";

    var floatRightPattern = "^\\}\\}\\}(?:([A-Za-z_0-9]+):)?" +
         breakPattern + "([\\S\\s]*?)(^\\}\\}\\}$" + breakPattern  + "?)";

    var floatLeftPattern = "^\\{\\{\\{(?:([A-Za-z_0-9]+):)?" +
         breakPattern + "([\\S\\s]*?)(^\\{\\{\\{$" + breakPattern  + "?)";

    var codePattern = "^%%%(?:([A-Za-z_0-9]+):)?" + breakPattern + "([\\S\\s]*?)(^%%%$" + breakPattern  + "?)";

    var structurePatterns = "(" + breakPattern +
        ")|(" + horizontalRulePattern +
        ")|(" + headerPattern +
        ")|(" + bulletListItemPattern +
        ")|(" + numberedListItemPattern +
        ")|(" + tablePattern +
        ")|(" + blockquotePattern +
        ")|(?:" + blockquotePattern2 +
        ")|(?:" + floatRightPattern +
        ")|(?:" + floatLeftPattern +
        ")|(?:" + codePattern +
        ")|(" + hardBreakPattern +
        ")|(" + columnSepPattern +
        ")";
    structureRegExp = new RegExp(structurePatterns,"mg");
    
    // ? potential support for escaped pipes: (.*?[^\\])(?:\||$)
    extArgsRegExp = new RegExp("([^\\|]*)\\|","g");

    summaryFilterRegExp = new RegExp("^(?:%%%|\\{\\{\\{|\\}\\}\\}|<<<)","mg");

    // Style patterns
    var boldPattern = "''(.*?)''";
    var strikePattern = "==(.*?)==";
    var underlinePattern = "__(.*?)__";
    var italicPattern = "\\\\\\\\(.*?)\\\\\\\\";
    var supPattern = "\\^\\^(.*?)\\^\\^";
    var subPattern = "~~(.*?)~~";
    var colorPattern = "@@(?:color\\(([^\\)]+)\\):|bgcolor\\(([^\\)]+)\\):){0,2}(.*?)@@";
    var stylePatterns = "(?:" + boldPattern +
        ")|(?:" + strikePattern +
        ")|(?:" + underlinePattern +
        ")|(?:" + italicPattern +
        ")|(?:" + supPattern +
        ")|(?:" + subPattern +
        ")|(?:" + colorPattern +
        ")";
    formatRegExp = new RegExp(stylePatterns,"mg");
    newlineRegExp = new RegExp("\\n","mg");
}


function wikifyStructures(parent,text,targetText,startPos,endPos,context)
{
    var body = parent;
    var theList = [];  // theList[0]: don't use
    var isInListMode = false;
    var isInHeaderMode = false;
    var isNewline = false;
    // The start of the fragment of the text being considered
    var nextPos = 0;
    // Loop through the bits of the body text
    structureRegExp.lastIndex = 0;
    do {
        // Get the next formatting match

        var formatMatch = structureRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;

         var rxLastIndex = structureRegExp.lastIndex;

        // Subwikify the plain text before the match
        if(nextPos < matchPos)
            {
            isNewline = false;
            context.highlightMatch = wikifyStyles(body,text,targetText.substring(nextPos,matchPos),startPos+nextPos,startPos+matchPos,context);
            }
        // Dump out the formatted match
        var level, scEnd, starTok, inside, theBlockquote;
        if(formatMatch)
            {
            // Dump out the link itself in the appropriate format
            if(formatMatch[1])
                {
                if(isNewline && isInListMode)
                    {
                    theList = [];
                    body = parent;
                    isInListMode = false;
                    }
                else if(isInHeaderMode)
                    {
                    body = parent;
                    isInHeaderMode = false;
                    }
                else
                    {
                    isNewline = true;
                    if (!isInListMode || "*#\n".indexOf(targetText.substring(rxLastIndex,rxLastIndex+1)) < 0)
                        body.appendChild(document.createElement("br"));
                    }
                }
            else if(formatMatch[2])
                {
                isNewline = false;
                body.appendChild(document.createElement("hr"));
                }
            else if(formatMatch[3])
                {
                if(isInListMode)
                    {
                    theList = [];
                    isInListMode = false;
                    }
                level = formatMatch[3].length;
                isNewline = false;
                isInHeaderMode = true;
                var theHeader = document.createElement("h" + level);
                parent.appendChild(theHeader);
                body = theHeader;
                }
            else if(formatMatch[4])
                {
                level = formatMatch[4].length;
                isNewline = false;
                isInListMode = true;
                if (theList[level] == null)
                    {
                    theList[level] = document.createElement("ul");
                    body.appendChild(theList[level]);
                    }
                else if (theList.length >= level)
                    {
                        theList.splice(level + 1, theList.length - level + 1);
                    }

                body = document.createElement("li");
                theList[level].appendChild(body);
                }
            else if(formatMatch[5])
                {
                level = formatMatch[5].length;
                isNewline = false;
                isInListMode = true;
                if (theList[level] == null)
                    {
                    theList[level] = document.createElement("ol");
                    body.appendChild(theList[level]);
                    }
                else if (theList.length >= level)
                    {
                        theList.splice(level + 1, theList.length - level + 1);
                    }
                body = document.createElement("li");
                theList[level].appendChild(body);
                }
            else if(formatMatch[6])
                {
                isNewline = false;
                context.highlightMatch = wikifyTable(body,text,formatMatch[6],startPos+matchPos,startPos+rxLastIndex,context);
                }
            else if(formatMatch[7])
                {
                isNewline = false;
                var quotedText = formatMatch[7].replace(new RegExp("^>(>*)","mg"),"$1");
                theBlockquote = document.createElement("blockquote");
                var newContext = new Object();
                objectExtend(newContext, context);
                if (context.highlightRegExp) {
                    newContext.highlightRegExp = new RegExp(context.highlightRegExp.source, "img");
                    newContext.highlightMatch = newContext.highlightRegExp.exec(quotedText);
                }
                wikifyStructures(theBlockquote,quotedText,quotedText,0,quotedText.length,newContext);
                body.appendChild(theBlockquote);
                }
            else if(formatMatch[8])
                {
                isNewline = false;
                theBlockquote = document.createElement("blockquote");
                inside = formatMatch[8];
                starTok = 4;
                context.highlightMatch = wikifyStructures(theBlockquote,text,inside,startPos+matchPos+starTok,startPos+rxLastIndex-formatMatch[9].length,context);
                body.appendChild(theBlockquote);
                }
            else if(formatMatch[11] || formatMatch[14])
                {
                isNewline = false;
                var theFloat = document.createElement("div");

                var styleClass;
                var ender;
                if (formatMatch[11]) {
                    styleClass = formatMatch[10];
                    inside = formatMatch[11];
                    ender  = formatMatch[12];
                    theFloat.className = "floatRight";
                } else {
                    styleClass = formatMatch[13];
                    inside = formatMatch[14];
                    ender  = formatMatch[15];
                    theFloat.className = "floatLeft";
                }
                scEnd = 0;
                if (styleClass) {
                    scEnd = styleClass.length + 1;
                    theFloat.className += " float_" + styleClass.toLowerCase();
                } else {
                    theFloat.className += " float_default";
                }
                starTok = 4;
                context.highlightMatch = wikifyStructures(theFloat,text,inside,startPos+matchPos+scEnd+starTok,startPos+rxLastIndex-ender.length,context);
                body.appendChild(theFloat);
                }
            else if(formatMatch[17])
                {
                var syntax = formatMatch[16];
                isNewline = false;
                var thePre = document.createElement("pre");
                starTok = 4;
                inside = formatMatch[17];
                scEnd = 0;
                if (syntax) {
                    scEnd = syntax.length + 1;
                }
                var pPos = startPos+matchPos+scEnd+starTok;
                context.highlightMatch = wikiCodeSyntax(syntax,thePre,text,inside,pPos,pPos+inside.length,context);
                body.appendChild(thePre);
                }
            else if(formatMatch[19])
                {
                isNewline = true;
                var br = document.createElement("div");
                br.className = "hard";
                body.appendChild(br);
                }
            else if(formatMatch[20])
                {
                    isNewline = true;
                    isInListMode = false;
                    theList = [];
                    isInHeaderMode = false;
                    if (!context.isSummary) {
                        // new column, change parent...
                        body = parent = Wiki.addColumn(parent, context);
                    } else {
                        body.appendChild(document.createElement("br"));
                    }
                }
            }

        // Move the next position past the formatting match
        nextPos = structureRegExp.lastIndex = rxLastIndex;
    } while(formatMatch);
    return context.highlightMatch;
}

function wikifyLinks(parent,text,targetText,startPos,endPos,context)
{
    // The start of the fragment of the text being considered
    var nextPos = 0;
    // Loop through the bits of the body text
    var theLink;
    wikiNameRegExp.lastIndex = 0;
    do {
        // Get the next formatting match
        var formatMatch = wikiNameRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;

        var rxLastIndex = wikiNameRegExp.lastIndex;

        // Subwikify the plain text before the match
        if(nextPos < matchPos)
            context.highlightMatch = subWikify(parent,text,startPos+nextPos,startPos+matchPos,context);
        // Dump out the formatted match
        if(formatMatch)
            {
            // Dump out the link itself in the appropriate format
            if(formatMatch[1])
                {
                theLink = createExternalLink(parent,formatMatch[0],context);
                context.highlightMatch = subWikify(theLink,text,startPos+matchPos,startPos+rxLastIndex,context);
                }
            else if(formatMatch[2])
                {
                theLink = createExternalLink(parent,formatMatch[3],context);
                context.highlightMatch = subWikify(theLink,text,startPos+matchPos+2,startPos+matchPos+2+formatMatch[2].length,context);
                }
            else if(formatMatch[4])
                {
                theLink = createArticleLink(parent,formatMatch[4],context);
                context.highlightMatch = subWikify(theLink,text,startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
             else if(formatMatch[5] && formatMatch[6])
                {
                var eArgs = [];
                var argText = formatMatch[6];
                var exMatch;
                extArgsRegExp.lastIndex = 0;
                var lastExtArg = 0;
                do {
                    exMatch = extArgsRegExp.exec(argText);
                    if (exMatch) {
                        eArgs.push(exMatch[1]);
                    } else {
                        eArgs.push(argText.substr(lastExtArg));
                    }
                    lastExtArg = extArgsRegExp.lastIndex;
                } while (exMatch);
                var newContext = new Object();
                objectExtend(newContext, context);
                if (context.highlightRegExp) {
                    newContext.highlightRegExp = new RegExp(context.highlightRegExp.source,"ig");
                    newContext.highlightMatch = null;
                }
                wikiExtension(parent,newContext,formatMatch[5],eArgs);

                if (context.highlightRegExp) {
                    // skip to next highlight
                    context.highlightRegExp.lastIndex = startPos + rxLastIndex;
                    context.highlightMatch = context.highlightRegExp.exec(text);
                }

                }
            }
        // Move the next position past the formatting match
        nextPos = wikiNameRegExp.lastIndex = rxLastIndex;
    } while(formatMatch);
    return context.highlightMatch;
}

function wikifyStyles(parent,text,targetText,startPos,endPos,context)
{

    // The start of the fragment of the text being considered
    var nextPos = 0;
    // Loop through the bits of the body text
    formatRegExp.lastIndex = 0;
    do {
        // Get the next formatting match
        var formatMatch = formatRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;
        var rxLastIndex = formatRegExp.lastIndex;

        // Subwikify the plain text before the match
        if(nextPos < matchPos)
            context.highlightMatch = wikifyLinks(parent,text,targetText.substring(nextPos,matchPos),startPos+nextPos,startPos+matchPos,context);
        // Dump out the formatted match
        if(formatMatch)
            {
            // Dump out the link itself in the appropriate format
            if(formatMatch[1])
                {
                var theBold = createTiddlyElement(parent,"b",null,null,null);
                context.highlightMatch = wikifyStyles(theBold,text,formatMatch[1],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[2])
                {
                var theStrike = createTiddlyElement(parent,"strike",null,null,null);
                context.highlightMatch = wikifyStyles(theStrike,text,formatMatch[2],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[3])
                {
                var theUnderline = createTiddlyElement(parent,"u",null,null,null);
                context.highlightMatch = wikifyStyles(theUnderline,text,formatMatch[3],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[4])
                {
                var theItalic = createTiddlyElement(parent,"i",null,null,null);
                context.highlightMatch = wikifyStyles(theItalic,text,formatMatch[4],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[5])
                {
                var theSup = createTiddlyElement(parent,"sup",null,null,null);
                context.highlightMatch = wikifyStyles(theSup,text,formatMatch[5],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[6])
                {
                var theSub = createTiddlyElement(parent,"sub",null,null,null);
                context.highlightMatch = wikifyStyles(theSub,text,formatMatch[6],startPos+matchPos+2,startPos+rxLastIndex-2,context);
                }
            else if(formatMatch[9])
                {
                var theSpan;
                if (formatMatch[7] == "" && formatMatch[8] == "" )
                    {
                    theSpan = createTiddlyElement(parent,"span",null,"marked",null);
                    }
                    else
                    {
                    theSpan = createTiddlyElement(parent,"span",null,null,null);
                    if (formatMatch[7] != "") theSpan.style.color = formatMatch[7];
                    if (formatMatch[8] != "") theSpan.style.background = formatMatch[8];
                    }
                context.highlightMatch = wikifyStyles(theSpan,text,formatMatch[9],startPos+rxLastIndex-2-formatMatch[9].length,startPos+rxLastIndex-2,context);
                }
            }
        // Move the next position past the formatting match
        nextPos = formatRegExp.lastIndex = rxLastIndex;
    } while(formatMatch);
    return context.highlightMatch;
}

// Create a table
function wikifyTable(parent,text,targetText,startPos,endPos,context)
{
    // The start of the fragment of the text being considered
    var nextPos = 0;
    var theTable = document.createElement("table");
    theTable.className = "wiki";
    var bodyRowLen = 0;
    var headRowLen = 0;
    var footRowLen = 0;
    var bodyRows = [];
    var headRows = [];
    var footRows = [];
    var theCaption = null;

    context.hadTSpan = false;
    // Loop through the bits of the body text
    tableRegExp.lastIndex = 0;
    do {
        // Get the next formatting match

        var formatMatch = tableRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;
        // Dump out the formatted match
        if(formatMatch) {
            if (formatMatch[2] == "c") {
                var cap = formatMatch[1].substring(0,formatMatch[1].length-1);
                theCaption = document.createElement("caption");
                context.highlightMatch = wikifyStyles(theCaption,text,cap,startPos+matchPos+1,startPos+cap.length,context);
                if (bodyRowLen == 0 && headRowLen == 0 && footRowLen == 0) {
                    theCaption.setAttribute("align", "top");
                } else {
                    theCaption.setAttribute("align", "bottom");
                }
            } else if (formatMatch[2] == "h") {
                context.highlightMatch = wikifyTableRow(headRows,headRowLen,text,formatMatch[1],startPos+matchPos,startPos+matchPos+formatMatch[1].length,context);
                headRowLen++;
            } else if (formatMatch[2] == "f") {
                context.highlightMatch = wikifyTableRow(footRows,footRowLen,text,formatMatch[1],startPos+matchPos,startPos+matchPos+formatMatch[1].length,context);
                footRowLen++;
            } else {
                context.highlightMatch = wikifyTableRow(bodyRows,bodyRowLen,text,formatMatch[1],startPos+matchPos,startPos+matchPos+formatMatch[1].length,context);
                bodyRowLen++;
            }
        }
        nextPos = tableRegExp.lastIndex;
    } while(formatMatch);

    if (theCaption != null) {
        theTable.appendChild(theCaption);
    }

    if (headRowLen > 0) {
        var theTableHead = document.createElement("thead");
        wikiMakeTableRows(headRows,theTableHead);
        theTable.appendChild(theTableHead);
    }

    if (bodyRowLen > 0) {
        var theTableBody = document.createElement("tbody");
        wikiMakeTableRows(bodyRows,theTableBody);
        theTable.appendChild(theTableBody);

    }
    if (footRowLen > 0) {
        var theTableFoot = document.createElement("tfoot");
        wikiMakeTableRows(footRows,theTableFoot);
        theTable.appendChild(theTableFoot);
    }

	parent.appendChild(theTable);
    return context.highlightMatch;
}

function wikifyTableRow(rows,rowIndex,text,targetText,startPos,endPos,context)
{
    // The start of the fragment of the text being considered
    var eIndex = 0;
    var elements = [];
    // Loop through the bits of the body text
    tableRowColRegExp.lastIndex = 0;
    do {
        // Get the next formatting match
        var formatMatch = tableRowColRegExp.exec(targetText);
        var matchPos = formatMatch ? formatMatch.index : targetText.length;
        if(formatMatch) {
            var eText = formatMatch[2];
            if (eText == "~" || eText == ">") {
                context.hadTSpan = true;
                elements[eIndex] = eText;
            } else {
                var eTextLen = eText.length;
                var align = "";
                if (eTextLen >= 1 && eText.charAt(0) == " ") {
                    if (eTextLen >= 3 && eText.charAt(eTextLen - 1) == " ") {
                        align = "center";
                        eText = eText.substring(1,eTextLen - 1);
                        //eTextLen -= 2;
                        eTextLen--;
                    } else {
                        align = "right";
                        eText = eText.substring(1);
                        eTextLen--;
                    }
                } else if (eTextLen >= 2 && eText.charAt(eTextLen - 1) == " ") {
                    align = "left";
                    eText = eText.substring(0,eTextLen - 1);
                    //eTextLen--;
                }

                var theElement;
                if (eTextLen >= 1 && eText.charAt(0) == "!") {
                    theElement = document.createElement("th");
                    eText = eText.substring(1);
                    eTextLen--;
                } else {
                    theElement = document.createElement("td");
                }

                if (align != "") {
                    theElement.align = align;
                }

                if (formatMatch[1]) {
                    theElement.style.background = formatMatch[1];
                }

                context.highlightMatch = wikifyStyles(theElement,text,eText,startPos+tableRowColRegExp.lastIndex-eTextLen,startPos+tableRowColRegExp.lastIndex-1,context);
                elements[eIndex] = theElement;
            }
            eIndex++;
        }
    } while(formatMatch);
    rows[rowIndex] = elements;
    return context.highlightMatch;
}

function wikiMakeTableRows(rows,parent)
{
    var i, j, k, cols;
    for (i = 0; i < rows.length; i++) {
        cols = rows[i];
        var theRow = document.createElement("tr");
        for (j = 0; j < cols.length; j++) {
            if (cols[j] == "~") continue;

            var rowspan = 1;
            for (k = i+1; k < rows.length; k++) {
                if (rows[k][j] != "~") break;
                rowspan++;
            }

            var colspan = 1;
            for (; j < cols.length - 1; j++) {
                if (cols[j] != ">") break;
                colspan++;
            }

            var theElement = cols[j];
            if (rowspan > 1) {
                theElement.setAttribute("rowSpan",rowspan);
                theElement.setAttribute("rowspan",rowspan);
                theElement.valign = "center";
            }
            if (colspan > 1) {
                theElement.setAttribute("colSpan",colspan);
                theElement.setAttribute("colspan",colspan);
            }
            theRow.appendChild(theElement);
        }
        parent.appendChild(theRow);
    }
}

// Helper for wikify that handles highlights within runs of text
function subWikify(parent,text,startPos,endPos,context)
{
    var highlightMatch = context.highlightMatch;
    var highlightRegExp = context.highlightRegExp;

    // Check for highlights
    while(highlightMatch && (highlightRegExp.lastIndex > startPos) && (highlightMatch.index < endPos) && (startPos < endPos))
        {
        // Deal with the plain text before the highlight
        if(highlightMatch.index > startPos)
            {
            parent.appendChild(document.createTextNode(text.substring(startPos,highlightMatch.index)));
            startPos = highlightMatch.index;
            }
        // Deal with the highlight
        var highlightEnd = Math.min(highlightRegExp.lastIndex,endPos);
        var theHighlight = createTiddlyElement(parent,"span",null,"highlight",text.substring(startPos,highlightEnd));
        startPos = highlightEnd;
        // Nudge along to the next highlight if we're done with this one
        if(startPos >= highlightRegExp.lastIndex)
            highlightMatch = highlightRegExp.exec(text);
        }
    // Do the unhighlighted text left over
    if(startPos < endPos)
        {
        parent.appendChild(document.createTextNode(text.substring(startPos,endPos)));
        //startPos = endPos;
        }
    return(highlightMatch);
}

function createTiddlyElement(theParent,theElement,theID,theClass,theText)
{
    var e = document.createElement(theElement);
    if(theClass != null)
        e.className = theClass;
    if(theID != null)
        e.setAttribute("id",theID);
    if(theText != null)
        e.appendChild(document.createTextNode(theText));
    if(theParent != null)
        theParent.appendChild(e);
    return(e);
}

function createExternalLink(place,url, context)
{
    var theLink = document.createElement(context.isSummary ? "div" : "a");
    theLink.className = "externalLink";
    theLink.href = url;
    theLink.title = "External link to " + url;
    theLink.target = "_blank";
    place.appendChild(theLink);
    return(theLink);
}

function escapeRegExp(s)
{
    // Escape any special characters with that character preceded by a backslash
    return s.replace(new RegExp("([\\\\\\^\\$\\*\\+\\?\\(\\)\\=\\!\\|\\,\\{\\}\\[\\]\\.])","g"),"\\$1")
}

wikiSetupRegexp();