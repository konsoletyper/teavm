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

import org.teavm.jso.JSClass;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSExportClasses;
import org.teavm.jso.JSProperty;

@JSExportClasses({ ModuleWithExportedClasses.A.class, ModuleWithExportedClasses.B.class })
public class ModuleWithExportedClasses {
    public static class A {
        @JSExport
        public static int foo() {
            return 23;
        }
    }

    @JSClass(name = "BB")
    public static class B {
        private int bar;

        public B(int bar) {
            this.bar = bar;
        }

        @JSExport
        @JSProperty
        public int getBar() {
            return bar;
        }

        @JSExport
        public static B create(int bar) {
            return new B(bar);
        }
    }
}
