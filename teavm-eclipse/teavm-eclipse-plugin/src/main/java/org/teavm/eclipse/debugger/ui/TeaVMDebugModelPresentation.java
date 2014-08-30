package org.teavm.eclipse.debugger.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.teavm.eclipse.debugger.TeaVMStackFrame;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMDebugModelPresentation extends LabelProvider implements IDebugModelPresentation {
    @Override
    public String getEditorId(IEditorInput input, Object element) {
        if (element instanceof IFile || element instanceof ILineBreakpoint) {
            return JavaUI.ID_CU_EDITOR;
        }
        return null;
    }

    @Override
    public IEditorInput getEditorInput(Object element) {
        if (element instanceof IFile) {
            return new FileEditorInput((IFile)element);
        }
        if (element instanceof ILineBreakpoint) {
            return new FileEditorInput((IFile)((ILineBreakpoint)element).getMarker().getResource());
        }
        return null;
    }

    @Override
    public void computeDetail(IValue arg0, IValueDetailListener arg1) {
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
    }

    @Override
    public String getText(Object element) {
        if (element instanceof TeaVMStackFrame) {
            return ((TeaVMStackFrame)element).getName();
        }
        return super.getText(element);
    }
}
