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

import com.intellij.application.options.ModuleDescriptionsComboBox;
import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.execution.ui.DefaultJreSelector;
import com.intellij.execution.ui.JrePathEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.ui.EditorTextFieldWithBrowseButton;
import com.intellij.util.ui.JBUI;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DecimalFormat;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.teavm.idea.devserver.TeaVMDevServerConfiguration;

public class TeaVMDevServerSettingsPanel extends JPanel {
    private final JrePathEditor jrePathEditor;

    private final ModuleDescriptionsComboBox moduleField;
    private final ConfigurationModuleSelector moduleSelector;

    private EditorTextFieldWithBrowseButton mainClassField;

    private JFormattedTextField portField;
    private JTextField pathToFileField;
    private JTextField fileNameField;
    private JCheckBox indicatorField;
    private JCheckBox deobfuscateStackField;
    private JCheckBox autoReloadField;
    private JFormattedTextField maxHeapField;
    private JTextField proxyUrlField;
    private JTextField proxyPathField;

    public TeaVMDevServerSettingsPanel(Project project) {
        moduleField = new ModuleDescriptionsComboBox();
        moduleSelector = new ConfigurationModuleSelector(project, moduleField);

        JavaCodeFragment.VisibilityChecker visibilityChecker = (declaration, place) -> {
            if (declaration instanceof PsiClass) {
                PsiClass cls = (PsiClass) declaration;
                if (ConfigurationUtil.MAIN_CLASS.value(cls) && PsiMethodUtil.findMainMethod(cls) != null
                        || place.getParent() != null && moduleSelector.findClass(cls.getQualifiedName()) != null) {
                    return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
                }
            }
            return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
        };
        mainClassField = new EditorTextFieldWithBrowseButton(project, true, visibilityChecker);
        mainClassField.setButtonEnabled(true);

        jrePathEditor = new JrePathEditor(DefaultJreSelector.fromSourceRootsDependencies(moduleField, mainClassField));

        portField = new JFormattedTextField(new DecimalFormat("#0"));
        fileNameField = new JTextField();
        pathToFileField = new JTextField();
        indicatorField = new JCheckBox("Display indicator on a web page:");
        deobfuscateStackField = new JCheckBox("Deobfuscate stack traces in exceptions");
        autoReloadField = new JCheckBox("Reload page automatically:");
        maxHeapField = new JFormattedTextField(new DecimalFormat("#0"));

        proxyUrlField = new JTextField();
        proxyPathField = new JTextField();

        initLayout();
    }

    private void initLayout() {
        setLayout(new GridBagLayout());
        setBorder(JBUI.Borders.empty(10));

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.insets.right = 5;
        labelConstraints.anchor = GridBagConstraints.LINE_START;

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets.top = 4;
        constraints.insets.bottom = 4;

        add(new JLabel("Main class:"), labelConstraints);
        add(mainClassField, constraints);

        add(new JLabel("Use classpath of module:"), labelConstraints);
        add(moduleField, constraints);

        add(jrePathEditor, constraints);

        add(new JLabel("Port:"), labelConstraints);
        add(portField, constraints);

        add(new JLabel("File name:"), labelConstraints);
        add(fileNameField, constraints);

        add(new JLabel("Path to file:"), labelConstraints);
        add(pathToFileField, constraints);

        add(indicatorField, constraints);
        add(deobfuscateStackField, constraints);
        add(autoReloadField, constraints);

        add(new JLabel("Server heap limit:"), labelConstraints);
        add(maxHeapField, constraints);

        add(new JLabel("Proxy URL:"), labelConstraints);
        add(proxyUrlField, constraints);

        add(new JLabel("Proxy from path:"), labelConstraints);
        add(proxyPathField, constraints);
    }

    public void load(TeaVMDevServerConfiguration configuration) {
        mainClassField.setText(configuration.getMainClass());
        moduleSelector.reset(configuration);
        jrePathEditor.setPathOrName(configuration.getJdkPath(), false);
        fileNameField.setText(configuration.getFileName());
        pathToFileField.setText(configuration.getPathToFile());
        indicatorField.setSelected(configuration.isIndicator());
        deobfuscateStackField.setSelected(configuration.isDeobfuscateStack());
        autoReloadField.setSelected(configuration.isAutomaticallyReloaded());
        maxHeapField.setText(Integer.toString(configuration.getMaxHeap()));
        portField.setText(Integer.toString(configuration.getPort()));
        proxyUrlField.setText(configuration.getProxyUrl());
        proxyPathField.setText(configuration.getProxyPath());
    }

    public void save(TeaVMDevServerConfiguration configuration) {
        configuration.setMainClass(mainClassField.getText().trim());
        moduleSelector.applyTo(configuration);
        configuration.setJdkPath(jrePathEditor.getJrePathOrName());
        configuration.setFileName(fileNameField.getText().trim());
        configuration.setPathToFile(pathToFileField.getText().trim());
        configuration.setIndicator(indicatorField.isSelected());
        configuration.setDeobfuscateStack(deobfuscateStackField.isSelected());
        configuration.setAutomaticallyReloaded(autoReloadField.isSelected());
        if (!maxHeapField.getText().trim().isEmpty()) {
            configuration.setMaxHeap(Integer.parseInt(maxHeapField.getText()));
        }
        if (!portField.getText().trim().isEmpty()) {
            configuration.setPort(Integer.parseInt(portField.getText()));
        }
        configuration.setProxyUrl(proxyUrlField.getText().trim());
        configuration.setProxyPath(proxyPathField.getText().trim());
    }
}
