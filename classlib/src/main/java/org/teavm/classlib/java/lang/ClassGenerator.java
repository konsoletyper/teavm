/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.lang.annotation.Retention;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.classlib.impl.ReflectionDependencyListener;
import org.teavm.classlib.impl.reflection.ObjectList;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MemberReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ClassGenerator implements Generator, Injector, DependencyPlugin {
    private static final FieldReference platformClassField =
            new FieldReference(Class.class.getName(), "platformClass");
    private static final String TYPE_VAR_IMPL = "java.lang.reflect.TypeVariableImpl";
    private static final MethodReference typeVarConstructor = new MethodReference(TYPE_VAR_IMPL,
            "create", ValueType.object("java.lang.String"), ValueType.object(TYPE_VAR_IMPL));
    private static final MethodReference typeVarConstructorWithBounds = new MethodReference(TYPE_VAR_IMPL,
            "create", ValueType.object("java.lang.String"), ValueType.parse(ObjectList.class),
            ValueType.object(TYPE_VAR_IMPL));
    private static final MethodReference typeVarBounds = new MethodReference(TYPE_VAR_IMPL,
            "getBounds", ValueType.parse(Type[].class));
    private static final MethodReference parameterizedTypeConstructor = new MethodReference(
            "java.lang.reflect.ParameterizedTypeImpl", "create", ValueType.parse(Class.class),
            ValueType.parse(ObjectList.class), ValueType.object("java.lang.reflect.ParameterizedTypeImpl"));
    private static final MethodReference wildcardTypeUpper = new MethodReference(
            "java.lang.reflect.WildcardTypeImpl", "upper", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.WildcardTypeImpl"));
    private static final MethodReference wildcardTypeLower = new MethodReference(
            "java.lang.reflect.WildcardTypeImpl", "lower", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.WildcardTypeImpl"));
    private static final MethodReference genericArrayTypeCreate = new MethodReference(
            "java.lang.reflect.GenericArrayTypeImpl", "create", ValueType.parse(Type.class),
            ValueType.object("java.lang.reflect.GenericArrayTypeImpl"));
    private static final MethodReference typeVarStubCreate = new MethodReference(
            "java.lang.reflect.TypeVariableStub", "create", ValueType.INTEGER,
            ValueType.object("java.lang.reflect.TypeVariableStub"));
    private static final MethodReference typeVarStubCreateWithLevel = new MethodReference(
            "java.lang.reflect.TypeVariableStub", "create", ValueType.INTEGER, ValueType.INTEGER,
            ValueType.object("java.lang.reflect.TypeVariableStub"));
    private static final MethodDescriptor CLINIT = new MethodDescriptor("<clinit>", void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "createMetadata":
                generateCreateMetadata(context, writer);
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "newEmptyInstance":
                context.getWriter().append("new").ws().append("(");
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append('.').appendField(platformClassField);
                context.getWriter().append(")");
                break;
            case "getDeclaredFieldsImpl":
                context.getWriter().appendFunction("$rt_undefinedAsNull").append("(");
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".").appendField(platformClassField)
                        .append(".$meta.fields").append(")");
                break;
            case "getDeclaredMethodsImpl":
                context.getWriter().appendFunction("$rt_undefinedAsNull").append("(");
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".").appendField(platformClassField)
                        .append(".$meta.methods").append(")");
                break;
            case "getTypeParametersImpl":
                context.getWriter().appendFunction("$rt_undefinedAsNull").append("(");
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".").appendField(platformClassField)
                        .append(".$meta.typeParams").append(")");
                break;
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getReference().getName()) {
            case "newEmptyInstance":
                method.getVariable(0).getClassValueNode().addConsumer(type -> {
                    if (!(type.getValueType() instanceof ValueType.Object)) {
                        return;
                    }
                    var className = ((ValueType.Object) type.getValueType()).getClassName();
                    var cls = agent.getClassSource().get(className);
                    if (cls != null && !cls.hasModifier(ElementModifier.ABSTRACT)
                            && !cls.hasModifier(ElementModifier.INTERFACE)) {
                        method.getResult().propagate(type);
                    }
                });
                break;
            case "getSuperclass":
                reachGetSuperclass(agent, method);
                break;
            case "getInterfaces":
                reachGetInterfaces(agent, method);
                break;
            case "getComponentType":
                reachGetComponentType(agent, method);
                break;
        }
    }

    private void reachGetSuperclass(DependencyAgent agent, MethodDependency method) {
        method.getVariable(0).getClassValueNode().addConsumer(type -> {
            if (!(type.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) type.getValueType()).getClassName();
            var cls = agent.getClassSource().get(className);
            if (cls != null && cls.getParent() != null) {
                method.getResult().getClassValueNode().propagate(agent.getType(ValueType.object(cls.getParent())));
            }
        });
    }

    private void reachGetInterfaces(DependencyAgent agent, MethodDependency method) {
        method.getVariable(0).getClassValueNode().addConsumer(type -> {
            if (!(type.getValueType() instanceof ValueType.Object)) {
                return;
            }

            var className = ((ValueType.Object) type.getValueType()).getClassName();
            var cls = agent.getClassSource().get(className);
            method.getResult().propagate(agent.getType(ValueType.arrayOf(ValueType.object("java.lang.Class"))));
            method.getResult().getArrayItem().propagate(agent.getType(ValueType.object("java.lang.Class")));
            if (cls != null) {
                for (String iface : cls.getInterfaces()) {
                    method.getResult().getArrayItem().getClassValueNode().propagate(agent.getType(
                            ValueType.object(iface)));
                }
            }
        });
    }

    private void reachGetComponentType(DependencyAgent agent, MethodDependency method) {
        method.getVariable(0).getClassValueNode().addConsumer(t -> {
            if (!(t.getValueType() instanceof ValueType.Array)) {
                return;
            }
            var itemType = ((ValueType.Array) t.getValueType()).getItemType();
            method.getResult().getClassValueNode().propagate(agent.getType(itemType));
        });
    }

    private void generateCreateMetadata(GeneratorContext context, SourceWriter writer) {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);
        for (String className : reflection.getClassesWithReflectableFields()) {
            generateCreateFieldsForClass(context, writer, className);
        }
        for (String className : reflection.getClassesWithReflectableMethods()) {
            generateCreateMethodsForClass(context, writer, className);
        }
        if (context.getDependency().getMethod(typeVarConstructor) != null) {
            var withBounds = context.getDependency().getMethod(typeVarBounds) != null;
            for (var className : context.getClassSource().getClassNames()) {
                var cls = context.getClassSource().get(className);
                if (cls != null) {
                    generateClassTypeParameters(context, writer, cls, withBounds);
                }
            }
        }
    }

    private void generateCreateFieldsForClass(GeneratorContext context, SourceWriter writer, String className) {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);
        Set<String> accessibleFields = reflection.getAccessibleFields(className);

        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return;
        }

        writer.appendClass(className).append(".$meta.fields").ws().append('=').ws().append('[').indent();
        var fieldsToExpose = accessibleFields == null ? cls.getFields() : cls.getFields().stream()
                .filter(f -> accessibleFields.contains(f.getName()))
                .collect(Collectors.toSet());

        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);
        generateCreateMembers(context, writer, skipPrivates, fieldsToExpose, field -> {
            appendProperty(writer, "type", false, () -> context.typeToClassString(writer, field.getType()));

            appendProperty(writer, "getter", false, () -> {
                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isRead(field.getReference())) {
                    renderGetter(context, writer, field);
                } else {
                    writer.append("null");
                }
            });

            appendProperty(writer, "setter", false, () -> {
                if (accessibleFields != null && accessibleFields.contains(field.getName())
                        && reflection.isWritten(field.getReference())) {
                    renderSetter(context, writer, field);
                } else {
                    writer.append("null");
                }
            });
        });

        writer.outdent().append("];").softNewLine();
    }

    private void generateCreateMethodsForClass(GeneratorContext context, SourceWriter writer, String className) {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);
        Set<MethodDescriptor> accessibleMethods = reflection.getAccessibleMethods(className);

        ClassReader cls = context.getInitialClassSource().get(className);
        if (cls == null) {
            return;
        }

        writer.appendClass(className).append(".$meta.methods").ws().append('=').ws().append('[').indent();

        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);
        var methodsToExpose = accessibleMethods == null ? cls.getMethods() : cls.getMethods().stream()
                .filter(m -> accessibleMethods.contains(m.getDescriptor()))
                .collect(Collectors.toList());
        var withBounds = context.getDependency().getMethod(typeVarBounds) != null;

        generateCreateMembers(context, writer, skipPrivates, methodsToExpose, method -> {
            appendProperty(writer, "parameterTypes", false, () -> {
                writer.append('[');
                for (int i = 0; i < method.parameterCount(); ++i) {
                    if (i > 0) {
                        writer.append(',').ws();
                    }
                    context.typeToClassString(writer, method.parameterType(i));
                }
                writer.append(']');
            });

            appendProperty(writer, "returnType", false, () -> {
                context.typeToClassString(writer, method.getResultType());
            });

            appendProperty(writer, "callable", false, () -> {
                if (accessibleMethods != null && accessibleMethods.contains(method.getDescriptor())
                        && reflection.isCalled(method.getReference())) {
                    renderCallable(context, writer, method);
                } else {
                    writer.append("null");
                }
            });

            var typeParameters = method.getTypeParameters();
            if (typeParameters != null && typeParameters.length > 0
                    && context.getDependency().getMethod(typeVarConstructor) != null) {
                appendProperty(writer, "typeParameters", false, () -> {
                    generateTypeParams(context, writer, typeParameters, cls, method, withBounds);
                });
            }
        });

        writer.outdent().append("];").softNewLine();
    }

    private void generateClassTypeParameters(GeneratorContext context, SourceWriter writer, ClassReader cls,
            boolean withBounds) {
        var parameters = cls.getGenericParameters();
        if (parameters == null || parameters.length == 0) {
            return;
        }
        writer.appendClass(cls.getName()).append(".$meta.typeParams")
                .ws().append("=").ws();
        generateTypeParams(context, writer, cls.getGenericParameters(), cls, null, withBounds);
        writer.append(";").softNewLine();
    }

    private void generateTypeParams(GeneratorContext context, SourceWriter writer, GenericTypeParameter[] parameters,
            ClassReader cls, MethodReader method, boolean withBounds) {
        writer.append('[');
        for (int i = 0; i < parameters.length; ++i) {
            var param = parameters[i];
            if (i > 0) {
                writer.append(",").ws();
            }
            var bounds = withBounds ? param.extractAllBounds() : List.<GenericValueType.Reference>of();
            if (bounds.isEmpty()) {
                writer.appendMethod(typeVarConstructor).append("(").appendFunction("$rt_s")
                        .append("(" + context.lookupString(param.getName()) + "))");
            } else {
                writer.appendMethod(typeVarConstructorWithBounds).append("(").appendFunction("$rt_s")
                        .append("(" + context.lookupString(param.getName()) + "),").ws();
                generateTypeParametersBounds(context, writer, cls, method, bounds);
                writer.append(")");
            }
        }
        writer.append(']');
    }

    private void generateTypeParametersBounds(GeneratorContext context, SourceWriter writer, ClassReader cls,
            MethodReader method, List<GenericValueType.Reference> bounds) {
        writer.append('[');

        for (int j = 0; j < bounds.size(); ++j) {
            var bound = bounds.get(j);
            if (j > 0) {
                writer.append(",").ws();
            }
            generateGenericType(context, writer, cls, method, bound);
        }
        writer.append(']');
    }

    private void generateGenericType(GeneratorContext context, SourceWriter writer, ClassReader owningClass,
            MethodReader owningMethod, GenericValueType type) {
        if (type instanceof GenericValueType.Variable) {
            var typeVar = (GenericValueType.Variable) type;
            if (owningMethod != null) {
                var params = owningMethod.getTypeParameters();
                for (var i = 0; i < params.length; ++i) {
                    if (typeVar.getName().equals(params[i].getName())) {
                        writer.appendMethod(typeVarStubCreate).append("(").append(i).append(")");
                        return;
                    }
                }
            }
            var params = owningClass.getGenericParameters();
            for (var i = 0; i < params.length; ++i) {
                if (typeVar.getName().equals(params[i].getName())) {
                    if (owningMethod != null) {
                        writer.appendMethod(typeVarStubCreateWithLevel).append("(").append(i).append(",").ws()
                                .append(1).append(")");
                    } else {
                        writer.appendMethod(typeVarStubCreate).append("(").append(i).append(")");
                    }
                    break;
                }
            }
        } else if (type instanceof GenericValueType.Object) {
            var parameterizedType = (GenericValueType.Object) type;
            var typeArgs = parameterizedType.getArguments();
            if (typeArgs == null || typeArgs.length == 0) {
                writer.appendFunction("$rt_cls").append("(").appendClass(parameterizedType.getClassName()).append(")");
            } else {
                writer.appendMethod(parameterizedTypeConstructor).append("(");
                writer.appendFunction("$rt_cls").append("(").appendClass(parameterizedType.getClassName()).append(")");
                writer.append(",").ws().append('[');
                for (var i = 0; i < typeArgs.length; ++i) {
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    generateGenericType(context, writer, owningClass, owningMethod, typeArgs[i]);
                }
                writer.append("])");
            }
        } else if (type instanceof GenericValueType.Array) {
            var nonGenericType = type.asValueType();
            if (nonGenericType == null) {
                var arrayType = (GenericValueType.Array) type;
                writer.appendMethod(genericArrayTypeCreate).append("(");
                generateGenericType(context, writer, owningClass, owningMethod, arrayType.getItemType());
                writer.append(")");
            } else {
                writer.appendFunction("$rt_cls").append("(");
                context.typeToClassString(writer, nonGenericType);
                writer.append(")");
            }
        } else if (type instanceof GenericValueType.Primitive) {
            var primitiveType = (GenericValueType.Primitive) type;
            writer.appendFunction("$rt_cls").append("(");
            context.typeToClassString(writer, ValueType.primitive(primitiveType.getKind()));
            writer.append(")");
        } else if (type instanceof GenericValueType.Void) {
            writer.appendFunction("$rt_cls").append("(");
            context.typeToClassString(writer, ValueType.VOID);
            writer.append(")");
        }
    }

    private void generateGenericType(GeneratorContext context, SourceWriter writer, ClassReader owningClass,
            MethodReader owningMethod, GenericValueType.Argument arg) {
        switch (arg.getKind()) {
            case INVARIANT:
                generateGenericType(context, writer, owningClass, owningMethod, arg.getValue());
                break;
            case ANY:
                writer.appendMethod(wildcardTypeUpper).append("(null)");
                break;
            case COVARIANT:
                writer.appendMethod(wildcardTypeUpper).append("(");
                generateGenericType(context, writer, owningClass, owningMethod, arg.getValue());
                writer.append(")");
                break;
            case CONTRAVARIANT:
                writer.appendMethod(wildcardTypeLower).append("(");
                generateGenericType(context, writer, owningClass, owningMethod, arg.getValue());
                writer.append(")");
                break;
        }
    }

    private <T extends MemberReader> void generateCreateMembers(GeneratorContext context, SourceWriter writer,
            boolean skipPrivates, Iterable<T> members, MemberRenderer<T> renderer) {
        boolean first = true;
        for (T member : members) {
            if (skipPrivates) {
                if (member.getLevel() == AccessLevel.PRIVATE || member.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            if (!first) {
                writer.append(",").ws();
            } else {
                writer.softNewLine();
            }
            first = false;
            writer.append("{").indent().softNewLine();

            appendProperty(writer, "name", true, () ->  writer.append('"')
                    .append(RenderingUtil.escapeString(member.getName())).append('"'));
            appendProperty(writer, "modifiers", false, () -> writer.append(
                    ElementModifier.pack(member.readModifiers())));
            appendProperty(writer, "accessLevel", false, () -> writer.append(member.getLevel().ordinal()));
            generateAnnotations(context, writer, member.getAnnotations().all());
            renderer.render(member);
            writer.outdent().softNewLine().append("}");
        }
    }

    private void generateAnnotations(GeneratorContext context, SourceWriter writer,
            Iterable<? extends AnnotationReader> annotations) {
        var annotationsToExpose = new ArrayList<AnnotationReader>();
        for (var annotation : annotations) {
            var annotationCls = context.getClassSource().get(annotation.getType());
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
        writer.append(",").softNewLine();
        writer.append("annotations").append(':').ws();
        writer.appendFunction("$rt_wrapArray").append("(").appendClass("java.lang.annotation.Annotation")
                .append(",").ws().append("[");
        if (!annotationsToExpose.isEmpty()) {
            generateAnnotation(context, writer, annotationsToExpose.get(0));
            for (var i = 1; i < annotationsToExpose.size(); ++i) {
                writer.append(",").ws();
                generateAnnotation(context, writer, annotationsToExpose.get(i));
            }
        }
        writer.append("])");
    }

    private void generateAnnotation(GeneratorContext context, SourceWriter writer, AnnotationReader annotation) {
        var annotCls = context.getClassSource().get(annotation.getType());
        var annotImpl = context.getClassSource().get(annotation.getType() + "$$_impl");

        var arguments = new ArrayList<Fragment>();
        var signature = new ArrayList<ValueType>();
        for (var methodDecl : annotCls.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            signature.add(methodDecl.getResultType());
            var ownValue = annotation.getValue(methodDecl.getName());
            var value = ownValue != null ? ownValue : methodDecl.getAnnotationDefault();
            arguments.add(() -> generateAnnotationValue(context, writer, value, methodDecl.getResultType()));
        }

        signature.add(ValueType.VOID);
        var ctor = new MethodReference(annotImpl.getName(), "<init>", signature.toArray(new ValueType[0]));
        writer.appendInit(ctor).append("(");
        if (!arguments.isEmpty()) {
            arguments.get(0).render();
            for (var i = 1; i < arguments.size(); ++i) {
                writer.append(",").ws();
                arguments.get(i).render();
            }
        }
        writer.append(")");
    }

    private void generateAnnotationValue(GeneratorContext context, SourceWriter writer,
            AnnotationValue value, ValueType type) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                writer.append(value.getBoolean() ? "1" : "0");
                break;
            case AnnotationValue.CHAR:
                writer.append((int) value.getChar());
                break;
            case AnnotationValue.BYTE:
                writer.append(value.getByte());
                break;
            case AnnotationValue.SHORT:
                writer.append(value.getShort());
                break;
            case AnnotationValue.INT:
                writer.append(value.getInt());
                break;
            case AnnotationValue.LONG:
                writer.append(String.valueOf(value.getLong()));
                break;
            case AnnotationValue.FLOAT:
                writer.append(String.valueOf(value.getFloat()));
                break;
            case AnnotationValue.DOUBLE:
                writer.append(String.valueOf(value.getDouble()));
                break;
            case AnnotationValue.STRING:
                writer.appendFunction("$rt_str").append("(\"").append(RenderingUtil.escapeString(value.getString()))
                        .append("\")");
                break;
            case AnnotationValue.LIST: {
                var itemType = ((ValueType.Array) type).getItemType();
                appendArrayConstructor(context, writer, itemType);
                var list = value.getList();
                writer.append("[");
                if (!list.isEmpty()) {
                    generateAnnotationValue(context, writer, list.get(0), itemType);
                    for (var i = 1; i < list.size(); ++i) {
                        writer.append(",").ws();
                        generateAnnotationValue(context, writer, list.get(i), itemType);
                    }
                }
                writer.append("])");
                break;
            }
            case AnnotationValue.ANNOTATION:
                generateAnnotation(context, writer, value.getAnnotation());
                return;
            case AnnotationValue.ENUM:
                if (context.isDynamicInitializer(value.getEnumValue().getClassName())) {
                    writer.append("(").appendClassInit(value.getEnumValue().getClassName()).append("(),").ws()
                            .appendStaticField(value.getEnumValue()).append(")");
                } else {
                    writer.appendStaticField(value.getEnumValue());
                }
                break;
            case AnnotationValue.CLASS:
                context.typeToClassString(writer, type);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void appendArrayConstructor(GeneratorContext context, SourceWriter writer, ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    writer.appendFunction("$rt_createBooleanArrayFromData").append("(");
                    return;
                case BYTE:
                    writer.appendFunction("$rt_createByteArrayFromData").append("(");
                    return;
                case SHORT:
                    writer.appendFunction("$rt_createShortArrayFromData").append("(");
                    return;
                case CHARACTER:
                    writer.appendFunction("$rt_createCharArrayFromData").append("(");
                    return;
                case INTEGER:
                    writer.appendFunction("$rt_createIntArrayFromData").append("(");
                    return;
                case LONG:
                    writer.appendFunction("$rt_createLongArrayFromData").append("(");
                    return;
                case FLOAT:
                    writer.appendFunction("$rt_createFloatArrayFromData").append("(");
                    return;
                case DOUBLE:
                    writer.appendFunction("$rt_createDoubleArrayFromData").append("(");
                    return;
            }
        }
        writer.appendFunction("$rt_wrapArray").append("(");
        context.typeToClassString(writer, itemType);
        writer.append(",").ws();
    }

    private void appendProperty(SourceWriter writer, String name, boolean first, Fragment value) {
        if (!first) {
            writer.append(",").softNewLine();
        }
        writer.append(name).append(':').ws();
        value.render();
    }

    private void renderGetter(GeneratorContext context, SourceWriter writer, FieldReader field) {
        writer.append("function(obj)").ws().append("{").indent().softNewLine();
        initClass(context, writer, field);
        writer.append("return ");
        boxIfNecessary(writer, field.getType(), () -> fieldAccess(writer, field));
        writer.append(";").softNewLine();
        writer.outdent().append("}");
    }

    private void renderSetter(GeneratorContext context, SourceWriter writer, FieldReader field) {
        writer.append("function(obj,").ws().append("val)").ws().append("{").indent().softNewLine();
        initClass(context, writer, field);
        fieldAccess(writer, field);
        writer.ws().append('=').ws();
        unboxIfNecessary(writer, field.getType(), () -> writer.append("val"));
        writer.append(";").softNewLine();
        writer.outdent().append("}");
    }

    private void renderCallable(GeneratorContext context, SourceWriter writer, MethodReader method) {
        writer.append("(obj,").ws().append("args)").ws().append("=>").ws().append("{").indent().softNewLine();

        initClass(context, writer, method);

        if (method.getReference().getName().equals("<init>")) {
            writer.append("obj").ws().append("=").ws().append("new ")
                    .appendClass(method.getReference().getClassName()).append(";").softNewLine();
        }
        if (method.getResultType() != ValueType.VOID) {
            writer.append("return ");
        }
        var receiverWritten = false;
        if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.FINAL)
                && method.getLevel() != AccessLevel.PRIVATE && !method.getName().equals("<init>")) {
            writer.append("obj.").appendVirtualMethod(method.getDescriptor());
            receiverWritten = true;
        } else {
            writer.appendMethod(method.getReference());
        }

        writer.append('(');
        boolean first = true;
        if (!receiverWritten && !method.hasModifier(ElementModifier.STATIC)) {
            writer.append("obj").ws();
            first = false;
        }
        for (int i = 0; i < method.parameterCount(); ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            int index = i;
            unboxIfNecessary(writer, method.parameterType(i), () -> writer.append("args[" + index + "]"));
        }
        writer.append(");").softNewLine();

        if (method.getReference().getName().equals("<init>")) {
            writer.append("return obj;").softNewLine();
        } else if (method.getResultType() == ValueType.VOID) {
            writer.append("return null;").softNewLine();
        }
        writer.outdent().append("}");
    }

    private void initClass(GeneratorContext context, SourceWriter writer, MemberReader member) {
        var cls = context.getClassSource().get(member.getOwnerName());
        if (member.hasModifier(ElementModifier.STATIC) && context.isDynamicInitializer(member.getOwnerName())
                && cls.getMethod(CLINIT) != null) {
            writer.appendClassInit(member.getOwnerName()).append("();").softNewLine();
        }
    }

    private void fieldAccess(SourceWriter writer, FieldReader field) {
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.appendStaticField(field.getReference());
        } else {
            writer.append("obj.").appendField(field.getReference());
        }
    }

    private void boxIfNecessary(SourceWriter writer, ValueType type, Fragment fragment) {
        boolean boxed = false;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.appendMethod(new MethodReference(Boolean.class, "valueOf", boolean.class,
                            Boolean.class));
                    break;
                case BYTE:
                    writer.appendMethod(new MethodReference(Byte.class, "valueOf", byte.class, Byte.class));
                    break;
                case SHORT:
                    writer.appendMethod(new MethodReference(Short.class, "valueOf", short.class, Short.class));
                    break;
                case CHARACTER:
                    writer.appendMethod(new MethodReference(Character.class, "valueOf", char.class,
                            Character.class));
                    break;
                case INTEGER:
                    writer.appendMethod(new MethodReference(Integer.class, "valueOf", int.class, Integer.class));
                    break;
                case LONG:
                    writer.appendMethod(new MethodReference(Long.class, "valueOf", long.class, Long.class));
                    break;
                case FLOAT:
                    writer.appendMethod(new MethodReference(Float.class, "valueOf", float.class, Float.class));
                    break;
                case DOUBLE:
                    writer.appendMethod(new MethodReference(Double.class, "valueOf", double.class, Double.class));
                    break;
            }
            writer.append('(');
            boxed = true;
        }
        fragment.render();
        if (boxed) {
            writer.append(')');
        }
    }

    private void unboxIfNecessary(SourceWriter writer, ValueType type, Fragment fragment) {
        boolean boxed = false;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.appendMethod(new MethodReference(Boolean.class, "booleanValue", boolean.class));
                    break;
                case BYTE:
                    writer.appendMethod(new MethodReference(Byte.class, "byteValue", byte.class));
                    break;
                case SHORT:
                    writer.appendMethod(new MethodReference(Short.class, "shortValue", short.class));
                    break;
                case CHARACTER:
                    writer.appendMethod(new MethodReference(Character.class, "charValue", char.class));
                    break;
                case INTEGER:
                    writer.appendMethod(new MethodReference(Integer.class, "intValue", int.class));
                    break;
                case LONG:
                    writer.appendMethod(new MethodReference(Long.class, "longValue", long.class));
                    break;
                case FLOAT:
                    writer.appendMethod(new MethodReference(Float.class, "floatValue", float.class));
                    break;
                case DOUBLE:
                    writer.appendMethod(new MethodReference(Double.class, "doubleValue", double.class));
                    break;
            }
            writer.append('(');
            boxed = true;
        }
        fragment.render();
        if (boxed) {
            writer.append(')');
        }
    }

    private interface Fragment {
        void render();
    }

    private interface MemberRenderer<T extends MemberReader> {
        void render(T member);
    }
}
