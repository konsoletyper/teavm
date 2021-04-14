/*
 *  Copyright 2021 Alexey Andreev.
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

public final class HashUtils {
    private HashUtils() {
    }

    public static String[] createHashTable(String[] values) {
        int tableSize = values.length * 2;
        int maxTableSize = Math.min(values.length * 5 / 2, tableSize + 10);

        String[] bestTable = null;
        int bestCollisionRatio = 0;
        while (tableSize <= maxTableSize) {
            String[] table = new String[tableSize];
            int maxCollisionRatio = 0;
            for (String key : values) {
                int hashCode = key.hashCode();
                int collisionRatio = 0;
                while (true) {
                    int index = Integer.remainderUnsigned(hashCode++, table.length);
                    if (table[index] == null) {
                        table[index] = key;
                        break;
                    }
                    collisionRatio++;
                }
                maxCollisionRatio = Math.max(maxCollisionRatio, collisionRatio);
            }

            if (bestTable == null || bestCollisionRatio > maxCollisionRatio) {
                bestCollisionRatio = maxCollisionRatio;
                bestTable = table;
            }

            tableSize++;
        }

        return bestTable;
    }
}
