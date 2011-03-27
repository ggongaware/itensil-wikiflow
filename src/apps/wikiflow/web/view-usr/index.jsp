<%@ page import="itensil.repository.RepositoryHelper"%>
<%@ page import="itensil.security.web.UserUtil"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Settings</title>
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
    request.setAttribute("help", "Settings");
    request.setAttribute("crumb", "Settings");
%>
<%@ include file="../include/header.inc.jsp" %>
<div class="settings">
    <fieldset>
        <legend>Personal Settings</legend>
        <div class="linkrow"><span class="link" onclick="UserTree.selfSet();">Change Name, Email, and Time-zone Settings &gt;</span></div>
        <div class="linkrow"><span class="link" onclick="UserTree.selfPass();">Change Password &gt;</span></div>
        <br/>
        <div class="linkrow" style="font-size:10px"><a class="link" href="../view-usr/apis.jsp">Advanced - Integration Support &gt;</a></div>
    </fieldset>
    
    <% if (user.getUserSpace().getFeatures().contains("orgs")) { %>
    <fieldset>
        <legend>Organization Settings</legend>
        <div class="linkrow"><a class="link" href="../view-usr/org.jsp">Organization Hierarchy &gt;</a></div>
    </fieldset>
    <% } %>
    <%
    
       if (UserUtil.isAdmin(request)) { %>
    <fieldset>
        <legend>Community Settings</legend>
        <div class="linkrow"><span class="link" onclick="UserTree.invite();">Invite User &gt;</span></div>
        <div class="linkrow"><span class="link" onclick="UserTree.manUsers();">Manage Users &gt;</span></div>
        <div class="linkrow"><span class="link" onclick="UserTree.manGroups();">Manage Groups &gt;</span></div>
    </fieldset>
    <% } %>
    <div style="font-size:10px;text-align:right">Version <%@ include file="../include/build.txt" %></div>
</div>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
