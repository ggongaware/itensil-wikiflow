<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="java.util.Properties,
         		 itensil.web.HTMLEncode,
                 itensil.web.ServletUtil,
                 itensil.io.HibernateUtil,
                 itensil.mail.MailService,
				 itensil.mail.web.MailHoster,
                 itensil.security.hibernate.SignOnHB,
                 itensil.util.Check" %>
<%!
	protected MailService getMailer() {
	    return ((MailHoster)
	        getServletContext().getAttribute("mailer-default")).getMailService();
	}

	protected String sendPassword(HttpServletRequest request) throws Exception {
		String name = request.getParameter("name");
		String email = request.getParameter("email");
		String brand = (String)request.getSession().getAttribute("brand");
		if (!Check.isEmpty(name) && !Check.isEmpty(email)) {
			Thread.sleep(5000);
			
			try {
				HibernateUtil.beginTransaction();
				SignOnHB signOn = new SignOnHB();
				email = email.trim();
				String pass = signOn.resetPassword(email, "forgot");
				
				HibernateUtil.commitTransaction();
				HibernateUtil.closeSession();
				
				if (pass != null) {
					MailService mailer = getMailer();
					Properties mProps = mailer.getProperties();
					mailer.send(
							MailService.address(email, name.trim()),
			                MailService.address(
			                		mProps.getProperty("alert.from.email", "alert@itensil.com"),
			                		mProps.getProperty("alert.from.name", "Alert")),
			                "",
			                "Password Reset",
			                null,
			                "Login to:\n" + ServletUtil.getAbsoluteContextPath(request) + "\n\n" +
			                "Username: " + email + "\n" +
			                "Password: " + pass + "\n");
				}
			} catch (Exception ex) {
				log("Forgot error", ex);
			}
			return email;
		}
		return null;
	}


%>
<%
String brand = (String)session.getAttribute("brand");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Forgot</title>
<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<style type="text/css">
    .login {
        margin: 80px;
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
<form method="post">
    <% String error = (String)request.getAttribute("error");
    if (error != null) { %>
    <p class="err"><%= error %></p>
    <% } %>
    <div style="padding-left: 100px">
    <%
	String email = sendPassword(request);
	if (email != null) {	
		out.print("If this is an active account, the password will be sent to:<br/>");
		out.print(HTMLEncode.encode(email));
	%>
	<p><a href=".">Go to Login</a></p>
	<% } else { %>
    <div><label for="name">First and Last Name:</label><br/>
    <input type="text" name="name" id="name" value='' /></div>
    <div><label for="email">Email:</label><br/>
    <input type="text" name="email" id="email" value='' /></div>
    <div><input type="submit" value="Send my password" /></div>
    <% } %>
    </div> 
</form>
</div>
</body>
</html> 