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
package org.teavm.jso.export;

import java.util.Arrays;
import org.teavm.jso.JSExport;

public final class ModuleWithPrimitiveTypes {
    private ModuleWithPrimitiveTypes() {
    }

    @JSExport
    public static boolean boolResult() {
        return true;
    }

    @JSExport
    public static byte byteResult() {
        return 1;
    }

    @JSExport
    public static short shortResult() {
        return 2;
    }

    @JSExport
    public static int intResult() {
        return 3;
    }

    @JSExport
    public static float floatResult() {
        return 4.1f;
    }

    @JSExport
    public static double doubleResult() {
        return 5.2f;
    }

    @JSExport
    public static String stringResult() {
        return "q";
    }

    @JSExport
    public static boolean[] boolArrayResult() {
        return new boolean[] { true, false };
    }

    @JSExport
    public static byte[] byteArrayResult() {
        return new byte[] { 1, 2 };
    }

    @JSExport
    public static short[] shortArrayResult() {
        return new short[] { 2, 3 };
    }

    @JSExport
    public static int[] intArrayResult() {
        return new int[] { 3, 4 };
    }

    @JSExport
    public static float[] floatArrayResult() {
        return new float[] { 4f, 5f };
    }

    @JSExport
    public static double[] doubleArrayResult() {
        return new double[] { 5, 6 };
    }

    @JSExport
    public static String[] stringArrayResult() {
        return new String[] { "q", "w" };
    }

    @JSExport
    public static String boolParam(boolean param) {
        return "bool:" + param;
    }

    @JSExport
    public static String byteParam(byte param) {
        return "byte:" + param;
    }

    @JSExport
    public static String shortParam(short param) {
        return "short:" + param;
    }

    @JSExport
    public static String intParam(int param) {
        return "int:" + param;
    }

    @JSExport
    public static String floatParam(float param) {
        return "float:" + param;
    }

    @JSExport
    public static String doubleParam(double param) {
        return "double:" + param;
    }

    @JSExport
    public static String stringParam(String param) {
        return "string:" + param;
    }

    @JSExport
    public static String intArrayParam(int[] param) {
        return "intArray:" + Arrays.toString(param);
    }
}
