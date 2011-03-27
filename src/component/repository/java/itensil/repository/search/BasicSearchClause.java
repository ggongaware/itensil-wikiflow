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

import javax.xml.namespace.QName;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface BasicSearchClause {

    /**
     * Literal Types
     */
    public static int TYPE_STRING   = 0; //Dates should be string type friendly
    public static int TYPE_INTEGER  = 1;
    public static int TYPE_DECIMAL  = 2;

    /**
     * BasicSearch operations
     */
    public static int OP_AND            = 0;
    public static int OP_OR             = 1;
    public static int OP_NOT            = 2;
    public static int OP_LT             = 3;
    public static int OP_LTE            = 4;
    public static int OP_GT             = 5;
    public static int OP_GTE            = 6;
    public static int OP_EQ             = 7;
    public static int OP_IS_COLLECTION  = 8;
    public static int OP_IS_DEFINED     = 9;
    public static int OP_LIKE           = 10;

    /**
     * These operations have sub clauses
     */
    public static int SUB_OPS[] = {OP_AND, OP_OR, OP_NOT};

    /**
     * length = 0 for empty
     * @return  null for non-sub clause Operation
     */
    public BasicSearchClause[] getSubClauses();

    /**
     * @return The property in the clause
     */
    public QName getProperty();

    /**
     * @return the variable type of the literal - TYPE_XXXX
     */
    public int getLiteralType();

    /**
     *
     * @return null if composed of sub clause
     */
    public String getLiteral();

    /**
     *
     * @return should be from OP_XXX list
     */
    public int getOp();

    /**
     *
     * @return is this operation case sensitive?
     */
    public boolean isCaseSensitive();

    /**
     * @return Does this operation use sub clauses?
     */
    public boolean needsSubClause();
}
