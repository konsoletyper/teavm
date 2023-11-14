/*
 *  Copyright 2023 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.reflect.TField;

class TReflectionBasedAtomicReferenceFieldUpdater<T, V> extends TAtomicReferenceFieldUpdater<T, V> {
    private TField field;

    TReflectionBasedAtomicReferenceFieldUpdater(TField field) {
        this.field = field;
    }

    @Override
    public boolean compareAndSet(T obj, V expect, V update) {
        checkInstance(obj);
        if (field.getWithoutCheck(obj) != expect) {
            return false;
        }
        field.setWithoutCheck(obj, update);
        return true;
    }

    @Override
    public boolean weakCompareAndSet(T obj, V expect, V update) {
        return compareAndSet(obj, expect, update);
    }

    @Override
    public void set(T obj, V newValue) {
        checkInstance(obj);
        field.setWithoutCheck(obj, newValue);
    }

    @Override
    public void lazySet(T obj, V newValue) {
        set(obj, newValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(T obj) {
        checkInstance(obj);
        return (V) field.getWithoutCheck(obj);
    }

    private void checkInstance(T obj) {
        if (!field.getDeclaringClass().isInstance((TObject) obj)) {
            throw new ClassCastException();
        }
    }
}
