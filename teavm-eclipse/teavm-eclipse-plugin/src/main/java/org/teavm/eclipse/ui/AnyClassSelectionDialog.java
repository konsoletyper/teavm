package org.teavm.eclipse.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.swt.widgets.Shell;

/**
 *
 * @author Alexey Andreev
 */
public class AnyClassSelectionDialog extends ClassSelectionDialog {
    public AnyClassSelectionDialog(Shell shell, IJavaProject javaProject) {
        super(shell, javaProject);
    }

    @Override
    protected SearchPattern createSearchPattern(String text) {
        return SearchPattern.createPattern("*" + text + "*", IJavaSearchConstants.CLASS,
                IJavaSearchConstants.DECLARATIONS, SearchPattern.R_PATTERN_MATCH);
    }

    @Override
    protected IType acceptMatch(SearchMatch match) throws CoreException {
        return (IType)match.getElement();
    }
}
