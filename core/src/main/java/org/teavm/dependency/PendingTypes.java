/*
 *  Copyright 2026 konsoletyper.
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
package org.teavm.dependency;

import java.util.Arrays;
import java.util.BitSet;

// Set of type indices waiting to be propagated through a transition. Small sets are stored as an array which
// may contain duplicates: they are rare, so instead of checking each added element, duplicates are removed
// when the array gets full. Type indices are dense, so large sets are stored as a bit set.
final class PendingTypes {
    private static final int INITIAL_SIZE = 4;
    private static final int SMALL_LIMIT = 16;

    private int[] small = new int[INITIAL_SIZE];
    private int size;
    private BitSet bits;

    boolean contains(int index) {
        if (bits != null) {
            return bits.get(index);
        }
        for (int i = 0; i < size; ++i) {
            if (small[i] == index) {
                return true;
            }
        }
        return false;
    }

    void add(int index) {
        if (bits != null) {
            bits.set(index);
            return;
        }
        if (size == small.length) {
            compact();
            // Require compaction to free at least half of the array, otherwise we would compact too often
            if (size > small.length / 2) {
                if (small.length < SMALL_LIMIT) {
                    small = Arrays.copyOf(small, small.length * 2);
                } else {
                    switchToBits(index);
                    return;
                }
            }
        }
        small[size++] = index;
    }

    private void switchToBits(int index) {
        int max = index;
        for (int i = 0; i < size; ++i) {
            max = Math.max(max, small[i]);
        }
        bits = new BitSet(max + 1);
        for (int i = 0; i < size; ++i) {
            bits.set(small[i]);
        }
        bits.set(index);
        small = null;
    }

    // Sorts the array and removes duplicates
    private void compact() {
        if (!isSorted()) {
            Arrays.sort(small, 0, size);
        }
        if (size == 0) {
            return;
        }
        int unique = 1;
        for (int i = 1; i < size; ++i) {
            if (small[i] != small[unique - 1]) {
                small[unique++] = small[i];
            }
        }
        size = unique;
    }

    private boolean isSorted() {
        for (int i = 1; i < size; ++i) {
            if (small[i - 1] > small[i]) {
                return false;
            }
        }
        return true;
    }

    // Returns distinct indices in ascending order.
    int[] toArray() {
        if (bits == null) {
            compact();
            return Arrays.copyOf(small, size);
        }
        int[] result = new int[bits.cardinality()];
        int j = 0;
        for (int index = bits.nextSetBit(0); index >= 0; index = bits.nextSetBit(index + 1)) {
            result[j++] = index;
        }
        return result;
    }
}
