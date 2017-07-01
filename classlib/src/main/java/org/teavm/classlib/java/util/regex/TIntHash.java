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

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util.regex;

/**
 * Hashtable implementation for int values.
 */
class TIntHash {
        int[] table;

        int[] values;

        int mask;

        int size; // maximum shift

        public TIntHash(int size) {
            while (size >= mask) {
                mask = (mask << 1) | 1;
            }
            mask = (mask << 1) | 1;
            table = new int[mask + 1];
            values = new int[mask + 1];
            this.size = size;
        }

        public void put(int key, int value) {
            int i = 0;
            int hashCode = key & mask;

            while (true) {
                if (table[hashCode] == 0              // empty
                        || table[hashCode] == key) {  // rewrite
                    table[hashCode] = key;
                    values[hashCode] = value;
                    return;
                }
                i++;
                i &= mask;

                hashCode += i;
                hashCode &= mask;
            }
        }

        public int get(int key) {
            int hashCode = key & mask;
            int i = 0;
            int storedKey;

            while (true) {
                storedKey = table[hashCode];

                if (storedKey == 0) { // empty
                    return size;
                }

                if (storedKey == key) {
                    return values[hashCode];
                }

                i++;
                i &= mask;

                hashCode += i;
                hashCode &= mask;
            }
        }
    }