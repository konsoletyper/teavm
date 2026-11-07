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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class IntegerDivisionTest {
    private static long sink;

    // Values come from polymorphic suppliers, so that compiler can't infer them
    private static IntSupplier intOf(int value) {
        return value % 2 == 0 ? () -> value : new IntSupplier() {
            @Override
            public int getAsInt() {
                return value;
            }
        };
    }

    private static LongSupplier longOf(long value) {
        return value % 2 == 0 ? () -> value : new LongSupplier() {
            @Override
            public long getAsLong() {
                return value;
            }
        };
    }

    @Test
    public void intDivisionByZeroThrows() {
        var a = intOf(23);
        var b = intOf(0);
        assertThrows(ArithmeticException.class, () -> consume(a.getAsInt() / b.getAsInt()));
        assertThrows(ArithmeticException.class, () -> consume(a.getAsInt() % b.getAsInt()));
    }

    @Test
    public void longDivisionByZeroThrows() {
        var a = longOf(23);
        var b = longOf(0);
        assertThrows(ArithmeticException.class, () -> consume(a.getAsLong() / b.getAsLong()));
        assertThrows(ArithmeticException.class, () -> consume(a.getAsLong() % b.getAsLong()));
    }

    @Test
    public void unusedDivisionByZeroThrows() {
        var a = intOf(23);
        var b = intOf(0);
        assertThrows(ArithmeticException.class, () -> {
            @SuppressWarnings("unused")
            var unused = a.getAsInt() / b.getAsInt();
        });
        var c = longOf(23);
        var d = longOf(0);
        assertThrows(ArithmeticException.class, () -> {
            @SuppressWarnings("unused")
            var unused = c.getAsLong() % d.getAsLong();
        });
        // Result is discarded after inlining
        assertThrows(ArithmeticException.class, () -> discard(a.getAsInt() / b.getAsInt()));
        assertThrows(ArithmeticException.class, () -> divideTwice(23, 0));
    }

    private static void discard(int value) {
        // do nothing
    }

    @Test
    public void constantZeroDivisorThrows() {
        var a = intOf(23).getAsInt();
        assertThrows(ArithmeticException.class, () -> consume(a / 0));
        var b = longOf(23).getAsLong();
        assertThrows(ArithmeticException.class, () -> consume(b % 0L));
    }

    @Test
    public void minValueDividedByMinusOne() {
        var a = intOf(Integer.MIN_VALUE).getAsInt();
        var b = intOf(-1).getAsInt();
        assertEquals(Integer.MIN_VALUE, a / b);
        assertEquals(0, a % b);

        var c = longOf(Long.MIN_VALUE).getAsLong();
        var d = longOf(-1).getAsLong();
        assertEquals(Long.MIN_VALUE, c / d);
        assertEquals(0L, c % d);
    }

    @Test
    public void regularDivision() {
        var a = intOf(-23).getAsInt();
        var b = intOf(5).getAsInt();
        assertEquals(-4, a / b);
        assertEquals(-3, a % b);
        assertEquals(4, a / -b);
        assertEquals(-3, a % -b);

        var c = longOf(-23_000_000_000L).getAsLong();
        var d = longOf(5_000_000_000L).getAsLong();
        assertEquals(-4L, c / d);
        assertEquals(-3_000_000_000L, c % d);
    }

    @Test
    public void exceptionCaughtInSameMethod() {
        assertEquals(-1, divideOrDefault(23, 0));
        assertEquals(4, divideOrDefault(23, 5));
        assertEquals(-1, remainderOrDefault(23, 0));
        assertEquals(3, remainderOrDefault(23, 5));
    }

    private static int divideOrDefault(int a, int b) {
        var x = intOf(a).getAsInt();
        var y = intOf(b).getAsInt();
        try {
            return x / y;
        } catch (ArithmeticException e) {
            return -1;
        }
    }

    private static long remainderOrDefault(long a, long b) {
        var x = longOf(a).getAsLong();
        var y = longOf(b).getAsLong();
        try {
            return x % y;
        } catch (ArithmeticException e) {
            return -1;
        }
    }

    @Test
    public void divisorUsedInHandler() {
        // The handler must not rely on 'divisor != 0', which only holds after division succeeds
        assertThrows(ArithmeticException.class, () -> consume(divideTwice(23, 0)));
        assertEquals(4 + 3, divideTwice(23, 5));
    }

    private static int divideTwice(int a, int b) {
        var x = intOf(a).getAsInt();
        var y = intOf(b).getAsInt();
        try {
            return x / y + x % y;
        } catch (ArithmeticException e) {
            return x % y;
        }
    }

    @Test
    public void divisionGuardedByCondition() {
        assertEquals(0, safeDivide(23, 0));
        assertEquals(4, safeDivide(23, 5));
        assertEquals(Integer.MIN_VALUE, safeDivide(Integer.MIN_VALUE, -1));
    }

    private static int safeDivide(int a, int b) {
        var x = intOf(a).getAsInt();
        var y = intOf(b).getAsInt();
        return y != 0 ? x / y : 0;
    }

    @Test
    public void divisionByLoopCounter() {
        var n = intOf(10).getAsInt();
        var a = intOf(2520).getAsInt();
        var sum = 0;
        for (var i = 1; i <= n; ++i) {
            sum += a / i + a % i;
        }
        assertEquals(7381, sum);

        var count = 0;
        for (var i = n; i >= 0; --i) {
            try {
                count += a / i;
            } catch (ArithmeticException e) {
                count += 1000000;
            }
        }
        assertEquals(1000000 + 7381, count);
    }

    @Test
    public void divisionByMaskedValue() {
        var a = intOf(100).getAsInt();
        var x = intOf(0x1234).getAsInt();
        assertEquals(100 % ((x & 7) + 1), a % ((x & 7) + 1));
        var zero = intOf(0x1230).getAsInt();
        assertThrows(ArithmeticException.class, () -> consume(a % (zero & 7)));
    }

    // Store result, so that the division is not eliminated as unused.
    private static void consume(long value) {
        sink = value;
    }
}
