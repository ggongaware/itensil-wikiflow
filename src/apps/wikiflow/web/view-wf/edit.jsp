<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page import="itensil.web.ServletUtil"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Design</title>
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

<script type="text/javascript" src="../js/brwsr/ProcMap.js"></script>
<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcCanvas.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcOutline.js"></script>

<script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

<script type="text/javascript" src="../js/brwsr/QuizWiz.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Entity.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/ProcMap.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />

<script type="text/javascript">
//<![CDATA[
App.activeFlow = "<%= HTMLEncode.jsQuoteEncode("" + request.getAttribute("flowUri")) %>";
App.activeActivityId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("activity")) %>";
App.activeStepId = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("step")) %>";

var formUri = "../view-wf/flow.xfrm.jsp";
//var simXfrmUri = "../itensil_js/process/simulate.xfrm";
var trackSvcUri = "../trak/";
var modelSvcUri = "<%= ServletUtil.getServletPath(request, "/mod/") %>";

function setup() {

	ActivityTree.isMeet = (App.activeFlow.indexOf("/meeting/") > 0);
	ActivityTree.isCourse = !ActivityTree.isMeet && (App.activeFlow.indexOf("/course/") > 0); 
	
    var ps = App.getPanelSet(200);

    var panPal = new Panel("Palette", false);
    ps.addMinor(panPal);
    
    var panAttr = new Panel("Attributes", false);
    panAttr.setShrink(true);
    ps.addMinor(panAttr);
    
    var panEntity = null;
    <% if (user.getUserSpace().getFeatures().contains("entity")) { %>
    panEntity = new Panel("Entity Relations", false);
    panEntity.setShrink(true);
    ps.addMinor(panEntity);
    <% } %>

    var panFiles = new Panel("Files", false);
    panFiles.setShrink(true);
    ps.addMinor(panFiles);

    var panProc = new Panel("Flowchart", false);
    ps.addMajor(panProc);

    var panWiki = new Panel("Design Workzone", false, "workzone");
    panWiki.setShrink(true);
    ps.addMajor(panWiki);

    ps.render(document.body, true);

    // Mode tabs
    Modes.drawModes(document.getElementById("mast"), "edit");

    var palList = new Tree(new PmPalTreeModel());
    palList.makeDragCanvas(panPal.contentElement, new PmPalTreeDNDType("palDummy"), true, true, true);
    palList.render(panPal.contentElement, "", "palette");
    
    

    var pCan = new PmCanvas(panProc,
            makeElement(panProc.contentElement, "div", "flowHead"),
            formUri,
            modelSvcUri + "getModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            modelSvcUri + "setModel?uri=" + Uri.escape(App.activeFlow) + "&activity=" + App.activeActivityId,
            App.activeFlow);

    pCan.panelWork = panWiki;
    pCan.panelFlow = panProc;
    pCan.panelSet = ps;
    pCan.setEditMode(true);
    
    App.resolver.modelUri = Uri.parent(App.activeFlow);
    
    App.addDispose(pCan);
    var proc = new PmModel(pCan, App.activeFlow, pCan.xfrm);
    pCan.setAttrPanel(panAttr);
    if (panEntity) pCan.setEntityPanel(panEntity);
    
    var outLink = makeElement(panPal.contentElement, "div", "minorBtn palOut", "Build from outline...");
    makeElement(outLink, "div", "mbIco mb_addIco");
    outLink._procCan = pCan;
    outLink.onclick = ProcOutline.buildClick;
    
    if (ActivityTree.isCourse) {
	    var quizLink = makeElement(panPal.contentElement, "div", "minorBtn palOut", "Quiz Wizard...");
	    makeElement(quizLink, "div", "mbIco mb_newIco");
	    quizLink._procCan = pCan;
	    quizLink.onclick = QuizWiz.startClick;
    }

    // Load wiki before active Activity
    var wik = new Wiki("../fil" + Uri.absolute(Uri.parent(App.activeFlow), "activities/activities.kb"), panWiki.contentElement, null,
            [{isStatic:true, isFullPage:true, isWorkzone:true,
                linkWiki : ("../fil" + Uri.absolute(Uri.root(App.activeFlow), "Reference.kb"))}], {preSearchCss:"searchActi", noSearch : true});
    App.addDispose(wik);
    pCan.wiki = wik;

    // File Tree
    FileTree.renderFiles(ActivityTree.isCourse ? "Course" : "Process", Uri.parent(App.activeFlow), true, panFiles.contentElement,
                [{title:"Community", uri:"/", showRoot:false}]);

	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
	
	if (App.activeStepId) {
		var wStp = proc.getStepById(App.activeStepId);
		if (wStp && wStp.constructor == PmStep) PmCanvas.showWorkzone(null, wStp);
	}
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%  
	String furi = "" + request.getAttribute("flowUri");
	String name = UriHelper.name(UriHelper.getParent(furi));
    request.setAttribute("crumb", name);
    request.setAttribute("help", furi.indexOf("/meeting/") > 0 ? "App Design" : "Design"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
