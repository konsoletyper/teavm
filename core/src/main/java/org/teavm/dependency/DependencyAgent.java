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
package org.teavm.dependency;

import java.util.Collection;
import org.teavm.callgraph.CallGraph;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.*;

public class DependencyAgent implements DependencyInfo, ServiceRepository {
    private DependencyChecker checker;

    DependencyAgent(DependencyChecker checker) {
        this.checker = checker;
    }

    public DependencyNode createNode() {
        return checker.createNode();
    }

    public DependencyType getType(String name) {
        return checker.getType(name);
    }

    public String generateClassName() {
        return checker.generateClassName();
    }

    public String submitClassFile(byte[] data) {
        return checker.submitClassFile(data);
    }

    public void submitClass(ClassHolder cls) {
        checker.submitClass(cls);
    }

    public void submitMethod(MethodReference method, Program program) {
        checker.submitMethod(method, program);
    }

    public MethodDependency linkMethod(MethodReference methodRef, CallLocation callLocation) {
        return checker.linkMethod(methodRef, callLocation);
    }

    public ClassDependency linkClass(String className, CallLocation callLocation) {
        return checker.linkClass(className, callLocation);
    }

    public FieldDependency linkField(FieldReference fieldRef, CallLocation callLocation) {
        return checker.linkField(fieldRef, callLocation);
    }

    public Diagnostics getDiagnostics() {
        return checker.getDiagnostics();
    }

    @Override
    public <T> T getService(Class<T> type) {
        return checker.getService(type);
    }

    @Override
    public ClassReaderSource getClassSource() {
        return checker.getClassSource();
    }

    @Override
    public ClassLoader getClassLoader() {
        return checker.getClassLoader();
    }

    @Override
    public Collection<MethodReference> getReachableMethods() {
        return checker.getReachableMethods();
    }

    @Override
    public Collection<FieldReference> getReachableFields() {
        return checker.getReachableFields();
    }

    @Override
    public Collection<String> getReachableClasses() {
        return checker.getReachableClasses();
    }

    @Override
    public FieldDependencyInfo getField(FieldReference fieldRef) {
        return checker.getField(fieldRef);
    }

    @Override
    public MethodDependencyInfo getMethod(MethodReference methodRef) {
        return checker.getMethod(methodRef);
    }

    @Override
    public MethodDependencyInfo getMethodImplementation(MethodReference methodRef) {
        return checker.getMethodImplementation(methodRef);
    }

    @Override
    public ClassDependencyInfo getClass(String className) {
        return checker.getClass(className);
    }

    @Override
    public CallGraph getCallGraph() {
        return checker.getCallGraph();
    }
}
