/*
 *  Copyright 2017 Alexey Andreev.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class LambdaTest {
    @Test
    public void lambdaWithMarkers() {
        Supplier<String> supplier = (Supplier<String> & A) () -> "OK";

        assertEquals("OK", supplier.get());
        assertTrue("Supplier is expected to implement A", supplier instanceof A);
    }

    @Test
    public void serializableLambda() {
        Supplier<String> supplier = (Supplier<String> & Serializable) () -> "OK";

        assertEquals("OK", supplier.get());
        assertTrue("Supplier is expected to implement Serializable", supplier instanceof Serializable);
    }

    @Test
    public void boxParameterToSupertype() {
        assertEquals(".*.*.*.*.*", acceptIntPredicate(this::oddPredicate));
    }

    @Test
    public void lambdaWorks() {
        Integer[] src = { 1, 2, 3 };
        Integer[] array = map(src, (Integer n) -> n * 2 + src[0]);
        assertArrayEquals(new Integer[] { 3, 5, 7 }, array);
    }

    @Test
    public void lambdaWorksWithLiteral() {
        B[] src = { new B(1), new B(2), new B(3) };
        B[] array = map(src, B::inc);
        assertEquals(3, array.length);
        assertEquals(2, array[0].value);
        assertEquals(3, array[1].value);
    }

    @Test
    public void lambdaWorksWithFieldLiteral() {
        B[] src = { new B(1), new B(2), new B(3) };
        int[] array = mapToInt(src, a -> a.value);
        assertArrayEquals(new int[] { 1, 2, 3 }, array);
    }

    @Test
    public void unusedLambda() {
        Consumer<String> foo = bar -> { };
    }

    @Test
    public void methodReferenceUnboxing() {
        var map = Map.of(1, 23.0, 2, 42.0);
        ToDoubleFunction<Integer> f = map::get;
        assertEquals(23.0, f.applyAsDouble(1), 0.01);
        assertNotEquals(24.0, f.applyAsDouble(1), 0.01);
        assertEquals(42.0, f.applyAsDouble(2), 0.01);

        var intMap = Map.of(1, 23, 2, 42);
        ToDoubleFunction<Integer> g = intMap::get;
        assertEquals(23.0, g.applyAsDouble(1), 0.01);
        assertNotEquals(24.0, g.applyAsDouble(1), 0.01);
        assertEquals(42.0, g.applyAsDouble(2), 0.01);
    }

    @Test
    public void methodReferenceBoxing() {
        java.util.function.Function<Integer, Object> f = this::intFunction;
        assertEquals(25, f.apply(23));
    }

    private int intFunction(int a) {
        return a + 2;
    }

    private String acceptIntPredicate(IntPredicate p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append(p.test(i) ? '*' : '.');
        }
        return sb.toString();
    }

    private boolean oddPredicate(Object o) {
        return o instanceof Integer && ((Integer) o) % 2 != 0;
    }

    interface A {
    }


    static <T> T[] map(T[] array, Function<T> f) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length);
        for (int i = 0; i < result.length; ++i) {
            result[i] = f.apply(array[i]);
        }
        return result;
    }

    static <T> int[] mapToInt(T[] array, IntFunction<T> f) {
        int[] result = new int[array.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = f.apply(array[i]);
        }
        return result;
    }

    interface Function<T> {
        T apply(T value);
    }

    interface IntFunction<T> {
        int apply(T value);
    }

    static class B {
        int value;

        B(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        B inc() {
            return new B(value + 1);
        }
    }
}
