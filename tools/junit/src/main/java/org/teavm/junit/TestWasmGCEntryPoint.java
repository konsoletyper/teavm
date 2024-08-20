/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.junit;

import org.teavm.classlib.impl.console.JSStdoutPrintStream;

final class TestWasmGCEntryPoint {
    private TestWasmGCEntryPoint() {
    }

    public static void main(String[] args) throws Throwable {
        try {
            TestEntryPoint.run(args.length > 0 ? args[0] : null);
            new JSStdoutPrintStream().println("SUCCESS");
        } catch (Throwable e) {
            var out = new JSStdoutPrintStream();
            e.printStackTrace(out);
            out.println("FAILURE");
        }
        TestEntryPoint.run(args.length > 0 ? args[0] : null);
    }
}
