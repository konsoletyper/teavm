/*
 *  Copyright 2026 Jonathan Coates.
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
package org.teavm.jso.fetch;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * The {@link Headers} interface of the Fetch API allows you to perform various actions on HTTP request and response
 * headers. These actions include retrieving, setting, adding to, and removing headers from the list of the request's
 * headers.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Headers">MDN Reference</a>
 */
@JSClass
public class Headers implements JSObject {
    public native void append(String key, String value);

    public native void forEach(ForEachConsumer consumer);

    @JSFunctor
    @FunctionalInterface
    public interface ForEachConsumer extends JSObject {
        void accept(String value, String key, Headers object);
    }
}
