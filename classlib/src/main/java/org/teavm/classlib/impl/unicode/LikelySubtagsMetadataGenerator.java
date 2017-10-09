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

public class LikelySubtagsMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<StringResource> map = context.createResourceMap();
        for (Map.Entry<String, String> entry : reader.getLikelySubtags().entrySet()) {
            StringResource subtagRes = context.createResource(StringResource.class);
            subtagRes.setValue(entry.getValue());
            map.put(entry.getKey(), subtagRes);
        }
        return map;
    }
}
