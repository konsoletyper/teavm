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
package org.teavm.classlib.java.util.stream;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class CollectorsTest {
    @Test
    public void joining() {
        assertEquals("123", Stream.of(1, 2, 3).map(Object::toString).collect(Collectors.joining()));
        assertEquals("1,2,3", Stream.of(1, 2, 3).map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("[1,2,3]", Stream.of(1, 2, 3).map(Object::toString).collect(Collectors.joining(",", "[", "]")));
        assertEquals("", Stream.empty().map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("[]", Stream.empty().map(Object::toString).collect(Collectors.joining(",", "[", "]")));
        assertEquals("1", Stream.of(1).map(Object::toString).collect(Collectors.joining(",")));
        assertEquals("[1]", Stream.of(1).map(Object::toString).collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void toList() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.toList()));
    }

    @Test
    public void toSet() {
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), Stream.of(1, 2, 3).collect(Collectors.toSet()));
    }

    @Test
    public void toCollection() {
        List<Integer> c = new ArrayList<>();
        Stream.of(1, 2, 3).collect(Collectors.toCollection(() -> c));
        assertEquals(Arrays.asList(1, 2, 3), c);
    }

    @Test(expected = NullPointerException.class)
    public void noNullsInToMap() {
        Stream.of(1, 2, null).collect(Collectors.toMap(Function.identity(), Function.identity()));
    }

    @Test(expected = IllegalStateException.class)
    public void noDuplicatesInToMap() {
        Stream.of(1, 2, 2).collect(Collectors.toMap(Function.identity(), Function.identity()));
    }

    @Test
    public void toMap() {
        Map<Integer, Integer> expected = new HashMap<>();
        IntStream.range(1, 4).forEach(i -> expected.put(i, i));

        assertEquals(expected,
                IntStream.range(1, 4).boxed().collect(Collectors.toMap(Function.identity(), Function.identity())));
    }
}
