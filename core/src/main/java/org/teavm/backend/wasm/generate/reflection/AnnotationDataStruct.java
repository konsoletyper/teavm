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
package org.teavm.backend.wasm.generate.reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmExpressionToInstructionConverter;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;

public class AnnotationDataStruct {
    private WasmModule module;
    private WasmGCNameProvider names;
    private String className;
    private ClassReaderSource classes;
    private BaseWasmFunctionRepository functions;
    private WasmGCClassInfoProvider classInfoProvider;
    private ReflectionTypes reflectionTypes;

    private WasmStructure structure;
    private Map<String, Field> fieldsByName = new HashMap<>();
    private List<Field> fields = new ArrayList<>();

    private WasmFunction constructor;

    AnnotationDataStruct(String className, String dataClassName, WasmGCNameProvider names, WasmModule module,
            ClassReaderSource classes, BaseWasmFunctionRepository functions, WasmGCClassInfoProvider classInfoProvider,
            ReflectionTypes reflectionTypes) {
        this.module = module;
        this.names = names;
        this.classes = classes;
        this.reflectionTypes = reflectionTypes;
        this.functions = functions;
        this.classInfoProvider = classInfoProvider;
        this.className = className;
        structure = new WasmStructure(names.topLevel(dataClassName), this::initFields);
        module.types.add(structure);
    }

    public WasmStructure structure() {
        return structure;
    }

    private void initFields(List<WasmField> fields) {
        var cls = classes.get(className);
        if (cls == null) {
            return;
        }

        for (var method : cls.getMethods()) {
            var index = fields.size();
            var field = new WasmField(reflectionTypes.typeForAnnotation(method.getResultType()), method.getName());
            fields.add(field);
            var fieldData = new Field(method.getName(), method.getResultType(), method.getAnnotationDefault(), index);
            fieldsByName.put(method.getName(), fieldData);
            this.fields.add(fieldData);
        }
    }


    public Field field(String name) {
        structure.init();
        return fieldsByName.get(name);
    }

    public List<? extends Field> fields() {
        structure.init();
        return fields;
    }

    public WasmFunction constructor() {
        if (constructor == null) {
            var fn = new WasmFunction(reflectionTypes.annotationInfo().constructorType());
            fn.setName(names.topLevel(names.suggestForClass(className) + "@constructor"));

            var dataClassName = className + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
            var implClassName = className + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
            var wasmDataType = classInfoProvider.getClassInfo(implClassName);

            var param = new WasmLocal(fn.getType().getParameterTypes().get(0));
            var result = new WasmLocal(wasmDataType.getType());
            fn.add(param);
            fn.add(result);

            var body = new ArrayList<WasmExpression>();
            body.add(new WasmSetLocal(result, new WasmStructNewDefault(wasmDataType.getStructure())));
            body.add(new WasmStructSet(
                    wasmDataType.getStructure(),
                    new WasmGetLocal(result),
                    WasmGCClassInfoProvider.CLASS_FIELD_OFFSET,
                    new WasmGetGlobal(wasmDataType.getVirtualTablePointer())
            ));

            var ctorRef = new MethodReference(implClassName, "<init>", ValueType.object(dataClassName),
                    ValueType.VOID);
            var ctor = functions.forInstanceMethod(ctorRef);
            var castParam = new WasmCast(new WasmGetLocal(param), structure.getReference());
            body.add(new WasmCall(ctor, new WasmGetLocal(result), castParam));
            body.add(new WasmGetLocal(result));

            new WasmExpressionToInstructionConverter(fn.getBody()).convertAll(body);

            module.functions.add(fn);
            constructor = fn;
        }
        return constructor;
    }

    public static class Field {
        public final String name;
        public final ValueType type;
        public final AnnotationValue defaultValue;
        public final int index;

        private Field(String name, ValueType type, AnnotationValue defaultValue, int index) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.index = index;
        }
    }
}
