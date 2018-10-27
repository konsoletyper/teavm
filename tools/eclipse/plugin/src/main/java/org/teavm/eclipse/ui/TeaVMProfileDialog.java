/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.eclipse.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.teavm.eclipse.TeaVMProfile;
import org.teavm.eclipse.TeaVMProjectSettings;

public class TeaVMProfileDialog extends Dialog {
    private TabFolder tabFolder;
    private Text nameField;
    private Text mainClassField;
    private Button mainClassChooseButton;
    private Text targetDirectoryField;
    private Button targetDirectoryWorkspaceButton;
    private Button targetDirectoryFileSystemButton;
    private Text targetFileNameField;
    private Button incrementalButton;
    private Text cacheDirectoryField;
    private Button cacheDirectoryWorkspaceButton;
    private Button cacheDirectoryFileSystemButton;
    private Button debugInformationButton;
    private Button sourceMapsButton;
    private Button sourceFilesCopiedButton;
    private TableViewer propertiesTableViewer;
    private Button addPropertyButton;
    private Button deletePropertyButton;
    private WritableList propertyList = new WritableList();
    private org.eclipse.swt.widgets.List classAliasesTableViewer;
    private Button addClassAliasButton;
    private Button removeClassAliasButton;
    private org.eclipse.swt.widgets.List transformersList;
    private Button addTransformerButton;
    private Button removeTransformerButton;
    private IJavaProject javaProject;
    private TeaVMProjectSettings settings;
    private TeaVMProfile profile;

    public TeaVMProfileDialog(Shell shell, TeaVMProjectSettings settings, TeaVMProfile profile) {
        super(shell);
        this.settings = settings;
        this.profile = profile;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Editing TeaVM profile");
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite)super.createDialogArea(parent);
        area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ScrolledComposite scrollContainer = new ScrolledComposite(area, SWT.V_SCROLL | SWT.H_SCROLL);
        scrollContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tabFolder = new TabFolder(scrollContainer, SWT.TOP);
        scrollContainer.setContent(tabFolder);
        scrollContainer.setExpandHorizontal(true);
        scrollContainer.setExpandVertical(true);
        TabItem generalItem = new TabItem(tabFolder, SWT.NONE);
        generalItem.setText("General");
        generalItem.setControl(createGeneralTab(tabFolder));
        TabItem advancedItem = new TabItem(tabFolder, SWT.NONE);
        advancedItem.setText("Advanced");
        advancedItem.setControl(createAdvancedTab(tabFolder));
        load();
        tabFolder.setSize(tabFolder.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrollContainer.setMinSize(tabFolder.getSize());
        return scrollContainer;
    }

    private Composite createGeneralTab(TabFolder folder) {
        Composite container = new Composite(folder, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 8;
        layout.verticalSpacing = 10;
        container.setLayout(layout);
        createMainGroup(container);
        createOutputGroup(container);
        createIncrementalGroup(container);
        createDebugGroup(container);
        createPropertiesGroup(container);
        return container;
    }

    private Composite createAdvancedTab(TabFolder folder) {
        Composite container = new Composite(folder, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 8;
        layout.verticalSpacing = 10;
        container.setLayout(layout);
        createClassesGroup(container);
        createTransformersGroup(container);
        return container;
    }

    private void createMainGroup(Composite parent) {
        Group group = createGroup(parent, "Main settings", 3, false);
        createNameField(group);
        createMainClassField(group);
    }

    private void createOutputGroup(Composite parent) {
        Group group = createGroup(parent, "Output settings", 4, false);
        createTargetDirectoryField(group);
        createTargetFileNameField(group);
    }

    private void createIncrementalGroup(Composite parent) {
        Group group = createGroup(parent, "Incremental build settings", 4, false);
        createIncrementalField(group);
        createCacheDirectoryField(group);
    }

    private void createDebugGroup(Composite parent) {
        Group group = createGroup(parent, "Debug settings", 1, false);
        createDebugInformationField(group);
        createSourceMapsField(group);
        createSourceFilesCopiedField(group);
    }

    private void createPropertiesGroup(Composite parent) {
        Group group = createGroup(parent, "Properties", 2, true);
        propertiesTableViewer = new TableViewer(group, SWT.BORDER | SWT.V_SCROLL);
        propertiesTableViewer.getTable().setLinesVisible(true);
        propertiesTableViewer.getTable().setHeaderVisible(true);
        propertiesTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
        propertiesTableViewer.setContentProvider(new ObservableListContentProvider());
        propertiesTableViewer.setInput(propertyList);

        TableViewerColumn propertyColumn = new TableViewerColumn(propertiesTableViewer, SWT.LEFT);
        propertyColumn.getColumn().setWidth(200);
        propertyColumn.getColumn().setText("Property");
        propertyColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) {
                KeyValue item = (KeyValue)element;
                return item.key;
            }
        });
        propertyColumn.setEditingSupport(new KeyValueEditingSupport(propertyColumn.getViewer(),
                propertiesTableViewer.getTable()) {
            @Override protected Object getValue(Object element) {
                KeyValue item = (KeyValue)element;
                return item.key;
            }
            @Override protected void setValue(Object element, Object value) {
                KeyValue item = (KeyValue)element;
                item.key = (String)value;
                getViewer().update(element, null);
            }
        });

        TableViewerColumn valueColumn = new TableViewerColumn(propertiesTableViewer, SWT.LEFT);
        valueColumn.getColumn().setWidth(200);
        valueColumn.getColumn().setText("Value");
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override public String getText(Object element) {
                KeyValue item = (KeyValue)element;
                return item.value;
            }
        });
        valueColumn.setEditingSupport(new KeyValueEditingSupport(valueColumn.getViewer(),
                propertiesTableViewer.getTable()) {
            @Override protected Object getValue(Object element) {
                KeyValue item = (KeyValue)element;
                return item.value;
            }
            @Override protected void setValue(Object element, Object value) {
                KeyValue item = (KeyValue)element;
                item.value = (String)value;
                getViewer().update(element, null);
            }
        });

        addPropertyButton = new Button(group, SWT.PUSH);
        addPropertyButton.setText("Add");
        addPropertyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        addPropertyButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                addProperty();
            }
        });

        deletePropertyButton = new Button(group, SWT.PUSH);
        deletePropertyButton.setText("Delete");
        deletePropertyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        deletePropertyButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                deleteProperty();
            }
        });
    }

    static abstract class KeyValueEditingSupport extends EditingSupport {
        private TextCellEditor editor;

        public KeyValueEditingSupport(ColumnViewer viewer, Table table) {
            super(viewer);
            editor = new TextCellEditor(table);
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return editor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }
    }

    private void addProperty() {
        KeyValue item = new KeyValue();
        propertyList.add(item);
    }

    private void deleteProperty() {
        int index = propertiesTableViewer.getTable().getSelectionIndex();
        if (index < 0) {
            return;
        }
        KeyValue item = (KeyValue)propertyList.get(index);
        boolean confirmed = MessageDialog.openConfirm(getShell(), "Property deletion confirmation",
                "Are you sure to delete property " + item.key + "?");
        if (!confirmed) {
            return;
        }
        propertyList.remove(index);
    }

    static class KeyValue {
        String key = "";
        String value = "";
    }

    private void createClassesGroup(Composite parent) {
        Group group = createGroup(parent, "Class aliases", 2, true);
        classAliasesTableViewer = new org.eclipse.swt.widgets.List(group, SWT.BORDER | SWT.SINGLE);
        classAliasesTableViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));

        addClassAliasButton = new Button(group, SWT.PUSH);
        addClassAliasButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addClassAliasButton.setText("Add...");
        addClassAliasButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { addClass(); }
        });

        removeClassAliasButton = new Button(group, SWT.PUSH);
        removeClassAliasButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        removeClassAliasButton.setText("Remove");
        removeClassAliasButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { removeClass(); }
        });
    }

    private void createTransformersGroup(Composite parent) {
        Group group = createGroup(parent, "TeaVM bytecode transformers", 2, true);
        transformersList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER);
        transformersList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));

        addTransformerButton = new Button(group, SWT.PUSH);
        addTransformerButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addTransformerButton.setText("Add...");
        addTransformerButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                addTransformer();
            }
        });

        removeTransformerButton = new Button(group, SWT.PUSH);
        removeTransformerButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        removeTransformerButton.setText("Remove");
        removeTransformerButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                removeTransformer();
            }
        });
    }

    private Group createGroup(Composite parent, String title, int columns, boolean fillVert) {
        Group group = new Group(parent, SWT.NONE);
        group.setLayoutData(new GridData(SWT.FILL, fillVert ? SWT.FILL : SWT.TOP, true, fillVert));
        group.setText(title);
        GridLayout layout = new GridLayout(columns, false);
        layout.horizontalSpacing = 3;
        layout.verticalSpacing = 2;
        layout.marginWidth = 10;
        group.setLayout(layout);
        return group;
    }

    private void createNameField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Name:");

        nameField = new Text(container, SWT.SINGLE | SWT.BORDER);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    }

    private void createMainClassField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Main class:");

        mainClassField = new Text(container, SWT.SINGLE | SWT.BORDER);
        mainClassField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        mainClassChooseButton = new Button(container, SWT.PUSH);
        mainClassChooseButton.setText("Choose...");
        mainClassChooseButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                chooseMainClass();
            }
        });
    }

    private void createTargetDirectoryField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Target directory:");

        targetDirectoryField = new Text(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        targetDirectoryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        targetDirectoryWorkspaceButton = new Button(container, SWT.PUSH);
        targetDirectoryWorkspaceButton.setText("Workspace...");
        targetDirectoryWorkspaceButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                String dir = chooseWorkspaceDirectory("Please, select a target directory");
                if (dir != null) {
                    targetDirectoryField.setText(dir);
                }
            }
        });

        targetDirectoryFileSystemButton = new Button(container, SWT.PUSH);
        targetDirectoryFileSystemButton.setText("External...");
        targetDirectoryFileSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                String dir = chooseFileSystemDirectory("Please, select a target directory");
                if (dir != null) {
                    targetDirectoryField.setText(dir);
                }
            }
        });
    }

    private void createTargetFileNameField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Target file:");

        targetFileNameField = new Text(container, SWT.SINGLE | SWT.BORDER);
        targetFileNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
    }

    private void createIncrementalField(Composite container) {
        incrementalButton = new Button(container, SWT.CHECK);
        incrementalButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        incrementalButton.setText("Build &incrementally");
        incrementalButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                updateCacheFieldsEnabled();
            }
        });
    }

    private void createCacheDirectoryField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Cac&he directory:");

        cacheDirectoryField = new Text(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        cacheDirectoryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        cacheDirectoryWorkspaceButton = new Button(container, SWT.PUSH);
        cacheDirectoryWorkspaceButton.setText("Workspace...");
        cacheDirectoryWorkspaceButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                String dir = chooseWorkspaceDirectory("Please, select a directory for the incremental cache");
                if (dir != null) {
                    cacheDirectoryField.setText(dir);
                }
            }
        });

        cacheDirectoryFileSystemButton = new Button(container, SWT.PUSH);
        cacheDirectoryFileSystemButton.setText("External...");
        cacheDirectoryFileSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                String dir = chooseFileSystemDirectory("Please, select a directory for the incremental cache");
                if (dir != null) {
                    cacheDirectoryField.setText(dir);
                }
            }
        });

        updateCacheFieldsEnabled();
    }

    private void createDebugInformationField(Composite container) {
        debugInformationButton = new Button(container, SWT.CHECK);
        debugInformationButton.setText("Generate debug information for TeaVM native debugger");
    }

    private void createSourceMapsField(Composite container) {
        sourceMapsButton = new Button(container, SWT.CHECK);
        sourceMapsButton.setText("Generate source maps");
    }

    private void createSourceFilesCopiedField(Composite container) {
        sourceFilesCopiedButton = new Button(container, SWT.CHECK);
        sourceFilesCopiedButton.setText("Copy source files");
    }

    public void setProject(IProject project) throws CoreException {
        if (project.hasNature(JavaCore.NATURE_ID)) {
            this.javaProject = JavaCore.create(project);
        } else {
            this.javaProject = null;
        }
    }

    private void chooseMainClass() {
        MainClassSelectionDialog selectionDialog = new MainClassSelectionDialog(getShell(), javaProject);
        if (selectionDialog.open() == MainClassSelectionDialog.OK) {
            Object[] result = selectionDialog.getResult();
            if (result.length > 0) {
                IType type = (IType)result[0];
                mainClassField.setText(type.getFullyQualifiedName());
            }
        }
    }

    private String chooseWorkspaceDirectory(String prompt) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        dialog.setTitle("Selecting directory");
        dialog.setMessage(prompt);
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
        if (dialog.open() == IDialogConstants.OK_ID) {
            IResource resource = (IResource)dialog.getFirstResult();
            if (resource != null) {
                String path = resource.getFullPath().toString();
                String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
                        .generateVariableExpression("workspace_loc", path);
                return fileLoc;
            }
        }
        return null;
    }

    private String chooseFileSystemDirectory(String prompt) {
        String filePath = targetDirectoryField.getText();
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setMessage(prompt);
        filePath = dialog.open();
        return filePath;
    }

    private void addTransformer() {
        TransformerClassSelectionDialog selectionDialog = new TransformerClassSelectionDialog(getShell(),
                javaProject);
        if (selectionDialog.open() == ClassSelectionDialog.OK) {
            Object[] result = selectionDialog.getResult();
            if (result.length > 0) {
                IType type = (IType)result[0];
                List<String> existingTypes = Arrays.asList(transformersList.getItems());
                if (!existingTypes.contains(type.getFullyQualifiedName())) {
                    transformersList.add(type.getFullyQualifiedName());
                }
            }
        }
    }

    private void removeTransformer() {
        if (transformersList.getSelectionCount() != 1) {
            return;
        }
        boolean confirmed = MessageDialog.openConfirm(getShell(), "Removal confirmation",
                "Are you sure to delete the " + transformersList.getSelection()[0] + " transformer?");
        if (!confirmed) {
            return;
        }
        transformersList.remove(transformersList.getSelectionIndex());
    }

    private void addClass() {
        AnyClassSelectionDialog selectionDialog = new AnyClassSelectionDialog(getShell(), javaProject);
        if (selectionDialog.open() == ClassSelectionDialog.OK) {
            Object[] result = selectionDialog.getResult();
            if (result.length > 0) {
                IType type = (IType)result[0];
                classAliasesTableViewer.add(type.getFullyQualifiedName());
            }
        }
    }

    private void removeClass() {
        if (classAliasesTableViewer.getSelectionCount() != 1) {
            return;
        }
        String className = classAliasesTableViewer.getItem(classAliasesTableViewer.getSelectionIndex());
        boolean confirmed = MessageDialog.openConfirm(getShell(), "Removal confirmation",
                "Are you sure to delete the " + className + " class?");
        if (!confirmed) {
            return;
        }
        classAliasesTableViewer.remove(classAliasesTableViewer.getSelectionIndex());
    }

    @Override
    protected void okPressed() {
        if (save()) {
            super.okPressed();
        } else {
            MessageBox mbox = new MessageBox(getShell(), SWT.ICON_ERROR);
            mbox.setMessage("Name " + nameField.getText() + " already used by another profile");
            mbox.setText("Invalid data supplied");
            mbox.open();
        }
    }

    private void updateCacheFieldsEnabled() {
        cacheDirectoryField.setEnabled(incrementalButton.getSelection());
        cacheDirectoryFileSystemButton.setEnabled(incrementalButton.getSelection());
        cacheDirectoryWorkspaceButton.setEnabled(incrementalButton.getSelection());
    }

    private static void setEnabledRecursive(Composite composite, boolean enabled) {
        Control[] children = composite.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Composite) {
                setEnabledRecursive((Composite) children[i], enabled);
            } else {
                children[i].setEnabled(enabled);
            }
        }
        composite.setEnabled(enabled);
    }

    private void load() {
        nameField.setText(profile.getName());
        mainClassField.setText(profile.getMainClass() != null ? profile.getMainClass() : "");
        targetDirectoryField.setText(profile.getTargetDirectory());
        targetFileNameField.setText(profile.getTargetFileName());
        incrementalButton.setSelection(profile.isIncremental());
        cacheDirectoryField.setText(profile.getCacheDirectory());
        debugInformationButton.setSelection(profile.isDebugInformationGenerated());
        sourceMapsButton.setSelection(profile.isSourceMapsGenerated());
        sourceFilesCopiedButton.setSelection(profile.isSourceFilesCopied());
        propertyList.clear();
        Properties properties = profile.getProperties();
        for (Object key : properties.keySet()) {
            KeyValue property = new KeyValue();
            property.key = (String)key;
            property.value = properties.getProperty((String)key);
            propertyList.add(property);
        }
        updateCacheFieldsEnabled();
        transformersList.setItems(profile.getTransformers());
        for (String className : profile.getClassesToPreserve()) {
            classAliasesTableViewer.add(className);
        }
        for (Control control : tabFolder.getTabList()) {
            if (control instanceof Composite) {
                setEnabledRecursive((Composite)control, profile.getExternalToolId().isEmpty());
            }
        }
    }

    private boolean save() {
        String name = nameField.getText().trim();
        TeaVMProfile existingProfile = settings.getProfile(name);
        if (existingProfile != null && existingProfile != profile) {
            return false;
        }
        profile.setName(name);
        String mainClass = mainClassField.getText().trim();
        profile.setMainClass(!mainClass.isEmpty() ? mainClass : null);
        profile.setTargetDirectory(targetDirectoryField.getText());
        profile.setTargetFileName(targetFileNameField.getText().trim());
        profile.setIncremental(incrementalButton.getSelection());
        profile.setCacheDirectory(cacheDirectoryField.getText());
        profile.setDebugInformationGenerated(debugInformationButton.getSelection());
        profile.setSourceMapsGenerated(sourceMapsButton.getSelection());
        profile.setSourceFilesCopied(sourceFilesCopiedButton.getSelection());
        Properties properties = new Properties();
        for (Object item : propertyList) {
            KeyValue property = (KeyValue)item;
            properties.setProperty(property.key, property.value);
        }
        profile.setProperties(properties);
        profile.setTransformers(transformersList.getItems());
        Set<String> classesToPreserve = new HashSet<>();
        for (int i = 0; i < classAliasesTableViewer.getItemCount(); ++i) {
            classesToPreserve.add(classAliasesTableViewer.getItem(i));
        }
        profile.setClassesToPreserve(classesToPreserve);
        return true;
    }
}
