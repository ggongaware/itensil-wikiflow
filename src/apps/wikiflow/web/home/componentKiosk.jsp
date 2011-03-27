<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page import="itensil.repository.*,itensil.security.*,itensil.security.hibernate.*,itensil.io.HibernateUtil,itensil.util.Check,itensil.web.*,java.util.*,itensil.workflow.activities.UserActivities,itensil.workflow.activities.state.Activity,itensil.workflow.activities.state.FlowState"
				%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Embed Home</title>
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

#actBody div.toggle {
	display:none;
}

body {
	font-family:arial,helvetica,sans-serif;
}

#actBody .tree div.trRowRoot,
#actBody .tree div.openRoot,
#actBody .tree div.trRowRootLast,
#actBody .tree div.openRootLast,
#actBody .tree div.trRow,
#actBody .tree div.open,
#actBody .tree div.trRowLast,
#actBody .tree div.openLast,
#actBody .tree div.kids,
#actBody .tree div.tail {
    background-image: none;
}

<%if (!isEmbed) {%>
#actBody th {
	font-size: 12px;
}

#actBody .tree {
	font-size: 14px;
}

#actBody .tree div.label {
	left: 40px;
}

#actBody div.activityTree .actItem .actLabel {
	width: 700px;
}

#actBody div.minorHead {
	font-size: 22px;
	margin-bottom: 20px;
}
<%}%>
</style>



<script type="text/javascript"> 
//<![CDATA[
           
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("activity")) %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("step")) %>";
var formUri = "../view-wf/flow.xfrm.jsp";


function displayEntity(name, id) {
	var ps = App.getPanelSet(0);
    
    panComp = new Panel("Entity", false);
    ps.addMajor(panComp);
    ps.render(document.body, true);
	 
    var entityUri = Uri.absolute("/home/entity", name);
    var modelUri = Uri.absolute(entityUri, "model.entity");
    
	var config = {relActs : 1};
	if(id) {
	   	config = {relActs : 1, idSet:[id]};
	}

    var eg = new EntityGrid.forPanel(panComp, modelUri, name, config);
    App.addDispose(eg);
}



function showComponentForm() {
    try {
        location.href = '../home/componentSelect.jsp?appId=<%=request.getParameter("appId")%>&appProcessId=<%=request.getParameter("appProcessId")%>';
    } catch(e) {}
}
           
           
function display(type, name, id) {
    if(type == 'ENT') {
    	displayEntity(name, id);
    }
	else if (type == 'ACT') {
        location.href = '../act/page?kiosk=1&embed=1&activity=' + id;
		// Activity tree
	   	App.addDispose(UserTree);
		App.addDispose(ActivityTree);
	}
}


function setup() {
	App.init();
	var appId = '<%=request.getParameter("appId")%>';
	var appProcessId = '<%=request.getParameter("appProcessId")%>';

	// Check mapping to see if waveId already has a complete map.
	var uri = "../index/find?appId=" + Uri.escape(appId) + "&appProcessId=" + Uri.escape(appProcessId);
	var resDoc = Data.xb.loadURI(uri);

	var mapId;
	var cmpType;
	var cmpName;
	var cmpId;
	if(resDoc) {
		if (Xml.matchOne(resDoc.documentElement, "map")) {
			map = Xml.matchOne(resDoc.documentElement, "map");
		    mapId = map.getAttribute("mapId");
		    cmpType = map.getAttribute("cmpType");
		    cmpName = map.getAttribute("cmpName");
		    cmpId = map.getAttribute("cmpId");
		}
		if(cmpName || cmpType == "ACT") {
			display(cmpType, cmpName, cmpId);
		}
		else {
		    try {
		        location.href = '../home/componentSelect.jsp?appId=<%=request.getParameter("appId")%>&appProcessId=<%=request.getParameter("appProcessId")%>';
		    } catch(e) {}
		}
	}
}
           
//]]>
</script> 


</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% if (isEmbed) { %>
<div id="actBody">
</div>
<% } else { %>
<div id="actBody" style="position:absolute;top:30px;left:100px;width:1000px;height:900px">
</div>
<% } %>
</body>
</html>
