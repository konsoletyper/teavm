/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.util;

/**
 *
 * @author bora
 */
public abstract class TEventListenerProxy implements TEventListener {

    private final TEventListener listener;

    public TEventListenerProxy(TEventListener listener) {
        super();
        this.listener = listener;
    }

    public TEventListener getListener() {
        return listener;
    }
}