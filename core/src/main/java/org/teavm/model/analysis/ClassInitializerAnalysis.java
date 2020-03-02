/*
 *  Copyright 2019 Alexey Andreev.
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

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.dependency.ValueDependencyInfo;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.StaticInit;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.optimization.Devirtualization;

public class ClassInitializerAnalysis implements ClassInitializerInfo {
    private static final MethodDescriptor CLINIT = new MethodDescriptor("<clinit>", void.class);
    private static final byte BEING_ANALYZED = 1;
    private static final byte DYNAMIC = 2;
    private static final byte STATIC = 3;
    private ObjectByteMap<String> classStatuses = new ObjectByteHashMap<>();
    private Map<MethodReference, MethodInfo> methodInfoMap = new HashMap<>();
    private ListableClassReaderSource classes;
    private ClassHierarchy hierarchy;
    private List<String> order = new ArrayList<>();
    private List<? extends String> readonlyOrder = Collections.unmodifiableList(order);
    private String currentAnalyzedClass;
    private DependencyInfo dependencyInfo;

    public ClassInitializerAnalysis(ListableClassReaderSource classes, ClassHierarchy hierarchy) {
        this.classes = classes;
        this.hierarchy = hierarchy;
    }

    public void analyze(DependencyInfo dependencyInfo) {
        if (methodInfoMap == null) {
            return;
        }
        this.dependencyInfo = dependencyInfo;

        for (String className : classes.getClassNames()) {
            analyze(className);
        }

        methodInfoMap = null;
        classes = null;
        hierarchy = null;
        this.dependencyInfo = null;
    }

    @Override
    public boolean isDynamicInitializer(String className) {
        return classStatuses.get(className) != STATIC;
    }

    @Override
    public List<? extends String> getInitializationOrder() {
        return readonlyOrder;
    }

    private void analyze(String className) {
        byte classStatus = classStatuses.get(className);
        switch (classStatus) {
            case BEING_ANALYZED:
                if (!className.equals(currentAnalyzedClass)) {
                    classStatuses.put(className, DYNAMIC);
                }
                return;
            case DYNAMIC:
            case STATIC:
                return;
        }

        ClassReader cls = classes.get(className);

        if (cls == null || cls.getAnnotations().get(StaticInit.class.getName()) != null) {
            classStatuses.put(className, STATIC);
            return;
        }

        classStatuses.put(className, BEING_ANALYZED);
        String previousClass = currentAnalyzedClass;
        currentAnalyzedClass = className;

        MethodReader initializer = cls.getMethod(CLINIT);
        boolean isStatic = true;
        if (initializer != null) {
            MethodInfo initializerInfo = analyzeMethod(initializer);
            if (isDynamicInitializer(initializerInfo, className)) {
                isStatic = false;
            }
        }

        currentAnalyzedClass = previousClass;
        if (classStatuses.get(className) == BEING_ANALYZED) {
            classStatuses.put(className, isStatic ? STATIC : DYNAMIC);
            if (isStatic && initializer != null) {
                order.add(className);
            }
        }
    }

    private boolean isDynamicInitializer(MethodInfo methodInfo, String className) {
        if (methodInfo.anyFieldModified) {
            return true;
        }
        if (methodInfo.classesWithModifiedFields != null) {
            for (String affectedClass : methodInfo.classesWithModifiedFields) {
                if (!affectedClass.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MethodInfo analyzeMethod(MethodReader method) {
        MethodInfo methodInfo = methodInfoMap.get(method.getReference());
        if (methodInfo == null) {
            methodInfo = new MethodInfo(method.getReference());
            methodInfoMap.put(method.getReference(), methodInfo);

            String currentClass = method.getDescriptor().equals(CLINIT) ? method.getOwnerName() : null;
            InstructionAnalyzer reader = new InstructionAnalyzer(currentClass, methodInfo);
            ProgramReader program = method.getProgram();
            if (program == null) {
                methodInfo.anyFieldModified = hasSideEffects(method);
            } else {
                for (BasicBlockReader block : program.getBasicBlocks()) {
                    block.readAllInstructions(reader);
                }
            }

            if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
                reader.initClass("java.lang.Thread");
            }

            methodInfo.complete = true;
        }

        return methodInfo;
    }

    private boolean hasSideEffects(MethodReader method) {
        if (method.hasModifier(ElementModifier.ABSTRACT)) {
            return false;
        }
        if (method.getAnnotations().get(NoSideEffects.class.getName()) != null) {
            return false;
        }
        ClassReader containingClass = classes.get(method.getOwnerName());
        if (containingClass.getAnnotations().get(NoSideEffects.class.getName()) != null) {
            return false;
        }

        return true;
    }

    class InstructionAnalyzer extends AbstractInstructionReader {
        String currentClass;
        MethodInfo methodInfo;
        MethodDependencyInfo methodDep;

        InstructionAnalyzer(String currentClass, MethodInfo methodInfo) {
            this.currentClass = currentClass;
            this.methodInfo = methodInfo;
            methodDep = dependencyInfo.getMethod(methodInfo.method);
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            analyzeInitializer("java.lang.String");
        }

        @Override
        public void create(VariableReader receiver, String type) {
            analyzeInitializer(type);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            if (instance == null) {
                touchField(field);
            }
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
            if (instance == null) {
                ClassReader cls = classes.get(field.getClassName());
                if (cls != null) {
                    FieldReader fieldReader = cls.getField(field.getFieldName());
                    if (fieldReader != null && fieldReader.hasModifier(ElementModifier.FINAL)) {
                        return;
                    }
                }
                touchField(field);
            }
        }

        private void touchField(FieldReference field) {
            analyzeInitializer(field.getClassName());
            if (!methodInfo.anyFieldModified && !field.getClassName().equals(currentClass)) {
                if (methodInfo.classesWithModifiedFields == null) {
                    methodInfo.classesWithModifiedFields = new HashSet<>();
                }
                methodInfo.classesWithModifiedFields.add(field.getClassName());
            }
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (type == InvocationType.VIRTUAL) {
                ValueDependencyInfo instanceDep = methodDep.getVariable(instance.getIndex());
                Set<MethodReference> implementations = Devirtualization.implementations(hierarchy, dependencyInfo,
                        instanceDep.getTypes(), method);
                for (MethodReference implementation : implementations) {
                    invokeMethod(implementation);
                }
            } else {
                analyzeInitializer(method.getClassName());
                invokeMethod(method);
            }
        }

        private void invokeMethod(MethodReference method) {
            ClassReader cls = classes.get(method.getClassName());
            if (cls != null) {
                MethodReader methodReader = cls.getMethod(method.getDescriptor());
                if (methodReader != null) {
                    analyzeCalledMethod(analyzeMethod(methodReader));
                }
            }
        }

        @Override
        public void initClass(String className) {
            analyzeInitializer(className);
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
            initClass("java.lang.Thread");
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            initClass("java.lang.Thread");
        }

        void analyzeInitializer(String className) {
            if (className.equals(currentClass)) {
                return;
            }

            analyze(className);
        }

        private void analyzeCalledMethod(MethodInfo calledMethod) {
            if (methodInfo.anyFieldModified) {
                return;
            }

            if (calledMethod.anyFieldModified) {
                methodInfo.anyFieldModified = true;
                methodInfo.classesWithModifiedFields = null;
            } else if (calledMethod.classesWithModifiedFields != null) {
                for (String className : calledMethod.classesWithModifiedFields) {
                    if (className.equals(currentClass)) {
                        if (methodInfo.classesWithModifiedFields == null) {
                            methodInfo.classesWithModifiedFields = new HashSet<>();
                        }
                        methodInfo.classesWithModifiedFields.add(className);
                    }
                }
            }
        }
    }

    static class MethodInfo {
        MethodReference method;
        boolean complete;
        Set<String> classesWithModifiedFields;
        boolean anyFieldModified;

        MethodInfo(MethodReference method) {
            this.method = method;
        }
    }
}
