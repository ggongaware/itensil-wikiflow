package itensil.scripting;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;

import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.Locator;
// import com.hp.hpl.jena.util.TypedStream;
import org.apache.log4j.Logger;
import com.hp.hpl.jena.util.ModelLoader;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.*;
import ch.uzh.ifi.sparqlml.arq.create.query.*;
import itensil.security.*;
import itensil.io.HibernateUtil;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.PasswordGen;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.USpaceUserEntity;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.util.Check;
import itensil.util.LocaleHelper;

public class JSQuery extends ScriptableObject{
	
	private final JSFiles files;

	public JSQuery(JSFiles files) {		
	    this.files = files;

	    String funcs[] = {
				"sparql"
		};
		
	    try {
	        this.defineFunctionProperties(
	            funcs,
	            JSQuery.class,
	            ScriptableObject.PERMANENT |
	            ScriptableObject.READONLY);
	    } catch (RhinoException e) {
	        e.printStackTrace();
	    }
	    
	    sealObject();		
	}



	@Override
	public String getClassName() {
		// TODO Auto-generated method stub
		return "JSQuery";
	}
	
	public void sparql (String query, String input, String output) throws
	Exception
	{	
/*
		List<String> urilist = new Vector<String>();
	    List<String> emptylist = new Vector<String>();
	    
	    java.net.URL inputURL;
	    try {
	    	inputURL = new java.net.URL(input);
	    } catch (java.net.MalformedURLException e) {
//	    	if (java.util.regex.Pattern.compile("^/").matcher(input).find()) {
    		input = ((UserEntity)itensil.security.SecurityAssociation.getUser()).getUserSpace().getBaseUrl() + input + "?j_signon_token=" + ((UserEntity)itensil.security.SecurityAssociation.getUser()).getToken();
//	    	} else if (input =~ ^.) {...}
//	    	}
	    }
	    // urilist.add(input+((UserEntity)itensil.security.SecurityAssociation.getUser()).getToken());
	    urilist.add(input);
	    // urilist.add("http://www.w3.org/2000/10/rdf-tests/Miscellaneous/animals.rdf");
	    // urilist.add(inputURL.toString());// "http://www.w3.org/2000/10/rdf-tests/RSS_1.0/rss_5.3_1.rdf");
	    
	    Dataset dset = DatasetFactory.create(urilist, urilist);
	    
		QueryML qry = QueryFactoryML.create(query);
		
		// FileManager fm = new FileManager();
		// ItensilLoc loc = new ItensilLoc(files);
		
		// fm.addLocator(loc);
		
		// Model model = ModelLoader.loadModel(input, ModelLoader.guessLang(input));
		// ModelMaker mmaker = ModelFactory.createMemModelMaker();
		// Model model = mmaker.getModel(input);
		QueryExecutionML qryExe = QueryExecutionFactoryML.create(qry, dset);
*/
		QueryExecutionML qryExe = QueryExecutionFactoryML.create(query);

		Model model1 = qryExe.execConstruct();

		if (files.exists(output)) files.move(output, new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'").format(new Date()) + ' ' + output);
		files.createFile(output);
		
		ByteArrayOutputStream outStr = new ByteArrayOutputStream();
		
		model1.write(outStr);
		
		files.saveBytes(output, outStr.toByteArray());		
	}
}
