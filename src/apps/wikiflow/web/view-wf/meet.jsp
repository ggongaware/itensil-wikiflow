<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Knowledge App</title>
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
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

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

<link rel="stylesheet" type="text/css" href="../css/App.css" />

<script type="text/javascript">
//<![CDATA[
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode((String)request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%
	String actId = (String)request.getAttribute("activityId");
	if (actId == null) actId = request.getParameter("activity");
	out.print(HTMLEncode.jsQuoteEncode(actId));
 %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("step")) %>";
ProcOutline.meetDraft = "<%= HTMLEncode.jsQuoteEncode((String)request.getAttribute("meet-draft")) %>";

var formUri = "../view-wf/flow.xfrm.jsp";
//var simXfrmUri = "../itensil_js/process/simulate.xfrm";
var trackSvcUri = "../trak/";
var modelSvcUri = "../mod/";

function setup() {
	ActivityTree.isMeet = true;
    var ps = App.getPanelSet(240);

    var panTeam = new Panel("Team", false);
    ps.addMinor(panTeam);

    var panFiles = new Panel("Files", false);
    ps.addMinor(panFiles);

    var panOut = new Panel("Outline", false);
    ps.addMajor(panOut);

    ps.render(document.body, true);

    var pCan = new PmCanvas(null,
            makeElement(null,"div"),
            formUri,
            modelSvcUri + "getModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            App.activeFlow);
    pCan.panelSet = ps;
    pCan.hidden = true;
    
    App.resolver.modelUri = Uri.parent(App.activeFlow);
    
    makeElement(panTeam.contentElement, "div", "minorHead", "App Team");
    
    var teamObj = new TeamRoster(
	    	makeElement(panTeam.contentElement, "div", "meetTeam"), 
	    	dndGetCanvas(document.body));
    
    teamObj.initAddButton(panTeam.contentElement, "Add Member...");

    teamObj.addFromFile(Uri.parent(App.activeFlow));
    
    App.addDispose(teamObj);   
    
    App.addDispose(pCan);
    var proc = new PmModel(pCan, App.activeFlow, pCan.xfrm);
    
    
    ProcOutline.panelized(pCan, panOut, "Save App", "vert3");
    ProcOutline.teamRoaster = teamObj;
    
    App.unloadListeners.push(ProcOutline.isDirty);

    // File Tree
    var fileMod = FileTree.renderFiles("App", Uri.parent(App.activeFlow), true, panFiles.contentElement,
                [{title:"Community", uri:"/", showRoot:false}]);
                
    fileMod.activityFiles(ActivityTree.activityInfo(App.activeActivityId));

	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
	
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%  String name = UriHelper.name(UriHelper.getParent("" + request.getAttribute("flowUri")));
    request.setAttribute("crumb", name);
    request.setAttribute("help", "meet".equals((String)session.getAttribute("brand")) ? "Setup" : "Knowledge App"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
