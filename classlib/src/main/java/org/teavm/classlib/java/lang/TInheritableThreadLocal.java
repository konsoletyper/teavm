/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class TInheritableThreadLocal<T> extends TThreadLocal<T> {
    // Each instance registers here, and a new thread takes its creator's values (TThread#inherited). The set
    // is weak, so an instance that nothing else holds can be collected. It stays null until the first instance:
    // TThread reads it in its constructor, and a field initializer here would run inside Thread's class
    // initialization.
    private static Set<TInheritableThreadLocal<?>> all;

    public TInheritableThreadLocal() {
        if (all == null) {
            all = Collections.newSetFromMap(new WeakHashMap<>());
        }
        all.add(this);
    }

    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     * The values a new thread inherits from the current thread, as pairs (instance, child value), or null if
     * there are none. The array may end with null entries. Only values the current thread holds count, as in the
     * JDK.
     */
    @SuppressWarnings("unchecked")
    static Object[] snapshot() {
        if (all == null || all.isEmpty()) {
            return null;
        }
        Object[] pairs = new Object[all.size() * 2];
        int count = 0;
        for (TInheritableThreadLocal<?> local : all) {
            Object v = local.rawValue();
            if (v != ABSENT) {
                pairs[count++] = local;
                pairs[count++] = v;
            }
        }
        if (count == 0) {
            return null;
        }
        // childValue is user code, so it runs after the loop: it may create another InheritableThreadLocal,
        // which changes the set.
        for (int i = 0; i < count; i += 2) {
            pairs[i + 1] = ((TInheritableThreadLocal<Object>) pairs[i]).childValue(pairs[i + 1]);
        }
        return pairs;
    }
}
