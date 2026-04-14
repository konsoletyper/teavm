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
package org.teavm.backend.wasm.intrinsics.reflection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.teavm.backend.wasm.generate.methods.WasmGCIntrinsicProvider;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.StringInfo;
import org.teavm.runtime.reflect.AnnotationConstructor;
import org.teavm.runtime.reflect.AnnotationData;
import org.teavm.runtime.reflect.AnnotationInfo;
import org.teavm.runtime.reflect.AnnotationValueArray;
import org.teavm.runtime.reflect.ClassInfo;
import org.teavm.runtime.reflect.ClassReflectionInfo;
import org.teavm.runtime.reflect.DerivedClassInfo;
import org.teavm.runtime.reflect.FieldInfo;
import org.teavm.runtime.reflect.FieldReflectionInfo;
import org.teavm.runtime.reflect.GenericArrayInfo;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.runtime.reflect.MethodReflectionInfo;
import org.teavm.runtime.reflect.ParameterizedTypeInfo;
import org.teavm.runtime.reflect.RawTypeInfo;
import org.teavm.runtime.reflect.TypeVariableInfo;
import org.teavm.runtime.reflect.TypeVariableReference;
import org.teavm.runtime.reflect.WildcardTypeInfo;

public class ReflectionIntrinsics implements WasmGCIntrinsicProvider {
    private ListableClassReaderSource classes;
    private Map<String, WasmGCIntrinsic> intrinsics = new HashMap<>();

    public ReflectionIntrinsics(ListableClassReaderSource classes, ReflectionDependencyListener reflection) {
        this.classes = classes;
        intrinsics.put(ClassInfo.class.getName(), new ClassInfoIntrinsic(reflection));
        intrinsics.put(ClassReflectionInfo.class.getName(), new ClassReflectionInfoIntrinsic());
        intrinsics.put(StringInfo.class.getName(), new StringInfoIntrinsic());
        intrinsics.put(AnnotationConstructor.class.getName(), new AnnotationConstructorIntrinsic());
        intrinsics.put(AnnotationInfo.class.getName(), new AnnotationInfoIntrinsic());
        intrinsics.put(DerivedClassInfo.class.getName(), new DerivedClassInfoIntrinsic());
        intrinsics.put(AnnotationValueArray.class.getName(), new AnnotationValueArrayIntrinsic());
        intrinsics.put(FieldInfo.class.getName(), new FieldInfoIntrinsic());
        intrinsics.put(FieldReflectionInfo.class.getName(), new FieldReflectionInfoIntrinsic());
        intrinsics.put(MethodInfo.class.getName(), new MethodInfoIntrinsic());
        intrinsics.put(MethodReflectionInfo.class.getName(), new MethodReflectionInfoIntrinsic());
        intrinsics.put(TypeVariableInfo.class.getName(), new TypeVariableInfoIntrinsic());
        intrinsics.put(GenericTypeInfo.class.getName(), new GenericTypeInfoIntrinsic());
        intrinsics.put(ParameterizedTypeInfo.class.getName(), new ParameterizedTypeInfoIntrinsic());
        intrinsics.put(TypeVariableReference.class.getName(), new TypeVariableReferenceIntrinsic());
        intrinsics.put(GenericArrayInfo.class.getName(), new GenericArrayInfoIntrinsic());
        intrinsics.put(WildcardTypeInfo.class.getName(), new WildcardTypeInfoIntrinsic());
        intrinsics.put(RawTypeInfo.class.getName(), new RawTypeInfoIntrinsic());

        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            if (Objects.equals(cls.getParent(), AnnotationData.class.getName())
                    && cls.getName().endsWith(AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX)) {
                var origClassName = className.substring(0, className.length()
                        - AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX.length());
                var intrinsic = new AnnotationDataIntrinsic(origClassName);
                intrinsics.put(className, intrinsic);
            }
        }
    }

    @Override
    public WasmGCIntrinsic get(MethodReference method) {
        var intrinsic = intrinsics.get(method.getClassName());
        if (intrinsic == null) {
            return null;
        }
        var cls = classes.get(method.getClassName());
        if (cls == null) {
            return null;
        }
        var methodReader = cls.getMethod(method.getDescriptor());
        if (methodReader == null || !methodReader.hasModifier(ElementModifier.NATIVE)) {
            return null;
        }
        return intrinsic;
    }
}
