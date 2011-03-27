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
 * Created on Jan 14, 2004
 *
 */
package itensil.repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.namespace.QName;

import itensil.io.FixedByteArrayOutputStream;
import itensil.io.StreamUtil;
import itensil.io.HibernateUtil;
import itensil.repository.hibernate.NodeEntity;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.util.Check;
import itensil.util.UriHelper;

import org.hibernate.LockMode;

/**
 * @author ggongaware@itensil.com
 * 
 */
public class RepositoryHelper {

	/**
     *
     */
	public static void beginTransaction() {
		HibernateUtil.beginTransaction();
	}

	/**
     *
     */
	public static void useReadOnly() {
		HibernateUtil.readOnlySession();
	}

	/**
	 * 
	 * @return
	 */
	public static boolean isReadOnly() {
		return HibernateUtil.isReadOnlySession();
	}

	/**
     *
     */
	public static void rollbackTransaction() {
		HibernateUtil.rollbackTransaction();
	}

	/**
     *
     */
	public static void commitTransaction() {
		HibernateUtil.commitTransaction();
	}

	/**
     *
     */
	public static void closeSession() {
		HibernateUtil.closeSession();
	}

	public static void releaseNode(RepositoryNode node) {
		HibernateUtil.getSession().lock(node, LockMode.NONE);
	}

	/**
	 * 
	 * @param node
	 */
	public static void saveNode(RepositoryNode node) {
		if (node instanceof NodeEntity) {
			HibernateUtil.getSession().saveOrUpdate(node);
		}
	}

	/**
	 * Find the node for this URI
	 * 
	 * @param uri
	 * @param forUpdate
	 *            - will this node's content/properties be changed? (not
	 *            required but helps locking)
	 * @return Node object
	 * @throws itensil.repository.NotFoundException
	 * @throws itensil.repository.AccessDeniedException
	 */
	public static MutableRepositoryNode getNode(String uri, boolean forUpdate)
			throws NotFoundException, AccessDeniedException {

		return getRepository(uri).getNodeByUri(uri, forUpdate);
	}

	public static InputStream loadContent(String uri)
			throws AccessDeniedException, NotFoundException, LockException {

		Repository repository = getRepository(uri);
		RepositoryNode node = repository.getNodeByUri(uri, false);
		NodeContent content = repository.getContent(node.getNodeId(),
				new DefaultNodeVersion());
		return content.getStream();
	}

	public static InputStream loadContent(MutableRepositoryNode node)
			throws AccessDeniedException, LockException {
		NodeContent content = node.getContent(new DefaultNodeVersion());
		return content.getStream();
	}

	/**
	 * 
	 * @param node
	 * @param in
	 * @param size
	 *            , -1 for auto-size
	 * @param contentType
	 * @throws AccessDeniedException
	 * @throws LockException
	 * @throws IOException
	 */
	public static void createContent(MutableRepositoryNode node,
			InputStream in, int size, String contentType)
			throws AccessDeniedException, LockException, IOException {

		NodeVersion version = new DefaultNodeVersion();
		NodeProperties props = node.getProperties(version);
		if (props != null) {
			version = RepositoryHelper.nextVersion(node, props.getVersion(),
					true);
			props = new DefaultNodeProperties(version, props.getPropertyMap());
		} else {
			props = new DefaultNodeProperties(version);
		}
		NodeContent content;
		if (size >= 0) {
			FixedByteArrayOutputStream out = new FixedByteArrayOutputStream(
					size);
			StreamUtil.copyStream(in, out);
			content = new DefaultNodeContent(out.getBytes(), version);
		} else {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamUtil.copyStream(in, out);
			byte buf[] = out.toByteArray();
			size = buf.length;
			content = new DefaultNodeContent(buf, version);
		}
		PropertyHelper.setStandardProperties(props, node.getUri(), contentType,
				size);
		node.setProperties(props);

		node.setContent(content);
	}


	/**
	 * 
	 * @param node
	 * @param bytes
	 * @throws AccessDeniedException
	 * @throws LockException
	 */
	public static void updateContent(MutableRepositoryNode node, byte bytes[])
			throws AccessDeniedException, LockException {

		NodeVersion version = new DefaultNodeVersion();
		NodeProperties props = node.getProperties(version);
		if (props != null) {
			version = RepositoryHelper.nextVersion(node, props.getVersion(),
					true);
			props = new DefaultNodeProperties(version, props.getPropertyMap());
		} else {
			props = new DefaultNodeProperties(version);
		}
		NodeContent content = new DefaultNodeContent(bytes, version);
		PropertyHelper.setStandardProperties(props, node.getUri(), props
				.getValue(PropertyHelper.defaultQName("getcontenttype")),
				content.getLength());

		node.setProperties(props);
		node.setContent(content);
	}

	public static RepositoryNode copy(String srcUri, String dstUri, boolean deep)
			throws AccessDeniedException, NotFoundException, LockException,
			DuplicateException {

		MutableRepositoryNode node = getNode(srcUri, false);
		dstUri = getAvailableUri(dstUri);
		return node.copy(dstUri, deep);
	}

	public static RepositoryNode copy(Repository srcRepository,
			String srcNodeId, String dstUri, boolean deep)
			throws AccessDeniedException, NotFoundException, LockException,
			DuplicateException {

		Repository dstRepository = getRepository(dstUri);
		if (srcRepository.equals(dstRepository)) {
			return srcRepository.copy(srcNodeId, dstUri, deep);
		} else {
			RepositoryManager manager = srcRepository.getManager();
			return manager.copy(srcRepository, srcNodeId, dstRepository,
					dstUri, deep);
		}
	}

	public static RepositoryNode move(Repository srcRepository,
			String srcNodeId, String dstUri) throws AccessDeniedException,
			NotFoundException, LockException, DuplicateException {

		Repository dstRepository = getRepository(dstUri);
		if (srcRepository.equals(dstRepository)) {
			return srcRepository.move(srcNodeId, dstUri);
		} else {
			RepositoryManager manager = srcRepository.getManager();
			return manager
					.move(srcRepository, srcNodeId, dstRepository, dstUri);
		}
	}

	public static void move(String srcUri, String dstUri)
			throws AccessDeniedException, NotFoundException, LockException,
			DuplicateException {

		MutableRepositoryNode node = getNode(srcUri, true);
		dstUri = getAvailableUri(dstUri);
		node.move(dstUri);
	}

	/**
	 * Find the repository for this URI for this user
	 * 
	 * @param uri
	 * @return respoitory object
	 * @throws itensil.repository.NotFoundException
	 */
	public static Repository getRepository(String uri) throws NotFoundException {

		RepositoryManager man = RepositoryManagerFactory
				.getManager(SecurityAssociation.getUser());
		String mount = UriHelper.getRoot(uri);
		return man.getRepository(mount);
	}

	/**
	 * Find the repository for this URI for this user
	 * 
	 * @param uri
	 * @param user
	 * @return respoitory object
	 * @throws itensil.repository.NotFoundException
	 */
	public static Repository getRepository(String uri, User user)
			throws NotFoundException {
		User preUser = SecurityAssociation.getUser();
		SecurityAssociation.clear();
		User againUser = SecurityAssociation.getUser();
		SecurityAssociation.setUser(user);
		RepositoryManager man = RepositoryManagerFactory.getManager(user);
		String mount = UriHelper.getRoot(uri);
		return man.getRepository(mount);
	}

	/**
	 * @param uri
	 * @return potentially altered name
	 * @throws itensil.repository.NotFoundException
	 * @throws itensil.repository.AccessDeniedException
	 */
	public static String getAvailableUri(String uri) throws NotFoundException,
			AccessDeniedException {

		Repository repository = getRepository(uri);
		int count = 1;
		String availUri = uri;
		try {
			MutableRepositoryNode n = null;
			try {
				n = repository.getNodeByUri(availUri, false);
			} catch (AccessDeniedException ade) {
				// eat it
			}
			MutableRepositoryNode pnode;
			if (n != null) {
				pnode = (MutableRepositoryNode) n.getParent();
			} else {
				String parUri = UriHelper.getParent(availUri);
				pnode = repository.getNodeByUri(parUri, false);
			}
			RepositoryNode kids[] = pnode.getChildren();
			int pos = uri.lastIndexOf('.');
			String simPart = uri;
			if (pos >= 0) {
				simPart = simPart.substring(0, pos);
			}
			HashSet<String> similarUris = new HashSet<String>(kids.length);
			for (RepositoryNode kid : kids) {
				if (kid.getUri().indexOf(simPart) >= 0) {
					similarUris.add(kid.getUri());
				}
			}
			while (similarUris.contains(availUri)) {
				StringBuffer buf = new StringBuffer(uri);
				if (pos >= 0) {
					buf.insert(pos, "(" + count + ")");
				} else {
					buf.append('(');
					buf.append(count);
					buf.append(')');
				}
				availUri = buf.toString();
				count++;
			}
		} catch (NotFoundException nfe) {
			return availUri;
		}
		return availUri;
	}

	/**
	 * Put a lock
	 * 
	 * @param node
	 * @param seconds
	 * @param exclusive
	 * @param deep
	 * @param ownerInfo
	 * @return lock object
	 * @throws itensil.repository.AccessDeniedException
	 * @throws itensil.repository.LockException
	 */
	public static NodeLock lockNode(MutableRepositoryNode node, int seconds,
			boolean exclusive, boolean deep, String ownerInfo)
			throws AccessDeniedException, LockException {

		long time = System.currentTimeMillis() + (seconds * 1000);
		Date expire = new Date(time);
		return node.putLock(SecurityAssociation.getUser(), expire, exclusive,
				deep, ownerInfo);
	}

	/**
	 * Renew a lock from a lockId
	 * 
	 * @param node
	 * @param lockId
	 * @param seconds
	 * @return lock object
	 * @throws itensil.repository.AccessDeniedException
	 */
	public static NodeLock renewLock(MutableRepositoryNode node, String lockId,
			int seconds) throws AccessDeniedException {

		long time = System.currentTimeMillis() + (seconds * 1000);
		Date expire = new Date(time);
		NodeLock lock = new DefaultNodeLock(new DefaultNodeLock(lockId,
				SecurityAssociation.getUser()), expire);
		node.renewLock(lock);
		return lock;
	}

	/**
	 * Generate a unique version number
	 * 
	 * @param node
	 * @param currentVersion
	 * @param setDefault
	 * @return new version
	 * @throws itensil.repository.AccessDeniedException
	 */
	public static NodeVersion nextVersion(MutableRepositoryNode node,
			NodeVersion currentVersion, boolean setDefault)
			throws AccessDeniedException {

		NodeVersion versions[] = node.getVersions();
		HashSet<String> verNums = new HashSet<String>();
		for (NodeVersion version : versions) {
			verNums.add(version.getNumber());
		}
		NodeVersion next = DefaultNodeVersion.nextVersion(currentVersion,
				setDefault);

		// if 1.2 exists go to 1.1.1 then 1.1.2 etc...
		if (verNums.contains(next.getNumber())) {
			String num = currentVersion.getNumber() + ".0";
			next = DefaultNodeVersion.nextVersion(num, setDefault);
			while (verNums.contains(next.getNumber())) {
				next = DefaultNodeVersion.nextVersion(next, setDefault);
			}
		}
		return next;
	}

	/**
	 * Create collection, creates any missing parent collections
	 * 
	 * @param uri
	 * @return
	 * @throws NotFoundException
	 * @throws LockException
	 * @throws DuplicateException
	 * @throws AccessDeniedException
	 */
	public static MutableRepositoryNode createCollection(String uri)
			throws NotFoundException, AccessDeniedException,
			DuplicateException, LockException {

		Repository repo = RepositoryHelper.getRepository(uri);
		ArrayList<String> crPars = new ArrayList<String>();
		String root = repo.getMount();
		boolean exists = false;
		String pUri = UriHelper.getParent(uri);
		while (!exists && !root.equals(pUri)) {
			try {
				repo.getNodeByUri(pUri, false);
				exists = true;
			} catch (NotFoundException nfe) {
				crPars.add(pUri);
				pUri = UriHelper.getParent(pUri);
			}
		}
		for (int ii = crPars.size() - 1; ii >= 0; ii--) {
			repo
					.createNode(crPars.get(ii), true, SecurityAssociation
							.getUser());
		}
		return repo.createNode(uri, true, SecurityAssociation.getUser());
	}

	public static Repository getPrimaryRepository() {
		RepositoryManager man = RepositoryManagerFactory
				.getManager(SecurityAssociation.getUser());
		return man.getPrimaryRepository();
	}

	/**
	 * Maps /home/ to /{primary}/
	 * 
	 * @param uri
	 * @return
	 */
	public static String resolveUri(String uri) {
		if (uri != null && uri.startsWith("/home")) {
			if (uri.length() == 5) {
				return getPrimaryRepository().getMount();
			} else if (uri.charAt(5) == '/') {
				return getPrimaryRepository().getMount() + uri.substring(5);
			}
		}
		return uri;
	}

	public static MutableRepositoryNode copyAndUpdate(String dstUri,
			String srcUri, User usr, String description)
			throws NotFoundException, AccessDeniedException,
			DuplicateException, LockException {
		dstUri = RepositoryHelper.resolveUri(dstUri);
		srcUri = RepositoryHelper.resolveUri(srcUri);
		MutableRepositoryNode node = getNode(srcUri, false);
		String cpUri = RepositoryHelper.getAvailableUri(dstUri);
		String parUri = UriHelper.getParent(cpUri);
		try {
			RepositoryHelper.getNode(parUri, false);
		} catch (NotFoundException nfe) {
			RepositoryHelper.createCollection(parUri);
		}
		MutableRepositoryNode resNode = (MutableRepositoryNode) node.copy(
				cpUri, true);

		HashMap<QName, String> props = new HashMap<QName, String>();
		// description included?
		if (!Check.isEmpty(description)) {
			props.put(PropertyHelper.itensilQName("description"), description);
		}
		props.put(PropertyHelper.defaultQName("getlastmodified"),
				PropertyHelper.dateString(new Date()));
		props.put(PropertyHelper.itensilQName("modifier"), usr.getUserId());
		PropertyHelper.setNodeValues(resNode, props);
		return resNode;
	}

	/**
	 * Return the better node permission
	 * 
	 * @param perm1
	 * @param perm2
	 * @return perm object
	 */
	public static NodePermission betterPermission(NodePermission perm1,
			NodePermission perm2) {

		if (perm1.canManage() || perm2.canManage()) {
			return perm1.canManage() ? perm1 : perm2;
		}
		if (perm1.canWrite() || perm2.canWrite()) {
			return perm1.canWrite() ? perm1 : perm2;
		}
		if (perm1.canCreate() || perm2.canCreate()) {
			return perm1.canCreate() ? perm1 : perm2;
		}
		if (perm1.canRead() || perm2.canRead()) {
			return perm1.canRead() ? perm1 : perm2;
		}
		return perm1;
	}

	/**
	 * 
	 * @param id
	 * @param forUpdate
	 *            - will this node's content/properties be changed? (not
	 *            required but helps locking)
	 * @return
	 * @throws NotFoundException
	 * @throws AccessDeniedException
	 */
	public static MutableRepositoryNode getNodeById(String id, boolean forUpdate)
			throws NotFoundException, AccessDeniedException {
		RepositoryManager man = RepositoryManagerFactory
				.getManager(SecurityAssociation.getUser());
		Repository repo = man.getRepositoryByNode(id);
		return repo.getNode(id, forUpdate);
	}

	/**
	 * 
	 * @param node
	 * @param in
	 * @param size
	 *            , -1 for auto-size
	 * @param contentType
	 * @throws AccessDeniedException
	 * @throws LockException
	 * @throws IOException
	 */
	public static void createContentKbCnvrt(NodeEntity node,
	// MutableRepositoryNode node,
			InputStream in, int size, String contentType)
			throws AccessDeniedException, LockException, IOException {

		NodeVersion version = new DefaultNodeVersion();
		NodeProperties props = node.getProperties(version);
		if (props != null) {
			version = RepositoryHelper.nextVersion(node, props.getVersion(),
					true);
			props = new DefaultNodeProperties(version, props.getPropertyMap());
		} else {
			props = new DefaultNodeProperties(version);
		}
		NodeContent content;
		if (size >= 0) {
			FixedByteArrayOutputStream out = new FixedByteArrayOutputStream(
					size);
			StreamUtil.copyStream(in, out);
			content = new DefaultNodeContent(out.getBytes(), version);
		} else {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamUtil.copyStream(in, out);
			byte buf[] = out.toByteArray();
			size = buf.length;
			content = new DefaultNodeContent(buf, version);
		}
		PropertyHelper.setStandardProperties(props, node.getUri(), contentType,
				size);
		node.setPropertiesKbCnvrt(props);

		node.setContentKbCnvrt(content);
	}

}
