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
import org.teavm.backend.wasm.debug.info.MethodInfo;

public class DebugMethodParser extends DebugSectionParser {
    private DebugStringParser strings;
    private DebugClassParser classes;
    private MethodInfoImpl[] methods;

    public DebugMethodParser(DebugStringParser strings, DebugClassParser classes) {
        super(DebugConstants.SECTION_METHODS);
        this.strings = strings;
        this.classes = classes;
    }

    public MethodInfo getMethod(int ptr) {
        return methods[ptr];
    }

    @Override
    protected void doParse() {
        var methods = new ArrayList<MethodInfoImpl>();
        while (ptr < data.length) {
            var cls = classes.getClass(readLEB());
            var name = strings.getString(readLEB());
            methods.add(new MethodInfoImpl(cls, name));
        }
        this.methods = methods.toArray(new MethodInfoImpl[0]);
    }

    private static class MethodInfoImpl extends MethodInfo {
        private ClassInfo cls;
        private String name;

        MethodInfoImpl(ClassInfo cls, String name) {
            this.cls = cls;
            this.name = name;
        }

        @Override
        public ClassInfo cls() {
            return cls;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
