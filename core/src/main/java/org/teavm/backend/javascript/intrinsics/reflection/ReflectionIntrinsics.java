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
package org.teavm.backend.javascript.intrinsics.reflection;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
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

public class ReflectionIntrinsics {
    private Map<MethodReference, Injector> injectors;
    private Map<MethodReference, Generator> generators;
    private ListableClassReaderSource classes;
    private ReflectionDependencyListener reflection;
    private DependencyInfo dependencies;

    public ReflectionIntrinsics(Map<MethodReference, Injector> injectors, Map<MethodReference, Generator> generators,
            ListableClassReaderSource classes, ReflectionDependencyListener reflection,
            DependencyInfo dependencies) {
        this.injectors = injectors;
        this.generators = generators;
        this.classes = classes;
        this.reflection = reflection;
        this.dependencies = dependencies;
    }

    public void apply() {
        var classGen = new ClassInfoGenerator();
        applyIntrinsics(ClassInfo.class, classGen, "newArrayInstance");
        generators.put(new MethodReference(ClassInfo.class, "newArrayInstance", int.class, Object.class), classGen);

        applyIntrinsics(ClassReflectionInfo.class, new ClassReflectionInfoGenerator(reflection, dependencies));
        applyIntrinsics(StringInfo.class, new StringInfoGenerator());
        applyIntrinsics(AnnotationInfo.class, new AnnotationInfoGenerator());
        applyIntrinsics(AnnotationConstructor.class, new AnnotationConstructorGenerator());
        applyIntrinsics(AnnotationValueArray.class, new AnnotationValueArrayGenerator());
        applyIntrinsics(DerivedClassInfo.class, new DerivedClassInfoGenerator());
        applyIntrinsics(FieldInfo.class, new FieldInfoGenerator());
        applyIntrinsics(FieldReflectionInfo.class, new FieldReflectionInfoGenerator());
        applyIntrinsics(MethodInfo.class, new MethodInfoGenerator());
        applyIntrinsics(MethodReflectionInfo.class, new MethodReflectionInfoGenerator());
        applyIntrinsics(TypeVariableInfo.class, new TypeVariableInfoGenerator());

        var genericTypeGen = new GenericTypeInfoGenerator();
        applyIntrinsics(GenericTypeInfo.class, genericTypeGen);
        applyIntrinsics(ParameterizedTypeInfo.class, genericTypeGen);
        applyIntrinsics(TypeVariableReference.class, genericTypeGen);
        applyIntrinsics(GenericArrayInfo.class, genericTypeGen);
        applyIntrinsics(WildcardTypeInfo.class, genericTypeGen);
        applyIntrinsics(RawTypeInfo.class, genericTypeGen);

        generators.put(new MethodReference(Object.class, "getClassInfo", ClassInfo.class), new ObjectGenerator());

        applyToAnnotationData();
    }

    private void applyToAnnotationData() {
        for (var className : classes.getClassNames()) {
            var cls = classes.get(className);
            if (Objects.equals(cls.getParent(), AnnotationData.class.getName())) {
                applyToAnnotationData(cls);
            }
        }
    }

    private void applyToAnnotationData(ClassReader cls) {
        var index = 0;
        for (var method : cls.getMethods()) {
            if (method.getName().equals("<init>")) {
                continue;
            }
            if (method.getName().equals("constructor")
                    && method.getResultType().isObject(AnnotationConstructor.class)) {
                injectors.put(method.getReference(), (ctx, m) -> {
                    var annotItfName = cls.getName().substring(0, cls.getName().length()
                            - AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX.length());
                    var annotImplName = annotItfName + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
                    var factory = new MethodReference(annotImplName, "create", ValueType.object(cls.getName()),
                            ValueType.object(annotImplName));
                    ctx.getWriter().appendMethod(factory);
                });
                continue;
            }
            var currentIndex = index;
            injectors.put(method.getReference(), (ctx, m) -> {
                ctx.writeExpr(ctx.getArgument(0), Precedence.MEMBER_ACCESS);
                ctx.getWriter().append("[").append(currentIndex).append("]");
            });
            ++index;
        }
    }

    private void applyIntrinsics(Class<?> cls, Injector generator, String... skip) {
        var clsReader = classes.get(cls.getName());
        if (clsReader == null) {
            return;
        }
        var skipSet = Set.of(skip);
        for (var method : clsReader.getMethods()) {
            if (method.hasModifier(ElementModifier.NATIVE) && !skipSet.contains(method.getName())) {
                injectors.put(method.getReference(), generator);
            }
        }
    }
}
