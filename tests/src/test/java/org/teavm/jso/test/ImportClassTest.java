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
package org.teavm.jso.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSObject;
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.OnlyPlatform;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
@OnlyPlatform(TestPlatform.JAVASCRIPT)
@EachTestCompiledSeparately
public class ImportClassTest {
    @Test
    public void indexer() {
        var o = create();
        o.set("foo", 23);
        set(o, "bar", 42);
        assertEquals(23, o.get("foo"));
        assertEquals(42, o.get("bar"));
    }

    @JSBody(script = "return {};")
    private static native O create();

    @JSBody(params = { "o", "key", "value" }, script = "o[key] = value;")
    private static native void set(O o, String key, int value);

    interface O extends JSObject {
        @JSIndexer
        int get(String key);

        @JSIndexer
        void set(String key, int value);
    }
}
