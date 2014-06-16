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

/**
 *
 * @author Alexey Andreev
 */
public class DateSymbolsMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getErasMap":
                return generateEras(context);
            default:
                throw new AssertionError("Unsupported method: " + method);
        }
    }

    private Resource generateEras(MetadataGeneratorContext context) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<ResourceArray<StringResource>> result = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> localeEntry : reader.getKnownLocales().entrySet()) {
            ResourceArray<StringResource> erasRes = context.createResourceArray();
            result.put(localeEntry.getKey(), erasRes);
            for (String era : localeEntry.getValue().getEras()) {
                StringResource eraRes = context.createResource(StringResource.class);
                eraRes.setValue(era);
                erasRes.add(eraRes);
            }
        }
        return result;
    }
}
