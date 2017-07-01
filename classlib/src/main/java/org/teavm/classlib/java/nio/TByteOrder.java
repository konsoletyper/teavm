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
package org.teavm.classlib.java.nio;

public final class TByteOrder {
    public static final TByteOrder BIG_ENDIAN = new TByteOrder("BIG_ENDIAN");
    public static final TByteOrder LITTLE_ENDIAN = new TByteOrder("LITTLE_ENDIAN");
    private String name;

    private TByteOrder(String name) {
        this.name = name;
    }

    public static TByteOrder nativeOrder() {
        return BIG_ENDIAN;
    }

    @Override
    public String toString() {
        return name;
    }
}
