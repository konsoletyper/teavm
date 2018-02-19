/*
 *  Copyright 2018 martin below.
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
package org.teavm.classlib.java.util.concurrent.atomic;

import java.io.Serializable;
import org.teavm.classlib.java.util.TObjects;

/**
 * @author martin below
 */
public class TAtomicReference<T> implements Serializable {
    private T value;

    public final boolean compareAndSet(T expect, final T update) {
        if (!TObjects.equals(value, expect)) {
            return false;
        }
        value = update;
        return true;
    }

    public final boolean weakCompareAndSet(final T expect, final T update) {
        return compareAndSet(expect, update);
    }

    public final T get() {
        return value;
    }

    public final void set(final T newValue) {
        value = newValue;
    }

    public final void lazySet(final T newValue) {
        set(newValue);
    }

    public final T getAndSet(final T update) {
        final T result = value;
        value = update;
        return result;
    }
}
