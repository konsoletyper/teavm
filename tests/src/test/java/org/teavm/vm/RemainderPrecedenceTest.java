/*
 *  Copyright 2026 Ethan Resnick.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class RemainderPrecedenceTest {
    private float[] floats = { 7, 2, 3, 12, 4 };
    private double[] doubles = { 7, 2, 3, 12, 4 };

    @Test
    public void floatRemainderKeepsGrouping() {
        var a = floats;
        assertEquals(1f, a[0] % (a[1] * a[2]), 0);
        assertEquals(1f, a[0] % (a[3] / a[4]), 0);
        assertEquals(14f, a[0] * (a[1] % a[2]), 0);
        assertEquals(2f, (a[0] * a[1]) % a[2], 0);
        assertEquals(3f, (a[0] % a[1]) * a[2], 0);
        assertEquals(0f, a[3] % (a[0] % a[1]), 0);
    }

    @Test
    public void doubleRemainderKeepsGrouping() {
        var a = doubles;
        assertEquals(1.0, a[0] % (a[1] * a[2]), 0);
        assertEquals(1.0, a[0] % (a[3] / a[4]), 0);
        assertEquals(14.0, a[0] * (a[1] % a[2]), 0);
        assertEquals(2.0, (a[0] * a[1]) % a[2], 0);
        assertEquals(3.0, (a[0] % a[1]) * a[2], 0);
        assertEquals(0.0, a[3] % (a[0] % a[1]), 0);
    }
}
