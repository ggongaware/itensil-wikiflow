package itensil.report;

import java.util.HashMap;

import javax.xml.transform.stream.StreamSource;

import org.dom4j.io.DocumentResult;

import itensil.repository.RepoTestHelper;
import itensil.repository.RepositoryHelper;
import itensil.security.LoginTestHelper;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import junit.framework.TestCase;

public class ReportJunit extends TestCase {

    //Repository repository;
    String mount = "/junit";
    User user;
    User user2;

    /**
     * Constructor for RepositoryJunit.
     * @param s
     */
    public ReportJunit(String s) {
        super(s);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReportJunit.class);
    }

    public void testExsltBasic() throws Exception {

        SecurityAssociation.setUser(SysAdmin.getUser());
        DocumentResult result = new DocumentResult();
        ExslTransform.transformXML(result, 
        		new StreamSource(ReportJunit.class.getResourceAsStream("xslinput1.xml")), 
        		new StreamSource(ReportJunit.class.getResourceAsStream("xslreport1.xsl.xml")), "", null);
        
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        		"<out>input1</out>", result.getDocument().asXML());
        
    }
    public void testExsltRepo1() throws Exception {
    	
    	RepositoryHelper.beginTransaction();
    	SecurityAssociation.setUser(SysAdmin.getUser());
    	User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
    	RepositoryHelper.commitTransaction();
    	
    	RepositoryHelper.beginTransaction();
    	RepoTestHelper.initRepository("/j1test", user);
        SecurityAssociation.setUser(user);
        RepoTestHelper.loadFile(ReportJunit.class.getResourceAsStream("xslinput2.xml"), "xslinput2.xml", -1,
                "/j1test/exsltreport", "text/xml");
    	RepositoryHelper.commitTransaction();
    	
    	RepositoryHelper.beginTransaction();
    	DocumentResult result = new DocumentResult();
    	ExslTransform.transformXML(result,
         		new StreamSource(ReportJunit.class.getResourceAsStream("xslinput1.xml")), 
         		new StreamSource(ReportJunit.class.getResourceAsStream("xslreport2.xsl.xml")), 
         		"/j1test/exsltreport", null);
    	
    	assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        		"<out>input1input2input2</out>", result.getDocument().asXML());
    	
    	RepositoryHelper.commitTransaction();
    }

}
