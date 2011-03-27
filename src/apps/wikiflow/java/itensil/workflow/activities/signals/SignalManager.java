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

package itensil.workflow.activities.signals;

import itensil.entities.EntityLazyRecordRoot;
import itensil.entities.EntityModelUpdater;
import itensil.io.HibernateUtil;
import itensil.repository.NotFoundException;
import itensil.security.User;
import itensil.security.hibernate.UserEntity;
import itensil.uidgen.IUID;
import itensil.uidgen.IUIDGenerator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * All access to signals should go through the SignalManager to ensure
 * consistent control and status of signals
 * 
 * SignalManager performs: All adds, updates, deletes related to signals records
 * (alerts, etc.) adds signal uuid (signal_uuid to user tables for token based
 * access) provide signal encode and decode of signal status bit per user
 * 
 * @author ejones@itensil.com
 */
public class SignalManager {
	protected static Logger logger = Logger.getLogger(SignalManager.class);

    //mailed=0(signal to be sent - active signal), mailed=1(signal sent - active signal), mailed=2(required action completed([optional state]), mail=3(de-activated by mechanism)
	public static final int SIGNAL_STATUS_ACTIVE_PENDING = 0;
	public static final int SIGNAL_STATUS_ACTIVE_SENT = 1;
	public static final int SIGNAL_STATUS_COMPLETED = 2;
	public static final int SIGNAL_STATUS_DEACTIVATED = 3;
	
	// read=0(not read, read=1(has been read)
	public static final int SIGNAL_STATUS_READ_FALSE = 0;
	public static final int SIGNAL_STATUS_READ_TRUE = 1;

	public static final String SIGNAL_STORED_NOT_EQUAL_TO_SIGNAL_ID = "Signals are not equal. signal id: ";
	
	private static SignalManager instance;
	private static HashMap<String, List<SignalImpl>> signals = new HashMap<String, List<SignalImpl>>();

	
	/**
	 * Singleton which manages Signals thoughout application
	 * 
	 * @return SignalManager
	 */
	public static SignalManager getInstance() {
		if (instance == null) {
			synchronized (SignalManager.class) {
				if (instance == null) {
					instance = new SignalManager();
				}
			}
		}
		return instance;
	}

	/**
	 * Get Signal mask for user 
	 * 
	 * @param usr
	 * @return
	 * @throws NotFoundException
	 */
	public static Long getSignalStatus(User usr) throws NotFoundException {
		Long signalStatus = new Long(SignalUtil.SIGNAL_INITIAL_MASK);
		if (pendingAlertCount(usr) > 0)
			signalStatus |= SignalUtil.SIGNAL_ACTIVE_ALERT_MASK;
		return signalStatus;
	}

	/**
	 * Get associated ActivityAlerts 
	 * 
	 * @param user
	 * @return
	 * @throws NotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static List<SignalImpl> getAlerts(User user) throws NotFoundException {
		List<SignalImpl> userSignals = getSignals(user);
		return getSignalType(userSignals, SignalType.ACTIVITY_ALERT);
	}

	/**
	 * Get associated Signals
	 * 
	 * @param user
	 * @return
	 * @throws NotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static List<SignalImpl> getSignals(User user) throws NotFoundException {
		if (user == null || user.getUserId() == null)
			throw new NotFoundException(null);
		return getInstance().getSignals(user.getUserId());
	}

	public static void markAlertAsRead(String alertId) throws NotFoundException {
		if (alertId == null)
			throw new NotFoundException(null);
		getInstance().markAlertReadStatus(alertId, true);
	}

	private void markAlertReadStatus(String alertId, boolean read) throws NotFoundException {
		if (alertId == null)
			throw new NotFoundException(null);

		synchronized (signals) 
		{
		
		boolean existingTransaction = HibernateUtil.isTransactionActive();
		if(!existingTransaction) HibernateUtil.beginTransaction();
		// get alert and user info
		SignalImpl alert = (SignalImpl) HibernateUtil.getSession().get(
				SignalImpl.class, alertId);
		if(!existingTransaction) {
			HibernateUtil.commitTransaction();
		}

		if (alert == null)
			throw new NotFoundException(null);

		alert.setRead(true);
		
		saveOrUpdate(alert);
		}
	}

	public static int pendingAlertCount(User user) throws NotFoundException {
		if (user == null)
			throw new NotFoundException(null);

		return 	getInstance().pendingTypeCount(user.getUserId(), SignalType.ACTIVITY_ALERT);
	}

	/**
	 * 	 All instances of SignalImpl should be persisted using this method
	 * 
	 * @param signalInternalApp
	 */
	public static void saveOrUpdateSignal(SignalImpl signalInternalApp) {
		getInstance().saveOrUpdate(signalInternalApp);
	}

	/**
	 * 	 All instances of SignalImpl should be deleted using this method
	 * 
	 * @param signalInternalApp
	 */
	public static void deleteSignal(SignalImpl signalInternalApp) throws NotFoundException {
		getInstance().delete(signalInternalApp);
	}

	
	/**
	 * @param signalInternalApp
	 */
	private void saveOrUpdate(SignalImpl signalInternalApp) {
		boolean existingTransaction = HibernateUtil.isTransactionActive();

		synchronized (signals) 
		{
			if (!existingTransaction) {
				HibernateUtil.beginTransaction();
			}

			HibernateUtil.getSession().saveOrUpdate((SignalImpl)signalInternalApp);

			if (!existingTransaction) {
				HibernateUtil.commitTransaction();
			}
			String userId= signalInternalApp.getAssignId();
			List<SignalImpl> userSignalList = signals.get(userId);
			if(userSignalList == null){
				userSignalList= new ArrayList<SignalImpl>();
			}
			else {
				String id= signalInternalApp.getId();
				int index=0;
				for (SignalImpl sig : userSignalList) {
					if(id.equals(sig.getId())) {
						userSignalList.remove(index);
						break;
					}
				}
			}
			userSignalList.add(signalInternalApp);
			signals.put(userId, userSignalList);
		}

	}

	/**
	 * Deletes SignalImpl object from persistent sotre as well and from internal hash
	 * 
	 * @param signalInternalApp
	 */
	private void delete(SignalImpl signalInternalApp) throws NotFoundException {
		if( signalInternalApp == null || signalInternalApp.getId() == null) 
			     throw new NotFoundException("SignalManager.delete: Signal Id is null");
		
		SignalImpl sigExternalStore;
		boolean existingTransaction = HibernateUtil.isTransactionActive();

		synchronized (signals) 
		{
			if (!existingTransaction) {
				HibernateUtil.beginTransaction();
			}
			
			//get true object from store for deletion
			sigExternalStore = (SignalImpl)HibernateUtil.getSession().get(SignalImpl.class, signalInternalApp.getId());
			// verify object is match
			if(sigExternalStore == null || !sigExternalStore.storeEqual(signalInternalApp)) 
				{
					if (!existingTransaction) 
					{
						HibernateUtil.commitTransaction();
					}
					throw new NotFoundException(SIGNAL_STORED_NOT_EQUAL_TO_SIGNAL_ID + signalInternalApp.getId()); 
				}
			HibernateUtil.getSession().delete(sigExternalStore);

			if (!existingTransaction) {
				HibernateUtil.commitTransaction();
			}
			String userId= signalInternalApp.getAssignId();
			List<SignalImpl> userSignalList = signals.get(userId);
			if(userSignalList != null) {
				for(SignalImpl sig : userSignalList)
				{
					if(sig.getId().equals(signalInternalApp.getId()))
					{
						userSignalList.remove(sig);
						break;
					}
				}
				if(userSignalList != null && userSignalList.size() > 0) 
				{
					signals.put(userId, userSignalList);
				}
				else 
				{
					signals.remove(userId);
				}

				}
			}
	}

	private SignalManager() {
	}

	/**
	 * @param id
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<SignalImpl> getSignals(String id) {
		if (id == null)
			return null;

		List<SignalImpl> idSignals = null;
		synchronized (signals) {
			if (signals.containsKey(id)) {
				idSignals = signals.get(id);
			} else {
				Session session = HibernateUtil.getSession();
				boolean existingTransaction = HibernateUtil.isTransactionActive();

				if(!existingTransaction) session.beginTransaction();
				Query qry = session.getNamedQuery("Signal.getActiveSignalsById");
				qry.setString("assignId", id);
				
				// TODO revisit getSignals maxResults(100) setting if signals are composed of internal process communications -ej
				qry.setMaxResults(100); // keep this from growing to DOS size

				idSignals = qry.list();

				if(!existingTransaction) {
					HibernateUtil.commitTransaction();
					HibernateUtil.closeSession();
				}

				applyUpdateRules(id, idSignals);

				signals.put(id, idSignals);
			}
		}
		return idSignals;

	}

	private int pendingTypeCount(String id, SignalType type) {
		if (id == null)
			return 0;
		int count;
		synchronized (signals) {
			if (signals.containsKey(id)) {
				count = getActiveTypeCount(signals.get(id), type);
			} else {
				count = getActiveTypeCount(id, type);
			}
		}
		return count;
	}

	private int getActiveTypeCount(String id, SignalType type) {
		int count = 0;
		String queryName;
		Session session = HibernateUtil.getSession();

		boolean existingTransaction = HibernateUtil.isTransactionActive();
		if(!existingTransaction) HibernateUtil.beginTransaction();
		Query qry;
		switch (type) {
		case ACTIVITY_ALERT:
			queryName = "Signal.AA.countActiveByAssignedIdSignalTypeMailed";

			qry = session.getNamedQuery(queryName);
			qry.setString("assignId", id);
			qry.setInteger("mailedLtOrEq", SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
			break;

			// TODO implement class and query for signalType=SIGNAL_ALERT	
/*
 		case SIGNAL_ALERT: 
			queryName = "Signal.SA.countActiveByAssignedIdSignalTypeMailed";

			qry = session.getNamedQuery(queryName);
			qry.setString("signalAssignedId", id);
			qry.setInteger("mailedLtOrEq", SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
			break;
*/
		// TODO implement class and query for signalType=SIGNAL_INTERPROCESS_DEF1	
/*
		case SIGNAL_INTERPROCESS_DEF1:
			queryName = "Signal.SA.countActiveByAssignedIdSignalTypeMailed";

			qry = session.getNamedQuery(queryName);
			qry.setString("signalAssignedId", id);
			qry.setInteger("mailedLtOrEq", SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
			break;
			// TODO test Signal.countActiveByAssignedIdMailed
*/
		default:
			queryName = "Signal.ALL.countActiveByAssignedIdMailed";

			qry = session.getNamedQuery(queryName);
			qry.setString("assignId", id);
			qry.setInteger("mailedLtOrEq", SignalManager.SIGNAL_STATUS_ACTIVE_SENT);
			break;
		}

		count = ((Number) qry.iterate().next()).intValue();

		if(!existingTransaction) {
			HibernateUtil.commitTransaction();
			HibernateUtil.closeSession();
		}

		return count;
	}

	private int getActiveTypeCount(List<SignalImpl> userSignals, SignalType type) {
		int count = 0;
		for (SignalImpl sig : userSignals) {
			if (type == sig.getSignalType() && sig.getMailed() <= SIGNAL_STATUS_ACTIVE_SENT )
			{
				count++;
			}
		}
		return count;
	}

	private static List<SignalImpl> getSignalType(List<SignalImpl> userSignals, SignalType type) {
		List<SignalImpl> ofSignalType = new ArrayList<SignalImpl>();
		for (SignalImpl sig : userSignals) {
			if (SignalType.ACTIVITY_ALERT == sig.getSignalType())
			{
				ofSignalType.add(sig);
			}
		}
		return ofSignalType;
	}

	private void applyUpdateRules(String id, List<SignalImpl> idSignal) {
		Session session = HibernateUtil.getSession();
		boolean existingTransaction = HibernateUtil.isTransactionActive();

		if(!existingTransaction) session.beginTransaction();

		for (Signal sig : idSignal) {
			boolean updateRequired = false;

			// TODO determine update rules for signals
			//updateRequired = true;
			if (updateRequired) {
				session.saveOrUpdate(sig);
			}
		}
		if(!existingTransaction) 
		{
			HibernateUtil.commitTransaction();
			HibernateUtil.closeSession();
		}
		return;
	}

}
