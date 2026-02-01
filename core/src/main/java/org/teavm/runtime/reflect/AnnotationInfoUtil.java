/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.runtime.reflect;

import java.lang.annotation.Annotation;

public final class AnnotationInfoUtil {
    private AnnotationInfoUtil() {
    }

    public static Annotation createAnnotation(AnnotationInfo info) {
        return (Annotation) info.constructor().createAnnotation(info.data());
    }

    public static boolean[] asBooleanArray(AnnotationValueArray array) {
        var result = new boolean[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getBoolean(i);
        }
        return result;
    }

    public static byte[] asByteArray(AnnotationValueArray array) {
        var result = new byte[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getByte(i);
        }
        return result;
    }

    public static short[] asShortArray(AnnotationValueArray array) {
        var result = new short[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getShort(i);
        }
        return result;
    }

    public static char[] asCharArray(AnnotationValueArray array) {
        var result = new char[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getChar(i);
        }
        return result;
    }

    public static int[] asIntArray(AnnotationValueArray array) {
        var result = new int[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getInt(i);
        }
        return result;
    }

    public static long[] asLongArray(AnnotationValueArray array) {
        var result = new long[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getLong(i);
        }
        return result;
    }

    public static float[] asFloatArray(AnnotationValueArray array) {
        var result = new float[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getFloat(i);
        }
        return result;
    }

    public static double[] asDoubleArray(AnnotationValueArray array) {
        var result = new double[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getDouble(i);
        }
        return result;
    }

    public static Class<?>[] asClassArray(AnnotationValueArray array) {
        var result = new Class<?>[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = ClassInfoUtil.resolve(array.getClass(i)).classObject();
        }
        return result;
    }

    public static String[] asStringArray(AnnotationValueArray array) {
        var result = new String[array.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = array.getString(i).getStringObject();
        }
        return result;
    }

    public static void fillAnnotationArray(AnnotationValueArray source, AnnotationConstructor constructor,
            Annotation[] target) {
        for (var i = 0; i < target.length; ++i) {
            target[i] = (Annotation) constructor.createAnnotation(source.getAnnotation(i));
        }
    }

    public static void fillEnumArray(AnnotationValueArray source, Enum<?>[] values, Enum<?>[] target) {
        for (var i = 0; i < target.length; ++i) {
            target[i] = values[source.getEnum(i)];
        }
    }
}
