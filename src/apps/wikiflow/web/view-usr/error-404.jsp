<%@ page import="itensil.workflow.activities.state.Activity,itensil.web.HTMLEncode,itensil.web.ServletUtil"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isErrorPage="true" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<base href="<%= ServletUtil.getAbsoluteContextPath(request) + "view-usr/" %>"/>
<title><%@ include file="../include/title.inc.jsp" %>  Not Available</title>
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
	App.addDispose(UserTree);
	App.addDispose(ActivityTree);
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%
    request.setAttribute("help", "Not Available");
    request.setAttribute("crumb", "Not Available");
%>
<%@ include file="../include/header.inc.jsp" %>
<div class="settings">
    <fieldset>
        <legend>Not Available</legend>
Sorry, the item you requested is not available for one of the following reasons:
<ul>
<li>Item may have been deleted or moved.</li>
<li>Item may have security restrictions.</li>
</ul>
    </fieldset>
</div>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
