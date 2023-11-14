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
package org.teavm.classlib.java.lang;

public final class TStrictMath extends TObject {
    public static final double E = 2.71828182845904523536;
    public static final double PI = 3.14159265358979323846;
    public static final double TAU = 2 * PI;

    private TStrictMath() {
    }

    public static double sin(double a) {
        return TMath.sin(a);
    }

    public static double cos(double a) {
        return TMath.cos(a);
    }

    public static double tan(double a) {
        return TMath.tan(a);
    }

    public static double asin(double a) {
        return TMath.asin(a);
    }

    public static double acos(double a) {
        return TMath.acos(a);
    }

    public static double atan(double a) {
        return TMath.atan(a);
    }

    public static double toRadians(double angdeg) {
        return TMath.toRadians(angdeg);
    }

    public static double toDegrees(double angrad) {
        return TMath.toDegrees(angrad);
    }

    public static double exp(double a) {
        return TMath.exp(a);
    }

    public static double log(double a) {
        return TMath.log(a);
    }

    public static double log10(double a) {
        return TMath.log10(a);
    }

    public static double sqrt(double a) {
        return TMath.sqrt(a);
    }

    public static double cbrt(double a) {
        return TMath.cbrt(a);
    }

    public static double IEEEremainder(double f1, double f2) {
        return TMath.IEEEremainder(f1, f2);
    }

    public static double ceil(double a) {
        return TMath.ceil(a);
    }

    public static double floor(double a) {
        return TMath.floor(a);
    }

    public static double rint(double a) {
        return TMath.rint(a);
    }

    public static double atan2(double y, double x) {
        return TMath.atan2(y, x);
    }

    public static double pow(double a, double b) {
        return TMath.pow(a, b);
    }

    public static int round(float a) {
        return TMath.round(a);
    }

    public static long round(double a) {
        return TMath.round(a);
    }

    public static int floorDiv(int a, int b) {
        return TMath.floorDiv(a, b);
    }

    public static long floorDiv(long a, int b) {
        return TMath.floorDiv(a, b);
    }

    public static long floorDiv(long a, long b) {
        return TMath.floorDiv(a, b);
    }

    public static int floorMod(int a, int b) {
        return TMath.floorMod(a, b);
    }

    public static int floorMod(long a, int b) {
        return TMath.floorMod(a, b);
    }

    public static long floorMod(long a, long b) {
        return TMath.floorMod(a, b);
    }

    public static int addExact(int a, int b) {
        return TMath.addExact(a, b);
    }

    public static long addExact(long a, long b) {
        return TMath.addExact(a, b);
    }

    public static int subtractExact(int a, int b) {
        return TMath.subtractExact(a, b);
    }

    public static long subtractExact(long a, long b) {
        return TMath.subtractExact(a, b);
    }

    public static int multiplyExact(int a, int b) {
        return TMath.multiplyExact(a, b);
    }

    public static long multiplyExact(long a, int b) {
        return TMath.multiplyExact(a, b);
    }

    public static long multiplyExact(long a, long b) {
        return TMath.multiplyExact(a, b);
    }

    public static int divideExact(int a, int b) {
        return TMath.divideExact(a, b);
    }

    public static long divideExact(long a, long b) {
        return TMath.divideExact(a, b);
    }

    public static int incrementExact(int a) {
        return TMath.incrementExact(a);
    }

    public static long incrementExact(long a) {
        return TMath.incrementExact(a);
    }

    public static int decrementExact(int a) {
        return TMath.decrementExact(a);
    }

    public static long decrementExact(long a) {
        return TMath.decrementExact(a);
    }

    public static int negateExact(int a) {
        return TMath.negateExact(a);
    }

    public static long negateExact(long a) {
        return TMath.negateExact(a);
    }

    public static int toIntExact(long value) {
        return TMath.toIntExact(value);
    }

    public static double random() {
        return TMath.random();
    }

    public static int abs(int a) {
        return TMath.abs(a);
    }

    public static long abs(long a) {
        return TMath.abs(a);
    }

    public static float abs(float a) {
        return TMath.abs(a);
    }

    public static double abs(double a) {
        return TMath.abs(a);
    }

    public static int max(int a, int b) {
        return TMath.max(a, b);
    }

    public static long max(long a, long b) {
        return TMath.max(a, b);
    }

    public static float max(float a, float b) {
        return TMath.max(a, b);
    }

    public static double max(double a, double b) {
        return TMath.max(a, b);
    }

    public static int min(int a, int b) {
        return TMath.min(a, b);
    }

    public static long min(long a, long b) {
        return TMath.min(a, b);
    }

    public static float min(float a, float b) {
        return TMath.min(a, b);
    }

    public static double min(double a, double b) {
        return TMath.min(a, b);
    }

    public static double ulp(double d) {
        return TMath.ulp(d);
    }

    public static float ulp(float f) {
        return TMath.ulp(f);
    }

    public static double signum(double d) {
        return TMath.signum(d);
    }

    public static float signum(float f) {
        return TMath.signum(f);
    }

    public static double sinh(double x) {
        return TMath.sinh(x);
    }

    public static double cosh(double x) {
        return TMath.cosh(x);
    }

    public static double tanh(double x) {
        return TMath.tanh(x);
    }

    public static double hypot(double x, double y) {
        return TMath.hypot(x, y);
    }

    public static double expm1(double x) {
        return TMath.expm1(x);
    }

    public static double log1p(double x) {
        return TMath.log1p(x);
    }

    public static double copySign(double magnitude, double sign) {
        return TMath.copySign(magnitude, sign);
    }

    public static float copySign(float magnitude, float sign) {
        return TMath.copySign(magnitude, sign);
    }

    public static int getExponent(float f) {
        return TMath.getExponent(f);
    }

    public static int getExponent(double d) {
        return TMath.getExponent(d);
    }

    public static double nextAfter(double start, double direction) {
        return TMath.nextAfter(start, direction);
    }

    public static float nextAfter(float start, double direction) {
        return TMath.nextAfter(start, direction);
    }

    public static double nextUp(double d) {
        return TMath.nextUp(d);
    }

    public static float nextUp(float f) {
        return TMath.nextUp(f);
    }

    public static int clamp(long value, int min, int max) {
        return TMath.clamp(value, min, max);
    }

    public static long clamp(long value, long min, long max) {
        return TMath.clamp(value, min, max);
    }

    public static double clamp(double value, double min, double max) {
        return TMath.clamp(value, min, max);
    }

    public static float clamp(float value, float min, float max) {
        return TMath.clamp(value, min, max);
    }
}
