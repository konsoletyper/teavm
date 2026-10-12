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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTest;
import org.teavm.junit.TestPlatform;

@TeaVMTest
@EachTestCompiledSeparately
public class VMTest {
    @Test
    @OnlyPlatform(TestPlatform.JAVASCRIPT)
    @SkipJVM
    public void typedArrayUsed() {
        var multiarray = new byte[1][2];
        assertTrue(isTypedArray(multiarray[0]));
    }

    @JSBody(params = "data", script = "return data instanceof Int8Array;")
    private static native boolean isTypedArray(@JSByRef(optional = true) byte[] data);

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
    public void finallyWithEmptyTryBody() {
        Object result;
        try {
            result = methodWithFinally("ok");
        } catch (Throwable t) {
            fail("Exception not expected here");
            return;
        }
        assertEquals("ok", result);
    }

    private static Object methodWithFinally(Object g) {
        try {
            return g;
        } finally {
            System.out.println("Never mind");
        }
    }

    @Test
    public void breakLoopFromCatch() {
        var caught = false;
        while (true) {
            try {
                "".charAt(-1);
            } catch (IndexOutOfBoundsException e) {
                caught = true;
                break;
            }
        }
        assertTrue(caught);
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
    @SkipPlatform(TestPlatform.C)
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
    @SkipPlatform(TestPlatform.C)
    public void asyncClinitField() {
        assertEquals("ok", AsyncClinitClass.state);
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void asyncClinitInstance() {
        AsyncClinitClass acl = new AsyncClinitClass();
        assertEquals("ok", AsyncClinitClass.state);
        assertEquals("ok", acl.instanceState);
    }

    @Test
    @SkipPlatform(TestPlatform.C)
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
    public void clinitReadsState() {
        initCount = 23;
        assertEquals(23, ReadingStateInClinit.state);
    }
    
    private static int[] createArray() {
        if (PlatformDetector.isC()) {
            return new int[] { 1, 2 };
        } else {
            return doCreateArrayJS();
        }
    }

    @JSBody(script = "return [1, 2]")
    private static native int[] doCreateArrayJS();

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
        assertTrue(exceptionCaught, "Exception was not caught");
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
    public void typeInferenceForArrayMerge() {
        int[][] a = falseBoolean() ? null : array();
        assertEquals(23, a[0][0]);
    }

    @Test
    public void nanComparison() {
        var d = Double.NaN;
        assertFalse(d == 1);
        assertFalse(d > 1);
        assertFalse(d < 1);
        assertTrue(d != 1);
        assertFalse(d == Double.NaN);
        assertFalse(d > Double.NaN);
        assertFalse(d < Double.NaN);
        assertTrue(d != Double.NaN);
        
        d = doubleNaN();
        assertFalse(d == 1);
        assertFalse(d > 1);
        assertFalse(d < 1);
        assertTrue(d != 1);
        assertFalse(d == Double.NaN);
        assertFalse(d > Double.NaN);
        assertFalse(d < Double.NaN);
        assertTrue(d != Double.NaN);

        assertFalse(doubleNaN() == 1);
        assertFalse(doubleNaN() > 1);
        assertFalse(doubleNaN() < 1);
        assertTrue(doubleNaN() != 1);
        assertFalse(doubleNaN() == Double.NaN);
        assertFalse(doubleNaN() > Double.NaN);
        assertFalse(doubleNaN() < Double.NaN);
        assertTrue(doubleNaN() != Double.NaN);
    }
    
    @Test
    public void nanComparisonFloat() {
        var f = Float.NaN;
        assertFalse(f == 1);
        assertFalse(f > 1);
        assertFalse(f < 1);
        assertTrue(f != 1);
        assertFalse(f == Float.NaN);
        assertFalse(f > Float.NaN);
        assertFalse(f < Float.NaN);
        assertTrue(f != Float.NaN);

        f = floatNaN();
        assertFalse(f == 1);
        assertFalse(f > 1);
        assertFalse(f < 1);
        assertTrue(f != 1);
        assertFalse(f == Float.NaN);
        assertFalse(f > Float.NaN);
        assertFalse(f < Float.NaN);
        assertTrue(f != Float.NaN);

        assertFalse(floatNaN() == 1);
        assertFalse(floatNaN() > 1);
        assertFalse(floatNaN() < 1);
        assertTrue(floatNaN() != 1);
        assertFalse(floatNaN() == Float.NaN);
        assertFalse(floatNaN() > Float.NaN);
        assertFalse(floatNaN() < Float.NaN);
        assertTrue(floatNaN() != Float.NaN);
    }
    
    private double doubleNaN() {
        return Double.NaN;
    }

    private float floatNaN() {
        return Float.NaN;
    }

    private boolean falseBoolean() {
        return false;
    }

    private int[][] array() {
        return new int[][] { { 23 } };
    }

    @Test
    public void variableJoiningPrimitiveAndReferenceArrays() {
        // the shape of scala.Array$.copyOf (Scala 2.13): one variable receives
        // either a primitive array or a reference array depending on the branch
        assertEquals("copied 3", copyAs(new String[] { "a", "b" }, 3));
        assertEquals("copied 3", copyAs(new int[] { 1, 2 }, 3));
    }

    private static String copyAs(Object original, int newLength) {
        Object copy;
        if (original instanceof int[]) {
            copy = Arrays.copyOf((int[]) original, newLength);
        } else {
            copy = Arrays.copyOf((Object[]) original, newLength);
        }
        if (copy == original) {
            return "same";
        }
        return "copied " + java.lang.reflect.Array.getLength(copy);
    }

    @Test
    public void exceptionHandlerInitializesClass() {
        assertEquals("B", classInitInHandler(true));
        assertEquals("A", classInitInHandler(false));
    }

    private static String classInitInHandler(boolean fail) {
        try {
            failIf(fail);
            return InitInHandler.A.name();
        } catch (IllegalStateException e) {
            return InitInHandler.B.name();
        }
    }

    private static void failIf(boolean fail) {
        if (fail) {
            throw new IllegalStateException();
        }
    }

    enum InitInHandler {
        A,
        B
    }

    @Test
    public void interfaceCallDoesNotInitializeInterface() {
        assertEquals("ok", readInterfaceConstant(new InterfaceWithConstantA()));
        assertEquals("ok", readInterfaceConstant(new InterfaceWithConstantB()));
    }

    private static String readInterfaceConstant(InterfaceWithConstant obj) {
        obj.foo();
        return InterfaceWithConstant.VALUE.toString();
    }

    interface InterfaceWithConstant {
        Object VALUE = new StringBuilder("ok");

        void foo();
    }

    static class InterfaceWithConstantA implements InterfaceWithConstant {
        @Override
        public void foo() {
        }
    }

    static class InterfaceWithConstantB implements InterfaceWithConstant {
        @Override
        public void foo() {
        }
    }

    @Test
    public void exceptionFromClassInitializerReachesHandler() {
        try {
            ThrowingInitializer.value();
            fail("Initializer did not throw");
        } catch (Throwable e) {
            // The JVM wraps the exception in ExceptionInInitializerError
            var cause = e instanceof ExceptionInInitializerError ? e.getCause() : e;
            assertEquals("thrown by initializer", ((InitializerException) cause).describe());
        }
    }

    static class ThrowingInitializer {
        static final String VALUE;

        static {
            throwInitializerException();
            VALUE = "initialized";
        }

        static String value() {
            return VALUE;
        }
    }

    private static void throwInitializerException() {
        throw new InitializerException();
    }

    // describe() is called only in the handler, so it exists in the output only if the exception type
    // reaches the handler
    static class InitializerException extends RuntimeException {
        String describe() {
            return "thrown by initializer";
        }
    }

    @Test
    public void failedClassInitializerIsNotRunAgain() {
        try {
            FailingInitializer.value();
            fail("Initializer did not throw");
        } catch (ExceptionInInitializerError e) {
            assertEquals(IllegalStateException.class, e.getCause().getClass());
        }
        try {
            FailingInitializer.value();
            fail("Class was initialized after its initializer threw");
        } catch (NoClassDefFoundError e) {
            // expected
        }
    }

    @Test
    public void errorFromClassInitializerIsNotWrapped() {
        try {
            FailingInitializerWithError.value();
            fail("Initializer did not throw");
        } catch (InitializerTestError e) {
            // expected
        }
        try {
            FailingInitializerWithError.value();
            fail("Class was initialized after its initializer threw");
        } catch (NoClassDefFoundError e) {
            // expected
        }
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void failedAsyncClassInitializerIsNotRunAgain() {
        try {
            FailingAsyncInitializer.value();
            fail("Initializer did not throw");
        } catch (ExceptionInInitializerError e) {
            assertEquals(IllegalStateException.class, e.getCause().getClass());
        }
        try {
            FailingAsyncInitializer.value();
            fail("Class was initialized after its initializer threw");
        } catch (NoClassDefFoundError e) {
            // expected
        }
    }

    static class FailingInitializer {
        static final String VALUE;

        static {
            failIf(true);
            VALUE = "initialized";
        }

        static String value() {
            return VALUE;
        }
    }

    static class FailingInitializerWithError {
        static final String VALUE;

        static {
            throwInitializerTestError();
            VALUE = "initialized";
        }

        static String value() {
            return VALUE;
        }
    }

    static class FailingAsyncInitializer {
        static final String VALUE;

        static {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            failIf(true);
            VALUE = "initialized";
        }

        static String value() {
            return VALUE;
        }
    }

    static class InitializerTestError extends Error {
    }

    private static void throwInitializerTestError() {
        throw new InitializerTestError();
    }

    @Test
    public void errorsFromFailedClassInitializerReachHandlers() {
        try {
            FailingInitializerForHandlers.value();
            fail("Initializer did not throw");
        } catch (ExceptionInInitializerError e) {
            assertTrue(e.getException() instanceof IllegalStateException);
        }
        try {
            FailingInitializerForHandlers.value();
            fail("Class was initialized after its initializer threw");
        } catch (Error e) {
            assertTrue(e instanceof NoClassDefFoundError);
            // The JDK reports the first failure as the cause; TeaVM gives no cause
            assertTrue(e.getCause() == null || e.getCause() instanceof ExceptionInInitializerError);
        }
    }

    // The JVM runs every test of this class in one class loader, so this test needs a failing class of its own
    static class FailingInitializerForHandlers {
        static final String VALUE;

        static {
            failIf(true);
            VALUE = "initialized";
        }

        static String value() {
            return VALUE;
        }
    }

    @Test
    public void exceptionHandlerSeesParameterBeforeNullCheck() {
        var key = newKey();
        assertEquals("attached built", attachOrCancel(new WorkingBuilder(), key));
        assertTrue(key.valid);
        key = newKey();
        assertEquals("cancelled: build failed", attachOrCancel(new FailingBuilder(), key));
        assertFalse(key.valid);
    }

    @Test
    public void exceptionHandlerSeesLocalBeforeNullCheck() {
        assertEquals("attached built", assignAndAttach(new WorkingBuilder()));
        assertEquals("key assigned: build failed", assignAndAttach(new FailingBuilder()));
    }

    private static String attachOrCancel(Builder builder, Key key) {
        try {
            Object built = builder.build(key);
            key.attach(built);
            return "attached " + built;
        } catch (IllegalStateException e) {
            key.cancel();
            return "cancelled: " + e.getMessage();
        }
    }

    private static String assignAndAttach(Builder builder) {
        Key key = null;
        try {
            key = newKey();
            Object built = builder.build(key);
            key.attach(built);
            return "attached " + built;
        } catch (IllegalStateException e) {
            return (key != null ? "key assigned: " : "key not assigned: ") + e.getMessage();
        }
    }

    private static final List<Key> KEYS = new ArrayList<>();

    // The compiler cannot prove that the result is not null, so each call on it gets a null check
    private static Key newKey() {
        KEYS.add(new Key());
        return KEYS.get(KEYS.size() - 1);
    }

    interface Builder {
        Object build(Key key);
    }

    static class WorkingBuilder implements Builder {
        @Override
        public Object build(Key key) {
            return "built";
        }
    }

    static class FailingBuilder implements Builder {
        @Override
        public Object build(Key key) {
            throw new IllegalStateException("build failed");
        }
    }

    static class Key {
        boolean valid = true;
        Object attachment;

        void attach(Object attachment) {
            this.attachment = attachment;
        }

        void cancel() {
            valid = false;
        }
    }
}
