/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.classlib;

import org.teavm.interop.PlatformMarker;
import org.teavm.interop.PlatformMarkers;

public final class PlatformDetector {
    private PlatformDetector() {
    }

    @PlatformMarker(PlatformMarkers.WEBASSEMBLY)
    public static boolean isWebAssembly() {
        return false;
    }

    @PlatformMarker(PlatformMarkers.JAVASCRIPT)
    public static boolean isJavaScript() {
        return false;
    }

    @PlatformMarker(PlatformMarkers.C)
    public static boolean isC() {
        return false;
    }

    @PlatformMarker(PlatformMarkers.LOW_LEVEL)
    public static boolean isLowLevel() {
        return false;
    }
}
