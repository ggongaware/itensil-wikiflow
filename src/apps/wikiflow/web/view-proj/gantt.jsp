<%@ page import="itensil.web.HTMLEncode"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%@ include file="../include/title.inc.jsp" %> - Project: <%= HTMLEncode.encode("" + request.getAttribute("proj")) %></title>
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

<script type="text/javascript" src="../js/brwsr/Gantt.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>

<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>
<script type="text/javascript" src="../js/brwsr/StatusGantt.js"></script>
<script type="text/javascript" src="../js/brwsr/ProjectGantt.js"></script>

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
<link rel="stylesheet" type="text/css" href="../css/Gantt.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/ProcMap.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />
<link rel="stylesheet" type="text/css" href="../css/Entity.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />
<style>
div.ganTree {
	width: 500px;
}
</style>
<script type="text/javascript">
//<![CDATA[

var project = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("proj")) %>";
var formUri = "../view-wf/flow.xfrm.jsp";
var modelSvcUri = "../mod/";

function setup() {

	var ps = new PanelSetSingle();
    ps.initHelp(App.chromeHelp);
    App.addDispose(ps); // for memory leak prevention
    App.init();
	
	var panStat = new Panel("Project Gantt", false);
    ps.add(panStat);
    
    // set a header zone div
    ps.header = document.getElementById("mast");
    
    // draw it, then the panel content nodes will be available
    ps.render(document.body, true); // stick it directly in the body for auto-strech
    
    // Mode tabs
    Modes.drawProjModes(document.getElementById("mast"), "projGantt");
    
    var headElem = makeElement(null, "div", "statHead flowHead");
    panStat.setGutters(headElem);
    var btn = makeElement(headElem, "button", "xfctrl", null, null, {type:'button'});
    var label = makeElement(btn, "label", null, null, null);
    label.style.width = "70px";
    if (SH.is_ie) btn.style.width = "92px";
    makeElementNbSpd(label, "span", "mbIco mb_savIco");
    makeElement(label, "span", null, "Save Project");
    setEventHandler(btn, "onclick", function() {
			ProjectGantt.savePlans();
    	});
    
    btn = null; label = null; headElem = null;
   
    // Status!
    ProjectGantt.renderProject(project, panStat.contentElement, panStat);
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% request.setAttribute("help", "Timeline"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>