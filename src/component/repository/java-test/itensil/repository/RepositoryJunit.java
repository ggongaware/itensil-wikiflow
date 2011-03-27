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

import java.util.*;

import javax.xml.namespace.QName;

import org.hibernate.Query;


import junit.framework.TestCase;

//import itensil.auth.signon.SignOnRemoteBD;
import itensil.repository.search.*;
import itensil.security.*;
import itensil.uidgen.*;
import itensil.util.Pair;
import itensil.util.StringHelper;
import itensil.io.HibernateUtil;


/**
 * @author ggongaware@itensil.com
 *
 */
public class RepositoryJunit extends TestCase {

    //Repository repository;
    String mount = "/junit";
    User user;
    User user2;
    Group group1;

    /**
     * Constructor for RepositoryJunit.
     * @param s
     */
    public RepositoryJunit(String s) {
        super(s);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RepositoryJunit.class);
    }

    public void testCreateRepository() throws Exception {

        SecurityAssociation.setUser(SysAdmin.getUser());
        

        // pre-clean
        HibernateUtil.beginTransaction();
        Query qry = HibernateUtil.getSession().createQuery(
        		"DELETE FROM Mount mnt WHERE mnt.userSpaceId = :userSpaceId");
        qry.setString("userSpaceId", user.getUserSpaceId());
        qry.executeUpdate();
        
        qry = HibernateUtil.getSession().createQuery(
				"UPDATE FROM RepositoryEntity repo SET repo.mount = :newMount WHERE repo.mount = :mount");
		qry.setString("mount", mount);
		qry.setString("newMount", mount + (new IUIDGenerator()).createID().toString());
		qry.executeUpdate();
        
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        RepositoryManager repoMan = RepositoryManagerFactory.getManager(user);
        try {
            repoMan.createRepository(mount, user, null, null, null, null);
            repoMan.addRepositoryMount(mount, true);
        } catch (DuplicateException dupe) {
            System.out.println(dupe.getMessage());
        }
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        assertEquals(mount, repoMan.getPrimaryRepository().getMount());
        HibernateUtil.commitTransaction();
    }

    public void testCreateNode() throws Exception {

        SecurityAssociation.setUser(user);


        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create node
        RepositoryNode node =
            repository.createNode(mount + "/test.txt", false, user);
        HibernateUtil.commitTransaction();

        assertNotNull(node.getNodeId());

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        node = repository.getNodeByUri(mount + "/test.txt", false);
        assertNotNull(node.getNodeId());
        assertEquals(node.getUri(), mount + "/test.txt");
        assertEquals(node.getOwner(), user);
        assertFalse(node.isCollection());
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        // remove node
        repository.removeNode(node.getNodeId());
        HibernateUtil.commitTransaction();
    }

    public void testReadNode() throws Exception {

        SecurityAssociation.setUser(user);

        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create node
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);
        byte [] bytes = "Hello world".getBytes();

        // set content
        DefaultNodeContent origContent =
            new DefaultNodeContent(bytes, new DefaultNodeVersion());
        node.setContent(origContent);
        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();

        node = (MutableRepositoryNode)repository.getNodeByUri(mount + "/test.txt", false);

        // check content
        NodeContent content = node.getContent(new DefaultNodeVersion());
        assertEquals(bytes.length, content.getLength());
        assertTrue(Arrays.equals(bytes, content.getBytes()));
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        // remove node
        node.remove();
        HibernateUtil.commitTransaction();
    }



    public void testVersions() throws Exception {

        SecurityAssociation.setUser(user);

        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create node
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);
        byte [] bytes = "Hello world".getBytes();

        // set content
        DefaultNodeContent origContent =
            new DefaultNodeContent(bytes, new DefaultNodeVersion());
        node.setContent(origContent);
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        NodeContent contentVer = node.getContent(new DefaultNodeVersion());
        NodeVersion origVersion = contentVer.getVersion();
        NodeVersion nextVersion = RepositoryHelper.nextVersion(node, contentVer.getVersion(), true);
        byte [] bytes2 = "Hello universe".getBytes();
        contentVer = new DefaultNodeContent(bytes2, nextVersion);
        node.setContent(contentVer);
        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();

        node = (MutableRepositoryNode)repository.getNodeByUri(mount + "/test.txt", false);

        // check content
        NodeContent content = node.getContent(new DefaultNodeVersion());
        assertEquals(bytes2.length, content.getLength());
        assertTrue(Arrays.equals(bytes2, content.getBytes()));

        content = node.getContent(origVersion);
        assertEquals(bytes.length, content.getLength());
        assertTrue(Arrays.equals(bytes, content.getBytes()));
        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        node = (MutableRepositoryNode)repository.getNodeByUri(mount + "/test.txt", true);
        node.setDefaultVersion(origVersion);
        content = node.getContent(new DefaultNodeVersion());
        assertEquals(bytes.length, content.getLength());
        assertTrue(Arrays.equals(bytes, content.getBytes()));
        HibernateUtil.commitTransaction();

        HibernateUtil.beginTransaction();
        // remove node
        node.remove();
        HibernateUtil.commitTransaction();
    }

    public void testProperties() throws Exception {

        SecurityAssociation.setUser(user);

        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);
        // create node
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);

        // set properties
        DefaultNodeProperties origProps =
            new DefaultNodeProperties(new DefaultNodeVersion());
        origProps.setValue("content-type", "text/plain");
        origProps.setValue("lang", "en");
        origProps.setValue("null", "null");
        assertEquals(3, origProps.getNames().length);
        node.setProperties(origProps);

        // check properties
        NodeProperties props = node.getProperties(new DefaultNodeVersion());
        assertEquals(3, props.getNames().length);
        assertEquals("text/plain", props.getValue("content-type"));
        assertEquals("en", props.getValue("lang"));
        assertEquals("null", props.getValue("null"));
        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        node = (MutableRepositoryNode)repository.getNodeByUri(mount + "/test.txt", false);
        // re-check properties
        props = node.getProperties(new DefaultNodeVersion());
        assertEquals(3, props.getNames().length);
        assertEquals("text/plain", props.getValue("content-type"));
        assertEquals("en", props.getValue("lang"));
        assertEquals("null", props.getValue("null"));

        // remove node
        node.remove();
        HibernateUtil.commitTransaction();
    }



    public void testPermissions() throws Exception {

        SecurityAssociation.setUser(user);

        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create node
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);

        DefaultUser duser =
            new DefaultUser("AAAAAAAAAAAAAAAAAAAD", "junitD");

        // grant permission
        DefaultNodePermission origPerm =
            new DefaultNodePermission(duser, DefaultNodePermission.WRITE, true);

        assertTrue(origPerm.canRead());
        assertTrue(origPerm.canCreate());
        assertTrue(origPerm.canWrite());
        assertTrue(origPerm.isInheritable());
        assertFalse(origPerm.isNone());
        assertEquals(duser, origPerm.getPrincipal());
        node.grantPermission(origPerm);

        // check permission
        NodePermission perms[] = node.getPermissions();
        assertEquals(1, perms.length);
        assertTrue(perms[0].canRead());
        assertTrue(perms[0].canCreate());
        assertTrue(perms[0].canWrite());
        assertTrue(perms[0].isInheritable());
        assertFalse(perms[0].isNone());
        assertEquals(duser, perms[0].getPrincipal());
        assertTrue(
            node.hasPermission(DefaultNodePermission.writePermission(duser)));

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        node = (MutableRepositoryNode)repository.getNodeByUri(mount + "/test.txt", true);

        // re-check
        perms = node.getPermissions();
        assertEquals(1, perms.length);
        assertTrue(perms[0].canRead());
        assertTrue(perms[0].canCreate());
        assertTrue(perms[0].canWrite());
        assertTrue(perms[0].isInheritable());
        assertFalse(perms[0].isNone());
        assertEquals(duser, perms[0].getPrincipal());
        assertTrue(
            node.hasPermission(DefaultNodePermission.writePermission(duser)));


        // revoke permission
        node.revokePermission(origPerm);
        perms = node.getPermissions();
        assertEquals(0, perms.length);
        assertFalse(
            node.hasPermission(DefaultNodePermission.writePermission(duser)));


        // remove node
        node.remove();
        HibernateUtil.commitTransaction();
    }


    public void testPermissionNoInherit() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // Test Inherit
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child", false, user);

        DefaultNodePermission perm =
            new DefaultNodePermission(user2, DefaultNodePermission.READ, false);

        parentNode.grantPermission(perm);

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(user2);
        parentNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parent", true);

        assertEquals(0, parentNode.getChildren().length);

        try {
            childNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parent/child", true);
            fail();
        } catch (AccessDeniedException ade) {
            assertEquals(mount + "/parent/child", ade.getId());
        }

        SecurityAssociation.setUser(user);
        childNode = repository.getNode(childNode.getNodeId(), true);

        // remove nodes
        childNode.remove();

        assertEquals(0, parentNode.getChildren().length);

        parentNode.remove();
        HibernateUtil.commitTransaction();
    }
    
    public void testPermissionRelative() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // Test Inherit
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parentRel", true, user);

        parentNode.setContextGroup(group1);
        
        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parentRel/childRel", false, user);


        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
        HibernateUtil.beginTransaction();
        
        SecurityAssociation.setUser(user2);
        
        try {
            childNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parentRel/childRel", true);
            fail();
        } catch (AccessDeniedException ade) {
            assertEquals(mount + "/parentRel/childRel", ade.getId());
        }
        
        SecurityAssociation.setUser(user);
        parentNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parentRel", true);
        
        
        DefaultNodePermission perm =
            new DefaultNodePermission(
            		new RelativeGroup(), DefaultNodePermission.READ, true, 
            		GroupAxis.ANCESTOR_OR_SELF, "pitcher");

        parentNode.grantPermission(perm);
        
        HibernateUtil.commitTransaction();
        HibernateUtil.closeSession();
        HibernateUtil.beginTransaction();
        
        parentNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parentRel", true);
        
        SecurityAssociation.setUser(user2);

        try {
            childNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parentRel/childRel", true);
        } catch (AccessDeniedException ade) {
        	fail();
            assertEquals(mount + "/parentRel/childRel", ade.getId());
        }

        SecurityAssociation.setUser(user);
        childNode = repository.getNode(childNode.getNodeId(), true);

        // remove nodes
        childNode.remove();

        assertEquals(0, parentNode.getChildren().length);

        parentNode.remove();
        HibernateUtil.commitTransaction();
    }


   public void testSecurity() throws Exception {

        SecurityAssociation.setUser(user);

        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create node
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);

        // Test read
        SecurityAssociation.setUser(user2);
        try {
            repository.getVersions(node.getNodeId());
            fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof AccessDeniedException);
            AccessDeniedException ade = (AccessDeniedException)ex;
            assertEquals(ade.getId(), node.getNodeId());
        }

        // remove node
        SecurityAssociation.setUser(user);
        node.remove();


        // Test Inherit
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child", false, user);

        DefaultNodeProperties origProps =
            new DefaultNodeProperties(new DefaultNodeVersion());
        origProps.setValue("lang", "en");
        childNode.setProperties(origProps);

        DefaultNodePermission perm =
            new DefaultNodePermission(user2, DefaultNodePermission.READ, true);

        parentNode.grantPermission(perm);

        SecurityAssociation.setUser(user2);
        NodeProperties props = childNode.getProperties(new DefaultNodeVersion());
        assertEquals(props.getValue("lang"), "en");
        try {
            props.setValue("deny", "deny this");
            childNode.setProperties(props);
            fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof AccessDeniedException);
            AccessDeniedException ade = (AccessDeniedException)ex;
            assertEquals(childNode.getUri(), ade.getId());
        }


        SecurityAssociation.setUser(user);
        props = childNode.getProperties(new DefaultNodeVersion());
        assertNull(props.getValue("deny"));

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        childNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parent/child", true);

        // re-check
        SecurityAssociation.setUser(user2);
        props = childNode.getProperties(new DefaultNodeVersion());
        assertEquals("en", props.getValue("lang"));
        assertEquals(mount + "/parent", childNode.getParent().getUri());

        SecurityAssociation.setUser(user);

        // remove nodes
        childNode.remove();
        parentNode = repository.getNode(parentNode.getNodeId(), true);
        parentNode.remove();
        HibernateUtil.commitTransaction();
    }


    public void testSecuritySub() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // Test Inherit
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child", false, user);

        DefaultNodePermission perm =
            new DefaultNodePermission(user2, DefaultNodePermission.READ, true);

        childNode.grantPermission(perm);

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(user2);

        childNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parent/child", true);

        try {
            childNode.getParent();
            fail();
        } catch (AccessDeniedException ade) {
            assertEquals(mount + "/parent", ade.getId());
        }

        SecurityAssociation.setUser(user);

        // remove nodes
        childNode.remove();
        parentNode = repository.getNode(parentNode.getNodeId(), true);
        parentNode.remove();
        HibernateUtil.commitTransaction();
    }

    public void testCopy() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // seed nodes
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child.txt", false, user);

        byte [] bytes = "Hello world".getBytes();

        // properties
        DefaultNodeProperties origProps =
            new DefaultNodeProperties(new DefaultNodeVersion());
        origProps.setValue("content-type", "text/plain");
        childNode.setProperties(origProps);

        // set content
        DefaultNodeContent origContent =
            new DefaultNodeContent(bytes, new DefaultNodeVersion());
        childNode.setContent(origContent);


        parentNode.copy(mount + "/parentCopy", true);

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();

        MutableRepositoryNode parentCopyNode =
            (MutableRepositoryNode)
                repository.getNodeByUri(mount + "/parentCopy", true);

        assertTrue(parentCopyNode.isCollection());
        RepositoryNode kids[] = parentCopyNode.getChildren();
        assertEquals(kids.length, 1);
        assertEquals(mount + "/parentCopy/child.txt", kids[0].getUri());
        NodeContent cont =
            repository.getContent(
                kids[0].getNodeId(), new DefaultNodeVersion());
        assertTrue(Arrays.equals(bytes, cont.getBytes()));
        NodeProperties props = repository.getProperties(
            kids[0].getNodeId(), new DefaultNodeVersion());
        assertEquals(props.getValue("content-type"), "text/plain");

        // clean up
        parentCopyNode.remove();
        parentNode.remove();
        HibernateUtil.commitTransaction();
    }

    public void testMove() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // seed nodes
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        //MutableRepositoryNode childNode =
        //    (MutableRepositoryNode)
        repository.createNode(mount + "/parent/child", false, user);

        parentNode.move(mount + "/parentMove");

        HibernateUtil.commitTransaction();

        HibernateUtil.closeSession();

        HibernateUtil.beginTransaction();

        assertEquals(parentNode.getUri(), mount + "/parentMove");

        parentNode = (MutableRepositoryNode)repository.getNodeByUri(mount + "/parentMove", true);

        assertTrue(parentNode.isCollection());
        RepositoryNode kids[] = parentNode.getChildren();
        assertEquals(kids.length, 1);
        assertEquals(mount + "/parentMove/child", kids[0].getUri());

        // clean up
        parentNode.remove();
        HibernateUtil.commitTransaction();
    }


    public void testLock() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        long day = 8640000;
        long minute = 60000;

        // seed nodes
        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child", false, user);

        DefaultNodePermission perm =
            new DefaultNodePermission(user2, DefaultNodePermission.READ, true);
        parentNode.grantPermission(perm);


        byte [] bytes = "Hello world".getBytes();

        // set content
        DefaultNodeContent content =
            new DefaultNodeContent(bytes, new DefaultNodeVersion());

        // test shared lock
        assertEquals(childNode.getLocks().length, 0);
        NodeLock lock =
            childNode.putLock(
                user,
                new Date(System.currentTimeMillis() + day),
                false,
                false,
                "");
        assertEquals(lock.getOwner(), user);
        assertFalse(lock.isExclusive());
        assertFalse(lock.isInheritable());
        NodeLock locks[] = childNode.getLocks();
        assertEquals(locks.length, 1);
        assertEquals(locks[0], lock);
        assertEquals(locks[0].getOwner(), user);
        assertFalse(locks[0].isExclusive());
        assertFalse(locks[0].isInheritable());
        childNode.killLock(lock);
        assertEquals(0, childNode.getLocks().length);

        // test lock expire
        //lock =
            childNode.putLock(
                user,
                new Date(System.currentTimeMillis() + 4000),
                false,
                false,
                "");

        Thread.sleep(5001);
        assertEquals(0, childNode.getLocks().length);

        // test exclusive lock
        lock =
            childNode.putLock(
                user,
                new Date(System.currentTimeMillis() + minute),
                true,
                false,
                "");
        assertEquals(lock.getOwner(), user);
        assertTrue(lock.isExclusive());
        locks = childNode.getLocks();
        assertEquals(locks.length, 1);
        assertEquals(locks[0], lock);
        assertEquals(locks[0].getOwner(), user);
        assertTrue(locks[0].isExclusive());

        // ok is lock owner
        childNode.setContent(content);
        SecurityAssociation.setUser(user2);
        try {
            childNode.getContent(new DefaultNodeVersion());
            fail();
        } catch (LockException le) {
            // ok content locked
        }
        SecurityAssociation.setUser(user);
        childNode.killLock(lock);
        SecurityAssociation.setUser(user2);
        childNode.getContent(new DefaultNodeVersion());
        SecurityAssociation.setUser(user);

        // test recursive exclusive lock
        lock =
            parentNode.putLock(
                user,
                new Date(System.currentTimeMillis() + minute),
                true,
                false,
                "");
        assertEquals(parentNode.getLocks().length, 1);

        // my own lock
        assertFalse(parentNode.isExclusiveLocked(user));

        // not recursive ok
        SecurityAssociation.setUser(user2);
        assertTrue(parentNode.isExclusiveLocked(user2));
        childNode.getContent(new DefaultNodeVersion());
        assertFalse(childNode.isExclusiveLocked(user2));

        SecurityAssociation.setUser(user);
        parentNode.killLock(lock);

        //lock =
            parentNode.putLock(
                user,
                new Date(System.currentTimeMillis() + minute),
                true,
                true,
                "");
        assertEquals(parentNode.getLocks().length, 1);


        SecurityAssociation.setUser(user2);
        assertTrue(parentNode.isExclusiveLocked(user2));
        try {
            childNode.getContent(new DefaultNodeVersion());
            fail();
        } catch (LockException le) {
            // ok content recursive locked
        }
        assertTrue(childNode.isExclusiveLocked(user2));
        SecurityAssociation.setUser(user);

        // clean up
        parentNode.remove();

        HibernateUtil.commitTransaction();
    }
    
    public void testGetLastModified() throws Exception {
    	
    	   SecurityAssociation.setUser(user);
           HibernateUtil.beginTransaction();
           Repository repository = RepositoryHelper.getRepository(mount);
           
           Query qry = HibernateUtil.getSession().getNamedQuery("Repo.recentlyModifiedNodes");
    	   qry.setEntity("repo", repository);
    	   qry.setString("uriPat", "%.txt");
    	   
    	   List nodes = qry.list();
    	   assertNotNull(nodes);
    	   
           HibernateUtil.commitTransaction();
    }
    
    public void testBasicSearchRequestReader() throws Exception {
        String request =
            "<d:searchrequest xmlns:d=\"DAV:\"" +
            " xmlns:ir=\"http://itensil.com/repository\">" +
            "    <d:basicsearch>" +
            "        <d:select>" +
            "            <d:prop><d:displayname/></d:prop>" +
            "            <d:prop><d:getlastmodified/></d:prop>" +
            "            <d:prop><d:getcontenttype/></d:prop>" +
            "            <d:prop><ir:description/></d:prop>" +
            "        </d:select>" +
            "        <d:from>" +
            "            <d:scope>" +
            "                <d:href>" + mount + "</d:href>" +
            "                <d:depth>infinity</d:depth>" +
            "            </d:scope>" +
            "        </d:from>" +
            "        <d:where>" +
            "            <d:and>" +
            "                <d:like>" +
            "                    <d:prop><ir:description/></d:prop>" +
            "                    <d:literal>%hello%</d:literal>" +
            "                </d:like>" +
            "                <d:not>" +
            "                    <d:is-collection/>" +
            "                </d:not>" +
            "            </d:and>" +
            "        </d:where>" +
            "        <d:orderby>" +
            "            <d:order>" +
            "                <d:prop><d:getlastmodified/></d:prop>" +
            "                <d:descending/>" +
            "           </d:order>" +
            "        </d:orderby>" +
            "        <d:limit>" +
            "            <d:nresults>10</d:nresults>" +
            "        </d:limit>" +
            "    </d:basicsearch>" +
            "</d:searchrequest>";

        BasicSearchRequestReader reader = new BasicSearchRequestReader();
        reader.parse(request);
        BasicSearch search = reader.getSearch();
        assertEquals(mount, search.getScopeUri());
        assertEquals(BasicSearch.INFINITE_DEPTH, search.getScopeDepth());
        assertEquals(10, search.getLimit());
        assertEquals(BasicSearchClause.OP_AND, search.getWhereClause().getOp());
        BasicSearchClause clause = search.getWhereClause();
        BasicSearchClause subClauses[] = clause.getSubClauses();
        assertEquals(2, subClauses.length);
        assertEquals(BasicSearchClause.OP_LIKE, subClauses[0].getOp());
        assertEquals(
            PropertyHelper.itensilQName("description"),
            subClauses[0].getProperty());
        assertEquals("%hello%", subClauses[0].getLiteral());
        assertEquals(BasicSearchClause.OP_NOT, subClauses[1].getOp());
        assertEquals(
            BasicSearchClause.OP_IS_COLLECTION,
            subClauses[1].getSubClauses()[0].getOp());

       BasicSearchOrderBy orderbys[] = search.getOrderBys();
       assertEquals(1, orderbys.length);
       assertEquals(
            PropertyHelper.defaultQName("getlastmodified"),
            orderbys[0].getProperty());
       assertTrue(orderbys[0].isDescending());
    }


    public void testBasicSearch() throws Exception {

        SecurityAssociation.setUser(user);
        HibernateUtil.beginTransaction();
        Repository repository = RepositoryHelper.getRepository(mount);

        // create nodes
        MutableRepositoryNode node =
            (MutableRepositoryNode)
                repository.createNode(mount + "/test.txt", false, user);

        MutableRepositoryNode parentNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent", true, user);

        MutableRepositoryNode childNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/child", false, user);

        MutableRepositoryNode kidNode =
            (MutableRepositoryNode)
                repository.createNode(mount + "/parent/kid", false, user);

        // set some properties
        DefaultNodeProperties props =
            new DefaultNodeProperties(new DefaultNodeVersion());
        props.setValue(PropertyHelper.itensilQName("description"),
             "my description");
        props.setValue(PropertyHelper.itensilQName("keywords"),
             "taco bravo tango");
        props.setValue(PropertyHelper.itensilQName("author"),
             "bob");
        node.setProperties(props);

        props = new DefaultNodeProperties(new DefaultNodeVersion());
        props.setValue(PropertyHelper.itensilQName("description"),
             "very different");
        props.setValue(PropertyHelper.itensilQName("keywords"),
             "alpha bravo tango");
        childNode.setProperties(props);

        props = new DefaultNodeProperties(new DefaultNodeVersion());
        props.setValue(PropertyHelper.defaultQName("getcontenttype"),
             "shoe/tennis");
        kidNode.setProperties(props);

        HibernateUtil.commitTransaction();


        HibernateUtil.beginTransaction();
        // search 1
        HashSet<String> uriSet = new HashSet<String>();
        uriSet.add(mount + "/test.txt");
        uriSet.add(mount + "/parent/child");
        DefaultBasicSearch search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.setSelectAllProperties();
        search.setWhereClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("keywords"),
                "%tango%"));
        BasicSearchResultSet results = repository.search(search);
        assertTrue(results.next());
        assertTrue(uriSet.contains(results.getNode().getUri()));
        String val = results.getProperties().getValue(
            PropertyHelper.itensilQName("keywords"));
        assertNotNull(val);
        assertTrue(val.indexOf("tango") >= 0);
        assertTrue(results.next());
        assertTrue(uriSet.contains(results.getNode().getUri()));
        val = results.getProperties().getValue(
            PropertyHelper.itensilQName("keywords"));
        assertNotNull(val);
        assertTrue(val.indexOf("tango") >= 0);
        assertFalse(results.next());

        // search 2
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.addSelectProperty(
            PropertyHelper.itensilQName("keywords"));
        search.setWhereClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.defaultQName("displayname"),
                "%child%"));
        results = repository.search(search);
        assertTrue(results.next());
        assertEquals(mount + "/parent/child", results.getNode().getUri());
        QName pNames[] = results.getProperties().getNames();
        assertEquals(1, pNames.length);
        assertEquals(PropertyHelper.itensilQName("keywords"), pNames[0]);
        assertFalse(results.next());

        // search 3
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.addSelectProperty(
            PropertyHelper.itensilQName("keywords"));
        search.addSelectProperty(
            PropertyHelper.itensilQName("description"));
        DefaultBasicSearchClause andClause =
            new DefaultBasicSearchClause(BasicSearchClause.OP_AND, null, null);
        andClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("keywords"),
                "%tango%"));
        andClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("description"),
                "%different%"));
        search.setWhereClause(andClause);
        results = repository.search(search);
        assertTrue(results.next());

        assertEquals(mount + "/parent/child", results.getNode().getUri());
        pNames = results.getProperties().getNames();
        assertEquals(2, pNames.length);
        assertFalse(results.next());

        // search 4
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.addSelectProperty(
            PropertyHelper.itensilQName("keywords"));
        DefaultBasicSearchClause orClause =
            new DefaultBasicSearchClause(BasicSearchClause.OP_OR, null, null);
        orClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.defaultQName("displayname"),
                "%child%"));
        orClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("author"),
                "%child%"));
        search.setWhereClause(orClause);
        results = repository.search(search);
        assertTrue(results.next());
        assertEquals(mount + "/parent/child", results.getNode().getUri());
        pNames = results.getProperties().getNames();
        assertEquals(1, pNames.length);
        assertEquals(PropertyHelper.itensilQName("keywords"), pNames[0]);
        assertFalse(results.next());


        // search 5
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.addSelectProperty(
            PropertyHelper.itensilQName("keywords"));
        orClause =
            new DefaultBasicSearchClause(BasicSearchClause.OP_OR, null, null);
        orClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.defaultQName("displayname"),
                "%kid%"));
        orClause.addSubClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("author"),
                "%kid%"));
        search.setWhereClause(orClause);
        results = repository.search(search);
        assertTrue(results.next());
        assertEquals(mount + "/parent/kid", results.getNode().getUri());
        pNames = results.getProperties().getNames();
        assertEquals(0, pNames.length);
        assertFalse(results.next());


        // search 6 with ascending sort
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.setSelectAllProperties();
        search.setWhereClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("keywords"),
                "%tango%"));
        search.addOrderBy(
            new DefaultBasicSearchOrderBy(
                PropertyHelper.itensilQName("keywords"))
            );

        results = repository.search(search);
        assertTrue(results.next());
        assertEquals(mount + "/parent/child", results.getNode().getUri());
        assertTrue(results.next());
        assertEquals(mount + "/test.txt", results.getNode().getUri());
        assertFalse(results.next());


        // search 7 with descending sort
        search = new DefaultBasicSearch(
            mount + "/",
            BasicSearch.INFINITE_DEPTH,
            false);
        search.setSelectAllProperties();
        search.setWhereClause(
            new DefaultBasicSearchClause(
                BasicSearchClause.OP_LIKE,
                PropertyHelper.itensilQName("keywords"),
                "%tango%"));
        search.addOrderBy(
            new DefaultBasicSearchOrderBy(
                PropertyHelper.itensilQName("keywords"), true)
            );

        results = repository.search(search);
        assertTrue(results.next());
        assertEquals(mount + "/test.txt", results.getNode().getUri());
        assertTrue(results.next());
        assertEquals(mount + "/parent/child", results.getNode().getUri());
        assertFalse(results.next());

        // remove nodes
        node.remove();
        parentNode.remove();
        HibernateUtil.commitTransaction();
    }


    protected void setUp() throws Exception {

        // local mode
    	TestUserSpace uspace = new TestUserSpace();
        user = uspace.user;
        user2 = uspace.user2;
        group1 = uspace.group1;
    }

    /*
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        SecurityAssociation.clear();
    }

    protected static class TestUserSpace implements UserSpace {
    	
        User user;
        User user2;
        User user3;
        Group group1;
        HashSet<Group> groups = new HashSet<Group>();
        HashMap<Group, ArrayList<Pair<User,Set<String>>>> groupMems = new HashMap<Group, ArrayList<Pair<User,Set<String>>>>();
        IUIDGenerator idGen = new IUIDGenerator(); 
        
    	TestUserSpace()  throws UserSpaceException {
    		user = new AuthenticatedUser(
                    "AAAAAAAAAAAAAAAAAAAB",
                    "junit@itensil.com",
                    "junit",
                    Locale.getDefault(),
                    TimeZone.getDefault(),
                    this,
                    System.currentTimeMillis()
                    );

            user2 = new AuthenticatedUser(
                    "AAAAAAAAAAAAAAAAAAAC",
                    "junit2@itensil.com",
                    "junit2",
                    Locale.getDefault(),
                    TimeZone.getDefault(),
                    this,
                    System.currentTimeMillis()
                    );
    	
            user3 = new AuthenticatedUser(
                    "AAAAAAAAAAAAAAAAAAAD",
                    "junit3@itensil.com",
                    "junit3",
                    Locale.getDefault(),
                    TimeZone.getDefault(),
                    this,
                    System.currentTimeMillis()
                    );
            Group grp;
            group1 = grp = createGroup("JUGroup2", "JUGroup2");
            
            addGroupUser(grp, user2);
            setGroupRoles(grp, user2, StringHelper.setFromString("pitcher batter"));
            
            grp = createGroup("JUGroup3", "JUGroup3");
            addGroupUser(grp, user3);
            setGroupRoles(grp, user3, StringHelper.setFromString("thirdbase batter"));
    	}

        public String getUserSpaceId() {
            return "AAAAAAAAAAAAAAAAAAAA";
        }

        public String getName()  {
            return "junit";
        }

        public String getBaseUrl() {
            return "http://localhost";
        }

        public Set<? extends User> getUsers() throws UserSpaceException {
            return null;
        }

        public User getUser(IUID userId) throws UserSpaceException {
            return null;
        }

        public User getUser(String userName) throws UserSpaceException {
            return null;
        }

        public User createUser(String userName, String simpleName, String password, Set<String> roles, Locale locale, TimeZone timezone) 
        		throws UserSpaceException {
            return null;
        }

        public void removeUser(User user) throws UserSpaceException {

        }

        public Set<? extends Group> getGroupsForUser(User user) throws UserSpaceException {
            return null;
        }

        public Set<? extends User> getGroupUsers(Group group) throws UserSpaceException {
        	HashSet<User> gUsers = new HashSet<User>();
        	ArrayList<Pair<User,Set<String>>> mems = groupMems.get(group);
        	if (mems != null) {
        		for (Pair<User,Set<String>> mem : mems) {
        			gUsers.add(mem.first);
        		}
        	}
            return gUsers;
        }

        public boolean isUserInGroup(Group group, User user) throws UserSpaceException {
        	Set<? extends User> gUsers = getGroupUsers(group);
            return gUsers.contains(gUsers);
        }

        public Group getGroup(IUID groupId) throws UserSpaceException {
            return null;
        }

        public Group getGroup(String groupName) throws UserSpaceException {
            return null;
        }

        public Group createGroup(String groupName, String simpleName) throws UserSpaceException {
        	
        	Group grp = new DefaultGroup(idGen.createID().toString(), groupName, simpleName);
        	groups.add(grp);
        	groupMems.put(grp, new ArrayList<Pair<User,Set<String>>>());
        	
            return grp;
        }

        public void removeGroup(Group group) throws UserSpaceException { }

        public Object addGroupUser(Group group, User user) throws UserSpaceException {
        	ArrayList<Pair<User,Set<String>>> mems = groupMems.get(group);
        	if (mems != null) {
        		mems.add(new Pair<User,Set<String>>(user, null));
        	}
        	return null;
        }

        public void removeGroupUser(Group group, User user) throws UserSpaceException { }

        public Set<? extends Group> getGroups() throws UserSpaceException {
            return groups;
        }

        public Group resolve(Group group) throws UserSpaceException {
            return group;
        }

        public User resolve(User user) throws UserSpaceException {
            return user;
        }

        public Set<String> getUserRoles(User user) throws UserSpaceException {
            return Collections.emptySet();
        }

        public void setUserRoles(User user, Set<String> roles) throws UserSpaceException { }

		public String getBrand() { return null; }

		public boolean isDisabled() { return false; }

		public Set<? extends User> getUsersInRole(String role) throws UserSpaceException {
			return null;
		}

		public Set<String> getFeatures() { return null; }

		public Set<? extends User> findGroupRoleUsers(Group contextGroup, GroupAxis axis, String role) throws UserSpaceException {
			ArrayList<Pair<User,Set<String>>> mems = groupMems.get(contextGroup);
			HashSet<User> fUsers = new HashSet<User>();
        	if (mems != null) {
        		for (Pair<User,Set<String>> mem : mems) {
        			if (mem.second != null && mem.second.contains(role))  {
        				fUsers.add(mem.first);
        			}
        		}
        	}
			return fUsers;
		}

		public Set<String> getGroupRoles(Group group, User user) throws UserSpaceException {
			ArrayList<Pair<User,Set<String>>> mems = groupMems.get(group);
        	if (mems != null) {
        		for (Pair<User,Set<String>> mem : mems) {
        			if (mem.first.equals(user))  {
        				return mem.second;
        			}
        		}
        	}
        	return null;
		}

		public void setGroupRoles(Group group, User user, Set<String> roles)  throws UserSpaceException {
			ArrayList<Pair<User,Set<String>>> mems = groupMems.get(group);
        	if (mems != null) {
        		for (Pair<User,Set<String>> mem : mems) {
        			if (mem.first.equals(user))  {
        				mem.second = roles;
        				return;
        			}
        		}
        	}
		}
    }

}
