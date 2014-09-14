package org.teavm.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
    private TeaVMProjectPropertyWidget widget;

    @Override
    protected Control createContents(Composite parent) {
        widget = new TeaVMProjectPropertyWidget(parent);
        widget.setRunnableContext(PlatformUI.getWorkbench().getProgressService());
        IProject project = (IProject)getElement().getAdapter(IProject.class);
        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                widget.setJavaProject(javaProject);
            }
        } catch (CoreException e) {
            TeaVMEclipsePlugin.logError(e);
        }
        widget.load(project);
        return widget;
    }

    @Override
    public boolean performOk() {
        widget.save((IProject)getElement().getAdapter(IProject.class));
        return super.performOk();
    }
}
