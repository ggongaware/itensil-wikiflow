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
		var prefs = new gadgets.Prefs();
		
		function launch_response(obj) {
			alert(obj.text);
		}
		
		function stateUpdated() {
			var actid = wave.getState().get('activityid', '');
      		
			if (!actid) {
				var params = {};
				var postData = '' +
				'<launch xmlns="">' +
		        	'<flow></flow>' +
		        	'<master-flow>/system/sysproc/Starter</master-flow>' +
		        	'<name>' + window.prompt('What would you like to name ths activity?', 'New Process') + '</name>' +
		        '</launch>' +
				'';
				params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.DOM;
				params[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.POST;
			  	params[gadgets.io.RequestParameters.POST_DATA] = gadgets.io.encodeValues(postData);
			  	var url = prefs.getString("itensilhost") + "/act/launch";
			  	gadgets.io.makeRequest(url, launch_response, params);
				document.getElementById('itensil_content').innerHTML = "Launching..."; 
				
				wave.getState().submitDelta({'activityid': actid});
			}
			
		}
		

		
		function init() {
	      if (wave && wave.isInWaveContainer()) {
	        wave.setStateCallback(stateUpdated);
	      }
	    }
    
		gadgets.util.registerOnLoadHandler(init);
		
	  </script>
    ]]>
  </Content>
</Module>