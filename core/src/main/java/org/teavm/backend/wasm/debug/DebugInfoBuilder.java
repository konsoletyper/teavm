/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmCustomSection;

public class DebugInfoBuilder {
    private DebugStringsBuilder strings;
    private DebugFilesBuilder files;
    private DebugPackagesBuilder packages;
    private DebugClassesBuilder classes;
    private DebugMethodsBuilder methods;
    private DebugLinesBuilder lines;

    public DebugInfoBuilder() {
        strings = new DebugStringsBuilder();
        files = new DebugFilesBuilder(strings);
        packages = new DebugPackagesBuilder(strings);
        classes = new DebugClassesBuilder(packages, strings);
        methods = new DebugMethodsBuilder(classes, strings);
        lines = new DebugLinesBuilder(files, methods);
    }

    public DebugStrings strings() {
        return strings;
    }

    public DebugFiles files() {
        return files;
    }

    public DebugPackages packages() {
        return packages;
    }

    public DebugClasses classes() {
        return classes;
    }

    public DebugMethodsBuilder methods() {
        return methods;
    }

    public DebugLinesBuilder lines() {
        return lines;
    }

    public List<WasmCustomSection> build() {
        var result = new ArrayList<WasmCustomSection>();
        addSection(result, "teavm_str", strings);
        addSection(result, "teavm_file", files);
        addSection(result, "teavm_pkg", packages);
        addSection(result, "teavm_classes", classes);
        addSection(result, "teavm_methods", methods);
        addSection(result, "teavm_line", lines);
        return result;
    }

    private void addSection(List<WasmCustomSection> sections, String name, DebugSectionBuilder builder) {
        if (builder.isEmpty()) {
            return;
        }
        sections.add(new WasmCustomSection(name, builder.build()));
    }
}
