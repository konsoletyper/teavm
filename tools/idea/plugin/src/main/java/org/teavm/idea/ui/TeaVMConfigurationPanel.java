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

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiMethodUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;
import org.teavm.idea.jps.model.TeaVMProperty;

class TeaVMConfigurationPanel extends JPanel {
    private final JComboBox<ComboBoxItem<Boolean>> skipField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final TextFieldWithBrowseButton mainClassField = new TextFieldWithBrowseButton(event -> chooseMainClass());
    private final TextFieldWithBrowseButton targetDirectoryField = new TextFieldWithBrowseButton();
    private final JComboBox<ComboBoxItem<Boolean>> sourceMapsField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final JComboBox<ComboBoxItem<Boolean>> copySourcesField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final JBTable propertiesTable = new JBTable();
    private final PropertiesModel propertiesModel = new PropertiesModel();
    private final TeaVMJpsConfiguration initialConfiguration = new TeaVMJpsConfiguration();
    private final Project project;

    private final List<ComboBoxItem<Boolean>> sourceMapsOptions = Arrays.asList(new ComboBoxItem<>(true, "Generate"),
            new ComboBoxItem<>(false, "Skip"));

    private final List<ComboBoxItem<Boolean>> copySourcesOptions = Arrays.asList(new ComboBoxItem<>(true, "Copy"),
            new ComboBoxItem<>(false, "Skip"));

    private final List<ComboBoxItem<Boolean>> skipOptions = Arrays.asList(new ComboBoxItem<>(true, "Skip"),
            new ComboBoxItem<>(false, "Don't skip"));

    private final List<Field<?>> fields = Arrays.asList(
            new Field<>(TeaVMJpsConfiguration::setSkipped, TeaVMJpsConfiguration::isSkipped,
                    value -> skipField.setSelectedIndex(value ? 0 : 1),
                    () -> skipOptions.get(skipField.getSelectedIndex()).value),
            new Field<>(TeaVMJpsConfiguration::setMainClass, TeaVMJpsConfiguration::getMainClass,
                    mainClassField::setText, mainClassField::getText),
            new Field<>(TeaVMJpsConfiguration::setTargetDirectory, TeaVMJpsConfiguration::getTargetDirectory,
                    targetDirectoryField::setText, targetDirectoryField::getText),
            new Field<>(TeaVMJpsConfiguration::setSourceMapsFileGenerated,
                    TeaVMJpsConfiguration::isSourceMapsFileGenerated,
                    value -> sourceMapsField.setSelectedIndex(value ? 0 : 1),
                    () -> sourceMapsOptions.get(sourceMapsField.getSelectedIndex()).value),
            new Field<>(TeaVMJpsConfiguration::setSourceFilesCopied,
                    TeaVMJpsConfiguration::isSourceFilesCopied,
                    value -> copySourcesField.setSelectedIndex(value ? 0 : 1),
                    () -> copySourcesOptions.get(copySourcesField.getSelectedIndex()).value)
    );

    TeaVMConfigurationPanel(Project project) {
        this.project = project;
        propertiesTable.setModel(propertiesModel);
        setupLayout();

        FileChooserDescriptor targetDirectoryChooserDescriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor();
        targetDirectoryField.addBrowseFolderListener("Target Directory", "Please, select folder where TeaVM should"
                + "write generated JS files", project, targetDirectoryChooserDescriptor);

        sourceMapsOptions.forEach(sourceMapsField::addItem);
        copySourcesOptions.forEach(copySourcesField::addItem);
        skipOptions.forEach(skipField::addItem);
    }

    private void setupLayout() {
        setLayout(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = GridBagConstraints.REMAINDER;
        labelConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        labelConstraints.weightx = 1;
        labelConstraints.weighty = 1;
        labelConstraints.insets.left = 5;
        labelConstraints.insets.right = 5;

        GridBagConstraints descriptionConstraints = (GridBagConstraints) labelConstraints.clone();
        descriptionConstraints.fill = GridBagConstraints.BOTH;
        descriptionConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        descriptionConstraints.insets.top = 3;

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.BASELINE_LEADING;
        fieldConstraints.weightx = 1;
        fieldConstraints.weighty = 1;
        fieldConstraints.insets.top = 5;
        fieldConstraints.insets.bottom = 20;
        fieldConstraints.insets.left = 10;
        fieldConstraints.insets.right = 10;

        add(bold(new JBLabel("Skip TeaVM compilation")), labelConstraints);
        add(skipField, fieldConstraints);

        add(bold(new JBLabel("Main class")), labelConstraints);
        add(mainClassField, fieldConstraints);

        add(bold(new JBLabel("Target directory")), labelConstraints);
        add(targetDirectoryField, fieldConstraints);

        fieldConstraints.fill = GridBagConstraints.NONE;

        add(bold(new JBLabel("Source maps")), labelConstraints);
        add(new JBLabel("Indicates whether TeaVM should generate source maps."), descriptionConstraints);
        add(new JBLabel("With source maps you can debug code in the browser's devtools."), descriptionConstraints);
        add(sourceMapsField, fieldConstraints);

        add(bold(new JBLabel("Copy source code")), labelConstraints);
        add(new JBLabel("Source maps require your server to provide source code."), descriptionConstraints);
        add(new JBLabel("TeaVM can copy source code to the corresponding location,"), descriptionConstraints);
        add(new JBLabel("which is very convenient if you are going to debug in the browser."), descriptionConstraints);
        add(copySourcesField, fieldConstraints);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 100;
        constraints.weightx = 1;

        JPanel propertiesPanel = new JPanel(new BorderLayout());
        propertiesPanel.setBorder(IdeBorderFactory.createTitledBorder("Properties"));
        propertiesPanel.add(ToolbarDecorator.createDecorator(propertiesTable).createPanel(), BorderLayout.CENTER);

        add(propertiesPanel, constraints);
    }

    private static JBLabel bold(JBLabel label) {
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    void load(TeaVMJpsConfiguration config) {
        if (config == null) {
            config = new TeaVMJpsConfiguration();
        }
        updateInitialConfiguration(config);
        for (Field<?> field : fields) {
            loadField(field, config);
        }

        copyProperties(config.getProperties(), propertiesModel.getProperties());
    }

    void save(TeaVMJpsConfiguration config) {
        for (Field<?> field : fields) {
            saveField(field, config);
        }
        copyProperties(propertiesModel.getProperties(), config.getProperties());
        updateInitialConfiguration(config);
    }

    boolean isModified() {
        return fields.stream().anyMatch(this::isFieldModified) || arePropertiesModified();
    }

    private <T> boolean isFieldModified(Field<T> field) {
        return !Objects.equals(field.dataSupplier.apply(initialConfiguration), field.editSupplier.get());
    }

    private boolean arePropertiesModified() {
        if (initialConfiguration.getProperties().size() != propertiesModel.getProperties().size()) {
            return true;
        }

        for (int i = 0; i < initialConfiguration.getProperties().size(); ++i) {
            TeaVMProperty initialProperty = initialConfiguration.getProperties().get(i);
            TeaVMProperty property = propertiesModel.getProperties().get(i);
            if (!initialProperty.getKey().equals(property.getKey())
                    || !initialProperty.getValue().equals(property.getValue())) {
                return true;
            }
        }

        return false;
    }

    private void updateInitialConfiguration(TeaVMJpsConfiguration config) {
        for (Field<?> field : fields) {
            copyField(field, config);
        }

        copyProperties(config.getProperties(), initialConfiguration.getProperties());
    }

    private void copyProperties(List<TeaVMProperty> from, List<TeaVMProperty> to) {
        to.clear();
        to.addAll(from.stream().map(property -> property.createCopy()).collect(Collectors.toList()));
    }

    private <T> void copyField(Field<T> field, TeaVMJpsConfiguration config) {
        field.dataConsumer.accept(initialConfiguration, field.dataSupplier.apply(config));
    }

    private <T> void loadField(Field<T> field, TeaVMJpsConfiguration config) {
        field.editConsumer.accept(field.dataSupplier.apply(config));
    }

    private <T> void saveField(Field<T> field, TeaVMJpsConfiguration config) {
        field.dataConsumer.accept(config, field.editSupplier.get());
    }

    private void chooseMainClass() {
        TreeClassChooser chooser = TreeClassChooserFactory
                .getInstance(project)
                .createWithInnerClassesScopeChooser("Choose main class",
                        GlobalSearchScope.allScope(project), this::isMainClass, null);
        chooser.showDialog();
        PsiClass cls = chooser.getSelected();
        if (cls != null) {
            mainClassField.setText(cls.getQualifiedName());
        }
    }

    private boolean isMainClass(PsiClass cls) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            return PsiMethodUtil.MAIN_CLASS.value(cls) && PsiMethodUtil.hasMainMethod(cls);
        });
    }

    private static class ComboBoxItem<T> {
        final T value;
        private final String title;

        ComboBoxItem(T value, String title) {
            this.value = value;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private static class Field<T> {
        final BiConsumer<TeaVMJpsConfiguration, T> dataConsumer;
        final Function<TeaVMJpsConfiguration, T> dataSupplier;
        final Consumer<T> editConsumer;
        final Supplier<T> editSupplier;

        Field(BiConsumer<TeaVMJpsConfiguration, T> dataConsumer, Function<TeaVMJpsConfiguration, T> dataSupplier,
                Consumer<T> editConsumer, Supplier<T> editSupplier) {
            this.dataConsumer = dataConsumer;
            this.dataSupplier = dataSupplier;
            this.editConsumer = editConsumer;
            this.editSupplier = editSupplier;
        }
    }

    class PropertiesModel extends AbstractTableModel implements EditableModel {
        private List<TeaVMProperty> properties = new ArrayList<>();

        List<TeaVMProperty> getProperties() {
            return properties;
        }

        @Override
        public void addRow() {
            properties.add(new TeaVMProperty());
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            TeaVMProperty old = properties.get(oldIndex);
            properties.set(oldIndex, properties.get(newIndex));
            properties.set(newIndex, old);
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            return true;
        }

        @Override
        public void removeRow(int idx) {
            properties.remove(idx);
        }

        @Override
        public int getRowCount() {
            return properties.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Name";
                case 1:
                    return "Value";
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return properties.get(rowIndex).getKey();
                case 1:
                    return properties.get(rowIndex).getValue();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    properties.get(rowIndex).setKey((String) aValue);
                    break;
                case 1:
                    properties.get(rowIndex).setValue((String) aValue);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }
    }
}
