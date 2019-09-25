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
package org.teavm.html4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.java.html.js.JavaScriptBody;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class JavaScriptBodyDependency extends AbstractDependencyListener {
    private DependencyNode allClassesNode;
    private Map<MethodReference, Set<MethodReference>> reachedMethods = new HashMap<>();
    Set<MethodReference> virtualMethods = new HashSet<>();

    @Override
    public void started(DependencyAgent agent) {
        allClassesNode = agent.createNode();
        allClassesNode.setTag("JavaScriptBody:global");
    }

    public String[] getClassesPassedToJavaScript() {
        return allClassesNode.getTypes();
    }

    static class OneDirectionalConnection implements DependencyConsumer {
        private DependencyNode target;
        OneDirectionalConnection(DependencyNode target) {
            this.target = target;
        }
        @Override public void consume(DependencyType type) {
            target.propagate(type);
        }
    }

    @Override
    public void classReached(DependencyAgent agent, String className) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls != null && !cls.hasModifier(ElementModifier.ABSTRACT)
                && !cls.hasModifier(ElementModifier.INTERFACE)) {
            allClassesNode.propagate(agent.getType(className));
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        reachedMethods.put(method.getReference(), new HashSet<>());
        if (method.isMissing()) {
            return;
        }
        AnnotationReader annot = method.getMethod().getAnnotations().get(JavaScriptBody.class.getName());
        if (annot != null) {
            includeDefaultDependencies(agent);
            AnnotationValue javacall = annot.getValue("javacall");
            if (method.getResult() != null) {
                allClassesNode.connect(method.getResult());
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getResult().getArrayItem()));
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getResult().getArrayItem()
                        .getArrayItem()));
            }
            for (int i = 0; i < method.getParameterCount(); ++i) {
                method.getVariable(i).connect(allClassesNode);
                method.getVariable(i).addConsumer(type -> {
                    if (agent.getClassHierarchy().isSuperType("java.lang.Enum", type.getName(), false)) {
                        MethodReference toStringMethod = new MethodReference(type.getName(), "toString",
                                ValueType.parse(String.class));
                        MethodDependency dependency = agent.linkMethod(toStringMethod);
                        dependency.getVariable(0).propagate(type);
                        dependency.use();
                    }
                });
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getVariable(i).getArrayItem()));
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getVariable(i).getArrayItem()
                        .getArrayItem()));
            }
            if (javacall != null && javacall.getBoolean()) {
                String body = annot.getValue("body").getString();
                new GeneratorJsCallback(agent, method).parse(body);
            }
        }
    }

    private void includeDefaultDependencies(DependencyAgent agent) {
        agent.linkMethod(JavaScriptConvGenerator.fromJsMethod).use();
        agent.linkMethod(JavaScriptConvGenerator.toJsMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.intValueMethod).propagate(0, Integer.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfIntMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.booleanValueMethod).propagate(0, Boolean.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfBooleanMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.doubleValueMethod).propagate(0, Double.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfDoubleMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.charValueMethod).propagate(0, Character.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfCharMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.byteValueMethod).propagate(0, Byte.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfByteMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.shortValueMethod).propagate(0, Short.class).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfShortMethod).use();

        agent.linkMethod(JavaScriptConvGenerator.valueOfLongMethod).use();

        allClassesNode.propagate(agent.getType("java.lang.Integer"));
        allClassesNode.propagate(agent.getType("java.lang.Float"));
        allClassesNode.propagate(agent.getType("java.lang.Double"));
        allClassesNode.propagate(agent.getType("java.lang.String"));
    }

    class GeneratorJsCallback extends JsCallback {
        private DependencyAgent agent;
        private MethodDependency caller;

        GeneratorJsCallback(DependencyAgent agent, MethodDependency caller) {
            this.agent = agent;
            this.caller = caller;
        }

        @Override
        protected void callMethod(String ident, String fqn, String method, String params) {
            MethodDescriptor desc = MethodDescriptor.parse(method + params + "V");
            MethodReference methodRef = new MethodReference(fqn, desc);

            MethodReader reader = findMethod(agent.getClassSource(), fqn, desc);
            if (reader == null) {
                agent.getDiagnostics().error(new CallLocation(caller.getReference()), "Can't resolve method {{m0}}",
                        methodRef);
            }

            methodRef = reader.getReference();
            MethodDependency methodDep = agent.linkMethod(methodRef);
            if (ident == null) {
                reachedMethods.get(caller.getReference()).add(methodRef);
                methodDep.use();
                for (int i = 0; i < methodDep.getParameterCount(); ++i) {
                    allClassesNode.connect(methodDep.getVariable(i));
                }
            } else {
                allClassesNode.addConsumer(new VirtualCallbackConsumer(agent, methodRef));
            }
        }

        @Override
        protected void append(String text) {
        }

        @Override
        protected void reportDiagnostic(String text) {
        }
    }

    class VirtualCallbackConsumer implements DependencyConsumer {
        private DependencyAgent agent;
        private MethodReference superMethod;

        VirtualCallbackConsumer(DependencyAgent agent, MethodReference superMethod) {
            this.agent = agent;
            this.superMethod = superMethod;
        }

        @Override
        public void consume(DependencyType type) {
            if (!agent.getClassHierarchy().isSuperType(superMethod.getClassName(), type.getName(), false)) {
                return;
            }
            MethodReader method = agent.getClassSource().resolveImplementation(new MethodReference(
                    type.getName(), superMethod.getDescriptor()));
            if (method == null) {
                return;
            }
            virtualMethods.add(method.getReference());
            MethodDependency methodDep = agent.linkMethod(method.getReference());
            methodDep.use();
            for (int i = 0; i < methodDep.getParameterCount(); ++i) {
                allClassesNode.connect(methodDep.getVariable(i));
                allClassesNode.connect(methodDep.getVariable(i).getArrayItem());
            }
        }
    }

    static MethodReader findMethod(ClassReaderSource classSource, String clsName, MethodDescriptor desc) {
        ClassReader cls = classSource.get(clsName);
        if (cls == null) {
            return null;
        }

        for (MethodReader method : cls.getMethods()) {
            if (method.getName().equals(desc.getName()) && sameParams(method.getDescriptor(), desc)) {
                return method;
            }
        }

        if (cls.getParent() != null) {
            MethodReader result = findMethod(classSource, cls.getParent(), desc);
            if (result != null) {
                return result;
            }
        }

        for (String iface : cls.getInterfaces()) {
            MethodReader result = findMethod(classSource, iface, desc);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static boolean sameParams(MethodDescriptor a, MethodDescriptor b) {
        if (a.parameterCount() != b.parameterCount()) {
            return false;
        }
        for (int i = 0; i < a.parameterCount(); ++i) {
            if (!a.parameterType(i).equals(b.parameterType(i))) {
                return false;
            }
        }
        return true;
    }
}
