/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.reflection;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.runtime.reflect.AnnotationInfo;

public class AnnotationGenerationHelper {
    public static final String ANNOTATION_IMPLEMENTOR_SUFFIX = "$$_impl";
    private Set<String> annotationImplementors = new HashSet<>();

    public final String getAnnotationImplementor(DependencyAgent agent, String annotationType) {
        String implementorName = annotationType + ANNOTATION_IMPLEMENTOR_SUFFIX;
        if (agent.getClassSource().get(implementorName) == null) {
            var implementor = createImplementor(agent.getClassHierarchy(), annotationType, implementorName);
            agent.submitClass(implementor);
        }
        return implementorName;
    }

    public Set<? extends String> getImplementors() {
        return annotationImplementors;
    }

    private ClassHolder createImplementor(ClassHierarchy hierarchy, String annotationType, String implementorName) {
        annotationImplementors.add(implementorName);
        ClassHolder implementor = new ClassHolder(implementorName);
        implementor.setParent("java.lang.Object");
        implementor.getInterfaces().add(annotationType);
        implementor.getModifiers().add(ElementModifier.FINAL);
        implementor.setLevel(AccessLevel.PUBLIC);

        ClassReader annotation = hierarchy.getClassSource().get(annotationType);
        if (annotation == null) {
            return implementor;
        }

        var infoField = new FieldHolder("info");
        infoField.setType(ValueType.parse(AnnotationInfo.class));
        infoField.setLevel(AccessLevel.PRIVATE);
        implementor.addField(infoField);

        var ctor = new MethodHolder("<init>", infoField.getType(), ValueType.VOID);
        ctor.setLevel(AccessLevel.PUBLIC);
        var pe = ProgramEmitter.create(ctor, hierarchy);
        var thisVar = pe.var(0, implementor);
        var infoVar = pe.var(1, infoField.getType());
        thisVar.setField(infoField.getName(), infoVar);
        pe.exit();
        implementor.addMethod(ctor);

        for (var methodDecl : annotation.getMethods()) {
            if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            var type = methodDecl.getResultType();

            var implementorMethod = new MethodHolder(methodDecl.getName(), infoField.getType(), type);
            implementorMethod.setLevel(AccessLevel.PRIVATE);
            implementorMethod.getModifiers().add(ElementModifier.STATIC);
            implementorMethod.getModifiers().add(ElementModifier.NATIVE);

            var accessor = new MethodHolder(methodDecl.getDescriptor());
            pe = ProgramEmitter.create(accessor, hierarchy);
            var thisVal = pe.var(0, implementor);
            var info = thisVal.getField(infoField.getName(), infoField.getType());
            pe.invoke(implementorMethod.getReference(), info).returnValue();
            implementor.addMethod(accessor);
        }

        var annotTypeMethod = new MethodHolder("annotationType", ValueType.parse(Class.class));
        pe = ProgramEmitter.create(annotTypeMethod, hierarchy);
        pe.constant(ValueType.object(annotationType)).returnValue();
        implementor.addMethod(annotTypeMethod);

        return implementor;
    }


    public void propagateAnnotationImplementations(DependencyAgent agent,
            Iterable<? extends AnnotationReader> inputAnnotations, DependencyNode outputNode) {
        for (var annotation : inputAnnotations) {
            agent.linkClass(annotation.getType());
        }

        var annotations = new ArrayList<AnnotationReader>();
        for (var annot : inputAnnotations) {
            var annotType = agent.getClassSource().get(annot.getType());
            if (annotType == null) {
                continue;
            }

            var retention = annotType.getAnnotations().get(Retention.class.getName());
            if (retention != null) {
                String retentionPolicy = retention.getValue("value").getEnumValue().getFieldName();
                if (retentionPolicy.equals("RUNTIME")) {
                    annotations.add(annot);
                }
            }
        }

        for (var annotation : annotations) {
            propagateAnnotationInstance(agent, annotation, outputNode);
        }
    }

    private void propagateAnnotationInstance(DependencyAgent agent, AnnotationReader annotation,
            DependencyNode outputNode) {
        ClassReader annotationClass = agent.getClassSource().get(annotation.getType());
        if (annotationClass == null) {
            return;
        }

        var implementor = getAnnotationImplementor(agent, annotation.getType());
        if (implementor != null) {
            agent.linkClass(implementor).initClass(null);
            outputNode.propagate(agent.getType(ValueType.object(implementor)));
            for (var methodDecl : annotationClass.getMethods()) {
                if (methodDecl.hasModifier(ElementModifier.STATIC)) {
                    continue;
                }
                var value = annotation.getValue(methodDecl.getName());
                if (value == null) {
                    value = methodDecl.getAnnotationDefault();
                }
                var field = agent.linkField(new FieldReference(implementor, "$" + methodDecl.getName()));
                propagateAnnotationValue(agent, value, methodDecl.getResultType(), field.getValue());
            }
            agent.linkMethod(new MethodReference(implementor, "<init>", ValueType.parse(AnnotationInfo.class),
                    ValueType.VOID)).use();
        }
    }

    private void propagateAnnotationValue(DependencyAgent agent, AnnotationValue value, ValueType type,
            DependencyNode outputNode) {
        switch (value.getType()) {
            case AnnotationValue.LIST: {
                outputNode.propagate(agent.getType(type));
                var itemType = ((ValueType.Array) type).getItemType();
                for (var annotationValue : value.getList()) {
                    propagateAnnotationValue(agent, annotationValue, itemType, outputNode.getArrayItem());
                }
                break;
            }
            case AnnotationValue.ENUM:
                break;
            case AnnotationValue.CLASS: {
                var cls = value.getJavaClass();
                while (cls instanceof ValueType.Array) {
                    cls = ((ValueType.Array) cls).getItemType();
                }
                if (cls instanceof ValueType.Object) {
                    var className = ((ValueType.Object) cls).getClassName();
                    agent.linkClass(className).initClass(null);
                }
                outputNode.getClassValueNode().propagate(agent.getType(value.getJavaClass()));
                outputNode.propagate(agent.getType(ValueType.object("java.lang.Class")));
                break;
            }
            case AnnotationValue.ANNOTATION:
                propagateAnnotationInstance(agent, value.getAnnotation(), outputNode);
                break;
        }
    }
}
