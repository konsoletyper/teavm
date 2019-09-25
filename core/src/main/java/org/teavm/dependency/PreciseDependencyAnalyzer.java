/*
 *  Copyright 2012 Alexey Andreev.
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

import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class PreciseDependencyAnalyzer extends DependencyAnalyzer {
    public PreciseDependencyAnalyzer(ClassReaderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Diagnostics diagnostics, ReferenceCache referenceCache) {
        super(classSource, classLoader, services, diagnostics, referenceCache);
    }

    @Override
    protected void processMethod(MethodDependency methodDep) {
        DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(this);
        graphBuilder.buildGraph(methodDep);
    }

    @Override
    DependencyNode createParameterNode(MethodReference method, ValueType type, int index) {
        DependencyNode node = createNode(type);
        node.method = method;
        if (shouldTag) {
            node.setTag(method + ":" + index);
        }
        return node;
    }

    @Override
    DependencyNode createResultNode(MethodReference method) {
        DependencyNode node = createNode(method.getReturnType());
        node.method = method;
        if (shouldTag) {
            node.setTag(method + ":RESULT");
        }
        return node;
    }

    @Override
    DependencyNode createThrownNode(MethodReference method) {
        DependencyNode node = createNode();
        node.method = method;
        if (shouldTag) {
            node.setTag(method + ":THROWN");
        }
        return node;
    }

    @Override
    DependencyNode createFieldNode(FieldReference field, ValueType type) {
        DependencyNode node = createNode(type);
        if (shouldTag) {
            node.setTag(field.getClassName() + "#" + field.getFieldName());
        }
        return node;
    }

    @Override
    DependencyNode createArrayItemNode(DependencyNode parent) {
        ValueType itemTypeFilter = parent.typeFilter instanceof ValueType.Array
                ? ((ValueType.Array) parent.typeFilter).getItemType()
                : null;
        DependencyNode node = createNode(itemTypeFilter);
        node.degree = parent.degree + 1;
        node.method = parent.method;
        if (DependencyAnalyzer.shouldTag) {
            node.tag = parent.tag + "[";
        }
        return node;
    }

    @Override
    DependencyNode createClassValueNode(int degree, DependencyNode parent) {
        DependencyNode node = createNode();
        node.degree = degree;
        node.classValueNode = node;
        node.classNodeParent = parent;
        if (DependencyAnalyzer.shouldTag) {
            node.tag = parent.tag + "@";
        }
        return node;
    }

    @Override
    boolean domainOptimizationEnabled() {
        return true;
    }
}