package org.teavm.classlib.java.beans;

import java.beans.PropertyChangeEvent;

public class TIndexedPropertyChangeEvent extends PropertyChangeEvent {

    private final int index;

    public TIndexedPropertyChangeEvent(final Object source, final String propertyName, final Object oldValue, final Object newValue, final int index) {
        super(source, propertyName, oldValue, newValue);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

}
