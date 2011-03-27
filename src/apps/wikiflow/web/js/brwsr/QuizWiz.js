var QuizWiz = {
	
	startClick : function(evt) {
		QuizWiz.dialog(this._procCan);
	},
	
	dialog : function(procCan) {
		QuizWiz.procCan = procCan;
        	
       	var diag = xfDialogConfig(
       			"Quiz Wizard", "../view-wf/quiz.xfrm", ActivityTree.xb,
				{diagHelp : (App ? App.chromeHelp : null), 
				 noResize : 2, 
				 defPath : procCan.xfrm.__defPath});
        
        var xfMod = diag.xform.getDefaultModel();
        var vb = getVisibleBounds();
        diag.contentElement.style.backgroundColor = "#fff";
        diag.show(100, 20, null, 610, vb.h - 40);
	},
	
	saveQuiz : function(root) {
		var flow = QuizWiz.procCan.getFlow();
		
		var maxY = 0;
		var ii;

	    var startStep = flow.idCtx.prefix ? 
	    	flow.getStepById(Uri.absolute(flow.idCtx.prefix, "$$S")) : flow.getStepById("Start");
	    
	    var nearEmpty = true;
	    ii=0; maxY = 0;
	    for (var sid in flow.steps) {
	    	var stpObj = flow.steps[sid];
	    	if (!(stpObj.constructor == PmEnd || stpObj.constructor == PmStart)) {
	    		nearEmpty = false;
	    	}
	    	var styleObj = styleString2Obj(stpObj.xNode.getAttribute("style"));
	    	if (styleObj.top) {
	    		var top = parseInt(styleObj.top);
	    		if (top > maxY) maxY = top + 75;
	    	}
	    }
	    
	    if (isNaN(maxY) || maxY < 0) maxY = 0;
	    
	    // prep input set
	  	var attIdCtx = new XmlId();
	    var attrNodes = flow.xfMod.selectNodeList("iw:data/iw:attr");
	    for (ii = 0; ii < attrNodes.length; ii++) {
	    	attIdCtx.addVar(attrNodes[ii].getAttribute("name"));
	    }
	    var dataRoot = flow.xfMod.selectNodeList("iw:data")[0];
	    var flowDoc = dataRoot.ownerDocument;
	    var prefix = flow.idCtx.prefix;
	    var stepRoot = flow.modelNode;
	    var pthMaster = flow.xfMod.selectNodeList("instance('pal')/path")[0];
	    var swiMaster = flow.xfMod.selectNodeList("instance('pal')/switch")[0]; 
	    
	    
	    var cursor = new Point(100, maxY + 50);
	    var sects = Xml.match(root, "quizSect");
	    var sNodes = [];
	    var startPath = null;
	    for(ii=0; ii < sects.length; ii++) {
	    	var acts = Xml.match(sects[ii], "activity");
	    	var lastAct = null, firstAct = null, questIds = [];
	    	for(var jj=0; jj < acts.length; jj++) {
	    		var act = Xml.nodeImport(flowDoc, stepRoot, acts[jj]);
	    		
	    		act.setAttribute("style", "left:" + cursor.x + "px;top:" + cursor.y + "px");
	    		
	    		var pid = act.getAttribute("id");
	    		if  (!pid) pid = "Sreen " + (jj+1);
	    		pid = Uri.absolute(prefix, pid);
	    		act.setAttribute("id", pid);
	    		sNodes.push(act);
	    		flow.xfMod.markChanged(act);
	    		
	    		if (startPath) {
	    			startPath.setAttribute("to", pid);
	    			startPath = null;
	    		}
	    		if (!firstAct) firstAct = act;
	    		if (lastAct) {
	    			var pRect = PmStep.getBounds(lastAct);
	    			var pthNod = Xml.cloneNode(pthMaster, true);
	    			pthNod.setAttribute("id", Uri.absolute(prefix, "ptG" + ii + "s" + jj));
	 				pthNod.setAttribute("to", pid);
	 				pthNod.setAttribute("startDir", Point.directions[Point.EAST]);
	 				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.EAST]]);
    				var sPnts = PmPath.pointSolve(
 						new Point(pRect.x + pRect.w, pRect.y + 30), Point.EAST,
 				 		new Point(cursor.x, cursor.y + 30), Point.EAST);
	 				
	 				pthNod.setAttribute("points", sPnts.join("-"));
	    			lastAct.appendChild(pthNod);
	    		}
	    		
	    		cursor.x += 160;
	    		
	    		var wrkArt = Xml.matchOne(act, "article");
	    		var wrkTxt = Xml.stringForNode(wrkArt);
	    		
	    		var medias = Xml.match(act, "media");
	    		for(var kk=0; kk < medias.length; kk++) {
	    			var uri = Xml.stringForNode(medias[kk]);
	    			if  (uri) {
	    				if (uri.charAt(0) != "/") {
	    					uri = "{model}/" + uri;
	    				}
	    				var ext = Uri.ext(uri).toLowerCase();
	    				var macro = { args : [] };
						if ("gif jpg jpeg png".indexOf(ext) >= 0) {
						    macro.id = "img";
						    macro.args[0] = uri;
						} else if ("swf mov avi wav mp3".indexOf(ext) >= 0 || uri.substring(uri.length - 11) == "_config.xml") {
						    macro.id = "media";
						    macro.args[0] = uri;
						    macro.args[1] = "400x300";
						} else if (ext == "xfrm") {
						    macro.id = "form";
						    macro.args[0] = uri;
						} else {
						    macro.id = "attachEdit";
						    macro.args[0] = uri;
						    macro.args[1] = Uri.name(uri);
						}
						wrkTxt += "\n[" + macro.id + "[" + macro.args.join("|") + "]]";
	    			}
	    			
	    		}
	    		
	    		var quests = Xml.match(act, "attr");
	    		for(var kk=0; kk < quests.length; kk++) {
	    			var qst = quests[kk];
	    			var attId = attIdCtx.uniqueVar("g" + ii + "s" + jj + "q" + kk);
	    			attIdCtx.addVar(attId);
	    			qst.setAttribute("name", attId);
	    			questIds.push(attId);
	    			
	    			// move to data
	    			dataRoot.appendChild(qst);
	    			flow.xfMod.markChanged(qst, "name");
	    			
	    			wrkTxt += "\n\n" + Xml.stringForNode(Xml.matchOne(qst, "quizText"));
	    			wrkTxt += "\n[input[" + attId + "|Choose one|full]]";
	    			
	    			var items = Xml.match(qst, "item");
	    			for(var ff=0; ff < items.length; ff++) {
	    				var itm = items[ff];
	    				var val = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(ff);
	    				itm.setAttribute("value", itm.getAttribute("quizKey") == 1 ? "1" : val);
	    			}
	    		}
	    		var subTxt = quests.length > 0 ? "Submit Answer" : "Continue";
	    		if (quests.length > 1) subTxt += "s";
	    		wrkTxt += "\n[submit[!"  + subTxt + "]]";
	    		
	    		Xml.setNodeValue(wrkArt, wrkTxt);
	    		lastAct = act;
	    	}
	    	
    		if (firstAct && lastAct) {
    			var swStep = Xml.cloneNode(swiMaster, true);
    			var pid = Uri.absolute(prefix, "Grade Section " + (ii+1));
    			
    			swStep.setAttribute("id", pid);
    			swStep.setAttribute("style", "left:" + cursor.x + "px;top:" + (cursor.y-10) + "px");
    			
    			stepRoot.appendChild(swStep);
    			sNodes.push(swStep);
    			
    			var pRect = PmStep.getBounds(lastAct);
    			var pthNod = Xml.cloneNode(pthMaster, true);
    			pthNod.setAttribute("id", Uri.absolute(prefix, "ptGA" + ii));
 				pthNod.setAttribute("to", pid);
 				pthNod.setAttribute("startDir", Point.directions[Point.EAST]);
 				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.EAST]]);
				var sPnts = PmPath.pointSolve(
					new Point(pRect.x + pRect.w, pRect.y + 30), Point.EAST,
			 		new Point(cursor.x, cursor.y + 30), Point.EAST);
 				
 				pthNod.setAttribute("points", sPnts.join("-"));
    			lastAct.appendChild(pthNod);
    			
    			var cond = "(" + questIds.join(" + ") + ") >= " + sects[ii].getAttribute("passScore");
    			
    			var lbl;
    			
    			pthNod = Xml.cloneNode(pthMaster, true);
    			pthNod.setAttribute("id", Uri.absolute(prefix, "ptGB" + ii));
 				pthNod.setAttribute("to", "");
 				pthNod.setAttribute("startDir", Point.directions[Point.SOUTH]);
 				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.SOUTH]]);
				var sPnts = PmPath.pointSolve(
					new Point(cursor.x + 60, cursor.y + 70), Point.SOUTH,
			 		new Point(120, cursor.y + 120), Point.SOUTH);
 				
 				pthNod.setAttribute("points", sPnts.join("-"));
 				Xml.element(pthNod, "condition", cond);
 				
 				lbl = Xml.matchOne(pthNod, "label");
 				lbl.setAttribute("style", "left:" + cursor.x + "px;top:" + (cursor.y+65) + "px")
 				Xml.setNodeValue(lbl, "Passed.");
 				
    			swStep.appendChild(pthNod);
    			flow.xfMod.markChanged(swStep);
    			
    			startPath = pthNod;
    			
    			pRect = PmStep.getBounds(firstAct);
    			pthNod = Xml.cloneNode(pthMaster, true);
    			pthNod.setAttribute("id", Uri.absolute(prefix, "ptGC" + ii));
 				pthNod.setAttribute("to", firstAct.getAttribute("id"));
 				pthNod.setAttribute("startDir", Point.directions[Point.NORTH]);
 				pthNod.setAttribute("endDir", Point.directions[Point.reverse[Point.SOUTH]]);
				sPnts = PmPath.pointSolve(
					new Point(cursor.x + 60, cursor.y - 10), Point.NORTH,
			 		new Point(pRect.x + (pRect.w/2), pRect.y), Point.SOUTH);
 				
 				pthNod.setAttribute("points", sPnts.join("-"));
 				
 				lbl = Xml.matchOne(pthNod, "label");
 				lbl.setAttribute("style", "left:" + cursor.x + "px;top:" + (cursor.y-35) + "px")
 				Xml.setNodeValue(lbl, "Too many wrong answers.");
 				
    			swStep.appendChild(pthNod);
    		}
	    	
	    	cursor.y += 120;
	    	cursor.x = 100;
	    }
	    flow.xfMod.markChanged(dataRoot);
	    //flow.xfMod.markChanged(Xml.firstElement(dataRoot));
	    flow.xfMod.rebuild();
	    
	    var sObjs = flow.digestStepList(sNodes, true);
	    
	    if (sObjs.length > 0) QuizWiz.procCan.dndCanvas.scrollToView(sObjs[0].element);
		flow.dndGroupList(sObjs, 0, 0);
	    
	    
	}
};