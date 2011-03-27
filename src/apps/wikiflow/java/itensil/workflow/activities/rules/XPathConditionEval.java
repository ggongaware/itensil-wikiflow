/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.workflow.activities.rules;

import itensil.workflow.model.element.Condition;
import itensil.workflow.model.element.Ruleset;
import itensil.workflow.model.element.Switch;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.AppElement;

import itensil.workflow.FlowEvent;
import itensil.workflow.rules.ConditionEval;
import itensil.workflow.rules.EvalException;
import itensil.util.Check;
import itensil.util.UriHelper;
import itensil.entities.EntityLazyRecordRoot;
import itensil.entities.EntityManager;
import itensil.entities.hibernate.EntityActivity;
import itensil.io.xml.XMLDocument;
import itensil.io.xml.XMLWriter;
import itensil.repository.*;
import itensil.rules.RulesEvaluator;
import itensil.rules.RulesXPathFunctions;
import itensil.security.SecurityAssociation;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.XPath;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.Navigator;
import org.jaxen.SimpleVariableContext;
import org.jaxen.UnsupportedAxisException;
import org.jaxen.XPathFunctionContext;
import org.jaxen.function.BooleanFunction;
import org.jaxen.function.StringFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import itensil.workflow.activities.ActivityXML;
import itensil.workflow.activities.state.Activity;

/**
 * @author ggongaware@itensil.com
 *
 */
public class XPathConditionEval<Dt> implements ConditionEval<Activity, Dt>, RulesXPathFunctions.XPHost {

    protected FlowModel model;
    protected Object contextNode;

    protected static String ITENSIL_WF_NS = "http://itensil.com/workflow";

    protected static Logger logger = Logger.getLogger(XPathConditionEval.class);

    public XPathConditionEval(FlowModel model) {
        this.model = model;
    }

    public boolean isAsync() {
        return false;
    }

    public String evalExclusive(String flowId, Condition[] conditions, FlowEvent<Activity, Dt> evt) throws EvalException {
        int last = conditions.length - 1;
        boolean allEmpty = true;
        for (int ii = 0; ii < last; ii++) {
            Condition cond = conditions[ii];
            String exp = cond.getInnerText();
            if (exp != null) exp = exp.trim();
            if (!Check.isEmpty(exp)) {
            	allEmpty = false;
            	if (evalCondition(cond, evt.getToken())) return cond.getReturnId();
            }
        }
        
        if (allEmpty) return null;
        
        // else
        Condition ec = conditions[last];
        return ec == null ? null : ec.getReturnId();
    }

    protected boolean evalCondition(Condition cond, Activity activity) throws EvalException {
	    Document datDoc = getDefaultData(activity);
	    if (cond.getParent().getParent() instanceof Switch) {
		    Switch sw = (Switch)cond.getParent().getParent();
		    Ruleset rs = sw.selectOneChild(Ruleset.class);
		    
		    // Rule set mode
		    if (rs != null && !Check.isEmpty(rs.getAttribute("src"))) {
		    	RulesEvaluator rulEval = (RulesEvaluator)rs.getRuleObj();
		    	if (rulEval == null) {
		    		Document rdoc = getRuleDoc(rs.getAttribute("src"), activity);
		    		rulEval = new RulesEvaluator(rdoc);
		    		rs.setRuleObj(rulEval);
		    	}
		    	
		    	String res;
		    	String subRule = rs.getAttribute("rule");
		    	if (Check.isEmpty(subRule)) {
		    		res = rulEval.match(datDoc.getRootElement());
		    	} else {
		    		res = rulEval.match(subRule, datDoc.getRootElement());
		    	}
		    	
		    	return res != null && res.equals(cond.getInnerText());
		    }
	    }
	    
	    XPath xp = getXPath(cond.getInnerText(), datDoc, activity);
	    this.contextNode = datDoc.getRootElement();
	    return xp.booleanValueOf(this.contextNode);
    }

	protected XPath getXPath(String exp, Document datDoc, Activity activity) {
    	XPath xp = datDoc.createXPath(exp);
    	XPathFunctionContext xpfc = new XPathFunctionContext(false);
    	xpfc.registerFunction(null, "sub-activities", new XPFuncSubActivities(activity));
    	xpfc.registerFunction(null, "is-sub", new XPFuncIsSub(activity));
    	xpfc.registerFunction(null, "parent", new XPFuncParent(activity));
    	RulesXPathFunctions.initFunctionContext(xpfc, this, activity);
    	xp.setFunctionContext(xpfc);
    	
    	// Entity variables here...
    	EntityManager entMan = new EntityManager(SecurityAssociation.getUser());
    	List<EntityActivity> entRecList = entMan.recordsAllInActivity(activity);
    	if (!entRecList.isEmpty()) {
	    	String entityId = null;
	    	String entName = null;
	    	
	    	SimpleVariableContext svc = new SimpleVariableContext();
	    	xp.setVariableContext(svc);
	    	HashMap<String, ArrayList<EntityLazyRecordRoot>> entMap = new HashMap<String, ArrayList<EntityLazyRecordRoot>>();
	    	for (EntityActivity entAct : entMan.recordsAllInActivity(activity)) {
	    		try {
		    		if (!entAct.getEntityId().equals(entityId)) {
		    			entityId = entAct.getEntityId();
		    			entName = UriHelper.name(RepositoryHelper.getNodeById(entityId, false).getUri());
		    		}
		    		String relName = XMLWriter.nmTokenFilter(entAct.getName());
		    		ArrayList<EntityLazyRecordRoot> ls = entMap.get(relName);
		    		if (ls == null) {
		    			ls = new ArrayList<EntityLazyRecordRoot>();
		    			entMap.put(relName, ls);
		    		}
		    		ls.add(new EntityLazyRecordRoot(entMan, entName, String.valueOf(entAct.getRecordId()), relName));
	    		
	    		} catch (Exception ex) {
	    			logger.warn("Entity rules problem", ex);
	    		}
	    	}
	    	
	    	for (Map.Entry<String, ArrayList<EntityLazyRecordRoot>> nmList : entMap.entrySet()) {
	    		ArrayList ls = nmList.getValue();
	    		svc.setVariableValue(nmList.getKey(), ls);
	    	}
    	}
    	
    	return xp;
    }

    public String[] evalInclusive(String flowId, Condition[] conditions, FlowEvent<Activity, Dt> evt) throws EvalException {
        ArrayList<String> rets = new ArrayList<String>();
        for (Condition cond : conditions) {
            if (cond != null && evalCondition(cond, evt.getToken())) rets.add(cond.getReturnId());
        }
        return rets.toArray(new String[rets.size()]);
    }

    /**
     * Will create or select the element in the pathExpr, set to the XPath in valExpr
     *
     * @param activity
     * @param pathExpr
     * @param valExpr
     * @throws EvalException
     */
    public void setDataValueExpr(Activity activity, String pathExpr, String valExpr) 
    	throws EvalException {
    	
    	Document datDoc = getDefaultData(activity);
    	if (pathExpr.startsWith("parent()/")) {
    		pathExpr = pathExpr.substring(9);
    		Activity parAct = activity.getParent();
    		if (parAct != null) {
    			Document parDatDoc = getDefaultData(parAct);
    			setDataValueExpr(activity, parDatDoc, datDoc, pathExpr, valExpr);
    			saveDefaultData(parAct, parDatDoc);
    		}
    	}
    	setDataValueExpr(activity, datDoc, datDoc, pathExpr, valExpr);
    	saveDefaultData(activity, datDoc);
    }
    
    /**
     * Will create or selet the elemen in the pathExpr, set to the XPath in valExpr
     *
     * @param activity
     * @param pathExpr
     * @param valExpr
     * @throws EvalException
     */
    public void setDataValueExpr(Activity activity, Document datDoc, Document valDoc, String pathExpr, String valExpr) 
    	throws EvalException {

        
        Element root = datDoc.getRootElement();
        // Element created if it did not exist
        Element elem = DocumentHelper.makeElement(root, pathExpr);
        XPath xp = getXPath(valExpr, valDoc, activity);
        // set it
        elem.setText(xp.valueOf(root)); 
    }
    
    public void setDataValues(Activity activity, Document datDoc, Map<String, String> values) {
    	Element root = datDoc.getRootElement();
    	for (Entry<String, String> ent : values.entrySet()) {
    		Element elem = DocumentHelper.makeElement(root, ent.getKey());
    		elem.setText(ent.getValue());
    	}
    }
    
    
    public void saveDefaultData(Activity activity, Document datDoc) 
    	throws EvalException {
    	
    	// save it
    	// WARNING: Potential error setting parent doc with non-standard data src name
        Collection<AppElement> elems = model.matchAppElements(ITENSIL_WF_NS, "data");
        if (elems.isEmpty()) throw new EvalException("Missing default data source");
        AppElement datElem = elems.iterator().next();
        try {
            String dataUri = UriHelper.absoluteUri(activity.getNode().getUri(), datElem.getAttribute("src"));
            MutableRepositoryNode file = RepositoryHelper.getNode(dataUri, true);
            byte buf[] = datDoc.asXML().getBytes("UTF-8");
            RepositoryHelper.updateContent(file, buf);
        } catch (Exception e) {
            throw new EvalException("Data source error", e);
        }
    }
    

    public Document getDefaultData(Activity activity) throws EvalException {
        return getDefaultData(activity, model);
    }
    
    public static Document getDefaultData(Activity activity, FlowModel model) throws EvalException {
    	 Document ddoc = activity.getDataCache();
    	 if (ddoc == null) {
	    	 Collection<AppElement> elems = model.matchAppElements(ITENSIL_WF_NS, "data");
	         if (elems.isEmpty()) {
	         	// throw new EvalException("Missing default data source");
	         	// supply an empty doc
	        	ddoc = DocumentHelper.createDocument();
	        	ddoc.addElement("data");
	         } else {
		            AppElement datElem = elems.iterator().next();
		            try {
		                String dataUri = UriHelper.absoluteUri(activity.getNode().getUri(), datElem.getAttribute("src"));
		                ddoc = XMLDocument.readStream(RepositoryHelper.loadContent(dataUri));
		            } catch (Exception e) {
		                throw new EvalException("Data source error", e);
		            }
	         }
	         activity.setDataCache(ddoc);
    	 }
         return ddoc;
    }
    
    protected Document getRuleDoc(String src, Activity activity) throws EvalException {
    	String uri = src;
    	try {
	    	if (!uri.startsWith("/")) {
	    		uri = UriHelper.absoluteUri(activity.getFlow().getNode().getUri(), src);
	    	}
			return XMLDocument.readStream(RepositoryHelper.loadContent(uri));
			
    	} catch (Exception ex) {
            throw new EvalException("Rule source error", ex);
        }
	}
    
	public Object getConextNode() {
		return contextNode;
	}
    
    /**
     * 
     * XPath function extension, returns the count of the sub activities (children)
     * 
     * 		number sub-activities(boolean?)
     * 
     *  Pass true to include ended sub-activities
     */
    static class XPFuncSubActivities implements Function {
    	
    	Activity act;
    	
    	XPFuncSubActivities(Activity act) {
    		this.act = act;
    	}
    	
		public Object call(Context context, List args) throws FunctionCallException {
			int count;
			// include inactive kids ?
			if (args.isEmpty() || !BooleanFunction.evaluate(args.get(0), context.getNavigator())) {
				count = act.getActiveChildren().size();
			} else {
				count = act.getChildren().size();
			}
			return new Double(count);
		}
    }
    
    /**
     * 
     * XPath function extension, returns if the current acivity is a sub activity
     * 
     * 		boolean is-sub()
     */
    static class XPFuncIsSub implements Function {
    	
    	Activity act;
    	
    	XPFuncIsSub(Activity act) {
    		this.act = act;
    	}
    	
		public Object call(Context context, List args) throws FunctionCallException {
			return new Boolean(act.getParent() != null);
		} 
    }
    
    /**
     * 
     * XPath function extension, returns parents data document
     * 
     * 		NodeSet parent()
     */
    static class XPFuncParent implements Function {
    	
    	Activity act;
    	Object parentRoot;
    	
    	XPFuncParent(Activity act) {
    		this.act = act;
    	}
    	
		public Object call(Context context, List args) throws FunctionCallException {
			if (parentRoot == null) {
				Activity parAct = act.getParent();
				if (parAct != null) {
					try {
						// TODO make rules.xml a standard or something
						String dataUri = UriHelper.absoluteUri(parAct.getNode().getUri(), "rules.xml");
						Document parentDoc = XMLDocument.readStream(RepositoryHelper.loadContent(dataUri));
						parentRoot = parentDoc.getRootElement();
					} catch (Exception ex) {
						logger.warn("XPath parent() error", ex);
					}
				} else { // return self
					Navigator nav = context.getNavigator();
					try {
						Object doc = nav.getDocumentNode(context.getNodeSet().get(0));
						parentRoot = nav.getChildAxisIterator(doc).next();
					} catch (Exception ex) {
						logger.warn("XPath parent() error", ex);
					}
				}
			}
			return parentRoot;
		} 
    }

}
