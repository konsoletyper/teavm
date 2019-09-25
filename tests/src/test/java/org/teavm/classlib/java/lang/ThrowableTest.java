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
package org.teavm.classlib.java.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ThrowableTest {
    @Test
    public void causeWorks() {
        RuntimeException e = new RuntimeException("fail", new RuntimeException("OK"));
        assertTrue(e.getCause() instanceof RuntimeException);
        assertEquals("OK", e.getCause().getMessage());
    }

    @Test
    public void toStringWorks() {
        assertEquals("java.lang.RuntimeException: fail", new RuntimeException("fail").toString());
        assertEquals("java.lang.RuntimeException", new RuntimeException().toString());
    }
}
