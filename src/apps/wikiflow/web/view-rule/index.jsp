<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Rules: <%= HTMLEncode.encode(UriHelper.name(request.getParameter("uri"))) %></title>

 <!-- Core Utils-->
 <script type="text/javascript" src="../js/ScriptHost.js"></script>
 <script type="text/javascript" src="../js/XMLBuilder.js"></script>
 <script type="text/javascript" src="../js/CoreUtil.js"></script>
 <script type="text/javascript" src="../js/xpath.js"></script>
 <script type="text/javascript" src="../js/XMLEdit.js"></script>

 <!-- Main Widgets -->
 <script type="text/javascript" src="../js/brwsr/Util.js"></script>
 <script type="text/javascript" src="../js/brwsr/Menu.js"></script>
 <script type="text/javascript" src="../js/brwsr/DND.js"></script>
 <script type="text/javascript" src="../js/brwsr/Tree.js"></script>
 <script type="text/javascript" src="../js/brwsr/ComboBox.js"></script>
 <script type="text/javascript" src="../js/brwsr/Calendar.js"></script>
 <script type="text/javascript" src="../js/brwsr/Panel.js"></script>
 <script type="text/javascript" src="../js/brwsr/Dialog.js"></script>

 <!-- XForms Includes -->
 <script type="text/javascript" src="../js/XSchema.js"></script>
 <script type="text/javascript" src="../js/xf/XFCommon.js"></script>
 <script type="text/javascript" src="../js/xf/XFActions.js"></script>
 <script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
 <script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/Rules.js"></script>
<script type="text/javascript" src="../js/Data.js"></script>

 <!-- For the Developer Structure link -->
 <script type="text/javascript" src="../js/brwsr/Grid.js"></script>

 <script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
 <script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

 <script type="text/javascript" src="../js/xf/brwsr/XPathEditor.js"></script>
 <script type="text/javascript" src="../js/brwsr/XMLIDE.js"></script>
 
 <script type="text/javascript" src="../js/brwsr/RuleCanvas.js"></script>

 <script type="text/javascript" src="../js/App.js"></script>

 <!-- styles -->
 <link rel="stylesheet" type="text/css" href="../css/Menu.css" />
 <link rel="stylesheet" type="text/css" href="../css/Tree.css" />
 <link rel="stylesheet" type="text/css" href="../css/Panel.css" />
 <link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
 <link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
 <link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />

 <link rel="stylesheet" type="text/css" href="../css/Grid.css" />
 <link rel="stylesheet" type="text/css" href="../css/XMLIDE.css" />
 <link rel="stylesheet" type="text/css" href="../css/Rules.css" />
 
 <link rel="stylesheet" type="text/css" href="../css/App.css" />
 <link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />

<script type="text/javascript">
//<![CDATA[
//SH.debug = true;

var ruleUri = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("uri")) %>";

function setup() {
    var ps = new PanelSetVSplit(false /* false=major area right */, 290);
    ps.initHelp(App.chromeHelp);
    App.addDispose(ps); // for memory leak prevention
    App.init();

    // set a header zone div
    ps.header = document.getElementById("mast");

    var pan1 = new Panel("Rule Fields", false /* can't close */);
    ps.addMinor(pan1);
    
    var pan2 = new Panel("Rule Returns", false);
    ps.addMinor(pan2);
    
    var pan3 = new Panel("Sub Rules", false);
    ps.addMinor(pan3);

    var panCan = new Panel("Rule Canvas", false);
    ps.addMajor(panCan);
    
    var panTest = new Panel("Rule Test Data", false);
    ps.addMajor(panTest);
    

    // draw it, then the panel content nodes will be available
    ps.render(document.body, true); // stick it directly in the body for auto-strech
    pan2.setHeight(50);
    pan3.setHeight(50);
    
    panTest.setHeight(180);
    
    var rc = new RuleCanvas(panCan, "rule-edit.xfrm.jsp", "../fil" + ruleUri);
    App.addDispose(rc);
    
    var tmpl = rc.xfrm.getIxTemplate("attrManager");
	tmpl.renderTemplate(null, pan1.contentElement);
	tmpl.addEventListener("xforms-value-changed", new RuletAttrListener(rc));
	
	tmpl = rc.xfrm.getIxTemplate("returnManager");
	tmpl.renderTemplate(null, pan2.contentElement);
	
	tmpl = rc.xfrm.getIxTemplate("subManager");
	tmpl.renderTemplate(null, pan3.contentElement);
	
	/*
	makeElement(panTest.contentElement, "blockquote", "", 
		"Coming soon. This panel will let you fill in values for fields and trace the rule processing.");
	*/
	rc.initTestXform(panTest);
	
	App.unloadListeners.push(RuleCanvas.unloadHandler);
	
	RuleCanvas.live = rc;

	setElementText(document.getElementById("xideStat"), "Editing: " + ruleUri);
}

var Wiki = {};

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


//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<div id="mast" style="font-weight:bold;padding:2px 2px 0px 4px;height:30px">
<table style="width:99%"><tr valign="top">
<td><button onclick="RuleCanvas.live.doSave()" class="xide">Save</button></td>
<td style="padding-top:3px"><div class="xideMsgMast" id="xideStat">Loading...</div></td>
<td style="font-size:13px;font-family:arial;padding:4px 2px 1px 4px;text-align:right;width:99%;">Rule Designer</td>
</tr>
</table>
</div>

</body>
</html>