/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.jps.model;

import com.intellij.util.xmlb.XmlSerializer;
import java.util.Arrays;
import java.util.List;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;

public class TeaVMModelSerializerService extends JpsModelSerializerExtension {
    @NotNull
    @Override
    public List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
        return Arrays.asList(serializer);
    }

    JpsFacetConfigurationSerializer<TeaVMJpsConfiguration> serializer =
            new JpsFacetConfigurationSerializer<TeaVMJpsConfiguration>(TeaVMJpsConfiguration.ROLE,
                    "teavm-js", "TeaVM (JS)") {
        @Override
        protected TeaVMJpsConfiguration loadExtension(@NotNull Element element, String s, JpsElement jpsElement,
                JpsModule jpsModule) {
            return XmlSerializer.deserialize(element, TeaVMJpsConfiguration.class);
        }

        @Override
        protected void saveExtension(TeaVMJpsConfiguration configuration, Element element, JpsModule jpsModule) {
        }
    };
}
