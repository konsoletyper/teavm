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
        testGC();
    }

    private static void testFibonacci() {
        int a = 0;
        int b = 1;
        println("Fibonacci numbers:");
        for (int i = 0; i < 30; ++i) {
            int c = a + b;
            a = b;
            b = c;
            println(String.valueOf(a));
        }
    }

    private static void testClasses() {
        println("A(2) + A(3) = " + (new A(2).getValue() + new A(3).getValue()));
    }

    private static void testVirtualCall() {
        for (int i = 0; i < 4; ++i) {
            println("instance(" + i + ") = " + instance(i).foo());
        }

        Base[] array = { new Derived1(), new Derived2() };
        println("array.length = " + array.length);
        for (Base elem : array) {
            println("array[i] = " + elem.foo());
        }
    }

    private static void testInstanceOf() {
        println("Derived2 instanceof Base = " + (new Derived2() instanceof Base));
        println("Derived3 instanceof Base = " + (new Derived3() instanceof Base));
        println("Derived2 instanceof Derived1 = " + ((Object) new Derived2() instanceof Derived1));
        println("Derived2 instanceof A = " + ((Object) new Derived2() instanceof A));
        println("A instanceof Base = " + (new A(23) instanceof Base));
    }

    private static void testPrimitiveArray() {
        byte[] bytes = { 5, 6, 10, 15 };
        for (byte bt : bytes) {
            println("bytes[i] = " + bt);
        }
    }

    private static void testLazyInitialization() {
        Initialized.foo();
        Initialized.foo();
        Initialized.foo();
    }

    private static void testHashCode() {
        Object o = new Object();
        println("hashCode1 = " + o.hashCode());
        println("hashCode1 = " + o.hashCode());
        println("hashCode2 = " + new Object().hashCode());
        println("hashCode3 = " + new Object().hashCode());
    }

    private static void testArrayList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(333, 444, 555));
        list.add(1234);
        list.remove((Integer) 444);

        for (int item : list) {
            println("list[i] = " + item);
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
        println(sb.toString());
    }

    private static void testArrayIsObject() {
        byte[] array = new byte[] { 1, 2, 3 };
        byte[] copy = array.clone();
        println("array.hashCode() = " + array.hashCode());
        println("copy.hashCode() = " + copy.hashCode());
        println("array.equals(array) = " + array.equals(array));
        println("array.equals(copy) = " + array.equals(copy));
    }

    private static void testIsAssignableFrom() {
        println("Object.isAssignableFrom(byte[]) = " + Object.class.isAssignableFrom(new byte[0].getClass()));
        println("Object[].isAssignableFrom(Integer[]) = " + Object[].class.isAssignableFrom(new Integer[0].getClass()));
        println("Object[].isAssignableFrom(Integer) = " + Object[].class.isAssignableFrom(Integer.class));
        println("byte[].isAssignableFrom(Object) = " + byte[].class.isAssignableFrom(ValueType.Object.class));
        println("Base.isAssignableFrom(Derived1) = " + Base.class.isAssignableFrom(Derived1.class));
        println("Base.isAssignableFrom(A) = " + Base.class.isAssignableFrom(A.class));
    }

    private static void testGC() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 100000; ++i) {
            list.add(i);
            if (list.size() == 1000) {
                list = new ArrayList<>();
            }
        }
        print("GC complete");
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
            println("Initialized.<clinit>()");
        }

        public static void foo() {
            println("Initialized.foo()");
        }
    }

    static void print(String value) {
        for (int i = 0; i < value.length(); ++i) {
            WasmRuntime.print(value.charAt(i));
        }
    }

    static void println(String value) {
        print(value);
        WasmRuntime.print('\n');
    }
}
