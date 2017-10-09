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
package org.teavm.classlib.impl.unicode;

import java.util.Map;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.*;

public abstract class LocaleMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        ResourceMap<ResourceMap<StringResource>> result = context.createResourceMap();
        CLDRReader reader = context.getService(CLDRReader.class);
        for (Map.Entry<String, CLDRLocale> entry : reader.getKnownLocales().entrySet()) {
            CLDRLocale locale = entry.getValue();
            ResourceMap<StringResource> names = context.createResourceMap();
            result.put(entry.getKey(), names);
            for (Map.Entry<String, String> nameEntry : getNameMap(locale).entrySet()) {
                StringResource name = context.createResource(StringResource.class);
                name.setValue(nameEntry.getValue());
                names.put(nameEntry.getKey(), name);
            }
        }
        return result;
    }

    protected abstract Map<String, String> getNameMap(CLDRLocale locale);
}
