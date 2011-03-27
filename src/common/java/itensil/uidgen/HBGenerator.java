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
package itensil.uidgen;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class HBGenerator implements IdentifierGenerator {

    private static IUIDGenerator txGen = new IUIDGenerator();

    public HBGenerator() {        
    }

    public Serializable generate(SessionImplementor sessionImplementor, Object object) throws HibernateException {
        return txGen.createID().toString();
    }
}
