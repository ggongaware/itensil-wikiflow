<%@ page import="itensil.web.HTMLEncode"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<script type="text/javascript" src="../js/ScriptHost.js"></script>
<script type="text/javascript" src="../js/XMLBuilder.js"></script>
<script type="text/javascript" src="../js/CoreUtil.js"></script>
<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/XMLEdit.js"></script>

<script type="text/javascript" src="../js/brwsr/Util.js"></script>
<script type="text/javascript" src="../js/brwsr/Menu.js"></script>
<script type="text/javascript" src="../js/brwsr/ComboBox.js"></script>
<script type="text/javascript" src="../js/brwsr/Calendar.js"></script>
<script type="text/javascript" src="../js/brwsr/DND.js"></script>
<script type="text/javascript" src="../js/brwsr/Tree.js"></script>
<script type="text/javascript" src="../js/brwsr/Grid.js"></script>
<script type="text/javascript" src="../js/brwsr/Panel.js"></script>
<script type="text/javascript" src="../js/brwsr/Dialog.js"></script>
<script type="text/javascript" src="../js/brwsr/XMLIDE.js"></script>

<script type="text/javascript" src="../js/Rules.js"></script>
<script type="text/javascript" src="../js/Data.js"></script>

<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>
<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/XMLIDE.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/Entity.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />
<style type="text/css">
body {
    padding: 0px;
    margin: 0px;
    overflow: hidden;
    font-family: arial, helvetica, sans-serif;
    font-size: 11px;
}
</style>
<script type="text/javascript">
//SH.debug = true;
var ide;

function setup() {
   App.init();
   Modes.mode = "edit";
   ide = new XmlIDE();
   ide.path = "../fil";
   ide.header = document.getElementById("mast");
   ide.render(document.body);
   ide.loadDocument("<%= HTMLEncode.jsQuoteEncode(request.getParameter("uri")) %>");
}
</script>
</head>


<body onload="setup()" onunload="ide.dispose()" scroll="no">
<div id="mast" style="font-weight:bold;padding:2px 2px 2px 4px;background-color:#ccc">
<table><tr valign="top">
<td><button onclick="ide.doSave()" class="xide">Save</button></td>
<td><div class="xideMsgMast" id="xideStat">Loading...</div></td>
</tr>
</table>
</div>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>

