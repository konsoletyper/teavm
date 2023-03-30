/*
 *  Copyright 2023 ihromant.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Comparator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class ComparatorTest {
    @Test
    public void naturalOrder() {
        Integer i = 2;
        Integer j = 3;
        Comparator<Integer> comp = Comparator.naturalOrder();
        assertTrue(comp.compare(i, j) < 0);
        assertEquals(0, comp.compare(i, i));
        assertTrue(comp.compare(j, i) > 0);
        try {
            comp.compare(i, null);
            fail("Expected NPE for comparing with null");
        } catch (NullPointerException e) {
            // OK
        }
        try {
            comp.compare(null, i);
            fail("Expected NPE for comparing with null");
        } catch (NullPointerException e) {
            // OK
        }
    }

    @Test
    public void testComparing() {
        Point a = new Point(1, 1);
        Point b = new Point(1, 2);
        Point c = new Point(1, null);
        Comparator<Point> comp = Comparator.comparing(Point::getX).thenComparing(Point::getY);
        assertTrue(comp.compare(a, b) < 1);
        try {
            comp.compare(a, c);
            fail("Expected NPE for comparing null fields");
        } catch (NullPointerException e) {
            // OK
        }
    }

    private static class Point {
        private final Integer x;
        private final Integer y;

        private Point(Integer x, Integer y) {
            this.x = x;
            this.y = y;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }
    }
}
