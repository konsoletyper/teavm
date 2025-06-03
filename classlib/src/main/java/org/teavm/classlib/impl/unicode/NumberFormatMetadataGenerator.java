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
package org.teavm.classlib.impl.unicode;

import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class NumberFormatMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader reader = context.getService(CLDRReader.class);
        var result = new ResourceMapBuilder<StringResourceBuilder>();
        FormatAccessor accessor;
        switch (method.getName()) {
            case "getNumberFormatMap":
                accessor = locale -> locale.numberFormat;
                break;
            case "getCurrencyFormatMap":
                accessor = locale -> locale.currencyFormat;
                break;
            case "getPercentFormatMap":
                accessor = locale -> locale.percentFormat;
                break;
            default:
                throw new AssertionError();
        }
        for (var entry : reader.getKnownLocales().entrySet()) {
            var format = new StringResourceBuilder();
            format.value = accessor.getFormat(entry.getValue());
            result.values.put(entry.getKey(), format);
        }
        return result;
    }

    interface FormatAccessor {
        String getFormat(CLDRLocale locale);
    }
}
