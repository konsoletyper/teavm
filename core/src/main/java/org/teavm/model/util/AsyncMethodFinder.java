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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.dependency.DependencyInfo;
import org.teavm.interop.Async;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.runtime.Fiber;

public class AsyncMethodFinder {
    private Set<MethodReference> asyncMethods = new HashSet<>();
    private DependencyInfo dependency;
    private Set<MethodReference> asyncFamilyMethods = new HashSet<>();
    private Set<MethodReference> readonlyAsyncMethods = Collections.unmodifiableSet(asyncMethods);
    private Set<MethodReference> readonlyAsyncFamilyMethods = Collections.unmodifiableSet(asyncFamilyMethods);
    private Map<String, List<String>> subclasses = new HashMap<>();
    private Set<MethodReference> methodVisitedByImplicitOverridesPass = new HashSet<>();
    private CallGraph callGraph;
    private ListableClassReaderSource classSource;
    private boolean hasAsyncMethods;

    public AsyncMethodFinder(CallGraph callGraph, DependencyInfo dependency) {
        this.callGraph = callGraph;
        this.dependency = dependency;
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
                if (!dependency.getReachableMethods().contains(method.getReference())
                        || asyncMethods.contains(method.getReference())) {
                    continue;
                }
                if (method.getAnnotations().get(Async.class.getName()) != null) {
                    add(method.getReference(), new CallStack(method.getReference(), null));
                }
            }
        }
        hasAsyncMethods = findAsyncMethods();
        if (hasAsyncMethods) {
            for (String clsName : classSource.getClassNames()) {
                ClassReader cls = classSource.get(clsName);
                for (MethodReader method : cls.getMethods()) {
                    if (!dependency.getReachableMethods().contains(method.getReference())
                            || asyncMethods.contains(method.getReference()) || method.getProgram() == null) {
                        continue;
                    }
                    if (hasMonitor(method)) {
                        add(method.getReference(), new CallStack(method.getReference(), null));
                    }
                }
            }
        }
        if (!asyncMethods.isEmpty()) {
            for (var clsName : classSource.getClassNames()) {
                var cls = classSource.get(clsName);
                if (cls.getParent() != null && !cls.hasModifier(ElementModifier.INTERFACE)) {
                    subclasses.computeIfAbsent(cls.getParent(), k -> new ArrayList<>()).add(clsName);
                }
                for (var itf : cls.getInterfaces()) {
                    subclasses.computeIfAbsent(itf, k -> new ArrayList<>()).add(clsName);
                }
            }
            for (var methodRef : asyncMethods) {
                var method = classSource.getMethod(methodRef);
                if (method == null || method.hasModifier(ElementModifier.STATIC)
                        || method.getLevel() == AccessLevel.PRIVATE || method.getName().equals("<init>")) {
                    // Not virtual: no overridden methods, but it's still a call target that can suspend
                    asyncFamilyMethods.add(methodRef);
                } else {
                    addOverriddenToFamily(methodRef);
                    addImplicitOverrides(method, method.getOwnerName());
                }
            }
            subclasses.clear();
            methodVisitedByImplicitOverridesPass.clear();
        }
    }

    private void addImplicitOverrides(MethodReader method, String className) {
        if (!methodVisitedByImplicitOverridesPass.add(new MethodReference(className, method.getDescriptor()))) {
            return;
        }
        var clsSubclasses = subclasses.get(className);
        if (clsSubclasses == null) {
            return;
        }
        for (var subclassName : clsSubclasses) {
            var subclass = classSource.get(subclassName);
            var subclassMethod = subclass.getMethod(method.getDescriptor());
            if (subclassMethod != null) {
                continue;
            }
            for (var itfName : subclass.getInterfaces()) {
                var itf = classSource.get(itfName);
                var itfMethod = itf != null ? itf.getMethod(method.getDescriptor()) : null;
                if (itfMethod == null || !(itfMethod.hasModifier(ElementModifier.STATIC)
                        || itfMethod.getLevel() == AccessLevel.PRIVATE)) {
                    addOverriddenToFamily(new MethodReference(itfName, method.getDescriptor()));
                }
            }
            addImplicitOverrides(method, subclassName);
        }
    }

    private boolean findAsyncMethods() {
        boolean result = false;
        loop: for (String clsName : classSource.getClassNames()) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                if (!asyncMethods.contains(method.getReference()) || method.getProgram() == null) {
                    continue;
                }
                if (hasMonitor(method)) {
                    result = true;
                    break loop;
                }
            }
        }
        boolean hasThreads = dependency.getReachableMethods().contains(new MethodReference(
                Thread.class, "start", void.class));
        return result && hasThreads;
    }

    public boolean hasAsyncMethods() {
        return hasAsyncMethods;
    }

    private boolean hasMonitor(MethodReader method) {
        if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
            return true;
        }
        ProgramReader program = method.getProgram();
        AsyncInstructionReader insnReader = new AsyncInstructionReader();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            program.basicBlockAt(i).readAllInstructions(insnReader);
            if (insnReader.async) {
                return true;
            }
        }
        return false;
    }

    private void add(MethodReference methodRef, CallStack stack) {
        if (methodRef.getClassName().equals(Fiber.class.getName())) {
            return;
        }

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

        if (!hasAsyncMethods && methodRef.getClassName().equals("java.lang.Object")
                && (methodRef.getName().equals("monitorEnter") || methodRef.getName().equals("monitorExit"))
                && methodRef.parameterCount() == 1) {
            return;
        }
        for (CallSite callSite : node.getCallerCallSites()) {
            for (CallGraphNode caller : callSite.getCallers()) {
                add(caller.getMethod(), new CallStack(caller.getMethod(), stack));
            }
        }
    }

    static class CallStack {
        MethodReference method;
        CallStack next;

        CallStack(MethodReference method, CallStack next) {
            this.method = method;
            this.next = next;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            CallStack stack = this;
            while (stack != null) {
                sb.append("\n    calling " + stack.method);
                stack = stack.next;
            }
            return sb.toString();
        }
    }

    private void addOverriddenToFamily(MethodReference methodRef) {
        if (!asyncFamilyMethods.add(methodRef)) {
            return;
        }
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        for (MethodReference overriddenMethod : findOverriddenMethods(cls, methodRef.getDescriptor())) {
            addOverriddenToFamily(overriddenMethod);
        }
    }

    private Set<MethodReference> findOverriddenMethods(ClassReader cls, MethodDescriptor methodDesc) {
        List<String> parents = new ArrayList<>();
        if (cls.getParent() != null) {
            parents.add(cls.getParent());
        }
        parents.addAll(cls.getInterfaces());

        Set<String> visited = new HashSet<>();
        Set<MethodReference> overridden = new HashSet<>();
        for (String parent : parents) {
            findOverriddenMethods(parent, methodDesc, overridden, visited);
        }
        return overridden;
    }

    private void findOverriddenMethods(String className, MethodDescriptor methodDesc, Set<MethodReference> result,
            Set<String> visited) {
        if (!visited.add(className)) {
            return;
        }
        if (methodDesc.getName().equals("<init>") || methodDesc.getName().equals("<clinit>")) {
            return;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(methodDesc);
        if (method != null && !method.hasModifier(ElementModifier.STATIC)
                && !method.hasModifier(ElementModifier.FINAL) && method.getLevel() != AccessLevel.PRIVATE) {
            result.add(method.getReference());
        } else {
            if (cls.getParent() != null && !cls.hasModifier(ElementModifier.INTERFACE)) {
                findOverriddenMethods(cls.getParent(), methodDesc, result, visited);
            }
            for (String iface : cls.getInterfaces()) {
                findOverriddenMethods(iface, methodDesc, result, visited);
            }
        }
    }

    static class AsyncInstructionReader extends AbstractInstructionReader {
        boolean async;

        @Override
        public void monitorEnter(VariableReader objectRef) {
            async = true;
        }
    }
}
