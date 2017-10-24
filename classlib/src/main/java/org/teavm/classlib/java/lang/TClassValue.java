/*
 *  Copyright 2013 Alexey Andreev.
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

public abstract class TClassValue<T> extends TObject {
    private Item items;

    protected TClassValue() {
    }

    protected abstract T computeValue(Class<?> clazz);

    public T get(Class<?> clazz) {
        Item it = items;
        while (it != null) {
            if (it.clazz == clazz) {
                return it.value;
            }
            it = it.next;
        }
        T value = computeValue(clazz);
        items = new Item(items, clazz, value);
        return value;
    }

    private class Item {
        final Class<?> clazz;
        final T value;
        final Item next;

        Item(Item next, Class<?> clazz, T value) {
            this.clazz = clazz;
            this.value = value;
            this.next = next;
        }
    }
}
