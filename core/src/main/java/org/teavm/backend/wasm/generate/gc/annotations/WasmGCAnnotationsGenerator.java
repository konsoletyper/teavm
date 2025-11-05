/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc.annotations;

import java.util.List;
import java.util.function.Consumer;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassGenerator;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.analysis.ClassMetadataRequirements;

public class WasmGCAnnotationsGenerator {
    private ClassReaderSource classes;
    private ClassMetadataRequirements metadataRequirements;
    private WasmGCClassGenerator classInfoProvider;
    private List<Consumer<WasmFunction>> parts;
    private WasmGCAnnotationsHelper helper;

    public WasmGCAnnotationsGenerator(ClassReaderSource classes, ClassMetadataRequirements metadataRequirements,
            WasmGCClassGenerator classInfoProvider, WasmGCStringProvider stringProvider,
            List<Consumer<WasmFunction>> parts) {
        helper = new WasmGCAnnotationsHelper(classes, classInfoProvider, stringProvider);
        this.classes = classes;
        this.metadataRequirements = metadataRequirements;
        this.classInfoProvider = classInfoProvider;
        this.parts = parts;
    }

    public void addClassAnnotations(String className, WasmGCClassInfo classInfo) {
        var cls = classes.get(className);
        if (cls == null) {
            return;
        }
        var info = metadataRequirements.getInfo(className);
        if (!info.annotations()) {
            return;
        }
        var annotationsExpr = helper.generateAnnotations(cls.getAnnotations().all());
        if (annotationsExpr == null) {
            return;
        }
        var global = classInfo.getPointer();
        var classClass = classInfoProvider.getClassInfo("java.lang.Class");
        var setAnnotations = new WasmStructSet(classClass.getStructure(), new WasmGetGlobal(global),
                classInfoProvider.getClassAnnotationsOffset(), annotationsExpr);
        parts.add(f -> f.getBody().add(setAnnotations));
    }
}
