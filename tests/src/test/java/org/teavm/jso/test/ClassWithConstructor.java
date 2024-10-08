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

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSTopLevel;

@JSClass
public class ClassWithConstructor implements JSObject {
    public ClassWithConstructor(int foo) {
    }

    public ClassWithConstructor() {
    }

    @JSProperty
    public native int getFoo();

    public native String bar();

    public static native String staticMethod();

    @JSTopLevel
    public static native String topLevelFunction();

    @JSTopLevel
    @JSProperty
    public static native String getTopLevelProperty();

    @JSTopLevel
    @JSProperty
    public static native void setTopLevelProperty(String value);

    @JSTopLevel
    public static native JSObject createClass(boolean subclass);
}
