/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.jso.core;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public final class JSObjects {
    private JSObjects() {
    }

    @JSBody(params = "object", script = "return Object.getOwnPropertyNames(object);")
    public static native String[] getOwnPropertyNames(JSObject object);

    @JSBody(script = "return {};")
    public static native <T extends JSObject> T create();

    @JSBody(script = "return Object.create(null);")
    public static native <T extends JSObject> T createWithoutProto();

    @JSBody(params = "object", script = "return typeof object === 'undefined';")
    public static native boolean isUndefined(JSObject object);

    @JSBody(script = "return void 0;")
    public static native JSObject undefined();

    @JSBody(params = "object", script = "return typeof object;")
    public static native String typeOf(JSObject object);

    @JSBody(params = "object", script = "return object.toString();")
    public static native String toString(JSObject object);

    @JSBody(params = { "object", "name" }, script = "return name in object;")
    public static native boolean hasProperty(JSObject object, String name);
}
