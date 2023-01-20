/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.idea.debug.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.debug.TeaVMDebugConfiguration;

public class TeaVMDebugSettingsEditor extends SettingsEditor<TeaVMDebugConfiguration> {
    private final TeaVMDebugSettingsPanel panel = new TeaVMDebugSettingsPanel();

    @Override
    protected void resetEditorFrom(TeaVMDebugConfiguration s) {
        panel.load(s);
    }

    @Override
    protected void applyEditorTo(TeaVMDebugConfiguration s) throws ConfigurationException {
        panel.save(s);
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return panel;
    }
}
