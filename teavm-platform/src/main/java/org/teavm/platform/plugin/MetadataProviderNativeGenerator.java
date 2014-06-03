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

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.*;
import org.teavm.platform.metadata.MetadataProvider;

/**
 *
 * @author Alexey Andreev
 */
class MetadataProviderNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());
        if (providerAnnot == null) {
            return;
        }
        if (!method.hasModifier(ElementModifier.NATIVE)) {
            throw new IllegalStateException("Method " + method.getReference() + " was marked with " +
                    MetadataProvider.class.getName() + " but it is not native");
        }
    }
}
