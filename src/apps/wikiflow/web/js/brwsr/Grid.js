/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: brwsr.Grid
 * Interactive Grid Classes and Functions
 * @see     ../../css/Grid.css
 * @see     ../../samples/grid.html
 * @see     ../../samples/grid-xml.html
 */

Grid.prototype.constructor = Grid;

/**
 * Grid is a typical table-style component widget with headers, columns and rows.
 * @argument model - accepts an existing model or will create a new one for you (GridModel).
 * @constructor
 */
function Grid(model) {
    this.__nRows = 0;
    this.__nCols = 0;
    if (model)  {
        this.model = model;
    } else {
        this.model = new GridModel();
    }
    this.model.grid = this;
    this.__headers = [];
}

/**
* render is NOT called automatically; must be explicitly called by the user after the model,
* data and other params are setup.
* Usage: grd.render(document.getElementById("content"));
* @argument hParent - an html element handle to render inside of, place grid in layout.
* @argument cssClass - Optional css class to use; Grid.css is default.
*/
Grid.prototype.render = function(hParent, cssClass) {
    this.__hParent = hParent;
    this.__hElem = makeElement(hParent, "table",
        "i_grid" + (cssClass != null ? " " + cssClass : ""));
    this.__hElem.setAttribute("cellPadding", "0");
    this.__hElem.setAttribute("cellSpacing", "0");
    if (this.__headers.length > 0) {
        this.__tHead = makeElement(this.__hElem, "thead");
        var tr = makeElement(this.__tHead, "tr");
        for (var ii =0; ii < this.__headers.length; ii++) {
            var hd = this.__headers[ii];
            if (hd.filter) {
            	hd.filter.render(makeElement(tr, "th", "filter " + (hd.cssClass || "")));
            } else {
            	makeElement(tr, "th", hd.cssClass, hd.label);
            }
        }
    }
    this.__tBody = makeElement(this.__hElem, "tbody");
    addEventHandler(this.__tBody, "mouseup", Grid.__mouseUp);
    this.model.onCellsReady(this);
};

/**
* addHeader - called per column, add a new header to a grid object.
* Usage: grd.addHeader("Col A");
* @argument {String} label - any text.
* @argument {String} cssClass - Optional css class to use.
* @argument {ComboBox} filterCombo - Optional filter widget
*/
Grid.prototype.addHeader = function(label, cssClass, filterCombo) {
    this.__headers.push({ label : label, cssClass : cssClass, filter : filterCombo});
};

/**
* setHeader - called per column. Similar to addHeader, but accepts an index, for existing headers.
* Usage: grd.setHeader(3, "Col A");
* @argument index - any text string.
* @argument label - any text string.
* @argument cssClass - Optional css class to use.
*/
Grid.prototype.setHeader = function(index, label, cssClass, filterCombo) {
    this.__headers[index] = {label : label, cssClass : cssClass, filter : filterCombo};
    if (this.__tHead != null) {
        var th = this.__tHead.firstChild.childNodes[index];
        if (th) {
            th.className = cssClass;
            setElementText(th, label);
        }
    }
};

/**
* editCell - set width and height of cell, based on an html element passed in.
* Usage: grd.editCell(cellA, myDiv);
* @argument cell - handle to grid cell
* @argument hElem - ?
*/
Grid.prototype.editCell = function(cell, hElem) {
    if (cell.canEdit()) {
        var edElem = Grid.__getEditElem();
        var r = getLocalBounds(this.__hParent, hElem);
        this.__hParent.appendChild(edElem);
        edElem.style.top = r.y + "px";
        edElem.style.left = r.x + "px";
        if (SH.is_safari || SH.is_opera) {
            edElem.style.width = (r.w - 1) + "px";
            edElem.style.height = (r.h - 1) + "px";
        } else {
            edElem.style.width = (r.w - 7) + "px";
            edElem.style.height = (r.h - 4) + "px";
        }
        Grid.__editCell = cell;
        edElem.value = cell.getValue();
        Grid.__editShow();
        cell.onEdit(edElem);
    }
};

/**
* renderCells - render specific area of grid, based on number of rows and columns passed in.
* Usage: grd.renderCells(3, 4);
* @argument nRows - integer
* @argument nCols - integer
*/
Grid.prototype.renderCells = function(nRows, nCols) {
    this.__nRows = nRows;
    this.__nCols = nCols;
    this.renderInsert(0, nRows);
};

/**
* renderReplace - replace the data displayed in the grid, based on an updated model, starting at a specific row number.
* Usage: grd.renderReplace(3);
* @argument startRow - integer
*/
Grid.prototype.renderReplace = function(startRow, newRowCount) {
    var kidRows = this.__tBody.childNodes;
    var ii;
    if (kidRows.length > startRow) {
        for (ii = kidRows.length - 1; ii >= startRow; ii--) {
            this.__disposeRow(kidRows[ii]);
            this.__tBody.removeChild(kidRows[ii]);
        }
    }
    if (newRowCount) this.__nRows = newRowCount;
    for (ii = startRow; ii < this.__nRows; ii++) {
        var tr = makeElement(this.__tBody, "tr",  this.model.getRowCssClass(ii));
        tr._model = this.model;
        tr._rowIdx = ii;
        addEventHandler(tr, "mouseup", Grid.__rowMouseUp);
        for (var jj = 0; jj < this.__nCols; jj++) {
            var css = (jj % 2) == 1 ? "even" : "odd";
            if (jj == 0) css += " first";
            else if (jj == (this.__nCols - 1)) css += " last";
            var cell = this.model.getCell(ii, jj);
            var td;
            if (cell && cell.colSpan) {
            	jj += cell.colSpan - 1;
            	td = makeElement(tr, "td", css, null, null, {colSpan : cell.colSpan});
            } else {
            	td = makeElement(tr, "td", css);
            }
            if (cell != null) {
                td._cell = cell;
                cell.grid = this;
                td.appendChild(cell.getHElem());
            }
        }
    }
};

/**
* renderInsert - runs once per row on initial render of grid; render grid elements and add event handlers
* per element for desired behavior.
* give arguments for for a specified number of rows
* Usage: grd.renderInsert(3, 6);
* @argument startRow - integer
* @argument nRows - integer
*/
Grid.prototype.renderInsert = function(startRow, nRows) {
    var kidRows = this.__tBody.childNodes;
    var beforeRow = null;
    var ii;

    if (kidRows.length > startRow) {
        beforeRow = kidRows[startRow];
    }
    for (ii = startRow; ii < (startRow + nRows); ii++) {
        var tr = makeElement(this.__tBody, "tr", this.model.getRowCssClass(ii), null, beforeRow);
        tr._model = this.model;
        tr._rowIdx = ii;
        addEventHandler(tr, "mouseup", Grid.__rowMouseUp);
        for (var jj = 0; jj < this.__nCols; jj++) {
            var css = (jj % 2) == 1 ? "even" : "odd";
            if (jj == 0) css += " first";
            else if (jj == (this.__nCols - 1)) css += " last";
            var cell = this.model.getCell(ii, jj);
 			var td;
            if (cell && cell.colSpan) {
            	jj += cell.colSpan - 1;
            	td = makeElement(tr, "td", css, null, null, {colSpan : cell.colSpan});
            } else {
            	td = makeElement(tr, "td", css);
            }
            if (cell != null) {
                td._cell = cell;
                cell.grid = this;
                td.appendChild(cell.getHElem());
            }
        }
    }
    this.__nRows += nRows;
};

/**
* getTreeStyle - return css data regarding the headers of the grid.
* Usage: grd.getTreeStyle();
* @return css text string
*/
Grid.prototype.getTreeStyle = function() {
    if (this.__headers.length == 0) {
        return "position:absolute;top:1px;background:none";
    } else {
        var hr = getLocalBounds(this.__hParent, this.__tHead);
        return "position:absolute;background:none;top:" + (hr.h + 1) + "px";
    }
};

/**
* setRowVisible - set a given row number to be visible or not (hide/show).
* Usage: grd.setRowVisible(4, true);
* @argument row - integer
* @argument visible - boolean
*/
Grid.prototype.setRowVisible = function(row, visible) {
    var kidRows = this.__tBody.childNodes;
    if (kidRows.length > row) {
        kidRows[row].style.display = visible ? "" : "none";
    }
};

/**
* resized - mark the grid object to be rerendered (onresize event).
* Usage: grd.resized();
*/
Grid.prototype.resized = function() {
    if (this.onresize != null) {
        this.onresize(getBounds(this.__hElem));
    }
};

/**
* resize - parent event - pass an html element with size data.
* Usage: grd.resize(myRect);
* @argument rect - html element with size coords set
*/
Grid.prototype.resize = function(rect, uiParent) {
    if (this.model.resize != null) {
        this.model.resize(rect);
    }
    
    if (uiParent) {
    	var sz = getLocalBounds(this.__hElem, uiParent);
    	if ((sz.x + sz.w) > rect.w || (sz.y + sz.h) > rect.h) {
    		uiParent.style.overflow = "auto";
    	}
    }
};

/**
* __disposeRow - internal garbage collection for IE.
* Usage: NOT PUBLIC
* @argument tr - table row
* @argument disposeCells - cells to destroy
*/
Grid.prototype.__disposeRow = function(tr, diposeCells) {
    var kidTds = tr.childNodes;
    for (var j = 0; j < kidTds.length; j++) {
        var td = kidTds[j];
        if (td._cell != null) {
            if (diposeCells) {
                td._cell.dispose();
            }
            td._cell = null;
        }
    }
};

/**
* __removeRow - internal garbage collection for IE.
* Usage: NOT PUBLIC
* @argument tr - table row
*/
Grid.prototype.__removeRow = function(tr) {
	if (tr) {
	    this.__tBody.removeChild(tr);
	    this.__disposeRow(tr, false);
	}
};

/**
* remove - destroy the grid; remove it from the layout
* Usage: grd.remove();
*/
Grid.prototype.remove = function() {
    if (this.__hElem != null && this.__hParent != null) {
        this.__hParent.removeChild(this.__hElem);
    }
    if (Grid.__editElem != null && Grid.__editElem.parentNode != null) {
        Grid.__editElem.parentNode.removeChild(Grid.__editElem);
    }
};

/**
* refreshRow - reads-out the latest value from getRowCssClass() and stuffs it in the <TR>
* Usage: grd.refreshRow();
* @argument integer index of row
*/
Grid.prototype.refreshRow = function(row) {
	var tr = this.__tBody.childNodes[row];
    if (tr) tr.className = this.model.getRowCssClass(row);
};

/**
* dispose - public garbage collection method for IE.
* Usage: grd.dispose();
*/
Grid.prototype.dispose = function() {
    if (this.__hElem != null) {
        var kidRows = this.__tBody.childNodes;
        for (var i =0; i < kidRows.length; i++) {
            this.__disposeRow(kidRows[i], true);
        }
        this.__hParent = null;
        this.__hElem = null;
        this.__tBody = null;
        this.__tHead = null;
    }
    Grid.__editElem = null;
    Grid.__editCell = null;
};

Grid.__editElem = null;
Grid.__editCell = null;

Grid.__mouseUp = function(evt) {
    var elem = getEventElement(evt);
    while (elem != null && elem !== this && elem._cell == null) {
        elem = elem.parentNode;
    }
    if (elem != null && elem !== this) {
        elem._cell.grid.editCell(elem._cell, elem);
    }
};


Grid.__rowMouseUp = function(evt) {
	if (this._model && this._model.onRowMouseUp)
		return this._model.onRowMouseUp(evt, this._rowIdx);
	return false;
};

Grid.__getEditElem = function() {
    if (Grid.__editElem == null) {
        Grid.__editElem = makeElement(null, "input", "i_gridEdit", "text", null, {name : "grid.ed"});
        if (SH.is_gecko) Grid.__editElem.setAttribute("autocomplete", "off");
    }
    return Grid.__editElem;
};

Grid.__editShow = function() {
    Grid.__editStarted = (new Date()).getTime();
    Grid.__editElem.style.display = "";
    window.setTimeout(function() {
            Grid.__editElem.focus();
        }, 10);
    Grid.__editElem.onblur = Grid.__editBlur;
    setEventHandler( Grid.__editElem, "onkeydown", Grid.__editInputKeyDown);
};

Grid.__editInputKeyDown = function(evt) {
    var code;
    if (evt.keyCode) code = evt.keyCode;
	else if (evt.which) code = evt.which;

	if (code == 13) { // return key
	    evt.cancelBubble = true;
	   	Grid.__editFinish();
	}
};

Grid.__editBlur = function () {
    if ((Grid.__editStarted + 500) < (new Date()).getTime()) {
        Grid.__editFinish();
    }
};

Grid.__editFinish = function() {
    if (Grid.__editElem != null) {
    	if (Grid.__editCell != null) {
    		if (!Grid.__editCell.onEditCanFinish(Grid.__editElem)) return;
    	}
        Grid.__editElem.onblur = null;
        Grid.__editElem.style.display = "none";
        if (Grid.__editCell != null) {
            Grid.__editCell.setValue(Grid.__editElem.value);
            Grid.__editCell.onEditFinish(Grid.__editElem);
            Grid.__editCell = null;
        }
    }
};

Grid.__editCancel = function() {
    if (Grid.__editElem != null) {
        Grid.__editElem.onblur = null;
        Grid.__editElem.style.display = "none";
        if (Grid.__editCell) Grid.__editCell.onEditCancel(Grid.__editElem);
        Grid.__editCell = null;
    }
};

GridCell.prototype.constructor = GridCell;

/**
 * GridCell is an individual cell object, a collection of which create the grid object.
 * @argument value - text, number or html string.
 * @argument cssClass - Optional css class to use.
 * @argument noEdit - Optional disable editing?
 * @constructor
 */
function GridCell(value, cssClass, noEdit) {
    this.__hElem = null;
    this.__edit = false;
    this.__css = cssClass || "";
    if (arguments.length > 0) {
        this.__edit = noEdit ? false : true;
        this.__value = value;
        this.__hElem = makeElement(
            null, "div", cssClass + " i_g", value == "" ? "\u00a0" : value);
    }
}

/**
* canEdit - query a cell for editability setting.
* Usage: grd.canEdit();
* @return boolean true/false
*/
GridCell.prototype.canEdit = function() {
    return this.__edit;
};

/**
* setReadonly - set a cells editability setting on or off.
* Usage: grd.setReadonly(true);
* @argument bool - set true or false
*/
GridCell.prototype.setReadonly = function(bool) {
    this.__edit = !bool;
    if (this.__hElem != null) {
        exAddClass(this.__hElem, "readonly", !bool);
    }
};

/**
* setValue - set the value of a given grid cell.
* Usage: grd.setValue(true);
* @argument value - text, number or html string.
*/
GridCell.prototype.setValue = function(value) {
    this.__value = value;
    if (this.__hElem == null) {
        this.__hElem = makeElement(
            null, "div", this.__css + " i_g", value == "" ? "\u00a0" : value);
    } else {
        setElementText(this.__hElem, value == "" ? "\u00a0" : value);
    }
};

/**
* getHElem - returns an html element for the grid cell object.
* Usage: var hElem = gridcell.getHElem();
* @returns html element of the object in the layout.
*/
GridCell.prototype.getHElem = function() {
    return this.__hElem;
};

/**
* getValue - called for edit-mode and sorting.
* Usage: var hElem = gridcell.getValue();
* @returns text string value of the cell.
*/
GridCell.prototype.getValue = function() {
    return this.__value;
};

/**
* dispose - public garbage collection method for IE.
* Usage: gridcell.dispose();
*/
GridCell.prototype.dispose = function() {
    this.__hElem = null;
};

/**
 * Grid edit event interfaces
 */
GridCell.prototype.onEdit = function(editElem) { };
GridCell.prototype.onEditCancel = function(editElem) { };
GridCell.prototype.onEditFinish = function(editElem) { };
GridCell.prototype.onEditCanFinish = function(editElem) { return true };



GridModel.prototype.constructor = GridModel;

function GridModel() {
    this.rows = [];
    this.nCols = 0;
    this.selectedRow = -1;
}

/**
* onCellsReady - override this method locally to populate a grid from a xml datasource.
* Usage: XGridModel.prototype.onCellsReady = function(grid)
* @argument grid - pass the local instance of the grid to populate it with data.
*/
GridModel.prototype.onCellsReady = function(grid) {
    grid.renderCells(this.rows.length, this.nCols);
};

/**
* onRowMouseUp - row click event handler, called by Grid.__rowMouseUp
* Usage: this._model.onRowMouseUp(evt, this._rowIdx);
* @argument evt - pass in the mouse event.
* @argument row - handle to the row in question.
*/
GridModel.prototype.onRowMouseUp = function(evt, row) {
    this.selectRow(row);
    return false;
};

/**
* getRowCssClass - return the name of the css class assigned a given row, override locally?
* Usage: XGridModel.getRowCssClass(row);
* @argument row - handle to the row in question.
*/
GridModel.prototype.getRowCssClass = function(row) {
    return row === this.selectedRow ? "g_select" : null;
};

/**
* getCell - returns the cell model handle (or value?) of a given location in the grid.
* Usage: var mycell = XGridModel.getCell(3, 5);
* @argument row - row integer index of cell in grid.
* @argument col - column integer index of cell in grid.
* @returns cell model handle or value?
*/
GridModel.prototype.getCell = function(row, col) {
    var rowArr = this.rows[row];
    if (rowArr == null) return null;
    return rowArr[col];
};

/**
* setCell - set the contents of a NEW grid cell into the grid.
* Usage: grd.model.setCell(new GridCell("Walking"), 0, 0);
* @argument cell - cell contents in text, number or html string
* @argument row - integer counter row
* @argument col - integer counter column
*/
GridModel.prototype.setCell = function(cell, row, col) {
    var rowArr = this.rows[row];
    if (rowArr == null) {
        rowArr = [];
        this.rows[row] = rowArr;
    }
    if (col >= this.nCols) this.nCols = col + 1;
    rowArr[col] = cell;
};

GridModel.prototype.selectRow = function(rowIdx) {
    if (this.selectedRow >= 0) {
		var oldSelect = this.selectedRow;
		this.selectedRow = -1;
		this.grid.refreshRow(oldSelect);
	}
    this.selectedRow = rowIdx;
    this.grid.refreshRow(rowIdx);
};


TreeGridModel.prototype = new TreeModel();
TreeGridModel.prototype.constructor = TreeGridModel;
/**
 * TreeGridModel applies the tree model superimposed into a grid; usefull to display metadata
 * of leaf items in a tree.
 * @constructor
 * @extends TreeModel
 */
function TreeGridModel() {
    TreeModel.apply(this, []);
    this.zeroCol = 0;
    this.rows = null;
    this.grid = null;
}

TreeGridModel.prototype.addItemRows = function (items, itemParent) {
    if (this.rows == null) this.rows = [];
    var i;
    var insIdx;
    if (itemParent === this) {
    	insIdx = this.rows.length;
        for (i = 0; i < items.length; i++) {
            var item = items[i];
            item.rowIdx = this.rows.length;
            this.rows.push(item);
        }
    } else {
    	insIdx = itemParent.rowIdx + 1;
        this.rows.splice.apply(
            this.rows, [insIdx, 0].concat(items));
        for (i = insIdx; i < this.rows.length; i++) {
            this.rows[i].rowIdx = i;
        }
    }
    if (itemParent === this && insIdx == 0) {
    	if (items.length > 0) {
    		var nCols = items[0].cellZero.colSpan || 1;
    		for (i = 0; i < items[0].cells.length; i++) {
    			nCols += items[0].cells[i].colSpan || 1;
    		}
       	 	this.grid.renderCells(this.rows.length, nCols);
    	}
    } else {
        this.grid.renderInsert(insIdx, items.length);
    }
    for (i = 0; i < items.length; i++) {
        items[i].syncSize(true);
    }
};

TreeGridModel.prototype.onReady = function(callback, tree, itemParent) {
    var items = itemParent.kids;
    callback.apply(tree, [items, itemParent]);
    this.addItemRows(items, itemParent);
};

TreeGridModel.prototype.onCellsReady = function(grid) {
    this.grid = grid;
};

TreeGridModel.prototype.getCell = function(row, col) {
    var rowItem = this.rows[row];
    if (rowItem == null) return null;
    if (col === this.zeroCol) {
        return rowItem.cellZero;
    }
    return rowItem.getCell(col > this.zeroCol ? col - 1 : col);
};

TreeGridModel.prototype.getRowCssClass = function(row) {
    var rowItem = this.rows[row];
    if (rowItem == null) return null;
    return (rowItem.rowCssClass || "")+ (rowItem.itemParent.getIndex(rowItem) % 2 == 1 ? "" : " alt");
};

TreeGridModel.prototype.removeItemRow = function(item) {
    var idx = arrayFindStrict(this.rows, item);
    if (idx < 0) return;
    this.rows.splice(idx, 1);
    for (var ii = idx; ii < this.rows.length; ii++) {
        this.rows[ii].rowIdx = ii;
    }
};

TreeGridModel.prototype.resize = function(rect) {
	if (!this.rows) return;
    for (var ii = 0; ii < this.rows.length; ii++) {
        this.rows[ii].syncSize(true);
    }
};

TreeGridItem.prototype = new TreeItem();
TreeGridItem.prototype.constructor = TreeGridItem;

function TreeGridItem(model, label, allowsKids, icon) {
    TreeItem.apply(this, [model, label, allowsKids, icon]);
    this.cellZero = new TreeGridCellZero(this);
    this.cells = [];
    this.borderH = 2;
}

TreeGridItem.prototype.syncSize = function(checkGrid) {
    var r = getLocalBounds(this._tree.__domElem, this.__domElem);
    if (r.h < 1) return;
    var hElem = this.cellZero.__hElem;
    if (checkGrid) {
        if (this.wasSynched) {
            var s = getSize(hElem.parentNode);
            if (r.h < s.h) {
                this.__domElem.style.height = (s.h) + "px";
                this.__domElem.style.width = r.w + "px";
                r.h = s.h - this.borderH;
            } else if (r.h > s.h) {
                this.__domElem.style.height = (r.h + this.borderH) + "px";
            } else {
                return;
            }
        } else {
            var holdThis = this;
            window.setTimeout(function() {holdThis.syncSize(true); }, 1);
        }
    }
    this.wasSynched = true;
    hElem.style.width = (r.x + r.w + 35) + "px";
    hElem.style.height = (r.h) + "px";
    this.model.grid.resized();
};

TreeGridItem.prototype.getCell = function(col) {
    return this.cells[col];
};

TreeGridItem.prototype.toggled = function() {
    Grid.__editFinish();
    var rows = this.model.rows;
    var grid = this.model.grid;
    var rowItem, i;
    if (!this.allowsKids) return this.rowIdx;

    var myDepth = 0;
    var depth;
    var pr;
    for (pr = this.itemParent; pr !== this.model; pr = pr.itemParent) myDepth++;
    for (i = this.rowIdx + 1; i < rows.length; i++)  {
        rowItem = rows[i];
        if (rowItem.itemParent === this.itemParent) break; // hit sibling
        depth = 0;
        for (pr = rowItem.itemParent; pr !== this.model; pr = pr.itemParent ) depth++;
        if (depth <= myDepth) break;
        if (this.expanded) {
            if (rowItem.itemParent === this) { // kids
                grid.setRowVisible(i, true);
                if (rowItem.expanded) i = rowItem.toggled();
            }
        } else {
            grid.setRowVisible(i, false);
        }
    }
    this.model.grid.resized();
    return i - 1;
};

TreeGridItem.prototype.optAct = treeMenuAction;
TreeGridItem.prototype.act = treeMenuAction;

TreeGridItem.prototype.editAct = function(evt) {
    Grid.__editFinish();
    var edElem = Grid.__getEditElem();
    var r = getLocalBounds(this._tree.__domElem, this.__labDomE);
    this._tree.__domElem.appendChild(edElem);
    edElem.style.top = r.y + "px";
    edElem.style.left = r.x + "px";
    if (SH.is_safari || SH.is_opera) {
        edElem.style.width = r.w  + "px";
        edElem.style.height = r.h + "px";
    } else {
        edElem.style.width = (r.w - 6) + "px";
        edElem.style.height = (r.h - 3) + "px";
    }
    Grid.__editCell = this;
    edElem.value = this.label;
    Grid.__editShow();
};

TreeGridItem.prototype.onEditCanFinish = function(editElem) { return true };
TreeGridItem.prototype.onEditFinish = function(editElem) { };

TreeGridItem.prototype.setValue = function(value) {
    this.setLabel(value);
    this.syncSize(true);
};

TreeGridItem.prototype.dispose = function() {
    TreeItem.prototype.dispose.apply(this, []);
    if (this.cellZero != null) {
        this.cellZero.dispose();
        this.cellZero = null;
    }
    if (this.cells != null) {
        for (var i = 0; i < this.cells.length; i++) {
            this.cells[i].dispose();
        }
        this.cells = null;
    }
};

TreeGridItem.prototype.remove = function() {
    if (this.cellZero.__hElem.parentNode) {
        var tr = this.cellZero.__hElem.parentNode.parentNode;
        var rows = this.model.rows;
        var grid = this.model.grid;
        this.model.removeItemRow(this);
        grid.__removeRow(tr);
    }
};


TreeGridCellZero.prototype = new GridCell();
TreeGridCellZero.prototype.constructor = TreeGridCellZero;

function TreeGridCellZero() {
    this.__hElem = makeElementNbSpd(null, "div", "treeCellZero");
}

TreeGridCellZero.prototype.canEdit = function() {
    return false;
};





DirGridModel.prototype = new TreeGridModel();
DirGridModel.prototype.constructor = DirGridModel;

function DirGridModel(/*String*/ path, /*Array<String>*/ list) {
    TreeGridModel.apply(this, []);
    this.path = path;
    this.list = list;
}


DirGridModel.escapeRegExp = function (s) {
    // Escape any special characters with that character preceded by a backslash
    return s.replace(new RegExp("([\\\\\\^\\$\\*\\+\\?\\(\\)\\=\\!\\|\\,\\{\\}\\[\\]\\.])","g"),"\\$1")
}

DirGridModel.prototype.onReady = function(callback, tree, itemParent) {
    var uniques = new Object();
    var path = "";
    if (itemParent != this) {
        path = itemParent.path + "/";
    }
    var rg = new RegExp("^" + DirGridModel.escapeRegExp(path) + "([^/]*)([/]?)", "i");
    for (var ii = 0; ii < this.list.length; ii++) {
         var dstr = this.list[ii];
         if(typeof dstr != "string") {     //TreeGridItem(model, label, allowsKids, icon)
            item = new TreeGridItem(this);
            item.setCssClass(dstr.cssClass);
            itemParent.add(item);
            item.noDrag = true;
            continue;
         }
         var cols = dstr.split("|");
         var mr = rg.exec(cols[0]);
         if (mr) {
            var lm = mr[1].toLowerCase();
            var item;
            if (!(lm in uniques)) {
                var ipath = path + mr[1];
                item = new TreeGridItem(this, mr[1], mr[2] != "");
                item.path = ipath;
                if (item.allowsKids) item.icon = "fldIco";
                itemParent.add(item);
                uniques[lm] = item;
                for (var jj = 1; jj < cols.length; jj++) {
	            	var cell = new GridCell(cols[jj]);
	            	item.cells.push(cell);
	            }
            }
         }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
};


DirGridModel.prototype.dispose = function() {
    TreeModel.prototype.dispose.apply(this, []);
    this.rows = null;
    this.grid = null;
    this.node = null;
};

