/*
 *  Copyright 2020 Alexey Andreev.
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
package org.threeten.bp.jdk8;

/**
 * A set of utility methods that provide additional functionality for working
 * with dates and times.
 * <p>
 * The contents of this class replace functionality available in JDK 8.
 *
 * <h3>Specification for implementors</h3>
 * This is a thread-safe utility class.
 * All returned classes are immutable and thread-safe.
 */
public final class Jdk8Methods {

    /**
     * Private constructor since this is a utility class.
     */
    private Jdk8Methods() {
    }

    //-----------------------------------------------------------------------
    /**
     * Safely adds two int values.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int safeAdd(int a, int b) {
        int sum = a + b;
        // check for a change of sign in the result when the inputs have the same sign
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException("Addition overflows an int: " + a + " + " + b);
        }
        return sum;
    }

    /**
     * Safely adds two long values.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long safeAdd(long a, long b) {
        long sum = a + b;
        // check for a change of sign in the result when the inputs have the same sign
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException("Addition overflows a long: " + a + " + " + b);
        }
        return sum;
    }

    //-----------------------------------------------------------------------
    /**
     * Safely subtracts one int from another.
     *
     * @param a  the first value
     * @param b  the second value to subtract from the first
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int safeSubtract(int a, int b) {
        int result = a - b;
        // check for a change of sign in the result when the inputs have the different signs
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException("Subtraction overflows an int: " + a + " - " + b);
        }
        return result;
    }

    /**
     * Safely subtracts one long from another.
     *
     * @param a  the first value
     * @param b  the second value to subtract from the first
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long safeSubtract(long a, long b) {
        long result = a - b;
        // check for a change of sign in the result when the inputs have the different signs
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException("Subtraction overflows a long: " + a + " - " + b);
        }
        return result;
    }

    //-----------------------------------------------------------------------
    /**
     * Safely multiply one int by another.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int safeMultiply(int a, int b) {
        long total = (long) a * (long) b;
        if (total < Integer.MIN_VALUE || total > Integer.MAX_VALUE) {
            throw new ArithmeticException("Multiplication overflows an int: " + a + " * " + b);
        }
        return (int) total;
    }

    /**
     * Safely multiply a long by an int.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the new total
     * @throws ArithmeticException if the result overflows a long
     */
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

    /**
     * Multiply two values throwing an exception if overflow occurs.
     *
     * @param a  the first value
     * @param b  the second value
     * @return the new total
     * @throws ArithmeticException if the result overflows a long
     */
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

    //-----------------------------------------------------------------------
    /**
     * Safely convert a long to an int.
     *
     * @param value  the value to convert
     * @return the int value
     * @throws ArithmeticException if the result overflows an int
     */
    public static int safeToInt(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new ArithmeticException("Calculation overflows an int: " + value);
        }
        return (int) value;
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the floor division.
     * <p>
     * This returns {@code 0} for {@code floorDiv(0, 4)}.<br />
     * This returns {@code -1} for {@code floorDiv(-1, 4)}.<br />
     * This returns {@code -1} for {@code floorDiv(-2, 4)}.<br />
     * This returns {@code -1} for {@code floorDiv(-3, 4)}.<br />
     * This returns {@code -1} for {@code floorDiv(-4, 4)}.<br />
     * This returns {@code -2} for {@code floorDiv(-5, 4)}.<br />
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor division
     */
    public static long floorDiv(long a, long b) {
        return a >= 0 ? a / b : ((a + 1) / b) - 1;
    }

    /**
     * Returns the floor modulus.
     * <p>
     * This returns {@code 0} for {@code floorMod(0, 4)}.<br />
     * This returns {@code 1} for {@code floorMod(-1, 4)}.<br />
     * This returns {@code 2} for {@code floorMod(-2, 4)}.<br />
     * This returns {@code 3} for {@code floorMod(-3, 4)}.<br />
     * This returns {@code 0} for {@code floorMod(-4, 4)}.<br />
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor modulus (positive)
     */
    public static long floorMod(long a, long b) {
        return ((a % b) + b) % b;
    }

    /**
     * Returns the floor modulus.
     * <p>
     * This returns {@code 0} for {@code floorMod(0, 4)}.<br />
     * This returns {@code 3} for {@code floorMod(-1, 4)}.<br />
     * This returns {@code 2} for {@code floorMod(-2, 4)}.<br />
     * This returns {@code 1} for {@code floorMod(-3, 4)}.<br />
     * This returns {@code 0} for {@code floorMod(-4, 4)}.<br />
     * This returns {@code 3} for {@code floorMod(-5, 4)}.<br />
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor modulus (positive)
     */
    public static int floorMod(long a, int b) {
        return (int) (((a % b) + b) % b);
    }

    /**
     * Returns the floor division.
     * <p>
     * This returns {@code 1} for {@code floorDiv(3, 3)}.<br />
     * This returns {@code 0} for {@code floorDiv(2, 3)}.<br />
     * This returns {@code 0} for {@code floorDiv(1, 3)}.<br />
     * This returns {@code 0} for {@code floorDiv(0, 3)}.<br />
     * This returns {@code -1} for {@code floorDiv(-1, 3)}.<br />
     * This returns {@code -1} for {@code floorDiv(-2, 3)}.<br />
     * This returns {@code -1} for {@code floorDiv(-3, 3)}.<br />
     * This returns {@code -2} for {@code floorDiv(-4, 3)}.<br />
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor division
     */
    public static int floorDiv(int a, int b) {
        return a >= 0 ? a / b : ((a + 1) / b) - 1;
    }

    /**
     * Returns the floor modulus.
     * <p>
     * This returns {@code 0} for {@code floorMod(3, 3)}.<br />
     * This returns {@code 2} for {@code floorMod(2, 3)}.<br />
     * This returns {@code 1} for {@code floorMod(1, 3)}.<br />
     * This returns {@code 0} for {@code floorMod(0, 3)}.<br />
     * This returns {@code 2} for {@code floorMod(-1, 3)}.<br />
     * This returns {@code 1} for {@code floorMod(-2, 3)}.<br />
     * This returns {@code 0} for {@code floorMod(-3, 3)}.<br />
     * This returns {@code 2} for {@code floorMod(-4, 3)}.<br />
     *
     * @param a  the dividend
     * @param b  the divisor
     * @return the floor modulus (positive)
     */
    public static int floorMod(int a, int b) {
        return ((a % b) + b) % b;
    }

}
