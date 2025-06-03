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
package org.teavm.classlib.impl;

import java.util.Map;
import org.teavm.classlib.impl.unicode.CLDRReader;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.IntResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

public abstract class WeekMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        var map = new ResourceMapBuilder<IntResourceBuilder>();
        for (var entry : getWeekData(context.getService(CLDRReader.class)).entrySet()) {
            var valueRes = new IntResourceBuilder();
            valueRes.value = entry.getValue();
            map.values.put(entry.getKey(), valueRes);
        }
        return map;
    }

    protected abstract Map<String, Integer> getWeekData(CLDRReader reader);
}
