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
package org.teavm.jso.export;

import org.teavm.jso.JSExport;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;

public final class SimpleModule {
    private SimpleModule() {
    }

    @JSExport
    public static int foo() {
        return 23;
    }

    @JSExport
    public static JSArray<JSString> bar(int count) {
        var array = new JSArray<JSString>();
        for (var i = 0; i < count; ++i) {
            array.push(JSString.valueOf("item" + i));
        }
        return array;
    }
}
