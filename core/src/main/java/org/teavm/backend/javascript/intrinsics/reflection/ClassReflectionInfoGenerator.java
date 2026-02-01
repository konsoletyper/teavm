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

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;

public class ClassReflectionInfoGenerator implements Injector {
    private ReflectionDependencyListener reflection;
    private DependencyInfo dependencyInfo;
    private boolean annotationsGenerated;
    private boolean metadataGenerated;
    private boolean fieldAnnotationsRequired;
    private boolean fieldGenericTypeRequired;
    private boolean methodAnnotationsRequired;
    private boolean methodTypeParametersRequired;
    private boolean methodGenericReturnTypeRequired;
    private boolean methodGenericParamTypesRequired;

    public ClassReflectionInfoGenerator(ReflectionDependencyListener reflection,
            DependencyInfo dependencyInfo) {
        this.reflection = reflection;
        this.dependencyInfo = dependencyInfo;
        fieldAnnotationsRequired = dependencyInfo.getMethod(new MethodReference(Field.class, "getDeclaredAnnotations",
                Annotation[].class)) != null;
        fieldGenericTypeRequired = dependencyInfo.getMethod(new MethodReference(Field.class, "getGenericType",
                Type.class)) != null;
        methodAnnotationsRequired = dependencyInfo.getMethod(new MethodReference(Executable.class,
                "getDeclaredAnnotations", Annotation[].class)) != null;
        methodTypeParametersRequired = dependencyInfo.getMethod(new MethodReference(Executable.class,
                "getTypeParameters", TypeVariable[].class)) != null;
        methodGenericReturnTypeRequired = dependencyInfo.getMethod(new MethodReference(Method.class,
                "getGenericReturnType", Type.class)) != null;
        methodGenericParamTypesRequired = dependencyInfo.getMethod(new MethodReference(Executable.class,
                "getGenericParameterTypes", Type[].class)) != null;
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "annotationCount":
                generateAnnotations(context);
                writeArrayLength(context, "annotations");
                break;
            case "annotation":
                generateAnnotations(context);
                writeArrayGet(context, "annotations");
                break;
            case "fieldCount":
                generateMetadata(context);
                writeArrayLength(context, "fields");
                break;
            case "field":
                generateMetadata(context);
                writeArrayGet(context, "fields");
                break;
            case "methodCount":
                generateMetadata(context);
                writeArrayLength(context, "methods");
                break;
            case "method":
                generateMetadata(context);
                writeArrayGet(context, "methods");
                break;
            case "typeParameterCount":
                generateMetadata(context);
                writeArrayLength(context, "typeParameters");
                break;
            case "typeParameter":
                generateMetadata(context);
                writeArrayGet(context, "typeParameters");
                break;
        }
    }

    private void writeArrayLength(InjectorContext context, String name) {
        context.writeExpr(context.getArgument(0));
        context.getWriter().append("." + name + ".length");
    }

    private void writeArrayGet(InjectorContext context, String name) {
        context.writeExpr(context.getArgument(0));
        context.getWriter().append("." + name + "[");
        context.writeExpr(context.getArgument(1));
        context.getWriter().append("]");
    }

    private void generateAnnotations(InjectorContext context) {
        if (annotationsGenerated) {
            return;
        }
        annotationsGenerated = true;
        var methodDep = context.getDependencies()
                .getMethod(new MethodReference(Class.class, "getDeclaredAnnotations", Annotation[].class));
        if (methodDep == null) {
            return;
        }
        var classes = Arrays.stream(methodDep.getVariable(0).getClassValueNode().getTypes())
                .filter(t -> t instanceof ValueType.Object)
                .map(t -> ((ValueType.Object) t).getClassName())
                .map(n -> collectAnnotations(context.getClassSource(), n))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (classes.isEmpty()) {
            return;
        }
        var writer = context.getMetadataWriter();
        writer.appendFunction("$rt_classAnnotationMetadata").append("([");
        var first = true;
        for (var entry : classes) {
            if (!first) {
                writer.append(",").ws();
            }
            first = false;
            writer.appendClass(entry.cls.getName()).append(",").ws();
            generateAnnotations(writer, context.getClassSource(), entry.annotations);
        }
        writer.append("]);").newLine();
    }

    private void generateAnnotations(SourceWriter writer, ClassReaderSource classes,
            List<AnnotationReader> annotations) {
        writer.append("[");
        var first = true;
        for (var annot : annotations) {
            if (!first) {
                writer.append(",").ws();
            }
            first = false;
            AnnotationsGenerator.generate(writer, classes, annot);
        }
        writer.append("]");
    }

    private ClassWithAnnotations collectAnnotations(ClassReaderSource classes, String className) {
        var cls = classes.get(className);
        if (cls == null) {
            return null;
        }
        var annotations = AnnotationGenerationHelper.collectRuntimeAnnotations(classes, cls.getAnnotations().all());
        return annotations.isEmpty() ? null : new ClassWithAnnotations(cls, annotations);
    }

    private static class ClassWithAnnotations {
        private ClassReader cls;
        private List<AnnotationReader> annotations;

        ClassWithAnnotations(ClassReader cls, List<AnnotationReader> annotations) {
            this.cls = cls;
            this.annotations = annotations;
        }
    }

    private void generateMetadata(InjectorContext context) {
        if (metadataGenerated) {
            return;
        }
        metadataGenerated = true;

        var writer = context.getMetadataWriter();
        writer.appendFunction("$rt_reflection").append("([").indent().softNewLine();
        var first = true;
        var classesWithReflectableTypeParameters = getClassesWithReflectableTypeParameters();
        for (var className : context.getClassSource().getClassNames()) {
            if (!reflection.getClassesWithReflectableFields().contains(className)
                    && !reflection.getClassesWithReflectableMethods().contains(className)
                    && !classesWithReflectableTypeParameters.contains(className)) {
                continue;
            }
            var cls = context.getClassSource().get(className);
            if (!first) {
                writer.append(",").ws().softNewLine();
            }
            first = false;
            writer.appendClass(cls.getName()).append(",").ws().append("{").indent();
            var needsComma = false;
            var genericParams = cls.getGenericParameters();
            if (classesWithReflectableTypeParameters.contains(className) && genericParams != null
                    && genericParams.length > 0) {
                generateTypeParameters(context, cls, null, cls.getGenericParameters());
                needsComma = true;
            }
            if (reflection.getClassesWithReflectableFields().contains(className)) {
                if (needsComma) {
                    writer.append(',').ws();
                }
                generateFields(context, cls);
                needsComma = true;
            }
            if (reflection.getClassesWithReflectableMethods().contains(className)) {
                if (needsComma) {
                    writer.append(',').ws();
                }
                generateMethods(context, cls);
            }
            writer.softNewLine().outdent().append("}");
        }
        writer.outdent().append("]);").newLine();
    }

    private Set<String> getClassesWithReflectableTypeParameters() {
        var methodDep = dependencyInfo.getMethod(new MethodReference(Class.class, "getTypeParameters",
                TypeVariable[].class));
        if (methodDep == null) {
            return Collections.emptySet();
        }
        var result = new LinkedHashSet<String>();
        for (var type : methodDep.getVariable(0).getClassValueNode().getTypes()) {
            if (type instanceof ValueType.Object) {
                result.add(((ValueType.Object) type).getClassName());
            }
        }
        return result;
    }

    private void generateTypeParameters(InjectorContext context, ClassReader cls, MethodReader method,
            GenericTypeParameter[] typeParameters) {
        var writer = context.getMetadataWriter();
        writer.append("p:").ws().append('[').indent();
        var first = true;
        for (var typeParam : typeParameters) {
            if (!first) {
                writer.append(",");
            }
            writer.softNewLine();
            first = false;
            writer.append('[');
            writer.append('"').append(RenderingUtil.escapeString(typeParam.getName())).append("\"");
            var bounds = typeParam.extractAllBounds();
            if (!bounds.isEmpty()) {
                writer.append(',').ws().append('[');
                generateGenericType(context, cls, method, bounds.get(0));
                for (var i = 1; i <  bounds.size(); i++) {
                    writer.append(',').ws();
                    generateGenericType(context, cls, method, bounds.get(i));
                }
                writer.append(']');
            }
            writer.append(']');
        }
        writer.softNewLine().outdent().append("]");
    }

    private void generateFields(InjectorContext context, ClassReader cls) {
        var writer = context.getMetadataWriter();
        writer.append("f:").ws().append('[').indent();
        var first = true;
        for (var fieldName : reflection.getAccessibleFields(cls.getName())) {
            if (!first) {
                writer.append(",");
            }
            writer.softNewLine();
            first = false;
            var field = cls.getField(fieldName);
            writer.append('[');
            writer.append('"').append(RenderingUtil.escapeString(field.getName())).append("\",").ws();
            writer.append(ElementModifier.asModifiersInfo(field.readModifiers(), field.getLevel())).append(",").ws();
            RenderingUtil.typeToClsString(writer, field.getType());
            writer.append(',').ws();
            renderGetter(writer, field);
            writer.append(',').ws();
            renderSetter(writer, field);
            var annotations = fieldAnnotationsRequired
                    ? AnnotationGenerationHelper.collectRuntimeAnnotations(context.getClassSource(),
                        field.getAnnotations().all())
                    : Collections.<AnnotationReader>emptyList();
            var genericType = fieldGenericTypeRequired ? field.getGenericType() : null;
            if (!annotations.isEmpty() || genericType != null) {
                writer.append(',').ws().append('{');
                var needsComma = false;
                if (!annotations.isEmpty()) {
                    writer.append("a:").ws();
                    generateAnnotations(writer, context.getClassSource(), annotations);
                    needsComma = true;
                }
                if (genericType != null) {
                    if (needsComma) {
                        writer.append(',').ws();
                    }
                    writer.append("t:").ws();
                    generateGenericType(context, cls, null, genericType);
                }
                writer.append('}');
            }
            writer.append(']');
        }
        writer.softNewLine().outdent().append("]");
    }

    private void renderGetter(SourceWriter writer, FieldReader field) {
        if (!reflection.isRead(field.getReference())) {
            writer.append("0");
            return;
        }
        writer.append("o").sameLineWs().append("=>").ws();
        fieldAccess(writer, field);
    }

    private void renderSetter(SourceWriter writer, FieldReader field) {
        if (!reflection.isWritten(field.getReference())) {
            writer.append("0");
            return;
        }
        writer.append("(o,").sameLineWs().append("v)").sameLineWs().append("=>").ws();
        fieldAccess(writer, field);
        writer.ws().append('=').ws().append("v");
    }

    private void fieldAccess(SourceWriter writer, FieldReader field) {
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.appendStaticField(field.getReference());
        } else {
            writer.append("o.").appendField(field.getReference());
        }
    }

    private void generateMethods(InjectorContext context, ClassReader cls) {
        var writer = context.getMetadataWriter();
        writer.append("m:").ws().append('[').indent();
        var first = true;
        for (var methodDesc : reflection.getAccessibleMethods(cls.getName())) {
            if (!first) {
                writer.append(",");
            }
            writer.softNewLine();
            first = false;
            var method = cls.getMethod(methodDesc);
            writer.append('[');
            writer.append('"').append(RenderingUtil.escapeString(method.getName())).append("\",").ws();
            writer.append(ElementModifier.asModifiersInfo(method.readModifiers(), method.getLevel())).append(",").ws();
            RenderingUtil.typeToClsString(writer, method.getResultType());
            writer.append(',').ws();
            if (method.parameterCount() == 0) {
                writer.append("0");
            } else {
                writer.append('[');
                RenderingUtil.typeToClsString(writer, method.parameterType(0));
                for (var i = 1; i < method.parameterCount(); ++i) {
                    writer.append(",").ws();
                    RenderingUtil.typeToClsString(writer, method.parameterType(i));
                }
                writer.append(']');
            }
            writer.append(',').ws();
            if (method.hasModifier(ElementModifier.STATIC) || method.getLevel() == AccessLevel.PRIVATE
                    || method.getName().equals("<init>")) {
                if (reflection.isCalled(method.getReference())) {
                    writer.appendMethod(method.getReference());
                } else {
                    writer.append("0");
                }
            } else {
                writer.append("o").sameLineWs().append("=>").ws().append("o.").appendVirtualMethod(methodDesc);
            }

            var annotations = methodAnnotationsRequired
                    ? AnnotationGenerationHelper.collectRuntimeAnnotations(context.getClassSource(),
                        method.getAnnotations().all())
                    : Collections.<AnnotationReader>emptyList();
            var typeParameters = methodTypeParametersRequired ? method.getTypeParameters() : null;
            var genericReturnType = methodGenericReturnTypeRequired && !method.getName().equals("<init>")
                    ? method.getGenericResultType() : null;
            var genericParamTypes = methodGenericParamTypesRequired ? method.getGenericParameterTypes() : null;
            if (!annotations.isEmpty()
                    || (typeParameters != null && typeParameters.length > 0)
                    || genericReturnType != null
                    || genericParamTypes != null && genericParamTypes.length > 0) {
                writer.append(',').ws().append('{');
                var needsComma = false;
                if (!annotations.isEmpty()) {
                    writer.append("a:").ws();
                    generateAnnotations(writer, context.getClassSource(), annotations);
                    needsComma = true;
                }
                if (typeParameters != null && typeParameters.length > 0) {
                    if (needsComma) {
                        writer.append(',').ws();
                    }
                    generateTypeParameters(context, cls, method, typeParameters);
                    needsComma = true;
                }
                if (genericReturnType != null) {
                    if (needsComma) {
                        writer.append(',').ws();
                    }
                    writer.append("r:").ws();
                    generateGenericType(context, cls, method, genericReturnType);
                    needsComma = true;
                }
                if (genericParamTypes != null && genericParamTypes.length > 0) {
                    if (needsComma) {
                        writer.append(',').ws();
                    }
                    writer.append("s:").ws().append('[');
                    generateGenericType(context, cls, method, genericParamTypes[0]);
                    for (var j = 1; j < genericParamTypes.length; ++j) {
                        writer.append(',').ws();
                        generateGenericType(context, cls, method, genericParamTypes[j]);
                    }
                    writer.append(']');
                }
                writer.append('}');
            }
            writer.append(']');
        }
        writer.softNewLine().outdent().append("]");
    }

    private void generateGenericType(InjectorContext context, ClassReader cls, MethodReader method,
            GenericValueType type) {
        var writer = context.getMetadataWriter();
        writer.append('[');
        if (type.canBeRepresentedAsRaw()) {
            writer.append("6,").ws();
            RenderingUtil.typeToClsString(writer, type.asValueType());
        } else if (type instanceof GenericValueType.Object) {
            var paramType = (GenericValueType.Object) type;
            writer.append("0,").ws().appendClass(paramType.getFullClassName());
            if (paramType.getParent() != null || paramType.getArguments().length > 0) {
                writer.append(",").ws();
                writer.append('[');
                var first = true;
                for (var arg : paramType.getArguments()) {
                    if (!first) {
                        writer.append(",").ws();
                    }
                    first = false;
                    generateGenericType(context, cls, method, arg);
                }
                writer.append(']');
                if (paramType.getParent() != null) {
                    writer.append(",").ws();
                    generateGenericType(context, cls, method, paramType.getParent());
                }
            }
        } else if (type instanceof GenericValueType.Variable) {
            writer.append("1,").ws();
            generateTypeVarRef(context, cls, method, (GenericValueType.Variable) type);
        } else if (type instanceof GenericValueType.Array) {
            writer.append("2,").ws();
            generateGenericType(context, cls, method, ((GenericValueType.Array) type).getItemType());
        }
        writer.append(']');
    }

    private void generateTypeVarRef(InjectorContext context, ClassReader cls, MethodReader method,
            GenericValueType.Variable varRef) {
        var level = 0;
        var writer = context.getMetadataWriter();
        if (method != null) {
            var params = method.getTypeParameters();
            for (var i = 0; i < params.length; ++i) {
                if (params[i].getName().equals(varRef.getName())) {
                    writer.append(i);
                    return;
                }
            }
            ++level;
        }
        while (cls != null) {
            var params = cls.getGenericParameters();
            if (params != null && params.length > 0) {
                for (var i = 0; i < params.length; ++i) {
                    if (params[i].getName().equals(varRef.getName())) {
                        writer.append(i);
                        if (level > 0) {
                            writer.append(",").ws().append(level);
                        }
                        return;
                    }
                }
            }
            ++level;
            var ownerName = cls.getOwnerName();
            cls = ownerName != null ? context.getClassSource().get(ownerName) : null;
        }
    }

    private void generateGenericType(InjectorContext context, ClassReader cls, MethodReader method,
            GenericValueType.Argument arg) {
        var writer = context.getMetadataWriter();
        switch (arg.getKind()) {
            case COVARIANT:
                writer.append("[3,").ws();
                generateGenericType(context, cls, method, arg.getValue());
                writer.append(']');
                break;
            case CONTRAVARIANT:
                writer.append("[4,").ws();
                generateGenericType(context, cls, method, arg.getValue());
                writer.append(']');
                break;
            case INVARIANT:
                generateGenericType(context, cls, method, arg.getValue());
                break;
            case ANY:
                writer.append("[5]");
                break;
        }
    }
}
