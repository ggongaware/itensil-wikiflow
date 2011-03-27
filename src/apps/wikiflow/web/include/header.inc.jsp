<%@ page import="itensil.security.web.UserUtil,itensil.workflow.activities.signals.SignalUtil,itensil.util.Keys,
                 itensil.util.Check"%>
<% if (UserUtil.isGuest(user)) { %>
<script type="text/javascript">
	App.guest = 1;
	<% if (UserUtil.hasRole(user, "actlog")) { %>
	App.actlog = 1;
	<% } %>
</script>	
<% } %>

<link rel="alternate" type="application/rss+xml" title="Your RSS" href=" ../rss/yourRSS?<%= Keys.FORM_TOKEN %>=<%= UserUtil.getToken(request) %>" />

<% if (!UserUtil.isGuest(request) && SignalUtil.hasAlert(request)) { %>
<script type="text/javascript">
	var rssMsg = H.div(); 
	rssMsg.innerHTML = "<a href='../rss/yourRSS?<%= Keys.FORM_TOKEN %>=<%= UserUtil.getToken(request) %>'>You have some pending alerts <img src='../pix/rss.gif' alt='RSS' border='0'/></a>";
   	window.setTimeout('Ephemeral.showAtTop(App.__panelSet ? App.__panelSet.__hElem : null, rssMsg, Ephemeral.RIGHT, 10000)', 1500);
</script>
<% } %>

<%
	String brand = Check.emptyIfNull((String)session.getAttribute("brand"));
	if ("meet".equals(brand)) { 




	

%>
<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<script type="text/javascript">
	App.meet = 1;
    App.tnavInit();
</script>
<div id="mast">
<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<td width="185" valign="top"><img src="../pix/brd/logo-meet.gif" alt="Itensil Meeting" style="margin:2px 1px 0px 4px"/></td>
    <td width="320">
    <div class="tnav">
	    <a href="../home/" class="tnav">&nbsp;Home&nbsp;</a> | 
	    <% if (!UserUtil.isGuest(request)) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuMeet)">&nbsp;Apps&nbsp;</a> |
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuProc)">&nbsp;Processes&nbsp;</a> |
	    <% } %>
    	<a href="#" class="tnav2" onclick="Wiki.popup('../fil/system/Help/Meeting.kb','Meeting <%= request.getAttribute("help") %>',890,500, {showIndex:1}); return false;"><img src="../pix/help.gif" alt="Help" border="0" align="absmiddle"/>Here's&nbsp;How</a>
	</div>
   </td>
   <td class="mastUsr"><%= userName %> (<%= spaceName %> <a href="../j_signoff" style="color:black">Log Out</a>) &nbsp;
    <% if (SysAdmin.isSysAdmin(user)) { %>
    <a href="../_comadmin/page">Communities</a> |	
    <% } %>
    <a href="../uspace/settings">Settings</a></td>
</tr>
</table>
</div>
<% } else if (brand.startsWith("edu")) { 




	

%>
<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<script type="text/javascript">
	App.edu = 1;
    App.tnavInit();
</script>
<div id="mast">
<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<td width="185" valign="top"><img src="../pix/brd/logo-<%= brand %>.gif" alt="Itensil Learning" style="margin:2px 1px 0px 4px"/></td>
    <td width="320">
    <div class="tnav">
	    <a href="../home/" class="tnav">&nbsp;Home&nbsp;</a> | 
	   <% if (!UserUtil.isGuest(request)) { %>
	    <a href="../view-usr/guest-acts.jsp" class="tnav">&nbsp;Students&nbsp;</a> |
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuCourse)">&nbsp;Courses&nbsp;</a> |
	   <% } %>
    	<a href="#" class="tnav2" onclick="Wiki.popup(App.helpBase,'<%= request.getAttribute("help") %>',890,500, {showIndex:1}); return false;"><img src="../pix/help.gif" alt="Help" border="0" align="absmiddle"/>Here's&nbsp;How</a>
	</div>
   </td>
   <td class="mastUsr"><%= userName %> (<%= spaceName %> <a href="../j_signoff" style="color:black">Log Out</a>) &nbsp;
    <a href="../uspace/settings">Settings</a></td>
</tr>
</table>
</div>
<% } else {
	
	
	
	
	
	
	
	%>
<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<script type="text/javascript">
	App.defBrand = 1;
    App.tnavInit();
</script>
<div id="mast">
<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<% if ("oi".equals(brand)) { %>
	<td width="170" valign="top"><img src="../pix/brd/logo-oi.gif" alt="Outsourcing Institute" style="margin:2px 8px 2px 4px"/></td>
	<% } else if ("sdw".equals(brand)) { %>
	<td width="100" valign="top"><img src="../pix/brd/logo-sdw.gif" alt="Smart Dental" style="margin:8px 14px 2px 16px"/></td>
	<% } else if ("custlogo".equals(brand)) { %>
	<td width="180" valign="middle"><img src="../fil/home/logo.gif"/></td>	
	<% } else { %>
	<td width="100" valign="top"><img src="../pix/brd/logo-itensil.gif" alt="Itensil" style="margin:2px 8px 2px 8px"/></td>
	<% } %>
    <td width="430">
    <div class="tnav">
	    <a href="../home/" class="tnav">&nbsp;Home&nbsp;</a> | 
	    <% if (!UserUtil.isGuest(request)) { %>
	    <% if (!UserUtil.hasRole(user, "noproj")) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuPrjt)">&nbsp;Projects&nbsp;</a> |
	    <% } %>
	    <% if (!UserUtil.hasRole(user, "noproc")) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuProc)">&nbsp;Processes&nbsp;</a> |
	    <% } %>
	    <% if (!user.getUserSpace().getFeatures().contains("nomeet") && !UserUtil.hasRole(user, "nomeet")) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuMeet)">&nbsp;Apps&nbsp;</a> |
	    <% } %>
	    <% if (user.getUserSpace().getFeatures().contains("course") && !UserUtil.hasRole(user, "nocourse")) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuCourse)">&nbsp;Courses&nbsp;</a> |
	    <% } %>
	    <% if (user.getUserSpace().getFeatures().contains("entity") && !UserUtil.hasRole(user, "noentity")) { %>
	    <a href="#" class="tnav" onclick="return App.tnavShowMenu(this, App.tnavMenuEntity)">&nbsp;Entities&nbsp;</a> |
	    <% } %>
	    <% } %>
    	<a href="#" class="tnav2" onclick="Wiki.popup(App.helpBase,'<%= request.getAttribute("help") %>',890,500, {showIndex:1}); return false;"><img src="../pix/help.gif" alt="Help" border="0" align="absmiddle"/>Here's&nbsp;How</a>
	</div>
   </td>
   <td class="mastUsr"><%= userName %> (<%= spaceName %> <a href="../j_signoff" style="color:black">Log Out</a>) &nbsp;
    <% if (SysAdmin.isSysAdmin(user)) { %>
    <a href="../_comadmin/page">Communities</a> |	
    <% } %>
    <a href="../uspace/settings">Settings</a></td>
</tr>
</table>
</div>
<% } %>