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
package org.teavm.jso.plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
class JSODependencyListener implements DependencyListener {
    private Map<String, ExposedClass> exposedClasses = new HashMap<>();
    private ClassReaderSource classSource;
    private DependencyAgent agent;
    private boolean anyAliasExists;

    @Override
    public void started(DependencyAgent agent) {
        this.agent = agent;
        classSource = agent.getClassSource();
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className, CallLocation location) {
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
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            ExposedClass parent = getExposedClass(cls.getParent());
            exposedCls.inheritedMethods.putAll(parent.inheritedMethods);
            exposedCls.inheritedMethods.putAll(parent.methods);
            exposedCls.implementedInterfaces.addAll(parent.implementedInterfaces);
        }
        addInterfaces(exposedCls, cls);
        for (MethodReader method : cls.getMethods()) {
            if (exposedCls.inheritedMethods.containsKey(method.getDescriptor()) ||
                    exposedCls.methods.containsKey(method.getDescriptor())) {
                MethodDependency methodDep = agent.linkMethod(method.getReference(), null);
                methodDep.getVariable(0).propagate(agent.getType(name));
                methodDep.use();
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

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency methodDep, CallLocation location) {
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field, CallLocation location) {
    }
}
