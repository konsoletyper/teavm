/*
 *  Copyright 2016 Alexey Andreev.
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

import org.teavm.testing.TestRunner;

final class TestEntryPoint {
    private static Object testCase;

    private TestEntryPoint() {
    }

    public static void run() throws Throwable {
        createRunner().run(() -> launchTest());
    }

    private static native TestRunner createRunner();

    private static native void launchTest();

    private static native boolean isExpectedException(Class<?> cls);

    public static void main(String[] args) throws Exception {
        try {
            run();
            System.out.println("SUCCESS");
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            System.out.println("FAILURE");
        }
    }
}
