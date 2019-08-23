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
import org.teavm.interop.Async;
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
    private Map<MethodReference, Boolean> asyncFamilyMethods = new HashMap<>();
    private Set<MethodReference> readonlyAsyncMethods = Collections.unmodifiableSet(asyncMethods);
    private Set<MethodReference> readonlyAsyncFamilyMethods = Collections.unmodifiableSet(asyncFamilyMethods.keySet());
    private CallGraph callGraph;
    private ListableClassReaderSource classSource;
    private boolean hasAsyncMethods;

    public AsyncMethodFinder(CallGraph callGraph) {
        this.callGraph = callGraph;
    }

    public Set<MethodReference> getAsyncMethods() {
        return readonlyAsyncMethods;
    }

    public Set<MethodReference> getAsyncFamilyMethods() {
        return readonlyAsyncFamilyMethods;
    }

    public void find(ListableClassReaderSource classSource) {
        this.classSource = classSource;
        hasAsyncMethods = findAsyncMethods();
        for (String clsName : classSource.getClassNames()) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                if (asyncMethods.contains(method.getReference())) {
                    continue;
                }
                if (method.getAnnotations().get(Async.class.getName()) != null) {
                    add(method.getReference(), new CallStack(method.getReference(), null));
                }
            }
        }
        if (hasAsyncMethods) {
            for (String clsName : classSource.getClassNames()) {
                ClassReader cls = classSource.get(clsName);
                for (MethodReader method : cls.getMethods()) {
                    if (asyncMethods.contains(method.getReference()) || method.getProgram() == null) {
                        continue;
                    }
                    if (hasMonitor(method)) {
                        add(method.getReference(), new CallStack(method.getReference(), null));
                    }
                }
            }
        }
        for (MethodReference method : asyncMethods) {
            addOverriddenToFamily(method);
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
        ClassReader cls = classSource.get("java.lang.Thread");
        MethodReader method = cls != null ? cls.getMethod(new MethodDescriptor("start", void.class)) : null;
        return result && method != null;
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
                && (methodRef.getName().equals("monitorEnter") || methodRef.getName().equals("monitorExit"))) {
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
        asyncFamilyMethods.put(methodRef, true);
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        for (MethodReference overriddenMethod : findOverriddenMethods(cls, methodRef.getDescriptor())) {
            addOverriddenToFamily(overriddenMethod);
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
        for (MethodReference overriddenMethod : findOverriddenMethods(cls, methodRef.getDescriptor())) {
            if (addToFamily(overriddenMethod)) {
                return true;
            }
        }
        return false;
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
        if (method != null) {
            if (!method.hasModifier(ElementModifier.STATIC) && !method.hasModifier(ElementModifier.FINAL)) {
                result.add(method.getReference());
            }
        } else {
            if (cls.getParent() != null) {
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
