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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.support.Proxiable;
import org.teavm.classlib.support.ProxyConfiguration;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class ProxyTest {
    @Test
    public void proxyWorks() {
        var sb = new StringBuilder();
        var handler = new LoggingInvocationHandler(sb);
        var proxy = Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class }, handler);
        assertTrue("Proxy should implement interface", proxy instanceof A);
        assertTrue("Proxy should extend Proxy class", proxy instanceof Proxy);
        assertTrue("Proxy.isProxyClass works", Proxy.isProxyClass(proxy.getClass()));
        assertSame("Invocation handler extracted", handler, Proxy.getInvocationHandler(proxy));

        var a = (A) proxy;
        a.foo();
        a.bar();
        a.toString();

        assertEquals("A.foo;A.bar;Object.toString;", sb.toString());
        
        var other = Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class }, handler);
        assertSame("Proxy class is the same", proxy.getClass(), other.getClass());
    }

    @Test
    public void proxyInstancePassed() {
        var captured = new Object[1];
        var a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (p, m, _) -> {
                    captured[0] = p;
                    if (m.getName().equals("toString")) {
                        return "proxy@" + System.identityHashCode(p);
                    }
                    return null;
                });
        a.foo();
        assertSame("Proxy instance captured", a, captured[0]);
    }

    @Test
    public void methodPriority() {
        var sb = new StringBuilder();
        var handler = new LoggingInvocationHandler(sb);
        var proxy = Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class, B.class }, handler);
        
        assertTrue("Proxy should implement A", proxy instanceof A);
        assertTrue("Proxy should implement B", proxy instanceof B);

        var a = (A) proxy;
        var b = (B) proxy;
        a.foo();
        a.bar();
        b.bar();
        b.baz();

        assertEquals("A.foo;A.bar;A.bar;B.baz;", sb.toString());

        sb.setLength(0);
        proxy = Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { B.class, A.class }, handler);

        a = (A) proxy;
        b = (B) proxy;
        a.foo();
        a.bar();
        b.bar();
        b.baz();

        assertEquals("A.foo;B.bar;B.bar;B.baz;", sb.toString());
    }

    @Test
    public void reflectionWorks() {
        var sb = new StringBuilder();
        var underlying = new C() {
            @Override
            public int foo(long x) {
                return (int) (x * 2);
            }

            @Override
            public String bar(Integer x) {
                return x != null ? "#" + Integer.toHexString(x) : null;
            }
        };
        var handler = new CapturingInvocationHandler(underlying, sb);
        var proxy = (C) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { C.class }, handler);
        assertEquals(46, proxy.foo(23));
        assertEquals("#fe", proxy.bar(254));
        assertNull(proxy.bar(null));

        assertEquals("C.foo(23[Long]):46\nC.bar(254[Integer]):#fe\nC.bar(null):null\n", sb.toString());
    }
    
    @Test
    public void inheritance() {
        var sb = new StringBuilder();
        var underlying = new D() {
            @Override
            public int foo(long x) {
                return (int) (x * 2);
            }

            @Override
            public String bar(Integer x) {
                return x != null ? "#" + Integer.toHexString(x) : null;
            }
            
            @Override
            public int baz(byte x, short y) {
                return x * 2 + y;
            }
        };
        var handler = new CapturingInvocationHandler(underlying, sb);
        var proxy = (D) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { D.class }, handler);
        assertEquals(46, proxy.foo(23));
        assertEquals("#fe", proxy.bar(254));
        assertEquals(43, proxy.baz((byte) 20, (short) 3));
        assertEquals("C.foo(23[Long]):46\nD.bar(254[Integer]):#fe\nD.baz(20[Byte],3[Short]):43\n", sb.toString());
    }

    @Test
    public void wrongArgs() {
        assertFalse("Proxy.isProxyClass works for non-proxy", Proxy.isProxyClass(Object.class));

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { NonInterface.class },
                    (_, _, _) -> null);
            fail("Should have thrown IAE with non-interface");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { null },
                    (_, _, _) -> null);
            fail("Should have thrown IAE with null in interface list");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class, A.class },
                    (_, _, _) -> null);
            fail("Should have thrown IAE with duplicate interface");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(null, new Class[] { A.class }, (_, _, _) -> null);
            fail("Should have thrown IAE with null class loader");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), null, (_, _, _) -> null);
            fail("Should have thrown NPE with null interfaces");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class }, null);
            fail("Should have thrown IAE with null handler");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            Proxy.getInvocationHandler(new Object());
            fail("Should have thrown IAE for non-proxy object");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void brokenHandler() {
        var c = (C) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { C.class },
                (_, _, _) -> null);
        try {
            c.foo(23);
            fail("Should have thrown NPE when handler returns null for primitive return type");
        } catch (NullPointerException e) {
            // expected
        }
        
        c = (C) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { C.class },
                (_, m, _) -> {
                    if (m.getName().equals("foo")) {
                        return "q";
                    } else {
                        return 23;
                    }
                });
        
        try {
            c.foo(23);
            fail("Should have thrown CCE when handler returns wrong type: Integer expected and got String");
        } catch (ClassCastException e) {
            // expected
        }

        try {
            c.bar(23);
            fail("Should have thrown CCE when handler returns wrong type: String expected and got Integer");
        } catch (ClassCastException e) {
            // expected
        }
    }
    
    @Test
    public void nullArgsArrayWhenNoParameters() {
        var nonNull = new boolean[1];
        var a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (_, _, args) -> {
                    nonNull[0] = args != null;
                    return null;
                });
        a.foo();
        assertFalse("Args array passed to handler should be null when no parameters", nonNull[0]);
    }
    
    @Test
    public void primitiveArgs() {
        var sb = new StringBuilder();
        var underlying = new E() {
            @Override
            public boolean foo(byte a, short b, int c, long d, float e, double f, char g, boolean h) {
                return true;
            }
        };
        var e = (E) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { E.class },
                new CapturingInvocationHandler(underlying, sb));
        e.foo((byte) 23, (short) 42, 23, 42L, 23.0f, 42.0, 'a', false);
        assertEquals("E.foo(23[Byte],42[Short],23[Integer],42[Long],23.0[Float],42.0[Double]," 
                + "a[Character],false[Boolean]):true\n", sb.toString());
    }

    @Test
    public void objectMethodsRouted() {
        var hashCodeResult = new int[] { -1 };
        var equalsArg = new Object[1];
        var a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (_, m, args) -> {
                    if ("hashCode".equals(m.getName())) { 
                        hashCodeResult[0] = 42; 
                        return 42; 
                    }
                    if ("equals".equals(m.getName())) { 
                        equalsArg[0] = args[0]; 
                        return false; 
                    }
                    return null;
                });
        assertEquals(42, a.hashCode());
        assertEquals(42, hashCodeResult[0]);
        var other = new Object();
        assertFalse(a.equals(other));
        assertSame(other, equalsArg[0]);
    }

    @Test
    public void getClassNotRouted() {
        var called = new boolean[1];
        var a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (_, _, _) -> {
                    called[0] = true;
                    return null;
                });
        called[0] = false;
        var cls = a.getClass();
        assertFalse("getClass() should not be routed through handler", called[0]);
        assertTrue("getClass() returns proxy class", Proxy.isProxyClass(cls));
    }

    @Test
    public void primitiveReturnTypes() {
        var f = (F) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { F.class },
                (_, m, _) -> switch (m.getName()) {
                    case "byteMethod" -> (byte) 23;
                    case "charMethod" -> 'q';
                    case "shortMethod" -> (short) 42;
                    case "longMethod" -> 100L;
                    case "floatMethod" -> 3.14f;
                    default -> 2.718;
                });
        assertEquals((byte) 23, f.byteMethod());
        assertEquals('q', f.charMethod());
        assertEquals((short) 42, f.shortMethod());
        assertEquals(100L, f.longMethod());
        assertEquals(3.14f, f.floatMethod(), 0.001f);
        assertEquals(2.718, f.doubleMethod(), 0.00001);
    }

    @Test
    @SkipJVM
    public void unregisteredInterface() {
        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { G.class },
                    (_, _, _) -> null);
            fail("Should have thrown SecurityException for unregistered interface");
        } catch (SecurityException e) {
            // expected
        }

        try {
            Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class, C.class },
                    (_, _, _) -> null);
            fail("Should have thrown SecurityException for unregistered interface");
        } catch (SecurityException e) {
            // expected
        }
    }

    @Test
    public void exceptionPropagated() throws Throwable {
        var a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (_, _, _) -> {
                    throw new IllegalArgumentException("test");
                });
        try {
            a.foo();
            fail("Exception not caught");
        } catch (IllegalArgumentException e) {
            assertEquals("test", e.getMessage());
        }

        a = (A) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { A.class },
                (_, _, _) -> {
                    throw new AssertionError("test error");
                });
        try {
            a.foo();
            fail("Error not caught");
        } catch (AssertionError e) {
            assertEquals("test error", e.getMessage());
        }
        
        var h = (H) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { H.class }, (_, _, _) -> {
            throw new IOException();
        });
        try {
            h.foo();
            fail("Checked exception not caught");
        } catch (IOException e) {
            // caught, as expected
        }
        try {
            h.bar();
            fail("Checked exception not wrapped");
        } catch (UndeclaredThrowableException e) {
            // caught, as expected
            assertTrue(e.getUndeclaredThrowable() instanceof IOException);
        }

        h = (H) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(), new Class[] { H.class }, (_, _, _) -> {
            throw new InterruptedException();
        });
        try {
            h.foo();
            fail("Unsupported checked exception not wrapped");
        } catch (UndeclaredThrowableException e) {
            // caught, as expected
            assertTrue(e.getUndeclaredThrowable() instanceof InterruptedException);
        }
        try {
            h.bar();
            fail("Checked exception not wrapped");
        } catch (UndeclaredThrowableException e) {
            // caught, as expected
            assertTrue(e.getUndeclaredThrowable() instanceof InterruptedException);
        }
    }

    private static class LoggingInvocationHandler implements InvocationHandler {
        private final StringBuilder sb;

        LoggingInvocationHandler(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            sb.append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName()).append(";");
            if (method.getName().equals("toString")) {
                return "";
            }
            return null;
        }
    }

    private static class CapturingInvocationHandler implements InvocationHandler {
        private final Object instance;
        private final StringBuilder sb;

        CapturingInvocationHandler(Object instance, StringBuilder sb) {
            this.instance = instance;
            this.sb = sb;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            sb.append(method.getDeclaringClass().getSimpleName()).append(".").append(method.getName()).append("(");
            if (args.length > 0) {
                sb.append(args[0]);
                if (args[0] != null) {
                    sb.append("[").append(args[0].getClass().getSimpleName()).append("]");
                }
                for (int i = 1; i < args.length; ++i) {
                    sb.append(",");
                    sb.append(args[i]);
                    if (args[i] != null) {
                        sb.append("[").append(args[i].getClass().getSimpleName()).append("]");
                    }
                }
            }
            sb.append("):");
            var result = method.invoke(instance, args);
            sb.append(result).append("\n");
            return result;
        }
    }

    @Proxiable
    @ProxyConfiguration(B.class)
    public interface A {
        void foo();

        void bar();

        String toString();
    }

    @Proxiable
    @ProxyConfiguration(A.class)
    public interface B {
        void bar();

        void baz();
    }

    @Proxiable
    public interface C {
        int foo(long x);

        String bar(Integer x);
    }

    @Proxiable
    public interface D extends C {
        @Override
        String bar(Integer x);
        
        int baz(byte x, short y);
    }
    
    @Proxiable
    public interface E {
        boolean foo(byte a, short b, int c, long d, float e, double f, char g, boolean h);
    }

    @Proxiable
    public interface F {
        byte byteMethod();
        char charMethod();
        short shortMethod();
        long longMethod();
        float floatMethod();
        double doubleMethod();
    }

    public interface G {
        void something();
    }

    @Proxiable
    public interface H {
        void foo() throws IOException;
        
        void bar();
    }

    public static class NonInterface {
    }
}
