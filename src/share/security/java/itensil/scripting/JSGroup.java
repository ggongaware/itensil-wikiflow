package itensil.scripting;

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import itensil.io.HibernateUtil;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.GroupUserEntity;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSGroup extends ScriptableObject implements Principal {

	GroupEntity group;
	
	public JSGroup(GroupEntity group) {
		this.group = group;
		
		String funcs[] = {
				"getMembers",
				"getParent",
				"getUserSpace",
				"memberJoin",
				"memberLeave",
				"save"
			};
		
		try {
			
            defineFunctionProperties(funcs, JSGroup.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
            defineProperty("custom1", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("custom2", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("custom3", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("custom4", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("groupName", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("remoteKey", JSGroup.class, ScriptableObject.PERMANENT);
            defineProperty("simpleName", JSGroup.class, ScriptableObject.PERMANENT);
            
            defineProperty("groupType", JSGroup.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("groupId", JSGroup.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("createTime", JSGroup.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
		} catch (RhinoException e) {
            e.printStackTrace();
        }
	}

	public String getCustom1() {
		return group.getCustom1();
	}

	public String getCustom2() {
		return group.getCustom2();
	}

	public String getCustom3() {
		return group.getCustom3();
	}

	public String getCustom4() {
		return group.getCustom4();
	}

	public String getGroupId() {
		return group.getGroupId();
	}

	public int getGroupType() {
		return group.getGroupType();
	}

	public Scriptable getMembers() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, JSAuthUser.getJSAuthUsers(group.getGroupUserEntities().keySet()));
	}
	
	public Scriptable memberJoin(JSAuthUser user) throws UserSpaceException {
		return new JSMember((GroupUserEntity)group.getUserSpaceEntity().addGroupUser(group, user.user));
	}
	
	public void memberLeave(JSAuthUser user) throws UserSpaceException {
		group.getUserSpaceEntity().removeGroupUser(group, user.user);
	}

	public String getName() {
		return group.getName();
	}
	
	public String getGroupName() {
		return group.getName();
	}

	public JSGroup getParent() {
		return new JSGroup(group.getParentGroupEntity());
	}

	public String getRemoteKey() {
		return group.getRemoteKey();
	}

	public String getSimpleName() {
		return group.getSimpleName();
	}

	public JSUserSpace getUserSpace() {
		return new JSUserSpace(group.getUserSpaceEntity());
	}

	public void setCustom1(String custom1) {
		group.setCustom1(custom1);
	}

	public void setCustom2(String custom2) {
		group.setCustom2(custom2);
	}

	public void setCustom3(String custom3) {
		group.setCustom3(custom3);
	}

	public void setCustom4(String custom4) {
		group.setCustom4(custom4);
	}

	public void setGroupName(String groupName) {
		group.setGroupName(groupName);
	}

	public void setRemoteKey(String remoteKey) {
		group.setRemoteKey(remoteKey);
	}

	public void setSimpleName(String simpleName) {
		group.setSimpleName(simpleName);
	}
	
	public Date getCreateTime() {
		return group.getCreateTime();
	}

	public String getClassName() {
		return "JSGroup";
	}
	
	public void save() throws UserSpaceException {
		JSAuthUser.checkAdminOnlyAccess(group.getUserSpaceEntity());
		HibernateUtil.getSession().update(group);
	}
	
	public static Object [] getJSGroups(Set<GroupEntity> groupEnts) {
		Object[] jgrps = new JSGroup[groupEnts.size()];
		int ii = 0;
		for (GroupEntity grp : groupEnts) {
			jgrps[ii++] = new JSGroup(grp);
		}
		return jgrps;
	}

}
