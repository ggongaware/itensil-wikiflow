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

import java.util.Locale;
import java.util.TimeZone;
import java.util.StringTokenizer;

/**
 * @author ggongaware@itensil.com
 *
 */
public class LocaleHelper {

    /**
     * Reads underscored seperated Locale
     * "en_US_POSIX"
     *
     * @param s
     * @return default on empty
     */
    public static Locale readLocal(String s) {

        if (Check.isEmpty(s)) {
            return Locale.getDefault();
        }
        StringTokenizer tok = new StringTokenizer(s.trim(), "_");
        String language = "";
        String country = "";
        String variant = "";
        if (tok.hasMoreTokens()) {
            language = tok.nextToken();
        }
        if (tok.hasMoreTokens()) {
            country = tok.nextToken();
        }
        if (tok.hasMoreTokens()) {
            variant = tok.nextToken();
        }
        return new Locale(language, country, variant);
    }

    /**
     * @param s
     * @return default on empty
     */
    public static TimeZone readTimeZone(String s) {

        if (Check.isEmpty(s)) {
            return TimeZone.getDefault();
        }
        return TimeZone.getTimeZone(s);
    }

}
