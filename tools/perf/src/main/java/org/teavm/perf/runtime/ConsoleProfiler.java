/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf.runtime;

import org.teavm.jso.JSBody;

/**
 * Records CPU profile with {@code console.profile}, which browser reports via DevTools protocol.
 */
public class ConsoleProfiler implements BenchmarkProfiler {
    @Override
    public void start(String title) {
        profile(title);
    }

    @Override
    public void stop(String title) {
        profileEnd(title);
    }

    @JSBody(params = "title", script = "console.profile(title);")
    private static native void profile(String title);

    @JSBody(params = "title", script = "console.profileEnd(title);")
    private static native void profileEnd(String title);
}
