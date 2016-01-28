/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.util.concurrent;

import org.teavm.classlib.java.util.THashMap;

public class TConcurrentHashMap<K,V> extends THashMap<K,V> {

    public TConcurrentHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public TConcurrentHashMap() {
    }

    public TConcurrentHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }
    public TConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        super(initialCapacity, loadFactor);
    }
    public V putIfAbsent(K key, V value) {
        if(this.containsKey(key)) {
            return get(key);
        } else {
            put(key,value);
            return null;
        }
    }
}