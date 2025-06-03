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

public class DecimalMetadataGenerator implements MetadataGenerator {
    @Override
    public ResourceBuilder generateMetadata(MetadataGeneratorContext context, MethodReference method) {
        CLDRReader reader = context.getService(CLDRReader.class);
        var map = new ResourceMapBuilder<DecimalDataBuilder>();
        for (var entry : reader.getKnownLocales().entrySet()) {
            CLDRDecimalData data = entry.getValue().getDecimalData();
            var dataRes = new DecimalDataBuilder();
            dataRes.decimalSeparator = data.getDecimalSeparator();
            dataRes.exponentSeparator = data.getExponentSeparator();
            dataRes.groupingSeparator = data.getGroupingSeparator();
            dataRes.infinity = data.getInfinity();
            dataRes.listSeparator = data.getListSeparator();
            dataRes.minusSign = data.getMinusSign();
            dataRes.nan = data.getNaN();
            dataRes.percent = data.getPercent();
            dataRes.perMille = data.getPerMille();
            map.values.put(entry.getKey(), dataRes);
        }
        return map;
    }
}
