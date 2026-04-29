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

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.classlib.java.lang.TObject;

/**
 * A concrete proxy instance that implements the specified interfaces
 * and delegates all method calls to the InvocationHandler.
 *
 * <p>This class serves as the runtime representation of a dynamic proxy
 * in TeaVM. Since TeaVM cannot generate new classes at runtime, all
 * proxy instances are of this type and method dispatch is handled
 * through the InvocationHandler.</p>
 */
public class TProxyInstance extends TProxy {
    private final Class<?>[] interfaces;

    TProxyInstance(TInvocationHandler h, Class<?>[] interfaces) {
        super(h);
        this.interfaces = interfaces.clone();
    }

    /**
     * Returns all interfaces implemented by this proxy instance.
     */
    public Class<?>[] getInterfaces() {
        return interfaces.clone();
    }

    /**
     * Dispatches a method call through the InvocationHandler.
     *
     * @param method the Method to invoke
     * @param args the arguments to the method
     * @return the result of the invocation
     * @throws Throwable if the invocation throws an exception
     */
    public Object dispatch(Method method, Object[] args) throws Throwable {
        try {
            return h.invoke(this, method, args);
        } catch (TUndeclaredThrowableException e) {
            throw e;
        } catch (Throwable e) {
            for (Class<?> intf : interfaces) {
                for (Method m : intf.getMethods()) {
                    if (m.getName().equals(method.getName())
                            && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) {
                        for (Class<?> exType : m.getExceptionTypes()) {
                            if (exType.isInstance(e)) {
                                throw e;
                            }
                        }
                    }
                }
            }
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new TUndeclaredThrowableException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return (String) h.invoke(this, Object.class.getMethod("toString"), null);
        } catch (Throwable e) {
            return super.toString();
        }
    }

    @Override
    public int hashCode() {
        try {
            Object result = h.invoke(this, Object.class.getMethod("hashCode"), null);
            return result instanceof Integer ? (Integer) result : super.hashCode();
        } catch (Throwable e) {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        try {
            Object result = h.invoke(this, Object.class.getMethod("equals", Object.class),
                    obj != null ? new Object[] { obj } : null);
            return result instanceof Boolean ? (Boolean) result : this == obj;
        } catch (Throwable e) {
            return this == obj;
        }
    }
}
