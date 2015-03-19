/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.*;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.javascript.spi.Async;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.javascript.spi.Sync;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class AsyncMethodFinder {
    private Set<MethodReference> asyncMethods = new HashSet<>();
    private Map<MethodReference, Boolean> asyncFamilyMethods = new HashMap<>();
    private Set<MethodReference> readonlyAsyncMethods = Collections.unmodifiableSet(asyncMethods);
    private Set<MethodReference> readonlyAsyncFamilyMethods = Collections.unmodifiableSet(asyncFamilyMethods.keySet());
    private CallGraph callGraph;
    private Diagnostics diagnostics;
    private ListableClassReaderSource classSource;

    public AsyncMethodFinder(CallGraph callGraph, Diagnostics diagnostics) {
        this.callGraph = callGraph;
        this.diagnostics = diagnostics;
    }

    public Set<MethodReference> getAsyncMethods() {
        return readonlyAsyncMethods;
    }

    public Set<MethodReference> getAsyncFamilyMethods() {
        return readonlyAsyncFamilyMethods;
    }

    public void find(ListableClassReaderSource classSource) {
        this.classSource = classSource;
        for (String clsName : classSource.getClassNames()) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                if (asyncMethods.contains(method.getReference())) {
                    continue;
                }
                if (method.getAnnotations().get(Async.class.getName()) != null) {
                    add(method.getReference());
                }
            }
        }
        if (hasAsyncMethods()) {
            for (String clsName : classSource.getClassNames()) {
                ClassReader cls = classSource.get(clsName);
                for (MethodReader method : cls.getMethods()) {
                    if (asyncMethods.contains(method.getReference()) || method.getProgram() == null) {
                        continue;
                    }
                    ProgramReader program = method.getProgram();
                    AsyncInstructionReader insnReader = new AsyncInstructionReader();
                    for (int i = 0; i < program.basicBlockCount(); ++i) {
                        program.basicBlockAt(i).readAllInstructions(insnReader);
                        if (insnReader.async) {
                            add(method.getReference());
                            break;
                        }
                    }
                }
            }
        }
        for (MethodReference method : asyncMethods) {
            addOverridenToFamily(method);
        }
        for (String clsName : classSource.getClassNames()) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                addToFamily(method.getReference());
            }
        }
        for (Map.Entry<MethodReference, Boolean> entry : new ArrayList<>(asyncFamilyMethods.entrySet())) {
            if (!entry.getValue()) {
                asyncFamilyMethods.remove(entry.getKey());
            }
        }
    }

    private boolean hasAsyncMethods() {
        int count = asyncMethods.size();
        if (asyncMethods.contains(new MethodReference(Object.class, "monitorEnter", Object.class, void.class))) {
            --count;
        }
        if (asyncMethods.contains(new MethodReference(Object.class, "monitorEnter", Object.class,
                int.class, void.class))) {
            --count;
        }
        if (asyncMethods.contains(new MethodReference(Object.class, "monitorEnterWait", Object.class,
                int.class, void.class))) {
            --count;
        }
        ClassReader cls = classSource.get("java.lang.Thread");
        MethodReader method = cls != null ? cls.getMethod(new MethodDescriptor("start", void.class)) : null;
        return count > 0 && method != null;
    }

    private void add(MethodReference methodRef) {
        if (!asyncMethods.add(methodRef)) {
            return;
        }
        CallGraphNode node = callGraph.getNode(methodRef);
        if (node == null) {
            return;
        }
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        if (method == null) {
            return;
        }
        if (method.getAnnotations().get(Sync.class.getName()) != null ||
                method.getAnnotations().get(InjectedBy.class.getName()) != null) {
            diagnostics.error(new CallLocation(methodRef), "Method {{m0}} is claimed to be synchronous, " +
                    "but it is has invocations of asynchronous methods", methodRef);
            return;
        }
        for (CallSite callSite : node.getCallerCallSites()) {
            add(callSite.getCaller().getMethod());
        }
    }

    private void addOverridenToFamily(MethodReference methodRef) {
        asyncFamilyMethods.put(methodRef, true);
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        for (MethodReference overridenMethod : findOverridenMethods(cls, methodRef)) {
            addOverridenToFamily(overridenMethod);
        }
    }

    private boolean addToFamily(MethodReference methodRef) {
        Boolean cachedResult = asyncFamilyMethods.get(methodRef);
        if (cachedResult != null) {
            return cachedResult;
        }
        boolean result = addToFamilyCacheMiss(methodRef);
        asyncFamilyMethods.put(methodRef, result);
        return result;
    }

    private boolean addToFamilyCacheMiss(MethodReference methodRef) {
        if (asyncMethods.contains(methodRef)) {
            return true;
        }
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return false;
        }
        for (MethodReference overridenMethod : findOverridenMethods(cls, methodRef)) {
            if (addToFamily(overridenMethod)) {
                return true;
            }
        }
        return false;
    }

    private Set<MethodReference> findOverridenMethods(ClassReader cls, MethodReference methodRef) {
        List<String> parents = new ArrayList<>();
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            parents.add(cls.getParent());
        }
        parents.addAll(cls.getInterfaces());

        Set<MethodReference> visited = new HashSet<>();
        Set<MethodReference> overriden = new HashSet<>();
        for (String parent : parents) {
            findOverridenMethods(new MethodReference(parent, methodRef.getDescriptor()), overriden, visited);
        }
        return overriden;
    }

    private void findOverridenMethods(MethodReference methodRef, Set<MethodReference> result,
            Set<MethodReference> visited) {
        if (!visited.add(methodRef)) {
            return;
        }
        if (methodRef.getName().equals("<init>") || methodRef.getName().equals("<clinit>")) {
            return;
        }
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        if (method != null) {
            if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.FINAL)) {
                result.add(methodRef);
            }
        } else {
            if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
                findOverridenMethods(new MethodReference(cls.getParent(), methodRef.getDescriptor()), result, visited);
            }
            for (String iface : cls.getInterfaces()) {
                findOverridenMethods(new MethodReference(iface, methodRef.getDescriptor()), result, visited);
            }
        }
    }

    class AsyncInstructionReader implements InstructionReader {
        boolean async;

        @Override
        public void location(InstructionLocation location) {
        }

        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
        }

        @Override
        public void nullConstant(VariableReader receiver) {
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
        }

        @Override
        public void jump(BasicBlockReader target) {
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
        }

        @Override
        public void exit(VariableReader valueToReturn) {
        }

        @Override
        public void raise(VariableReader exception) {
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
        }

        @Override
        public void create(VariableReader receiver, String type) {
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value,
                ValueType fieldType) {
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value) {
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        }

        @Override
        public void initClass(String className) {
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
            async = true;
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
        }
    }
}
