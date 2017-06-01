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

public class DateFormatMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getDateFormatMap":
                return getDateFormatMap(context, locale -> locale.getDateFormats());
            case "getTimeFormatMap":
                return getDateFormatMap(context, locale -> locale.getTimeFormats());
            case "getDateTimeFormatMap":
                return getDateFormatMap(context, locale -> locale.getDateTimeFormats());
            default:
                throw new IllegalArgumentException("Method is not supported: " + method);
        }
    }

    private Resource getDateFormatMap(MetadataGeneratorContext context, FormatExtractor extractor) {
        CLDRReader reader = context.getService(CLDRReader.class);
        ResourceMap<DateFormatCollection> result = context.createResourceMap();
        for (Map.Entry<String, CLDRLocale> entry : reader.getKnownLocales().entrySet()) {
            DateFormatCollection formatRes = context.createResource(DateFormatCollection.class);
            CLDRDateFormats formats = extractor.extract(entry.getValue());
            formatRes.setShortFormat(formats.getShortFormat());
            formatRes.setMediumFormat(formats.getMediumFormat());
            formatRes.setLongFormat(formats.getLongFormat());
            formatRes.setFullFormat(formats.getFullFormat());
            result.put(entry.getKey(), formatRes);
        }
        return result;
    }

    interface FormatExtractor {
        CLDRDateFormats extract(CLDRLocale locale);
    }
}
