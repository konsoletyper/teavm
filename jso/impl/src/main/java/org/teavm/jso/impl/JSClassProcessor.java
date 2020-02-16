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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.teavm.backend.javascript.rendering.JSParser;
import org.teavm.cache.IncrementalDependencyRegistration;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;

class JSClassProcessor {
    private static final String NO_SIDE_EFFECTS = NoSideEffects.class.getName();
    private final ClassReaderSource classSource;
    private final JSBodyRepository repository;
    private final JavaInvocationProcessor javaInvocationProcessor;
    private Program program;
    private final List<Instruction> replacement = new ArrayList<>();
    private final JSTypeHelper typeHelper;
    private final Diagnostics diagnostics;
    private final Map<MethodReference, MethodReader> overriddenMethodCache = new HashMap<>();
    private JSValueMarshaller marshaller;
    private IncrementalDependencyRegistration incrementalCache;

    JSClassProcessor(ClassReaderSource classSource, JSTypeHelper typeHelper, JSBodyRepository repository,
            Diagnostics diagnostics, IncrementalDependencyRegistration incrementalCache) {
        this.classSource = classSource;
        this.typeHelper = typeHelper;
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.incrementalCache = incrementalCache;
        javaInvocationProcessor = new JavaInvocationProcessor(typeHelper, repository, classSource, diagnostics);
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    MethodReference isFunctor(String className) {
        if (!typeHelper.isJavaScriptImplementation(className)) {
            return null;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return null;
        }
        Map<MethodDescriptor, MethodReference> methods = new HashMap<>();
        getFunctorMethods(className, methods);
        if (methods.size() == 1) {
            return methods.values().iterator().next();
        }
        return null;
    }

    private void getFunctorMethods(String className, Map<MethodDescriptor, MethodReference> methods) {
        classSource.getAncestors(className).forEach(cls -> {
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null && marshaller.isProperFunctor(cls)) {
                MethodReference method = cls.getMethods().iterator().next().getReference();
                if (!methods.containsKey(method.getDescriptor())) {
                    methods.put(method.getDescriptor(), method);
                }
            }
        });
    }

    void processClass(ClassHolder cls) {
        Set<MethodDescriptor> preservedMethods = new HashSet<>();
        for (String iface : cls.getInterfaces()) {
            if (typeHelper.isJavaScriptClass(iface)) {
                addPreservedMethods(iface, preservedMethods);
            }
        }
    }

    private void addPreservedMethods(String ifaceName, Set<MethodDescriptor> methods) {
        ClassReader iface = classSource.get(ifaceName);
        for (MethodReader method : iface.getMethods()) {
            methods.add(method.getDescriptor());
        }
        for (String superIfaceName : iface.getInterfaces()) {
            addPreservedMethods(superIfaceName, methods);
        }
    }

    void processMemberMethods(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                ValueType[] staticSignature = getStaticSignature(method.getReference());
                MethodHolder callerMethod = new MethodHolder(new MethodDescriptor(method.getName() + "$static",
                        staticSignature));
                callerMethod.getModifiers().add(ElementModifier.STATIC);
                Program program = ProgramUtils.copy(method.getProgram());
                program.createVariable();
                InstructionVariableMapper variableMapper = new InstructionVariableMapper(var ->
                         program.variableAt(var.getIndex() + 1));
                for (int i = program.variableCount() - 1; i > 0; --i) {
                    program.variableAt(i).setDebugName(program.variableAt(i - 1).getDebugName());
                    program.variableAt(i).setLabel(program.variableAt(i - 1).getLabel());
                }
                for (int i = 0; i < program.basicBlockCount(); ++i) {
                    BasicBlock block = program.basicBlockAt(i);
                    variableMapper.apply(block);
                }
                callerMethod.setProgram(program);
                ModelUtils.copyAnnotations(method.getAnnotations(), callerMethod.getAnnotations());
                cls.addMethod(callerMethod);
            }
        }
    }

    private MethodReader getOverriddenMethod(MethodReader finalMethod) {
        MethodReference ref = finalMethod.getReference();
        if (!overriddenMethodCache.containsKey(ref)) {
            overriddenMethodCache.put(ref, findOverriddenMethod(finalMethod.getOwnerName(), finalMethod));
        }
        return overriddenMethodCache.get(ref);
    }

    private MethodReader findOverriddenMethod(String className, MethodReader finalMethod) {
        if (finalMethod.getName().equals("<init>")) {
            return null;
        }
        return classSource.getAncestors(className)
                .skip(1)
                .map(cls -> cls.getMethod(finalMethod.getDescriptor()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static ValueType[] getStaticSignature(MethodReference method) {
        ValueType[] signature = method.getSignature();
        ValueType[] staticSignature = new ValueType[signature.length + 1];
        for (int i = 0; i < signature.length; ++i) {
            staticSignature[i + 1] = signature[i];
        }
        staticSignature[0] = ValueType.object(method.getClassName());
        return staticSignature;
    }

    private void setCurrentProgram(Program program) {
        this.program = program;
        marshaller = new JSValueMarshaller(diagnostics, typeHelper, classSource, program, replacement);
    }

    void processProgram(MethodHolder methodToProcess) {
        setCurrentProgram(methodToProcess.getProgram());
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (insn instanceof CastInstruction) {
                    replacement.clear();
                    CallLocation callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    if (processCast((CastInstruction) insn, callLocation)) {
                        insn.insertNextAll(replacement);
                        insn.delete();
                    }
                } else if (insn instanceof InvokeInstruction) {
                    InvokeInstruction invoke = (InvokeInstruction) insn;

                    MethodReader method = getMethod(invoke.getMethod().getClassName(),
                            invoke.getMethod().getDescriptor());
                    if (method == null) {
                        continue;
                    }
                    CallLocation callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    replacement.clear();
                    if (processInvocation(method, callLocation, invoke, methodToProcess)) {
                        insn.insertNextAll(replacement);
                        insn.delete();
                    }
                }
            }
        }
    }

    private boolean processCast(CastInstruction cast, CallLocation location) {
        if (!(cast.getTargetType() instanceof ValueType.Object)) {
            return false;
        }

        String targetClassName = ((ValueType.Object) cast.getTargetType()).getClassName();
        if (!typeHelper.isJavaScriptClass(targetClassName)) {
            return false;
        }
        ClassReader targetClass = classSource.get(targetClassName);
        if (targetClass.getAnnotations().get(JSFunctor.class.getName()) == null) {
            return false;
        }

        Variable result = marshaller.unwrapFunctor(location, cast.getValue(), targetClass);
        AssignInstruction assign = new AssignInstruction();
        assign.setLocation(location.getSourceLocation());
        assign.setAssignee(result);
        assign.setReceiver(cast.getReceiver());
        replacement.add(assign);

        return true;
    }

    private boolean processInvocation(MethodReader method, CallLocation callLocation, InvokeInstruction invoke,
            MethodHolder methodToProcess) {
        if (method.getAnnotations().get(JSBody.class.getName()) != null) {
            return processJSBodyInvocation(method, callLocation, invoke, methodToProcess);
        }

        if (!typeHelper.isJavaScriptClass(invoke.getMethod().getClassName())) {
            return false;
        }

        if (method.hasModifier(ElementModifier.STATIC)) {
            return false;
        }

        if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
            MethodReader overridden = getOverriddenMethod(method);
            if (overridden != null) {
                diagnostics.error(callLocation, "JS final method {{m0}} overrides {{m1}}. "
                        + "Overriding final method of overlay types is prohibited.",
                        method.getReference(), overridden.getReference());
            }
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                invoke.setMethod(new MethodReference(method.getOwnerName(), method.getName() + "$static",
                        getStaticSignature(method.getReference())));
                Variable[] newArguments = new Variable[invoke.getArguments().size() + 1];
                newArguments[0] = invoke.getInstance();
                for (int i = 0; i < invoke.getArguments().size(); ++i) {
                    newArguments[i + 1] = invoke.getArguments().get(i);
                }
                invoke.setArguments(newArguments);
                invoke.setInstance(null);
            }
            invoke.setType(InvocationType.SPECIAL);
            return false;
        }

        if (method.getAnnotations().get(JSProperty.class.getName()) != null) {
            return processProperty(method, callLocation, invoke);
        } else if (method.getAnnotations().get(JSIndexer.class.getName()) != null) {
            return processIndexer(method, callLocation, invoke);
        } else {
            return processMethod(method, callLocation, invoke);
        }
    }

    private boolean processJSBodyInvocation(MethodReader method, CallLocation callLocation, InvokeInstruction invoke,
            MethodHolder methodToProcess) {
        boolean[] byRefParams = new boolean[method.parameterCount()];
        validateSignature(method, callLocation, byRefParams);
        if (invoke.getInstance() != null) {
            if (!typeHelper.isSupportedType(ValueType.object(method.getOwnerName()))) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + "declaration. It is non-static and declared on a non-overlay class {{c1}}",
                        invoke.getMethod(), method.getOwnerName());
            }
        }

        boolean returnByRef = method.getAnnotations().get(JSByRef.class.getName()) != null;
        if (returnByRef && !typeHelper.isSupportedByRefType(method.getResultType())) {
            diagnostics.error(callLocation, "Method {{m0}} is marked with @JSByRef, but does not return valid "
                    + "array type", method.getReference());
            return false;
        }

        requireJSBody(diagnostics, method);
        MethodReference delegate = repository.methodMap.get(method.getReference());
        if (delegate == null) {
            return false;
        }

        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
        InvokeInstruction newInvoke = new InvokeInstruction();
        newInvoke.setMethod(delegate);
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);
        newInvoke.setLocation(invoke.getLocation());
        List<Variable> newArgs = new ArrayList<>();
        if (invoke.getInstance() != null) {
            Variable arg = marshaller.wrapArgument(callLocation, invoke.getInstance(),
                    ValueType.object(method.getOwnerName()), false);
            newArgs.add(arg);
        }
        for (int i = 0; i < invoke.getArguments().size(); ++i) {
            Variable arg = marshaller.wrapArgument(callLocation, invoke.getArguments().get(i),
                    method.parameterType(i), byRefParams[i]);
            newArgs.add(arg);
        }
        newInvoke.setArguments(newArgs.toArray(new Variable[0]));
        replacement.add(newInvoke);
        if (result != null) {
            result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), returnByRef);
            copyVar(result, invoke.getReceiver(), invoke.getLocation());
        }

        incrementalCache.addDependencies(methodToProcess.getReference(), method.getOwnerName());

        return true;
    }

    private boolean processProperty(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        boolean pure = method.getAnnotations().get(NO_SIDE_EFFECTS) != null;
        if (isProperGetter(method)) {
            String propertyName = extractSuggestedPropertyName(method);
            if (propertyName == null) {
                propertyName = method.getName().charAt(0) == 'i' ? cutPrefix(method.getName(), 2)
                        : cutPrefix(method.getName(), 3);
            }
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            addPropertyGet(propertyName, invoke.getInstance(), result, invoke.getLocation(), pure);
            if (result != null) {
                result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false);
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetter(method)) {
            String propertyName = extractSuggestedPropertyName(method);
            if (propertyName == null) {
                propertyName = cutPrefix(method.getName(), 3);
            }
            Variable wrapped = marshaller.wrapArgument(callLocation, invoke.getArguments().get(0),
                    method.parameterType(0), false);
            addPropertySet(propertyName, invoke.getInstance(), wrapped, invoke.getLocation(), pure);
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript property "
                + "declaration", invoke.getMethod());
        return false;
    }

    private String extractSuggestedPropertyName(MethodReader method) {
        AnnotationReader annot = method.getAnnotations().get(JSProperty.class.getName());
        AnnotationValue value = annot.getValue("value");
        return value != null ? value.getString() : null;
    }

    private boolean processIndexer(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        if (isProperGetIndexer(method.getDescriptor())) {
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            addIndexerGet(invoke.getInstance(), marshaller.wrapArgument(callLocation, invoke.getArguments().get(0),
                    method.parameterType(0), false), result, invoke.getLocation());
            if (result != null) {
                result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false);
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetIndexer(method.getDescriptor())) {
            Variable index = marshaller.wrapArgument(callLocation, invoke.getArguments().get(0),
                    method.parameterType(0), false);
            Variable value = marshaller.wrapArgument(callLocation, invoke.getArguments().get(1),
                    method.parameterType(1), false);
            addIndexerSet(invoke.getInstance(), index, value, invoke.getLocation());
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript indexer "
                + "declaration", invoke.getMethod());
        return false;
    }

    private boolean validateSignature(MethodReader method, CallLocation callLocation, boolean[] byRefParams) {
        if (method.getResultType() != ValueType.VOID && !typeHelper.isSupportedType(method.getResultType())) {
            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                    + "declaration, since it returns wrong type", method.getReference());
            return false;
        }

        ValueType[] parameterTypes = method.getParameterTypes();
        AnnotationContainerReader[] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterTypes.length; i++) {
            ValueType paramType = parameterTypes[i];
            if (!typeHelper.isSupportedType(paramType)) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + "declaration: its " + (i + 1) + "th parameter has wrong type", method.getReference());
                return false;
            }
            if (parameterAnnotations[i].get(JSByRef.class.getName()) != null) {
                if (!typeHelper.isSupportedByRefType(paramType)) {
                    diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                            + "declaration: its " + (i + 1) + "th parameter is declared as JSByRef, "
                            + "which has incompatible type", method.getReference());
                    return false;
                }
                byRefParams[i] = true;
            }
        }

        return true;
    }

    private boolean processMethod(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        String name = method.getName();

        AnnotationReader methodAnnot = method.getAnnotations().get(JSMethod.class.getName());
        if (methodAnnot != null) {
            AnnotationValue redefinedMethodName = methodAnnot.getValue("value");
            if (redefinedMethodName != null) {
                name = redefinedMethodName.getString();
            }
        }

        boolean[] byRefParams = new boolean[method.parameterCount() + 1];
        if (!validateSignature(method, callLocation, byRefParams)) {
            return false;
        }

        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
        InvokeInstruction newInvoke = new InvokeInstruction();
        newInvoke.setMethod(JSMethods.invoke(method.parameterCount()));
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);
        List<Variable> newArguments = new ArrayList<>();
        newArguments.add(invoke.getInstance());
        newArguments.add(marshaller.addStringWrap(marshaller.addString(name, invoke.getLocation()),
                invoke.getLocation()));
        newInvoke.setLocation(invoke.getLocation());
        for (int i = 0; i < invoke.getArguments().size(); ++i) {
            Variable arg = marshaller.wrapArgument(callLocation, invoke.getArguments().get(i),
                    method.parameterType(i), byRefParams[i]);
            newArguments.add(arg);
        }
        newInvoke.setArguments(newArguments.toArray(new Variable[0]));
        replacement.add(newInvoke);
        if (result != null) {
            result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false);
            copyVar(result, invoke.getReceiver(), invoke.getLocation());
        }

        return true;
    }

    private void requireJSBody(Diagnostics diagnostics, MethodReader methodToProcess) {
        if (!repository.processedMethods.add(methodToProcess.getReference())) {
            return;
        }
        processJSBody(diagnostics, methodToProcess);
    }

    private void processJSBody(Diagnostics diagnostics, MethodReader methodToProcess) {
        CallLocation location = new CallLocation(methodToProcess.getReference());
        boolean isStatic = methodToProcess.hasModifier(ElementModifier.STATIC);

        // validate parameter names
        AnnotationReader bodyAnnot = methodToProcess.getAnnotations().get(JSBody.class.getName());
        AnnotationValue paramsValue = bodyAnnot.getValue("params");
        int jsParamCount = paramsValue != null ? paramsValue.getList().size() : 0;
        if (methodToProcess.parameterCount() != jsParamCount) {
            diagnostics.error(location, "JSBody method {{m0}} declares " + methodToProcess.parameterCount()
                    + " parameters, but annotation specifies " + jsParamCount, methodToProcess.getReference());
            return;
        }

        // generate parameter types for original method and validate
        int paramCount = methodToProcess.parameterCount();
        if (!isStatic) {
            ++paramCount;
        }
        if (!isStatic) {
            ValueType paramType = ValueType.object(methodToProcess.getOwnerName());
            if (!typeHelper.isSupportedType(paramType)) {
                diagnostics.error(location, "Non-static JSBody method {{m0}} is owned by non-JS class {{c1}}",
                        methodToProcess.getReference(), methodToProcess.getOwnerName());
            }
        }
        if (methodToProcess.getResultType() != ValueType.VOID
                && !typeHelper.isSupportedType(methodToProcess.getResultType())) {
            diagnostics.error(location, "JSBody method {{m0}} returns unsupported type {{t1}}",
                    methodToProcess.getReference(), methodToProcess.getResultType());
        }

        // generate parameter types for proxy method
        ValueType[] proxyParamTypes = new ValueType[paramCount + 1];
        for (int i = 0; i < paramCount; ++i) {
            proxyParamTypes[i] = ValueType.parse(JSObject.class);
        }
        proxyParamTypes[paramCount] = methodToProcess.getResultType() == ValueType.VOID
                ? ValueType.VOID
                : ValueType.parse(JSObject.class);

        ClassReader ownerClass = classSource.get(methodToProcess.getOwnerName());
        int methodIndex = indexOfMethod(ownerClass, methodToProcess);

        // create proxy method
        MethodReference proxyMethod = new MethodReference(methodToProcess.getOwnerName(),
                methodToProcess.getName() + "$js_body$_" + methodIndex, proxyParamTypes);
        String script = bodyAnnot.getValue("script").getString();
        String[] parameterNames = paramsValue != null ? paramsValue.getList().stream()
                .map(AnnotationValue::getString)
                .toArray(String[]::new) : new String[0];

        // Parse JS script
        TeaVMErrorReporter errorReporter = new TeaVMErrorReporter(diagnostics,
                new CallLocation(methodToProcess.getReference()));
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        env.setIdeMode(true);
        JSParser parser = new JSParser(env, errorReporter);
        AstRoot rootNode;
        try {
            rootNode = (AstRoot) parser.parseAsObject(new StringReader("function(){" + script + "}"), null, 0);
        } catch (IOException e) {
            throw new RuntimeException("IO Error occurred", e);
        }
        AstNode body = ((FunctionNode) rootNode.getFirstChild()).getBody();

        repository.methodMap.put(methodToProcess.getReference(), proxyMethod);
        if (errorReporter.hasErrors()) {
            repository.emitters.put(proxyMethod, new JSBodyBloatedEmitter(isStatic, proxyMethod,
                    script, parameterNames));
        } else {
            AstNode expr = JSBodyInlineUtil.isSuitableForInlining(methodToProcess.getReference(),
                    parameterNames, body);
            if (expr != null) {
                repository.inlineMethods.add(methodToProcess.getReference());
            } else {
                expr = body;
            }
            javaInvocationProcessor.process(location, expr);
            repository.emitters.put(proxyMethod, new JSBodyAstEmitter(isStatic, expr, parameterNames));
        }
    }

    private int indexOfMethod(ClassReader cls, MethodReader method) {
        int index = 0;
        for (MethodReader m : cls.getMethods()) {
            if (m.getDescriptor().equals(method.getDescriptor())) {
                return index;
            }
            ++index;
        }
        return -1;
    }

    void createJSMethods(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            MethodReference methodRef = method.getReference();
            if (method.getAnnotations().get(JSBody.class.getName()) == null) {
                continue;
            }

            requireJSBody(diagnostics, method);
            if (!repository.methodMap.containsKey(method.getReference())) {
                continue;
            }

            MethodReference proxyRef = repository.methodMap.get(methodRef);
            MethodHolder proxyMethod = new MethodHolder(proxyRef.getDescriptor());
            proxyMethod.getModifiers().add(ElementModifier.NATIVE);
            proxyMethod.getModifiers().add(ElementModifier.STATIC);
            if (method.getAnnotations().get(NoSideEffects.class.getName()) != null) {
                proxyMethod.getAnnotations().add(new AnnotationHolder(NoSideEffects.class.getName()));
            }
            boolean inline = repository.inlineMethods.contains(methodRef);
            AnnotationHolder generatorAnnot = new AnnotationHolder(inline
                    ? DynamicInjector.class.getName() : DynamicGenerator.class.getName());
            proxyMethod.getAnnotations().add(generatorAnnot);
            cls.addMethod(proxyMethod);

            Set<MethodReference> callbacks = repository.callbackMethods.get(proxyRef);
            if (callbacks != null) {
                for (MethodReference callback : callbacks) {
                    generateCallbackCaller(cls, callback);
                }
            }
        }
    }

    private void generateCallbackCaller(ClassHolder cls, MethodReference callback) {
        MethodReference calleeRef = repository.callbackCallees.get(callback);
        MethodReader callee = classSource.resolve(calleeRef);
        MethodHolder callerMethod = new MethodHolder(callback.getDescriptor());
        callerMethod.getModifiers().add(ElementModifier.STATIC);
        CallLocation location = new CallLocation(callback);

        setCurrentProgram(new Program());
        for (int i = 0; i <= callback.parameterCount(); ++i) {
            program.createVariable();
        }
        BasicBlock block = program.createBasicBlock();

        int paramIndex = 1;
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(calleeRef);
        replacement.clear();
        if (!callee.hasModifier(ElementModifier.STATIC)) {
            insn.setInstance(marshaller.unwrapReturnValue(location, program.variableAt(paramIndex++),
                    ValueType.object(calleeRef.getClassName()), false));
        }
        Variable[] args = new Variable[callee.parameterCount()];
        for (int i = 0; i < callee.parameterCount(); ++i) {
            args[i] = marshaller.unwrapReturnValue(location, program.variableAt(paramIndex++),
                    callee.parameterType(i), false);
        }
        insn.setArguments(args);
        if (callee.getResultType() != ValueType.VOID) {
            insn.setReceiver(program.createVariable());
        }
        block.addAll(replacement);
        block.add(insn);

        ExitInstruction exit = new ExitInstruction();
        if (insn.getReceiver() != null) {
            replacement.clear();
            exit.setValueToReturn(marshaller.wrap(insn.getReceiver(), callee.getResultType(), null, false));
            block.addAll(replacement);
        }
        block.add(exit);

        callerMethod.setProgram(program);
        cls.addMethod(callerMethod);
        processProgram(callerMethod);
    }

    private void addPropertyGet(String propertyName, Variable instance, Variable receiver,
            TextLocation location, boolean pure) {
        Variable nameVar = marshaller.addStringWrap(marshaller.addString(propertyName, location), location);
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(pure ? JSMethods.GET_PURE : JSMethods.GET);
        insn.setReceiver(receiver);
        insn.setArguments(instance, nameVar);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addPropertySet(String propertyName, Variable instance, Variable value, TextLocation location,
            boolean pure) {
        Variable nameVar = marshaller.addStringWrap(marshaller.addString(propertyName, location), location);
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(pure ? JSMethods.SET_PURE : JSMethods.SET);
        insn.setArguments(instance, nameVar, value);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addIndexerGet(Variable array, Variable index, Variable receiver, TextLocation location) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(JSMethods.GET);
        insn.setReceiver(receiver);
        insn.setArguments(array, index);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addIndexerSet(Variable array, Variable index, Variable value, TextLocation location) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(JSMethods.SET);
        insn.setArguments(array, index, value);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void copyVar(Variable a, Variable b, TextLocation location) {
        AssignInstruction insn = new AssignInstruction();
        insn.setAssignee(a);
        insn.setReceiver(b);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private MethodReader getMethod(String className, MethodDescriptor descriptor) {
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return null;
        }
        MethodReader method = cls.getMethod(descriptor);
        if (method != null) {
            return method;
        }
        if (cls.getParent() != null && !cls.getParent().equals("java.lang.Object")) {
            method = getMethod(cls.getParent(), descriptor);
            if (method != null) {
                return method;
            }
        }
        for (String iface : cls.getInterfaces()) {
            method = getMethod(iface, descriptor);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private boolean isProperGetter(MethodReader method) {
        if (method.parameterCount() > 0 || !typeHelper.isSupportedType(method.getResultType())) {
            return false;
        }
        if (extractSuggestedPropertyName(method) != null) {
            return true;
        }

        if (method.getResultType().equals(ValueType.BOOLEAN)) {
            if (isProperPrefix(method.getName(), "is")) {
                return true;
            }
        }
        return isProperPrefix(method.getName(), "get");
    }

    private boolean isProperSetter(MethodReader method) {
        if (method.parameterCount() != 1 || !typeHelper.isSupportedType(method.parameterType(0))
                || method.getResultType() != ValueType.VOID) {
            return false;
        }

        return extractSuggestedPropertyName(method) != null || isProperPrefix(method.getName(), "set");
    }

    private boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c) || !Character.isAlphabetic(c) && Character.isJavaIdentifierStart(c);
    }

    private boolean isProperGetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 1 && typeHelper.isSupportedType(desc.parameterType(0))
                && typeHelper.isSupportedType(desc.getResultType());
    }

    private boolean isProperSetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 2 && typeHelper.isSupportedType(desc.parameterType(0))
                && typeHelper.isSupportedType(desc.parameterType(1)) && desc.getResultType() == ValueType.VOID;
    }

    private static String cutPrefix(String name, int prefixLength) {
        if (name.length() == prefixLength + 1) {
            return name.substring(prefixLength).toLowerCase();
        }
        char c = name.charAt(prefixLength + 1);
        if (Character.isUpperCase(c)) {
            return name.substring(prefixLength);
        }
        return Character.toLowerCase(name.charAt(prefixLength)) + name.substring(prefixLength + 1);
    }
}
