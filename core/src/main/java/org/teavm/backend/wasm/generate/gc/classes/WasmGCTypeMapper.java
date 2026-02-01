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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.gc.reflection.ReflectionTypes;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmPackedType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.runtime.reflect.AnnotationData;

public class WasmGCTypeMapper {
    private ClassReaderSource classes;
    private WasmGCClassInfoProvider classInfoProvider;
    private WasmFunctionTypes functionTypes;
    private ReflectionTypes reflectionTypes;
    private List<WasmGCCustomTypeMapper> customTypeMappers;
    private Map<String, WasmType> typeCache = new HashMap<>();

    WasmGCTypeMapper(ClassReaderSource classes, WasmGCClassInfoProvider classInfoProvider,
            WasmFunctionTypes functionTypes, ReflectionTypes reflectionTypes) {
        this.classes = classes;
        this.classInfoProvider = classInfoProvider;
        this.functionTypes = functionTypes;
        this.reflectionTypes = reflectionTypes;
    }

    void setCustomTypeMappers(List<WasmGCCustomTypeMapper> customTypeMappers) {
        this.customTypeMappers = List.copyOf(customTypeMappers);
    }

    public WasmStorageType mapStorageType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                case BOOLEAN:
                    return WasmStorageType.packed(WasmPackedType.INT8);
                case SHORT:
                case CHARACTER:
                    return WasmStorageType.packed(WasmPackedType.INT16);
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
        } else {
            return mapType(type).asStorage();
        }
    }

    public WasmType mapType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHARACTER:
                case INTEGER:
                    return WasmType.INT32;
                case LONG:
                    return WasmType.INT64;
                case FLOAT:
                    return WasmType.FLOAT32;
                case DOUBLE:
                    return WasmType.FLOAT64;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Void) {
            return null;
        } else if (type instanceof ValueType.Object) {
            return mapClassType(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Array) {
            var degree = 0;
            while (type instanceof ValueType.Array) {
                type = ((ValueType.Array) type).getItemType();
                ++degree;
            }
            if (type instanceof ValueType.Object) {
                var className = ((ValueType.Object) type).getClassName();
                var cls = classes.get(className);
                if (cls == null) {
                    type = ValueType.object("java.lang.Object");
                }
            }
            while (degree-- > 0) {
                type = ValueType.arrayOf(type);
            }
            return classInfoProvider.getClassInfo(type).getType();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private WasmType mapClassType(String className) {
        var result = typeCache.get(className);
        if (result == null) {
            for (var customMapper : customTypeMappers) {
                result = customMapper.map(className);
                if (result != null) {
                    break;
                }
            }
            if (result == null) {
                switch (className) {
                    case "org.teavm.interop.Address":
                        result = WasmType.INT32;
                        break;
                    case "org.teavm.runtime.reflect.ClassInfo":
                        result = reflectionTypes.classInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.ClassReflectionInfo":
                        result = reflectionTypes.classReflectionInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.DerivedClassInfo":
                        result = reflectionTypes.derivedClassInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.AnnotationInfo":
                        result = reflectionTypes.annotationInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.AnnotationData":
                        result = WasmType.STRUCT;
                        break;
                    case "org.teavm.runtime.reflect.AnnotationConstructor":
                        result = reflectionTypes.annotationInfo().constructorType().getReference();
                        break;
                    case "org.teavm.runtime.reflect.AnnotationValueArray":
                        result = WasmType.ARRAY;
                        break;
                    case "org.teavm.runtime.reflect.FieldInfo":
                        result = reflectionTypes.fieldInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.FieldReflectionInfo":
                        result = reflectionTypes.fieldReflectionInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.MethodInfo":
                        result = reflectionTypes.methodInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.MethodReflectionInfo":
                        result = reflectionTypes.methodReflectionInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.TypeVariableInfo":
                        result = reflectionTypes.typeVariableInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.GenericTypeInfo":
                        result = WasmType.STRUCT;
                        break;
                    case "org.teavm.runtime.reflect.ParameterizedTypeInfo":
                        result = reflectionTypes.parameterizedTypeInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.TypeVariableReference":
                        result = reflectionTypes.typeVariableReference().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.GenericArrayInfo":
                        result = reflectionTypes.genericArrayInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.WildcardTypeInfo":
                        result = reflectionTypes.wildcardTypeInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.reflect.RawTypeInfo":
                        result = reflectionTypes.derivedClassInfo().structure().getReference();
                        break;
                    case "org.teavm.runtime.StringInfo":
                        result = classInfoProvider.getClassInfo("java.lang.String").getType();
                        break;
                    default: {
                        var cls = classes.get(className);
                        if (cls == null) {
                            className = "java.lang.Object";
                        } else if (Objects.equals(cls.getParent(), AnnotationData.class.getName())) {
                            var annotClassName = cls.getName().substring(0, cls.getName().length()
                                    - AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX.length());
                            return classInfoProvider.reflectionTypes().annotationData(annotClassName)
                                    .structure().getReference();
                        }
                        var classInfo = classInfoProvider.getClassInfo(className);
                        if (classInfo.isHeapStructure()) {
                            result = WasmType.INT32;
                        } else {
                            result = classInfo.getType();
                        }
                    }
                }
                typeCache.put(className, result);
            }
        }
        return result;
    }

    public WasmFunctionType getFunctionType(WasmType receiverType, MethodDescriptor methodDesc) {
        var returnType = mapType(methodDesc.getResultType());
        var javaParamTypes = methodDesc.getParameterTypes();
        var paramTypes = new WasmType[javaParamTypes.length + 1];
        paramTypes[0] = receiverType;
        for (var i = 0; i < javaParamTypes.length; ++i) {
            paramTypes[i + 1] = mapType(javaParamTypes[i]);
        }
        return functionTypes.of(returnType, paramTypes);
    }

    public WasmFunctionType getFunctionType(String className, MethodDescriptor methodDesc) {
        return getFunctionType(classInfoProvider.getClassInfo(className).getType(), methodDesc);
    }
}
