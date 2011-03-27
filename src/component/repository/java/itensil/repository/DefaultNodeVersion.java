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
 * Created on Dec 8, 2003
 *
 */
package itensil.repository;

import java.io.Serializable;

/**
 * @author ggongaware@itensil.com
 *
 */
public class DefaultNodeVersion implements NodeVersion, Serializable {


    protected String number;
    protected String label;
    protected boolean isDefault;

    /**
     *
     * @param number
     * @param label
     * @param isDefault
     */
    public DefaultNodeVersion(String number, String label, boolean isDefault) {
        this.number = number;
        this.label = label;
        this.isDefault = isDefault;
    }

    /**
     * This number
     * @param number
     */
    public DefaultNodeVersion(String number, boolean isDefault) {
        this(number, null, isDefault);
    }

    /**
     * Just the default version
     */
    public DefaultNodeVersion() {
        this(null, null, true);
    }

    /*
     * @see itensil.repository.NodeVersion#isDefault()
     */
    public boolean isDefault() {
        return isDefault;
    }

    /*
     * @see itensil.repository.NodeVersion#getNumber()
     */
    public String getNumber() {
        return number;
    }

    /*
     * @see itensil.repository.NodeVersion#getLabel()
     */
    public String getLabel() {
        return label;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (number != null) {
            buf.append(number);
        }
        if (label != null) {
            if (buf.length() > 0) buf.append(' ');
            buf.append('\'');
            buf.append(label);
            buf.append("' ");
        }
        if (isDefault) {
            if (buf.length() > 0) buf.append(' ');
            buf.append("default");
        }
        return buf.toString();
    }

    /**
     * Increment this version for next version
     *
     * @param currentVersion
     * @param setDefault
     * @return version object
     */
    public static NodeVersion nextVersion(
        NodeVersion currentVersion, boolean setDefault) {

        return nextVersion(currentVersion.getNumber(), setDefault);
    }

    /**
     * @param current
     * @param setDefault
     * @return version object
     */
    public static NodeVersion nextVersion(String current, boolean setDefault) {
        String sNum = current;
        String prefix = "";
        int idx;
        if ((idx = current.lastIndexOf('.')) >= 0) {
            prefix = current.substring(0, idx+1);
            sNum = current.substring(idx+1);
        }
        int bNum = Integer.parseInt(sNum.trim());
        bNum++;
        return new DefaultNodeVersion(
            prefix + String.valueOf(bNum), "", setDefault);
    }


}
