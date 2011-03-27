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
package itensil.io;

import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.naming.Context;

/**
 * Basic Hibernate helper class, handles SessionFactory, Session and Transaction.
 * <p>
 * Uses a static initializer for the initial SessionFactory creation
 * and holds Session and Transactions in thread local variables. All
 * exceptions are wrapped in an unchecked InfrastructureException.
 *
 * Original author: christian@hibernate.org
 */
public class HibernateUtil {

    private static Logger log =  Logger.getLogger(HibernateUtil.class);

    private static SessionFactory sessionFactory;
    private static Configuration configuration;
    private static final ThreadLocal<Session> threadSession = new ThreadLocal<Session>();

    /**
     * Returns the SessionFactory used for this static class.
     *
     * @return SessionFactory
     */
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateUtil.class) {
                if (sessionFactory == null) {
                	log.debug("Creating SessionFactory.");
                	try {
                		if (configuration == null) configuration = new Configuration();
                		sessionFactory = configuration.configure().buildSessionFactory();
                	} catch (Exception ex) {
                		log.error("SessionFactory problem.", ex);
                	}
                }
            }
        }
        return sessionFactory;
    }

    /**
     * Retrieves the current Session local to the thread.
     * <p/>
     * If no Session is open, opens a new Session for the running thread.
     *
     * @return Session
     */
    public static Session getSession() {
    	Session sess = threadSession.get();
    	if (sess == null) {
    		sess = getSessionFactory().openSession();
    		threadSession.set(sess);
    	}
    	return sess;
    }

    /**
     * Set this session to read-only mode
     *
     */
    public static void readOnlySession() {
    	log.debug("Setting Session to read-only.");
        getSession().setFlushMode(FlushMode.MANUAL);
    }
    
    /**
     * Is this a read-only session?
     * @return true if read-only
     */
    public static boolean isReadOnlySession() {
    	return FlushMode.isManualFlushMode(getSession().getFlushMode());
    }
    
    
    /**
     * Closes the Session local to the thread.
     */
    public static void closeSession() {
        try {
            Session s = threadSession.get();
            threadSession.set(null);
            if (s != null && s.isOpen()) {
                log.debug("Closing Session of this thread.");
                s.close();
            }
        } catch (HibernateException ex) {
            log.error(ex);
            throw ex;
        }
    }

    /**
     * Start a new database transaction.
     */
    public static void beginTransaction() {
    	Session sess = getSession();
    	//Transaction tx = sess != null ? sess.getTransaction() : null;
        try {
            //if (tx == null || !tx.isActive()) {
                log.debug("Starting new database transaction in this thread.");
                sess.beginTransaction();
            //}
        } catch (HibernateException ex) {
            log.error(ex);
            throw ex;
        }
    }

    /**
     * Has the transaction(beginTransaction) been started (been called).
     */
    public static boolean isTransactionActive() {
    	Session sess = getSession();
    	Transaction tx = sess != null ? sess.getTransaction() : null;
        try {
            if (tx != null && tx.isActive()) {
            	return true;
            }
        } catch (HibernateException ex) {
            log.error(ex);
            throw ex;
        }
        return false;
    }

    /**
     * Commit the database transaction.
     */
    public static void commitTransaction() {
    	Session sess = threadSession.get();
        Transaction tx = sess != null ? sess.getTransaction() : null;
        try {
            if ( tx != null && !tx.wasCommitted()
                            && !tx.wasRolledBack() ) {
                log.debug("Committing database transaction of this thread.");
                tx.commit();
            }
        } catch (HibernateException ex) {
            rollbackTransaction();
            log.error(ex);
            throw ex;
        }
    }

    /**
     * Commit the database transaction.
     */
    public static void rollbackTransaction() {
    	Session sess = threadSession.get();
    	Transaction tx = sess != null ? sess.getTransaction() : null;
        try {
            if ( tx != null && !tx.wasCommitted() && !tx.wasRolledBack() ) {
                log.debug("Trying to rollback database transaction of this thread.");
                tx.rollback();
            }
        } catch (HibernateException ex) {
            log.error(ex);
            throw ex;
        } finally {
            if (sess != null && sess.isOpen()) {
                log.debug("Closing Session of this thread.");
                sess.close();
            }
            threadSession.set(null);
        }
    }

    /**
     * Only effective for Non-Jndi use
     * @return current non-Jndi config
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Only effective for Non-Jndi use
     * @param configuration
     */
    public static void setConfiguration(Configuration configuration) {
        HibernateUtil.configuration = configuration;
    }

}
