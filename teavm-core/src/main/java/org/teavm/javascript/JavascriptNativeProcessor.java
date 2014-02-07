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
package org.teavm.javascript;

import java.util.*;
import org.teavm.javascript.ni.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
class JavascriptNativeProcessor {
    private ClassHolderSource classSource;
    private Program program;
    private List<Instruction> replacement = new ArrayList<>();
    private NativeJavascriptClassRepository nativeRepos;

    public JavascriptNativeProcessor(ClassHolderSource classSource) {
        this.classSource = classSource;
        nativeRepos = new NativeJavascriptClassRepository(classSource);
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
        ClassHolder iface = classSource.getClassHolder(ifaceName);
        for (MethodHolder method : iface.getMethods()) {
            methods.add(method.getDescriptor());
        }
        for (String superIfaceName : iface.getInterfaces()) {
            addPreservedMethods(superIfaceName, methods);
        }
    }

    public void processProgram(Program program) {
        this.program = program;
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
                MethodHolder method = getMethod(invoke.getMethod());
                if (method.getAnnotations().get(JSProperty.class.getName()) != null) {
                    if (isProperGetter(method.getDescriptor())) {
                        String propertyName = method.getName().charAt(0) == 'i' ? cutPrefix(method.getName(), 2) :
                                cutPrefix(method.getName(), 3);
                        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                        addPropertyGet(propertyName, invoke.getInstance(), result);
                        if (result != null) {
                            result = unwrap(result, method.getResultType());
                            copyVar(result, invoke.getReceiver());
                        }
                    } else if (isProperSetter(method.getDescriptor())) {
                        Variable wrapped = wrap(invoke.getArguments().get(3), method.parameterType(2));
                        addPropertySet(cutPrefix(method.getName(), 3), invoke.getArguments().get(0), wrapped);
                    } else {
                        throw new RuntimeException("Method " + invoke.getMethod() + " is not " +
                                "a proper native JavaScript property declaration");
                    }
                } else if (method.getAnnotations().get(JSIndexer.class.getName()) != null) {
                    if (isProperGetIndexer(method.getDescriptor())) {
                        Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                        addIndexerGet(invoke.getInstance(), wrap(invoke.getArguments().get(0),
                                method.parameterType(0)), result);
                        if (result != null) {
                            result = unwrap(result, method.getResultType());
                            copyVar(result, invoke.getReceiver());
                        }
                    } else if (isProperSetIndexer(method.getDescriptor())) {
                        Variable index = wrap(invoke.getArguments().get(0), method.parameterType(0));
                        Variable value = wrap(invoke.getArguments().get(1), method.parameterType(1));
                        addIndexerSet(invoke.getInstance(), index, value);
                    } else {
                        throw new RuntimeException("Method " + invoke.getMethod() + " is not " +
                                "a proper native JavaScript indexer declaration");
                    }
                } else {
                    if (method.getResultType() != ValueType.VOID && !isSupportedType(method.getResultType())) {
                        throw new RuntimeException("Method " + invoke.getMethod() + " is not " +
                                "a proper native JavaScript method declaration");
                    }
                    for (ValueType arg : method.getParameterTypes()) {
                        if (!isSupportedType(arg)) {
                            throw new RuntimeException("Method " + invoke.getMethod() + " is not " +
                                    "a proper native JavaScript method declaration");
                        }
                    }
                    Variable result = invoke.getReceiver() != null ? program.createVariable() : null;
                    InvokeInstruction newInvoke = new InvokeInstruction();
                    ValueType[] signature = new ValueType[method.parameterCount() + 3];
                    Arrays.fill(signature, ValueType.object(JSObject.class.getName()));
                    newInvoke.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("invoke",
                            signature)));
                    newInvoke.setType(InvocationType.SPECIAL);
                    newInvoke.setReceiver(result);
                    newInvoke.getArguments().add(invoke.getInstance());
                    newInvoke.getArguments().add(addStringWrap(addString(method.getName())));
                    for (int k = 0; k < invoke.getArguments().size(); ++k) {
                        Variable arg = wrap(invoke.getArguments().get(k), method.parameterType(k));
                        newInvoke.getArguments().add(arg);
                    }
                    replacement.add(newInvoke);
                    if (result != null) {
                        result = unwrap(result, method.getResultType());
                        copyVar(result, invoke.getReceiver());
                    }
                }
                block.getInstructions().set(j, replacement.get(0));
                block.getInstructions().addAll(j + 1, replacement.subList(1, replacement.size()));
                j += replacement.size() - 1;
            }
        }
    }

    private void addPropertyGet(String propertyName, Variable instance, Variable receiver) {
        Variable nameVar = addStringWrap(addString(propertyName));
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("get",
                ValueType.object(JSObject.class.getName()), ValueType.object(JSObject.class.getName()),
                ValueType.object(JSObject.class.getName()))));
        insn.setReceiver(receiver);
        insn.getArguments().add(instance);
        insn.getArguments().add(nameVar);
        replacement.add(insn);
    }

    private void addPropertySet(String propertyName, Variable instance, Variable value) {
        Variable nameVar = addStringWrap(addString(propertyName));
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("set",
                ValueType.object(JSObject.class.getName()), ValueType.object(JSObject.class.getName()),
                ValueType.VOID)));
        insn.getArguments().add(instance);
        insn.getArguments().add(nameVar);
        insn.getArguments().add(value);
        replacement.add(insn);
    }

    private void addIndexerGet(Variable array, Variable index, Variable receiver) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("get",
                ValueType.object(JSObject.class.getName()), ValueType.object(JSObject.class.getName()),
                ValueType.object(JSObject.class.getName()))));
        insn.setReceiver(receiver);
        insn.getArguments().add(array);
        insn.getArguments().add(index);
        replacement.add(insn);
    }

    private void addIndexerSet(Variable array, Variable index, Variable value) {
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("set",
                ValueType.object(JSObject.class.getName()), ValueType.object(JSObject.class.getName()),
                ValueType.object(JSObject.class.getName()), ValueType.VOID)));
        insn.getArguments().add(array);
        insn.getArguments().add(index);
        insn.getArguments().add(value);
        replacement.add(insn);
    }

    private void copyVar(Variable a, Variable b) {
        AssignInstruction insn = new AssignInstruction();
        insn.setAssignee(a);
        insn.setReceiver(b);
        replacement.add(insn);
    }

    private Variable addStringWrap(Variable var) {
        return wrap(var, ValueType.object("java.lang.String"));
    }

    private Variable addString(String str) {
        Variable var = program.createVariable();
        StringConstantInstruction nameInsn = new StringConstantInstruction();
        nameInsn.setReceiver(var);
        nameInsn.setConstant(str);
        replacement.add(nameInsn);
        return var;
    }

    private Variable unwrap(Variable var, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive)type).getKind()) {
                case BOOLEAN:
                    return unwrap(var, "unwrapBoolean", ValueType.BOOLEAN);
                case BYTE:
                    return unwrap(var, "unwrapByte", ValueType.BYTE);
                case SHORT:
                    return unwrap(var, "unwrapShort", ValueType.SHORT);
                case INTEGER:
                    return unwrap(var, "unwrapShort", ValueType.INTEGER);
                case CHARACTER:
                    return unwrap(var, "unwrapCharacter", ValueType.CHARACTER);
                case DOUBLE:
                    return unwrap(var, "unwrapDouble", ValueType.DOUBLE);
                case FLOAT:
                    return unwrap(var, "unwrapFloat", ValueType.FLOAT);
                case LONG:
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            if (className.equals(JSObject.class.getName())) {
                return var;
            } else if (className.equals("java.lang.String")) {
                return unwrap(var, "unwrapString", ValueType.object("java.lang.String"));
            } else {
                Variable result = program.createVariable();
                CastInstruction castInsn = new CastInstruction();
                castInsn.setReceiver(result);
                castInsn.setValue(var);
                castInsn.setTargetType(type);
                replacement.add(castInsn);
                return result;
            }
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private Variable unwrap(Variable var, String methodName, ValueType resultType) {
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor(methodName,
                resultType)));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        replacement.add(insn);
        return result;
    }

    private Variable wrap(Variable var, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            if (!className.equals("java.lang.String")) {
                return var;
            }
        }
        Variable result = program.createVariable();
        InvokeInstruction insn = new InvokeInstruction();
        insn.setMethod(new MethodReference(JS.class.getName(), new MethodDescriptor("wrap", type,
                ValueType.object(JSObject.class.getName()))));
        insn.getArguments().add(var);
        insn.setReceiver(result);
        insn.setType(InvocationType.SPECIAL);
        replacement.add(insn);
        return result;
    }

    private MethodHolder getMethod(MethodReference ref) {
        ClassHolder cls = classSource.getClassHolder(ref.getClassName());
        MethodHolder method = cls.getMethod(ref.getDescriptor());
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
