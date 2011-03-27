<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Status</title>
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
<script type="text/javascript" src="../js/brwsr/StatusTree.js"></script>

<script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

<script type="text/javascript" src="../js/brwsr/ProcMap.js"></script>
<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcCanvas.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcOutline.js"></script>

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

<script type="text/javascript">
//<![CDATA[
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%
	String actId = (String)request.getAttribute("activityId");
	if (actId == null) actId = request.getParameter("activity");
	out.print(HTMLEncode.jsQuoteEncode(actId));
 %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("step")) %>";
ProcOutline.meetDraft = "<%= HTMLEncode.jsQuoteEncode((String)request.getAttribute("meet-draft")) %>";
ProcOutline.meetLDraft = "<%= HTMLEncode.jsQuoteEncode((String)request.getAttribute("meet-ldraft")) %>";

var formUri = "../view-wf/flow.xfrm.jsp";
var trackSvcUri = "../trak/";
var modelSvcUri = "../mod/";

function setup() {
	ActivityTree.isMeet = StatusTree.isMeet = (App.activeFlow.indexOf("/meeting/") > 0);
	ActivityTree.isCourse = !ActivityTree.isMeet && (App.activeFlow.indexOf("/course/") > 0); 
	
    var ps = App.getPanelSet(StatusTree.isMeet ? 240 : 320);


	var panTodo;
	if (!StatusTree.isMeet) {
		panTodo = new Panel("To Do List", false);
    	ps.addMinor(panTodo);
   	}
    
    
    var panTeam = new Panel(StatusTree.isMeet ? "Team" : "Team Roles", false);
    ps.addMinor(panTeam);

    var panFiles = new Panel("Files", false);
    if (!StatusTree.isMeet) panFiles.setShrink(true);
    ps.addMinor(panFiles);

    var panStat = new Panel("Status Report", false);
    ps.addMajor(panStat);
    
    var panOut, panWiki = null;
    if (StatusTree.isMeet) {
	    panOut = new Panel("Additions Outline", false);
	    ps.addMajor(panOut);
	} else {
	//	panWiki = new Panel("Activity Log", false, "workzone");
    //	ps.addMajor(panWiki);
	}

    ps.render(document.body, true);
    
    if (StatusTree.isMeet) panOut.setHeight(100);
    
    // Mode tabs
    Modes.drawModes(document.getElementById("mast"), "stat");
    

    var pCan = new PmCanvas(null,
            makeElement(null,"div"),
            formUri,
            modelSvcUri + "getModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            App.activeFlow);
    pCan.panelSet = ps;
    pCan.hidden = true;
    
    App.resolver.modelUri = Uri.parent(App.activeFlow);
    
    var proc = new PmModel(pCan, App.activeFlow, pCan.xfrm);
    
    var headElem = makeElement(null, "div", "statHead flowHead");
    panStat.setGutters(headElem);
    var btn = makeElement(headElem, "button", "xfctrl", null, null, {type:'button'});
    var label = makeElement(btn, "label", null, null, null);
    label.style.width = "70px";
    if (SH.is_ie) btn.style.width = "92px";
    makeElementNbSpd(label, "span", "mbIco mb_savIco");
    makeElement(label, "span", null, "Save Status");
    setEventHandler(btn, "onclick", function() {
			StatusTree.savePlans();
    	});
    
    btn = null; label = null; headElem = null;
    
    var teamObj;
    if (StatusTree.isMeet) {
    	makeElement(panTeam.contentElement, "div", "minorHead", "Team");
	    teamObj = new TeamRoster(
		    	makeElement(panTeam.contentElement, "div", "meetTeam"), 
		    	dndGetCanvas(document.body));
	    
	    teamObj.initAddButton(panTeam.contentElement, "Add Member...");
	    
	    teamObj.addFromFile(Uri.parent(App.activeFlow));
	    
    	App.addDispose(teamObj);  
    } else {
    	// Team view
	    PmCanvas.renderTeam(proc, panTeam.contentElement);
	    teamObj = PmCanvas.teamRoster;
	   	App.activityListeners.push(function(node) {
	    		PmCanvas.teamRoster.clearTeam();
	    		// team before stat, for role maps
	            PmCanvas.renderTeam(proc, panTeam.contentElement);
	            StatusTree.refreshStatus(node.getAttribute("id"));
	        });
    }
    
    App.addDispose(pCan);
    
    
    // Activity tree
    if (!StatusTree.isMeet) {
    	ActivityTree.renderActivities(App.edu ? "submitted" : "assigned", App.activeFlow, panTodo.contentElement);
    } else {
    	App.activeActivityNode = ActivityTree.activityInfo(App.activeActivityId);
    }
    
    // Status!
    StatusTree.renderStatus(proc, App.activeActivityId, teamObj, panStat.contentElement);
    
    // Additions outline
    if (StatusTree.isMeet) {
    	ProcOutline.panelized(pCan, panOut, "Save Additions", "vert3");
    	ProcOutline.teamRoaster = teamObj;
    	App.unloadListeners.push(ProcOutline.isDirty);
    }
    
    // File Tree
    var fileMod = FileTree.renderFiles("Process", Uri.parent(App.activeFlow), true, panFiles.contentElement,
                [{title:"Community", uri:"/", showRoot:false}]);

	if (StatusTree.isMeet) {
		fileMod.activityFiles(App.activeActivityNode);
	} else {
	 	App.activityListeners.push(FileTree.activityListenActivityFiles(fileMod));
	}

	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
	
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%  
	String furi = "" + request.getAttribute("flowUri");
	String name = UriHelper.name(UriHelper.getParent(furi));
    request.setAttribute("crumb", name);
    request.setAttribute("help", furi.indexOf("/meeting/") > 0 ? "Meeting Status" : "Status"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
