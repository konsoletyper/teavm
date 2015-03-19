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
import org.teavm.dependency.*;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class JavaScriptBodyDependency implements DependencyListener {
    private DependencyNode allClassesNode;
    private Map<MethodReference, Set<MethodReference>> achievedMethods = new HashMap<>();

    @Override
    public void started(DependencyAgent agent) {
        allClassesNode = agent.createNode();
        allClassesNode.setTag("JavaScriptBody:global");
    }

    private static class OneDirectionalConnection implements DependencyConsumer {
        private DependencyNode target;
        public OneDirectionalConnection(DependencyNode target) {
            this.target = target;
        }
        @Override public void consume(DependencyType type) {
            target.propagate(type);
        }
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className, CallLocation location) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls != null && !cls.hasModifier(ElementModifier.ABSTRACT) &&
                !cls.hasModifier(ElementModifier.INTERFACE)) {
            allClassesNode.propagate(agent.getType(className));
        }
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        Set<MethodReference> methodsToAchieve = achievedMethods.get(method.getReference());
        if (methodsToAchieve != null) {
            for (MethodReference methodToAchieve : methodsToAchieve) {
                agent.linkMethod(methodToAchieve, location);
            }
            return;
        }
        achievedMethods.put(method.getReference(), new HashSet<MethodReference>());
        if (method.isMissing()) {
            return;
        }
        AnnotationReader annot = method.getMethod().getAnnotations().get(JavaScriptBody.class.getName());
        if (annot != null) {
            includeDefaultDependencies(agent, location);
            AnnotationValue javacall = annot.getValue("javacall");
            if (method.getResult() != null) {
                allClassesNode.connect(method.getResult());
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getResult().getArrayItem()));
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getResult().getArrayItem()
                        .getArrayItem()));
            }
            for (int i = 0; i < method.getParameterCount(); ++i) {
                method.getVariable(i).connect(allClassesNode);
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getVariable(i).getArrayItem()));
                allClassesNode.addConsumer(new OneDirectionalConnection(method.getVariable(i).getArrayItem()
                        .getArrayItem()));
            }
            if (javacall != null && javacall.getBoolean()) {
                String body = annot.getValue("body").getString();
                new GeneratorJsCallback(agent, method, location).parse(body);
            }
        }
    }

    private void includeDefaultDependencies(DependencyAgent agent, CallLocation location) {
        agent.linkMethod(JavaScriptConvGenerator.fromJsMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.toJsMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.intValueMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfIntMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.booleanValueMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfBooleanMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.doubleValueMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfDoubleMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.charValueMethod, location).use();
        agent.linkMethod(JavaScriptConvGenerator.valueOfCharMethod, location).use();
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency fieldDep, CallLocation location) {
    }

    private static MethodReader findMethod(ClassReaderSource classSource, String clsName, MethodDescriptor desc) {
        while (clsName != null) {
            ClassReader cls = classSource.get(clsName);
            if (cls == null) {
                return null;
            }
            for (MethodReader method : cls.getMethods()) {
                if (method.getName().equals(desc.getName()) && sameParams(method.getDescriptor(), desc)) {
                    return method;
                }
            }
            clsName = cls.getParent();
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

    private class GeneratorJsCallback extends JsCallback {
        private DependencyAgent agent;
        private MethodDependency caller;
        private CallLocation location;
        public GeneratorJsCallback(DependencyAgent agent, MethodDependency caller, CallLocation location) {
            this.agent = agent;
            this.caller = caller;
            this.location = location;
        }
        @Override protected CharSequence callMethod(String ident, String fqn, String method, String params) {
            MethodDescriptor desc = MethodDescriptor.parse(method + params + "V");
            MethodReader reader = findMethod(agent.getClassSource(), fqn, desc);
            MethodReference ref = reader != null ? reader.getReference() : new MethodReference(fqn, desc);
            MethodDependency methodDep = agent.linkMethod(ref, location);
            achievedMethods.get(caller.getReference()).add(ref);
            if (!methodDep.isMissing()) {
                if (reader.hasModifier(ElementModifier.STATIC) || reader.hasModifier(ElementModifier.FINAL)) {
                    methodDep.use();
                    for (int i = 0; i < methodDep.getParameterCount(); ++i) {
                        allClassesNode.connect(methodDep.getVariable(i));
                    }
                } else {
                    allClassesNode.addConsumer(new VirtualCallbackConsumer(agent, reader, caller));
                }
            }
            return "";
        }
    }

    private class VirtualCallbackConsumer implements DependencyConsumer {
        private DependencyAgent agent;
        private MethodReader superMethod;
        private ClassReader superClass;
        private MethodDependency caller;
        public VirtualCallbackConsumer(DependencyAgent agent, MethodReader superMethod, MethodDependency caller) {
            this.agent = agent;
            this.superMethod = superMethod;
            this.caller = caller;
            this.superClass = agent.getClassSource().get(superMethod.getOwnerName());
        }
        @Override public void consume(DependencyType type) {
            if (!isAssignableFrom(superClass, type.getName())) {
                return;
            }
            MethodReference methodRef = new MethodReference(type.getName(), superMethod.getDescriptor());
            MethodDependency method = agent.linkMethod(methodRef, new CallLocation(caller.getReference()));
            method.use();
            for (int i = 0; i < method.getParameterCount(); ++i) {
                allClassesNode.connect(method.getVariable(i));
            }
        }
        private boolean isAssignableFrom(ClassReader supertype, String subtypeName) {
            ClassReaderSource classSource = agent.getClassSource();
            if (supertype.getName().equals(subtypeName)) {
                return true;
            }
            ClassReader subtype = classSource.get(subtypeName);
            if (subtype == null) {
                return false;
            }
            if (subtype.getParent() != null && isAssignableFrom(supertype, subtype.getParent())) {
                return true;
            }
            for (String iface : subtype.getInterfaces()) {
                if (isAssignableFrom(supertype, iface)) {
                    return true;
                }
            }
            return false;
        }
    }
}
