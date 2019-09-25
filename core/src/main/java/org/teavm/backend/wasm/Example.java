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
package org.teavm.backend.wasm;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.model.ValueType;

public final class Example {
    private Example() {
    }

    public static void main(String[] args) {
        testFibonacci();
        testClasses();
        testVirtualCall();
        testInstanceOf();
        testPrimitiveArray();
        testLazyInitialization();
        testHashCode();
        testArrayList();
        testArrayCopy();
        testArrayIsObject();
        testIsAssignableFrom();
        testExceptions();
        testArrayReflection();
        testBigInteger();
        testGC();
    }

    private static void testBigInteger() {
        System.out.println("Running BigInteger benchmark");
        BigInteger result = BigInteger.ONE;
        for (int j = 0; j < 100; ++j) {
            long start = System.currentTimeMillis();

            for (int k = 0; k < 5000; ++k) {
                BigInteger a = BigInteger.ZERO;
                BigInteger b = BigInteger.ONE;
                for (int i = 0; i < 1000; ++i) {
                    BigInteger c = a.add(b);
                    a = b;
                    b = c;
                }
                result = a;
            }

            long end = System.currentTimeMillis();

            System.out.println("Operation took " + (end - start) + " milliseconds");
        }
        System.out.println(result.toString());
    }

    private static void testFibonacci() {
        int a = 0;
        int b = 1;
        System.out.println("Fibonacci numbers:");
        for (int i = 0; i < 30; ++i) {
            int c = a + b;
            a = b;
            b = c;
            System.out.println(String.valueOf(a));
        }
    }

    private static void testClasses() {
        System.out.println("A(2) + A(3) = " + (new A(2).getValue() + new A(3).getValue()));
    }

    private static void testVirtualCall() {
        for (int i = 0; i < 4; ++i) {
            System.out.println("instance(" + i + ") = " + instance(i).foo());
        }

        Base[] array = { new Derived1(), new Derived2() };
        System.out.println("array.length = " + array.length);
        for (Base elem : array) {
            System.out.println("array[i] = " + elem.foo());
        }
    }

    private static void testInstanceOf() {
        System.out.println("Derived2 instanceof Base = " + (new Derived2() instanceof Base));
        System.out.println("Derived3 instanceof Base = " + (new Derived3() instanceof Base));
        System.out.println("Derived2 instanceof Derived1 = " + ((Object) new Derived2() instanceof Derived1));
        System.out.println("Derived2 instanceof A = " + ((Object) new Derived2() instanceof A));
        System.out.println("A instanceof Base = " + (new A(23) instanceof Base));
    }

    private static void testPrimitiveArray() {
        byte[] bytes = { 5, 6, 10, 15 };
        for (byte bt : bytes) {
            System.out.println("bytes[i] = " + bt);
        }
    }

    private static void testLazyInitialization() {
        Initialized.foo();
        Initialized.foo();
        Initialized.foo();
    }

    private static void testHashCode() {
        Object o = new Object();
        System.out.println("hashCode1 = " + o.hashCode());
        System.out.println("hashCode1 = " + o.hashCode());
        System.out.println("hashCode2 = " + new Object().hashCode());
        System.out.println("hashCode3 = " + new Object().hashCode());
    }

    private static void testArrayList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(333, 444, 555));
        list.add(1234);
        list.remove((Integer) 444);

        for (int item : list) {
            System.out.println("list[i] = " + item);
        }
    }

    private static void testArrayCopy() {
        byte[] array = new byte[100];
        byte[] negatives = new byte[100];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) i;
            negatives[i] = (byte) (-i - 1);
        }
        System.arraycopy(negatives, 0, array, 4, 12);
        System.arraycopy(negatives, 1, array, 21, 12);
        System.arraycopy(negatives, 2, array, 35, 12);
        System.arraycopy(negatives, 1, array, 8, 3);
        System.arraycopy(array, 50, array, 54, 12);
        StringBuilder sb = new StringBuilder("arrayCopy result:");
        for (int i = 0; i < array.length; ++i) {
            sb.append(" ").append(array[i]);
        }
        System.out.println(sb.toString());
    }

    private static void testArrayIsObject() {
        byte[] array = new byte[] { 1, 2, 3 };
        byte[] copy = array.clone();
        System.out.println("array.hashCode() = " + array.hashCode());
        System.out.println("copy.hashCode() = " + copy.hashCode());
        System.out.println("array.equals(array) = " + array.equals(array));
        System.out.println("array.equals(copy) = " + array.equals(copy));
    }

    private static void testIsAssignableFrom() {
        System.out.println("Object.isAssignableFrom(byte[]) = "
                + Object.class.isAssignableFrom(new byte[0].getClass()));
        System.out.println("Object[].isAssignableFrom(Integer[]) = "
                + Object[].class.isAssignableFrom(new Integer[0].getClass()));
        System.out.println("Object[].isAssignableFrom(Integer) = "
                + Object[].class.isAssignableFrom(Integer.class));
        System.out.println("byte[].isAssignableFrom(Object) = "
                + byte[].class.isAssignableFrom(ValueType.Object.class));
        System.out.println("Base.isAssignableFrom(Derived1) = "
                + Base.class.isAssignableFrom(Derived1.class));
        System.out.println("Base.isAssignableFrom(A) = "
                + Base.class.isAssignableFrom(A.class));
    }

    private static void testGC() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 4000000; ++i) {
            list.add(i);
            if (list.size() == 1000) {
                list = new ArrayList<>();
            }
        }
        System.out.println("GC complete");
    }

    private static void testExceptions() {
        try {
            throwsException();
        } catch (IllegalStateException e) {
            System.out.println("Caught 1: " + e.getMessage());
            e.printStackTrace();
        }

        int x = 0;
        try {
            x = 1;
            doesNotThrow();
            x = 2;
            throwsException();
            x = 3;
        } catch (IllegalStateException e) {
            System.out.println("Caught 2: " + e.getMessage());
            System.out.println("x = " + x);
        }

        try {
            throwsWithFinally();
        } catch (IllegalStateException e) {
            System.out.println("Caught 3: " + e.getMessage());
        }

        Object[] objects = { "a", null };
        for (Object o : objects) {
            try {
                System.out.println(o.toString());
            } catch (RuntimeException e) {
                System.out.println("Caught NPE");
                e.printStackTrace();
            }
        }
    }

    private static void testArrayReflection() {
        Object[] arrays = new Object[] {
                new int[] { 23, 42 },
                new byte[] { (byte) 1, (byte) 2, (byte) 3 },
                new String[] { "foo", "bar" },
        };

        for (Object array : arrays) {
            int sz = Array.getLength(array);
            System.out.println("Array type: " + array.getClass().getName() + ", length " + sz);
            for (int i = 0; i < sz; ++i) {
                Object item = Array.get(array, i);
                System.out.println("  [" + i + "] = " + item + ": " + item.getClass().getName());
            }
        }
    }

    private static void doesNotThrow() {
    }

    private static void throwsWithFinally() {
        try {
            throwsException();
        } finally {
            System.out.println("Finally reached");
        }
    }

    private static void throwsException() {
        throw new IllegalStateException("exception message");
    }

    private static Base instance(int index) {
        switch (index) {
            case 0:
                return new Derived1();
            case 1:
                return new Derived2();
            case 2:
                return new Derived3();
            default:
                return new Derived4();
        }
    }

    private static class A {
        private int value;

        public A(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    interface Base {
        int foo();
    }

    static class Derived1 implements Base {
        @Override
        public int foo() {
            return 234;
        }
    }

    static class Derived2 implements Base {
        @Override
        public int foo() {
            return 345;
        }
    }

    static class Derived3 extends Derived2 {
    }

    static class Derived4 extends Derived1 {
        @Override
        public int foo() {
            return 123;
        }
    }

    static class Initialized {
        static {
            System.out.println("Initialized.<clinit>()");
        }

        public static void foo() {
            System.out.println("Initialized.foo()");
        }
    }
}
