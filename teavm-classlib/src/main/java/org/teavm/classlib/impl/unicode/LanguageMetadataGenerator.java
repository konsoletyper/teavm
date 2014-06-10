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
public class LanguageMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        ResourceMap<ResourceMap<StringResource>> languages = context.createResourceMap();
        CLDRReader reader = context.getService(CLDRReader.class);
        for (Map.Entry<String, CLDRLocale> entry : reader.getKnownLocales().entrySet()) {
            CLDRLocale locale = entry.getValue();
            ResourceMap<StringResource> languageNames = context.createResourceMap();
            languages.put(entry.getKey(), languageNames);
            for (Map.Entry<String, String> language : locale.getLanguages().entrySet()) {
                StringResource languageName = context.createResource(StringResource.class);
                languageName.setValue(language.getValue());
                languageNames.put(language.getKey(), languageName);
            }
        }
        return languages;
    }
}
