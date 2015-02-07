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

import java.util.HashSet;
import java.util.Set;
import org.teavm.callgraph.CallGraph;
import org.teavm.callgraph.CallGraphNode;
import org.teavm.callgraph.CallSite;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.javascript.spi.Async;
import org.teavm.javascript.spi.InjectedBy;
import org.teavm.javascript.spi.Sync;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class AsyncMethodFinder {
    private Set<MethodReference> asyncMethods = new HashSet<>();
    private CallGraph callGraph;
    private Diagnostics diagnostics;
    private ListableClassReaderSource classSource;

    public AsyncMethodFinder(CallGraph callGraph, Diagnostics diagnostics) {
        this.callGraph = callGraph;
        this.diagnostics = diagnostics;
    }

    public Set<MethodReference> getAsyncMethods() {
        return asyncMethods;
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
        Set<MethodReference> visited = new HashSet<>();
        Set<MethodReference> overriden = new HashSet<>();
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            findOverridenMethods(new MethodReference(cls.getParent(), methodRef.getDescriptor()), overriden, visited);
        }
        for (String iface : cls.getInterfaces()) {
            findOverridenMethods(new MethodReference(iface, methodRef.getDescriptor()), overriden, visited);
        }
        for (MethodReference overridenMethod : overriden) {
            add(overridenMethod);
        }
    }

    private void findOverridenMethods(MethodReference methodRef, Set<MethodReference> result,
            Set<MethodReference> visited) {
        if (!visited.add(methodRef)) {
            return;
        }
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        if (method != null) {
            result.add(methodRef);
        } else {
            if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
                findOverridenMethods(new MethodReference(cls.getParent(), methodRef.getDescriptor()), result, visited);
            }
            for (String iface : cls.getInterfaces()) {
                findOverridenMethods(new MethodReference(iface, methodRef.getDescriptor()), result, visited);
            }
        }
    }
}
