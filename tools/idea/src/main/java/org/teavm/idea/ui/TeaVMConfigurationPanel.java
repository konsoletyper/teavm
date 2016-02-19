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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

class TeaVMConfigurationPanel extends JPanel {
    private final JCheckBox enabledCheckBox = new JCheckBox("TeaVM enabled for this module");
    private final JTextField mainClassField = new JTextField();
    private final JTextField targetDirectoryField = new JTextField();
    private final TeaVMJpsConfiguration initialConfiguration = new TeaVMJpsConfiguration();
    private final List<JComponent> editComponents = Arrays.asList(mainClassField, targetDirectoryField);
    private final List<Field<?>> fields = Arrays.asList(
            new Field<>(TeaVMJpsConfiguration::setEnabled, TeaVMJpsConfiguration::isEnabled,
                    enabledCheckBox::setSelected, enabledCheckBox::isSelected),
            new Field<>(TeaVMJpsConfiguration::setMainClass, TeaVMJpsConfiguration::getMainClass,
                    mainClassField::setText, mainClassField::getText),
            new Field<>(TeaVMJpsConfiguration::setTargetDirectory, TeaVMJpsConfiguration::getTargetDirectory,
                    targetDirectoryField::setText, targetDirectoryField::getText)
    );

    TeaVMConfigurationPanel() {
        enabledCheckBox.addActionListener(event -> updateEnabledState());
        setupLayout();
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(enabledCheckBox, constraints);

        GridBagConstraints labelConstrains = new GridBagConstraints();
        labelConstrains.gridwidth = GridBagConstraints.RELATIVE;
        labelConstrains.anchor = GridBagConstraints.BASELINE_TRAILING;

        GridBagConstraints fieldConstrains = new GridBagConstraints();
        fieldConstrains.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstrains.fill = GridBagConstraints.HORIZONTAL;
        labelConstrains.anchor = GridBagConstraints.BASELINE_LEADING;
        labelConstrains.insets.right = 5;

        add(new JLabel("Main class:"), labelConstrains);
        add(mainClassField, fieldConstrains);

        add(new JLabel("Target directory:"), labelConstrains);
        add(targetDirectoryField, fieldConstrains);
    }

    public void load(TeaVMJpsConfiguration config) {
        if (config == null) {
            config = new TeaVMJpsConfiguration();
        }
        updateInitialConfiguration(config);
        for (Field<?> field : fields) {
            loadField(field, config);
        }
        updateEnabledState();
    }

    public void save(TeaVMJpsConfiguration config) {
        for (Field<?> field : fields) {
            saveField(field, config);
        }
        updateInitialConfiguration(config);
    }

    private <T> void loadField(Field<T> field, TeaVMJpsConfiguration config) {
        field.editConsumer.accept(field.dataSupplier.apply(config));
    }

    private <T> void saveField(Field<T> field, TeaVMJpsConfiguration config) {
        field.dataConsumer.accept(config, field.editSupplier.get());
    }

    private void updateEnabledState() {
        for (JComponent component : editComponents) {
            component.setEnabled(enabledCheckBox.isSelected());
        }
    }

    public boolean isModified() {
        return fields.stream().anyMatch(this::isFieldModified);
    }

    private <T> boolean isFieldModified(Field<T> field) {
        return !Objects.equals(field.dataSupplier.apply(initialConfiguration), field.editSupplier.get());
    }

    private void updateInitialConfiguration(TeaVMJpsConfiguration config) {
        initialConfiguration.setEnabled(config.isEnabled());
        initialConfiguration.setMainClass(config.getMainClass());
        initialConfiguration.setTargetDirectory(config.getTargetDirectory());
    }

    static class Field<T> {
        final BiConsumer<TeaVMJpsConfiguration, T> dataConsumer;
        final Function<TeaVMJpsConfiguration, T> dataSupplier;
        final Consumer<T> editConsumer;
        final Supplier<T> editSupplier;

        public Field(BiConsumer<TeaVMJpsConfiguration, T> dataConsumer, Function<TeaVMJpsConfiguration, T> dataSupplier,
                Consumer<T> editConsumer, Supplier<T> editSupplier) {
            this.dataConsumer = dataConsumer;
            this.dataSupplier = dataSupplier;
            this.editConsumer = editConsumer;
            this.editSupplier = editSupplier;
        }
    }
}
