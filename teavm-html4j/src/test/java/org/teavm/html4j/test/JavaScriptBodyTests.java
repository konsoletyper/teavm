/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.html4j.test;

import static org.junit.Assert.*;
import net.java.html.js.JavaScriptBody;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JavaScriptBodyTests {
    @Test
    public void javaScriptBodyHandled() {
        assertEquals(23, simpleNativeMethod());
    }

    @JavaScriptBody(args = {}, body = "return 23;")
    private native int simpleNativeMethod();

    @Test
    public void dependencyPropagated() {
        A a = (A)returnValuePassed(new AImpl());
        assertEquals(23, a.foo());
    }

    private static interface A {
        public int foo();
    }
    private static class AImpl implements A {
        @Override public int foo() {
            return 23;
        }
    }
    @JavaScriptBody(args = { "value" }, body = "return value;")
    private native Object returnValuePassed(Object value);
}
