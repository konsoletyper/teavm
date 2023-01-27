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
import org.teavm.backend.wasm.debug.info.ClassInfo;
import org.teavm.backend.wasm.debug.info.PackageInfo;

public class DebugClassParser extends DebugSectionParser {
    private DebugStringParser strings;
    private DebugPackageParser packages;
    private ClassInfoImpl[] classes;

    public DebugClassParser(DebugStringParser strings, DebugPackageParser packages) {
        super(DebugConstants.SECTION_CLASSES, strings, packages);
        this.strings = strings;
        this.packages = packages;
    }

    public ClassInfo getClass(int ptr) {
        return classes[ptr];
    }

    @Override
    protected void doParse() {
        var classes = new ArrayList<ClassInfoImpl>();
        while (ptr < data.length) {
            var pkg = packages.getPackage(readLEB());
            var name = strings.getString(readLEB());
            classes.add(new ClassInfoImpl(pkg, name));
        }
        this.classes = classes.toArray(new ClassInfoImpl[0]);
    }

    private static class ClassInfoImpl extends ClassInfo {
        private PackageInfo pkg;
        private String name;

        ClassInfoImpl(PackageInfo pkg, String name) {
            this.pkg = pkg;
            this.name = name;
        }

        @Override
        public PackageInfo pkg() {
            return pkg;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
