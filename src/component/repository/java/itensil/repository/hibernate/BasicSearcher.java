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
package itensil.repository.hibernate;

import itensil.repository.search.DefaultBasicSearchResultSet;
import itensil.repository.search.BasicSearch;
import itensil.repository.search.SearchException;
import itensil.repository.search.BasicSearchClause;
import itensil.repository.PropertyHelper;
import itensil.io.HibernateUtil;
import itensil.util.Check;

import java.util.List;
import java.util.ArrayList;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;

import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 *
 */
public class BasicSearcher {

    protected static final QName
        QNAME_DISPLAYNAME = PropertyHelper.defaultQName("displayname");

    @SuppressWarnings("unchecked")
	public static DefaultBasicSearchResultSet doSearch(RepositoryEntity repo, BasicSearch query)
            throws SearchException {

        Session session = HibernateUtil.getSession();
        Criteria crit = session.createCriteria(NodeEntity.class);
        crit.add(Restrictions.eq("repoEntity", repo));
        crit.add(Restrictions.eq("deleted", 0L));
        if (query.getScopeDepth() != 0) {
            if (!Check.isEmpty(query.getScopeUri())) {
                String sUri = query.getScopeUri();
                if (!sUri.endsWith("%")) {
                    sUri += "/%";
                }
                crit.add(Restrictions.like("localUri",repo.localizeUri(sUri)));
            }
        } else {
            crit.add(Restrictions.eq("localUri", repo.localizeUri(query.getScopeUri())));
        }

        Criterion where = convertCriteria(crit, query.getWhereClause(), "wa");
        if (where != null) {
            crit.add(where);
        }
        crit.addOrder(Order.asc("localUri"));

        List<NodeEntity> nodes = crit.list();
        List<DefaultBasicSearchResultSet.Entry> entryList =
                new ArrayList<DefaultBasicSearchResultSet.Entry>(nodes.size());
        for (NodeEntity node : nodes) {
            if (!query.getScopeIncludeVersions()) {
                VersionEntity def = node.getDefaultVersionEnt();
                if (def == null) def = new VersionEntity();
                entryList.add(new DefaultBasicSearchResultSet.Entry(node, def));
            } else {
                //Criteria verCrit = session.createCriteria(VersionEntity.class);
                throw new SearchException("Version search not yet supported.");
            }
        }
        return  new DefaultBasicSearchResultSet(entryList);
    }

    protected static Criterion convertCriteria(Criteria crit, BasicSearchClause clause, String alias) {
        if (clause.needsSubClause()) {
            switch (clause.getOp()) {
                case BasicSearchClause.OP_AND:
                    Junction conj = Restrictions.conjunction();
                    boolean empty = true;
                    int acount = 0;
                    for (BasicSearchClause subClause : clause.getSubClauses()) {
                        Criterion subCrit = convertCriteria(crit, subClause, alias + "a" + acount);
                        acount++;
                        if (subCrit != null) {
                            empty = false;
                            conj.add(subCrit);
                        }
                    }
                    return empty ? null : conj;

                case BasicSearchClause.OP_NOT:
                    Criterion subCrit = convertCriteria(crit, clause.getSubClauses()[0], alias + "n");
                    if (subCrit != null) {
                        return Restrictions.not(subCrit);
                    }
                    return null;

                default: // probably OR
                    return null;
            }
        }
        if (clause.getOp() == BasicSearchClause.OP_IS_COLLECTION) {
            return Restrictions.eq("collection", true);
        }
        String argName;
        Criterion nameCrit = null;
        if (QNAME_DISPLAYNAME.equals(clause.getProperty())) {
            if (clause.getOp() == BasicSearchClause.OP_IS_DEFINED) return null;
            argName = "localUri";
        } else {

            /* This has bugged in hibernate

            crit.createAlias("versionEntities", alias);
            crit.createAlias(alias + ".propertyVals", alias + "pv");
            //crit.createAlias(alias + "p.name", alias + "nm");
            argName = alias + "pv.value";


            crit.createAlias("defaultVersion.directProps", alias);
            crit.createAlias("defaultVersion.directProps.name", alias + "nm");

            //crit.createAlias(alias + "dp.name", alias + "nm");
            argName = alias + ".value";
            nameCrit = Restrictions.eq(alias + "nm.localName", clause.getProperty().getLocalPart());
            */
            return null;
        }
        Criterion valCrit;
        switch (clause.getOp()) {
            case BasicSearchClause.OP_IS_DEFINED:
                return nameCrit;
            case BasicSearchClause.OP_EQ:
                valCrit = Restrictions.eq(argName, clause.getLiteral());
                break;
            case BasicSearchClause.OP_GT:
                valCrit = Restrictions.gt(argName, clause.getLiteral());
                break;
            case BasicSearchClause.OP_GTE:
                valCrit = Restrictions.ge(argName, clause.getLiteral());
                break;
            case BasicSearchClause.OP_LT:
                valCrit = Restrictions.lt(argName, clause.getLiteral());
                break;
            case BasicSearchClause.OP_LTE:
                valCrit = Restrictions.le(argName, clause.getLiteral());
                break;
            case BasicSearchClause.OP_LIKE:
                valCrit = Restrictions.like(argName, clause.getLiteral());
                break;
            default:
                return null;
        }
        return nameCrit == null ? valCrit : Restrictions.and(nameCrit, valCrit);
    }

}
