/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.runtime.WasmSupport;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;

@NoSideEffects
public final class TMath extends TObject {
    public static final double E = 2.71828182845904523536;
    public static final double PI = 3.14159265358979323846;
    public static final double TAU = 2 * PI;

    private TMath() {
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "sin")
    @Unmanaged
    public static native double sin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "cos")
    @Unmanaged
    public static native double cos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "tan")
    @Unmanaged
    public static native double tan(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "asin")
    @Unmanaged
    public static native double asin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "acos")
    @Unmanaged
    public static native double acos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "atan")
    @Unmanaged
    public static native double atan(double a);

    public static double toRadians(double angdeg) {
        return angdeg * PI / 180;
    }

    public static double toDegrees(double angrad) {
        return angrad * 180 / PI;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "exp")
    @Unmanaged
    public static native double exp(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "log")
    @Unmanaged
    public static native double log(double a);

    public static double log10(double a) {
        return log(a) / 2.302585092994046 /* log_e 10 */;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "sqrt")
    @Unmanaged
    public static native double sqrt(double a);

    public static double cbrt(double a) {
        return a > 0 ? pow(a, 1.0 / 3) : -pow(-a, 1.0 / 3);
    }

    public static double IEEEremainder(double f1, double f2) {
        int n = (int) (f1 / f2);
        return f1 - n * f2;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "ceil")
    @Unmanaged
    public static native double ceil(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "floor")
    @Unmanaged
    public static native double floor(double a);

    public static double pow(double x, double y) {
        if (PlatformDetector.isWebAssembly()) {
            return WasmSupport.pow(x, y);
        } else {
            return powImpl(x, y);
        }
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "pow")
    @Unmanaged
    private static native double powImpl(double x, double y);

    public static double rint(double a) {
        return round(a);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "atan2")
    @Unmanaged
    public static native double atan2(double y, double x);

    public static int round(float a) {
        return (int) (a + signum(a) * 0.5f);
    }

    public static long round(double a) {
        return (long) (a + signum(a) * 0.5);
    }

    public static int floorDiv(int a, int b) {
        int div = a / b;
        return (a ^ b) < 0 && div * b != a ? div - 1 : div;
    }

    public static long floorDiv(long a, int b) {
        return floorDiv(a, (long) b);
    }

    public static long floorDiv(long a, long b) {
        long div = a / b;
        return (a ^ b) < 0 && div * b != a ? div - 1 : div;
    }

    public static int floorMod(int a, int b) {
        return a - floorDiv(a, b) * b;
    }

    public static int floorMod(long a, int b) {
        return (int) (a - floorDiv(a, b) * b);
    }

    public static long floorMod(long a, long b) {
        return a - floorDiv(a, b) * b;
    }

    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException();
        }
        return a + 1;
    }

    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException();
        }
        return a + 1L;
    }

    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return a - 1;
    }

    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return a - 1L;
    }

    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return -a;
    }

    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return -a;
    }

    public static int toIntExact(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return (int) value;
    }

    public static int addExact(int a, int b) {
        int sum = a + b;
        if ((a ^ sum) < 0 && (a ^ b) >= 0) { // a and b samesigned, but sum is not
            throw new ArithmeticException();
        }
        return sum;
    }

    public static long addExact(long a, long b) {
        long sum = a + b;
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException();
        }
        return sum;
    }

    public static int subtractExact(int a, int b) {
        int result = a - b;
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException();
        }
        return result;
    }

    public static long subtractExact(long a, long b) {
        long result = a - b;
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException();
        }
        return result;
    }

    public static int multiplyExact(int a, int b) {
        if (b == 1) {
            return a;
        } else if (a == 1) {
            return b;
        } else if (a == 0 || b == 0) {
            return 0;
        }
        int total = a * b;
        if ((a == Integer.MIN_VALUE && b == -1) || (b == Integer.MIN_VALUE && a == -1) || total / b != a) {
            throw new ArithmeticException();
        }
        return total;
    }

    public static long multiplyExact(long a, int b) {
        return multiplyExact(a, (long) b);
    }

    public static long multiplyExact(long a, long b) {
        if (b == 1) {
            return a;
        } else if (a == 1) {
            return b;
        } else if (a == 0 || b == 0) {
            return 0;
        }
        long total = a * b;
        if ((a == Long.MIN_VALUE && b == -1) || (b == Long.MIN_VALUE && a == -1) || total / b != a) {
            throw new ArithmeticException();
        }
        return total;
    }

    public static int divideExact(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return a / b;
    }

    public static long divideExact(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return a / b;
    }

    @Unmanaged
    public static double random() {
        if (PlatformDetector.isC()) {
            return randomC();
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmSupport.random();
        } else {
            return randomImpl();
        }
    }

    @Import(name = "teavm_rand")
    private static native double randomC();

    @GeneratedBy(MathNativeGenerator.class)
    private static native double randomImpl();

    public static int min(int a, int b) {
        return a < b ? a : b;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static long min(long a, long b) {
        return a < b ? a : b;
    }

    public static long max(long a, long b) {
        return a > b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float minImpl(double a, double b);

    @Unmanaged
    public static double min(double a, double b) {
        if (PlatformDetector.isJavaScript()) {
            return minImpl(a, b);
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmRuntime.min(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0.0 && b == 0.0 && 1 / b == Double.NEGATIVE_INFINITY) {
            return b;
        }
        return a <= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float maxImpl(double a, double b);

    @Unmanaged
    public static double max(double a, double b) {
        if (PlatformDetector.isJavaScript()) {
            return maxImpl(a, b);
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmRuntime.max(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0.0 && b == 0.0 && 1 / a == Double.NEGATIVE_INFINITY) {
            return b;
        }
        return a >= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float minImpl(float a, float b);

    @Unmanaged
    public static float min(float a, float b) {
        if (PlatformDetector.isJavaScript()) {
            return minImpl(a, b);
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmRuntime.min(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0 && b == 0 && 1 / b == Float.NEGATIVE_INFINITY) {
            return b;
        }
        return a <= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float maxImpl(float a, float b);

    @Unmanaged
    public static float max(float a, float b) {
        if (PlatformDetector.isJavaScript()) {
            return maxImpl(a, b);
        } else if (PlatformDetector.isWebAssembly()) {
            return WasmRuntime.max(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0 && b == 0 && 1 / a == Float.NEGATIVE_INFINITY) {
            return b;
        }
        return a >= b ? a : b;
    }

    public static int abs(int n) {
        return n >= 0 ? n : -n;
    }

    public static long abs(long n) {
        return n >= 0 ? n : -n;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native float absImpl(float d);

    @Import(name = "fabs")
    private static native float absC(float d);

    public static float abs(float n) {
        if (PlatformDetector.isJavaScript()) {
            return absImpl(n);
        } else if (PlatformDetector.isC()) {
            return absC(n);
        }
        return n <= 0f ? 0f - n : n;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native double absImpl(double d);

    @Import(name = "fabs")
    private static native double absC(double d);

    public static double abs(double n) {
        if (PlatformDetector.isJavaScript()) {
            return absImpl(n);
        } else if (PlatformDetector.isC()) {
            return absC(n);
        }
        return n <= 0.0 ? 0.0 - n : n;
    }

    public static double ulp(double d) {
        if (TDouble.isNaN(d)) {
            return d;
        } else if (TDouble.isInfinite(d)) {
            return TDouble.POSITIVE_INFINITY;
        }

        if (TDouble.isNaN(d)) {
            return d;
        } else if (TDouble.isInfinite(d)) {
            return TDouble.POSITIVE_INFINITY;
        }

        long bits = TDouble.doubleToLongBits(d);
        bits &= 0xEFF0000000000000L;
        if (bits >= 53L << 52L) {
            bits -= 52L << 52L;
        } else {
            int exponent = (int) (bits >> 52);
            bits = 1L << Math.max(0, exponent - 1);
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float ulp(float d) {
        if (TFloat.isNaN(d)) {
            return d;
        } else if (TFloat.isInfinite(d)) {
            return TFloat.POSITIVE_INFINITY;
        }

        int bits = TFloat.floatToIntBits(d);
        bits &= 0x7F800000;
        if (bits >= 24 << 23) {
            bits -= 23 << 23;
        } else {
            int exponent = bits >> 23;
            bits = 1 << Math.max(0, exponent - 1);
        }
        return TFloat.intBitsToFloat(bits);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native double sign(double d);

    public static double signum(double d) {
        if (PlatformDetector.isJavaScript()) {
            return sign(d);
        }
        if (Double.isNaN(d)) {
            return d;
        }
        return d < 0 ? -1 : d > 0 ? 1 : d;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native float sign(float d);

    public static float signum(float d) {
        if (PlatformDetector.isJavaScript()) {
            return sign(d);
        }
        if (Double.isNaN(d)) {
            return d;
        }
        return d < 0 ? -1 : d > 0 ? 1 : d;
    }

    public static double sinh(double x) {
        double e = exp(x);
        return (e - 1 / e) / 2;
    }

    public static double cosh(double x) {
        double e = exp(x);
        return (e + 1 / e) / 2;
    }

    public static double tanh(double x) {
        double e = exp(x);
        return (e - 1 / e) / (e + 1 / e);
    }

    public static double hypot(double x, double y) {
        return sqrt(x * x + y * y);
    }

    public static double expm1(double x) {
        return exp(x) - 1;
    }

    public static double log1p(double x) {
        return log(x + 1);
    }

    public static float copySign(float magnitude, float sign) {
        return Float.intBitsToFloat((Float.floatToRawIntBits(sign) & Integer.MIN_VALUE)
                | (Float.floatToRawIntBits(magnitude) & Integer.MAX_VALUE));
    }

    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) & Long.MIN_VALUE)
                | (Double.doubleToRawLongBits(magnitude) & Long.MAX_VALUE));
    }

    public static int getExponent(double d) {
        long bits = TDouble.doubleToRawLongBits(d);
        int exponent = (int) ((bits >> 52) & 0x7FF);
        return exponent - 1023;
    }

    public static int getExponent(float f) {
        int bits = TFloat.floatToRawIntBits(f);
        int exponent = (bits >> 23) & 0xFF;
        return exponent - 127;
    }

    public static double nextAfter(double start, double direction) {
        if (start == direction) {
            return direction;
        }
        return direction > start ? nextUp(start) : nextDown(start);
    }

    public static float nextAfter(float start, double direction) {
        if (start == direction) {
            return start;
        }
        return direction > start ? nextUp(start) : nextDown(start);
    }

    public static double nextUp(double d) {
        if (TDouble.isNaN(d) || d == TDouble.POSITIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return Double.MIN_VALUE;
        }
        long bits = TDouble.doubleToLongBits(d);
        if (d < 0) {
            bits--;
        } else {
            bits++;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextUp(float d) {
        if (TFloat.isNaN(d) || d == TFloat.POSITIVE_INFINITY) {
            return d;
        }
        if (d == 0) {
            return Float.MIN_VALUE;
        }
        int bits = TFloat.floatToIntBits(d);
        if (d < 0) {
            bits--;
        } else {
            bits++;
        }
        return TFloat.intBitsToFloat(bits);
    }

    public static double nextDown(double d) {
        if (TDouble.isNaN(d) || d == TDouble.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return -Double.MIN_VALUE;
        }
        long bits = TDouble.doubleToLongBits(d);
        if (d < 0) {
            bits++;
        } else {
            bits--;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextDown(float d) {
        if (TFloat.isNaN(d) || d == TFloat.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0) {
            return -Float.MIN_VALUE;
        }
        int bits = TFloat.floatToIntBits(d);
        if (d < 0) {
            bits++;
        } else {
            bits--;
        }
        return TFloat.intBitsToFloat(bits);
    }

    public static int clamp(long value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return (int) Math.min(max, Math.max(value, min));
    }

    public static long clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }

    public static double clamp(double value, double min, double max) {
        if (!(min < max) && (Double.isNaN(min) || Double.isNaN(max) || Double.compare(min, max) > 0)) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }

    public static float clamp(float value, float min, float max) {
        if (!(min < max) && (Float.isNaN(min) || Float.isNaN(max) || Float.compare(min, max) > 0)) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }
}
