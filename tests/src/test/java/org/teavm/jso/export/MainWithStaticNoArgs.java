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
package org.teavm.jso.export;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSExport;

// Static no-arg main must neither require nor call a no-arg constructor
public final class MainWithStaticNoArgs {
    private static List<String> log = new ArrayList<>();

    static {
        log.add("init");
    }

    private MainWithStaticNoArgs(int value) {
        throw new AssertionError("Static main must not construct its class");
    }

    static void main() {
        log.add("static");
    }

    @JSExport
    public static String log() {
        return String.join(";", log);
    }
}
