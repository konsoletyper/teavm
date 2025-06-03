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
package org.teavm.classlib.java.lang;

import org.teavm.classlib.impl.unicode.UnicodeHelper;
import org.teavm.classlib.impl.unicode.UnicodeSupport;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.MetadataGeneratorContext;
import org.teavm.platform.metadata.builders.ResourceBuilder;
import org.teavm.platform.metadata.builders.StringResourceBuilder;

public class CharacterMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "obtainDigitMapping":
                return generateObtainDigitMapping(context);
            case "obtainClasses":
                return generateObtainClasses(context);
            case "acquireTitleCaseMapping":
                return generateAcquireTitleCaseMapping(context);
            case "acquireUpperCaseMapping":
                return generateAcquireUpperCaseMapping(context);
            case "acquireLowerCaseMapping":
                return generateAcquireLowerCaseMapping(context);
            default:
                return null;
        }
    }

    private ResourceBuilder generateObtainDigitMapping(MetadataGeneratorContext context) {
        var res = new StringResourceBuilder();
        res.value = UnicodeHelper.encodeIntPairsDiff(UnicodeSupport.getDigitValues());
        return res;
    }

    private ResourceBuilder generateObtainClasses(MetadataGeneratorContext context) {
        var res = new StringResourceBuilder();
        res.value = UnicodeHelper.compressRle(UnicodeSupport.getClasses());
        return res;
    }

    private ResourceBuilder generateAcquireTitleCaseMapping(MetadataGeneratorContext context) {
        var res = new StringResourceBuilder();
        res.value = UnicodeHelper.encodeCaseMapping(UnicodeSupport.getTitleCaseMapping());
        return res;
    }

    private ResourceBuilder generateAcquireUpperCaseMapping(MetadataGeneratorContext context) {
        var res = new StringResourceBuilder();
        res.value = UnicodeHelper.encodeCaseMapping(UnicodeSupport.getUpperCaseMapping());
        return res;
    }

    private ResourceBuilder generateAcquireLowerCaseMapping(MetadataGeneratorContext context) {
        var res = new StringResourceBuilder();
        res.value = UnicodeHelper.encodeCaseMapping(UnicodeSupport.getLowerCaseMapping());
        return res;
    }
}
