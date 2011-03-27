/**
 * Copyright (C) 2005 Itensil, Inc.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Author: ggongaware (at) itensil.com
 *
 */

/*@cc_on
  	@if (@_jscript_version >= 7)
  	import MSXML;
  	@end
@*/

if (SH.is_rhino)
{

/*@cc_on
    @if (!@_jscript) */
    defineClass("XMLBuilder");
/*@end
@*/

}
else
{

function XMLBuilder() {
/*@cc_on
    @if (!@_jscript) */
    this.parser = new DOMParser();
    this.serializer = new XMLSerializer();
/*@end
@*/
}

XMLBuilder.prototype.create = function() {
/*@cc_on
  	@if (@_jscript_version >= 7)
  	    var doc = new DOMDocument40();
  	    return doc;
  	@elif (@_jscript)
  	    var doc;
  	    try {
  	        doc = new ActiveXObject("Msxml2.DOMDocument.4.0");
  	    } catch (e) {
  	        doc = new ActiveXObject("Msxml2.DOMDocument");
  	    }
  	    return doc;
  	@else */
  	try {
		// DOM2
		if (document.implementation && document.implementation.createDocument) {
			var doc = document.implementation.createDocument("", "", null);
			return this.__readyInit(doc);
		}
	} catch (ex) {}
    throw new Error("This Scripting Host does not support XmlDocument objects");
/*@end
@*/
};

XMLBuilder.prototype.createWithRoot = function(rootName, nameSpace) {
/*@cc_on
  	@if (@_jscript_version >= 7)
  	    var doc = new DOMDocument40();
  	    doc.appendChild(doc.createNode(1, rootName, nameSpace));
  	    return doc;
  	@elif (@_jscript)
  	    var doc;
  	    try {
  	        doc = new ActiveXObject("Msxml2.DOMDocument.4.0");
  	    } catch (e) {
  	        doc = new ActiveXObject("Msxml2.DOMDocument");
  	    }
  	    doc.appendChild(doc.createNode(1, rootName, nameSpace));
  	    return doc;
  	@else */
  	try {
		// DOM2
		if (document.implementation && document.implementation.createDocument) {
			var doc = document.implementation.createDocument(nameSpace, rootName, null);
			// safari missing root bug
            if (doc.documentElement == null) {
                var dr = doc.createElementNS(nameSpace, rootName);
                doc.appendChild(dr);
            }
			return this.__readyInit(doc);
		}
	} catch (ex) {}
    throw new Error("This Scripting Host does not support XmlDocument objects");
/*@end
@*/
};

XMLBuilder.prototype.loadXML = function(xmlSrc) {
/*@cc_on
  	@if (@_jscript)
      	var doc = this.create();
      	doc.loadXML(xmlSrc);
      	return doc;
  	@else */
  	var doc = this.parser.parseFromString(xmlSrc, "text/xml");
  	return this.__readyInit(doc);
/*@end
@*/
};


XMLBuilder.prototype.toXML = function(root) {
/*@cc_on
    @if (@_jscript)
        return root.xml;
    @else */
    return this.serializer.serializeToString(root);
/*@end
@*/
};

XMLBuilder.getXMLHTTP = function() {
/*@cc_on
  	@if (@_jscript)
  	    var xhttp;
  	    try {
  	        xhttp = new ActiveXObject("Msxml2.XmlHttp.4.0");
  	    } catch (e) {
  	        xhttp = new ActiveXObject("Msxml2.XmlHttp");
  	    }
      	return xhttp;
  	@else */
    return new XMLHttpRequest();
/*@end
@*/
};

XMLBuilder.prototype.__readyInit = function(doc) {
	// safari crashes without this
	if (doc.readyState == null) {
		doc.readyState = 1;
		doc.addEventListener("load", function () {
			doc.readyState = 4;
			if (typeof doc.onreadystatechange == "function")
				doc.onreadystatechange();
		}, false);
	}
	return doc;
};

XMLBuilder.prototype.__getResponseDoc = function(xmlHttp) {
    var doc = null;
    if (xmlHttp.status >= 400) return null;
    if (xmlHttp.responseXML && xmlHttp.responseXML.documentElement) {
        doc = xmlHttp.responseXML;
        this.__readyInit(doc);
    } else { // incase the response mime type didn't specifically say xml
        doc = this.loadXML(xmlHttp.responseText);
    }
    return doc;
};

/**
 * @param {Object} headerSet - ByRef hash-array values to be over-written 
 * 			headerSet["X-Myhead"] = null; will return:
 * 
 * 			  headerSet["X-Myhead"] == "Value from server"
 * 			
 */
XMLBuilder.prototype.loadURI = function(sUri, headerSet) {
    var xmlHttp = XMLBuilder.getXMLHTTP();
    var async = false;
    xmlHttp.open("GET", sUri, async);
    if (SH.is_gecko) xmlHttp.overrideMimeType("text/xml");
    xmlHttp.send("");
    if (headerSet) {
    	for (var key in headerSet) {
    		headerSet[key] = xmlHttp.getResponseHeader(key);
    	}
    }
    return this.__getResponseDoc(xmlHttp);
};

// callback should be a function of the form: func(xmlDoc, arg, xmlHttp)
// callback not called on error
XMLBuilder.prototype.loadURIAsync = function(sUri, callback, arg) {
    var xmlHttp = XMLBuilder.getXMLHTTP();
    var holdThis = this;
    xmlHttp.open("GET", sUri, true);
    xmlHttp.onreadystatechange = function () {
		    if (xmlHttp.readyState == 4) {
		        if (xmlHttp.status == 404) {
		            callback(null, arg, xmlHttp);
		        } else {
		            callback(holdThis.__getResponseDoc(xmlHttp), arg, xmlHttp);
		        }
		        arg = null; xmlHttp = null; // IE enclosure clean-up
		    }
	    };
    if (SH.is_gecko) xmlHttp.overrideMimeType("text/xml");
    try {
        xmlHttp.send("");
    } catch (e) {
        callback(null, arg, xmlHttp);
    }
};

XMLBuilder.prototype.loadURIPost = function(sUri, xml) {
    var xmlHttp = XMLBuilder.getXMLHTTP();
    var async = false;
    xmlHttp.open("POST", sUri, async);
    if (SH.is_gecko) xmlHttp.overrideMimeType("text/xml");
    xmlHttp.send(xml);
    return this.__getResponseDoc(xmlHttp);
};


}

var _libBase = "";
var _libs = new Object();
var _libXhttp = null;

function loadLib(sUri) {
    sUri = _libBase + sUri;
    if (_libs[sUri]) return;
    if (SH.is_browser) {
        if (_libXhttp == null) {
            _libXhttp = XMLBuilder.getXMLHTTP();
        }
        _libXhttp.open("GET", sUri, false); //sync
        _libXhttp.send("");
        var src = _libXhttp.responseText;
        var elem = document.createElement('script');
        elem.type = 'text/javascript';
        elem.text = src;
        document.body.appendChild(elem);
    }
    _libs[sUri] = true;
}

var Xml = {

  // get inner text
  stringForNode : function(nn) {
    var ss = "";
	if (nn != null) {
    	if (nn.nodeType == 1 || nn.nodeType == 11 /*DOCUMENT_FRAGMENT_NODE*/) {
        	for (var n2 = nn.firstChild; n2 != null; n2 = n2.nextSibling) {
        		if (n2.nodeType == 3 /*TEXT*/
        		        || n2.nodeType == 4 /*CDATA_SECTION_NODE*/ ) {
        			ss += n2.nodeValue;
        		} else if (n2.nodeType == 1 /*ELEMENT*/) {
        			ss += Xml.stringForNode(n2);
        		}
        	}
        } else if (nn.nodeType == 3 || nn.nodeType == 2 /*ATTRIBUTE*/) {
            ss = nn.nodeValue;
        }
    }
	return ss;
  },

  // last child ELEMENT
  lastElement : function(node) {
    node = node.lastChild;
    while (node != null && node.nodeType != 1) {
        node = node.previousSibling;
    }
    return node;
  },

  // next ELEMENT
  nextSiblingElement : function(node) {
    node = node.nextSibling;
    while (node != null && node.nodeType != 1) {
        node = node.nextSibling;
    }
    return node;
  },

  // previous ELEMENT
  prevSiblingElement : function(node) {
    node = node.previousSibling;
    while (node != null && node.nodeType != 1) {
        node = node.previousSibling;
    }
    return node;
  },

  // first child ELEMENT
  firstElement : function(node) {
    node = node.firstChild;
    while (node != null && node.nodeType != 1) {
        node = node.nextSibling;
    }
    return node;
  },

  // append node list
  importNodes : function(doc, dstNode, srcNodes) {
    var rNode = null;
    for (var ii =0; ii < srcNodes.length; ii++) {
        var sn = srcNodes[ii];
        if (doc.importNode != null) {
            rNode = doc.importNode(sn, true);
            dstNode.appendChild(rNode);
        } else {
            rNode = sn.cloneNode(true);
            dstNode.appendChild(rNode);
        }
    }
    return rNode;
  },

  // insert one node
  nodeImport : function(doc, dstNode, srcNode, beforeNode) {
    var nn;
    if (doc.importNode != null) {
        nn = doc.importNode(srcNode, true);
        Xml.__initClone(nn, true);
        if (dstNode) dstNode.insertBefore(nn, beforeNode);
        return nn;
    } else {
        nn = srcNode.cloneNode(true);
        Xml.__initClone(nn, true);
        if (dstNode) dstNode.insertBefore(nn, beforeNode);
        return nn;
    }
  },

  nodeImportReplace : function(dstNode, srcNode) {
    if (dstNode === srcNode) return dstNode;
    var doc = dstNode.ownerDocument;
    if (doc.importNode != null) {
        var nn = doc.importNode(srcNode, true);
        dstNode.parentNode.replaceChild(nn, dstNode);
        return nn;
    } else {
        dstNode.parentNode.replaceChild(srcNode, dstNode);
        return srcNode;
    }
  },

  // copy attributes
  copyElementAttrs : function(srcElem, dstElem) {
    var atts = srcElem.attributes;
    for (var ii = 0; ii < atts.length; ii++) {
        var aa = atts[ii];
        if (aa.nodeName != "xmlns") {
            if (SH.is_gecko) {
                dstElem.setAttributeNS(aa.namespaceURI, aa.nodeName, aa.nodeValue);
            } else dstElem.setAttribute(aa.nodeName, aa.nodeValue);
        }
    }
  },

  // local tag name
  getLocalName : function(node) {
    return node.localName == null ?
        (node.baseName == null ? node.nodeName : node.baseName) :
        node.localName;
  },

  // if the types, names, and innerText match
  nodesEqual : function(n1, n2) {
    return n1.nodeType == n2.nodeType
            && n1.namespaceURI == n2.namespaceURI
            && Xml.getLocalName(n1) == Xml.getLocalName(n2)
            && Xml.stringForNode(n1) == Xml.stringForNode(n2);
  },

  // index of matching in the set, -1 for no match
  nodeEqualInSet : function(n1, ns) {
    var n1Name = Xml.getLocalName(n1);
    var n1Str = Xml.stringForNode(n1);
    for (var ii = 0; ii < ns.length; ii++) {
        var n2 = ns[ii];
        if (n1.nodeType == n2.nodeType
                && n1.namespaceURI == n2.namespaceURI
                && n1Name == Xml.getLocalName(n2)
                && n1Str == Xml.stringForNode(n2)) {
            return ii;
        }
    }
    return -1;
  },

  // set the inner text or value depending on type
  setNodeValue : function(node, val) {
    switch (node.nodeType) {
        case 1: // ELEMENT
            var doc = node.ownerDocument;
            var kid = node.firstChild;
            while (kid != null) {
                node.removeChild(kid);
                kid = node.firstChild;
            }
            node.appendChild(doc.createTextNode(val));
            break;
        case 2: // ATTRIBUTE
            if (node.constructor === XmlAttribute) {
                 node.attNode.nodeValue = val;
            }
        case 3: // TEXT_NODE
        default:
            node.nodeValue = val;
            break;
    }
  },

  /*
  For match and qualify:
    parentNode - the start context
    localName - use '*' for any
    attrName - optional
    attrVal - optional
  */
  match : function(parentNode, localName, attrName, attrVal) {
    var matches = [];
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1
                && Xml.qualify(kid, localName, attrName, attrVal)) {
            matches.push(kid);
        }
    }
    return matches;
  },

  // get the first match
  matchOne : function(parentNode, localName, attrName, attrVal) {
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1
                && Xml.qualify(kid, localName, attrName, attrVal)) {
            return kid;
        }
    }
    return null;
  },

  // search children and descendants
  matchDeep : function(parentNode, localName, attrName, attrVal) {
    var matches = [];
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1
                && Xml.qualify(kid, localName, attrName, attrVal)) {
            matches.push(kid);
        }
        var deep = Xml.matchDeep(kid, localName, attrName, attrVal);
        for (var jj = 0; jj < deep.length; jj++) {
            matches.push(deep[jj]);
        }
    }
    return matches;
  },

  matchOneDeep : function(parentNode, localName, attrName, attrVal) {
    var kids = parentNode.childNodes;
    for (var ii = 0; ii < kids.length; ii++) {
        var kid = kids[ii];
        if (kid.nodeType == 1
                && Xml.qualify(kid, localName, attrName, attrVal)) {
           	return kid;
        }
        var deep = Xml.matchOneDeep(kid, localName, attrName, attrVal);
        if (deep) return deep;
    }
    return null;
  },


  // does this node meet tests?
  qualify : function(node, localName, attrName, attrVal) {
    if (localName != "*" && Xml.getLocalName(node) != localName) {
        return false;
    }
    if (attrName != null) {
        var an = node.getAttributeNode(attrName);
        if (an == null || (attrVal != null && an.nodeValue != attrVal)) {
            return false;
        }
    }
    return true;
  },

  // IsAncestor
  isAncestor : function(anc, desc) {
    var nd = desc.parentNode;
    while (nd != null) {
        if (nd === anc) return true;
        nd = nd.parentNode;
    }
    return false;
  },

  // Insert, refList is an array of names
  insertChildBefore : function(parNod, newChild, refList) {
    var node = parNod.firstChild;
    while (node != null) {
        if (node.nodeType == 1) {
            var tag = Xml.getLocalName(node);
            for (var ii = 0; ii < refList.length; ii++) {
                if (tag == refList[ii]) {
                    parNod.insertBefore(newChild, node);
                    return;
                }
            }
        }
        node = node.nextSibling;
    }
    parNod.appendChild(newChild);
 },

 element : function(parent, name, value) {
     var elem = parent.ownerDocument.createElement(name);
     parent.appendChild(elem);
     if (value) Xml.setNodeValue(elem, value);
     return elem;
 },
 
 elementNs : function(parent, name, namespace, value) {
     var elem = SH.is_ie ?
		parent.ownerDocument.createNode(1, name, namespace) :
	 	parent.ownerDocument.createElementNS(namespace, name);
     parent.appendChild(elem);
     if (value) Xml.setNodeValue(elem, value);
     return elem;
 },
 
 jsToXml : function(jsObj, rootName) {
 	if (!rootName) rootName = "object";
 	var xb = new XMLBuilder();
 	var doc = xb.createWithRoot(rootName, "");
 	var parElem = doc.documentElement;
 	for (var ii in jsObj) {
 		if (ii.charAt(0) == "_") continue; // skip private
		var aa = jsObj[ii];
		Xml.__jsToXml(aa, Xml.element(parElem, ii), ii);
	}
	return doc;
 },
 
 cloneNode : function(node, deep) {
 	var cn = node.cloneNode(deep);
 	if (SH.is_ie && cn.nodeType == 1) {
 		cn.removeAttribute("_itenNx_");
 		if (deep) {
 			var dns = Xml.matchDeep(cn, "*", "_itenNx_");
 			for (var ii=0; ii < dns.length; ii++) {
 				dns[ii].removeAttribute("_itenNx_");
 			}
 		}
 	}
 	Xml.__initClone(cn, deep);
 	return cn;
 },
 
 __initClone : function(cn, deep) {
 	if (SH.is_ie && cn.nodeType == 1) {
 		cn.removeAttribute("_itenNx_");
 		if (deep) {
 			var dns = Xml.matchDeep(cn, "*", "_itenNx_");
 			for (var ii=0; ii < dns.length; ii++) {
 				dns[ii].removeAttribute("_itenNx_");
 			}
 		}
 	}
 },
 
 __jsToXml : function(jsObj, parElem, name) {
 	if (!name) name = "object";
	if (typeof(jsObj) == "function") { }
	else if (typeof(jsObj) == "object") {
		if (jsObj.constructor === Array)  {
			for (var jj = 0; jj < jsObj.length; jj++)
				Xml.__jsToXml(jsObj[jj], parElem, name);
		} else if (jsObj.constructor === Date) {
			Xml.element(parElem, name, jsObj.toString());
		} else {
			for (var ii in jsObj) {
				if (ii.charAt(0) == "_")  continue; // skip private
				var aa = jsObj[ii];
				Xml.__jsToXml(aa, Xml.element(parElem, ii), ii);
			}
		}
	}
	else {
		Xml.element(parElem, name, jsObj.toString());
	}
 }

};


// make a new one of these for each document
function XmlNodeExts() {
    this.assoc = [];
    this.aUniq = 0;
    this.docRootExt = {};
}

XmlNodeExts.prototype.getProp = function(node, name) {
    return this.__getExtObj(node)[name];
};

XmlNodeExts.prototype.setProp = function(node, name, value) {
    this.__getExtObj(node)[name] = value;
};

XmlNodeExts.prototype.__getExtObj = function(node) {
    if (node.constructor === XmlAttribute || !SH.is_ie) {
        var eob = node._itenNx;
        if (eob == null) {
            eob = new Object();
            node._itenNx = eob;
        }
        return eob;
    } else if (node.nodeType == 9 ) { // document
    	return this.docRootExt;
	} else {
        var aIdx = node.getAttribute("_itenNx_");
        var eob;
        if (aIdx == null) {
            eob = new Object();
            aIdx = this.aUniq++;
            this.assoc[aIdx] = eob;
            node.setAttribute("_itenNx_", aIdx);
        } else {
        	aIdx = parseInt(aIdx);
            eob = this.assoc[aIdx];
            if (!eob) {
            	eob = new Object();
            	this.assoc[aIdx] = eob;
            }
        }
        return eob;
    }
};

XmlNodeExts.prototype.getAttribute = function(node, attNode) {
    return this.getAttributeByName(node, attNode.nodeName, attNode);
};

XmlNodeExts.prototype.getAttributeByName = function(node, attName, attNode) {
    var eob = this.__getExtObj(node);
    var name = "@" + attName;
    var att = eob[name];
    if (att == null) {
    	if (!attNode) attNode = node.getAttributeNode(attName);
        att = new XmlAttribute(node, attNode);
        eob[name] = att;
    }
    return att;
};

XmlNodeExts.prototype.reset = function() {
	this.assoc = [];
};

// static
XmlNodeExts.stringBasedCleaner = function(str) {
    return str.replace(/[ ]?_itenNx_="[^"]*"/g, "");
};

function XmlAttribute(ownerElement, attNode) {
    this.attNode = attNode;
    this.namespaceURI = attNode.namespaceURI;
    this.nodeName = attNode.nodeName;
    this.ownerElement = ownerElement;
    this.ownerDocument = ownerElement.ownerDocument;
    this.nodeValue = attNode.nodeValue;
}

XmlAttribute.prototype.syncValue = function() {
    this.nodeValue = this.attNode.nodeValue;
};

XmlAttribute.prototype.nodeType = 2;

XmlAttribute.createNode = function(ownerElement, name, namespace, value) {
	var attNode;
	if (namespace) {
		if (SH.is_ie) {
			attNode = ownerElement.ownerDocument.createNode(2, name, namespace);
		} else {
			attNode = ownerElement.ownerDocument.createAttributeNS(namespace, name);
		}
	} else {
		attNode = ownerElement.ownerDocument.createAttribute(name);
	}
	ownerElement.setAttributeNode(attNode);
	
	if (value) attNode.nodeValue = value;
	return new XmlAttribute(ownerElement, attNode);
};


/**
 * Legacy function name map
 */

var xmlStringForNode = Xml.stringForNode;
var xmlLastElement = Xml.lastElement;
var xmlNextSiblingElement = Xml.nextSiblingElement;
var xmlFirstElement = Xml.firstElement;
var xmlImportNodes = Xml.importNodes;
var xmlNodeImport = Xml.nodeImport;
var xmlCopyElementAttrs = Xml.copyElementAttrs;
var xmlGetLocalName = Xml.getLocalName;
var xmlNodesEqual = Xml.nodesEqual;
var xmlNodeEqualInSet = Xml.nodeEqualInSet;
var xmlSetNodeValue = Xml.setNodeValue;
