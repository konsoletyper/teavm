package org.teavm.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;
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
import org.teavm.eclipse.TeaVMEclipsePlugin;
import org.teavm.eclipse.TeaVMProjectSettings;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectPropertyWidget extends Composite {
    private Button natureButton;
    private Text mainClassField;
    private Button mainClassChooseButton;
    private Text targetDirectoryField;
    private Button targetDirectoryWorkspaceButton;
    private Button targetDirectoryFileSystemButton;
    private IJavaProject javaProject;
    private IRunnableContext runnableContext;

    public TeaVMProjectPropertyWidget(Composite parent) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 10;
        layout.marginWidth = 10;
        setLayout(layout);
        natureButton = new Button(this, SWT.CHECK);
        natureButton.setText("TeaVM build enabled");
        createOptionsContainer();

        natureButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEnabled(natureButton.getSelection());
            }
        });
    }

    private void updateEnabled(boolean enabled) {
        mainClassField.setEnabled(enabled);
        mainClassChooseButton.setEnabled(enabled);
        targetDirectoryField.setEnabled(enabled);
        targetDirectoryWorkspaceButton.setEnabled(enabled);
        targetDirectoryFileSystemButton.setEnabled(enabled);
    }

    private void createOptionsContainer() {
        Composite container = new Composite(this, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = 5;
        container.setLayout(layout);
        createMainClassField(container);
        createTargetDirectoryField(container);
    }

    private void createMainClassField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("&Main class:");

        Composite row = new Composite(container, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        GridLayout rowLayout = new GridLayout();
        rowLayout.numColumns = 2;
        rowLayout.horizontalSpacing = 2;
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
        GridLayout rowLayout = new GridLayout();
        rowLayout.numColumns = 3;
        rowLayout.horizontalSpacing = 2;
        row.setLayout(rowLayout);

        targetDirectoryField = new Text(row, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        targetDirectoryField.setData(new GridData(SWT.FILL, SWT.CENTER, true, false));

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
    public void setJavaProject(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    public void setRunnableContext(IRunnableContext runnableContext) {
        this.runnableContext = runnableContext;
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

    public void load(IProject project) {
        try {
            natureButton.setSelection(project.hasNature(TeaVMEclipsePlugin.NATURE_ID));
            TeaVMProjectSettings settings = TeaVMEclipsePlugin.getDefault().getSettings(project);
            settings.load();
            mainClassField.setText(settings.getMainClass());
            targetDirectoryField.setText(settings.getTargetDirectory());
            updateEnabled(natureButton.getSelection());
        } catch (CoreException e) {
            reportError(e);
        }
    }

    public boolean save(IProject project) {
        try {
            if (natureButton.getSelection()) {
                if (!project.hasNature(TeaVMEclipsePlugin.NATURE_ID)) {
                    addNature(project);
                }
            } else {
                if (project.hasNature(TeaVMEclipsePlugin.NATURE_ID)) {
                    removeNature(project);
                }
            }
            TeaVMProjectSettings settings = TeaVMEclipsePlugin.getDefault().getSettings(project);
            settings.setMainClass(mainClassField.getText().trim());
            settings.setTargetDirectory(targetDirectoryField.getText());
            settings.save();
            return true;
        } catch (CoreException e) {
            reportError(e);
            return false;
        }
    }

    private void addNature(final IProject project) {
        reportStatus(TeaVMEclipsePlugin.getDefault().addNature(runnableContext, project));
    }

    private void removeNature(final IProject project) {
        reportStatus(TeaVMEclipsePlugin.getDefault().removeNature(runnableContext, project));
    }

    private void reportError(Throwable e) {
        reportStatus(TeaVMEclipsePlugin.makeError(e));
    }

    private void reportStatus(IStatus status) {
        if (!status.isOK()) {
            TeaVMEclipsePlugin.getDefault().getLog().log(status);
        }
        if (status.getSeverity() == IStatus.ERROR) {
            ErrorDialog.openError(getShell(), "Error occured", "Error occured", status);
        }
    }
}
