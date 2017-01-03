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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.cache.NoCache;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Sync;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
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
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;

class JSClassProcessor {
    private final ClassReaderSource classSource;
    private final JSBodyRepository repository;
    private final JavaInvocationProcessor javaInvocationProcessor;
    private Program program;
    private final List<Instruction> replacement = new ArrayList<>();
    private final JSTypeHelper typeHelper;
    private final Diagnostics diagnostics;
    private int methodIndexGenerator;
    private final Map<MethodReference, MethodReader> overridenMethodCache = new HashMap<>();

    public JSClassProcessor(ClassReaderSource classSource, JSBodyRepository repository, Diagnostics diagnostics) {
        this.classSource = classSource;
        this.repository = repository;
        this.diagnostics = diagnostics;
        typeHelper = new JSTypeHelper(classSource);
        javaInvocationProcessor = new JavaInvocationProcessor(typeHelper, repository, classSource, diagnostics);
    }

    public ClassReaderSource getClassSource() {
        return classSource;
    }

    public boolean isNative(String className) {
        return typeHelper.isJavaScriptClass(className);
    }

    public boolean isNativeImplementation(String className) {
        return typeHelper.isJavaScriptImplementation(className);
    }

    public MethodReference isFunctor(String className) {
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
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null && isProperFunctor(cls)) {
                MethodReference method = cls.getMethods().iterator().next().getReference();
                if (!methods.containsKey(method.getDescriptor())) {
                    methods.put(method.getDescriptor(), method);
                }
            }
        });
    }

    public void processClass(ClassHolder cls) {
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

    public void processMemberMethods(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                ValueType[] staticSignature = getStaticSignature(method.getReference());
                MethodHolder callerMethod = new MethodHolder(new MethodDescriptor(method.getName() + "$static",
                        staticSignature));
                callerMethod.getModifiers().add(ElementModifier.STATIC);
                final Program program = ProgramUtils.copy(method.getProgram());
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

    private MethodReader getOverridenMethod(MethodReader finalMethod) {
        MethodReference ref = finalMethod.getReference();
        if (!overridenMethodCache.containsKey(ref)) {
            overridenMethodCache.put(ref, findOverridenMethod(finalMethod.getOwnerName(), finalMethod));
        }
        return overridenMethodCache.get(ref);
    }

    private MethodReader findOverridenMethod(String className, MethodReader finalMethod) {
        if (finalMethod.getName().equals("<init>")) {
            return null;
        }
        return classSource.getAncestors(className)
                .skip(1)
                .map(cls -> cls.getMethod(finalMethod.getDescriptor()))
                .filter(method -> method != null)
                .findFirst()
                .orElse(null);
    }

    public void addFunctorField(ClassHolder cls, MethodReference method) {
        if (cls.getAnnotations().get(FunctorImpl.class.getName()) != null) {
            return;
        }

        FieldHolder field = new FieldHolder("$$jso_functor$$");
        field.setLevel(AccessLevel.PUBLIC);
        field.setType(ValueType.parse(JSObject.class));
        cls.addField(field);

        AnnotationHolder annot = new AnnotationHolder(FunctorImpl.class.getName());
        annot.getValues().put("value", new AnnotationValue(method.getDescriptor().toString()));
        cls.getAnnotations().add(annot);
    }

    public void makeSync(ClassHolder cls) {
        Set<MethodDescriptor> methods = new HashSet<>();
        findInheritedMethods(cls, methods, new HashSet<>());
        for (MethodHolder method : cls.getMethods()) {
            if (methods.contains(method.getDescriptor()) && method.getAnnotations().get(Sync.class.getName()) == null) {
                AnnotationHolder annot = new AnnotationHolder(Sync.class.getName());
                method.getAnnotations().add(annot);
            }
        }
    }

    private void findInheritedMethods(ClassReader cls, Set<MethodDescriptor> methods, Set<String> visited) {
        if (!visited.add(cls.getName())) {
            return;
        }
        if (isNative(cls.getName())) {
            for (MethodReader method : cls.getMethods()) {
                if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.FINAL)
                        && method.getLevel() != AccessLevel.PRIVATE) {
                    methods.add(method.getDescriptor());
                }
            }
        } else if (isNativeImplementation(cls.getName())) {
            if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
                ClassReader parentCls = classSource.get(cls.getParent());
                if (parentCls != null) {
                    findInheritedMethods(parentCls, methods, visited);
                }
            }
            for (String iface : cls.getInterfaces()) {
                ClassReader parentCls = classSource.get(iface);
                if (parentCls != null) {
                    findInheritedMethods(parentCls, methods, visited);
                }
            }
        }
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

    void processProgram(MethodHolder methodToProcess) {
        program = methodToProcess.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) insn;

                MethodReader method = getMethod(invoke.getMethod());
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
            MethodReader overridden = getOverridenMethod(method);
            if (overridden != null) {
                diagnostics.error(callLocation, "JS final method {{m0}} overrides {{m1}}. "
                        + "Overriding final method of overlay types is prohibited.",
                        method.getReference(), overridden.getReference());
            }
            if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
                invoke.setMethod(new MethodReference(method.getOwnerName(), method.getName() + "$static",
                        getStaticSignature(method.getReference())));
                invoke.getArguments().add(0, invoke.getInstance());
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
        boolean valid = true;
        for (int i = 0; i < method.parameterCount(); ++i) {
            ValueType arg = method.parameterType(i);
            if (!typeHelper.isSupportedType(arg)) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + " declaration. Its parameter #" + (i + 1) + " has invalid type {{t1}}", invoke.getMethod(),
                        arg);
                valid = false;
            }
        }
        if (invoke.getInstance() != null) {
            if (!typeHelper.isSupportedType(ValueType.object(method.getOwnerName()))) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + " declaration. It is non-static and declared on a non-overlay class {{c1}}",
                        invoke.getMethod(), method.getOwnerName());
                valid = false;
            }
        }
        if (method.getResultType() != ValueType.VOID && !typeHelper.isSupportedType(method.getResultType())) {
            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                    + " declaration, since it returns invalid type {{t1}}", invoke.getMethod(),
                    method.getResultType());
            valid = false;
        }
        if (!valid) {
            return false;
        }

        requireJSBody(diagnostics, method);
        MethodReference delegate = repository.methodMap.get(method.getReference());
        if (delegate == null) {
            return false;
        }

        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
        InvokeInstruction newInvoke = new InvokeInstruction();
        ValueType[] signature = new ValueType[method.parameterCount() + 3];
        Arrays.fill(signature, ValueType.object(JSObject.class.getName()));
        newInvoke.setMethod(delegate);
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);
        newInvoke.setLocation(invoke.getLocation());
        if (invoke.getInstance() != null) {
            Variable arg = wrapArgument(callLocation, invoke.getInstance(), ValueType.object(method.getOwnerName()));
            newInvoke.getArguments().add(arg);
        }
        for (int k = 0; k < invoke.getArguments().size(); ++k) {
            Variable arg = wrapArgument(callLocation, invoke.getArguments().get(k),
                    method.parameterType(k));
            newInvoke.getArguments().add(arg);
        }
        replacement.add(newInvoke);
        if (result != null) {
            result = unwrap(callLocation, result, method.getResultType());
            copyVar(result, invoke.getReceiver(), invoke.getLocation());
        }

        if (methodToProcess.getAnnotations().get(NoCache.class.getName()) == null) {
            methodToProcess.getAnnotations().add(new AnnotationHolder(NoCache.class.getName()));
        }

        return true;
    }

    private boolean processProperty(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        if (isProperGetter(method.getDescriptor())) {
            String propertyName;
            AnnotationReader annot = method.getAnnotations().get(JSProperty.class.getName());
            if (annot.getValue("value") != null) {
                propertyName = annot.getValue("value").getString();
            } else {
                propertyName = method.getName().charAt(0) == 'i' ? cutPrefix(method.getName(), 2)
                        : cutPrefix(method.getName(), 3);
            }
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            addPropertyGet(propertyName, invoke.getInstance(), result, invoke.getLocation());
            if (result != null) {
                result = unwrap(callLocation, result, method.getResultType());
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetter(method.getDescriptor())) {
            String propertyName;
            AnnotationReader annot = method.getAnnotations().get(JSProperty.class.getName());
            if (annot.getValue("value") != null) {
                propertyName = annot.getValue("value").getString();
            } else {
                propertyName = cutPrefix(method.getName(), 3);
            }
            Variable wrapped = wrapArgument(callLocation, invoke.getArguments().get(0),
                    method.parameterType(0));
            addPropertySet(propertyName, invoke.getInstance(), wrapped, invoke.getLocation());
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript property "
                + "declaration", invoke.getMethod());
        return false;
    }

    private boolean processIndexer(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        if (isProperGetIndexer(method.getDescriptor())) {
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            addIndexerGet(invoke.getInstance(), wrap(invoke.getArguments().get(0),
                    method.parameterType(0), invoke.getLocation()), result, invoke.getLocation());
            if (result != null) {
                result = unwrap(callLocation, result, method.getResultType());
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetIndexer(method.getDescriptor())) {
            Variable index = wrap(invoke.getArguments().get(0), method.parameterType(0),
                    invoke.getLocation());
            Variable value = wrap(invoke.getArguments().get(1), method.parameterType(1),
                    invoke.getLocation());
            addIndexerSet(invoke.getInstance(), index, value, invoke.getLocation());
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript indexer "
                + "declaration", invoke.getMethod());
        return false;
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
        if (method.getResultType() != ValueType.VOID && !typeHelper.isSupportedType(method.getResultType())) {
            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                    + "declaration", invoke.getMethod());
            return false;
        }

        for (ValueType arg : method.getParameterTypes()) {
            if (!typeHelper.isSupportedType(arg)) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + " declaration", invoke.getMethod());
                return false;
            }
        }

        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
        InvokeInstruction newInvoke = new InvokeInstruction();
        ValueType[] signature = new ValueType[method.parameterCount() + 3];
        Arrays.fill(signature, ValueType.object(JSObject.class.getName()));
        newInvoke.setMethod(new MethodReference(JS.class.getName(), "invoke", signature));
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);
        newInvoke.getArguments().add(invoke.getInstance());
        newInvoke.getArguments().add(addStringWrap(addString(name, invoke.getLocation()),
                invoke.getLocation()));
        newInvoke.setLocation(invoke.getLocation());
        for (int k = 0; k < invoke.getArguments().size(); ++k) {
            Variable arg = wrapArgument(callLocation, invoke.getArguments().get(k),
                    method.parameterType(k));
            newInvoke.getArguments().add(arg);
        }
        replacement.add(newInvoke);
        if (result != null) {
            result = unwrap(callLocation, result, method.getResultType());
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
        int jsParamCount = bodyAnnot.getValue("params").getList().size();
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

        // create proxy method
        MethodReference proxyMethod = new MethodReference(methodToProcess.getOwnerName(),
                methodToProcess.getName() + "$js_body$_" + methodIndexGenerator++, proxyParamTypes);
        String script = bodyAnnot.getValue("script").getString();
        String[] parameterNames = bodyAnnot.getValue("params").getList().stream()
                .map(AnnotationValue::getString)
                .toArray(String[]::new);

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
            rootNode = parser.parse(new StringReader("function(){" + script + "}"), null, 0);
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

    public void createJSMethods(ClassHolder cls) {
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
            boolean inline = repository.inlineMethods.contains(methodRef);
            AnnotationHolder generatorAnnot = new AnnotationHolder(inline
                    ? InjectedBy.class.getName() : GeneratedBy.class.getName());
            generatorAnnot.getValues().put("value",
                    new AnnotationValue(ValueType.parse(JSBodyGenerator.class)));
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

        program = new Program();
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
            insn.setInstance(unwrap(location, program.variableAt(paramIndex++),
                    ValueType.object(calleeRef.getClassName())));
        }
        for (int i = 0; i < callee.parameterCount(); ++i) {
            insn.getArguments().add(unwrap(location, program.variableAt(paramIndex++), callee.parameterType(i)));
        }
        if (callee.getResultType() != ValueType.VOID) {
            insn.setReceiver(program.createVariable());
        }
        block.addAll(replacement);
        block.add(insn);

        ExitInstruction exit = new ExitInstruction();
        if (insn.getReceiver() != null) {
            replacement.clear();
            exit.setValueToReturn(wrap(insn.getReceiver(), callee.getResultType(), null));
            block.addAll(replacement);
        }
        block.add(exit);

        callerMethod.setProgram(program);
        cls.addMethod(callerMethod);
        processProgram(callerMethod);
    }

    private void addPropertyGet(String propertyName, Variable instance, Variable receiver,
            TextLocation location) {
        Variable nameVar = addStringWrap(addString(propertyName, location), location);
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "get", JSObject.class, JSObject.class, JSObject.class));
        insn.setReceiver(receiver);
        insn.getArguments().add(instance);
        insn.getArguments().add(nameVar);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addPropertySet(String propertyName, Variable instance, Variable value, TextLocation location) {
        Variable nameVar = addStringWrap(addString(propertyName, location), location);
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "set", JSObject.class, JSObject.class,
                JSObject.class, void.class));
        insn.getArguments().add(instance);
        insn.getArguments().add(nameVar);
        insn.getArguments().add(value);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addIndexerGet(Variable array, Variable index, Variable receiver, TextLocation location) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "get", JSObject.class, JSObject.class, JSObject.class));
        insn.setReceiver(receiver);
        insn.getArguments().add(array);
        insn.getArguments().add(index);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addIndexerSet(Variable array, Variable index, Variable value, TextLocation location) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "set", JSObject.class, JSObject.class,
                JSObject.class, void.class));
        insn.getArguments().add(array);
        insn.getArguments().add(index);
        insn.getArguments().add(value);
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

    private Variable addStringWrap(Variable var, TextLocation location) {
        return wrap(var, ValueType.object("java.lang.String"), location);
    }

    private Variable addString(String str, TextLocation location) {
        Variable var = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setReceiver(var);
        nameInsn.setConstant(str);
        nameInsn.setLocation(location);
        replacement.add(nameInsn);
        return var;
    }

    private Variable unwrap(CallLocation location, Variable var, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return unwrap(var, "unwrapBoolean", ValueType.parse(JSObject.class), ValueType.BOOLEAN,
                            location.getSourceLocation());
                case BYTE:
                    return unwrap(var, "unwrapByte", ValueType.parse(JSObject.class), ValueType.BYTE,
                            location.getSourceLocation());
                case SHORT:
                    return unwrap(var, "unwrapShort", ValueType.parse(JSObject.class), ValueType.SHORT,
                            location.getSourceLocation());
                case INTEGER:
                    return unwrap(var, "unwrapInt", ValueType.parse(JSObject.class), ValueType.INTEGER,
                            location.getSourceLocation());
                case CHARACTER:
                    return unwrap(var, "unwrapCharacter", ValueType.parse(JSObject.class), ValueType.CHARACTER,
                            location.getSourceLocation());
                case DOUBLE:
                    return unwrap(var, "unwrapDouble", ValueType.parse(JSObject.class), ValueType.DOUBLE,
                            location.getSourceLocation());
                case FLOAT:
                    return unwrap(var, "unwrapFloat", ValueType.parse(JSObject.class), ValueType.FLOAT,
                            location.getSourceLocation());
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", ValueType.parse(JSObject.class), ValueType.parse(String.class),
                        location.getSourceLocation());
            } else if (isNative(className)) {
                Variable result = program.createVariable();
                CastInstruction castInsn = new CastInstruction();
                castInsn.setReceiver(result);
                castInsn.setValue(var);
                castInsn.setTargetType(type);
                castInsn.setLocation(location.getSourceLocation());
                replacement.add(castInsn);
                return result;
            }
        } else if (type instanceof ValueType.Array) {
            return unwrapArray(location, var, (ValueType.Array) type);
        }
        diagnostics.error(location, "Unsupported type: {{t0}}", type);
        return var;
    }

    private Variable unwrapArray(CallLocation location, Variable var, ValueType.Array type) {
        ValueType itemType = type;
        int degree = 0;
        while (itemType instanceof ValueType.Array) {
            ++degree;
            itemType = ((ValueType.Array) itemType).getItemType();
        }

        CastInstruction castInsn = new CastInstruction();
        castInsn.setValue(var);
        castInsn.setTargetType(ValueType.parse(JSArrayReader.class));
        var = program.createVariable();
        castInsn.setReceiver(var);
        castInsn.setLocation(location.getSourceLocation());
        replacement.add(castInsn);

        var = degree == 1
                ? unwrapSingleDimensionArray(location, var, itemType)
                : unwrapMultiDimensionArray(location, var, itemType, degree);

        return var;
    }

    private Variable unwrapSingleDimensionArray(CallLocation location, Variable var, ValueType type) {
        Variable result = program.createVariable();

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(singleDimensionArrayUnwrapper(type));
        insn.setType(InvocationType.SPECIAL);

        if (insn.getMethod().parameterCount() == 2) {
            Variable cls = program.createVariable();
            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);
            insn.getArguments().add(cls);
        }

        insn.getArguments().add(var);
        insn.setReceiver(result);
        replacement.add(insn);
        return result;
    }

    private Variable unwrapMultiDimensionArray(CallLocation location, Variable var, ValueType type, int degree) {
        Variable function = program.createVariable();

        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(multipleDimensionArrayUnwrapper(type));
        insn.setType(InvocationType.SPECIAL);

        if (insn.getMethod().parameterCount() == 1) {
            Variable cls = program.createVariable();
            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);
            insn.getArguments().add(cls);
        }

        insn.setReceiver(function);
        replacement.add(insn);

        while (--degree > 1) {
            type = ValueType.arrayOf(type);
            Variable cls = program.createVariable();

            ClassConstantInstruction clsInsn = new ClassConstantInstruction();
            clsInsn.setConstant(type);
            clsInsn.setLocation(location.getSourceLocation());
            clsInsn.setReceiver(cls);
            replacement.add(clsInsn);

            insn = new InvokeInstruction();
            insn.setMethod(new MethodReference(JS.class, "arrayUnmapper", Class.class, Function.class,
                    Function.class));
            insn.setType(InvocationType.SPECIAL);
            insn.getArguments().add(cls);
            insn.getArguments().add(function);
            function = program.createVariable();
            insn.setReceiver(function);
            replacement.add(insn);
        }

        Variable cls = program.createVariable();
        ClassConstantInstruction clsInsn = new ClassConstantInstruction();
        clsInsn.setConstant(ValueType.arrayOf(type));
        clsInsn.setLocation(location.getSourceLocation());
        clsInsn.setReceiver(cls);
        replacement.add(clsInsn);

        insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class, "unmapArray", Class.class, JSArrayReader.class, Function.class,
                Object[].class));
        insn.getArguments().add(cls);
        insn.getArguments().add(var);
        insn.getArguments().add(function);
        insn.setReceiver(var);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);

        return var;
    }

    private MethodReference singleDimensionArrayUnwrapper(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    return new MethodReference(JS.class, "unwrapBooleanArray", JSArrayReader.class, boolean[].class);
                case BYTE:
                    return new MethodReference(JS.class, "unwrapByteArray", JSArrayReader.class, byte[].class);
                case SHORT:
                    return new MethodReference(JS.class, "unwrapShortArray", JSArrayReader.class, short[].class);
                case CHARACTER:
                    return new MethodReference(JS.class, "unwrapCharArray", JSArrayReader.class, char[].class);
                case INTEGER:
                    return new MethodReference(JS.class, "unwrapIntArray", JSArrayReader.class, int[].class);
                case FLOAT:
                    return new MethodReference(JS.class, "unwrapFloatArray", JSArrayReader.class, float[].class);
                case DOUBLE:
                    return new MethodReference(JS.class, "unwrapDoubleArray", JSArrayReader.class, double[].class);
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return new MethodReference(JS.class, "unwrapStringArray", JSArrayReader.class, String[].class);
        }
        return new MethodReference(JS.class, "unwrapArray", Class.class, JSArrayReader.class, JSObject[].class);
    }

    private MethodReference multipleDimensionArrayUnwrapper(ValueType itemType) {
        if (itemType instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) itemType).getKind()) {
                case BOOLEAN:
                    return new MethodReference(JS.class, "booleanArrayUnwrapper", Function.class);
                case BYTE:
                    return new MethodReference(JS.class, "byteArrayUnwrapper", Function.class);
                case SHORT:
                    return new MethodReference(JS.class, "shortArrayUnwrapper", Function.class);
                case CHARACTER:
                    return new MethodReference(JS.class, "charArrayUnwrapper", Function.class);
                case INTEGER:
                    return new MethodReference(JS.class, "intArrayUnwrapper", Function.class);
                case FLOAT:
                    return new MethodReference(JS.class, "floatArrayUnwrapper", Function.class);
                case DOUBLE:
                    return new MethodReference(JS.class, "doubleArrayUnwrapper", Function.class);
                default:
                    break;
            }
        } else if (itemType.isObject(String.class)) {
            return new MethodReference(JS.class, "stringArrayUnwrapper", Function.class);
        }
        return new MethodReference(JS.class, "arrayUnwrapper", Class.class, Function.class);
    }

    private Variable unwrap(Variable var, String methodName, ValueType argType, ValueType resultType,
            TextLocation location) {
        if (!argType.isObject(JSObject.class.getName())) {
            Variable castValue = program.createVariable();
            CastInstruction castInsn = new CastInstruction();
            castInsn.setValue(var);
            castInsn.setReceiver(castValue);
            castInsn.setLocation(location);
            castInsn.setTargetType(argType);
            replacement.add(castInsn);
            var = castValue;
        }
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), methodName, argType, resultType));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location);
        replacement.add(insn);
        return result;
    }

    private Variable wrapArgument(CallLocation location, Variable var, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return wrapFunctor(location, var, cls);
            }
        }
        return wrap(var, type, location.getSourceLocation());
    }

    private boolean isProperFunctor(ClassReader type) {
        return type.hasModifier(ElementModifier.INTERFACE) && type.getMethods().size() == 1;
    }

    private Variable wrapFunctor(CallLocation location, Variable var, ClassReader type) {
        if (!isProperFunctor(type)) {
            diagnostics.error(location, "Wrong functor: {{c0}}", type.getName());
            return var;
        }
        String name = type.getMethods().iterator().next().getName();
        Variable functor = program.createVariable();
        Variable nameVar = addStringWrap(addString(name, location.getSourceLocation()), location.getSourceLocation());
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "function", JSObject.class, JSObject.class, JSObject.class));
        insn.setReceiver(functor);
        insn.getArguments().add(var);
        insn.getArguments().add(nameVar);
        insn.setLocation(location.getSourceLocation());
        replacement.add(insn);
        return functor;
    }

    private Variable wrap(Variable var, ValueType type, TextLocation location) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (!className.equals("java.lang.String")) {
                return var;
            }
        }
        Variable result = program.createVariable();

        ValueType itemType = type;
        int degree = 0;
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
            ++degree;
        }

        if (degree <= 1) {
            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(new MethodReference(JS.class.getName(), "wrap", getWrappedType(type),
                    getWrapperType(type)));
            insn.getArguments().add(var);
            insn.setReceiver(result);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);
        } else {
            Variable function = program.createVariable();

            InvokeInstruction insn = new InvokeInstruction();
            insn.setMethod(getWrapperFunction(itemType));
            insn.setReceiver(function);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);

            while (--degree > 1) {
                insn = new InvokeInstruction();
                insn.setMethod(new MethodReference(JS.class, "arrayMapper", Function.class, Function.class));
                insn.getArguments().add(function);
                function = program.createVariable();
                insn.setReceiver(function);
                insn.setType(InvocationType.SPECIAL);
                insn.setLocation(location);
                replacement.add(insn);
            }

            insn = new InvokeInstruction();
            insn.setMethod(new MethodReference(JS.class.getName(), "map", getWrappedType(type),
                    ValueType.parse(Function.class), getWrapperType(type)));
            insn.getArguments().add(var);
            insn.getArguments().add(function);
            insn.setReceiver(result);
            insn.setType(InvocationType.SPECIAL);
            insn.setLocation(location);
            replacement.add(insn);
        }
        return result;
    }

    private MethodReference getWrapperFunction(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return new MethodReference(JS.class, "booleanArrayWrapper", Function.class);
                case BYTE:
                    return new MethodReference(JS.class, "byteArrayWrapper", Function.class);
                case SHORT:
                    return new MethodReference(JS.class, "shortArrayWrapper", Function.class);
                case CHARACTER:
                    return new MethodReference(JS.class, "charArrayWrapper", Function.class);
                case INTEGER:
                    return new MethodReference(JS.class, "intArrayWrapper", Function.class);
                case FLOAT:
                    return new MethodReference(JS.class, "floatArrayWrapper", Function.class);
                case DOUBLE:
                    return new MethodReference(JS.class, "doubleArrayWrapper", Function.class);
                default:
                    break;
            }
        } else if (type.isObject(String.class)) {
            return new MethodReference(JS.class, "stringArrayWrapper", Function.class);
        }
        return new MethodReference(JS.class, "arrayWrapper", Function.class);
    }

    private ValueType getWrappedType(ValueType type) {
        if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            if (itemType instanceof ValueType.Array) {
                return ValueType.parse(Object[].class);
            } else {
                return ValueType.arrayOf(getWrappedType(itemType));
            }
        } else if (type instanceof ValueType.Object) {
            if (type.isObject(String.class)) {
                return type;
            } else {
                return ValueType.parse(JSObject.class);
            }
        } else {
            return type;
        }
    }

    private ValueType getWrapperType(ValueType type) {
        if (type instanceof ValueType.Array) {
            return ValueType.parse(JSArray.class);
        } else {
            return ValueType.parse(JSObject.class);
        }
    }

    private MethodReader getMethod(MethodReference ref) {
        ClassReader cls = classSource.get(ref.getClassName());
        if (cls == null) {
            return null;
        }
        MethodReader method = cls.getMethod(ref.getDescriptor());
        if (method != null) {
            return method;
        }
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())
                && !cls.getParent().equals("java.lang.Object")) {
            method = getMethod(new MethodReference(cls.getParent(), ref.getDescriptor()));
            if (method != null) {
                return method;
            }
        }
        for (String iface : cls.getInterfaces()) {
            method = getMethod(new MethodReference(iface, ref.getDescriptor()));
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private boolean isProperGetter(MethodDescriptor desc) {
        if (desc.parameterCount() > 0 || !typeHelper.isSupportedType(desc.getResultType())) {
            return false;
        }
        if (desc.getResultType().equals(ValueType.BOOLEAN)) {
            if (isProperPrefix(desc.getName(), "is")) {
                return true;
            }
        }
        return isProperPrefix(desc.getName(), "get");
    }

    private boolean isProperSetter(MethodDescriptor desc) {
        if (desc.parameterCount() != 1 || !typeHelper.isSupportedType(desc.parameterType(0))
                || desc.getResultType() != ValueType.VOID) {
            return false;
        }
        return isProperPrefix(desc.getName(), "set");
    }

    private boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c);
    }

    private boolean isProperGetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 1 && typeHelper.isSupportedType(desc.parameterType(0))
                && typeHelper.isSupportedType(desc.getResultType());
    }

    private boolean isProperSetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 2 && typeHelper.isSupportedType(desc.parameterType(0))
                && typeHelper.isSupportedType(desc.parameterType(0)) && desc.getResultType() == ValueType.VOID;
    }

    private String cutPrefix(String name, int prefixLength) {
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
