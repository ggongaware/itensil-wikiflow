<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Run</title>
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

<script type="text/javascript">
//<![CDATA[
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("activity")) %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("step")) %>";
var formUri = "../view-wf/flow.xfrm.jsp";
//var simXfrmUri = "../itensil_js/process/simulate.xfrm";
var trackSvcUri = "../trak/";
var modelSvcUri = "../mod/";

function setup() {

	ActivityTree.isMeet = (App.activeFlow.indexOf("/meeting/") > 0);
	ActivityTree.isCourse = !ActivityTree.isMeet && (App.activeFlow.indexOf("/course/") > 0); 
	
    var ps = App.getPanelSet(340);

    var panTodo = new Panel(ActivityTree.isCourse ? "Status" : "To Do List", false);
    ps.addMinor(panTodo);
    
    if (!App.guest) {
    
    	if (!ActivityTree.isCourse) {
		    var panTeam = new Panel(ActivityTree.isMeet ? "Team" : "Team Roles", false);
		    ps.addMinor(panTeam);
		}
	
	    var panFiles = new Panel("Files", false);
	    if (!ActivityTree.isMeet) panFiles.setShrink(true);
	    ps.addMinor(panFiles);
	
	    var panProc = new Panel("Flowchart", false);
	    panProc.setShrink(true);
	    ps.addMajor(panProc);
	}
	
    var panWiki = new Panel("Workzone", false, "workzone");
    ps.addMajor(panWiki);

    ps.render(document.body, true);

    // Mode tabs
    Modes.drawModes(document.getElementById("mast"), "todo");

    var pCan = new PmCanvas(panProc,
            App.guest ? makeElement(null,"div") : makeElement(panProc.contentElement, "div", "flowHead pmRead"),
            formUri,
            modelSvcUri + "getModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            App.activeFlow);

	App.resolver.modelUri = Uri.parent(App.activeFlow);

    pCan.panelWork = panWiki;
    if (App.guest) {
    	pCan.hidden = true;
    } else {
    	pCan.panelFlow = panProc;
    }
    pCan.panelSet = ps;
    App.addDispose(pCan);
    var proc = new PmModel(pCan, App.activeFlow, pCan.xfrm);
    

    var headElem = makeElement(null, "div", "flowHead wzToolbar");
    panWiki.setGutters(headElem);
    ActivityTree.progressCtrl = makeElement(headElem, "span");
    ActivityTree.progressCtrl.style.display = "none";
    ActivityTree.progressPanel = panWiki;
    
    if (!(App.guest || ActivityTree.isCourse)) {
	    var launLink = makeElement(ActivityTree.progressCtrl, "div", "minorBtn", "Use a process for this task...")
	 	makeElementNbSpd(launLink, "div", "mbIco mb_launIco");
	 	setEventHandler(launLink, "onclick", 
	 		function(evt) {
	 			if (!ActivityTree._taskProcMenu) {
	            	ActivityTree._taskProcMenu = new Menu(
	            		new TNavProcMenuModel("activeProcesses", ActivityTree.taskProcPick), 
	            		ActivityTree.taskProcPick);
	            	App.addDispose(ActivityTree._taskProcMenu);
	            }
	            ActivityTree._taskProcMenu.popUp(evt);
			});
	}
    
    var btn = makeElement(ActivityTree.progressCtrl, "button", "xfctrl", null, null, {type:'button'});
    var label = makeElement(btn, "label", null, null, null);
    label.style.width = "80px";
    if (SH.is_ie) btn.style.width = "102px";
    makeElementNbSpd(label, "span", "mbIco mb_savIco");
    makeElement(label, "span", null, "Save Progress");
    setEventHandler(btn, "onclick", function() {
			ActivityTree.saveProgress();
    	});
    
    if (!ActivityTree.isCourse) {
	   	ActivityTree.percInput = makeElement(ActivityTree.progressCtrl, "input", "text perc", "text", null, { name : "actPerc0"});
	    ActivityTree.percInput.setAttribute("autocomplete", "off");	
	    makeElement(ActivityTree.progressCtrl, "span", "percLabel", "% Complete");
	    if (!App.guest) {
	    	makeElement(ActivityTree.progressCtrl, "div", "todoTo", "To:");
	    	ActivityTree.todoAssignCtrl = makeElement(ActivityTree.progressCtrl, "div", "assign");
	    }
	}
    
    App.activeStateListeners.push(ActivityTree.progressListener);
    
    btn = null; label = null; headElem = null;

    // Load wiki before active Activity
    var hasLog = !ActivityTree.isCourse && (!App.guest || App.actlog);
    var wikViews = [{isStatic:true, isFullPage: !hasLog, isWorkzone:true,
                linkWiki : ("../fil" + Uri.absolute(Uri.root(App.activeFlow), "Reference.kb"))}];
    if (hasLog) wikViews.push( {isFullPage:false, cssClass:"wiki2nd", titlePrefix:"Log For: "});
    var wik = new Wiki("../fil" + Uri.absolute(Uri.parent(App.activeFlow), "activities/activities.kb"), panWiki.contentElement, null,
            wikViews, {preSearchCss:"searchActi", noSearch : true});
    App.addDispose(wik);
    pCan.wiki = wik;

    // activity on lower view
    if (hasLog) ActivityTree.hookWikiActivity(wik.views[1]);

	if (!App.guest) {
	    // File Tree
	    var fileMod = FileTree.renderFiles(ActivityTree.isCourse ? "Course" : "Process", Uri.parent(App.activeFlow), true, panFiles.contentElement,
	                [{title:"Community", uri:"/", showRoot:false}]);
	
	    App.activityListeners.push(FileTree.activityListenActivityFiles(fileMod));
    }

    // Activity tree
    ActivityTree.renderActivities("assigned", App.activeFlow, panTodo.contentElement);

	if (!App.guest && !ActivityTree.isCourse) {
    // Team view
	    var teamObj;
	    if (ActivityTree.isMeet) {
	    	makeElement(panTeam.contentElement, "div", "minorHead", "Team");
		    teamObj = new TeamRoster(
			    	makeElement(panTeam.contentElement, "div", "meetTeam"), 
			    	dndGetCanvas(document.body));
		    
		    //teamObj.initAddButton(panTeam.contentElement, "Add Member...");
		    teamObj.addFromFile(Uri.parent(App.activeFlow));
		    PmCanvas.teamRoster = teamObj;
	    	App.addDispose(teamObj);  
	    } else {
	    	// Team view
		    PmCanvas.renderTeam(proc, panTeam.contentElement, true);
		    teamObj = PmCanvas.teamRoster;
		   	App.activityListeners.push(function(node) {
		    		PmCanvas.teamRoster.clearTeam();
		    		// team before stat, for role maps
		            PmCanvas.renderTeam(proc, panTeam.contentElement);
		        });
	    }
	    if (ActivityTree.todoAssignCtrl) {
	    	ActivityTree.todoAssignCtrl.assignObj = ActivityTree.progressAssigner;
	    	teamObj.canvas.makeDropTarget(ActivityTree.todoAssignCtrl, TeamRoster.dndType.type);
	    	setEventHandler(ActivityTree.todoAssignCtrl, "onclick", ActivityTree.progressAssigner.menuClick);
	    	setEventHandler(ActivityTree.todoAssignCtrl, "oncontextmenu", ActivityTree.progressAssigner.menuClick);
	    }
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
    request.setAttribute("help", furi.indexOf("/meeting/") > 0 ? "Meeting Run" : "Run"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
