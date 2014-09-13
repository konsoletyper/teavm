package org.teavm.eclipse.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectPropertyWidget extends Composite {
    private Button natureButton;

    public TeaVMProjectPropertyWidget(Composite parent) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 12;
        layout.marginWidth = 12;
        setLayout(layout);
        natureButton = new Button(this, SWT.CHECK);
        natureButton.setText("TeaVM build enabled");
    }

    public void load(IProject project) {
        try {
            natureButton.setSelection(project.hasNature(TeaVMEclipsePlugin.NATURE_ID));
        } catch (CoreException e) {
            reportError(e);
        }
    }

    private void reportError(Throwable e) {
        e.printStackTrace();
        Status status = new Status(Status.ERROR, TeaVMEclipsePlugin.ID, getToolTipText(), e);
        ErrorDialog.openError(getShell(), "Error occured", "Error occured", status);
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
            return true;
        } catch (CoreException e) {
            reportError(e);
            return false;
        }
    }

    private void addNature(final IProject project) {
        ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(getShell());
        try {
            progressDialog.run(false, true, new IRunnableWithProgress() {
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
            reportError(e.getTargetException());
        }
    }

    private void removeNature(final IProject project) {
        ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(getShell());
        try {
            progressDialog.run(false, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        String[] natureIds = project.getDescription().getNatureIds();
                        String[] newNatureIds = new String[natureIds.length - 1];
                        for (int i = 0; i < natureIds.length; ++i) {
                            if (natureIds.equals(TeaVMEclipsePlugin.NATURE_ID)) {
                                System.arraycopy(natureIds, 0, newNatureIds, 0, i - 1);
                                System.arraycopy(natureIds, i + 1, newNatureIds, i, newNatureIds.length - i);
                                project.getDescription().setNatureIds(newNatureIds);
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
            reportError(e.getTargetException());
        }
    }
}
