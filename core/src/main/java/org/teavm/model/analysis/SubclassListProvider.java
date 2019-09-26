/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.model.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

class SubclassListProvider {
    private Map<String, ClassInfo> classes = new HashMap<>();
    private Map<MethodDescriptor, List<MethodReference>> methodImplementations = new HashMap<>();
    private int limit;

    SubclassListProvider(ClassReaderSource classSource, Iterable<? extends String> classNames, int limit) {
        this.limit = limit;

        for (String className : classNames) {
            registerClass(classSource, className);
        }
    }

    private ClassInfo registerClass(ClassReaderSource classSource, String className) {
        ClassInfo classInfo = classes.get(className);
        if (classInfo == null) {
            classInfo = new ClassInfo();
            classes.put(className, classInfo);

            ClassReader cls = classSource.get(className);
            if (cls != null) {
                if (!cls.hasModifier(ElementModifier.INTERFACE) && !cls.hasModifier(ElementModifier.ABSTRACT)) {
                    classInfo.concrete = true;
                }
                increaseClassCount(classSource, className, new HashSet<>(), classInfo.concrete);

                if (cls.getParent() != null) {
                    ClassInfo parentInfo = registerClass(classSource, cls.getParent());
                    if (parentInfo.directSubclasses == null) {
                        parentInfo.directSubclasses = new ArrayList<>();
                    }
                    parentInfo.directSubclasses.add(className);
                }

                for (String itf : cls.getInterfaces()) {
                    ClassInfo parentInfo = registerClass(classSource, itf);
                    if (parentInfo.directSubclasses == null) {
                        parentInfo.directSubclasses = new ArrayList<>();
                    }
                    parentInfo.directSubclasses.add(className);
                }

                for (MethodReader method : cls.getMethods()) {
                    if (method.hasModifier(ElementModifier.STATIC)
                            || method.hasModifier(ElementModifier.ABSTRACT)
                            || method.getLevel() == AccessLevel.PRIVATE) {
                        continue;
                    }
                    List<MethodReference> implementations = methodImplementations.get(method.getDescriptor());
                    if (implementations == null) {
                        implementations = new ArrayList<>();
                        methodImplementations.put(method.getDescriptor(), implementations);
                    }
                    implementations.add(method.getReference());
                }
            }
        }

        return classInfo;
    }

    private void increaseClassCount(ClassReaderSource classSource, String className, Set<String> visited,
            boolean concrete) {
        if (!visited.add(className)) {
            return;
        }

        ClassInfo classInfo = registerClass(classSource, className);
        if ((!concrete || classInfo.concreteCount > limit) && classInfo.count > limit) {
            return;
        }
        classInfo.count++;
        if (concrete) {
            classInfo.concreteCount++;
        }

        ClassReader cls = classSource.get(className);
        if (cls != null) {
            if (cls.getParent() != null) {
                increaseClassCount(classSource, cls.getParent(), visited, concrete);
            }
            for (String itf : cls.getInterfaces()) {
                increaseClassCount(classSource, itf, visited, concrete);
            }
        }
    }

    List<? extends String> getSubclasses(String className, boolean includeAbstract) {
        ClassInfo classInfo = classes.get(className);
        if (classInfo == null) {
            return null;
        }

        if (includeAbstract) {
            if (classInfo.count > limit) {
                return null;
            }
        } else {
            if (classInfo.concreteCount > limit) {
                return null;
            }
        }


        String[] result = new String[includeAbstract ? classInfo.count : classInfo.concreteCount];
        collectSubclasses(className, result, 0, new HashSet<>(), includeAbstract);
        return Arrays.asList(result);
    }

    List<? extends MethodReference> getMethods(MethodDescriptor descriptor) {
        return methodImplementations.get(descriptor);
    }

    private int collectSubclasses(String className, String[] consumer, int index, Set<String> visited,
            boolean includeAbstract) {
        if (!visited.add(className)) {
            return index;
        }

        ClassInfo classInfo = classes.get(className);
        if (classInfo == null) {
            return index;
        }

        if (includeAbstract || classInfo.concrete) {
            consumer[index++] = className;
        }
        if (classInfo.directSubclasses != null) {
            for (String subclassName : classInfo.directSubclasses) {
                index = collectSubclasses(subclassName, consumer, index, visited, includeAbstract);
            }
        }

        return index;
    }

    static class ClassInfo {
        int count;
        int concreteCount;
        boolean concrete;
        List<String> directSubclasses;
    }
}
