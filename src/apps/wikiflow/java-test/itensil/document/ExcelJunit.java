package itensil.document;

import java.io.OutputStreamWriter;

import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import junit.framework.TestCase;

public class ExcelJunit extends TestCase {

	public void testXLSReading() throws Exception {
		ExcelXMLProxy xmlProx = new ExcelXMLProxy(getClass().getResourceAsStream("UnitTester1.xls"));
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("excel");
		
		Element cells = root.addElement("cells");
		cells.addAttribute("ref", "B2:D4");
		
		xmlProx.getCells(cells, "B2:D4");
		
		assertEquals("3", cells.attributeValue("rows"));
		assertEquals("3", cells.attributeValue("cols"));
		
		assertEquals(9, cells.elements().size());
		
		assertEquals("d4", cells.selectSingleNode("D4").getText());
		
		
		xmlProx.getNamedGroups(root);
		assertEquals(4, root.elements("Named").size());
		
		assertEquals("c4", root.selectSingleNode("Named[@name='Group1']/*[1 * (../@cols) + 1]").getText());
		
		assertEquals("1977-05-10", root.selectSingleNode("Named[@name='sheetToo']/*[2]").getText());
		
		assertEquals("202", root.selectSingleNode("Named[@name='theSum']/*").getText());
		
		OutputStreamWriter out = new OutputStreamWriter(System.out);
		doc.write(out);
		out.flush();
		System.out.println("\n---");
	}
	
	public void testXLSWriting() throws Exception {
		
		ExcelXMLProxy xmlProx = new ExcelXMLProxy();
		Document doc = DocumentHelper.parseText("<cells>"
				 + "\n<B2>b2</B2><C2>c2</C2><D2>d2</D2>"
				 + "\n<B3>b3</B3><C3>c3</C3><D3>d3</D3>"
				 + "\n<B4>b4</B4><C4>c4</C4><D4>d4</D4>"
				 + "\n</cells>");
		
		xmlProx.setCells(doc.getRootElement(), "B2:D4");
		
		HSSFWorkbook wb = xmlProx.getWorkbook();
		
		assertEquals(1, wb.getNumberOfSheets());
		HSSFRow rowObj = wb.getSheetAt(0).getRow(2);
		assertEquals("c3", rowObj.getCell(2).toString());
		
		ExcelExtractor extr = new ExcelExtractor(wb);
		
		System.out.println(extr.getText());
		System.out.println("---");
	}
	
	public void testBudgetLoad() throws Exception {
		ExcelXMLProxy xmlProx = new ExcelXMLProxy(getClass().getResourceAsStream("budgetworkbook_unp.xls"));
		
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("excel");
		xmlProx.getCells(root, "G20:G20");
		
		OutputStreamWriter out = new OutputStreamWriter(System.out);
		doc.write(out);
		out.flush();
		System.out.println("\n---");
	}
}
