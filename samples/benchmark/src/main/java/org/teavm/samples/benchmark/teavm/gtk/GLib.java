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
package org.teavm.samples.benchmark.teavm.gtk;

import org.teavm.interop.Address;
import org.teavm.interop.Function;
import org.teavm.interop.Import;
import org.teavm.interop.Structure;

public final class GLib {
    private GLib() {
    }

    public static class GObject extends Structure {
    }

    @Import(name = "g_signal_connect")
    public static native long signalConnect(GObject instance, String signalName, Callback callback, Address data);

    @Import(name = "g_timeout_add")
    public static native int delay(int interval, TimerFunction function, Address data);

    public static abstract class Callback extends Function {
        public abstract void call();
    }

    public static abstract class TimerFunction extends Function {
        public abstract int run();
    }
}
