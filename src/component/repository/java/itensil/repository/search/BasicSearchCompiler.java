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
 * Created on Jan 21, 2004
 *
 */
package itensil.repository.search;


import itensil.repository.NodeProperties;
import itensil.repository.PropertyHelper;
import itensil.repository.RepositoryNode;
import itensil.util.UriHelper;
import itensil.util.Check;
import itensil.util.WildcardPattern;

import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 *
 */
public class BasicSearchCompiler {

    protected static final QName
        QNAME_DISPLAYNAME = PropertyHelper.defaultQName("displayname");

    private Operation rootOp;

    /**
     * Compile the Seach
     * @param rootClause
     * @throws SearchException
     */
    public BasicSearchCompiler(BasicSearchClause rootClause)
        throws SearchException {

        rootOp = compileClause(rootClause);
    }

    /**
     * Execute the compiled search on a node and its properties
     * @param node
     * @param props
     * @return pass test?
     */
    public boolean test(RepositoryNode node, NodeProperties props) {
        return rootOp.test(node, props);
    }

    private Operation compileClause(BasicSearchClause clause)
        throws SearchException {

        // recursive zone
        if (clause.needsSubClause()) {
            BasicSearchClause subClauses[] = clause.getSubClauses();
            if (Check.isEmpty(subClauses)) {
                throw new SearchException("Sub clause missing");
            }
            CompoundOperation op;
            switch (clause.getOp()) {
                case BasicSearchClause.OP_AND:
                    op = new AndOp(subClauses.length);
                    break;
                case BasicSearchClause.OP_OR:
                    op = new OrOp(subClauses.length);
                    break;
                case BasicSearchClause.OP_NOT:
                    op = new NotOp();
                    if (subClauses.length > 1) {
                        throw new SearchException("NOT allows only 1 clause");
                    }
                    break;
                default:
                    throw new SearchException(
                        "Unrecognized operation: " + clause.getOp());
            }
            for (BasicSearchClause subClause : subClauses) {
                op.addSubOp(compileClause(subClause));
            }
            return op;
        } else {

            switch (clause.getOp()) {
                case BasicSearchClause.OP_LT:
                    return new LtOp(clause.getProperty(), clause.getLiteral());
                case BasicSearchClause.OP_LTE:
                    return new LteOp(clause.getProperty(), clause.getLiteral());
                case BasicSearchClause.OP_GT:
                    return new GtOp(clause.getProperty(), clause.getLiteral());
                case BasicSearchClause.OP_GTE:
                    return new GteOp(clause.getProperty(),clause.getLiteral());
                case BasicSearchClause.OP_EQ:
                    return new EqOp(
                        clause.getProperty(),
                        clause.getLiteral(),
                        clause.isCaseSensitive());
                case BasicSearchClause.OP_IS_COLLECTION:
                    return new IsCollectionOp();
                case BasicSearchClause.OP_IS_DEFINED:
                    return new IsDefinedOp(clause.getProperty());
                case BasicSearchClause.OP_LIKE:
                    return new LikeOp(
                        clause.getProperty(),
                        clause.getLiteral(),
                        clause.isCaseSensitive());
                default:
                    throw new SearchException(
                        "Unrecognized operation: " + clause.getOp());
            }
        }
    }


    static String getValue(
        QName name, RepositoryNode node, NodeProperties props) {

        if (QNAME_DISPLAYNAME.equals(name)) {
            return UriHelper.name(node.getUri());
        } else {
            return props.getValue(name);
        }
    }

    static interface Operation {

        boolean test(RepositoryNode node, NodeProperties props);
    }

    static abstract class CompoundOperation implements Operation {

        Operation subOps[];
        int opCount = 0;

        void addSubOp(Operation subOp) {
            subOps[opCount++] = subOp;
        }

        public abstract boolean test(RepositoryNode node, NodeProperties props);

    }

    static class AndOp extends CompoundOperation {

        AndOp(int subSize) {
            subOps = new Operation[subSize];
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            for (int i=0; i <  opCount; i++) {
                if (!subOps[i].test(node, props)) return false;
            }
            return true;
        }
    }

    static class OrOp extends CompoundOperation {

        OrOp(int subSize) {
            subOps = new Operation[subSize];
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            for (int i=0; i <  opCount; i++) {
                if (subOps[i].test(node, props)) return true;
            }
            return false;
        }
    }

    static class NotOp extends CompoundOperation  {

        Operation subOp;

        NotOp() {}

        void addSubOp(Operation subOp) {
            this.subOp = subOp;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            return !subOp.test(node, props);
        }
    }

    static class LtOp implements Operation {

        String literal;
        QName property;

        LtOp (QName property, String literal) {
            this.property = property;
            this.literal = literal;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            return val != null && literal.compareTo(val) > 0;
        }
    }

    static class LteOp implements Operation {

        String literal;
        QName property;

        LteOp (QName property, String literal) {
            this.property = property;
            this.literal = literal;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            return val != null && literal.compareTo(val) >= 0;
        }
    }

    static class GtOp implements Operation {

        String literal;
        QName property;

        GtOp (QName property, String literal) {
            this.property = property;
            this.literal = literal;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            return val != null && literal.compareTo(val) < 0;
        }
    }

    static class GteOp implements Operation {

        String literal;
        QName property;

        GteOp (QName property, String literal) {
            this.property = property;
            this.literal = literal;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            return val != null && literal.compareTo(val) <= 0;
        }
    }

    static class EqOp implements Operation {

        String literal;
        QName property;
        boolean caseSensitive;

        EqOp (QName property, String literal, boolean caseSensitive) {
            this.property = property;
            this.literal = literal;
            this.caseSensitive = caseSensitive;
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            if (caseSensitive) {
                return val != null && literal.equals(val);
            } else {
                return val != null && literal.equalsIgnoreCase(val);
            }
        }
    }

    static class IsCollectionOp implements Operation {

        IsCollectionOp() {}

        public boolean test(RepositoryNode node, NodeProperties props) {
            return node.isCollection();
        }
    }

    static class IsDefinedOp implements Operation {

        QName property;

        IsDefinedOp (QName property) {
            this.property = property;
        }

         public boolean test(RepositoryNode node, NodeProperties props) {
                if (QNAME_DISPLAYNAME.equals(property)) {
                    return true;
                }
                return props.getPropertyMap().containsKey(property);
         }
    }

    static class LikeOp implements Operation {

        WildcardPattern pat;
        QName property;

        LikeOp (QName property, String literal, boolean caseSensitive) {
            this.property = property;
            pat = new WildcardPattern(literal, '%', '_', caseSensitive);
        }

        public boolean test(RepositoryNode node, NodeProperties props) {
            String val = getValue(property, node, props);
            return val != null && pat.match(val);
        }
    }

}
