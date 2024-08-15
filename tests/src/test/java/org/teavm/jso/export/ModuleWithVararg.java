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

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSExportClasses;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.testng.util.Strings;

@JSExportClasses(ModuleWithVararg.A.class)
public class ModuleWithVararg {
    private ModuleWithVararg() {
    }

    @JSExport
    public static String strings(String... values) {
        var sb = new StringBuilder();
        sb.append("strings: ");
        sb.append(Strings.join(", ", values));
        return sb.toString();
    }

    @JSExport
    public static String prefixStrings(int a, String... values) {
        var sb = new StringBuilder();
        sb.append("strings(").append(a).append("): ");
        sb.append(Strings.join(", ", values));
        return sb.toString();
    }

    @JSExport
    public static String ints(int... values) {
        var sb = new StringBuilder();
        sb.append("ints: ");
        sb.append(IntStream.of(values).mapToObj(Integer::toString).collect(Collectors.joining(", ")));
        return sb.toString();
    }

    @JSExport
    public static String prefixInts(String prefix, int... values) {
        var sb = new StringBuilder();
        sb.append("ints(").append(prefix).append("): ");
        sb.append(IntStream.of(values).mapToObj(Integer::toString).collect(Collectors.joining(", ")));
        return sb.toString();
    }

    @JSExport
    public static String objects(ObjectWithString... values) {
        var sb = new StringBuilder();
        sb.append("objects: ");
        sb.append(Stream.of(values).map(v -> v.getStringValue()).collect(Collectors.joining(", ")));
        return sb.toString();
    }

    public interface ObjectWithString extends JSObject {
        @JSProperty
        String getStringValue();
    }

    public static class A {
        @JSExport
        public A() {
        }

        @JSExport
        public String strings(String... values) {
            var sb = new StringBuilder();
            sb.append("A.strings: ");
            sb.append(Strings.join(", ", values));
            return sb.toString();
        }
    }
}
