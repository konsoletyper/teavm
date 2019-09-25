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

import org.teavm.jso.JSBody;
import org.teavm.jso.geolocation.Geolocation;

public final class Navigator {
    private Navigator() {
    }

    @JSBody(script = "return navigator.onLine;")
    public static native boolean isOnline();

    @JSBody(script = "return navigator.geolocation;")
    public static native Geolocation getGeolocation();

    @JSBody(script = "return (\"geolocation\" in navigator);")
    public static native boolean isGeolocationAvailable();

    @JSBody(script = "return navigator.language;")
    public static native String getLanguage();

    @JSBody(script = "return navigator.languages;")
    public static native String[] getLanguages();
}
