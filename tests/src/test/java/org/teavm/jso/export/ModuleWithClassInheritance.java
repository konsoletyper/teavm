/*
 *  Copyright 2025 konsoletyper.
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
import org.teavm.jso.JSExportClasses;
import org.teavm.jso.JSProperty;

@JSExportClasses({
        ModuleWithClassInheritance.BaseClass.class,
        ModuleWithClassInheritance.Subclass.class,
        ModuleWithClassInheritance.SubAbstract.class
})
public class ModuleWithClassInheritance {
    public static class BaseClass {
        @JSExport
        public BaseClass() {
        }

        @JSExport
        public String foo() {
            return "Base.foo";
        }
    }

    public static class Subclass extends BaseClass {
        @JSExport
        public Subclass() {
        }

        @Override
        public String foo() {
            return "Sub.foo";
        }

        @JSExport
        public String bar() {
            return "Sub.bar";
        }
    }

    public static abstract class BaseAbstract {
        @JSExport
        @JSProperty
        public abstract String getFoo();
    }

    public static class SubAbstract extends BaseAbstract {
        @JSExport
        public SubAbstract() {
        }

        @Override
        public String getFoo() {
            return "foo";
        }
    }
}
