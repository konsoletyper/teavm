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

final class TestEntryPoint {
    private static Object testCase;

    private TestEntryPoint() {
    }

    public static void run(String name) throws Exception {
        before();
        try {
            launchTest(name);
        } finally {
            try {
                after();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private static native void before();

    private static native void launchTest(String name) throws Exception;

    private static native void after();

    public static void main(String[] args) throws Throwable {
        run(args.length == 1 ? args[0] : null);
    }
}
