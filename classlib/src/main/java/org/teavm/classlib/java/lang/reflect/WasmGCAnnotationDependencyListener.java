/*
 *  Copyright 2025 Alexey Andreev.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCAnnotationDependencyListener extends BaseAnnotationDependencyListener {
    public WasmGCAnnotationDependencyListener() {
        super(true);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        super.methodReached(agent, method);

        MethodReference methodRef = method.getMethod().getReference();
        if (methodRef.getClassName().equals("java.lang.Class")
                && methodRef.getName().equals("getDeclaredAnnotationsImpl")) {
            method.getResult().propagate(agent.getType("[" + Annotation.class.getName()));
            reachGetAnnotations(agent, method.getVariable(0), method.getResult().getArrayItem());
        }
    }

    private void reachGetAnnotations(DependencyAgent agent, DependencyNode inputNode, DependencyNode outputNode) {
        inputNode.getClassValueNode().addConsumer(type -> {
            var className = type.getName();
            var cls = agent.getClassSource().get(className);
            if (cls == null) {
                return;
            }

            for (var annotation : cls.getAnnotations().all()) {
                agent.linkClass(annotation.getType());
            }

            propagateAnnotationImplementations(agent, cls, outputNode);
        });
    }

    private void propagateAnnotationImplementations(DependencyAgent agent, ClassReader cls,
            DependencyNode outputNode) {
        var annotations = new ArrayList<AnnotationReader>();
        for (var annot : cls.getAnnotations().all()) {
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
            outputNode.propagate(agent.getType(implementor));
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
        }
    }

    private void propagateAnnotationValue(DependencyAgent agent, AnnotationValue value, ValueType type,
            DependencyNode outputNode) {
        switch (value.getType()) {
            case AnnotationValue.LIST: {
                outputNode.propagate(agent.getType(type.toString()));
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
                if (value.getJavaClass() instanceof ValueType.Object) {
                    var className = ((ValueType.Object) value.getJavaClass()).getClassName();
                    outputNode.getClassValueNode().propagate(agent.getType(className));
                } else {
                    outputNode.getClassValueNode().propagate(agent.getType(value.getJavaClass().toString()));
                }
                outputNode.propagate(agent.getType("java.lang.Class"));
                break;
            }
            case AnnotationValue.ANNOTATION:
                propagateAnnotationInstance(agent, value.getAnnotation(), outputNode);
                break;
        }
    }
}
