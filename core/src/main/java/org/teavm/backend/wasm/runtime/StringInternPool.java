/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.runtime;

public final class StringInternPool {
    private static Entry[] table = new Entry[16];
    private static int occupiedCells;
    private static int occupiedCellsThreshold = table.length * 3 / 4;

    private StringInternPool() {
    }

    public static String query(String s) {
        var hash = s.hashCode();
        var index = Integer.remainderUnsigned(hash, table.length);
        var entry = table[index];
        while (entry != null) {
            if (entry.hash == hash) {
                var value = entry.getValue();
                if (value.equals(s)) {
                    return value;
                }
            }
            entry = entry.next;
        }

        if (table[index] == null) {
            if (++occupiedCells > occupiedCellsThreshold) {
                rehash();
            }
            index = Integer.remainderUnsigned(hash, table.length);
            if (table[index] == null) {
                ++occupiedCells;
            }
        }
        table[index] = new Entry(index, hash, table[index], s);
        return s;
    }

    private static void rehash() {
        var oldTable = table;
        table = new Entry[oldTable.length * 2];
        occupiedCells = 0;
        occupiedCellsThreshold = table.length * 3 / 4;
        for (var value : oldTable) {
            var entry = value;
            while (entry != null) {
                var next = entry.next;
                var index = Integer.remainderUnsigned(entry.hash, table.length);
                entry.index = index;
                entry.next = table[index];
                if (entry.next == null) {
                    ++occupiedCells;
                }
                table[index] = entry;
                entry = next;
            }
        }
    }

    static void remove(Entry entry) {
        var index = entry.index;
        Entry previous = null;
        var e = table[index];
        while (e != null) {
            if (e == entry) {
                if (previous == null) {
                    table[index] = e.next;
                    if (e.next == null) {
                        --occupiedCells;
                    }
                } else {
                    previous.next = e.next;
                }
                break;
            }
            previous = e;
            e = e.next;
        }
    }

    static class Entry {
        int index;
        final int hash;
        Entry next;

        Entry(int index, int hash, Entry next, String str) {
            this.index = index;
            this.hash = hash;
            this.next = next;
            setValue(str);
        }

        final native String getValue();

        final native void setValue(String str);
    }
}
