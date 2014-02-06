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
package org.teavm.javascript.ni;

/**
 *
 * @author Alexey Andreev
 */
public interface JSObject {
    @JSProperty
    JSObject getPrototype();

    JSObject get(String name);

    void set(String name, JSObject obj);

    JSObject get(JSString name);

    void set(JSString name, JSObject obj);

    JSObject get(int index);

    void set(int index, JSObject obj);

    JSObject invoke(String method);

    JSObject invoke(String method, JSObject a);

    JSObject invoke(String method, JSObject a, JSObject b);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c, JSObject d);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f, JSObject g);

    JSObject invoke(String method, JSObject a, JSObject b, JSObject c,  JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h);

    JSObject invoke(JSString method);

    JSObject invoke(JSString method, JSObject a);

    JSObject invoke(JSString method, JSObject a, JSObject b);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c, JSObject d);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g);

    JSObject invoke(JSString method, JSObject a, JSObject b, JSObject c, JSObject d, JSObject e, JSObject f,
            JSObject g, JSObject h);

    @JSProperty
    boolean hasOwnProperty(String property);

    @JSProperty
    boolean hasOwnProperty(JSString property);

    boolean asBoolean();

    int asInteger();

    double asNumber();

    String asString();

    boolean isNull();

    boolean isUndefined();

    @JSMethod("toString")
    JSString toJSString();
}
