<%@ page import="itensil.repository.*,
				itensil.security.*,
				itensil.security.hibernate.*,
				itensil.io.HibernateUtil,
				itensil.util.Check,
				itensil.web.*,
				java.util.*"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title><%@ include file="../include/title.inc.jsp" %> - Community Admin</title>
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
<script type="text/javascript" src="../js/Rules.js"></script>
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
}

var Community = {
	xb : new XMLBuilder(),

	startCreate : function() {
		var diag = xfDialog("Create Community", true, document.body, "../view-comadmin/community.xfrm", Community.xb);
        diag.showModal(200, 200);
        App.addDispose(diag);
	},
	
	mailConfig : function() {
		var diag = xfDialog("Mail Delivery Settings", true, document.body, "../view-comadmin/config.xfrm",
		 	Community.xb, null, "../_comadmin/getConfigProp?component=mailer-default");
		diag.showModal(150, 50);
        App.addDispose(diag);
	},
	
	switchUser : function(userId, uspaceId) {
		if (confirm("Are you sure you want to login as this user?\nYou will need to re-login as the sysadmin.")) {
			location.href = "../_comadmin/switchUser?userId=" + userId + "&uspaceId=" + uspaceId;
		}
	},
	
	destroyComm : function(id, name) {
		if (confirm("Are you sure you want to destroy the community '" + name + "' ?")) {
			location.href = "../_comadmin/destroyComm?uspaceId=" + id;
		}
	},
	
	editComm : function(id) {
		var diag = xfDialog("Community Settings", true, document.body, "../view-comadmin/comm-edit.xfrm",
		 	Community.xb, null, "../_comadmin/getInfo?uspaceId=" + id);
		diag.showModal(150, 50);
        App.addDispose(diag);
	}

};

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" style="overflow:auto">
<%
    request.setAttribute("help", "Settings");
    request.setAttribute("crumb", "Community Admin");
%>
<%@ include file="../include/header.inc.jsp" %>
<div class="settings">
    <fieldset>
        <legend>Actions</legend>
        <div class="linkrow"><span class="link" onclick="Community.startCreate()">Create Community &gt;</span></div>
        <div class="linkrow"><span class="link" onclick="Community.mailConfig()">Mail Delivery Settings &gt;</span></div>
    </fieldset>
    <% 
    	List uspaceList = (List)request.getAttribute("uspaceList");
    	if (uspaceList != null) {
    %>
    <fieldset>
        <legend>Communities</legend>
        <table style="margin:6px">
        	<tr>
        		<th>Name</th>
        		<th>Base URL</th>
        		<th>Brand</th>
        		<th>Features</th>
        		<th>Owner</th>
        		<th>&nbsp;&nbsp;</th>
        		<th>&nbsp;</th>
        	</tr>
        <%
        	for (Object ob : uspaceList) {
        		UserSpaceEntity uspEnt = (UserSpaceEntity) ob;
        		Repository repo  = RepositoryManagerFactory.getManager(uspEnt).getPrimaryRepository();
        		MutableRepositoryNode root = repo != null ? repo.getNodeByUri("",false) : null;
        		%>
        		<tr>
        		<td><span title="Click to edit" class="link"
        			onclick="Community.editComm('<%= uspEnt.getUserSpaceId() %>')"><%= HTMLEncode.encode(uspEnt.getName()) %></span></td>
        		<td><%= HTMLEncode.encode(uspEnt.getBaseUrl()) %></td>
        		<td><%= HTMLEncode.encode(Check.emptyIfNull(uspEnt.getBrand()))  %></td>
        		<td><%= HTMLEncode.encode(Check.emptyIfNull(uspEnt.getFeaturesStr()))  %></td>
        		<% if (root != null) { 
        			String userId = root.getOwner().getUserId();
        			UserEntity usr = ((UserEntity)HibernateUtil.getSession().get(UserEntity.class, userId));
        		%>
        		<td><%= usr != null ? HTMLEncode.encode(usr.getName()) : userId %>
        		<span class="link" onclick="Community.switchUser('<%= userId %>', '<%= uspEnt.getUserSpaceId() %>')">Login &gt;</span></td>
        		<% } else { %>
        		<td>[incomplete community]</td>
        		<% } %>
        		<td>&nbsp;</td>
        		<td><span class="link" style="color:#B00" 
        			onclick="Community.destroyComm('<%= uspEnt.getUserSpaceId() %>', '<%= HTMLEncode.sglQuoteEncode(uspEnt.getName()) %>')">destroy</span></td>
        		</tr>
        		<%
        	}
        %>
        </table>
    </fieldset>
    <% } %>
</div>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>
