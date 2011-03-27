<%@ page import="itensil.web.HTMLEncode"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title>Folder List</title>
<script type="text/javascript" src="../js/ScriptHost.js"></script>
<script type="text/javascript" src="../js/XMLBuilder.js"></script>
<script type="text/javascript" src="../js/CoreUtil.js"></script>
<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/brwsr/Util.js"></script>
<script type="text/javascript" src="../js/brwsr/Menu.js"></script>
<script type="text/javascript" src="../js/brwsr/DND.js"></script>
<script type="text/javascript" src="../js/brwsr/Dialog.js"></script>
<script type="text/javascript" src="../js/brwsr/Tree.js"></script>
<script type="text/javascript" src="../js/brwsr/Grid.js"></script>
 
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>
 
<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />

 <link rel="stylesheet" type="text/css" href="../css/App.css" />

 <style type="text/css">
    body {
        margin: 0px;
        overflow: hidden;
        font-family: arial, helvetica, sans-serif;
    	font-size: 11px;
    }
 </style>

 <script type="text/javascript">
 //<![CDATA[

 function setup() {
 	App.init();
    var uri = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("uri")) %>";
    var tMod = new FileTreeModel(uri, uri.length > 2);
    var aTree = new Tree(tMod);
    App.setFileMenu(aTree);

    var cont = document.getElementById('content');
    aTree.render(cont, null, "fileTree");
    aTree.makeDragCanvas(cont, new FileTreeDNDType("fileTree"), true, true, true);
    App.addDispose(aTree);
    App.addDispose(UserTree);
 }


 //]]>
 </script>
</head>

<body onload="setup()" onunload="App.dispose()" scroll="no"  style="padding:20px">
<div id="content" style="position:relative"></div>
<!-- IE css image preload -->
<div style="display:none">
<img src="../pix/loadingAni.gif" alt=""/>
<img src="../pix/trTog.gif" alt=""/>
<img src="../pix/trPips.gif" alt=""/>
<img src="../pix/trV.gif" alt=""/>
</div>
</body>
</html>