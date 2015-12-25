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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static org.junit.Assert.assertEquals;
import java.util.Calendar;
import net.java.html.js.JavaScriptBody;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author Alexey Andreev
 */
public class JavaScriptBodyTest {
    @Test
    public void readResource() throws IOException {
        InputStream is = JavaScriptBodyTest.class.getResourceAsStream("jvm.txt");
        assertNotNull("Resource jvm.txt found", is);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line = r.readLine();
            assertEquals("Line read", "TeaVM", line);
        }
    }

    @Test
    public void javaScriptBodyHandled() {
        assertEquals(23, simpleNativeMethod());
    }

    @Test
    public void dependencyPropagated() {
        A a = (A)returnValuePassed(new AImpl());
        assertEquals(23, a.foo());
    }

    @Test
    public void dependencyPropagatedThroughProperty() {
        storeObject(new AImpl());
        A a = (A)retrieveObject();
        assertEquals(23, a.foo());
    }

    @Test
    public void dependencyPropagatedThroughArray() {
        storeObject(new Object[] { new AImpl() });
        A[] array = (A[])retrieveObject();
        assertEquals(23, array[0].foo());
    }

    @Test
    public void valuePropagatedToCallback() {
        A a = new AImpl();
        assertEquals(23, invokeCallback(a));
    }

    @Test
    public void staticCallbackInvoked() {
        assertEquals(23, invokeStaticCallback(new AImpl()));
    }

    @Test
    public void unusedArgumentIgnored() {
        final int[] array = new int[1];
        invokeCallback(new Callback() {
            @Override
            public void exec(Calendar input) {
                array[0] = 23;
            }
        });
        assertEquals(23, array[0]);
    }

    private static class AImpl implements A {
        @Override public int foo() {
            return 23;
        }
    }

    @JavaScriptBody(args = {}, body = "return 23;")
    private native int simpleNativeMethod();

    @JavaScriptBody(args = { "value" }, body = "return value;")
    private native Object returnValuePassed(Object value);

    @JavaScriptBody(args = { "obj" }, body = "window._global_ = obj;")
    private native void storeObject(Object obj);

    @JavaScriptBody(args = {}, body = "return window._global_;")
    private native Object retrieveObject();

    @JavaScriptBody(args = { "callback" }, body = "return callback." +
            "@org.teavm.html4j.test.A::foo()()", javacall = true)
    private native int invokeCallback(A callback);

    @JavaScriptBody(args = { "callback" }, body = "return callback." +
            "@org.teavm.html4j.test.B::bar(" +
            "Lorg/teavm/html4j/test/A;)(_global_)", javacall = true)
    private native int invokeCallback(B callback);

    public static int staticCallback(A a) {
        return a.foo();
    }

    @JavaScriptBody(args = { "a" }, body = "return " +
            "@org.teavm.html4j.test.JavaScriptBodyTest::staticCallback(" +
            "Lorg/teavm/html4j/test/A;)(a)", javacall = true)
    private native int invokeStaticCallback(A a);

    @JavaScriptBody(args = "callback", body = "callback.@org.teavm.html4j.test.Callback::exec(" +
            "Ljava/util/Calendar;)(null)", javacall = true)
    private native void invokeCallback(Callback callback);
}
