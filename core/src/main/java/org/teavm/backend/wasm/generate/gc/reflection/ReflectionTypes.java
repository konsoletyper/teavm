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
package org.teavm.backend.wasm.generate.gc.reflection;

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.reflection.AnnotationGenerationHelper;

public class ReflectionTypes {
    private WasmGCNameProvider names;
    private WasmModule module;
    private DependencyInfo dependency;
    private ClassReaderSource classes;
    private WasmFunctionTypes functionTypes;
    private WasmGCClassInfoProvider classInfoProvider;
    private BaseWasmFunctionRepository functions;
    private ClassMetadataRequirements metadataRequirements;

    private ClassInfoStruct classInfoStruct;
    private ClassReflectionInfoStruct classReflectionInfoStruct;
    private AnnotationInfoStruct annotationInfoStruct;
    private DerivedClassInfoStruct derivedClassInfoStruct;
    private FieldInfoStruct fieldInfoStruct;
    private FieldReflectionInfoStruct fieldReflectionInfoStruct;
    private MethodInfoStruct methodInfoStruct;
    private MethodReflectionInfoStruct methodReflectionInfoStruct;
    private TypeVariableInfoStruct typeVariableInfoStruct;
    private ParameterizedTypeInfoStruct parameterizedTypeInfoStruct;
    private TypeVariableReferenceStruct typeVariableReferenceStruct;
    private GenericArrayInfoStruct genericArrayInfoStruct;
    private WildcardTypeInfoStruct wildcardTypeInfoStruct;
    private Map<String, AnnotationDataStruct> annotationDataStructMap = new HashMap<>();
    private Map<WasmStorageType, WasmArray> annotationTypesForArrays = new HashMap<>();

    public ReflectionTypes(WasmGCNameProvider names, WasmModule module, DependencyInfo dependency,
            ClassReaderSource classes, WasmFunctionTypes functionTypes, WasmGCClassInfoProvider classInfoProvider,
            BaseWasmFunctionRepository functions, ClassMetadataRequirements metadataRequirements) {
        this.names = names;
        this.module = module;
        this.dependency = dependency;
        this.classes = classes;
        this.functionTypes = functionTypes;
        this.classInfoProvider = classInfoProvider;
        this.functions = functions;
        this.metadataRequirements = metadataRequirements;
    }

    public ClassInfoStruct classInfo() {
        if (classInfoStruct == null) {
            classInfoStruct = new ClassInfoStruct(names, module, functionTypes, classInfoProvider,
                    functions, metadataRequirements, dependency, this);
        }
        return classInfoStruct;
    }

    public ClassReflectionInfoStruct classReflectionInfo() {
        if (classReflectionInfoStruct == null) {
            classReflectionInfoStruct = new ClassReflectionInfoStruct(names, module, dependency,
                    metadataRequirements, this);
        }
        return classReflectionInfoStruct;
    }

    public DerivedClassInfoStruct derivedClassInfo() {
        if (derivedClassInfoStruct == null) {
            derivedClassInfoStruct = new DerivedClassInfoStruct(names, module, this);
        }
        return derivedClassInfoStruct;
    }

    public AnnotationInfoStruct annotationInfo() {
        if (annotationInfoStruct == null) {
            annotationInfoStruct = new AnnotationInfoStruct(names, module, functionTypes, classInfoProvider);
        }
        return annotationInfoStruct;
    }

    public AnnotationDataStruct annotationData(String annotationClassName) {
        return annotationDataStructMap.computeIfAbsent(annotationClassName, k -> {
            return new AnnotationDataStruct(k, k + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX, names,
                    module, classes, functions, classInfoProvider, this);
        });
    }

    public FieldInfoStruct fieldInfo() {
        if (fieldInfoStruct == null) {
            fieldInfoStruct = new FieldInfoStruct(names, module, functionTypes, dependency, classInfoProvider);
        }
        return fieldInfoStruct;
    }

    public FieldReflectionInfoStruct fieldReflectionInfo() {
        if (fieldReflectionInfoStruct == null) {
            fieldReflectionInfoStruct = new FieldReflectionInfoStruct(names, module, dependency, classInfoProvider);
        }
        return fieldReflectionInfoStruct;
    }

    public MethodInfoStruct methodInfo() {
        if (methodInfoStruct == null) {
            methodInfoStruct = new MethodInfoStruct(names, module, functionTypes, dependency, classInfoProvider);
        }
        return methodInfoStruct;
    }

    public MethodReflectionInfoStruct methodReflectionInfo() {
        if (methodReflectionInfoStruct == null) {
            methodReflectionInfoStruct = new MethodReflectionInfoStruct(names, module, dependency, classInfoProvider);
        }
        return methodReflectionInfoStruct;
    }

    public TypeVariableInfoStruct typeVariableInfo() {
        if (typeVariableInfoStruct == null) {
            typeVariableInfoStruct = new TypeVariableInfoStruct(names, module, dependency, classInfoProvider);
        }
        return typeVariableInfoStruct;
    }

    public ParameterizedTypeInfoStruct parameterizedTypeInfo() {
        if (parameterizedTypeInfoStruct == null) {
            parameterizedTypeInfoStruct = new ParameterizedTypeInfoStruct(names, module, dependency, this);
        }
        return parameterizedTypeInfoStruct;
    }

    public TypeVariableReferenceStruct typeVariableReference() {
        if (typeVariableReferenceStruct == null) {
            typeVariableReferenceStruct = new TypeVariableReferenceStruct(names, module);
        }
        return typeVariableReferenceStruct;
    }

    public GenericArrayInfoStruct genericArrayInfo() {
        if (genericArrayInfoStruct == null) {
            genericArrayInfoStruct = new GenericArrayInfoStruct(names, module);
        }
        return genericArrayInfoStruct;
    }

    public WildcardTypeInfoStruct wildcardTypeInfo() {
        if (wildcardTypeInfoStruct == null) {
            wildcardTypeInfoStruct = new WildcardTypeInfoStruct(names, module);
        }
        return wildcardTypeInfoStruct;
    }

    public WasmStorageType typeForAnnotation(ValueType type) {
        return typeForAnnotation(type, false);
    }

    public WasmStorageType typeForAnnotation(ValueType type, boolean array) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return WasmStorageType.INT8;
                case SHORT:
                case CHARACTER:
                    return WasmStorageType.INT16;
                case INTEGER:
                    return WasmType.INT32.asStorage();
                case LONG:
                    return WasmType.INT64.asStorage();
                case FLOAT:
                    return WasmType.FLOAT32.asStorage();
                case DOUBLE:
                    return WasmType.FLOAT64.asStorage();
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Array) {
            var itemType = typeForAnnotation(((ValueType.Array) type).getItemType(), true);
            return arrayTypeOf(itemType).getReference().asStorage();
        } else if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            switch (className) {
                case "java.lang.Class":
                    return derivedClassInfo().structure().getReference().asStorage();
                case "java.lang.String":
                    return classInfoProvider.getClassInfo("java.lang.String").getType().asStorage();
                default:
                    var cls = classes.get(className);
                    if (cls == null) {
                        return WasmType.STRUCT.asStorage();
                    }
                    if (cls.hasModifier(ElementModifier.ENUM)) {
                        return WasmStorageType.INT16;
                    } else {
                        return array
                                ? WasmType.STRUCT.asStorage()
                                : annotationData(className).structure().getReference().asStorage();
                    }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public WasmArray arrayTypeOf(WasmStorageType type) {
        return annotationTypesForArrays.computeIfAbsent(type, item -> {
            var array = new WasmArray(null, item);
            module.types.add(array);
            return array;
        });
    }

    public WasmArray genericTypeArray() {
        return arrayTypeOf(WasmType.STRUCT.asStorage());
    }
}
