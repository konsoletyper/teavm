/*
 *  Copyright 2023 Alexey Andreev.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSString;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@EachTestCompiledSeparately
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@SkipJVM
public class AsyncTest {
    @Test
    public void primitives() {
        assertEquals(23, getPrimitive());
    }

    @Async
    private native int getPrimitive();

    private void getPrimitive(AsyncCallback<Integer> callback) {
        Window.setTimeout(() -> callback.complete(23), 0);
    }

    @Test
    public void jsObjects() {
        var str = getJsString();
        assertEquals(3, str.getLength());
        assertEquals("foo", str.stringValue());
    }

    @Async
    private native JSString getJsString();

    private void getJsString(AsyncCallback<JSString> callback) {
        Window.setTimeout(() -> callback.complete(JSString.valueOf("foo")), 0);
    }
}
