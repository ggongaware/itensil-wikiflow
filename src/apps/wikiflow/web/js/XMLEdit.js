/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 *
 * @requires XMLBuilder.js
 *
 */

var XMLEdit = { xb : new XMLBuilder(), __xpaths : new Object() };

XMLEdit.appendXML = function(node, xmlStr) {
    var parsedDoc = XMLEdit.stringToDoc(node, xmlStr);
    if (parsedDoc.documentElement != null &&
            parsedDoc.documentElement.namespaceURI !=
            "http://www.mozilla.org/newlayout/xml/parsererror.xml") {
        return Xml.importNodes(
            node.ownerDocument, node, parsedDoc.documentElement.childNodes);
    }
    return null;
};

XMLEdit.stringToDoc = function (node, xmlStr) {
    var nameSpaces = XMLEdit.__soakUpNamespaces(node);
	var rootTag = "<root ";
	for (var xmlns in nameSpaces) {
	    rootTag += xmlns + "=\"" + nameSpaces[xmlns] + "\" ";
	}
    var parsedDoc = XMLEdit.xb.loadXML(rootTag + ">" + xmlStr + "</root>");
    return parsedDoc;
};

XMLEdit.__soakUpNamespaces = function(node) {

    // soak up the namespaces
    var nameSpaces = new Object();
    var n = node;
    var i;
	while (n != null && n.nodeType == 1 /*ELEMENT*/) {
		var atts = n.attributes;
		for (i = 0; i < atts.length; i++) {
			var a = atts[i];
			var aname = a.nodeName;
			if (aname.indexOf("xmlns") >= 0) {
				if (nameSpaces[aname] == null) { // keep closest
				    nameSpaces[aname] = a.nodeValue;
				}
			}
		}
		n = n.parentNode;
	}
	return nameSpaces;
};

XMLEdit.replaceXML = function(node, xmlStr) {
    var parsedDoc = XMLEdit.stringToDoc(node, xmlStr);
    if (parsedDoc.documentElement != null &&
            parsedDoc.documentElement.namespaceURI !=
            "http://www.mozilla.org/newlayout/xml/parsererror.xml") {
        var rNode = xmlFirstElement(parsedDoc.documentElement);
        var pn = node.parentNode;
        var doc = node.ownerDocument;
        var impNode = xmlNodeImport(doc, pn, rNode, node);
        pn.removeChild(node);
        return impNode;
    }
    return null;
};

XMLEdit.outterXML = function(node) {
    if (node.nodeType == 9 /* DOCUMENT */) {
        return XMLEdit.xb.toXML(node);
    }
    var doc = node.ownerDocument;
    if (node === doc.documentElement) {
        return XMLEdit.xb.toXML(doc);
    }
    var nameSpaces = XMLEdit.__soakUpNamespaces(node);
    var rootTag = "<root ";
	for (var xmlns in nameSpaces) {
	    rootTag += xmlns + "=\"" + nameSpaces[xmlns] + "\" ";
	}
    var transDoc = XMLEdit.xb.loadXML(rootTag + "></root>");
    var hostElem = transDoc.documentElement;
    xmlNodeImport(transDoc, hostElem, node);
    var str = XMLEdit.xb.toXML(transDoc);
    return str.substring(str.indexOf(">") + 1, str.lastIndexOf("<"));
};

XMLEdit.renameElement = function(element, name) {
    var doc = element.ownerDocument;
    var p = element.parentNode;
    var rn = doc.createElement(name);
    var i;
    var atts = element.attributes;
	for (i = atts.length - 1; i >= 0; i--) {
		rn.setAttribute(atts[i].nodeName, atts[i].nodeValue);
	}
    var kid = element.firstChild;
    while (kid != null) {
        rn.appendChild(kid);
        kid = element.firstChild;
    }
    p.replaceChild(rn, element);
    return rn;
};

XMLEdit.getPath = function(node) {
    var n = node;
    var pth = [];
    do {
        pth.unshift(n.nodeName);
        n = n.parentNode;
    } while (n != null && n.nodeType == 1);
    return pth;
};


XMLEdit.isAncestor = function(anc, desc) {
    var nd = desc.parentNode;
    while (nd != null) {
        if (nd === anc) return true;
        nd = nd.parentNode;
    }
    return false;
}


XMLEdit.isDescendant = function(anc, desc) {
    var nd = anc.firstChild;
    while (nd != null) {
        if (nd === desc) return true;
        if (XMLEdit.isDescendant(nd, desc)) return true;
        nd = nd.nextSibling;
    }
    return false;
};


XMLEdit.selectSingleNode = function(contextNode, xpath) {
    var ns = XMLEdit.selectNodes(contextNode, xpath);
    if (ns.length > 0) {
        return ns[0];
    } else {
        return null;
    }
};

XMLEdit.selectNodes = function(contextNode, xpath) {
    var exp = XMLEdit.__xpaths[xpath];
    if (exp == null) {
        var parser = new XPathParser();
        exp = parser.parse(xpath);
        XMLEdit.__xpaths[xpath] = exp;
    }
    var context = new XPathContext();
    context.expressionContextNode = contextNode;
    var res = exp.evaluate(context);
    if (res.constructor === XNodeSet) {
        return res.toArray();
    } else {
        return [];
    }
};
