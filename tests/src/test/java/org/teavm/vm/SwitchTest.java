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
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.testng.annotations.Test;

@RunWith(TeaVMTestRunner.class)
public class SwitchTest {
    @Test
    public void testSwitch() {
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
        assertEquals(Short.MAX_VALUE, switchWithLogic(new D(Byte.MIN_VALUE, Short.MAX_VALUE)));
        assertEquals(4, switchWithLogic(TestEnum.E));
    }

    private int switchWithLogic(Object o) {
        return switch (o) {
           // case null -> -1;
            case A a when (a.af & 31) == 5 -> 0;
            case A a -> 1;
            case B b -> {
                b.bbf = 21;
                yield b.f.length();
            }
            case C c -> c.cf.af;
//            case D(byte c, short d) when ((int) d & 31) == 31 -> d;
            case D(byte c, short d) -> d;
            case TestEnum te -> te.ordinal();
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    public void testEnumSwitch() {
        assertEquals(1, enumSwitchWithLogic(TestEnum.A));
        assertEquals(2, enumSwitchWithLogic(TestEnum.C));
        assertEquals(3, enumSwitchWithLogic(TestEnum.F));
    }

    private int enumSwitchWithLogic(TestEnum o) {
        return switch (o) {
            case A, B -> 1;
            case C, D, E -> 2;
            case F -> 3;
        };
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
}
