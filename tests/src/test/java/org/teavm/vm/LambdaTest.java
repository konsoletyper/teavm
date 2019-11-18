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
package org.teavm.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.Serializable;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class LambdaTest {
    @Test
    public void lambdaWithMarkers() {
        Supplier<String> supplier = (Supplier<String> & A) () -> "OK";

        assertEquals("OK", supplier.get());
        assertTrue("Supplier is expected to implement A", supplier instanceof A);
    }

    @Test
    public void serializableLambda() {
        Supplier<String> supplier = (Supplier<String> & Serializable) () -> "OK";

        assertEquals("OK", supplier.get());
        assertTrue("Supplier is expected to implement Serializable", supplier instanceof Serializable);
    }

    @Test
    public void boxParameterToSupertype() {
        assertEquals(".*.*.*.*.*", acceptIntPredicate(this::oddPredicate));
    }

    private String acceptIntPredicate(IntPredicate p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; ++i) {
            sb.append(p.test(i) ? '*' : '.');
        }
        return sb.toString();
    }

    private boolean oddPredicate(Object o) {
        return o instanceof Integer && ((Integer) o) % 2 != 0;
    }

    interface A {
    }
}
