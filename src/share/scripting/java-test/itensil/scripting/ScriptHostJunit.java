package itensil.scripting;

import junit.framework.TestCase;
import itensil.scripting.ScriptHost;
import itensil.scripting.ScriptError;

import itensil.scripting.util.JSWebService;
import itensil.scripting.util.JSDomData;


import org.apache.axis2.client.ServiceClient;
import org.dom4j.DocumentHelper;
import org.dom4j.dom.DOMDocumentFactory;
import org.dom4j.io.SAXReader;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ScriptHostJunit extends TestCase {

    /**
     * Constructor for ScriptHostJunit.
     * @param s
     */
    public ScriptHostJunit(String s) {
        super(s);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ScriptHostJunit.class);
    }


    public void testVersion() throws ScriptError {

        ScriptHost scr = new ScriptHost("junit");
        assertEquals("1.7", scr.evaluateToString("Host.version;"));
        assertTrue(scr.evaluateToBoolean("true;"));
    }

    public void testReset() throws ScriptError {

        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("taco", "all beef");
        assertEquals("all beef", scr.evaluateToString("taco;"));
        assertEquals("fresh",
            scr.evaluateToString("var patty = 'fresh'; patty;"));
        assertEquals("fresh", scr.evaluateToString("patty;"));
        scr.reset();
        assertEquals("dead",
            scr.evaluateToString(
                "try {patty;} catch(e) {'dead';}"));
        assertEquals("all beef", scr.evaluateToString("taco;"));
    }
    
    public void testXMLHttpRequest()  throws ScriptError {
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("ws", new JSWebService());
        String body = scr.evaluateToString("var xhttp = ws.getXMLHttpRequest();" +
        		"xhttp.open('GET','http://www.google.com');" +
        		"xhttp.send();" +
        		"xhttp.responseText");
        System.out.println(body);
    }

    public void testSoap()  throws ScriptError {
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("ws", new JSWebService());
        
        
        assertEquals("0", scr.evaluateToString(
            "ws.soapCall('http://www.webservicex.net/CurrencyConvertor.asmx'," +
            " 'http://www.webserviceX.NET/', 'ConversionRate', { FromCurrency :'USD', ToCurrency :  'USD'} );"
        ));
        
        
        assertTrue(scr.evaluateToNumber(
            "ws.soapCall('http://www.webservicex.net/CurrencyConvertor.asmx'," +
            " 'http://www.webserviceX.NET/', 'ConversionRate', { FromCurrency :'AUD', ToCurrency :  'USD'} );"
        ) > 0.0);
        
        try {
            scr.evaluateToNumber(
                "ws.soapCall('http://www.webservicex.net/CurrencyConvertor.asmx'," +
                " 'http://www.webserviceX.NET/', 'BadMethod', { FromCurrency :'AUD', ToCurrency :  'USD'} );");
            fail();
        } catch (ScriptError se) {
            System.out.println(se.toString());
        }

    }

    public void testDomData()  throws Exception {

        String xmlsrc = "<data xmlns=\"\">\n" +
                "<view-item>1</view-item>\n" +
                "<item>\n" +
                "    <color>red</color>\n" +
                "</item>\n" +
                "</data>";
        JSDomData jsd = new JSDomData(DocumentHelper.parseText(xmlsrc), null);
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("data", jsd);
        assertEquals("red",
            scr.evaluateToString("data.getValue('item/color');"));
        assertFalse(jsd.isDirty());
        scr.evaluateToString("data.setValue('item/color', 'blue');");
        assertTrue(jsd.isDirty());
        assertEquals("blue",
            scr.evaluateToString("data.getValue('item/color');"));
    }
    
    public void testDomDoc()  throws Exception {

        JSDomData jsd = new JSDomData(null, null);
        ScriptHost scr = new ScriptHost("junit");
        scr.defineObject("data", jsd);
        scr.evaluate("data.setDocument(data.parseXML(\"<data>" +
                "<view-item>1</view-item>" +
                "<item>" +
                "    <color>red</color>" +
                "</item>" +
                "</data>\"))");
                        
        assertEquals("red",
            scr.evaluateToString("data.getValue('item/color');"));
        assertFalse(jsd.isDirty());
        scr.evaluateToString("data.setValue('item/color', 'blue');");
        assertTrue(jsd.isDirty());
        assertEquals("blue",
            scr.evaluateToString("data.getValue('item/color');"));
    }
    
    public void testSoapMessage() throws Exception {
		 ScriptHost scr = new ScriptHost("junit");
	     scr.defineObject("ws", new JSWebService());
	     scr.defineObject("data", new JSDomData(null, null));
	     scr.defineObject("helloBytes", "hello".getBytes());
         System.out.println(
	     scr.evaluateToString(
	    		 "var msg = ws.createSoapMessage();" +
	    		 "msg.setBody(data.parseXML(\"" + 
		    		 "<gg:GetOpportunityListRequest xmlns:gg='http://apply.grants.gov/WebServices/ApplicantIntegrationServices-V1.0' xmlns:mm='yaya'>"
	    	    	 + "   <gg:OpportunityID>ED-GRANTS-102003-003</gg:OpportunityID>"
	    	    	 + "   <gg:CFDANumber mm:rr='jj'></gg:CFDANumber>"
	    	    	 + "   <gg:CompetitionID xml:lang='en'></gg:CompetitionID>"
	    	    	 + "</gg:GetOpportunityListRequest>"
	    		 + "\"));" +
	    		 "msg.attachFile('hb1', helloBytes, 'text/plain');" +
	    		 "msg.deliverTo('http://localhost:8080/app-s2s-server/services/ApplicantIntegrationSoapPort').asXML();"
	    		 )
	     );
	    // assertEquals("0", scr.evaluateToString(
    }
    
    public void testGrantsGovApply() throws Exception {
    	SAXReader reader = new SAXReader(DOMDocumentFactory.getInstance());
    	JSDomData data = new JSDomData(reader.read(getClass().getResourceAsStream("oppDE-PS36-04GO94006-cfda81.117.xml")), null);
    	JSDomData data2 = new JSDomData(DocumentHelper.parseText(
    			"<gais:SubmitApplicationRequest xmlns:gais='http://apply.grants.gov/WebServices/ApplicantIntegrationServices-V1.0'" +
    			" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
    			"\n\t<gais:GrantApplicationXML xsi:type='xsd:string'/>" +
    			"\n</gais:SubmitApplicationRequest>"
    		), null);
    	
    	byte att1[] = "Attachment 1 Text".getBytes();
    	byte att2[] = "Attachment 2 Text".getBytes();
    	ScriptHost scr = new ScriptHost("junit");
    	scr.defineObject("ws", new JSWebService());
    	scr.defineObject("data", data);
    	scr.defineObject("data2", data2);
    	scr.defineObject("att1", att1);
    	scr.defineObject("att2", att2);
    	
    	
    	data.setValue("*[local-name() = 'GrantSubmissionHeader']/*[local-name() = 'HashValue']",
    			data.sha1HashC14Node("*[local-name() = 'Forms']"));
    	
    	System.out.println(
    		scr.evaluateToString(
    		 "var msg = ws.createSoapMessage();" +
    		 "data.setValue(\"grant:Forms/Project:ProjectNarrativeAttachments/Project:Attachments/p:AttachedFile/glob:HashValue\"," +
    		 " ws.sha1HashBytes(att1));" +
    		 "data.setValue(\"grant:Forms/Other:OtherNarrativeAttachments/Other:Attachments/p:AttachedFile/glob:HashValue\"," +
    		 " ws.sha1HashBytes(att2));" +
    		 "data.setValue(\"*[local-name() = 'GrantSubmissionHeader']/*[local-name() = 'HashValue']\"," +
    		 " data.sha1HashC14Node(\"*[local-name() = 'Forms']\"));" +
    		 "data2.setValue('gais:GrantApplicationXML', data.getDocument().asXML());" +
    		 "var bdoc = data2.getDocument();"  +
    		 "msg.setBody(bdoc);" +
    		 "msg.attachFile('att1.txt', att1, 'text/plain');" +
    		 "msg.attachFile('att2.txt', att2, 'text/plain');" +
    		 "msg.deliverTo('http://localhost:8080/app-s2s-server/services/ApplicantIntegrationSoapPort').asXML();"
 	   	));	
    }
    
    public void testShutter()  throws Exception {
    	ScriptHost scr = new ScriptHost("junit");
    	try {
    		scr.evaluate("java.lang.System.out.println('out now');");
    		fail();
	    } catch (ScriptError se) {
	        System.out.println(se.toString());
	    }
    }
    
    public void testExecLimit()  throws Exception {
    	ScriptHost scr = new ScriptHost("junit");
    	scr.defineObject("sleeper", new Sleeper());
    	try {
	    	scr.evaluate("for (var ii=0; ii < 1000; ii++) sleeper.sleep(200);");
	    	fail();
	    } catch (Error er) {
	        System.out.println(er.toString());
	    }
    }
    
    
    public void testJSXMLHash() throws Exception {
    	//ScriptHost scr = new ScriptHost("junit");
    	SAXReader reader = new SAXReader(DOMDocumentFactory.getInstance());
    	JSDomData data =  new JSDomData(reader.read(getClass().getResourceAsStream("Hash_Test.xml")), null);
    	assertEquals(data.getValue("*[local-name() = 'GrantSubmissionHeader']/*[local-name() = 'HashValue']"),
    			data.sha1HashC14Node("*[local-name() = 'Forms']"));
    	//scr.defineObject("data", new JSDomData(reader.read(getClass().getResourceAsStream("Hash_Test.xml"))));
    }
    
    
    public void testBootJSC() throws Exception {
    	ScriptHost scr = new ScriptHost("junit");
    	assertEquals("{\"working\":1}",
    			scr.evaluateToString("JSON.stringify({ working: 1 })"));
    }
    
    public void testRecurseLimit()  throws Exception {
    	ScriptHost scr = new ScriptHost("junit");
    	scr.defineObject("sleeper", new Sleeper());
    	try {
	    	scr.evaluate("function evil() { evil(); }; evil();");
	    	fail();
	    } catch (Error er) {
	        System.out.println(er.toString());
	    }
    }
    
    class Sleeper extends ScriptableObject {
    	
    	public Sleeper() {
    		try {
                this.defineFunctionProperties(
                    new String[]{"sleep"},
                    Sleeper.class,
                    ScriptableObject.PERMANENT |
                    ScriptableObject.READONLY);
            } catch (RhinoException e) {
                e.printStackTrace();
            }
    	}
    	
    	public void sleep(int ms) {
    		try {
				java.lang.Thread.sleep(ms);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	
        public String getClassName() {
            return "Junit Sleeper";
        }
    }

}
