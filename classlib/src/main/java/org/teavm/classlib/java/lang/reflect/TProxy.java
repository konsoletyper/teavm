/*
 *  Copyright 2026 Alexey Andreev.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.interop.Intrinsified;
import org.teavm.interop.NoSideEffects;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ModifiersInfo;

public class TProxy implements TSerializable {
    protected TInvocationHandler h;
    private static Map<List<? extends TClass<?>>, TClass<?>> proxyClasses = new HashMap<>();
    private static List<TClass<?>> currentInterfaces = new ArrayList<>();

    protected TProxy(TInvocationHandler h) {
        this.h = h;
    }

    static {
        registerProxyClasses();
    }

    public static Object newProxyInstance(ClassLoader loader, TClass<?>[] interfaces, TInvocationHandler h) {
        if (loader == null) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(h);
        for (var i = 0; i < interfaces.length; i++) {
            var itf = interfaces[i];
            if (!itf.isInterface()) {
                throw new IllegalArgumentException();
            }
            for (var j = 0; j < i; ++j) {
                if (interfaces[j] == itf) {
                    throw new IllegalArgumentException();
                }
            }
        }
        var cls = proxyClasses.get(List.of(interfaces));
        if (cls == null) {
            throw new SecurityException();
        }
        var proxy = (TProxy) cls.getClassInfo().newInstance();
        proxy.h = h;
        return wrapDependency(proxy);
    }

    public static boolean isProxyClass(TClass<?> cls) {
        return (cls.getClassInfo().modifiers() & ModifiersInfo.PROXY) != 0;
    }

    public static TInvocationHandler getInvocationHandler(Object proxy) {
        if (proxy == null || !isProxyClass((TClass<?>) (Object) proxy.getClass())) {
            throw new IllegalArgumentException();
        }
        var p = (TProxy) proxy;
        return p.h;
    }

    @Intrinsified
    @NoSideEffects
    private static native Object wrapDependency(Object obj);

    @Intrinsified
    @NoSideEffects
    private static native void registerProxyClasses();

    private static void appendInterface(ClassInfo itf) {
        currentInterfaces.add((TClass<?>) (Object) itf.classObject());
    }

    private static void registerClass(ClassInfo cls) {
        proxyClasses.put(List.copyOf(currentInterfaces), (TClass<?>) (Object) cls.classObject());
        currentInterfaces.clear();
    }
}
