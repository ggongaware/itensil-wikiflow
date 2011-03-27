package itensil.scripting;

import java.util.Arrays;

import org.mozilla.javascript.Context;

import itensil.io.HibernateUtil;
import itensil.report.ReportJunit;
import itensil.repository.DuplicateException;
import itensil.repository.RepoTestHelper;
import itensil.repository.RepositoryHelper;
import itensil.scripting.util.JSDomData;
import itensil.security.LoginTestHelper;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.UriHelper;
import junit.framework.TestCase;

public class WikiflowScriptJunit extends TestCase {

	
	public void testFiles() throws Exception {
		
		HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(SysAdmin.getUser());
        User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        RepoTestHelper.initRepository("/j1test", user);
        SecurityAssociation.setUser(user);
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("xslinput1.xml"), "xslinput1.xml", -1,
                "/j1test/scripting", "text/xml");
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("xslreport1.xsl.xml"), "xslreport1.xsl", -1,
                "/j1test/scripting", "text/xml");
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("UnitTester1.xls"), "UnitTester1.xls", -1,
                "/j1test/scripting", "application/vnd.ms-excel");
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("UnitRun.js"), "UnitRun.js", -1,
                "/j1test/scripting", "text/javascript");
    	RepositoryHelper.commitTransaction();
    	
    	RepositoryHelper.beginTransaction();
    	
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("data", new JSDomData(null, null));
        scr.defineObject("files", new JSFiles("/j1test/scripting", scr));
    	
    	
    	assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        		"<out>input1</out>", 
        		 scr.evaluateToString("files.xslt(files.loadXML('xslinput1.xml'), files.loadXML('xslreport1.xsl')).asXML()"));
    	byte xsltBytes[] = (byte [])Context.jsToJava(
    			scr.evaluate("files.xsltBytes(files.loadXML('xslinput1.xml'), files.loadXML('xslreport1.xsl'))"), Object.class);
    	//System.out.write(xsltBytes);
//    	assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//        		"<out>input1</out>\n", new String(xsltBytes));
    	
    	String xlsAsXml = scr.evaluateToString("files.loadExcel('UnitTester1.xls', ['B2:D4']).asXML()");
    	System.out.println(xlsAsXml);
    	
    	assertFalse(Check.isEmpty(xlsAsXml));
    	
    	assertEquals(xlsAsXml,
    			scr.evaluateToString("files.loadExcel('UnitTester1.xls', 'B2:D4').asXML()"));
    	
    	assertEquals("I ran ok", scr.evaluateToString("files.runScript('UnitRun.js')"));
    	assertEquals("/j1test/scripting/UnitRun.js|/j1test/scripting/UnitTester1.xls|" +
    			"/j1test/scripting/xslinput1.xml|/j1test/scripting/xslreport1.xsl",
    			scr.evaluateToString("files.list('/j1test/scripting').sort().join('|')"));

    	assertEquals("old", scr.evaluateToString("var ret; try {files.createFile('xslinput1.xml'); ret = 'new';} catch(e) { ret = 'old'; } ret;"));
    	assertTrue(scr.evaluateToBoolean("files.exists('UnitTester1.xls')"));
    	
    	String fname = "fold" + System.currentTimeMillis();
    	
    	scr.evaluateToString("files.setPath('/j1test'); files.createPrivateFolder('" + fname + "');");
    	
    	assertEquals("1|0|GROUP_EVERYONE", 
    			scr.evaluateToString("var p1=files.getPermissions('" + fname 
    					+ "'); [p1.length, p1[0].permission, p1[0].principal].join('|')"));

    	assertEquals("0|2|4|SIBLING|junit||", 
    			scr.evaluateToString("var cp1 = files.createPermission('GROUP_RELATIVE');" +
    					" cp1.permission = cp1.MANAGE; cp1.role = 'junit'; cp1.axis = cp1.AXIS_SIBLING;" +
    					"	files.grantPermission('" + fname 
    					+ "', cp1); var p2 = files.getPermissions('" + fname 
    					+ "'); [p2.length, p2[0].permission, p2[0].role, p2[0].axis," +
    							"p2[1].permission, p2[1].role, p2[1].axis].sort().join('|')"));
 
    	assertEquals("<rdf:RDF\n" + 
"    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
"    xmlns:rss=\"http://purl.org/rss/1.0/\" > \n" +
"  <rdf:Description rdf:about=\"http://www.xml.com/xml/news.rss\">\n" + 
"    <rss:title>XML.com</rss:title>" +
"  </rdf:Description>" +
"</rdf:RDF>"
, scr.evaluate("query.sparql('prefix rss:<http://purl.org/rss/1.0/> CONSTRUCT { ?chan rss:title ?name } from <http://www.w3.org/2000/10/rdf-tests/RSS_1.0/rss_5.3_1.rdf> where { ?chan rss:title ?name. }', '/fil/system/process/rss_5.3_1.rdf', 'results.xml')"));

//    	assertEquals(1,0);
    	RepositoryHelper.commitTransaction();
    	
    	
	}
	
	public void testEntities() throws Exception {
		
		HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(SysAdmin.getUser());
        User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
        HibernateUtil.commitTransaction();
        HibernateUtil.beginTransaction();
        RepoTestHelper.initRepository("/j1test", user);
        try {
        	RepositoryHelper.createCollection("/j1test/entity");
        } catch (DuplicateException de) {
        	// eat it 
        }
        SecurityAssociation.setUser(user);
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("fish-model.entity.xml"), "model.entity", -1,
                "/j1test/entity/fish", "text/xml");
        RepoTestHelper.loadFile(WikiflowScriptJunit.class.getResourceAsStream("fish-data.xml"), "data.xml", -1,
                "/j1test/entity/fish", "text/xml");
      
        try {
        	RepositoryHelper.createCollection("/j1test/entity/fish/records");
        } catch (DuplicateException de) {
        	// eat it 
        }
	
        
    	RepositoryHelper.commitTransaction();
    	
    	RepositoryHelper.beginTransaction();
    	
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("data", new JSDomData(null, null));
        JSFiles files;
        scr.defineObject("files", files = new JSFiles("/j1test", scr));
        scr.defineObject("entities", new JSEntities(user));
        scr.defineObject("query", new JSQuery(files));
        
        assertEquals("/j1test/entity/fish/records",
        		UriHelper.getParent(
        				scr.evaluateToString("entities.createRecord('fish')")));
        
        
        scr.evaluateToString("entities.recordActivities('fish',1)");
        scr.evaluateToString("query.sparql('fish','', '')");
        
        RepositoryHelper.commitTransaction();
        
	}
    	
	
}
