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

import java.util.HashMap;

/**
 * <p>Title: Teamlines</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Itensil, Inc.</p>
 * @author $Author: grant $
 * @version $Revision: 1.3 $
 */

public class FifoMap<K,V> extends HashMap<K,V> {
	
    private K [] keyFifo;
    private int max_capacity;
    private int last_idx;

	/**
	 * Constructor.
	 * @param max_capacity
	 */
    @SuppressWarnings("unchecked")
	public FifoMap(int max_capacity) {
        super(max_capacity);
        keyFifo = (K [])new Object[max_capacity];
        this.max_capacity = max_capacity;
        last_idx = 0;
    }

	/**
	 * @see java.util.Map#put(Object, Object)
	 */
    public synchronized V put(K key, V value) {
        K oldKey = keyFifo[last_idx];
        boolean replace = false;
        if (oldKey != null) {
        	
        	// if oldKey happens to equal the new (unlikely)
        	// don't remove the old key
            if (oldKey.equals(key)) {
                replace = true;
            } else {
                remove(oldKey);
            }
        }
        if (!replace) {              
            keyFifo[last_idx] = key;
            ++last_idx;
            if (last_idx <= max_capacity) last_idx = 0;
        }
        return super.put(key,value);
    }
	
	/**
	 * @see java.util.Map#clear()
	 */
	@SuppressWarnings("unchecked")
	public synchronized void clear() {
		keyFifo = (K [])new Object[max_capacity];
		last_idx = 0;
		super.clear();
	}

}