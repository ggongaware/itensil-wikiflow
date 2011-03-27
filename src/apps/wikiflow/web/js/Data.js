/**
 * (c) 2008 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 *  Data utilities
 * 
 */
 
var Data = {

	xb : new XMLBuilder(),
	
	xformFromAttrs : function(dataElement, formTemplateUri) {
			
		var formUri = formTemplateUri;
	
		var xfDoc = Data.xb.loadURI(formUri);
		var xfRoot = xfDoc.documentElement;
		var fMod = Xml.matchOne(xfRoot, "model");
		if (!fMod) return null;
		
		var insRoot = Xml.matchOneDeep(fMod, "data");
		if (!insRoot) return null;
		
		var divMain = Xml.matchOneDeep(xfRoot, "div", "class", "main");
		
		Data.__xfAttrConv(xfRoot, insRoot, fMod, dataElement, divMain, "");

	    // clean-out templates
	    var tmpls = Xml.match(xfRoot, "template", "name");
	    for (var ii = 0; ii < tmpls.length; ii++) {
	    	if (tmpls[ii].getAttribute("name").indexOf("template-") == 0) {
	    		xfRoot.removeChild(tmpls[ii]);
	    	}
	    }

	    //console.dirxml(xfDoc);
	    
	    return xfDoc;
	},
	
	__xfAttrConv : function(xfRoot, insRoot, fMod, dataElement, divMain, path) {
		
		var attrs = Xml.match(dataElement, "*");
		var ii, jj;
		
		var nmRegex = xsdTypes.itensilXF.varName.regex;
		var bnEl;
		
		// convert types to binds
	    for (ii = 0; ii < attrs.length; ii++) {
	    	var attr = attrs[ii];
	    	var locNm = Xml.getLocalName(attr);
	    	var atName = attr.getAttribute("name");
		    
			if (nmRegex.test(atName)) {
				// create data elem
				var atDatEl = Xml.element(insRoot, atName, attr.getAttribute("default"));
				var label = attr.getAttribute("label") || atName;
				var imp;
				
				if (locNm == "attr") {
					var attType = attr.getAttribute("type");
					var isArr = false, chPos;
					if (attType.indexOf("ix:composite") == 0) {
						var grpEl;
						isArr = attType.indexOf(":array") > 0;
						if (isArr) {
							
							var tmpl = Xml.matchOne(xfRoot, "template", "name", "template-composite-array");
							imp = Xml.cloneNode(Xml.firstElement(tmpl), true);
							imp.setAttribute("ref", atName);
							grpEl = Xml.matchOneDeep(imp, "group", "ref", ".");
							
    						divMain.appendChild(imp);
    						
    						var ens = Xml.matchOneDeep(imp, "variable", "name", "ns");
				    		if (ens) ens.setAttribute("value", "../" + atName);
				    		var lblo = Xml.matchOneDeep(imp, "output");
				    		if (lblo) lblo.setAttribute("value", "concat('" + label + " ',$pos)");
				    		
				    		var dupEl = Xml.matchOneDeep(imp, "duplicate");
							if (dupEl) dupEl.setAttribute("origin", "instance('moddat')/" + path + atName);
    						
						} else {
							
							grpEl = Xml.elementNs(divMain, "group", "http://www.w3.org/2002/xforms");
							grpEl.setAttribute("ref", atName);
							Xml.elementNs(grpEl, "label", "http://www.w3.org/2002/xforms", label);
							
						}
					
						Data.__xfAttrConv(xfRoot, atDatEl, fMod, attr, grpEl, path + atName + "/");
						continue;
					}
								
					bnEl = Xml.elementNs(fMod, "bind", "http://www.w3.org/2002/xforms");

			    	if ((chPos = attType.indexOf(":array")) > 0) {
			    		isArr = true;
			    		attType = attType.substring(0, chPos);
			    	}
				    bnEl.setAttribute("type", attType);
			    	bnEl.setAttribute("nodeset", path + atName);
			    	
			  		imp = Data.__xfAttrTmpl(attr, attType, isArr, xfRoot, divMain);

			    	if (isArr) {
			    		var ens = Xml.matchOneDeep(imp, "variable", "name", "ns");
			    		if (ens) ens.setAttribute("value", atName);
			    		var lblo = Xml.matchOneDeep(imp, "output");
			    		if (lblo) lblo.setAttribute("value", "concat('" + label + " ',$pos)");
			    		var dupEl = Xml.matchOneDeep(imp, "duplicate");
						if (dupEl) dupEl.setAttribute("origin", "instance('moddat')/" + path + atName);
			    	} else {
				    	var lbl = Xml.matchOneDeep(imp, "label");
				    	if (lbl) Xml.setNodeValue(lbl, label);
				    	var ctrl = Xml.matchOneDeep(imp, "*", "ref");
				    	if (ctrl) ctrl.setAttribute("ref", atName);
			    	}
		    	
				} else if (locNm == "entity") {
					
					atDatEl.setAttribute("ie_recId", "");
					atDatEl.setAttribute("ie_createTime", "");
					
					var tmpl = Xml.matchOne(xfRoot, "template", "name", "template-entity");
					imp = Xml.cloneNode(Xml.firstElement(tmpl), true);
    				divMain.appendChild(imp);
    				var lbl = Xml.matchOne(imp, "div", "class", "entLabel");
    				if (lbl) Xml.setNodeValue(lbl, label);
    				var ctrl = Xml.matchOne(imp, "entity");
    				ctrl.setAttribute("nodeset", atName);
    				ctrl.setAttribute("type", attr.getAttribute("type"));
    				ctrl.setAttribute("relation", attr.getAttribute("relation"));
    				
					var col = Xml.matchOne(ctrl, "column");
					var subAtts = Xml.match(attr, "attr");
					
					if (subAtts.length > 0)  {
						for (jj = 0; jj < subAtts.length; jj++) {
							var subAtt = subAtts[jj];
							var subAtName = subAtt.getAttribute("name");
							if (nmRegex.test(subAtName)) {
								atDatEl.setAttribute(subAtName, subAtt.getAttribute("default") || "");
								bnEl = Xml.elementNs(fMod, "bind", "http://www.w3.org/2002/xforms");
								bnEl.setAttribute("type", subAtt.getAttribute("type"));
								bnEl.setAttribute("nodeset", atName + "/@" + subAtName);
								
								var ccol = Xml.cloneNode(col, true);
								ctrl.appendChild(ccol);
								
								ccol.setAttribute("head", subAtt.getAttribute("label") || subAtName);
								var cimp = Data.__xfAttrTmpl(subAtt, subAtt.getAttribute("type"), false, xfRoot, ccol);
								var cctrl = Xml.matchOneDeep(cimp, "*", "ref");
				    			if (cctrl) cctrl.setAttribute("ref", "@" + subAtName);
							}
						}
					}
					
					// clear column template
					ctrl.removeChild(col);
					
				} // entity
				
	    	}
	    }
	},
	
	__xfAttrTmpl : function(attr, attType, isArr, xfRoot, xParent) {
		var tmpl, tname;
		var sel = 0;
		switch (attType) {
    		case "xsd:boolean":
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-check");
    			break;

    		case "xsd:date":
			case "xsd:dateTime":
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-date");
				break;

			case "xsd:float":
			case "ix:percent":
			case "ix:currencyUSD":
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-number");
				break;

				
    		case "xsd:NMTOKENS":
    			sel = 2;
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-select");
    			break;
    			
    		case "xsd:NMTOKEN":
    			sel = 1;
    			tname = "template-select1";
    			if (attr.getAttribute("appearance") == "full") {
    				tname += "-full";
    				sel = 2;
    			}
    			tmpl = Xml.matchOne(xfRoot, "template", "name", isArr ? "template-select1-array" : tname);
    			break;
    			
			case "ix:email":
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-email");
				break;
    			
			case "ix:http":
    			tmpl = Xml.matchOne(xfRoot, "template", "name", "template-http");
				break;
    			
    		default:
    			tname = "template-default";
    			if (attr.getAttribute("appearance") == "full") {
    				tname += "-full";
    			}
    			tmpl = Xml.matchOne(xfRoot, "template", "name", isArr ? "template-default-array" : tname);
    	 		break;
		}
    	var imp = Xml.cloneNode(Xml.firstElement(tmpl), true);
    	xParent.appendChild(imp);
    	if (sel) {
    		var itEl = Xml.matchOneDeep(imp, "item");
    		var jj = 0;
    		var atItms = Xml.match(attr, "item");
    		if (sel > 1) {
    			jj = 1;
    			Xml.setNodeValue(Xml.matchOne(itEl, "label"), atItms.length > 0 ? atItms[0].getAttribute("label") : "");
    			Xml.setNodeValue(Xml.matchOne(itEl, "value"), atItms.length > 0 ? atItms[0].getAttribute("value") : "");
    		}
    		for (; jj < atItms.length; jj++) {
    			var ne = Xml.cloneNode(itEl, true);
    			itEl.parentNode.appendChild(ne);
    			Xml.setNodeValue(Xml.matchOne(ne, "label"), atItms[jj].getAttribute("label"));
    			Xml.setNodeValue(Xml.matchOne(ne, "value"), atItms[jj].getAttribute("value"));
    		}
		}
		return imp;
	},
	
	rulesFromAttrs : function(dataElement) {
		var sendDoc = Data.genRuleDoc();
		
		// transfer attributes
		var srcDat = dataElement;
		if (!srcDat) return null;

		var dstDat = Xml.matchOne(sendDoc.documentElement, "data");
		if (!dstDat) return null;
		
		Xml.setNodeValue(dstDat, "\n"); // clear it
		
		Data.__rlAttrs(dstDat, srcDat);
		
		return sendDoc;
	},
	
	__rlAttrs : function(dstDat, srcDat) {
		var srcAtts = Xml.match(srcDat, "attr");
		for (var ii=0; ii < srcAtts.length; ii++) {
			var srcAtt = srcAtts[ii];
			var atName = srcAtt.getAttribute("name");
			if (xsdTypes.itensilXF.varName.regex.test(atName)) {
				var dstAtt = Xml.element(dstDat, "attr");
				dstAtt.setAttribute("name", atName);
				var attType = srcAtt.getAttribute("type");
				dstAtt.setAttribute("type", attType);
				if (attType.indexOf("ix:composite") == 0) {
					Data.__rlAttrs(dstAtt, srcAtt);
				} else {
					var srcItems = Xml.match(srcAtt, "item");
					for (var jj=0; jj < srcItems.length; jj++) {
						var srcItem = srcItems[jj];
						var dstItem = Xml.element(dstAtt, "item");
						dstItem.setAttribute("label", srcItem.getAttribute("label"));
						dstItem.setAttribute("value", srcItem.getAttribute("value"));
					}
				}
			}
		}
	},
	
	/**
	 * @param {Function} callback - func(resDoc, uri, xmlHttp)
	 */
	saveDoc : function(doc, uri, callback) {
		var xmlHttp = XMLBuilder.getXMLHTTP();
	    xmlHttp.open("PUT", uri, true);
	    xmlHttp.onreadystatechange = function () {
			    if (xmlHttp.readyState == 4) {
			        if (xmlHttp.status == 404) {
			        	Data.saveError();
			        } else {
			        	var resDoc = Data.xb.__getResponseDoc(xmlHttp);
			        	if (!(resDoc && resDoc.documenElement) || !App.checkError(resDoc))
			            	callback(resDoc, uri, xmlHttp);
			        }
			        xmlHttp = null; // IE enclosure clean-up
			    }
		    };
	    try {
	        if (SH.is_ie) {
                xmlHttp.send(XmlNodeExts.stringBasedCleaner(doc.xml));
                doc = null; // IE enclosure clean-up
            } else {
                xmlHttp.send(doc);
            }
	    } catch (e) {
	        Data.saveError(e);
	    }
	},
	
	saveError : function(err) {
		alert("Problem saving...");
	},

	genRuleDoc : function() {
		var xmlSrc = "<ruleset xmlns=\"http://itensil.com/ns/rules\">"
		 + "\n	<data/>"
		 + "\n"
		 + "\n	<returns>"
		 + "\n	<return id=\"pass\"/>"
		 + "\n		<return id=\"fail\"/>"
		 + "\n	</returns>"
		 + "\n"
		 + "\n	<rule id=\"default\">"
		 + "\n		<otherwise>"
		 + "\n			<return id=\"pass\"/>"
		 + "\n		</otherwise>"
		 + "\n	</rule>"
		 + "\n"
		 + "\n	<testdata xmlns=\"\"/>"
		 + "\n"
		 + "\n</ruleset>";
		 
		return Data.xb.loadXML(xmlSrc);
	},
		
	getEditGridValue : function(egFrameName, egCellRef, xmlDestNode, sheetName) {
 		var egFrame = window.frames[egFrameName];
	    if (egFrame) {
	   		return egFrame.getCellValue(egCellRef, sheetName);
	    }
	    return null;
	}

};