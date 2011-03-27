package itensil.rules;

import itensil.document.ExcelXMLProxy;
import itensil.entities.EntityLazyRecordRoot;
import itensil.entities.EntityManager;
import itensil.repository.AccessDeniedException;
import itensil.repository.LockException;
import itensil.repository.NotFoundException;
import itensil.repository.RepositoryHelper;
import itensil.security.SecurityAssociation;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.state.Activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.dom4j.dom.DOMDocumentFactory;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.NumberFunction;
import org.jaxen.function.StringFunction;
import org.jaxen.function.BooleanFunction;

public class RulesXPathFunctions {

	protected static ThreadLocal<String> recordUri = new ThreadLocal<String>();
	
	/**
	 * Includes functions:
	 * 
	 * 	if(test,X,Y)
	 * 	now()
	 * 	days-from-date(str)
	 * 	current()
	 * 	avg(node-set)
	 * 	min(node-set)
	 * 	max(node-set)
	 * 	stddev(node-set)
	 *  entity(string type, string id)
	 *  xls-ref(string uri, string ref)
	 *  xls-name(string uri, string name)
	 * 
	 * @param xpfc
	 */
	public static void initFunctionContext(XPathFunctionContext xpfc, XPHost host, Activity activity) {
		xpfc.registerFunction(null, "if", new XPFuncIf());
		xpfc.registerFunction(null, "now", new XPFuncNow());
    	xpfc.registerFunction(null, "days-from-date", new XPFuncDaysFromDate());
    	if (host != null) xpfc.registerFunction(null, "current", new XPFuncCurrent(host));
    	xpfc.registerFunction(null, "avg", new XPFuncAvg());
    	xpfc.registerFunction(null, "min", new XPFuncMin());
    	xpfc.registerFunction(null, "max", new XPFuncMax());
    	xpfc.registerFunction(null, "stddev", new XPFuncStddev());
    	xpfc.registerFunction(null, "entity", new XPFuncEntity());
    	HashMap<String, ExcelXMLProxy> xlsCache = new HashMap<String, ExcelXMLProxy>();
    	setRecordUri(null);
    	xpfc.registerFunction(null, "xls-ref", new XPFuncXLSRef(xlsCache, activity));
    	xpfc.registerFunction(null, "xls-name", new XPFuncXLSName(xlsCache, activity));
	}
	
	public static void setRecordUri(String uri) {
		recordUri.set(uri);
	}
	
	public interface XPHost {
		public Object getConextNode();
	}
	
	
	public static class XPFuncIf implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		if (args.size() < 3) {
    			throw new FunctionCallException("Function if expects ( boolean, value, value)");
    		}
    		return BooleanFunction.evaluate(args.get(0), context.getNavigator()) ? args.get(1) : args.get(2);
    	}

    }
	
    /**
     * 
     * The now function returns the current system date and time as a string value 
     * in the canonical XML Schema xsd:dateTime  normalized to UTC). If no time zone 
     * information is available, an implementation default is used.
     * 
     * 		String now()
     */
    public static class XPFuncNow implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		return ActivityXML.dateFmtZ.format(new Date());
    	}

    }
    
    /**
     * 
     * This function returns a whole number of days, according to the following rules:
     * 
     * If the string parameter represents a legal lexical xsd:date or xsd:dateTime, the return 
     * value is equal to the number of days difference between the specified date and 1970-01-01. H
     * our, minute, and second components are ignored. Any other input parameter causes a return value of NaN.
     * 
     * Examples:
     * 
     *     days-from-date("2002-01-01") returns 11688
     *     days-from-date("1969-12-31") returns -1
     * 
     * 	 number days-from-date(string)
     */
    static class XPFuncDaysFromDate implements Function {
    	public Object call(Context context, List args) throws FunctionCallException {
    		String dateStr = args.isEmpty() ?  null : StringFunction.evaluate(args.get(0), context.getNavigator());
    		if (Check.isEmpty(dateStr)) {
    			throw new FunctionCallException("Function days-from-date expects ( string )");
    		}
    		Date dt = ActivityXML.parseDate(dateStr);
    		return new Double(Math.round(dt.getTime() / 86400000));
    	}
    }
    
    
    /**
     * 
     *
     */
	public static class XPFuncCurrent implements Function {
		
		XPHost host;
		
		public XPFuncCurrent(XPHost host) {
			this.host = host;
		}
		
    	public Object call(Context context, List args) throws FunctionCallException {
    		return this.host.getConextNode();
    	}

    }
	
	
	
	public static class XPFuncAvg implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		
    		Object obj;
    		if (args.size() < 1 || !((obj = args.get(0)) instanceof List)) {
    			throw new FunctionCallException("Function avg expects ( node-set )");
            }
    		
    		Navigator nav = context.getNavigator();
    		int count = 0;
    		double sum = 0;
    		          
            Iterator nodeIter = ((List)obj).iterator();
            while (nodeIter.hasNext()) {
            	count++;
                double term = NumberFunction.evaluate(nodeIter.next(), nav).doubleValue();
                sum += term;
            }
            
            return new Double(sum / count);
    	}

    }
	
	
	
	public static class XPFuncMin implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		
    		Object obj;
    		if (args.size() < 1 || !((obj = args.get(0)) instanceof List)) {
    			throw new FunctionCallException("Function min expects ( node-set )");
            }
    		
    		Navigator nav = context.getNavigator();
    		int count = 0;
    		double min = 0;
    		          
            Iterator nodeIter = ((List)obj).iterator();
            while (nodeIter.hasNext()) {
                double term = NumberFunction.evaluate(nodeIter.next(), nav).doubleValue();
                if (term < min || count == 0) min = term;
                count++;
            }
            
            return new Double(min);
    	}

    }

	
	
	
	public static class XPFuncMax implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		
    		Object obj;
    		if (args.size() < 1 || !((obj = args.get(0)) instanceof List)) {
    			throw new FunctionCallException("Function max expects ( node-set )");
            }
    		
    		Navigator nav = context.getNavigator();
    		int count = 0;
    		double max = 0;
    		          
            Iterator nodeIter = ((List)obj).iterator();
            while (nodeIter.hasNext()) {
                double term = NumberFunction.evaluate(nodeIter.next(), nav).doubleValue();
                if (term > max || count == 0) max = term;
                count++;
            }
            
            return new Double(max);
    	}

    }
	
	
	
	public static class XPFuncStddev implements Function {
    	
    	public Object call(Context context, List args) throws FunctionCallException {
    		
    		Object obj;
    		if (args.size() < 1 || !((obj = args.get(0)) instanceof List)) {
    			throw new FunctionCallException("Function stddev expects ( node-set )");
            }
    		
    		Navigator nav = context.getNavigator();
    		int count = 0;
    		double sum = 0;
    		double sumSq = 0;
    		          
            Iterator nodeIter = ((List)obj).iterator();
            while (nodeIter.hasNext()) {
            	count++;
                double term = NumberFunction.evaluate(nodeIter.next(), nav).doubleValue();
                sum += term;
                sumSq += term * term;
            }
            double mean = sum / count;
            return new Double(Math.sqrt((sumSq / count) - mean*mean));
    	}

    }
	
    /**
     * 
     * XPath function extension, returns parents data document
     * 
     * 		NodeSet entity(string type, string id)
     */
	public static class XPFuncEntity implements Function {
		
		EntityManager entMan;
		
		public XPFuncEntity() {
			this.entMan = new EntityManager(SecurityAssociation.getUser(), DOMDocumentFactory.getInstance());
		}
		
    	public Object call(Context context, List args) throws FunctionCallException {
    		if (args.size() < 2) {
    			throw new FunctionCallException("Function entity expects ( string type, string id)");
    		}
    		String entity = StringFunction.evaluate(args.get(0), context.getNavigator());
    		String id = StringFunction.evaluate(args.get(1), context.getNavigator());
    		ArrayList<EntityLazyRecordRoot> nodes = new ArrayList<EntityLazyRecordRoot>(1);
    		nodes.add(new EntityLazyRecordRoot(entMan, entity, id));
    		return nodes;
    	}
    	
    }
	
	protected static ExcelXMLProxy getExcel(String uri, HashMap<String, ExcelXMLProxy> xlsCache) {
		ExcelXMLProxy xls = xlsCache.get(uri);
		if (xls == null) {
			try {
				xls = new ExcelXMLProxy(RepositoryHelper.loadContent(uri));
				xlsCache.put(uri, xls);
			} catch (IOException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
			} catch (AccessDeniedException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
			} catch (NotFoundException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
			} catch (LockException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
			}
		}
		return xls;
	}
	
	protected static String resolveUri(String uri, Activity activity) 
			throws NotFoundException, AccessDeniedException {
		String res;
		
		if (uri.startsWith("/")) {
			res = uri;
		} else {
			String base = recordUri.get();
			if (Check.isEmpty(base)) {
				base = activity != null ? activity.getNode().getUri() : RepositoryHelper.getPrimaryRepository().getMount();
			}
			res = UriHelper.absoluteUri(base, uri);
		}
		
		return RepositoryHelper.resolveUri(res);
	}
	
	public static class XPFuncXLSRef implements Function {
		
		HashMap<String, ExcelXMLProxy> xlsCache;
		Activity activity;
		
		public XPFuncXLSRef(HashMap<String, ExcelXMLProxy> xlsCache, Activity activity) {
			this.xlsCache = xlsCache;
			this.activity = activity;
		}
		
		public Object call(Context context, List args) throws FunctionCallException {
    		if (args.size() < 2) {
    			throw new FunctionCallException("Function xls-ref expects ( string uri, string ref)");
    		}
    		String uri = StringFunction.evaluate(args.get(0), context.getNavigator());
    		String ref = StringFunction.evaluate(args.get(1), context.getNavigator());
    		if (Check.isEmpty(uri) || Check.isEmpty(ref)) return "";
    		
    		ExcelXMLProxy xls;
			try {
				xls = getExcel(resolveUri(uri, activity), xlsCache);
			} catch (NotFoundException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
				return "";
			} catch (AccessDeniedException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
				return "";
			}
    		if (xls == null) return "";
    		
    		return xls.getCellValue(ref);
		}
	}
	
	public static class XPFuncXLSName implements Function {
		
		HashMap<String, ExcelXMLProxy> xlsCache;
		Activity activity;
		
		public XPFuncXLSName(HashMap<String, ExcelXMLProxy> xlsCache, Activity activity) {
			this.xlsCache = xlsCache;
			this.activity = activity;
		}
		
		public Object call(Context context, List args) throws FunctionCallException {
    		if (args.size() < 2) {
    			throw new FunctionCallException("Function xls-name expects ( string uri, string name)");
    		}
    		String uri = StringFunction.evaluate(args.get(0), context.getNavigator());
    		String name = StringFunction.evaluate(args.get(1), context.getNavigator());
    		
    		if (Check.isEmpty(uri) || Check.isEmpty(name)) return "";
    		
    		ExcelXMLProxy xls;
			try {
				xls = getExcel(resolveUri(uri, activity), xlsCache);
			} catch (NotFoundException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
				return "";
			} catch (AccessDeniedException e) {
				ExcelXMLProxy.logger.warn("XPath + XLS error", e);
				return "";
			}
    		if (xls == null) return "";
    		String ref = xls.getRefForName(name);
    		
    		if (Check.isEmpty(ref)) return "";
    		
    		return xls.getCellValue(ref);
		}
	}
}
