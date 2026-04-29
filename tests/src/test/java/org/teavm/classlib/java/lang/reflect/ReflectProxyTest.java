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
package org.teavm.classlib.java.lang.reflect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.lang.annotation.Native;
import java.lang.annotation.Repeatable;
import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@SkipPlatform(TestPlatform.WASI)
public class ReflectProxyTest {

    @Test
    public void proxyNewProxyInstance() {
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Runnable.class, TestInterface.class },
                (proxyObj, method, args) -> {
                    if (method.getName().equals("run")) {
                        return null;
                    }
                    if (method.getName().equals("getValue")) {
                        return "proxied";
                    }
                    return null;
                }
        );
        assertNotNull(proxy);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
    }

    @Test
    public void proxyGetInvocationHandler() {
        InvocationHandler handler = (proxyObj, method, args) -> null;
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Runnable.class },
                handler
        );
        InvocationHandler retrieved = Proxy.getInvocationHandler(proxy);
        assertEquals(handler, retrieved);
    }

    @Test
    public void proxyIsProxyClass() {
        assertFalse(Proxy.isProxyClass(String.class));
        assertFalse(Proxy.isProxyClass(Object.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void proxyNonInterface() {
        Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { String.class },
                (proxyObj, method, args) -> null
        );
    }

    @Test
    public void phantomReferenceAlwaysReturnsNull() {
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object obj = new Object();
        PhantomReference<Object> ref = new PhantomReference<>(obj, queue);
        assertEquals(null, ref.get());
    }

    @Test
    public void cleanerCreate() {
        Cleaner cleaner = Cleaner.create();
        assertNotNull(cleaner);
    }

    @Test
    public void cleanerRegisterAndClean() {
        Cleaner cleaner = Cleaner.create();
        boolean[] cleaned = { false };
        Object obj = new Object();
        Cleaner.Cleanable cleanable = cleaner.register(obj, () -> cleaned[0] = true);
        assertFalse(cleaned[0]);
        cleanable.clean();
        assertTrue(cleaned[0]);
    }

    @Test
    public void cleanerCleanOnlyOnce() {
        Cleaner cleaner = Cleaner.create();
        int[] count = { 0 };
        Object obj = new Object();
        Cleaner.Cleanable cleanable = cleaner.register(obj, () -> count[0]++);
        cleanable.clean();
        cleanable.clean();
        assertEquals(1, count[0]);
    }

    @Test
    public void undeclaredThrowableException() {
        Exception cause = new Exception("test");
        UndeclaredThrowableException ex = new UndeclaredThrowableException(cause);
        assertEquals(cause, ex.getUndeclaredThrowable());
        assertEquals(cause, ex.getCause());

        UndeclaredThrowableException ex2 = new UndeclaredThrowableException(cause, "message");
        assertEquals("message", ex2.getMessage());
        assertEquals(cause, ex2.getUndeclaredThrowable());
    }

    interface TestInterface {
        String getValue();
    }
}
