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
package org.teavm.backend.c.generate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.VirtualTable;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;
import org.teavm.runtime.reflect.ClassReflectionInfo;
import org.teavm.runtime.reflect.FieldReflectionInfo;
import org.teavm.runtime.reflect.GenericTypeInfo;
import org.teavm.runtime.reflect.MethodInfo;
import org.teavm.runtime.reflect.MethodReflectionInfo;
import org.teavm.runtime.reflect.ParameterInfo;
import org.teavm.runtime.reflect.TypeVariableInfo;

class ClassReflectionGenerator {
    private GenerationContext context;
    private CodeWriter writer;
    private IncludeManager includes;
    private ReflectionDependencyListener reflection;
    private Collection<ValueType> types;
    private MethodConvertersGenerator methodConvertersGenerator;
    private boolean needMethodAnnotations;
    private boolean needMethodParamAnnotations;
    private boolean needCheckedExceptions;
    private boolean needFieldAnnotations;
    private boolean needFieldGenericType;
    private boolean needMethodGenericTypes;
    private boolean needMethodTypeParams;
    private boolean needTypeParameters;
    private boolean needBounds;
    private boolean needInnerClasses;
    private Set<String> innerClassesAccessed = new HashSet<>();

    ClassReflectionGenerator(GenerationContext context, ReflectionDependencyListener reflection,
            Collection<ValueType> types, MethodConvertersGenerator methodConvertersGenerator) {
        this.context = context;
        this.reflection = reflection;
        this.types = types;
        this.methodConvertersGenerator = methodConvertersGenerator;
        needMethodAnnotations = context.getDependencies().getMethod(
                new MethodReference(MethodReflectionInfo.class, "annotationCount", int.class)) != null;
        needMethodParamAnnotations = context.getDependencies().getMethod(
                new MethodReference(MethodReflectionInfo.class, "parameterInfoCount", int.class)) != null;
        needCheckedExceptions = context.getDependencies().getMethod(
                new MethodReference(MethodInfo.class, "checkedExceptionCount", int.class)) != null;
        needFieldAnnotations = context.getDependencies().getMethod(
                new MethodReference(FieldReflectionInfo.class, "annotationCount", int.class)) != null;
        needFieldGenericType = context.getDependencies().getMethod(
                new MethodReference(FieldReflectionInfo.class, "genericType", GenericTypeInfo.class)) != null;
        needMethodGenericTypes = context.getDependencies().getMethod(
                new MethodReference(MethodReflectionInfo.class, "genericReturnType", GenericTypeInfo.class)) != null
                || context.getDependencies().getMethod(
                new MethodReference(ParameterInfo.class, "genericType", GenericTypeInfo.class)) != null;
        needMethodTypeParams = context.getDependencies().getMethod(
                new MethodReference(MethodReflectionInfo.class, "typeParameterCount", int.class)) != null;
        needTypeParameters = context.getDependencies().getMethod(
                new MethodReference(ClassReflectionInfo.class, "typeParameterCount", int.class)) != null;
        needBounds = context.getDependencies().getMethod(
                new MethodReference(TypeVariableInfo.class, "boundCount", int.class)) != null;
        var classesMethod = context.getDependencies().getMethod(new MethodReference(Class.class,
                "getDeclaredClasses", Class[].class));
        if (classesMethod != null) {
            needInnerClasses = true;
            for (var type : classesMethod.getVariable(0).getClassValueNode().getTypes()) {
                if (type instanceof ValueType.Object) {
                    innerClassesAccessed.add(((ValueType.Object) type).getClassName());
                }
            }
        }
    }

    void prepare(CodeWriter writer, IncludeManager includes) {
        methodConvertersGenerator.startForClass(writer, includes);
    }

    void finish() {
        methodConvertersGenerator.endForClass();
    }

    void generate(CodeWriter writer, IncludeManager includes, ClassReader cls) {
        var annotations = extractAnnotations(cls);
        var fields = extractFields(cls);
        var methods = extractMethods(cls);
        var typeParameters = extractTypeParameters(cls);
        var innerClasses = extractInnerClasses(cls);
        if (annotations.isEmpty() && fields.isEmpty() && methods.isEmpty() && typeParameters.length == 0
                && innerClasses.isEmpty()) {
            writer.print("NULL");
            return;
        }

        this.writer = writer;
        this.includes = includes;

        includes.includePath("reflection.h");
        writer.println("&(TeaVM_ClassReflection) {").indent();

        var needComma = false;
        if (!annotations.isEmpty()) {
            writer.print(".annotations = ");
            generateAnnotations(annotations);
            needComma = true;
        }
        if (!fields.isEmpty()) {
            if (needComma) {
                writer.println(",");
            }
            writer.print(".fields = ");
            generateFields(cls, fields);
            needComma = true;
        }
        if (!methods.isEmpty()) {
            if (needComma) {
                writer.println(",");
            }
            writer.print(".methods = ");
            generateMethods(methods);
            needComma = true;
        }
        if (typeParameters.length > 0) {
            if (needComma) {
                writer.println(",");
            }
            writer.print(".typeParameters = ");
            generateTypeParameters(typeParameters, cls);
            needComma = true;
        }
        if (!innerClasses.isEmpty()) {
            if (needComma) {
                writer.println(",");
            }
            writer.print(".innerClasses = ");
            generateInnerClasses(innerClasses);
        }

        writer.println();
        writer.outdent().print("}");

        this.writer = null;
        this.includes = null;
    }

    private void generateFields(ClassReader cls, List<FieldReader> fields) {
        writer.print("(TeaVM_FieldInfoList*) &(struct { int32_t count; TeaVM_FieldInfo data[")
                .print(String.valueOf(fields.size())).println("];}) {").indent();
        writer.print(".count = ").print(String.valueOf(fields.size())).println(",");
        writer.print(".data = ").println("{").indent();
        generateField(cls, fields.get(0));
        for (var i = 1; i < fields.size(); ++i) {
            writer.println(",");
            generateField(cls, fields.get(i));
        }
        writer.println().outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateField(ClassReader cls, FieldReader field) {
        includes.addInclude("<stddef.h>");

        var names = context.getNames();
        writer.println("{").indent();

        var nameIndex = context.getStringPool().getStringIndex(field.getName());
        writer.print(".name = TEAVM_GET_STRING_ADDRESS(").print(String.valueOf(nameIndex)).println("),");

        var modifiers = ElementModifier.asModifiersInfo(field.readModifiers(), field.getLevel());
        writer.print(".modifiers = ").print(String.valueOf(modifiers)).println(",");

        var fieldAnnotations = needFieldAnnotations
                ? AnnotationGenerationHelper.collectRuntimeAnnotations(context.getClassSource(),
                        field.getAnnotations().all())
                : List.<AnnotationReader>of();
        var fieldGenericType = needFieldGenericType ? field.getGenericType() : null;
        if (fieldGenericType != null && fieldGenericType.canBeRepresentedAsRaw()
                && fieldGenericType.asValueType() != null
                && fieldGenericType.asValueType().equals(field.getType())) {
            fieldGenericType = null;
        }
        if (!fieldAnnotations.isEmpty() || fieldGenericType != null) {
            writer.print(".reflection = &(TeaVM_FieldReflectionInfo) {");
            var needReflectionComma = false;
            if (!fieldAnnotations.isEmpty()) {
                writer.print(" .annotations = ");
                generateAnnotations(fieldAnnotations);
                needReflectionComma = true;
            }
            if (fieldGenericType != null) {
                if (needReflectionComma) {
                    writer.print(", ");
                } else {
                    writer.print(" ");
                }
                writer.print(".genericType = ");
                generateGenericType(fieldGenericType, context.getClassSource().get(field.getOwnerName()), null);
            }
            writer.println(" },");
        }

        writer.print(".type = ");
        writeType(field.getType());
        writer.println(",");

        writer.print(".location = { ");
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.print(".memory = &")
                    .print(names.forStaticField(field.getReference()));
        } else {
            writer.print(".instance = (int16_t) offsetof(")
                    .print(names.forClass(cls.getName())).print(", ")
                    .print(names.forMemberField(field.getReference()))
                    .print(")");
        }
        writer.println(" }");
        writer.outdent().print("}");
    }

    private void generateMethods(List<MethodReader> methods) {
        writer.print("(TeaVM_MethodInfoList*) &(struct { int32_t count; TeaVM_MethodInfo data[")
                .print(String.valueOf(methods.size())).println("];}) {").indent();
        writer.print(".count = ").print(String.valueOf(methods.size())).println(",");
        writer.print(".data = ").println("{").indent();
        generateMethod(methods.get(0));
        for (var i = 1; i < methods.size(); ++i) {
            writer.println(",");
            generateMethod(methods.get(i));
        }
        writer.println().outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateMethod(MethodReader method) {
        includes.addInclude("<stddef.h>");

        writer.println("{").indent();

        var nameIndex = context.getStringPool().getStringIndex(method.getName());
        writer.print(".name = TEAVM_GET_STRING_ADDRESS(").print(String.valueOf(nameIndex)).println("),");

        var modifiers = ElementModifier.asModifiersInfo(method.readModifiers(), method.getLevel());
        writer.print(".modifiers = ").print(String.valueOf(modifiers)).println(",");

        writer.print(".returnType = ");
        writeType(method.getResultType());

        if (method.parameterCount() > 0) {
            writer.println(",");
            writer.print(".parameterTypes = ");
            writer.print("(TeaVM_ClassPtrList*) &(struct { int32_t count; TeaVM_ClassPtr data[")
                    .print(String.valueOf(method.parameterCount())).println("];}) {").indent();
            writer.print(".count = ").print(String.valueOf(method.parameterCount())).println(",");
            writer.print(".data = ").println("{").indent();
            for (var i = 0; i < method.parameterCount(); ++i) {
                if (i > 0) {
                    writer.println(",");
                }
                writeType(method.parameterType(i));
            }
            writer.println();
            writer.outdent().println("}");
            writer.outdent().print("}");
        }

        if (needCheckedExceptions) {
            var thrownTypes = method.getThrownTypes();
            if (thrownTypes != null && !thrownTypes.isEmpty()) {
                writer.println(",");
                writer.print(".checkedExceptionTypes = ");
                writer.print("(TeaVM_ClassRefList*) &(struct { int32_t count; TeaVM_Class* data[")
                        .print(String.valueOf(thrownTypes.size())).println("];}) {").indent();
                writer.print(".count = ").print(String.valueOf(thrownTypes.size())).println(",");
                writer.print(".data = ").println("{").indent();
                for (var i = 0; i < thrownTypes.size(); ++i) {
                    if (i > 0) {
                        writer.println(",");
                    }
                    var exType = ValueType.object(thrownTypes.get(i));
                    includes.includeType(exType);
                    types.add(exType);
                    writer.print("(TeaVM_Class*) &").print(context.getNames().forClassInstance(exType));
                }
                writer.println();
                writer.outdent().println("}");
                writer.outdent().print("}");
            }
        }

        if (needMethodAnnotations || needMethodParamAnnotations || needMethodGenericTypes || needMethodTypeParams) {
            List<AnnotationReader> methodAnnotations = needMethodAnnotations
                    ? AnnotationGenerationHelper.collectRuntimeAnnotations(
                            context.getClassSource(), method.getAnnotations().all())
                    : List.of();
            var paramAnnotations = needMethodParamAnnotations ? collectParamAnnotations(method) : null;
            var hasParamAnnotations = paramAnnotations != null && hasNonEmptyList(paramAnnotations);

            var cls = context.getClassSource().get(method.getOwnerName());
            var genericReturnType = needMethodGenericTypes ? method.getGenericResultType() : null;
            if (genericReturnType != null && genericReturnType.canBeRepresentedAsRaw()
                    && genericReturnType.asValueType() != null
                    && genericReturnType.asValueType().equals(method.getResultType())) {
                genericReturnType = null;
            }
            GenericValueType[] genericParamTypes = null;
            var hasGenericParams = false;
            if (needMethodGenericTypes) {
                genericParamTypes = method.getGenericParameterTypes();
                if (genericParamTypes != null) {
                    for (var i = 0; i < genericParamTypes.length; ++i) {
                        var pt = genericParamTypes[i];
                        if (!pt.canBeRepresentedAsRaw() || !pt.asValueType().equals(method.parameterType(i))) {
                            hasGenericParams = true;
                            break;
                        }
                    }
                    if (!hasGenericParams) {
                        genericParamTypes = null;
                    }
                }
            }
            var methodTypeParams = needMethodTypeParams ? method.getTypeParameters() : null;

            if (!methodAnnotations.isEmpty() || hasParamAnnotations || genericReturnType != null
                    || hasGenericParams || (methodTypeParams != null && methodTypeParams.length > 0)) {
                writer.println(",");
                writer.print(".reflection = &(TeaVM_MethodReflectionInfo) {").indent();
                var needReflectionComma = false;
                if (genericReturnType != null) {
                    writer.println();
                    writer.print(".genericReturnType = ");
                    generateGenericType(genericReturnType, cls, method);
                    needReflectionComma = true;
                }
                if (!methodAnnotations.isEmpty()) {
                    if (needReflectionComma) {
                        writer.println(",");
                    } else {
                        writer.println();
                    }
                    writer.print(".annotations = ");
                    generateAnnotations(methodAnnotations);
                    needReflectionComma = true;
                }
                if (hasParamAnnotations || hasGenericParams) {
                    if (needReflectionComma) {
                        writer.println(",");
                    } else {
                        writer.println();
                    }
                    writer.print(".parameterInfos = ");
                    generateParameterInfos(method, paramAnnotations, genericParamTypes, cls);
                    needReflectionComma = true;
                }
                if (methodTypeParams != null && methodTypeParams.length > 0) {
                    if (needReflectionComma) {
                        writer.println(",");
                    } else {
                        writer.println();
                    }
                    writer.print(".typeParameters = ");
                    generateTypeParameters(methodTypeParams, cls, method);
                }
                writer.println();
                writer.outdent().print("}");
            }
        }

        if (reflection.isCalled(method.getReference())) {
            writer.println(",");
            writer.println(".caller = &(TeaVM_MethodCaller) {").indent();
            var conv = methodConvertersGenerator.getSignatureFunction(!method.hasModifier(ElementModifier.STATIC),
                    method.getResultType(), method.getParameterTypes());
            writer.println(".converter = &" + conv + ",");
            writer.print(".functionRef = { ");
            if (method.hasModifier(ElementModifier.STATIC) || method.getLevel() == AccessLevel.PRIVATE
                    || method.getName().equals("<init>")) {
                writer.print(".directFnRef = &" + context.getNames().forMethod(method.getReference()));
            } else {
                var vt = context.getVirtualTableProvider().lookup(method.getOwnerName());
                String vtableClass = null;
                if (vt != null) {
                    VirtualTable containingVt = vt.findMethodContainer(method.getDescriptor());
                    if (containingVt != null) {
                        vtableClass = containingVt.getClassName();
                    }
                }
                String offset;
                if (vtableClass == null) {
                    offset = "0";
                } else {
                    includes.addInclude("<stddef.h>");
                    offset = "(int16_t) offsetof(" + context.getNames().forClassClass(vtableClass) + ","
                            + context.getNames().forVirtualMethod(method.getDescriptor()) + ")";
                }
                writer.print(".vtOffset = " + offset);
            }
            writer.print(" }");
            if (method.getName().equals("<init>")) {
                writer.println(",");
                writer.print(".forceDirect = true");
            }
            writer.println();
            writer.outdent().print("}");
        }
        writer.println();
        writer.outdent().print("}");
    }

    private List<List<AnnotationReader>> collectParamAnnotations(MethodReader method) {
        var paramAnnots = method.getParameterAnnotations();
        if (paramAnnots == null) {
            return null;
        }
        var result = new ArrayList<List<AnnotationReader>>();
        for (AnnotationContainerReader container : paramAnnots) {
            result.add(AnnotationGenerationHelper.collectRuntimeAnnotations(
                    context.getClassSource(), container.all()));
        }
        return result;
    }

    private boolean hasNonEmptyList(List<List<AnnotationReader>> lists) {
        for (var list : lists) {
            if (!list.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void generateParameterInfos(MethodReader method, List<List<AnnotationReader>> paramAnnotations,
            GenericValueType[] genericParamTypes, ClassReader contextClass) {
        var count = method.parameterCount();
        writer.print("(TeaVM_ParameterInfoList*) &(struct { int32_t count; TeaVM_ParameterInfo data[")
                .print(String.valueOf(count)).println("]; }) {").indent();
        writer.print(".count = ").print(String.valueOf(count)).println(",");
        writer.print(".data = ").println("{").indent();
        for (var i = 0; i < count; ++i) {
            if (i > 0) {
                writer.println(",");
            }
            var annots = paramAnnotations != null && i < paramAnnotations.size()
                    ? paramAnnotations.get(i) : List.<AnnotationReader>of();
            GenericValueType paramGenericType = null;
            if (genericParamTypes != null && i < genericParamTypes.length) {
                var pt = genericParamTypes[i];
                if (!pt.canBeRepresentedAsRaw() || !pt.asValueType().equals(method.parameterType(i))) {
                    paramGenericType = pt;
                }
            }
            writer.print("{");
            if (!annots.isEmpty()) {
                writer.print(" .annotations = ");
                generateAnnotations(annots);
            }
            if (paramGenericType != null) {
                writer.print(" .genericType = ");
                generateGenericType(paramGenericType, contextClass, method);
            }
            writer.print(" }");
        }
        writer.println();
        writer.outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateAnnotations(List<AnnotationReader> annotations) {
        writer.println("(TeaVM_AnnotationInfoList*) &(struct { int32_t count; "
                + "TeaVM_AnnotationInfo data[" + annotations.size() + "]; }) {").indent();
        writer.print(".count = ").print(String.valueOf(annotations.size())).println(",");
        writer.println(".data = {").indent();
        generateAnnotation(annotations.get(0));
        for (var i = 1; i < annotations.size(); ++i) {
            writer.println(",");
            generateAnnotation(annotations.get(i));
        }
        writer.println();
        writer.outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateAnnotation(AnnotationReader annotation) {
        var implClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
        var dataClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
        var ctor = new MethodReference(implClass, "create", ValueType.object(dataClass),
                ValueType.object(implClass));
        includes.includeClass(dataClass);
        includes.includeClass(ctor.getClassName());
        writer.println("{").indent();
        writer.print(".constructor = &").print(context.getNames().forMethod(ctor)).println(",");
        writer.print(".data = ");
        generateAnnotationData(annotation);
        writer.println();
        writer.outdent().print("}");
    }

    private void generateAnnotationData(AnnotationReader annotation) {
        var cls = context.getClassSource().get(annotation.getType());
        if (cls == null) {
            writer.print("NULL");
            return;
        }

        var dataClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
        includes.includeClass(dataClass);
        writer.print("&(").print(context.getNames().forClass(dataClass)).println(") {").indent();

        var first = true;
        for (var method : cls.getMethods()) {
            if (!first) {
                writer.println(",");
            }
            first = false;
            var fieldName = context.getNames().forMemberField(new FieldReference(dataClass, method.getName()));
            writer.print(".").print(fieldName).print(" = ");
            var value = annotation.getValue(method.getName());
            if (value == null) {
                value = method.getAnnotationDefault();
            }
            generateAnnotationValue(method.getResultType(), value);
        }
        writer.outdent().print("}");
    }

    private void generateAnnotationValue(ValueType type, AnnotationValue value) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                writer.print(value.getBoolean() ? "1" : "0");
                break;
            case AnnotationValue.BYTE:
                writer.print("(int8_t) ");
                writer.print(String.valueOf(value.getByte()));
                break;
            case AnnotationValue.SHORT:
                writer.print("(int16_t) ");
                writer.print(String.valueOf(value.getShort()));
                break;
            case AnnotationValue.CHAR:
                writer.print("(uint16_t) ");
                CodeGeneratorUtil.writeIntValue(writer, value.getInt());
                break;
            case AnnotationValue.INT:
                CodeGeneratorUtil.writeIntValue(writer, value.getInt());
                break;
            case AnnotationValue.LONG:
                CodeGeneratorUtil.writeValue(writer, value.getLong());
                break;
            case AnnotationValue.FLOAT:
                CodeGeneratorUtil.writeValue(writer, value.getFloat());
                break;
            case AnnotationValue.DOUBLE:
                CodeGeneratorUtil.writeValue(writer, value.getDouble());
                break;
            case AnnotationValue.STRING:
                writer.print("(TeaVM_Object**) TEAVM_GET_STRING_ADDRESS("
                        + context.getStringPool().getStringIndex(value.getString()) + ")");
                break;
            case AnnotationValue.CLASS:
                writeType(value.getJavaClass());
                break;
            case AnnotationValue.ENUM: {
                var enumCls = context.getClassSource().get(value.getEnumValue().getClassName());
                if (enumCls == null) {
                    writer.print("NULL");
                    break;
                }
                var index = 0;
                for (var field : enumCls.getFields()) {
                    if (field.hasModifier(ElementModifier.ENUM)) {
                        if (field.getName().equals(value.getEnumValue().getFieldName())) {
                            break;
                        }
                        ++index;
                    }
                }
                writer.print(String.valueOf(index));
                break;
            }
            case AnnotationValue.ANNOTATION:
                generateAnnotationData(value.getAnnotation());
                break;
            case AnnotationValue.LIST: {
                var itemType = ((ValueType.Array) type).getItemType();
                var list = value.getList();
                writer.print("(");
                ClassGenerator.generateAnnotationFieldType(context, includes, writer, type);
                writer.print(") &(struct { int32_t count; ");
                ClassGenerator.generateAnnotationFieldType(context, includes, writer, itemType);
                writer.println(" data[" + list.size() + "]; }) {").indent();
                writer.print(".count = ").print(String.valueOf(list.size())).println(",");
                writer.print(".data = {").indent();
                if (!list.isEmpty()) {
                    writer.println();
                    generateAnnotationValue(itemType, list.get(0));
                    for (var i = 1; i < list.size(); ++i) {
                        writer.println(",");
                        generateAnnotationValue(itemType, list.get(i));
                    }
                    writer.println();
                }
                writer.outdent().println("}");
                writer.outdent().print("}");
                break;
            }
        }
    }

    private void writeType(ValueType cls) {
        var degree = 0;
        while (cls instanceof ValueType.Array) {
            cls = ((ValueType.Array) cls).getItemType();
            degree++;
        }
        includes.includeType(cls);
        types.add(cls);
        writer.print("{ .baseClass = (TeaVM_Class*) &").print(context.getNames().forClassInstance(cls))
                .print(", .arrayDegree = ").print(String.valueOf(degree)).print(" }");
    }

    private List<AnnotationReader> extractAnnotations(ClassReader cls) {
        if (!context.getMetadataRequirements().hasGetAnnotations()) {
            return List.of();
        }
        var info = context.getMetadataRequirements().getInfo(cls.getName());
        if (info == null || !info.annotations()) {
            return List.of();
        }
        return AnnotationGenerationHelper.collectRuntimeAnnotations(context.getClassSource(),
                cls.getAnnotations().all());
    }

    private List<FieldReader> extractFields(ClassReader cls) {
        var accessibleFields = reflection.getAccessibleFields(cls.getName());
        if (accessibleFields == null || accessibleFields.isEmpty()) {
            return List.of();
        }
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);
        var fields = new ArrayList<FieldReader>();
        for (var field : cls.getFields()) {
            if (!accessibleFields.contains(field.getName())) {
                continue;
            }
            if (skipPrivates) {
                if (field.getLevel() == AccessLevel.PRIVATE || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            fields.add(field);
        }
        return fields;
    }

    private List<MethodReader> extractMethods(ClassReader cls) {
        var accessibleMethods = reflection.getAccessibleMethods(cls.getName());
        if (accessibleMethods == null || accessibleMethods.isEmpty()) {
            return List.of();
        }
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);
        var methods = new ArrayList<MethodReader>();
        for (var method : cls.getMethods()) {
            if (!accessibleMethods.contains(method.getDescriptor())) {
                continue;
            }
            if (skipPrivates) {
                if (method.getLevel() == AccessLevel.PRIVATE || method.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            methods.add(method);
        }
        return methods;
    }

    private GenericTypeParameter[] extractTypeParameters(ClassReader cls) {
        if (!needTypeParameters) {
            return new org.teavm.model.GenericTypeParameter[0];
        }
        var params = cls.getGenericParameters();
        return params != null ? params : new org.teavm.model.GenericTypeParameter[0];
    }

    private List<String> extractInnerClasses(ClassReader cls) {
        if (!needInnerClasses || !innerClassesAccessed.contains(cls.getName())) {
            return List.of();
        }
        var result = new ArrayList<String>();
        for (var innerCls : cls.getInnerClasses()) {
            if (context.getDependencies().getClass(innerCls) != null) {
                result.add(innerCls);
            }
        }
        return result;
    }

    private void generateInnerClasses(List<String> innerClasses) {
        writer.print("(TeaVM_ClassRefList*) &(struct { int32_t count; TeaVM_Class* data[")
                .print(String.valueOf(innerClasses.size())).println("];}) {").indent();
        writer.print(".count = ").print(String.valueOf(innerClasses.size())).println(",");
        writer.print(".data = ").println("{").indent();
        for (var i = 0; i < innerClasses.size(); ++i) {
            if (i > 0) {
                writer.println(",");
            }
            var innerType = ValueType.object(innerClasses.get(i));
            includes.includeType(innerType);
            types.add(innerType);
            writer.print("(TeaVM_Class*) &").print(context.getNames().forClassInstance(innerType));
        }
        writer.println();
        writer.outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateTypeParameters(org.teavm.model.GenericTypeParameter[] params, ClassReader cls) {
        generateTypeParameters(params, cls, null);
    }

    private void generateTypeParameters(org.teavm.model.GenericTypeParameter[] params, ClassReader cls,
            MethodReader method) {
        writer.print("(TeaVM_TypeVariableInfoList*) &(struct { int32_t count; TeaVM_TypeVariableInfo data[")
                .print(String.valueOf(params.length)).println("]; }) {").indent();
        writer.print(".count = ").print(String.valueOf(params.length)).println(",");
        writer.print(".data = ").println("{").indent();
        for (var i = 0; i < params.length; ++i) {
            if (i > 0) {
                writer.println(",");
            }
            var param = params[i];
            var nameIndex = context.getStringPool().getStringIndex(param.getName());
            writer.print("{ .name = TEAVM_GET_STRING_ADDRESS(").print(String.valueOf(nameIndex)).print(")");
            if (needBounds) {
                var bounds = param.extractAllBounds();
                if (!bounds.isEmpty()) {
                    writer.println(",");
                    writer.print(".boundCount = ").print(String.valueOf(bounds.size())).println(",");
                    writer.print(".bounds = ");
                    generateBounds(bounds, cls, method);
                }
            }
            writer.print(" }");
        }
        writer.println();
        writer.outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateBounds(List<GenericValueType.Reference> bounds, ClassReader contextClass,
            MethodReader contextMethod) {
        writer.print("(TeaVM_GenericTypeInfo*[").print(String.valueOf(bounds.size())).print("]) {").indent();
        for (var i = 0; i < bounds.size(); ++i) {
            writer.println();
            if (i > 0) {
                writer.println(",");
            }
            generateGenericType(bounds.get(i), contextClass, contextMethod);
        }
        writer.println();
        writer.outdent().print("}");
    }

    private void generateGenericType(GenericValueType type, ClassReader contextClass, MethodReader contextMethod) {
        includes.includePath("reflection.h");
        if (type instanceof GenericValueType.Object objectType) {
            var args = objectType.getArguments();
            if (args.length == 0) {
                generateRawType(ValueType.object(objectType.getFullClassName()));
            } else {
                generateParameterizedType(objectType, contextClass, contextMethod);
            }
        } else if (type instanceof GenericValueType.Variable varType) {
            generateTypeVariableRef(varType.getName(), contextClass, contextMethod);
        } else if (type instanceof GenericValueType.Array arrayType) {
            var nonGeneric = type.asValueType();
            if (nonGeneric != null) {
                generateRawType(nonGeneric);
            } else {
                writer.print("&(TeaVM_GenericTypeInfo) { .kind = 2, .itemType = ");
                generateGenericType(arrayType.getItemType(), contextClass, contextMethod);
                writer.print(" }");
            }
        } else {
            throw new IllegalArgumentException("Unsupported generic type: " + type);
        }
    }

    private void generateParameterizedType(GenericValueType.Object objectType, ClassReader contextClass,
            MethodReader contextMethod) {
        var vt = ValueType.object(objectType.getFullClassName());
        includes.includeType(vt);
        types.add(vt);
        var args = objectType.getArguments();
        writer.print("&(TeaVM_GenericTypeInfo) { .kind = 0, .parameterized = {").indent();
        writer.println();
        writer.print(".rawType = (TeaVM_Class*) &")
                .print(context.getNames().forClassInstance(vt)).println(",");
        writer.print(".actualTypeArgumentCount = ").print(String.valueOf(args.length)).println(",");
        writer.print(".actualTypeArguments = (TeaVM_GenericTypeInfo*[")
                .print(String.valueOf(args.length)).print("]) {").indent();
        for (var i = 0; i < args.length; ++i) {
            writer.println();
            if (i > 0) {
                writer.println(",");
            }
            generateTypeArgument(args[i], contextClass, contextMethod);
        }
        writer.println();
        writer.outdent().println("},");
        var parent = objectType.getParent();
        if (parent != null) {
            writer.print(".ownerType = ");
            generateGenericType(parent, contextClass, contextMethod);
        } else {
            writer.print(".ownerType = NULL");
        }
        writer.println();
        writer.outdent().print("} }");
    }

    private void generateTypeArgument(GenericValueType.Argument arg, ClassReader contextClass,
            MethodReader contextMethod) {
        switch (arg.getKind()) {
            case INVARIANT:
                generateGenericType(arg.getValue(), contextClass, contextMethod);
                break;
            case ANY:
                writer.print("&(TeaVM_GenericTypeInfo) { .kind = 5 }");
                break;
            case COVARIANT:
                writer.print("&(TeaVM_GenericTypeInfo) { .kind = 3, .bound = ");
                generateGenericType(arg.getValue(), contextClass, contextMethod);
                writer.print(" }");
                break;
            case CONTRAVARIANT:
                writer.print("&(TeaVM_GenericTypeInfo) { .kind = 4, .bound = ");
                generateGenericType(arg.getValue(), contextClass, contextMethod);
                writer.print(" }");
                break;
            default:
                throw new IllegalArgumentException("Unknown argument kind: " + arg.getKind());
        }
    }

    private void generateTypeVariableRef(String varName, ClassReader contextClass, MethodReader contextMethod) {
        var level = 0;
        if (contextMethod != null) {
            var params = contextMethod.getTypeParameters();
            for (var i = 0; i < params.length; ++i) {
                if (params[i].getName().equals(varName)) {
                    writeTypeVariableRef(0, i);
                    return;
                }
            }
            ++level;
        }
        var currentClass = contextClass;
        while (currentClass != null) {
            var params = currentClass.getGenericParameters();
            if (params != null) {
                for (var i = 0; i < params.length; ++i) {
                    if (params[i].getName().equals(varName)) {
                        writeTypeVariableRef(level, i);
                        return;
                    }
                }
            }
            ++level;
            if (currentClass.getOwnerName() == null) {
                break;
            }
            currentClass = context.getClassSource().get(currentClass.getOwnerName());
        }
        throw new IllegalArgumentException("Unknown type variable: " + varName);
    }

    private void writeTypeVariableRef(int level, int index) {
        writer.print("&(TeaVM_GenericTypeInfo) { .kind = 1, .typeVar = { .level = ")
                .print(String.valueOf(level)).print(", .index = ").print(String.valueOf(index))
                .print(" } }");
    }

    private void generateRawType(ValueType type) {
        includes.includePath("reflection.h");
        var degree = 0;
        var current = type;
        while (current instanceof ValueType.Array) {
            current = ((ValueType.Array) current).getItemType();
            ++degree;
        }
        includes.includeType(current);
        types.add(current);
        writer.print("&(TeaVM_GenericTypeInfo) { .kind = 6, .classPtr = ")
                .print("{ .baseClass = (TeaVM_Class*) &").print(context.getNames().forClassInstance(current))
                .print(", .arrayDegree = ").print(String.valueOf(degree)).print(" } }");
    }
}
