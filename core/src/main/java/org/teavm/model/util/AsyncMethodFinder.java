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
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Async;
import org.teavm.interop.Sync;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;

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
                    if (hasMonitor(method)) {
                        add(method.getReference());
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
        boolean result = false;
        loop: for (String clsName : classSource.getClassNames()) {
            ClassReader cls = classSource.get(clsName);
            for (MethodReader method : cls.getMethods()) {
                if (!asyncMethods.contains(method.getReference()) || method.getProgram() == null) {
                    continue;
                }
                if (hasMonitor(method)) {
                    break loop;
                }
            }
        }
        ClassReader cls = classSource.get("java.lang.Thread");
        MethodReader method = cls != null ? cls.getMethod(new MethodDescriptor("start", void.class)) : null;
        return result && method != null;
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
        if (method.getAnnotations().get(Sync.class.getName()) != null
                || method.getAnnotations().get(InjectedBy.class.getName()) != null) {
            diagnostics.error(new CallLocation(methodRef), "Method {{m0}} is claimed to be synchronous, "
                    + "but it is has invocations of asynchronous methods", methodRef);
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

    class AsyncInstructionReader extends AbstractInstructionReader {
        boolean async;

        @Override
        public void monitorEnter(VariableReader objectRef) {
            async = true;
        }
    }
}
