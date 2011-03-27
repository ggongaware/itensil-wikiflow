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
 * Created on Jan 23, 2004
 *
 */
package itensil.util;

import java.util.ArrayList;


/**
 * @author ggongaware@itensil.com
 *
 * Generic Stack with a String path
 */
public class Stack<E> extends ArrayList<E> {


    /**
     * @param obj
     */
    public void push(E obj) {
        add(obj);
    }

    /**
     * @return remove and return top object on stack
     */
    public E pop() {
        return remove(size() - 1);
    }

    /**
     * @return top object on stack
     */
    public E peek() {
        return get(size() - 1);
    }

    /**
     * @param seperator
     * @return a string of all the objects.toString() in the stack
     */
    public String getPath(char seperator) {

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < size(); i++) {
            buf.append(get(i));
            buf.append(seperator);
        }
        buf.setLength(buf.length() - 1);
        return buf.toString();
    }

}
