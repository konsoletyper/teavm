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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class EnumTest {
    private enum Foo {
        A, B, C
    }

    private enum Bar {
        D, E
    }

    @Test
    public void sameConstantsAreEqual() {
        assertEquals(Foo.A, Foo.A);
    }

    @Test
    public void differentConstansAreNotEqual() {
        assertNotEquals(Foo.A, Foo.B);
    }

    @Test
    public void constantsOfDifferentEnumsAreNotEqual() {
        assertNotEquals(Foo.A, Bar.D);
    }

    @Test
    public void declaringClassComputed() {
        assertEquals(Foo.class, Foo.A.getDeclaringClass());
    }

    @Test
    public void comparisonGivesZeroForSameConstants() {
        assertEquals(0, Foo.A.compareTo(Foo.A));
    }

    @Test
    public void comparisonGivesPositiveForLaterConstant() {
        assertTrue(Foo.B.compareTo(Foo.A) > 0);
    }

    @Test
    public void comparisonGivesNegativeForEarlierConstant() {
        assertTrue(Foo.A.compareTo(Foo.B) < 0);
    }

    @Test
    public void valueOfReturnsConstant() {
        assertEquals("A", Enum.valueOf(Foo.class, "A").name());
    }
}
