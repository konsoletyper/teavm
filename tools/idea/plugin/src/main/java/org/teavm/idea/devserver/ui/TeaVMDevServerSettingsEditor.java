/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea.devserver.ui;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.teavm.idea.devserver.TeaVMDevServerConfiguration;

public class TeaVMDevServerSettingsEditor extends SettingsEditor<TeaVMDevServerConfiguration> {
    private final Project project;
    private TeaVMDevServerSettingsPanel panel;

    public TeaVMDevServerSettingsEditor(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull TeaVMDevServerConfiguration s) {
        if (panel == null) {
            return;
        }

        panel.load(s);
    }

    @Override
    protected void applyEditorTo(@NotNull TeaVMDevServerConfiguration s) {
        if (panel == null) {
            return;
        }

        panel.save(s);
    }

    @Override
    protected void disposeEditor() {
        if (panel != null) {
            panel = null;
        }
        super.disposeEditor();
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        if (panel == null) {
            panel = new TeaVMDevServerSettingsPanel(project);
        }
        return panel;
    }
}
