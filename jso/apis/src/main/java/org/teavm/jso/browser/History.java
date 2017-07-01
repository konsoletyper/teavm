/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.browser;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface History extends JSObject {
    @JSProperty
    int getLength();

    @JSProperty
    JSObject getState();

    void back();

    void forward();

    void go(int delta);

    void pushState(JSObject data, String title);

    void pushState(JSObject data, String title, String url);

    void replaceState(JSObject data, String title);

    void replaceState(JSObject data, String title, String url);

    static History current() {
        return Window.current().getHistory();
    }
}
