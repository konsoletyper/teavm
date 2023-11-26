/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.jso.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@EachTestCompiledSeparately
public class JSWrapperTest {
    private List<Object> list = new ArrayList<>();

    private Object field1;

    @Test
    public void simple() {
        list.add(JSNumber.valueOf(23));
        list.add(JSString.valueOf("q"));
        list.add(JSString.valueOf("q"));
        list.add("q");
        list.add("w");

        assertEquals("23", list.get(0).toString());
        assertEquals("q", list.get(1).toString());
        assertEquals(list.get(1), list.get(2));
        assertNotEquals(list.get(0), list.get(2));
        assertNotEquals(list.get(1), list.get(3));

        assertEquals(23, ((JSNumber) list.get(0)).intValue());
        assertEquals("q", ((JSString) list.get(1)).stringValue());

        try {
            assertEquals("q", ((JSString) list.get(3)).stringValue());
            fail("Expected exception not thrown");
        } catch (ClassCastException e) {
            // ok
        }
    }

    @Test
    public void testHashCode() {
        list.add(JSNumber.valueOf(23));

        var o1 = JSObjects.create();
        list.add(o1);
        list.add(o1);

        var o2 = JSObjects.create();
        list.add(o2);

        assertEquals(23, list.get(0).hashCode());
        assertEquals(list.get(1).hashCode(), list.get(2).hashCode());
        assertNotEquals(list.get(1).hashCode(), list.get(3).hashCode());
    }

    @Test
    public void referentialEquality() {
        var o1 = JSObjects.create();
        list.add(o1);
        list.add(o1);

        var o2 = JSObjects.create();
        list.add(o2);

        assertSame(list.get(0), list.get(1));
        assertNotSame(list.get(0), list.get(2));

        assertSame(JSString.valueOf("q"), JSString.valueOf("q"));
        assertSame(JSNumber.valueOf(23), JSNumber.valueOf(23));
    }

    @Test
    public void equality() {
        var o1 = JSObjects.create();
        list.add(o1);
        list.add(o1);

        var o2 = JSObjects.create();
        list.add(o2);

        list.add(JSString.valueOf("q"));
        list.add(JSString.valueOf("q"));
        list.add(JSString.valueOf("w"));

        assertEquals(list.get(0), list.get(1));
        assertNotEquals(list.get(0), list.get(2));
        assertEquals(list.get(3), list.get(4));
        assertNotEquals(list.get(3), list.get(5));
    }

    @Test
    public void wrapNull() {
        list.add(jsNull());
        assertEquals("null", Objects.toString(list.get(0)));
        try {
            list.get(0).toString();
            fail("Expected exception not thrown");
        } catch (NullPointerException e) {
            // ok
        }
    }

    @Test
    public void unwrapNull() {
        list.add(null);
        assertTrue(isNull((JSObject) list.get(0)));
    }

    @Test
    public void instanceOf() {
        list.add(23);
        list.add(JSNumber.valueOf(23));
        list.add(null);
        list.add(jsNull());

        assertTrue(list.get(0) instanceof Object);
        assertTrue(list.get(0) instanceof Integer);
        assertFalse(list.get(0) instanceof JSNumber);

        assertTrue(list.get(1) instanceof Object);
        assertFalse(list.get(1) instanceof Integer);
        assertTrue(list.get(1) instanceof JSNumber);

        assertFalse(list.get(2) instanceof Object);
        assertFalse(list.get(2) instanceof Integer);
        assertFalse(list.get(2) instanceof JSNumber);

        assertTrue(JSObjects.create() instanceof JSObject);
    }

    @Test
    public void mergeTypes() {
        for (var i = 0; i < 2; ++i) {
            var o = i == 0 ? JSNumber.valueOf(23) : 23;
            list.add(o);
        }

        assertTrue(list.get(0) instanceof JSNumber);
        assertFalse(list.get(0) instanceof Integer);

        assertFalse(list.get(1) instanceof JSNumber);
        assertTrue(list.get(1) instanceof Integer);
    }

    @Test
    public void testClass() {
        list.add(23);
        list.add(JSNumber.valueOf(23));

        assertEquals(Integer.class, list.get(0).getClass());
        assertEquals(JSNumber.class, list.get(1).getClass());

        assertEquals("java.lang.Integer", list.get(0).getClass().getName());
        assertEquals("org.teavm.jso.impl.JSWrapper", list.get(1).getClass().getName());
    }

    @Test
    public void array() {
        var array = new JSString[3];
        array[0] = JSString.valueOf("q");
        array[1] = JSString.valueOf("q");
        array[2] = JSString.valueOf("w");

        assertEquals(JSString.valueOf("q"), array[0]);
        assertEquals(JSString.valueOf("w"), array[2]);
        assertEquals("q", array[0].stringValue());
        assertEquals("w", array[2].stringValue());
        assertEquals(array[0], array[1]);
        assertEquals(JSString[].class, array.getClass());
        assertEquals(JSString.class, array.getClass().getComponentType());
    }

    @Test
    public void objectArray() {
        var array = new Object[4];
        array[0] = JSString.valueOf("q");
        array[1] = JSString.valueOf("q");
        array[2] = JSString.valueOf("w");
        array[3] = "q";

        assertEquals(JSString.valueOf("q"), array[0]);
        assertEquals(JSString.valueOf("w"), array[2]);
        assertEquals("q", array[3]);

        assertEquals("q", ((JSString) array[0]).stringValue());
        assertEquals(array[0], array[1]);
        assertNotEquals(array[0], array[2]);
        assertNotEquals(array[0], array[3]);
    }

    @Test
    public void field() {
        field1 = 23;
        assertEquals("java.lang.Integer", field1.getClass().getName());

        field1 = JSNumber.valueOf(23);
        assertEquals("org.teavm.jso.impl.JSWrapper", field1.getClass().getName());
    }

    @Test
    public void passJavaToJS() {
        var a = processObject(new A(23));
        assertEquals("A(23)", a.toString());
        assertTrue(a instanceof A);
        assertEquals(23, ((A) a).getX());

        a = processObject(JSString.valueOf("qwe"));
        assertEquals("qwe", a.toString());
        assertTrue(a instanceof JSString);
        assertEquals("qwe", ((JSString) a).stringValue());

        a = processObject(JSNumber.valueOf(23));
        assertTrue(a instanceof JSString);
        assertEquals("number", ((JSString) a).stringValue());

        a = processObject(processObject(new A(24)));
        assertEquals("A(24)", a.toString());
        assertTrue(a instanceof A);
        assertEquals(24, ((A) a).getX());

        a = processObject(identity(processObject(new A(25))));
        assertEquals("A(25)", a.toString());
        assertTrue(a instanceof A);
        assertEquals(25, ((A) a).getX());

        a = processObject(identity(processObject(JSString.valueOf("asd"))));
        assertEquals("asd", a.toString());
        assertTrue(a instanceof JSString);
        assertEquals("asd", ((JSString) a).stringValue());

        a = processObject(processObject(identity(JSString.valueOf("zxc"))));
        assertEquals("zxc", a.toString());
        assertTrue(a instanceof JSString);
        assertEquals("zxc", ((JSString) a).stringValue());
    }

    @Test
    public void exportedObject() {
        var r = new ReturningObject() {
            @Override
            public Object get() {
                var o = createEmpty();
                setProperty(o, "foo", JSNumber.valueOf(23));
                return o;
            }
        };
        var o = extract(r);
        var foo = getProperty(o, "foo");
        assertTrue(foo instanceof JSNumber);
        assertEquals(JSNumber.valueOf(23), foo);
    }

    @Test
    public void setProperty() {
        var o = createEmpty();
        callSetProperty(o, JSNumber.valueOf(23));
        assertEquals(JSNumber.valueOf(23), getProperty(o, "foo"));
    }

    @Test
    public void createArray() {
        var array = new J[] {
                new JImpl(23),
                new JImpl(42)
        };
        assertEquals(23, array[0].foo());
        assertEquals(42, callFoo(array[1]));
        assertEquals("23,42", concatFoo(array));
    }

    @Test
    public void createArrayAndReturnToJS() {
        assertEquals("23,42", concatFoo(() -> new J[] {
                new JImpl(23),
                new JImpl(42)
        }));
    }

    private void callSetProperty(Object instance, Object o) {
        setProperty(instance, "foo", o);
    }

    @JSBody(script = "return null;")
    private static native JSObject jsNull();

    @JSBody(params = "o", script = "return o === null;")
    private static native boolean isNull(JSObject o);

    @JSBody(params = "o", script = "return typeof o === 'number' ? 'number' : o;")
    private static native Object processObject(Object o);

    private Object identity(Object o) {
        return o;
    }

    @JSBody(params = { "o", "name" }, script = "return o[name];")
    private static native Object getProperty(Object o, String name);

    @JSBody(params = { "o", "name", "value" }, script = "o[name] = value;")
    private static native void setProperty(Object o, String name, Object value);

    @JSBody(script = "return {};")
    private static native Object createEmpty();

    @JSBody(params = "o", script = "return o.get();")
    private static native Object extract(ReturningObject o);

    static class A {
        private int x;

        A(int x) {
            this.x = x;
        }

        int getX() {
            return x;
        }

        @Override
        public String toString() {
            return "A(" + x + ")";
        }
    }

    interface ReturningObject extends JSObject {
        Object get();
    }

    @JSBody(params = "o", script = "return o.foo();")
    private static native int callFoo(JSObject o);

    @JSBody(params = "array", script = "return array[0].foo() + ',' + array[1].foo(); ")
    private static native String concatFoo(J[] array);

    @JSBody(params = "supplier", script = "let array = supplier.get(); "
            + "return array[0].foo() + ',' + array[1].foo();")
    private static native String concatFoo(JArraySupplier supplier);

    interface J extends JSObject {
        int foo();
    }

    class JImpl implements J {
        private int value;

        JImpl(int value) {
            this.value = value;
        }

        @Override
        public int foo() {
            return value;
        }
    }

    interface JArraySupplier extends JSObject {
        J[] get();
    }
}
