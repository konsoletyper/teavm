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

public final class Example {
    private Example() {
    }

    public static void main(String[] args) {
        int a = 0;
        int b = 1;
        println("Fibonacci numbers:");
        for (int i = 0; i < 30; ++i) {
            int c = a + b;
            a = b;
            b = c;
            println(String.valueOf(a));
        }

        println("A(2) + A(3) = " + (new A(2).getValue() + new A(3).getValue()));

        for (int i = 0; i < 4; ++i) {
            println("instance(" + i + ") = " + instance(i).foo());
        }

        Base[] array = { new Derived1(), new Derived2() };
        println("array.length = " + array.length);
        for (Base elem : array) {
            println("array[i] = " + elem.foo());
        }

        println("Derived2 instanceof Base = " + (new Derived2() instanceof Base));
        println("Derived3 instanceof Base = " + (new Derived3() instanceof Base));
        println("Derived2 instanceof Derived1 = " + ((Object) new Derived2() instanceof Derived1));
        println("Derived2 instanceof A = " + ((Object) new Derived2() instanceof A));
        println("A instanceof Base = " + (new A(23) instanceof Base));

        byte[] bytes = { 5, 6, 10, 15 };
        for (byte bt : bytes) {
            println("bytes[i] = " + bt);
        }

        Initialized.foo();
        Initialized.foo();
        Initialized.foo();

        Object o = new Object();
        println("hashCode1 = " + o.hashCode());
        println("hashCode1 = " + o.hashCode());
        println("hashCode2 = " + new Object().hashCode());
        println("hashCode3 = " + new Object().hashCode());

        List<Integer> list = new ArrayList<>(Arrays.asList(333, 444, 555));
        list.add(1234);
        list.remove((Integer) 444);

        for (int item : list) {
            println("list[i] = " + item);
        }
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
