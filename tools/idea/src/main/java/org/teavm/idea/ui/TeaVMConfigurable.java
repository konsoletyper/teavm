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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.TeaVMConfigurationStorage;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

public class TeaVMConfigurable implements Configurable {
    private final Module module;
    private TeaVMConfigurationPanel panel;

    public TeaVMConfigurable(Module module) {
        this.module = module;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "TeaVM";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null) {
            panel = new TeaVMConfigurationPanel(module.getProject());
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        return panel != null && panel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        TeaVMJpsConfiguration config = new TeaVMJpsConfiguration();
        panel.save(config);
        TeaVMConfigurationStorage configStorage = ModuleServiceManager.getService(module,
                TeaVMConfigurationStorage.class);
        assert configStorage != null;
        configStorage.loadState(config);
    }

    @Override
    public void reset() {
        if (panel == null) {
            return;
        }
        TeaVMConfigurationStorage configStorage = ModuleServiceManager.getService(module,
                TeaVMConfigurationStorage.class);
        assert configStorage != null;
        panel.load(configStorage.getState());
    }

    @Override
    public void disposeUIResources() {
        this.panel = null;
    }
}
