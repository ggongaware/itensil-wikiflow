<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="itensil.web.HTMLEncode"%>
<html>
  <head>
      <script type="text/javascript">
          try {
              parent.fileUploaded("<%= request.getAttribute("clientId") %>", "<%= HTMLEncode.jsQuoteEncode(
            		  (String)request.getAttribute("uri")) %>");
          } catch (e) {}
      </script>
  </head>
</html>