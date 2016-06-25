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
import java.util.Objects;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;

public class TeaVMModelSerializerService extends JpsModelSerializerExtension {
    @Override
    public void loadModuleOptions(@NotNull JpsModule module, @NotNull Element rootElement) {
        rootElement.getChildren("component").stream()
                .filter(child -> Objects.equals(child.getAttributeValue("name"), "teavm"))
                .forEach(child -> readConfig(module, child));
    }

    private void readConfig(@NotNull JpsModule module, @NotNull Element element) {
        TeaVMJpsConfiguration config = XmlSerializer.deserialize(element, TeaVMJpsConfiguration.class);
        assert config != null;
        config.setTo(module);
    }
}
