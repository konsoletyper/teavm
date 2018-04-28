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
package org.teavm.classlib.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.ClassScopedMetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.Resource;

public class DeclaringClassMetadataGenerator implements ClassScopedMetadataGenerator {
    @Override
    public Map<String, Resource> generateMetadata(MetadataGeneratorContext context,
            Collection<? extends String> classNames, MethodReference method) {
        Map<String, Resource> result = new HashMap<>();
        for (String clsName : classNames) {
            ClassReader cls = context.getClassSource().get(clsName);
            if (cls.getOwnerName() != null) {
                result.put(clsName, context.createClassResource(cls.getOwnerName()));
            }
        }
        return result;
    }
}
