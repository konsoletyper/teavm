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
package org.teavm.classlib.java.lang.reflect;

import java.util.ArrayList;
import java.util.List;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;

/**
 *
 * @author Alexey Andreev
 */
public class AnnotationDependencyListener implements DependencyListener {
    @Override
    public void started(final DependencyAgent agent) {
    }

    @Override
    public void classAchieved(DependencyAgent agent, String className, CallLocation location) {
        ClassReader cls = agent.getClassSource().get(className);
        if (cls == null) {
            return;
        }

        if (cls.hasModifier(ElementModifier.ANNOTATION)) {
            getAnnotationImplementor(agent, className);
        }
        for (AnnotationReader annotation : cls.getAnnotations().all()) {
            agent.linkClass(annotation.getType(), location);
        }
    }

    private String getAnnotationImplementor(DependencyAgent agent, String annotationType) {
        String implementorName = annotationType + "$$_impl";
        if (agent.getClassSource().get(implementorName) == null) {
            ClassHolder implementor = createImplementor(agent.getClassSource(), annotationType, implementorName);
            agent.submitClass(implementor);
        }
        return implementorName;
    }

    private ClassHolder createImplementor(ClassReaderSource classSource, String annotationType,
            String implementorName) {
        ClassHolder implementor = new ClassHolder(implementorName);
        implementor.setParent("java.lang.Object");
        implementor.getInterfaces().add(annotationType);
        implementor.getModifiers().add(ElementModifier.FINAL);
        implementor.setLevel(AccessLevel.PUBLIC);

        ClassReader annotation = classSource.get(annotationType);
        if (annotation == null) {
            return implementor;
        }

        List<ValueType> ctorSignature = new ArrayList<>();
        for (MethodReader methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            FieldHolder field = new FieldHolder("$" + methodDecl.getName());
            field.setType(methodDecl.getResultType());
            field.setLevel(AccessLevel.PRIVATE);
            implementor.addField(field);

            MethodHolder accessor = new MethodHolder(methodDecl.getDescriptor());
            ProgramEmitter pe = ProgramEmitter.create(accessor);
            ValueEmitter thisVal = pe.newVar();
            ValueEmitter result = thisVal.getField(field.getReference(), field.getType());
            if (field.getType() instanceof ValueType.Array) {
                result = result.cloneArray();
            }
            result.returnValue();
            implementor.addMethod(accessor);

            ctorSignature.add(field.getType());
        }
        ctorSignature.add(ValueType.VOID);

        MethodHolder ctor = new MethodHolder("<init>", ctorSignature.toArray(new ValueType[ctorSignature.size()]));
        ProgramEmitter pe = ProgramEmitter.create(ctor);
        ValueEmitter thisVal = pe.newVar();
        thisVal.invokeSpecial(new MethodReference(Object.class, "<init>", void.class));
        for (MethodReader methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            ValueEmitter param = pe.newVar();
            FieldReference field = new FieldReference(implementorName, "$" + methodDecl.getName());
            thisVal.setField(field, methodDecl.getResultType(), param);
        }
        pe.exit();
        implementor.addMethod(ctor);

        MethodHolder annotTypeMethod = new MethodHolder("annotationType", ValueType.parse(Class.class));
        pe = ProgramEmitter.create(annotTypeMethod);
        pe.newVar();
        pe.constant(ValueType.object(annotationType)).returnValue();
        implementor.addMethod(annotTypeMethod);

        return implementor;
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        ValueType type = method.getMethod().getResultType();
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array)type).getItemType();
        }
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object)type).getClassName();
            ClassReader cls = agent.getClassSource().get(className);
            if (cls != null && cls.hasModifier(ElementModifier.ANNOTATION)) {
                agent.linkClass(className, location);
            }
        }
    }

    @Override
    public void fieldAchieved(DependencyAgent agent, FieldDependency field, CallLocation location) {
    }
}
