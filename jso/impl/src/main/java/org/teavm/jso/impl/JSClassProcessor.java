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

import static org.teavm.jso.impl.JSMethods.JS_CLASS;
import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.teavm.backend.javascript.rendering.JSParser;
import org.teavm.cache.IncrementalDependencyRegistration;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSBuffer;
import org.teavm.jso.JSBufferType;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSPrimitiveType;
import org.teavm.jso.JSProperty;
import org.teavm.jso.JSTopLevel;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ModelUtils;
import org.teavm.model.util.ProgramUtils;

class JSClassProcessor {
    private static final String NO_SIDE_EFFECTS = NoSideEffects.class.getName();
    private final ClassReaderSource classSource;
    private final ClassHierarchy hierarchy;
    private final JSBodyRepository repository;
    private final JavaInvocationProcessor javaInvocationProcessor;
    private Program program;
    private int[] variableAliases;
    private boolean[] nativeConstructedObjects;
    private JSTypeInference types;
    private final List<Instruction> replacement = new ArrayList<>();
    private final JSTypeHelper typeHelper;
    private final Diagnostics diagnostics;
    private final Map<MethodReference, MethodReader> overriddenMethodCache = new HashMap<>();
    private final boolean strict;
    private JSValueMarshaller marshaller;
    private IncrementalDependencyRegistration incrementalCache;
    private JSImportAnnotationCache annotationCache;
    private ClassReader objectClass;
    private Predicate<String> classFilter = n -> true;
    private boolean wasmGC;

    JSClassProcessor(ClassReaderSource classSource, ClassHierarchy hierarchy, JSTypeHelper typeHelper,
            JSBodyRepository repository, Diagnostics diagnostics, IncrementalDependencyRegistration incrementalCache,
            boolean strict) {
        this.classSource = classSource;
        this.hierarchy = hierarchy;
        this.typeHelper = typeHelper;
        this.repository = repository;
        this.diagnostics = diagnostics;
        this.incrementalCache = incrementalCache;
        this.strict = strict;
        javaInvocationProcessor = new JavaInvocationProcessor(typeHelper, repository, classSource, diagnostics);

        annotationCache = new JSImportAnnotationCache(classSource, diagnostics);
    }

    void setWasmGC(boolean wasmGC) {
        this.wasmGC = wasmGC;
    }

    void setClassFilter(Predicate<String> classFilter) {
        this.classFilter = classFilter;
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
                var variableMapper = new InstructionVariableMapper(var -> program.variableAt(var.getIndex() + 1));
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
        staticSignature[0] = JS_OBJECT;
        return staticSignature;
    }

    private void setCurrentProgram(Program program) {
        this.program = program;
        marshaller = new JSValueMarshaller(diagnostics, typeHelper, classSource, hierarchy, program, replacement);
        findVariableAliases();
    }

    private void findVariableAliases() {
        if (program == null) {
            return;
        }
        variableAliases = new int[program.variableCount()];
        nativeConstructedObjects = new boolean[program.variableCount()];
        var resolved = new boolean[program.variableCount()];
        Arrays.fill(resolved, true);
        for (var i = 0; i < variableAliases.length; ++i) {
            variableAliases[i] = i;
        }
        for (var block : program.getBasicBlocks()) {
            for (var instruction : block) {
                if (instruction instanceof AssignInstruction) {
                    var assign = (AssignInstruction) instruction;
                    var from = assign.getAssignee().getIndex();
                    var to = assign.getReceiver().getIndex();
                    variableAliases[to] = from;
                    resolved[assign.getAssignee().getIndex()] = true;
                } else if (instruction instanceof ConstructInstruction) {
                    var construct = (ConstructInstruction) instruction;
                    if (typeHelper.isJavaScriptClass(construct.getType())) {
                        nativeConstructedObjects[construct.getReceiver().getIndex()] = true;
                    }
                }
            }
        }
        for (var i = 0; i < variableAliases.length; ++i) {
            getVariableAlias(i, resolved);
        }
    }

    private int getVariableAlias(int index, boolean[] resolved) {
        if (resolved[index]) {
            return variableAliases[index];
        }
        resolved[index] = true;
        variableAliases[index] = getVariableAlias(variableAliases[index], resolved);
        return variableAliases[index];
    }

    void processProgram(MethodHolder methodToProcess) {
        setCurrentProgram(methodToProcess.getProgram());
        types = new JSTypeInference(typeHelper, classSource, program, methodToProcess.getReference(), wasmGC);
        types.ensure();
        if (wasmGC) {
            wrapJsPhis(methodToProcess.getReference());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            var block = program.basicBlockAt(i);
            for (var insn : block) {
                if (insn instanceof CastInstruction) {
                    replacement.clear();
                    var callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    if (processCast((CastInstruction) insn, callLocation)) {
                        insn.insertNextAll(replacement);
                        insn.delete();
                    }
                } else if (insn instanceof IsInstanceInstruction) {
                    processIsInstance((IsInstanceInstruction) insn);
                } else if (insn instanceof InvokeInstruction) {
                    var invoke = (InvokeInstruction) insn;
                    var callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    if (processToString(invoke, callLocation)) {
                        continue;
                    }

                    var method = getMethod(invoke.getMethod().getClassName(), invoke.getMethod().getDescriptor());
                    processInvokeArgs(invoke, method);
                    if (method == null) {
                        continue;
                    }
                    replacement.clear();
                    if (processInvocation(method, callLocation, invoke, methodToProcess)) {
                        insn.insertNextAll(replacement);
                        insn.delete();
                    }
                } else if (insn instanceof GetFieldInstruction) {
                    var callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    processGetField((GetFieldInstruction) insn, callLocation);
                } else if (insn instanceof PutFieldInstruction) {
                    var callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                    processPutField((PutFieldInstruction) insn, callLocation);
                } else if (insn instanceof GetElementInstruction) {
                    processGetFromArray((GetElementInstruction) insn);
                } else if (insn instanceof PutElementInstruction) {
                    processPutIntoArray((PutElementInstruction) insn);
                } else if (insn instanceof ConstructArrayInstruction) {
                    processConstructArray((ConstructArrayInstruction) insn);
                } else if (insn instanceof ExitInstruction) {
                    var exit = (ExitInstruction) insn;
                    exit.setValueToReturn(convertValue(insn, exit.getValueToReturn(),
                            methodToProcess.getResultType()));
                } else if (insn instanceof ClassConstantInstruction) {
                    processClassConstant((ClassConstantInstruction) insn);
                } else if (insn instanceof ConstructInstruction) {
                    processConstructObject((ConstructInstruction) insn);
                } else if (insn instanceof AssignInstruction) {
                    var assign = (AssignInstruction) insn;
                    var index = variableAliases[assign.getReceiver().getIndex()];
                    if (nativeConstructedObjects[index]) {
                        assign.delete();
                    }
                } else if (insn instanceof BinaryBranchingInstruction) {
                    processReferenceEquality((BinaryBranchingInstruction) insn);
                } else if (insn instanceof BranchingInstruction) {
                    processReferenceEquality((BranchingInstruction) insn);
                }
            }
        }

        var varMapper = new InstructionVariableMapper(v -> {
            if (v.getIndex() < nativeConstructedObjects.length
                    && nativeConstructedObjects[variableAliases[v.getIndex()]]) {
                return program.variableAt(variableAliases[v.getIndex()]);
            }
            return v;
        });
        for (var block : program.getBasicBlocks()) {
            varMapper.apply(block);
        }
    }

    private void wrapJsPhis(MethodReference methodReference) {
        var changed = false;
        for (var block : program.getBasicBlocks()) {
            for (var phi : block.getPhis()) {
                if (types.typeOf(phi.getReceiver()) == JSType.MIXED) {
                    for (var incoming : phi.getIncomings()) {
                        if (types.typeOf(incoming.getValue()) == JSType.JS) {
                            changed = true;
                            var wrap = new InvokeInstruction();
                            wrap.setType(InvocationType.SPECIAL);
                            wrap.setMethod(new MethodReference(JS_WRAPPER_CLASS, "wrap", JS_OBJECT, OBJECT));
                            wrap.setArguments(incoming.getValue());
                            wrap.setReceiver(program.createVariable());
                            incoming.getSource().getLastInstruction().insertPrevious(wrap);
                            incoming.setValue(wrap.getReceiver());
                        }
                    }
                }
            }
        }
        if (changed) {
            types = new JSTypeInference(typeHelper, classSource, program, methodReference, wasmGC);
            types.ensure();
        }
    }

    private void processInvokeArgs(InvokeInstruction invoke, MethodReader methodToInvoke) {
        if (methodToInvoke != null && methodToInvoke.getAnnotations().get(JSBody.class.getName()) != null) {
            return;
        }
        var className = invoke.getMethod().getClassName();
        if (typeHelper.isJavaScriptClass(invoke.getMethod().getClassName())) {
            if (invoke.getMethod().getName().equals("<init>")
                    || getObjectClass().getMethod(invoke.getMethod().getDescriptor()) == null) {
                return;
            } else {
                className = "java.lang.Object";
            }
        }

        convertInvokeArgs(invoke, className);
    }

    private void convertInvokeArgs(InvokeInstruction invoke, String className) {
        Variable[] newArgs = null;
        for (var i = 0; i < invoke.getArguments().size(); ++i) {
            var type = invoke.getMethod().parameterType(i);
            var arg = invoke.getArguments().get(i);
            var newArg = convertValue(invoke, arg, type);
            if (newArg != arg) {
                if (newArgs == null) {
                    newArgs = invoke.getArguments().toArray(new Variable[0]);
                }
                newArgs[i] = newArg;
            }
        }
        if (newArgs != null) {
            invoke.setArguments(newArgs);
        }

        if (invoke.getInstance() != null) {
            invoke.setInstance(convertValue(invoke, invoke.getInstance(), ValueType.object(className)));
        }
    }

    private void processGetField(GetFieldInstruction insn, CallLocation callLocation) {
        if (!isJsField(insn.getField(), insn.getInstance() == null)) {
            return;
        }

        replacement.clear();
        var propertyName = getPropertyName(insn.getField());
        var result = insn.getReceiver() != null ? program.createVariable() : null;
        var instance = getCallTarget(insn.getInstance(), insn.getField(), insn.getLocation());
        addPropertyGet(propertyName, instance, result, insn.getLocation(), true);
        if (result != null) {
            result = marshaller.unwrapReturnValue(callLocation, result, insn.getFieldType(), false,
                    canBeOnlyJava(insn.getReceiver()));
            copyVar(result, insn.getReceiver(), insn.getLocation());
        }
        insn.insertNextAll(replacement);
        insn.delete();
    }

    private void processPutField(PutFieldInstruction insn, CallLocation callLocation) {
        insn.setValue(convertValue(insn, insn.getValue(), insn.getFieldType()));
        if (!isJsField(insn.getField(), insn.getInstance() == null)) {
            return;
        }

        replacement.clear();
        var propertyName = getPropertyName(insn.getField());
        var instance = getCallTarget(insn.getInstance(), insn.getField(), insn.getLocation());
        var value = insn.getValue();
        value = marshaller.wrapArgument(callLocation, value, insn.getFieldType(), types.typeOf(value), false, null);
        addPropertySet(propertyName, instance, value, insn.getLocation(), true);
        insn.insertNextAll(replacement);
        insn.delete();
    }

    private boolean isJsField(FieldReference fieldRef, boolean isStatic) {
        if (!typeHelper.isJavaScriptClass(fieldRef.getClassName())) {
            return false;
        }
        if (!isStatic) {
            return true;
        }
        var cls = classSource.get(fieldRef.getClassName());
        if (cls == null) {
            return false;
        }
        var field = cls.getField(fieldRef.getFieldName());
        return field != null && field.getAnnotations().get(JSProperty.class.getName()) != null;
    }

    private String getPropertyName(FieldReference fieldRef) {
        var cls = classSource.get(fieldRef.getClassName());
        if (cls != null) {
            var field = cls.getField(fieldRef.getFieldName());
            if (field != null) {
                var annot = field.getAnnotations().get(JSProperty.class.getName());
                if (annot != null) {
                    var value = annot.getValue("value");
                    if (value != null) {
                        return value.getString();
                    }
                }
            }
        }
        return fieldRef.getFieldName();
    }

    private void processGetFromArray(GetElementInstruction insn) {
        if (insn.getType() != ArrayElementType.OBJECT) {
            return;
        }

        var type = types.typeOf(insn.getReceiver());
        if (type == JSType.JS || type == JSType.MIXED) {
            var unwrap = new InvokeInstruction();
            unwrap.setType(InvocationType.SPECIAL);
            unwrap.setMethod(type == JSType.MIXED ? JSMethods.MAYBE_UNWRAP : JSMethods.UNWRAP);
            unwrap.setArguments(program.createVariable());
            unwrap.setReceiver(insn.getReceiver());
            unwrap.setLocation(insn.getLocation());
            insn.setReceiver(unwrap.getArguments().get(0));
            insn.insertNext(unwrap);

            if (wasmGC) {
                var invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(new MethodReference(JS_CLASS, "jsArrayItem", OBJECT, ValueType.INTEGER, OBJECT));
                invoke.setReceiver(insn.getReceiver());
                invoke.setArguments(insn.getArray(), insn.getIndex());
                invoke.setLocation(insn.getLocation());
                insn.replace(invoke);
            }
        }
    }

    private void processPutIntoArray(PutElementInstruction insn) {
        if (insn.getType() != ArrayElementType.OBJECT) {
            return;
        }

        var type = types.typeOf(insn.getValue());
        if (type == JSType.JS || type == JSType.MIXED) {
            var wrap = new InvokeInstruction();
            wrap.setType(InvocationType.SPECIAL);
            wrap.setMethod(type == JSType.MIXED ? JSMethods.MAYBE_WRAP : JSMethods.WRAP);
            wrap.setArguments(insn.getValue());
            wrap.setReceiver(program.createVariable());
            wrap.setLocation(insn.getLocation());
            insn.setValue(wrap.getReceiver());
            insn.insertPrevious(wrap);
        }
    }

    private void processConstructArray(ConstructArrayInstruction insn) {
        var arrayType = processType(ValueType.arrayOf(insn.getItemType()));
        insn.setItemType(((ValueType.Array) arrayType).getItemType());
    }

    private void processClassConstant(ClassConstantInstruction insn) {
        insn.setConstant(processType(insn.getConstant()));
    }

    private void processConstructObject(ConstructInstruction insn) {
        if (nativeConstructedObjects[insn.getReceiver().getIndex()]) {
            insn.delete();
        }
    }

    private void processReferenceEquality(BinaryBranchingInstruction instruction) {
        if (!wasmGC) {
            return;
        }

        boolean equal;
        switch (instruction.getCondition()) {
            case REFERENCE_EQUAL:
                equal = true;
                break;
            case REFERENCE_NOT_EQUAL:
                equal = false;
                break;
            default:
                return;
        }

        var first = types.typeOf(instruction.getFirstOperand());
        var second = types.typeOf(instruction.getSecondOperand());
        if (first == JSType.JS || second == JSType.JS) {
            var call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setLocation(instruction.getLocation());
            var conditionVar = program.createVariable();
            if (first == JSType.NULL || second == JSType.NULL) {
                call.setMethod(new MethodReference(JS_CLASS, "isNull", JS_OBJECT, ValueType.BOOLEAN));
                call.setArguments(first == JSType.NULL
                        ? instruction.getSecondOperand()
                        : instruction.getFirstOperand());
                call.setReceiver(conditionVar);
                instruction.insertPrevious(call);
            } else {
                var firstOperand = instruction.getFirstOperand();
                var secondOperand = instruction.getSecondOperand();
                if (first != JSType.JS) {
                    firstOperand = convertToJs(firstOperand, instruction);
                }
                if (second != JSType.JS) {
                    secondOperand = convertToJs(secondOperand, instruction);
                }
                call.setMethod(new MethodReference(JS_CLASS, "sameRef", JS_OBJECT, JS_OBJECT, ValueType.BOOLEAN));
                call.setArguments(firstOperand, secondOperand);
                call.setReceiver(conditionVar);
                instruction.insertPrevious(call);
            }

            var newCondition = new BranchingInstruction(equal
                    ? BranchingCondition.NOT_EQUAL
                    : BranchingCondition.EQUAL);
            newCondition.setOperand(call.getReceiver());
            newCondition.setConsequent(instruction.getConsequent());
            newCondition.setAlternative(instruction.getAlternative());
            newCondition.setLocation(instruction.getLocation());
            instruction.replace(newCondition);
        }
    }

    private void processReferenceEquality(BranchingInstruction instruction) {
        if (!wasmGC) {
            return;
        }

        boolean equal;
        switch (instruction.getCondition()) {
            case NULL:
                equal = true;
                break;
            case NOT_NULL:
                equal = false;
                break;
            default:
                return;
        }

        var type = types.typeOf(instruction.getOperand());
        if (type == JSType.JS) {
            var call = new InvokeInstruction();
            call.setType(InvocationType.SPECIAL);
            call.setLocation(instruction.getLocation());
            call.setMethod(new MethodReference(JS_CLASS, "isNull", JS_OBJECT, ValueType.BOOLEAN));
            call.setArguments(instruction.getOperand());
            call.setReceiver(program.createVariable());
            instruction.insertPrevious(call);
            instruction.setOperand(call.getReceiver());
            instruction.setCondition(equal ? BranchingCondition.NOT_EQUAL : BranchingCondition.EQUAL);
        }
    }

    private Variable convertToJs(Variable value, Instruction instruction) {
        var call = new InvokeInstruction();
        call.setType(InvocationType.SPECIAL);
        call.setMethod(new MethodReference(JS_CLASS, "directJavaToJs", OBJECT, JS_OBJECT));
        call.setArguments(value);
        call.setReceiver(program.createVariable());
        call.setLocation(instruction.getLocation());
        instruction.insertPrevious(call);
        return call.getReceiver();
    }

    private ValueType processType(ValueType type) {
        return processType(typeHelper, type);
    }

    static ValueType processType(JSTypeHelper typeHelper, ValueType type) {
        var originalType = type;
        var degree = 0;
        while (type instanceof ValueType.Array) {
            degree++;
            type = ((ValueType.Array) type).getItemType();
        }
        if (!(type instanceof ValueType.Object)) {
            return originalType;
        }

        var className = ((ValueType.Object) type).getClassName();
        if (!typeHelper.isJavaScriptClass(className)) {
            return originalType;
        }

        type = degree > 0 ? OBJECT : ValueType.object(JS_WRAPPER_CLASS);
        while (degree-- > 0) {
            type = ValueType.arrayOf(type);
        }
        return type;
    }

    private boolean processCast(CastInstruction cast, CallLocation location) {
        if (cast.isWeak()) {
            return false;
        }
        if (!(cast.getTargetType() instanceof ValueType.Object)) {
            cast.setTargetType(processType(cast.getTargetType()));
            return false;
        }

        String targetClassName = ((ValueType.Object) cast.getTargetType()).getClassName();
        if (!typeHelper.isJavaScriptClass(targetClassName)) {
            return false;
        }

        cast.setValue(unwrapJavaToJs(cast, cast.getValue()));

        ClassReader targetClass = classSource.get(targetClassName);
        if (targetClass.getAnnotations().get(JSFunctor.class.getName()) == null) {
            if (!strict || isTransparent(targetClassName)) {
                var assign = new AssignInstruction();
                assign.setLocation(location.getSourceLocation());
                assign.setAssignee(cast.getValue());
                assign.setReceiver(cast.getReceiver());
                replacement.add(assign);
            } else {
                var instanceOfResult = program.createVariable();
                processIsInstanceUnwrapped(cast.getLocation(), cast.getValue(), targetClassName, instanceOfResult);

                var invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(JSMethods.THROW_CCE_IF_FALSE);
                invoke.setArguments(instanceOfResult, cast.getValue());
                invoke.setReceiver(cast.getReceiver());
                replacement.add(invoke);
            }
            return true;
        }

        Variable result = marshaller.unwrapFunctor(location, cast.getValue(), targetClass);
        var assign = new AssignInstruction();
        assign.setLocation(location.getSourceLocation());
        assign.setAssignee(result);
        assign.setReceiver(cast.getReceiver());
        replacement.add(assign);

        return true;
    }

    private void processIsInstance(IsInstanceInstruction isInstance) {
        if (!(isInstance.getType() instanceof ValueType.Object)) {
            isInstance.setType(processType(isInstance.getType()));
            return;
        }

        String targetClassName = ((ValueType.Object) isInstance.getType()).getClassName();
        if (!typeHelper.isJavaScriptClass(targetClassName)) {
            return;
        }

        replacement.clear();
        processIsInstance(isInstance.getLocation(), types.typeOf(isInstance.getValue()), isInstance.getValue(),
                targetClassName, isInstance.getReceiver());
        isInstance.insertPreviousAll(replacement);
        isInstance.delete();
        replacement.clear();
    }

    private void processIsInstance(TextLocation location, JSType type, Variable value, String targetClassName,
            Variable receiver) {
        if (type == JSType.JS) {
            if (isTransparent(targetClassName)) {
                var cst = new IntegerConstantInstruction();
                cst.setConstant(1);
                cst.setReceiver(receiver);
                cst.setLocation(location);
                replacement.add(cst);
            } else {
                var primitiveType = getPrimitiveType(targetClassName);
                var invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(primitiveType != null ? JSMethods.IS_PRIMITIVE : JSMethods.INSTANCE_OF);
                var secondArg = primitiveType != null
                        ? marshaller.addJsString(primitiveType, location)
                        : marshaller.classRef(targetClassName, location);
                invoke.setArguments(value, secondArg);
                invoke.setReceiver(receiver);
                invoke.setLocation(location);
                replacement.add(invoke);
            }
        } else {
            if (isTransparent(targetClassName)) {
                var invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(JSMethods.IS_JS);
                invoke.setArguments(value);
                invoke.setReceiver(receiver);
                invoke.setLocation(location);
                replacement.add(invoke);
            } else {
                var primitiveType = getPrimitiveType(targetClassName);
                var invoke = new InvokeInstruction();
                invoke.setType(InvocationType.SPECIAL);
                invoke.setMethod(primitiveType != null
                        ? JSMethods.WRAPPER_IS_PRIMITIVE
                        : JSMethods.WRAPPER_INSTANCE_OF);
                var secondArg = primitiveType != null
                        ? marshaller.addJsString(primitiveType, location)
                        : marshaller.classRef(targetClassName, location);
                invoke.setArguments(value, secondArg);
                invoke.setReceiver(receiver);
                invoke.setLocation(location);
                replacement.add(invoke);
            }
        }
    }

    private void processIsInstanceUnwrapped(TextLocation location, Variable value, String targetClassName,
            Variable receiver) {
        var primitiveType = getPrimitiveType(targetClassName);
        var invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(primitiveType != null ? JSMethods.IS_PRIMITIVE : JSMethods.INSTANCE_OF_OR_NULL);
        var secondArg = primitiveType != null
                ? marshaller.addJsString(primitiveType, location)
                : marshaller.classRef(targetClassName, location);
        invoke.setArguments(value, secondArg);
        invoke.setReceiver(receiver);
        invoke.setLocation(location);
        replacement.add(invoke);
    }

    private boolean isTransparent(String className) {
        var cls = classSource.get(className);
        if (cls == null) {
            return true;
        }
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            return true;
        }
        var clsAnnot = cls.getAnnotations().get(JSClass.class.getName());
        if (clsAnnot != null) {
            var transparent = clsAnnot.getValue("transparent");
            if (transparent != null) {
                return transparent.getBoolean();
            }
        }
        return false;
    }

    private String getPrimitiveType(String className) {
        var cls = classSource.get(className);
        if (cls == null) {
            return null;
        }
        var clsAnnot = cls.getAnnotations().get(JSPrimitiveType.class.getName());
        if (clsAnnot != null) {
            var value = clsAnnot.getValue("value");
            if (value != null) {
                return value.getString();
            }
        }
        return null;
    }

    private Variable convertValue(Instruction instruction, Variable var, ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return var;
        }

        var cls = ((ValueType.Object) type).getClassName();
        if (typeHelper.isJavaScriptClass(cls)) {
            return convertJavaValueToJs(instruction, var);
        } else {
            return convertJsValueToJava(instruction, var);
        }
    }

    private Variable convertJsValueToJava(Instruction instruction, Variable var) {
        var varType = types.typeOf(var);
        if (varType != JSType.JS && varType != JSType.MIXED) {
            return var;
        }
        var wrap = new InvokeInstruction();
        wrap.setType(InvocationType.SPECIAL);
        wrap.setMethod(varType == JSType.JS ? JSMethods.WRAP : JSMethods.MAYBE_WRAP);
        wrap.setArguments(var);
        wrap.setReceiver(program.createVariable());
        wrap.setLocation(instruction.getLocation());
        instruction.insertPrevious(wrap);
        return wrap.getReceiver();
    }

    private Variable convertJavaValueToJs(Instruction instruction, Variable var) {
        var varType = types.typeOf(var);
        if (varType == JSType.JS || varType == JSType.MIXED || varType == JSType.NULL) {
            return var;
        }
        var wrap = new InvokeInstruction();
        wrap.setType(InvocationType.SPECIAL);
        wrap.setMethod(JSMethods.UNWRAP);
        wrap.setArguments(var);
        wrap.setReceiver(program.createVariable());
        wrap.setLocation(instruction.getLocation());
        instruction.insertPrevious(wrap);
        return wrap.getReceiver();
    }

    private Variable unwrapJavaToJs(Instruction instruction, Variable var) {
        var varType = types.typeOf(var);
        if (varType != JSType.JAVA && varType != JSType.MIXED) {
            return var;
        }
        var unwrap = new InvokeInstruction();
        unwrap.setType(InvocationType.SPECIAL);
        unwrap.setMethod(varType == JSType.JAVA ? JSMethods.UNWRAP : JSMethods.MAYBE_UNWRAP);
        unwrap.setArguments(var);
        unwrap.setReceiver(program.createVariable());
        unwrap.setLocation(instruction.getLocation());
        instruction.insertPrevious(unwrap);
        return unwrap.getReceiver();
    }

    private boolean processToString(InvokeInstruction invoke, CallLocation location) {
        if (!invoke.getMethod().getName().equals("toString") || !invoke.getArguments().isEmpty()
                || invoke.getInstance() == null || !invoke.getMethod().getReturnType().isObject(String.class)
                || types.typeOf(invoke.getInstance()) != JSType.JS) {
            return false;
        }

        replacement.clear();
        var methodName = marshaller.addStringWrap(marshaller.addString("toString", invoke.getLocation()),
                invoke.getLocation());

        var jsInvoke = new InvokeInstruction();
        jsInvoke.setType(InvocationType.SPECIAL);
        jsInvoke.setMethod(JSMethods.invoke(0));
        jsInvoke.setReceiver(program.createVariable());
        jsInvoke.setLocation(invoke.getLocation());
        jsInvoke.setArguments(invoke.getInstance(), methodName);
        replacement.add(jsInvoke);

        var assign = new AssignInstruction();
        assign.setAssignee(marshaller.unwrapReturnValue(location, jsInvoke.getReceiver(),
                invoke.getMethod().getReturnType(), false, canBeOnlyJava(invoke.getReceiver())));
        assign.setReceiver(invoke.getReceiver());
        assign.setLocation(invoke.getLocation());
        replacement.add(assign);

        invoke.insertNextAll(replacement);
        replacement.clear();
        invoke.delete();

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

        if (method.getName().equals("<init>")) {
            return processConstructor(method, callLocation, invoke);
        }

        var isStatic = method.hasModifier(ElementModifier.STATIC);
        if (method.getProgram() != null && method.getProgram().basicBlockCount() > 0) {
            if (isStatic) {
                convertInvokeArgs(invoke, method.getOwnerName());
                return false;
            }
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
                    newArguments[i + 1] = convertValue(invoke, invoke.getArguments().get(i),
                            invoke.getMethod().parameterType(i + 1));
                }
                invoke.setArguments(newArguments);
                invoke.setInstance(null);
            }
            invoke.setType(InvocationType.SPECIAL);
            return false;
        }

        var annot = annotationCache.get(method.getReference(), callLocation);
        if (annot != null) {
            switch (annot.kind) {
                case PROPERTY:
                    return processProperty(method, annot.name, callLocation, invoke);
                case INDEXER:
                    return processIndexer(method, callLocation, invoke);
                case METHOD:
                    return processMethod(method, annot.name, callLocation, invoke);
            }
        }
        return processMethod(method, null, callLocation, invoke);
    }

    private boolean processJSBodyInvocation(MethodReader method, CallLocation callLocation, InvokeInstruction invoke,
            MethodHolder methodToProcess) {
        if (!classFilter.test(method.getOwnerName())) {
            return false;
        }
        boolean[] byRefParams = new boolean[method.parameterCount()];
        validateSignature(method, callLocation, byRefParams);
        if (invoke.getInstance() != null) {
            if (!typeHelper.isJavaScriptClass(method.getOwnerName())) {
                diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                        + "declaration. It is non-static and declared on a non-overlay class {{c1}}",
                        invoke.getMethod(), method.getOwnerName());
            }
        }

        var returnByRef = false;
        var byRef = method.getAnnotations().get(JSByRef.class.getName());
        if (byRef != null) {
            if (!typeHelper.isSupportedByRefType(method.getResultType())) {
                diagnostics.error(callLocation, "Method {{m0}} is marked with @JSByRef, but does not return valid "
                        + "array type", method.getReference());
                return false;
            }
            if (wasmGC) {
                var optionalValue = byRef.getValue("optional");
                if (optionalValue == null || !optionalValue.getBoolean()) {
                    diagnostics.error(callLocation, "Method {{m0}} is marked with "
                            + "@JSByRef, which is not supported in Wasm GC", method.getReference());
                    return false;
                }
            } else {
                returnByRef = true;
            }
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
            var arg = invoke.getInstance();
            arg = marshaller.wrapArgument(callLocation, arg,
                    ValueType.object(method.getOwnerName()), types.typeOf(arg), false, null);
            newArgs.add(arg);
        }
        for (int i = 0; i < invoke.getArguments().size(); ++i) {
            var arg = invoke.getArguments().get(i);
            var bufferType = extractBufferType(method.parameterAnnotation(i), method.parameterType(i),
                    "parameter" + (i + 1), method.getReference(), callLocation);
            arg = marshaller.wrapArgument(callLocation, invoke.getArguments().get(i),
                    method.parameterType(i), types.typeOf(arg), byRefParams[i], bufferType);
            newArgs.add(arg);
        }
        newInvoke.setArguments(newArgs.toArray(new Variable[0]));
        replacement.add(newInvoke);
        if (result != null) {
            result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), returnByRef,
                    canBeOnlyJava(invoke.getReceiver()));
            copyVar(result, invoke.getReceiver(), invoke.getLocation());
        }

        incrementalCache.addDependencies(methodToProcess.getReference(), method.getOwnerName());

        return true;
    }

    private JSBufferType extractBufferType(AnnotationContainerReader annotations, ValueType type, String subject,
            MethodReference method, CallLocation callLocation) {
        var annot = annotations.get(JSBuffer.class.getName());
        if (annot == null) {
            return null;
        }
        if (!isBufferType(type)) {
            diagnostics.error(callLocation, subject + " of {{m0}} is marked with @JSBuffer, "
                    + "but is not valid java.nio.Buffer", method);
            return null;
        }
        return JSBufferType.valueOf(annot.getValue("value").getEnumValue().getFieldName());
    }

    private boolean isBufferType(ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var className = ((ValueType.Object) type).getClassName();
        return hierarchy.isSuperType("java.nio.Buffer", className, false);
    }

    private boolean processProperty(MethodReader method, String suggestedName, CallLocation callLocation,
            InvokeInstruction invoke) {
        boolean pure = method.getAnnotations().get(NO_SIDE_EFFECTS) != null;
        if (isProperGetter(method, suggestedName)) {
            var propertyName = suggestedName;
            if (propertyName == null) {
                propertyName = method.getName().charAt(0) == 'i' ? cutPrefix(method.getName(), 2)
                        : cutPrefix(method.getName(), 3);
            }
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            addPropertyGet(propertyName, getCallTarget(invoke), result, invoke.getLocation(), pure);
            if (result != null) {
                result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false,
                        canBeOnlyJava(invoke.getReceiver()));
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetter(method, suggestedName)) {
            var propertyName = suggestedName;
            if (propertyName == null) {
                propertyName = cutPrefix(method.getName(), 3);
            }
            var value = invoke.getArguments().get(0);
            value = marshaller.wrapArgument(callLocation, value, method.parameterType(0), types.typeOf(value), false,
                    null);
            addPropertySet(propertyName, getCallTarget(invoke), value, invoke.getLocation(), pure);
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript property "
                + "declaration", invoke.getMethod());
        return false;
    }

    private boolean processIndexer(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        if (isProperGetIndexer(method.getDescriptor())) {
            Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
            var index = invoke.getArguments().get(0);
            addIndexerGet(getCallTarget(invoke), marshaller.wrapArgument(callLocation, index,
                    method.parameterType(0), types.typeOf(index), false, null), result, invoke.getLocation());
            if (result != null) {
                result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false,
                        canBeOnlyJava(invoke.getReceiver()));
                copyVar(result, invoke.getReceiver(), invoke.getLocation());
            }
            return true;
        }
        if (isProperSetIndexer(method.getDescriptor())) {
            var index = invoke.getArguments().get(0);
            index = marshaller.wrapArgument(callLocation, index, method.parameterType(0), types.typeOf(index), false,
                    null);
            var value = invoke.getArguments().get(1);
            value = marshaller.wrapArgument(callLocation, value, method.parameterType(1),
                    types.typeOf(value), false, null);
            addIndexerSet(getCallTarget(invoke), index, value, invoke.getLocation());
            return true;
        }
        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript indexer "
                + "declaration", invoke.getMethod());
        return false;
    }

    private boolean validateSignature(MethodReader method, CallLocation callLocation, boolean[] byRefParams) {
        ValueType[] parameterTypes = method.getParameterTypes();
        AnnotationContainerReader[] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterTypes.length; i++) {
            ValueType paramType = parameterTypes[i];
            var byRef = parameterAnnotations[i].get(JSByRef.class.getName());
            if (byRef != null) {
                if (!typeHelper.isSupportedByRefType(paramType)) {
                    diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method "
                            + "declaration: its " + (i + 1) + "th parameter is declared as JSByRef, "
                            + "which has incompatible type", method.getReference());
                    return false;
                } else if (wasmGC) {
                    var optionalValue = byRef.getValue("optional");
                    if (optionalValue == null || !optionalValue.getBoolean()) {
                        diagnostics.error(callLocation, "Parameter " + (i + 1) + " of method {{m0}} is marked with "
                                + "@JSByRef, which is not supported in Wasm GC", method.getReference());
                        return false;
                    }
                } else {
                    byRefParams[i] = true;
                }
            }
        }

        return true;
    }

    private boolean canBeOnlyJava(Variable variable) {
        var type = types.typeOf(variable);
        return type != JSType.JS && type != JSType.MIXED;
    }

    private boolean processMethod(MethodReader method, String name, CallLocation callLocation,
            InvokeInstruction invoke) {
        if (name == null) {
            name = method.getName();
        }
        boolean[] byRefParams = new boolean[method.parameterCount() + 1];
        if (!validateSignature(method, callLocation, byRefParams)) {
            return false;
        }

        var vararg = method.hasModifier(ElementModifier.VARARGS);
        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
        InvokeInstruction newInvoke = new InvokeInstruction();
        newInvoke.setMethod(vararg ? JSMethods.APPLY : JSMethods.invoke(method.parameterCount()));
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);

        List<Variable> newArguments = new ArrayList<>();
        newArguments.add(getCallTarget(invoke));
        newArguments.add(marshaller.addStringWrap(marshaller.addString(name, invoke.getLocation()),
                invoke.getLocation()));
        newInvoke.setLocation(invoke.getLocation());

        var callArguments = new ArrayList<Variable>();
        for (int i = 0; i < invoke.getArguments().size(); ++i) {
            var arg = invoke.getArguments().get(i);
            var byRef = byRefParams[i];
            if (vararg && i == invoke.getArguments().size() - 1
                    && typeHelper.isSupportedByRefType(method.parameterType(i))
                    && !wasmGC) {
                byRef = true;
            }
            var bufferType = extractBufferType(method.parameterAnnotation(i), method.parameterType(i),
                    "Parameter " + (i + 1), method.getReference(), callLocation);
            arg = marshaller.wrapArgument(callLocation, arg, method.parameterType(i), types.typeOf(arg), byRef,
                    bufferType);
            callArguments.add(arg);
        }

        if (vararg) {
            Variable prefixArg = null;
            if (callArguments.size() > 1) {
                var arrayOfInvocation = new InvokeInstruction();
                arrayOfInvocation.setType(InvocationType.SPECIAL);
                arrayOfInvocation.setArguments(callArguments.subList(0, callArguments.size() - 1)
                        .toArray(new Variable[0]));
                arrayOfInvocation.setMethod(JSMethods.arrayOf(callArguments.size() - 1));
                arrayOfInvocation.setReceiver(program.createVariable());
                arrayOfInvocation.setLocation(invoke.getLocation());
                replacement.add(arrayOfInvocation);
                prefixArg = arrayOfInvocation.getReceiver();
            }

            var arrayArg = callArguments.get(callArguments.size() - 1);

            if (prefixArg != null) {
                var concat = new InvokeInstruction();
                concat.setType(InvocationType.SPECIAL);
                concat.setArguments(prefixArg, arrayArg);
                concat.setMethod(JSMethods.CONCAT_ARRAY);
                concat.setReceiver(program.createVariable());
                concat.setLocation(invoke.getLocation());
                replacement.add(concat);
                arrayArg = concat.getReceiver();
            }
            newArguments.add(arrayArg);
        } else {
            newArguments.addAll(callArguments);
        }
        newInvoke.setArguments(newArguments.toArray(new Variable[0]));

        replacement.add(newInvoke);
        if (result != null) {
            result = marshaller.unwrapReturnValue(callLocation, result, method.getResultType(), false,
                    canBeOnlyJava(invoke.getReceiver()));
            copyVar(result, invoke.getReceiver(), invoke.getLocation());
        }

        return true;
    }

    private ClassReader getObjectClass() {
        if (objectClass == null) {
            objectClass = classSource.get("java.lang.Object");
        }
        return objectClass;
    }

    private Variable getCallTarget(InvokeInstruction invoke) {
        if (invoke.getInstance() != null) {
            return invoke.getInstance();
        }
        var cls = classSource.get(invoke.getMethod().getClassName());
        var method = cls != null ? cls.getMethod(invoke.getMethod().getDescriptor()) : null;
        var isTopLevel = (cls != null && cls.getAnnotations().get(JSTopLevel.class.getName()) != null)
                || (method != null && method.getAnnotations().get(JSTopLevel.class.getName()) != null);
        if (isTopLevel) {
            var methodAnnotations = method != null ? method.getAnnotations() : null;
            return marshaller.moduleRef(invoke.getMethod().getClassName(), methodAnnotations, invoke.getLocation());
        } else {
            return marshaller.classRef(invoke.getMethod().getClassName(), invoke.getLocation());
        }
    }

    private Variable getCallTarget(Variable instance, FieldReference fieldRef, TextLocation location) {
        if (instance != null) {
            return instance;
        }
        var cls = classSource.get(fieldRef.getClassName());
        var field = cls != null ? cls.getField(fieldRef.getFieldName()) : null;
        var isTopLevel = (cls != null && cls.getAnnotations().get(JSTopLevel.class.getName()) != null)
                || (field != null && field.getAnnotations().get(JSTopLevel.class.getName()) != null);
        if (isTopLevel) {
            var fieldAnnotations = field != null ? field.getAnnotations() : null;
            return marshaller.moduleRef(fieldRef.getClassName(), fieldAnnotations, location);
        } else {
            return marshaller.classRef(fieldRef.getClassName(), location);
        }
    }

    private boolean processConstructor(MethodReader method, CallLocation callLocation, InvokeInstruction invoke) {
        var byRefParams = new boolean[method.parameterCount() + 1];
        if (!validateSignature(method, callLocation, byRefParams)) {
            return false;
        }

        var result = program.variableAt(variableAliases[invoke.getInstance().getIndex()]);
        var newInvoke = new InvokeInstruction();
        newInvoke.setMethod(JSMethods.construct(method.parameterCount()));
        newInvoke.setType(InvocationType.SPECIAL);
        newInvoke.setReceiver(result);
        var newArguments = new ArrayList<Variable>();
        newArguments.add(marshaller.classRef(invoke.getMethod().getClassName(), invoke.getLocation()));
        newInvoke.setLocation(invoke.getLocation());
        for (int i = 0; i < invoke.getArguments().size(); ++i) {
            var arg = invoke.getArguments().get(i);
            var bufferType = extractBufferType(method.parameterAnnotation(i), method.parameterType(i),
                    "Parameter " + (i + 1), method.getReference(), callLocation);
            arg = marshaller.wrapArgument(callLocation, arg, method.parameterType(i), types.typeOf(arg),
                    byRefParams[i], bufferType);
            newArguments.add(arg);
        }
        newInvoke.setArguments(newArguments.toArray(new Variable[0]));
        replacement.add(newInvoke);

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
            if (!typeHelper.isJavaScriptClass(methodToProcess.getOwnerName())) {
                diagnostics.error(location, "Non-static JSBody method {{m0}} is owned by non-JS class {{c1}}",
                        methodToProcess.getReference(), methodToProcess.getOwnerName());
            }
        }

        // generate parameter types for proxy method
        ValueType[] proxyParamTypes = new ValueType[paramCount + 1];
        for (int i = 0; i < paramCount; ++i) {
            proxyParamTypes[i] = JS_OBJECT;
        }
        proxyParamTypes[paramCount] = methodToProcess.getResultType() == ValueType.VOID
                ? ValueType.VOID
                : JS_OBJECT;

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
        var errorReporter = new TeaVMErrorReporter(diagnostics, new CallLocation(methodToProcess.getReference()));
        var env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        env.setIdeMode(true);
        var parser = new JSParser(env, errorReporter);
        AstRoot rootNode;
        try {
            rootNode = (AstRoot) parser.parseAsObject(new StringReader("function(){" + script + "}"), null, 0);
        } catch (IOException e) {
            throw new RuntimeException("IO Error occurred", e);
        }
        var body = ((FunctionNode) rootNode.getFirstChild()).getBody();

        JsBodyImportInfo[] imports;
        var importsValue = bodyAnnot.getValue("imports");
        if (importsValue != null) {
            var importsList = importsValue.getList();
            imports = new JsBodyImportInfo[importsList.size()];
            for (var i = 0; i < importsList.size(); ++i) {
                var importAnnot = importsList.get(0).getAnnotation();
                imports[i] = new JsBodyImportInfo(importAnnot.getValue("alias").getString(),
                        importAnnot.getValue("fromModule").getString());
            }
        } else {
            imports = new JsBodyImportInfo[0];
        }

        repository.methodMap.put(methodToProcess.getReference(), proxyMethod);
        if (errorReporter.hasErrors()) {
            repository.emitters.put(proxyMethod, new JSBodyBloatedEmitter(isStatic, proxyMethod,
                    script, parameterNames, imports));
        } else {
            var expr = JSBodyInlineUtil.isSuitableForInlining(methodToProcess.getReference(),
                    parameterNames, body);
            if (expr != null) {
                repository.inlineMethods.add(methodToProcess.getReference());
            } else {
                expr = body;
            }
            javaInvocationProcessor.process(location, expr);
            var emitter = new JSBodyAstEmitter(isStatic, methodToProcess.getReference(), expr, rootNode,
                    parameterNames, imports);
            repository.emitters.put(proxyMethod, emitter);
        }
        if (imports.length > 0) {
            repository.imports.put(proxyMethod, imports);
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
            proxyMethod.getAnnotations().add(new AnnotationHolder(JSBodyDelegate.class.getName()));
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
                    ValueType.object(calleeRef.getClassName()), false, true));
        }
        Variable[] args = new Variable[callee.parameterCount()];
        for (int i = 0; i < callee.parameterCount(); ++i) {
            args[i] = marshaller.unwrapReturnValue(location, program.variableAt(paramIndex++),
                    callee.parameterType(i), false, true);
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
            exit.setValueToReturn(marshaller.wrap(insn.getReceiver(), callee.getResultType(), JSType.MIXED,
                    null, false, null));
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

    private boolean isProperGetter(MethodReader method, String suggestedName) {
        if (method.parameterCount() > 0) {
            return false;
        }
        if (suggestedName != null) {
            return true;
        }

        if (method.getResultType().equals(ValueType.BOOLEAN)) {
            if (isProperPrefix(method.getName(), "is")) {
                return true;
            }
        }
        return isProperPrefix(method.getName(), "get");
    }

    private boolean isProperSetter(MethodReader method, String suggestedName) {
        if (method.parameterCount() != 1 || method.getResultType() != ValueType.VOID) {
            return false;
        }

        return suggestedName != null || isProperPrefix(method.getName(), "set");
    }

    private boolean isProperPrefix(String name, String prefix) {
        if (!name.startsWith(prefix) || name.length() == prefix.length()) {
            return false;
        }
        char c = name.charAt(prefix.length());
        return Character.isUpperCase(c) || !Character.isAlphabetic(c) && Character.isJavaIdentifierStart(c);
    }

    private boolean isProperGetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 1;
    }

    private boolean isProperSetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 2 && desc.getResultType() == ValueType.VOID;
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
