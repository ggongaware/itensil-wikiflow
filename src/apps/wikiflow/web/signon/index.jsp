<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="itensil.web.HTMLEncode,
                 itensil.web.ServletUtil,
                 itensil.util.Check" %>
<%
	String brand = Check.emptyIfNull((String)session.getAttribute("brand"));
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Login</title>
<base href="<%= ServletUtil.getAbsoluteContextPath(request) + "signon/" %>"/>
<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<style type="text/css">
    .login {
        margin: 80px 80px 40px 80px;
        padding: 8px;
        width: 300px;
    }
</style>
</head>
<body>
<% if ("meet".equals(brand)) { %>
<div class="diagFrm login" style="border:1px solid #999;background-image:url(../pix/brd/mastBg.gif);background-repeat:repeat-x;">
<img src="../pix/brd/logo-meet.gif" alt="Itensil Meeting"/>
<% } else if (brand.startsWith("edu")) { %>
<div class="diagFrm login" style="border:1px solid #999;background-image:url(../pix/brd/mastBg.gif);background-repeat:repeat-x;">
<img src="../pix/brd/logo-<%= brand %>.gif" alt="Itensil Learning"/>
<% } else { %>
<div class="diagFrm login" style="border:1px solid #999;background-image:url(../pix/brd/mastBg.gif);background-repeat:repeat-x;">
<img src="../pix/brd/logo-itensil.gif" alt="Itensil"/>
<% } %>
<form method="post" action="../j_signon_check">
    <% String error = (String)request.getAttribute("error");
    if (error != null) { %>
    <p class="err"><%= error %></p>
    <% } %>
    <div style="padding-left: 100px">
    <div><label for="j_username">User ID:</label><br/>
    <input type="text" name="j_username" id="j_username" value='<%
            String name = request.getParameter("j_username");
            if (Check.isEmpty(name)) {
                name = "";
                Cookie cookies[] = request.getCookies();
                if (cookies != null) {
                    for (Cookie ck : cookies) {
                        if ("soName".equals(ck.getName())) {
                            name = ck.getValue();
                            break;
                        }
                    }
                }
            }
            out.print(HTMLEncode.encode(name));
        %>' /></div>
    <div><label for="j_password">Password:</label><br/>
    <input type="password" name="j_password" id="j_password" />
    </div>
    <div>
    Remember me <input type="checkbox" name="j_remember" value="1" checked="checked" />
    </div>
    <div><input type="submit" value="Login" /></div>
    <p><a href="forgot.jsp">Forgot Password?</a></p>
    </div>   
</form>
</div>
</body>
</html>
