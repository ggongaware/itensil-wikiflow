<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Embed Run</title>
<script type="text/javascript" src="../js/ScriptHost.js"></script>
<script type="text/javascript" src="../js/XMLBuilder.js"></script>
<script type="text/javascript" src="../js/CoreUtil.js"></script>
<script type="text/javascript" src="../js/brwsr/Util.js"></script>
<script type="text/javascript" src="../js/brwsr/Menu.js"></script>
<script type="text/javascript" src="../js/brwsr/DND.js"></script>
<script type="text/javascript" src="../js/brwsr/Dialog.js"></script>
<script type="text/javascript" src="../js/brwsr/Tree.js"></script>
<script type="text/javascript" src="../js/brwsr/Grid.js"></script>
<script type="text/javascript" src="../js/brwsr/ComboBox.js"></script>
<script type="text/javascript" src="../js/brwsr/Calendar.js"></script>
<script type="text/javascript" src="../js/brwsr/Panel.js"></script>

<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/Rules.js"></script>
<script type="text/javascript" src="../js/Data.js"></script>
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

<script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

<script type="text/javascript" src="../js/brwsr/ProcMap.js"></script>
<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcCanvas.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/ProcMap.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />
<link rel="stylesheet" type="text/css" href="../css/Entity.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />

<%@ include file="../include/kiosk.inc.jsp" %>

<style>

<% if (!isEmbed) { %>
#wikBody div.wikiViewBox {
	font-size: 16px;
}

#wikBody .wikiHead h1 {
	font-size: 36px;
}

#wikBody .wikiView h1 {
	font-size: 26px;
}
<% } %>
</style>
<script type="text/javascript">
//<![CDATA[
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%= HTMLEncode.jsQuoteEncode(Check.emptyIfNull(request.getParameter("activity"))) %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(Check.emptyIfNull(request.getParameter("step"))) %>";
var formUri = "../view-wf/flow.xfrm.jsp";
//var simXfrmUri = "../itensil_js/process/simulate.xfrm";
var trackSvcUri = "../trak/";
var modelSvcUri = "../mod/";

function setup() {

	ActivityTree.isMeet = (App.activeFlow.indexOf("/meeting/") > 0);
	ActivityTree.isCourse = !ActivityTree.isMeet && (App.activeFlow.indexOf("/course/") > 0); 
	
	App.init();
	
	Modes.mode = "run";

    var pCan = new PmCanvas(null,
            makeElement(null,"div"),
            formUri,
            modelSvcUri + "getModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            App.activeFlow);

	App.resolver.modelUri = Uri.parent(App.activeFlow);

   	// pCan.panelWork = panWiki;
  	pCan.hidden = true;
  	
    App.addDispose(pCan);
    var proc = new PmModel(pCan, App.activeFlow, pCan.xfrm);
        
    btn = null; label = null; headElem = null;

    // Load wiki before active Activity
    var wikViews = [{isStatic:true, isFullPage:true, isWorkzone:true,
                linkWiki : ("../fil" + Uri.absolute(Uri.root(App.activeFlow), "Reference.kb"))}];
    var wik = new Wiki("../fil" + Uri.absolute(Uri.parent(App.activeFlow), "activities/activities.kb"), 
    		document.getElementById('wikBody'), null,
            wikViews, {preSearchCss:"searchActi", noSearch : true});
    App.addDispose(wik);
    pCan.wiki = wik;

    // Activity tree
    ActivityTree.renderActivities("assigned", App.activeFlow, makeElement(null,"div"));

    
   	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% if (isEmbed) { %>
<script>
var rect = getVisibleBounds();
document.write('<div id="wikBody" style="position:absolute;left:0px;top:0px;width:' + rect.w  + 'px;height:' + rect.h  +  'px"></div>');
</script>
<% } else { %>
<div id="wikBody" style="position:absolute;top:0px;left:20px;width:1000px;height:500px">
</div>
<% } %>
</body>
</html>
