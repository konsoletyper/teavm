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

import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;

/**
 *
 * @author Alexey Andreev
 */
class ProgrammableDependencyGraphCreator implements DependencyGraphCreator {
    int nodeCount;
    Connection[] connections;
    TypeRestrictedConnection[] typeRestrictedConnections;
    TypePropagation[] typePropagations;
    String[] initializedClasses;
    int[] variableMap;
    int resultIndex;

    ProgrammableDependencyGraphCreator() {
    }

    @Override
    public DependencyGraphCreatorProduct createDependency(DependencyChecker checker, DependencyStack stack) {
        DependencyNode[] nodes = new DependencyNode[nodeCount];
        for (int i = 0; i < nodes.length; ++i) {
            nodes[i] = checker.createNode();
        }
        for (Connection conn : connections) {
            nodes[conn.from].connect(nodes[conn.to]);
        }
        for (TypeRestrictedConnection conn : typeRestrictedConnections) {
            nodes[conn.from].connect(nodes[conn.to], new DependencySupertypeFilter(checker.getClassSource(),
                    checker.getClassSource().get(conn.superclass)));
        }
        for (TypePropagation propagation : typePropagations) {
            nodes[propagation.var].propagate(propagation.type);
        }
        for (String className : initializedClasses) {
            checker.initClass(className, stack);
        }
        return null;
    }

    static class Connection {
        int from;
        int to;
    }

    static class TypeRestrictedConnection {
        int from;
        int to;
        String superclass;
    }

    static class TypePropagation {
        int var;
        String type;
    }

    static class DependencySupertypeFilter implements DependencyTypeFilter {
        private ClassReaderSource classSource;
        private ClassReader superClass;
        public DependencySupertypeFilter(ClassReaderSource classSource, ClassReader superClass) {
            this.classSource = classSource;
            this.superClass = superClass;
        }
        @Override public boolean match(String type) {
            if (superClass.getName().equals("java.lang.Object")) {
                return true;
            }
            return isAssignableFrom(classSource, superClass, type);
        }
    }

    private static boolean isAssignableFrom(ClassReaderSource classSource, ClassReader supertype,
            String subtypeName) {
        if (supertype.getName().equals(subtypeName)) {
            return true;
        }
        ClassReader subtype = classSource.get(subtypeName);
        if (subtype == null) {
            return false;
        }
        if (subtype.getParent() != null && isAssignableFrom(classSource, supertype, subtype.getParent())) {
            return true;
        }
        for (String iface : subtype.getInterfaces()) {
            if (isAssignableFrom(classSource, supertype, iface)) {
                return true;
            }
        }
        return false;
    }
}
