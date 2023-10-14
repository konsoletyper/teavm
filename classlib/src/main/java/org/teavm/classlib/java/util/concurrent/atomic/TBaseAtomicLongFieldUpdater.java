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

abstract class TBaseAtomicLongFieldUpdater<T> extends TAtomicLongFieldUpdater<T> {
    @Override
    public boolean compareAndSet(T obj, long expect, long update) {
        if (get(obj) != expect) {
            return false;
        }
        set(obj, update);
        return true;
    }

    @Override
    public boolean weakCompareAndSet(T obj, long expect, long update) {
        return compareAndSet(obj, expect, update);
    }

    @Override
    public void lazySet(T obj, long newValue) {
        set(obj, newValue);
    }
}
