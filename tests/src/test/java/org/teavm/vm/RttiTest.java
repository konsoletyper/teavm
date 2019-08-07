/*
 *  Copyright 2019 Alexey Andreev.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class RttiTest {
    @Test
    public void instanceOfInterface() {
        checkImplements(new A(), true, false, false);
        checkImplements(new B(), false, true, true);
        checkImplements(new C(), false, true, false);
        checkImplements(new D(), false, true, true);
        checkImplements(new E(), true, false, false);
        checkImplements(new F(), false, true, true);
        checkImplements(new G(), true, true, false);
    }

    @Test
    public void objectIsNotSupertypeOfPrimitive() {
        assertFalse(Object.class.isAssignableFrom(int.class));
    }

    @Test
    public void instanceOfObjectArray() {
        List<Object> list = new ArrayList<>();
        list.add("123");
        list.add(new Object[0]);
        list.add(new String[0]);
        list.add(new int[0]);
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            sb.append(item instanceof Object[] ? 't' : 'f');
        }
        assertEquals("fttf", sb.toString());
    }

    private void checkImplements(Object o, boolean i, boolean j, boolean k) {
        assertTrue(predicate(o, i, "I"), !i ^ o instanceof I);
        assertTrue(predicate(o, j, "J"), !j ^ o instanceof J);
        assertTrue(predicate(o, k, "K"), !k ^ o instanceof K);
    }

    private String predicate(Object o, boolean b, String name) {
        return o.getClass().getName() + " should" + (b ? " not" : "") + " implement " + name;
    }

    static class A implements I {
    }

    static class B implements K {
    }

    static class C implements J {
    }

    static class D extends C implements K {
    }

    static class E extends A implements I {
    }

    static class F extends B implements J {
    }

    static class G extends C implements I {
    }

    interface I {
    }

    interface J {
    }

    interface K extends J {
    }
}
