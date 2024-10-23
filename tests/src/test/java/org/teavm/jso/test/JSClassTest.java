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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform({ TestPlatform.JAVASCRIPT, TestPlatform.WEBASSEMBLY_GC})
@EachTestCompiledSeparately
public class JSClassTest {
    @Test
    public void passJSObjectToArgOfOverlayMethod() {
        var c = C.create();
        var result = c.bar(() -> "test");
        assertEquals("bar,test", result);
    }

    static abstract class C implements JSObject {
        private C() {
        }

        abstract String foo(String a, JSSupplier b);

        String bar(JSSupplier b) {
            return foo("bar", b);
        }

        @JSBody(script = ""
                + "return {"
                    + "foo: function(a, b) { return a + ',' + b(); }"
                + "}")
        static native C create();
    }

    @JSFunctor
    interface JSSupplier extends JSObject {
        String get();
    }
}
