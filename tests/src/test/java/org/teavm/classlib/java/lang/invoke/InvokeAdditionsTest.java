/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.lang.invoke;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.lang.invoke.VarHandle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class InvokeAdditionsTest {

    @Test
    public void varHandleAccessMode() {
        VarHandle.AccessMode mode = VarHandle.AccessMode.GET;
        assertEquals("get", mode.methodName());

        VarHandle.AccessMode fromName = VarHandle.AccessMode.valueFromMethodName("compareAndSet");
        assertEquals(VarHandle.AccessMode.COMPARE_AND_SET, fromName);
    }

    @Test
    public void constantCallSite() {
        ConstantCallSite site = new ConstantCallSite(null);
        assertNotNull(site);
        assertEquals(null, site.getTarget());
        try {
            site.setTarget(null);
            assertTrue("Expected IllegalStateException", false);
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void mutableCallSite() {
        MutableCallSite site = new MutableCallSite((MethodType) null);
        assertEquals(null, site.getTarget());
        // setTarget should work
        site.setTarget(null);
        assertEquals(null, site.getTarget());
    }

    @Test
    public void switchPointValid() {
        SwitchPoint sp = new SwitchPoint();
        assertFalse(sp.hasBeenInvalidated());
    }

    @Test
    public void switchPointInvalidate() {
        SwitchPoint sp = new SwitchPoint();
        assertFalse(sp.hasBeenInvalidated());
        sp.invalidateAll();
        assertTrue(sp.hasBeenInvalidated());
    }

    @Test
    public void constantBootstrapsNullConstant() throws Throwable {
        Object result = ConstantBootstraps.nullConstant(MethodHandles.lookup(), "test", String.class);
        assertEquals(null, result);
    }

    @Test
    public void constantBootstrapsPrimitiveClass() throws Throwable {
        Class<?> intClass = ConstantBootstraps.primitiveClass(MethodHandles.lookup(), "int", Class.class);
        assertEquals(int.class, intClass);
    }

    @Test
    public void constantBootstrapsEnumConstant() throws Throwable {
        Object result = ConstantBootstraps.enumConstant(MethodHandles.lookup(), "NANOSECONDS", TimeUnit.class);
        assertEquals(TimeUnit.NANOSECONDS, result);
    }
}
