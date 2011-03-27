package itensil.scripting;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import itensil.io.HibernateUtil;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.GroupUserEntity;
import itensil.security.hibernate.UserEntity;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSMember extends ScriptableObject {

	GroupUserEntity guser;
	
	public JSMember(GroupUserEntity guser) {
		this.guser = guser;
		String funcs[] = {
				"getGroup",
				"getRoles",
				"setRoles",
				"getUser",
				"save"
			};
		
		try {
			
            defineFunctionProperties(funcs, JSMember.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            defineProperty("joinTime", JSMember.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
            
		} catch (RhinoException e) {
            e.printStackTrace();
        }
		sealObject();
	}
	
	public JSGroup getGroup() {
		return new JSGroup(guser.getGroupEntity());
	}

	public Date getJoinTime() {
		return guser.getJoinTime();
	}

	public JSAuthUser getUser() {
		return new JSAuthUser(guser.getUserEntity());
	}

	public Scriptable getRoles() {
		Context ctx = Context.getCurrentContext();
		return ctx.newArray(this, guser.getRoles().toArray());
	}

	public void setRoles(Scriptable jsRoles) throws UserSpaceException {
		JSAuthUser.checkAdminOnlyAccess(guser.getGroupEntity().getUserSpaceEntity());
		Context ctx = Context.getCurrentContext();
		HashSet<String> roles = new HashSet<String>();
		for (Object obj : ctx.getElements(jsRoles)) {
			roles.add(Context.toString(obj));
		}
		guser.setRoles(roles);
	}
	
	public void save() throws UserSpaceException {
		JSAuthUser.checkAdminOnlyAccess(guser.getGroupEntity().getUserSpaceEntity());
		HibernateUtil.getSession().update(guser);
	}
	
	public String getClassName() {
		return "JSMember";
	}

}
