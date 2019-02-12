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

import java.util.List;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.VariableReader;

class FastInstructionAnalyzer extends AbstractInstructionAnalyzer {
    private FastDependencyAnalyzer dependencyAnalyzer;
    private CallLocation impreciseLocation;

    FastInstructionAnalyzer(FastDependencyAnalyzer dependencyAnalyzer) {
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    protected void invokeSpecial(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments) {
        if (instance != null) {
            invokeGetClass(method);
        }
        CallLocation callLocation = impreciseLocation;
        if (instance == null) {
            dependencyAnalyzer.linkClass(method.getClassName()).initClass(callLocation);
        }
        MethodDependency methodDep = dependencyAnalyzer.linkMethod(method);
        methodDep.addLocation(callLocation);
        methodDep.use(false);
    }

    @Override
    protected void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments) {
        invokeGetClass(method);
        dependencyAnalyzer.getVirtualCallConsumer(method).addLocation(impreciseLocation);
    }

    private void invokeGetClass(MethodReference method) {
        if (method.getName().equals("getClass") && method.parameterCount() == 0
                && method.getReturnType().isObject(Class.class)) {
            dependencyAnalyzer.instancesNode.connect(dependencyAnalyzer.classesNode);
        }
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        MethodDependency cloneDep = getAnalyzer().linkMethod(CLONE_METHOD);
        cloneDep.addLocation(impreciseLocation);
        cloneDep.use();
    }

    @Override
    protected DependencyNode getNode(VariableReader variable) {
        return dependencyAnalyzer.instancesNode;
    }

    @Override
    protected DependencyAnalyzer getAnalyzer() {
        return dependencyAnalyzer;
    }

    @Override
    protected CallLocation getCallLocation() {
        return impreciseLocation;
    }

    @Override
    public void setCaller(MethodReference caller) {
        super.setCaller(caller);
        impreciseLocation = new CallLocation(caller);
    }
}
