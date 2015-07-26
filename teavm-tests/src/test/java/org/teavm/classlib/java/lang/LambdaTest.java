package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertArrayEquals;
import java.lang.reflect.Array;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class LambdaTest {
    @Test
    public void lambdaWorks() {
        Integer[] src = { 1, 2, 3 };
        Integer[] array = map(src, (Integer n) -> n * 2 + src[0]);
        assertArrayEquals(new Integer[] { 3, 5, 7 }, array);
    }

    static <T> T[] map(T[] array, Function<T> f) {
        @SuppressWarnings("unchecked")
        T[] result = (T[])Array.newInstance(array.getClass().getComponentType(), array.length);
        for (int i = 0; i < result.length; ++i) {
            result[i] = f.apply(array[i]);
        }
        return result;
    }

    interface Function<T> {
        T apply(T value);
    }
}
