<%@ page contentType="video/x-flv" language="java"
	import="itensil.util.Check,
			itensil.io.StreamUtil,
			java.io.File,
			java.io.FileInputStream" %>
<%

String file = request.getParameter("file");
String pos = request.getParameter("position");
int seekPos = Check.isEmpty(pos) ? 0 : Integer.parseInt(pos);
File ff = !Check.isEmpty(file) && file.endsWith(".flv") ? new File(getServletContext().getRealPath(file)) : null;
if (ff != null && ff.exists()) {
	int size = (int)ff.length();
	size -= seekPos > 0 ? seekPos + 1 : 0;
	response.addHeader("Content-Disposition", "attachment; filename=\"" + ff.getName() + "\"");
	response.setContentLength(size);
	FileInputStream fin = new FileInputStream(ff);
	
	ServletOutputStream sout = response.getOutputStream();
	
	
	if (seekPos > 0) {
		
		// Write FLV the header
		
		sout.write(new byte [] {0x46, 0x4c, 0x56, 0x01, 0x01, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x09});
		fin.skip(seekPos);
	}
	
	StreamUtil.copyStream(fin, sout);

} else {
	response.sendError(404);
}

%>
