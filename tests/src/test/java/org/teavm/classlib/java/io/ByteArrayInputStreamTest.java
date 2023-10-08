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
package org.teavm.classlib.java.io;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ByteArrayInputStreamTest {
    @Test
    public void readsSingleByte() {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[] { 0, 1, -1, 127, -128, 23, -23 });
        assertEquals(0, input.read());
        assertEquals(1, input.read());
        assertEquals(255, input.read());
        assertEquals(127, input.read());
        assertEquals(128, input.read());
        assertEquals(23, input.read());
        assertEquals(233, input.read());
        assertEquals(-1, input.read());
    }
}
