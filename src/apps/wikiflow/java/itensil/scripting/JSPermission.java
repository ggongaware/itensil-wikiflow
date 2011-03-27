package itensil.scripting;

import java.security.Principal;

import itensil.repository.DefaultNodePermission;
import itensil.repository.hibernate.PermissionEntity;
import itensil.security.Everyone;
import itensil.security.Group;
import itensil.security.GroupAxis;
import itensil.security.RelativeGroup;
import itensil.security.SecurityAssociation;
import itensil.security.SysAdmin;
import itensil.security.User;
import itensil.security.UserSpaceException;
import itensil.security.hibernate.GroupEntity;
import itensil.security.hibernate.UserEntity;

import org.mozilla.javascript.ScriptableObject;

public class JSPermission extends ScriptableObject {
	
	PermissionEntity perm;
	
	public static Everyone EVERYONE = new Everyone();
	public static RelativeGroup RELATIVE = new RelativeGroup();
	
	public static String GROUP_RELATIVE = "GROUP_RELATIVE";
	public static String GROUP_EVERYONE = "GROUP_EVERYONE";
	
	
	public JSPermission() {
		this(new PermissionEntity());
	}
	
	public JSPermission(PermissionEntity perm) {
		this.perm = perm;
		
		this.putConst("NONE", this, DefaultNodePermission.NONE);
		this.putConst("READ", this, DefaultNodePermission.READ);
		this.putConst("CREATE", this, DefaultNodePermission.CREATE);
		this.putConst("WRITE", this, DefaultNodePermission.WRITE);
		this.putConst("MANAGE", this, DefaultNodePermission.MANAGE);
		
		this.putConst("AXIS_SELF", this, GroupAxis.SELF.toString());
		this.putConst("AXIS_PARENT", this, GroupAxis.PARENT.toString());
		this.putConst("AXIS_ANCESTOR", this, GroupAxis.ANCESTOR.toString());
		this.putConst("AXIS_CHILD", this, GroupAxis.CHILD.toString());
		this.putConst("AXIS_ANCESTOR_OR_SELF", this, GroupAxis.ANCESTOR_OR_SELF.toString());
		this.putConst("AXIS_CHILD_OR_SELF", this, GroupAxis.CHILD_OR_SELF.toString());
		this.putConst("AXIS_SIBLING", this, GroupAxis.SIBLING.toString());
		
		this.putConst("GROUP_RELATIVE", this, GROUP_RELATIVE);
		this.putConst("GROUP_EVERYONE", this, GROUP_EVERYONE);
		
		defineProperty("canRead", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		defineProperty("canCreate", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		defineProperty("canWrite", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		defineProperty("canManage", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		defineProperty("isNone", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		
		defineProperty("principal", JSPermission.class, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
		
		defineProperty("permission", JSPermission.class, ScriptableObject.PERMANENT);
		defineProperty("inherit", JSPermission.class, ScriptableObject.PERMANENT);
		defineProperty("role", JSPermission.class, ScriptableObject.PERMANENT);
		defineProperty("axis", JSPermission.class, ScriptableObject.PERMANENT);
	}
	
	public String getClassName() {
		return "JSPermission";
	}

	public boolean canCreate() {
		return perm.canCreate();
	}

	public boolean canManage() {
		return perm.canManage();
	}

	public boolean canRead() {
		return perm.canRead();
	}

	public boolean canWrite() {
		return perm.canWrite();
	}

	public String getAxis() {
		return perm.getAxisStr();
	}

	public int getPermission() {
		return perm.getPermission();
	}

	public Object getPrincipal() throws UserSpaceException {
		if (perm.isRelativeRole()) {
			return GROUP_RELATIVE;
		}
		Principal prin = perm.getPrincipal();
		if (EVERYONE.equals(prin)) {
			return GROUP_EVERYONE;
		} else if (prin instanceof User) {
			if (SysAdmin.isSysAdmin((User)prin)) {
				return "SYSADMIN";
			} else {
				UserEntity usr = (UserEntity)SecurityAssociation.getUser().getUserSpace().resolve((User)prin);
				if (usr == null)
					return "???";
				else
					return new JSAuthUser(usr);
			}
		} else {
			GroupEntity grp = (GroupEntity)SecurityAssociation.getUser().getUserSpace().resolve((Group)prin);
			if (grp == null)
				return "???";
			else
				return new JSGroup(grp);
		}
	}

	public String getRole() {
		return perm.getRole();
	}

	public boolean isGroup() {
		return perm.isGroup();
	}

	public boolean isNone() {
		return perm.isNone();
	}

	public boolean isRelativeRole() {
		return perm.isRelativeRole();
	}

	public void setAxis(String axisStr) {
		perm.setAxisStr(axisStr);
	}

	public void setPermission(int permission) {
		perm.setPermission(permission);
	}

	public void setRole(String role) {
		perm.setRole(role);
	}
	
	public boolean getInherit() {
		return perm.isInherit();
	}
	
	public void setInherit(boolean inherit) {
		perm.setInherit(inherit);
	}

}
