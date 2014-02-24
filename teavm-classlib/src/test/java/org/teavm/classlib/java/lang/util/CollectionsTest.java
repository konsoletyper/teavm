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

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class CollectionsTest {
    private List<Integer> array;

    @Test
    public void arraySorted() {
        array = new ArrayList<>();
        array.addAll(Arrays.asList(2, 5, 7, 3, 5, 6));
        Collections.sort(array);
        assertEquals(Integer.valueOf(2), array.get(0));
        assertEquals(Integer.valueOf(3), array.get(1));
        assertEquals(Integer.valueOf(5), array.get(2));
        assertEquals(Integer.valueOf(5), array.get(3));
        assertEquals(Integer.valueOf(6), array.get(4));
        assertEquals(Integer.valueOf(7), array.get(5));
    }
}
