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
package org.teavm.jso.plugin;

import java.util.*;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.PreserveOriginalName;
import org.teavm.jso.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
class JavascriptNativeProcessor {
    private ClassReaderSource classSource;
    private Program program;
    private List<Instruction> replacement = new ArrayList<>();
    private NativeJavascriptClassRepository nativeRepos;
    private Diagnostics diagnostics;
    private int methodIndexGenerator;

    public JavascriptNativeProcessor(ClassReaderSource classSource) {
        this.classSource = classSource;
        nativeRepos = new NativeJavascriptClassRepository(classSource);
    }

    public void setDiagnostics(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void processClass(ClassHolder cls) {
        Set<MethodDescriptor> preservedMethods = new HashSet<>();
        for (String iface : cls.getInterfaces()) {
            if (nativeRepos.isJavaScriptClass(iface)) {
                addPreservedMethods(iface, preservedMethods);
            }
        }
        for (MethodHolder method : cls.getMethods()) {
            if (preservedMethods.contains(method.getDescriptor()) &&
                    method.getAnnotations().get(PreserveOriginalName.class.getName()) == null) {
                method.getAnnotations().add(new AnnotationHolder(PreserveOriginalName.class.getName()));
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

    public void processProgram(MethodHolder methodToProcess) {
        program = methodToProcess.getProgram();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            List<Instruction> instructions = block.getInstructions();
            for (int j = 0; j < instructions.size(); ++j) {
                Instruction insn = instructions.get(j);
                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction)insn;
                if (!nativeRepos.isJavaScriptClass(invoke.getMethod().getClassName())) {
                    continue;
                }
                replacement.clear();
                MethodReader method = getMethod(invoke.getMethod());
                CallLocation callLocation = new CallLocation(methodToProcess.getReference(), insn.getLocation());
                if (method.getAnnotations().get(JSProperty.class.getName()) != null) {
                    if (isProperGetter(method.getDescriptor())) {
                        String propertyName;
                        AnnotationReader annot = method.getAnnotations().get(JSProperty.class.getName());
                        if (annot.getValue("value") != null) {
                            propertyName = annot.getValue("value").getString();
                        } else {
                            propertyName = method.getName().charAt(0) == 'i' ? cutPrefix(method.getName(), 2) :
                                cutPrefix(method.getName(), 3);
                        }
                        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                        addPropertyGet(propertyName, invoke.getInstance(), result, invoke.getLocation());
                        if (result != null) {
                            result = unwrap(callLocation, result, method.getResultType());
                            copyVar(result, invoke.getReceiver(), invoke.getLocation());
                        }
                    } else if (isProperSetter(method.getDescriptor())) {
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
                    } else {
                        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript property " +
                                "declaration", invoke.getMethod());
                        continue;
                    }
                } else if (method.getAnnotations().get(JSIndexer.class.getName()) != null) {
                    if (isProperGetIndexer(method.getDescriptor())) {
                        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                        addIndexerGet(invoke.getInstance(), wrap(invoke.getArguments().get(0),
                                method.parameterType(0), invoke.getLocation()), result, invoke.getLocation());
                        if (result != null) {
                            result = unwrap(callLocation, result, method.getResultType());
                            copyVar(result, invoke.getReceiver(), invoke.getLocation());
                        }
                    } else if (isProperSetIndexer(method.getDescriptor())) {
                        Variable index = wrap(invoke.getArguments().get(0), method.parameterType(0),
                                invoke.getLocation());
                        Variable value = wrap(invoke.getArguments().get(1), method.parameterType(1),
                                invoke.getLocation());
                        addIndexerSet(invoke.getInstance(), index, value, invoke.getLocation());
                    } else {
                        diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript indexer " +
                                "declaration", invoke.getMethod());
                        continue;
                    }
                } else {
                    String name = method.getName();
                    AnnotationReader constructorAnnot = method.getAnnotations().get(JSConstructor.class.getName());
                    boolean isConstructor = false;
                    if (constructorAnnot != null) {
                        if (!isSupportedType(method.getResultType())) {
                            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript " +
                                    "constructor declaration", invoke.getMethod());
                            continue;
                        }
                        AnnotationValue nameVal = constructorAnnot.getValue("value");
                        name = nameVal != null ? constructorAnnot.getValue("value").getString() : "";
                        if (name.isEmpty()) {
                            if (!method.getName().startsWith("new") || method.getName().length() == 3) {
                                diagnostics.error(callLocation, "Method {{m0}} is not declared as a native " +
                                        "JavaScript constructor, but its name does not satisfy conventions",
                                        invoke.getMethod());
                                continue;
                            }
                            name = method.getName().substring(3);
                        }
                        isConstructor = true;
                    } else {
                        AnnotationReader methodAnnot = method.getAnnotations().get(JSMethod.class.getName());
                        if (methodAnnot != null) {
                            AnnotationValue redefinedMethodName = methodAnnot.getValue("value");
                            if (redefinedMethodName != null) {
                                name = redefinedMethodName.getString();
                            }
                        }
                        if (method.getResultType() != ValueType.VOID && !isSupportedType(method.getResultType())) {
                            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method " +
                                    "declaration", invoke.getMethod());
                            continue;
                        }
                    }
                    for (ValueType arg : method.getParameterTypes()) {
                        if (!isSupportedType(arg)) {
                            diagnostics.error(callLocation, "Method {{m0}} is not a proper native JavaScript method " +
                                    "or constructor declaration", invoke.getMethod());
                            continue;
                        }
                    }
                    Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                    InvokeInstruction newInvoke = new InvokeInstruction();
                    ValueType[] signature = new ValueType[method.parameterCount() + 3];
                    Arrays.fill(signature, ValueType.object(JSObject.class.getName()));
                    newInvoke.setMethod(new MethodReference(JS.class.getName(),
                            isConstructor ? "instantiate" : "invoke", signature));
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
                }
                block.getInstructions().set(j, replacement.get(0));
                block.getInstructions().addAll(j + 1, replacement.subList(1, replacement.size()));
                j += replacement.size() - 1;
            }
        }
    }

    public void processJSBody(ClassHolder cls, MethodHolder methodToProcess) {
        CallLocation location = new CallLocation(methodToProcess.getReference());
        boolean isStatic = methodToProcess.hasModifier(ElementModifier.STATIC);

        // validate parameter names
        AnnotationHolder bodyAnnot = methodToProcess.getAnnotations().get(JSBody.class.getName());
        int jsParamCount = bodyAnnot.getValue("params").getList().size();
        if (methodToProcess.parameterCount() != jsParamCount) {
            diagnostics.error(location, "JSBody method {{m0}} declares " + methodToProcess.parameterCount() +
                    " parameters, but annotation specifies " + jsParamCount, methodToProcess);
            return;
        }

        // remove annotation and make non-native
        methodToProcess.getAnnotations().remove(JSBody.class.getName());
        methodToProcess.getModifiers().remove(ElementModifier.NATIVE);

        // generate parameter types for original method and validate
        int paramCount = methodToProcess.parameterCount();
        if (!isStatic) {
            ++paramCount;
        }
        ValueType[] paramTypes = new ValueType[paramCount];
        int offset = 0;
        if (!isStatic) {
            ValueType paramType = ValueType.object(cls.getName());
            paramTypes[offset++] = paramType;
            if (!isSupportedType(paramType)) {
                diagnostics.error(location, "Non-static JSBody method {{m0}} is owned by non-JS class {{c1}}",
                        methodToProcess.getReference(), cls.getName());
            }
        }
        if (methodToProcess.getResultType() != ValueType.VOID && !isSupportedType(methodToProcess.getResultType())) {
            diagnostics.error(location, "JSBody method {{m0}} returns unsupported type {{t1}}",
                    methodToProcess.getReference(), methodToProcess.getResultType());
        }

        // generate parameter types for proxy method
        for (int i = 0; i < methodToProcess.parameterCount(); ++i) {
            paramTypes[offset++] = methodToProcess.parameterType(i);
        }
        ValueType[] proxyParamTypes = new ValueType[paramCount + 1];
        for (int i = 0; i < paramCount; ++i) {
            proxyParamTypes[i] = ValueType.parse(JSObject.class);
        }
        proxyParamTypes[paramCount] = methodToProcess.getResultType() == ValueType.VOID ? ValueType.VOID :
                ValueType.parse(JSObject.class);

        // create proxy method
        MethodHolder proxyMethod = new MethodHolder("$js_body$_" + methodIndexGenerator++, proxyParamTypes);
        proxyMethod.getModifiers().add(ElementModifier.NATIVE);
        proxyMethod.getModifiers().add(ElementModifier.STATIC);
        AnnotationHolder genBodyAnnot = new AnnotationHolder(JSBodyImpl.class.getName());
        genBodyAnnot.getValues().put("script", bodyAnnot.getValue("script"));
        genBodyAnnot.getValues().put("params", bodyAnnot.getValue("params"));
        genBodyAnnot.getValues().put("isStatic", new AnnotationValue(isStatic));
        AnnotationHolder generatorAnnot = new AnnotationHolder(GeneratedBy.class.getName());
        generatorAnnot.getValues().put("value", new AnnotationValue(ValueType.parse(JSBodyGenerator.class)));
        proxyMethod.getAnnotations().add(genBodyAnnot);
        proxyMethod.getAnnotations().add(generatorAnnot);
        cls.addMethod(proxyMethod);

        // create program that invokes proxy method
        program = new Program();
        BasicBlock block = program.createBasicBlock();
        List<Variable> params = new ArrayList<>();
        for (int i = 0; i < paramCount; ++i) {
            params.add(program.createVariable());
        }
        methodToProcess.setProgram(program);

        // Generate invoke instruction
        replacement.clear();
        InvokeInstruction invoke = new InvokeInstruction();
        invoke.setType(InvocationType.SPECIAL);
        invoke.setMethod(proxyMethod.getReference());
        for (int i = 0; i < paramCount; ++i) {
            Variable var = program.createVariable();
            invoke.getArguments().add(wrapArgument(location, var, paramTypes[i]));
        }
        block.getInstructions().addAll(replacement);
        block.getInstructions().add(invoke);

        // Generate return
        ExitInstruction exit = new ExitInstruction();
        if (methodToProcess.getResultType() != ValueType.VOID) {
            replacement.clear();
            Variable result = program.createVariable();
            invoke.setReceiver(result);
            exit.setValueToReturn(unwrap(location, result, methodToProcess.getResultType()));
            block.getInstructions().addAll(replacement);
        }
        block.getInstructions().add(exit);
    }

    private void addPropertyGet(String propertyName, Variable instance, Variable receiver,
            InstructionLocation location) {
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

    private void addPropertySet(String propertyName, Variable instance, Variable value, InstructionLocation location) {
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

    private void addIndexerGet(Variable array, Variable index, Variable receiver, InstructionLocation location) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setType(InvocationType.SPECIAL);
        insn.setMethod(new MethodReference(JS.class, "get", JSObject.class, JSObject.class, JSObject.class));
        insn.setReceiver(receiver);
        insn.getArguments().add(array);
        insn.getArguments().add(index);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private void addIndexerSet(Variable array, Variable index, Variable value, InstructionLocation location) {
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

    private void copyVar(Variable a, Variable b, InstructionLocation location) {
        AssignInstruction insn = new AssignInstruction();
        insn.setAssignee(a);
        insn.setReceiver(b);
        insn.setLocation(location);
        replacement.add(insn);
    }

    private Variable addStringWrap(Variable var, InstructionLocation location) {
        return wrap(var, ValueType.object("java.lang.String"), location);
    }

    private Variable addString(String str, InstructionLocation location) {
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
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    return unwrap(var, "unwrapBoolean", ValueType.BOOLEAN, location.getSourceLocation());
                case BYTE:
                    return unwrap(var, "unwrapByte", ValueType.BYTE, location.getSourceLocation());
                case SHORT:
                    return unwrap(var, "unwrapShort", ValueType.SHORT, location.getSourceLocation());
                case INTEGER:
                    return unwrap(var, "unwrapInt", ValueType.INTEGER, location.getSourceLocation());
                case CHARACTER:
                    return unwrap(var, "unwrapCharacter", ValueType.CHARACTER, location.getSourceLocation());
                case DOUBLE:
                    return unwrap(var, "unwrapDouble", ValueType.DOUBLE, location.getSourceLocation());
                case FLOAT:
                    return unwrap(var, "unwrapFloat", ValueType.FLOAT, location.getSourceLocation());
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", ValueType.object("java.lang.String"), location.getSourceLocation());
            } else {
                Variable result = program.createVariable();
                CastInstruction castInsn = new CastInstruction();
                castInsn.setReceiver(result);
                castInsn.setValue(var);
                castInsn.setTargetType(type);
                castInsn.setLocation(location.getSourceLocation());
                replacement.add(castInsn);
                return result;
            }
        }
        diagnostics.error(location, "Unsupported type: {{t0}}", type);
        return var;
    }

    private Variable unwrap(Variable var, String methodName, ValueType resultType, InstructionLocation location) {
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), methodName, ValueType.object(JSObject.class.getName()),
                resultType));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location);
        replacement.add(insn);
        return result;
    }

    private Variable wrapArgument(CallLocation location, Variable var, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            ClassReader cls = classSource.get(className);
            if (cls.getAnnotations().get(JSFunctor.class.getName()) != null) {
                return wrapFunctor(location, var, cls);
            }
        }
        return wrap(var, type, location.getSourceLocation());
    }

    private Variable wrapFunctor(CallLocation location, Variable var, ClassReader type) {
        if (!type.hasModifier(ElementModifier.INTERFACE) || type.getMethods().size() != 1) {
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

    private Variable wrap(Variable var, ValueType type, InstructionLocation location) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            if (!className.equals("java.lang.String")) {
                return var;
            }
        }
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), "wrap", type, ValueType.parse(JSObject.class)));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        insn.setLocation(location);
        replacement.add(insn);
        return result;
    }

    private MethodReader getMethod(MethodReference ref) {
        ClassReader cls = classSource.get(ref.getClassName());
        MethodReader method = cls.getMethod(ref.getDescriptor());
        if (method != null) {
            return method;
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
        if (desc.parameterCount() > 0 || !isSupportedType(desc.getResultType())) {
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
        if (desc.parameterCount() != 1 || !isSupportedType(desc.parameterType(0)) ||
                desc.getResultType() != ValueType.VOID) {
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
        return desc.parameterCount() == 1 && isSupportedType(desc.parameterType(0)) &&
                isSupportedType(desc.getResultType());
    }

    private boolean isProperSetIndexer(MethodDescriptor desc) {
        return desc.parameterCount() == 2 && isSupportedType(desc.parameterType(0)) &&
                isSupportedType(desc.parameterType(0)) && desc.getResultType() == ValueType.VOID;
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

    private boolean isSupportedType(ValueType type) {
        if (type == ValueType.VOID) {
            return false;
        }
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case LONG:
                    return false;
                default:
                    return true;
            }
        } else if (type instanceof ValueType.Array) {
            return isSupportedType(((ValueType.Array)type).getItemType());
        } else if (type instanceof ValueType.Object) {
            String typeName = ((ValueType.Object)type).getClassName();
            return typeName.equals("java.lang.String") || nativeRepos.isJavaScriptClass(typeName);
        } else {
            return false;
        }
    }
}
