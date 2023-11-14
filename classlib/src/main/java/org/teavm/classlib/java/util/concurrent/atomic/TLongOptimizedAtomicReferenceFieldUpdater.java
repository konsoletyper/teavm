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

abstract class TLongOptimizedAtomicReferenceFieldUpdater<T>
        extends TBaseAtomicReferenceFieldUpdater<T, Long> {
    @Override
    public boolean compareAndSet(T obj, Long expect, Long update) {
        if (getAsLong(obj) != expect) {
            return false;
        }
        set(obj, update.longValue());
        return true;
    }

    @Override
    public void set(T obj, Long newValue) {
        set(obj, newValue.longValue());
    }

    @Override
    public Long get(T obj) {
        return getAsLong(obj);
    }

    abstract long getAsLong(T obj);

    abstract void set(T obj, long value);
}
