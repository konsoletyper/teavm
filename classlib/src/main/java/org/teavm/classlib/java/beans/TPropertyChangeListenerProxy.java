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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListenerProxy;

public class TPropertyChangeListenerProxy extends EventListenerProxy<PropertyChangeListener>
        implements PropertyChangeListener {

    private final String propertyName;

    public TPropertyChangeListenerProxy(final String propertyName,
            final PropertyChangeListener propertyChangeListener) {
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
