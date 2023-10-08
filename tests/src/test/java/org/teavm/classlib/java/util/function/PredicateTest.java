/*
 *  Copyright 2023 Jasper Siepkes.
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
package org.teavm.classlib.java.util.function;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class PredicateTest {

    @Test
    public void andWorks() {
        Predicate<Integer> greaterThenFour = x -> x > 4;
        Predicate<Integer> greaterThenEight = x -> x > 8;

        assertTrue(greaterThenFour.and(greaterThenEight).test(10));
        assertFalse(greaterThenFour.and(greaterThenEight).test(6));
    }

    @Test
    public void negateWorks() {
        Predicate<Integer> greaterThenFour = x -> x > 4;

        assertFalse(greaterThenFour.negate().test(10));
        assertTrue(greaterThenFour.negate().test(1));
    }

    @Test
    public void orWorks() {
        Predicate<Integer> smallerThenFour = x -> x < 4;
        Predicate<Integer> greaterThenEight = x -> x > 8;

        assertTrue(smallerThenFour.or(greaterThenEight).test(3));
        assertFalse(smallerThenFour.or(greaterThenEight).test(6));
        assertTrue(smallerThenFour.or(greaterThenEight).test(9));
    }

    @Test
    public void isEqualWorks() {
        Predicate<Integer> isThree = Predicate.isEqual(3);

        assertFalse(isThree.test(2));
        assertTrue(isThree.test(3));
        assertFalse(isThree.test(4));
    }

    @Test
    public void notWorks() {
        Predicate<Integer> isThree = x -> x == 3;

        assertTrue(Predicate.not(isThree).test(2));
        assertFalse(Predicate.not(isThree).test(3));
        assertTrue(Predicate.not(isThree).test(4));
    }
}
