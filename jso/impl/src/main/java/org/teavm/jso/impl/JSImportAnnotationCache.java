/*
 *  Copyright 2024 Alexey Andreev.
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

import java.util.Arrays;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSIndexer;
import org.teavm.jso.JSMethod;
import org.teavm.jso.JSProperty;
import org.teavm.model.AnnotationReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;

class JSImportAnnotationCache extends JSAnnotationCache<JSImportDescriptor> {
    JSImportAnnotationCache(ClassReaderSource classes, Diagnostics diagnostics) {
        super(classes, diagnostics);
    }

    @Override
    protected JSImportDescriptor take(MethodReader method, CallLocation location) {
        var propertyAnnot = method.getAnnotations().get(JSProperty.class.getName());
        var indexerAnnot = method.getAnnotations().get(JSIndexer.class.getName());
        var methodAnnot = method.getAnnotations().get(JSMethod.class.getName());
        var found = false;
        for (var annot : Arrays.asList(propertyAnnot, indexerAnnot, methodAnnot)) {
            if (annot != null) {
                if (!found) {
                    found = true;
                } else {
                    diagnostics.error(location, "@JSProperty, @JSIndexer and @JSMethod are mutually exclusive "
                            + "and can't appear simultaneously on {{m}}", method.getReference());
                    return null;
                }
            }
        }
        if (propertyAnnot != null) {
            return new JSImportDescriptor(JSImportKind.PROPERTY, extractValue(propertyAnnot));
        }
        if (indexerAnnot != null) {
            return new JSImportDescriptor(JSImportKind.INDEXER, null);
        }
        if (methodAnnot != null) {
            return new JSImportDescriptor(JSImportKind.METHOD, extractValue(methodAnnot));
        }
        return null;
    }

    private String extractValue(AnnotationReader annotation) {
        var value = annotation.getValue("value");
        return value != null ? value.getString() : null;
    }
}
