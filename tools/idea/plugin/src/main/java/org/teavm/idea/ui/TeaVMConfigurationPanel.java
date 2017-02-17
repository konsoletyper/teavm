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
import com.intellij.ui.components.JBLabel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import org.teavm.idea.jps.model.TeaVMJpsConfiguration;

class TeaVMConfigurationPanel extends JPanel {
    private final TextFieldWithBrowseButton mainClassField = new TextFieldWithBrowseButton(event -> chooseMainClass());
    private final TextFieldWithBrowseButton targetDirectoryField = new TextFieldWithBrowseButton();
    private final JComboBox<ComboBoxItem<Boolean>> minifyingField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final JComboBox<ComboBoxItem<Boolean>> sourceMapsField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final JComboBox<ComboBoxItem<Boolean>> copySourcesField = new JComboBox<>(new DefaultComboBoxModel<>());
    private final TeaVMJpsConfiguration initialConfiguration = new TeaVMJpsConfiguration();
    private final Project project;

    private final List<ComboBoxItem<Boolean>> minifiedOptions = Arrays.asList(new ComboBoxItem<>(false, "Readable"),
            new ComboBoxItem<>(true, "Minified (obfuscated)"));

    private final List<ComboBoxItem<Boolean>> sourceMapsOptions = Arrays.asList(new ComboBoxItem<>(true, "Generate"),
            new ComboBoxItem<>(false, "Skip"));

    private final List<ComboBoxItem<Boolean>> copySourcesOptions = Arrays.asList(new ComboBoxItem<>(true, "Copy"),
            new ComboBoxItem<>(false, "Skip"));

    private final List<Field<?>> fields = Arrays.asList(
            new Field<>(TeaVMJpsConfiguration::setMainClass, TeaVMJpsConfiguration::getMainClass,
                    mainClassField::setText, mainClassField::getText),
            new Field<>(TeaVMJpsConfiguration::setTargetDirectory, TeaVMJpsConfiguration::getTargetDirectory,
                    targetDirectoryField::setText, targetDirectoryField::getText),
            new Field<>(TeaVMJpsConfiguration::setMinifying, TeaVMJpsConfiguration::isMinifying,
                    value -> minifyingField.setSelectedIndex(value ? 1 : 0),
                    () -> minifiedOptions.get(minifyingField.getSelectedIndex()).value),
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
        setupLayout();

        FileChooserDescriptor targetDirectoryChooserDescriptor = FileChooserDescriptorFactory
                .createSingleFolderDescriptor();
        targetDirectoryField.addBrowseFolderListener("Target Directory", "Please, select folder where TeaVM should"
                + "write generated JS files", project, targetDirectoryChooserDescriptor);

        minifiedOptions.forEach(minifyingField::addItem);
        sourceMapsOptions.forEach(sourceMapsField::addItem);
        copySourcesOptions.forEach(copySourcesField::addItem);
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

        add(bold(new JBLabel("Main class")), labelConstraints);
        add(mainClassField, fieldConstraints);

        add(bold(new JBLabel("Target directory")), labelConstraints);
        add(targetDirectoryField, fieldConstraints);

        fieldConstraints.fill = GridBagConstraints.NONE;
        add(bold(new JBLabel("Minification")), labelConstraints);
        add(new JBLabel("Indicates whether TeaVM should minify (obfuscate) generated JavaScript."),
                descriptionConstraints);
        add(new JBLabel("It is highly desirable for production environment, since minified code is up to 3 "
                + "times smaller."), descriptionConstraints);
        add(minifyingField, fieldConstraints);

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
        add(new JPanel(), constraints);
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
    }

    void save(TeaVMJpsConfiguration config) {
        for (Field<?> field : fields) {
            saveField(field, config);
        }
        updateInitialConfiguration(config);
    }

    boolean isModified() {
        return fields.stream().anyMatch(this::isFieldModified);
    }

    private <T> boolean isFieldModified(Field<T> field) {
        return !Objects.equals(field.dataSupplier.apply(initialConfiguration), field.editSupplier.get());
    }

    private void updateInitialConfiguration(TeaVMJpsConfiguration config) {
        for (Field<?> field : fields) {
            copyField(field, config);
        }
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
}
