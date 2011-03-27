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
 * Created on Jan 20, 2004
 *
 */
package itensil.repository.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultBasicSearchClause
    implements BasicSearchClause, Serializable {

    static final long serialVersionUID = 1079554220539L;

    private int op;
    private QName property;
    private String literal;
    private int literalType;

    private boolean caseSensitive;
    private List<BasicSearchClause> subClauses;

    /**
     * Defaults to case insensitive String type
     * @param op
     * @param property
     * @param literal
     */
    public DefaultBasicSearchClause(
        int op,
        QName property,
        String literal) {

        this(op, property, literal, false);
    }

    public DefaultBasicSearchClause(
        int op,
        QName property,
        String literal,
        boolean caseSensitive) {

        this(op, property, literal, TYPE_STRING, caseSensitive);
    }

    /**
     * @param op
     * @param property
     * @param literal
     * @param literalType
     * @param caseSensitive
     */
    public DefaultBasicSearchClause(
        int op,
        QName property,
        String literal,
        int literalType,
        boolean caseSensitive
        ) {

        this.op = op;
        this.property = property;
        this.literal = literal;
        this.literalType = literalType;
        this.caseSensitive = caseSensitive;
        if (needsSubClause()) {
           subClauses = new ArrayList<BasicSearchClause>();
        }
    }

    public boolean needsSubClause() {
        int op = getOp();
        for (int aSUB_OPS : SUB_OPS) {
            if (aSUB_OPS == op) return true;
        }
        return false;
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#getSubClauses()
     */
    public BasicSearchClause[] getSubClauses() {
        if (subClauses == null) {
            return null;
        }
        return subClauses.toArray(new BasicSearchClause[subClauses.size()]);
    }

    public void addSubClause(BasicSearchClause clause) {
        if (subClauses != null) {
            subClauses.add(clause);
        }
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#getProperty()
     */
    public QName getProperty() {
        return property;
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#getLiteralType()
     */
    public int getLiteralType() {
        return literalType;
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#getLiteral()
     */
    public String getLiteral() {
        return literal;
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#getOp()
     */
    public int getOp() {
        return op;
    }

    /*
     * @see itensil.repository.search.BasicSearchClause#isCaseSensitive()
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * @param prop
     */
    public void setProperty(QName prop) {
        property = prop;
    }

    /**
     * @param s
     */
    public void setLiteral(String s) {
        literal = s;
    }

}
