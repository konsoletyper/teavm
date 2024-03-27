/*
 *  Copyright 2024 ihromant.
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
package org.teavm.jso.ajax;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLFormElement;
import org.teavm.jso.file.Blob;

@JSClass
public class FormData implements JSObject {
    public FormData() {
    }

    public FormData(HTMLFormElement form) {
    }

    public FormData(HTMLFormElement form, HTMLElement submitter) {
    }

    public native void append(String name, String value);

    public native void append(String name, Blob value);

    public native void append(String name, String value, String fileName);

    public native void append(String name, Blob value, String fileName);

    public native void delete(String name);

    /**
     * Actual element is either {@link Blob} or {@link org.teavm.jso.core.JSString}
     */
    // TODO: update signature when union types supported in TeaVM
    public native JSObject get(String name);

    /**
     * Actual elements are {@link Blob} or {@link org.teavm.jso.core.JSString}
     */
    // TODO: update signature when union types supported in TeaVM
    public native JSArray<JSObject> getAll(String name);

    public native boolean has(String name);

    public native void set(String name, String value);

    public native void set(String name, Blob value);

    public native void set(String name, String value, String fileName);

    public native void set(String name, Blob value, String fileName);
}
