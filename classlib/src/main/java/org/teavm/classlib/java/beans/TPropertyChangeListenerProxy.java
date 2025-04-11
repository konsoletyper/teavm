package org.teavm.classlib.java.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListenerProxy;

public class TPropertyChangeListenerProxy extends EventListenerProxy<PropertyChangeListener> implements PropertyChangeListener {

    private final String propertyName;

    public TPropertyChangeListenerProxy(final String propertyName, final PropertyChangeListener propertyChangeListener) {
        super(propertyChangeListener);
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        getListener().propertyChange(propertyChangeEvent);
    }

}
