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
package org.teavm.platform.plugin;

import org.teavm.platform.metadata.Resource;

final class ResourceAccessor {
    private ResourceAccessor() {
    }

    public static native Object getProperty(Object obj, String propertyName);

    public static native Resource get(Object obj, String propertyName);

    public static native Object keys(Object obj);

    public static String[] keysToStrings(Object keys) {
        int sz = size(keys);
        String[] result = new String[sz];
        for (int i = 0; i < sz; ++i) {
            result[i] = castToString(get(keys, i));
        }
        return result;
    }

    public static native void put(Object obj, String propertyName, Object elem);

    public static native Resource get(Object obj, int index);

    public static native void add(Object obj, Resource elem);

    public static native boolean has(Object obj, String key);

    public static native int size(Object obj);

    public static native int castToInt(Object obj);

    public static native short castToShort(Object obj);

    public static native byte castToByte(Object obj);

    public static native boolean castToBoolean(Object obj);

    public static native float castToFloat(Object obj);

    public static native double castToDouble(Object obj);

    public static native String castToString(Object obj);

    public static native Object castFromInt(int value);

    public static native Object castFromShort(short value);

    public static native Object castFromByte(byte value);

    public static native Object castFromBoolean(boolean value);

    public static native Object castFromFloat(float value);

    public static native Object castFromDouble(double value);

    public static native Object castFromString(String value);
}
