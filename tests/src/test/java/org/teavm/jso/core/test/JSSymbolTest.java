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
package org.teavm.jso.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.JSSymbol;
import org.teavm.jso.core.JSUndefined;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC })
public class JSSymbolTest {
    @Test
    public void getWorks() {
        var s = JSSymbol.create("customString");
        var o1 = JSObjects.create();
        var o2 = JSObjects.create();
        writeSymbol(s, o1, JSString.valueOf("works"));

        assertEquals(JSString.valueOf("works"), s.get(o1));
        assertTrue(s.presentIn(o1));
        assertEquals(JSUndefined.instance(), s.get(o2));
        assertFalse(s.presentIn(o2));
    }

    @Test
    public void setWorks() {
        var s = JSSymbol.create("customString");
        var o1 = JSObjects.create();
        var o2 = JSObjects.create();

        s.set(o1, JSString.valueOf("works"));
        assertEquals(JSString.valueOf("works"), readSymbol(s, o1));
        assertEquals(JSUndefined.instance(), readSymbol(s, o2));
    }

    @JSBody(params = { "symbol", "o", "value" }, script = "o[symbol] = value;")
    private static native void writeSymbol(JSSymbol<Object> symbol, Object o, JSObject value);

    @JSBody(params = { "symbol", "o" }, script = "return o[symbol];")
    private static native JSObject readSymbol(JSSymbol<Object> symbol, Object o);
}
