package itensil.scripting;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import itensil.io.HibernateUtil;
import itensil.security.DefaultGroup;
import itensil.security.DefaultUser;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.PasswordGen;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.USpaceUserEntity;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.util.Check;
import itensil.util.LocaleHelper;

import org.hibernate.Query;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSUserSpace extends ScriptableObject {

	public UserSpaceEntity userSpace;
	
	public JSUserSpace(UserSpaceEntity userSpace) {
		this.userSpace = userSpace;
		
		String funcs[] = {
				"createGroup",
				"createUser",
				"findGroupRoleUsers",
				"genPassword",
				"getCreateTime",
				"getFeatures",
				"getGroup",
				"getGroupById",
				"getGroupByRemote",
				"getGroups",
				"getUser",
				"getUserById",
				"getUsers",
				"removeGroup",
				"removeUser"
			};
		
		try {
            defineFunctionProperties(funcs, JSUserSpace.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
            defineProperty("name", JSUserSpace.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("baseUrl", JSUserSpace.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("brand", JSUserSpace.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
		} catch (RhinoException e) {
            e.printStackTrace();
        }
		
		sealObject();
	}
	
	public String getClassName() {
		return "JSUserSpace";
	}

	public JSGroup createGroup(String simpleName, String groupName, String remoteKey, int groupType, Object parent) 
			throws UserSpaceException {
		 
        if (groupType == 1 && !userSpace.getFeatures().contains("orgs")) 
        	throw new UserSpaceException("Org feature required");

        GroupEntity grp = new GroupEntity();
        grp.initNew();
        grp.setUserSpaceEntity(userSpace);
        grp.setGroupType(1);
        grp.setSimpleName(simpleName);
        grp.setGroupName(groupName);
        if (Check.isEmpty(grp.getGroupName()))
        	grp.setGroupName(grp.getSimpleName());
        grp.setRemoteKey(remoteKey);
        if (parent != null) {
        	grp.setParentGroupId(((JSGroup)parent).group.getGroupId());
        }
        HibernateUtil.getSession().save(grp);
        return new JSGroup(grp);
	}

	public JSAuthUser createUser(String userName, String simpleName, String password, Object jsRoles, String locale, String timezone) 
			throws UserSpaceException {
		
		TimeZone tzobj = LocaleHelper.readTimeZone(timezone);
		Locale  lobj = LocaleHelper.readLocal(locale);
		Context ctx = Context.getCurrentContext();
		HashSet<String> roles = new HashSet<String>();
		if (jsRoles != null) {
			for (Object obj : ctx.getElements((Scriptable)jsRoles)) {
				roles.add(Context.toString(obj));
			}
		}
		return new JSAuthUser(userSpace.createUser(userName, simpleName, password, roles, lobj, tzobj));
	}

	public Scriptable findGroupRoleUsers(JSGroup contextGroup, String axis, String role) throws UserSpaceException {
		GroupAxis gax = GroupAxis.valueOf(axis.toUpperCase());
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, JSAuthUser.getJSAuthUsers(userSpace.findGroupRoleUsers(contextGroup.group, gax, role)));
	}

	public String getBaseUrl() {
		return userSpace.getBaseUrl();
	}

	public String getBrand() {
		return userSpace.getBrand();
	}

	public Date getCreateTime() {
		return userSpace.getCreateTime();
	}

	public Scriptable getFeatures() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(getParentScope(), userSpace.getFeatures().toArray());
	}
	
	public JSGroup getGroup(String groupName) throws UserSpaceException {
		GroupEntity grp = (GroupEntity)userSpace.resolve(new DefaultGroup(null, groupName));
		return grp == null ? null : new JSGroup(grp);
	}

	public JSGroup getGroupById(String groupId) throws UserSpaceException {
		GroupEntity grp = (GroupEntity)userSpace.getGroup(groupId);
		return grp == null ? null : new JSGroup(grp);
	}
	
	public JSGroup getGroupByRemote(String remoteKey) throws UserSpaceException {
		Query qry = HibernateUtil.getSession().getNamedQuery("USpace.groupByRemote");
        qry.setString("rkey", remoteKey);
        qry.setEntity("uspace", userSpace);
        GroupEntity grp = (GroupEntity)qry.uniqueResult();
        return grp == null ? null : new JSGroup(grp);
	}

	public Scriptable getGroups() throws UserSpaceException {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(getParentScope(), JSGroup.getJSGroups(userSpace.getGroupEntities()));
	}

	public String getName() {
		return userSpace.getName();
	}

	public JSAuthUser getUserById(String userId) throws UserSpaceException {
		User usr = userSpace.getUser(userId);
		return usr == null ? null : new JSAuthUser(usr);
	}
	
	public JSAuthUser getUser(String userName) throws UserSpaceException {
		UserEntity usr = (UserEntity)userSpace.resolve(new DefaultUser(null, userName));
		if (usr != null) {
			USpaceUserEntity uus = usr.getUserSpaceUsers().get(userSpace);
			usr.setUSpaceUser(uus);
			return new JSAuthUser(usr);
		}
		return null;
	}

	public Scriptable getUsers() throws UserSpaceException {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(getParentScope(), JSAuthUser.getJSAuthUsers(userSpace.getUsers()));
	}

	public void removeGroup(JSGroup group) throws UserSpaceException {
		userSpace.removeGroup(group.group);
	}

	public void removeUser(JSAuthUser user) throws UserSpaceException {
		userSpace.removeUser(user.user);
	}

	public String genPassword() {
		return PasswordGen.generatePassword();
	}
}
