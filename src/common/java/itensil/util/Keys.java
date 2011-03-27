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
package itensil.util;

/**
 * This interface provides a central location for final KEYS 
 *
 *
 * @author ejones@itensil.com
 */
public interface Keys {

		public static final String URL_SIGNON = "/j_signon_check";
		public static final String URL_SIGNOFF = "/j_signoff";

		// form keys
		public static final String FORM_USER_NAME = "j_username";
		public static final String FORM_PASSWORD = "j_password";
		public static final String FORM_REMEMBER = "j_remember";
		public static final String FORM_TARGET_URL = "j_target_url";
		public static final String FORM_TOKEN = "j_signon_token";

		// request keys
		public static final String REQUEST_SIGNON_ERROR = "signonError";
		public static final String REQUEST_SIGNON_USER = "signonUser";
		//public static final String REQUEST_AUTH_TOKEN = "auth";
		

		// session keys
		public static final String USER_OBJECT = "j_signon_object";
		public static final String SIGNED_ON_USER  = "j_signon";
		public static final String ORIGINAL_URL = "j_signon_original_url";

		// cookie keys
		public static final String COOKIE_NAME = "soName";
		public static final String COOKIE_AUTH = "soAuth";

		// config keys
		public static final String CFG_DIS_REMEMBER_USER = "disable-remember-user";
		public static final String CFG_DIS_REMEMBER_AUTH = "disable-remember-auth";
		public static final String CFG_DISABLE_WEAK_MASK = "disable-weak-mask";
		public static final String CFG_REQUIRE_SSL = "require-ssl";
		public static final String CFG_REALM_NAME = "realm-name";
		public static final String CFG_FORM_LOGIN_PAGE = "form-login-page";
		public static final String CFG_FORM_ERROR_PAGE = "form-error-page";
		public static final String CFG_LOGGED_OUT_PAGE = "logged-out-page";
		public static final String CFG_FORM_PATTTERN = "form-protect-pattern";
		public static final String CFG_BASIC_PATTTERN = "basic-protect-pattern";
	    public static final String CFG_ANON_PATTTERN = "anonymous-pattern";
	    public static final String CFG_ZONE = "zone";


}
