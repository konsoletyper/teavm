/*
 *  Copyright 2020 adam.
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
package org.teavm.parsing.substitution;

import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class OrderedProperties extends Properties {
    private Vector<Object> keys;

    public OrderedProperties() {
        keys = new Vector<>();
    }

    @Override
    public Object put(Object key, Object value) {
        keys.remove(key);
        keys.add(key);
        return super.put(key, value);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return keys.elements();
    }

    @Override
    public Set<String> stringPropertyNames() {
        Set<String> stringPropertyNames = new LinkedHashSet<>(keys.size());
        for (Object key : keys) {
            if (key instanceof String) {
                stringPropertyNames.add((String) key);
            }
        }
        return stringPropertyNames;
    }

    @Override
    public Object remove(Object key) {
        keys.remove(key);
        return super.remove(key);
    }
}
