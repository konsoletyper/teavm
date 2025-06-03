/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.jso.impl;

import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT_CLASS;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSProperty;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

class JSObjectClassTransformer implements ClassHolderTransformer {
    private JSClassProcessor processor;
    private JSBodyRepository repository;
    private JSTypeHelper typeHelper;
    private ClassHierarchy hierarchy;
    private Map<String, ExposedClass> exposedClasses = new HashMap<>();
    private Predicate<String> classFilter = n -> true;
    private boolean wasmGC;

    JSObjectClassTransformer(JSBodyRepository repository) {
        this.repository = repository;
    }

    void setClassFilter(Predicate<String> classFilter) {
        this.classFilter = classFilter;
    }

    void forWasmGC() {
        wasmGC = true;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        this.hierarchy = context.getHierarchy();
        if (processor == null || processor.getClassSource() != hierarchy.getClassSource()) {
            typeHelper = new JSTypeHelper(hierarchy.getClassSource());
            processor = new JSClassProcessor(hierarchy.getClassSource(), hierarchy, typeHelper, repository,
                    context.getDiagnostics(), context.getIncrementalCache(), context.isStrict());
            processor.setWasmGC(wasmGC);
            processor.setClassFilter(classFilter);
        }
        processor.processClass(cls);
        if (isJavaScriptClass(cls) && !isJavaScriptImplementation(cls)) {
            processor.processMemberMethods(cls);
        }

        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.getProgram() != null) {
                processor.processProgram(method);
            }
        }
        processor.createJSMethods(cls);

        if (isJavaScriptClass(cls) && !isJavaScriptImplementation(cls)) {
            return;
        }

        var hasStaticMethods = false;
        var hasMemberMethods = false;

        if (!cls.hasModifier(ElementModifier.ABSTRACT)) {
            MethodReference functorMethod = processor.isFunctor(cls.getName());
            if (functorMethod != null) {
                if (processor.isFunctor(cls.getParent()) != null) {
                    functorMethod = null;
                }
            }

            ClassReader originalClass = hierarchy.getClassSource().get(cls.getName());
            ExposedClass exposedClass;
            if (originalClass != null) {
                exposedClass = getExposedClass(cls.getName());
            } else {
                exposedClass = new ExposedClass();
                createExposedClass(cls, exposedClass);
            }

            exposeMethods(cls, exposedClass, context.getDiagnostics(), functorMethod);
            if (!exposedClass.methods.isEmpty()) {
                hasMemberMethods = true;
                cls.getAnnotations().add(new AnnotationHolder(JSClassToExpose.class.getName()));
            }
        }
        hasStaticMethods = exportStaticMethods(cls, context.getDiagnostics(), context.getEntryPoint());
        if (hasMemberMethods || hasStaticMethods) {
            cls.getAnnotations().add(new AnnotationHolder(JSClassObjectToExpose.class.getName()));
        }

        if (wasmGC && hasMemberMethods) {
            var createWrapperMethod = new MethodHolder(JSMethods.MARSHALL_TO_JS);
            createWrapperMethod.setLevel(AccessLevel.PUBLIC);
            createWrapperMethod.getModifiers().add(ElementModifier.NATIVE);
            cls.addMethod(createWrapperMethod);
            cls.getInterfaces().add(JSMethods.JS_MARSHALLABLE);
        }
    }

    private void exposeMethods(ClassHolder classHolder, ExposedClass classToExpose, Diagnostics diagnostics,
            MethodReference functorMethod) {
        int index = 0;
        for (var entry : classToExpose.methods.entrySet()) {
            var method = entry.getKey();
            var export = entry.getValue();
            MethodReference methodRef = new MethodReference(classHolder.getName(), method);
            CallLocation callLocation = new CallLocation(methodRef);

            var isConstructor = entry.getKey().getName().equals("<init>");
            var paramCount = method.parameterCount();
            if (export.vararg) {
                --paramCount;
            }
            if (isConstructor) {
                --paramCount;
            }
            var exportedMethodSignature = new ValueType[paramCount + 2];
            Arrays.fill(exportedMethodSignature, JS_OBJECT);
            if (methodRef.getReturnType() == ValueType.VOID && !isConstructor) {
                exportedMethodSignature[exportedMethodSignature.length - 1] = ValueType.VOID;
            }
            MethodDescriptor exportedMethodDesc = new MethodDescriptor(method.getName() + "$exported$" + index++,
                    exportedMethodSignature);
            MethodHolder exportedMethod = new MethodHolder(exportedMethodDesc);
            exportedMethod.getModifiers().add(ElementModifier.STATIC);
            exportedMethod.getAnnotations().add(new AnnotationHolder(JSInstanceExpose.class.getName()));
            Program program = new Program();
            exportedMethod.setProgram(program);
            program.createVariable();
            if (!isConstructor) {
                program.createVariable();
            }

            BasicBlock basicBlock = program.createBasicBlock();
            List<Instruction> marshallInstructions = new ArrayList<>();
            JSValueMarshaller marshaller = new JSValueMarshaller(diagnostics, typeHelper, hierarchy.getClassSource(),
                    hierarchy, program, marshallInstructions);

            Variable[] variablesToPass = new Variable[method.parameterCount()];
            for (int i = 0; i < method.parameterCount(); ++i) {
                variablesToPass[i] = program.createVariable();
            }
            if (export.vararg) {
                transformVarargParam(variablesToPass, program, marshallInstructions, exportedMethod, 1);
            }

            for (int i = 0; i < method.parameterCount(); ++i) {
                var byRef = i == method.parameterCount() - 1 && export.vararg
                        && typeHelper.isSupportedByRefType(method.parameterType(i));
                variablesToPass[i] = marshaller.unwrapReturnValue(callLocation, variablesToPass[i],
                        method.parameterType(i), byRef, true);
            }

            basicBlock.addAll(marshallInstructions);
            marshallInstructions.clear();

            Variable receiverToPass;
            if (isConstructor) {
                var create = new ConstructInstruction();
                create.setReceiver(program.createVariable());
                create.setType(classHolder.getName());
                basicBlock.add(create);
                receiverToPass = create.getReceiver();
            } else {
                var unmarshalledInstance = new InvokeInstruction();
                unmarshalledInstance.setType(InvocationType.SPECIAL);
                unmarshalledInstance.setReceiver(program.createVariable());
                unmarshalledInstance.setArguments(program.variableAt(1));
                unmarshalledInstance.setMethod(new MethodReference(JS_WRAPPER_CLASS,
                        "unmarshallJavaFromJs", JS_OBJECT, OBJECT));
                basicBlock.add(unmarshalledInstance);

                var castInstance = new CastInstruction();
                castInstance.setValue(unmarshalledInstance.getReceiver());
                castInstance.setReceiver(program.createVariable());
                castInstance.setWeak(true);
                castInstance.setTargetType(ValueType.object(classHolder.getName()));
                basicBlock.add(castInstance);

                receiverToPass = castInstance.getReceiver();
            }

            InvokeInstruction invocation = new InvokeInstruction();
            invocation.setType(isConstructor ? InvocationType.SPECIAL : InvocationType.VIRTUAL);
            invocation.setInstance(receiverToPass);
            invocation.setMethod(methodRef);
            invocation.setArguments(variablesToPass);
            basicBlock.add(invocation);

            ExitInstruction exit = new ExitInstruction();
            if (method.getResultType() != ValueType.VOID || isConstructor) {
                Variable result;
                ValueType resultType;
                if (isConstructor) {
                    result = receiverToPass;
                    resultType = ValueType.object(classHolder.getName());
                } else {
                    result = program.createVariable();
                    invocation.setReceiver(result);
                    resultType = method.getResultType();
                }
                exit.setValueToReturn(marshaller.wrapArgument(callLocation, result, resultType,
                        typeHelper.mapType(method.getResultType()), false, null));
                basicBlock.addAll(marshallInstructions);
                marshallInstructions.clear();
            }
            basicBlock.add(exit);

            classHolder.addMethod(exportedMethod);
            exportedMethod.getAnnotations().add(createExportAnnotation(export));

            if (methodRef.equals(functorMethod)) {
                addFunctorField(classHolder, exportedMethod.getReference());
            }
        }
    }

    private boolean exportStaticMethods(ClassHolder classHolder, Diagnostics diagnostics,
            String entryPointName) {
        int index = 0;
        var hasMethods = false;
        for (var method : classHolder.getMethods().toArray(new MethodHolder[0])) {
            if (!method.hasModifier(ElementModifier.STATIC)
                    || method.getAnnotations().get(JSExport.class.getName()) == null) {
                continue;
            }
            hasMethods = true;

            var paramCount = method.parameterCount();
            var vararg = method.hasModifier(ElementModifier.VARARGS);
            if (vararg) {
                --paramCount;
            }
            var callLocation = new CallLocation(method.getReference());
            var exportedMethodSignature = new ValueType[paramCount + 1];
            Arrays.fill(exportedMethodSignature, JS_OBJECT);
            if (method.getResultType() == ValueType.VOID) {
                exportedMethodSignature[exportedMethodSignature.length - 1] = ValueType.VOID;
            }
            var exportedMethodDesc = new MethodDescriptor(method.getName() + "$exported$" + index++,
                    exportedMethodSignature);
            var exportedMethod = new MethodHolder(exportedMethodDesc);
            exportedMethod.getModifiers().add(ElementModifier.STATIC);
            var program = new Program();
            program.createVariable();
            exportedMethod.setProgram(program);

            var basicBlock = program.createBasicBlock();
            if (entryPointName != null && !entryPointName.equals(classHolder.getName())) {
                var clinit = new InitClassInstruction();
                clinit.setClassName(entryPointName);
                basicBlock.add(clinit);
            }

            var marshallInstructions = new ArrayList<Instruction>();
            var marshaller = new JSValueMarshaller(diagnostics, typeHelper, hierarchy.getClassSource(), hierarchy,
                    program, marshallInstructions);

            var variablesToPass = new Variable[method.parameterCount()];
            for (int i = 0; i < method.parameterCount(); ++i) {
                variablesToPass[i] = program.createVariable();
            }
            if (vararg) {
                transformVarargParam(variablesToPass, program, marshallInstructions, exportedMethod, 0);
            }

            for (int i = 0; i < method.parameterCount(); ++i) {
                var byRef = i == method.parameterCount() - 1 && vararg
                        && typeHelper.isSupportedByRefType(method.parameterType(i));
                variablesToPass[i] = marshaller.unwrapReturnValue(callLocation, variablesToPass[i],
                        method.parameterType(i), byRef, true);
            }

            basicBlock.addAll(marshallInstructions);
            marshallInstructions.clear();

            var invocation = new InvokeInstruction();
            invocation.setType(InvocationType.SPECIAL);
            invocation.setMethod(method.getReference());
            invocation.setArguments(variablesToPass);
            basicBlock.add(invocation);

            var exit = new ExitInstruction();
            if (method.getResultType() != ValueType.VOID) {
                invocation.setReceiver(program.createVariable());
                exit.setValueToReturn(marshaller.wrapArgument(callLocation, invocation.getReceiver(),
                        method.getResultType(), typeHelper.mapType(method.getResultType()), false, null));
                basicBlock.addAll(marshallInstructions);
                marshallInstructions.clear();
            }
            basicBlock.add(exit);

            classHolder.addMethod(exportedMethod);

            var export = createMethodExport(method);
            exportedMethod.getAnnotations().add(createExportAnnotation(export));
        }
        return hasMethods;
    }

    private void transformVarargParam(Variable[] variablesToPass, Program program,
            List<Instruction> instructions, MethodHolder method, int additionalSkip) {
        var last = variablesToPass.length - 1;

        var lastConstant = new IntegerConstantInstruction();
        lastConstant.setReceiver(program.createVariable());
        lastConstant.setConstant(last + additionalSkip);
        instructions.add(lastConstant);

        var extractVarargs = new InvokeInstruction();
        extractVarargs.setType(InvocationType.SPECIAL);
        extractVarargs.setMethod(JSMethods.ARGUMENTS_BEGINNING_AT);
        extractVarargs.setArguments(lastConstant.getReceiver());
        extractVarargs.setReceiver(variablesToPass[last]);
        instructions.add(extractVarargs);

        method.getAnnotations().add(new AnnotationHolder(JSVararg.class.getName()));
    }

    private AnnotationHolder createExportAnnotation(MethodExport export) {
        String annotationName;
        switch (export.kind) {
            case GETTER:
                annotationName = JSGetterToExpose.class.getName();
                break;
            case SETTER:
                annotationName = JSSetterToExpose.class.getName();
                break;
            case CONSTRUCTOR:
                annotationName = JSConstructorToExpose.class.getName();
                break;
            default:
                annotationName = JSMethodToExpose.class.getName();
                break;
        }
        var annot = new AnnotationHolder(annotationName);
        if (export.kind != MethodKind.CONSTRUCTOR) {
            annot.getValues().put("name", new AnnotationValue(export.alias));
        }
        return annot;
    }

    private ExposedClass getExposedClass(String name) {
        ExposedClass cls = exposedClasses.get(name);
        if (cls == null) {
            cls = createExposedClass(name);
            exposedClasses.put(name, cls);
        }
        return cls;
    }

    private ExposedClass createExposedClass(String name) {
        ClassReader cls = hierarchy.getClassSource().get(name);
        ExposedClass exposedCls = new ExposedClass();
        if (cls != null) {
            createExposedClass(cls, exposedCls);
        }
        return exposedCls;
    }

    private void createExposedClass(ClassReader cls, ExposedClass exposedCls) {
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            return;
        }
        if (cls.getParent() != null) {
            ExposedClass parent = getExposedClass(cls.getParent());
            exposedCls.inheritedMethods.addAll(parent.inheritedMethods);
            exposedCls.inheritedMethods.addAll(parent.methods.keySet());
            exposedCls.implementedInterfaces.addAll(parent.implementedInterfaces);
        }
        if (!addInterfaces(exposedCls, cls)) {
            addExportedMethods(exposedCls, cls);
        }
    }

    private boolean isJavaScriptClass(ClassReader cls) {
        if (typeHelper.isJavaScriptClass(cls.getName())) {
            return true;
        }
        if (cls.getParent() != null && typeHelper.isJavaScriptClass(cls.getParent())) {
            return true;
        }
        for (var itf : cls.getInterfaces()) {
            if (typeHelper.isJavaScriptClass(itf)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJavaScriptImplementation(ClassReader cls) {
        if (typeHelper.isJavaScriptImplementation(cls.getName())) {
            return true;
        }
        if (cls.getAnnotations().get(JSClass.class.getName()) != null || cls.hasModifier(ElementModifier.ABSTRACT)) {
            return false;
        }
        if (cls.getParent() != null) {
            if (typeHelper.isJavaScriptClass(cls.getParent())) {
                return true;
            }
        }
        return cls.getInterfaces().stream().anyMatch(typeHelper::isJavaScriptClass);
    }

    private boolean addInterfaces(ExposedClass exposedCls, ClassReader cls) {
        boolean added = false;
        for (String ifaceName : cls.getInterfaces()) {
            if (exposedCls.implementedInterfaces.contains(ifaceName)) {
                continue;
            }
            ClassReader iface = hierarchy.getClassSource().get(ifaceName);
            if (iface == null) {
                continue;
            }
            if (addInterface(exposedCls, iface)) {
                added = true;
                for (MethodReader method : iface.getMethods()) {
                    if (method.hasModifier(ElementModifier.STATIC)
                            || (method.getProgram() != null && method.getProgram().basicBlockCount() > 0)) {
                        continue;
                    }
                    addExportedMethod(exposedCls, method);
                }
            } else {
                addExportedMethods(exposedCls, iface);
            }
        }
        return added;
    }

    private boolean addInterface(ExposedClass exposedCls, ClassReader cls) {
        if (cls.getName().equals(JS_OBJECT_CLASS)) {
            return true;
        }
        return addInterfaces(exposedCls, cls);
    }

    private void addExportedMethods(ExposedClass exposedCls, ClassReader cls) {
        for (var method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            if (method.getAnnotations().get(JSExport.class.getName()) != null) {
                addExportedMethod(exposedCls, method);
            }
        }
    }

    private void addExportedMethod(ExposedClass exposedCls, MethodReader method) {
        if (!exposedCls.inheritedMethods.contains(method.getDescriptor())) {
            exposedCls.methods.put(method.getDescriptor(), createMethodExport(method));
        }
    }

    private MethodExport createMethodExport(MethodReader method) {
        String name = null;
        MethodKind kind = MethodKind.METHOD;
        if (method.getName().equals("<init>")) {
            kind = MethodKind.CONSTRUCTOR;
        } else {
            var methodAnnot = method.getAnnotations().get(JSMethod.class.getName());
            if (methodAnnot != null) {
                name = method.getName();
                var nameVal = methodAnnot.getValue("value");
                if (nameVal != null) {
                    String nameStr = nameVal.getString();
                    if (!nameStr.isEmpty()) {
                        name = nameStr;
                    }
                }
            } else {
                var propertyAnnot = method.getAnnotations().get(JSProperty.class.getName());
                if (propertyAnnot != null) {
                    var nameVal = propertyAnnot.getValue("value");
                    if (nameVal != null) {
                        String nameStr = nameVal.getString();
                        if (!nameStr.isEmpty()) {
                            name = nameStr;
                        }
                    }
                    String expectedPrefix;
                    if (method.parameterCount() == 0) {
                        if (method.getResultType() == ValueType.BOOLEAN) {
                            expectedPrefix = "is";
                        } else {
                            expectedPrefix = "get";
                        }
                        kind = MethodKind.GETTER;
                    } else {
                        expectedPrefix = "set";
                        kind = MethodKind.SETTER;
                    }

                    if (name == null) {
                        name = method.getName();
                        if (name.startsWith(expectedPrefix) && name.length() > expectedPrefix.length()
                                && Character.isUpperCase(name.charAt(expectedPrefix.length()))) {
                            name = Character.toLowerCase(name.charAt(expectedPrefix.length()))
                                    + name.substring(expectedPrefix.length() + 1);
                        }
                    }
                }
            }
            if (name == null) {
                name = method.getName();
            }
        }
        return new MethodExport(name, kind, method.hasModifier(ElementModifier.VARARGS));
    }

    private void addFunctorField(ClassHolder cls, MethodReference method) {
        if (cls.getAnnotations().get(FunctorImpl.class.getName()) != null) {
            return;
        }

        FieldHolder field = new FieldHolder("$$jso_functor$$");
        field.setLevel(AccessLevel.PUBLIC);
        field.setType(JS_OBJECT);
        cls.addField(field);

        AnnotationHolder annot = new AnnotationHolder(FunctorImpl.class.getName());
        annot.getValues().put("value", new AnnotationValue(method.getDescriptor().toString()));
        cls.getAnnotations().add(annot);
    }

    static class ExposedClass {
        Set<MethodDescriptor> inheritedMethods = new HashSet<>();
        Map<MethodDescriptor, MethodExport> methods = new HashMap<>();
        Set<String> implementedInterfaces = new HashSet<>();
    }

    enum MethodKind {
        METHOD,
        GETTER,
        SETTER,
        CONSTRUCTOR
    }

    static class MethodExport {
        final String alias;
        final MethodKind kind;
        boolean vararg;

        MethodExport(String alias, MethodKind kind, boolean vararg) {
            this.alias = alias;
            this.kind = kind;
            this.vararg = vararg;
        }
    }
}
