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
/*
 * Created on Dec 31, 2003
 *
 */
package itensil.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * @author ggongaware@itensil.com
 *
 */
public class Check {

    public static boolean isEmpty(String s) {
        if (s == null) {
            return true;
        } else {
            return s.length() < 1;
        }
    }

    public static boolean isEmpty(Object o) {
        if (o == null) {
            return true;
        } else if (o instanceof String) {
            return ((String)o).length() < 1;
        } else if (o instanceof Collection) {
            return ((Collection)o).isEmpty();
        } else if (o instanceof Map) {
            return ((Map)o).isEmpty();
        } else if (o.getClass().isArray()) {
            return Array.getLength(o) < 1;
        } else {
            return false;
        }
    }
    
    public static String emptyIfNull(String val) {
    	return val == null ? "" : val;
    }
    	

    public static String maxLength(String val, int length) {
        if (val != null && val.length() > length) {
            return val.substring(0, length);
        }
        return val;
    }

	public static boolean equalStrings(String str1, String str2) {
		if (str1 != null) return str1.equals(str2);
		if (str2 != null) return str2.equals(str1);
		return true;
	}
}
