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
package org.teavm.jso.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

class JSObjectClassTransformer implements ClassHolderTransformer {
    private JSClassProcessor processor;
    private JSBodyRepository repository;
    private JSTypeHelper typeHelper;
    private ClassReaderSource innerSource;
    private Map<String, ExposedClass> exposedClasses = new HashMap<>();

    JSObjectClassTransformer(JSBodyRepository repository) {
        this.repository = repository;
    }

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        this.innerSource = innerSource;
        if (processor == null || processor.getClassSource() != innerSource) {
            typeHelper = new JSTypeHelper(innerSource);
            processor = new JSClassProcessor(innerSource, typeHelper, repository, diagnostics);
        }
        processor.processClass(cls);
        if (typeHelper.isJavaScriptClass(cls.getName())) {
            processor.processMemberMethods(cls);
        }
        if (typeHelper.isJavaScriptImplementation(cls.getName())) {
            processor.makeSync(cls);
        }

        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.getProgram() != null) {
                processor.processProgram(method);
            }
        }
        processor.createJSMethods(cls);

        MethodReference functorMethod = processor.isFunctor(cls.getName());
        if (functorMethod != null) {
            if (processor.isFunctor(cls.getParent()) != null) {
                functorMethod = null;
            }
        }

        ClassReader originalClass = innerSource.get(cls.getName());
        ExposedClass exposedClass;
        if (originalClass != null) {
            exposedClass = getExposedClass(cls.getName());
        } else {
            exposedClass = new ExposedClass();
            createExposedClass(cls, exposedClass);
        }

        exposeMethods(cls, exposedClass, diagnostics, functorMethod);
    }

    private void exposeMethods(ClassHolder classHolder, ExposedClass classToExpose, Diagnostics diagnostics,
            MethodReference functorMethod) {
        int index = 0;
        for (MethodDescriptor method : classToExpose.methods.keySet()) {
            MethodReference methodRef = new MethodReference(classHolder.getName(), method);
            CallLocation callLocation = new CallLocation(methodRef);

            ValueType[] exportedMethodSignature = Arrays.stream(method.getSignature())
                    .map(type -> ValueType.object(JSObject.class.getName()))
                    .toArray(ValueType[]::new);
            MethodDescriptor exportedMethodDesc = new MethodDescriptor(method.getName() + "$exported$" + index++,
                    exportedMethodSignature);
            MethodHolder exportedMethod = new MethodHolder(exportedMethodDesc);
            Program program = new Program();
            exportedMethod.setProgram(program);
            program.createVariable();

            BasicBlock basicBlock = program.createBasicBlock();
            List<Instruction> marshallInstructions = new ArrayList<>();
            JSValueMarshaller marshaller = new JSValueMarshaller(diagnostics, typeHelper, innerSource, program,
                    marshallInstructions);

            List<Variable> variablesToPass = new ArrayList<>();
            for (int i = 0; i < method.parameterCount(); ++i) {
                variablesToPass.add(program.createVariable());
            }

            for (int i = 0; i < method.parameterCount(); ++i) {
                Variable var = marshaller.unwrapReturnValue(callLocation, variablesToPass.get(i),
                        method.parameterType(i));
                variablesToPass.set(i, var);
            }

            basicBlock.addAll(marshallInstructions);
            marshallInstructions.clear();

            InvokeInstruction invocation = new InvokeInstruction();
            invocation.setType(InvocationType.VIRTUAL);
            invocation.setInstance(program.variableAt(0));
            invocation.setMethod(methodRef);
            invocation.getArguments().addAll(variablesToPass);
            basicBlock.add(invocation);

            ExitInstruction exit = new ExitInstruction();
            if (method.getResultType() != ValueType.VOID) {
                invocation.setReceiver(program.createVariable());
                exit.setValueToReturn(marshaller.wrapArgument(callLocation, invocation.getReceiver(),
                        method.getResultType(), false));
                basicBlock.addAll(marshallInstructions);
                marshallInstructions.clear();
            }
            basicBlock.add(exit);

            classHolder.addMethod(exportedMethod);

            String publicAlias = classToExpose.methods.get(method);
            AnnotationHolder annot = new AnnotationHolder(JSMethodToExpose.class.getName());
            annot.getValues().put("name", new AnnotationValue(publicAlias));
            exportedMethod.getAnnotations().add(annot);

            if (methodRef.equals(functorMethod)) {
                addFunctorField(classHolder, exportedMethod.getReference());
            }
        }
    }

    private ExposedClass getExposedClass(String name) {
        return exposedClasses.computeIfAbsent(name, this::createExposedClass);
    }

    private ExposedClass createExposedClass(String name) {
        ClassReader cls = innerSource.get(name);
        ExposedClass exposedCls = new ExposedClass();
        if (cls != null) {
            createExposedClass(cls, exposedCls);
        }
        return exposedCls;
    }

    private void createExposedClass(ClassReader cls, ExposedClass exposedCls) {
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            return;
        }
        if (cls.getParent() != null) {
            ExposedClass parent = getExposedClass(cls.getParent());
            exposedCls.inheritedMethods.putAll(parent.inheritedMethods);
            exposedCls.inheritedMethods.putAll(parent.methods);
            exposedCls.implementedInterfaces.addAll(parent.implementedInterfaces);
        }
        addInterfaces(exposedCls, cls);
    }

    private boolean addInterfaces(ExposedClass exposedCls, ClassReader cls) {
        boolean added = false;
        for (String ifaceName : cls.getInterfaces()) {
            if (exposedCls.implementedInterfaces.contains(ifaceName)) {
                continue;
            }
            ClassReader iface = innerSource.get(ifaceName);
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

    private void addFunctorField(ClassHolder cls, MethodReference method) {
        if (cls.getAnnotations().get(FunctorImpl.class.getName()) != null) {
            return;
        }

        FieldHolder field = new FieldHolder("$$jso_functor$$");
        field.setLevel(AccessLevel.PUBLIC);
        field.setType(ValueType.parse(JSObject.class));
        cls.addField(field);

        AnnotationHolder annot = new AnnotationHolder(FunctorImpl.class.getName());
        annot.getValues().put("value", new AnnotationValue(method.getDescriptor().toString()));
        cls.getAnnotations().add(annot);
    }

    static class ExposedClass {
        Map<MethodDescriptor, String> inheritedMethods = new HashMap<>();
        Map<MethodDescriptor, String> methods = new HashMap<>();
        Set<String> implementedInterfaces = new HashSet<>();
    }
}
