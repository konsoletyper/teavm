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
package org.teavm.backend.wasm.generate.gc.classes;

public class WasmGCStandardClasses {
    private WasmGCClassInfoProvider classGenerator;
    private WasmGCClassInfo classClassInfo;
    private WasmGCClassInfo stringClassInfo;
    private WasmGCClassInfo objectClassInfo;

    public WasmGCStandardClasses(WasmGCClassInfoProvider classGenerator) {
        this.classGenerator = classGenerator;
    }

    public WasmGCClassInfo classClass() {
        if (classClassInfo == null) {
            classClassInfo = classGenerator.getClassInfo("java.lang.Class");
        }
        return classClassInfo;
    }

    public WasmGCClassInfo stringClass() {
        if (stringClassInfo == null) {
            stringClassInfo = classGenerator.getClassInfo("java.lang.String");
        }
        return stringClassInfo;
    }

    public WasmGCClassInfo objectClass() {
        if (objectClassInfo == null) {
            objectClassInfo = classGenerator.getClassInfo("java.lang.Object");
        }
        return objectClassInfo;
    }
}
