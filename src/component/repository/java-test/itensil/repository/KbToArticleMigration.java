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
package itensil.repository;

import itensil.io.HibernateUtil;
import itensil.io.xml.XMLDocument;
import itensil.io.xml.XMLWriter;
import itensil.repository.hibernate.NodeEntity;
import itensil.security.Group;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.security.hibernate.UserEntity;
import itensil.util.UriHelper;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.ObjectNotFoundException;
import org.mozilla.javascript.Context;

/**
 *
 */
public class KbToArticleMigration extends TestCase {

	static Logger log = Logger.getLogger(KbToArticleMigration.class);
	static final boolean DEBUG = true;
	static final String URI_PAT = "kb";
	static final String GET_NODES_LIKE_URI = "select id, ownerId from iten_repo_node where deleted=0 and collection =0 and uri like '%kb%' order by ownerId";

	// Repository repository;
	String[] baseUris = { "/debug1/Community.kb" };
	// String[] baseUris = {"/system/Community.kb"};
	String mount = "/junit";
	User user;
	User user2;
	Group group1;

	public void migrateKb() throws Exception {

		// 1. initial

		// 2. go through repository nodes and get list of all nodeId that
		// contain *.kb file name.

		List<VO> nodesList = getNodesLikeUri(URI_PAT);

		if (nodesList == null || nodesList.isEmpty()) {
			assertFalse("nodes not found", false);
			return;
		}

		/*
		 * select from iten_repo_node where deleted=0 and collection =0 and uri
		 * like '%kb%' ;
		 */

		// 3. begin transaction
		HibernateUtil.beginTransaction();

		Repository repository;
		NodeEntity nodeToMod;
		int nodeCnt = 0;
		int articleCnt = 0;

		// . for each node in list
		for (VO vo : nodesList) {
			nodeCnt++;
			// a. set authenticated user as node ownerId
			// so that node revisioning is kept with the current ownerId
			String ownerId = vo.ownerId;

			SecurityAssociation.clear();

			UserEntity user = (UserEntity) HibernateUtil.getSession().get(
					UserEntity.class, ownerId);

			SecurityAssociation.setUser(user);

			// b. now get node to be modified in proper user context
			String id = vo.id;
			try {
				nodeToMod = (NodeEntity) RepositoryHelper.getNodeById(id, true);
			} catch (ObjectNotFoundException onfe) {
				log.warn("node not found not found for nodeId: " + id);
				log.warn(onfe.getStackTrace());

				continue;
			}

			// c. something wrong node is already a collection quit
			if (nodeToMod.isCollection()) {
				assertFalse("nodeToMod is already a collection nodeId:"
						+ nodeToMod.getNodeId(), false);
				continue;
			}

			// cc. if can't find a mount for the user/uri combo log it and try
			// next node. don't change node
			try {
				RepositoryHelper.getRepository(nodeToMod.getUri());
			} catch (NotFoundException nfe) {
				log.warn("mount not found for nodeUri: " + nodeToMod.getUri()
						+ " nodeId: " + nodeToMod.getNodeId() + "nodeOnwerId: "
						+ nodeToMod.getOwnerId());
				log.warn(nfe.getStackTrace());
				continue;
			}

			// d. setup next revision of nodeToMod
			NodeVersion version = new DefaultNodeVersion();
			NodeProperties props = nodeToMod.getProperties(version);
			// now that each article will be a file (nodes themselves)

			// e. get node content in doc form
			Document doc = XMLDocument.readStream(RepositoryHelper
					.loadContent(nodeToMod));

			// f. get element root name
			Element KbRoot = doc.getRootElement();
			String kbName = KbRoot.getName();

			// g. should be knowledgebase (parent-to-articles)
			// if root not knowledgebase let do something not sure what
			if (!"knowledgebase".equalsIgnoreCase(KbRoot.getName())) {
				processNonKbRoot(nodeToMod, doc);
				continue;
			}
			// if root is knowledgebase then we need to process all the article
			// elements and
			// make each of them a child node(non collection)

			// if kb then get the version from the properties
			if (props != null) {
				version = props.getVersion();
			}

			// incremet parent node version to save new kb children nodes
			// below it(articles per file)
			version = RepositoryHelper.nextVersion(nodeToMod, version, version
					.isDefault());
			// this current nodeToMod will be a collection (of article
			// Nodes)
			nodeToMod.setCollection(true);

			// leave original node content (pre-kb-migration) here.
			// can delete content later
			// parent nodes properties will not be changed except for new
			// e-tag
			String etag = PropertyHelper.makeEtag(nodeToMod.getUri(), version
					.getNumber(), props.getValue("getlastmodified"), 0);
			props.setValue("getetag", etag);

			// Override permissions for set properties
			nodeToMod.setProperties(props);

			// update node with new version number
			RepositoryHelper.saveNode(nodeToMod);

			// h. process each article element in the doc making new one new
			// child node per article
			// identify each article within the node
			for (Element elem : (List<Element>) doc.getRootElement().elements(
					"article")) {
				articleCnt++;

				// Begin generation of unique article files
				// 1. collect article attributes
				String articleId = elem.attributeValue("id");
				// String articleId =
				// XMLWriter.encode(elem.attributeValue("id"));
				String refId = elem.attributeValue("refId");
				String createTime = elem.attributeValue("createTime");
				String createBy = elem.attributeValue("createBy");
				String modifyTime = elem.attributeValue("modifyTime");
				String modifyBy = elem.attributeValue("modifyBy");
				String layout = elem.attributeValue("layout");
				String articleText = elem.getText();

				// TODO special chars like trademark
				// ??? need to clean this encode up a bit for special chars like
				// trademark
				String articleTextEncoded = XMLWriter.encode(articleText);

				// contentType does not exist as an attribute
				String contentType = elem.attributeValue("getcontentType");

				// ??? not sure what contentType should be set to if null
				// maybe
				// if(contentType == null) contentType="article+xml";

				// 2. Generate unique filename per article
				// filter out win32 incompatible chars
				String articleTitle = UriHelper.filterName(articleId);

				// 3. build content hack
				StringBuilder articleXml = new StringBuilder();
				articleXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				articleXml.append("<article");
				articleXml.append(" id=\"" + XMLWriter.encode(articleTitle)
						+ ".art\"");
				articleXml.append(" layout=\"" + layout + "\"");
				articleXml.append(">");
				articleXml.append("<articleText>");
				articleXml.append(articleTextEncoded);
				articleXml.append("</articleText>");
				articleXml.append("</article>");
				byte[] articleXmlBytes = articleXml.toString().getBytes();

				// 4. build child uri
				// ??? should it be getLocalUri or getUri
				String childUri = nodeToMod.getUri() + "/" + articleTitle
						+ ".art";

				// 5. build child version and properties
				UserEntity fromUser = (UserEntity) HibernateUtil.getSession()
						.get(UserEntity.class, nodeToMod.getOwnerId());

				// new node will hold metadata for individual article
				NodeEntity childNode = (NodeEntity) createFile(childUri,
						fromUser);

				NodeVersion childVersion = new DefaultNodeVersion();
				NodeProperties childProps = new DefaultNodeProperties(
						childVersion);

				childProps.setValue("getlastmodified", modifyTime);
				childProps.setValue("getcontentlength", Integer
						.toString(articleXmlBytes.length));

				String articleETag = PropertyHelper.makeEtag(
						nodeToMod.getUri(), version.getNumber(), props
								.getValue("getlastmodified"), 0);
				childProps.setValue("getetag", articleETag);

				// save metadata in node
				childNode.setProperties(childProps);
				// nodeToMod.setProperties(childProps);

				String readbackUri = childNode.getUri();

				// put file text in child node context object
				// SecurityAssociation.setUser(sysAdminUser);
				loadBytes(childUri, articleXmlBytes);

				log.warn("***************** nodeCnt: " + nodeCnt
						+ "articleCnt: " + articleCnt);
				log.warn("child node created: uri: " + childUri
						+ " readbackUri: " + readbackUri + "childNodeId: "
						+ childNode.getNodeId() + "parentNodeId: "
						+ nodeToMod.getNodeId());
			}

		}
		HibernateUtil.commitTransaction();
		HibernateUtil.closeSession();

	}

	public void processNonKbRoot(NodeEntity node, Document doc) {
		log.warn("processNonKbRoot nodeId: " + node.getNodeId()
				+ " docRootName: " + doc.getRootElement().getName());
	}

	/**
	 * Constructor for RepositoryJunit.
	 * 
	 * @param s
	 */
	public KbToArticleMigration(String s) {
		super(s);
	}

	public MutableRepositoryNode createFile(String uri, User parentNodeOwner)
			throws Exception {
		try {
			uri = resolveUri(uri);
			Repository repo = RepositoryHelper.getRepository(uri);
			// parentNodeOwner);
			return repo.createNode(uri, false, parentNodeOwner);
		} catch (Exception e) {
			throw new Exception(e);
		}

		// return repo.createNode(uri, false, SecurityAssociation.getUser());
	}

	public void loadBytes(String articleUri, Object jsByteArray)
			throws Exception {

		byte data[] = (byte[]) Context.jsToJava(jsByteArray, Object.class);
		articleUri = resolveUri(articleUri);
		NodeEntity repoNode = (NodeEntity) RepositoryHelper.getNode(articleUri,
				true);
		// MutableRepositoryNode repoNode = RepositoryHelper.getNode(articleUri,
		// true);

		RepositoryHelper.createContentKbCnvrt(repoNode,
				new ByteArrayInputStream(data), data.length,
				RepositoryManagerFactory.getMimeType(articleUri));
	}

	protected String resolveUri(String uri) {
		String path = "";
		String res = UriHelper.absoluteUri(path, uri);
		return RepositoryHelper.resolveUri(res);
	}

	/**
	 * 
	 * @param uri
	 * @return List(NodeEntity) object
	 * @throws NotFoundException
	 */
	@SuppressWarnings("deprecation")
	public List getNodesLikeUri(String uriPat) throws AccessDeniedException,
			NotFoundException {
		int count = 0;
		ArrayList list = new ArrayList();
		Connection conn;
		Statement stmt;
		ResultSet rs;

		try {
			conn = HibernateUtil.getSession().connection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(GET_NODES_LIKE_URI);

			// Populating value objects from a result set.

			while (rs.next()) {
				count++;
				String id = rs.getString("id");
				String ownerId = rs.getString("ownerId");
				VO vo = new VO(id, ownerId);
				list.add(vo);
			}
			rs.close();
			stmt.close();
			conn.close();

		} catch (Exception e) {
			return null;
		} finally {
			// Releasing JDBC resources.

		}
		return list;
	}

	private class VO {
		public String id;
		public String ownerId;

		public VO(String nodeId, String nodeOwnerId) {
			id = nodeId;
			ownerId = nodeOwnerId;
		}

	}

}
