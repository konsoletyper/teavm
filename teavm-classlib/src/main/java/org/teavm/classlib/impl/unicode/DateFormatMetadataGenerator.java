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
public class DateFormatMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getDateFormatMap":
                return getDateFormatMap(context, new FormatExtractor() {
                    @Override public String extract(CLDRLocale locale) {
                        return locale.getMediumDateFormat();
                    }
                });
            case "getShortDateFormatMap":
                return getDateFormatMap(context, new FormatExtractor() {
                    @Override public String extract(CLDRLocale locale) {
                        return locale.getShortDateFormat();
                    }
                });
            case "getLongDateFormatMap":
                return getDateFormatMap(context, new FormatExtractor() {
                    @Override public String extract(CLDRLocale locale) {
                        return locale.getLongDateFormat();
                    }
                });
            case "getFullDateFormatMap":
                return getDateFormatMap(context, new FormatExtractor() {
                    @Override public String extract(CLDRLocale locale) {
                        return locale.getFullDateFormat();
                    }
                });
            default:
                throw new IllegalArgumentException("Method is not supported: " + method);
        }
    }

    private Resource getDateFormatMap(MetadataGeneratorContext context, FormatExtractor extractor) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<StringResource> result = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> entry : reader.getKnownLocales().entrySet()) {
            StringResource formatRes = context.createResource(StringResource.class);
            formatRes.setValue(extractor.extract(entry.getValue()));
            result.put(entry.getKey(), formatRes);
        }
        return result;
    }

    interface FormatExtractor {
        String extract(CLDRLocale locale);
    }
}
