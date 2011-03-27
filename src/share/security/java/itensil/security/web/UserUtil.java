package itensil.security.web;


import itensil.security.AuthenticatedUser;
import itensil.security.User;
import itensil.security.hibernate.UserEntity;

import javax.servlet.http.HttpServletRequest;


public class UserUtil {

	public static boolean isGuest(HttpServletRequest hreq) {
		return isGuest((User)hreq.getUserPrincipal());
	}

	public static boolean isGuest(User user) {
		return hasRole(user, "guest", true);
	}

	public static boolean hasRole(User user, String role) {
		return hasRole(user, role, false);
	}
	
	public static boolean hasRole(User user, String role, boolean hDefault) {
		return  user instanceof AuthenticatedUser ? ((AuthenticatedUser)user).getRoles().contains(role) : hDefault;
	}

	public static boolean isAdmin(HttpServletRequest hreq) {
		return isAdmin((User)hreq.getUserPrincipal());
	}

	public static boolean isAdmin(User user) {
		return hasRole(user, "admin");
	}

	
	public static String getToken(HttpServletRequest hreq) {
		return getToken((UserEntity)hreq.getUserPrincipal());
	}

	public static String getToken(UserEntity user) {
		return  user != null ? user.getToken() : "";
	}

}

