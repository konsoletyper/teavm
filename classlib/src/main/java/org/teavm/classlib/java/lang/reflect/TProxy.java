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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TObject;

/**
 * Proxy provides dynamic proxy class generation for interfaces.
 *
 * <p>In TeaVM's single-threaded JavaScript environment, true runtime class generation
 * is not available. Instead, this implementation creates proxy instances that implement
 * the specified interfaces and delegate method calls to the provided InvocationHandler.</p>
 *
 * <p>Proxy instances are created via {@link #newProxyInstance} and method calls are
 * forwarded to the InvocationHandler's invoke method. The Method object and arguments
 * passed to the handler are populated at the call site.</p>
 *
 * <p>Limitations compared to JDK Proxy:</p>
 * <ul>
 *   <li>Proxy classes are not actually generated at runtime; instead a lightweight
 *       dispatching mechanism is used</li>
 *   <li>{@link #getProxyClass} returns a synthetic class rather than a truly generated one</li>
 *   <li>equals/hashCode/toString are dispatched to the handler like any other method</li>
 * </ul>
 */
public class TProxy extends TObject {
    protected TInvocationHandler h;

    private static int nextClassNameIndex = 0;

    protected TProxy(TInvocationHandler h) {
        this.h = h;
    }

    public static TInvocationHandler getInvocationHandler(Object proxy) {
        if (proxy instanceof TProxy) {
            return ((TProxy) proxy).h;
        }
        throw new TIllegalArgumentException("not a proxy instance");
    }

    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces,
            TInvocationHandler h) {
        if (h == null) {
            throw new NullPointerException();
        }
        if (interfaces == null) {
            throw new NullPointerException();
        }
        for (Class<?> intf : interfaces) {
            if (!intf.isInterface()) {
                throw new TIllegalArgumentException(intf.getName() + " is not an interface");
            }
        }
        return new TProxyInstance(h, interfaces);
    }

    public static Class<?> getProxyClass(ClassLoader loader, Class<?>... interfaces) {
        if (interfaces.length > 65535) {
            throw new TIllegalArgumentException("interface limit exceeded");
        }
        for (Class<?> intf : interfaces) {
            if (!intf.isInterface()) {
                throw new TIllegalArgumentException(intf.getName() + " is not an interface");
            }
        }
        return TProxyInstance.class;
    }

    public static boolean isProxyClass(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        return TProxyInstance.class.isAssignableFrom(cl);
    }

    /**
     * Returns the invocation handler for the specified proxy instance.
     */
    public static TInvocationHandler getProxyInvocationHandler(Object proxy) {
        return getInvocationHandler(proxy);
    }
}
