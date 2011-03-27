<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.UriHelper"%>
<%@ page
	import="itensil.repository.*,itensil.security.*,itensil.security.hibernate.*,itensil.io.HibernateUtil,itensil.util.Check,itensil.web.*,java.util.*,itensil.workflow.activities.UserActivities,itensil.workflow.activities.state.Activity,itensil.workflow.activities.state.FlowState"%>
<%@ page
	import="itensil.security.web.UserUtil,itensil.workflow.activities.signals.SignalUtil,itensil.util.Keys,itensil.util.Check"%>

<html>
<head>
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
<script type="text/javascript" src="../js/brwsr/Calendar.js"></script>
<script type="text/javascript" src="../js/brwsr/Panel.js"></script>

<script type="text/javascript" src="../js/xpath.js"></script>
<script type="text/javascript" src="../js/XSchema.js"></script>
<script type="text/javascript" src="../js/Rules.js"></script>
<script type="text/javascript" src="../js/Data.js"></script>
<script type="text/javascript" src="../js/xf/XFCommon.js"></script>
<script type="text/javascript" src="../js/xf/XFActions.js"></script>
<script type="text/javascript" src="../js/xf/brwsr/XFControls.js"></script>
<script type="text/javascript" src="../js/xf/XForms.js"></script>

<script type="text/javascript" src="../js/brwsr/FileTree.js"></script>
<script type="text/javascript" src="../js/brwsr/ActivityTree.js"></script>
<script type="text/javascript" src="../js/brwsr/UserTree.js"></script>

<script type="text/javascript" src="../js/brwsr/EntityGrid.js"></script>

<script type="text/javascript" src="../js/brwsr/ProcMap.js"></script>
<script type="text/javascript" src="../js/brwsr/Wiki.js"></script>
<script type="text/javascript" src="../js/brwsr/WikiEdit.js"></script>
<script type="text/javascript" src="../js/brwsr/ProcCanvas.js"></script>

<script type="text/javascript" src="../js/App.js"></script>

<link rel="stylesheet" type="text/css" href="../css/Menu.css" />
<link rel="stylesheet" type="text/css" href="../css/ComboBox.css" />
<link rel="stylesheet" type="text/css" href="../css/Calendar.css" />
<link rel="stylesheet" type="text/css" href="../css/Dialog.css" />
<link rel="stylesheet" type="text/css" href="../css/Panel.css" />
<link rel="stylesheet" type="text/css" href="../css/Tree.css" />
<link rel="stylesheet" type="text/css" href="../css/Grid.css" />
<link rel="stylesheet" type="text/css" href="../css/Files.css" />
<link rel="stylesheet" type="text/css" href="../css/ProcMap.css" />
<link rel="stylesheet" type="text/css" href="../css/Wiki.css" />
<link rel="stylesheet" type="text/css" href="../css/Entity.css" />

<link rel="stylesheet" type="text/css" href="../css/App.css" />




<style>
table {
	text-decoration: none;
	font-family: arial, helvetica, sans-serif;
	font-size: 11px;
	color: #000;
	padding-bottom: 1px;
}

td {
	font-family: arial, helvetica, sans-serif;
	font-size: 14px;
	font-weight:normal;
	text-align: left;
	color: #000;
}

td.title {
	font-family: arial, helvetica, sans-serif;
	font-size: 16px;
	font-weight:bold;
	color: #000;
}

</style>

<script type="text/javascript"> 
//<![CDATA[

           
function displayEntity(name, id) {
	var ps = App.getPanelSet(0);
    
    panComp = new Panel("Entity", false);
    ps.addMajor(panComp);
    ps.render(document.body, true);
	 
    var entityUri = Uri.absolute("/home/entity", name);
    var modelUri = Uri.absolute(entityUri, "model.entity");
    
	var config = {relActs : 1};
	if(id && id !='null') {
	   	config = {relActs : 1, idSet:[id]};
	}

    var eg = new EntityGrid.forPanel(panComp, modelUri, name, config);
    App.addDispose(eg);
}
           

           
           
function doSave(thisForm) {
  App.init();
  var formElem = document.getElementById('actEntitySelect');  
  var divElem = document.getElementById('div123');  
  var appId = '<%=request.getParameter("appId")%>';
  var appProcessId = '<%=request.getParameter("appProcessId")%>';
  var cmpType = thisForm.form.cmpType.value;
  var cmpName = thisForm.form.cmpName.value;
  var cmpId = thisForm.form.cmpId.value;

  if(cmpType == 'ACT' ) {
	  if(!cmpName && !cmpId) { alert("Select a process and name your new project"); return; }
	  if(!cmpName) { alert("Select a process"); return; }
	  if(!cmpId) { alert("Name your new project"); return; }
  }
  else if(cmpType == 'ENT' && !cmpName) {
		alert("Name required for Entity");
		return;
  }

  var uri = "../index/join?appId=" + Uri.escape(appId) + "&appProcessId=" + Uri.escape(appProcessId) +
    	       "&cmpType=" + Uri.escape(cmpType)  + "&cmpName="  + Uri.escape(cmpName) + "&cmpId=" + Uri.escape(cmpId);

 	// Step 1. Save map 	
  	var resDoc = Data.xb.loadURI(uri);
        if (App.checkError(resDoc)) {
            if (cmpType == 'ACT') {
                alert('Error loading Activity');
                return;
            }
            else {
                alert('Error loading Activity');
                return;
                }
        }
		// For activities cmpId value is replaced by activityId value
        if(resDoc) {
    		if (Xml.matchOne(resDoc.documentElement, "map")) {
    			map = Xml.matchOne(resDoc.documentElement, "map");
    		    cmpId = map.getAttribute("cmpId");
    		}
    	}

	// Step 3. Show Activity or Entity display    
    if(cmpType == 'ACT') {
    	var node = ActivityTree.activityInfo(cmpId, true);    
        // TODO activity info test if flow exist
        App.activeActivityId = node.getAttribute("id");

    	location.href = '../act/page?kiosk=1&embed=1&activity=' + App.activeActivityId;
    }
    else if (cmpType == 'ENT') {
		displayEntity(cmpName, cmpId);
    }    	
  }
           
//]]>
</script>



</head>
<body>
<div id="actBody"></div>

<div id="div123">
<form method="GET" id="actEntitySelect"
	enctype="application/x-www-form-urlencoded">


<table>
<table>
	<tr>
		<td class="title" align="left" Valign="MIDDLE"><b>Collaboration Enabler</b>  - Trial</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>Select a business process and name your project </td>
	</tr>
</table>
<table>
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><select name="cmpType">
			<option value="ACT">Activity</option>
		</select></td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
</table>
<table>
	<tr>
		<td>Process</td>
		<td>&nbsp;</td>
		<td>
		<select name="cmpName">
			<option value=>-Select one-</option>

<% 
		RepositoryHelper.beginTransaction();
		RepositoryHelper.useReadOnly();
		User user = (User)request.getUserPrincipal();
		UserActivities uActivities = new UserActivities(user, HibernateUtil.getSession());
		
		//root.addAttribute("uri", UriHelper.absoluteUri(RepositoryHelper.getPrimaryRepository().getMount(), "process"));
		
		for (RepositoryNode kid : uActivities.getActiveFlows(22)) {
			if (kid.isCollection()) {
				String kName = UriHelper.name(kid.getUri());
				%>
			<option value="<%= kName %>"><%= kName %></option>
				<% 
			}
		}
		RepositoryHelper.commitTransaction();
%>
		</td>
	</tr>
	<tr>
		<td>Name</td>
		<td>&nbsp;</td>
		<td><INPUT TYPE="text" NAME="cmpId" VALUE=""
			SIZE="50" MAXLENGTH="256"></td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><INPUT TYPE="hidden" NAME="appProcessId"
	VALUE="<%=request.getParameter("appProcessId")%>" SIZE="128"
	MAXLENGTH="128" /> <INPUT type="button" value="Submit"
	onclick="doSave(this)" />
		</td>
	</tr>
<INPUT TYPE="hidden" NAME="appId"
	VALUE="<%=request.getParameter("appId")%>" SIZE="10" MAXLENGTH="10" />

</table>
<table>
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>Requirements: Chrome browser version 4+; login to your itensil account on same browser before use</td>
	</tr>
</table>
</table>
</form>
</div>
</body>
</html>