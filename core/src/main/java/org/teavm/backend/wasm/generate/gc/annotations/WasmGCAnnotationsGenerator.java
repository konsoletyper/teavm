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

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassGenerator;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassMetadataRequirements;

public class WasmGCAnnotationsGenerator {
    private ClassReaderSource classes;
    private ClassMetadataRequirements metadataRequirements;
    private WasmGCClassGenerator classInfoProvider;
    private WasmGCStringProvider stringProvider;
    private List<Consumer<WasmFunction>> parts;

    public WasmGCAnnotationsGenerator(ClassReaderSource classes, ClassMetadataRequirements metadataRequirements,
            WasmGCClassGenerator classInfoProvider, WasmGCStringProvider stringProvider,
            List<Consumer<WasmFunction>> parts) {
        this.classes = classes;
        this.metadataRequirements = metadataRequirements;
        this.classInfoProvider = classInfoProvider;
        this.stringProvider = stringProvider;
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
        var annotationsToExpose = new ArrayList<AnnotationReader>();
        for (var annotation : cls.getAnnotations().all()) {
            var annotationCls = classes.get(annotation.getType());
            if (annotationCls == null) {
                continue;
            }
            var retention = annotationCls.getAnnotations().get(Retention.class.getName());
            if (retention == null) {
                continue;
            }
            if (Objects.equals(retention.getValue("value").getEnumValue().getFieldName(), "RUNTIME")) {
                annotationsToExpose.add(annotation);
            }
        }
        if (annotationsToExpose.isEmpty()) {
            return;
        }

        var array = new WasmArrayNewFixed(classInfoProvider.getObjectArrayType());
        for (var annotation : annotationsToExpose) {
            array.getElements().add(generateAnnotation(annotation));
        }
        var global = classInfo.getPointer();
        var classClass = classInfoProvider.getClassInfo("java.lang.Class");
        var setAnnotations = new WasmStructSet(classClass.getStructure(), new WasmGetGlobal(global),
                classInfoProvider.getClassAnnotationsOffset(), array);
        parts.add(f -> f.getBody().add(setAnnotations));
    }

    private WasmExpression generateAnnotation(AnnotationReader annotation) {
        var annotCls = classes.get(annotation.getType());
        var annotImpl = classes.get(annotation.getType() + "$$_impl");
        var annotImplInfo = classInfoProvider.getClassInfo(annotImpl.getName());
        var constructor = new WasmStructNew(annotImplInfo.getStructure());
        constructor.getInitializers().add(new WasmGetGlobal(annotImplInfo.getVirtualTablePointer()));
        constructor.getInitializers().add(new WasmNullConstant(WasmType.Reference.EQ));
        var additionalFields = annotImplInfo.getStructure().getFields().size() - 2;
        constructor.getInitializers().addAll(Collections.nCopies(additionalFields, null));
        for (var method : annotCls.getMethods()) {
            var name = method.getName();
            var field = annotImpl.getField("$" + name);
            var fieldIndex = classInfoProvider.getFieldIndex(field.getReference());
            var value = annotation.getValue(name);
            if (value == null) {
                value = method.getAnnotationDefault();
            }
            constructor.getInitializers().set(fieldIndex, generateAnnotationValue(value, field.getType()));
        }
        return constructor;
    }

    private WasmExpression generateAnnotationValue(AnnotationValue value, ValueType type) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                return new WasmInt32Constant(value.getBoolean() ? 1 : 0);
            case AnnotationValue.CHAR:
                return new WasmInt32Constant(value.getChar());
            case AnnotationValue.BYTE:
                return new WasmInt32Constant(value.getByte());
            case AnnotationValue.SHORT:
                return new WasmInt32Constant(value.getShort());
            case AnnotationValue.INT:
                return new WasmInt32Constant(value.getInt());
            case AnnotationValue.LONG:
                return new WasmInt64Constant(value.getLong());
            case AnnotationValue.FLOAT:
                return new WasmFloat32Constant(value.getFloat());
            case AnnotationValue.DOUBLE:
                return new WasmFloat64Constant(value.getDouble());
            case AnnotationValue.STRING:
                return new WasmGetGlobal(stringProvider.getStringConstant(value.getString()).global);
            case AnnotationValue.LIST: {
                var util = new WasmGCGenerationUtil(classInfoProvider);
                var itemType = ((ValueType.Array) type).getItemType();
                return util.allocateArrayWithElements(itemType, () -> value.getList()
                        .stream()
                        .map(elem -> generateAnnotationValue(elem, itemType))
                        .collect(Collectors.toList()));
            }
            case AnnotationValue.ANNOTATION:
                return generateAnnotation(value.getAnnotation());
            case AnnotationValue.ENUM: {
                var enumCls = classes.get(value.getEnumValue().getClassName());
                if (enumCls == null) {
                    return new WasmInt32Constant(0);
                }
                var index = 0;
                for (var field : enumCls.getFields()) {
                    if (field.hasModifier(ElementModifier.STATIC) && field.hasModifier(ElementModifier.ENUM)) {
                        if (field.getName().equals(value.getEnumValue().getFieldName())) {
                            break;
                        }
                        ++index;
                    }
                }
                return new WasmInt32Constant(index);
            }
            case AnnotationValue.CLASS: {
                var valueClassInfo = classInfoProvider.getClassInfo(value.getJavaClass());
                return new WasmGetGlobal(valueClassInfo.getPointer());
            }
            default:
                throw new IllegalArgumentException();
        }
    }
}
