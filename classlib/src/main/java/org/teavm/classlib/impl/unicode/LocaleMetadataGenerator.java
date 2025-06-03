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
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public abstract class LocaleMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        var result = new ResourceMapBuilder<ResourceMapBuilder<StringResourceBuilder>>();
        CLDRReader reader = context.getService(CLDRReader.class);
        for (var entry : reader.getKnownLocales().entrySet()) {
            CLDRLocale locale = entry.getValue();
            var names = new ResourceMapBuilder<StringResourceBuilder>();
            result.values.put(entry.getKey(), names);
            for (var nameEntry : getNameMap(locale).entrySet()) {
                var name = new StringResourceBuilder();
                name.value = nameEntry.getValue();
                names.values.put(nameEntry.getKey(), name);
            }
        }
        return result;
    }

    protected abstract Map<String, String> getNameMap(CLDRLocale locale);
}
