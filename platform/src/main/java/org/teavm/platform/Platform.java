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
package org.teavm.platform;

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Platforms;
import org.teavm.interop.Unmanaged;
import org.teavm.interop.UnsupportedOn;
import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.platform.plugin.PlatformGenerator;

@UnsupportedOn(Platforms.WEBASSEMBLY_GC)
public final class Platform {
    private Platform() {
    }

    @InjectedBy(PlatformGenerator.class)
    @Unmanaged
    @NoSideEffects
    public static native PlatformObject getPlatformObject(Object obj);

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    @NoSideEffects
    public static native Object clone(Object obj);

    @JSBody(script = "return $rt_nextId();")
    @NoSideEffects
    public static native int nextObjectId();

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native void startThread(PlatformRunnable runnable);

    private static void launchThread(PlatformRunnable runnable) {
        runnable.run();
    }

    public static void postpone(PlatformRunnable runnable) {
        schedule(runnable, 0);
    }

    @GeneratedBy(PlatformGenerator.class)
    @PluggableDependency(PlatformGenerator.class)
    public static native int schedule(PlatformRunnable runnable, int timeout);

    public static void killSchedule(int id) {
        Window.clearTimeout(id);
    }
}
