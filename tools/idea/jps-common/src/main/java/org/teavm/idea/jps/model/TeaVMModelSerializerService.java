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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer;
import org.teavm.tooling.TeaVMTargetType;

public class TeaVMModelSerializerService extends JpsModelSerializerExtension {
    @NotNull
    @Override
    public List<? extends JpsFacetConfigurationSerializer<?>> getFacetConfigurationSerializers() {
        return Arrays.asList(jsSerializer, wasmSerializer);
    }

    private TeaVMFacetSerializer jsSerializer = new TeaVMFacetSerializer(TeaVMJpsConfiguration.JS_ROLE,
            "teavm-js", "TeaVM (JS)", TeaVMTargetType.JAVASCRIPT);

    private TeaVMFacetSerializer wasmSerializer = new TeaVMFacetSerializer(TeaVMJpsConfiguration.WEBASSEMBLY_ROLE,
            "teavm-wasm", "TeaVM (WebAssembly)", TeaVMTargetType.WEBASSEMBLY);

    private class TeaVMFacetSerializer
            extends JpsFacetConfigurationSerializer<TeaVMJpsConfiguration> {
        private TeaVMTargetType targetType;

        public TeaVMFacetSerializer(JpsElementChildRole<TeaVMJpsConfiguration> role, String facetTypeId,
                @Nullable String facetName, TeaVMTargetType targetType) {
            super(role, facetTypeId, facetName);
            this.targetType = targetType;
        }

        @Override
        protected TeaVMJpsConfiguration loadExtension(@NotNull Element element, String s, JpsElement jpsElement,
                JpsModule jpsModule) {
            TeaVMJpsConfiguration configuration = XmlSerializer.deserialize(element, TeaVMJpsConfiguration.class);
            configuration.setTargetType(targetType);
            return configuration;
        }

        @Override
        protected void saveExtension(TeaVMJpsConfiguration configuration, Element element, JpsModule jpsModule) {
        }
    }
}
