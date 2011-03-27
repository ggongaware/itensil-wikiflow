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
 * Created on Aug 27, 2003
 *
 */
package itensil.util;

/**
 * @author ggongaware@itensil.com
 */
public class UriHelper {

    public static final String FILTER_CHARS = ":*?<>|\\/\"!";

    /**
     * Clean out the characters that don't work with Win32 filenames
     * 
     * @param name
     * @return filtered name
     */
    public static String filterName(String name) {
        int len = name.length();
		StringBuffer buf = new StringBuffer(len);
		for (int i=0; i < len; i++) {
		 	char ch = name.charAt(i);
		 	if (FILTER_CHARS.indexOf(ch) >= 0) ch = '_';
		 	buf.append(ch);
		}
		return buf.toString().trim();
	}

    /**
     * Chop the base off the front
     * @param base
     * @param uri
     * @return the path in relation to the base
     */
	public static String localizeUri(String base, String uri) {
		if (Check.isEmpty(base)) return uri;
		if (base.charAt(base.length() - 1) != '/') base += '/';
		if (uri.startsWith(base)) {
			uri = uri.substring(base.length());
			while (uri.startsWith("/")) uri = uri.substring(1);
		}
		return uri;
	}

    /**
     * Get the last path element
     * @param uri
     * @return the last path element
     */
	public static String name(String uri) {

		int pos = uri.lastIndexOf('/');
		if (pos >= 0) {
			return uri.substring(pos+1);
		} else {
			return uri;
		}
	}

    /**
     * Get the first path element
     * @param uri
     * @return the first path element
     */
    public static String getRoot(String uri) {

        int start = 0;
        if (uri.charAt(0) == '/') start++;
        int pos = uri.indexOf('/', start);
        if (pos > 0) {
            return uri.substring(0, pos);
        } else {
            return uri;
        }
    }

    /**
     * Get the first path element
     * @param uri
     * @return the first path element, null if no-path
     */
    public static String getRootPath(String uri) {

        int start = 0;
        if (uri.length() == 0) return null;
        if (uri.charAt(0) == '/') start++;
        int pos = uri.indexOf('/', start);
        if (pos > 0) {
            return uri.substring(0, pos);
        } else {
            return null;
        }
    }


    /**
     * Prepend the base to a local URI
     * @param base
     * @param uri
     * @return full uri with base + local
     */
	public static String absoluteUri(String base, String uri) {

		if (uri.startsWith("/")) {
			return uri;
		} else if (uri.indexOf("://") > 0) {
			return uri;
		} else {
			if (base.endsWith("/"))  {
				return base + uri;
			}
			return base + '/' + uri;
		}
	}

    /**
     * Get up to the last path element
     * @param uri
     * @return up to the last path element
     */
	public static String getParent(String uri) {

		while (uri.endsWith("/")) {
			uri = uri.substring(0,uri.length()-1);
		}
		int pos = uri.lastIndexOf('/');
		if (pos > 0) {
			return uri.substring(0,pos);
		} else {
			return "";
		}
	}

    /**
     * @param uri
     */
    public static int getDepth(String uri) {
        int depth = 0;

        // skip char 0
        for (int i = 1; i < uri.length(); i++) {
            if (uri.charAt(i) == '/') depth++;
        }
        return depth;
    }

    /**
     *
     * @param uri
     *
     * Examples:
     *  reduce("/root/../test.txt") = "/test.txt"
     *  reduce("/root/sub/../test.txt") = "/root/test.txt"
     */
    public static String reduce(String uri) {

        int pos;
        if (uri.startsWith("..") || (pos = uri.indexOf("../")) < 0) {
			return uri;
		}

        do {
            uri = getParent(uri.substring(0, pos)) + uri.substring(pos + 2);
        } while ((pos = uri.indexOf("../")) >= 0);

        return uri;
    }

    /**
     * Examples:
     *
     *  shrinkName("Eat at joes", 8, 2) = "EatAtJoes"
     *  shrinkName("Eat meat", 8, 2) = "Eat meat"
     *  shrinkName("I like oatmeal", 8, 2) = "ILikeOat"
     *
     * @param name
     * @param shrinkLen
     * @param tolerance
     * @return a shorter version of the name if required
     */
    public static String shrinkName(String name, int shrinkLen, int tolerance) {
        StringBuffer buf = new StringBuffer(shrinkLen);
        int len = name.length();
        if (len <= shrinkLen) {
            return name;
        }
        boolean nextCap = false;
        int j = 0;
        int i;
        for (i=0; i < len; i++) {
            char ch = name.charAt(i);
            if (Character.isWhitespace(ch)) {
                // skip and make next character capital
                nextCap = true;
            } else {
                if (nextCap) {
                    buf.append(Character.toUpperCase(ch));
                    nextCap = false;
                } else {
                    buf.append(ch);
                }
                if (++j >= shrinkLen) {
                    if (len - i > tolerance) {
                        break;
                    }
                }
            }
        }
        return buf.toString();
    }

    /**
     * Find the relative from one uri to another.
     * Examples:
     *      relativePath("parent/kid", "parent/file.txt") = "../file.txt"
     *      relativePath("/parent/kid2", "/parent/kid1/file.txt") = "../kid1/file.txt"
     *      relativePath("/parent/kid2/sub", "/parent/kid1/file.txt") = "../../kid1/file.txt"
     *      relativePath("/parent/kid", "/parent/kid/sub/file.txt") = "sub/file.txt"
     *      relativePath("/parent/kid2", "/parent/kid1/sub/file.txt") = "../kid1/sub/file.txt"
     *
     * @param fromUri starting path
     * @param toUri ending file
     * @return null if no relativity
     */
    public static String relativePath(String fromUri, String toUri) {

        if (fromUri.length() == 0 || toUri.length() == 0 || fromUri.charAt(0) != toUri.charAt(0)) {
            return null;
        }
        if (!fromUri.endsWith("/")) fromUri = fromUri + "/";
        String fRem = fromUri;
        String tRem = toUri;
        String fRoot = getRootPath(fRem);
        String tRoot = getRootPath(tRem);
        int match = 0;
        while (fRoot != null && fRoot.equals(tRoot)) {
            fRem = fRem.substring(fRoot.length() + 1);
            if (tRoot != null ) tRem = tRem.substring(tRoot.length() + 1);
            fRoot = getRootPath(fRem);
            tRoot = tRoot == null ? null : getRootPath(tRem);
            match++;
        }
        if (match > 0) {
            if (fRoot == null) {
                return tRem;
            } else {
                StringBuffer buf = new StringBuffer();
                for (int d = getDepth(fRem); d > 0; d--) buf.append("../");
                buf.append(tRem);
                return buf.toString();
            }
        } else if (toUri.charAt(0) == '/') {
            return toUri;
        }
        return null;
    }

	public static String getExtension(String uri) {
		int pos = uri.lastIndexOf('.');
		if (pos > 0) {
			return uri.substring(pos + 1);
		}
		return "";
	}

	/**
	 * 
	 * @param uri
	 * @return
	 */
	public static String shiftPath(String uri) {
		int pos = uri.indexOf('/', 1);
		if (pos > 0) {
			uri = uri.substring(pos);
		}
		return uri;
	}

}
