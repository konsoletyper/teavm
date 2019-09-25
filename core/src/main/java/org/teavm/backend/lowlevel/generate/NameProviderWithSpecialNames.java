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
package org.teavm.backend.lowlevel.generate;

import java.util.HashMap;
import java.util.Map;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class NameProviderWithSpecialNames implements NameProvider {
    private NameProvider underlyingProvider;
    private ClassReaderSource classSource;
    private Map<MethodReference, String> cache = new HashMap<>();

    public NameProviderWithSpecialNames(NameProvider underlyingProvider, ClassReaderSource classSource) {
        this.underlyingProvider = underlyingProvider;
        this.classSource = classSource;
    }

    @Override
    public String forMethod(MethodReference method) {
        return cache.computeIfAbsent(method, r -> {
            String special = getSpecialName(r);
            return special != null ? special : underlyingProvider.forMethod(method);
        });
    }

    private String getSpecialName(MethodReference methodReference) {
        MethodReader method = classSource.resolve(methodReference);
        if (method == null) {
            return null;
        }

        AnnotationReader exportAnnot = method.getAnnotations().get(Export.class.getName());
        if (exportAnnot != null) {
            return exportAnnot.getValue("name").getString();
        }

        AnnotationReader importAnnot = method.getAnnotations().get(Import.class.getName());
        if (importAnnot != null) {
            return importAnnot.getValue("name").getString();
        }

        return null;
    }

    @Override
    public String forVirtualMethod(MethodDescriptor method) {
        return underlyingProvider.forVirtualMethod(method);
    }

    @Override
    public String forStaticField(FieldReference field) {
        return underlyingProvider.forStaticField(field);
    }

    @Override
    public String forMemberField(FieldReference field) {
        return underlyingProvider.forMemberField(field);
    }

    @Override
    public String forClass(String className) {
        return underlyingProvider.forClass(className);
    }

    @Override
    public String forClassInitializer(String className) {
        return underlyingProvider.forClassInitializer(className);
    }

    @Override
    public String forClassSystemInitializer(ValueType type) {
        return underlyingProvider.forClassSystemInitializer(type);
    }

    @Override
    public String forClassClass(String className) {
        return underlyingProvider.forClassClass(className);
    }

    @Override
    public String forClassInstance(ValueType type) {
        return underlyingProvider.forClassInstance(type);
    }

    @Override
    public String forSupertypeFunction(ValueType type) {
        return underlyingProvider.forSupertypeFunction(type);
    }
}
