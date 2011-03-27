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
package itensil.mail;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Author: grant@gongaware.com
 */
public class MailServicePool {

    public static final int MAX_THREADS = 5;

    /**
     * PoolThread Class
     */
    protected static class PoolThread implements Runnable {

        Thread myThread;
        ArrayList<MailService> services;
        boolean keepRunning;

        PoolThread() {
            keepRunning = true;
            services = new ArrayList<MailService>();
            myThread = new Thread(this);
            myThread.setPriority(Thread.MIN_PRIORITY);
            myThread.start();
        }

        public void run() {

            while (keepRunning) {
            	try {
            		for (MailService serv : services) {
            			serv.run();
            		}
            	} catch (ConcurrentModificationException cme) {
            		// eat it
            	}
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                    return;
                }
            }
        }

        void addService(MailService serv) {
            services.add(serv);
        }

        void removeService(MailService serv) {
            services.remove(serv);
        }

        int serviceCount() {
            return services.size();
        }

        boolean isAlive() {
            return keepRunning && myThread.isAlive();
        }

        void die() {
            keepRunning = false;
        }

    }

    /**
     * ServicePair class
     */
    protected static class ServicePair {
        PoolThread pThread;
        MailService mailer;
    }


    /**
     * MailServicePool members
     */
    private static MailServicePool _pool;

    private Map<String, ServicePair> services;
    private final PoolThread pThreads[] = new PoolThread[MAX_THREADS];
    private int pThreadCount;

    private MailServicePool() {
        pThreadCount = 0;
        services = new HashMap<String, ServicePair>();
    }

    /**
     * Fetch the singleton
     * @return the pool
     */
    public static MailServicePool getPool() {

        if (_pool == null) {
            synchronized (MailServicePool.class) {
                if (_pool == null) {
                    _pool = new MailServicePool();
                }
            }
        }
        return _pool;
    }

    /**
     *
     * @param serviceId
     * @return service object
     */
    public MailService getService(String serviceId) {

        ServicePair servP = services.get(serviceId);
        if (servP != null) {
            return servP.mailer;
        }
        return null;
    }

    public boolean hasServices() {
        return !services.isEmpty();
    }

    public void removeService(String serviceId) {
        setService(serviceId, null);
    }

    /**
     *
     * @param serviceId
     * @param service set to null to kill
     */
    public void setService(String serviceId, MailService service) {

        ServicePair servP = services.get(serviceId);
        if (servP != null) {

            // update or remove
            servP.pThread.removeService(servP.mailer);
            if (service != null) {
                servP.mailer = service;
                servP.pThread.addService(servP.mailer);
            } else {
                services.remove(serviceId);
            }
        } else if (service != null) {

            // add and balance
            servP = new ServicePair();
            servP.mailer = service;
            synchronized (pThreads) {
                if (pThreadCount < MAX_THREADS) {
                    PoolThread pt = new PoolThread();
                    pThreads[pThreadCount++] = pt;
                    pt.addService(service);
                    servP.pThread = pt;
                } else {
                    PoolThread min = pThreads[0];
                    for (int i = 1; i < pThreadCount; i++) {
                        if (min.serviceCount() > pThreads[i].serviceCount()) {
                            min = pThreads[i];
                        }
                    }
                    min.addService(service);
                    servP.pThread = min;
                }
            }
            services.put(serviceId, servP);
        }
    }

    /**
     * Shut her down
     */
    public void shutdown() {
        for (int ii=0; ii < pThreadCount; ii++) {
            pThreads[ii].die();
            pThreads[ii] = null;
        }
        pThreadCount = 0;
    }

}
