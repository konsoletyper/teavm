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

    public static int SIZE_OFFSET = 0;
    public static int FLAGS_OFFSET = 4;
    public static int LOWER_TAG_OFFSET = 8;
    public static int UPPER_TAG_OFFSET = 12;
    public static int EXCLUDED_RANGE_COUNT_OFFSET = 16;
    public static int EXCLUDED_RANGE_ADDRESS_OFFSET = 20;
    public static int CANARY_OFFSET = 24;
    public static int VIRTUAL_TABLE_OFFSET = 28;

    public static int ARRAY_CLASS = -1;
    public static int BOOLEAN_CLASS = -2;
    public static int BYTE_CLASS = -3;
    public static int SHORT_CLASS = -4;
    public static int CHAR_CLASS = -5;
    public static int INT_CLASS = -6;
    public static int LONG_CLASS = -7;
    public static int FLOAT_CLASS = -8;
    public static int DOUBLE_CLASS = -9;

    public int size;
    public int flags;
    public int lowerTag;
    public int upperTag;
    public int excludedRangeCount;
    public int excludedRangesAddress;

    public static int computeCanary(int size, int lowerTag, int upperTag) {
        return size ^ (lowerTag << 8) ^ (lowerTag >>> 24) ^ (upperTag << 24) ^ (lowerTag >>> 8);
    }

    public int computeCanary() {
        return computeCanary(size, lowerTag, upperTag);
    }
}
