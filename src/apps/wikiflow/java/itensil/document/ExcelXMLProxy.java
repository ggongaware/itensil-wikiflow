package itensil.document;

import itensil.util.Check;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFName;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.AreaReference;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.util.CellReference;
import org.dom4j.Element;

public class ExcelXMLProxy {

	public static Logger logger = Logger.getLogger(ExcelXMLProxy.class);
	
	DateFormat dateFmt;
	DecimalFormat numFmt;
    boolean dirty;
	HSSFWorkbook wb;
	HSSFFormulaEvaluator evaluator;

	public ExcelXMLProxy(InputStream ins) throws IOException {
		wb = new HSSFWorkbook(ins);
		dateFmt = new SimpleDateFormat("yyyy-MM-dd");
		numFmt = new DecimalFormat();
		numFmt.setDecimalSeparatorAlwaysShown(false);
		numFmt.setGroupingUsed(false);
		dirty = false;
	}
	
	public ExcelXMLProxy() {
		wb = new HSSFWorkbook();
		wb.createSheet();
		dirty = false;
	}
	
	public HSSFWorkbook getWorkbook() {
		return wb;
	}
	
	public void getNamedGroups(Element parent) {
		int nmCount = wb.getNumberOfNames();
		for (int ii=0; ii < nmCount; ii++) {
			HSSFName nmObj = wb.getNameAt(ii);
			Element elem = parent.addElement("Named");
			elem.addAttribute("name", nmObj.getNameName());
			try {
				String ref = nmObj.getRefersToFormula();
				getCells(elem, ref);
			} catch (IllegalArgumentException iae) { /* skip it */ }
		}
	}
	
	public void getNamedGroup(Element parent, String name) {
		String ref = getRefForName(name);
		if (ref != null) {
			Element elem = parent.addElement("Named");
			elem.addAttribute("name", name);
			getCells(elem, ref);
		}
	}
	
	public String getRefForName(String name) {
		int idx = wb.getNameIndex(name);
		String ref = null;
		try {
			if (idx >= 0) {
				HSSFName nmObj = wb.getNameAt(idx);
				ref = nmObj.getRefersToFormula();
			}
		} catch (IllegalArgumentException iae) { /* leave it null */ }
		return ref;
	}
	
	public void getCells(Element parent, String ref) {
		AreaReference aref;
		try {
			aref = new AreaReference(ref);
		} catch (Exception iae) { 
			/* skip it */
			return;
		}
		getCells(parent, aref);
	}
	
	public void getCells(Element parent, AreaReference ref) {
		 CellReference firstCell = ref.getFirstCell();
		 CellReference lastCell = ref.getLastCell();
		 int sRow, eRow, sCol, eCol;
		 if (ref.isSingleCell()) {
			 
			 sRow = eRow = firstCell.getRow();
			 sCol = eCol = firstCell.getCol();
			 
		 } else {

			 sRow = firstCell.getRow();
			 eRow = lastCell.getRow();
			 sCol = firstCell.getCol();
			 eCol = lastCell.getCol();
			 
		 }
		 parent.addAttribute("ref", ref.formatAsString());
		 parent.addAttribute("rows", String.valueOf(eRow - sRow + 1));
		 parent.addAttribute("cols", String.valueOf(eCol - sCol + 1));
		 
		 HSSFSheet sheet = Check.isEmpty(firstCell.getSheetName()) ? 
				 wb.getSheetAt(0) : wb.getSheet(firstCell.getSheetName());
		
		 if (sheet == null) return;

		 for (int ii = sRow; ii <= eRow; ii++) {
			HSSFRow rowObj = sheet.getRow(ii);
			
			parent.addText("\n");
			for (int jj = sCol; jj <= eCol; jj++) {
				
				CellReference crf = new CellReference(ii, jj, false, false);
				Element cellEl = parent.addElement(crf.formatAsString());
				
				HSSFCell cellObj = rowObj != null ? rowObj.getCell(jj) : null;
				
				if (cellObj != null) {
					cellEl.setText(getCellValue(cellObj));
				}
			}
			
		 }
	}
	
	public String getCellValue(String ref) {
		return getCellValue(new AreaReference(ref));
	}
	
	public String getCellValue(AreaReference ref) {
		CellReference firstCell = ref.getFirstCell();
		HSSFSheet sheet = Check.isEmpty(firstCell.getSheetName()) ? 
				 wb.getSheetAt(0) : wb.getSheet(firstCell.getSheetName());
		
		 if (sheet == null) return "";
		 
		 HSSFRow rowObj = sheet.getRow(firstCell.getRow());
		 
		 HSSFCell cellObj = rowObj != null ? rowObj.getCell((int)firstCell.getCol()) : null;
		 
		 return cellObj != null ? getCellValue(cellObj) : "";
	}
	
	public String getCellValue(HSSFCell cellObj) {
		switch (cellObj.getCellType()) {
			case HSSFCell.CELL_TYPE_BOOLEAN:
				return cellObj.getBooleanCellValue() ? "1" : "0";
			case HSSFCell.CELL_TYPE_NUMERIC: {
				if (HSSFDateUtil.isCellDateFormatted(cellObj)) {
					Date dt = HSSFDateUtil.getJavaDate(cellObj.getNumericCellValue());
					return dateFmt.format(dt);
				} else {
					return numFmt.format(cellObj.getNumericCellValue());
				}
			}
			case HSSFCell.CELL_TYPE_STRING:
				HSSFRichTextString txt = cellObj.getRichStringCellValue();
				return txt != null ? txt.toString() : "";
			case HSSFCell.CELL_TYPE_FORMULA: {
				initEvaluator();
				try {
					CellValue cellVal = evaluator.evaluate(cellObj);
					switch (cellVal.getCellType()) {
						case HSSFCell.CELL_TYPE_BOOLEAN:
							return cellVal.getBooleanValue() ? "1" : "0";
						case HSSFCell.CELL_TYPE_NUMERIC: {
							if (HSSFDateUtil.isCellDateFormatted(cellObj)) {
								Date dt = HSSFDateUtil.getJavaDate(cellVal.getNumberValue());
								return dateFmt.format(dt);
								
							} else {
								return numFmt.format(cellVal.getNumberValue());
							}
						}
						case HSSFCell.CELL_TYPE_STRING:
							return Check.emptyIfNull(cellVal.getStringValue());
					}
				} catch (Exception ex) {
					logger.warn("Problem evaluating XLS formula", ex);
				}
				return "";
			}
		    default:
		    	return cellObj.toString();
		}
	}
	
	private void initEvaluator() {
		if (evaluator == null)
			evaluator = new HSSFFormulaEvaluator(wb);
	}

	public void setCells(Element parent, String ref) {
		AreaReference aref;
		try {
			aref = new AreaReference(ref);
		} catch (Exception iae) { 
			/* skip it */
			return;
		}
		setCells(parent, aref);
	}
	
	public void setCells(Element parent, AreaReference ref) {
		 CellReference firstCell = ref.getFirstCell();
		 CellReference lastCell = ref.getLastCell();
		 int sRow, eRow, sCol, eCol;
		 if (ref.isSingleCell()) {
			 
			 sRow = eRow = firstCell.getRow();
			 sCol = eCol = firstCell.getCol();
			 
		 } else {

			 sRow = firstCell.getRow();
			 eRow = lastCell.getRow();
			 sCol = firstCell.getCol();
			 eCol = lastCell.getCol();
			 
		 }
		 HSSFSheet sheet = Check.isEmpty(firstCell.getSheetName()) ? 
				 wb.getSheetAt(0) : wb.getSheet(firstCell.getSheetName());
		
		 if (sheet == null) return;
				 
		 for (int ii = sRow; ii <= eRow; ii++) {

			HSSFRow rowObj = sheet.getRow(ii);
			if (rowObj == null)
				rowObj = sheet.createRow(ii);
			
			for (int jj = sCol; jj <= eCol; jj++) {

				CellReference crf = new CellReference(ii, jj, false, false);
				Element cellEl = parent.element(crf.formatAsString());
				if (cellEl != null) {
					HSSFCell cellObj = rowObj.getCell(jj);
					if (cellObj == null)
						cellObj = rowObj.createCell(jj);
					
					switch (cellObj.getCellType()) {
						case HSSFCell.CELL_TYPE_BOOLEAN:
							cellObj.setCellValue("1".equals(cellEl.getTextTrim()));
					    	break;
						case HSSFCell.CELL_TYPE_NUMERIC: {
							String txt = cellEl.getTextTrim();
							if (Check.isEmpty(txt)) {
								cellObj.setCellValue(0);
							} else {
								try {
									if (HSSFDateUtil.isCellDateFormatted(cellObj)) {
										cellObj.setCellValue(dateFmt.parse(txt));
									} else {								
										cellObj.setCellValue(Double.parseDouble(txt));
									}
								} catch (Exception ex) {
									logger.warn("Problem setting XLS cell", ex);
								}
							}
					    	break;
						}
						
						case HSSFCell.CELL_TYPE_BLANK:
						case HSSFCell.CELL_TYPE_STRING:
							cellObj.setCellValue(new HSSFRichTextString(cellEl.getText()));
							break;
					}
					dirty = true;
				}
			}
		 }
	}
	
	public void evaluateAllFormulas() {
		try {
			HSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
		} catch (Exception ex) {
			logger.error("Problem evaluating XLS formulas", ex);
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}
	

}
