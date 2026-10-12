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
package org.teavm.wasm;

import org.teavm.classlib.support.Reflectable;

public final class ReflectionEntry {
    private ReflectionEntry() {
    }

    public static void main(String[] args) {
        new Target().foo();
        // Class.getDeclaredConstructors() and Class.getDeclaredMethods() read reflection metadata
        // in two different methods.
        var constructors = Target.class.getDeclaredConstructors().length;
        var methods = Target.class.getDeclaredMethods().length;
        System.out.println(constructors + " " + methods);
    }

    public static class Target {
        @Reflectable
        public Target() {
        }

        @Reflectable
        public void foo() {
        }
    }
}
