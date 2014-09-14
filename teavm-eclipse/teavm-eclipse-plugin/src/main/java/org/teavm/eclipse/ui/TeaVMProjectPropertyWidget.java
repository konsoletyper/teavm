package org.teavm.eclipse.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectPropertyWidget extends Composite {
    private Button natureButton;
    private Text mainClassField;
    private Button mainClassChooseButton;
    private IJavaProject javaProject;
    private IRunnableContext runnableContext;

    public TeaVMProjectPropertyWidget(Composite parent) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 12;
        layout.marginWidth = 12;
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
    }

    private void createOptionsContainer() {
        Composite container = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 6;
        layout.horizontalSpacing = 6;
        container.setLayout(layout);
        createMainClassField(container);
    }

    private void createMainClassField(Composite container) {
        Label label = new Label(container, SWT.NONE);
        label.setText("Main class:");
        Composite row = new Composite(container, SWT.NONE);
        RowLayout rowLayout = new RowLayout();
        row.setLayout(rowLayout);
        rowLayout.type = SWT.HORIZONTAL;
        rowLayout.spacing = 3;
        mainClassField = new Text(row, SWT.SINGLE | SWT.BORDER);
        RowData rowData = new RowData();
        rowData.width = 300;
        mainClassField.setLayoutData(rowData);
        mainClassChooseButton = new Button(row, SWT.PUSH);
        mainClassChooseButton.setText("Choose...");
        mainClassChooseButton.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                chooseMainClass();
            }
        });
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

    public void setJavaProject(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    public void setRunnableContext(IRunnableContext runnableContext) {
        this.runnableContext = runnableContext;
    }

    public void load(IProject project) {
        try {
            natureButton.setSelection(project.hasNature(TeaVMEclipsePlugin.NATURE_ID));
            ProjectScope scope = new ProjectScope(project);
            IEclipsePreferences preferences = scope.getNode(TeaVMEclipsePlugin.ID);
            preferences.sync();
            mainClassField.setText(preferences.get("mainClass", ""));
            updateEnabled(natureButton.getSelection());
        } catch (CoreException | BackingStoreException e) {
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
            ProjectScope scope = new ProjectScope(project);
            IEclipsePreferences preferences = scope.getNode(TeaVMEclipsePlugin.ID);
            preferences.put("mainClass", mainClassField.getText().trim());
            preferences.flush();
            return true;
        } catch (CoreException | BackingStoreException e) {
            reportError(e);
            return false;
        }
    }

    private void addNature(final IProject project) {
        try {
            runnableContext.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        IProjectDescription projectDescription = project.getDescription();
                        String[] natureIds = projectDescription.getNatureIds();
                        natureIds = Arrays.copyOf(natureIds, natureIds.length + 1);
                        natureIds[natureIds.length - 1] = TeaVMEclipsePlugin.NATURE_ID;
                        projectDescription.setNatureIds(natureIds);
                        project.setDescription(projectDescription, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            reportError(e);
        } catch (InvocationTargetException e) {
            return;
        }
    }

    private void removeNature(final IProject project) {
        try {
            runnableContext.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        IProjectDescription projectDescription = project.getDescription();
                        String[] natureIds = projectDescription.getNatureIds();
                        String[] newNatureIds = new String[natureIds.length - 1];
                        for (int i = 0; i < natureIds.length; ++i) {
                            if (natureIds[i].equals(TeaVMEclipsePlugin.NATURE_ID)) {
                                System.arraycopy(natureIds, 0, newNatureIds, 0, i);
                                System.arraycopy(natureIds, i + 1, newNatureIds, i, newNatureIds.length - i);
                                projectDescription.setNatureIds(newNatureIds);
                                project.setDescription(projectDescription, monitor);
                                break;
                            }
                        }
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            reportError(e);
        } catch (InvocationTargetException e) {
            return;
        }
    }

    private void reportError(Throwable e) {
        IStatus status = TeaVMEclipsePlugin.makeError(e);
        TeaVMEclipsePlugin.getDefault().getLog().log(status);
        ErrorDialog.openError(getShell(), "Error occured", "Error occured", status);
    }
}
