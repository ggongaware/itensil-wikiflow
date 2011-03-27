<%@ page import="itensil.web.HTMLEncode"%>
<%@ page import="itensil.util.Check"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<?xml version="1.0" encoding="UTF-8" ?>
<!-- Fix for IE7 -->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<link rel="stylesheet" type="text/css" media="screen" href="http://www.editgrid.com/static/style/default.css"/>
<link rel="stylesheet" type="text/css" href="http://www.editgrid.com/static/style/grid/default.css" />
<script type="text/javascript" src="http://www.editgrid.com/js/grid.js"></script>
<script type="text/javascript">
   	// make global
  	var edGrid;
  	var edWorkbook;

	// public value retrieval method
  	function getCellValue(cellRef, sheetName) {
 		var sheet;

	    if (sheetName) {
			sheet = edWorkbook.getSheetByName(sheetName);    // Gets sheet by name for work book
	    } else {
   			sheet = edWorkbook.getSheets()[0];    // Gets all sheets for work book
	    }
	    
	    if (!sheet) return null;
	    var cell = sheet.getCell(cellRef);

	   	if (!cell) return null;
	   	
	   	return cell.getValue();
  	}
</script>

<script type="text/javascript">

function setup() {
	var sessionKey = "<%= HTMLEncode.jsQuoteEncode( Check.emptyIfNull(request.getParameter("session")) ) %>"; 

	edGrid = new editgrid.Grid({ sessionKey: sessionKey, suppressSessionKeyWarning: 1 });
	
	// Layout
	var layout = edGrid.getStandardLayout();
	layout.doLayout(document.getElementById('gridContainer'), edGrid);
	
	// Load a workbook with its path. You can also load a book with the book id or book rev id,
	// by specifying it with hash key "id" or "bookRevId"
	edGrid.openBook({ path: "<%= HTMLEncode.jsQuoteEncode( Check.emptyIfNull(request.getParameter("book")) ) %>" });

	edWorkbook = edGrid.getWorkbook(); 
	/*
	edWorkbook.addOnLoadListener(function (edWorkbook) {
    	edWorkbook.getSheets();  //.addOnValueChangeListener("A1:Z99", onRangeChange);
	});
	*/

}
</script>
</head>
<body style="margin:0;padding:0;overflow:hidden" onload="setup()" scroll="no">
    <div id="gridContainer" style="height:100%;width:100%"></div>
</body>
</html>