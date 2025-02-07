/*
 *  Copyright 2025 Alexey Andreev.
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
import static org.junit.Assert.assertNotSame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
public class ByRefConversionTest {
    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY_GC)
    public void passesArrayByRef() {
        int[] array = { 23, 42 };

        mutateByRef(array);
        assertEquals(24, array[0]);
        assertEquals(43, array[1]);

        createByRefMutator().mutate(array);
        assertEquals(25, array[0]);
        assertEquals(44, array[1]);
    }

    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY_GC)
    public void passesLongArrayByRef() {
        long[] array = { 23, 42 };

        mutateByRef(array);
        assertEquals(24, array[0]);
        assertEquals(43, array[1]);

        createByRefLongMutator().mutate(array);
        assertEquals(25, array[0]);
        assertEquals(44, array[1]);
    }

    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY_GC)
    public void returnsArrayByRef() {
        int[] first = { 23, 42 };
        int[] second = rewrap(first);
        assertNotSame(first, second);
        second[0] = 99;
        assertEquals(99, first[0]);
    }

    @Test
    @SkipPlatform(TestPlatform.WEBASSEMBLY_GC)
    public void returnsLongArrayByRef() {
        long[] first = { 23, 42 };
        var second = rewrap(first);
        assertNotSame(first, second);
        second[0] = 99;
        assertEquals(99, first[0]);
    }


    @JSBody(params = "array", script = ""
            + "for (var i = 0; i < array.length; ++i) {"
            + "array[i]++;"
            + "}")
    private static native void mutateByRef(@JSByRef int[] array);


    @JSBody(params = "array", script = ""
            + "for (var i = 0; i < array.length; ++i) {"
            + "array[i]++;"
            + "}")
    private static native void mutateByRef(@JSByRef long[] array);

    private interface ByRefMutator extends JSObject {
        void mutate(@JSByRef int[] array);
    }

    private interface ByRefLongMutator extends JSObject {
        void mutate(@JSByRef long[] array);
    }

    @JSBody(script = ""
            + "return {"
            + "mutate : function(array) {"
            + "for (var i = 0; i < array.length; ++i) {"
            + "array[i]++;"
            + "}"
            + "}"
            + "};")
    private static native ByRefMutator createByRefMutator();


    @JSBody(script = ""
            + "return {"
            + "mutate : function(array) {"
            + "for (var i = 0; i < array.length; ++i) {"
            + "array[i]++;"
            + "}"
            + "}"
            + "};")
    private static native ByRefLongMutator createByRefLongMutator();

    @JSByRef
    @JSBody(params = "array", script = "return array;")
    private static native int[] rewrap(@JSByRef int[] array);

    @JSByRef
    @JSBody(params = "array", script = "return array;")
    private static native long[] rewrap(@JSByRef long[] array);
}
