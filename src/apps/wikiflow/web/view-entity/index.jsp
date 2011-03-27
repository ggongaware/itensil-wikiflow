<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title><%@ include file="../include/title.inc.jsp" %> - Entity: <%=  HTMLEncode.encode(request.getParameter("entity")) %></title>

 <!-- Core Utils-->
 <script type="text/javascript" src="../js/ScriptHost.js"></script>
 <script type="text/javascript" src="../js/XMLBuilder.js"></script>
 <script type="text/javascript" src="../js/CoreUtil.js"></script>
 <script type="text/javascript" src="../js/xpath.js"></script>
 <script type="text/javascript" src="../js/XMLEdit.js"></script>

 <!-- Main Widgets -->
 <script type="text/javascript" src="../js/brwsr/Util.js"></script>
 <script type="text/javascript" src="../js/brwsr/Menu.js"></script>
 <script type="text/javascript" src="../js/brwsr/DND.js"></script>
 <script type="text/javascript" src="../js/brwsr/Tree.js"></script>
 <script type="text/javascript" src="../js/brwsr/ComboBox.js"></script>
 <script type="text/javascript" src="../js/brwsr/Calendar.js"></script>
 <script type="text/javascript" src="../js/brwsr/Panel.js"></script>
 <script type="text/javascript" src="../js/brwsr/Dialog.js"></script>
 <script type="text/javascript" src="../js/brwsr/Grid.js"></script>
 	
 <!-- XForms Includes -->
 <script type="text/javascript" src="../js/XSchema.js"></script>
 <script type="text/javascript" src="../js/xf/XFCommon.js"></script>
 <script type="text/javascript" src="../js/xf/XFActions.js"></script>
 <script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
 <script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/Rules.js"></script>
<script type="text/javascript" src="../js/Data.js"></script>

 <!-- For the Developer Structure link -->

 <script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
 <script type="text/javascript" src="../js/brwsr/UserTree.js"></script>
 <script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
 
 <script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

 <script type="text/javascript" src="../js/App.js"></script>

 <!-- styles -->
 <link rel="stylesheet" type="text/css" href="../css/Menu.css" />
 <link rel="stylesheet" type="text/css" href="../css/Tree.css" />
 <link rel="stylesheet" type="text/css" href="../css/Panel.css" />
 <link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
 <link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
 <link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />

 <link rel="stylesheet" type="text/css" href="../css/Grid.css" />
 <link rel="stylesheet" type="text/css" href="../css/Files.css" />
 <link rel="stylesheet" type="text/css" href="../css/Entity.css" />
 <link rel="stylesheet" type="text/css" href="../css/App.css" />

<%-- 
Helpful Firebug console debug line:

console.dirxml(EntityCanvas.live.xfrm.getDefaultModel().getDefaultInstance());

--%>

<script type="text/javascript">
//<![CDATA[
//SH.debug = true;

var entityName = "<%= HTMLEncode.jsQuoteEncode(request.getParameter("entity")) %>";

function setup() {
    var ps = new PanelSetSingle();
    ps.initHelp(App.chromeHelp);
    App.addDispose(ps); // for memory leak prevention
    App.init();

    // set a header zone div
    ps.header = document.getElementById("mast");

    var panCan = new Panel("Entity Records", false);
    ps.add(panCan);
    

    // draw it, then the panel content nodes will be available
    ps.render(document.body, true); // stick it directly in the body for auto-strech
    
    
    var headElem = makeElement(null, "div", "entHead");
    panCan.setGutters(headElem);
    var btn = makeElement(headElem, "button", "xfctrl", null, null, {type:'button'});
    var label = makeElement(btn, "label", null, null, null);
    label.style.width = "auto";
    makeElement(label, "span", null, noBreakString("Create New " + entityName + "  "));
    setEventHandler(btn, "onclick", function() {
			 EntityGrid.live.createNew();
    	});
    	
    btn = null; label = null; headElem = null;
    
    var entityUri = Uri.absolute("/home/entity", entityName);
    var modelUri = Uri.absolute(entityUri, "model.entity");
    
    var eg = new EntityGrid.forPanel(panCan, modelUri, entityName, {relActs : 1});
    App.addDispose(eg);
    
    EntityGrid.live = eg;

   	document.getElementById("mast").appendChild(
   			 H.div({klass:"modes"},
   				H.div({klass:"md todo act"}, "Browse"),
   			 	H.div({klass:"md stat", onclick:"location.href='edit?entity=' + Uri.escape(entityName)"}, "Design"),
   			 	H.div({klass:"crumb"}, "Entity: " + entityName)
   			 	)
   		); 
   
   
   	//testing
  	//var diag = EntityGrid.dialog("/getModel?id=T$Bl6BgBAJo7wKhNZD9$", null, {idSet : [4,2]]});
  	//var diag = EntityGrid.dialog("/getModel?id=T$Bl6BgBAJo7wKhNZD9$", null,
  	//	 {activityKeys : {activity:"BYYggRgBAHa0wKhNZ4mJ", relName:"BookFish"} });
  	//diag.show(100,100);
}

var Wiki = {};

Wiki.popup = function(uri, title, width, height, params) {
    var url = "../kb/page?uri=" + Uri.escape(uri) + "&title=" + Uri.escape(title);
    for (var pm in params) {
    	url += "&" + Uri.escape(pm) + "=" + Uri.escape(params[pm]) ;
    }
    try {
	    var win = window.open(url, "_blank",
	    "height= " + height + "," +
	    "width= "  + width + ",resizable=yes,status=no,toolbar=no,menubar=no,location=no,scrollbars=no");
	    win.focus();
	} catch(e) {
		alert("Please turn off any pop-up blockers for this website, and try again.");
	}
};


//]]>
</script>
</head>
<body onload="setup()" onbeforeunload="return App.unloadCatch(event);" onunload="App.dispose()" scroll="no">
<% request.setAttribute("help", "Entity Edit"); %>
<%@ include file="../include/header.inc.jsp" %>
<%@ include file="../include/footer.inc.jsp" %>
</body>
</html>