<%@ page contentType="text/javascript;charset=UTF-8" language="java" %>
<%@ page import="itensil.web.HTMLEncode, 
		itensil.web.ServletUtil, 
		itensil.util.UriHelper, 
		itensil.util.Keys, 
		itensil.util.Check" %>
/**
 * (c) 2008 Itensil, Inc.
 *
 * For embeding users task and workzones in other web applications.
 * 
 * Query parameters:
 *
 *		key - guest login key
 *		width - width of frame
 *		height - height of frame
 */
 <%
 	String src = ServletUtil.getAbsoluteContextPath(request) + "home/kiosk.jsp?embed=1";
 	String fw = "400"; String fh = "300";
 	if (!Check.isEmpty(request.getParameter("width"))) {
 		fw = HTMLEncode.jsQuoteEncode(request.getParameter("width"));
 	}
 	if (!Check.isEmpty(request.getParameter("height"))) {
 		fh = HTMLEncode.jsQuoteEncode(request.getParameter("height"));
 	}
 	if (!Check.isEmpty(request.getParameter("key"))) {
 		src += "&" +  Keys.FORM_TOKEN + "=" + HTMLEncode.jsQuoteEncode(request.getParameter("key"));
 	}
 %>
 var ItenFrame = {
 	home : "<%= src %>",
 	goHome : function() {
 	 	window.open(ItenFrame.home, "itenFrame");
 	}
 };
 document.write('<iframe name="itenFrame" src="<%= src %>" border="0" frameborder="0" width="<%= fw %>" height="<%= fh %>" class="itenFrame"></iframe>');
 