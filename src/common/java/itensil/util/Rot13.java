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
 * Created on Oct 31, 2003
 *
 */
package itensil.util;

/**
 * @author ggongaware@itensil.com
 *
 * Make Strings different.
 *
 */
public class Rot13 {


	public static String rot13(String s) {
		char chs[] = s.toCharArray();
		for (int i=0; i < chs.length; i++) {
			chs[i] = rot13(chs[i]);
		}
		return new String(chs);
	}

	public static char rot13(char c) {
		if((c >= 'A') && (c <= 'Z')) {
			c += 13;
			if(c > 'Z') {
					c -= 26;
			}
		} else if((c >= 'a') && ( c <= 'z')) {
			c += 13;
			if(c > 'z') {
					c -= 26;
			}
		}
		return c;
	}

}
