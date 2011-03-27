<%@ page import="itensil.security.web.UserUtil,itensil.workflow.activities.signals.SignalUtil,itensil.util.Keys,
                 itensil.util.Check"%>
                 
<% 
	String brand = Check.emptyIfNull((String)session.getAttribute("brand"));
	boolean isEmbed = Boolean.TRUE.equals(session.getAttribute("embed"));
	if ("1".equals(request.getParameter("embed"))) { 
		session.setAttribute("embed", Boolean.TRUE);
		isEmbed = true;
	} else if ("0".equals(request.getParameter("embed"))) { 
		session.setAttribute("embed", Boolean.FALSE);
		isEmbed = false;
	}
%>
<script>
	App.guest = 1;
	App.edu = <%= brand.startsWith("edu") ? 1 : 0 %>;
	App.kiosk = 1;
	App.embed = <%= isEmbed ? 1 : 0 %>;
</script>