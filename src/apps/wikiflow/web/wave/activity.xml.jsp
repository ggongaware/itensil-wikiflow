<%@ page contentType="text/javascript;charset=UTF-8" language="java" %>
<%@ page import="itensil.web.HTMLEncode, 
		itensil.web.ServletUtil, 
		itensil.util.UriHelper, 
		itensil.util.Keys, 
		itensil.util.Check,
		itensil.security.web.UserUtil" %>
<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Itensil Activity">
    <Require feature="wave" />
    <Require feature="setprefs" />
  </ModulePrefs>
<%
	String hpath = ServletUtil.getAbsoluteContextPath(request);
%>
  <UserPref name="itensilhost" display_name="Itensil Address" required="true" default_value="<%= hpath %>" />
  <UserPref name="activity"/> 
  <Content type="html" preferred_width="800" preferred_height="400" >
    <![CDATA[

<script type="text/javascript" src="<%= hpath %>js/ScriptHost.js"></script>
<script type="text/javascript" src="<%= hpath %>js/CoreUtil.js"></script>
<script type="text/javascript" src="<%= hpath %>js/XMLBuilder.js"></script>
<script type="text/javascript" src="<%= hpath %>js/xpath.js"></script>
<script type="text/javascript" src="<%= hpath %>js/XSchema.js"></script>
<script type="text/javascript" src="<%= hpath %>js/brwsr/Util.js"></script>
<script type="text/javascript" src="<%= hpath %>js/brwsr/Menu.js"></script>
<script type="text/javascript" src="<%= hpath %>js/brwsr/ComboBox.js"></script>
<script type="text/javascript" src="<%= hpath %>js/brwsr/Calendar.js"></script>
<script type="text/javascript" src="<%= hpath %>js/xf/XFCommon.js"></script>
<script type="text/javascript" src="<%= hpath %>js/xf/XFActions.js"></script>
<script type="text/javascript" src="<%= hpath %>js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="<%= hpath %>js/xf/XForms.js"></script>
<link rel="stylesheet" type="text/css" href="<%= hpath %>css/Menu.css" />
<link rel="stylesheet" type="text/css" href="<%= hpath %>css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="<%= hpath %>css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="<%= hpath %>css/Wiki.css" />

	  <div id="itensil_content"></div>
      <script type="text/javascript">
		var prefs = new gadgets.Prefs();
		
		
		function init() {
		   	var src = prefs.getString("itensilhost") + "/wave/plaunch.jsp?embed=1&gmod=" + prefs.getModuleId();
			//src += "&j_signon_token=" + prefs.getString("authkey");
			var element = document.getElementById('itensil_content');  
			element.innerHTML = '<iframe name="itenFrame" src="'  + src + '" border="0" frameborder="0" width="800" height="400" class="itenFrame"></iframe>';
		}
		
		gadgets.util.registerOnLoadHandler(init);
		
	  </script>
    ]]>
  </Content>
</Module>