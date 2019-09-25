/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.dependency;

public class DependencyTestData {
    private DependencyTestData() {
    }

    public static void virtualCall() {
        MetaAssertions.assertTypes(getI(0).foo(), String.class, Integer.class, Class.class);
    }

    public static void instanceOf() {
        MetaAssertions.assertTypes((String) getI(0).foo(), String.class);
    }

    public static void catchException() throws Exception {
        try {
            throw createException(0);
        } catch (IndexOutOfBoundsException e) {
            MetaAssertions.assertTypes(e, IndexOutOfBoundsException.class);
        } catch (RuntimeException e) {
            MetaAssertions.assertTypes(e, UnsupportedOperationException.class, IllegalArgumentException.class);
        }
    }

    public static void propagateException() {
        try {
            catchException();
        } catch (Throwable e) {
            MetaAssertions.assertTypes(e, Exception.class);
        }
    }

    public static void arrays() {
        Object[] array = { new String("123"), new Integer(123), String.class };
        MetaAssertions.assertTypes(array[0], String.class, Integer.class, Class.class);
    }

    public static void arraysPassed() {
        Object[] array = new Object[3];
        fillArray(array);
        MetaAssertions.assertTypes(array[0], String.class, Integer.class, Class.class);

        Object[] array2 = new Object[3];
        staticArrayField = array2;
        fillStaticArray();
        MetaAssertions.assertTypes(array2[0], Long.class, RuntimeException.class);
    }

    public static void arraysRetrieved() {
        Object[] array = createArray();
        MetaAssertions.assertTypes(array[0], String.class, Integer.class, Class.class);

        staticArrayField = new Object[3];
        Object[] array2 = staticArrayField;
        fillStaticArray();
        MetaAssertions.assertTypes(array2[0], Long.class, RuntimeException.class);
    }

    static Object[] staticArrayField;

    private static Object[] createArray() {
        Object[] array = new Object[3];
        fillArray(array);
        return array;
    }

    private static void fillArray(Object[] array) {
        array[0] = "123";
        array[1] = 123;
        array[2] = String.class;
    }

    private static void fillStaticArray() {
        staticArrayField[0] = 42L;
        staticArrayField[0] = new RuntimeException();
    }

    private static I getI(int index) {
        switch (index) {
            case 0:
                return new A();
            case 1:
                return new B();
            default:
                return new C();
        }
    }

    private static Exception createException(int index) {
        switch (index) {
            case 0:
                throw new IndexOutOfBoundsException();
            case 1:
                throw new UnsupportedOperationException();
            case 2:
                throw new IllegalArgumentException();
            default:
                return new Exception();
        }
    }

    interface I {
        Object foo();
    }

    static class A implements I {
        @Override
        public Object foo() {
            return "123";
        }
    }

    static class B implements I {
        @Override
        public Object foo() {
            return Object.class;
        }
    }

    static class C implements I {
        @Override
        public Object foo() {
            return 123;
        }
    }
}
