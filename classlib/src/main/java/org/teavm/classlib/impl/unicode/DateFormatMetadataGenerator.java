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

import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.*;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

public class DateFormatMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
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

    private ResourceBuilder getDateFormatMap(MetadataGeneratorContext context, FormatExtractor extractor) {
        CLDRReader reader = context.getService(CLDRReader.class);
        var result = new ResourceMapBuilder<DateFormatCollectionBuilder>();
        for (var entry : reader.getKnownLocales().entrySet()) {
            var formatRes = new DateFormatCollectionBuilder();
            var formats = extractor.extract(entry.getValue());
            formatRes.shortFormat = formats.getShortFormat();
            formatRes.mediumFormat = formats.getMediumFormat();
            formatRes.longFormat = formats.getLongFormat();
            formatRes.fullFormat = formats.getFullFormat();
            result.values.put(entry.getKey(), formatRes);
        }
        return result;
    }

    interface FormatExtractor {
        CLDRDateFormats extract(CLDRLocale locale);
    }
}
