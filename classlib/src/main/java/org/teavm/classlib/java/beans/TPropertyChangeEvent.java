package org.teavm.classlib.java.beans;

import java.util.EventObject;

public class TPropertyChangeEvent extends EventObject {

    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;
    private Object propagationId;

    public TPropertyChangeEvent(final Object source, final String propertyName, final Object oldValue, final Object newValue) {
        super(source);
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setPropagationId(Object propagationId) {
        this.propagationId = propagationId;
    }

    public Object getPropagationId() {
        return propagationId;
    }

    public String toString() {
        return "[propertyName=" + propertyName + "; oldValue=" + oldValue + "; newValue=" + newValue + "; propagationId=" + propagationId + "; source=" + source + "]";
    }
}
