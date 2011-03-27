package itensil.scripting;

import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import itensil.io.HibernateUtil;
import itensil.security.Group;
import itensil.security.SecurityAssociation;
import itensil.security.User;
import itensil.security.UserSpace;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.GroupUserEntity;
import itensil.security.hibernate.SignOnHB;
import itensil.security.hibernate.USpaceUserEntity;
import itensil.security.hibernate.UserEntity;
import itensil.security.hibernate.UserSpaceEntity;
import itensil.util.Check;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSAuthUser extends ScriptableObject implements Principal {
	
	UserEntity user;
	
	public JSAuthUser(User user) {
		// TODO auto upgrade to UserEntity from any implenting user class
		this.user = (UserEntity)user;
		
		String funcs[] = {
				"getFlags",
				"setFlags",
				"getGroups",
				"getMemberInfo",
				"getRoles",
				"setRoles",
				"getUserSpace",
				"isUserInGroup",
				"save"
			};
		
		try {
			
            defineFunctionProperties(funcs, JSAuthUser.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
            defineProperty("custom1", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("custom2", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("custom3", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("custom4", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("email", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("locale", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("remoteKey", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("simpleName", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("timezone", JSAuthUser.class, ScriptableObject.PERMANENT);
            defineProperty("userName", JSAuthUser.class, ScriptableObject.PERMANENT);
            
            defineProperty("userId", JSAuthUser.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("loginCount", JSAuthUser.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("lastLogin", JSAuthUser.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("createTime", JSAuthUser.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
		} catch (RhinoException e) {
            e.printStackTrace();
        }

	}

	public String getClassName() {
		return "JSAuthUser";
	}

	public String getUserId() {
		return user.getUserId();
	}

	public Date getCreateTime() {
		return user.getCreateTime();
	}

	public String getEmail() {
		return user.getEmail();
	}

	public Scriptable getGroups() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, JSGroup.getJSGroups(user.getGroupEntities()));
	}
	
	public JSMember getMemberInfo(JSGroup group) {
		GroupUserEntity guser = user.getGroupUserEntities().get(group.group);
		JSMember mem = null;
		if (guser != null) {
			mem = new JSMember(guser);
		}
		return mem;
	}

	public Date getLastLogin() {
		return user.getLastLogin();
	}

	public String getLocale() {
		return user.getLocaleStr();
	}

	public int getLoginCount() {
		return user.getLoginCount();
	}

	public String getName() {
		return user.getName();
	}

	public String getSimpleName() {
		return user.getSimpleName();
	}

	public String getTimezone() {
		return user.getTimezoneStr();
	}

	public String getUserName() {
		return user.getUserName();
	}

	public JSUserSpace getUserSpace() {
		return new JSUserSpace((UserSpaceEntity)user.getUserSpace());
	}

	public void setEmail(String email) {
		user.setEmail(email);
	}

	public void setLocale(String localeStr) {
		user.setLocaleStr(localeStr);
	}

	public void setSimpleName(String simpleName) {
		user.setSimpleName(simpleName);
	}

	public void setTimezone(String timezoneStr) {
		user.setTimezoneStr(timezoneStr);
	}

	public void setUserName(String userName) {
		user.setUserName(userName);
	}

	public boolean isUserInGroup(JSGroup group) {
		return user.isUserInGroup(group.group);
	}

	public String getRemoteKey() {
		return user.getRemoteKey();
	}

	public Scriptable getRoles() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, user.getRoles().toArray());
	}

	public void setRoles(Scriptable jsRoles) throws UserSpaceException {
		checkAdminOnlyAccess(user.getUserSpace());
		Context ctx = Context.getCurrentContext();
		HashSet<String> roles = new HashSet<String>();
		for (Object obj : ctx.getElements(jsRoles)) {
			roles.add(Context.toString(obj));
		}
		user.setRoles(roles);
	}
	
	public void setFlags(Scriptable jsFlags) {
		Context ctx = Context.getCurrentContext();
		HashSet<String> flags = new HashSet<String>();
		for (Object obj : ctx.getElements(jsFlags)) {
			flags.add(Context.toString(obj));
		}
		user.setFlags(flags);
	}
	
	public Scriptable getFlags() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, user.getFlags().toArray());
	}
	
	public void setRemoteKey(String remoteKey) {
		user.setRemoteKey(remoteKey);
	}

	public String getCustom1() {
		return user.getCustom1();
	}

	public String getCustom2() {
		return user.getCustom2();
	}

	public String getCustom3() {
		return user.getCustom3();
	}

	public String getCustom4() {
		return user.getCustom4();
	}

	public void setCustom1(String custom1) {
		user.setCustom1(custom1);
	}

	public void setCustom2(String custom2) {
		user.setCustom2(custom2);
	}

	public void setCustom3(String custom3) {
		user.setCustom3(custom3);
	}

	public void setCustom4(String custom4) {
		user.setCustom4(custom4);
	}
	
	static void checkAdminOnlyAccess(UserSpace userSpace) throws UserSpaceException {
		((UserSpaceEntity)userSpace).checkAccess(UserSpaceEntity.ADMIN_ROLE);
	}
	
	private void checkAccess() throws UserSpaceException {
		User caller = SecurityAssociation.getUser();
		if (!caller.equals(user)) {
			checkAdminOnlyAccess(user.getUserSpace());
		}
	}
	
	public void resetPassword(String pass) throws UserSpaceException {
		checkAccess();
		if (!Check.isEmpty(pass) && pass.length() > 0 && pass.length() < 32) {
			user.setPasswordHash(SignOnHB.hashPassword(pass));
		}
	}
	
	public void save() throws UserSpaceException {
		checkAccess();
		HibernateUtil.getSession().update(user);
		HibernateUtil.getSession().update(user.getUSpaceUser());
	}

	public static Object[] getJSAuthUsers(Set<? extends User> userEnts) {
		Object[] jusrs = new Object[userEnts.size()];
		int ii = 0;
		for (User usr : userEnts) {
			
			jusrs[ii++] = new JSAuthUser((UserEntity)usr);
		}
		return jusrs;
	}
	
}
