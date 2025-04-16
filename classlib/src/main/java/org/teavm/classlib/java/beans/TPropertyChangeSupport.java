/*
 *  Copyright 2025 pizzadox9999.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.beans;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TPropertyChangeSupport {

    private final Object sourceBean;

    private final HashMap<String, ArrayList<PropertyChangeListener>> eventToListenerMap = new HashMap<>();

    public TPropertyChangeSupport(final Object sourceBean) {
        this.sourceBean = sourceBean;
    }

    public synchronized void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        if (propertyChangeListener == null) {
            return;
        }
        if (propertyChangeListener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy propertyChangeListenerProxy = (PropertyChangeListenerProxy) propertyChangeListener;
            addPropertyChangeListener(propertyChangeListenerProxy.getPropertyName(), propertyChangeListenerProxy.getListener());
        } else {
            addToMap(null, propertyChangeListener);
        }
    }

    public synchronized void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
        if (propertyChangeListener == null) {
            return;
        }
        if (propertyChangeListener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy propertyChangeListenerProxy = (PropertyChangeListenerProxy) propertyChangeListener;
            removePropertyChangeListener(propertyChangeListenerProxy.getPropertyName(), propertyChangeListenerProxy.getListener());
        } else {
            removeFromMap(null, propertyChangeListener);
        }
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        final ArrayList<PropertyChangeListener> propertyChangeListeners = new ArrayList<>();
        for (final String key : eventToListenerMap.keySet()) {
            propertyChangeListeners.addAll(eventToListenerMap.get(key));
        }
        return propertyChangeListeners.toArray(new PropertyChangeListener[propertyChangeListeners.size()]);
    }

    public synchronized void addPropertyChangeListener(final String propertyName, final PropertyChangeListener propertyChangeListener) {
        if (propertyName == null || propertyChangeListener == null) {
            return;
        }
        addToMap(propertyName, unwrap(propertyChangeListener));
    }

    public synchronized void removePropertyChangeListener(final String propertyName, final PropertyChangeListener propertyChangeListener) {
        if (propertyName == null || propertyChangeListener == null) {
            return;
        }
        removeFromMap(propertyName, unwrap(propertyChangeListener));
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners(final String propertyName) {
        if (propertyName == null) {
            return new PropertyChangeListener[0];
        }
        final List<PropertyChangeListener> propertyChangeListeners = eventToListenerMap.get(propertyName);
        if (propertyChangeListeners == null) {
            return new PropertyChangeListener[0];
        }
        return propertyChangeListeners.toArray(new PropertyChangeListener[propertyChangeListeners.size()]);
    }

    public void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
        firePropertyChange(new PropertyChangeEvent(sourceBean, propertyName, oldValue, newValue));
    }

    public void firePropertyChange(final String propertyName, final int oldValue, final int newValue) {
        firePropertyChange(propertyName, oldValue, newValue);
    }

    public void firePropertyChange(final String propertyName, final boolean oldValue, final boolean newValue) {
        firePropertyChange(propertyName, oldValue, newValue);
    }

    public synchronized void firePropertyChange(final PropertyChangeEvent event) {
        if (event == null || event.getOldValue() == null || event.getNewValue() == null || event.getOldValue().equals(event.getNewValue()) == false) {
            List<PropertyChangeListener> fireList = new ArrayList<>();
            if (eventToListenerMap.get(null) != null) {
                fireList.addAll(eventToListenerMap.get(null));
            }
            if (eventToListenerMap.get(event.getPropertyName()) != null) {
                fireList.addAll(eventToListenerMap.get(event.getPropertyName()));
            }
            for (PropertyChangeListener propertyChangeListener : fireList) {
                propertyChangeListener.propertyChange(event);
            }
        }
    }

    public void fireIndexedPropertyChange(final String propertyName, final int index, final Object oldValue, final Object newValue) {
        firePropertyChange(new IndexedPropertyChangeEvent(sourceBean, propertyName, oldValue, newValue, index));
    }

    public void fireIndexedPropertyChange(final String propertyName, final int index, final int oldValue, final int newValue) {
        fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    public void fireIndexedPropertyChange(final String propertyName, final int index, final boolean oldValue, final boolean newValue) {
        fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

    public synchronized boolean hasListeners(final String propertyName) {
        return propertyName != null && eventToListenerMap.get(propertyName) != null && eventToListenerMap.get(propertyName).size() != 0;
    }

    private void addToMap(final String key, final PropertyChangeListener propertyChangeListener) {
        ArrayList<PropertyChangeListener> propertyChangeListeners = eventToListenerMap.get(key);
        if (propertyChangeListeners == null) {
            propertyChangeListeners = new ArrayList<>();
            eventToListenerMap.put(key, propertyChangeListeners);
        }
        propertyChangeListeners.add(propertyChangeListener);
    }

    private void removeFromMap(final String key, final PropertyChangeListener propertyChangeListener) {
        final ArrayList<PropertyChangeListener> propertyChangeListeners = eventToListenerMap.get(key);
        if (propertyChangeListeners == null) {
            return;
        }
        propertyChangeListeners.remove(propertyChangeListener);
    }

    private static PropertyChangeListener unwrap(PropertyChangeListener propertyChangeListener) {
        while (propertyChangeListener instanceof PropertyChangeListenerProxy) {
            if (((PropertyChangeListenerProxy) propertyChangeListener).getListener() != null) {
                propertyChangeListener = ((PropertyChangeListenerProxy) propertyChangeListener).getListener();
            }
        }
        return propertyChangeListener;
    }

}
