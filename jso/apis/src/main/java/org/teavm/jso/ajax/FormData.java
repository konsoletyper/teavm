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
package org.teavm.jso.ajax;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.blob.Blob;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.html.HTMLFormElement;

/**
*
* @author Jan-Felix Wittmann
*/
public abstract class FormData implements JSObject {

    public abstract void append(String name, String value);

    public abstract void append(String name, String value, String filename);

    public abstract <T extends Blob> void append(String name, T value);

    public abstract <T extends Blob> void append(String name, T value, String filename);

    public abstract void delete(String name);

    public abstract JSObject get(String name);

    public abstract JSArray<JSObject> getAll(String name);

    public abstract boolean has(String name);

    public abstract void set(String name, String value);

    public abstract void set(String name, String value, String filename);

    public abstract <T extends Blob> void set(String name, T value);

    public abstract <T extends Blob> void set(String name, T value, String filename);

    @JSBody(params = {}, script = "return new FormData();")
    public static native FormData create();

    @JSBody(params = { "form" }, script = "return new FormData(form);")
    public static native FormData create(HTMLFormElement form);
}
