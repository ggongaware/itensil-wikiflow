<%@ page import="itensil.repository.RepositoryHelper"%>
<%@ page import="itensil.web.HTMLEncode"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

<title>Knowledgebase Article</title>

<script type="text/javascript" src="../js/ScriptHost.js"></script>
<script type="text/javascript" src="../js/XMLBuilder.js"></script>
<script type="text/javascript" src="../js/CoreUtil.js"></script>
<script type="text/javascript" src="../js/brwsr/Util.js"></script>
<script type="text/javascript" src="../js/brwsr/Menu.js"></script>
<script type="text/javascript" src="../js/brwsr/DND.js"></script>
<script type="text/javascript" src="../js/brwsr/Dialog.js"></script>
<script type="text/javascript" src="../js/brwsr/Tree.js"></script>
<script type="text/javascript" src="../js/brwsr/Grid.js"></script>
<script type="text/javascript" src="../js/brwsr/ComboBox.js"></script>

<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/brd-itensil.css" />
<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />


<script type="text/javascript">
//<![CDATA[
var kbUri = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("uri")) %>";
var title = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("title")) %>";
var wik;

function setup() { //art.jsp
	App.init();
	var config = new Object();
	<% if ("1".equals(request.getParameter("showIndex"))) { %>
   config.showIndex = true;
    <% } %>
    wik = new Wiki(kbUri, document.body, title, null, config);
    wik.setWindowResize();
    App.addDispose(wik);
	App.addDispose(ActivityTree);
}

function printAllView() {
	var win = window.open("blank.html", "_blank",
	    "height=600,width=800,resizable=yes,status=no,toolbar=no,menubar=no,location=no,scrollbars=yes");
	    
	var pdoc = win.document;
	pdoc.open();
	pdoc.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
		"<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
		"<head>"+
		"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>"+
		"<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/App.css\" />"+
		"<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/brd-itensil.css\" />"+
		"<link rel=\"stylesheet\" type=\"text/css\" href=\"../css/Wiki.css\" />"+
		"</head>"+
		"<body class=\"wikiView\">\n");
	
	var tList = [];
    for (var a in wik.articleIdx) {
        tList.push([a]);
    }
    tList.sort(function (a,b) { if(a[0] == b[0]) return 0; else return a[0] > b[0] ? 1 : -1; });
    
    var pDiv = makeElement(null, "div");
    
    for (var ii = 0; ii < tList.length; ii++) {
    	var artId = tList[ii];
    	var art = wik.articleIdx[artId];
    	makeElement(pDiv, "h1", null, artId);
   		Wiki.wikify(art.getContent(), pDiv, Wiki.createContext(wik.views[0], art, null, true));
    	makeElement(pDiv, "hr");
    	makeElement(pDiv, "hr");
    }
	
	pdoc.write(pDiv.innerHTML);
	
	
	pdoc.write("</body>");
	pdoc.write("</html>");
	
	pdoc.close();
}

//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
</body>
</html>
		