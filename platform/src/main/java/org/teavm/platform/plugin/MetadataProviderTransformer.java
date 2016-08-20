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

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.cache.NoCache;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.util.ModelUtils;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.metadata.ClassScopedMetadataProvider;
import org.teavm.platform.metadata.MetadataProvider;

class MetadataProviderTransformer implements ClassHolderTransformer {
    static int fieldIdGen;

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());
            if (providerAnnot != null) {
                transformMetadataMethod(cls, method, diagnostics, innerSource);
            }
            providerAnnot = method.getAnnotations().get(ClassScopedMetadataProvider.class.getName());
            if (providerAnnot != null) {
                ValueType[] params = method.getParameterTypes();
                if (params.length != 1 && params[0].isObject(PlatformClass.class.getName())) {
                    diagnostics.error(new CallLocation(method.getReference()), "Method {{m0}} marked with {{c1}} "
                            + "must take exactly one parameter of type {{c2}}",
                            method.getReference(), ClassScopedMetadataProvider.class.getName(),
                            PlatformClass.class.getName());
                }

                AnnotationHolder genAnnot = new AnnotationHolder(GeneratedBy.class.getName());
                genAnnot.getValues().put("value", new AnnotationValue(ValueType.object(
                        ClassScopedMetadataProviderNativeGenerator.class.getName())));
                method.getAnnotations().add(genAnnot);

                AnnotationHolder noCacheAnnot = new AnnotationHolder(NoCache.class.getName());
                method.getAnnotations().add(noCacheAnnot);
            }
        }
    }

    private void transformMetadataMethod(ClassHolder cls, MethodHolder method, Diagnostics diagnostics,
            ClassReaderSource classSource) {
        if (!validate(method, diagnostics)) {
            return;
        }

        FieldHolder field = new FieldHolder("$$metadata$$" + fieldIdGen++);
        field.setType(method.getResultType());
        field.setLevel(AccessLevel.PRIVATE);
        field.getModifiers().add(ElementModifier.STATIC);
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

        AnnotationHolder refAnnot = new AnnotationHolder(MetadataProviderRef.class.getName());
        refAnnot.getValues().put("value", new AnnotationValue(method.getReference().toString()));
        createMethod.getAnnotations().add(refAnnot);

        method.getModifiers().remove(ElementModifier.NATIVE);
        ProgramEmitter pe = ProgramEmitter.create(method, classSource);
        pe.when(pe.getField(field.getReference(), field.getType()).isNull())
                .thenDo(() -> pe.setField(field.getReference(), pe.invoke(createMethod.getReference().getClassName(),
                            createMethod.getReference().getName(), createMethod.getResultType())));
        pe.getField(field.getReference(), field.getType())
                .returnValue();

        AnnotationHolder noCacheAnnot = new AnnotationHolder(NoCache.class.getName());
        method.getAnnotations().add(noCacheAnnot);
    }

    private boolean validate(MethodHolder method, Diagnostics diagnostics) {
        AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());
        if (providerAnnot == null) {
            return false;
        }
        if (!method.hasModifier(ElementModifier.NATIVE)) {
            diagnostics.error(new CallLocation(method.getReference()), "Method {{m0}} is marked with "
                    + "{{c1}} annotation, but it is not native", method.getReference(),
                    MetadataProvider.class.getName());
            return false;
        }
        return true;
    }
}
