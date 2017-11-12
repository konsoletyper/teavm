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
    private DependencyAnalyzer analyzer;

    DependencyAgent(DependencyAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public DependencyNode createNode() {
        return analyzer.createNode();
    }

    public DependencyType getType(String name) {
        return analyzer.getType(name);
    }

    public String generateClassName() {
        return analyzer.generateClassName();
    }

    public String submitClassFile(byte[] data) {
        return analyzer.submitClassFile(data);
    }

    public void submitClass(ClassHolder cls) {
        analyzer.submitClass(cls);
    }

    public void submitMethod(MethodReference method, Program program) {
        analyzer.submitMethod(method, program);
    }

    public MethodDependency linkMethod(MethodReference methodRef, CallLocation callLocation) {
        return analyzer.linkMethod(methodRef, callLocation);
    }

    public ClassDependency linkClass(String className, CallLocation callLocation) {
        return analyzer.linkClass(className, callLocation);
    }

    public FieldDependency linkField(FieldReference fieldRef, CallLocation callLocation) {
        return analyzer.linkField(fieldRef, callLocation);
    }

    public Diagnostics getDiagnostics() {
        return analyzer.getDiagnostics();
    }

    @Override
    public <T> T getService(Class<T> type) {
        return analyzer.getService(type);
    }

    @Override
    public ClassReaderSource getClassSource() {
        return analyzer.getClassSource();
    }

    @Override
    public ClassLoader getClassLoader() {
        return analyzer.getClassLoader();
    }

    @Override
    public Collection<MethodReference> getReachableMethods() {
        return analyzer.getReachableMethods();
    }

    @Override
    public Collection<FieldReference> getReachableFields() {
        return analyzer.getReachableFields();
    }

    @Override
    public Collection<String> getReachableClasses() {
        return analyzer.getReachableClasses();
    }

    @Override
    public FieldDependencyInfo getField(FieldReference fieldRef) {
        return analyzer.getField(fieldRef);
    }

    @Override
    public MethodDependencyInfo getMethod(MethodReference methodRef) {
        return analyzer.getMethod(methodRef);
    }

    @Override
    public MethodDependencyInfo getMethodImplementation(MethodReference methodRef) {
        return analyzer.getMethodImplementation(methodRef);
    }

    @Override
    public ClassDependencyInfo getClass(String className) {
        return analyzer.getClass(className);
    }

    @Override
    public CallGraph getCallGraph() {
        return analyzer.getCallGraph();
    }
}
