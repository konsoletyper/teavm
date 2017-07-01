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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.lang.reflect.Array;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class LambdaTest {
    @Test
    public void lambdaWorks() {
        Integer[] src = { 1, 2, 3 };
        Integer[] array = map(src, (Integer n) -> n * 2 + src[0]);
        assertArrayEquals(new Integer[] { 3, 5, 7 }, array);
    }

    @Test
    public void lambdaWorksWithLiteral() {
        A[] src = { new A(1), new A(2), new A(3) };
        A[] array = map(src, A::inc);
        assertEquals(3, array.length);
        assertEquals(2, array[0].value);
        assertEquals(3, array[1].value);
    }

    @Test
    public void lambdaWorksWithFieldLiteral() {
        A[] src = { new A(1), new A(2), new A(3) };
        int[] array = mapToInt(src, a -> a.value);
        assertArrayEquals(new int[] { 1, 2, 3 }, array);
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

    static class A {
        int value;

        A(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        A inc() {
            return new A(value + 1);
        }
    }
}
