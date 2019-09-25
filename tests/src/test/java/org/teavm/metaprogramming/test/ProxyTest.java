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
package org.teavm.metaprogramming.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.proxy;
import static org.teavm.metaprogramming.Metaprogramming.unsupportedCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;

@CompileTime
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ProxyTest {
    @Test
    public void createsProxy() {
        A proxy = createProxy(A.class, "!");
        assertEquals("foo!", proxy.foo());
        assertEquals("bar!", proxy.bar());
    }

    @Meta
    private static native <T> T createProxy(Class<T> proxyType, String add);
    private static <T> void createProxy(ReflectClass<T> proxyType, Value<String> add) {
        if (proxyType.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
            String name = method.getName();
            exit(() -> name + add.get());
        });
        exit(() -> proxy.get());
    }

    @Test
    public void defaultReturnValue() {
        StringBuilder log = new StringBuilder();
        B proxy = createProxyWithDefaultReturnValue(B.class, log);
        assertNull(proxy.foo());
        assertEquals(0, proxy.bar());
        proxy.baz();

        assertEquals("foo;bar;baz;", log.toString());
    }

    @Meta
    private static native <T> T createProxyWithDefaultReturnValue(Class<T> proxyType, StringBuilder sb);
    private static <T> void createProxyWithDefaultReturnValue(ReflectClass<T> proxyType, Value<StringBuilder> sb) {
        if (proxyType.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }
        Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
            String name = method.getName();
            emit(() -> sb.get().append(name + ";"));
        });
        exit(() -> proxy.get());
    }

    @Test
    public void boxParameters() {
        C proxy = createProxyWithBoxedParameters(C.class);
        assertEquals("foo(true,0,1,2,3,4,5)", proxy.foo(true, (byte) 0, '1', (short) 2, 3, 4, "5"));
    }

    @Meta
    private static native <T> T createProxyWithBoxedParameters(Class<T> proxyType);
    private static <T> void createProxyWithBoxedParameters(ReflectClass<T> proxyType) {
        if (proxyType.getAnnotation(MetaprogrammingClass.class) == null) {
            unsupportedCase();
            return;
        }

        Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
            Value<StringBuilder> result = emit(() -> new StringBuilder());

            String name = method.getName();
            emit(() -> result.get().append(name).append('('));
            if (args.length > 0) {
                emit(() -> result.get().append(args[0].get().toString()));
                for (int i = 1; i < args.length; ++i) {
                    Value<Object> arg = args[i];
                    emit(() -> result.get().append(',').append(arg.get().toString()));
                }
            }
            emit(() -> result.get().append(')'));

            exit(() -> result.get().toString());
        });
        exit(() -> proxy.get());
    }

    @MetaprogrammingClass
    interface A {
        String foo();

        String bar();
    }

    @MetaprogrammingClass
    interface B {
        String foo();

        int bar();

        void baz();
    }

    @MetaprogrammingClass
    interface C {
        String foo(boolean a, byte b, char c, short d, int e, long f, String g);
    }
}
