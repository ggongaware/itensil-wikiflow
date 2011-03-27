<%@ page import="itensil.repository.RepositoryHelper"%>
<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.Keys,itensil.security.web.UserUtil"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
if (UserUtil.isGuest(request)) {
	response.sendRedirect("guest.jsp");
	return;
}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<link rel="alternate" type="application/rss+xml" title="Your RSS" href=" ../rss/yourRSS?<%= Keys.FORM_TOKEN %>=<%= UserUtil.getToken(request) %>" />

<title><%@ include file="../include/title.inc.jsp" %> - Home</title>
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

<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />

<script type="text/javascript">
//<![CDATA[

function setup() {
    var ps = App.getPanelSet(370);

    var panTodo = new Panel(App.edu ? "Courses" : "To Do List", false);
    ps.addMinor(panTodo);

    var panFiles = new Panel("Files", false);
    panFiles.setShrink(true);
    ps.addMinor(panFiles);
    
    var panDash = new Panel("Dashboard", false);
    if (App.defBrand) panDash.setShrink(true);
    ps.addMajor(panDash);

    var panWiki = new Panel("Community <%= request.getParameter("page") == null ? "Home" : "Team"  %> Page", false);
    ps.addMajor(panWiki);

    ps.render(document.body, true);
    
    if (App.edu) {
    	panDash.setHeight(50);
     	renderEduDashboard(panDash.contentElement);
    } else renderDashboard(panDash.contentElement);

    // Activity tree
    ActivityTree.renderActivities("assigned", null, panTodo.contentElement);

    var wik = new Wiki("../fil<%
        RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        String mount = RepositoryHelper.getPrimaryRepository().getMount();
        out.print(mount);
        boolean custDash = false;
        try {
        	RepositoryHelper.getNode(mount + "/Dashboard.xfrm", false);
        	custDash = true;
        } catch (Exception ex) {}
        RepositoryHelper.commitTransaction();
        RepositoryHelper.closeSession();
    %>/Community.kb", panWiki.contentElement, "<%= 
    	request.getParameter("page") == null ? "" : 
    		HTMLEncode.jsQuoteEncode("" + request.getParameter("page")) %>");
    App.addDispose(wik);
    
    <% if ("1".equals(request.getParameter("newTeam"))) { %>
    var tArt = new WikiTempArt(
    	"This article is currently empty, to create content click edit.", 
    	Uri.parent(wik.uri));
    tArt.id = "My Team";
    tArt.setAttribute("refId", "team");
    wik.showArticle(tArt);
    <% } %>

    // File Tree
    FileTree.renderFiles("Community", "/", false, panFiles.contentElement);
	
	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
}
<% if (custDash) { %>
function renderDashboard(hElem) {
	var div = posRelIEFix(makeElement(hElem, "div", "wikiView"));
	domSetCss(div, "margin:0");
	var formUri = "../fil/home/Dashboard.xfrm";
	ActivityTree.xb.loadURIAsync(formUri,
            function(doc) {
	  			var xf = new XForm(doc, "Dashboard", ActivityTree.xb, formUri);
	  			xf.render(div);
	  			div = null; hElem = null;
	  		}
	  	);
}
<% } else { %>
function renderDashboard(hElem) {
	hElem.appendChild(
		H.div({klass:'hmDash'},
			H.div({klass:'quick'}, H.b('Quick start: '), H.span(
				App.meet ? 'Manage app action steps in a sharable team process' : 
				(App.ahm ? 'Launch a scenario or design a template.' :
				'Share launch-able processes and organize them into projects.'))),
			H.div({klass:'btns'},
			
				App.meet ? null : H.div({klass:'hmBtn', onclick:"ActivityTree.preLaunch(false)"},
					H.div({klass:'hmLaunch'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Launch a ' + (App.ahm ? 'Scenario' : 'Process'))
				),
				
				!App.meet ? null : H.div({klass:'hmBtn', onclick:"App.tnavNewMeet()"},
					H.div({klass:'hmMeet'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Start a Knowledge App')
				),
				
				H.div({klass:'hmBtn', onclick:"App.snavMenuProcDes.popUp(event)"},
					H.div({klass:'hmDesign'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Design a ' + (App.ahm ? 'Template' : 'Process'))
				),
				
				App.meet ? null : H.div({klass:'hmBtn', onclick:"App.tnavNewPrjt()"},
					H.div({klass:'hmProject'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Start a Project')
				),
				
				App.meet ? null : H.div({klass:'hmBtn', onclick:"App.tnavNewMeet()"},
					H.div({klass:'hmMeet'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Start a Knowledge App')
				),
				
				H.div({klass:'hmBtn hmBtnHelp', onclick:"Wiki.popup('../fil/system/Help/Help.kb','Getting Started',690,500)"},
					H.div({klass:'hmHelp'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Getting Started')
				)
			)
		)
	);
	
	var dashes = App.meet ? ["../proj/meetDash"] : ["../proj/dashList", "../proj/meetDash"];
	
	for (var ii = 0; ii < dashes.length; ii++) {
		ActivityTree.xb.loadURIAsync(dashes[ii],
	           function(doc, isMeet, xmlHttp) {
	           	var pElem = H.div({klass:"hmProj"});
			 	
			 	pElem.appendChild(H.div({klass:"title"}, isMeet ? "Select a App:" : "Projects:"));
			 	var projElems = doc ? Xml.match(doc.documentElement, "project") : [];
			 	if (projElems.length == 0) {
			 		pElem.appendChild(H.div({klass:"empty"}, 
			 		isMeet ? "You currently have no Apps, click 'Start a Knowledge App' to create one." :
			 			"You currently have no projects, click 'Start a Project' to create one."));
			 	} else {
				 	for (var ii=0; ii < projElems.length; ii++) {
				 		var proj = projElems[ii];
				 		var row = H.div({klass:"row"},
				 				H.span({klass:"projName", 
				 					onclick:"location.href = "
				 						 + (isMeet ? " '../act/meetStat?meet=/home/meeting/'" : " '../proj/page?proj='" )
				 						 + " + Uri.escape(Xml.stringForNode(this))"},
				 					proj.getAttribute("uri")), H.nbsp + H.nbsp,
				 				H.span({klass:"projDesc"}, proj.getAttribute("description"))
				 			);
				 		
				 		var actElems = Xml.match(proj, "activity");
				 		if (actElems.length > 0) {
				 			var actRows = H.ul({klass:"actRows"});
				 			row.appendChild(actRows);
				 			if (isMeet) {
				 				var act = actElems[0];
				 				var stEls = Xml.match(act, "state");
				 				
				 				stEls.sort(function (aEl,bEl) { 
				 					var a = aEl.getAttribute("timeStamp"); var b = bEl.getAttribute("timeStamp");
				 					if(a == b) return 0; else return a < b ? 1 : -1; });
				 					
				 				var cnt = 0;
				 				for (var jj=0; jj < stEls.length; jj++) {
				 					var sta = stEls[jj];
				 					if (sta.getAttribute("subState") == "ENTER_END") continue;
				 					if (++cnt > 3) break;
				 					var upDate = sta.getAttribute("timeStamp");
				 					actRows.appendChild(
						 				H.li({klass:"act"},
							 				H.div({klass:"name"}, sta.getAttribute("stepId")),
							 				H.div({klass:"desc"}, UserTree.getUserName(sta.getAttribute("assignId"))),
						 					H.div({klass:"update"}, (upDate ?
						 						DateUtil.toLocaleShort(DateUtil.parse8601(upDate, true), true) : ""))
						 				)
						 			);
				 				}
				 			} else {
					 			
						 		for (var jj=0; jj < actElems.length; jj++) {
						 			var act = actElems[jj];
						 			var upDate = "";
						 			var stEls = Xml.match(act, "state");
						 			for (var kk=0; kk < stEls.length; kk++) {
						 				var dt = stEls[kk].getAttribute("timeStamp");
						 			 	if (dt > upDate) upDate = dt;
						 			}
						 			actRows.appendChild(
						 				H.li({klass:"act"},
							 				H.div({klass:"name"}, act.getAttribute("name")),
						 					H.div({klass:"desc"}, act.getAttribute("description")),
						 					H.div({klass:"update"}, (upDate ?
						 						DateUtil.toLocaleShort(DateUtil.parse8601(upDate, true), true) : ""))
						 				)
						 			);
						 		}
						}
					 	}
				 		pElem.appendChild(row);
				 	}
			 	}
			 	
			 	hElem.appendChild(pElem);
			 	
			}, dashes[ii].indexOf("meet") > 0);
			
	} // dashes
}

function renderEduDashboard(hElem) {
	hElem.appendChild(
		H.div({klass:'hmDash'},
			H.div({klass:'quick'}, H.b('Quick start: '), H.span(
				'Design courses and enroll students')),
			H.div({klass:'btns'},
			
				
				H.div({klass:'hmBtn', onclick:"location.href = '../view-usr/guest-acts.jsp'"},
					H.div({klass:'hmMeet'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Enroll Students')
				),
				
				H.div({klass:'hmBtn', onclick:"App.snavMenuCourDes.popUp(event)"},
					H.div({klass:'hmDesign'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Design a Course')
				),
				H.div({klass:'hmBtn hmBtnHelp', onclick:"Wiki.popup('../fil/system/Help/Help.kb','Getting Started',690,500)"},
					H.div({klass:'hmHelp'}, H.nbsp),
					H.div({klass:'hmLabel'}, 'Getting Started')
				)
			)
		)
	);
}
<% } %>

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% request.setAttribute("help", "Home"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
