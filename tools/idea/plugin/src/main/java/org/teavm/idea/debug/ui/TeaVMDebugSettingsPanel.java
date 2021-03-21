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
package org.teavm.idea.debug.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import org.teavm.idea.debug.TeaVMDebugConfiguration;

class TeaVMDebugSettingsPanel extends JPanel {
    private final JBTextField portField = new JBTextField();

    public TeaVMDebugSettingsPanel() {
        setBorder(JBUI.Borders.empty(10));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        labelConstraints.weightx = 0;
        labelConstraints.weighty = 1;
        labelConstraints.insets.left = 5;
        labelConstraints.insets.right = 5;

        GridBagConstraints fieldConstraints = (GridBagConstraints) labelConstraints.clone();

        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.weightx = 1;
        fieldConstraints.weighty = 1;
        fieldConstraints.fill = GridBagConstraints.BOTH;
        fieldConstraints.anchor = GridBagConstraints.BASELINE_LEADING;

        setLayout(new GridBagLayout());

        add(bold(new JBLabel("Listen port:")), labelConstraints);
        add(portField, fieldConstraints);
    }

    public void load(TeaVMDebugConfiguration runConfiguration) {
        portField.setText(String.valueOf(runConfiguration.getPort()));
    }

    private static JBLabel bold(JBLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    public void save(TeaVMDebugConfiguration runConfiguration) throws ConfigurationException {
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Port is not a number");
        }
        runConfiguration.setPort(port);
    }
}
