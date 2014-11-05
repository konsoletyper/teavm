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

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.model.*;
import org.teavm.platform.metadata.MetadataProvider;

/**
 *
 * @author Alexey Andreev
 */
class MetadataProviderTransformer implements ClassHolderTransformer {
    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        for (MethodHolder method : cls.getMethods()) {
            AnnotationReader providerAnnot = method.getAnnotations().get(MetadataProvider.class.getName());
            if (providerAnnot == null) {
                continue;
            }
            AnnotationHolder genAnnot = new AnnotationHolder(GeneratedBy.class.getName());
            genAnnot.getValues().put("value", new AnnotationValue(ValueType.object(
                    MetadataProviderNativeGenerator.class.getName())));
            method.getAnnotations().add(genAnnot);
        }
    }
}
