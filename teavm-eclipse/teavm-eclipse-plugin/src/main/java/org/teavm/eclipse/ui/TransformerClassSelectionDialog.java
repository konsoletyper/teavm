package org.teavm.eclipse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.swt.widgets.Shell;
import org.teavm.model.ClassHolderTransformer;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TransformerClassSelectionDialog extends ClassSelectionDialog {
    public TransformerClassSelectionDialog(Shell shell, IJavaProject javaProject) {
        super(shell, javaProject);
        setTitle("Selecting TeaVM transformer");
    }

    @Override
    protected SearchPattern createSearchPattern(String text) {
        return SearchPattern.createPattern(ClassHolderTransformer.class.getName(), IJavaSearchConstants.CLASS,
                IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH);
    }

    @Override
    protected IType acceptMatch(SearchMatch match) throws CoreException {
        return (IType)match.getElement();
    }
}
