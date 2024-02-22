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
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.gamepad.Gamepad;
import org.teavm.jso.geolocation.Geolocation;

@JSClass(name = "navigator")
public final class Navigator implements JSObject {
    private Navigator() {
    }

    @JSProperty("onLine")
    public static native boolean isOnline();

    @JSProperty
    public static native Geolocation getGeolocation();

    @JSBody(script = "return (\"geolocation\" in navigator);")
    public static native boolean isGeolocationAvailable();

    @JSProperty
    public static native String getLanguage();

    @JSProperty
    public static native String[] getLanguages();
    
    @JSProperty
    public static native Gamepad[] getGamepads();

    @JSBody(script = "return navigator.hardwareConcurrency")
    public static native int hardwareConcurrency();
}
