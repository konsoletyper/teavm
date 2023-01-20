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
    private DebugVariablesBuilder variables;
    private DebugLinesBuilder lines;
    private DebugClassLayoutBuilder classLayout;

    public DebugInfoBuilder() {
        strings = new DebugStringsBuilder();
        files = new DebugFilesBuilder(strings);
        packages = new DebugPackagesBuilder(strings);
        classes = new DebugClassesBuilder(packages, strings);
        methods = new DebugMethodsBuilder(classes, strings);
        variables = new DebugVariablesBuilder(strings);
        lines = new DebugLinesBuilder(files, methods);
        classLayout = new DebugClassLayoutBuilder(classes, strings);
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

    public DebugMethods methods() {
        return methods;
    }

    public DebugVariables variables() {
        return variables;
    }

    public DebugLines lines() {
        return lines;
    }

    public DebugClassLayout classLayout() {
        return classLayout;
    }

    public List<WasmCustomSection> build() {
        var result = new ArrayList<WasmCustomSection>();
        addSection(result, strings);
        addSection(result, files);
        addSection(result, packages);
        addSection(result, classes);
        addSection(result, methods);
        addSection(result, variables);
        addSection(result, lines);
        addSection(result, classLayout);
        return result;
    }

    private void addSection(List<WasmCustomSection> sections, DebugSectionBuilder builder) {
        if (builder.isEmpty()) {
            return;
        }
        sections.add(new WasmCustomSection(builder.name(), builder.build()));
    }
}
