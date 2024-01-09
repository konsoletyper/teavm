/*
 *  Copyright 2024 Alexey Andreev.
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

import org.teavm.jso.JSExport;
import org.teavm.jso.JSProperty;

public final class ModuleWithExportedClassMembers {
    private ModuleWithExportedClassMembers() {
    }

    @JSExport
    public static C createObject(String prefix) {
        return new C(prefix);
    }

    @JSExport
    public static String consumeObject(C c) {
        return "consumeObject:" + c.bar();
    }

    public static class C {
        private String prefix;

        public C(String prefix) {
            this.prefix = prefix;
        }

        @JSExport
        @JSProperty
        public int getFoo() {
            return 23;
        }

        @JSExport
        public String bar() {
            return prefix + ":" + 42;
        }

        @JSExport
        public static int baz() {
            return 99;
        }

        @JSExport
        @JSProperty
        public static String staticProp() {
            return "I'm static";
        }
    }
}
