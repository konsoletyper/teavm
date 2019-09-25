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
package org.teavm.platform.plugin;

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.interop.NoGcRoot;
import org.teavm.interop.NoSideEffects;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.util.ModelUtils;

class MetadataProviderTransformer implements ClassHolderTransformer {
    private Set<MethodReference> metadataMethods = new HashSet<>();

    void addMetadataMethod(MethodReference method) {
        metadataMethods.add(method);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        int index = 0;
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (metadataMethods.contains(method.getReference())) {
                transformMetadataMethod(cls, method, context.getHierarchy(), index++);
            }
        }
    }

    private void transformMetadataMethod(ClassHolder cls, MethodHolder method, ClassHierarchy hierarchy, int suffix) {
        FieldHolder field = new FieldHolder("$$metadata$$" + suffix);
        field.setType(method.getResultType());
        field.setLevel(AccessLevel.PRIVATE);
        field.getModifiers().add(ElementModifier.STATIC);
        field.getAnnotations().add(new AnnotationHolder(NoGcRoot.class.getName()));
        cls.addField(field);

        MethodHolder createMethod = new MethodHolder(method.getName() + "$$create", method.getSignature());
        createMethod.setLevel(AccessLevel.PRIVATE);
        createMethod.getModifiers().add(ElementModifier.NATIVE);
        createMethod.getModifiers().add(ElementModifier.STATIC);
        cls.addMethod(createMethod);
        AnnotationHolder genAnnot = new AnnotationHolder(GeneratedBy.class.getName());
        genAnnot.getValues().put("value", new AnnotationValue(ValueType.object(
                MetadataProviderNativeGenerator.class.getName())));
        createMethod.getAnnotations().add(genAnnot);
        ModelUtils.copyAnnotations(method.getAnnotations(), createMethod.getAnnotations());
        if (createMethod.getAnnotations().get(NoSideEffects.class.getName()) == null) {
            createMethod.getAnnotations().add(new AnnotationHolder(NoSideEffects.class.getName()));
        }

        AnnotationHolder refAnnot = new AnnotationHolder(MetadataProviderRef.class.getName());
        refAnnot.getValues().put("value", new AnnotationValue(method.getReference().toString()));
        createMethod.getAnnotations().add(refAnnot);

        method.getModifiers().remove(ElementModifier.NATIVE);
        ProgramEmitter pe = ProgramEmitter.create(method, hierarchy);
        pe.when(pe.getField(field.getReference(), field.getType()).isNull())
                .thenDo(() -> pe.setField(field.getReference(), pe.invoke(createMethod.getReference().getClassName(),
                            createMethod.getReference().getName(), createMethod.getResultType())));
        pe.getField(field.getReference(), field.getType())
                .returnValue();
    }
}
