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

import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;

/**
 * The Fetch API provides an interface for fetching resources (including across the network).
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API">MDN Reference</a>
 */
public final class Fetch {
    private Fetch() {
    }

    @JSBody(params = "url", script = "return fetch(url)")
    public static native JSPromise<Response> fetch(String url);

    @JSBody(params = { "url", "options" }, script = "return fetch(url, options)")
    public static native JSPromise<Response> fetch(String url, RequestInit options);
}
