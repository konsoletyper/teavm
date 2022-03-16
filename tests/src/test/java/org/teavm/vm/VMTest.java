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
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
        try {
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            // do nothing
        }
    }

    @Test
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
    public void asyncClinitField() {
        assertEquals("ok", AsyncClinitClass.state);
    }

    @Test
    public void asyncClinitInstance() {
        AsyncClinitClass acl = new AsyncClinitClass();
        assertEquals("ok", AsyncClinitClass.state);
        assertEquals("ok", acl.instanceState);
    }

    @Test
    public void asyncWait() {
        AsyncClinitClass acl = new AsyncClinitClass();
        acl.doWait();
        assertEquals("ok", acl.instanceState);
    }

    @Test
    @SkipJVM
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
        static final Integer ONE = new Integer(1);

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
    public void indirectDefaultMethod() {
        StringBuilder sb = new StringBuilder();
        for (FirstPath o : new FirstPath[] { new PathJoint(), new FirstPathOptimizationPrevention() }) {
            sb.append(o.foo()).append(";");
        }
        assertEquals("SecondPath.foo;FirstPath.foo;", sb.toString());
    }

    @Test
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
}