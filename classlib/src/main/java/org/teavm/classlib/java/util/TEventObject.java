/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.util;

public class TEventObject {
 
    protected Object source;

    public TEventObject(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[source=" + source + "]";
    }
}