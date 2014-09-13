package org.teavm.eclipse.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMProjectPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
    private TeaVMProjectPropertyWidget widget;

    @Override
    protected Control createContents(Composite parent) {
        widget = new TeaVMProjectPropertyWidget(parent);
        widget.load((IProject)getElement().getAdapter(IProject.class));
        return widget;
    }

    @Override
    public boolean performOk() {
        widget.save((IProject)getElement().getAdapter(IProject.class));
        return super.performOk();
    }
}
