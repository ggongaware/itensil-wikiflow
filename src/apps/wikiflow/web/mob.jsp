<%@ page import="itensil.security.web.UserUtil,itensil.workflow.activities.signals.SignalUtil,itensil.util.Keys,
                 itensil.util.Check"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%@ include file="include/title.inc.jsp" %></title>
 <!-- Core Utils-->
 <script type="text/javascript" src="js/ScriptHost.js"></script>
 <script type="text/javascript" src="js/XMLBuilder.js"></script>
 <script type="text/javascript" src="js/CoreUtil.js"></script>
 <script type="text/javascript" src="js/xpath.js"></script>
 <script type="text/javascript" src="js/XMLEdit.js"></script>

 <!-- Main Widgets -->
 <script type="text/javascript" src="js/brwsr/Util.js"></script>
 <script type="text/javascript" src="js/brwsr/Menu.js"></script>
 <script type="text/javascript" src="js/brwsr/DND.js"></script>
 <script type="text/javascript" src="js/brwsr/Tree.js"></script>
 <script type="text/javascript" src="js/brwsr/ComboBox.js"></script>
 <script type="text/javascript" src="js/brwsr/Calendar.js"></script>
 <script type="text/javascript" src="js/brwsr/Panel.js"></script>
 <script type="text/javascript" src="js/brwsr/Dialog.js"></script>
 <script type="text/javascript" src="js/brwsr/Grid.js"></script>
 
 <script type="text/javascript" src="js/brwsr/FileTree.js"></script>
 <script type="text/javascript" src="js/App.js"></script>

 <!-- styles -->
 <link rel="stylesheet" type="text/css" href="css/Menu.css" />
 <link rel="stylesheet" type="text/css" href="css/Tree.css" />
 <link rel="stylesheet" type="text/css" href="css/Panel.css" />
 <link rel="stylesheet" type="text/css" href="css/Dialog.css" />
 <link rel="stylesheet" type="text/css" href="css/Calendar.css" />
 <link rel="stylesheet" type="text/css" href="css/ComboBox.css" />

 <link rel="stylesheet" type="text/css" href="css/App.css" />

<script type="text/javascript">
//<![CDATA[


function setup() {
    var ps = new PanelSetSingle();
    App.addDispose(ps); // for memory leak prevention
    App.init();

    // set a header zone div
    ps.header = document.getElementById("mast");
    var panFrame = new Panel("", false);
    
    ps.add(panFrame);
    
    // draw it, then the panel content nodes will be available
    ps.render(document.body, true); // stick it directly in the body for auto-strech

    new PanelIframe(panFrame, "mobFrame", "home/kiosk.jsp?embed=1", null);

}
//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<%
	String brand = Check.emptyIfNull((String)session.getAttribute("brand"));
%>
<link rel="stylesheet" type="text/css" href="css/brd-itensil.css" />
<div id="mast">
<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<% if ("custlogo".equals(brand)) { %>
	<td width="180" valign="middle"><img src="fil/home/logo.gif"/></td>	
	<% } else { %>
	<td width="180" valign="top"><img src="pix/brd/logo-itensil.gif" alt="Itensil" style="margin:2px 8px 2px 8px"/></td>
	<% } %>
    <td>
    <div class="tnav" style="text-align:right">
	    <a href="home/kiosk.jsp?embed=1" target="mobFrame" class="tnav" style="font-size:14px">&nbsp;&gt; Home&nbsp;</a>&nbsp;
	</div>
   </td>
</tr>
</table>
</div>
</body>
</html>