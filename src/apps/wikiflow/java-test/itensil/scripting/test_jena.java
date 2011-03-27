package itensil.scripting;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.*;


import java.io.*;

/*
class ItensilLoc implements Locator {
	public String getName()  { return "ItensilLoc"; }
	
	public TypedStream open(String arg) 
	{
		File f = new File("test_jena.output");
		FileInputStream str;
		
		try {
		  str = new FileInputStream(f);
		} catch (FileNotFoundException e)
		{
		  str = null;
		  System.out.println("File not found");
		}
		TypedStream ret = new TypedStream(str);
		
		return ret;
	}
}

*/

public class test_jena {
	public static void main(String[] args) throws
	IOException, FileNotFoundException
	{	
		Query qry = QueryFactory.create("prefix rss:<http://purl.org/rss/1.0/> CONSTRUCT { ?chan rss:title ?name } from <http://www.w3.org/2000/10/rdf-tests/RSS_1.0/rss_5.3_1.rdf> where { ?chan rss:title ?name. }");
		
		QueryExecution qryExe = QueryExecutionFactory.create(qry);
		
		Model model = qryExe.execConstruct();
		
		File file = new File("test_jena.output");
		
		FileOutputStream out;
		
		try {
		  out = new FileOutputStream(file);
		}
		finally {}
		
		model.write(out);
		
		out.close();
		
		FileManager fm = new FileManager();
		//ItensilLoc loc = new ItensilLoc();
		
		//fm.addLocator(loc);
		
		Query qry1 = QueryFactory.create("prefix rss:<http://purl.org/rss/1.0/> CONSTRUCT { ?chan rss:title ?name } from <http://www.itensil/2000/10/rdf-tests/RSS_1.0/rss_5.3_1.rdf> where { ?chan rss:title ?name. }");
		
		QueryExecution qryExe1 = QueryExecutionFactory.create(qry1, fm);
		
		Model model1 = qryExe1.execConstruct();
		
		File file1 = new File("test_jena_1.output");
		
		FileOutputStream out1;
		
		try {
		  out1 = new FileOutputStream(file1);
		}
		finally {}
		
		model1.write(out1);
		
		out1.close();
	}
}