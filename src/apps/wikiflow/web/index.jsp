<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% 
if ("OPTIONS".equals(request.getMethod())) response.sendRedirect("fil/"); 
else response.sendRedirect("home/"); %>