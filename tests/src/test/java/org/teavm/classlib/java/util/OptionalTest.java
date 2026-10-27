/*
 *  Copyright 2026 Ethan Resnick.
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
package org.teavm.classlib.java.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/*
 * Optional.toString() Javadoc (Java SE 21): "The exact presentation format is unspecified and may vary
 * between implementations and versions." Implementation Requirements: "If a value is present the result
 * must include its string representation in the result. Empty and present Optionals must be unambiguously
 * differentiable." OptionalInt, OptionalLong and OptionalDouble say the same.
 *
 * So the exact "Optional[...]" / "Optional.empty" format checked below is OpenJDK's choice, not a spec
 * requirement; TeaVM matches it because it costs nothing (the strings are shorter than the old ones).
 */
@TeaVMTest
public class OptionalTest {
    @Test
    public void toStringIsTheOpenJdkOne() {
        assertEquals("Optional[42]", Optional.of(42).toString());
        assertEquals("Optional.empty", Optional.empty().toString());
        assertEquals("OptionalInt[5]", OptionalInt.of(5).toString());
        assertEquals("OptionalInt.empty", OptionalInt.empty().toString());
        assertEquals("OptionalLong[6]", OptionalLong.of(6).toString());
        assertEquals("OptionalLong.empty", OptionalLong.empty().toString());
        assertEquals("OptionalDouble[1.5]", OptionalDouble.of(1.5).toString());
        assertEquals("OptionalDouble.empty", OptionalDouble.empty().toString());
    }

    @Test
    public void toStringIncludesValueString() {
        assertEquals("Optional[]", Optional.of("").toString());
        assertEquals("Optional[a]b]", Optional.of("a]b").toString());
        assertEquals("Optional[Optional.empty]", Optional.of(Optional.empty()).toString());
        assertEquals("Optional[Optional[x]]", Optional.of(Optional.of("x")).toString());
        assertEquals("Optional[custom]", Optional.of(new Object() {
            @Override
            public String toString() {
                return "custom";
            }
        }).toString());
        assertEquals("Optional[null]", Optional.of(new Object() {
            @Override
            public String toString() {
                return null;
            }
        }).toString());

        int[] ints = { 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE };
        for (int v : ints) {
            assertEquals("OptionalInt[" + v + "]", OptionalInt.of(v).toString());
        }
        long[] longs = { 0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE };
        for (long v : longs) {
            assertEquals("OptionalLong[" + v + "]", OptionalLong.of(v).toString());
        }
        double[] doubles = { 0.0, -0.0, 1e300, Double.MIN_VALUE, Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY };
        for (double v : doubles) {
            assertEquals("OptionalDouble[" + v + "]", OptionalDouble.of(v).toString());
        }
        assertEquals("OptionalDouble[-0.0]", OptionalDouble.of(-0.0).toString());
        assertEquals("OptionalDouble[NaN]", OptionalDouble.of(Double.NaN).toString());
    }

    @Test
    public void emptyAndPresentAreDifferentiable() {
        // A present value whose string is exactly what the empty Optional prints must still differ
        assertNotEquals(Optional.empty().toString(), Optional.of("empty").toString());
        assertNotEquals(Optional.empty().toString(), Optional.of("Optional.empty").toString());
        assertNotEquals(Optional.empty().toString(), Optional.of("").toString());
        assertNotEquals(OptionalInt.empty().toString(), OptionalInt.of(0).toString());
        assertNotEquals(OptionalLong.empty().toString(), OptionalLong.of(0).toString());
        assertNotEquals(OptionalDouble.empty().toString(), OptionalDouble.of(0).toString());
        assertNotEquals(OptionalDouble.empty().toString(), OptionalDouble.of(Double.NaN).toString());
    }
}
