/*
 *  Copyright 2016 Alexey Andreev.
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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class LongTest {
    @Test
    public void longIntegersMultipied() {
        long a = id(1199747L);
        long b = id(1062911L);
        assertEquals(1275224283517L, a * b);
        assertEquals(-1275224283517L, a * -b);
        a = id(229767376164L);
        b = id(907271478890L);
        assertEquals(-5267604004427634456L, a * b);
        assertEquals(5267604004427634456L, a * -b);
    }

    @Test
    public void longIntegersDivided() {
        long a = id(12752242835177213L);
        long b = id(1062912L);
        assertEquals(11997458712L, a / b);
        assertEquals(-11997458712L, a / -b);
    }

    @Test
    public void longAdditionWorks() {
        long a = id(1134903170);
        long b = id(1836311903);
        assertEquals(2971215073L, a + b);
    }

    @Test
    public void smallLongDivision() {
        long a = id(-1);
        long b = 3;
        assertEquals(0, a / b);
    }

    private static long id(long value) {
        return value;
    }
}
