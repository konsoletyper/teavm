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
package org.teavm.dependency;

import java.util.BitSet;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodDescriptor;

class VirtualCallConsumer implements DependencyConsumer {
    private final DependencyNode node;
    private final MethodDescriptor methodDesc;
    private final DependencyAnalyzer analyzer;
    private final DependencyNode[] parameters;
    private final DependencyNode result;
    private final CallLocation location;
    private final BitSet knownTypes = new BitSet();
    private DependencyGraphBuilder.ExceptionConsumer exceptionConsumer;
    private DependencyTypeFilter filter;
    private boolean isPolymorphic;
    private MethodDependency monomorphicCall;

    VirtualCallConsumer(DependencyNode node, String filterClass,
            MethodDescriptor methodDesc, DependencyAnalyzer analyzer, DependencyNode[] parameters,
            DependencyNode result, CallLocation location,
            DependencyGraphBuilder.ExceptionConsumer exceptionConsumer) {
        this.node = node;
        this.filter = analyzer.getSuperClassFilter(filterClass);
        this.methodDesc = methodDesc;
        this.analyzer = analyzer;
        this.parameters = parameters;
        this.result = result;
        this.location = location;
        this.exceptionConsumer = exceptionConsumer;
    }

    @Override
    public void consume(DependencyType type) {
        if (!filter.match(type)) {
            return;
        }

        if (knownTypes.get(type.index)) {
            return;
        }
        knownTypes.set(type.index);

        String className = type.getName();
        /*
        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". "
                    + "Target class is " + className);
        }

         */
        if (className.startsWith("[")) {
            className = "java.lang.Object";
        }

        MethodDependency methodDep = analyzer.linkMethod(className, methodDesc);
        methodDep.addLocation(location);
        if (!methodDep.isMissing()) {
            methodDep.use(false);
            if (isPolymorphic) {
                methodDep.external = true;
            } else if (monomorphicCall == null) {
                monomorphicCall = methodDep;
            } else {
                monomorphicCall.external = true;
                monomorphicCall = null;
                methodDep.external = true;
                isPolymorphic = true;
            }
            DependencyNode[] targetParams = methodDep.getVariables();
            if (parameters[0] != null && targetParams[0] != null) {
                parameters[0].connect(targetParams[0],
                        analyzer.getSuperClassFilter(methodDep.getMethod().getOwnerName()));
            }
            for (int i = 1; i < parameters.length; ++i) {
                if (parameters[i] != null && targetParams[i] != null) {
                    parameters[i].connect(targetParams[i]);
                }
            }
            if (result != null && methodDep.getResult() != null) {
                methodDep.getResult().connect(result);
            }
            methodDep.getThrown().addConsumer(exceptionConsumer);
        }
    }
}
