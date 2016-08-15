/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.runtime;

import org.teavm.interop.Structure;

public class RuntimeClass extends Structure {
    public static int INITIALIZED = 1;

    public static int BOOLEAN_CLASS = -1;
    public static int BYTE_CLASS = -2;
    public static int SHORT_CLASS = -3;
    public static int CHAR_CLASS = -4;
    public static int INT_CLASS = -5;
    public static int LONG_CLASS = -6;
    public static int FLOAT_CLASS = -7;
    public static int DOUBLE_CLASS = -8;

    public int size;
    public int flags;
    public int tag;
    public int canary;

    public static int computeCanary(int size, int tag) {
        return size ^ (tag << 8) ^ (tag >>> 24) ^ (0xAAAAAAAA);
    }

    public int computeCanary() {
        return computeCanary(size, tag);
    }

    public static native RuntimeClass getArrayClass();
}
