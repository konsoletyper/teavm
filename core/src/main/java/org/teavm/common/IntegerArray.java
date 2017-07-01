/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.common;

import java.util.Arrays;

public class IntegerArray {
    private static final int[] emptyData = new int[0];
    private int[] data;
    private int sz;

    private IntegerArray() {
    }

    public IntegerArray(int capacity) {
        this.data = new int[capacity];
    }

    public static IntegerArray of(int... values) {
        IntegerArray array = new IntegerArray();
        array.data = Arrays.copyOf(values, values.length);
        array.sz = values.length;
        return array;
    }

    public void clear() {
        sz = 0;
    }

    public void optimize() {
        if (sz > data.length) {
            data = Arrays.copyOf(data, sz);
        }
    }

    public int[] getAll() {
        return sz > 0 ? Arrays.copyOf(data, sz) : emptyData;
    }

    public int get(int index) {
        return data[index];
    }

    public int[] getRange(int start, int end) {
        return Arrays.copyOfRange(data, start, end);
    }

    public void set(int index, int value) {
        if (index >= sz) {
            throw new IndexOutOfBoundsException("Index " + index + " is greater than the list size " + sz);
        }
        data[index] = value;
    }

    private void ensureCapacity() {
        if (sz <= data.length) {
            return;
        }
        int newCap = data.length;
        while (sz > newCap) {
            newCap = newCap * 3 / 2 + 1;
        }
        data = Arrays.copyOf(data, newCap);
    }

    public int size() {
        return sz;
    }

    public void addAll(int[] items) {
        int target = sz;
        sz += items.length;
        ensureCapacity();
        System.arraycopy(items, 0, data, target, items.length);
    }

    public void add(int item) {
        ++sz;
        ensureCapacity();
        data[sz - 1] = item;
    }

    public void remove(int index) {
        remove(index, 1);
    }

    public void remove(int index, int count) {
        System.arraycopy(data, index + count, data, index, sz - index - count);
        sz -= count;
    }

    public boolean contains(int item) {
        for (int i = 0; i < sz; ++i) {
            if (data[i] == item) {
                return true;
            }
        }
        return false;
    }
}
