/*
 *  Copyright 2013 Alexey Andreev.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ObjectTest {
    @Test
    public void objectCreated() {
        Object a = new Object();
        assertNotNull(a);
    }

    @Test
    public void differentInstancesNotEqual() {
        Object a = new Object();
        Object b = new Object();
        assertNotEquals(a, b);
    }

    @Test
    public void sameInstancesAreEqual() {
        Object a = new Object();
        Object b = a;
        assertEquals(a, b);
    }

    @Test
    public void multipleGetClassCallsReturnSameValue() {
        Object a = new Object();
        assertSame(a.getClass(), a.getClass());
    }

    @Test
    public void sameClassesAreEqual() {
        Object a = new Object();
        Object b = new Object();
        assertSame(a.getClass(), b.getClass());
    }

    @Test
    public void properInstanceDetected() {
        assertTrue(Object.class.isInstance(new Object()));
    }

    @Test
    public void toStringWorks() {
        assertTrue(new Object().toString().startsWith("java.lang.Object@"));
        assertTrue(new Object[2].toString().startsWith("[Ljava.lang.Object;@"));
        assertTrue(new byte[3].toString().startsWith("[B@"));
    }

    @Test
    @SkipPlatform(TestPlatform.WASI)
    public void waitWorks() throws InterruptedException {
        long start = System.currentTimeMillis();
        final Object lock = new Object();
        synchronized (lock) {
            lock.wait(110);
        }
        long end = System.currentTimeMillis();
        assertTrue(end - start > 100);
    }
}
