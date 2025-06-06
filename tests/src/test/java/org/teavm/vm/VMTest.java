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
package org.teavm.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class VMTest {
    @Test
    public void multiArrayCreated() {
        int[][] array = new int[2][3];
        assertEquals(2, array.length);
        assertEquals(3, array[0].length);
        assertEquals(int[][].class, array.getClass());
        assertEquals(int[].class, array[0].getClass());
    }

    @Test
    public void longMultiArrayCreated() {
        long[][] array = new long[3][2];
        assertEquals(3, array.length);
        assertEquals(2, array[1].length);
        assertEquals(2, array[2].length);

        for (int i = 0; i < array.length; ++i) {
            assertEquals(2, array[i].length);
            for (int j = 0; j < array[i].length; ++j) {
                assertEquals(0, array[i][j]);
            }
        }

        for (int i = 0; i < array.length; ++i) {
            Arrays.fill(array[i], 0x0123456789ABCDEFL);
        }

        for (int i = 0; i < array.length; ++i) {
            assertEquals(2, array[i].length);
            for (int j = 0; j < array[i].length; ++j) {
                assertEquals(0x0123456789ABCDEFL, array[i][j]);
            }
        }
    }

    @Test
    public void catchExceptionFromLambda() {
        try {
            Runnable r = () -> throwException();
            r.run();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Test
    public void emptyMultiArrayCreated() {
        int[][] array = new int[0][0];
        assertEquals(0, array.length);
        assertEquals(int[][].class, array.getClass());
    }

    @Test
    public void emptyMultiArrayCreated2() {
        int[][][] array = new int[1][0][1];
        assertEquals(1, array.length);
        assertEquals(0, array[0].length);
        assertEquals(int[][][].class, array.getClass());
    }

    @Test
    public void emptyMultiArrayCreated3() {
        int[][][] array = new int[1][1][0];
        assertEquals(1, array.length);
        assertEquals(1, array[0].length);
        assertEquals(0, array[0][0].length);
        assertEquals(int[][][].class, array.getClass());
    }

    @Test
    public void catchesException() {
        var wasCaught = false;
        try {
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            wasCaught = true;
        }
        assertTrue(wasCaught);
    }

    @Test
    public void tryCatchInCatch() {
        var list = new ArrayList<String>();
        try {
            list.add("1");
            throw new RuntimeException();
        } catch (Throwable t) {
            try {
                list.add("2");
                throw new RuntimeException();
            } catch (Throwable t2) {
                list.add("3");
            }
        }
        assertEquals(List.of("1", "2", "3"), list);
    }

    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY)
    public void setsVariableBeforeTryCatch() {
        int a = 23;
        try {
            a = Integer.parseInt("not a number");
            fail("Exception not thrown");
        } catch (NumberFormatException e) {
            // do nothing
        }
        assertEquals(23, a);
    }

    @Test
    public void emptyTryCatchInsideFinally() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("before;");
            try {
                sb.append("inside;");
                Integer.parseInt("not a number");
                sb.append("ignore;");
            } catch (NumberFormatException e) {
                // do nothing
            }
            sb.append("after;");
        } finally {
            sb.append("finally;");
        }
        assertEquals("before;inside;after;finally;", sb.toString());
    }

    @Test
    public void catchFinally() {
        StringBuilder sb = new StringBuilder();
        List<String> a = Arrays.asList("a", "b");
        if (a.isEmpty()) {
            return;
        }
        try {
            for (String b : a) {
                if (b.length() < 3) {
                    sb.append(b);
                }
            }
        } finally {
            sb.append("finally;");
        }
    }

    @Test
    public void surrogateInStringLiteralsWork() {
        assertEquals(0xDDC2, "a\uDDC2b".charAt(1));
    }

    @Test
    public void subtractingNegativeWorks() {
        int a = 23;
        int b = a - 0xFFFFFFFF;
        assertEquals(24, b);
    }

    @Test
    public void separatesExceptionAndVariable() {
        int n = foo();
        try {
            bar();
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals(2, n);
        }
    }
    private int foo() {
        return 2;
    }
    private void bar() {
        throw new RuntimeException();
    }

    // See https://github.com/konsoletyper/teavm/issues/167
    @Test
    public void passesStaticFieldToSuperClassConstructor()  {
        SubClass obj = new SubClass();
        assertNotNull(obj.getValue());
    }

    // See https://github.com/konsoletyper/teavm/issues/196
    @Test
    public void stringConstantsInitializedProperly() {
        assertEquals("FIRST ", ClassWithStaticField.foo(true));
        assertEquals("SECOND ", ClassWithStaticField.foo(false));
    }

    @Test
    public void stringConcat() {
        assertEquals("(23)", surroundWithParentheses(23));
        assertEquals("(42)", surroundWithParentheses(42));
        assertEquals("(17)", surroundWithParentheses((byte) 17));
        assertEquals("(19)", surroundWithParentheses((short) 19));
    }

    private String surroundWithParentheses(int value) {
        return "(" + value + ")";
    }

    private String surroundWithParentheses(byte value) {
        return "(" + value + ")";
    }

    private String surroundWithParentheses(short value) {
        return "(" + value + ")";
    }

    @Test
    public void variableReadInCatchBlock() {
        int n = foo();
        try {
            for (int i = 0; i < 10; ++i) {
                n += foo();
            }
            bar();
            n += foo() * 5;
        } catch (RuntimeException e) {
            assertEquals(RuntimeException.class, e.getClass());
            assertEquals(22, n);
        }
    }

    @Test
    public void inlineThrow() {
        int x = id(23);
        if (x == 42) {
            x++;
            throwException();
            x++;
        }
        assertEquals(x, id(23));
    }

    @Test
    @SkipJVM
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncClinit() {
        assertEquals(0, initCount);
        assertEquals("foo", AsyncClinitClass.foo());
        assertEquals(1, initCount);
        assertEquals("ok", AsyncClinitClass.state);
        assertEquals("bar", AsyncClinitClass.bar());
        assertEquals(1, initCount);
        assertEquals("ok", AsyncClinitClass.state);
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncClinitField() {
        assertEquals("ok", AsyncClinitClass.state);
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncClinitInstance() {
        AsyncClinitClass acl = new AsyncClinitClass();
        assertEquals("ok", AsyncClinitClass.state);
        assertEquals("ok", acl.instanceState);
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncWait() {
        AsyncClinitClass acl = new AsyncClinitClass();
        acl.doWait();
        assertEquals("ok", acl.instanceState);
    }

    @Test
    @SkipJVM
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void loopAndExceptionPhi() {
        int[] a = createArray();
        int s = 0;
        for (int i = 0; i < 10; ++i) {
            int x = 0;
            try {
                x += 2;
                x += 3;
            } catch (RuntimeException e) {
                fail("Unexpected exception caught: " + x);
            }
            s += a[0] + a[1];
        }
        assertEquals(30, s);
    }

    @Test
    @SkipJVM
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncTryCatch() {
        try {
            throwExceptionAsync();
            fail("Exception should have been thrown");
        } catch (RuntimeException e) {
            assertEquals("OK", e.getMessage());
        }
    }

    @Test
    @SkipJVM
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void asyncExceptionHandler() {
        try {
            throw new RuntimeException("OK");
        } catch (RuntimeException e) {
            assertEquals("OK", suspendAndReturn(e).getMessage());
        }
    }

    @Async
    private static native void throwExceptionAsync();
    private static void throwExceptionAsync(AsyncCallback<Void> callback) {
        callback.error(new RuntimeException("OK"));
    }

    @Async
    private static native <T> T suspendAndReturn(T value);
    private static <T> void suspendAndReturn(T value, AsyncCallback<T> callback) {
        callback.complete(value);
    }

    @Test
    public void defaultMethodsSupported() {
        WithDefaultMethod[] instances = { new WithDefaultMethodDerivedA(), new WithDefaultMethodDerivedB(),
                new WithDefaultMethodDerivedC() };
        StringBuilder sb = new StringBuilder();
        for (WithDefaultMethod instance : instances) {
            sb.append(instance.foo() + "," + instance.bar() + ";");
        }

        assertEquals("default,A;default,B;overridden,C;", sb.toString());
    }

    @Test
    public void clinitReadsState() {
        initCount = 23;
        assertEquals(23, ReadingStateInClinit.state);
    }

    @Test
    public void implementInBaseMethodWithDefault() {
        SubclassWithInheritedImplementation o = new SubclassWithInheritedImplementation();
        assertEquals(1, o.x);
        assertEquals(2, new SubclassWithInheritedDefaultImplementation().foo());
    }

    static class BaseClassWithImplementation {
        public int foo() {
            return 1;
        }
    }

    interface BaseInterfaceWithDefault {
        default int foo() {
            return 2;
        }
    }

    static class IntermediateClassInheritingImplementation extends BaseClassWithImplementation {
    }

    static class SubclassWithInheritedImplementation extends IntermediateClassInheritingImplementation
            implements BaseInterfaceWithDefault {
        int x;

        SubclassWithInheritedImplementation() {
            x = foo();
        }
    }

    static class SubclassWithInheritedDefaultImplementation implements BaseInterfaceWithDefault {
    }

    interface WithDefaultMethod {
        default String foo() {
            return "default";
        }

        String bar();
    }

    class WithDefaultMethodDerivedA implements WithDefaultMethod {
        @Override
        public String bar() {
            return "A";
        }
    }

    class WithDefaultMethodDerivedB implements WithDefaultMethod {
        @Override
        public String bar() {
            return "B";
        }
    }
    class WithDefaultMethodDerivedC implements WithDefaultMethod {
        @Override
        public String foo() {
            return "overridden";
        }

        @Override
        public String bar() {
            return "C";
        }
    }


    @JSBody(script = "return [1, 2]")
    private static native int[] createArray();

    static int initCount;

    private static class ReadingStateInClinit {
        public static final int state = initCount;
    }

    private static class AsyncClinitClass {
        static String state = "";
        String instanceState = "";

        static {
            initCount++;
            try {
                Thread.sleep(1);
                state += "ok";
            } catch (InterruptedException e) {
                state += "error";
            }
        }

        public static String foo() {
            return "foo";
        }

        public static String bar() {
            return "bar";
        }

        public AsyncClinitClass() {
            instanceState += "ok";
            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }

        public synchronized void doWait() {
            new Thread(() -> {
                synchronized (this) {
                    notify();
                }
            }).start();

            try {
                Thread.sleep(1);
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ie) {
                instanceState = "error";
                throw new RuntimeException(ie);
            }
        }
    }

    private void throwException() {
        throw new RuntimeException();
    }

    private int id(int value) {
        return value;
    }

    private static class ClassWithStaticField {
        public final static String CONST1 = "FIRST";
        public final static String CONST2 = "SECOND";

        public static String foo(boolean value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value ? CONST1 : CONST2).append(" ");
            return sb.toString();
        }
    }

    static class SuperClass {
        static final Integer ONE = Integer.valueOf(1);

        private Integer value;

        public SuperClass(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    static class SubClass extends SuperClass {
        SubClass() {
            super(ONE);
        }
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void indirectDefaultMethod() {
        StringBuilder sb = new StringBuilder();
        for (FirstPath o : new FirstPath[] { new PathJoint(), new FirstPathOptimizationPrevention() }) {
            sb.append(o.foo()).append(";");
        }
        assertEquals("SecondPath.foo;FirstPath.foo;", sb.toString());
    }

    @Test
    @SkipPlatform({TestPlatform.C, TestPlatform.WEBASSEMBLY, TestPlatform.WASI})
    public void indirectDefaultMethodSubclass() {
        StringBuilder sb = new StringBuilder();
        for (FirstPath o : new FirstPath[] { new PathJointSubclass(), new FirstPathOptimizationPrevention() }) {
            sb.append(o.foo()).append(";");
        }
        assertEquals("SecondPath.foo;FirstPath.foo;", sb.toString());
    }

    interface FirstPath {
        default String foo() {
            return "FirstPath.foo";
        }
    }

    interface SecondPath extends FirstPath {
        @Override
        default String foo() {
            return "SecondPath.foo";
        }
    }

    class PathJoint implements FirstPath, SecondPath {
    }

    class PathJointSubclass extends PathJoint implements FirstPath {
    }

    class FirstPathOptimizationPrevention implements FirstPath {
        // Used to ensure that the implementation of FirstPath.foo() is not optimized away by TeaVM.
    }

    @Test
    public void cloneArray() {
        String[] a = new String[] { "foo" };
        String[] b = a.clone();
        assertNotSame(a, b);
        a[0] = "bar";
        assertEquals("foo", b[0]);
    }

    @Test
    public void stringConstantsInBaseClass() {
        new DerivedClassWithConstantFields();
    }

    static class BaseClassWithConstantFields {
        public final String foo = "bar";
    }

    static class DerivedClassWithConstantFields extends BaseClassWithConstantFields {
    }

    interface ScriptExecutionWrapper {
        Object wrap(Supplier<Object> execution);
    }

    @Test
    public void uncaughtExceptionRethrown() {
        boolean exceptionCaught = false;
        try {
            try {
                throw new NullPointerException("ok");
            } catch (IndexOutOfBoundsException e) {
                fail("Should not get there");
            }
            fail("Should not get there");
        } catch (NullPointerException e) {
            assertEquals("ok", e.getMessage());
            exceptionCaught = true;
        }
        assertTrue("Exception was not caught", exceptionCaught);
    }

    @Test
    @SkipPlatform({TestPlatform.WASI, TestPlatform.WEBASSEMBLY_GC})
    public void arrayMonitor() throws InterruptedException {
        int[] array = { 1, 2, 3 };
        synchronized (array) {
            array.wait(1);
        }
    }

    @Test
    public void castMultiArray() {
        Object o = new String[0][0];
        assertEquals(0, ((String[][]) o).length);
        o = new String[0][];
        assertEquals(0, ((String[][]) o).length);
        o = new int[0][0];
        assertEquals(0, ((int[][]) o).length);
        o = new int[0][];
        assertEquals(0, ((int[][]) o).length);
    }

    @Test
    public void precedence() {
        float a = count(3);
        float b = count(7);
        float c = 5;
        assertEquals(1, a * b % c, 0.1f);
        assertEquals(6, a * (b % c), 0.1f);
    }

    private int count(int value) {
        int result = 0;
        for (int i = 0; i < value; ++i) {
            result += 1;
        }
        return result;
    }

    @Test
    public void virtualCallWithPrivateMethods() {
        assertEquals("ap", callA(new B()));
    }

    @Test
    public void virtualTableCase1() {
        interface I {
            String f();
        }
        interface J extends I {
            String g();
        }
        class A {
        }
        class C extends A implements J {
            @Override
            public String f() {
                return "C.f";
            }
            @Override
            public String g() {
                return "C.g";
            }
        }
        class D implements I {
            @Override
            public String f() {
                return "D.f";
            }
        }
        
        var list = List.<I>of(new C(), new D());
        var sb = new StringBuilder();
        for (var item : list) {
            sb.append(item.f()).append(";");
        }
        
        assertEquals("C.f;D.f;", sb.toString());
    }

    @Test
    public void typeInferenceForArrayMerge() {
        int[][] a = falseBoolean() ? null : array();
        assertEquals(23, a[0][0]);
    }

    private boolean falseBoolean() {
        return false;
    }

    private int[][] array() {
        return new int[][] { { 23 } };
    }

    private static String callA(A a) {
        return a.a();
    }

    static class A {
        String a() {
            return "a" + p();
        }

        private String p() {
            return "p";
        }
    }

    static class B extends A {
        private String p() {
            return "q";
        }
    }
}