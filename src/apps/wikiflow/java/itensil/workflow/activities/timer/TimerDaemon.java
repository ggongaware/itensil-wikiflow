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
package itensil.workflow.activities.timer;

import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.LockException;
import itensil.repository.NotFoundException;
import itensil.security.AuthenticatedUser;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceAdmin;
import itensil.security.UserSpaceException;
import itensil.util.Check;
import itensil.workflow.FlowEvent;
import itensil.workflow.RunException;
import itensil.workflow.Runner;
import itensil.workflow.activities.UserActivities;
import itensil.workflow.activities.signals.SignalImpl;
import itensil.workflow.activities.state.Activity;
import itensil.workflow.activities.state.ActivityStateStore;
import itensil.workflow.model.FlowModel;
import itensil.workflow.model.element.Condition;
import itensil.workflow.model.element.Path;
import itensil.workflow.model.element.Step;
import itensil.workflow.model.element.Timer;
import itensil.workflow.model.element.Until;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.xml.sax.SAXException;

public class TimerDaemon implements Runnable {

    Thread _thread;
    boolean keepRunning;
    Date nextTimer;
    Runner<Activity,String> lastRun;
    Activity lastAct;
    long sleepTime;
    
    static Logger log = Logger.getLogger(TimerDaemon.class);
    public final static long DEFAULT_SLEEP_TIME = 37 * 1000; // 37 sec
    
    // singleton
    private static TimerDaemon instance;
    
    private TimerDaemon() {
    	sleepTime = DEFAULT_SLEEP_TIME;
    	keepRunning = false;
    }
    
    /**
     * should only be called by environmental inits
     * @return
     */
    public static TimerDaemon initInstance() {
    	if (instance == null) instance = new TimerDaemon();
    	if (!instance.isAlive()) instance.start();
    	return instance;
    }
    
    /**
     * 
     * @return might be null for this VM
     */
    public static TimerDaemon getInstance() {
    	return instance;
    }
    
    public void start() {
        keepRunning = true;
        _thread = new Thread(this);
        _thread.setPriority(Thread.MIN_PRIORITY);
        _thread.start();
    }
    
    @SuppressWarnings("unchecked")
    public void run() {
    	log.info("Started");
    	try {
	    	// query for next timer
	    	HibernateUtil.beginTransaction();
	    	HibernateUtil.readOnlySession();
	    	
	    	Query qry = HibernateUtil.getSession().getNamedQuery("Timer.getNextTimer");
	    	qry.setMaxResults(1);
	    	Iterator<ActivityTimer> atItr = qry.iterate();
	    	ActivityTimer nxtActTimer = atItr.hasNext() ? atItr.next() : null;
	    	nextTimer = nxtActTimer != null ? nxtActTimer.getAtTime() : null;
	    	
	    	HibernateUtil.commitTransaction();
	    	HibernateUtil.closeSession();
	    	
	    	// super user thread
	        SecurityAssociation.setUser(SysAdmin.getUser());
    	} catch (Exception ex) {
    		log.error(ex);
    		
    		// try attmept in 3 minutes, DB is probably down
    		nextTimer = new Date(System.currentTimeMillis() + 3 * 60000);
    	}
        
    	// main loop
    	while (keepRunning) {
    		try {
	    		
	    		// check on next timer
	    		if (nextTimer != null && nextTimer.getTime() <= System.currentTimeMillis()) {
	    			
	    			// query all timers ready at this moment, ordered by flow
	    			HibernateUtil.beginTransaction();
	    			HibernateUtil.readOnlySession();
	    			
	    			Query qry = HibernateUtil.getSession().getNamedQuery("Timer.getReadyTimers");
	    			qry.setTimestamp("fromTime", new Date());
	                List<ActivityTimer> timers = qry.list();
	    			
	    			HibernateUtil.commitTransaction();
	    	    	HibernateUtil.closeSession();
	    	    	
	    	    	// for each timer
	    	    	for (ActivityTimer att : timers) {
	    	    		try {
	    	    			Activity act = att.getActivity();
	    	    			String timerId = att.getTimerId();
	    	    			
	    	    			HibernateUtil.beginTransaction();
	    	    			
	    	    			// if activity not dead
	    	    			if (act != null && act.getSubmitId() != null && act.getStates().containsKey(timerId)) {  
		    	    			HibernateUtil.getSession().refresh(act);
				    		
		    	    			AuthenticatedUser aUser = UserSpaceAdmin.getAuthUser(act.getSubmitId());
		    	    			UserSpace uspace = aUser != null ? UserSpaceAdmin.getUserSpace(act.getUserSpaceId()) : null;
		    	    			
		    	    			if (aUser == null || uspace == null) {
		    	    				HibernateUtil.getSession().delete(att);
		    	    				continue;
		    	    			}
		    	    			
		    	    			aUser.setActiveUserSpace(uspace);
		    	    			
		    	    			// switch user
								SecurityAssociation.setUser(aUser);
								
					    		// get runner
								try {
									Runner<Activity,String> run = getRunner(act);
									
									// fire!
									try {
										run.handleEvent(new FlowEvent<Activity,String>(act, timerId));
									} catch (RunException re) {
										// TODO - alert the activity owner
										log.warn(re);
										FlowModel mod = run.getModel();
										Step stp = mod.getStep(timerId);
										boolean killTimer = false;
										if (stp == null) { 
											killTimer = true;
										} else {
											
											Collection<Path> paths = stp.getPaths();
											if (paths.isEmpty()) 
												killTimer = true;
											else
												for (Path pth : paths) {
													if (pth.getToStep() == null) {
														killTimer = true;
														break;
													}
												}

										}
										if (killTimer) HibernateUtil.getSession().delete(att);
									}
								} catch (NotFoundException nfe) {
									log.warn(nfe);
									HibernateUtil.getSession().delete(att);
								}
					    		
	    	    			} else {
	    	    				HibernateUtil.getSession().delete(att);
	    	    			}
	    	    			
				    		// update checkTime
				    		HibernateUtil.commitTransaction();
				    		
			    		} catch (Exception ex) {
							log.error(ex);
							HibernateUtil.rollbackTransaction();
						}
			    		
			    		// switch user back to super
			    		SecurityAssociation.setUser(SysAdmin.getUser());
	
	    	    	} // end for loop
			    	HibernateUtil.closeSession();
			    	clearRunner();
	    			
	    			// query for next timer
	    	    	HibernateUtil.beginTransaction();
	    			HibernateUtil.readOnlySession();
	    			
	    			qry = HibernateUtil.getSession().getNamedQuery("Timer.getNextTimer");
	    	    	qry.setMaxResults(1);
	    	    	Iterator<ActivityTimer> atItr = qry.iterate();
	    	    	ActivityTimer nxtActTimer = atItr.hasNext() ? atItr.next() : null;
	    	    	nextTimer = nxtActTimer != null ? nxtActTimer.getAtTime() : null;
	    			
	    			HibernateUtil.commitTransaction();
	    	    	HibernateUtil.closeSession();
	    	    	
	    	    	Thread.yield();
	    		}
	    		
	    		// query a batch conditionals with older checkTimes, ordered by flow
	    		HibernateUtil.beginTransaction();
				HibernateUtil.readOnlySession();
				
				Query qry = HibernateUtil.getSession().getNamedQuery("Timer.getCondTimers");
				qry.setMaxResults(53);
	            List<ActivityTimer> timers = qry.list();
				
				HibernateUtil.commitTransaction();
		    	HibernateUtil.closeSession();
		    	
		    	// for each conditional timer
		    	for (ActivityTimer att : timers) {
		    		try {
		    			Activity act = att.getActivity();
		    			String timerId = att.getTimerId();
		    			
		    			HibernateUtil.beginTransaction();
		    			
		    			// if activity not dead
		    			if (act != null && act.getSubmitId() != null && act.getStates().containsKey(timerId)) {  
	    	    			HibernateUtil.getSession().refresh(act);
	    	    			
	    	    			
	    	    			AuthenticatedUser aUser = UserSpaceAdmin.getAuthUser(act.getSubmitId());
	    	    			UserSpace uspace = aUser != null ? UserSpaceAdmin.getUserSpace(act.getUserSpaceId()) : null;
	    	    			
	    	    			if (aUser == null || uspace == null) {
	    	    				HibernateUtil.getSession().delete(att);
	    	    				continue;
	    	    			}

	    	    			aUser.setActiveUserSpace(uspace);
	    	    			
	    	    			// switch user
							SecurityAssociation.setUser(aUser);
							
				    		// get runner
							Runner<Activity,String> run;
							try {
								run = getRunner(act);
							} catch (NotFoundException nfe) {
								log.warn(nfe);
								HibernateUtil.getSession().delete(att);
								HibernateUtil.commitTransaction();
								continue;
							}
				    		
							// get the condition from the model
							Step step = run.getModel().getStep(timerId);
							if (step != null && step instanceof Timer) {							
								Timer timerDef = (Timer)step;
								Until ud = timerDef.selectOneChild(Until.class);
								Condition cond = ud != null ? ud.selectOneChild(Condition.class) : null;
								if (cond != null) {
									// test condition
									FlowEvent<Activity,String> evt = new FlowEvent<Activity,String>(act, timerId);
									Condition conds[] = new Condition[]{cond, null};
				            		String ret = run.getEvals().evalExclusive(act.getFlow().getId(), conds, evt);
				            		if (ret != null) {
				            			run.handleEvent(evt);
				            		} else {
				            			att.setCheckTime(new Date());
				            			HibernateUtil.getSession().update(att);
				            		}
								} else {
									log.warn("Condition missing: " + act.getFlow().getNode().getUri() + "#" + timerId);
									HibernateUtil.getSession().delete(att);
								}
							} else {
								log.warn("Timer missing: " + act.getFlow().getNode().getUri() + "#" + timerId);
								HibernateUtil.getSession().delete(att);
							}
							
		    			} else {
		    				HibernateUtil.getSession().delete(att);
		    			}
		    			
		    			act.clearCache();
		    			
			    		// update checkTime
			    		HibernateUtil.commitTransaction();
			    		
		    		} catch (Exception ex) {
						log.error(ex);
						HibernateUtil.rollbackTransaction();
					}
		    		
		    		// switch user back to super
		    		SecurityAssociation.setUser(SysAdmin.getUser());
		    		
		    		Thread.yield();
		    		
		    	} // end for loop
		    	HibernateUtil.closeSession();
		    	clearRunner();
	    		
		    	synchronized (ActivityStateStore.timerSync) {
		    		ActivityStateStore.timerSync.notifyAll();
		    	}
		    	
		    	// schedule some sleep
		    	try {
		    		if (nextTimer != null && (nextTimer.getTime() - System.currentTimeMillis()) < sleepTime) {
		    			long slp = nextTimer.getTime() - System.currentTimeMillis();
			    		Thread.sleep(slp > 50 ? slp : 50);
			    	} else {
			    		Thread.sleep(sleepTime);
			    	}
	            } catch (InterruptedException e) {
	                log.info(e);
	                keepRunning = false;
	            }
    		}
    		catch (Exception ex) {
        		log.error(ex); // probably DB offline, keep trying
        		Thread.yield();
        	}
    	} 
    	
    }
    
    public void scheduledTimer(Date atTime) {
    	if (nextTimer == null || atTime.before(nextTimer)) {
    		nextTimer = atTime;
    	}
    }
    
    public void setSleepTime(long millis) {
    	sleepTime = millis;
    }
    
    protected void clearRunner() {
    	lastRun = null;
    }
    
    protected Runner<Activity,String> getRunner(Activity activityEnt) 
    		throws NotFoundException, AccessDeniedException, IOException, SAXException, LockException {
    	if (lastRun != null) {
    		if (activityEnt.getFlow().getId().equals(lastRun.getStates().getFlowId())
    				&& Check.isEmpty(activityEnt.getVariationId())
    				&& Check.isEmpty(lastAct.getVariationId()))
    			return lastRun;
    	}
    	UserActivities ua = new UserActivities(SecurityAssociation.getUser(), HibernateUtil.getSession());
    	lastRun = ua.getRunner(activityEnt);
    	lastAct = activityEnt;
    	return lastRun;
    }
    
    public boolean isAlive() {
        return keepRunning && _thread.isAlive();
    }
    
    public void die() {
    	die(false);
    }

    public void die(boolean join) {
        keepRunning = false;
        try {
			if (_thread != null) {
				_thread.interrupt();
				if (join) _thread.join();
			}
			_thread = null;
		} catch (InterruptedException e) { 
			log.error(e);	
		}
    }

}
