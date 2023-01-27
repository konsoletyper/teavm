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
package org.teavm.backend.wasm.debug.parser;

import java.util.ArrayList;
import org.teavm.backend.wasm.debug.DebugConstants;
import org.teavm.backend.wasm.debug.info.PackageInfo;

public class DebugPackageParser extends DebugSectionParser {
    private DebugStringParser strings;
    private PackageInfoImpl[] packages;

    public DebugPackageParser(DebugStringParser strings) {
        super(DebugConstants.SECTION_PACKAGES, strings);
        this.strings = strings;
    }

    public PackageInfo getPackage(int index) {
        return index == 0 ? null : packages[index - 1];
    }

    @Override
    protected void doParse() {
        var packageBuilders = new ArrayList<PackageBuilder>();
        while (ptr < data.length) {
            packageBuilders.add(new PackageBuilder(readLEB(), strings.getString(readLEB())));
        }
        packages = new PackageInfoImpl[packageBuilders.size()];
        for (var i = 0; i < packages.length; ++i) {
            packages[i] = new PackageInfoImpl(packageBuilders.get(i).name);
        }
        for (var i = 0; i < packages.length; ++i) {
            packages[i].parent = getPackage(packageBuilders.get(i).parent);
        }
    }

    private static class PackageInfoImpl extends PackageInfo {
        private PackageInfo parent;
        private String name;

        PackageInfoImpl(String name) {
            this.name = name;
        }

        @Override
        public PackageInfo parent() {
            return parent;
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static class PackageBuilder {
        int parent;
        String name;

        PackageBuilder(int parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }
}
