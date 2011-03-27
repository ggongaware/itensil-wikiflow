<%@ page import="itensil.repository.RepositoryHelper"%>
<%@ page import="itensil.security.web.UserUtil"%>
<%@ page import="itensil.web.HTMLEncode, 
		itensil.web.ServletUtil, 
		itensil.util.UriHelper, 
		itensil.util.Keys, 
		itensil.util.Check" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - APIs</title>
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
<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>

<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

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
<style type="text/css">
    #mast {
        height: 42px;
    }
</style>

<script type="text/javascript">
//<![CDATA[

function setup() {
	App.init();
	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%
    request.setAttribute("help", "APIs");
    request.setAttribute("crumb", "APIs");
%>
<%@ include file="../include/header.inc.jsp" %>
<div class="settings">
    <fieldset>
        <legend>Integrations</legend>
        
<p>For receiving task alerts and updates in a desktop or web-based news reader.</p>    
        <div class="linkrow">
        	<a href='../rss/yourRSS?<%= Keys.FORM_TOKEN %>=<%= UserUtil.getToken(request) %>'>RSS Alert Feed  <img src='../pix/rss.gif' alt='RSS' border='0'/></a>
        </div>
        <br/>
<p>For adding task lists with workzone screen support in portals or other web applications.</p>
        <div class="linkrow">
Web-Page embeddable frame:<br/>
<textarea rows="4" cols="115" style="font-size:10px">
<script src="<%= ServletUtil.getAbsoluteContextPath(request) %>embed.js.jsp?key=<%= UserUtil.getToken(request) %>&width=500&height=400">
</script>
</textarea>
        <div style="font-size:10px">Please copy and paste source code</div>
        </div>
        <br/>
<p>For connecting your personal environment with applications already built to integrate.</p>
        <div class="linkrow">
        Personal API Key:<br/>
        <input type="text" value="<%= UserUtil.getToken(request) %>" size="50"/>
        </div>
    </fieldset>
    <div style="font-size:10px;text-align:right">Version <%@ include file="../include/build.txt" %></div>
</div>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
