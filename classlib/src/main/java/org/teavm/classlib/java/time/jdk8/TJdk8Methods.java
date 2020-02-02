/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time.jdk8;

public final class TJdk8Methods {

    private TJdk8Methods() {

    }

    public static <T> T requireNonNull(T value) {

        if (value == null) {
            throw new NullPointerException("Value must not be null");
        }
        return value;
    }

    public static <T> T requireNonNull(T value, String parameterName) {

        if (value == null) {
            throw new NullPointerException(parameterName + " must not be null");
        }
        return value;
    }

    public static boolean equals(Object a, Object b) {

        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }
        return a.equals(b);
    }

    public static int compareInts(int a, int b) {

        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    public static int compareLongs(long a, long b) {

        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        return 0;
    }

    public static int safeAdd(int a, int b) {

        int sum = a + b;
        // check for a change of sign in the result when the inputs have the same sign
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException("Addition overflows an int: " + a + " + " + b);
        }
        return sum;
    }

    public static long safeAdd(long a, long b) {

        long sum = a + b;
        // check for a change of sign in the result when the inputs have the same sign
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException("Addition overflows a long: " + a + " + " + b);
        }
        return sum;
    }

    public static int safeSubtract(int a, int b) {

        int result = a - b;
        // check for a change of sign in the result when the inputs have the different signs
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException("Subtraction overflows an int: " + a + " - " + b);
        }
        return result;
    }

    public static long safeSubtract(long a, long b) {

        long result = a - b;
        // check for a change of sign in the result when the inputs have the different signs
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException("Subtraction overflows a long: " + a + " - " + b);
        }
        return result;
    }

    public static int safeMultiply(int a, int b) {

        long total = (long) a * (long) b;
        if (total < Integer.MIN_VALUE || total > Integer.MAX_VALUE) {
            throw new ArithmeticException("Multiplication overflows an int: " + a + " * " + b);
        }
        return (int) total;
    }

    public static long safeMultiply(long a, int b) {

        switch (b) {
            case -1:
                if (a == Long.MIN_VALUE) {
                    throw new ArithmeticException("Multiplication overflows a long: " + a + " * " + b);
                }
                return -a;
            case 0:
                return 0L;
            case 1:
                return a;
        }
        long total = a * b;
        if (total / b != a) {
            throw new ArithmeticException("Multiplication overflows a long: " + a + " * " + b);
        }
        return total;
    }

    public static long safeMultiply(long a, long b) {

        if (b == 1) {
            return a;
        }
        if (a == 1) {
            return b;
        }
        if (a == 0 || b == 0) {
            return 0;
        }
        long total = a * b;
        if (total / b != a || (a == Long.MIN_VALUE && b == -1) || (b == Long.MIN_VALUE && a == -1)) {
            throw new ArithmeticException("Multiplication overflows a long: " + a + " * " + b);
        }
        return total;
    }

    public static int safeToInt(long value) {

        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new ArithmeticException("Calculation overflows an int: " + value);
        }
        return (int) value;
    }

    public static long floorDiv(long a, long b) {

        return (a >= 0 ? a / b : ((a + 1) / b) - 1);
    }

    public static long floorMod(long a, long b) {

        return ((a % b) + b) % b;
    }

    public static int floorMod(long a, int b) {

        return (int) (((a % b) + b) % b);
    }

    public static int floorDiv(int a, int b) {

        return (a >= 0 ? a / b : ((a + 1) / b) - 1);
    }

    public static int floorMod(int a, int b) {

        return ((a % b) + b) % b;
    }

}
