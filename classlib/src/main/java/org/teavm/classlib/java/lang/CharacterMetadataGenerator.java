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
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.StringResource;

public class CharacterMetadataGenerator implements MetadataGenerator {
    @Override
    public Resource generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "obtainDigitMapping":
                return generateObtainDigitMapping(context);
            case "obtainClasses":
                return generateObtainClasses(context);
            case "acquireTitleCaseMapping":
                return generateObtainTitleCaseMapping(context);
            default:
                return null;
        }
    }

    private Resource generateObtainDigitMapping(MetadataGeneratorContext context) {
        StringResource res = context.createResource(StringResource.class);
        res.setValue(UnicodeHelper.encodeIntPairsDiff(UnicodeSupport.getDigitValues()));
        return res;
    }

    private Resource generateObtainClasses(MetadataGeneratorContext context) {
        StringResource res = context.createResource(StringResource.class);
        res.setValue(UnicodeHelper.compressRle(UnicodeSupport.getClasses()));
        return res;
    }

    private Resource generateObtainTitleCaseMapping(MetadataGeneratorContext context) {
        StringResource res = context.createResource(StringResource.class);
        res.setValue(UnicodeHelper.encodeIntDiff(UnicodeSupport.getTitleCaseMapping()));
        return res;
    }
}
