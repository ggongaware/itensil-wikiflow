package itensil.scripting;

import itensil.io.HibernateUtil;
import itensil.security.LoginTestHelper;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.util.Check;
import junit.framework.TestCase;

public class ScriptUserspaceJunit extends TestCase {
	
	String groupName;
	
    protected void setUp() throws Exception {
    	groupName = "junit" + System.currentTimeMillis();
    }
    
	public void testUser() throws Exception {
		HibernateUtil.beginTransaction();
        SecurityAssociation.setUser(SysAdmin.getUser());
        User user = LoginTestHelper.createOrLogin("junit1", "passunit1");
        HibernateUtil.commitTransaction();
        
        HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(user);
		ScriptHost scr = new ScriptHost("junit", new JSAuthUser(user));
		
		assertEquals("junit1", scr.evaluateToString("Host.getCurrentUser().userName"));
		assertEquals(groupName, scr.evaluateToString(
					"var uspace = Host.getCurrentUser().getUserSpace();" +
					"var grp = uspace.createGroup('" + groupName + "','" + groupName + "', 'remoty', 0, null);" +
					"grp.simpleName"));
		
		assertEquals("1", scr.evaluateToString(
				"var mem = grp.memberJoin(Host.getCurrentUser()); grp.getMembers().length"));
		
		assertEquals("2", scr.evaluateToString("mem.setRoles(['fish','food','fish']); mem.save(); mem.getRoles().length"));
		
		String time = scr.evaluateToString(
				"var rg = uspace.getGroupByRemote('remoty');" +
				"rg.custom1 = 'myval';" +
				"rg.save();" +
				"Host.getCurrentUser().getMemberInfo(rg).joinTime"
			);
		assertFalse(Check.isEmpty(time));
		
		HibernateUtil.commitTransaction();
		
		HibernateUtil.beginTransaction();
        HibernateUtil.getSession().refresh(user);
		assertEquals("2", scr.evaluateToString(
				"uspace = Host.getCurrentUser().getUserSpace();" +
				"grp = uspace.getGroup('" + groupName + "');" +
				"Host.getCurrentUser().getMemberInfo(grp).getRoles().length"
			));
		assertEquals("myval", scr.evaluateToString("grp.custom1"));
		assertEquals("junit1", scr.evaluateToString("uspace.findGroupRoleUsers(grp,'ANCESTOR_OR_SELF','food')[0].userName"));
				
		scr.evaluateToString("uspace.removeGroup(grp)");
		
		HibernateUtil.commitTransaction();
	}
	
}
