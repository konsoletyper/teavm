/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.model.lowlevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.common.DisjointSet;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Function;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;

public class ExportDependencyListener extends AbstractDependencyListener {
    private Set<MethodReference> exportedMethods = new LinkedHashSet<>();
    private Set<? extends MethodReference> readonlyExportedMethods = Collections.unmodifiableSet(exportedMethods);
    private Map<ExportedMethodKey, MethodReference> resolvedMethods = new HashMap<>();
    private Map<? extends ExportedMethodKey, ? extends MethodReference> readonlyResolvedMethods =
            Collections.unmodifiableMap(resolvedMethods);
    private Characteristics characteristics;

    @Override
    public void started(DependencyAgent agent) {
        characteristics = new Characteristics(agent.getClassSource());
    }

    public Set<? extends MethodReference> getExportedMethods() {
        return readonlyExportedMethods;
    }

    public Map<? extends ExportedMethodKey, ? extends MethodReference> getResolvedMethods() {
        return readonlyResolvedMethods;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod() == null || method.getMethod().getProgram() == null) {
            return;
        }

        ProgramReader program = method.getMethod().getProgram();
        FunctionGetFinder finder = new FunctionGetFinder(program.variableCount());
        for (BasicBlockReader block : program.getBasicBlocks()) {
            block.readAllInstructions(finder);
        }

        if (!finder.invocations.isEmpty()) {
            processInvocations(agent, method.getMethod(), finder);
        }
    }

    private void processInvocations(DependencyAgent agent, MethodReader method, FunctionGetFinder finder) {
        int[] variableClasses = finder.variableClasses.pack(method.getProgram().variableCount());
        String[] stringConstants = new String[finder.stringConstants.length];
        ValueType[] classConstants = new ValueType[finder.classConstants.length];
        for (int i = 0; i < stringConstants.length; ++i) {
            stringConstants[variableClasses[i]] = finder.stringConstants[i];
            classConstants[variableClasses[i]] = finder.classConstants[i];
        }

        Diagnostics diagnostics = agent.getDiagnostics();

        for (Invocation invocation : finder.invocations) {
            ValueType functionClass = classConstants[variableClasses[invocation.functionClassVar]];
            ValueType targetClass = classConstants[variableClasses[invocation.classVar]];

            String methodName = stringConstants[variableClasses[invocation.methodVar]];
            CallLocation location = new CallLocation(method.getReference(), invocation.location);

            boolean valid = true;
            if (!(functionClass instanceof ValueType.Object)) {
                diagnostics.error(location, "First argument must be class literal representing "
                        + "non-array and no-primitive class");
                valid = false;
            }

            if (!(targetClass instanceof ValueType.Object)) {
                diagnostics.error(location, "Second argument must be class literal representing "
                        + "non-array and no-primitive class");
                valid = false;
            }

            if (methodName == null) {
                diagnostics.error(location, "Third argument must be string literal");
                valid = false;
            }

            if (valid) {
                processInvocation(agent, location, ((ValueType.Object) functionClass).getClassName(),
                        ((ValueType.Object) targetClass).getClassName(), methodName);
            }
        }
    }

    private void processInvocation(DependencyAgent agent, CallLocation location, String functionClassName,
            String targetClassName, String methodName) {
        Diagnostics diagnostics = agent.getDiagnostics();
        ClassHierarchy hierarchy = agent.getClassHierarchy();
        boolean valid = true;

        ClassReader functionClass = hierarchy.getClassSource().get(functionClassName);
        if (functionClass == null) {
            diagnostics.error(location, "Class '{{c0}}' not found in class path", functionClassName);
            valid = false;
        } else if (!characteristics.isFunction(functionClassName)) {
            diagnostics.error(location, "Class '{{c0}}' does not represent a function", functionClassName);
            valid = false;
        }

        ClassReader targetClass = hierarchy.getClassSource().get(targetClassName);
        if (targetClass == null) {
            diagnostics.error(location, "Class '{{c0}}' not found in class path", functionClassName);
            valid = false;
        }

        if (!valid) {
            return;
        }

        MethodReader sam = extractSingleMethod(diagnostics, location, functionClass);
        if (sam == null) {
            valid = false;
        }

        List<MethodReader> candidates = targetClass.getMethods().stream()
                .filter(method -> method.getName().equals(methodName) && method.hasModifier(ElementModifier.STATIC))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            diagnostics.error(location, "There's no static method '" + methodName + "' in class '{{c0}}'",
                    targetClass.getName());
            valid = false;
        }

        if (!valid) {
            return;
        }

        List<MethodReader> signatureCandidates = candidates.stream()
                .filter(method -> matchSignature(hierarchy, sam, method))
                .collect(Collectors.toList());
        if (signatureCandidates.isEmpty()) {
            if (candidates.size() == 1) {
                diagnostics.error(location, "Method '{{m0}}' does not match signature of function method '{{m1}}'",
                        candidates.get(0).getReference(), sam.getReference());
            } else {
                diagnostics.error(location, "None of '" + methodName + "' methods match signature of function "
                        + "method '{{m0}}'", sam.getReference());
            }
            return;
        }

        MethodReader resolvedMethod = findMostSpecific(diagnostics, location, hierarchy, signatureCandidates);
        if (resolvedMethod != null) {
            MethodReference reference = resolvedMethod.getReference();
            resolvedMethods.put(new ExportedMethodKey(functionClassName, targetClassName, methodName), reference);
            exportedMethods.add(reference);
            MethodDependency dep = agent.linkMethod(reference);
            dep.addLocation(location);
            dep.use();
        }
    }

    private MethodReader extractSingleMethod(Diagnostics diagnostics, CallLocation location, ClassReader cls) {
        MethodReader candidate = null;
        for (MethodReader method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.STATIC) || !method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }

            if (candidate != null) {
                diagnostics.error(location, "Function class {{c0}} must have one abstract method, it has multiple",
                        cls.getName());
                return null;
            } else {
                candidate = method;
            }
        }

        if (candidate == null) {
            diagnostics.error(location, "Function class {{c0}} must have one abstract method, it has none",
                    cls.getName());
            return null;
        }

        return candidate;
    }

    private MethodReader findMostSpecific(Diagnostics diagnostics, CallLocation location,
            ClassHierarchy hierarchy, List<MethodReader> methods) {
        MethodReader mostSpecificSoFar = methods.get(0);
        for (int i = 1; i < methods.size(); ++i) {
            MethodReader candidate = methods.get(i);
            if (matchSignature(hierarchy, mostSpecificSoFar, candidate)) {
                mostSpecificSoFar = candidate;
            } else if (!matchSignature(hierarchy, candidate, mostSpecificSoFar)) {
                diagnostics.error(location, "Ambiguous methods found for this export, examples are '{{m0}}' "
                        + "and {{m1}}", candidate, mostSpecificSoFar);
                return null;
            }
        }

        return mostSpecificSoFar;
    }

    private boolean matchSignature(ClassHierarchy hierarchy, MethodReader functionMethod,
            MethodReader candidateMethod) {
        if (functionMethod.parameterCount() > candidateMethod.parameterCount()) {
            return false;
        }

        for (int i = 0; i < functionMethod.parameterCount(); ++i) {
            if (!hierarchy.isSuperType(functionMethod.parameterType(i), candidateMethod.parameterType(i), false)) {
                return false;
            }
        }

        return true;
    }

    static class FunctionGetFinder extends AbstractInstructionReader {
        DisjointSet variableClasses = new DisjointSet();
        String[] stringConstants;
        ValueType[] classConstants;
        List<Invocation> invocations = new ArrayList<>();
        private TextLocation location;

        FunctionGetFinder(int variableCount) {
            for (int i = 0; i < variableCount; ++i) {
                variableClasses.create();
            }
            stringConstants = new String[variableCount];
            classConstants = new ValueType[variableCount];
        }

        @Override
        public void location(TextLocation location) {
            this.location = location;
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            classConstants[receiver.getIndex()] = cst;
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            stringConstants[receiver.getIndex()] = cst;
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            variableClasses.union(receiver.getIndex(), assignee.getIndex());
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (method.getClassName().equals(Function.class.getName()) && method.getName().equals("get")
                    && type == InvocationType.SPECIAL && instance == null && arguments.size() == 3) {
                Invocation invocation = new Invocation(location, arguments.get(0).getIndex(),
                        arguments.get(1).getIndex(), arguments.get(2).getIndex());
                invocations.add(invocation);
            }
        }
    }

    static class Invocation {
        TextLocation location;
        int functionClassVar;
        int classVar;
        int methodVar;

        Invocation(TextLocation location, int functionClassVar, int classVar, int methodVar) {
            this.location = location;
            this.functionClassVar = functionClassVar;
            this.classVar = classVar;
            this.methodVar = methodVar;
        }
    }
}
