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
import org.junit.runner.RunWith;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSTopLevel;
import org.teavm.junit.AttachJavaScript;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;
import org.testng.annotations.Test;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@EachTestCompiledSeparately
public class CallTest {
    @Test
    @AttachJavaScript("org/teavm/jso/test/vararg.js")
    public void simpleVararg() {
        assertEquals("va:q:w", TestClass.allVararg("q", "w"));
        assertEquals("va:23:42", TestClass.allVarargInt(23, 42));

        var array = new String[3];
        for (var i = 0; i < array.length; ++i) {
            array[i] = String.valueOf((char) ('A' + i));
        }
        assertEquals("va:A:B:C", TestClass.allVararg(array));

        var intArray = new int[3];
        for (var i = 0; i < array.length; ++i) {
            intArray[i] = 6 + i;
        }
        assertEquals("va:6:7:8", TestClass.allVarargInt(intArray));

        assertEquals("va", TestClass.allVararg());
        assertEquals("va", TestClass.allVarargInt());
    }

    @Test
    @AttachJavaScript("org/teavm/jso/test/vararg.js")
    public void restVararg() {
        assertEquals("a:q,b:23,va:w:e", TestClass.restVararg("q", 23, "w", "e"));
        assertEquals("a:23,b:q,va:5:7", TestClass.restVararg(23, "q", 5, 7));

        assertEquals("a:q,b:23,va", TestClass.restVararg("q", 23));
        assertEquals("a:23,b:q,va", TestClass.restVararg(23, "q"));

        var array = new String[3];
        for (var i = 0; i < array.length; ++i) {
            array[i] = String.valueOf((char) ('A' + i));
        }
        assertEquals("a:q,b:23,va:A:B:C", TestClass.restVararg("q", 23, array));

        var intArray = new int[3];
        for (var i = 0; i < array.length; ++i) {
            intArray[i] = 6 + i;
        }
        assertEquals("a:23,b:q,va:6:7:8", TestClass.restVararg(23, "q", intArray));
    }

    @Test
    @AttachJavaScript("org/teavm/jso/test/vararg.js")
    public void topLevelVararg() {
        assertEquals("tva:q:w", TestClass.topLevelVararg("q", "w"));
        assertEquals("tva:23:42", TestClass.topLevelVarargInt(23, 42));

        var array = new String[3];
        for (var i = 0; i < array.length; ++i) {
            array[i] = String.valueOf((char) ('A' + i));
        }
        assertEquals("tva:A:B:C", TestClass.topLevelVararg(array));

        var intArray = new int[3];
        for (var i = 0; i < array.length; ++i) {
            intArray[i] = 6 + i;
        }
        assertEquals("tva:6:7:8", TestClass.topLevelVarargInt(intArray));

        assertEquals("tva", TestClass.topLevelVararg());
        assertEquals("tva", TestClass.topLevelVarargInt());
    }

    @JSClass
    public static class TestClass implements JSObject {
        public static native String allVararg(String... args);

        @JSMethod("allVararg")
        public static native String allVarargInt(int... args);

        public static native String restVararg(String a, int b, String... args);

        public static native String restVararg(int a, String b, int... args);

        @JSTopLevel
        public static native String topLevelVararg(String... args);

        @JSTopLevel
        @JSMethod("topLevelVararg")
        public static native String topLevelVarargInt(int... args);
    }
}
