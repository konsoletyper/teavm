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
package org.teavm.idea.ui;

import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.module.Module;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMFacetEditorTab extends FacetEditorTab {
    private TeaVMConfigurable configurable;

    public TeaVMFacetEditorTab(Module module, TeaVMJpsConfiguration configuration) {
        configurable = new TeaVMConfigurable(module, configuration);
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return configurable.createComponent();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "General settings";
    }

    @Override
    public boolean isModified() {
        return configurable.isModified();
    }

    @Override
    public void apply() {
        configurable.apply();
    }

    @Override
    public void disposeUIResources() {
        configurable.disposeUIResources();
    }

    @Override
    public void reset() {
        configurable.reset();
    }
}
