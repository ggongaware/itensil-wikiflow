<%@ page import="itensil.security.User"%>
<%@ page import="itensil.security.SysAdmin"%>
<%@ page import="itensil.util.Check"%>
<%
String userName = "";
String spaceName = "";
User user = (User)request.getUserPrincipal();
if (user != null) {
	userName = user.getSimpleName();
	spaceName = user.getUserSpace().getName();
	out.print(spaceName);
}
%>