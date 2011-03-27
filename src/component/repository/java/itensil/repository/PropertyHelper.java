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
 * Created on Jan 19, 2004
 *
 */
package itensil.repository;

import itensil.security.SecurityAssociation;
import itensil.util.Base64;
import itensil.util.Check;

import javax.xml.namespace.QName;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author ggongaware@itensil.com
 *
 */
public class PropertyHelper {

    public static final String DEFAULT_QNAMESPACE = "DAV:";
    public static final String DEFAULT_PREFIX = "d";
    public static final String
        ITENSIL_QNAMESPACE = "http://itensil.com/repository";
    public static final String ITENSIL_PREFIX = "ir";
    public static final String
    	ITENSIL_ENTITY_QNAMESPACE = "http://itensil.com/ns/entity";
    public static final String
		ITENSIL_ENTITY_PREFIX  = "ie";


    public static class QNamespaces {

        HashMap<String,String> map;
        int unknown;
        boolean noPrefixDefault;

        public QNamespaces() {
            this(false);
        }

        public QNamespaces(boolean noPrefixDefault) {
            map = new HashMap<String,String>();
            if (!noPrefixDefault) map.put(DEFAULT_QNAMESPACE, DEFAULT_PREFIX);
            map.put(ITENSIL_QNAMESPACE, ITENSIL_PREFIX);
            map.put(ITENSIL_ENTITY_QNAMESPACE, ITENSIL_ENTITY_PREFIX);
            this.noPrefixDefault = noPrefixDefault;
            unknown = 0;
        }

        public void addPrefix(String namespaceUri, String prefix) {

            if (!DEFAULT_QNAMESPACE.equals(namespaceUri)
                && !ITENSIL_QNAMESPACE.equals(namespaceUri)
                && !ITENSIL_ENTITY_QNAMESPACE.equals(namespaceUri)) {
                map.put(namespaceUri, prefix);
            }
        }

        public boolean hasPrefix(String namespaceUri) {
            return map.containsKey(namespaceUri);
        }

        public String getPrefix(String namespaceUri) {
            String pre = map.get(namespaceUri);
            if (Check.isEmpty(pre)) {
                pre = "ns" + unknown++;
                map.put(namespaceUri, pre);
            }
            return pre;
        }

        public String fullName(QName name) {
            return fullName(name.getNamespaceURI(), name.getLocalPart());
        }

        public String fullName(String namespaceUri, String localName) {
            if (noPrefixDefault && DEFAULT_QNAMESPACE.equals(namespaceUri)) {
                return localName;
            }
            String pre = getPrefix(namespaceUri);
            return pre + ":" + localName;
        }

    }

    /**
     * HTTP date format.
     */
    private static final SimpleDateFormat dateHttpFormat =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    /**
     * Simple date format for the internal date (string sortable)
     */
    private static final SimpleDateFormat dateInternalFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Date formats using for Date parsing.
     */
    private static final SimpleDateFormat dateFormats[] = {
        // most common
        dateInternalFormat,
        dateHttpFormat,

        // others
        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"),
        new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy"),
        new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy"),
        new SimpleDateFormat("MM/dd/yy HH:mm:ss")};

    // set all format timezones to GMT
    static {
        TimeZone tzGmt = TimeZone.getTimeZone("GMT");
        dateHttpFormat.setTimeZone(tzGmt);
        dateInternalFormat.setTimeZone(tzGmt);
        for (SimpleDateFormat dateFormat : dateFormats) {
            dateFormat.setTimeZone(tzGmt);
        }
    }

    /**
     * MD5 message digest provider.
     */
    private static MessageDigest md5Digester;
    static {
        // Load the MD5 helper used to calculate signatures.
        try {
            md5Digester = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            throw new IllegalStateException(nsae.getMessage());
        }
    }

    /**
     * Read in potential date strings
     * @param dateStr
     * @return parsed date object or null if not recognized
     */
    public static Date parseDate(String dateStr) {
        if (Check.isEmpty(dateStr)) return null;
        for (SimpleDateFormat dateFormat : dateFormats) {
            Date dVal;
            try {
                dVal = dateFormat.parse(dateStr);
                if (dVal != null)
                    return dVal;
            } catch (ParseException e) {
                // eat it
            }
        }
        return null;
    }

    /**
     * Get the date in the standard internal format
     * @param dVal
     * @return insternaly formated date string
     */
    public static String dateString(Date dVal) {

        return dateInternalFormat.format(dVal);
    }

    /**
     * Get the date for HTTP headers
     * @param dVal
     * @return string in http format
     */
    public static String httpDateString(Date dVal) {

        return dateHttpFormat.format(dVal);
    }

    /**
     * Set some common (webdav) properties
     * @param props
     */
    public static void setStandardProperties(
        NodeProperties props,
        String uri,
        String mimeType,
        int contentLength
        ) {

        String vNum = props.getVersion().getNumber();
        String modified = dateString(new Date());
        props.setValue("getcontentlength", String.valueOf(contentLength));
        props.setValue("getlastmodified", modified);
        if (mimeType != null) {
            props.setValue("getcontenttype", mimeType);
        }
        props.setValue("getcontentlanguage", Locale.getDefault().getLanguage());
        props.setValue("getetag", makeEtag(uri, vNum, modified, contentLength));
        props.setValue(
            itensilQName("modifier"),
            SecurityAssociation.getUser().getUserId());
    }

    public static String makeEtag(

        String uri, String version, String modified, int contentLength) {
        String tagParts = uri.hashCode() + ":" + version + ":" + modified
            + ":" + contentLength;
        return new String(
            Base64.encode(md5Digester.digest(tagParts.getBytes()), true));
    }

    /**
     * Build a QName with itensil namespace URI
     * @param localName
     * @return QName in the itensil space
     */
    public static QName itensilQName(String localName) {
        return new QName(ITENSIL_QNAMESPACE, localName);
    }
    
    /**
     * Build a QName with itensil entity namespace URI
     * @param localName
     * @return QName in the itensil entity space
     */
    public static QName itensilEntityQName(String localName) {
        return new QName(ITENSIL_ENTITY_QNAMESPACE, localName);
    }

    /**
     * Build a QName with itensil namespace URI
     * @param localName
     * @return QName in the default space
     */
    public static QName defaultQName(String localName) {
        return new QName(DEFAULT_QNAMESPACE, localName);
    }

    public static void mergeProperties(
        NodeProperties dstProps, NodeProperties mergeProps) {

        Map<QName,String> props = mergeProps.getPropertyMap();
        for (Map.Entry<QName,String> ent : props.entrySet()) {
            dstProps.setValue(ent.getKey(), ent.getValue());
        }
    }
    
    /**
     * Add/replace property values on a node, creates a DefaultVersion NodeProperties, if
     * none exists.
     * 
     * @param node
     * @param props
     * @throws AccessDeniedException 
     * @throws LockException 
     */
    public static void setNodeValues(MutableRepositoryNode node, Map<QName,String> props) 
    		throws AccessDeniedException, LockException {
    	
    	DefaultNodeVersion ver = new DefaultNodeVersion();
    	NodeProperties dstProps = node.getProperties(ver);
    	if (dstProps == null) {
    		dstProps = new DefaultNodeProperties(ver);
    	}
    	for (Map.Entry<QName,String> ent : props.entrySet()) {
            dstProps.setValue(ent.getKey(), ent.getValue());
        }
    	node.setProperties(dstProps);
    }

}
