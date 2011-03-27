<%@ page import="itensil.repository.RepositoryHelper"%>
<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.security.web.UserUtil"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

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

    var panWiki = new Panel("Dashboard", false);
    ps.addMajor(panWiki);

    ps.render(document.body, true);

    // Activity tree
    ActivityTree.renderActivities("assigned", null, panTodo.contentElement);

    var wik = new Wiki("../fil<%
        RepositoryHelper.beginTransaction();
        RepositoryHelper.useReadOnly();
        out.print(RepositoryHelper.getPrimaryRepository().getMount());
        RepositoryHelper.commitTransaction();
        RepositoryHelper.closeSession();
    %>/Guest.kb", panWiki.contentElement, "<%= 
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

	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% request.setAttribute("help", "Home"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
