/*
 *  Copyright 2024 Alexey Andreev.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSNumber;
import org.teavm.junit.AttachJavaScript;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@EachTestCompiledSeparately
public class InstanceOfTest {
    @Test
    @AttachJavaScript("org/teavm/jso/test/classWithConstructor.js")
    public void instanceOf() {
        var a = createClassWithConstructor();
        assertTrue(a instanceof ClassWithConstructor);
        assertFalse(a instanceof JSNumber);
        assertTrue(a instanceof I);
        assertTrue(a instanceof C);

        var b = callCreateClassWithConstructor();
        assertTrue(b instanceof ClassWithConstructor);
        assertFalse(b instanceof JSNumber);
        assertTrue(b instanceof I);
        assertTrue(b instanceof C);

        var c = createNumber();
        assertFalse(c instanceof ClassWithConstructor);
        assertTrue(c instanceof JSNumber);
        assertTrue(c instanceof I);
        assertTrue(c instanceof C);

        var d = callCreateNumber();
        assertFalse(d instanceof ClassWithConstructor);
        assertTrue(d instanceof JSNumber);
        assertTrue(d instanceof I);
        assertTrue(d instanceof C);
    }

    @Test
    @AttachJavaScript("org/teavm/jso/test/classWithConstructor.js")
    public void cast() {
        var a = createClassWithConstructor();
        assertEquals(99, ((ClassWithConstructor) a).getFoo());

        try {
            assertEquals(99, ((JSNumber) a).intValue());
            fail("CCE not thrown");
        } catch (ClassCastException e) {
            // expected
        }
        assertEquals(99, ((I) a).getFoo());
        assertEquals(99, ((C) a).getFoo());

        var c = createNumber();
        assertEquals(23, ((JSNumber) c).intValue());
        try {
            assertEquals(99, ((ClassWithConstructor) c).getFoo());
            fail("CCE not thrown");
        } catch (ClassCastException e) {
            // expected
        }

        var d = (ClassWithConstructor) returnNull();
        assertNull(d);
    }

    private Object callCreateClassWithConstructor() {
        return createClassWithConstructor();
    }

    @JSBody(script = "return new ClassWithConstructor();")
    private static native JSObject createClassWithConstructor();

    @JSBody(script = "return null;")
    private static native JSObject returnNull();

    private Object callCreateNumber() {
        return createNumber();
    }

    @JSBody(script = "return 23;")
    private static native JSObject createNumber();

    interface I extends JSObject {
        @JSProperty
        int getFoo();
    }

    @JSClass(transparent = true)
    static abstract class C implements JSObject {
        @JSProperty
        public abstract int getFoo();
    }
}
