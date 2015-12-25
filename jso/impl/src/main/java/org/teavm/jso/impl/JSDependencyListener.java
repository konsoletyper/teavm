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
package org.teavm.jso.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class JSDependencyListener extends AbstractDependencyListener {
    private Map<String, ExposedClass> exposedClasses = new HashMap<>();
    private ClassReaderSource classSource;
    private DependencyAgent agent;
    private JSBodyRepository repository;
    private boolean anyAliasExists;

    public JSDependencyListener(JSBodyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void started(DependencyAgent agent) {
        this.agent = agent;
        classSource = agent.getClassSource();
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method, CallLocation location) {
        MethodReference ref = method.getReference();
        Set<MethodReference> callbackMethods = repository.callbackMethods.get(ref);
        if (callbackMethods != null) {
            for (MethodReference callbackMethod : callbackMethods) {
                agent.linkMethod(callbackMethod, new CallLocation(ref)).use();
            }
        }
    }

    @Override
    public void classReached(DependencyAgent agent, String className, CallLocation location) {
        getExposedClass(className);
    }

    boolean isAnyAliasExists() {
        return anyAliasExists;
    }

    Map<String, ExposedClass> getExposedClasses() {
        return exposedClasses;
    }

    static class ExposedClass {
        Map<MethodDescriptor, String> inheritedMethods = new HashMap<>();
        Map<MethodDescriptor, String> methods = new HashMap<>();
        Set<String> implementedInterfaces = new HashSet<>();
        FieldReference functorField;
        MethodDescriptor functorMethod;
    }

    private ExposedClass getExposedClass(String name) {
        ExposedClass cls = exposedClasses.get(name);
        if (cls == null) {
            cls = createExposedClass(name);
            exposedClasses.put(name, cls);
        }
        return cls;
    }

    private ExposedClass createExposedClass(String name) {
        ClassReader cls = classSource.get(name);
        ExposedClass exposedCls = new ExposedClass();
        if (cls == null || cls.hasModifier(ElementModifier.INTERFACE)) {
            return exposedCls;
        }
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            ExposedClass parent = getExposedClass(cls.getParent());
            exposedCls.inheritedMethods.putAll(parent.inheritedMethods);
            exposedCls.inheritedMethods.putAll(parent.methods);
            exposedCls.implementedInterfaces.addAll(parent.implementedInterfaces);
        }
        addInterfaces(exposedCls, cls);
        for (MethodReader method : cls.getMethods()) {
            if (method.getName().equals("<init>")) {
                continue;
            }
            if (exposedCls.inheritedMethods.containsKey(method.getDescriptor())
                    || exposedCls.methods.containsKey(method.getDescriptor())) {
                MethodDependency methodDep = agent.linkMethod(method.getReference(), null);
                methodDep.getVariable(0).propagate(agent.getType(name));
                methodDep.use();
            }
        }
        if (exposedCls.functorField == null) {
            FieldReader functorField = cls.getField("$$jso_functor$$");
            if (functorField != null) {
                exposedCls.functorField = functorField.getReference();
                AnnotationReader annot = cls.getAnnotations().get(FunctorImpl.class.getName());
                exposedCls.functorMethod = MethodDescriptor.parse(annot.getValue("value").getString());
            }
        }
        return exposedCls;
    }

    private boolean addInterfaces(ExposedClass exposedCls, ClassReader cls) {
        boolean added = false;
        for (String ifaceName : cls.getInterfaces()) {
            if (exposedCls.implementedInterfaces.contains(ifaceName)) {
                continue;
            }
            ClassReader iface = classSource.get(ifaceName);
            if (iface == null) {
                continue;
            }
            if (addInterface(exposedCls, iface)) {
                added = true;
                for (MethodReader method : iface.getMethods()) {
                    if (method.hasModifier(ElementModifier.STATIC)
                            || (method.getProgram() != null && method.getProgram().basicBlockCount() > 0)) {
                        continue;
                    }
                    if (!exposedCls.inheritedMethods.containsKey(method.getDescriptor())) {
                        String name = method.getName();
                        AnnotationReader methodAnnot = method.getAnnotations().get(JSMethod.class.getName());
                        if (methodAnnot != null) {
                            AnnotationValue nameVal = methodAnnot.getValue("value");
                            if (nameVal != null) {
                                String nameStr = nameVal.getString();
                                if (!nameStr.isEmpty()) {
                                    name = nameStr;
                                }
                            }
                        }
                        exposedCls.methods.put(method.getDescriptor(), name);
                        anyAliasExists = true;
                    }
                }
            }
        }
        return added;
    }

    private boolean addInterface(ExposedClass exposedCls, ClassReader cls) {
        if (cls.getName().equals(JSObject.class.getName())) {
            return true;
        }
        return addInterfaces(exposedCls, cls);
    }
}
