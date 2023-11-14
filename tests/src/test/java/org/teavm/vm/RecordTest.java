/*
 *  Copyright 2022 Alexey Andreev.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class RecordTest {
    @Test
    public void equalsMethod() {
        assertFalse(new A(2, "q").equals(null));
        assertEquals(new A(2, "q"), new A(2, "q"));
        assertNotEquals(new A(2, "q"), new A(3, "q"));
        assertNotEquals(new A(2, "q"), new A(2, "w"));
    }

    @Test
    public void hashCodeMethod() {
        assertEquals(new A(2, "q").hashCode(), new A(2, "q").hashCode());
    }

    @Test
    public void toStringMethod() {
        String s = new B(2, "q", 3L, (byte) 4, (short) 5).toString();

        int index = 0;

        index = s.indexOf("B", index);
        assertTrue(index >= 0);
        ++index;

        index = checkRecordKey(s, "a", "2", index);
        index = checkRecordKey(s, "b", "q", index);
        index = checkRecordKey(s, "c", "3", index);
        index = checkRecordKey(s, "d", "4", index);
        checkRecordKey(s, "e", "5", index);
    }

    private static int checkRecordKey(String s, String key, String value, int index) {
        index = s.indexOf(key, index);
        assertTrue(index > 0);
        ++index;

        index = s.indexOf(value, index);
        assertTrue(index > 0);
        ++index;

        return index;
    }

    record A(int x, String y) {
    }

    record B(int a, String b, Long c, byte d, short e) {
    }
}
