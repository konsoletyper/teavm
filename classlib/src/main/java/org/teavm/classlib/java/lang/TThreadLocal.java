/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class TThreadLocal<T> extends TObject {
    private Map<Object, Object> map;
    private boolean initialized;
    private T value;
    static final Object ABSENT = new Object();

    public TThreadLocal() {
        super();
    }

    protected T initialValue() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (isInMainThread()) {
            if (!initialized) {
                value = initialValue();
                initialized = true;
            }
            cleanupMap();
            return value;
        } else {
            var key = TThread.currentThread().key;
            initMap();
            var value = map.get(key);
            if (value == null) {
                value = initialValue();
                map.put(key, value == null ? NULL : value);
            } else if (value == NULL) {
                value = null;
            }
            cleanupMap();
            return (T) value;
        }
    }

    public void set(T value) {
        if (isInMainThread()) {
            initialized = true;
            this.value = value;
            cleanupMap();
        } else {
            initMap();
            map.put(TThread.currentThread().key, value == null ? NULL : value);
            cleanupMap();
        }
    }

    public void remove() {
        if (isInMainThread()) {
            initialized = false;
            value = null;
            cleanupMap();
        } else {
            if (map != null) {
                map.remove(TThread.currentThread().key);
                cleanupMap();
            }
        }
    }

    private void initMap() {
        if (map == null) {
            map = new WeakHashMap<>();
        }
    }

    private void cleanupMap() {
        if (map != null && map.isEmpty()) {
            map = null;
        }
    }

    public static <S> TThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new TThreadLocal<>() {
            @Override
            protected S initialValue() {
                return supplier.get();
            }
        };
    }

    /**
     * The current thread's value, or {@link #ABSENT} if it has none. Unlike {@link #get}, this
     * never calls {@link #initialValue}. A new thread inherits only the values its creator has set, as in the JDK.
     */
    final Object rawValue() {
        if (isInMainThread()) {
            return initialized ? value : ABSENT;
        }
        if (map == null) {
            return ABSENT;
        }
        Object v = map.get(TThread.currentThread().key);
        return v == null ? ABSENT : v == NULL ? null : v;
    }

    private static boolean isInMainThread() {
        return TThread.currentThread() == TThread.getMainThread();
    }

    private static final Object NULL = new Object();
}
