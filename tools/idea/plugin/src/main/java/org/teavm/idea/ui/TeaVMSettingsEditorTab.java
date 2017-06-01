/*
 *  Copyright 2017 Alexey Andreev.
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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.idea.TeaVMDaemonComponent;

public class TeaVMSettingsEditorTab implements SearchableConfigurable {
    private JPanel contentPane;
    private JCheckBox daemonCheckBox;
    private JCheckBox incrementalCheckBox;
    private TeaVMDaemonComponent daemonComponent;

    public TeaVMSettingsEditorTab(TeaVMDaemonComponent daemonComponent) {
        this.daemonComponent = daemonComponent;

        contentPane = new JPanel();
        daemonCheckBox = new JCheckBox("use build daemon (can increase performance in most cases)");
        incrementalCheckBox = new JCheckBox("incremental build (only available with daemon)");
        contentPane.setLayout(new GridBagLayout());

        daemonCheckBox.addActionListener(e -> incrementalCheckBox.setEnabled(daemonCheckBox.isSelected()));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        labelConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        labelConstraints.weightx = 1;
        labelConstraints.weighty = 1;
        labelConstraints.insets.left = 5;
        labelConstraints.insets.right = 5;

        contentPane.add(daemonCheckBox, labelConstraints);
        contentPane.add(incrementalCheckBox, labelConstraints);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 100;
        constraints.weightx = 1;
        contentPane.add(new JPanel(), constraints);
    }

    @NotNull
    @Override
    public String getId() {
        return "project.teavm.settings";
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
        return contentPane;
    }

    @Override
    public boolean isModified() {
        return daemonCheckBox.isSelected() != daemonComponent.isDaemonRunning()
                || incrementalCheckBox.isSelected() != daemonComponent.isIncremental();
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean shouldRestartDaemon = true;

        if (incrementalCheckBox.isSelected() && !daemonComponent.isIncremental()) {
            shouldRestartDaemon = true;
        }
        daemonComponent.setIncremental(incrementalCheckBox.isSelected());

        if (daemonCheckBox.isSelected()) {
            if (!daemonComponent.isDaemonRunning()) {
                daemonComponent.startDaemon();
                shouldRestartDaemon = false;
            }
        } else {
            daemonComponent.stopDaemon();
            shouldRestartDaemon = false;
        }

        if (shouldRestartDaemon) {
            daemonComponent.stopDaemon();
            daemonComponent.startDaemon();
        }

        daemonComponent.applyChanges();
    }

    @Override
    public void reset() {
        daemonCheckBox.setSelected(daemonComponent.isDaemonRunning());
        incrementalCheckBox.setSelected(daemonComponent.isIncremental());
    }
}
