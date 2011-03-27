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
/*
 * Created on Jan 29, 2004
 *
 */
package itensil.repository.search;

import java.util.HashMap;
import itensil.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import itensil.io.xml.SAXHandlerPlus;

import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 *
 */
public class BasicSearchRequestReader extends SAXHandlerPlus {

    private DefaultBasicSearch search;
    private DefaultBasicSearchOrderBy lastOrderBy;
    private Stack<DefaultBasicSearchClause> clauseStack;

    private static HashMap<String,Integer> opMap;

    static {
        opMap = new HashMap<String,Integer>();
        opMap.put("and", BasicSearchClause.OP_AND);
        opMap.put("or", BasicSearchClause.OP_OR);
        opMap.put("not", BasicSearchClause.OP_NOT);
        opMap.put("lt", BasicSearchClause.OP_LT);
        opMap.put("lte", BasicSearchClause.OP_LTE);
        opMap.put("gt", BasicSearchClause.OP_GT);
        opMap.put("gte", BasicSearchClause.OP_GTE);
        opMap.put("eq", BasicSearchClause.OP_EQ);
        opMap.put("is-collection", BasicSearchClause.OP_IS_COLLECTION);
        opMap.put("is-defined", BasicSearchClause.OP_IS_DEFINED);
        opMap.put("like", BasicSearchClause.OP_LIKE);
    }

    public BasicSearchRequestReader() {
        super(null);
        search = new DefaultBasicSearch(
            "", DefaultBasicSearch.INFINITE_DEPTH, false);
        clauseStack = new Stack<DefaultBasicSearchClause>();
    }

    public void endElement(
        String namespaceUri,
        String sName, // simple name
        String qName // qualified name
        ) throws SAXException {

        super.endElement(namespaceUri, sName, qName);
        ElementInfo element = getLastElement();
        String path = element.path;

        if (path.startsWith(        "searchrequest/basicsearch/select/")) {
            if ("allprop".equals(element.sName)) {
                search.setSelectAllProperties();
            } else if (path.indexOf("prop/") > 0){
                search.addSelectProperty(
                    new QName(element.nameSpace, element.sName));
            }
        } else if (path.startsWith( "searchrequest/basicsearch/from/scope/")) {
            if ("href".equals(element.sName)) {
                search.setScopeUri(element.value);
            } else if ("depth".equals(element.sName)){
                if (!"infinity".equalsIgnoreCase(element.value)) {
                    search.setScopeDepth(Integer.parseInt(element.value));
                }
            }
        } else if (path.startsWith( "searchrequest/basicsearch/where/")) {
            if (path.indexOf("prop/") > 0) {
                DefaultBasicSearchClause clause = clauseStack.peek();
                clause.setProperty(
                    new QName(element.nameSpace, element.sName));
            } else if ("literal".equals(element.sName)) {
                DefaultBasicSearchClause clause = clauseStack.peek();
                clause.setLiteral(element.value);
            } else if (opMap.containsKey(sName)) {
                DefaultBasicSearchClause clause = clauseStack.pop();
                if (clauseStack.size() > 0) {
                    DefaultBasicSearchClause parent = clauseStack.peek();
                    parent.addSubClause(clause);
                }
            }
        } else if (path.startsWith( "searchrequest/basicsearch/orderby/order/")) {
            if (path.indexOf("prop/") > 0) {
                lastOrderBy = new DefaultBasicSearchOrderBy(
                    new QName(element.nameSpace, element.sName));
                search.addOrderBy(lastOrderBy);
            } else if ("descending".equals(element.sName)) {
                lastOrderBy.setDescending(true);
            }
        } else if (path.startsWith( "searchrequest/basicsearch/limit/nresults")) {
            search.setLimit(Integer.parseInt(element.value));
        }
    }

    /**
     * @return search object
     */
    public DefaultBasicSearch getSearch() {
        return search;
    }

    /*
     * @see org.xml.sax.ContentHandler#startElement(
     *  String, String, String, org.xml.sax.Attributes)
     */
    public void startElement(
        String namespaceUri,
        String sName,
        String qName,
        Attributes attrs)
        throws SAXException {

        super.startElement(namespaceUri, sName, qName, attrs);
        if ("DAV:".equals(namespaceUri)) {
            Integer op;
            if ((op = opMap.get(sName)) != null) {
                boolean caseSensitive = false;
                if ("no".equalsIgnoreCase(attrs.getValue("caseless"))) {
                    caseSensitive = true;
                }
                DefaultBasicSearchClause clause =
                    new DefaultBasicSearchClause(op, null, null, caseSensitive);
                clauseStack.push(clause);
                if (clauseStack.size() == 1) {
                    search.setWhereClause(clause);
                }
            }
        }
    }

}
