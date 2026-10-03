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
package org.teavm.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@TeaVMTest
public class JupiterParameterizedTest {
    private int beforeEachCount;

    static final List<String> WORDS = List.of("a", "bb", "ccc");

    static final Supplier<Stream<Arguments>> WORDS_WITH_LENGTH = () -> Stream.of(
            Arguments.of("a", 1),
            Arguments.of("bb", 2));

    @BeforeEach
    public void setUp() {
        beforeEachCount++;
    }

    @Test
    public void plainTestStillWorks() {
        assertEquals(1, beforeEachCount);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    public void intValues(int value) {
        assertEquals(1, beforeEachCount);
        assertTrue(value >= 1 && value <= 3);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    public void widening(long value) {
        assertTrue(value == 1L || value == 2L);
    }

    @ParameterizedTest
    @ValueSource(strings = { "RED", "GREEN" })
    public void stringToEnum(Color color) {
        assertNotNull(color);
        assertTrue(color != Color.BLUE);
    }

    @ParameterizedTest
    @ValueSource(chars = { 'a', 'b' })
    public void chars(char c) {
        assertTrue(c == 'a' || c == 'b');
    }

    @ParameterizedTest
    @ValueSource(booleans = { true })
    public void booleans(boolean b) {
        assertTrue(b);
    }

    @ParameterizedTest
    @ValueSource(classes = { String.class })
    public void classes(Class<?> cls) {
        assertEquals("java.lang.String", cls.getName());
    }

    @ParameterizedTest
    @ValueSource(ints = { 5 })
    public void boxing(Object value) {
        assertEquals(5, value);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 2, 3",
            "10, 20, 30",
            "-1, 0x10, 15"
    })
    public void csv(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            # comment
            apple,         5

            'lemon, lime', 11
            '',            0
            """)
    public void csvTextBlock(String fruit, int length) {
        assertEquals(length, fruit.length());
    }

    @ParameterizedTest
    @CsvSource(value = { "NIL; x", "; x" }, delimiter = ';', nullValues = "NIL")
    public void csvNulls(String value, String other) {
        assertNull(value);
        assertEquals("x", other);
    }

    @ParameterizedTest
    @CsvSource({ "1.5, 1.5" })
    public void csvStringFactory(BigDecimal value, double expected) {
        assertEquals(expected, value.doubleValue(), 0.0);
    }

    @ParameterizedTest
    @EnumSource
    public void allEnumConstants(Color color) {
        assertNotNull(color);
    }

    @ParameterizedTest
    @EnumSource(names = "BLUE", mode = EnumSource.Mode.EXCLUDE)
    public void enumExclude(Color color) {
        assertTrue(color != Color.BLUE);
    }

    @ParameterizedTest
    @EnumSource(names = "^G.*", mode = EnumSource.Mode.MATCH_ANY)
    public void enumMatch(Color color) {
        assertEquals(Color.GREEN, color);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "x")
    public void nullSource(String value) {
        assertTrue(value == null || value.equals("x"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void nullAndEmpty(String value) {
        assertTrue(value == null || value.isEmpty());
    }

    @ParameterizedTest
    @EmptySource
    public void emptyList(List<String> value) {
        assertTrue(value.isEmpty());
    }

    @ParameterizedTest
    @EmptySource
    public void emptyArray(int[] value) {
        assertEquals(0, value.length);
    }

    @ParameterizedTest
    @MethodSource
    public void factoryMethod(String word, int length) {
        assertEquals(1, beforeEachCount);
        assertEquals(length, word.length());
    }

    static Stream<Arguments> factoryMethod() {
        return Stream.of(
                Arguments.of("foo", 3),
                Arguments.of("hello", 5),
                Arguments.of(Named.of("empty", ""), 0));
    }

    @ParameterizedTest
    @MethodSource({ "ints", "org.teavm.junit.JupiterParameterizedTest$ExternalSource#values" })
    public void multipleFactories(int value) {
        assertTrue(value > 0);
    }

    static IntStream ints() {
        return IntStream.range(1, 4);
    }

    @ParameterizedTest
    @MethodSource("arrays")
    public void objectArrays(String a, String b) {
        assertEquals(a, b);
    }

    static Object[][] arrays() {
        return new Object[][] { { "x", "x" }, { "y", "y" } };
    }

    @ParameterizedTest
    @FieldSource("WORDS")
    public void fieldSource(String word) {
        assertTrue(word.chars().allMatch(c -> c == word.charAt(0)));
    }

    @ParameterizedTest
    @FieldSource("WORDS_WITH_LENGTH")
    public void fieldSupplier(String word, int length) {
        assertEquals(length, word.length());
    }

    @ParameterizedTest
    @ValueSource(ints = 1)
    @SkipJVM
    public void skipJvm(int value) {
        assertEquals(1, value);
    }

    public enum Color {
        RED,
        GREEN,
        BLUE
    }

    public static class ExternalSource {
        static int[] values() {
            return new int[] { 7, 8 };
        }
    }
}
