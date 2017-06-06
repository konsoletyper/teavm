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
package org.teavm.jso.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class ExportClass {
    @Test
    public void simpleClassExported() {
        assertEquals("(OK)", callIFromJs(new SimpleClass()));
        assertEquals("[OK]", callIFromJs(new DerivedSimpleClass()));
    }

    @JSBody(params = "a", script = "return a.foo('OK');")
    private static native String callIFromJs(I a);

    interface I extends JSObject {
        String foo(String a);
    }

    static class SimpleClass implements I {
        @Override
        public String foo(String a) {
            return "(" + a + ")";
        }
    }

    static class DerivedSimpleClass implements I {
        @Override
        public String foo(String a) {
            return "[" + a + "]";
        }
    }

}
