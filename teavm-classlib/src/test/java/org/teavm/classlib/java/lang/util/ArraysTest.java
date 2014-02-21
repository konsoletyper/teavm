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
package org.teavm.classlib.java.lang.util;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class ArraysTest {
    @Test
    public void arraySorted() {
        Integer[] array = { 2, 5, 7, 3, 5, 6 };
        Arrays.sort(array);
        assertEquals(Integer.valueOf(2), array[0]);
        assertEquals(Integer.valueOf(3), array[1]);
        assertEquals(Integer.valueOf(5), array[2]);
        assertEquals(Integer.valueOf(5), array[3]);
        assertEquals(Integer.valueOf(6), array[4]);
        assertEquals(Integer.valueOf(7), array[5]);
    }

    @Test
    public void arrayExposedAsList() {
        Integer[] array = { 2, 3, 4 };
        List<Integer> list = Arrays.asList(array);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(4), list.get(2));
    }
}
