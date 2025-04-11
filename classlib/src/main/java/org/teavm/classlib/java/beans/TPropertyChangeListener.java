package org.teavm.classlib.java.beans;

import java.beans.PropertyChangeEvent;
import java.util.EventListener;

public interface TPropertyChangeListener extends EventListener {
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent);
}
