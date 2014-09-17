package org.teavm.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;
import org.teavm.eclipse.TeaVMProfile;
import org.teavm.eclipse.TeaVMProjectSettings;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProfileDialog extends Dialog {
    private Text nameField;
    private Text mainClassField;
    private Button mainClassChooseButton;
    private Text targetDirectoryField;
    private Button targetDirectoryWorkspaceButton;
    private Button targetDirectoryFileSystemButton;
    private Text targetFileNameField;
    private Button minifyingButton;
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
        area.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 5;
        container.setLayout(layout);
        createNameField(container);
        createMainClassField(container);
        createTargetDirectoryField(container);
        createTargetFileNameField(container);
        createMinifyField(container);
        load();
        return container;
    }

    private void createNameField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Name:");

        nameField = new Text(container, SWT.SINGLE | SWT.BORDER);
        nameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createMainClassField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Main class:");

        Composite row = new Composite(container, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 2;
        row.setLayout(rowLayout);

        mainClassField = new Text(row, SWT.SINGLE | SWT.BORDER);
        mainClassField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        mainClassChooseButton = new Button(row, SWT.PUSH);
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

        Composite row = new Composite(container, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout rowLayout = new GridLayout(3, false);
        rowLayout.marginWidth = 2;
        row.setLayout(rowLayout);

        targetDirectoryField = new Text(row, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        targetDirectoryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        targetDirectoryWorkspaceButton = new Button(row, SWT.PUSH);
        targetDirectoryWorkspaceButton.setText("Workspace...");
        targetDirectoryWorkspaceButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                chooseWorkspaceTargetDirectory();
            }
        });

        targetDirectoryFileSystemButton = new Button(row, SWT.PUSH);
        targetDirectoryFileSystemButton.setText("External...");
        targetDirectoryFileSystemButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                chooseFileSystemTargetDirectory();
            }
        });
    }

    private void createTargetFileNameField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Target file:");

        targetFileNameField = new Text(container, SWT.SINGLE | SWT.BORDER);
        targetFileNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void createMinifyField(Composite container) {
        minifyingButton = new Button(container, SWT.CHECK);
        minifyingButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        minifyingButton.setText("generate minified (obfuscated) code");
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

    private void chooseWorkspaceTargetDirectory() {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        dialog.setTitle("Selecting target directory");
        dialog.setMessage("Please, select a target directory for TeaVM build");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
        if (dialog.open() == IDialogConstants.OK_ID) {
            IResource resource = (IResource)dialog.getFirstResult();
            if (resource != null) {
                String path = resource.getFullPath().toString();
                String fileLoc = VariablesPlugin.getDefault().getStringVariableManager()
                        .generateVariableExpression("workspace_loc", path);
                targetDirectoryField.setText(fileLoc);
            }
        }
    }

    private void chooseFileSystemTargetDirectory() {
        String filePath = targetDirectoryField.getText();
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        filePath = dialog.open();
        if (filePath != null) {
            targetDirectoryField.setText(filePath);
        }
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

    private void load() {
        nameField.setText(profile.getName());
        mainClassField.setText(profile.getMainClass() != null ? profile.getMainClass() : "");
        targetDirectoryField.setText(profile.getTargetDirectory());
        targetFileNameField.setText(profile.getTargetFileName());
        minifyingButton.setSelection(profile.isMinifying());
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
        profile.setMinifying(minifyingButton.getSelection());
        return true;
    }
}
