<%@ page import="itensil.config.ConfigManager" %>
<%@ page import="itensil.web.HTMLEncode" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style type="text/css">

body {
    font-family: Tahoma, Verdana, Arial, Helvetica, sans-serif;
    font-size: 11px;
}

</style>
</head>
<body>
<fieldset>
	<legend>Configuration Problem</legend>
	<%= HTMLEncode.encode(ConfigManager.getError()) %>
</fieldset>
</body>
</html>