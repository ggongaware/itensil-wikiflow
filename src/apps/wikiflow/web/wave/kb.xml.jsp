<%@ page contentType="text/javascript;charset=UTF-8" language="java" %>
<%@ page import="itensil.web.HTMLEncode, 
		itensil.web.ServletUtil, 
		itensil.util.UriHelper, 
		itensil.util.Keys, 
		itensil.util.Check,
		itensil.security.web.UserUtil" %>
<?xml version="1.0" encoding="UTF-8" ?>
<Module>
  <ModulePrefs title="Itensil Home">
    <Require feature="wave" />
    <Require feature="setprefs" />
  </ModulePrefs>
  <UserPref name="itensilhost" display_name="Itensil Address" required="true" default_value="<%= ServletUtil.getAbsoluteContextPath(request) %>" />
  <Content type="html" preferred_width="800" preferred_height="400" >
    <![CDATA[
	  <div id="itensil_content"></div>
      <script type="text/javascript">
		var kbUri = "../fil<%= HTMLEncode.jsQuoteEncode(request.getParameter("uri")) %>";
		var title = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("title")) %>";
		var prefs = new gadgets.Prefs();
		
		
		function init() {
		   	var src = prefs.getString("itensilhost") + "/view-wf/kb.jsp?embed=1&uri=" + escape(kbUri) + "&title=" + escape(title);
			//src += "&j_signon_token=" + prefs.getString("authkey");
			var element = document.getElementById('itensil_content');  
			element.innerHTML = '<iframe name="itenFrame" src="'  + src + '" border="0" frameborder="0" width="800" height="400" class="itenFrame"></iframe>';
		}
		
		gadgets.util.registerOnLoadHandler(init);
		
	  </script>
    ]]>
  </Content>
</Module>