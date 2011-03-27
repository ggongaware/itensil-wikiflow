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

import itensil.io.HibernateUtil;
import itensil.repository.AccessDeniedException;
import itensil.repository.DefaultNodePermission;
import itensil.repository.DefaultNodeProperties;
import itensil.repository.DuplicateException;
import itensil.repository.LockException;
import itensil.repository.MutableRepositoryNode;
import itensil.repository.NodeContent;
import itensil.repository.NodeLock;
import itensil.repository.NodePermission;
import itensil.repository.NodeProperties;
import itensil.repository.NodeVersion;
import itensil.repository.NotFoundException;
import itensil.repository.Repository;
import itensil.repository.RepositoryException;
import itensil.repository.RepositoryHelper;
import itensil.repository.RepositoryNode;
import itensil.repository.event.ContentEvent;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.Group;
import itensil.security.RelativeGroup;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceException;
import itensil.util.Check;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author ggongaware@itensil.com
 * 
 */
public class NodeEntity implements MutableRepositoryNode, Serializable {

	private static Logger log = Logger.getLogger(NodeEntity.class);

	protected static long MAX_LOCK_MILLIS = 10 * 60 * 1000; // 10 minutes

	private RepositoryEntity repoEntity;
	private Set<VersionEntity> versionEntities;
	private VersionEntity defaultVersion;

	private Set<LockEntity> lockEntities;
	private Set<NodeEntity> childEntities;
	private Map<String, PermissionEntity> permissionEntities;
	private NodeEntity parentNode;

	private boolean collection;
	private String nodeId;
	private String localUri;
	private long deleted;
	private User owner;
	private Group contextGroup;

	private String ownerId;
	private String contextGroupId;
	private Date createTime;

	public NodeEntity() {
	}

	public NodeEntity(String nodeId) {
		this.nodeId = nodeId;
	}

	public void initNew() {
		versionEntities = new HashSet<VersionEntity>();
		lockEntities = new HashSet<LockEntity>();
		childEntities = new HashSet<NodeEntity>();
	}

	public Map<String, PermissionEntity> getPermissionEntities() {
		return permissionEntities;
	}

	public void setPermissionEntities(
			Map<String, PermissionEntity> permissionEntities) {
		this.permissionEntities = permissionEntities;
	}

	public Set<VersionEntity> getVersionEntities() {
		return versionEntities;
	}

	public void setVersionEntities(Set<VersionEntity> versionEntities) {
		this.versionEntities = versionEntities;
	}

	public RepositoryEntity getRepoEntity() {
		return repoEntity;
	}

	public void setRepoEntity(RepositoryEntity repoEntity) {
		this.repoEntity = repoEntity;
	}

	public String getContextGroupId() {
		return contextGroupId;
	}

	public void setContextGroupId(String contextGroupId) {
		this.contextGroupId = contextGroupId;
	}

	public Set<LockEntity> getLockEntities() {

		if (!lockEntities.isEmpty()) {
			// clean expired
			Date now = new Date();
			ArrayList<LockEntity> expires = new ArrayList<LockEntity>(
					lockEntities.size());
			for (LockEntity lock : lockEntities) {
				if (now.compareTo(lock.getExpireTime()) >= 0) {
					expires.add(lock);
				}
			}
			if (!expires.isEmpty()) {
				Session session = HibernateUtil.getSession();
				for (LockEntity lock : expires) {
					session.delete(lock);
					lockEntities.remove(lock);
				}
			}
		}
		return lockEntities;
	}

	public void setLockEntities(Set<LockEntity> lockEntities) {
		this.lockEntities = lockEntities;
	}

	/**
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public NodeVersion[] getVersions() throws AccessDeniedException {
		VersionEntity def = getDefaultVersionEnt();
		if (def == null) {
			return new NodeVersion[0];
		}
		Set<VersionEntity> versions = getVersionEntities();
		NodeVersion vers[] = new NodeVersion[versions.size()];
		vers[0] = def;
		int i = 1;
		for (VersionEntity ve : versions) {
			if (!ve.equals(def)) {
				vers[i++] = ve;
			}
		}
		return vers;
	}

	/**
	 * 
	 * @param version
	 * @return
	 * @throws AccessDeniedException
	 */
	public NodeProperties getProperties(NodeVersion version)
			throws AccessDeniedException {
		VersionEntity ver = resolveVersion(version);
		return ver == null ? null : new DefaultNodeProperties(ver, ver
				.getPropertyMap());
	}

	/**
	 * 
	 * @param properties
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public void setProperties(NodeProperties properties)
			throws AccessDeniedException, LockException {
		checkPermission(DefaultNodePermission.WRITE);
		checkLocked();
		VersionEntity ver = resolveVersion(properties.getVersion());
		if (ver == null) {
			ver = createVersion(properties.getVersion());
			// HibernateUtil.getSession().refresh(ver);
		}
		if (properties != ver) { // updating self
			ver.replaceProperties(properties);
		}
		HibernateUtil.getSession().update(ver);
	}

	/**
	 * 
	 * @param properties
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public void setPropertiesKbCnvrt(NodeProperties properties)
			throws AccessDeniedException, LockException {
		// checkPermission(DefaultNodePermission.WRITE);
		// checkLocked();
		VersionEntity ver = resolveVersion(properties.getVersion());
		if (ver == null) {
			ver = createVersion(properties.getVersion());
			// HibernateUtil.getSession().refresh(ver);
		}
		if (properties != ver) { // updating self
			ver.replaceProperties(properties);
		}
		HibernateUtil.getSession().update(ver);
	}

	/**
	 * 
	 * @param version
	 * @return
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public NodeContent getContent(NodeVersion version)
			throws AccessDeniedException, LockException {
		checkLocked();
		VersionEntity ver = resolveVersion(version);
		if (ver != null) {
			Set<ContentEntity> cont = ver.getContentEntities();
			return cont.isEmpty() ? null : cont.iterator().next();
		}
		return null;
	}

	/**
	 * 
	 * @param content
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public void setContent(NodeContent content) throws AccessDeniedException,
			LockException {
		checkPermission(DefaultNodePermission.WRITE);
		checkLocked();

		VersionEntity ver = resolveVersion(content.getVersion());
		Set<ContentEntity> conts;
		if (ver == null) {
			ver = createVersion(content.getVersion());
			conts = ver.getContentEntities();
		} else {
			conts = ver.getContentEntities();
		}
		ContentEvent.Type evType = ContentEvent.Type.UPDATE;
		if (conts.isEmpty()) {
			evType = ContentEvent.Type.CREATE;
			ContentEntity cont = new ContentEntity();
			cont.setVersionEntity(ver);
			cont.replaceContent(content);
			conts.add(cont);
			HibernateUtil.getSession().persist(cont);
		} else {
			ContentEntity cont = conts.iterator().next();
			cont.replaceContent(content);
			HibernateUtil.getSession().update(cont);
		}
		HibernateUtil.getSession().flush();
		getRepoEntity().getManager().fireContentChangeEvent(this, content,
				evType);
	}


	/**
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public NodeLock[] getLocks() throws AccessDeniedException {
		Set<LockEntity> locks = getLockEntities();
		return locks.toArray(new NodeLock[locks.size()]);
	}

	/**
	 * 
	 * @param owner
	 * @param expireTime
	 * @param exclusive
	 * @param inheritable
	 * @param ownerInfo
	 * @return
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public NodeLock putLock(User owner, Date expireTime, boolean exclusive,
			boolean inheritable, String ownerInfo)
			throws AccessDeniedException, LockException {

		checkPermission(DefaultNodePermission.WRITE);
		checkLocked();
		long lockMillis = expireTime.getTime() - System.currentTimeMillis();
		if (lockMillis <= 0) {
			return null;
		} else if (lockMillis > MAX_LOCK_MILLIS) {
			expireTime.setTime(System.currentTimeMillis() + MAX_LOCK_MILLIS);
		}

		Set<LockEntity> lockEnts = getLockEntities();
		for (LockEntity currlock : lockEnts) {
			if (owner.equals(currlock.getOwner())
					&& currlock.isExclusive() == exclusive) {

				// lock like this already exsists
				return currlock;
			}
		}
		LockEntity lock = new LockEntity();
		lock.setNodeEntity(this);
		lock.setOwner(owner);
		lock.setExpireTime(expireTime);
		lock.setExclusive(exclusive);
		lock.setInheritable(inheritable);
		lock.setOwnerInfo(ownerInfo);
		HibernateUtil.getSession().persist(lock);
		lockEnts.add(lock);
		return lock;
	}

	/**
	 * 
	 * @param lock
	 * @throws AccessDeniedException
	 */
	public void killLock(NodeLock lock) throws AccessDeniedException {

		checkPermission(DefaultNodePermission.WRITE);
		LockEntity lockEnt = resolveLock(lock);
		if (lockEnt != null) {
			HibernateUtil.getSession().delete(lockEnt);
			Set<LockEntity> locks = getLockEntities();
			if (Hibernate.isInitialized(locks)) {
				locks.remove(lockEnt);
			}
		}
	}

	/**
	 * 
	 * @param toUser
	 * @return
	 * @throws AccessDeniedException
	 */
	public boolean isExclusiveLocked(User toUser) throws AccessDeniedException {
		NodeLock lock = getExclusiveLock();
		return lock != null && !toUser.equals(lock.getOwner());
	}

	protected LockEntity resolveLock(NodeLock lock)
			throws AccessDeniedException {

		// find the lock
		LockEntity lockEnt = (LockEntity) HibernateUtil.getSession().get(
				LockEntity.class, lock.getLockId());
		if (lockEnt != null) {
			if (!equals(lockEnt.getNodeEntity())) {
				throw new AccessDeniedException(lock.getLockId(), "node");
			}
			if (!lockEnt.getOwner().equals(lock.getOwner())) {

				// a manager can get other peoples locks
				if (!hasPermission(SecurityAssociation.getUser(),
						DefaultNodePermission.MANAGE)) {
					throw new AccessDeniedException(lock.getLockId(), "owner");
				}
			}
		}
		return lockEnt;
	}

	/**
	 * 
	 * @param lock
	 * @throws AccessDeniedException
	 */
	public void renewLock(NodeLock lock) throws AccessDeniedException {

		checkPermission(DefaultNodePermission.WRITE);

		long lockMillis = lock.getExpireTime().getTime()
				- System.currentTimeMillis();
		if (lockMillis <= 0) {
			killLock(lock);
		} else if (lockMillis > MAX_LOCK_MILLIS) {
			lockMillis = System.currentTimeMillis() + MAX_LOCK_MILLIS;
		}

		LockEntity lockEnt = resolveLock(lock);

		if (lockEnt == null) {

			// perhaps it already was expired and cleaned
			// so regenerate it
			lockEnt = new LockEntity();
			lockEnt.setLockId(lock.getLockId());
			lockEnt.setNodeEntity(this);
			lockEnt.setOwner(lock.getOwner());
			lockEnt.setExpireTime(new Date(lockMillis));
			lockEnt.setExclusive(lock.isExclusive());
			lockEnt.setInheritable(lock.isInheritable());
			lockEnt.setOwnerInfo(lock.getOwnerInfo());
			HibernateUtil.getSession().persist(lock);
		} else {
			lockEnt.setExpireTime(new Date(lockMillis));
			HibernateUtil.getSession().update(lockEnt);
		}
	}

	/**
	 * 
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public void remove() throws AccessDeniedException, LockException {
		try {
			repoEntity.removeNode(this);
		} catch (NotFoundException e) {
			throw new RepositoryException(e);
		}
	}

	/**
	 * 
	 * @return perm array
	 * @throws AccessDeniedException
	 */
	public NodePermission[] getPermissions() throws AccessDeniedException {
		Map<String, PermissionEntity> permEnts = getPermissionEntities();
		if (permEnts == null) {
			return new NodePermission[0];
		}
		Collection<PermissionEntity> perms = permEnts.values();
		return perms.toArray(new NodePermission[perms.size()]);
	}

	/**
	 * 
	 * @param perm
	 * @return true/false
	 * @throws AccessDeniedException
	 */
	public boolean hasPermission(NodePermission perm)
			throws AccessDeniedException {
		// try {
		return hasPermission(perm.getPrincipal(), DefaultNodePermission
				.permissionToInteger(perm));
		// } catch (AccessDeniedException ade) {
		// if (SecurityAssociation.getUser().equals(perm.getPrincipal())) {
		// return false;
		// }
		// throw ade;
		// }
	}

	/**
	 * 
	 * @param prin
	 * @param perm
	 * @return true/false
	 * @throws AccessDeniedException
	 */
	boolean hasPermission(Principal prin, int perm) {
		return hasPermission(prin, perm, null, true);
	}

	/**
	 * 
	 * @return
	 */
	public boolean readAccess() {
		return hasPermission(SecurityAssociation.getUser(),
				DefaultNodePermission.READ);
	}

	/**
	 * 
	 * @param prin
	 * @param perm
	 * @param direct
	 * @return true/false
	 * @throws AccessDeniedException
	 */
	protected boolean hasPermission(Principal prin, int perm, Group dCtxGroup,
			boolean direct) {
		// 1. sysadmin is override all restrictions
		if (prin instanceof User) {
			if (SysAdmin.isSysAdmin((User) prin)) {
				return true;
			}
			// 2. if user is owner (iten_repo_node.ownerId) allow access
			String prinicipalId = ((User) prin).getUserId();
			if (getOwnerId().equals(prinicipalId)) {
				return true;
			}
		} else if (!(prin instanceof Group)) {
			throw new RepositoryException("Unrecognized principal type");
		}
		PermissionEntity nodPerm = findPermission(prin, dCtxGroup);
		if (nodPerm != null) {
			if (direct || nodPerm.isInherit()) {
				if (nodPerm.hasPermission(perm)) {
					return true;
				} else if (!nodPerm.isGroup()) {
					return false;
				} else if (nodPerm.isNone()) {
					return false;
				}
			} else if (!direct) {
				return false;
			}
		}

		// recursive
		NodeEntity parNode = getParentNode();
		if (parNode != null) {
			Group ctxGroup = getContextGroup();
			return parNode.hasPermission(prin, perm,
					ctxGroup != null ? ctxGroup : dCtxGroup, false);
		}
		return false;
	}

	protected PermissionEntity findPermission(Principal prin, Group dCtxGroup) {
		Map<String, PermissionEntity> permEnts = getPermissionEntities();
		if (permEnts == null) {
			return null;
		}
		PermissionEntity bestPerm = null;
		UserSpace uspace = prin instanceof User ? ((User) prin).getUserSpace()
				: null;
		for (PermissionEntity nodPerm : permEnts.values()) {
			Principal permPrin = nodPerm.getPrincipal();
			if (prin.equals(permPrin)) {
				// return instantly on exact match
				return nodPerm;
			} else if (prin instanceof User && permPrin instanceof Group) {

				if (RelativeGroup.isRelative((Group) permPrin)) {
					Group ctxGroup = getContextGroup();
					if (ctxGroup == null) {
						ctxGroup = dCtxGroup;
					}
					String role = nodPerm.getRole();
					if (!Check.isEmpty(role)) {
						try {
							boolean roleMatch = false;
							if (ctxGroup != null) {
								Set<? extends User> userSet = uspace
										.findGroupRoleUsers(ctxGroup, nodPerm
												.getAxis(), role);
								roleMatch = userSet.contains(prin);
							} else {
								User ownUser = uspace.resolve(getOwner());
								if (ownUser != null) {
									for (Group grp : ownUser.getGroups()) {
										Set<? extends User> userSet = uspace
												.findGroupRoleUsers(grp,
														nodPerm.getAxis(), role);
										if (userSet.contains(prin)) {
											roleMatch = true;
											break;
										}
									}
								}
							}
							if (roleMatch) {
								if (nodPerm.canManage()) {
									return nodPerm;
								} else {
									if (bestPerm == null) {
										bestPerm = nodPerm;
									} else {
										bestPerm = (PermissionEntity) RepositoryHelper
												.betterPermission(bestPerm,
														nodPerm);
									}
								}
							}
						} catch (UserSpaceException ue) {
							// eat it
							log.debug(ue);
						}
					}

				} else {

					// wait for best permission for group
					if (((User) prin).isUserInGroup((Group) permPrin)) {

						if (nodPerm.canManage()) {
							return nodPerm;
						} else {
							if (bestPerm == null) {
								bestPerm = nodPerm;
							} else {
								bestPerm = (PermissionEntity) RepositoryHelper
										.betterPermission(bestPerm, nodPerm);
							}
						}
					} // if in group
				}
			}
		}
		return bestPerm;
	}

	/**
	 * 
	 * @param permission
	 * @throws AccessDeniedException
	 */
	public void grantPermission(NodePermission permission)
			throws AccessDeniedException {
		checkPermission(DefaultNodePermission.MANAGE);
		PermissionEntity permEnt = new PermissionEntity(permission);
		permEnt.setNodeEntity(this);
		Map<String, PermissionEntity> permEnts = getPermissionEntities();
		if (permEnts == null) {
			permEnts = new HashMap<String, PermissionEntity>();
			setPermissionEntities(permEnts);
		} else {
			PermissionEntity cp = permEnts.get(permEnt.getPrincipalId());
			if (cp != null) {
				if (!cp.hasPermission(permEnt.getPermission())) {
					cp.setPermission(permEnt.getPermission());
					HibernateUtil.getSession().update(cp);
				}
				return;
			}
		}
		permEnts.put(permEnt.getPrincipalId(), permEnt);
		HibernateUtil.getSession().persist(permEnt);
	}

	/**
	 * 
	 * @param permission
	 * @throws AccessDeniedException
	 */
	public void revokePermission(NodePermission permission)
			throws AccessDeniedException {

		// if (hasPermission(permission)) {
		checkPermission(DefaultNodePermission.MANAGE);

		Map<String, PermissionEntity> permEnts = getPermissionEntities();
		if (permEnts != null) {
			String prinicipalId;
			Principal prin = permission.getPrincipal();
			if (prin instanceof User) {
				prinicipalId = ((User) prin).getUserId();
			} else if (prin instanceof Group) {
				prinicipalId = ((Group) prin).getGroupId();
			} else {
				throw new RepositoryException("Unrecognized principal type");
			}
			PermissionEntity perm = permEnts.remove(prinicipalId);
			if (perm != null) {
				HibernateUtil.getSession().delete(perm);
			}
		}
		// }
	}

	/**
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public RepositoryNode[] getChildren() throws AccessDeniedException {
		Set<NodeEntity> kids = getChildEntities();
		RepositoryEntity repo = getRepoEntity();
		ArrayList<NodeEntity> accKids = new ArrayList<NodeEntity>(kids.size());
		for (NodeEntity kid : kids) {
			kid.setRepoEntity(repo);
			if (kid.readAccess()) {
				accKids.add(kid);
			}
		}
		return accKids.toArray(new RepositoryNode[accKids.size()]);
	}

	/**
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public RepositoryNode getParent() throws AccessDeniedException {
		NodeEntity par = getParentNode();
		if (par != null) {
			par.checkPermission(DefaultNodePermission.READ);
		}
		return par;
	}

	/**
	 * 
	 * @param dstUri
	 * @param deep
	 * @return
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * @throws DuplicateException
	 * @throws LockException
	 */
	public RepositoryNode copy(String dstUri, boolean deep)
			throws AccessDeniedException, NotFoundException,
			DuplicateException, LockException {

		return getRepoEntity().copy(getNodeId(), dstUri, deep);
	}

	/**
	 * 
	 * @param dstUri
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * @throws DuplicateException
	 * @throws LockException
	 */
	public void move(String dstUri) throws AccessDeniedException,
			NotFoundException, DuplicateException, LockException {

		getRepoEntity().move(getNodeId(), dstUri);
	}

	/**
	 * 
	 * @param name
	 * @return
	 * @throws AccessDeniedException
	 */
	public String getPropertyValue(QName name) throws AccessDeniedException {
		VersionEntity def = getDefaultVersionEnt();
		return def == null ? null : def.getValue(name);
	}

	/**
	 * 
	 * @param version
	 * @throws AccessDeniedException
	 * @throws NotFoundException
	 * @throws LockException
	 */
	public void setDefaultVersion(NodeVersion version)
			throws AccessDeniedException, NotFoundException, LockException {
		checkPermission(DefaultNodePermission.WRITE);
		checkLocked();

		VersionEntity ver = resolveVersion(version);
		if (ver == null) {
			throw new NotFoundException(version.toString());
		}

		if (!ver.isDefault()) {
			VersionEntity def = getDefaultVersionEnt();
			def.setDefault(false);
			setDefaultVersionEnt(ver);
			HibernateUtil.getSession().update(this);
		}
	}

	/**
	 * 
	 * @param version
	 * @return ver object
	 */
	protected VersionEntity resolveVersion(NodeVersion version) {
		VersionEntity def = getDefaultVersionEnt();
		if (def == null) {
			return null;
		}

		Session session = HibernateUtil.getSession();
		String num = version.getNumber();
		if (!Check.isEmpty(num)) {

			if (num.equals(def.getNumber())) {
				return def;
			}
			Query qry = session.getNamedQuery("Repo.resolveVerNumber");
			qry.setEntity("node", this);
			qry.setString("number", num);
			return (VersionEntity) qry.uniqueResult();
		} else if (!Check.isEmpty(version.getLabel())) {

			if (version.getLabel().equals(def.getLabel())) {
				return def;
			}
			Query qry = session.getNamedQuery("Repo.resolveVerLabel");
			qry.setEntity("node", this);
			qry.setString("label", version.getLabel());
			return (VersionEntity) qry.uniqueResult();
		} else if (version.isDefault()) {

			// label & number both empty, so it must mean the plain-old default
			return def;
		}
		return null;
	}

	/**
	 * 
	 * @param version
	 * @return ver object
	 */
	public VersionEntity createVersion(NodeVersion version) {
		VersionEntity ver = new VersionEntity(version);
		ver.initNew();
		ver.setNodeEntity(this);
		if (Check.isEmpty(ver.getNumber())) {
			ver.setNumber("1.0");
		}
		VersionEntity def = getDefaultVersionEnt();
		Session session = HibernateUtil.getSession();
		if (def == null) {
			setDefaultVersionEnt(ver);
		} else if (ver.isDefault()) {
			def.setDefault(false);
			setDefaultVersionEnt(ver);
		}
		session.persist(ver);
		session.update(this);
		Set<VersionEntity> vers = getVersionEntities();
		if (Hibernate.isInitialized(vers)) {
			vers.add(ver);
		}
		return ver;
	}

	/**
	 * 
	 * @throws LockException
	 * @throws AccessDeniedException
	 */
	protected void checkLocked() throws LockException, AccessDeniedException {
		User usr = SecurityAssociation.getUser();
		if (isExclusiveLocked(usr)) {
			String userName = "unknown";
			try {
				User lockOwner = getExclusiveLock().getOwner();
				User rlockOwner = usr.getUserSpace().resolve(lockOwner);
				userName = rlockOwner != null ? rlockOwner.toString()
						: lockOwner.toString();
			} catch (UserSpaceException e) {
				e.printStackTrace();
			}
			throw new LockException(getUri(), userName);
		}
	}

	/**
	 * 
	 * @param perm
	 * @throws AccessDeniedException
	 */
	protected void checkPermission(int perm) throws AccessDeniedException {
		if (!hasPermission(SecurityAssociation.getUser(), perm)) {
			throw new AccessDeniedException(getUri(), DefaultNodePermission
					.permissionIntToString(perm));
		}
	}

	/**
	 * @return 0 if not deleted, else the unix-time of deletion
	 */
	public long getDeleted() {
		return deleted;
	}

	public void setDeleted(long deleted) {
		this.deleted = deleted;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getUri() {
		if (repoEntity != null) {
			return repoEntity.absoluteUri(localUri);
		}
		return localUri;
	}

	public boolean isCollection() {
		return collection;
	}

	public User getOwner() {
		if (owner == null) {
			owner = new DefaultUser(getOwnerId());
		}
		return owner;
	}

	public Group getContextGroup() {
		if (contextGroup == null) {
			String gid = getContextGroupId();
			contextGroup = Check.isEmpty(gid) ? null : new DefaultGroup(gid);
		}
		return contextGroup;
	}

	public String getParentNodeId() {
		NodeEntity par = getParentNode();
		return par != null ? par.getNodeId() : null;
	}

	public Set<NodeEntity> getChildEntities() {
		return childEntities;
	}

	public void setChildEntities(Set<NodeEntity> childEntities) {
		this.childEntities = childEntities;
	}

	public NodeEntity getParentNode() {
		return parentNode;
	}

	public void setParentNode(NodeEntity parentNode) {
		this.parentNode = parentNode;
	}

	public void setOwner(User owner) {
		this.owner = owner;
		setOwnerId(owner.getUserId());
	}

	public void setCollection(boolean collection) {
		this.collection = collection;
	}

	public String getLocalUri() {
		return localUri;
	}

	public void setLocalUri(String localUri) {
		this.localUri = localUri;
	}

	NodeLock getExclusiveLock() {
		return getExclusiveLock(true);
	}

	protected NodeLock getExclusiveLock(boolean direct) {

		for (LockEntity lock : getLockEntities()) {
			if (lock.isExclusive() && (direct || lock.isInheritable())) {
				return lock;
			}
		}

		// recursive
		NodeEntity parNode = getParentNode();
		return parNode != null ? parNode.getExclusiveLock(false) : null;
	}

	public void setDefaultVersionEnt(VersionEntity defaultVersion) {
		if (defaultVersion != null) {
			defaultVersion.setDefault(true);
		}
		this.defaultVersion = defaultVersion;
	}

	public VersionEntity getDefaultVersionEnt() {
		return defaultVersion;
	}

	public Repository getRepository() throws AccessDeniedException {
		return getRepoEntity();
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final NodeEntity that = (NodeEntity) o;

		return nodeId.equals(that.nodeId);
	}

	@Override
	public int hashCode() {
		return nodeId.hashCode();
	}

	public void pruneVersions(int keepRecentCount)
			throws AccessDeniedException, LockException {
		checkPermission(DefaultNodePermission.WRITE);
		checkLocked();
		int remain = keepRecentCount - 1;
		Session sess = HibernateUtil.getSession();
		Query qry = sess.getNamedQuery("Repo.deleteContent");
		for (VersionEntity ve : getVersionEntities()) {
			if (!ve.isDefault() && remain > 0) {
				remain--;
			} else {
				qry.setEntity("ver", ve);
				qry.executeUpdate();
				sess.delete(ve);
			}
		}
	}

	public void setContextGroup(Group group) throws AccessDeniedException {
		checkPermission(DefaultNodePermission.MANAGE);
		setContextGroupId(group != null ? group.getGroupId() : null);
	}

	/**
	 * 
	 * @param content
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public void setContentKbCnvrt(NodeContent content)
			throws AccessDeniedException, LockException {
		// checkPermission(DefaultNodePermission.WRITE);
		// checkLocked();

		VersionEntity ver = resolveVersion(content.getVersion());
		Set<ContentEntity> conts;
		if (ver == null) {
			ver = createVersion(content.getVersion());
			conts = ver.getContentEntities();
		} else {
			conts = ver.getContentEntities();
		}
		ContentEvent.Type evType = ContentEvent.Type.UPDATE;
		if (conts.isEmpty()) {
			evType = ContentEvent.Type.CREATE;
			ContentEntity cont = new ContentEntity();
			cont.setVersionEntity(ver);
			cont.replaceContent(content);
			conts.add(cont);
			HibernateUtil.getSession().persist(cont);
		} else {
			ContentEntity cont = conts.iterator().next();
			cont.replaceContent(content);
			HibernateUtil.getSession().update(cont);
		}
		HibernateUtil.getSession().flush();
		getRepoEntity().getManager().fireContentChangeEvent(this, content,
				evType);
	}
}
