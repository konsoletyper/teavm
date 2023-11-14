/*
 *  Copyright 2023 ihromant.
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
import static org.junit.Assert.fail;
import org.junit.runner.RunWith;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTestRunner;
import org.testng.annotations.Test;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
public class SwitchTest {
    private static int switchWithLogic(Object o) {
        return switch (o) {
            case null -> -1;
            case A a when (a.af & 31) == 5 -> 0;
            case A a -> 1;
            case B b -> {
                b.bbf = 21;
                yield b.f.length();
            }
            case C c -> c.cf.af;
            case D(byte c, short d) when ((int) d & 31) == 31 -> c;
            case D(byte c, short d) -> d;
            case TestEnum te -> te.ordinal();
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    public void genericSwitch() {
        assertEquals(-1, switchWithLogic(null));
        A a = new A();
        assertEquals(1, switchWithLogic(a));
        a.af = 5;
        assertEquals(0, switchWithLogic(a));
        B b = new B();
        b.f = "abc";
        assertEquals(3, switchWithLogic(b));
        assertEquals(21, b.bbf);
        C c = new C();
        c.cf = a;
        assertEquals(5, switchWithLogic(c));
        assertEquals(Byte.MIN_VALUE, switchWithLogic(new D(Byte.MIN_VALUE, Short.MAX_VALUE)));
        assertEquals(Short.MIN_VALUE, switchWithLogic(new D(Byte.MIN_VALUE, Short.MIN_VALUE)));
        assertEquals(4, switchWithLogic(TestEnum.E));
        try {
            switchWithLogic(new Object());
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private static int enumSwitchWithLogic(TestEnum o) {
        return switch (o) {
            case A, B -> 1;
            case TestEnum e when e.ordinal() % 3 == 0 -> 3;
            case C, D, E, F -> 2;
        };
    }

    @Test
    public void enumSwitch() {
        assertEquals(1, enumSwitchWithLogic(TestEnum.A));
        assertEquals(2, enumSwitchWithLogic(TestEnum.C));
        assertEquals(3, enumSwitchWithLogic(TestEnum.D));
        assertEquals(2, enumSwitchWithLogic(TestEnum.F));
    }

    private static int integerSwitchWithLogic(Integer o) {
        return switch (o) {
            case 23 -> 1;
            case Integer i when i < 10 -> 2;
            case 42 -> 3;
            default -> 4;
        };
    }

    @Test
    public void integerSwitch() {
        assertEquals(1, integerSwitchWithLogic(23));
        assertEquals(3, integerSwitchWithLogic(42));
        assertEquals(4, integerSwitchWithLogic(11));
        assertEquals(2, integerSwitchWithLogic(5));
    }

    private static int characterSwitchWithLogic(Character c) {
        return switch (c) {
            case Character ch when ch >= 'a' && ch <= 'z' -> 5;
            case 'R' -> 1;
            case 'T' -> 2;
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    public void characterSwitch() {
        assertEquals(5, characterSwitchWithLogic('a'));
        assertEquals(1, characterSwitchWithLogic('R'));
        assertEquals(2, characterSwitchWithLogic('T'));
        try {
            characterSwitchWithLogic('A');
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private static int stringSwitchWithLogic(String s) {
        return switch (s) {
            case String str when str.length() < 3 -> 0;
            case "abc" -> 1;
            default -> 2;
        };
    }

    @Test
    public void stringSwitch() {
        assertEquals(0, stringSwitchWithLogic(""));
        assertEquals(1, stringSwitchWithLogic("abc"));
        assertEquals(2, stringSwitchWithLogic("bcd"));
    }

    private static int switchWithHierarchy(Object o) {
        return switch (o) {
            case Superclass s when s.x == 23 -> 1;
            case SubclassA a -> 2;
            case Superclass s when s.x == 24 -> 3;
            case SubclassB b -> 4;
            default -> 5;
        };
    }

    @Test
    public void hierarchySwitch() {
        assertEquals(1, switchWithHierarchy(new SubclassA(23)));
        assertEquals(2, switchWithHierarchy(new SubclassA(24)));
        assertEquals(2, switchWithHierarchy(new SubclassA(1)));
        assertEquals(1, switchWithHierarchy(new SubclassB(23)));
        assertEquals(3, switchWithHierarchy(new SubclassB(24)));
        assertEquals(4, switchWithHierarchy(new SubclassB(1)));
        assertEquals(5, switchWithHierarchy("foo"));
        assertEquals(5, switchWithHierarchy(new Superclass(1)));
        assertEquals(1, switchWithHierarchy(new Superclass(23)));
    }

    private static class A {
        private int af;
    }

    private static class B {
        private String f;
        private int bbf;
    }

    private static class C {
        private A cf;
        private long ccf;
        private boolean cccf;
    }

    private record D(byte a, short b) {

    }

    private enum TestEnum {
        A, B, C, D, E, F
    }

    private static class Superclass {
        final int x;

        public Superclass(int x) {
            this.x = x;
        }
    }

    private static class SubclassA extends Superclass {
        public SubclassA(int x) {
            super(x);
        }
    }

    private static class SubclassB extends Superclass {
        public SubclassB(int x) {
            super(x);
        }
    }
}
